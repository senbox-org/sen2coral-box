package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.sen2coral.algorithms.utils.Sen2CoralTestUtils;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by obarrile on 05/01/2017.
 */
public class DeglintTest {
    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new DeglintOp.Spi();


    @Test
    public void testDeglintDatasetIntermediate() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_withGeom.dim").toString(), null);

        final DeglintOp op = (DeglintOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_1"};
        op.setSourceBandNames(sourceBands);
        String[] referenceBands = {"band_9"};
        op.setReferenceBands(referenceBands);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);
        // get targetProduct gets initialize to be executed
        Product targetProduct = op.getTargetProduct();

        double minNIRBand1 = op.getCalculatedMinNIR(0);
        double slopeBand1 = op.getRegression(0).getSlope();
        double rSquaredBand1 = op.getRegression(0).getRSquare();
        long nBand1 = op.getRegression(0).getN();

        float[] value = new float[1];

        targetProduct.getBandAt(0).readPixels(256, 46, 1, 1, value, ProgressMonitor.NULL);
        float finalValueBand1A = value[0];
        targetProduct.getBandAt(0).readPixels(253, 42, 1, 1, value, ProgressMonitor.NULL);
        float finalValueBand1B = value[0];

        op.dispose();

        //other bands
        op.setSourceProduct(product);
        String[] sourceBands2 = {"band_5"};
        op.setSourceBandNames(sourceBands2);
        String[] referenceBands2 = {"band_7"};
        op.setReferenceBands(referenceBands2);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);
        // get targetProduct gets initialize to be executed
        op.update();
        targetProduct = op.getTargetProduct();

        double minNIRBand5 = op.getCalculatedMinNIR(0);
        double slopeBand5 = op.getRegression(0).getSlope();
        double rSquaredBand5 = op.getRegression(0).getRSquare();
        long nBand5 = op.getRegression(0).getN();



        targetProduct.getBandAt(0).readPixels(256, 46, 1, 1, value, ProgressMonitor.NULL);
        float finalValueBand5A = value[0];

        targetProduct.getBandAt(0).readPixels(253, 42, 1, 1, value, ProgressMonitor.NULL);
        float finalValueBand5B = value[0];


        op.dispose();

        //other bands
        op.setSourceProduct(product);
        String[] sourceBands3 = {"band_2","band_3","band_4"};
        op.setSourceBandNames(sourceBands3);
        String[] referenceBands3 = {"band_8"};
        op.setReferenceBands(referenceBands3);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);
        // get targetProduct gets initialize to be executed
        op.update();
        targetProduct = op.getTargetProduct();

        double minNIRBand2 = op.getCalculatedMinNIR(0);
        double slopeBand2 = op.getRegression(0).getSlope();
        double rSquaredBand2 = op.getRegression(0).getRSquare();
        long nBand2 = op.getRegression(0).getN();
        double minNIRBand3 = op.getCalculatedMinNIR(1);
        double slopeBand3 = op.getRegression(1).getSlope();
        double rSquaredBand3 = op.getRegression(1).getRSquare();
        long nBand3 = op.getRegression(1).getN();
        double minNIRBand4 = op.getCalculatedMinNIR(2);
        double slopeBand4 = op.getRegression(2).getSlope();
        double rSquaredBand4 = op.getRegression(2).getRSquare();
        long nBand4 = op.getRegression(2).getN();



        targetProduct.getBandAt(0).readPixels(256, 46, 1, 1, value);
        float finalValueBand2A = value[0];

        targetProduct.getBandAt(0).readPixels(253, 42, 1, 1, value);
        float finalValueBand2B = value[0];

        targetProduct.getBandAt(1).readPixels(256, 46, 1, 1, value);
        float finalValueBand3A = value[0];

        targetProduct.getBandAt(1).readPixels(253, 42, 1, 1, value);
        float finalValueBand3B = value[0];

        targetProduct.getBandAt(2).readPixels(256, 46, 1, 1, value);
        float finalValueBand4A = value[0];

        targetProduct.getBandAt(2).readPixels(253, 42, 1, 1, value);
        float finalValueBand4B = value[0];


        op.dispose();
        product.dispose();

        //write Intermediate values to File
        new File("target/validation-reports").mkdir();
        FileWriter file = null;
        PrintWriter pw = null;
        try
        {
            file = new FileWriter("target/validation-reports/deglintIntermediate.txt");
            pw = new PrintWriter(file);

            pw.println(String.format(Locale.ROOT, "Band 1 -> MinNIR: %6.1f    n: %6d    b: %2.6f   r-squared: %2.6f", minNIRBand1, nBand1, slopeBand1, rSquaredBand1));
            pw.println(String.format(Locale.ROOT, "Band 2 -> MinNIR: %6.1f    n: %6d    b: %2.6f   r-squared: %2.6f", minNIRBand2, nBand2, slopeBand2, rSquaredBand2));
            pw.println(String.format(Locale.ROOT, "Band 3 -> MinNIR: %6.1f    n: %6d    b: %2.6f   r-squared: %2.6f", minNIRBand3, nBand3, slopeBand3, rSquaredBand3));
            pw.println(String.format(Locale.ROOT, "Band 4 -> MinNIR: %6.1f    n: %6d    b: %2.6f   r-squared: %2.6f", minNIRBand4, nBand4, slopeBand4, rSquaredBand4));
            pw.println(String.format(Locale.ROOT, "Band 5 -> MinNIR: %6.1f    n: %6d    b: %2.6f   r-squared: %2.6f", minNIRBand5, nBand5, slopeBand5, rSquaredBand5));
            pw.println(" ");
            pw.println(String.format(Locale.ROOT, "Band 1 -> R' at pixel (256, 46):%12.6f      R' at pixel (253, 42):%12.6f ", finalValueBand1A, finalValueBand1B));
            pw.println(String.format(Locale.ROOT, "Band 2 -> R' at pixel (256, 46):%12.6f      R' at pixel (253, 42):%12.6f ", finalValueBand2A, finalValueBand2B));
            pw.println(String.format(Locale.ROOT, "Band 3 -> R' at pixel (256, 46):%12.6f      R' at pixel (253, 42):%12.6f ", finalValueBand3A, finalValueBand3B));
            pw.println(String.format(Locale.ROOT, "Band 4 -> R' at pixel (256, 46):%12.6f      R' at pixel (253, 42):%12.6f ", finalValueBand4A, finalValueBand4B));
            pw.println(String.format(Locale.ROOT, "Band 5 -> R' at pixel (256, 46):%12.6f      R' at pixel (253, 42):%12.6f ", finalValueBand5A, finalValueBand5B));

        } catch (Exception e) {

        } finally {
            try {
                if (null != file)
                    file.close();
            } catch (Exception e2) {
            }
        }

        //asserts values
        assertEquals(minNIRBand1, 48, 0.0001f);
        assertEquals(slopeBand1, 1.485459, 0.000001f);
        assertEquals(rSquaredBand1, 0.191, 0.001f);
        assertEquals(nBand1, 12000);
        assertEquals(finalValueBand1A, 1296.72, 0.01f);
        assertEquals(finalValueBand1B, 1295.17, 0.01f);

        assertEquals(minNIRBand5, 194, 0.0001f);
        assertEquals(slopeBand5, 0.829298, 0.000001f);
        assertEquals(rSquaredBand5, 0.809, 0.001f);
        assertEquals(nBand5, 12000);
        assertEquals(finalValueBand5A, 266.36, 0.01f);
        assertEquals(finalValueBand5B, 228.487, 0.001f);

        assertEquals(minNIRBand2, 151, 0.0001f);
        assertEquals(minNIRBand3, 151, 0.0001f);
        assertEquals(minNIRBand4, 151, 0.0001f);
        assertEquals(slopeBand2, 0.856348, 0.000001f);
        assertEquals(slopeBand3, 0.869187, 0.000001f);
        assertEquals(slopeBand4, 0.806518, 0.000001f);
        assertEquals(rSquaredBand2, 0.763, 0.001f);
        assertEquals(rSquaredBand3, 0.788, 0.001f);
        assertEquals(rSquaredBand4, 0.581, 0.001f);
        assertEquals(nBand2, 12000);
        assertEquals(nBand3, 12000);
        assertEquals(nBand4, 12000);
        assertEquals(finalValueBand2A, 916.59, 0.01f);
        assertEquals(finalValueBand2B, 898.011, 0.001f);
        assertEquals(finalValueBand3A, 484.95, 0.01f);//TODO check in ATBD, the value is different
        assertEquals(finalValueBand3B, 498.831, 0.001f);
        assertEquals(finalValueBand4A, 233.86, 0.01f);
        assertEquals(finalValueBand4B, 292.708, 0.001f);

    }


    @Test
    public void testDeglintBand1() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_withGeom.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_dg1_9.tif").toString(), null);

        final DeglintOp op = (DeglintOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_1"};
        op.setSourceBandNames(sourceBands);
        String[] referenceBands = {"band_9"};
        op.setReferenceBands(referenceBands);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBandAt(0);
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }

    @Test
    public void testDeglintBand5() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_withGeom.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_dg5_7.tif").toString(), null);

        final DeglintOp op = (DeglintOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_5"};
        op.setSourceBandNames(sourceBands);
        String[] referenceBands = {"band_7"};
        op.setReferenceBands(referenceBands);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBandAt(0);
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }

    @Test
    public void testDeglintBands234() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_withGeom.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/palau_north_2016_02_10_sub1_dg234_8.tif").toString(), null);

        final DeglintOp op = (DeglintOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_2","band_3","band_4"};
        op.setSourceBandNames(sourceBands);
        String[] referenceBands = {"band_8"};
        op.setReferenceBands(referenceBands);
        op.setSunGlintVector("deglint");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);
        op.setIncludeReferences(false);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        Band band = targetProduct.getBandAt(0);
        Band validationBand = validationProduct.getBandAt(0);
        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        //another band
        band = targetProduct.getBandAt(1);
        validationBand = validationProduct.getBandAt(1);
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        //another band
        band = targetProduct.getBandAt(2);
        validationBand = validationProduct.getBandAt(2);
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }
}
