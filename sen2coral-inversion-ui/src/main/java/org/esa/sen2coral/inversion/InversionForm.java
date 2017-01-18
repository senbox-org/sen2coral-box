package org.esa.sen2coral.inversion;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;

/**
 * Created by obarrile on 13/01/2017.
 */
public class InversionForm extends JPanel {
    private SourceProductSelector masterProductSelector;
    private SourceProductSelector slaveProductSelector;

    private JCheckBox renameMasterComponentsCheckBox;
    private JCheckBox renameSlaveComponentsCheckBox;
    private JTextField masterComponentPatternField;
    private JTextField slaveComponentPatternField;
    private TargetProductSelector targetProductSelector;

    public InversionForm(PropertySet propertySet, TargetProductSelector targetProductSelector, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        masterProductSelector = new SourceProductSelector(appContext, "Master (pixel values are conserved):");
        slaveProductSelector = new SourceProductSelector(appContext, "Slave (pixel values are resampled onto the master grid):");
        renameMasterComponentsCheckBox = new JCheckBox("Rename master components:");
        renameSlaveComponentsCheckBox = new JCheckBox("Rename slave components:");
        masterComponentPatternField = new JTextField();
        slaveComponentPatternField = new JTextField();


        slaveProductSelector.getProductNameComboBox().addActionListener(e -> {
            Product slaveProduct = slaveProductSelector.getSelectedProduct();
            boolean validPixelExpressionUsed = isValidPixelExpressionUsed(slaveProduct);
        });

        createComponents();
        bindComponents(propertySet);
    }

    public void prepareShow() {
        masterProductSelector.initProducts();
        if (masterProductSelector.getProductCount() > 0) {
            masterProductSelector.setSelectedIndex(0);
        }
        slaveProductSelector.initProducts();
        if (slaveProductSelector.getProductCount() > 1) {
            slaveProductSelector.setSelectedIndex(1);
        }
    }

    public void prepareHide() {
        masterProductSelector.releaseProducts();
        slaveProductSelector.releaseProducts();
    }

    Product getMasterProduct() {
        return masterProductSelector.getSelectedProduct();
    }

    Product getSlaveProduct() {
        return slaveProductSelector.getSelectedProduct();
    }

    private void createComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(createTargetProductPanel());
        add(createRenamingPanel());
        add(createResamplingPanel());
    }

    private void bindComponents(PropertySet propertySet) {
    }

    private JPanel createSourceProductPanel() {
        final JPanel masterPanel = new JPanel(new BorderLayout(3, 3));
        masterPanel.add(masterProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        masterProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        masterPanel.add(masterProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        masterPanel.add(masterProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final JPanel slavePanel = new JPanel(new BorderLayout(3, 3));
        slavePanel.add(slaveProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        slavePanel.add(slaveProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        slavePanel.add(slaveProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        panel.add(masterPanel);
        panel.add(slavePanel);

        return panel;
    }

    private JPanel createTargetProductPanel() {
        return targetProductSelector.createDefaultPanel();
    }

    private JPanel createRenamingPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Renaming of Source Product Components"));
        panel.add(renameMasterComponentsCheckBox);
        panel.add(masterComponentPatternField);
        panel.add(renameSlaveComponentsCheckBox);
        panel.add(slaveComponentPatternField);

        return panel;
    }

    private JPanel createResamplingPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Resampling"));
        panel.add(new JLabel("Method:"));
        panel.add(new JLabel());

        return panel;
    }


    static  boolean isValidPixelExpressionUsed(Product product) {
        if (product != null) {
            for (final Band band : product.getBands()) {
                final String expression = band.getValidPixelExpression();
                if (expression != null && !expression.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
