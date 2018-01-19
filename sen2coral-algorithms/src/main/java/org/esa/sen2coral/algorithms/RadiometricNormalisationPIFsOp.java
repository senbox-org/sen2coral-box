package org.esa.sen2coral.algorithms;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import javafx.geometry.BoundingBox;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
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
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.Maths;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.geotools.referencing.CRS.decode;

/**
 * Radiometric Normalisation with PIFs operator
 */
@OperatorMetadata(alias = "RadiometricNormalisationPIFsOp",
        category = "Optical/Thematic Water Processing/Sen2Coral",
        authors = "Omar Barrilero",
        version = "1.0",
        internal = true,
        description = "RadiometricNormalisationPIFsOp algorithm")
public class RadiometricNormalisationPIFsOp extends PixelOperatorMultisize {

    @SourceProduct(alias = "Slave product", description = "The source product which serves as slave.")
    private Product slaveProduct;

    @SourceProduct(alias = "Reference product", description = "The source product which serves as reference.")
    private Product referenceProduct;

    @Parameter(description = "The list of source bands to be corrected", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(description = "Pseudo-Invariant Features (PIFs) vector",
            label = "Pseudo-Invariant Features (PIFs) vector")
    private String pifVector = "";

    //Processing parameters
    private SimpleRegression[] regressions = null;
    private double[] slopes = null;
    private double[] intercepts = null;
    private double[] noDataValue = null;
    private boolean areCoregistered = true;

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


    //getters used by the test

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
            /*if(!slaveBand.getRasterSize().equals(referenceBand.getRasterSize())) {
                throw new OperatorException(String.format("The band %s has a different raster size in slave and master product",
                                                          sourceBandName));
            }*/
        }

        //Check pifVector
        if (pifVector == null || pifVector.isEmpty() || referenceProduct.getVectorDataGroup().get(pifVector) == null) {
            throw new OperatorException(String.format("Error when reading pseudo-invariant features in %s. It must contain at least one polygon.",
                                                      pifVector));
        }

        //check that products intersects and their intersections intersects the PIF
        Geometry refGeometry = createGeoBoundaryPolygonLatLon(referenceProduct);
        Geometry slaveGeometry = createGeoBoundaryPolygonLatLon(slaveProduct);
        Geometry intersection = refGeometry.intersection(slaveGeometry);
        MathTransform transform = null;
        boolean bFoundIntersection = false;
        try {
            transform = CRS.findMathTransform(referenceProduct.getSceneCRS(), CRS.decode("EPSG:4326"), true);
        } catch (FactoryException e) {
            throw new OperatorException("Unable to transform Pseudo-Invariant Features vector File");
        }
        FeatureIterator iterator = referenceProduct.getVectorDataGroup().get(pifVector).getFeatureCollection().features();
        while(iterator.hasNext() && !bFoundIntersection) {
            SimpleFeature feature = (SimpleFeature) iterator.next();
            /*if(feature == null) {
                continue;
            }*/
            for(int i = 0 ; i < feature.getAttributeCount() ; i++) {
                if(!(feature.getAttribute(i) instanceof Geometry)) {
                    continue;
                }
                Geometry vectorGeometry =  (Geometry) feature.getAttribute(i);
                if(vectorGeometry == null) {
                    continue;
                }
                try {
                    Geometry geometry2 = JTS.transform(vectorGeometry, transform);
                    if(intersection.intersects(geometry2)) {
                        bFoundIntersection = true;
                        break;
                    };
                } catch (TransformException e) {
                    e.printStackTrace();
                }
            }
        }

        if(!bFoundIntersection) {
            throw new OperatorException("The input files and the Pseudo-Invariant Features vector File do not intersect.");
        }





