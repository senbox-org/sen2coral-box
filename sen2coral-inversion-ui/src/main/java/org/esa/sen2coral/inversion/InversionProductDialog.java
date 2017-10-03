package org.esa.sen2coral.inversion;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by obarrile on 24/03/2017.
 */
public class InversionProductDialog extends DefaultSingleTargetProductDialog {

    private static final float TIME_THRESHOLD_WARNING = 3; //in minutes
    private static final float TIME_PER_PIXEL = 0.1f; //in seconds
    private static final float MINIMUM_BANDS = 4; //in seconds
    List<String> methods = Arrays.asList("SLSQP");
    List<String> errors = Arrays.asList("alpha", "alpha_f", "lsq", "f");

    public InversionProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(operatorName, appContext, title, helpID);
    }

    @Override
    protected boolean canApply() {
        if(!super.canApply()) {
            return false;
        }


        ArrayList sourceProductSelectorList = getDefaultIOParametersPanel().getSourceProductSelectorList();
        Product sourceProduct = ((SourceProductSelector) sourceProductSelectorList.get(0)).getSelectedProduct();
        if(sourceProduct == null) {
            this.showErrorDialog("Please specify a source product.");
            return false;
        }




        String valueError = (String) this.getBindingContext().getBinding("error_name").getPropertyValue();
        //boolean valueShallow = (boolean) this.getBindingContext().getBinding("shallow_flag").getPropertyValue();
        //boolean valueRelaxed = (boolean) this.getBindingContext().getBinding("relaxed_cons").getPropertyValue();
        File valueSiop = (File) this.getBindingContext().getBinding("xmlpath_siop").getPropertyValue();
        File valueParam = (File) this.getBindingContext().getBinding("xmlpath_parameters").getPropertyValue();
        String valueOptMethod = (String) this.getBindingContext().getBinding("opt_method").getPropertyValue();
        //boolean valueRrs = (boolean) this.getBindingContext().getBinding("above_rrs_flag").getPropertyValue();
        File valueSensor = (File) this.getBindingContext().getBinding("xmlpath_sensor").getPropertyValue();
        //float valueMax = (float) this.getBindingContext().getBinding("max_wlen").getPropertyValue();
        //float valueMin = (float) this.getBindingContext().getBinding("min_wlen").getPropertyValue();

//        //TODO check at least MINIMUM_BANDS
//        int validBandCount = 0;
//        for(int i = 0; i<sourceProduct.getNumBands() ; i++) {
//            Band band = sourceProduct.getBandAt(i);
//            float spectralWavelength = band.getSpectralWavelength();
//            if (spectralWavelength >= valueMin  && spectralWavelength <= valueMax) {
//                validBandCount ++;
//            }
//        }
//        if(validBandCount < MINIMUM_BANDS) {
//            this.showErrorDialog("Not enough valid spectral bands in the source product." +
//                                         "Try to change wavelength range or to add spectral information to the bands.");
//            return false;
//        }
//
//        //Check parameters
//        if(valueMin >= valueMax) {
//            this.showErrorDialog("Max value must be higher than Min value.");
//            return false;
//        }
        if(valueSiop == null) {
            this.showErrorDialog("Siop path cannot be null. Please, select a file.");
            return false;
        }
        if(valueParam == null) {
            this.showErrorDialog("Parameters path cannot be null. Please, select a file.");
            return false;
        }
        if(valueSensor == null) {
            this.showErrorDialog("Sensor path cannot be null. Please, select a file.");
            return false;
        }
        if(!methods.contains(valueOptMethod)) {
            this.showErrorDialog("Method not supported. Try with 'SLSQP'.");
            return false;
        }
        if(!errors.contains(valueError)) {
            this.showErrorDialog("Error not supported. Try with 'alpha', 'alpha_f', 'lsq' or 'f'");
            return false;
        }
        //Check content of xml files? If not Python will throw an exception
        if(!XmlFilesUtils.isValidSensorXmlFile(valueSensor)) {
            this.showErrorDialog("Invalid sensor xml file.");
            return false;
        }
        if(!XmlFilesUtils.isValidParametersXmlFile(valueParam)) {
            this.showErrorDialog("Invalid parameter xml file.");
            return false;
        }
        if(!XmlFilesUtils.isValidSiopXmlFile(valueSiop)) {
            this.showErrorDialog("Invalid siop xml file.");
            return false;
        }


        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        int numberOfPixelsToCompute = width * height;
        float estimatedTime = TIME_PER_PIXEL * numberOfPixelsToCompute;

        if (estimatedTime >= TIME_THRESHOLD_WARNING) {
            String message = String.format("Depending on the machine you are using, this may take %.1f minutes, do you want to proceed?",
                                           estimatedTime / 60.0);
            final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                             getTitle(), JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        return true;
    }


}
