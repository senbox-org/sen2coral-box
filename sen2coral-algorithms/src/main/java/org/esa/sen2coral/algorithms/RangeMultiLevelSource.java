package org.esa.sen2coral.algorithms;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.GenericMultiLevelSource;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.media.jai.ImageLayout;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * Created by obarrile on 06/01/2017.
 */
public final class RangeMultiLevelSource extends GenericMultiLevelSource {

    private final double minThreshold;
    private final double maxThreshold;

    public RangeMultiLevelSource(MultiLevelSource source, double minThreshold, double maxThreshold) {
        super(new MultiLevelSource[]{source});
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
    }

    @Override
    protected RenderedImage createImage(RenderedImage[] sourceImages, int level) {
        final RenderedImage source = sourceImages[0];
        final SampleModel sampleModel = new SingleBandedSampleModel(DataBuffer.TYPE_BYTE,
                                                                    source.getSampleModel().getWidth(),
                                                                    source.getSampleModel().getHeight());


        final ImageLayout imageLayout = new ImageLayout(source);
        imageLayout.setSampleModel(sampleModel);
        imageLayout.setColorModel(null);

        return new RangeOpImage(source, minThreshold, maxThreshold, imageLayout);
    }
}
