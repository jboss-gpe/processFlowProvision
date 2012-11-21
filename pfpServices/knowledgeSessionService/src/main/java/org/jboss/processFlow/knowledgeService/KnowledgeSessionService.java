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
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.drools.definition.process.Process;
import org.drools.io.*;
import org.drools.runtime.process.ProcessInstance;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceMXBean;

/**
 *<pre>
 *architecture
 *  - this singleton utilizes a 'processInstance per knowledgeSession' architecture
 *  - although the jbpm5 API technically allows for a StatefulKnowledgeSession to manage the lifecycle of multiple process instances,
 *      we choose not to have to deal with optimistic lock exception handling (in particular with the sessionInfo) during highly concurrent environments
 *
 *
 *
 *Drools knowledgeBase management
 *  - this implementation instantiates a single instance of org.drools.KnowledgeBase
 *  - this KnowledgeBase is kept current by interacting with a remote BRMS guvnor service
 *  - note: this KnowledgeBase instance is instantiated the first time any IKnowledgeSessionService operation is invoked
 *  - the KnowledgeBase is not instantiated in a start() method because the BRMS guvnor may be co-located on the same jvm
 *      as this KnowledgeSessionService and may not yet be available (depending on boot-loader order)
 *      
 *
 *WorkItemHandler Management
 *  - Creating & configuring custom work item handlers in PFP is almost identical to creating custom work item handlers in stock BRMS
 *     - Background Documentation :       12.1.3  Registering your own service handlers
 *      - The following are a few processFlowProvision additions :
 *
 *       1)  programmatically registered work item handlers
 *         -- every StatefulKnowledgeSession managed by the processFlowProvision knowledgeSessionService is automatically registered with
 *
 *          the following workItemHandlers :
 *           1)  "Human Task"    :   org.jboss.processFlow.tasks.handlers.PFPAddHumanTaskHandler
 *           2)  "Skip Task"     :   org.jboss.processFlow.tasks.handlers.PFPSkipTaskHandler
 *           3)  "Fail Task"     :   org.jboss.processFlow.tasks.handlers.PFPFailTaskHandler
 *           4)  "Email"         :   org.jboss.processFlow.tasks.handlers.PFPEmailWorkItemHandler
 *
 *      2)  defining configurable work item handlers
 *        -- jbpm5 allows for more than one META-INF/drools.session.conf in the runtime classpath
 *          -- subsequently, there is the potential for mulitple locations that define custom work item handlers
 *         -- the ability to have multiple META-INF/drools.session.conf files on the runtime classpath most likely will lead to
 *               increased difficulty isolating problems encountered with defining and registering custom work item handlers
 *        -- processFlowProvision/build.properties includes the following property:  space.delimited.workItemHandler.configs
 *         -- rather than allowing for multiple locations to define custom work item handlers,
 *               use of the 'space.delimited.workItemHandler.configs' property centralalizes where to define additional custom workItemHandlers
 *         -- please see documentation provided for that property in the build.properties
 *
 *
 *
 *
 *notes on Transactions
 *  - most publicly exposed methods in this singleton assumes a container managed trnx demarcation of REQUIRED
 *  - in some methods, bean managed transaction demarcation is used IOT dispose of the ksession *AFTER* the transaction has committed
 *  - otherwise, the method will fail due to implementation of JBRULES-1880
 *
 *
 *processEventListeners
 *      - ProcessEventListeners get registered with the knowledgeSession/processEngine
 *      - when any of the corresponding events occurs in the lifecycle of a process instance, those processevent listeners get invoked
 *      - a configurable list of process event listeners can be registered with the process engine via the following system prroperty:
 *          IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS
 *
 *      - in processFlowProvision, we have two classes that implement org.drools.event.process.ProcessEventListener :
 *          1)  the 'busySessionsListener' inner class constructed in this knowledgeSessionService    
 *              -- used to help maintain our ksessionid state
 *              -- a new instance is automatically registered with a ksession with new ksession creation or ksession re-load
 *          2)  org.jboss.processFlow.bam.AsyncBAMProducer
 *              -- sends BAM events to a hornetq queue
 *              -- registered by including it in IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS system property
 *
 *
 *BAM audit logging
 *  - this implementation leverages a pool of JMS producers to send BAM events to a JMS provider
 *  - a corresponding BAM consumer receives those BAM events and persists to the BRMS BAM database
 *  - it is possible to disable the production of BAM events by NOT including 'org.jboss.processFlow.bam.AsyncBAMProducer' as a value
 *    in the IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS property
 *  - note:  if 'org.jboss.processFlow.bam.AsyncBAMProducer' is not included, then any clients that query the BRMS BAM database will be affected
 *  - an example is the BRMS gwt-console-server
 *      the gwt-console-server queries the BRMS BAM database for listing of active process instances
 *      
 *     
 *      
 *ksession management
 *  - in this IKnowledgeSessionService implementation, a ksessionId is allocated to a process instance (and any subprocesses) for its entire lifecycle 
 *  - upon completion of a process instance, the ksessionId is made available again for a new process instance
 *  - this singleton utilizes two data structures, busySessions & availableSessions, to maintain which ksessionIds are available for reuse
 *  - a sessioninfo record in the jbpm database corresponds to a single StatefulKnowledgeSession
 *  - a sessioninfo record typically includes the state of :
 *          * timers
 *          * business rule data
 *          * business rule state
 *  - a sessioninfo record is never purged from the database ... in this implementation it is simply re-cycled for use by a new process instance
 *  - ksessionId state :
 *      - some of the public methods implemented by this bean take both a 'processInstanceId' and a 'ksessionId' as a parameter
 *      - for the purposes of this implementation, the 'ksessionId' is always optional 
 *          if null is passed to any of the methods accepting a ksessionid, then this implementation will query the jbpm5 task table
            to determine the mapping between processInstanceId and ksessionId
 *
 *  TO-DO :  prevent potential optimisticlock exception scenarios when invoking 'abortProcess', 'signalEvent', etc
 *      see comments on loadStatefulKnowledgeSession(...) method
 *
 *
 *</pre>
 */
