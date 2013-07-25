package org.jboss.processFlow.diagnostics;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.log4j.Logger;

public class CDIDiagnostics implements Extension {
    
    private static final String LOG_BEFORE_BEAN_DISCOVERY="org.jboss.processFlow.diagnostics.logBeforeBeanDiscovery";
    private static final String LOG_PROCESS_ANNOTATED_TYPE="org.jboss.processFlow.diagnostics.logProcessAnnotatedType";
    private static final String LOG_AFTER_BEAN_DISCOVERY="org.jboss.processFlow.diagnostics.logAfterBeanDiscovery";
    private static final String LOG_PROCESS_INJECTION_TARGET="org.jboss.processFlow.diagnostics.logProcessInjectionTarget";
    private static final String LOG_PROCESS_PRODUCER="org.jboss.processFlow.diagnostics.logProcessProducer";
    private static final String LOG_AFTER_DEPLOYMENT_VALIDATION="org.jboss.processFlow.diagnostics.logAfterDeploymentValidation";
    private static final String LOG_BEFORE_SHUTDOWN="org.jboss.processFlow.diagnostics.logBeforeShutdown";
    private static final String LOG_PROCESS_BEAN="org.jboss.processFlow.diagnostics.logProcessBean";
    private static final String VETO_CLASSES="org.jboss.processFlow.cdi.comma.delimited.veto.classes";
    
    
    private static Logger log = Logger.getLogger("CDIDiagnostics");
    private boolean logBeforeBeanDiscovery = true;
    private boolean logProcessAnnotatedType = true;
    private boolean logProcessInjectionTarget = true;
    private boolean logProcessProducer = true;
    private boolean logAfterBeanDiscovery = true;
    private boolean logAfterDeploymentValidation = true;
    private boolean logBeforeShutdown = true;
    private boolean logProcessBean = true;
    private List<String> vetoClasses;
   
    /*
     *  the CDI container will pick up this extension via the Java ServiceLoader mechanism
     *  place a file named javax.enterprise.inject.spi.Extension (the same name as the interface) within the META-INF/services
     *  ensure this file contins the fully-qualified name of this Extension implementation
     *  The container will look up the name of the class at runtime and instantiate it via the default constructor  
     */ 
    public CDIDiagnostics(){
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("CDIDiagnostics() public constructor");
        logBeforeBeanDiscovery = Boolean.parseBoolean(System.getProperty(this.LOG_BEFORE_BEAN_DISCOVERY, "TRUE"));
        logProcessAnnotatedType = Boolean.parseBoolean(System.getProperty(this.LOG_PROCESS_ANNOTATED_TYPE, "TRUE"));
        logProcessInjectionTarget = Boolean.parseBoolean(System.getProperty(this.LOG_PROCESS_INJECTION_TARGET, "TRUE"));
        logProcessProducer = Boolean.parseBoolean(System.getProperty(this.LOG_PROCESS_PRODUCER, "TRUE"));
        logAfterBeanDiscovery = Boolean.parseBoolean(System.getProperty(this.LOG_AFTER_BEAN_DISCOVERY, "TRUE"));
        logAfterDeploymentValidation = Boolean.parseBoolean(System.getProperty(this.LOG_AFTER_DEPLOYMENT_VALIDATION, "TRUE"));
        logBeforeShutdown = Boolean.parseBoolean(System.getProperty(this.LOG_BEFORE_SHUTDOWN, "TRUE"));
        logProcessBean = Boolean.parseBoolean(System.getProperty(this.LOG_PROCESS_BEAN, "TRUE"));
        String vetoClassesString = System.getProperty(this.VETO_CLASSES);
        
        sBuilder.append("\nlogBeforeBeanDiscovery = "+logBeforeBeanDiscovery);
        sBuilder.append("\nlogProcessAnnotatedType = "+logProcessAnnotatedType);
        sBuilder.append("\nlogProcessInjectionTarget = "+logProcessInjectionTarget);
        sBuilder.append("\nlogProcessProducer = "+logProcessProducer);
        sBuilder.append("\nlogAfterBeanDiscovery = "+logAfterBeanDiscovery);
        sBuilder.append("\nlogAfterDeploymentValidation = "+logAfterDeploymentValidation);
        sBuilder.append("\nlogBeforeShutdown = "+logBeforeShutdown);
        sBuilder.append("\nlogProcessBean = "+logProcessBean);
        sBuilder.append("\nvetoClassesString = "+vetoClassesString);
        log.info(sBuilder.toString());
        
        if(vetoClassesString != null){
            vetoClasses = new ArrayList<String>();
            String[] vetoClassesArray = vetoClassesString.split("\\s");
            for(String vetoClass : vetoClassesArray){
                vetoClasses.add(vetoClass);
            }
        }
    }
    
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        if(this.logBeforeBeanDiscovery)
            log.info("beforeBeanDiscovery() bbd = "+bbd);
    }
    
    public void processAnnotatedType(@Observes ProcessAnnotatedType pat, BeanManager bm) {
        String name = pat.getAnnotatedType().getJavaClass().getName();
        if(this.logProcessAnnotatedType)
            log.info("processAnnotatedType() class = "+name);
        
        if(vetoClasses != null && vetoClasses.contains(name)){
            pat.veto();
            log.info("processAnnotatedType() just vetoed : "+ name);
        }
    }
    
    public void processInjectionTarget(@Observes ProcessInjectionTarget pit, BeanManager bm){
        if(this.logProcessInjectionTarget)
            log.info("processInjectionTarget() class = "+pit.getAnnotatedType().toString());
    }
    
    public void processBean(@Observes ProcessBean pBean, BeanManager bm) {
        String name = pBean.getBean().getBeanClass().getName();
        if(this.logProcessBean)
            log.info("processBean() class = "+name);
    
    }
    
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        if(this.logAfterBeanDiscovery){
            Set<Bean<?>> allBeans = bm.getBeans(Object.class, new AnnotationLiteral<Any>() {});
            StringBuilder sBuilder = new StringBuilder();
            for(Bean bObj : allBeans){
                sBuilder.append("\n\t"+bObj.getBeanClass().getName());
            }
            log.info("afterBeanDiscovery() registered beans = "+sBuilder.toString());
        }
    }
    
    public void logProcessInjectionTarget(@Observes ProcessInjectionTarget pit) {
        if(this.logProcessInjectionTarget){
            Iterator injectionPoints = pit.getInjectionTarget().getInjectionPoints().iterator();
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("processInjectionTarget() pit = "+pit.getClass());
            while(injectionPoints.hasNext()) {
                Object obj = injectionPoints.next();
                if(obj instanceof InjectionPoint){
                    sBuilder.append("\n\tinjectionPoint = "+((InjectionPoint)obj).toString());
                }else {
                    sBuilder.append("\n\t class = "+obj.getClass().toString());
                }
            }
        }
    }

}
