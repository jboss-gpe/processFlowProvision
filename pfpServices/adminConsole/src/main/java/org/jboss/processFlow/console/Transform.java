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

import org.drools.definition.process.Process;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.model.ProcessInstanceRef;
import org.jboss.bpm.console.client.model.TaskRef;
import org.jboss.bpm.console.client.model.TokenReference;
import org.jboss.processFlow.knowledgeService.SerializableProcessMetaData;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.task.I18NText;
import org.jbpm.task.Task;
import org.jbpm.task.query.TaskSummary;

public class Transform {

        /**
         * JA Bride:  modified to accept Map rather than Process (which may not be Serializable
         *             which will cause issues between EJB remoting layers
         */
    public static ProcessDefinitionRef processDefinition(SerializableProcessMetaData pMetaData) {
        long version = pMetaData.getProcessVersion();
        ProcessDefinitionRef result = new ProcessDefinitionRef(pMetaData.getProcessId(),pMetaData.getProcessName(), version);
        result.setPackageName(pMetaData.getPackageName());
        result.setDeploymentId("N/A");
        return result;
    }
    
    public static ProcessInstanceRef processInstance(ProcessInstanceLog processInstance) {
        ProcessInstanceRef result = new ProcessInstanceRef(
            processInstance.getProcessInstanceId() + "",
            processInstance.getProcessId(),
            processInstance.getStart(),
            processInstance.getEnd(),
            false);
        TokenReference token = new TokenReference(
            processInstance.getProcessInstanceId() + "", null, "");
        result.setRootToken(token);
        return result;
    }
    
    public static TaskRef task(TaskSummary task) {
        return new TaskRef(
            task.getId(),
            Long.toString(task.getProcessInstanceId()),
            "",
            task.getName(),
            task.getActualOwner() == null ? null : task.getActualOwner().getId(),
            false,
            false);
    }

    public static TaskRef task(Task task) {
        String name = "";
        for (I18NText text: task.getNames()) {
            if ("en-UK".equals(text.getLanguage())) {
                name = text.getText();
            }
        }
        return new TaskRef(
            task.getId(),
            Long.toString(task.getTaskData().getProcessInstanceId()),
            "",
            name,
            task.getTaskData().getActualOwner() == null ? null : task.getTaskData().getActualOwner().getId(),
            false,
            false);
    }

}
