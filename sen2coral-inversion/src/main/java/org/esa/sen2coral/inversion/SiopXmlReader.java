package org.esa.sen2coral.inversion;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by obarrile on 17/08/2017.
 */
public class SiopXmlReader {
    Document documentJDOM = null;

    public SiopXmlReader(File file) {

        SAXBuilder builder = new SAXBuilder();
        try {
            documentJDOM = builder.build(file);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getXNapLambda0x() {
        Element element= documentJDOM.getRootElement().getChild("x_nap_lambda0x");
        double xNapLambda0x = Double.parseDouble(element.getValue());
        return xNapLambda0x;
    }

    public int getSubstrateCount() {
        Element element= documentJDOM.getRootElement().getChild("substrates");
        List substrates = element.getChildren();
        return substrates.size();
    }

    public double[][] getSubstrates(int index) {
        Element parentElement= documentJDOM.getRootElement().getChild("substrates");
        List substratesWaves = ((Element) ((Element) parentElement.getChildren().get(index)).getChildren().get(0)).getChildren();
        List substratesValues = ((Element) ((Element) parentElement.getChildren().get(index)).getChildren().get(1)).getChildren();

        double[][] substrates = new double[2][substratesWaves.size()];
        for(int i = 0 ; i<substratesWaves.size() ; i++ ) {
            Element elementWave = (Element) substratesWaves.get(i);
            Element elementValue = (Element) substratesValues.get(i);
            substrates[0][i] = Float.parseFloat(elementWave.getValue());
            substrates[1][i] = Float.parseFloat(elementValue.getValue());
        }
        return substrates;
    }

    public double[][] getSubstrates() {
        int count = getSubstrateCount();
        int length = getSubstrates(0).length;
        double[][] substrates = new double[count][length];
        for(int i=0;i<count;i++) {
            substrates[i]=getSubstrates(i)[1];
        }
        return substrates;
    }

    public double getBbLambdaRef() {
        Element element= documentJDOM.getRootElement().getChild("bb_lambda_ref");
        double bbLambdaRef = Double.parseDouble(element.getValue());
        return bbLambdaRef;
    }

    public double getANapLambda0nap() {
        Element element= documentJDOM.getRootElement().getChild("a_nap_lambda0nap");
        double aNapLambda0nap = Double.parseDouble(element.getValue());
        return aNapLambda0nap;
    }

    public double[][] getAWater() {
        Element parentElement= documentJDOM.getRootElement().getChild("a_water");
        List itemWaves = ((Element) parentElement.getChildren().get(0)).getChildren();
        List itemValues = ((Element) parentElement.getChildren().get(1)).getChildren();

        double[][] aWater = new double[2][itemWaves.size()];
        for(int i = 0 ; i<itemWaves.size() ; i++ ) {
            Element elementWave = (Element) itemWaves.get(i);
            Element elementValue = (Element) itemValues.get(i);
            aWater[0][i] = Float.parseFloat(elementWave.getValue());
            aWater[1][i] = Float.parseFloat(elementValue.getValue());
        }
        return aWater;
    }

    public double getBbNapSlope() {
        Element element= documentJDOM.getRootElement().getChild("bb_nap_slope");
        try{
        double bbNapSlope = Double.parseDouble(element.getValue());
        return bbNapSlope;
        } catch (Exception e) {
            return 0.0d;
        }
    }

    public double getACdomLambda0cdom() {
        Element element= documentJDOM.getRootElement().getChild("a_cdom_lambda0cdom");
        double aCdomLambda0cdom = Double.parseDouble(element.getValue());
        return aCdomLambda0cdom;
    }

    public String[] getSubstrateNames() {
        Element element= documentJDOM.getRootElement().getChild("substrate_names");
        List items = element.getChildren();
        String[] substrateNames = new String[items.size()];
        for(int i = 0 ; i<items.size() ; i++ ) {
            Element elementName = (Element) items.get(i);
            substrateNames[i] = elementName.getValue();
        }

        return substrateNames;
    }

    public double getACdomSlope() {
        Element element= documentJDOM.getRootElement().getChild("a_cdom_slope");
        double aCdomSlope = Double.parseDouble(element.getValue());
        return aCdomSlope;
    }

    public double getLambda0nap() {
        Element element= documentJDOM.getRootElement().getChild("lambda0nap");
        double lambda0nap = Double.parseDouble(element.getValue());
        return lambda0nap;
    }

    public double getXPhLambda0x() {
        Element element= documentJDOM.getRootElement().getChild("x_ph_lambda0x");
        double xPhLambda0x = Double.parseDouble(element.getValue());
        return xPhLambda0x;
    }

    public double getBbPhSlope() {
        Element element= documentJDOM.getRootElement().getChild("bb_ph_slope");
        double bbPhSlope = Double.parseDouble(element.getValue());
        return bbPhSlope;
    }

    public double[][] getAPhStar() {
        Element parentElement= documentJDOM.getRootElement().getChild("a_ph_star");
        List itemWaves = ((Element) parentElement.getChildren().get(0)).getChildren();
        List itemValues = ((Element) parentElement.getChildren().get(1)).getChildren();

        double[][] aPhStar = new double[2][itemWaves.size()];
        for(int i = 0 ; i<itemWaves.size() ; i++ ) {
            Element elementWave = (Element) itemWaves.get(i);
            Element elementValue = (Element) itemValues.get(i);
            aPhStar[0][i] = Float.parseFloat(elementWave.getValue());
            aPhStar[1][i] = Float.parseFloat(elementValue.getValue());
        }
        return aPhStar;
    }

    public double getANapSlope() {
        Element element= documentJDOM.getRootElement().getChild("a_nap_slope");
        double aNapSlope = Double.parseDouble(element.getValue());
        return aNapSlope;
    }

    public double getLambda0x() {
        Element element= documentJDOM.getRootElement().getChild("lambda0x");
        double lambda0x = Double.parseDouble(element.getValue());
        return lambda0x;
    }

    public double getLambda0cdom() {
        Element element= documentJDOM.getRootElement().getChild("lambda0cdom");
        double lambda0cdom = Double.parseDouble(element.getValue());
        return lambda0cdom;
    }

    public double getWaterRefractiveIndex() {
        Element element= documentJDOM.getRootElement().getChild("water_refractive_index");
        double waterRefractiveIndex = Double.parseDouble(element.getValue());
        return waterRefractiveIndex;
    }
}
