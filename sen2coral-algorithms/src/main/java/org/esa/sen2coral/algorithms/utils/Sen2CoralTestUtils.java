package org.esa.sen2coral.algorithms.utils;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by obarrile on 05/01/2017.
 */
public class Sen2CoralTestUtils {

    public static void compareIntegerPixels(final Band band, final Band expectedBand) throws Exception {

        if (band == null)
            throwErr("targetBand is null");
        if (expectedBand == null)
            throwErr("expectedBand is null");

        //check Size
        if(band.getRasterHeight() != expectedBand.getRasterHeight() || band.getRasterWidth() != expectedBand.getRasterWidth())
            throwErr("Bands have different size");


        // readPixels
        final int[] bandValues = new int[band.getRasterHeight()*band.getRasterWidth()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), bandValues, ProgressMonitor.NULL);

        final int[] expectedValues = new int[band.getRasterHeight()*band.getRasterWidth()];
        expectedBand.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), expectedValues, ProgressMonitor.NULL);


        if (!Arrays.equals(bandValues, expectedValues)) {
            throwErr("Pixels are different");
        }
    }

    public static void compareFloatPixels(final Band band, final Band expectedBand, final float delta) throws Exception {

        if (band == null)
            throwErr("targetBand is null");
        if (expectedBand == null)
            throwErr("expectedBand is null");

        //check Size
        if(band.getRasterHeight() != expectedBand.getRasterHeight() || band.getRasterWidth() != expectedBand.getRasterWidth())
            throwErr("Bands have different size");


        // readPixels
        final float[] bandValues = new float[band.getRasterHeight()*band.getRasterWidth()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), bandValues, ProgressMonitor.NULL);

        final float[] expectedValues = new float[band.getRasterHeight()*band.getRasterWidth()];
        expectedBand.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), expectedValues, ProgressMonitor.NULL);


        for(int i = 0 ; i < band.getRasterHeight()*band.getRasterWidth(); i++) {
            if(floatsAreDifferent(bandValues[i], expectedValues[i], delta))
                throwErr("Pixels are different");
        }
    }


    public static void compareDoublePixels(final Band band, final Band expectedBand, final double delta) throws Exception {

        if (band == null)
            throwErr("targetBand is null");
        if (expectedBand == null)
            throwErr("expectedBand is null");

        //check Size
        if(band.getRasterHeight() != expectedBand.getRasterHeight() || band.getRasterWidth() != expectedBand.getRasterWidth())
            throwErr("Bands have different size");


        // readPixels
        final double[] bandValues = new double[band.getRasterHeight()*band.getRasterWidth()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), bandValues, ProgressMonitor.NULL);

        final double[] expectedValues = new double[band.getRasterHeight()*band.getRasterWidth()];
        expectedBand.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), expectedValues, ProgressMonitor.NULL);


        for(int i = 0 ; i < band.getRasterHeight()*band.getRasterWidth(); i++) {
            if(doublesAreDifferent(bandValues[i], expectedValues[i], delta))
                throwErr("Pixels are different");
        }
    }

    public static boolean floatsAreDifferent(float f1, float f2, float delta) {
        return Float.compare(f1, f2) == 0 ? false : Math.abs(f1 - f2) > delta;
    }

    public static boolean doublesAreDifferent(double f1, double f2, double delta) {
        return Double.compare(f1, f2) == 0 ? false : Math.abs(f1 - f2) > delta;
    }

    private static void throwErr(final String description) throws Exception {
        throw new Exception(description);
    }

    public static Path getTestDataDirPath() throws Exception {
        Path dir = Paths.get("./src/test/data/");
        if (!Files.exists(dir)) {
            dir = Paths.get("./sen2coral-algorithms/src/test/data/");
            if (!Files.exists(dir)) {
                throwErr(String.format("Can't find my test data. Where is '" + dir.toString() + "'?"));
            }
        }
        return dir;
    }

    public static Path getTestDataPath(String path) throws Exception {
        Path fullPath = getTestDataDirPath().resolve(path);
        if (!Files.exists(fullPath)) {
            throwErr(String.format("Can't find my test data. Where is '" + fullPath.toString() + "'?"));
        }
        return fullPath;
    }
}
