package org.jboss.processFlow.console.binding;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.TransformerUtils;

/**
 * The abstrace dataBinder that apply type conversion for primmitive types and data binding for the complex types
 * 
 * @author tanxu
 * @date Feb 16, 2012
 * @since
 */
public abstract class AbstractDataBinder implements IDataBinder {

    static HashMap<String, Class> primitiveTypes = new HashMap<String, Class>();

    protected String taskName;
    protected BindingContext bindingContext;

    static {
        primitiveTypes.put("integer", Integer.class);
        primitiveTypes.put("long", Long.class);
        primitiveTypes.put("string", String.class);
        primitiveTypes.put("int", Integer.class);
    }

    public AbstractDataBinder() {
    }

    @Override
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Override
    public void bind(Map<String, Object> sourceContext) throws Exception {
        BeanDefinition beanDefinition = bindingContext.getBeanDefinition(taskName);

        if (beanDefinition == null)
            return;

        List<PropertyDefinition> propDefList = beanDefinition.getPropertyDefinitions();
        for (PropertyDefinition propDef : propDefList) {
            if (isPrimitiveType(propDef.getValueType())) {
                // just do the type conversion for primitive type entryies
                Object newValue = doConvert(getType(propDef.getValueType()), sourceContext.get(propDef.getProperty()));
                // update the value
                sourceContext.put(propDef.getProperty(), newValue);
            }
            else {
                Map<String, Object> target = new HashMap<String, Object>();
                if (isList(propDef.getValueType())) {
                    Class type = getGenericTypeOfList(propDef.getValueType());
                    // construct a lazy list that will add non-existing elements when it indexed
                    // since normal data binding framework won't create the new element for the target list
                    List obj = ListUtils.lazyList(new ArrayList(), FactoryUtils.instantiateFactory(type));
                    target.put(propDef.getProperty(), obj);
                }
                else if (isMap(propDef.getValueType())) {
                    // construct a lazy map that will add non-existing elements when it indexed
                    // since normal data binding framework won't create the new entry for the target map
                    // XXX need pay attention to the key type and value type?
                    Map obj = MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer());
                    target.put(propDef.getProperty(), obj);
                }
                Object targetObj = renderTargetIfNeeded(target, sourceContext);
                Map<String, Object> context = renderContextIfNeeded(targetObj, sourceContext);

                Map<String, Object> newValues = doBind(targetObj, context);
                // update the values with new binded ones
                sourceContext.putAll(newValues);
            }
        }
    }

    /**
     * @param valueType
     * @return
     * @throws ClassNotFoundException
     */
    private Class getGenericTypeOfList(String valueType) throws ClassNotFoundException {
        int beginIdx = valueType.indexOf('<');
        if (beginIdx <= 0)
            return String.class;
        int endIdx = valueType.indexOf('>');
        if (endIdx <= 0)
            throw new IllegalArgumentException("invalid value type, generic type should be closed" + valueType);
        String className = valueType.substring(beginIdx + 1, endIdx);
        return Class.forName(className);
    }

    /**
     * @param valueType
     * @return
     */
    private boolean isMap(String valueType) {
        return valueType.toLowerCase().startsWith("map");
    }

    /**
     * @param valueType
     * @return
     */
    private boolean isList(String valueType) {
        return valueType.toLowerCase().startsWith("list");
    }

    /**
     * @param property
     * @return
     */
    private Class getType(String type) {
        return primitiveTypes.get(type.toLowerCase());
    }

    /**
     * @param type
     * @return
     */
    private boolean isPrimitiveType(String type) {
        return primitiveTypes.containsKey(type);
    }

    /**
     * @param property
     * @return <code>true</code> if it's navigatiable property for the EL
     */
    protected boolean isNavigatiableProperty(String property) {
        int dotIdx = property.indexOf('.'); // normal property path
        if (dotIdx >= 0)
            return true;

        int bracketIdx = property.indexOf('['); // indexed property path
        return bracketIdx >= 0;
    }

    protected static final Object[] NULL_ARG = new Object[] {};

    /**
     * Get the bean properties as <code>java.uti.Map</code>
     * 
     * @param bean
     * @return
     * @throws IntrospectionException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected Map getProperties(Object bean) throws IntrospectionException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Map<String, Object> properties = new HashMap<String, Object>();
        BeanInfo bi = Introspector.getBeanInfo(bean.getClass());
        PropertyDescriptor[] props = bi.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            Method getter = props[i].getReadMethod();
            if (getter == null)
                continue;

            String name = props[i].getName();
            Object result = getter.invoke(bean, NULL_ARG);

            properties.put(name, result);
        }
        return properties;
    }

    /**
     * Convert the <code>source</code> to the <code>targetType</code>. <br/>
     * It mainly used to convert string to java primitive type
     * 
     * @param targetType
     * @param source
     * @return
     */
    abstract protected Object doConvert(Class targetType, Object source);

    /**
     * Binding the values to complex object <code>target</code>.
     * 
     * @param target
     * @param sourceContext
     * @throws Exception
     */
    abstract protected Map<String, Object> doBind(Object target, Map<String, Object> sourceContext) throws Exception;

    /**
     * This is useful if you want to do pre-process on the <code>sourceContext</code>, for example, convert the
     * key/propertyName, etc.
     * 
     * @param target
     * @param sourceContext
     * @return
     */
    abstract protected Map<String, Object> renderContextIfNeeded(Object target, Map<String, Object> sourceContext);

    /**
     * The is useful if you want to do pre-process on the <code>target</code> object, for example, wrap a map as a
     * InternalMapBean for Spring since Spring data binding don't take a Map as a Bean
     * 
     * @param target
     * @param sourceContext
     * @return
     */
    abstract protected Object renderTargetIfNeeded(Object target, Map<String, Object> sourceContext);

}
