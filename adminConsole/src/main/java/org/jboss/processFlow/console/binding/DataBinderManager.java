package org.jboss.processFlow.console.binding;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manages the task associated {@link IDataBinder}, provides mechanism to register the {@link IDataBinder} which in
 * essence adapt to the 3rd party matrure databinding/EL vender.<br/>
 * 
 * Currently use {@link MvelDataBinder} by default, by you can switch to other adapter via setting the
 * {@link #PROPERTY_DATA_BINDER} in the System Properties
 * 
 * @author tanxu
 * @date Feb 6, 2012
 * @since
 */
public class DataBinderManager {

    private static final Logger logger = LoggerFactory.getLogger(DataBinderManager.class);
    public static final String PROPERTY_DATA_BINDER = "org.jboss.processFlow.dataBinder";

    private Map<String, IDataBinder> binders;
    private BindingContext bindingContext;
    private Class binderClass;

    public DataBinderManager() {
        binders = new HashMap<String, IDataBinder>();
        bindingContext = new BindingContext();

        binderClass = MvelDataBinder.class; // use MvelDataBinder by default
        String binderClassName = System.getProperty(PROPERTY_DATA_BINDER);
        logger.info("DataBinder: " + binderClassName);
        if (binderClassName != null && !binderClassName.isEmpty()) {
            try {
                binderClass = Class.forName(binderClassName);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException("data binder not found: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get the data binder associated with the task, the instance is reused once created successfully
     * 
     * @param taskName
     * @return
     */
    public IDataBinder getDataBinder(String taskName) {
        IDataBinder binder = binders.get(taskName);
        if (binder != null)
            return binder;

        if (bindingContext.containsBean(taskName)) {
            try {
                binder = (IDataBinder) binderClass.newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException("failed to new dataBinder instance: " + binderClass);
            }

            binder.setTaskName(taskName);
            binder.setBindingContext(bindingContext);

            binders.put(taskName, binder);
            return binder;
        }
        return null;
    }

}
