package org.esa.sen2coral.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by obarrile on 28/03/2017.
 */
public class InversionActivator implements Activator {

    @Override
    public void start() {
        Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata/sen2coral/xml");
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("sen2coral").resolve("xmlFiles");;
        if (auxdataDirectory == null) {
            SystemUtils.LOG.severe("Sen2Coral configuration error: failed to retrieve auxdata path");
            return;
        }
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            resourceInstaller.install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Sen2Coral configuration error: failed to create " + auxdataDirectory);
            return;
        }
    }

    @Override
    public void stop() {
        // Purposely no-op
    }
}
