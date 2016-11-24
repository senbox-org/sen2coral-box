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


    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String[] referenceBandNames;

    @Parameter(defaultValue = "0.10", description = "Maximum valid value", label = "Maximum valid value")
    private Double threshold = 0.10;

    @Parameter(label = "Apply mask to all bands", defaultValue = "true")
    private Boolean applyMask = true;


    @Parameter(label = "Mask all negative reflectance values", defaultValue = "true")
    private Boolean maskNegativeValues = true;

    private final String maskName = "LandCloudWhiteCapMask";
    private final String maskDescription = "Land, Cloud and White Cap Mask";

    private Map<Band, String> expressionMap = new HashMap<>();



    @Override
    public void initialize() throws OperatorException {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);


        for (String srcBandName : referenceBandNames) {
            ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
        }


        if(applyMask) {
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
                ProductUtils.copyGeoCoding(srcBand,targetBand);
                targetProduct.addBand(targetBand);

                expressionMap.put(targetBand, createExpression(srcBand, getReferenceBandName(srcBand.getName())));
            }
        }

        Set<Dimension> distictDimension = new HashSet<>();

        //add masks

        for (String referenceBandName : referenceBandNames) {
            Band band = sourceProduct.getBand(referenceBandName);
            if(distictDimension.add(band.getRasterSize())) {
                Mask mask = Mask.BandMathsType.create(String.format("%s_%s", maskName, band.getName()), maskDescription, band.getRasterWidth(), band.getRasterHeight(),
                                                      String.format("%s.raw < %f", referenceBandName, threshold), Color.CYAN, 0.5);

                ProductUtils.copyGeoCoding(band,mask);
                targetProduct.addMask(mask);
            }
        }


    }

    private String createExpression(final Band srcBand, String referenceBandName) {
        final StringBuilder str = new StringBuilder("");

        str.append("(");
        str.append(referenceBandName);
        str.append(" <= ");
        str.append(threshold);
        str.append(") ? ");
        str.append(srcBand.getName());
        str.append(" : ");
        str.append(srcBand.getNoDataValue());

        return str.toString();
    }

    @Override
    public synchronized void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle rect = targetTile.getRectangle();
            final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);

            final String expression = expressionMap.get(targetBand);
            final Term term = createTerm(expression);

            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            for (RasterDataSymbol symbol : refRasterDataSymbols) {
                final Tile tile = getSourceTile(symbol.getRaster(), rect);
                symbol.setData(tile.getRawSamples());
            }

            pm.beginTask("Evaluating expression", rect.height);
            int pixelIndex = 0;
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    env.setElemIndex(pixelIndex);

                    double val = term.evalD(env);
                    targetTile.setSample(x, y, val);
                    pixelIndex++;
                }
                pm.worked(1);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private Term createTerm(String expression) {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{sourceProduct}, 0,
                                                                            new SourceProductPrefixProvider());
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    private String getReferenceBandName(String srcBand) {
        if(srcBand == null || referenceBandNames == null || referenceBandNames.length<1) {
            return null;
        }
        for(String referenceBandName : referenceBandNames) {
            if(sourceProduct.getBand(referenceBandName).getRasterSize().equals(sourceProduct.getBand(srcBand).getRasterSize())) {
                return referenceBandName;
            }
        }
        return null;
    }

    private static class SourceProductPrefixProvider implements ProductNamespacePrefixProvider {

        @Override
        public String getPrefix(Product product) {
            //return "$" + getSourceProductId(product) + ".";
            return BandArithmetic.getProductNodeNamePrefix(product);
        }
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
