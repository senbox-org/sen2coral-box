package org.esa.sen2coral.chains; /**
 * Created by obarrile on 18/11/2016.
 */

import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.modules.OnStart;

public class sen2coralProcessingChainsModule {
    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            ResourceUtils.installGraphs(this.getClass(), "org/esa/sen2coral/chains/graphs/");
        }
    }
}
