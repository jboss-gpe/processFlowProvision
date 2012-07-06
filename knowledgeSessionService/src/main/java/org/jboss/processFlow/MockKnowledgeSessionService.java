package org.jboss.processFlow;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSessionService;

@Local(IBaseKnowledgeSessionService.class)
@Singleton(name="mockKSessionProxy")
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MockKnowledgeSessionService implements IBaseKnowledgeSessionService {
    
    private Logger log = Logger.getLogger(MockKnowledgeSessionService.class);

    @Override
    public Map<String, Object> startProcessAndReturnId(String processId,
            Map<String, Object> parameters) throws Exception {
        log.info("startProcessAndReturnId() processId = "+processId+" : will return empty map");
        return new HashMap<String, Object>();
    }

    @Override
    public void completeWorkItem(Integer ksessionId, Long workItemId,
            Map<String, Object> pInstanceVariables) {
        log.info("completeWorkItem() ksessionId = "+ksessionId+" : workItemId = "+workItemId);
    }

    @Override
    public void disposeStatefulKnowledgeSessionAndExtras(Integer sessionId) {
        log.info("disposeStatefulKnowledgeSessionAndExtras() ksessionId = "+sessionId);
    }

}