@Remote(IKnowledgeSessionService.class)
@Local(IBaseKnowledgeSession.class)
@Singleton(name="prodKSessionProxy")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KnowledgeSessionService implements IKnowledgeSessionService, KnowledgeSessionServiceMXBean {
    
    @Inject
    private IKnowledgeSessionBean kBean;
    
    protected ObjectName objectName;
    protected MBeanServer platformMBeanServer;
    
    @PostConstruct
    public void start() throws Exception {
        try {
            objectName = new ObjectName("org.jboss.processFlow:type="+this.getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
    
    @PreDestroy 
    public void stop() throws Exception{
        try {
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new RuntimeException("Problem during unregistration of Monitoring into JMX:" + e);
        }
    }
    
    public void createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder() {
        kBean.createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder();
    }
    public void rebuildKnowledgeBaseViaKnowledgeAgent() throws ConnectException{
        kBean.rebuildKnowledgeBaseViaKnowledgeAgent();
    }
    public void rebuildKnowledgeBaseViaKnowledgeBuilder() {
        kBean.rebuildKnowledgeBaseViaKnowledgeBuilder();
    }
    public void addProcessToKnowledgeBase(Process processObj, Resource resourceObj) {
        kBean.addProcessToKnowledgeBase(processObj, resourceObj);
    }
    public void addProcessToKnowledgeBase(File bpmnFile) {
        kBean.addProcessToKnowledgeBase(bpmnFile);
    }
    public String getAllProcessesInPackage(String pkgName){
        return kBean.getAllProcessesInPackage(pkgName);
    }
    public String printKnowledgeBaseContent() {
        return kBean.printKnowledgeBaseContent();
    }
    public String printWorkItemHandlers() { 
        return kBean.printWorkItemHandlers();
    }
    public List<SerializableProcessMetaData> retrieveProcesses() throws Exception {
        return kBean.retrieveProcesses();
    }
    public SerializableProcessMetaData getProcess(String processId) {
        return kBean.getProcess(processId);
    }
    public void removeProcess(String processId) {
        throw new UnsupportedOperationException();
    }
    public List<ProcessInstance> getActiveProcessInstances(Map<String, Object> queryCriteria) {
        return kBean.getActiveProcessInstances(queryCriteria);
    }

    /*
     *  disposeStatefulKnowledgeSessionAndExtras
     *<pre>
     *- disposes of a StatefulKnowledgeSession object currently in use
     *- NOTE:  can no longer dispose knowledge session within scope of a transaction due to side effects from fix for JBRULES-1880
     *</pre>
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void disposeStatefulKnowledgeSessionAndExtras(Integer sessionId) {
        kBean.disposeStatefulKnowledgeSessionAndExtras(sessionId);
    }
    
    public String dumpSessionStatusInfo() {
        return kBean.dumpSessionStatusInfo();
    }

    public String dumpBAMProducerPoolInfo() {
        return kBean.dumpBAMProducerPoolInfo();
    }
    
    

 
/******************************************************************************
 *************              Process Instance Management              *********/
    
    /**
     *startProcessAndReturnId
     *<pre>
     *- this method will block until the newly created process instance either completes or arrives at a wait state
     *- at completion of the process instance (or arrival at a wait state), the StatefulKnowledgeSession will be disposed
     *- bean managed transaction demarcation is used by this method IOT dispose of the ksession *AFTER* the transaction has committed
     *- otherwise, this method will fail due to implementation of JBRULES-1880
     *</pre>
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> parameters) {
        return kBean.startProcessAndReturnId(processId, parameters);
    }
    public void completeWorkItem(Integer ksessionId, Long workItemId, Map<String, Object> pInstanceVariables) {
        kBean.completeWorkItem(ksessionId, workItemId, pInstanceVariables);
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void signalEvent(String signalType, Object signalValue, Long processInstanceId, Integer ksessionId) {
        kBean.signalEvent(signalType, signalValue, processInstanceId, ksessionId);
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void abortProcessInstance(Long processInstanceId, Integer ksessionId) {
        kBean.abortProcessInstance(processInstanceId, ksessionId);
    }
    public String printActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId) {
        return kBean.printActiveProcessInstanceVariables(processInstanceId, ksessionId);
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) 
    public Map<String, Object> getActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId) {
        return kBean.getActiveProcessInstanceVariables(processInstanceId, ksessionId);
    }
    public void setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables, Integer ksessionId) {
        kBean.setProcessInstanceVariables(processInstanceId, variables, ksessionId);
    }
}