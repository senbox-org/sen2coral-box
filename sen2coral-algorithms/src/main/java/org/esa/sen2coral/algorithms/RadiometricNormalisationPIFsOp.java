package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * deglint operator
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
    SimpleRegression[] regressions = null;
    private double noDataValue[] = null;


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
        //TODO comprueba bandas seleccionadas existen an reference y slave


        if (pifVector != null && !pifVector.isEmpty()) {
            //todo throw...
        }

        //
        noDataValue = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = referenceProduct.getBand(sourceBandNames[i]).getNoDataValue();
        }
        setSourceProduct(slaveProduct);
    }



    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample targetSample/*, int index*/) {
        regressions = getRegressions();
        //for(int i = 0 ; i<sourceBandNames.length ; i++) {
        int index = targetSample.getIndex();
            double correctedValue = sourceSamples[index].getFloat()*regressions[index].getSlope() + regressions[index].getIntercept();
            correctedValue = (correctedValue >= 0) ? correctedValue : noDataValue[index];
            targetSample.set(correctedValue);
        //}
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
            //pm.beginTask("Computing linear regression...", sourceBandNames.length);
            try {
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
            } finally {
                //pm.done();
            }
        }
        return regressions;
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
