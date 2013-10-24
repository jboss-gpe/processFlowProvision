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

import org.drools.core.SessionConfiguration;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.command.runtime.process.CompleteWorkItemCommand;
import org.drools.core.command.runtime.process.GetProcessIdsCommand;
import org.drools.core.command.runtime.process.GetProcessInstanceCommand;
import org.drools.core.command.runtime.process.SetProcessInstanceVariablesCommand;
import org.drools.core.command.runtime.process.SignalEventCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.drools.persistence.SingleSessionCommandService;
import org.jbpm.runtime.manager.impl.AbstractRuntimeManager;
import org.jbpm.runtime.manager.impl.SimpleRuntimeEnvironment;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.RegisterableItemsFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.services.remote.cdi.RuntimeManagerManager;
import org.kie.services.remote.exception.DomainNotFoundBadRequestException;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/*  purpose
 *    - provide an EJB remoting interface to BPM process engine
 *    
 *  notes
 *    - shares the same deployment unit registration mechanism employed by the REST/JMS execution server
 */
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

    // returns process instance state after having been signaled
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
    
    // returns process instance state after workItem having been completed
    public int completeWorkItem(Long workItemId, Map<String, Object> pInstanceVariables, Long pInstanceId, String deploymentId) {
        Command<?> pCmd = new GetProcessInstanceCommand(pInstanceId);
        Command<?> sCmd = new CompleteWorkItemCommand(workItemId, pInstanceVariables);
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
    
    public void abortProcessInstance(Long pInstanceId, String deploymentId) {
        Command<?> aCmd = new GetProcessInstanceCommand(pInstanceId);
        try {
            RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, pInstanceId);
            KieSession kieSession = runtimeEngine.getKieSession();
            SingleSessionCommandService sscs = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession) kieSession).getCommandService();
            kieSession.execute(aCmd);
        }catch(Exception e){
            if( e instanceof RuntimeException ) { 
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    
    public List<String> getProcessIds(String deploymentId) throws Exception {
        Command<?> cmd = new GetProcessIdsCommand();
        try {
            RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, null);
            KieSession kieSession = runtimeEngine.getKieSession();
            return (List<String>) kieSession.execute(cmd);
        }catch(Exception x){
            if( x instanceof RuntimeException ) { 
                throw (RuntimeException) x;
            } else {
                throw new RuntimeException(x);
            }
        }
    }

    public void setProcessInstanceVariables(Long pInstanceId, Map<String, Object> pVariables, String deploymentId) {
        Command<?> cmd = new SetProcessInstanceVariablesCommand(pInstanceId, pVariables);
        try {
            RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, pInstanceId);
            KieSession kieSession = runtimeEngine.getKieSession();
            kieSession.execute(cmd);
        }catch(Exception x){
            if( x instanceof RuntimeException ) { 
                throw (RuntimeException) x;
            } else {
                throw new RuntimeException(x);
            }
        }
        
    }
    

    public String printWorkItemHandlers(String deploymentId) {
        AbstractRuntimeManager runtimeManager = (AbstractRuntimeManager)runtimeMgrMgr.getRuntimeManager(deploymentId);
        RuntimeEngine runtimeEngine = getRuntimeEngine(deploymentId, null);
        RegisterableItemsFactory factory = runtimeManager.getEnvironment().getRegisterableItemsFactory();
        Map<String, WorkItemHandler> workItemHandlers = factory.getWorkItemHandlers(runtimeEngine);
        
        StringBuilder sBuilder = new StringBuilder("[");
        int x = 0;
        for(Map.Entry<?, ?> entry : workItemHandlers.entrySet()){
            if(x > 0)
                sBuilder.append(",");
            sBuilder.append("{\""+ entry.getKey()+"\":\"");
            sBuilder.append(( (WorkItemHandler)entry.getValue()).getClass().getName());
            sBuilder.append("\"}");
            x++;
        }
        sBuilder.append("]");
        return sBuilder.toString();
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
}
