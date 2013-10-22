package org.jboss.processFlow.taskService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.processFlow.tasks.ITaskService;
import org.jbpm.services.task.commands.ClaimTaskCommand;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.DelegateTaskCommand;
import org.jbpm.services.task.commands.FailTaskCommand;
import org.jbpm.services.task.commands.GetContentCommand;
import org.jbpm.services.task.commands.GetTaskAssignedAsPotentialOwnerCommand;
import org.jbpm.services.task.commands.GetTaskByWorkItemIdCommand;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.GetTasksByStatusByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.NominateTaskCommand;
import org.jbpm.services.task.commands.ReleaseTaskCommand;
import org.jbpm.services.task.commands.SkipTaskCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.jbpm.services.task.exception.TaskException;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.InternalTaskService;


@Remote(ITaskService.class)
@Singleton(name="taskProxy")
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)

/* enabled EJB security so that authorization is forced subsequently allowing functionality in JAASUserGroupCallback to work correctly
 * this can be disabled if another UserGroupCallback implementation is configured
 * ideally, this annotation would exist as a configuration in WEB-INF/jboss-ejb3.xml .... however experiencing parse errors with that config
 */
@SecurityDomain("other")
public class HumanTaskService implements ITaskService {
    
    @Inject  // org.jbpm.services.task.impl.TaskServiceEntryPointImpl
    private TaskService taskService;
    
    @Resource
    private SessionContext ctx;

    public void claimTask(Long taskId, String userId ) throws TaskException {
        ClaimTaskCommand cmd = new ClaimTaskCommand(taskId, userId);
        ((InternalTaskService)taskService).execute(cmd);
    }
    public List<TaskSummary> claimNextAvailable(String userId, String language) {
        taskService.claimNextAvailable(userId, language);
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        return taskService.getTasksAssignedAsPotentialOwnerByStatus(userId, statuses, language);
    }
    public void completeTask(Long taskId, Map<String, Object> outboundTaskVars, String userId) {
        CompleteTaskCommand cmd = new CompleteTaskCommand(taskId, userId, outboundTaskVars);
        ((InternalTaskService)taskService).execute(cmd);   
    }
    public void delegateTask(Long taskId, String userId, String targetUserId) {
        DelegateTaskCommand cmd = new DelegateTaskCommand(taskId, userId, targetUserId);
        ((InternalTaskService)taskService).execute(cmd);
    }

    public void failTask(Long taskId, Map<String, Object> outboundTaskVars, String userId, Map<String, Object> faultData) {
        FailTaskCommand cmd = new FailTaskCommand(taskId, userId, faultData);
        ((InternalTaskService)taskService).execute(cmd);
    }

    public Task getTask(Long taskId) {
        GetTaskCommand cmd = new GetTaskCommand(taskId);
        return ((InternalTaskService)taskService).execute(cmd);
    }
    public Task getTaskByWorkItemId(Long workItemId) {
        GetTaskByWorkItemIdCommand cmd = new GetTaskByWorkItemIdCommand(workItemId);
        return ((InternalTaskService)taskService).execute(cmd);
    }
    public void skipTask(Long taskId, String userId) {
        SkipTaskCommand cmd = new SkipTaskCommand(taskId, userId);
        ((InternalTaskService)taskService).execute(cmd);
    }
    public void skipTaskByWorkItemId(Long workItemId, String userId) {
        Task taskObj = this.getTaskByWorkItemId(workItemId);
        this.skipTask(taskObj.getId(), userId);
    }
    public void startTask(Long taskId, String userId) {
        StartTaskCommand cmd = new StartTaskCommand(taskId, userId);
        ((InternalTaskService)taskService).execute(cmd);
    }
    public void nominateTask(long taskId, String userId, List<OrganizationalEntity> potentialOwners) {
        NominateTaskCommand cmd = new NominateTaskCommand(taskId, userId, potentialOwners);
        ((InternalTaskService)taskService).execute(cmd);
    }
    public void releaseTask(Long taskId, String userId) {
        ReleaseTaskCommand cmd = new ReleaseTaskCommand(taskId, userId);
        ((InternalTaskService)taskService).execute(cmd);
    }
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, String language, List<Status> statuses) {
        GetTaskAssignedAsPotentialOwnerCommand cmd = new GetTaskAssignedAsPotentialOwnerCommand(userId, language, statuses);
        return ((InternalTaskService)taskService).execute(cmd);
    }

    public List<Long> getTasksByProcessInstance(Long pInstanceId) {
        GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand(pInstanceId);
        return ((InternalTaskService)taskService).execute(cmd);
    }
    public List<TaskSummary> getTasksByStatusByProcessInstanceIdCommand(Long pInstanceId, String language, List<Status> statuses){
        GetTasksByStatusByProcessInstanceIdCommand cmd = new GetTasksByStatusByProcessInstanceIdCommand(pInstanceId, language, statuses );
        return ((InternalTaskService)taskService).execute(cmd);
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

    public List<I18NText> getTaskNames(Long taskId) {
        Task tObj = this.getTask(taskId);
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

    public Content getContentById(Long contentId) {
        GetContentCommand cmd = new GetContentCommand(contentId);
        return ((InternalTaskService)taskService).execute(cmd);
    }
    
    public List<Content> getAllContentByTaskId(long taskId) {
        return ((InternalTaskService)taskService).getAllContentByTaskId(taskId);
    }
    
    public Map<String, String> getContentListByTaskId(long taskId) {
        Task taskInstanceById = taskService.getTaskById(taskId);
        long documentContentId = taskInstanceById.getTaskData().getDocumentContentId();
        Content contentById = getContentById(documentContentId);
        if (contentById == null) {
            return new HashMap<String, String>();
        }
        Object unmarshall = ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
        if (unmarshall instanceof String) {
            if (((String) unmarshall).equals("")) {
                return new HashMap<String, String>();
            }
        }
        return (Map<String, String>) unmarshall;
    }

    public Map<String, String> getTaskOutputContentByTaskId(long taskId) {
        Task taskInstanceById = taskService.getTaskById(taskId);
        long documentContentId = taskInstanceById.getTaskData().getOutputContentId();
        if (documentContentId > 0) {
            Content contentById = getContentById(documentContentId);
            if (contentById == null) {
                return new HashMap<String, String>();
            }
            Object unmarshall = ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
            return (Map<String, String>) unmarshall;
        }
        return new HashMap<String, String>();
    }

    
    public Map<String, String> getContentListById(long contentId) {
        Content contentById = getContentById(contentId);
        Object unmarshall = ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
        return (Map<String, String>) unmarshall;
    }
    
    public String getSecurityInfo() {
        Principal principal = ctx.getCallerPrincipal();
        return principal.getName();
    }
}
