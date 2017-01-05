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

import static org.junit.Assert.assertNotNull;

/**
 * Created by obarrile on 05/01/2017.
 */
public class EmpiricalBathymetryTest {
    static {
        TestUtils.initTestEnvironment();
    }
    private OperatorSpi spi = new EmpiricalBathymetryOp.Spi();

    /*//TODO uncommemt when test data set available
    @Test
    public void testCompareOutput() throws Exception {
        DimapProductReaderPlugIn readerPlugIn = new DimapProductReaderPlugIn();
        final DimapProductReader productReader = new DimapProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/XXXX.dim").toString(), null);


        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        final Product validationProduct = productReader2.readProductNodes(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/XXXX_deg.tif").toString(), null);

        final EmpiricalBathymetryOp op = (EmpiricalBathymetryOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(product);
        String[] sourceBands = {"band_2", "band_3"};
        op.setSourceBandNames(sourceBands);
        op.setBathymetryFile(new File(Sen2CoralTestUtils.getTestDataPath("EmpiricalBathymetry/bathymetry_data.csv").toString()));
        op.setnValue(10000.0);
        op.setMinRSquared(0.7);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();

        final Band band = targetProduct.getBand("EmpiricalBathymetry_band_2band_3");
        final Band validationBand = validationProduct.getBandAt(0);

        //compareBands
        Sen2CoralTestUtils.compareFloatPixels(band, validationBand, 0.0001f);

        product.dispose();
        targetProduct.dispose();
        validationProduct.dispose();
    }*/
}
