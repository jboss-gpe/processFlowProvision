package org.jboss.processFlow.diagnostics;

import java.util.Iterator;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.Producer;

import org.apache.log4j.Logger;

public class CDIDiagnostics implements Extension {
    
    private static final String LOG_BEFORE_BEAN_DISCOVERY="org.jboss.processFlow.diagnostics.logBeforeBeanDiscovery";
    private static final String LOG_PROCESS_ANNOTATED_TYPE="org.jboss.processFlow.diagnostics.logProcessAnnotatedType";
    private static final String LOG_PROCESS_INJECTION_TARGET="org.jboss.processFlow.diagnostics.logProcessInjectionTarget";
    private static final String LOG_PROCESS_PRODUCER="org.jboss.processFlow.diagnostics.logProcessProducer";
    private static final String LOG_AFTER_BEAN_DISCOVERY="org.jboss.processFlow.diagnostics.logAfterBeanDiscovery";
    private static final String LOG_AFTER_DEPLOYMENT_VALIDATION="org.jboss.processFlow.diagnostics.logAfterDeploymentValidation";
    private static final String LOG_BEFORE_SHUTDOWN="org.jboss.processFlow.diagnostics.logBeforeShutdown";
    
    
    private static Logger log = Logger.getLogger("CDIDiagnostics");
    private boolean logBeforeBeanDiscovery = true;
    private boolean logProcessAnnotatedType = true;
    private boolean logProcessInjectionTarget = true;
    private boolean logProcessProducer = true;
    private boolean logAfterBeanDiscovery = true;
    private boolean logAfterDeploymentValidation = true;
    private boolean logBeforeShutdown = true;
   
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
        
        sBuilder.append("\nlogBeforeBeanDiscovery = "+logBeforeBeanDiscovery);
        sBuilder.append("\nlogProcessAnnotatedType = "+logProcessAnnotatedType);
        sBuilder.append("\nlogProcessInjectionTarget = "+logProcessInjectionTarget);
        sBuilder.append("\nlogProcessProducer = "+logProcessProducer);
        sBuilder.append("\nlogAfterBeanDiscovery = "+logAfterBeanDiscovery);
        sBuilder.append("\nlogAfterDeploymentValidation = "+logAfterDeploymentValidation);
        sBuilder.append("\nlogBeforeShutdown = "+logBeforeShutdown);
        log.info(sBuilder.toString());
    }
    
    public void logBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        log.info("logBeforeBeanDiscovery() bbd = "+bbd);
    }
    public void logProcessAnnotatedType(@Observes ProcessAnnotatedType pat) {
        log.info("logProcessAnnotatedType() class = "+pat.getAnnotatedType().toString());
    }
    public void logProcessInjectionTarget(@Observes ProcessInjectionTarget pit) {
        Iterator injectionPoints = pit.getInjectionTarget().getInjectionPoints().iterator();
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("logProcessInjectionTarget() pit = "+pit.getClass());
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
