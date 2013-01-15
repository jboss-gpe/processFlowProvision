package org.jboss.processFlow.knowledgeService;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/processInstances
     */
    @GET
    @Path("/processInstances")
    @Produces({ "text/plain" })
    public Response printActiveProcessInstances() {
        ResponseBuilder builder = Response.ok(kProxy.printActiveProcessInstances(null));
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X PUT -HAccept:text/plain $HOSTNAME:8330/knowledgeService/kbase
     *  curl -X PUT -HAccept:text/plain http://pfpcore-jbride0.rhcloud.com/knowledgeService/kbase
     */
    @PUT
    @Path("/kbase")
    public Response createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder() {
        ResponseBuilder builder = Response.ok();
        try {
            kProxy.createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder();
        }catch(RuntimeException x){
            builder = Response.status(Status.SERVICE_UNAVAILABLE);
        }
        return builder.build();
    }

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/kbase/content
     *  curl -X GET -HAccept:text/plain http://pfpcore-jbride0.rhcloud.com/knowledgeService/kbase/content
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
     *  curl -X GET -HAccept:text/plain http://pfpcore-jbride0.rhcloud.com/knowledgeService/workItemHandlers
     */
    @GET
    @Path("/workItemHandlers")
    public Response printWorkItemHandlers() {
        String kBaseContent = kProxy.printWorkItemHandlers();
        ResponseBuilder builder = Response.ok(kBaseContent);
        return builder.build();
    }

    /**
     * purpose      : provide a REST API for signalling kSessionService that allows for a payload in the HttpRequest body
     *                business-central-service REST API expose a signalling function.
     *                however, payload data needs to be added as QueryData to the URL .... which is sufficient only with a small payload
     * sample usage :
     *  curl -X PUT -HAccept:text/plain $HOSTNAME:8330/knowledgeService/rs/process/tokens/1/transition?signalType=test
     *  curl -X PUT -HAccept:text/plain http://pfpcore-jbride0.rhcloud.com/knowledgeService/rs/process/tokens/1/transition?signalType=test
     */
    @PUT
    @Path("/rs/process/tokens/{pInstanceId: .*}/transition")
    public Response signalEvent(@PathParam("pInstanceId")final Long pInstanceId, 
                                @QueryParam("signalType")final String signalType,
                                @QueryParam("ksessionId")final Integer ksessionId,
                                final String signalPayload
                                ) {
        ResponseBuilder builder = Response.ok();
        try {
        	String[] signalData = signalPayload.split("\\$");
        	Map<String, String> signalMap = new HashMap<String, String>();
            for(int t = 1; t< signalData.length; t++) {
                signalMap.put(signalData[t], signalData[t+1]);
                t++;
            }
            kProxy.signalEvent(signalType, signalMap, pInstanceId, ksessionId);
        }catch(RuntimeException x){
            builder = Response.status(Status.SERVICE_UNAVAILABLE);
        }
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
