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
import java.util.List;
import java.util.Map;

/*import org.jbpm.task.OrganizationalEntity;
import org.jbpm.task.Task;
import org.jbpm.task.Status;
import org.jbpm.task.Content;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.TaskException;
import org.jbpm.task.service.CannotAddTaskException;*/

import org.kie.internal.task.api.model.*;

/**
 *
 *<b>Responsibilities</b>
 *<pre>
 * WS-HT state transitions
 *   - Provide an EJB based service that allows for manipulation of WS-HT state transitions on human tasks
 *   - see the following link for an overview of WS-HT defined task states and their transitions:
 *      http://docs.oasis-open.org/bpel4people/ws-humantask-1.1.html
 *          section 4.10 :  Human Task Behavior and State Transitions
 *
 * Interaction with org.jbpm.task.service.TaskServiceSession
 *  - org.jbpm.task.service.TaskServiceSession provides the majority of the functionality to manage jbpm human tasks
 *  - implementations of this interface expose various TaskServiceSession derived operations as an EJB 'service'
 *  - implementations may choose to interact with the TaskServiceSession directly.
 *  - another approach may be to interact with the TaskServiceSession via any of the numerous jbpm5 human task 'servers' which implement: org.jbpm.task.service.TaskServer
 *
 *
 *</pre>
 *
 *<p>
 *<b>Recommendations</b>
 *<pre>
 * performance considerations
 *  - some of these operations will be invoked significantly more than others
 *  - in particular, implementations will want to ensure that the claimTask(....) operation is implemented in a manner that is very performant
 *  - the scenario for requiring a performance 'claimTask(...)' implementation is as follows :
 *    - if human tasks are assigned to a group queue, then clients will want to invoke one of the several 'getTasksAssignedAsPotentialOwner(...)' operations
 *    - clients then iterate through the List<TaskSummary> returned from getTasksAssignedAsPotentialOwner(...)
 *    - which each iteration through the List<TaskSummary>, the 'claimTask(..)' operation is invoked
 *    - in a concurrent environment, it is quite likely that other clients may have already RESERVED some of the tasks 
           originally returned from 'getTasksAssignedAsPotentialOwner'
 *
 * 'guaranteedClaimTask' operation
 *   - to avoid the heavy network hit imposed by remote clients invoking 'claimTask(...)' as they each iterate through a List<TaskSummary>,
 *     may want to consider implementing a method called 'guaranteedClaimTask(...)'
 *   - the purpose of 'guaranteedClaimTask(..)' would be to return a Task to the client that is guaranteed to have been RESERVED by that client
 *   - this ITaskService implementation would iterate through a List<TaskSummary> rather than having remote clients do so
 *</pre>
 *</p>
 *
 *<p>
 *<b>WS-HT task state overview</b>
 *<pre>
 *start states
 *    Created
 *
 *active states:
 *    Ready, Reserved, InProgress
 *
 *final states:
 *    Completed, Failed, Error, Exited, Obsolete
 *</pre>
 *</p>
 *
 */
public interface ITaskService {

    public static final String TASK_SERVICE_JNDI = "ejb:/processFlow-taskService//taskProxy!org.jboss.processFlow.tasks.ITaskService";
    public static final String TASK_SERVICE_PROVIDER_URL = "org.jboss.processFlow.tasks.TASK_SERVICE_PROVIDER_URL";
    public static final String ADMINISTRATOR = "admin";
    public static final String HUMAN_TASK = "Human Task";
    public static final String SKIP_TASK="pfpSkipTask";
    public static final String SKIP_TASK_SIGNAL="skipTaskSignal";
    public static final String FAIL_TASK="pfpFailTask";
    public static final String FAIL_TASK_SIGNAL="failTaskSignal";
    public static final String TASK_ID = "TASK_ID";
    public static final String TASK_STATUS="TASK_STATUS";
    public static final String DEADLINE_HANDLER = "org.jboss.processFlow.tasks.DeadlineHandler";
    public static final String DOCUMENT_CONTENT = "documentContent";
    public static final String OUTBOUND_CONTENT = "outboundContent";
    public static final String FAULT_CONTENT = "faultContent";

    /**
     *creates a task with status of 'Ready'
     *<pre>
     * NOTE : this operation will most likely will be invoked by a BRMS customer 'workItemHandler'
     *</pre>
     */
    public long addTask(Task taskObj, ContentData cData);

    /**
     * changes task status :  Ready --> Reserved
     */
    public void claimTask(Long taskId, String userId, List<String> roles);
    public void claimNextAvailable(String userId, List<String> groupIds, String language);
    
