package org.jboss.processFlow.consoleIntegration;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kie.api.command.Command;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.ContentMarshallerContext;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.internal.task.api.UserInfo;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.FaultData;
import org.kie.internal.task.api.model.SubTasksStrategy;
import org.kie.internal.task.api.model.TaskDef;
import org.kie.internal.task.api.model.TaskEvent;

@ApplicationScoped
public class RemoteTaskService implements InternalTaskService {
    
    private static Logger log = LoggerFactory.getLogger("RemoteTaskService");
    
    public RemoteTaskService() {
        System.out.println("********** RemoteTaskService");
    }

    @Override
    public void activate(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void claim(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void claimNextAvailable(String userId, String language) {
        // TODO Auto-generated method stub

    }

    @Override
    public void complete(long taskId, String userId, Map<String, Object> data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delegate(long taskId, String userId, String targetUserId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exit(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void fail(long taskId, String userId, Map<String, Object> faultData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void forward(long taskId, String userId, String targetEntityId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Task getTaskByWorkItemId(long workItemId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Task getTaskById(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(
            String userId, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(
            String userId, List<Status> status, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksOwned(String userId, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksOwnedByStatus(String userId,
            List<Status> status, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceId(
            long processInstanceId, List<Status> status, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> getTasksByProcessInstanceId(long processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksByVariousFields(List<Long> workItemIds,
            List<Long> taskIds, List<Long> procInstIds, List<String> busAdmins,
            List<String> potOwners, List<String> taskOwners,
            List<Status> status, boolean union) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksByVariousFields(
            Map<String, List<?>> parameters, boolean union) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long addTask(Task task, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void release(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void skip(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void start(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspend(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void nominate(long taskId, String userId,
            List<OrganizationalEntity> potentialOwners) {
        // TODO Auto-generated method stub

    }

    @Override
    public Content getContentById(long contentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Attachment getAttachmentById(long attachId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T execute(Command<T> command) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addGroup(Group group) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addUser(User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public int archiveTasks(List<TaskSummary> tasks) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void claim(long taskId, String userId, List<String> groupIds) {
        // TODO Auto-generated method stub

    }

    @Override
    public void claimNextAvailable(String userId, List<String> groupIds,
            String language) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteFault(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteOutput(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deployTaskDef(TaskDef def) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<TaskSummary> getActiveTasks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getActiveTasks(Date since) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskDef> getAllTaskDef(String filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getArchivedTasks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getCompletedTasks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getCompletedTasks(Date since) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getCompletedTasksByProcessId(Long processId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Group getGroupById(String groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Group> getGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getSubTasksAssignedAsPotentialOwner(long parentId,
            String userId, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getSubTasksByParent(long parentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPendingSubTasksByParent(long parentId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public TaskDef getTaskDefById(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDate(
            String userId, List<Status> statuses, Date expirationDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDateOptional(
            String userId, List<Status> statuses, Date expirationDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDate(String userId,
            List<Status> statuses, Date expirationDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDateOptional(
            String userId, List<Status> statuses, Date expirationDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsExcludedOwner(String userId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId,
            List<String> groupIds, String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId,
            List<String> groupIds, String language, int firstResult,
            int maxResults) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatusByGroup(
            String userId, List<String> groupIds, List<Status> status,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsRecipient(String userId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsTaskInitiator(String userId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsTaskStakeholder(String userId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDateBeforeSpecifiedDate(
            String userId, List<Status> status, Date date) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceIdByTaskName(
            long processInstanceId, List<Status> status, String taskName,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Long, List<OrganizationalEntity>> getPotentialOwnersForTaskIds(
            List<Long> taskIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUserById(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> getUsers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long addTask(Task task, ContentData data) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void remove(long taskId, String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeGroup(String groupId) {
        // TODO Auto-generated method stub

    }

    @Override
    public int removeTasks(List<TaskSummary> tasks) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void removeUser(String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFault(long taskId, String userId, FaultData fault) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setOutput(long taskId, String userId, Object outputContentData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPriority(long taskId, int priority) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTaskNames(long taskId, List<I18NText> taskNames) {
        // TODO Auto-generated method stub

    }

    @Override
    public void undeployTaskDef(String id) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<TaskEvent> getTaskEventsById(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserInfo getUserInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setUserInfo(UserInfo userInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addUsersAndGroups(Map<String, User> users,
            Map<String, Group> groups) {
        // TODO Auto-generated method stub

    }

    @Override
    public int removeAllTasks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long addContent(long taskId, Content content) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long addContent(long taskId, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteContent(long taskId, long contentId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Content> getAllContentByTaskId(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long addAttachment(long taskId, Attachment attachment,
            Content content) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteAttachment(long taskId, long attachmentId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Attachment> getAllAttachmentsByTaskId(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeTaskEventsById(long taskId) {
        // TODO Auto-generated method stub

    }

    @Override
    public OrganizationalEntity getOrganizationalEntityById(String entityId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExpirationDate(long taskId, Date date) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDescriptions(long taskId, List<I18NText> descriptions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSkipable(long taskId, boolean skipable) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSubTaskStrategy(long taskId, SubTasksStrategy strategy) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getPriority(long taskId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Date getExpirationDate(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<I18NText> getDescriptions(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSkipable(long taskId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SubTasksStrategy getSubTaskStrategy(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Task getTaskInstanceById(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getCompletedTaskByUserId(String userId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getPendingTaskByUserId(String userId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<TaskSummary> getTasksAssignedByGroup(String groupId,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TaskSummary> getTasksAssignedByGroups(List<String> groupIds,
            String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long addComment(long taskId, Comment comment) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteComment(long taskId, long commentId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Comment> getAllCommentsByTaskId(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Comment getCommentById(long commentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getTaskContent(long taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addMarshallerContext(String ownerId,
            ContentMarshallerContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeMarshallerContext(String ownerId) {
        // TODO Auto-generated method stub

    }

    @Override
    public ContentMarshallerContext getMarshallerContext(Task task) {
        // TODO Auto-generated method stub
        return null;
    }

}
