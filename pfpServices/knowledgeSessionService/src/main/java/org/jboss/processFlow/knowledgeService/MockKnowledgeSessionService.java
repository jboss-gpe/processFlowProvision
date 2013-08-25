package org.jboss.processFlow.knowledgeService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;

@Local(IBaseKnowledgeSession.class)
@Singleton(name="mockKSessionProxy")
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MockKnowledgeSessionService implements IBaseKnowledgeSession {
    
    private Logger log = Logger.getLogger(MockKnowledgeSessionService.class);

    @Override
    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> parameters) {
        log.info("startProcessAndReturnId() processId = "+processId+" : will return empty map");
        return new HashMap<String, Object>();
    }

    @Override
    public void completeWorkItem(Long workItemId, Map<String, Object> pInstanceVariables, Long pInstanceId, Integer ksessionId) {
        log.info("completeWorkItem() ksessionId = "+ksessionId+" : workItemId = "+workItemId);
    }

    @Override
    public int signalEvent(String type, Object event, Long processInstanceId, Integer ksessionId) {
        log.info("signalEvent() type = "+type+" : pInstanceId = "+processInstanceId+" : ksessionId = "+ksessionId);
        return 0;
    }

    @Override
    public int processJobExecutionContext(Serializable jobExecutionContext) {
        // TODO Auto-generated method stub
        return 0;
        
    }

    @Override
    public String getCurrentTimerJobsAsJson(String jobGroup) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int purgeCurrentTimerJobs(String jobGroup) {
        // TODO Auto-generated method stub
        return 0;
    }

}
