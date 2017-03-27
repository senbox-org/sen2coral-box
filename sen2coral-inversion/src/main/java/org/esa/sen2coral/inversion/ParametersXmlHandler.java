package org.esa.sen2coral.inversion;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Created by obarrile on 27/03/2017.
 */
public class ParametersXmlHandler extends DefaultHandler {

    private final int EXPECTED_ITEMS = 7;
    private boolean qFactorFound;
    private boolean offNadirFound;
    private boolean pMaxFound;
    private boolean pMinFound;
    private boolean thetaAirFound;
    private int numberOfpMaxItems;
    private int numberOfpMinItems;
    private boolean insidePMin;
    private boolean insidePMax;

    @Override
    public void startDocument () throws SAXException {
        qFactorFound = false;
        offNadirFound = false;
        pMaxFound = false;
        pMinFound = false;
        thetaAirFound = false;
        numberOfpMaxItems = 0;
        numberOfpMinItems = 0;
        insidePMin = false;
        insidePMax = false;
    }

    @Override
    public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(localName.equals("q_factor")) {
            qFactorFound = true;
            return;
        }
        if(localName.equals("off_nadir")) {
            offNadirFound = true;
            return;
        }
        if(localName.equals("theta_air")) {
            thetaAirFound = true;
            return;
        }
        if(localName.equals("p_max")) {
            pMaxFound = true;
            insidePMax = true;
            return;
        }
        if(localName.equals("p_min")) {
            pMinFound = true;
            insidePMin = true;
            return;
        }
        if(localName.equals("item") && insidePMax) {
            numberOfpMaxItems ++;
            return;
        }
        if(localName.equals("item") && insidePMin) {
            numberOfpMinItems ++;
            return;
        }
    }


    @Override
    public void endElement (String uri, String localName, String qName) throws SAXException {
        if(localName.equals("p_max")) {
            insidePMax = false;
            return;
        }
        if(localName.equals("p_min")) {
            insidePMin = false;
            return;
        }
    }

    @Override
    public void endDocument () throws SAXException {
        if(!(pMaxFound && pMinFound && qFactorFound && offNadirFound && thetaAirFound)) {
            throw new SAXException("Incomplete parameter file.");
        }

        if((numberOfpMaxItems != EXPECTED_ITEMS) || (numberOfpMinItems != EXPECTED_ITEMS)) {
            throw new SAXException("Incompatible number of parameters.");
        }
    }
}
