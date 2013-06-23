package org.jboss.processFlow.rest;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.tasks.identity.PFPUserGroupCallback;

@Stateless
@Path("/task")
public class TaskManagementFacade {
    
    @EJB(lookup="java:global/processFlow-taskService/taskProxy!org.jboss.processFlow.tasks.ITaskService")
    ITaskService taskServiceProxy;
    
    @POST
    @Path("{userId}/{taskId}/assign/{idRef}")
    @Produces({ "application/json" })
    public Response assignTask(@Context HttpServletRequest request, @PathParam("taskId") long taskId, @PathParam("idRef") String idRef, @PathParam("userId") String userId) {
        if (idRef == null) {
            taskServiceProxy.releaseTask(taskId, userId);
        } else if (idRef.equals(userId)) {
            List<String> callerRoles = getCallerRoles(idRef);
            taskServiceProxy.claimTask(taskId, idRef, idRef, callerRoles);
        } else {
            taskServiceProxy.delegateTask(taskId, userId, idRef);
        }        
        return Response.ok().build();
    }
    
    @POST
    @Path("{userId}/{taskId}/release")
    @Produces({ "application/json" })
    public Response releaseTask(@Context HttpServletRequest request, @PathParam("userId") String userId, @PathParam("taskId") long taskId) {
        taskServiceProxy.releaseTask(taskId, userId);
        return Response.ok().build();
    }

    @POST
    @Path("{userId}/{taskId}/close")
    @Produces({ "application/json" })
    public Response closeTask(@Context HttpServletRequest request, @PathParam("userId") String userId, @PathParam("taskId") long taskId) {
        taskServiceProxy.completeTask(taskId, null, userId);
        return Response.ok().build();
    }

    private List<String> getCallerRoles(String idRef) {
        PFPUserGroupCallback callback = new PFPUserGroupCallback();
        return callback.getGroupsForUser(idRef, null, null);
    }

}
