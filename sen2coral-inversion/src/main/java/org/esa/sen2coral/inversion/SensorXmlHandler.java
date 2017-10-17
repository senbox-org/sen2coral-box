package org.esa.sen2coral.inversion;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Created by obarrile on 27/03/2017.
 */

public class SensorXmlHandler extends DefaultHandler {

    private int nNumberOfWavelengths;
    private ArrayList<Float> wavelengths;
    private ArrayList<Integer> nFilterWeights;
    private ArrayList<ArrayList<Float>> weights;
    private int nNumberOfNedrWavelengths;
    private ArrayList<Float> centralWavelengths;
    private int nNumberOfNedrValues;
    private ArrayList<Float> nedrs;
    private int itemDepth;
    private boolean insideSensorFilter;
    private boolean insideNedr;
    private boolean finishedWL;
    private int count;
    @Override
    public void startDocument () throws SAXException {
        nNumberOfWavelengths = 0;
        nFilterWeights = new ArrayList<>(13);
        nNumberOfNedrWavelengths = 0;
        nNumberOfNedrValues = 0;
        itemDepth = 0;
        insideSensorFilter = false;
        insideNedr = false;
        finishedWL = false;
        count = 0;
    }

    @Override
    public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(localName.equals("sensor_filter")) {
            insideSensorFilter = true;
            return;
        }
        if(localName.equals("nedr")) {
            insideNedr = true;
            return;
        }
        if(localName.equals("item")) {
            itemDepth++;
        }
        if(itemDepth == 2 && insideSensorFilter && !finishedWL) {

            nNumberOfWavelengths++;
            return;
        }

        if(itemDepth == 3 && insideSensorFilter && finishedWL) {
            count++;
            return;
        }

        if(itemDepth == 2 && insideNedr && !finishedWL) {
            nNumberOfNedrWavelengths++;
            return;
        }

        if(itemDepth == 2 && insideNedr && finishedWL) {
            nNumberOfNedrValues++;
            return;
        }
    }


    @Override
    public void endElement (String uri, String localName, String qName) throws SAXException {
        if(localName.equals("sensor_filter")) {
            insideSensorFilter = false;
            finishedWL = false;
            return;
        }
        if(localName.equals("nedr")) {
            insideNedr = false;
            finishedWL = false;
            return;
        }

        if(localName.equals("item")) {
            if(itemDepth == 1 && nNumberOfWavelengths > 0 && insideSensorFilter) {
                finishedWL = true;
            }
            if(itemDepth == 1 && nNumberOfNedrWavelengths > 0 && insideNedr) {
                finishedWL = true;
            }
            if(itemDepth == 2 && insideSensorFilter && finishedWL) {
                nFilterWeights.add(count);
                count = 0;
            }
            itemDepth--;
        }
    }

    @Override
    public void endDocument () throws SAXException {
        if(nFilterWeights.size() == 0) {
            throw new SAXException("No band data.");
        }
        for(int nFilterWeight : nFilterWeights) {
            if(nFilterWeight != nNumberOfWavelengths) {
                throw new SAXException("Filter not compatible with wavelengths.");
            }
        }
        if((nFilterWeights.size() != nNumberOfNedrValues) || (nNumberOfNedrValues != nNumberOfNedrWavelengths)) {
            throw new SAXException("No compatible NEDR values.");
        }
    }
}
