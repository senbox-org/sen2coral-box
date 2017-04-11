package org.esa.sen2coral.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.python.gpf.PyOperator;
import org.esa.snap.python.gpf.PyOperatorSpi;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.esa.snap.core.util.Guardian.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by obarrile on 10/04/2017.
 */
public class InversionTest {
    static {
        TestUtils.initTestEnvironment();
    }


    /*@Test
    public void testSWAMnotRegression() throws Exception {
        //The product generated  with SWAM operator is system dependent. It uses python and some optimization libraries
        //depend on the operative system, the processor... This test will only pass when executing in the
        //same system where the validation product was generated.
        DimapProductReaderPlugIn readerPlugInSource = new DimapProductReaderPlugIn();
        final DimapProductReader productReaderSource = new DimapProductReader(readerPlugInSource);
        final Product source = productReaderSource.readProductNodes(InversionTest.getTestDataPath("mini_lizard_2016_03_23_tdg_cmn_u0_tau0p18_LEboa.dim").toString(), null);

        DimapProductReaderPlugIn readerPlugInValidation = new DimapProductReaderPlugIn();
        final DimapProductReader productReaderValidation = new DimapProductReader(readerPlugInValidation);
        final Product validationProduct = productReaderValidation.readProductNodes(InversionTest.getTestDataPath("mini_lizard_2016_03_23_tdg_cmn_u0_tau0p18_LEboa_inversion.dim").toString(), null);

        File moduleDir = InversionTest.getResourceFile("/sambuca_snap_op.py");

        URL infoXmlFile = PyOperatorSpi.class.getResource("/sambuca_snap_op-info.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(infoXmlFile, getClass().getClassLoader());
        PyOperatorSpi spi = new PyOperatorSpi(descriptor);

        PyOperator operator = new PyOperator();
        operator.setSpi(spi);
        operator.setParameterDefaultValues();
        operator.setPythonModulePath(moduleDir.getParentFile().getPath());
        operator.setPythonModuleName("sambuca_snap_op");
        operator.setPythonClassName("sambuca_snap_op");
        operator.setParameter("xmlpath_sensor", getTestDataPath("swampy_s2_5_bands_filter_nedr.xml").toString());
        operator.setParameter("xmlpath_parameters", getTestDataPath("swampy_s2_parameters_depth30.xml").toString());
        operator.setParameter("xmlpath_siop", getTestDataPath("swampy_s2_6_siop_Lizard.xml").toString());
        operator.setParameter("error_name", "alpha_f");
        operator.setParameter("opt_method", "SLSQP");
        operator.setParameter("min_wlen", "400.0");
        operator.setParameter("max_wlen", "750.0");
        operator.setParameter("above_rrs_flag", true);
        operator.setParameter("shallow_flag", true);
        operator.setParameter("relaxed_cons", true);
        operator.setSourceProduct("source", source);
        Product target = operator.getTargetProduct();

        if(!areEquivalentProducts(target, validationProduct, 0.0f, 0.0f, 0.0000000001f)) {
            throw new Exception("Generated and validation products are not identical. " +
                                        "If the validation product was not generated in this computer, this fail could not be relevant.");
        }
    }*/


