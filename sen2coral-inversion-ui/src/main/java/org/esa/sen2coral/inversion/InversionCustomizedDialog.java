package org.esa.sen2coral.inversion;

import com.bc.ceres.binding.Property;
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
    private static final float TIME_PER_PIXEL = 0.036f; //in seconds
    private static final float MINIMUM_BANDS = 4; //in seconds
    private static final float MAX_DIFF_WAVELENGHTS = 20; //in seconds

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
        super(appContext, InversionAction.DIALOG_TITLE, ID_APPLY_CLOSE, "ModelInversionSWAMAlgorithm"); //TODO check title and helpID
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

        File valueSiop = (File) this.getBindingContext().getBinding("xmlpath_siop").getPropertyValue();
        File valueParam = (File) this.getBindingContext().getBinding("xmlpath_parameters").getPropertyValue();
        File valueSensor = (File) this.getBindingContext().getBinding("xmlpath_sensor").getPropertyValue();

        //check at least MINIMUM_BANDS
        String[] selectedBands = new String[sensorBandsTable.getRowCount()];
        int validBandCount = 0;
        for(int i = 0 ; i < sensorBandsTable.getRowCount() ; i++) {
            selectedBands[i] = (String) sensorBandsTable.getModel().getValueAt(i,2);
            if(!selectedBands[i].equals("NULL") && !selectedBands[i].equals("null")) {
                validBandCount++;
            }
        }
        if(validBandCount < MINIMUM_BANDS) {
            this.showErrorDialog("Not enough valid spectral bands selected");
            return false;
        }

        //Check selected bands have the same rasterSize
        int width = 0, height = 0;
        for(int i = 0 ; i < sensorBandsTable.getRowCount() ; i++) {
            if(selectedBands[i].equals("NULL") || selectedBands[i].equals("null")) {
                continue;
            }
            int currentWidth = currentProduct.getBand(selectedBands[i]).getRasterWidth();
            int currentHeight = currentProduct.getBand(selectedBands[i]).getRasterHeight();
            if(width == 0 && height == 0) {
                width = currentWidth;
                height = currentHeight;
            } else if(width != currentWidth || height != currentHeight) {
                this.showErrorDialog("Selected bands have different raster sizes. " +
                                             "Select other bands or resample the product before applying this operator.");
                return false;
            }
        }

        //Check wavelengths selected, only if bands have wavelength
        for(int i = 0 ; i < sensorBandsTable.getRowCount() ; i++) {
            if(selectedBands[i].equals("NULL") || selectedBands[i].equals("null")) {
                continue;
            }
            float selectedBandWavelength = currentProduct.getBand(selectedBands[i]).getSpectralWavelength();
            if (selectedBandWavelength <= 0.0f) {
                continue;
            }

            float centralWavelength = Float.parseFloat((String) sensorBandsTable.getModel().getValueAt(i,1));


            if(Math.abs(centralWavelength-selectedBandWavelength) > MAX_DIFF_WAVELENGHTS) {
                String message = String.format("There is a difference of %.1f nm in the band with ID %d. Do you want to continue?",
                                               Math.abs(centralWavelength-selectedBandWavelength), i+1);
                final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                 getTitle(), JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }

        //Check if there are repeated bands
        for(int i = 0 ; i < sensorBandsTable.getRowCount() ; i++) {
            if(selectedBands[i].equals("NULL") || selectedBands[i].equals("null")) {
                continue;
            }
            for(int j = 0; j < sensorBandsTable.getRowCount() ; j++) {
                if( i == j ) {
                    continue;
                }
                if (selectedBands[i].equals(selectedBands[j])) {
                    String message = "There is at least one band selected twice. Do you want to continue?";
                    final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                     getTitle(), JOptionPane.YES_NO_OPTION);
                    if (answer != JOptionPane.YES_OPTION) {
                        return false;
                    }
                    break;
                }
            }
        }


        //Check parameters
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



        int numberOfPixelsToCompute = width * height;
        float estimatedTime = TIME_PER_PIXEL * numberOfPixelsToCompute;

        if (estimatedTime >= TIME_THRESHOLD_WARNING * 60) {
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
        //create band names
        final PropertySet propertySet = parameterSupport.getPropertySet();
        String[] selectedBands = new String[sensorBandsTable.getRowCount()];
        for(int i = 0 ; i < sensorBandsTable.getRowCount() ; i++) {
            selectedBands[i] = (String) sensorBandsTable.getModel().getValueAt(i,2);
        }
        try {
            propertySet.getProperty("band_names").setValue(selectedBands);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
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
            if(sensorBandsTable == null || productChangedHandler == null) {
                return;
            }
            Property property = bindingContext.getPropertySet().getProperty(UIUtils.PROPERTY_SOURCE_PRODUCT);
            if (property != null) {
                property.setValue(productChangedHandler.currentProduct);
            }
            this.currentProduct = productChangedHandler.currentProduct;
            setUpBandsColumn(sensorBandsTable,sensorBandsTable.getColumnModel().getColumn(2),productChangedHandler.currentProduct);
            updateTableModel(bindingContext.getPropertySet().getProperty("xmlpath_sensor").getValue());
        } catch (ValidationException e) {
            throw new IllegalStateException("Property '" + UIUtils.PROPERTY_SOURCE_PRODUCT + "' must be of type " + Product.class + ".", e);
        }
    }

    private JPanel createParametersPanel() {

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
        editorComponentSensor.setPreferredSize(new Dimension(300, 20));
        bindingContext.addPropertyChangeListener("xmlpath_sensor", (PropertyChangeEvent evt) ->{
            updateTableModel((File) evt.getNewValue());
        });
        panel.add(editorComponentSensor);

        //create table
        SensorBandsTableModel sensorBandsTableModel = new SensorBandsTableModel();
        sensorBandsTable = new JTable(sensorBandsTableModel);

        sensorBandsTable.setPreferredScrollableViewportSize(new Dimension(400, 120));
        sensorBandsTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(sensorBandsTable);
        setUpBandsColumn(sensorBandsTable, sensorBandsTable.getColumnModel().getColumn(2),product);
        panel.add(new JLabel("Select input bands:"));
        panel.add(scrollPane);

        panel.add(new JLabel("Path of the parameters xml file:"));
        FileEditor fileEditorParameters = new FileEditor();
        PropertyDescriptor propertyDescriptorParameters = propertySet.getDescriptor("xmlpath_parameters");
        JComponent editorComponentParameters = fileEditorParameters.createEditorComponent(propertyDescriptorParameters, bindingContext);
        editorComponentParameters.setPreferredSize(new Dimension(300, 20));
        panel.add(editorComponentParameters);

        panel.add(new JLabel("Path of the siop and substrates xml file:"));
        FileEditor fileEditorSiop = new FileEditor();
        PropertyDescriptor propertyDescriptorSiop = propertySet.getDescriptor("xmlpath_siop");
        JComponent editorComponentSiop = fileEditorSiop.createEditorComponent(propertyDescriptorSiop, bindingContext);
        editorComponentSiop.setPreferredSize(new Dimension(300, 20));
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

//        panel.add(new JLabel("The shorter wavelength:"));
//        NumericEditor numericEditorShorter = new NumericEditor();
//        PropertyDescriptor propertyDescriptorShorter = propertySet.getDescriptor("min_wlen");
//        JComponent editorComponentShorter = numericEditorShorter.createEditorComponent(propertyDescriptorShorter, bindingContext);
//        panel.add(editorComponentShorter);
//
//        panel.add(new JLabel("The longer wavelength:"));
//        NumericEditor numericEditorLonger = new NumericEditor();
//        PropertyDescriptor propertyDescriptorLonger = propertySet.getDescriptor("max_wlen");
//        JComponent editorComponentLonger = numericEditorLonger.createEditorComponent(propertyDescriptorLonger, bindingContext);
//        panel.add(editorComponentLonger);

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

        return panel;
    }

    private void updateTableModel(File sensorXml) {
        if(sensorXml == null) {
            return;
        }
        SensorBandsTableModel sensorBandsTableModel = new SensorBandsTableModel();
        SensorXMLReader sensorReader = new SensorXMLReader(sensorXml);
        double[] centralWavelengths = sensorReader.getCentralWavelengths();


        String[][] dataString = new String [centralWavelengths.length][3];
        for (int i = 0 ; i < centralWavelengths.length ; i ++) {
            dataString[i][0] = String.valueOf(i+1);
            dataString[i][1] = String.valueOf(centralWavelengths[i]);
            dataString[i][2] = getSimilarBand(currentProduct, (float) centralWavelengths[i], 20.0f);
        }

        sensorBandsTableModel.setData(dataString);
        sensorBandsTable.setModel(sensorBandsTableModel);
        setUpBandsColumn(sensorBandsTable, sensorBandsTable.getColumnModel().getColumn(2),productChangedHandler.currentProduct);

    }

    private String getSimilarBand(Product product, float approxWavelength, float validRange) {

        String bandName = "NULL";
        if (product == null) {
            return bandName;
        }
        float distance = Float.MAX_VALUE;
        for(Band band : product.getBands()) {
            float spectralWavelength = band.getSpectralWavelength();
            if(spectralWavelength == 0f) continue;
            if(Math.abs(spectralWavelength-approxWavelength) <= validRange && Math.abs(spectralWavelength-approxWavelength) < distance) {
                bandName = band.getName();
                distance = Math.abs(spectralWavelength-approxWavelength);
            }
        }
        return bandName;
    }

    private void reactToSourceProductChange(Product product) {
        updateSourceProduct();
    }

    private class SensorBandsTableModel extends AbstractTableModel {
        private String[] columnNames = {"Sensor filter ID", "Central Wavelength",
                "Input Product Band"};
        private Object[][] data = null;


        public void setData(Object[][] data) {
            this.data = data.clone();
        }
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            if(data == null) {
                return 0;
            }
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if(data == null) {
                return null;
            }
            return data[row][col];
        }

        public boolean isCellEditable(int row, int col) {
            if (col < 2) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            if(data == null) {
                return;
            }
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    public void setUpBandsColumn(JTable table,
                                 TableColumn bandColumn, Product product) {
        //Set up the editor for the sport cells.
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("NULL");
        if(product != null && product.getNumBands() >0) {
            //If some bands have Wavelength, I only add the bands with wavelengths
            if (containsWavelengths(product)) {
                for (Band band : product.getBands()) {
                    if(band.getSpectralWavelength() > 0.0f) {
                        comboBox.addItem(band.getName());
                    }
                }
            } else {
                for (String bandName : product.getBandNames()) {
                    comboBox.addItem(bandName);
                }
            }
        }

        bandColumn.setCellEditor(new DefaultCellEditor(comboBox));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        bandColumn.setCellRenderer(renderer);
    }

    private static boolean containsWavelengths(Product product) {
        for( Band band : product.getBands()) {
            if(band.getSpectralWavelength() > 0.0f) {
                return true;
            }
        }
        return false;
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

}
