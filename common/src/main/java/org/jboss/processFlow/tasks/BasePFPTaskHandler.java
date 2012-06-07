package org.jboss.processFlow.tasks;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;


public class BasePFPTaskHandler {

    public static final String KSESSION_ID = "ksessionId";
    private static Object lockObj = new Object();
    protected static boolean enableLog = false;
    protected static ITaskService taskProxy = null;
    protected static IKnowledgeSessionService kSessionProxy = null;

    protected int ksessionId;

    public void init(StatefulKnowledgeSession sessionObj) {
        ksessionId = sessionObj.getId();
        if(taskProxy == null){
        	synchronized(lockObj){
        		if(taskProxy != null)
        			return;
        		
        		Context jndiContext = null;
        		try {
        			String logString = System.getProperty("org.jboss.enableLog");
        			if(logString != null)
        				enableLog = Boolean.parseBoolean(logString);
                
        			jndiContext = new InitialContext();
        			taskProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
        			kSessionProxy = (IKnowledgeSessionService)jndiContext.lookup((IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI));
        		} catch(Exception x) {
        				throw new RuntimeException("static()", x);
        		}finally {
        			try {
        				if(jndiContext != null)
        					jndiContext.close();
        			}catch(Exception x){
        				x.printStackTrace();
        			}
        		}
        	}
        }
    }
    
    public void dispose() {
    }
}
