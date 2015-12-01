package org.jboss.processFlow.tasks.handlers;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.tasks.ITaskService;


public class BasePFPTaskHandler {

    public static final String KSESSION_ID = "ksessionId";
    private static Object lockObj = new Object();
    protected static boolean enableLog = false;
    protected static ITaskService taskProxy = null;
    protected static IKnowledgeSession kSessionProxy = null;
    
    protected int ksessionId;
    protected StatefulKnowledgeSession ksession;

    public BasePFPTaskHandler() {
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
                    kSessionProxy = (IKnowledgeSession)jndiContext.lookup((IKnowledgeSession.KNOWLEDGE_SESSION_SERVICE_JNDI));
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

    public void init(StatefulKnowledgeSession sessionObj) {
    	ksession = sessionObj;
        ksessionId = sessionObj.getId();
    }
    
    public void dispose() {
    }
}
