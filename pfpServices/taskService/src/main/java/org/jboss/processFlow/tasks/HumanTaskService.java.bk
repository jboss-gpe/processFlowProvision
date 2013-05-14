/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.processFlow.tasks;

import java.io.Externalizable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.jboss.processFlow.PFPBaseService;
import org.jbpm.services.task.exception.IllegalTaskStateException;
import org.jbpm.services.task.exception.PermissionDeniedException;
import org.jbpm.services.task.exception.TaskException;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.internal.task.api.TaskService;
import org.kie.internal.task.api.model.Content;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.I18NText;
import org.kie.internal.task.api.model.OrganizationalEntity;
import org.kie.internal.task.api.model.Status;
import org.kie.internal.task.api.model.Task;
import org.kie.internal.task.api.model.TaskSummary;
import org.kie.internal.task.api.model.User;

/**  
 *  JA Bride
 *  23 March 2011
 *  Purpose :  synchronous API to various human task functions
 */
@Remote(ITaskService.class)
@Singleton(name="taskProxy")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class HumanTaskService extends PFPBaseService implements ITaskService {

    public static final String JBPM_TASK_EMF_RESOURCE_LOCAL = "org.jbpm.task.resourceLocal";
    public static final String JBPM_TASK_EMF = "org.jbpm.task";
    public static final String LOCAL_JTA = "local-JTA";
    public static final String RESOURCE_LOCAL = "RESOURCE_LOCAL";
    public static final String TASK_BY_TASK_ID = "TaskByTaskId";

    private static final Logger log = Logger.getLogger("HumanTaskService");
    
    @Inject
    TaskService taskService;
    
    private boolean enableLog = false;

    public long addTask(Task taskObj, ContentData cData) {
        return taskService.addTask(taskObj, cData);
    }
    
    public void claimTask(Long taskId, String userId, List<String> roles) throws TaskException {
        taskService.claim(taskId, userId, roles);
    }
    public void claimNextAvailable(String userId, List<String> groupIds, String language){
        taskService.claimNextAvailable(userId, groupIds, language);
    }
    
    public TaskSummary guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> statuses, String language, Integer firstResult, Integer maxResults){
        List<TaskSummary> taskList = this.getTasksAssignedAsPotentialOwnerByStatusByGroup(userId, groupIds, statuses, language, firstResult, maxResults);
        TaskSummary claimedTask = null;
        for(TaskSummary tObj : taskList){
            try {
                this.claimTask(tObj.getId(), userId, groupIds);
                claimedTask = tObj;
                break;
            }catch(org.jbpm.task.service.PermissionDeniedException x){
                if(enableLog){
                    log.error("guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup() PermissionDeniedException for taskId = "+tObj.getId());
                }
            }
        }
        return claimedTask;
    }

    /**
        - changes task status from : InProgress --> Completed
        - this method blocks until process instance arrives at next 'safe point'
        - this method uses BMT to define the scope of the transaction
     */
    public void completeTask(Long taskId, Map<String, Object> outboundTaskVars, String userId) {
        Task taskObj = taskService.getTaskById(taskId);
        if(taskObj.getTaskData().getStatus() != Status.InProgress) {
            log.warn("completeTask() task with following id will be changed to status of InProgress: "+taskId);
            taskService.start(taskId, userId);
        }
        taskService.complete(taskId, userId, outboundTaskVars);
    }

    public void delegateTask(Long taskId, String userId, String targetUserId) {
        taskService.delegate(taskId, userId, targetUserId);
    }

    /**
     * NOTE:  allows for a null userId ... in which case this implementation will use the actual owner of the task to execute the fail task operation
     */
    public void failTask(Long taskId, Map<String, Object> faultTaskVars, String userId) {
        Task taskObj = taskService.getTaskById(taskId);
        if(userId == null)
            userId = taskObj.getTaskData().getActualOwner().getId();
        
        if(taskObj.getTaskData().getStatus() != Status.InProgress) {
            throw new PermissionDeniedException("failTask() will not attempt operation due to incorrect existing status of : "+taskObj.getTaskData().getStatus());
        }
        taskService.fail(taskId, userId, faultTaskVars);
    }
    
    public void nominateTask(final long taskId, String userId, final List<OrganizationalEntity> potentialOwners){
        taskService.nominate(taskId, userId, potentialOwners);
    }

    public void releaseTask(Long taskId, String userId){
        taskService.release(taskId, userId);
    }
    
    public void skipTask(Long taskId, String userId, Map<String, Object> outboundTaskVars) {
        addOutboundContent(taskService.getTaskById(taskId), outboundTaskVars);
        taskService.skip(taskId, userId);
    }

    public void skipTaskByWorkItemId(Long workItemId){
        Task taskObj = getTaskByWorkItemId(workItemId);
        Status tStatus = taskObj.getTaskData().getStatus();
        if(tStatus == Status.Obsolete || tStatus == Status.Error || tStatus == Status.Failed) {
            log.error("skipTaskByWorkItemId() can not skip task since status is : "+tStatus.name());
        }
        String userId = ITaskService.ADMINISTRATOR;
        User actualOwner = taskObj.getTaskData().getActualOwner();
        if(actualOwner != null)
            userId = actualOwner.getId();

        taskService.skip(taskObj.getId(), userId);
    }

    // changes task status from : Reserved --> InProgress
    public void startTask(Long taskId, String userId) {
        taskService.start(taskId, userId);
    }

    /*
     *  NOTE:  use with caution
     *      in particular, this implementation uses a JTA enabled entity manager factory 
     *      the reason is that in postgresql, using a RESOURCE_LOCAL EMF throws a "Large Object exception" (google it)
     *      so try to avoid taxing the transaction manager with an abusive amount of calls to this function
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Externalizable getTask(Long taskId){
        Task taskObj = taskService.getTaskById(taskId);
        org.jbpm.task.query.TaskSummary tSummary = new org.jbpm.task.query.TaskSummary();
        tSummary.setActivationTime(taskObj.getTaskData().getExpirationTime());
        tSummary.setCreatedOn(taskObj.getTaskData().getCreatedOn());
        tSummary.setDescription(taskObj.getDescriptions().get(0).getText());
        tSummary.setExpirationTime(taskObj.getTaskData().getExpirationTime());
        tSummary.setId(taskObj.getId());
        tSummary.setName(taskObj.getNames().get(0).getText());
        tSummary.setPriority(taskObj.getPriority());
        tSummary.setProcessId(taskObj.getTaskData().getProcessId());
        tSummary.setProcessInstanceId(taskObj.getTaskData().getProcessInstanceId());
        tSummary.setSubject(taskObj.getSubjects().get(0).getText());
        
        org.jbpm.task.User actualOwner = new org.jbpm.task.User();
        actualOwner.setId(taskObj.getTaskData().getActualOwner().getId());
        tSummary.setActualOwner(actualOwner);
        
        org.jbpm.task.User createdOwner = new org.jbpm.task.User();
        createdOwner.setId(taskObj.getTaskData().getCreatedBy().getId());
        tSummary.setCreatedBy(createdOwner);
        
        String statusString = taskObj.getTaskData().getStatus().toString();
        tSummary.setStatus(org.jbpm.task.Status.valueOf(statusString));
        
        return tSummary;
    }
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Task getTaskByWorkItemId(Long workItemId){
        return taskService.getTaskByWorkItemId(workItemId);
    }

    /*
     *  NOTE:  use with caution
     *      in particular, this implementation uses a JTA enabled entity manager factory 
     *      the reason is that in postgresql, using a RESOURCE_LOCAL EMF throws a "Large Object exception" (google it)
     *      so try to avoid taxing the transaction manager with an abusive amount of calls to this function
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String getTaskName(Long taskId, String language) throws IllegalTaskStateException {
        Task taskObj = taskService.getTaskById(taskId);

        Iterator<I18NText> iTasks = taskObj.getNames().iterator();
        while(iTasks.hasNext()){
            I18NText iObj = (I18NText)iTasks.next();
            if(iObj.getLanguage().equals(language))
                return iObj.getText();
        }
        throw new IllegalTaskStateException("getTaskName() can not find taskName for taskId = "+taskId+" : language = "+language);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language) {
        return taskService.getTasksAssignedAsPotentialOwner(userId, groupIds, language);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<Long> getTasksByProcessInstance(Long processInstanceId) {
        return taskService.getTasksByProcessInstanceId(processInstanceId);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language, Integer firstResult, Integer maxResults) {
        return taskService.getTasksAssignedAsPotentialOwner(userId, groupIds, language, firstResult, maxResults);
    }
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> statuses, String language, Integer firstResult, Integer maxResults){
        return taskService.getTasksAssignedAsPotentialOwnerByStatusByGroup(userId, groupIds, statuses, language);
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> statuses, String language, Integer firstResult, Integer maxResults){
        return taskService.getTasksAssignedAsPotentialOwnerByStatus(userId, statuses, language);
    }

    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksOwned(String userId, List<Status> statuses, String language) {
        return taskService.getTasksOwned(userId, statuses, language);
    }
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<TaskSummary> getTasksOwnedByExpirationDate(String userId, List<Status> statuses, Date expirationDate) {
        return taskService.getTasksOwnedByExpirationDate(userId, statuses, expirationDate);
    }
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String,Object> getTaskContent(Long taskId, String contentType) {
        Task taskObj = taskService.getTaskById(taskId);
        long contentId = 0L;
        Map<String, Object> taskContent = null;
        if(ITaskService.DOCUMENT_CONTENT.equals(contentType)){
            contentId = taskObj.getTaskData().getDocumentContentId();
        }else if(ITaskService.OUTBOUND_CONTENT.equals(contentType)){
            contentId = taskObj.getTaskData().getOutputContentId();
        }else if(ITaskService.FAULT_CONTENT.equals(contentType)){
            contentId = taskObj.getTaskData().getFaultContentId();
        }else{
            throw new IllegalArgumentException("getTaskContent() invalid contentType = "+contentType);
        }
        List<Content> contentList = taskService.getAllContentByTaskId(taskId);
        for(Content cObj : contentList){
            if(cObj.getId() == contentId)
                taskContent = (Map<String, Object>)ContentMarshallerHelper.unmarshall(cObj.getContent(), null);
        }
        return taskContent;
    }

    public void addOutboundContent(Task taskObj, Map<String, Object> taskContent) {
        taskService.addContent(taskObj.getId(), taskContent);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String printTaskContent(Long taskId, String contentType) {
        Map<?,?> contentHash = getTaskContent(taskId, contentType);
        StringBuilder sBuilder = new StringBuilder("taskContent taskId = ");
        sBuilder.append(taskId);
        sBuilder.append("\t: contentType = ");
        sBuilder.append(contentType);
        for (Map.Entry<?, ?> entry: contentHash.entrySet()) {
            sBuilder.append("\n\tkey ="+entry.getKey()+"\t: value = "+entry.getValue());
        }
        return sBuilder.toString();
    }

    public void logTaskDetails(Task taskObj) {
        StringBuilder sBuilder = new StringBuilder("completeTask()");
        long workItemId = taskObj.getTaskData().getWorkItemId();
        int ksessionIdFromTask = taskObj.getTaskData().getProcessSessionId(); 
        long documentContentId = taskObj.getTaskData().getDocumentContentId();
        long outputContentId = taskObj.getTaskData().getOutputContentId();
        long processInstanceId = taskObj.getTaskData().getProcessInstanceId();
    
        sBuilder.append("\n\ttaskId = ");
        sBuilder.append(taskObj.getId());
        sBuilder.append("\n\tworkItemId = ");
        sBuilder.append(workItemId);
        sBuilder.append("\n\tvariables:  processInstance --> task :  documentContentId = ");
        sBuilder.append(documentContentId);
        sBuilder.append("\n\tvariables:  task --> processInstance :  outputContentId = ");
        sBuilder.append(outputContentId);
        sBuilder.append("\n\tprocessInstanceId = ");
        sBuilder.append(processInstanceId);
        sBuilder.append("\n\tksessionIdFromTask = ");
        sBuilder.append(ksessionIdFromTask);
        log.info(sBuilder.toString());
    }

    @PostConstruct
    public void start() throws Exception {
        log.info("start()");
    }

    @PreDestroy
    public void stop() throws Exception {
        log.info("stop()");
    }

}
