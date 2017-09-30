package org.esa.sen2coral.inversion;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;
import com.bc.ceres.swing.binding.internal.FileEditor;
import com.bc.ceres.swing.binding.internal.NumericEditor;
import com.bc.ceres.swing.binding.internal.SingleSelectionEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.internal.RasterDataNodeValues;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.*;

/**
 * Created by obarrile on 27/09/2017.
 */
public class InversionCustomizedDialog extends SingleTargetProductDialog {

    private static final float TIME_THRESHOLD_WARNING = 3; //in minutes
    private static final float TIME_PER_PIXEL = 0.1f; //in seconds
    private static final float MINIMUM_BANDS = 4; //in seconds

    private final String operatorName;
    private final OperatorDescriptor operatorDescriptor;
    private DefaultIOParametersPanel ioParametersPanel;
    private final OperatorParameterSupport parameterSupport;
    private final BindingContext bindingContext;

    private JTabbedPane form;
    private PropertyDescriptor[] rasterDataNodeTypeProperties;
    private String targetProductNameSuffix;
    private InversionCustomizedDialog.ProductChangedHandler productChangedHandler;

    private Product product;
    private Product currentProduct;


    JTable sensorBandsTable = null;


    public InversionCustomizedDialog(AppContext appContext, Product product, boolean modal) {
        super(appContext, "SWAM", ID_APPLY_CLOSE, "swamAction"); //TODO check title and helpID
        this.operatorName = InversionAction.OPERATOR_NAME;
        this.product = product;
        targetProductNameSuffix = InversionAction.TARGET_PRODUCT_NAME_SUFFIX;

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        operatorDescriptor = operatorSpi.getOperatorDescriptor();
        ioParametersPanel = new DefaultIOParametersPanel(getAppContext(), operatorDescriptor, getTargetProductSelector(), true);

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        final PropertySet propertySet = parameterSupport.getPropertySet();
        try {
            propertySet.getProperty("band_names").setValue(new String[]{"B2","B3"});
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        Map<String, Object> map = parameterSupport.getParameterMap();
        bindingContext = new BindingContext(propertySet);

        if (propertySet.getProperties().length > 0) {
            if (!sourceProductSelectorList.isEmpty()) {
                Property[] properties = propertySet.getProperties();
                java.util.List<PropertyDescriptor> rdnTypeProperties = new ArrayList<>(properties.length);
                for (Property property : properties) {
                    PropertyDescriptor parameterDescriptor = property.getDescriptor();
                    if (parameterDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                        rdnTypeProperties.add(parameterDescriptor);
                    }
                }
                rasterDataNodeTypeProperties = rdnTypeProperties.toArray(
                        new PropertyDescriptor[rdnTypeProperties.size()]);
            }
        }
        productChangedHandler = new InversionCustomizedDialog.ProductChangedHandler();
        if (!sourceProductSelectorList.isEmpty()) {
            sourceProductSelectorList.get(0).addSelectionChangeListener(productChangedHandler);
        }
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
        float valueMax = (float) this.getBindingContext().getBinding("max_wlen").getPropertyValue();
        float valueMin = (float) this.getBindingContext().getBinding("min_wlen").getPropertyValue();

        //TODO check at least MINIMUM_BANDS
        int validBandCount = 0;
        for(int i = 0; i<sourceProduct.getNumBands() ; i++) {
            Band band = sourceProduct.getBandAt(i);
            float spectralWavelength = band.getSpectralWavelength();
            if (spectralWavelength >= valueMin  && spectralWavelength <= valueMax) {
                validBandCount ++;
            }
        }
        if(validBandCount < MINIMUM_BANDS) {
            this.showErrorDialog("Not enough valid spectral bands in the source product." +
                                         "Try to change wavelength range or to add spectral information to the bands.");
            return false;
        }

        //Check parameters
        if(valueMin >= valueMax) {
            this.showErrorDialog("Max value must be higher than Min value.");
            return false;
        }
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



    @Override
    public int show() {
        ioParametersPanel.initSourceProductSelectors();
        if (form == null) {
            initForm();
            if (getJDialog().getJMenuBar() == null) {
                final OperatorMenu operatorMenu = createDefaultMenuBar();
                getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
            }
        }
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        productChangedHandler.releaseProduct();
        ioParametersPanel.releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = ioParametersPanel.createSourceProductsMap();
        return GPF.createProduct(operatorName, parameterSupport.getParameterMap(), sourceProducts);
    }

    protected DefaultIOParametersPanel getDefaultIOParametersPanel() {
        return ioParametersPanel;
    }

    public String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    private void initForm() {

        form = new JTabbedPane();
        form.add("I/O Parameters", ioParametersPanel);
        form.add("Resampling Parameters", new JScrollPane(createParametersPanel()));
        reactToSourceProductChange(ioParametersPanel.getSourceProductSelectorList().get(0).getSelectedProduct());

    }




    private OperatorMenu createDefaultMenuBar() {
        return new OperatorMenu(getJDialog(),
                                operatorDescriptor,
                                parameterSupport,
                                getAppContext(),
                                getHelpID());
    }

    private void updateSourceProduct() {
        try {
            Property property = bindingContext.getPropertySet().getProperty(UIUtils.PROPERTY_SOURCE_PRODUCT);
            if (property != null) {
                property.setValue(productChangedHandler.currentProduct);
            }
            setUpBandsColumn(sensorBandsTable,sensorBandsTable.getColumnModel().getColumn(1),productChangedHandler.currentProduct);
        } catch (ValidationException e) {
            throw new IllegalStateException("Property '" + UIUtils.PROPERTY_SOURCE_PRODUCT + "' must be of type " + Product.class + ".", e);
        }
    }

    private JPanel createParametersPanel() {
        //TODO
        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        final PropertySet propertySet = bindingContext.getPropertySet();


        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);
        final JPanel panel = new JPanel(tableLayout);

        panel.add(new JLabel("Path of the sensor xml file:"));
        FileEditor fileEditorSensor = new FileEditor();
        PropertyDescriptor propertyDescriptorSensor = propertySet.getDescriptor("xmlpath_sensor");
        JComponent editorComponentSensor = fileEditorSensor.createEditorComponent(propertyDescriptorSensor, bindingContext);
        bindingContext.addPropertyChangeListener("xmlpath_sensor", (PropertyChangeEvent evt) ->{
            updateTableModel();
        });
        panel.add(editorComponentSensor);

        //create table
        SensorBandsTableModel sensorBandsTableModel = new SensorBandsTableModel();
        Object[][] data = {
                {"1", "B1"},
                {"2", "B4"},

        };
        sensorBandsTableModel.setData(data);//TODO
        this.sensorBandsTable = new JTable(sensorBandsTableModel);
        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        //table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(sensorBandsTable);
        setUpBandsColumn(sensorBandsTable, sensorBandsTable.getColumnModel().getColumn(1),product);
        panel.add(new JLabel("Select input bands:"));
        panel.add(scrollPane);

        panel.add(new JLabel("Path of the parameters xml file:"));
        FileEditor fileEditorParameters = new FileEditor();
        PropertyDescriptor propertyDescriptorParameters = propertySet.getDescriptor("xmlpath_parameters");
        JComponent editorComponentParameters = fileEditorParameters.createEditorComponent(propertyDescriptorParameters, bindingContext);
        panel.add(editorComponentParameters);

        panel.add(new JLabel("Path of the siop and substrates xml file:"));
        FileEditor fileEditorSiop = new FileEditor();
        PropertyDescriptor propertyDescriptorSiop = propertySet.getDescriptor("xmlpath_siop");
        JComponent editorComponentSiop = fileEditorSiop.createEditorComponent(propertyDescriptorSiop, bindingContext);
        panel.add(editorComponentSiop);

        panel.add(new JLabel("Error:"));
        SingleSelectionEditor singleSelectionEditorError = new SingleSelectionEditor();
        PropertyDescriptor propertyDescriptorError = propertySet.getDescriptor("error_name");
        JComponent editorComponentError = singleSelectionEditorError.createEditorComponent(propertyDescriptorError, bindingContext);
        panel.add(editorComponentError);

        panel.add(new JLabel("Optimization method:"));
        SingleSelectionEditor singleSelectionEditorMethod = new SingleSelectionEditor();
        PropertyDescriptor propertyDescriptorMethod = propertySet.getDescriptor("opt_method");
        JComponent editorComponentMethod = singleSelectionEditorMethod.createEditorComponent(propertyDescriptorMethod, bindingContext);
        panel.add(editorComponentMethod);

        panel.add(new JLabel("The shorter wavelength:"));
        NumericEditor numericEditorShorter = new NumericEditor();
        PropertyDescriptor propertyDescriptorShorter = propertySet.getDescriptor("min_wlen");
        JComponent editorComponentShorter = numericEditorShorter.createEditorComponent(propertyDescriptorShorter, bindingContext);
        panel.add(editorComponentShorter);

        panel.add(new JLabel("The longer wavelength:"));
        NumericEditor numericEditorLonger = new NumericEditor();
        PropertyDescriptor propertyDescriptorLonger = propertySet.getDescriptor("max_wlen");
        JComponent editorComponentLonger = numericEditorLonger.createEditorComponent(propertyDescriptorLonger, bindingContext);
        panel.add(editorComponentLonger);

        panel.add(new JLabel("Is it above water rrs?"));
        CheckBoxEditor checkBoxEditorAbove = new CheckBoxEditor();
        PropertyDescriptor propertyDescriptorAbove = propertySet.getDescriptor("above_rrs_flag");
        JComponent editorComponentAbove = checkBoxEditorAbove.createEditorComponent(propertyDescriptorAbove, bindingContext);
        panel.add(editorComponentAbove);

        panel.add(new JLabel("Is it shallow?"));
        CheckBoxEditor checkBoxEditorShallow = new CheckBoxEditor();
        PropertyDescriptor propertyDescriptorShallow = propertySet.getDescriptor("shallow_flag");
        JComponent editorComponentShallow = checkBoxEditorShallow.createEditorComponent(propertyDescriptorShallow, bindingContext);
        panel.add(editorComponentShallow);

        panel.add(new JLabel("Do you want relaxed constraints?"));
        CheckBoxEditor checkBoxEditorRelaxed = new CheckBoxEditor();
        PropertyDescriptor propertyDescriptorRelaxed = propertySet.getDescriptor("relaxed_cons");
        JComponent editorComponentRelaxed = checkBoxEditorRelaxed.createEditorComponent(propertyDescriptorRelaxed, bindingContext);
        panel.add(editorComponentRelaxed);

       /* final TableLayout defineTargetResolutionPanelLayout = new TableLayout(2);
        defineTargetResolutionPanelLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        defineTargetResolutionPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        defineTargetResolutionPanelLayout.setColumnWeightX(1, 1.0);
        defineTargetResolutionPanelLayout.setTablePadding(4, 5);
        final JPanel defineTargetSizePanel = new JPanel(defineTargetResolutionPanelLayout);
        defineTargetSizePanel.setBorder(BorderFactory.createTitledBorder("Define size of resampled product"));
        final ButtonGroup targetSizeButtonGroup = new ButtonGroup();
        referenceBandButton = new JRadioButton("By reference band from source product:");
        referenceBandButton.setToolTipText(REFERENCE_BAND_TOOLTIP_TEXT);
        widthAndHeightButton = new JRadioButton("By target width and height:");
        widthAndHeightButton.setToolTipText(TARGET_WIDTH_AND_HEIGHT_TOOLTIP_TEXT);
        resolutionButton = new JRadioButton("By pixel resolution (in m):");
        resolutionButton.setToolTipText(TARGET_RESOLUTION_TOOLTIP_TEXT);
        targetSizeButtonGroup.add(referenceBandButton);
        targetSizeButtonGroup.add(widthAndHeightButton);
        targetSizeButtonGroup.add(resolutionButton);

        defineTargetSizePanel.add(referenceBandButton);
        referenceBandNameBoxPanel = new ResamplingDialog.ReferenceBandNameBoxPanel();
        defineTargetSizePanel.add(referenceBandNameBoxPanel);

        defineTargetSizePanel.add(widthAndHeightButton);
        targetWidthAndHeightPanel = new ResamplingDialog.TargetWidthAndHeightPanel();
        defineTargetSizePanel.add(targetWidthAndHeightPanel);

        defineTargetSizePanel.add(resolutionButton);
        targetResolutionPanel = new ResamplingDialog.TargetResolutionPanel();
        defineTargetSizePanel.add(targetResolutionPanel);

        referenceBandButton.addActionListener(e -> {
            if (referenceBandButton.isSelected()) {
                enablePanel(REFERENCE_BAND_NAME_PANEL_INDEX);
            }
        });
        widthAndHeightButton.addActionListener(e -> {
            if (widthAndHeightButton.isSelected()) {
                enablePanel(TARGET_WIDTH_AND_HEIGHT_PANEL_INDEX);
            }
        });
        resolutionButton.addActionListener(e -> {
            if (resolutionButton.isSelected()) {
                enablePanel(TARGET_RESOLUTION_PANEL_INDEX);
            }
        });

        referenceBandButton.setSelected(true);

        final JPanel upsamplingMethodPanel = createPropertyPanel(propertySet, "upsamplingMethod", registry);
        final JPanel downsamplingMethodPanel = createPropertyPanel(propertySet, "downsamplingMethod", registry);
        final JPanel flagDownsamplingMethodPanel = createPropertyPanel(propertySet, "flagDownsamplingMethod", registry);
        final JPanel resampleOnPyramidLevelsPanel = createPropertyPanel(propertySet, "resampleOnPyramidLevels", registry);
        final JPanel parametersPanel = new JPanel(tableLayout);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        parametersPanel.add(defineTargetSizePanel);
        parametersPanel.add(upsamplingMethodPanel);
        parametersPanel.add(downsamplingMethodPanel);
        parametersPanel.add(flagDownsamplingMethodPanel);
        parametersPanel.add(resampleOnPyramidLevelsPanel);
        parametersPanel.add(tableLayout.createVerticalSpacer());
        return parametersPanel;*/
        return panel;
    }

    private void updateTableModel() {
        SensorBandsTableModel sensorBandsTableModel = new SensorBandsTableModel();

        Object[][] data = {
                {"3", "B8"},
                {"4", "B8"},

        };
        sensorBandsTableModel.setData(data);
        sensorBandsTable.setModel(sensorBandsTableModel);
        setUpBandsColumn(sensorBandsTable, sensorBandsTable.getColumnModel().getColumn(1),productChangedHandler.currentProduct);

    }

    private void reactToSourceProductChange(Product product) {
        //TODO
    }

    private class SensorBandsTableModel extends AbstractTableModel {
        private String[] columnNames = {"Sensor filter ID",
                "Input Band"};
        private Object[][] data = null;


        public void setData(Object[][] data) {
            this.data = data.clone();
        }
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        //public Class getColumnClass(int c) {
        //    return getValueAt(0, c).getClass();
        //}


        public boolean isCellEditable(int row, int col) {
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    public void setUpBandsColumn(JTable table,
                                 TableColumn bandColumn, Product product) {
        //Set up the editor for the sport cells.
        JComboBox comboBox = new JComboBox();
        if(product == null || product.getNumBands() == 0) {
            return;
        }
        for(String bandName : product.getBandNames()) {
            comboBox.addItem(bandName);
        }

        bandColumn.setCellEditor(new DefaultCellEditor(comboBox));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        bandColumn.setCellRenderer(renderer);
    }



    private class ProductChangedHandler extends AbstractSelectionChangeListener implements ProductNodeListener {

        private Product currentProduct;


        public void releaseProduct() {
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(this);
                currentProduct = null;
                updateSourceProduct();
            }
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            if (selection != null) {
                final Product selectedProduct = (Product) selection.getSelectedValue();
                if (selectedProduct != currentProduct) {
                    if (currentProduct != null) {
                        currentProduct.removeProductNodeListener(this);
                    }
                    currentProduct = selectedProduct;
                    if (currentProduct != null) {
                        currentProduct.addProductNodeListener(this);
                    }
                    if(getTargetProductSelector() != null){
                        updateTargetProductName();
                    }
                    updateValueSets(currentProduct);
                    updateSourceProduct();
                }
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        private void updateTargetProductName() {
            String productName = "";
            if (currentProduct != null) {
                productName = currentProduct.getName();
            }
            final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
            targetProductSelectorModel.setProductName(productName + getTargetProductNameSuffix());
        }

        private void handleProductNodeEvent() {
            updateValueSets(currentProduct);
        }

        private void updateValueSets(Product product) {
            if (rasterDataNodeTypeProperties != null) {
                for (PropertyDescriptor propertyDescriptor : rasterDataNodeTypeProperties) {
                    updateValueSet(propertyDescriptor, product);
                }
            }
        }
    }

    private static void updateValueSet(PropertyDescriptor propertyDescriptor, Product product) {
        String[] values = new String[0];
        if (product != null) {
            Object object = propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME);
            if (object != null) {
                @SuppressWarnings("unchecked")
                Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) object;
                boolean includeEmptyValue = !propertyDescriptor.isNotNull() && !propertyDescriptor.isNotEmpty() &&
                        !propertyDescriptor.getType().isArray();
                values = RasterDataNodeValues.getNames(product, rasterDataNodeType, includeEmptyValue);
            }
        }
        propertyDescriptor.setValueSet(new ValueSet(values));
    }

    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel editorPanel = new JPanel(new BorderLayout(2, 2));
        editorPanel.add(textField, BorderLayout.CENTER);
        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            File currentFile = (File) binding.getPropertyValue();
            if (currentFile != null) {
                fileChooser.setSelectedFile(currentFile);
            } else {
                File selectedFile = (File) propertyDescriptor.getDefaultValue();
                fileChooser.setSelectedFile(selectedFile);
            }
            int i = fileChooser.showDialog(editorPanel, "Select");
            if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                binding.setPropertyValue(fileChooser.getSelectedFile());
            }
        });
        editorPanel.add(etcButton, BorderLayout.EAST);
        return editorPanel;
    }


}
