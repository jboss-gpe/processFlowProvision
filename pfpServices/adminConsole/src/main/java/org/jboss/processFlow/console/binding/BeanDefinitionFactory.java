package org.jboss.processFlow.console.binding;

/**
 * The factory to produce the {@link BeanDefinition}, it abstracts how the {@link BeanDefinition} stored, no matter in
 * form of a xml file or anything else.
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public interface BeanDefinitionFactory {

    /**
     * @param taskName
     * @return
     */
    boolean containsBean(String taskName);

    BeanDefinition getBeanDefinition(String name);
}
