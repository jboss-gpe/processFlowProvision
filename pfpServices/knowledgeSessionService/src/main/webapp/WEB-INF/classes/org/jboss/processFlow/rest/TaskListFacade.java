package org.jboss.processFlow.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.tasks.identity.PFPUserGroupCallback;
import org.jbpm.task.Status;
import org.jbpm.task.User;
import org.jbpm.task.query.TaskSummary;

import com.google.gson.Gson;

@Stateless
@Path("/tasks")
public class TaskListFacade {
    
    @EJB(lookup="java:global/processFlow-taskService/taskProxy!org.jboss.processFlow.tasks.ITaskService")
    ITaskService taskProxy;
    
    public TaskListFacade() {    }
    
    @GET
    @Path("{idRef}/unassigned")
    @Produces({"application/json"})
    public Response getUnassignedTasksForIdRef(@PathParam("idRef") String idRef) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        List<TaskSummary> tasks = null;
        List<String> callerRoles = getCallerRoles(idRef);
        List<Status> onlyReady = Collections.singletonList(Status.Ready);
        tasks = taskProxy.getTasksAssignedAsPotentialOwnerByStatusByGroup(idRef, callerRoles, onlyReady, "en-UK", 0, 10);
        User emptyUser = new User();
        for (TaskSummary task: tasks) {
            task.setActualOwner(emptyUser);
            result.add(Transform.task(task));
        }
        TaskRefWrapper wrapper = new TaskRefWrapper(result);
        return createJsonResponse(wrapper);               
    }
    
    @GET
    @Path("{idRef}")
    @Produces({"application/json"})    
    public Response getAssignedTasksForIdRef(@PathParam("idRef") String idRef) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        List<TaskSummary> tasks = null;
        List<Status> onlyReady = Collections.singletonList(Status.Reserved);
        tasks = taskProxy.getAssignedTasks(idRef, onlyReady, "en-UK");
        for (TaskSummary task : tasks) {
            if (task.getStatus() == Status.Reserved) {
                result.add(Transform.task(task));
            }
        }
        TaskRefWrapper wrapper = new TaskRefWrapper(result);
        return createJsonResponse(wrapper); 
    }
    
    @GET
    @Path("{idRef}/taskcontents")
    @Produces({"application/json"})
    public Response getAssignedTasksForIdRefWithTaskContents(@PathParam("idRef") String idRef) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        List<TaskSummary> tasks = null;
        List<Status> onlyReady = Collections.singletonList(Status.Reserved);
        tasks = taskProxy.getAssignedTasks(idRef, onlyReady, "en-UK");
        for (TaskSummary task : tasks) {
            if (task.getStatus() == Status.Reserved) {
                TaskRef taskRef = Transform.task(task);
                Map<String, Object> content = taskProxy.getTaskContent(task.getId(), true);
                taskRef.setTaskContent(Transform.taskContent(content));
                result.add(taskRef);
            }
        }
        TaskRefWrapper wrapper = new TaskRefWrapper(result);
        return createJsonResponse(wrapper); 
    }
    
    @GET
    @Path("{idRef}/unassigned/taskcontents")
    @Produces({"application/json"})
    public Response getUnassignedTasksForIdRefWithTaskContents(@PathParam("idRef") String idRef) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        List<TaskSummary> tasks = null;
        List<String> callerRoles = getCallerRoles(idRef);
        List<Status> onlyReady = Collections.singletonList(Status.Ready);
        tasks = taskProxy.getTasksAssignedAsPotentialOwnerByStatusByGroup(idRef, callerRoles, onlyReady, "en-UK", 0, 10);
        User emptyUser = new User();
        for (TaskSummary task: tasks) {
            task.setActualOwner(emptyUser);
            TaskRef taskRef = Transform.task(task);
            Map<String, Object> content = taskProxy.getTaskContent(task.getId(), true);
            taskRef.setTaskContent(Transform.taskContent(content));
            result.add(taskRef);
        }
        TaskRefWrapper wrapper = new TaskRefWrapper(result);
        return createJsonResponse(wrapper);               
    }
    
    private List<String> getCallerRoles(String idRef) {
        PFPUserGroupCallback callback = new PFPUserGroupCallback();
        return callback.getGroupsForUser(idRef, null, null);
    }
    
    private Response createJsonResponse(Object wrapper) {
      Gson gson = GsonFactory.createInstance();
      String json = gson.toJson(wrapper);
      return Response.ok(json).type("application/json").build();
    }
    
    

}
