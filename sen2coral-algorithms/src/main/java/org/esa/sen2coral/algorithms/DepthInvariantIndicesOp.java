package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.analysis.function.Log;
import org.apache.commons.math3.stat.regression.SimpleRegression;
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
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
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
    double[] deepWaterReflectance = null;
    double attenuationCoeffRatio = 0.0;
    private double noDataValue[] = {0.0,0.0};

    @Override
    protected void prepareInputs() throws OperatorException {
        //TODO comprueba tamano de sourcebands, q solo haya 2...
        ensureSingleRasterSize(sourceProduct, sourceBandNames);

        //TODO check vector

        //TODO load noDataValues
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

        if(source1 == noDataValue[0] || source2 == noDataValue[1] || source1 <= getDeepWaterReflectance()[0] || source2 <= getDeepWaterReflectance()[1]) {
            targetSamples[0].set(getTargetProduct().getBandAt(0).getNoDataValue());
        }
        else {
            double Xi = Math.log(source1 - getDeepWaterReflectance()[0]);
            double Xj = Math.log(source2 - getDeepWaterReflectance()[1]);
            targetSamples[0].set(Xi - getAttenuationCoeffRatio() * Xj);
        }
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

    private synchronized double[] getDeepWaterReflectance() {
        if (deepWaterReflectance == null) {
            Band srcBand1 = sourceProduct.getBand(sourceBandNames[0]);
            Band srcBand2 = sourceProduct.getBand(sourceBandNames[0]);

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
                //todo throw?
                e.printStackTrace();
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
            //TODO do something if numPixels is 0
            if (numPixels1 > 0 && numPixels2 > 0) {
                deepWaterReflectance = new double[2];
                deepWaterReflectance[0] = pixelSum1 / numPixels1;
                deepWaterReflectance[1] = pixelSum2 / numPixels2;
            }
            mask.dispose();
        }
        return deepWaterReflectance;
    }


    private synchronized double getAttenuationCoeffRatio() {
        if (attenuationCoeffRatio == 0.0) {
            Band srcBand1 = sourceProduct.getBand(sourceBandNames[0]);
            Band srcBand2 = sourceProduct.getBand(sourceBandNames[0]);

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
                //todo throw?
                e.printStackTrace();
            }

            //compute statistics
            int numPixels = 0;
            double sumX = 0.0;
            double sumY = 0.0;
            double sumXX = 0.0;
            double sumYY = 0.0;
            double sumXY = 0.0;
            double meanX, meanY, sigmaXX, sigmaXY, sigmaYY, a;
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
                        sumX = sumX + X;
                        sumY = sumY + Y;
                        sumXX = sumXX + X*X;
                        sumYY = sumYY + Y*Y;
                        sumXY = sumXY + X*Y;
                        numPixels++;
                    }
                }
            }

            //TODO do something if numPixels is 0
            if (numPixels > 0) {
                meanX = sumX/numPixels;
                meanY = sumY/numPixels;
                sigmaXX = sumXX/numPixels-meanX*meanX;
                sigmaXY = sumXY/numPixels-meanX*meanY;
                sigmaYY = sumYY/numPixels-meanY*meanY;
                a = (sigmaXX-sigmaYY)/(2.0*sigmaXY);
                attenuationCoeffRatio = a + Math.sqrt(a*a+1);
            }
            mask.dispose();
        }
        return attenuationCoeffRatio;
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

