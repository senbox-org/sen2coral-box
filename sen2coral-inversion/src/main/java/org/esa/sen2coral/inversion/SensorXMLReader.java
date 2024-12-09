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
public class SensorXMLReader {

    Document documentJDOM = null;
    public SensorXMLReader(File file) {

        SAXBuilder builder = new SAXBuilder();
        try {
            documentJDOM = builder.build(file);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double[] getWavelengths() {
        List wavelengthList = ((Element) documentJDOM.getRootElement().getChild("sensor_filter").getChildren().get(0)).getChildren();
        double[] wavelengths = new double[wavelengthList.size()];
        for(int i = 0 ; i<wavelengthList.size() ; i++ ) {
            Element element = (Element) wavelengthList.get(i);
            wavelengths[i] = Double.parseDouble(element.getValue());
        }
        return wavelengths;
    }

    public double[][] getWeights() {
        List bands = ((Element) documentJDOM.getRootElement().getChild("sensor_filter").getChildren().get(1)).getChildren();
        List band1 = ((Element)bands.get(0)).getChildren();
        double[][] weights = new double[bands.size()][band1.size()];
        for(int i = 0 ; i<bands.size() ; i++ ) {
            Element element = ((Element)bands.get(i));
            for(int j = 0 ; j<band1.size() ; j++ ) {
                Element element2 = (Element)element.getChildren().get(j);
                weights[i][j] = Double.parseDouble(element2.getValue());
            }
        }
        return weights;
    }

    public double[] getCentralWavelengths() {
        List wavelengthList = ((Element) documentJDOM.getRootElement().getChild("nedr").getChildren().get(0)).getChildren();
        double[] wavelengths = new double[wavelengthList.size()];
        for(int i = 0 ; i<wavelengthList.size() ; i++ ) {
            Element element = (Element) wavelengthList.get(i);
            wavelengths[i] = Double.parseDouble(element.getValue());
        }
        return wavelengths;
    }

    public double[] getNEDRs() {
        List nedrList = ((Element) documentJDOM.getRootElement().getChild("nedr").getChildren().get(1)).getChildren();
        double[] nedrs = new double[nedrList.size()];
        for(int i = 0 ; i<nedrList.size() ; i++ ) {
            Element element = (Element) nedrList.get(i);
            nedrs[i] = Double.parseDouble(element.getValue());
        }
        return nedrs;
    }
}
