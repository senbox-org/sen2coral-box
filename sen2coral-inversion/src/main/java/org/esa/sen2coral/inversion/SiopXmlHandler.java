package org.esa.sen2coral.inversion;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Created by obarrile on 27/03/2017.
 */
public class SiopXmlHandler extends DefaultHandler {


    private final String X_PH_LAMBDA0X = "x_ph_lambda0x";
    private final String BB_PH_SLOPE = "bb_ph_slope";
    private final String A_CDOM_SLOPE = "a_cdom_slope";
    private final String X_NAP_LAMBDA0X = "x_nap_lambda0x";
    private final String A_NAP_SLOPE = "a_nap_slope";
    private final String BB_NAP_SLOPE = "bb_nap_slope";
    private final String A_NAP_LAMBDA0NAP = "a_nap_lambda0nap";
    private final String SUBSTRATE_NAMES = "substrate_names";
    private final String LAMBDA0NAP = "lambda0nap";
    private final String SUBSTRATES = "substrates";
    private final String BB_LAMBDA_REF = "bb_lambda_ref";
    private final String A_WATER = "a_water";
    private final String LAMBDA0CDOM = "lambda0cdom";
    private final String A_PH_STAR = "a_ph_star";
    private final String A_CDOM_LAMBDA0CDOM = "a_cdom_lambda0cdom";
    private final String LAMBDA0X = "lambda0x";
    private final String WATER_REFLECTANCE_INDEX = "water_refractive_index";

    private final String ITEM = "item";

    private boolean xPhLambda0xFound;
    private boolean bbPhSlopeFound;
    private boolean aCdomSlopeFound;
    private boolean xNapLambda0xFound;
    private boolean aNapSlopeFound;
    private boolean bbNapSlopeFound;
    private boolean aNapLambda0napFound;
    private boolean substrateNamesFound;
    private boolean lambda0napFound;
    private boolean substratesFound;
    private boolean bbLambdaRefFound;
    private boolean aWaterFound;
    private boolean lambda0cdomFound;
    private boolean aPhStarFound;
    private boolean aCdomLambda0cdomFound;
    private boolean lambda0xFound;
    private boolean waterRefractiveIndexFound;

    private int numberOfSubstrateNames;
    private int numberOfAWaterWavelengths;
    private int numberOfAWaterValues;
    private int numberOfAPhStarWavelengths;
    private int numberOfAPhStarValues;
    private ArrayList<Integer> substratesWavelengths;
    private ArrayList<Integer> substratesValues;

    private boolean insideSubstrateNames;
    private boolean insideAWater;
    private boolean insideAPhStar;
    private boolean insideSubstrates;

    private int itemDepth;
    private boolean finishedWL;
    private int count;

