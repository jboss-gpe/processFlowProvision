package org.jboss.processFlow.console.binding;

import java.util.Map;

/**
 * The interfaces to binding the key/value based properties to the task result mapping.
 * <p>
 * The typical scenario is the result data of the human task from jbpm-console are generally key/value based in string
 * type, but the process instance might required for type other than string, e.g. long, integer, or event complex object
 * like list/custom objects, etc.
 * </p>
 * 
 * @author tanxu
 * @date Jan 27, 2012
 * @since
 */
public interface IDataBinder {

    /**
     * Set the task name, which is used to query the corresponding {@link BeanDefinition} for {@link BindingContext}
     * 
     * @param taskName
     */
    void setTaskName(String taskName);

    /**
     * Set the bindingContext from which to query the {@link BeanDefinition}.<br/>
     * Generally the <code>bindingContext</code> is shared by several/all dataBinder
     * 
     * @param bindingContext
     */
    void setBindingContext(BindingContext bindingContext);

    /**
     * do the binding
     * 
     * @param sourceContext the source context map that tells bind what value to which property. The key in the source
     *            context map is generally the expression of the property. The expression syntax follow the EL of the
     *            specified data binder adapter, for example, MVEL, or SeEL.
     *            <p>
     *            One exception is if you want to binding to a map, you need to do two things:
     *            <ul>
     *            <li>specify the <code>value-type</code> of the property as <code>map</code>, for example:
     *            <code>&lt;entry key=&quot;FIELDVALUES&quot; value-type=&quot;map&quot;/&gt;</code></li>
     *            <li>pass the properties (generally from ftl) in form of
     *            "property[$idx].key=keyname, property[$idx].value=val", for example:
     *            <code>FIELDVALUES[0].key=testkey1, FIELDVALUES[0].value=testvalue1</code>, this will put one entry
     *            <code>(testkey1, testvalue1)</code> to the target map</li>
     *            </ul>
     *            </p>
     * @throws Exception
     */
    void bind(Map<String, Object> sourceContext) throws Exception;

}
