package org.jboss.processFlow.services.remote.cdi;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/*
 purpose:  entry-point into application
*/
@Singleton
@Startup
public class RESTApplicationStartup {

    @Inject
    IDeploymentMgmtBean dBean;
    
    @PostConstruct
    public void start() throws Exception {
        dBean.start();
    }
    
    @PreDestroy
    public void stop() {
       dBean.stop();
    }
    
}