    public TaskSummary guaranteedClaimTaskAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> statuses, String language, Integer firstResult, Integer maxResults);

    /**
     * changes task status :  Reserved --> Completed
     *
     * <pre>
     * NOTE : it's expected that implementations of this operation will actually invoke 2 WS-HT based operations in this single operation :
     *  1)  TaskServiceSession.start(...)       :   changes status from Reserved --> InProgress 
     *  2)  TaskServiceSession.complete(...)    :   changes status from InProgress --> Completed
     *
     *  please see the following section WS-HT specification:  4.7.1  Normal Processing of a Human Task
     *
     *  implementations of this function will invoke IKnowledgeSessionService.completeWorkItem(...) to continue process instance execution
     *  
     *  most remote clients will leverage the method signature that does not include the 'disposeKsession' parameter
     * </pre>
     */
    public void completeTask(Long taskId, Map<String, Object> outboundTaskVars, String userId);
    
    /**
     * delegateTask
     * places task back to a status of "Ready" to be subsequently claimed by "targetUserId"
     * as per WS-HT spec, original status of task can be either Ready, Reserved or InProgress
     * as the task remains in an 'active' state, the the work flow branch that this task is a part of does not continue
     * @param taskId
     * @param userId
     * @param targetUserId
     */
    public void delegateTask(Long taskId, String userId, String targetUserId);

    /**
     * failTask
     * places task in a status of "Failed" and continues process instance execution
     * as per WS-HT specification, section 4.7 ,  task status must already be "InProgress" for this operation to be valid
     * most remote clients will leverage the method signature that does not include the 'disposeKsession' parameter
     * @param taskId
     * @param outboundTaskVars
     * @param userId
     * @param faultName
     * @param disposeKsession
     * 
     *
     * NOTE:  allows for a null userId ... in which case this implementation will use the actual owner of the task to execute the fail task operation
     */
    public void failTask(Long taskId, Map<String, Object> outboundTaskVars, String userId);

    /**
     * 
     * @param taskId
     * @return Externalizable  :  specifically, instance of:  org.jbpm.task.query.TaskSummary
     */
    public Externalizable getTask(Long taskId);

    /**
     * Note:  returns a Hibernate lazy-loaded entity.  Subsequently client to this function
     * should use the Task entity within the same transaction used to invoke this function
     * @param workItemId
     * @return
     */
    public Task getTaskByWorkItemId(Long workItemId);

    /**
     * skipTask
     * places task in a status of "Obsolete" and continues execution of work flow branch that this task is a part of
     * <pre>
     * NOTE:  underlying jbpm5 TaskServiceSession does not allow for outbound task variables with Operation.Skip
     *        will still use the "outboundTaskVars" passed in this function to populate process instance variables with completeWorkItem() invocation
     * </pre>
     */
    public void skipTask(Long taskId, String userId, Map<String, Object> outboundTaskVars);
    
    /**
     * invoked internally be PFPAddHumanTaskHandler
     * will place task in status of Obsolete
     * @param workItemId
     */
    public void skipTaskByWorkItemId(Long workItemId);

    /**
     * changes task status :  Reserved --> InProgress
     */
    public void startTask(Long taskId, String userId);
    
    
    /**
     * sets list of potentialOwners to an existing task
     * <pre>
     * provides similar functionality to section 4.7.3 of WS-HT
     * however, jbpm5 TaskServiceSession does not seem to change task state as described in 4.7.3
     * </pre>
     */
    public void nominateTask(final long taskId, String userId, final List<OrganizationalEntity> potentialOwners);

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language);

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language, Integer firstResult, Integer maxResults);
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> statuses, String language, Integer firstResult, Integer maxResults);
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> statuses, String language, Integer firstResult, Integer maxResults);

    public List<Long> getTasksByProcessInstance(Long processInstanceId);


    /**
     * 
     * @param taskId
     * @param contentType options:
     *         ITaskService.DOCUMENT_CONTENT
     *         ITaskService.OUTBOUND_CONTENT
     *         ITaskService.FAULT_CONTENT
     * @return Map<String,Object> contentData
     */
    public Map<String,Object> getTaskContent(Long taskId, String contentType);
    
    public void addOutboundContent(Task taskObj, Map<String, Object> taskContent);
    
    
    /**
     * printTaskContent
     * @param taskId taskId
     * @param contentType options:
     *         ITaskService.DOCUMENT_CONTENT
     *         ITaskService.OUTBOUND_CONTENT
     *         ITaskService.FAULT_CONTENT
     * @return
     */
    public String printTaskContent(Long taskId, String contentType);

    public String getTaskName(Long taskId, String language);
    public List<TaskSummary> getTasksOwned(String userId, List<Status> statuses, String language);
    public void releaseTask(Long taskId, String userId);

}
