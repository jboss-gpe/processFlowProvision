package org.kie.services.remote.cdi;

import java.util.List;

import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.drools.core.command.runtime.process.GetProcessIdsCommand;
import org.kie.services.remote.cdi.ProcessRequestBean;
import org.kie.api.command.Command;

@Stateless
@Path("/additional/runtime/{id: [a-zA-Z0-9-:\\.]+}")
public class AdditionalRESTResources {
	
	private static final Logger logger = LoggerFactory.getLogger(AdditionalRESTResources.class);
    
    @Inject
    private ProcessRequestBean processRequestBean;

    @PathParam("id")
    private String deploymentId;

    @Context
    private HttpServletRequest request;
    
    
    /**
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/kie-jbpm-services/rest/additional/runtime/general/processes
     */
    @GET
    @Produces({ "text/plain" })
    @Path("/processes")
    public Response listProcesses() {
        Command<?> cmd = new GetProcessIdsCommand();
        List<String> pList = (List<String>) processRequestBean.doKieSessionOperation(cmd, deploymentId);
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
     * sample usage :
     *  curl -X GET -HAccept:text/plain $HOSTNAME:8330/kie-jbpm-services/rest/additional/runtime/general/sanityCheck
     */
    @GET
    @Path("/sanityCheck")
    @Produces({ "text/plain" })
    public Response sanityCheck() {
        ResponseBuilder builder = Response.ok("good to go\n");
        return builder.build();
    }


}
