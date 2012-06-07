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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.process.workitem.wsht.HumanTaskHandlerHelper;
import org.jbpm.task.AccessType;
import org.jbpm.task.Group;
import org.jbpm.task.I18NText;
import org.jbpm.task.OrganizationalEntity;
import org.jbpm.task.PeopleAssignments;
import org.jbpm.task.SubTasksStrategy;
import org.jbpm.task.SubTasksStrategyFactory;
import org.jbpm.task.Task;
import org.jbpm.task.TaskData;
import org.jbpm.task.User;
import org.jbpm.task.service.ContentData;

/*
 * 15 Sept 2011
 * Purpose :
 *  - create a new Task object and invoke the taskService.addTask(...) method
 *
 * invocation and lifecycle :
 *  - this.executeWorkItem() is invoked by the JPAWorkIteManager
 *  - an instance of this class shares the same lifecycle as its corresponding knowledge session and workItemManager
 *
 *
 * deadlines
 *  - this custom work item handler assumes a process instance variable of 'Deadline_Time' of type String
 *  - if 'Deadline_Time' is set, this custom work item handler will create a new Deadline with Escalation and flush to database
 */
public class PFPAddHumanTaskHandler extends BasePFPTaskHandler implements WorkItemHandlerLifecycle {

    public static final Logger log = Logger.getLogger("PFPAddHumanTaskHandler");

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String taskName = (String) workItem.getParameter("TaskName");
        
        // 1)  create a Task instance from this workItem instance
        Task task = new Task();
        if (taskName != null) {
            List<I18NText> names = new ArrayList<I18NText>();
            names.add(new I18NText("en-UK", taskName));
            task.setNames(names);
        }
        String comment = (String) workItem.getParameter("Comment");
        if (comment != null) {
            List<I18NText> descriptions = new ArrayList<I18NText>();
            descriptions.add(new I18NText("en-UK", comment));
            task.setDescriptions(descriptions);
            List<I18NText> subjects = new ArrayList<I18NText>();
            subjects.add(new I18NText("en-UK", comment));
            task.setSubjects(subjects);
        }
        String priorityString = (String) workItem.getParameter("Priority");
        int priority = 0;
        if (priorityString != null) {
            try {
                priority = new Integer(priorityString);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        task.setPriority(priority);
        
        
        TaskData taskData = new TaskData();
        taskData.setWorkItemId(workItem.getId());
        taskData.setProcessInstanceId(workItem.getProcessInstanceId());
        taskData.setSkipable(!"false".equals(workItem.getParameter("Skippable")));
        taskData.setProcessSessionId(ksessionId);
        //Sub Task Data
        Long parentId = (Long) workItem.getParameter("ParentId");
        if(parentId != null){
            taskData.setParentId(parentId);
        }

        String subTaskStrategiesCommaSeparated = (String) workItem.getParameter("SubTaskStrategies");
        if(subTaskStrategiesCommaSeparated!= null && !subTaskStrategiesCommaSeparated.equals("")){
            String[] subTaskStrategies =  subTaskStrategiesCommaSeparated.split(",");
            List<SubTasksStrategy> strategies = new ArrayList<SubTasksStrategy>();
            for(String subTaskStrategyString : subTaskStrategies){
                SubTasksStrategy subTaskStrategy = SubTasksStrategyFactory.newStrategy(subTaskStrategyString);
                strategies.add(subTaskStrategy);
            }
            task.setSubTaskStrategies(strategies);
        }

        PeopleAssignments assignments = new PeopleAssignments();
        List<OrganizationalEntity> potentialOwners = new ArrayList<OrganizationalEntity>();

        String actorId = (String) workItem.getParameter("ActorId");
        if (actorId != null && actorId.trim().length() > 0) {
            String[] actorIds = actorId.split(",");
            for (String id: actorIds) {
                potentialOwners.add(new User(id.trim()));
            }
            //Set the first user as creator ID??? hmmm might be wrong
            if (potentialOwners.size() > 0){
                taskData.setCreatedBy((User)potentialOwners.get(0));
            }
        }
        
        String groupId = (String) workItem.getParameter("GroupId");
        if (groupId != null && groupId.trim().length() > 0) {
            String[] groupIds = groupId.split(",");
            for (String id: groupIds) {
                potentialOwners.add(new Group(id.trim()));
            }
        }

        assignments.setPotentialOwners(potentialOwners);
        List<OrganizationalEntity> businessAdministrators = new ArrayList<OrganizationalEntity>();
        
        // JA Bride :  ITaskService.ADMINISTRATOR being added as business administrator to allow for subsequent operations on the task if need be as per :
        //      jbpm-human-task/src/main/resources/org/jbpm/task/service/operations-dsl.mvel
        businessAdministrators.add(new User(ITaskService.ADMINISTRATOR));
        assignments.setBusinessAdministrators(businessAdministrators);
        task.setPeopleAssignments(assignments);
        
        task.setDeadlines(HumanTaskHandlerHelper.setDeadlines(workItem, businessAdministrators));
        
        task.setTaskData(taskData);

        // grab existing content from workItem or initialize empty content HashMap if doesn't exist in workItem
        ContentData content = null;
        
        // Nick: retrieve all parameters as the input contentData
        HashMap<String, Object> contentObject = new HashMap<String, Object>();
        contentObject.putAll(workItem.getParameters());

        Map<String, Object> contentMap = (HashMap<String, Object>) workItem.getParameter("Content");
        if(contentMap == null) {
            if(enableLog) {
                log.warn("executeWorkItem() processInstance-->task variable 'Content' is null for workItemId = "+workItem.getId()+ "\n\tthis could occur for either one (or both) of the following reasons\n\t\t1)  upstream to this workItem, a knowledge context variable of type java.util.HashMap with key = 'map' was not set.\n\t\t2)  Parameter Mapping of 'Content=map' was not set in this task node in the process definition \n\twill initialize an empty hashmap and use this empty hashmap when invoking TaskServiceSession.addTask()");
            }
            //Jeff: the FreeMarker templates used in the gwt-console use it and if that hash doesn't exist and/or it doesn't have the right key/values in it
            contentMap = new HashMap<String, Object>();
        } else {
            if(enableLog) {
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append("executeWorkItem() processInstance-->task variables as follows for pInstanceId = ");
                sBuilder.append(workItem.getProcessInstanceId());
                sBuilder.append(" : taskName = ");
                sBuilder.append(taskName);
                for (Map.Entry<String, Object> entry: contentMap.entrySet()) {
                    sBuilder.append("\n\t");
                    sBuilder.append(entry.getKey());
                    sBuilder.append(" : ");
                    sBuilder.append(entry.getValue());
                }
                log.info(sBuilder.toString());
            }
        }

        // Nick: use "Content" anyway if it is defined
        contentObject.putAll(contentMap);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(contentObject);
            out.close();
            content = new ContentData();
            content.setContent(bos.toByteArray());
            content.setAccessType(AccessType.Inline);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null)
                    out.close();
            } catch(Exception x){
                x.printStackTrace();
            }
        }

        task.setDeadlines(HumanTaskHandlerHelper.setDeadlines(workItem, businessAdministrators));
        try {
            // 5)  synch call to task server to add newly created task
        	long taskId = taskProxy.addTask(task, content);	
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    	log.error("abortWorkItem() workItemId = "+workItem.getId() +" :  workItemName = "+workItem.getName());
    	taskProxy.skipTaskByWorkItemId(workItem.getId());
    }
}
