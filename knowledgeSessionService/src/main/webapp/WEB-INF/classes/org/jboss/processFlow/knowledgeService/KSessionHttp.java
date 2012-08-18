package org.jboss.processFlow.knowledgeService;

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

import org.drools.runtime.process.ProcessInstance;

@Stateless // this annotation is a convenient way to enable dependency injection. If missing, kProxy field won't be initialized by the container
@Path("/")
public class KSessionHttp {

    @EJB(lookup="java:global/processFlow-knowledgeSessionService/prodKSessionProxy!org.jboss.processFlow.knowledgeService.IKnowledgeSessionService")
    IKnowledgeSessionService kProxy;

    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/knowledgeService/processInstances/count
     */
    @GET
    @Path("/processInstances/count")
    @Produces({ "text/plain" })
    public Response createPojo() {
        List<ProcessInstance> pInstances = kProxy.getActiveProcessInstances(null);
        int count = 0;
        if(pInstances != null)
            count = pInstances.size();
        ResponseBuilder builder = Response.ok(count);
        return builder.build();
    }
}
