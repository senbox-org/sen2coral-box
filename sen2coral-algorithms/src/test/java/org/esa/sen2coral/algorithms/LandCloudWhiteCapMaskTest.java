package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.sen2coral.algorithms.utils.Sen2CoralTestUtils;
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
public class LandCloudWhiteCapMaskTest {

    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new LandCloudWhiteCapMaskOp.Spi();

    @Test
    public void testCompareOutput() throws Exception {
        GeoTiffProductReaderPlugIn readerPlugIn = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader = new GeoTiffProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("LandCloudWhiteCapMask/lizard_2016_02_22_LEtoa.tif").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("LandCloudWhiteCapMask/lizard_2016_02_22_b8_mask_gt_0p0274.tif").toString(), null);

        final LandCloudWhiteCapMaskOp op = (LandCloudWhiteCapMaskOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] bands = {"band_8"};
        op.setReferenceBandNames(bands);
        op.setThresholdString("0.0274");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBand("LandCloudWhiteCapMask_band_8");
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareIntegerPixels(band, validationBand);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }

}
