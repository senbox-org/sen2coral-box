package org.esa.sen2coral.inversion;


import org.esa.snap.core.gpf.ui.DefaultOperatorAction;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ActionID(
        category = "Tools",
        id = "InversionAction"
)
@ActionRegistration(
        displayName = "#CTL_InversionAction_MenuText",
        popupText = "#CTL_InversionAction_MenuText"
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Optical/Sen2Coral Processing/Processing modules",
                position = 250
        ),

})
@NbBundle.Messages({
        "CTL_InversionAction_MenuText=Shallow Water Analytical Model",
        "CTL_InversionAction_ShortDescription=Apply model inversion using python operator"
})
public class InversionAction extends AbstractSnapAction {

    private ModelessDialog dialog;
    public static final String OPERATOR_NAME = "py_sambuca_snap_op";
    public static final String DIALOG_TITLE = "Shallow Water Analytical Model";
    public static final String TARGET_PRODUCT_NAME_SUFFIX = "_inversion";

    public static InversionAction create(Map<String, Object> properties) {
        InversionAction action = new InversionAction();
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = createOperatorDialog();
        }
        dialog.show();
    }




    protected ModelessDialog createOperatorDialog() {
        InversionProductDialog productDialog = new InversionProductDialog(OPERATOR_NAME, getAppContext(),
                                                                          DIALOG_TITLE, getHelpId());

        productDialog.setTargetProductNameSuffix(TARGET_PRODUCT_NAME_SUFFIX);

        return productDialog;
    }
}

