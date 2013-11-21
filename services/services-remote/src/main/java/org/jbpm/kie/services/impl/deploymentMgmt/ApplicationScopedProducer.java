package org.jbpm.kie.services.impl.deploymentMgmt;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.runtime.manager.api.WorkItemHandlerProducer;
import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  provides beans that are injected into various other jbpm services at start-up to include :
    1)  EntityManagerFactory
    3)  UserGroupCallback
    4)  List<DeploymentUnit>
        - a configurable JSON data file is parsed. JSON consists of one or more 'deployments' that the jbpm engine should use from which to build a kieBase
        - these deployments can be of type:  simpleFile, git of kjar
 */

@ApplicationScoped
public class ApplicationScopedProducer {
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");

    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;

    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory bamEMF;
    
    @Inject
    @Selectable  // specified in this runtime artifact's META-INF/beans.xml
    private UserGroupCallback userGroupCallback;
    
    @Produces
    public UserGroupCallback produceSelectedUserGroupCallback() {
        return userGroupCallback;
    }
    
    /* injects into:  
        1) 3) org.jbpm.shared.services.impl.JbpmServicesPersistenceManagerImpl
        2) org.jbpm.kie.service.impl.AbstractDeploymentService 
        3) org.jboss.processFlow.services.remote.cdi.RESTRequestScopedProducer
        
    */ 
    @Produces
    public EntityManagerFactory getCoreEntityManagerFactory() {
        return jbpmCoreEMF;
    }
    
}
