package org.esa.sen2coral.inversion;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;


public class InversionOpUI extends BaseOperatorUI {


    private final JTextField elevationBandName = new JTextField("");
    private final JTextField externalDEM = new JTextField("");


    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        elevationBandName.setText(String.valueOf(paramMap.get("elevationBandName")));
        externalDEM.setText(String.valueOf(paramMap.get("externalDEM")));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        paramMap.put("elevationBandName", elevationBandName.getText());
        paramMap.put("externalDEM", externalDEM.getText());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();


        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "External DEM:", externalDEM);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Elevation Band Name:", elevationBandName);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
