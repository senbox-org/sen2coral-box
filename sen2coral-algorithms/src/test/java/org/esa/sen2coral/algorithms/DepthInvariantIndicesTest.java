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
        double[] parameters = op.getRegressionParameters();
        assertEquals(parameters[2], 0.1306, 0.0001);
        assertEquals(parameters[3], 0.2946, 0.0001);
        assertEquals(parameters[4], 0.1948, 0.0001);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        band.readRasterDataFully(ProgressMonitor.NULL);

        Double value = band.getPixelDouble(512, 707);
        assertEquals(value, 2.256, 0.001);
        value = band.getPixelDouble(494, 667);
        assertEquals(value, 1.544, 0.001);
        value = band.getPixelDouble(441, 651);
        assertEquals(value, 1.884, 0.001);

        // assert mean values in deep water ROI
        assertEquals(op.getDeepWaterReflectance()[0], 923.601, 0.001);
        assertEquals(op.getDeepWaterReflectance()[1], 514.944, 0.001);

        // assert slope value
        assertEquals(op.getAttenuationCoeffRatio(), 0.664, 0.0005);
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

}
