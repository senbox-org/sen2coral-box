package org.esa.sen2coral.algorithms;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

import java.io.File;

/**
 * deglint operator
 */
@OperatorMetadata(alias = "RadiometricNormalisationPIFsOp",
        category = "Raster",
        authors = "Omar Barrilero",
        version = "1.0",
        description = "RadiometricNormalisationPIFsOp algorithm")
public class RadiometricNormalisationPIFsOp extends PixelOperator {

    @SourceProduct(alias = "reference", description = "The source product which serves as reference.")
    private Product referenceProduct;

    @SourceProduct(alias = "slave", description = "The source product which serves as slave.")
    private Product slaveProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter(description = "Pseudo-Invariant Features (PIFs) vector File",
            label = "Pseudo-Invariant Features (PIFs) vector File")
    private String pifVector;


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RadiometricNormalisationPIFsOp() {

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {

    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {

    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RadiometricNormalisationPIFsOp.class);
        }
    }
}
