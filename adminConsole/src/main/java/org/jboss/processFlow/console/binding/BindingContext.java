package org.jboss.processFlow.console.binding;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BindingContext connects the source object with the target object.
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public class BindingContext {

    private static final Logger logger = LoggerFactory.getLogger(BindingContext.class);
    private static final String DEFAULT_CENTRAL_CONFIGURATION = "central-binding.xml";
    public static final String PROP_BEAN_FACTORYIES = "org.jboss.processFlow.dataBinding.beanFactoryies";

    private List<BeanDefinitionFactory> beanFactories;

    public BindingContext() {
        this(new String[] {
            DEFAULT_CENTRAL_CONFIGURATION
        });
    }

    /**
     * @param configuraions the xml configuration file names
     */
    public BindingContext(String[] configuraions) {
        String factoryStr = System.getProperty(PROP_BEAN_FACTORYIES);
        String[] factoryClassNames = new String[0];
        if (factoryStr != null) {
            factoryClassNames = factoryStr.split(" ");
        }
        beanFactories = new ArrayList<BeanDefinitionFactory>(factoryClassNames.length + 1);
        XmlBeanDefinitionFactory centralParser = new XmlBeanDefinitionFactory(configuraions);
        if (!centralParser.isEmpty())
            beanFactories.add(centralParser);
        for (String className : factoryClassNames) {
            try {
                BeanDefinitionFactory factory = (BeanDefinitionFactory) Class.forName(className).newInstance();
                beanFactories.add(factory);
            }
            catch (Exception e) {
                logger.error("Error instantiating beanFactory: " + e.getMessage(), e);
            }
        }
    }

    public BeanDefinition getBeanDefinition(String taskName) {
        if (!containsBean(taskName))
            return null;

        for (BeanDefinitionFactory factory : beanFactories) {
            if (factory.containsBean(taskName)) {
                BeanDefinition beanDef = factory.getBeanDefinition(taskName);
                if (beanDef != null)
                    return beanDef;
            }
        }
        return null;
    }

    /**
     * the query priority is:
     * <ul>
     * <li>pre-defined {@link BeanDefinitionFactory} that configured as System Property with key
     * {@link #PROP_BEAN_FACTORYIES}</li>
     * <li>the central xml bean definition configuration file {@link #DEFAULT_CENTRAL_CONFIGURATION} on the classpath</li>
     * <li>the task specific bean definition configuraion file <code>${taskName}-binding.xml</code></li>
     * </ul>
     * 
     * @param taskName
     * @return <code>true</code> if contians the {@link BeanDefinition} of this task
     */
    public boolean containsBean(String taskName) {
        for (BeanDefinitionFactory factory : beanFactories) {
            if (factory.containsBean(taskName)) {
                return true;
            }
        }

        // try to load the task specific configuration file
        XmlBeanDefinitionFactory parser = new XmlBeanDefinitionFactory(new String[] {
            taskName + "-binding.xml"
        });
        if (parser.containsBean(taskName)) {
            beanFactories.add(parser);
            return true;
        }

        return false;
    }

}
