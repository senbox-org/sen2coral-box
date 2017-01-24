package org.esa.sen2coral.algorithms;


import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.sen2coral.algorithms.utils.PerpendicularOffsetRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Empirical Bathymetry operator
 */
@OperatorMetadata(alias = "EmpiricalBathymetryOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        internal = true,
        description = "EmpiricalBathymetryOp algorithm")
public class EmpiricalBathymetryOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;


    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Bands to use")
    private String[] sourceBandNames;

    @Parameter(label = "Bathymetry point data")
    private File bathymetryFile = null;

    @Parameter(defaultValue = "10000", description = "'n' default value")
    private Double nValue = 10000.0;

    //TODO future implementation?
    //@Parameter(defaultValue = "0.7", description = "Minimum r-squared value")
    //private Double minRSquared = 0.7;
    //@Parameter(defaultValue = "5", description = "Maximum number of steps")
    //private int maxSteps = 5;

    //processing parameters
    private int currentStep = 0;
    private double[] noDataValue = null;
    private double[] regressionCoefficients = null; //m0, m1, r-squared
    private ArrayList<BathymetryPoint> bathymetryPointList = null;

    //Setters
    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }
    public void setBathymetryFile(File bathymetryFile) {
        this.bathymetryFile = bathymetryFile;
    }
    public void setnValue(Double nValue) {
        this.nValue = nValue;
    }

    //getters for testing
    public double getM0() {
        return getRegressionCoefficients()[0];
    }
    public double getM1() {
        return getRegressionCoefficients()[1];
    }
    public double getRSquared() {
        return getRegressionCoefficients()[2];
    }
    public double getRegressionPointCount() {
        return getRegressionCoefficients()[3];
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

        //check bathymetry file
        if(bathymetryFile == null) {
            throw new OperatorException("A bathymetry file must be selected.");
        }
        if(!bathymetryFile.exists()) {
            throw new OperatorException("Bathymetry file does not exist.");
        }

        bathymetryPointList = getBathymetryPointData(bathymetryFile);
        if(bathymetryPointList == null || bathymetryPointList.size() <= 2) {
            throw new OperatorException("Unable to obtain at least two points from the bathymetry file selected.");
        }

        //load noDataValues
        noDataValue = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = getSourceProduct().getBand(sourceBandNames[i]).getNoDataValue();
        }

        //todo in future? check rSquared
        //while(getRegressionCoefficients()[2] < minRSquared && currentStep < maxSteps) {
        //    regressionCoefficients = null;
        //    currentStep++;
        //    //TODO define a new method to compute the new value of nValue
        //    nValue = nValue*1000;
        //}
    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        if(sourceBandNames != null && sourceProduct != null) {
            int sceneWidth = sourceProduct.getBand(sourceBandNames[0]).getRasterWidth();
            int sceneHeight = sourceProduct.getBand(sourceBandNames[0]).getRasterHeight();


            Product targetProduct = new Product(sourceProduct.getName() + "_empiricalBathymetry", sourceProduct.getProductType(), sceneWidth, sceneHeight);
            targetProduct.setNumResolutionsMax(1);

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            //Add band
            Band targetBand = new Band("EmpiricalBathymetry_" + sourceBandNames[0] + sourceBandNames[1], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            targetBand.setDescription("EmpiricalBathymetry " + sourceBandNames[0] + " and " + sourceBandNames[1]);
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
        return new Product("_empiricalBathymetry", "dummy", 2, 2);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double source1 = sourceSamples[0].getDouble();
        double source2 = sourceSamples[1].getDouble();

        if(source1 == noDataValue[0] || source2 == noDataValue[1]) {
            targetSamples[0].set(getTargetProduct().getBandAt(0).getNoDataValue());
        }
        else {

            targetSamples[0].set(getRegressionCoefficients()[1] * (Math.log(nValue*source1)/Math.log(nValue*source2)) + getRegressionCoefficients()[0]);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        if(sourceBandNames != null) {
            for (int i = 0; i < sourceBandNames.length; i++) {
                sampleConfigurer.defineSample(i, sourceBandNames[i]);
            }
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        //if(getTargetProduct() != null) {
            sampleConfigurer.defineSample(0, getTargetProduct().getBandAt(0).getName());
        //}
    }

    private synchronized double[] getRegressionCoefficients() {
        if (regressionCoefficients == null) {
            regressionCoefficients = new double[4];
            SimpleRegression regression = new SimpleRegression();
            PerpendicularOffsetRegression regressionTest = new PerpendicularOffsetRegression();
            Band band1 = sourceProduct.getBand(sourceBandNames[0]);
            Band band2 = sourceProduct.getBand(sourceBandNames[1]);
            try {
                band1.loadRasterData();
                band2.loadRasterData();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(BathymetryPoint bathymetryPoint : bathymetryPointList) {
                PixelPos pixelPos = band1.getGeoCoding().getPixelPos(new GeoPos(bathymetryPoint.getLat(),bathymetryPoint.getLon()),null);
                // It is supposed that the position is the same for both bands

                //check pixel positions obtained
                if(!pixelPos.isValid()) {
                    continue;
                }
                int pixelPosX = (int) Math.floor(pixelPos.x);
                int pixelPosY = (int) Math.floor(pixelPos.y);

                if(pixelPosX < 0 || pixelPosY < 0 || pixelPosX >= band1.getRasterWidth() || pixelPosY >= band1.getRasterHeight()) {
                    continue;
                }

                double valueBand1 = band1.getPixelDouble(pixelPosX,pixelPosY);
                double valueBand2 = band2.getPixelDouble(pixelPosX,pixelPosY);

                if(valueBand1 != band1.getNoDataValue() && valueBand2 != band2.getNoDataValue() && ((nValue * valueBand1) > 1) && ((nValue * valueBand2) > 1)) {
                    regression.addData(Math.log(nValue * valueBand1) / Math.log(nValue * valueBand2), bathymetryPoint.getZ());
                    regressionTest.addData(Math.log(nValue * valueBand1) / Math.log(nValue * valueBand2), bathymetryPoint.getZ());
                }
            }
            if(regression.getN() < 2) {
                throw new OperatorException("Unable to find at least two points from the bathymetry file in the source image.");
            }

            regressionCoefficients[0] = regression.getIntercept();
            regressionCoefficients[1] = regression.getSlope();
            regressionCoefficients[2] = regression.getRSquare();
            regressionCoefficients[3] = regression.getN();
        }
        return regressionCoefficients;
    }

    private ArrayList<BathymetryPoint> getBathymetryPointData(File file) {
        ArrayList<BathymetryPoint> bathymetryPoints = new ArrayList<>();
        String line = "";
        String separators = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            while ((line = br.readLine()) != null) {
                if(line.startsWith("#")) {
                    continue;
                }
                // use comma as separator
                String[] bathymetryMesure = line.split(separators);
                if(bathymetryMesure.length == 3) {
                    try {
                        BathymetryPoint point = new BathymetryPoint(Double.parseDouble(bathymetryMesure[0]),
                                                                    Double.parseDouble(bathymetryMesure[1]),
                                                                    Double.parseDouble(bathymetryMesure[2]));
                        bathymetryPoints.add(point);
                    } catch (NumberFormatException e) {
                        //nothing to do
                    }
                }
            }

        } catch (IOException e) {
            return null;
        }

        return bathymetryPoints;
    }

    private class BathymetryPoint {
        private double lat, lon, z;

        BathymetryPoint(double lat, double lon, double z) {
            this.lat = lat;
            this.lon = lon;
            this.z = z;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public double getZ() {
            return z;
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
            super(EmpiricalBathymetryOp.class);
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

