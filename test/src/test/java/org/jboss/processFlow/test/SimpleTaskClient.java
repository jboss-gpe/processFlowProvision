package org.jboss.processFlow.test;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.naming.InitialContext;
import javax.naming.Context;

import org.apache.log4j.Logger;

import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskException;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;

public class SimpleTaskClient {

    private static final String JBOSS_EJB_CLIENT_PROPERTIES = "jboss-ejb-client.properties";
    private static Logger log = Logger.getLogger("SimpleTaskClient");
    private static ITaskService taskServiceProxy = null;
    private static IKnowledgeSessionService kSessionProxy = null;
    private static String absolutePathToBpmn = null;

    public static void main(String args[]) throws Exception {
        absolutePathToBpmn = System.getProperty("org.jboss.processFlow.test.absolutePathToBpmn");
        lookupProxies();
        addProcessToKnowledgeBase();
        executeProcessInstanceLifecycle();
    }

    
    public static void lookupProxies() throws Exception {
        Context jndiContext = null;
        InputStream iStream = null;
        try {
            iStream = SimpleTaskClient.class.getResourceAsStream("/"+JBOSS_EJB_CLIENT_PROPERTIES);
            if(iStream == null)
                throw new Exception("lookupProxies() required properties file is not in your classpath : "+JBOSS_EJB_CLIENT_PROPERTIES);
            
            Properties jndiProps = new Properties();
            jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            jndiContext = new InitialContext(jndiProps);

            taskServiceProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
            kSessionProxy = (IKnowledgeSessionService)jndiContext.lookup(IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI);
        } finally {
            if(jndiContext != null)
                jndiContext.close();
            if(iStream != null)
                iStream.close();
        }
    }

    public static void addProcessToKnowledgeBase() {
        File bpmnFile = new File(absolutePathToBpmn);
        if(!bpmnFile.exists())
            throw new RuntimeException("addProcessToKnowledgeBase() the following bpmn file does not exist: "+absolutePathToBpmn);
        kSessionProxy.addProcessToKnowledgeBase(bpmnFile);
        log.info("addProcessToKnowledgeBase() just added the following bpmn to the PFP knowledgebase : "+absolutePathToBpmn);
    }


    public static void executeProcessInstanceLifecycle() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("pInstanceVar1", new Integer(1500));
        parameters.put("pInstanceVar2", "Red Hat");

        // 1)  start new process instance
        Map<String, Object> returnMap = kSessionProxy.startProcessAndReturnId("simpleTask", parameters);
        long processInstanceId = (Long)returnMap.get(IKnowledgeSessionService.PROCESS_INSTANCE_ID);
        Map<String, Object> pVariables = kSessionProxy.getActiveProcessInstanceVariables(processInstanceId, null);
        log.info("executeProcessInstanceLifecycle() created pInstance w/ id = "+processInstanceId+ " : # of pInstance variables = "+pVariables.size());

        
        // 2)  query for any tasks with role 'creditController'
        List<String> groupList = new ArrayList<String>();
        groupList.add("creditController");
        List<TaskSummary> taskList = taskServiceProxy.getUnclaimedTasksAssignedAsPotentialOwner("jbride", groupList, "en-UK", 0, 10);
        if(groupList.size() == 0) {
            log.error("executeProcessInstanceLifecycle() # of tasks with group creditController == 0");
            return;
        }


        // 3)  iterate through list of tasks and claim the first task still available (ie:  not claimed by another user in 'creditController' group )
        TaskSummary claimedTask = null;
        for(TaskSummary tObj : taskList) {
            try {
                TaskSummary tTestObj = taskServiceProxy.getTask(tObj.getId());
                log.info("***** test obj = "+tTestObj);

                Map<String, Object> inboundContent = taskServiceProxy.getTaskContent(tObj.getId(), true);
                log.info("***** size of inbound task content  = "+inboundContent.size());

                taskServiceProxy.claimTask(tObj.getId(), "jbride", "jbride", groupList);
                log.info("executeProcessInstanceLifecycle() : just claimed task = "+tObj.getId());
                claimedTask = tObj;
                break;
            }catch(org.jbpm.task.service.PermissionDeniedException x){
                log.error("executeProcessInstanceLifecycle() claimTask.  PermissionDeniedException : taskId = "+tObj.getId());
            }
        }

        if(claimedTask == null) {
            log.error("executeProcessInstanceLifecycle() : no tasks claimed nor completed");
            return;
        }

        // 4)  complete the task previously claimed
        Map<String, Object> completedTaskHash = null;
        completedTaskHash = new HashMap();
        completedTaskHash.put("taskVar1", new Integer(3000));
        completedTaskHash.put("taskVar2", "JBoss");
        taskServiceProxy.completeTask(claimedTask.getId(), completedTaskHash, "jbride");
    }
}
