package org.jboss.processFlow.console.binding;

import java.util.List;

/**
 * BeanDefinition contains the runtime type info of value of the task result mapping.
 * 
 * @author tanxu
 * @date Feb 16, 2012
 * @since
 */
public class BeanDefinition {

    private List<PropertyDefinition> propertyDefinitions;

    public BeanDefinition() {
    }

    /**
     * @return the propertyDefinitions
     */
    public List<PropertyDefinition> getPropertyDefinitions() {
        return propertyDefinitions;
    }

    /**
     * @param propertyDefinitions the propertyDefinitions to set
     */
    public void setPropertyDefinitions(List<PropertyDefinition> propertyDefinitions) {
        this.propertyDefinitions = propertyDefinitions;
    }

    @Override
    public String toString() {
        return "BeanDefinition [propertyDefinitions=" + propertyDefinitions + "]";
    }

}
