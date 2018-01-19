package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.GenericMultiLevelSource;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
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
import org.esa.snap.core.util.jai.SingleBandedSampleModel;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.UnpackedImageData;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * deglint operator
 */
@OperatorMetadata(alias = "LandCloudWhiteCapMaskOp",
        category = "Optical/Thematic Water Processing/Sen2Coral",
        authors = "Omar Barrilero",
        version = "1.0",
        internal = true,
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

    @Parameter(label = "Include reference bands", defaultValue = "true")
    private Boolean includeReferenceBands = true;

    @Parameter(label = "Inverse mask (by default water=1 Land/Cloud...=0)", defaultValue = "false")
    private Boolean inverseMask = true;

    private final String maskName = "LandCloudWhiteCapMask";
    private final String maskDescription = "Land, Cloud and White Cap Mask";

    //Setters
    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }
    public void setReferenceBandNames(String[] referenceBandNames) {
        this.referenceBandNames = referenceBandNames;
    }
    public void setThresholdString(String thresholdString) {
        this.thresholdString = thresholdString;
    }
    public void setInverseMask(Boolean inverseMask) {
        this.inverseMask = inverseMask;
    }
    public void setIncludeReferenceBands(Boolean includeReferenceBands) {
        this.includeReferenceBands = includeReferenceBands;
    }

    public void setMaskNegativeValues(Boolean maskNegativeValues) {
        this.maskNegativeValues = maskNegativeValues;
    }

    private Map<String, Double> referenceThresholdMap = new HashMap<>();
    private Map<String, String> maskReferenceMap = new HashMap<>();

    @Override
    public void initialize() throws OperatorException {

        //check that there is a source product
        if(getSourceProduct() == null) {
            throw new OperatorException("Source product cannot be null");
        }

        //check that at least one reference band has been selected
        if(referenceBandNames == null || referenceBandNames.length<=0) {
            throw new OperatorException("At least one reference band must be selected");
        }

        //check that it has been introduced one threshold (valid for all the references) or as many thresholds as reference bands
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

        //Compute maximum sceneRaster in bands
        int width = 0, height = 0;
        Set<Integer> distictWidths = new HashSet<>();
        for(String referenceBandName : referenceBandNames) {
            Band band = sourceProduct.getBand(referenceBandName);
            if(width < band.getRasterWidth()) {
                width = band.getRasterWidth();
            }
            if(height < band.getRasterHeight()) {
                height = band.getRasterHeight();
            }
            distictWidths.add(band.getRasterHeight());
        }
        if(sourceBandNames!=null && sourceBandNames.length>0) {
            for(String sourceBandName : sourceBandNames) {
                Band band = sourceProduct.getBand(sourceBandName);
                if(width < band.getRasterWidth()) {
                    width = band.getRasterWidth();
                }
                if(height < band.getRasterHeight()) {
                    height = band.getRasterHeight();
                }
                distictWidths.add(band.getRasterHeight());
            }
        }

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    width,
                                    height);
        targetProduct.setNumResolutionsMax(distictWidths.size());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if(referenceBandNames != null) {
            if(includeReferenceBands) {
                for (String srcBandName : referenceBandNames) {
                    ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
                    ProductUtils.copyGeoCoding(sourceProduct.getRasterDataNode(srcBandName), targetProduct.getRasterDataNode(srcBandName));
                }
            }

            //add masks
            for (String referenceBandName : referenceBandNames) {
                Band band = sourceProduct.getBand(referenceBandName);
                String maskName = getMaskName(band);
                maskReferenceMap.put(maskName,referenceBandName);

                /*//TODO if I want to add as Range masks, then they must be included the reference bands
                if(includeReferenceBands) {
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


                    Rectangle rectangle = new Rectangle(mask.getRasterWidth(), mask.getRasterHeight());
                    try {
                        CrsGeoCoding geoCoding = new CrsGeoCoding(sourceProduct.getSceneCRS(), rectangle, band.getSourceImage().getModel().getImageToModelTransform(0));
                        mask.setGeoCoding(geoCoding);
                    } catch (FactoryException e) {
                        e.printStackTrace();
                    } catch (TransformException e) {
                        e.printStackTrace();
                    }
                    targetProduct.addMask(mask);
                }*/

                //Add as a band
                Band bandMask = new Band(maskName, ProductData.TYPE_INT8, band.getRasterWidth(), band.getRasterHeight());
                Rectangle rectangle = new Rectangle(bandMask.getRasterWidth(), bandMask.getRasterHeight());
                bandMask.setDescription(maskDescription);
                RangeMultiLevelSource multiLevelSource = null;
                double scale = band.getScalingFactor();
                double offset = band.getScalingOffset();
                if(!inverseMask) {
                    multiLevelSource = new RangeMultiLevelSource(sourceProduct.getBand(referenceBandName).getSourceImage(), Double.MIN_VALUE, (referenceThresholdMap.get(referenceBandName)-offset)/scale);
                } else {
                    multiLevelSource = new RangeMultiLevelSource(sourceProduct.getBand(referenceBandName).getSourceImage(), (referenceThresholdMap.get(referenceBandName)-offset)/scale, Double.MAX_VALUE);
                }
                bandMask.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
                try {
                    CrsGeoCoding geoCoding = new CrsGeoCoding(sourceProduct.getSceneCRS(), rectangle, band.getSourceImage().getModel().getImageToModelTransform(0));
                    bandMask.setGeoCoding(geoCoding);
                } catch (FactoryException e) {
                    e.printStackTrace();
                } catch (TransformException e) {
                    e.printStackTrace();
                }
                targetProduct.addBand(bandMask);

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
                if(maskNegativeValues) {
                    validExpression = validExpression + " && " + srcBand.getName() + ".raw > 0";
                }
                targetBand.setValidPixelExpression(validExpression);
                targetBand.setSourceImage(srcBand.getSourceImage());
                ProductUtils.copyGeoCoding(srcBand, targetBand);
                targetProduct.addBand(targetBand);
            }
        }
    }

    /*@Override
    public void computeTile (Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            Rectangle rectangle = targetTile.getRectangle();
            Tile sourceTile = getSourceTile(this.sourceProduct.getBand(maskReferenceMap.get(targetBand.getName())), rectangle);
            Band reference = getSourceProduct().getBand(maskReferenceMap.get(targetBand.getName()));
            String referenceName = reference.getName();

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    targetTile.setSample(x,y,sourceTile.getSampleFloat(x,y)>referenceThresholdMap.get(referenceName));
                }
            }
        } finally {
            pm.done();
        }
    }*/


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
