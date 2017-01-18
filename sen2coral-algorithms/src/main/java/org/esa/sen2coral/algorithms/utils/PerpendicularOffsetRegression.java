package org.esa.sen2coral.algorithms.utils;

import org.apache.commons.math3.util.FastMath;

import java.io.Serializable;

/**
 * Created by obarrile on 18/01/2017.
 */
public class PerpendicularOffsetRegression implements Serializable {

    private static final long serialVersionUID = 8053648990705635972L;
    private double sumX;
    private double sumXX;
    private double sumY;
    private double sumYY;
    private double sumXY;
    private long n;


    public PerpendicularOffsetRegression() {
        this.sumX = 0.0D;
        this.sumXX = 0.0D;
        this.sumY = 0.0D;
        this.sumYY = 0.0D;
        this.sumXY = 0.0D;
        this.n = 0L;

    }

    public void addData(double x, double y) {

        this.sumX += x;
        this.sumY += y;
        this.sumXX += x * x;
        this.sumYY += y * y;
        this.sumXY += x * y;
        ++this.n;
    }


    public void clear() {
        this.sumX = 0.0D;
        this.sumXX = 0.0D;
        this.sumY = 0.0D;
        this.sumYY = 0.0D;
        this.sumXY = 0.0D;
        this.n = 0L;
    }

    public long getN() {
        return this.n;
    }

    public double getSumX() {
        return sumX;
    }

    public double getSumXX() {
        return sumXX;
    }

    public double getSumY() {
        return sumY;
    }

    public double getSumYY() {
        return sumYY;
    }

    public double getSumXY() {
        return sumXY;
    }

    public double getMeanX() {
        return  sumX/n;
    }

    public double getMeanY() {
        return  sumY/n;
    }

    public double getSigmaXX() {
        return  sumXX / n - getMeanX() * getMeanX();
    }

    public double getSigmaYY() {
        return  sumYY / n - getMeanY() * getMeanY();
    }

    public double getSigmaXY() {
        return  sumXY / n - getMeanX() * getMeanY();
    }

    public double getSlope() {
        if (n < 2) return 0.0D/0.0;
        double a = (getSigmaXX()-getSigmaYY())/(2*getSigmaXY());
        return a + Math.sqrt(a*a+1);
    }

    public double getIntercept() {
        if (n < 2) return 0.0D/0.0;
        return getMeanY()-getSlope()*getMeanX();
    }

    public double getRSquare() {
        return (getSigmaXY()*getSigmaXY()) / (getSigmaXX()*getSigmaYY());
    }

}
