package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.processFlow.console.binding.BeanDefinition;
import org.jboss.processFlow.console.binding.PropertyDefinition;
import org.jboss.processFlow.console.binding.XmlBeanDefinitionFactory;
import org.junit.Test;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 17, 2012
 * @since
 */
public class XmlBeanDefinitionFactoryTest {

    @Test
    public void testParse() throws Exception {
        XmlBeanDefinitionFactory parser = new XmlBeanDefinitionFactory(new String[] {
            "map-binding.xml"
        });

        assertTrue(parser.containsBean("UA0001"));
        BeanDefinition def = parser.getBeanDefinition("UA0001");
        System.out.println("BeanDefinition: " + def);
        assertNotNull(def);
        List<PropertyDefinition> propDefs = def.getPropertyDefinitions();
        assertEquals(25, propDefs.size());
    }
}
