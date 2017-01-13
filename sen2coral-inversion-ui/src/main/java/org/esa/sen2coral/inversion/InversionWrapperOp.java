package org.esa.sen2coral.inversion;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import java.io.IOException;

/**
 * Created by obarrile on 12/01/2017.
 */
@OperatorMetadata(alias = "InversionWrapperOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        description = "InversionOp algorithm")
public class InversionWrapperOp extends Operator {


    @Override
    public void initialize() throws OperatorException {

        OperatorSpi spi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("py_modelInversion_op");
        Operator operator = spi.createOperator();
        operator.setParameter("lowerName", "band_4");
        operator.setParameter("upperName", "band_8");

        GeoTiffProductReaderPlugIn readerPlugIn2 = new GeoTiffProductReaderPlugIn();
        final GeoTiffProductReader productReader2 = new GeoTiffProductReader(readerPlugIn2);
        try {
            final Product source = productReader2.readProductNodes("G:\\sen2coral\\Produits_sen2coral\\lizard_2016_01_03_s2.tif", null);
            operator.setSourceProduct("source", source);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTargetProduct(operator.getTargetProduct());
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(InversionWrapperOp.class);
        }
    }
}
