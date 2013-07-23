package org.kie.services.remote.cdi;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jbpm.kie.services.api.DeployedUnit;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.RuntimeDataService;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.kie.services.impl.model.ProcessDesc;

import org.kie.api.command.Command;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.services.client.serialization.jaxb.impl.JaxbExceptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kie.services.remote.exception.KieServiceBadRequestException;

import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;

import org.kie.services.remote.util.RESTUserGroupCallback;

/*
    - alternative implementation to:  
        - https://github.com/droolsjbpm/droolsjbpm-integration/blob/master/kie-remote/kie-services-remote/src/main/java/org/kie/services/remote/cdi/ProcessRequestBean.java
*/
@ApplicationScoped
public class ProcessRequestBean {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRequestBean.class);

    private RuntimeEnvironmentBuilder reBuilder = null;
    private RuntimeEnvironment rEnvironment = null;
    private RuntimeManager rManager = null;
    private Object rManagerLock = new Object();

    //@Inject
    private TaskService taskService;

    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;

    @PostConstruct
    public void start() {
        logger.info("start");
        try {
            createRuntimeEnvironmentBuilder();
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    @PreDestroy 
    public void stop() {
        logger.info("stop");
            if(rEnvironment != null)
                rEnvironment.close();
        
            if(rManager != null)
                rManager.close();
            
    }

    private void createRuntimeEnvironmentBuilder() {
        reBuilder = RuntimeEnvironmentBuilder.getDefault()
            .registerableItemsFactory(new org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory())
            .entityManagerFactory(this.jbpmCoreEMF)
            .userGroupCallback(new RESTUserGroupCallback());
    }
    
    private RuntimeManager getRuntimeManager() {
        if(rManager != null)
            return rManager;

        synchronized(rManagerLock) {
            if(rManager != null)
                return rManager;
            
            rEnvironment = reBuilder.get();
            rManager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(rEnvironment);
            return rManager;
        }
    }
    
    //JA Bride:  TO-DO
    public Object listAssets() {
    	return new Object();
    }
    
    public void addAssetToRuntimeEnvironment(String processString){
        reBuilder.addAsset(ResourceFactory.newByteArrayResource(processString.getBytes()), ResourceType.BPMN2);
    }

    public Object doKieSessionOperation(Command<?> cmd, String deploymentId, Long processInstanceId) {
        KieSession kieSession = null;
        if(processInstanceId == 0L)
            kieSession = getRuntimeManager().getRuntimeEngine(ProcessInstanceIdContext.get()).getKieSession();
        else
            kieSession = getRuntimeManager().getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();


        Object result = null;
        try { 
            result = kieSession.execute(cmd);
        } catch( Exception e ) { 
            JaxbExceptionResponse exceptResp = new JaxbExceptionResponse(e, cmd);
            logger.warn( "Unable to execute " + exceptResp.getCommandName() + " because of " + e.getClass().getSimpleName(), e);
            result = exceptResp;
        }
        return result;
    }
    
    public Object doTaskOperation(Command<?> cmd) {
        Object result = null;
        try { 
            result = ((InternalTaskService) taskService).execute(cmd);
        } catch( Exception e ) { 
            JaxbExceptionResponse exceptResp = new JaxbExceptionResponse(e, cmd);
            logger.warn( "Unable to execute " + exceptResp.getCommandName() + " because of " + e.getClass().getSimpleName(), e);
            result = exceptResp;
        }
        return result;
    }

}