    @Override
    public void startDocument() throws SAXException {

        xPhLambda0xFound = false;
        bbPhSlopeFound = false;
        aCdomSlopeFound = false;
        xNapLambda0xFound = false;
        aNapSlopeFound = false;
        bbNapSlopeFound = false;
        aNapLambda0napFound = false;
        substrateNamesFound = false;
        lambda0napFound = false;
        substratesFound = false;
        bbLambdaRefFound = false;
        aWaterFound = false;
        lambda0cdomFound = false;
        aPhStarFound = false;
        aCdomLambda0cdomFound = false;
        lambda0xFound = false;
        waterRefractiveIndexFound = false;

        numberOfSubstrateNames = 0;
        numberOfAWaterWavelengths = 0;
        numberOfAWaterValues = 0;
        numberOfAPhStarWavelengths = 0;
        numberOfAPhStarValues = 0;
        substratesWavelengths = new ArrayList<>(7);
        substratesValues = new ArrayList<>(7);

        insideSubstrateNames = false;
        insideAWater = false;
        insideAPhStar = false;
        insideSubstrates = false;

        finishedWL = false;
        count = 0;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (localName.equals(X_PH_LAMBDA0X)) {
            xPhLambda0xFound = true;
            return;
        }
        if (localName.equals(BB_PH_SLOPE)) {
            bbPhSlopeFound = true;
            return;
        }
        if (localName.equals(A_CDOM_SLOPE)) {
            aCdomSlopeFound = true;
            return;
        }
        if (localName.equals(X_NAP_LAMBDA0X)) {
            xNapLambda0xFound = true;
            return;
        }
        if (localName.equals(A_NAP_SLOPE)) {
            aNapSlopeFound = true;
            return;
        }
        if (localName.equals(BB_NAP_SLOPE)) {
            bbNapSlopeFound = true;
            return;
        }
        if (localName.equals(A_NAP_LAMBDA0NAP)) {
            aNapLambda0napFound = true;
            return;
        }
        if (localName.equals(SUBSTRATE_NAMES)) {
            substrateNamesFound = true;
            insideSubstrateNames = true;
            return;
        }
        if (localName.equals(LAMBDA0NAP)) {
            lambda0napFound = true;
            return;
        }
        if (localName.equals(SUBSTRATES)) {
            substratesFound = true;
            insideSubstrates = true;
            return;
        }
        if (localName.equals(BB_LAMBDA_REF)) {
            bbLambdaRefFound = true;
            return;
        }
        if (localName.equals(A_WATER)) {
            aWaterFound = true;
            insideAWater = true;
            return;
        }
        if (localName.equals(LAMBDA0CDOM)) {
            lambda0cdomFound = true;
            return;
        }
        if (localName.equals(A_PH_STAR)) {
            aPhStarFound = true;
            insideAPhStar = true;
            return;
        }
        if (localName.equals(A_CDOM_LAMBDA0CDOM)) {
            aCdomLambda0cdomFound = true;
            return;
        }
        if (localName.equals(LAMBDA0X)) {
            lambda0xFound = true;
            return;
        }
        if (localName.equals(WATER_REFLECTANCE_INDEX)) {
            waterRefractiveIndexFound = true;
            return;
        }

        if (localName.equals(ITEM)) {
            itemDepth++;

            if (insideSubstrateNames) {
                numberOfSubstrateNames++;
                return;
            }

            if (itemDepth == 3 && insideSubstrates) {
                count++;
                return;
            }

            if (itemDepth == 2 && insideAWater && !finishedWL) {
                numberOfAWaterWavelengths++;
                return;
            }

            if (itemDepth == 2 && insideAWater && finishedWL) {
                numberOfAWaterValues++;
                return;
            }

            if (itemDepth == 2 && insideAPhStar && !finishedWL) {
                numberOfAPhStarWavelengths++;
                return;
            }

            if (itemDepth == 2 && insideAPhStar && finishedWL) {
                numberOfAPhStarValues++;
                return;
            }

        }
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals(SUBSTRATE_NAMES)) {
            insideSubstrateNames = false;
            finishedWL = false;
            return;
        }
        if (localName.equals(SUBSTRATES)) {
            insideSubstrates = false;
            finishedWL = false;
            return;
        }
        if (localName.equals(A_WATER)) {
            insideAWater = false;
            finishedWL = false;
            return;
        }
        if (localName.equals(A_PH_STAR)) {
            insideAPhStar = false;
            finishedWL = false;
            return;
        }


        if (localName.equals(ITEM)) {

            if (itemDepth == 2 && count > 0 && insideSubstrates && !finishedWL) {
                finishedWL = true;
                substratesWavelengths.add(count);
                count = 0;
            }

            if (itemDepth == 2 && count > 0 && insideSubstrates && finishedWL) {
                finishedWL = false;
                substratesValues.add(count);
                count = 0;
            }

            if (itemDepth == 1 && numberOfAWaterWavelengths > 0 && insideAWater) {
                finishedWL = true;
            }

            if (itemDepth == 1 && numberOfAPhStarWavelengths > 0 && insideAPhStar) {
                finishedWL = true;
            }

            itemDepth--;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!(xPhLambda0xFound &&
                bbPhSlopeFound &&
                aCdomSlopeFound &&
                xNapLambda0xFound &&
                aNapSlopeFound &&
                bbNapSlopeFound &&
                aNapLambda0napFound &&
                substrateNamesFound &&
                lambda0napFound &&
                substratesFound &&
                bbLambdaRefFound &&
                aWaterFound &&
                lambda0cdomFound &&
                aPhStarFound &&
                aCdomLambda0cdomFound &&
                lambda0xFound &&
                waterRefractiveIndexFound)) {

            throw new SAXException("Not all data required in siop xml file.");
        }

        if ((numberOfSubstrateNames != substratesWavelengths.size()) || (numberOfSubstrateNames != substratesWavelengths.size())) {
            throw new SAXException("Error when reading substrate data.");
        }
        for (int i = 0; i < numberOfSubstrateNames; i++) {
            if (substratesValues.get(i).compareTo(substratesWavelengths.get(i)) != 0) {
                throw new SAXException("Incompatible substrate data.");
            }
        }
        if (numberOfAWaterValues != numberOfAWaterWavelengths) {
            throw new SAXException("Incompatible a_water data.");
        }
        if (numberOfAPhStarValues != numberOfAPhStarWavelengths) {
            throw new SAXException("Incompatible a_ph_star data.");
        }
    }
}
