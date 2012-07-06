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

import java.io.File;

import java.util.Map;
import java.util.List;

import org.drools.definition.process.Process;
import org.drools.io.Resource;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.process.audit.ProcessInstanceLog;

/**
 *
 *<b>Responsibilities</b>
 *<pre>
 * StatefulKnowledgeSession management
 *   - implementations of this interface manage the lifecycle of one or more org.drools.runtime.StatefulKnowledgeSession objects
 *   - these StatefulKnowledgeSession objects implement org.drools.runtime.process.ProcessRuntime
 *   - subsequently, this interface exposes various ProcessRuntime derived operations as an EJB 'service'
 *   - ksessionId state :
 *      - some of the methods exposed by this interface take both a 'processInstanceId' and a  'ksessionId' as a parameter
 *      - the 'ksessionId' is optional depending on several considerations:
 *          - for an implementation that only maintains one StatefulKnowledgeSession (ie: similar to CommandDelegate of jbpm5 gwt-console-server)
 *            the ksessionId is irrelevant
 *          - for an implementation that assigns a StatefulKnowledgeSession to a single process instance or a group of process instances,
 *            that implementation should maintain a mapping of ksessionIds to processInstanceIds.  If null is passed to any of the methods
 *            accepting a ksessionId, then the implementation should execute a lookup for ksessionId.
 *
 * KnowledgeBase management
 *   - implementations of this interface will typically manage the lifecycle of an org.drools.KnowledgeBase
 *   - an implementation will use this KnowledgeBase object to create/load the runtime StatefulKnowledgeSession objects
 *   - the KnowledgeBase object is a repository of all knowledge definitions to include :  rules, processes, functions and type models
 *   - this KnowledgeBase object is typically kept current by interacting with a remote BRMS guvnor service
 *   - the remote BRMS guvnor service is the actual 'system-of-record' of knowledge definitions
 *
 * Business Activity Monitoring (BAM) audit logging
 *   - implementations of this may or may not feed a BAM data warehouse of process instance events
 *</pre>
 */
public interface IKnowledgeSessionService extends IBaseKnowledgeSessionService {
    public static final String KNOWLEDGE_SESSION_SERVICE_JNDI = "ejb:pfp/processFlow-knowledgeSessionService//prodKSessionProxy!org.jboss.processFlow.knowledgeService.IKnowledgeSessionService";
    public static final String KNOWLEDGE_SERVICE_PROVIDER_URL = "org.jboss.processFlow.knowledgeService.KNOWLEDGE_SERVICE_PROVIDER_URL";
    public static final String SPACE_DELIMITED_PROCESS_EVENT_LISTENERS = "space.delimited.process.event.listeners";
    public static final String TASK_CLEAN_UP_PROCESS_EVENT_LISTENER_IMPL="task.clean.up.process.event.listener.impl";
    public static final String PROCESS_ID = "processid";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String KSESSION_ID = "ksessionId";
    public static final String EMAIL = "Email";

    /**
     * printWorkItemHandlers
     * <pre>
     * returns a listing of registered workItemHandlers with knowledgeSessions
     * will include workItemHandlers loaded programmatically and via configuration
     * </pre>
     */
    public String printWorkItemHandlers();

    /**
     * Signals the process instance that an event has occurred. The type parameter defines
     * which type of event and the event parameter can contain additional information
     * related to the event.  All node instances inside the given process instance that
     * are listening to this type of (internal) event will be notified.  Note that the event
     * will only be processed inside the given process instance.  All other process instances
     * waiting for this type of event will not be notified.
     *
     * @param type the type of event
     * @param event the data associated with this event
     * @param processInstanceId the id of the process instance that should be signaled
     * @param ksessionId the id of the KnowledgeSession that is managing the lifecycle of the process instance
     */
    public void signalEvent(String type, Object event, Long processInstanceId, Integer ksessionId);


    /**
     * Aborts the process instance with the given id.  If the process instance has been completed
     * (or aborted), or the process instance cannot be found, this method will throw an
     * <code>IllegalArgumentException</code>.
     *
     * @param id the id of the process instance
     * @param ksessionId the id of the KnowledgeSession that is managing the lifecycle of the process instance
     */
    public void abortProcessInstance(Long processInstanceId, Integer ksessionId);

    /**
     * refreshKnowledgeBase and knowledgeAgent managed by PFP knowledgeSessionService
     * use in conjunction with various guvnor.* properties include in META-INF/jbpm-console.properties of the knowledgeSessionService implementation artifact
     *
     * should see a similar log statement from guvnor as follows:
     *      INFO  [PackageAssembler] Following assets have been included in package build: simpleHumanTask, defaultemailicon, defaultlogicon, WorkDefinitions, pfpFailTask, pfpSkipTask, task_skip_by_signalIntermediateEvent, simpleTask-taskform, nominateAndAwardBonusTask-taskform, simpleTask-image, pInstance_terminate_by_signalIntermediateEvent
     */
    public void rebuildKnowledgeBaseViaKnowledgeAgent();

    /**
     * rebuild KnowledgeBase via instantiation of a KnowledgeBuilder
     * use in conjunction with various guvnor.* properties include in META-INF/jbpm-console.properties of the knowledgeSessionService implementation artifact
     *
     * should see a similar log statement from guvnor as follows:
     *      INFO  [PackageAssembler] Following assets have been included in package build: simpleHumanTask, defaultemailicon, defaultlogicon, WorkDefinitions, pfpFailTask, pfpSkipTask, task_skip_by_signalIntermediateEvent, simpleTask-taskform, nominateAndAwardBonusTask-taskform, simpleTask-image, pInstance_terminate_by_signalIntermediateEvent
     */
    public void rebuildKnowledgeBaseViaKnowledgeBuilder();
    /**
     *return a snapshot of all process definitions that the KnowledgeBase is currently aware of
     */
    public String printKnowledgeBaseContent();
    
    /**
     * Uses GuvnorConnectionUtils to query guvnor for 'assets' of a particular package using the following URL convention:
     * <guvnor.protocol>://<guvnor.host>/<guvnorsubdomain>/rest/packages/<guvnor.package>/assets
     *
     */
    public String getAllProcessesInPackage(String pkgName);

    /**
     *retrieve a list of all Process definition objects that the KnowledgeBase is currently aware of
     */
    public List<Process> retrieveProcesses() throws Exception ;

    public void addProcessToKnowledgeBase(Process processObj, Resource resourceObj);

    public void addProcessToKnowledgeBase(File bpmnFile);

    /**
     *getActiveProcessInstances
     *<pre>
     *given an optional Map of query criteria, return a List of ProcessInstance objects
     *currently, only one type of query criteria is supported and is keyed by PROCESS_ID
     *</pre>
     */
    public List<ProcessInstance> getActiveProcessInstances(Map<String,Object> queryCriteria);

    public Process                  getProcess(String processId);
    public Process                  getProcessByName(String name) throws Exception;
    public void                     removeProcess(String processId);
    
    public String                     printActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId);
    public Map<String, Object>      getActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId);
    public void                     setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables, Integer ksessionId );

    /**
     * returns a snapshot of all KnowledgeSessions and the state that each session is currently in 
     */
    public String                   dumpSessionStatusInfo();

    /**
     * knowledgeSessionService may have a process event listener that sends events asynchroneously to a message broker
     * these events will subsequently be stored in a business activity monitoring data wharehouse for future analysis
     * this function lists the # of active and idle producers from a pool of JMS producers
     */  
    public String                   dumpBAMProducerPoolInfo();

}