    @Test
    public void testSWAMEquivalentProduct() throws Exception {
        DimapProductReaderPlugIn readerPlugInSource = new DimapProductReaderPlugIn();
        final DimapProductReader productReaderSource = new DimapProductReader(readerPlugInSource);
        final Product source = productReaderSource.readProductNodes(InversionTest.getTestDataPath("mini_lizard_2016_03_23_tdg_cmn_u0_tau0p18_LEboa.dim").toString(), null);

        DimapProductReaderPlugIn readerPlugInValidation = new DimapProductReaderPlugIn();
        final DimapProductReader productReaderValidation = new DimapProductReader(readerPlugInValidation);
        final Product validationProduct = productReaderValidation.readProductNodes(InversionTest.getTestDataPath("mini_LIZARD_SAMBUCA_ALL.dim").toString(), null);

        File moduleDir = InversionTest.getResourceFile("/sambuca_snap_op.py");

        URL infoXmlFile = PyOperatorSpi.class.getResource("/sambuca_snap_op-info.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(infoXmlFile, getClass().getClassLoader());
        PyOperatorSpi spi = new PyOperatorSpi(descriptor);

        PyOperator operator = new PyOperator();
        operator.setSpi(spi);
        operator.setParameterDefaultValues();
        operator.setPythonModulePath(moduleDir.getParentFile().getPath());
        operator.setPythonModuleName("sambuca_snap_op");
        operator.setPythonClassName("sambuca_snap_op");
        operator.setParameter("xmlpath_sensor", getTestDataPath("swampy_s2_5_bands_filter_nedr.xml").toString());
        operator.setParameter("xmlpath_parameters", getTestDataPath("swampy_s2_parameters_depth30.xml").toString());
        operator.setParameter("xmlpath_siop", getTestDataPath("swampy_s2_6_siop_Lizard.xml").toString());
        operator.setParameter("error_name", "alpha_f");
        operator.setParameter("opt_method", "SLSQP");
        operator.setParameter("min_wlen", "400.0");
        operator.setParameter("max_wlen", "750.0");
        operator.setParameter("above_rrs_flag", true);
        operator.setParameter("shallow_flag", true);
        operator.setParameter("relaxed_cons", true);
        operator.setSourceProduct("source", source);
        Product target = operator.getTargetProduct();

        if(!areEquivalentProducts(target, validationProduct, 0.02f, 0.05f, 0.0000000001f)) {
            throw new Exception("Generated and validation products are not identical.");
        }
    }

    public static File getResourceFile(String name) {
        URL resource = PyOperator.class.getResource(name);
        assertNotNull("missing resource '" + name + "'", resource);
        return new File(URI.create(resource.toString()));
    }

    public static Path getTestDataDirPath() throws Exception {
        Path dir = Paths.get("./src/test/data/");
        if (!Files.exists(dir)) {
            dir = Paths.get("./sen2coral-inversion/src/test/data/");
            if (!Files.exists(dir)) {
                throw new Exception(String.format("Can't find my test data. Where is '" + dir.toString() + "'?"));
            }
        }
        return dir;
    }

    public static Path getTestDataPath(String path) throws Exception {
        Path fullPath = getTestDataDirPath().resolve(path);
        if (!Files.exists(fullPath)) {
            throw new Exception(String.format("Can't find my test data. Where is '" + fullPath.toString() + "'?"));
        }
        return fullPath;
    }

    public static boolean areEquivalentProducts(Product product1, Product product2, float dnDiffPercentageAllowed, float nPixelsDiffAllowed, float minValueNotConsideredZero) {
        if (product1 == null || product2 == null || product1.getNumBands() != product2.getNumBands()) {
            return false;
        }


        for (int i = 0; i < product1.getNumBands(); i++) {
            int count = 0;
            Band band1 = product1.getBandAt(i);
            Band band2 = product2.getBandAt(i);
            //check Size
            int height = band1.getRasterHeight();
            int width = band1.getRasterWidth();
            if (height != band2.getRasterHeight() || width != band2.getRasterWidth()) {
                return false;
            }

            // readPixels
            final double[] band1Values = new double[height * width];
            final double[] band2Values = new double[height * width];

            try {
                band1.readPixels(0, 0, width, height, band1Values, ProgressMonitor.NULL);
                band2.readPixels(0, 0, width, height, band2Values, ProgressMonitor.NULL);
            } catch (IOException e) {
                return false;
            }

            for (int j = 0; j < height * width; j++) {
                if (Math.abs(band1Values[j]) > minValueNotConsideredZero) {
                    if (Math.abs((band1Values[j] - band2Values[j]) / band1Values[j]) > 0.02) {
                        count++;
                    }
                } else if (Math.abs(band2Values[j]) > minValueNotConsideredZero) {
                    if (Math.abs((band1Values[j] - band2Values[j]) / band2Values[j]) > 0.02) {
                        count++;
                    }
                }
            }
            if (((count * 1.0) / (height * width) > 0.05)) {
                return false;
            }
        }
        return true;
    }
}
