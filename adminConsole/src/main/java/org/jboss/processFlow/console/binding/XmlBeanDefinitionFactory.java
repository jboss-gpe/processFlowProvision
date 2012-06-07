package org.jboss.processFlow.console.binding;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The <code>BeanDefinitionFactory</code> that parse the xml configuration files
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public class XmlBeanDefinitionFactory implements BeanDefinitionFactory {

    private static final Logger logger = LoggerFactory.getLogger(XmlBeanDefinitionFactory.class);

    private static final String TAG_BEAN = "task";
    private static final String TAG_PROPERTY = "entry";
    private static final String ATTR_TYPE = "value-type";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_NAME = "name";

    private String[] configuraions;
    private Map<String, BeanDefinition> beanDefinitions;

    /**
     * @param configuraions
     */
    public XmlBeanDefinitionFactory(String[] configuraions) {
        this.configuraions = configuraions;

        try {
            loadBeanDefinitions();
        }
        catch (Exception e) {
            logger.error("load bean definition failed: " + e.getMessage(), e);
        }
    }

    protected void loadBeanDefinitions() throws SAXException, IOException, ParserConfigurationException {
        beanDefinitions = new HashMap<String, BeanDefinition>();
        for (String config : configuraions) {
            InputStream is = XmlBeanDefinitionFactory.class.getResourceAsStream("/" + config);
            if (is == null) {
                logger.info("bean configuration file not found on the classpath: " + config);
                continue;
            }

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            Element rootEle = doc.getDocumentElement();
            NodeList beanList = rootEle.getElementsByTagName(TAG_BEAN);
            for (int b = 0; b < beanList.getLength(); b++) {
                BeanDefinition def = new BeanDefinition();
                List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>();
                def.setPropertyDefinitions(propertyDefinitions);
                Element beanEle = (Element) beanList.item(b);
                NodeList propList = beanEle.getElementsByTagName(TAG_PROPERTY);
                for (int p = 0; p < propList.getLength(); p++) {
                    Element propEle = (Element) propList.item(p);
                    PropertyDefinition pd = new PropertyDefinition();
                    pd.setProperty(propEle.getAttribute(ATTR_KEY));
                    pd.setValueType(propEle.getAttribute(ATTR_TYPE));

                    propertyDefinitions.add(pd);
                }

                beanDefinitions.put(beanEle.getAttribute(ATTR_NAME), def);
            }
        }
    }

    @Override
    public boolean containsBean(String taskName) {
        return beanDefinitions.containsKey(taskName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitions.get(name);
    }

    public boolean isEmpty() {
        return beanDefinitions == null || beanDefinitions.isEmpty();
    }
}
