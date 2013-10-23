package org.jboss.processFlow.services.remote.cdi;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.apache.commons.lang.StringUtils;
import org.jboss.processFlow.services.remote.cdi.IPfpDeploymentUnit.ProcessEnginePersistenceType;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.api.DeploymentUnit.RuntimeStrategy;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentService;
import org.jbpm.runtime.manager.api.WorkItemHandlerProducer;
import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceDotFileImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  provides beans that are injected into various other jbpm services at start-up to include :
    1)  EntityManagerFactory
    2)  VFS based IOService
    3)  UserGroupCallback
    4)  List<DeploymentUnit>
        - a configurable JSON data file is parsed. JSON consists of one or more 'deployments' that the jbpm engine should use from which to build a kieBase
        - these deployments can be of type:  simpleFile, git of kjar
 */

@ApplicationScoped
public class RESTApplicationScopedProducer {
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");

    private IOService vfsIOService;
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable  // specified in this runtime artifact's META-INF/beans.xml
    private UserGroupCallback userGroupCallback;
    
    @Inject
    @Vfs  // org.jbpm.kie.services.impl.VFSDeploymentService
    private DeploymentService vfsService;
    
    
    /* 
     * required for ioStrategy field in:  
       1)  org.jbpm.shared.services.impl.VFSFileServiceImpl    
       2)  org.jboss.processFlow.services.remote.cdi.RESTApplicationStartup
    */
    @Produces
    @Named("ioStrategy")
    public IOService getIOService() {
        try{
            if(vfsIOService == null) {

                // TO-DO:  investigate the details between all of these ??
                vfsIOService = new IOServiceDotFileImpl();
                //vfsIOService = new IOServiceIndexedImpl(indexEngine, DublinCoreView.class, VersionAttributeView.class, OtherMetaView.class)
                //vfsIOService = vfsIOServiceClusterImpl(service, clusterServiceFactory);
                //vfsIOService = new IOServiceNio2WrapperImpl();
            }
            return vfsIOService;
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }
    
    @Produces
    public UserGroupCallback produceSelectedUserGroupCallback() {
        return userGroupCallback;
    }
    
    @Produces
    public WorkItemHandlerProducer setWorkItemHandlerProducer() {
        return new VfsMVELWorkItemHandlerProducer((VFSDeploymentService)vfsService, getIOService());
    }
   
    /* injects into:  
        1) 3) org.jbpm.shared.services.impl.JbpmServicesPersistenceManagerImpl
        2) org.jbpm.kie.service.impl.AbstractDeploymentService 
        3) org.jboss.processFlow.services.remote.cdi.RESTRequestScopedProducer
        
    */ 
    @Produces
    public EntityManagerFactory getEntityManagerFactory() {
        return jbpmCoreEMF;
    }
    
}
