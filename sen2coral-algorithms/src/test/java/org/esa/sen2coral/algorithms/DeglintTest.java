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

import static org.junit.Assert.assertNotNull;

/**
 * Created by obarrile on 05/01/2017.
 */
public class DeglintTest {
    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new DeglintOp.Spi();

    /*//TODO uncommemt when test data set available
    @Test
    public void testCompareOutput() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/XXXX.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("Deglint/XXXX_deg.tif").toString(), null);

        final DeglintOp op = (DeglintOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_2", "band_3", "band_4"};
        op.setSourceBandNames(sourceBands);
        String[] referenceBands = {"band_8"};
        op.setReferenceBands(referenceBands);
        op.setSunGlintVector("glintVector");
        op.setMinNIRString("-1");
        op.setMaskNegativeValues(false);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBand("band2_deglint");
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }*/
}
