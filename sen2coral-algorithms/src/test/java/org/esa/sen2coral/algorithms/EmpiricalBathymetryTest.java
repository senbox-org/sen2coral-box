package org.esa.sen2coral.algorithms;

import org.esa.sen2coral.algorithms.utils.Sen2CoralTestUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by obarrile on 05/01/2017.
 */
public class EmpiricalBathymetryTest {
    static {
        TestUtils.initTestEnvironment();
    }

    private OperatorSpi spi = new EmpiricalBathymetryOp.Spi();

    @Test
    public void testEmpiricalBathymetryComparison() throws Exception {
        GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader = new GeoTiffProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/lizard_2016_07_21_s2.tif").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/lizard_2016_07_21_s2_bathy.tif").toString(), null);

        final EmpiricalBathymetryOp op = (EmpiricalBathymetryOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_2", "band_3"};
        op.setSourceBandNames(sourceBands);
        op.setBathymetryFile(new File(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/lizard_bathy_training_data.csv").toString()));
        op.setnValue(0.01);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBand("EmpiricalBathymetry_band_2band_3");
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }

    @Test
    public void testEmpiricalBathymetryIntermediate() throws Exception {
        GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader = new GeoTiffProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/lizard_2016_07_21_s2.tif").toString(), null);
        Band band1 = product.getBand("band_2");
        Band band2 = product.getBand("band_3");

        final EmpiricalBathymetryOp op = (EmpiricalBathymetryOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_2", "band_3"};
        op.setSourceBandNames(sourceBands);
        op.setBathymetryFile(new File(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/lizard_bathy_training_data.csv").toString()));
        final double nValue = 0.01;
        op.setnValue(nValue);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        double m0 = op.getM0();
        double m1 = op.getM1();
        double rSquared = op.getRSquared();

        //Validation data
        PixelInfo[] pixels = new PixelInfo[3];
        pixels[0] = new PixelInfo(452, 670, 1484, 1049, 2.69733, 2.35042, 5.99386);
        pixels[1] = new PixelInfo(410, 641, 1770, 1574, 2.87356, 2.75621, 0.717938);
        pixels[2] = new PixelInfo(575, 660, 1143, 641, 2.43624, 1.85786, 14.2196);

        //write Intermediate values to File
        new File("target/validation-reports").mkdir();
        FileWriter file = null;
        PrintWriter pw = null;
        try {
            file = new FileWriter("target/validation-reports/empiricalBathymetryIntermediate.txt");
            pw = new PrintWriter(file);

            pw.println(String.format(Locale.ROOT, "Regression -> m0: %.6f    m1: %.6f    r-squared: %.6f", m0, m1, rSquared));
            pw.println(" ");
            for (PixelInfo pixel : pixels) {
                printPixelOutputLine(pw, pixel.x, pixel.y, band1, band2, nValue, m0, m1);
            }
        } catch (Exception e) {
        } finally {
            try {
                if (null != file)
                    file.close();
            } catch (Exception e2) {
            }
        }


        double[] aux = new double[1];
        double[] aux2 = new double[1];
        double Rw1, Rw2, ln1, ln2, z;

        //assure values
        assertEquals(m0, -51.6624, 0.0001);
        assertEquals(m1, 50.2411, 0.0001);
        assertEquals(rSquared, 0.80056, 0.00001);

        for (PixelInfo pixel : pixels) {
            try {
                band1.readPixels(pixel.x, pixel.y, 1, 1, aux);
                band2.readPixels(pixel.x, pixel.y, 1, 1, aux2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Rw1 = aux[0];
            Rw2 = aux2[0];
            ln1 = Math.log(nValue * Rw1);
            ln2 = Math.log(nValue * Rw2);
            z = m0 + m1 * (ln1 / ln2);
            assertEquals(Rw1, pixel.Rw1, 0.00001);
            assertEquals(Rw2, pixel.Rw2, 0.00001);
            assertEquals(ln1, pixel.ln1, 0.00001);
            assertEquals(ln2, pixel.ln2, 0.00001);
            assertEquals(z, pixel.z, 0.0001);
        }

        product.dispose();
        targetProduct.dispose();
    }

    private void printPixelOutputLine(PrintWriter pw, int PixelX, int PixelY, Band band1, Band band2, double n, double m0, double m1) {
        double[] aux = new double[1];
        double[] aux2 = new double[1];
        double Rw1, Rw2, ln1, ln2, z;
        try {
            band1.readPixels(PixelX, PixelY, 1, 1, aux);
            band2.readPixels(PixelX, PixelY, 1, 1, aux2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Rw1 = aux[0];
        Rw2 = aux2[0];
        ln1 = Math.log(n * Rw1);
        ln2 = Math.log(n * Rw2);
        z = m0 + m1 * (ln1 / ln2);
        pw.println(String.format(Locale.ROOT, "Pixel(%4d,%4d)-> Rw Band1: %6.1f     Rw Band2: %6.1f     n: %.6f     ln(n x band1): %2.6f     ln(n x band2): %2.6f      z: %11.8f", PixelX, PixelY, Rw1, Rw2, n, ln1, ln2, z));
    }

    //Validation data container
    class PixelInfo {
        int x, y;
        double Rw1, Rw2, ln1, ln2, z;

        PixelInfo(int x, int y, double Rw1, double Rw2, double ln1, double ln2, double z) {
            this.x = x;
            this.y = y;
            this.Rw1 = Rw1;
            this.Rw2 = Rw2;
            this.ln1 = ln1;
            this.ln2 = ln2;
            this.z = z;
        }
    }
}
