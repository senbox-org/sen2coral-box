package org.esa.sen2coral.algorithms;


import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Radiometric Normalisation with PIFs operator
 */
@OperatorMetadata(alias = "RadiometricNormalisationPIFsOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        description = "RadiometricNormalisationPIFsOp algorithm")
public class RadiometricNormalisationPIFsOp extends PixelOperatorMultisize {

    @SourceProduct(alias = "Slave product", description = "The source product which serves as slave.")
    private Product slaveProduct;

    @SourceProduct(alias = "Reference product", description = "The source product which serves as reference.")
    private Product referenceProduct;

    @Parameter(description = "The list of source bands to be corrected", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "Pseudo-Invariant Features (PIFs) vector File",
            label = "Pseudo-Invariant Features (PIFs) vector File")
    private String pifVector;

    //Processing parameters
    private SimpleRegression[] regressions = null;
    private double[] slopes = null;
    private double[] intercepts = null;
    private double[] noDataValue = null;

    //Setters
    public void setSlaveProduct(Product slaveProduct) {
        this.slaveProduct = slaveProduct;
    }
    public void setReferenceProduct(Product referenceProduct) {
        this.referenceProduct = referenceProduct;
    }
    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }
    public void setPifVector(String pifVector) {
        this.pifVector = pifVector;
    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        int sceneWidth = 0, sceneHeight = 0;
        Set<Integer> distictWidths = new HashSet<>();
        for (Band band : slaveProduct.getBands()) {
            if (sceneWidth < band.getRasterWidth()) {
                sceneWidth = band.getRasterWidth();
                sceneHeight = band.getRasterHeight();
            }
            distictWidths.add(band.getRasterHeight());
        }
        Product targetProduct = new Product(slaveProduct.getName() + "_normalized", slaveProduct.getProductType(), sceneWidth, sceneHeight);

        targetProduct.setNumResolutionsMax(distictWidths.size());

        ProductUtils.copyProductNodes(slaveProduct,targetProduct);
        ProductUtils.copyFlagBands(slaveProduct, targetProduct, true);
        ProductUtils.copyOverlayMasks(slaveProduct, targetProduct);

        for (int i = 0; i < sourceBandNames.length; i++) {
            Band sourceBand = slaveProduct.getBand(sourceBandNames[i]);
            int sourceBandWidth = sourceBand.getRasterWidth();
            int sourceBandHeight = sourceBand.getRasterHeight();

            Band targetBand = new Band(sourceBand.getName(), sourceBand.getDataType(), sourceBandWidth, sourceBandHeight);
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            targetBand.setDescription(sourceBand.getDescription() + "(normalized)");
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
            targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
            targetBand.setGeoCoding(sourceBand.getGeoCoding());
            targetProduct.addBand(targetBand);
        }

        return targetProduct;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        if(getSourceProduct() == null) {
            throw new OperatorException("Source product cannot be null");
        }

        //Check that sourceBands is not empty
        if(sourceBandNames == null || sourceBandNames.length<=0) {
            throw new OperatorException("At least one source band must be selected");
        }

        //Check that bands selected exist and have the same raster size
        for(String sourceBandName : sourceBandNames) {
            Band slaveBand = slaveProduct.getBand(sourceBandName);
            Band referenceBand = referenceProduct.getBand(sourceBandName);
            if(slaveBand == null) {
                throw new OperatorException(String.format("The band %s is not available in the product %s.",
                                                          sourceBandName, slaveProduct.getName()));
            }
            if(referenceBand == null) {
                throw new OperatorException(String.format("The band %s is not available in the product %s.",
                                                          sourceBandName, referenceProduct.getName()));
            }
            if(!slaveBand.getRasterSize().equals(referenceBand.getRasterSize())) {
                throw new OperatorException(String.format("The band %s has a different raster size in slave and master product",
                                                          sourceBandName));
            }
        }

        //Check pifVector
        if (pifVector != null && !pifVector.isEmpty()) {
            throw new OperatorException(String.format("Error when reading pseudo-invariant features in %s. It must contain at least one polygon.",
                                                      pifVector));
        }

        //get no data values
        noDataValue = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = referenceProduct.getBand(sourceBandNames[i]).getNoDataValue();
        }
        setSourceProduct(slaveProduct);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        //regressions = getRegressions();
        int index = targetSample.getIndex();
        double correctedValue = sourceSamples[index].getFloat() */*regressions[index].getSlope()*/getSlope(index) + /*regressions[index].getIntercept()*/getIntercept(index);
        correctedValue = (correctedValue >= 0) ? correctedValue : noDataValue[index];
        targetSample.set(correctedValue);
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
        if(sourceBandNames != null) {
            for (int i = 0; i < sourceBandNames.length; i++) {
                sampleConfigurer.defineSample(i, getTargetProduct().getBandAt(i).getName());
            }
        }
    }


    private synchronized SimpleRegression[] getRegressions() {
        if (regressions == null) {
            regressions = new SimpleRegression[sourceBandNames.length];
            int iCounter = 0;

            final Band[] sourceBands = OperatorUtils.getSourceBands(slaveProduct, sourceBandNames, false);
            for (Band srcBand : sourceBands) {
                Band refBand = referenceProduct.getBand(srcBand.getName());
                regressions[iCounter] = new SimpleRegression();

                //create mask
                final Mask mask = new Mask("tempMask",
                                           srcBand.getRasterWidth(),
                                           srcBand.getRasterHeight(),
                                           Mask.VectorDataType.INSTANCE);
                Mask.VectorDataType.setVectorData(mask, referenceProduct.getVectorDataGroup().get(pifVector));
                ProductUtils.copyImageGeometry(srcBand, mask, false);

                //get noData values to exclude them
                double noData = srcBand.getNoDataValue();
                double referenceNoData = refBand.getNoDataValue();

                //load data if it is not loaded
                try {
                    mask.loadRasterData();
                    srcBand.loadRasterData();
                    refBand.loadRasterData();
                } catch (IOException e) {
                    throw new OperatorException("Unable to load the raster data.");
                }

                //add data to regressions
                for (int i = 0; i < srcBand.getRasterWidth(); i++) {
                    for (int j = 0; j < srcBand.getRasterHeight(); j++) {
                        if (mask.getPixelInt(i, j) == 0) {
                            continue;
                        }
                        double value = srcBand.getPixelDouble(i, j);
                        if (value != noData) {
                            double referenceValue = refBand.getPixelDouble(i, j);
                            if (referenceValue == referenceNoData) {
                                continue;
                            }
                            regressions[iCounter].addData(value, referenceValue);
                        }
                    }
                }
                iCounter++;
                mask.dispose();
                srcBand.unloadRasterData();
                refBand.unloadRasterData();
            }
        }
        return regressions;
    }

    private synchronized double getSlope(int index) {
        if(slopes == null) {
            slopes = new double[sourceBandNames.length];
        }
        if(slopes[index] == 0.0d){
            slopes[index] = getRegression(index).getSlope();
        }
        return slopes[index];
    }

    private synchronized double getIntercept(int index) {
        if(intercepts == null) {
            intercepts = new double[sourceBandNames.length];
        }
        if(intercepts[index] == 0.0d){
            intercepts[index] = getRegression(index).getIntercept();
        }
        return intercepts[index];
    }

    private synchronized SimpleRegression getRegression(int index) {
        if (index >= sourceBandNames.length) {
            return null;
        }

        if (regressions == null) {
            regressions = new SimpleRegression[sourceBandNames.length];
        }

        if (regressions[index] == null) {
            regressions[index] = new SimpleRegression();
            //get bands
            Band srcBand = slaveProduct.getBand(sourceBandNames[index]);
            Band refBand = referenceProduct.getBand(sourceBandNames[index]);

            //create temporary mask
            final Mask mask = new Mask("tempMask",
                                       srcBand.getRasterWidth(),
                                       srcBand.getRasterHeight(),
                                       Mask.VectorDataType.INSTANCE);
            Mask.VectorDataType.setVectorData(mask, referenceProduct.getVectorDataGroup().get(pifVector));
            ProductUtils.copyImageGeometry(srcBand, mask, false);

            //get noData values to exclude them
            double noData = srcBand.getNoDataValue();
            double referenceNoData = refBand.getNoDataValue();

            //load data if it is not loaded
            try {
                mask.loadRasterData();
                srcBand.loadRasterData();
                refBand.loadRasterData();
            } catch (IOException e) {
                throw new OperatorException(String.format("Unable to load raster data of the band %s.",
                                                          sourceBandNames[index]));
            }

            //add data to regressions
            for (int i = 0; i < srcBand.getRasterWidth(); i++) {
                for (int j = 0; j < srcBand.getRasterHeight(); j++) {
                    if (mask.getPixelInt(i, j) == 0) {
                        continue;
                    }
                    double value = srcBand.getPixelDouble(i, j);
                    if (value != noData) {
                        double referenceValue = refBand.getPixelDouble(i, j);
                        if (referenceValue == referenceNoData) {
                            continue;
                        }
                        regressions[index].addData(value, referenceValue);
                    }
                }
            }
            mask.dispose();
            srcBand.unloadRasterData();
            refBand.unloadRasterData();

        }
        return regressions[index];
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
            super(RadiometricNormalisationPIFsOp.class);
        }
    }
}
