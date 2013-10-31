package org.kie.services.remote.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.kie.services.client.serialization.jaxb.impl.audit.JaxbHistoryLogList;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.services.remote.util.Paginator;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/history")
@RequestScoped
public class HistoryResource extends ResourceBase {

    private static final Logger logger = LoggerFactory.getLogger(HistoryResource.class);
    private static final String BAM_PERSISTENCE_UNIT_NAME = "org.jbpm.audit.jpa.ex.server";

    /* REST information */
    @Context
    private HttpServletRequest request;
    
    @Context
    private Request restRequest;

    @POST
    @Path("/clear")
    public Response clearProcessInstanceLogs() {
        JPAAuditLogService lService = new JPAAuditLogService();
        lService.setPersistenceUnitName(BAM_PERSISTENCE_UNIT_NAME);
        lService.clear();
        return createCorrectVariant(new JaxbGenericResponse(request), restRequest);
    }

    @GET
    @Path("/instance/{procInstId: [0-9]+}")
    public Response getSpecificProcessInstanceLogs(@PathParam("procInstId") long procInstId) {
        JPAAuditLogService lService = new JPAAuditLogService();
        lService.setPersistenceUnitName(BAM_PERSISTENCE_UNIT_NAME);
        ProcessInstanceLog procInstLog = lService.findProcessInstance(procInstId);
        Map<String, List<String>> params = getRequestParams(request);
        int [] pageInfo = getPageNumAndPageSize(params);
        
        List<ProcessInstanceLog> logList = new ArrayList<ProcessInstanceLog>();
        logList.add(procInstLog);
        
        logList = (new Paginator<ProcessInstanceLog>()).paginate(pageInfo, logList);
        return createCorrectVariant(new JaxbHistoryLogList(logList), restRequest);
    }
    
    /**
     * sample usage :
     *  curl -v -u jboss:brms -X GET -HAccept:text/plain $HOSTNAME:8330/kie-jbpm-services/rest/history/sanityCheck
     */
    @GET
    @Path("/sanityCheck")
    @Produces({ "text/plain" })
    public Response sanityCheck() {
        ResponseBuilder builder = Response.ok("good to go\n");
        return builder.build();
    }

}
