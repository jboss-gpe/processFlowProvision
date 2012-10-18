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

package org.jboss.processFlow.console;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.security.Principal;
import java.security.acl.Group;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;

import javax.naming.InitialContext;

import org.jboss.bpm.console.client.model.TaskRef;
import org.jboss.processFlow.console.binding.DataBinderManager;
import org.jboss.processFlow.console.binding.IDataBinder;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.util.PFPServicesLookupUtil;
import org.jbpm.task.Status;
import org.jbpm.task.User;
import org.jbpm.task.query.TaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/*
 *  14 Nov 2011 :  what is the proper way to forward a user friendly message to the gwt javascript during exception handling ?
        -- will throw RuntimeExcetions until this is figured out
 */
public class TaskManagement implements org.jboss.bpm.console.server.integration.TaskManagement {
    
    private static Logger log = LoggerFactory.getLogger(TaskManagement.class);
    private static ITaskService taskServiceProxy = null;
    private DataBinderManager dataBinderManager;

    public TaskManagement() {
        dataBinderManager = new DataBinderManager();
        taskServiceProxy = PFPServicesLookupUtil.getTaskProxy();
    }

    public TaskRef getTaskById(long taskId) {
        try {
            TaskSummary task = taskServiceProxy.getTask(taskId);
            return Transform.task(task);
        }catch(javax.persistence.EntityNotFoundException x){
            throw new RuntimeException(x);
        }
    }

    public void assignTask(long taskId, String idRef, String userId) {
        if (idRef == null) {
            taskServiceProxy.releaseTask(taskId, userId);
        } else if (idRef.equals(userId)) {
            List<String> callerRoles = getCallerRoles();
            taskServiceProxy.claimTask(taskId, idRef, idRef, callerRoles);
        } else {
            taskServiceProxy.delegateTask(taskId, userId, idRef);
        }
    }

    @SuppressWarnings("unchecked")
    public void completeTask(long taskId, Map data, String userId) {
        try {
            // 1. get the task object to retrieve the taskName
            String taskName = taskServiceProxy.getTaskName(taskId, "en-UK");

            // 2. do data binding for the complex objects in the <code>data</code> map
            IDataBinder dataBinder = dataBinderManager.getDataBinder(taskName);
            if (dataBinder != null) {
                dataBinder.bind(data);
            }

            taskServiceProxy.completeTask(taskId, data, userId);
        }
        catch(Exception x){
            throw new RuntimeException(x);
        }
    }

    @SuppressWarnings("unchecked")
    public void completeTask(long taskId, String outcome, Map data, String userId) {
        data.put("outcome", outcome);
        completeTask(taskId, data, userId);
    }

    public void releaseTask(long taskId, String userId) {
        // TODO: this method is not being invoked, it's using
        // assignTask with null parameter instead
        taskServiceProxy.releaseTask(taskId, userId);
    }

    public List<TaskRef> getAssignedTasks(String idRef) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        try {
            List<Status> onlyReserved = Collections.singletonList(Status.Reserved);
            List<TaskSummary> tasks = taskServiceProxy.getAssignedTasks(idRef, onlyReserved, "en-UK");
            for (TaskSummary task: tasks) {
                result.add(Transform.task(task));
            }
            return result;
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }

    public List<TaskRef> getUnassignedTasks(String userId, String participationType) {
        List<TaskRef> result = new ArrayList<TaskRef>();
        List<TaskSummary> tasks = null;
        try {
            List<String> callerRoles = getCallerRoles();
            List<Status> onlyReady = Collections.singletonList(Status.Ready);
            
            tasks = taskServiceProxy.getTasksAssignedAsPotentialOwnerByStatusByGroup(userId, callerRoles, onlyReady, "en-UK", 0, 10);
            User emptyUser = new User();
            for (TaskSummary task: tasks) {
                
                /* JA Bride : 21 Sept 2012  : as per
                    1)  org.jboss.bpm.console.client.task.OpenTasksView.renderUpdate(...)
                    2)  org.jboss.bpm.console.client.model.TaskRef.initOrUpdateState(...)
                       actualOwner needs to be either null or empty value 
                */
                task.setActualOwner(emptyUser);
                result.add(Transform.task(task));
            }
            return result;
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }

    private List<String> getCallerRoles() {
        List<String> roles = null;
        try {
            Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");

            if (subject != null) {
                Set<Principal> principals = subject.getPrincipals();

                if (principals != null) {
                    roles = new ArrayList<String>();
                    for (Principal principal : principals) {
                        log.info("principal = "+principal);
                        if (principal instanceof Group  && "Roles".equalsIgnoreCase(principal.getName())) {
                            Enumeration<? extends Principal> groups = ((Group) principal).members();

                            while (groups.hasMoreElements()) {
                                Principal groupPrincipal = (Principal) groups.nextElement();
                                roles.add(groupPrincipal.getName());

                            }
                            break;
                        }

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return roles;
    }

}
