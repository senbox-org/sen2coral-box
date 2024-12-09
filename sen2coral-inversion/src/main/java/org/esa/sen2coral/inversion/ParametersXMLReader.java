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
public class ParametersXMLReader {
    Document documentJDOM = null;

    public ParametersXMLReader(File file) {

        SAXBuilder builder = new SAXBuilder();
        try {
            documentJDOM = builder.build(file);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getQFactor() {
        Element element= documentJDOM.getRootElement().getChild("q_factor");
        double qFactor = Double.parseDouble(element.getValue());
        return qFactor;
    }

    public double getOffNadir() {
        Element element= documentJDOM.getRootElement().getChild("off_nadir");
        double offNadir = Double.parseDouble(element.getValue());
        return offNadir;
    }

    public double getThetaAir() {
        Element element= documentJDOM.getRootElement().getChild("theta_air");
        double thetaAir = Double.parseDouble(element.getValue());
        return thetaAir;
    }

    public double[][] getBounds() {
        List minList= documentJDOM.getRootElement().getChild("p_min").getChildren();
        List maxList= documentJDOM.getRootElement().getChild("p_max").getChildren();
        double[][] bounds = new double[2][minList.size()];
        for(int i = 0 ; i<minList.size() ; i++ ) {
            Element elementMin = (Element) minList.get(i);
            Element elementMax = (Element) maxList.get(i);
            bounds[0][i] = Float.parseFloat(elementMin.getValue());
            bounds[1][i] = Float.parseFloat(elementMax.getValue());
        }
        return bounds;
    }
}
