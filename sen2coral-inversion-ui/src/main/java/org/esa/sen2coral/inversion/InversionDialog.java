package org.esa.sen2coral.inversion;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.ui.AppContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by obarrile on 13/01/2017.
 */
public class InversionDialog extends SingleTargetProductDialog {
    public static final String HELP_ID = "collocation";

    private final OperatorParameterSupport parameterSupport;
    private final InversionForm form;

    public InversionDialog(AppContext appContext) {
        super(appContext, "Inversion", ID_APPLY_CLOSE, HELP_ID);
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(InversionWrapperOp.Spi.class.getName()/*"py_modelInversion_op"*/);

        parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     HELP_ID);

        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());

        form = new InversionForm(parameterSupport.getPropertySet(), getTargetProductSelector(), appContext);

    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", form.getMasterProduct());
        final Map<String, Object> test =  parameterSupport.getParameterMap();
        parameterSupport.getParameterMap().put("lowerName", "band_4");
        parameterSupport.getParameterMap().put("upperName", "band_8");

        return GPF.createProduct(OperatorSpi.getOperatorAlias(InversionWrapperOp.class)/*"py_modelInversion_op"*/, parameterSupport.getParameterMap(),
                                 productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }
}
