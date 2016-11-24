package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.PointOperator;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * deglint operator
 */
@OperatorMetadata(alias = "DeglintOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        description = "Deglint algorithm")
public class DeglintOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;
    //@TargetProduct
    //private Product targetProduct;

    @Parameter(description = "Sun Glint areas", label = "Sun Glint Areas")
    private String sunGlintVector;

    @Parameter(description = "The list of source bands to be corrected", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "NIR band to be used as glint reference", alias = "referenceBand",
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String referenceBand;

    @Parameter(defaultValue = "-1.0", description = "Minimum expected NIR value in the absence of glint")
    private Double minNIR = -1.0;

    @Parameter(label = "Mask all negative reflectance values", defaultValue = "true")
    private Boolean maskNegativeValues = true;


    private final static String tmpVirtBandName = "_tmpVirtBand";

    //Processing parameters
    SimpleRegression[] regressions = null;
    double calculatedMinNIR = Double.MAX_VALUE;
    private double noDataValue[] = null;




    @Override
    protected Product createTargetProduct() throws OperatorException {
        int sceneWidth = 0, sceneHeight = 0;
        Set<Integer> distictWidths = new HashSet<>();
        for (Band band : sourceProduct.getBands()) {
            if (sceneWidth < band.getRasterWidth()) {
                sceneWidth = band.getRasterWidth();
                sceneHeight = band.getRasterHeight();
            }
            distictWidths.add(band.getRasterHeight());
        }
        Product targetProduct = new Product(sourceProduct.getName() + "_deglint", sourceProduct.getProductType(), sceneWidth, sceneHeight);

        targetProduct.setNumResolutionsMax(distictWidths.size());

        ProductUtils.copyProductNodes(sourceProduct,targetProduct);

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        copyMasks(sourceProduct, targetProduct, sourceBandNames);

        ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);

        Band[] sourceBands = new Band[sourceProduct.getBands().length];


        for (int i = 0; i < sourceProduct.getBands().length; i++) {
            boolean isComputedBand = false;
            Band sourceBand = sourceProduct.getBand(sourceProduct.getBands()[i].getName());
            sourceBands[i] = this.sourceProduct.getBand(sourceProduct.getBands()[i].getName());
            int sourceBandWidth = sourceBands[i].getRasterWidth();
            int sourceBandHeight = sourceBands[i].getRasterHeight();

            Band targetBand = null;

            //add sourceImage depending on the band
            for(String sourceBandName : sourceBandNames) {
                if(sourceBandName.equals(sourceProduct.getBands()[i].getName())) {
                    isComputedBand = true;
                    break;
                }
            }
            if(!isComputedBand) {
                targetBand = new Band(sourceProduct.getBands()[i].getName(), sourceProduct.getBands()[i].getDataType(), sourceBandWidth, sourceBandHeight);
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            } else {
                targetBand = new Band(sourceProduct.getBands()[i].getName(), ProductData.TYPE_FLOAT32, sourceBandWidth, sourceBandHeight);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
                targetBand.setDescription(sourceBand.getDescription() + "(deglint applied)");
                targetBand.setUnit(" ");
                targetBand.setScalingFactor(1.0);
                targetBand.setScalingOffset(0);
                targetBand.setLog10Scaled(false);
                targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
                targetBand.setNoDataValue(sourceBand.getNoDataValue());
                targetBand.setValidPixelExpression(" ");
            }
            targetBand.setGeoCoding(sourceBand.getGeoCoding());

            targetProduct.addBand(targetBand);
        }

        return targetProduct;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        //TODO comprueba tamano de sourcebands y reference bands
        ensureSingleRasterSize(sourceProduct, sourceBandNames);

        if (sunGlintVector != null && !sunGlintVector.isEmpty()) {
            //todo throw...
        }

        noDataValue = new double[sourceBandNames.length];
        for(int i = 0 ; i < sourceBandNames.length; i++) {
            noDataValue[i] = sourceProduct.getBand(sourceBandNames[i]).getNoDataValue();
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        for(int i = 0 ; i<sourceBandNames.length ; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
        sampleConfigurer.defineSample(sourceBandNames.length, referenceBand);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        for(int i = 0 ; i<sourceBandNames.length ; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
    }


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        regressions = getRegressions();
        if(minNIR < 0) {
            minNIR = calculatedMinNIR;
        }
        for(int i = 0 ; i<sourceBandNames.length ; i++) {
            double correctedValue = sourceSamples[i].getFloat()-regressions[i].getSlope()*(sourceSamples[sourceBandNames.length].getFloat()-minNIR);
            correctedValue = (correctedValue >= 0) ? correctedValue : noDataValue[i];
            targetSamples[i].set(correctedValue);
        }
    }

    private synchronized SimpleRegression[] getRegressions() {
        if (regressions == null) {
            regressions = new SimpleRegression[sourceBandNames.length];
            int iCounter = 0;
            //pm.beginTask("Computing linear regression...", sourceBandNames.length);
            try {
                final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
                for (Band srcBand : sourceBands) {
                    regressions[iCounter] = new SimpleRegression();

                    //create mask
                    final Mask mask = new Mask("tempMask",
                                               srcBand.getRasterWidth(),
                                               srcBand.getRasterHeight(),
                                               Mask.VectorDataType.INSTANCE);
                    Mask.VectorDataType.setVectorData(mask, sourceProduct.getVectorDataGroup().get(sunGlintVector));
                    ProductUtils.copyImageGeometry(srcBand, mask, false);

                    //get noData values to exclude them
                    double noData = srcBand.getNoDataValue();
                    double referenceNoData = sourceProduct.getRasterDataNode(referenceBand).getNoDataValue();

                    //load data if it is not loaded
                    try {
                        mask.loadRasterData();
                        srcBand.loadRasterData();
                        sourceProduct.getRasterDataNode(referenceBand).loadRasterData();
                    } catch (IOException e) {
                        //todo throw?
                        e.printStackTrace();
                    }

                    //add data to regressions
                    for (int i = 0; i < srcBand.getRasterWidth(); i++) {
                        for (int j = 0; j < srcBand.getRasterHeight(); j++) {
                            if (mask.getPixelInt(i, j) == 0) {
                                continue;
                            }
                            double value = srcBand.getPixelDouble(i, j);
                            if (value != noData) {
                                double referenceValue = sourceProduct.getRasterDataNode(referenceBand).getPixelDouble(i, j);
                                if (referenceValue == referenceNoData) {
                                    continue;
                                }
                                if(referenceValue<calculatedMinNIR) calculatedMinNIR = referenceValue;
                                regressions[iCounter].addData(referenceValue, value);
                            }
                        }
                    }
                    iCounter++;
                    mask.dispose();
                    //pm.worked(1);
                }
            } finally {
                //pm.done();

            }
        }
        return regressions;
    }

    private Dimension ensureSingleRasterSize(Product product, String[] sourceBandNames) throws OperatorException{
        Dimension rasterSize = null;
        if(sourceBandNames == null || sourceBandNames.length < 1) {
            throw new OperatorException("At least one source band should be selected");
        }
        RasterDataNode firstRasterDataNode = product.getRasterDataNode(sourceBandNames[0]);
        if(firstRasterDataNode == null) {
            throw new OperatorException(String.format("Error when reading band: %s ",
                                                      firstRasterDataNode.getName()));
        }
        rasterSize = firstRasterDataNode.getRasterSize();
        for (String sourceBandName : sourceBandNames) {
            if (rasterSize == null || !rasterSize.equals(product.getRasterDataNode(sourceBandName).getRasterSize())) {
                throw new OperatorException(String.format("All source rasters must have the same size of %d x %d pixels.",
                                                          rasterSize.width,
                                                          rasterSize.height));
            }
        }
        return rasterSize;
    }

    private void copyMasks(Product sourceProduct, Product targetProduct, String...bandNames) {
        //TODO revisar de ReflectanceToRadianceOp
            final ProductNodeGroup<Mask> sourceMaskGroup = sourceProduct.getMaskGroup();
            for (int i = 0; i < sourceMaskGroup.getNodeCount(); i++) {
                final Mask mask = sourceMaskGroup.get(i);
                if (!targetProduct.getMaskGroup().contains(mask.getName())) {
                    mask.getImageType().transferMask(mask, targetProduct);
                }
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
            super(DeglintOp.class);
        }
    }
}
