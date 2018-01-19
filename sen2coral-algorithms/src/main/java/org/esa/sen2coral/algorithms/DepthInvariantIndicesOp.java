package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.analysis.function.Log;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.sen2coral.algorithms.utils.PerpendicularOffsetRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
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
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Depth invariant indices operator
 */
@OperatorMetadata(alias = "DepthInvariantIndicesOp",
        category = "Optical/Thematic Water Processing/Sen2Coral",
        authors = "Omar Barrilero",
        version = "1.0",
        internal = true,
        description = "DepthInvariantIndicesOp algorithm")
public class DepthInvariantIndicesOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "Deep water area", label = "Deep water area")
    private String deepWaterVector;

    @Parameter(description = "Same bottom areas", label = "Same bottom areas")
    private String sameBottomVectors;

    //Processing parameters
    private double[] deepWaterReflectance = null;
    PerpendicularOffsetRegression perpOffsetRegression = null;
    private double attenuationCoeffRatio = 0.0;
    private double[] noDataValue = null;

    //Setters
    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }
    public void setDeepWaterVector(String deepWaterVector) {
        this.deepWaterVector = deepWaterVector;
    }
    public void setSameBottomVectors(String sameBottomVectors) {
        this.sameBottomVectors = sameBottomVectors;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        if(getSourceProduct() == null) {
            throw new OperatorException("Source product cannot be null");
        }

        if(sourceBandNames == null || sourceBandNames.length != 2) {
            throw new OperatorException("Two source bands must be selected");
        }
        //check raster size
        ensureSingleRasterSize(sourceProduct, sourceBandNames);

        //check vectors
        if (deepWaterVector == null || deepWaterVector.isEmpty() || sourceProduct.getVectorDataGroup().get(deepWaterVector) == null) {
            throw new OperatorException(String.format("Error when reading deep water areas in %s. It must contain at least one polygon.",
                                                      deepWaterVector));
        }

        if (sameBottomVectors == null || sameBottomVectors.isEmpty() || sourceProduct.getVectorDataGroup().get(sameBottomVectors) == null) {
            throw new OperatorException(String.format("Error when reading \"same bottom\" areas in %s. It must contain at least one polygon.",
                                                      sameBottomVectors));
        }

        //load noDataValues
        noDataValue = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = getSourceProduct().getBand(sourceBandNames[i]).getNoDataValue();
        }
    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        int sceneWidth = sourceProduct.getBand(sourceBandNames[0]).getRasterWidth();
        int sceneHeight = sourceProduct.getBand(sourceBandNames[0]).getRasterHeight();


        Product targetProduct = new Product(sourceProduct.getName() + "_invariantIndices", sourceProduct.getProductType(), sceneWidth, sceneHeight);
        targetProduct.setNumResolutionsMax(1);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        //Add band
        Band targetBand = new Band("DepthInvariantIndices" + sourceBandNames[0] + sourceBandNames[1], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        targetBand.setDescription("Depth invariant indices calculated with bands " + sourceBandNames[0] + " and " + sourceBandNames[1]);
        targetBand.setUnit(" ");
        targetBand.setScalingFactor(1.0);
        targetBand.setScalingOffset(0);
        targetBand.setLog10Scaled(false);
        targetBand.setNoDataValueUsed(true);
        targetBand.setNoDataValue(0.0);
        targetBand.setValidPixelExpression(" ");
        targetBand.setGeoCoding(sourceProduct.getBand(sourceBandNames[0]).getGeoCoding());
        targetProduct.addBand(targetBand);

        return targetProduct;
    }


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        double source1 = sourceSamples[0].getDouble();
        double source2 = sourceSamples[1].getDouble();

        if(source1 == noDataValue[0] || source2 == noDataValue[1]) {
            targetSamples[0].set(getTargetProduct().getBandAt(0).getNoDataValue());
            return;
        }

        double dif1 = source1 - getDeepWaterReflectance()[0];
        double dif2 = source2 - getDeepWaterReflectance()[1];

        if(dif1 <= 0) dif1 = 0.01;
        if(dif2 <= 0) dif2 = 0.01;

        double Xi = Math.log(dif1);
        double Xj = Math.log(dif2);
        targetSamples[0].set(Xi - getAttenuationCoeffRatio() * Xj);

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        for(int i = 0 ; i<sourceBandNames.length ; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, getTargetProduct().getBandAt(0).getName());
    }

    public synchronized double[] getDeepWaterReflectance() {
        if (deepWaterReflectance == null) {
            Band srcBand1 = sourceProduct.getBand(sourceBandNames[0]);
            Band srcBand2 = sourceProduct.getBand(sourceBandNames[1]);

            //create mask
            final Mask mask = new Mask("tempMask",
                                       srcBand1.getRasterWidth(),
                                       srcBand1.getRasterHeight(),
                                       Mask.VectorDataType.INSTANCE);
            Mask.VectorDataType.setVectorData(mask, sourceProduct.getVectorDataGroup().get(deepWaterVector));
            ProductUtils.copyImageGeometry(srcBand1, mask, false);

            //get noData values to exclude them
            double noData1 = srcBand1.getNoDataValue();
            double noData2 = srcBand2.getNoDataValue();

            //load data if it is not loaded
            try {
                mask.loadRasterData();
                srcBand1.loadRasterData();
                srcBand2.loadRasterData();
            } catch (IOException e) {
                throw new OperatorException("Unable to load the raster data.");
            }

            //compute mean reflectance
            int numPixels1 = 0;
            int numPixels2 = 0;
            double pixelSum1 = 0.0;
            double pixelSum2 = 0.0;
            for (int i = 0; i < srcBand1.getRasterWidth(); i++) {
                for (int j = 0; j < srcBand1.getRasterHeight(); j++) {
                    if (mask.getPixelInt(i, j) == 0) {
                        continue;
                    }
                    double value1 = srcBand1.getPixelDouble(i, j);
                    double value2 = srcBand2.getPixelDouble(i, j);
                    if (value1 != noData1) {
                        pixelSum1 = pixelSum1 + value1;
                        numPixels1++;
                    }
                    if (value2 != noData2) {
                        pixelSum2 = pixelSum2 + value2;
                        numPixels2++;
                    }
                }
            }
            mask.dispose();

            if (numPixels1 > 0 && numPixels2 > 0) {
                deepWaterReflectance = new double[2];
                deepWaterReflectance[0] = pixelSum1 / numPixels1;
                deepWaterReflectance[1] = pixelSum2 / numPixels2;
            } else {
                throw new OperatorException("Unable to compute deep water reflectance. " +
                                                    "There are no valid pixels inside the deep water area selected.");
            }
        }
        return deepWaterReflectance;
    }


    public synchronized double getAttenuationCoeffRatio() {
        if (attenuationCoeffRatio == 0.0) {
            try {
                attenuationCoeffRatio = getPerpendicularOffsetRegression().getSlope();
            } catch (Exception e){
                throw new OperatorException("Unable to compute the ratio of attenuation coefficients. " +
                                                    "There are no valid pixels inside the \"same bottom\" areas selected.");
            }
        }
        return attenuationCoeffRatio;
    }

    // Public to be used by DepthInvariantIndicesTest
    public synchronized PerpendicularOffsetRegression getPerpendicularOffsetRegression() {

        if(perpOffsetRegression == null) {
            perpOffsetRegression = new PerpendicularOffsetRegression();

            Band srcBand1 = sourceProduct.getBand(sourceBandNames[0]);
            Band srcBand2 = sourceProduct.getBand(sourceBandNames[1]);

            //create mask
            final Mask mask = new Mask("tempMask",
                                       srcBand1.getRasterWidth(),
                                       srcBand1.getRasterHeight(),
                                       Mask.VectorDataType.INSTANCE);
            Mask.VectorDataType.setVectorData(mask, sourceProduct.getVectorDataGroup().get(sameBottomVectors));
            ProductUtils.copyImageGeometry(srcBand1, mask, false);

            //get noData values to exclude them
            double noData1 = srcBand1.getNoDataValue();
            double noData2 = srcBand2.getNoDataValue();

            //load data if it is not loaded
            try {
                mask.loadRasterData();
                srcBand1.loadRasterData();
                srcBand2.loadRasterData();
            } catch (IOException e) {
                throw new OperatorException("Unable to load the raster data.");
            }

            //addData to regression
            for (int i = 0; i < srcBand1.getRasterWidth(); i++) {
                for (int j = 0; j < srcBand1.getRasterHeight(); j++) {
                    if (mask.getPixelInt(i, j) == 0) {
                        continue;
                    }
                    double value1 = srcBand1.getPixelDouble(i, j);
                    double value2 = srcBand2.getPixelDouble(i, j);

                    if (value1 != noData1 && value2 != noData2 &&  (value1 - getDeepWaterReflectance()[0])!= 0  && (value2 - getDeepWaterReflectance()[1])!= 0) {
                        double X = Math.log(value1 - getDeepWaterReflectance()[0]);
                        double Y = Math.log(value2 - getDeepWaterReflectance()[1]);
                        perpOffsetRegression.addData(X, Y);
                    }
                }
            }
            mask.dispose();
        }

        return perpOffsetRegression;
    }

    @Override
    public void dispose() {
        super.dispose();
        deepWaterReflectance = null;
        perpOffsetRegression = null;
        attenuationCoeffRatio = 0.0;
        noDataValue = null;
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
            super(DepthInvariantIndicesOp.class);
        }
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
}

