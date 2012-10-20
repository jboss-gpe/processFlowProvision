package org.jboss.processFlow.knowledgeService;

import java.net.ConnectException;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.drools.runtime.process.ProcessInstance;

/* test interface for ksessionService via http
 *
 * addition of @Stateless annotation provides following advantages:
 *
 *   Injection capabilities: you can easily inject other EJBs, EntityManagers, JMS-resources, DataSources or JCA connectors
 *   Transactions: all changes made in a REST-call will be automatically and transparently synchronized with the database
 *   Single threading programming model -> the old EJB goodness.
 *   Monitoring: an EJB is visible in JMX
 *   Throttling: its easy to restrict the concurrency of an EJB using ThreadPools or bean pools
 *   Vendor-independence: EJB 3 runs on multiple containers, without any modification (and without any XML in particular :-))
*/
@Stateless 
@Path("/")
public class KSessionHttp {

    @EJB(lookup="java:global/processFlow-knowledgeSessionService/prodKSessionProxy!org.jboss.processFlow.knowledgeService.IKnowledgeSessionService")
    IKnowledgeSessionService kProxy;

    private Logger log = LoggerFactory.getLogger("KSessionHttp");

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/processInstances/count
     */
    @GET
    @Path("/processInstances/count")
    @Produces({ "text/plain" })
    public Response getActiveProcessInstancesCount() {
        log.info("getActiveProcessInstancesCount() .. invoking ...");
        List<ProcessInstance> pInstances = kProxy.getActiveProcessInstances(null);
        log.info("getActiveProcessInstancesCount() .. returned ...");
        int count = 0;
        if(pInstances != null)
            count = pInstances.size();
        ResponseBuilder builder = Response.ok(count);
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X PUT -HAccept:text/plain $HOSTNAME:8330/knowledgeService/kbase/agent
     *  curl -X PUT -HAccept:text/plain https://pfpcore-jbride0.rhcloud.com/knowledgeService/kbase/agent
     */
    @PUT
    @Path("/kbase/agent")
    public Response rebuildKnowledgeBaseViaKnowledgeAgent() {
    	ResponseBuilder builder = Response.ok();
    	try {
    		kProxy.rebuildKnowledgeBaseViaKnowledgeAgent();
    	}catch(ConnectException x){
    		builder = Response.status(Status.SERVICE_UNAVAILABLE);
    	}
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/kbase/content
     *  curl -X GET -HAccept:text/plain https://pfpcore-jbride0.rhcloud.com/knowledgeService/kbase/content
     */
    @GET
    @Path("/kbase/content")
    public Response printKnowledgeBaseContent() {
        String kBaseContent = kProxy.printKnowledgeBaseContent();
        ResponseBuilder builder = Response.ok(kBaseContent);
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/workItemHandlers
     *  curl -X GET -HAccept:text/plain https://pfpcore-jbride0.rhcloud.com/knowledgeService/workItemHandlers
     */
    @GET
    @Path("/workItemHandlers")
    public Response printWorkItemHandlers() {
        String kBaseContent = kProxy.printWorkItemHandlers();
        ResponseBuilder builder = Response.ok(kBaseContent);
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/sanityCheck
     */
    @GET
    @Path("/sanityCheck")
    @Produces({ "text/plain" })
    public Response sanityCheck() {
        ResponseBuilder builder = Response.ok("good to go\n");
        return builder.build();
    }
}
