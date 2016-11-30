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
public class DeglintOp extends PixelOperatorMultisize {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Sun Glint areas", label = "Sun Glint Areas")
    private String sunGlintVector;

    @Parameter(description = "The list of source bands to be corrected", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "NIR bands to be used as glint reference", alias = "referenceBand",
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String[] referenceBands;

    @Parameter(label = "Include reference bands in output product", defaultValue = "true")
    private Boolean includeReferences = true;

    @Parameter(defaultValue = "-1.0", description = "Minimum expected NIR value in the absence of glint")
    private Double minNIR = -1.0;

    @Parameter(label = "Mask all negative reflectance values", defaultValue = "true")
    private Boolean maskNegativeValues = true;


    //Processing parameters
    SimpleRegression[] regressions = null;
    double calculatedMinNIR = Double.MAX_VALUE;
    private double noDataValue[] = null;
    HashMap<String,String> sourcesReferencesMap = null;
    HashMap<String,Integer> referencesIndexMap = new HashMap<>();




    @Override
    protected Product createTargetProduct() throws OperatorException {
        int sceneWidth = 0, sceneHeight = 0;
        Set<Integer> distictWidths = new HashSet<>();
        for (Band band : getSourceProduct().getBands()) {
            if (sceneWidth < band.getRasterWidth()) {
                sceneWidth = band.getRasterWidth();
                sceneHeight = band.getRasterHeight();
            }
            distictWidths.add(band.getRasterHeight());
        }
        Product targetProduct = new Product(getSourceProduct().getName() + "_deglint", getSourceProduct().getProductType(), sceneWidth, sceneHeight);

        targetProduct.setNumResolutionsMax(distictWidths.size());

        ProductUtils.copyProductNodes(getSourceProduct(),targetProduct);

        ProductUtils.copyTiePointGrids(getSourceProduct(), targetProduct);
        ProductUtils.copyGeoCoding(getSourceProduct(), targetProduct);
        ProductUtils.copyFlagBands(getSourceProduct(), targetProduct, true);

        if(sourceBandNames != null) {
            copyMasks(getSourceProduct(), targetProduct, sourceBandNames);
        }

        ProductUtils.copyOverlayMasks(getSourceProduct(), targetProduct);

        //add bands that are going to be 'deglinted'
        for (String bandName : sourceBandNames) {
            /*Band sourceBand = getSourceProduct().getBand(bandName);
            int sourceBandWidth = sourceBand.getRasterWidth();
            int sourceBandHeight = sourceBand.getRasterHeight();

            Band targetBand = new Band(sourceBand.getName(), ProductData.TYPE_FLOAT32, sourceBandWidth, sourceBandHeight);
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
            targetProduct.addBand(targetBand);*/

            Band band = ProductUtils.copyBand(bandName, getSourceProduct(), targetProduct, false);
            band.setGeoCoding(getSourceProduct().getBand(bandName).getGeoCoding());
        }

        //add reference bands if includeReferences is true
        if(includeReferences) {
            for (String bandName : referenceBands) {
                ProductUtils.copyBand(bandName, getSourceProduct(), targetProduct, true);
            }
        }

        return targetProduct;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        if(getSourceProduct() == null) {
            throw new OperatorException("Source product cannot be null");
        }
        if(sourceBandNames != null && sourceBandNames.length<=0) {
            throw new OperatorException("At least one source band must be selected");
        }
        if(referenceBands != null && referenceBands.length<=0) {
            throw new OperatorException("At least one reference band must be selected");
        }

        sourcesReferencesMap = buildSourcesReferencesMap(getSourceProduct(), sourceBandNames, referenceBands);

        if (sunGlintVector == null && sunGlintVector.isEmpty()) {
            throw new OperatorException(String.format("Error when reading Sun glint areas in %s. It must contain at least one polygon.",
                                                      sunGlintVector));
        }

        noDataValue = new double[sourceBandNames.length + referenceBands.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = getSourceProduct().getBand(sourceBandNames[i]).getNoDataValue();
        }
        for (int i = 0; i < referenceBands.length; i++) {
            noDataValue[sourceBandNames.length + i] = getSourceProduct().getBand(referenceBands[i]).getNoDataValue();
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {

        for (int i = 0; i < sourceBandNames.length; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
        for (int i = 0; i < referenceBands.length; i++) {
            referencesIndexMap.put(referenceBands[i],sourceBandNames.length + i);
            sampleConfigurer.defineSample(sourceBandNames.length + i, referenceBands[i]);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < sourceBandNames.length; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample targetSample/*, int index*/) {
        regressions = getRegressions();
        if(minNIR < 0) {
            minNIR = calculatedMinNIR;
        }
        int index = targetSample.getIndex();
        int referenceIndex = referencesIndexMap.get(sourcesReferencesMap.get(sourceBandNames[index]));
        double correctedValue = sourceSamples[index].getFloat() - regressions[index].getSlope() * (sourceSamples[referenceIndex].getFloat() - minNIR);
        correctedValue = (correctedValue >= 0) ? correctedValue : noDataValue[index];
        targetSample.set(correctedValue);
    }

    private synchronized SimpleRegression[] getRegressions() {
        if (regressions == null) {
            regressions = new SimpleRegression[sourceBandNames.length];
            int iCounter = 0;
            //pm.beginTask("Computing linear regression...", sourceBandNames.length);
            try {
                for (String srcBandName : sourceBandNames) {
                    //get corresponding referenceBand
                    String referenceBand = sourcesReferencesMap.get(srcBandName);
                    Band srcBand = getSourceProduct().getBand(srcBandName);

                    regressions[iCounter] = new SimpleRegression();

                    //create mask
                    final Mask mask = new Mask("tempMask",
                                               srcBand.getRasterWidth(),
                                               srcBand.getRasterHeight(),
                                               Mask.VectorDataType.INSTANCE);
                    Mask.VectorDataType.setVectorData(mask, getSourceProduct().getVectorDataGroup().get(sunGlintVector));
                    ProductUtils.copyImageGeometry(srcBand, mask, false);

                    //get noData values to exclude them
                    double noData = srcBand.getNoDataValue();
                    double referenceNoData = getSourceProduct().getRasterDataNode(referenceBand).getNoDataValue();

                    //load data if it is not loaded
                    try {
                        mask.loadRasterData();
                        srcBand.loadRasterData();
                        getSourceProduct().getRasterDataNode(referenceBand).loadRasterData();
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
                                double referenceValue = getSourceProduct().getRasterDataNode(referenceBand).getPixelDouble(i, j);
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

    private HashMap<String,String> buildSourcesReferencesMap(Product product, String[] sourceBandNames, String[] referenceBandNames) throws OperatorException {
        HashMap<String,String> map = new HashMap<>();
        for(String srcBandName : sourceBandNames) {
            boolean bFound = false;
            Band srcBand = product.getBand(srcBandName);
            int srcHeight = srcBand.getRasterHeight();
            int srcWidth = srcBand.getRasterWidth();

            //select the reference band
            for (String refBandName : referenceBandNames) {
                int refHeight = product.getBand(refBandName).getRasterHeight();
                int refWidth = product.getBand(refBandName).getRasterWidth();
                if (refHeight == srcHeight && refWidth == srcWidth) {
                    map.put(srcBandName, refBandName);
                    bFound = true;
                    break;
                }
            }

            if(!bFound) {
                String compatibleBands="";
                for(Band band : product.getBands()) {
                    if(band.getRasterHeight() == srcHeight && band.getRasterWidth() == srcWidth && band.getName() != srcBandName) {
                        compatibleBands = compatibleBands + band.getName() + " ";
                    }
                }
                throw new OperatorException(String.format("There are not reference bands for the band %s. Try to add as reference band one of the following: %s",
                                                          srcBandName,compatibleBands));
            }
        }
        return map;
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
