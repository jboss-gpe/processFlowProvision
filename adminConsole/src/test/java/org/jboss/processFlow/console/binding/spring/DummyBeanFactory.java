package org.jboss.processFlow.console.binding.spring;

import java.util.ArrayList;
import java.util.List;

import org.jboss.processFlow.console.binding.BeanDefinition;
import org.jboss.processFlow.console.binding.BeanDefinitionFactory;
import org.jboss.processFlow.console.binding.PropertyDefinition;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public class DummyBeanFactory implements BeanDefinitionFactory {

    @Override
    public boolean containsBean(String name) {
        if ("UA0001".equals(name) || "ERR001".equals(name))
            return true;
        return false;
    }

    @Override
    public BeanDefinition getBeanDefinition(String name) {
        if ("UA0001".equals(name)) {
            BeanDefinition bd = new BeanDefinition();
            List<PropertyDefinition> pdList = new ArrayList<PropertyDefinition>();
            pdList.add(new PropertyDefinition("MAINCASEID", "long"));
            pdList.add(new PropertyDefinition("USERACTION", "string"));
            pdList.add(new PropertyDefinition("ATTACHMENTS", "list<org.jboss.processFlow.console.task.AttachmentInfo>"));
            bd.setPropertyDefinitions(pdList);
            return bd;
        }
        else if ("ERR001".equals(name)) {
            BeanDefinition bd = new BeanDefinition();
            List<PropertyDefinition> pdList = new ArrayList<PropertyDefinition>();
            pdList.add(new PropertyDefinition("fieldValues", "map"));
            pdList.add(new PropertyDefinition("fieldTypes", "map"));
            bd.setPropertyDefinitions(pdList);
            return bd;
        }
        return null;
    }

}
