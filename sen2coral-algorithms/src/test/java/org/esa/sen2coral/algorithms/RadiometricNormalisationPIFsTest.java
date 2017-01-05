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
public class RadiometricNormalisationPIFsTest {
    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new RadiometricNormalisationPIFsOp.Spi();

    /*//TODO uncommemt when test data set available
    @Test
    public void testCompareOutput() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product referenceProduct = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/MASTER.dim").toString(), null);

        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product slaveProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/SLAVE.tif").toString(), null);

        GeoTiffProductReaderPlugIn readerPlugIn3 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader3 = new GeoTiffProductReader(readerPlugIn3);
        final Product validationProduct = productReader3.readProductNodes(Sen2CoralTestUtils.getTestDataPath("RadiometricNormalisation/XXXX_rad.tif").toString(), null);

        final RadiometricNormalisationPIFsOp op = (RadiometricNormalisationPIFsOp) spi.createOperator();
        assertNotNull(op);
        op.setSlaveProduct(slaveProduct);
        op.setReferenceProduct(referenceProduct);
        String[] sourceBands = {"band_2", "band_3", "band_4"};
        op.setSourceBandNames(sourceBands);
        op.setPifVector("pifVector");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBand("band2_normalized");
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        slaveProduct.dispose();
        referenceProduct.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }*/
}
