package org.esa.sen2coral.inversion;

import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;

/**
 * Created by obarrile on 27/03/2017.
 */
public class XmlFilesUtils {
    public static boolean isValidSensorXmlFile (File file) {

        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            parserFactory.setNamespaceAware(true);
            parserFactory.setValidating(false);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(new SensorXmlHandler());
            reader.parse(file.toURI().toURL().toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isValidParametersXmlFile (File file) {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            parserFactory.setNamespaceAware(true);
            parserFactory.setValidating(false);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(new ParametersXmlHandler());
            reader.parse(file.toURI().toURL().toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isValidSiopXmlFile (File file) {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            parserFactory.setNamespaceAware(true);
            parserFactory.setValidating(false);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(new SiopXmlHandler());
            reader.parse(file.toURI().toURL().toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
