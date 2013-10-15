package org.jboss.processFlow.knowledgeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.command.runtime.process.GetProcessInstanceCommand;
import org.drools.core.command.runtime.process.SignalEventCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.drools.persistence.SingleSessionCommandService;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.services.remote.cdi.RuntimeManagerManager;
import org.kie.services.remote.exception.DomainNotFoundBadRequestException;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Remote(IKnowledgeSession.class)
@Local(IBaseKnowledgeSession.class)
@Singleton(name="prodKSessionProxy")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KnowledgeSessionService implements IKnowledgeSession {
    
    private static Logger log = LoggerFactory.getLogger("KnowledgeSessionService");
    
    @Inject
    private RuntimeManagerManager runtimeMgrMgr;

    @Inject
    private TaskService taskService;

    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> params, String deploymentId ) {
        Command<?> cmd = new StartProcessCommand(processId, params);
        Map<String, Object> returnMap = new HashMap<String, Object>();
        try {
            RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, null);
            KieSession kieSession = runtimeEngine.getKieSession();
            SingleSessionCommandService sscs = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession) kieSession).getCommandService();
            ProcessInstance pInstance = (ProcessInstance)kieSession.execute(cmd);
            
            // now always return back to client the latest (possibly modified) pInstance variables
            // thank you  Jano Kasarda
            Map<String, Object> variables = ((WorkflowProcessInstanceImpl) pInstance).getVariables();
            for (String key : variables.keySet()) {
                returnMap.put(key, variables.get(key));
            }
            returnMap.put(IKnowledgeSession.PROCESS_INSTANCE_ID, pInstance.getId());
            returnMap.put(IKnowledgeSession.PROCESS_INSTANCE_STATE, pInstance.getState());
            
            return returnMap;
        }catch (Exception e) {
            if( e instanceof RuntimeException ) { 
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public int signalEvent(String signalType, Object signalPayload, Long pInstanceId, String deploymentId) {
        Command<?> sCmd = new SignalEventCommand(pInstanceId, signalType, signalPayload);
        Command<?> pCmd = new GetProcessInstanceCommand(pInstanceId);
        try {
            RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, pInstanceId);
            KieSession kieSession = runtimeEngine.getKieSession();
            SingleSessionCommandService sscs = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession) kieSession).getCommandService();
            
            ProcessInstance pInstance = (ProcessInstance)kieSession.execute(pCmd);
            if(pInstance == null){
                log.warn("signalEvent() not able to locate pInstance with id = "+pInstanceId+" : for deploymentId = "+deploymentId);
                return ProcessInstance.STATE_COMPLETED;
            }else {
                kieSession.execute(sCmd);
                return pInstance.getState();
            }
            
        }catch(Exception e){
            if( e instanceof RuntimeException ) { 
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    
    private RuntimeEngine getRuntimeEngine(String deploymentId, Long processInstanceId) {
        RuntimeManager runtimeManager = runtimeMgrMgr.getRuntimeManager(deploymentId);
        Context<?> runtimeContext;
        if (processInstanceId != null) {
            runtimeContext = new ProcessInstanceIdContext(processInstanceId);
        } else {
            runtimeContext = EmptyContext.get();
        }
        if (runtimeManager == null) {
            throw new DomainNotFoundBadRequestException("No runtime manager could be found for deployment '" + deploymentId + "'.");
        }
        return runtimeManager.getRuntimeEngine(runtimeContext);
    }

    public void completeWorkItem(Long workItemId, Map<String, Object> pInstanceVariables, Long processInstanceId, String deploymentId) {
        // TODO Auto-generated method stub
    }

    public void abortProcessInstance(Long processInstanceId, String deploymentId) {
        // TODO Auto-generated method stub
        
    }

    public List<String> retrieveProcesses(String deploymentId) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
