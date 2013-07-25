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
import org.jbpm.kie.services.api.bpmn2.BPMN2DataService;
import org.jbpm.kie.services.impl.DeployedUnitImpl;
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
import org.kie.commons.java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kie.services.remote.exception.KieServiceBadRequestException;

import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.shared.services.api.FileException;
import org.jbpm.shared.services.api.FileService;

import org.kie.services.remote.util.RESTUserGroupCallback;

/*
    - alternative implementation to:  
        - https://github.com/droolsjbpm/droolsjbpm-integration/blob/master/kie-remote/kie-services-remote/src/main/java/org/kie/services/remote/cdi/ProcessRequestBean.java
        
    - much of this functionality is also taken from VFSDeploymentService
*/
@ApplicationScoped
public class ProcessRequestBean {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRequestBean.class);

    private RuntimeEnvironmentBuilder reBuilder = null;
    private RuntimeEnvironment rEnvironment = null;
    private RuntimeManager rManager = null;
    private Object rManagerLock = new Object();

    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    //@Inject
    private TaskService taskService;
    
    //@Inject
    private FileService fs;
    
    //@Inject
    private BPMN2DataService bpmn2Service;


    @PostConstruct
    public void start() {
        logger.info("start");
        try {
            createRuntimeEnvironmentBuilder();
            
            //loadProcesses(vfsUnit, builder, deployedUnit);
            //loadRules(vfsUnit, builder, deployedUnit); 
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
    
    // from VFSDeploymentService
    protected void loadProcesses(VFSDeploymentUnit vfsUnit, RuntimeEnvironmentBuilder builder, DeployedUnitImpl deployedUnit) {
        Iterable<Path> loadProcessFiles = null;

        try {
            Path processFolder = fs.getPath(vfsUnit.getRepository() + vfsUnit.getRepositoryFolder());
            loadProcessFiles = fs.loadFilesByType(processFolder, ".+bpmn[2]?$");
        } catch (FileException ex) {
            logger.error("Error while loading process files", ex);
        }
        for (Path p : loadProcessFiles) {
            String processString = "";
            try {
                processString = new String(fs.loadFile(p));
                builder.addAsset(ResourceFactory.newByteArrayResource(processString.getBytes()), ResourceType.BPMN2);
                ProcessDesc process = bpmn2Service.findProcessId(processString, null);
                process.setOriginalPath(p.toUri().toString());
                process.setDeploymentId(vfsUnit.getIdentifier());
                deployedUnit.addAssetLocation(process.getId(), process);
                
            } catch (Exception ex) {
                logger.error("Error while reading process files", ex);
            }
        }
    }
    
    //from VFSDeploymentService
    protected void loadRules(VFSDeploymentUnit vfsUnit, RuntimeEnvironmentBuilder builder, DeployedUnitImpl deployedUnit) {
        Iterable<Path> loadRuleFiles = null;

        try {
            Path rulesFolder = fs.getPath(vfsUnit.getRepository() + vfsUnit.getRepositoryFolder());
            loadRuleFiles = fs.loadFilesByType(rulesFolder, ".+drl");
        } catch (FileException ex) {
            logger.error("Error while loading rule files", ex);
        }
        for (Path p : loadRuleFiles) {
            String ruleString = "";
            try {
                ruleString = new String(fs.loadFile(p));
                builder.addAsset(ResourceFactory.newByteArrayResource(ruleString.getBytes()), ResourceType.DRL);                
                
            } catch (Exception ex) {
                logger.error("Error while reading rule files", ex);
            }
        }
    }

}
