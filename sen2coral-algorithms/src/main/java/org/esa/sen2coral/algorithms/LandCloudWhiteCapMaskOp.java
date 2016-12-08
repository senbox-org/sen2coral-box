package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.ProductNamespacePrefixProvider;
import org.esa.snap.core.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * deglint operator
 */
@OperatorMetadata(alias = "LandCloudWhiteCapMaskOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        description = "LandCloudWhiteCapMaskOp algorithm")
public class LandCloudWhiteCapMaskOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands to be masked", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;


    @Parameter(description = "The list of reference bands.", alias = "referenceBands",
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String[] referenceBandNames;

    @Parameter(defaultValue = "0.10", description = "Maximum valid value", label = "Maximum valid value")
    private String thresholdString = "0.10";

    @Parameter(label = "Mask all negative reflectance values", defaultValue = "true")
    private Boolean maskNegativeValues = true;

    private final String maskName = "LandCloudWhiteCapMask";
    private final String maskDescription = "Land, Cloud and White Cap Mask";

    private Map<String, Double> referenceThresholdMap = new HashMap<>();

    @Override
    public void initialize() throws OperatorException {

        if(getSourceProduct() == null) {
            throw new OperatorException("Source product cannot be null");
        }

        if(referenceBandNames != null && referenceBandNames.length<=0) {
            throw new OperatorException("At least one reference band must be selected");
        }
        String thresholdSplit[] = thresholdString.split(";");
        if(thresholdSplit.length == 1) {
            for(String referenceBandName : referenceBandNames) {
                try {
                    referenceThresholdMap.put(referenceBandName,Double.parseDouble(thresholdSplit[0]));
                } catch (Exception e) {
                    throw new OperatorException(e.getMessage());
                }
            }
        } else if (thresholdSplit.length == referenceBandNames.length) {
            for(int i = 0; i < referenceBandNames.length; i++) {
                try {
                    referenceThresholdMap.put(referenceBandNames[i],Double.parseDouble(thresholdSplit[i]));
                } catch (Exception e) {
                    throw new OperatorException(e.getMessage());
                }
            }
        } else {
            throw new OperatorException("Maximum valid value must be one value or as many values as selected reference bands separated by ';'. For example: 0.7;0.3;0.45 if three reference bands have been selected");
        }



        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);


        if(referenceBandNames != null) {
            for (String srcBandName : referenceBandNames) {
                ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
            }

            //add masks
            for (String referenceBandName : referenceBandNames) {
                Band band = sourceProduct.getBand(referenceBandName);

                Mask mask = new Mask(getMaskName(band), band.getRasterWidth(), band.getRasterHeight(), Mask.RangeType.INSTANCE);
                mask.setDescription(maskDescription);
                mask.setImageColor(Color.CYAN);
                mask.setImageTransparency(0.5);
                Mask.RangeType.setRasterName(mask, referenceBandName);
                Mask.RangeType.setMaximum(mask, referenceThresholdMap.get(referenceBandName));
                if (maskNegativeValues) {
                    Mask.RangeType.setMinimum(mask, 0.0);
                } else {
                    Mask.RangeType.setMinimum(mask, Double.MIN_VALUE);
                }

                ProductUtils.copyGeoCoding(band, mask);
                targetProduct.addMask(mask);
            }
        }

        if(sourceBandNames != null) {
            final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
            for (Band srcBand : sourceBands) {
                final String targetBandName = srcBand.getName();
                final Band targetBand = new Band(targetBandName,
                                                 srcBand.getDataType(),
                                                 srcBand.getRasterWidth(),
                                                 srcBand.getRasterHeight());

                targetBand.setUnit(srcBand.getUnit());
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setNoDataValueUsed(true);
                String validExpression = "";
                for (String maskName : getMaskNames(srcBand.getRasterSize())) {
                    if (validExpression.length() == 0) {
                        validExpression = maskName;
                    } else {
                        validExpression = validExpression + " && " + maskName;
                    }
                }
                targetBand.setValidPixelExpression(validExpression);
                targetBand.setSourceImage(srcBand.getSourceImage());
                ProductUtils.copyGeoCoding(srcBand, targetBand);
                targetProduct.addBand(targetBand);
            }
        }
    }

    private String getMaskName(Band referenceBand) {
        return String.format("%s_%s", maskName, referenceBand.getName());
    }

    private ArrayList<String> getMaskNames(Dimension dim) {
        ArrayList<String> list = new ArrayList<>();
        for(String referenceBandName : referenceBandNames) {
            Band refBand = sourceProduct.getBand(referenceBandName);
            if(refBand.getRasterSize().equals(dim)) {
                list.add(getMaskName(refBand));
            }
        }
        return list;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(LandCloudWhiteCapMaskOp.class);
        }
    }
}
