package org.jboss.processFlow.console.binding;

/**
 * PropertyDefinition contains the name of the property/key in the task result mapping, and the value-type of the
 * property
 * 
 * @author tanxu
 * @date Feb 16, 2012
 * @since
 */
public class PropertyDefinition {
    private String property;
    private String valueType;

    private static final String DEFAULT_TYPE = "string";

    /**
     * The default consturctor, which use {@link #DEFAULT_TYPE} as the default type-value
     */
    public PropertyDefinition() {
        valueType = DEFAULT_TYPE;
    }

    /**
     * @param property
     * @param valueType
     */
    public PropertyDefinition(String property, String valueType) {
        this.property = property;
        this.valueType = valueType;
    }

    /**
     * @return the property
     */
    public String getProperty() {
        return property;
    }

    /**
     * @param property the property to set
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * @return the valueType
     * @see #setValueType(String)
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * @param valueType the valueType to set<br/>
     *            <p>
     *            the allowed value types includes:
     *            <ul>
     *            <li>integer</li>
     *            <li>int</li>
     *            <li>string</li>
     *            <li>long</li>
     *            <li>string - used by default if not set</li>
     *            <li>list&lt;full qualified class name of the generic type&gt; - e.g. "
     *            <code>list&lt;java.lang.Integer&gt;</code>"</li>
     *            <li>map - generic type if not yet supported, use <code>&lt;string, string&gt;</code> by default</li>
     *            <li>or, full qualified class name of the complex object - <b>NOT YET SUPPORTED</b></li>
     *            </ul>
     *            </p>
     */
    public void setValueType(String valueType) {
        if (valueType == null || valueType.isEmpty())
            this.valueType = DEFAULT_TYPE;
        else
            this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "PropertyDefinition [property=" + property + ", valueType=" + valueType + "]";
    }

}
