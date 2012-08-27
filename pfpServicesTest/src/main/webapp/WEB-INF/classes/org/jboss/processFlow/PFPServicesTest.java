package org.jboss.processFlow;

import java.io.File;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.naming.InitialContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskException;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;

@Path("/")
public class PFPServicesTest {

    private static final String JBOSS_BIND_ADDRESS="jboss.bind.address";
    private static Logger log = LoggerFactory.getLogger("PFPServicesTest");
    private static ITaskService taskServiceProxy = null;
    private static IKnowledgeSessionService kSessionProxy = null;
    private static String absolutePathToBpmn = null;

    /* 
      - A servlet configuration object used by a servlet container to pass 'init-param' configs from web.xml to a servlet during initialization
      - there is only one ServletConfig per servlet
    */
    @Context ServletConfig servletConfig;

    /*
      - used to access 'context-param' elements configured in web.xml
    */
    @Context ServletContext servletContext;

    @Context SecurityContext securityContext;

    // NOTE:  this lifecycle annotation is ignored in jax-rs
    @PostConstruct
    public void start() {
        log.info("start() ");
    }

    public PFPServicesTest() {
        absolutePathToBpmn = System.getProperty("org.jboss.processFlow.test.absolutePathToBpmn");
        log.info("constructor() absolutePathToBpmn = "+absolutePathToBpmn);
    }

    /**
     * Useage :  curl $HOSTNAME:8230/pfpServicesTest/all
     */
    @GET
    @Path("/all")
    @Produces("text/plain")
    public Response all() throws Exception {
        lookup();
        addProcessToKnowledgeBase();
        executeProcessInstanceLifecycle();
        ResponseBuilder builder = Response.ok("Great Success on nodeId = "+System.getProperty(JBOSS_BIND_ADDRESS)+" !!!\n");
        return builder.build();
    }


    private void lookup() throws Exception {
        javax.naming.Context jndiContext = null;
        try {
            Properties jndiProps = new Properties();
            jndiProps.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            jndiContext = new InitialContext(jndiProps);
            taskServiceProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
            kSessionProxy = (IKnowledgeSessionService)jndiContext.lookup(IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI);
        }finally {
            if(jndiContext != null) {
                try {
                    jndiContext.close();
                } catch(Exception x) {x.printStackTrace();}
            }
        }
    }
    /**
     * Useage :  curl $HOSTNAME:8230/pfpServicesTest/ejb/proxies
     */
    @GET
    @Path("/ejb/proxies")
    @Produces("text/plain")
    public Response restLookup() {
        ResponseBuilder builder = null;
        try {
            lookup();
            String response = "lookupProxies() taskServiceProxy = "+taskServiceProxy+" : kSessionProxy = "+kSessionProxy;
            builder = Response.ok(response);
        } catch(Exception x) {
            x.printStackTrace();
            builder = Response.status(Status.INTERNAL_SERVER_ERROR);
        }
        return builder.build();
    }

    private void addProcessToKnowledgeBase() throws Exception {
        File bpmnFile = new File(absolutePathToBpmn);
        if(!bpmnFile.exists())
            throw new Exception("addProcessToKnowledgeBase() the following bpmn file does not exist: "+absolutePathToBpmn);
        kSessionProxy.addProcessToKnowledgeBase(bpmnFile);
    }

    /**
     * Useage :  curl $HOSTNAME:8230/pfpServicesTest/knowledgeBase/add
     */
    @GET
    @Path("/knowledgeBase/add")
    @Produces("text/plain")
    public Response restAddProcessToKnowledgeBase() {
        ResponseBuilder builder = null;
        try {
            String response = "addProcessToKnowledgeBase() just added the following bpmn to the PFP knowledgebase : "+absolutePathToBpmn;
            builder = Response.ok(response);
        } catch(Exception x) {
            x.printStackTrace();
            builder = Response.status(Status.INTERNAL_SERVER_ERROR);
        }
        return builder.build();
    }

    private void executeProcessInstanceLifecycle() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("pInstanceVar1", new Integer(1500));
        parameters.put("pInstanceVar2", "Red Hat");

        // 1)  start new process instance
        Map<String, Object> returnMap = kSessionProxy.startProcessAndReturnId("org.jboss.processFlow.simpleTask", parameters);
        long processInstanceId = (Long)returnMap.get(IKnowledgeSessionService.PROCESS_INSTANCE_ID);
        Map<String, Object> pVariables = kSessionProxy.getActiveProcessInstanceVariables(processInstanceId, null);
        log.info("executeProcessInstanceLifecycle() created pInstance w/ id = "+processInstanceId+ " : # of pInstance variables = "+pVariables.size());

        // 2)  query for any tasks with role 'creditController'
        List<String> groupList = new ArrayList<String>();
        groupList.add("creditController");
        List<TaskSummary> taskList = taskServiceProxy.getTasksAssignedAsPotentialOwner("jbride", groupList, "en-UK", 0, 10);
        if(groupList.size() == 0) {
            log.error("executeProcessInstanceLifecycle() # of tasks with group creditController == 0");
            return;
        }

        // 3)  iterate through list of tasks and claim the first task still available (ie:  not claimed by another user in 'creditController' group )
        TaskSummary claimedTask = null;
        for(TaskSummary tObj : taskList) {
            try {
                TaskSummary tTestObj = taskServiceProxy.getTask(tObj.getId());
                log.info("***** test obj with id : "+tObj.getId()+" = "+tTestObj);

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
        log.info("executeProcessInstanceLifecycle() : just completed task = "+claimedTask.getId());
    }

    /**
     * Useage :  curl $HOSTNAME:8230/pfpServicesTest/pInstanceLifecycle
     */
    @GET
    @Path("/pInstanceLifecycle")
    public void restExecuteProcessInstanceLifecycle() throws Exception {
        executeProcessInstanceLifecycle();
    }
    


    // NOTE:  this lifecycle annotation is ignored in jax-rs
    @PreDestroy
    public void stop() {
        log.info("stop()");
    }
}
