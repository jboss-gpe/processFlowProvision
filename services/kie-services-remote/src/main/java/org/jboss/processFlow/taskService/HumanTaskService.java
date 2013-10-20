package org.jboss.processFlow.taskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import javax.inject.Inject;

import org.jboss.processFlow.tasks.ITaskService;
import org.jbpm.services.task.commands.ClaimNextAvailableTaskCommand;
import org.jbpm.services.task.exception.TaskException;
import org.kie.api.command.Command;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;


@Remote(ITaskService.class)
@Singleton(name="taskProxy")
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class HumanTaskService implements ITaskService {
    
    @Inject
    private TaskService taskService;

    public void claimTask(Long taskId, String userId ) throws TaskException {
        taskService.claim(taskId, userId);
    }
    public List<TaskSummary> claimNextAvailable(String userId, String language) {
        taskService.claimNextAvailable(userId, language);
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        return taskService.getTasksAssignedAsPotentialOwnerByStatus(userId, statuses, language);
    }
    public void completeTask(Long taskId, Map<String, Object> outboundTaskVars, String userId) {
        taskService.complete(taskId, userId, outboundTaskVars);    
    }
    public void delegateTask(Long taskId, String userId, String targetUserId) {
        taskService.delegate(taskId, userId, targetUserId);
    }

    public void failTask(Long taskId, Map<String, Object> outboundTaskVars, String userId, Map<String, Object> faultData) {
        taskService.fail(taskId, userId, faultData);;
    }

    public Task getTask(Long taskId) {
        return taskService.getTaskById(taskId);
    }
    public Task getTaskByWorkItemId(Long workItemId) {
        return taskService.getTaskByWorkItemId(workItemId);
    }
    public void skipTask(Long taskId, String userId) {
        taskService.skip(taskId, userId);
    }
    public void skipTaskByWorkItemId(Long workItemId, String userId) {
        Task taskObj = taskService.getTaskByWorkItemId(workItemId);
        taskService.skip(taskObj.getId(), userId);
    }
    public void startTask(Long taskId, String userId) {
        taskService.start(taskId, userId);
    }
    public void nominateTask(long taskId, String userId, List<OrganizationalEntity> potentialOwners) {
        taskService.nominate(taskId, userId, potentialOwners);
    }
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, String language) {
        return taskService.getTasksAssignedAsPotentialOwner(userId, language);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, String language, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> statuses, String language, Integer firstResult, Integer maxResults) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> statuses, String language, Integer firstResult, Integer maxResults) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksByProcessInstance(Long processInstanceId, Status taskStatus) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getTaskContent(Long taskId, Boolean inbound) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTaskContent(Task taskObj, Boolean inbound, Map<String, Object> taskContent) {
        // TODO Auto-generated method stu
        
    }

    @Override
    public String printTaskContent(Long taskId, Boolean inbound) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<I18NText> getTaskNames(Long taskId, String language) {
        Task tObj = taskService.getTaskById(taskId);
        return tObj.getNames();
    }

    @Override
    public List<TaskSummary> getAssignedTasks(String userId, List<Status> statuses, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List query(String qlString, Integer size, Integer offset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Content getContent(Long contentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map populateHashWithTaskContent(Content contentObj, String keyName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void releaseTask(Long taskId, String userId) {
        // TODO Auto-generated method stub
        
    }
}
