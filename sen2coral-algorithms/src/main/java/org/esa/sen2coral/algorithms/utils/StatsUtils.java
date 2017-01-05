package org.esa.sen2coral.algorithms.utils;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;

import java.io.IOException;

/**
 * Created by obarrile on 05/01/2017.
 */
public class StatsUtils {


    public static double[] getRegressionParameters(Band bandX, Band bandY, Mask mask) throws Exception{
        double[] parameters = null;

        //Check raster sizes
        if(bandX.getRasterHeight() != bandY.getRasterHeight() || bandX.getRasterHeight() != mask.getRasterHeight() ||
                bandX.getRasterWidth() != bandY.getRasterWidth() || bandX.getRasterWidth() != mask.getRasterWidth()) {
            throw new Exception("Invalid raster sizes, they are not identical");
        }

        //get noData values to exclude them
        double noDataX = bandX.getNoDataValue();
        double noDataY = bandY.getNoDataValue();

        //load data if it is not loaded
        try {
            mask.loadRasterData();
            bandX.loadRasterData();
            bandY.loadRasterData();
        } catch (IOException e) {
            throw new OperatorException("Unable to load the raster data.");
        }

        //compute statistics
        int numPixels = 0;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumYY = 0.0;
        double sumXY = 0.0;
        double meanX, meanY, sigmaXX, sigmaXY, sigmaYY;
        for (int i = 0; i < bandX.getRasterWidth(); i++) {
            for (int j = 0; j < bandY.getRasterHeight(); j++) {
                if (mask.getPixelInt(i, j) == 0) {
                    continue;
                }
                double X = bandX.getPixelDouble(i, j);
                double Y = bandY.getPixelDouble(i, j);
                if (X != noDataX && Y != noDataY) {
                    sumX = sumX + X;
                    sumY = sumY + Y;
                    sumXX = sumXX + X * X;
                    sumYY = sumYY + Y * Y;
                    sumXY = sumXY + X * Y;
                    numPixels++;
                }
            }
        }

        if (numPixels > 0) {
            meanX = sumX / numPixels;
            meanY = sumY / numPixels;
            sigmaXX = sumXX / numPixels - meanX * meanX;
            sigmaXY = sumXY / numPixels - meanX * meanY;
            sigmaYY = sumYY / numPixels - meanY * meanY;

            parameters = new double[5];
            parameters[0] = meanX;
            parameters[1] = meanY;
            parameters[2] = sigmaXX;
            parameters[3] = sigmaYY;
            parameters[4] = sigmaXY;
        }
        return parameters;
    }

}
