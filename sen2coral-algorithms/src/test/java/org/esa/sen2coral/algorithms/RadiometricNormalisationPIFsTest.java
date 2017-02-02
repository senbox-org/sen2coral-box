package org.esa.sen2coral.algorithms;

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
public class RadiometricNormalisationPIFsTest {
    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new RadiometricNormalisationPIFsOp.Spi();

    @Test
    public void testRadiometricNormalisationIntermediate() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product referenceProduct = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/wistari_subset_2016_01_31.dim").toString(), null);

        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product slaveProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/wistari_subset_2016_05_30.tif").toString(), null);

        final RadiometricNormalisationPIFsOp op = (RadiometricNormalisationPIFsOp) spi.createOperator();
        assertNotNull(op);
        op.setSlaveProduct(slaveProduct);
        op.setReferenceProduct(referenceProduct);
        String[] sourceBands = {"band_1", "band_2", "band_3", "band_4","band_5", "band_6", "band_7", "band_8","band_9", "band_10", "band_11", "band_12","band_13"};
        op.setSourceBandNames(sourceBands);
        op.setPifVector("PIF_Polygon");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        //write Intermediate values to File
        new File("target/validation-reports").mkdir();
        FileWriter file = null;
        PrintWriter pw = null;
        try {
            file = new FileWriter("target/validation-reports/radiometricNormalisationIntermediate.txt");
            pw = new PrintWriter(file);
            for (int i = 0 ; i < sourceBands.length ; i++) {
                pw.println(String.format(Locale.ROOT, "Regression %-10s-> n: %d    m:%10.6f    c:%10.6f    r-squared: %.6f", sourceBands[i], op.getRegression(i).getN(), op.getRegression(i).getSlope(), op.getRegression(i).getIntercept(), op.getRegression(i).getRSquare()));
            }
        } catch (Exception e) {
            pw.println("Unable to compute the linear regression.");
        } finally {
            try {
                if (null != file)
                    file.close();
            } catch (Exception e2) {
            }
        }

        //Assert values
        assertEquals(op.getRegression(0).getSlope(), 1.168592, 0.00001f);
        assertEquals(op.getRegression(0).getIntercept(), -0.009537, 0.00001f);
        assertEquals(op.getRegression(0).getRSquare(), 0.991160, 0.00001f);
        assertEquals(op.getRegression(1).getSlope(), 1.104447, 0.00001f);
        assertEquals(op.getRegression(1).getIntercept(), -0.003985, 0.00001f);
        assertEquals(op.getRegression(1).getRSquare(), 0.968952, 0.00001f);
        assertEquals(op.getRegression(2).getSlope(), 1.118214, 0.00001f);
        assertEquals(op.getRegression(2).getIntercept(), -0.001860, 0.00001f);
        assertEquals(op.getRegression(2).getRSquare(), 0.975006, 0.00001f);
        assertEquals(op.getRegression(3).getSlope(), 0.836372, 0.00001f);
        assertEquals(op.getRegression(3).getIntercept(), 0.001302, 0.00001f);
        assertEquals(op.getRegression(3).getRSquare(), 0.989289, 0.00001f);
        assertEquals(op.getRegression(4).getSlope(), 0.544914, 0.00001f);
        assertEquals(op.getRegression(4).getIntercept(), 0.002997, 0.00001f);
        assertEquals(op.getRegression(4).getRSquare(), 0.988664, 0.00001f);
        assertEquals(op.getRegression(5).getSlope(), 0.056456, 0.00001f);
        assertEquals(op.getRegression(5).getIntercept(), 0.005339, 0.00001f);
        assertEquals(op.getRegression(5).getRSquare(), 0.900145, 0.00001f);
        assertEquals(op.getRegression(6).getSlope(), 0.072640, 0.00001f);
        assertEquals(op.getRegression(6).getIntercept(), 0.004771, 0.00001f);
        assertEquals(op.getRegression(6).getRSquare(), 0.891101, 0.00001f);
        assertEquals(op.getRegression(7).getSlope(), 0.050720, 0.00001f);
        assertEquals(op.getRegression(7).getIntercept(), 0.003896, 0.00001f);
        assertEquals(op.getRegression(7).getRSquare(), 0.857485, 0.00001f);
        assertEquals(op.getRegression(8).getSlope(), 0.012598, 0.00001f);
        assertEquals(op.getRegression(8).getIntercept(), 0.003657, 0.00001f);
        assertEquals(op.getRegression(8).getRSquare(), 0.670248, 0.00001f);
        assertEquals(op.getRegression(9).getSlope(), 0.019943, 0.00001f);
        assertEquals(op.getRegression(9).getIntercept(), 0.001191, 0.00001f);
        assertEquals(op.getRegression(9).getRSquare(), 0.239113, 0.00001f);
        assertEquals(op.getRegression(10).getSlope(), 0.081706, 0.00001f);
        assertEquals(op.getRegression(10).getIntercept(), 0.000129, 0.00001f);
        assertEquals(op.getRegression(10).getRSquare(), 0.002605, 0.00001f);
        assertEquals(op.getRegression(11).getSlope(), 0.033781, 0.00001f);
        assertEquals(op.getRegression(11).getIntercept(), 0.001269, 0.00001f);
        assertEquals(op.getRegression(11).getRSquare(), 0.014135, 0.00001f);
        assertEquals(op.getRegression(12).getSlope(), 0.031467, 0.00001f);
        assertEquals(op.getRegression(12).getIntercept(), 0.000831, 0.00001f);
        assertEquals(op.getRegression(12).getRSquare(), 0.001444, 0.00001f);

    }


    @Test
    public void testRadiometricNormalisationComparison() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product referenceProduct = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/wistari_subset_2016_01_31.dim").toString(), null);

        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product slaveProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/wistari_subset_2016_05_30.tif").toString(), null);

        GeoTiffProductReaderPlugIn readerPlugIn3 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader3 = new GeoTiffProductReader(readerPlugIn3);
        final Product validationProduct = productReader3.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/wistari_subset_2016_05_30_normalised.tif").toString(), null);

        final RadiometricNormalisationPIFsOp op = (RadiometricNormalisationPIFsOp) spi.createOperator();
        assertNotNull(op);
        op.setSlaveProduct(slaveProduct);
        op.setReferenceProduct(referenceProduct);
        String[] sourceBands = {"band_1", "band_2", "band_3", "band_4","band_5", "band_6", "band_7", "band_8","band_9", "band_10", "band_11", "band_12","band_13"};
        op.setSourceBandNames(sourceBands);
        op.setPifVector("PIF_Polygon");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        //compare bands
        for(String sourceBand : sourceBands) {
            Band band = targetProduct.getBand(sourceBand);
            Band validationBand = validationProduct.getBand(sourceBand);
            Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.00001f);
        }



        slaveProduct.dispose();
        referenceProduct.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }
}
