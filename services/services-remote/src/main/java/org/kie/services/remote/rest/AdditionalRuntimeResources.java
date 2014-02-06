package org.kie.services.remote.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.drools.core.SessionConfiguration;
import org.drools.core.command.runtime.process.GetProcessIdsCommand;
import org.jbpm.kie.services.impl.deploymentMgmt.IDeploymentMgmtBean;
import org.jbpm.runtime.manager.impl.AbstractRuntimeManager;
import org.kie.services.remote.cdi.DeploymentInfoBean;
import org.kie.services.remote.rest.RestProcessRequestBean;
import org.kie.api.command.Command;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RegisterableItemsFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;

/**
    - what about a function to check status of registered workItemHandlers
    - jbpm-services/jbpm-kie-services/src/main/java/org/jbpm/kie/services/api/bpmn2/BPMN2DataService.java
        - has a function get getAllServiceTasks(...) .... which might be similar ?
        - it also has a bunch of other valueable functions that should be exposed via REST /JMS API
*/

@RequestScoped
@Path("/additional/runtime/{id: [a-zA-Z0-9-:\\.]+}")
public class AdditionalRuntimeResources {
   
    private static final String LIST_PROCESSES_EXCEPTION = "listProcessesException"; 
    private static final Logger logger = LoggerFactory.getLogger(AdditionalRuntimeResources.class);
    
    @Inject
    private RestProcessRequestBean processRequestBean;
    
    @Inject
    private DeploymentInfoBean runtimeMgrMgr;
    
    @Inject
    private IDeploymentMgmtBean dBean;

    @PathParam("id")
    private String deploymentId;

    @Context
    private HttpServletRequest request;
    
    
    /**
     * provides visibility of BPMN2 process(s) actually registered with KieBase
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/services-remote/rest/additional/runtime/org.acme.insurance:policyquote:1.0.0/processes
     */
    @GET
    @Produces({ "text/plain" })
    @Path("/processes")
    public Response listProcesses() {
        Command<?> cmd = new GetProcessIdsCommand();
        List<String> pList = (List<String>) processRequestBean.doKieSessionOperation(cmd, deploymentId, null, LIST_PROCESSES_EXCEPTION );
        StringBuilder sBuilder = new StringBuilder();
        if(pList != null && pList.size() > 0){
            sBuilder.append("processes from deploymentId : "+deploymentId);
            for(String pString : pList){
                sBuilder.append("\n\t"+pString);
            }
            sBuilder.append("\n");
        }else {
            sBuilder.append("no processes found for deploymentId : "+deploymentId+"\n");
        }
        ResponseBuilder builder = Response.ok(sBuilder.toString());
        return builder.build();
    }
    
    /**
     * provides visibility of WorkItemHandler(s) actually registered with KieSession
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/services-remote/rest/additional/runtime/org.acme.insurance:policyquote:1.0.0/workItemHandlers
     */
    @GET
    @Produces({ "text/plain" })
    @Path("/workItemHandlers")
    public Response printWorkItemHandlers() {
        Map<String, Object> workItemHandlers = new HashMap<String, Object>();
        
        //1)  get workItemHandler mappings registered as part of KModule deployment unit
        try {
            AbstractRuntimeManager runtimeManager = (AbstractRuntimeManager)runtimeMgrMgr.getRuntimeManager(deploymentId);
            RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(EmptyContext.get());
            RegisterableItemsFactory factory = runtimeManager.getEnvironment().getRegisterableItemsFactory();
            workItemHandlers.putAll(factory.getWorkItemHandlers(runtimeEngine));
        }catch(Exception x){
            x.printStackTrace();
            workItemHandlers.put("deploymentUnit mappings", "exception.  check log");
        }
        
        //2)  get workItemHandler mappings registered as per org.drools.core.SessionConfiguration
        try {
            SessionConfiguration sConfiguration = SessionConfiguration.getDefaultInstance();
            workItemHandlers.putAll(sConfiguration.getWorkItemHandlers());
        }catch(Exception x){
            x.printStackTrace();
            workItemHandlers.put("SessionConfiguration mappings", "exception. check log");
        }
        
        //3)  iterate and print as json
        StringBuilder sBuilder = new StringBuilder("[");
        int x = 0;
        for(Map.Entry<?, ?> entry : workItemHandlers.entrySet()){
            if(x > 0)
                sBuilder.append(",");
            sBuilder.append("{\""+ entry.getKey()+"\":\"");
            Class classObj = entry.getValue().getClass();
            if(classObj == String.class)
                sBuilder.append(entry.getValue());
            else
                sBuilder.append((entry.getValue()).getClass().getName());
            sBuilder.append("\"}");
            x++;
        }
        sBuilder.append("]");
        ResponseBuilder builder = Response.ok(sBuilder.toString());
        return builder.build();
    }
    
    /**
     * bounces registration of VFS or KModule Deployment Units
     * similar to discussion here:  https://bugzilla.redhat.com/show_bug.cgi?id=1001972
     * sample usage :
     *   curl -X PUT $HOSTNAME:8330/services-remote/rest/additional/runtime/all/deploymentUnits
     */
    @PUT
    @Produces({ "text/plain" })
    @Path("/deploymentUnits")
    public Response refreshDeploymentUnits() {
        try {
            dBean.stop();
            dBean.start();
            return Response.ok().build();
        }catch(Exception x){
            x.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * sample usage :
     *  curl -v -u jboss:brms -X GET http://$HOSTNAME:8330/services-remote/rest/additional/runtime/any/sanityCheck
     */
    @GET
    @Path("/sanityCheck")
    @Produces({ "text/plain" })
    public Response sanityCheck() {
        ResponseBuilder builder = Response.ok("good to go\n");
        return builder.build();
    }


}