        //get no data values
        noDataValue = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            noDataValue[i] = referenceProduct.getBand(sourceBandNames[i]).getNoDataValue();
        }
        setSourceProduct(slaveProduct);

        //check if the products are coregistered (at least the bands selected)
        for (int i = 0; i < sourceBandNames.length; i++) {
            GeoCoding slaveGeocoding = slaveProduct.getBand(sourceBandNames[i]).getGeoCoding();
            GeoCoding referenceGeocoding = referenceProduct.getBand(sourceBandNames[i]).getGeoCoding();
            if(!equalGeocoding(slaveGeocoding,referenceGeocoding)) {
                areCoregistered = false;
                break;
            }
        }


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

    //public only to be used by the test
    public synchronized SimpleRegression getRegression(int index) {
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
                                       refBand.getRasterWidth(),
                                       refBand.getRasterHeight(),
                                       Mask.VectorDataType.INSTANCE);
            Mask.VectorDataType.setVectorData(mask, referenceProduct.getVectorDataGroup().get(pifVector));
            ProductUtils.copyImageGeometry(refBand, mask, false);

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
            for (int i = 0; i < refBand.getRasterWidth(); i++) {
                for (int j = 0; j < refBand.getRasterHeight(); j++) {
                    if (mask.getPixelInt(i, j) == 0) {
                        continue;
                    }

                    if(areCoregistered) {
                        double value = srcBand.getPixelDouble(i, j);
                        if (value != noData) {
                            double referenceValue = refBand.getPixelDouble(i, j);
                            if (referenceValue == referenceNoData) {
                                continue;
                            }
                            regressions[index].addData(value, referenceValue);
                        }
                    } else {
                        double refValue = refBand.getPixelDouble(i, j);
                        if (refValue != referenceNoData) {
                            GeoPos geoPos = refBand.getGeoCoding().getGeoPos(new PixelPos((float) i, (float) j), null);
                            PixelPos pixelPos = srcBand.getGeoCoding().getPixelPos(geoPos, null);
                            int i1 = (int) Math.floor(pixelPos.x);
                            int j1 = (int) Math.floor(pixelPos.y);
                            if (i1 < 0 || j1 < 0 || i1 >= srcBand.getRasterWidth() || j1 >= srcBand.getRasterHeight()) {
                                continue;
                            }
                            double value = srcBand.getPixelDouble(i1, j1);
                            if (value == noData) {
                                continue;
                            }
                            regressions[index].addData(value, refValue);
                        }
                    }
                }
            }
            mask.dispose();
            srcBand.unloadRasterData();
            refBand.unloadRasterData();

            updateBandDescription(index,getSlope(index), getIntercept(index), regressions[index].getRSquare());
        }
        return regressions[index];
    }

    private void updateBandDescription(int index,double slope, double intercept, double rSquare) {
        String description = getTargetProduct().getBandAt(index).getDescription();
        description = description + String.format(" - NormalisationInfo-> Regression slope = %f  Regression intercept = %f  Regression R - squared= %f",
                                                  slope, intercept, rSquare);
        getTargetProduct().getBandAt(index).setDescription(description);
    }


    private boolean equalGeocoding(GeoCoding slaveGeocoding, GeoCoding referenceGeocoding) {

        if(!slaveGeocoding.canGetGeoPos() || !slaveGeocoding.canGetPixelPos() || !referenceGeocoding.canGetGeoPos() || !referenceGeocoding.canGetPixelPos()) {
            return false;
        }
        PixelPos pixelPos0 = new PixelPos(0.0,0.0);
        PixelPos pixelPos1 = new PixelPos(10000,10000);
        PixelPos pixelPos0ref = referenceGeocoding.getPixelPos(slaveGeocoding.getGeoPos(pixelPos0, null),null);
        PixelPos pixelPos1ref = referenceGeocoding.getPixelPos(slaveGeocoding.getGeoPos(pixelPos1, null),null);
        if(Math.abs(pixelPos0.x - pixelPos0ref.x) > 0.01 || Math.abs(pixelPos0.y - pixelPos0ref.y) > 0.01 || Math.abs(pixelPos1.x - pixelPos1ref.x) > 0.01 || Math.abs(pixelPos1.y - pixelPos1ref.y) > 0.01) {
            return false;
        }

        return true;
    }


    private static Geometry createGeoBoundaryPolygonLatLon(Product product) {
        GeometryFactory gf = new GeometryFactory();
        GeoPos[] geoPositions = ProductUtils.createGeoBoundary(product, 100);
        Coordinate[] coordinates;
        if(geoPositions.length >= 0 && geoPositions.length <= 3) {
            coordinates = new Coordinate[0];
        } else {
            coordinates = new Coordinate[geoPositions.length + 1];

            for(int i = 0; i < geoPositions.length; ++i) {
                GeoPos geoPos = geoPositions[i];
                coordinates[i] = new Coordinate(geoPos.lat, geoPos.lon);
            }

            coordinates[coordinates.length - 1] = coordinates[0];
        }

        return gf.createPolygon(gf.createLinearRing(coordinates), (LinearRing[])null);
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
