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

package org.jboss.processFlow.knowledgeService;

import java.util.Map;

public interface IBaseKnowledgeSessionService {

    /**
     *Given the id of a process definition and a Map of process instance variables, start a process instance.
     *<pre>
     *This method will block until the new process instance either completes or reaches a safe point (ie:  human task).
     *returns a Map with the following content :
     *  - IKnowledgeSessionService.PROCESS_INSTANCE_ID  / java.lang.Long
     *  - IKnowledgeSessionService.KSESSION_ID          / java.lang.Integer
     *</pre>
     */
    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> parameters) throws Exception;


    /**
     * complete a workItem.
     *<pre>
     *Completion of a work item signals to a StatefulKnowledgeSession to continue to the next node of the process instance.
     *This method will block until the new process instance either completes or reaches a safe point (ie:  another human task).
     
     *This method is available to remote services that were involved in the processing of an 'asynchroneous' workItemHandler.
     *One example of a service commonly invoking this method is org.jboss.processFlow.tasks.HumanTaskService. 
     *  during execution of its 'completeTask(...), this method is subsequently invoked
    </pre>
     * @param ksessionId the id of the KnowledgeSession that is managing the lifecycle of the process instance
     * @param workItemId the id of the workItem that has completed
     * @param pInstanceVariables Map of any parameter results to be passed to process instance 
     */
    public void completeWorkItem(Integer ksessionId, Long workItemId, Map<String, Object> pInstanceVariables);

    public void disposeStatefulKnowledgeSessionAndExtras(Integer sessionId);
}
