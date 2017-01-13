package org.esa.sen2coral.inversion;


import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

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
        "CTL_InversionAction_MenuText=Apply model inversion",
        "CTL_InversionAction_ShortDescription=Apply model inversion using python wrapper"
})
public class InversionAction extends AbstractSnapAction {

    @Override
    public void actionPerformed(ActionEvent event) {
        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final InversionDialog inversionDialog = new InversionDialog(appContext);
        inversionDialog.show();
        try {
            inversionDialog.createTargetProduct();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

