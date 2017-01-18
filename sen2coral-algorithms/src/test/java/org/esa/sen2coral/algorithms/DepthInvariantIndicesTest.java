package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.sen2coral.algorithms.utils.PerpendicularOffsetRegression;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by obarrile on 04/01/2017.
 */
public class DepthInvariantIndicesTest {

    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new DepthInvariantIndicesOp.Spi();

    @Test
    public void testDepthInvariantIndicesParameters() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("DepthInvariant/lizard_2016_07_21_s2_with_geoms.dim").toString(), null);
        Band band2 = product.getBand("band_2");
        Band band3 = product.getBand("band_3");

        final DepthInvariantIndicesOp op = (DepthInvariantIndicesOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        op.setDeepWaterVector("deep");
        String[] bands = {"band_2","band_3"};
        op.setSourceBandNames(bands);
        op.setSameBottomVectors("geometry");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        //assert regression parameters
        PerpendicularOffsetRegression regression = op.getPerpendicularOffsetRegression();
        double sigmaXX = regression.getSigmaXX();
        double sigmaXY = regression.getSigmaXY();
        double sigmaYY = regression.getSigmaYY();
        double slope = regression.getSlope();
        double rSquare = regression.getRSquare();


        double meanDeepWater2 = op.getDeepWaterReflectance()[0];
        double meanDeepWater3 = op.getDeepWaterReflectance()[1];


        //write Intermediate values to File
        new File("target/validation-reports").mkdir();
        FileWriter file = null;
        PrintWriter pw = null;
        try
        {
            file = new FileWriter("target/validation-reports/depthInvariantIndicesIntermediate.txt");
            pw = new PrintWriter(file);

            pw.println("Mean deep water values -> Band 2: " + meanDeepWater2 + "    Band 3: " + meanDeepWater3 );
            pw.println("Regression values -> sigmaXX: " + sigmaXX + "   sigmaYY: " + sigmaYY + "   sigmaXY: " + sigmaXY);
            pw.println("Regression values -> slope (ki/kj): " + slope + "      r-squared: " + rSquare);
            pw.println(" ");
            pw.println("Values from reference ROIs");
            printPixelLine(pw, 470, 678, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 471, 678, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 470, 679, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 471, 679, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 463, 660, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 464, 660, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 463, 661, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 464, 661, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 464, 631, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 465, 631, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 464, 632, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 465, 632, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 489, 689, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 490, 689, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 489, 690, band2, band3, meanDeepWater2, meanDeepWater3);
            printPixelLine(pw, 490, 690, band2, band3, meanDeepWater2, meanDeepWater3);
            pw.println(" ");
            pw.println("Examples of output pixels");
            printPixelOutputLine(pw, 512, 707, band2, band3, meanDeepWater2, meanDeepWater3, slope);
            printPixelOutputLine(pw, 494, 667, band2, band3, meanDeepWater2, meanDeepWater3, slope);
            printPixelOutputLine(pw, 441, 651, band2, band3, meanDeepWater2, meanDeepWater3, slope);


        } catch (Exception e) {

        } finally {
            try {
                if (null != file)
                    file.close();
            } catch (Exception e2) {
            }
        }

        //Assert values
        assertEquals(meanDeepWater2, 923.601, 0.001);
        assertEquals(meanDeepWater3, 514.944, 0.001);
        assertEquals(sigmaXX, 0.1306, 0.0001);
        assertEquals(sigmaYY, 0.2946, 0.0001);
        assertEquals(sigmaXY, 0.1948, 0.0001);
        assertEquals(slope, 0.664, 0.0005);
        assertEquals(rSquare, 0.986, 0.0005);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        band.readRasterDataFully(ProgressMonitor.NULL);

        double value [] = new double[1];
        band.readPixels(512, 707, 1, 1, value);
        assertEquals(value[0], 2.256, 0.001);
        band.readPixels(494, 667, 1, 1, value);
        assertEquals(value[0], 1.544, 0.001);
        band.readPixels(441, 651, 1, 1, value);
        assertEquals(value[0], 1.884, 0.001);

        product.dispose();
        targetProduct.dispose();
    }

    @Test
    public void testDepthInvariantIndices() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("DepthInvariant/lizard_2016_07_21_s2_with_geoms.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("DepthInvariant/lizard_2016_07_21_s2_dg23.tif").toString(), null);

        final DepthInvariantIndicesOp op = (DepthInvariantIndicesOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        op.setDeepWaterVector("deep");
        String[] bands = {"band_2","band_3"};
        op.setSourceBandNames(bands);
        op.setSameBottomVectors("geometry");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBandAt(0);
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.00001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }


    private void printPixelLine (PrintWriter pw, int PixelX, int PixelY, Band band2, Band band3, double deepBand2, double deepBand3) {
        double[] aux = new double[1];
        double[] aux2 = new double[1];
        try {
            band2.readPixels(PixelX,PixelY,1,1,aux);
            band3.readPixels(PixelX,PixelY,1,1,aux2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        aux[0] = aux[0] - deepBand2;
        aux2[0] = aux2[0] - deepBand3;
        pw.println(String.format(Locale.ROOT,"Pixel(%4d,%4d)-> Ri-Rideep: %.6f    Rj-Rjdeep: %.6f   Xi: %.6f   Xj: %.6f", PixelX, PixelY, aux[0], aux2[0], Math.log(aux[0]), Math.log(aux2[0])));
    }

    private void printPixelOutputLine (PrintWriter pw, int PixelX, int PixelY, Band band2, Band band3, double deepBand2, double deepBand3, double slope) {
        double[] aux = new double[1];
        double[] aux2 = new double[1];
        double Ri, Rj, Xi, Xj, output;
        try {
            band2.readPixels(PixelX,PixelY,1,1,aux);
            band3.readPixels(PixelX,PixelY,1,1,aux2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Ri = aux[0];
        Rj = aux2[0];
        Xi = Math.log(Ri - deepBand2);
        Xj = Math.log(Rj - deepBand3);
        output = Xi - slope * Xj;
        pw.println(String.format(Locale.ROOT,"Pixel(%4d,%4d)-> Ri: %6.1f     Rj: %6.1f     Xi: %2.6f     Xj: %2.6f     output index: %2.6f", PixelX, PixelY, Ri, Rj, Xi, Xj, output));
    }

}
