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

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.persistence.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.drools.SystemEventListenerFactory;
import org.drools.core.util.DelegatingSystemEventListener;
import org.drools.KnowledgeBaseFactory;
import org.drools.agent.impl.PrintStreamSystemEventListener;
import org.drools.command.SingleSessionCommandService;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.common.AbstractWorkingMemory;
import org.drools.common.InternalKnowledgeRuntime;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.io.*;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.persistence.jpa.JpaJDKTimerService;
import org.drools.persistence.jpa.processinstance.JPAWorkItemManager;
import org.drools.persistence.jpa.processinstance.JPAWorkItemManagerFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.Environment;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemHandler;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;
import org.jbpm.integration.console.shared.GuvnorConnectionUtils;
import org.jbpm.task.admin.TaskCleanUpProcessEventListener;
import org.jbpm.task.admin.TasksAdmin;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;

import org.jboss.processFlow.bam.IBAMService;
import org.jboss.processFlow.bam.AsyncBAMProducerPool;
import org.jboss.processFlow.bam.AsyncBAMProducer;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.util.LogSystemEventListener;

/**
 *<pre>
 *architecture
 *  - this singleton utilizes a 'processInstance per knowledgeSession' architecture
 *  - although the jbpm5 API technically allows for a StatefulKnowledgeSession to manage the lifecycle of multiple process instances,
 *      we choose not to have to deal with optimistic lock exception handling (in particular with the sessionInfo) during highly concurrent environments
 *
 *notes on Transactions
 *  - most publicly exposed methods in this singleton assumes a container managed trnx demarcation of REQUIRED
 *  - in some methods, bean managed transaction demarcation is used IOT dispose of the ksession *AFTER* the transaction has committed
 *  - otherwise, the method will fail due to implementation of JBRULES-1880
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
 *          to determine the mapping between processInstanceId and ksessionId
 *  - this implementation is ideal in a multi-thread, concurrent client environment where the following is either met or is acceptable:
 *      1)  process definitions do include rule data
 *      2)  from a performance perspective, it's critical that process instance lifecycle functions are executed in parallel rather than synchroneously
 *          NOTE:  see org.drools.persistence.SingleSessionCommandService.execute(...) function
 *
 *</pre>
 */

@ApplicationScoped
@Alternative
@Default
public class SessionPerPInstanceBean extends BaseKnowledgeSessionBean implements IKnowledgeSessionBean {

    private ConcurrentMap<Integer, KnowledgeSessionWrapper> kWrapperHash = new ConcurrentHashMap<Integer, KnowledgeSessionWrapper>();
    private Logger log = Logger.getLogger(SessionPerPInstanceBean.class);
    private AsyncBAMProducerPool bamProducerPool=null;
    private IKnowledgeSessionPool sessionPool;
/******************************************************************************
 **************        Singleton Lifecycle Management                     *********/
    @PostConstruct
    public void start() {
        if(System.getProperty("org.jboss.processFlow.drools.resource.scanner.interval") != null)
            droolsResourceScannerInterval = System.getProperty("org.jboss.processFlow.drools.resource.scanner.interval");
        log.info("start() drools guvnor scanner interval = "+droolsResourceScannerInterval);

        taskCleanUpImpl = System.getProperty(IKnowledgeSessionService.TASK_CLEAN_UP_PROCESS_EVENT_LISTENER_IMPL);

        /*  - set KnowledgeBase properties
         *  - the alternative to this programmatic approach is a 'META-INF/drools.session.conf' on the classpath
         */
        ksconfigProperties = new Properties();
        ksconfigProperties.put("drools.commandService", SingleSessionCommandService.class.getName());
        ksconfigProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
        ksconfigProperties.setProperty( "drools.workItemManagerFactory", JPAWorkItemManagerFactory.class.getName() );
        ksconfigProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
        ksconfigProperties.setProperty( "drools.timerService", JpaJDKTimerService.class.getName() );

        guvnorUtils = new GuvnorConnectionUtils();

        // instantiate kSession pool
        try {
            if (System.getProperty("org.jboss.processFlow.KnowledgeSessionPool") != null) {
                String clazzName = System.getProperty("org.jboss.processFlow.KnowledgeSessionPool");
                sessionPool = (IKnowledgeSessionPool) Class.forName(clazzName).newInstance();
            }
            else {
                sessionPool = new InMemoryKnowledgeSessionPool();
            }

            String logString = System.getProperty("org.jboss.enableLog");
            if(logString != null)
                enableLog = Boolean.parseBoolean(logString);

            if(System.getProperty(IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS) != null)
                processEventListeners = System.getProperty(IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS).split("\\s");

            if(System.getProperty("org.jboss.processFlow.statefulKnowledge.enableKnowledgeRuntimeLogger") != null) {
                enableKnowledgeRuntimeLogger = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.statefulKnowledge.enableKnowledgeRuntimeLogger"));
            }

            // 2) set the Drools system event listener to our implementation ...
            originalSystemEventListener = SystemEventListenerFactory.getSystemEventListener();
            if (originalSystemEventListener == null || originalSystemEventListener instanceof DelegatingSystemEventListener) {
                // We need to check for DelegatingSystemEventListener so we don't get a
                // StackOverflowError when we set it back.  If it is a DelegatingSystemEventListener,
                // we instead use what drools wraps by default, which is PrintStreamSystemEventListener.
                // Refer to org.drools.impl.SystemEventListenerServiceImpl for more information.
                originalSystemEventListener = new PrintStreamSystemEventListener();
            }
            SystemEventListenerFactory.setSystemEventListener(new LogSystemEventListener());


            programmaticallyLoadedWorkItemHandlers.put(ITaskService.HUMAN_TASK, Class.forName("org.jboss.processFlow.tasks.handlers.PFPAddHumanTaskHandler"));
            programmaticallyLoadedWorkItemHandlers.put(ITaskService.SKIP_TASK, Class.forName("org.jboss.processFlow.tasks.handlers.PFPSkipTaskHandler"));
            programmaticallyLoadedWorkItemHandlers.put(ITaskService.FAIL_TASK, Class.forName("org.jboss.processFlow.tasks.handlers.PFPFailTaskHandler"));
            programmaticallyLoadedWorkItemHandlers.put(IKnowledgeSessionService.EMAIL, Class.forName("org.jboss.processFlow.email.PFPEmailWorkItemHandler"));
        }catch(Exception x){
            x.printStackTrace();
        }
    }
  
    @PreDestroy 
    public void stop() {
        log.info("stop");
        // JA Bride :  completely plagarized from David Ward in his org.jboss.internal.soa.esb.services.rules.DroolsResourceChangeService implementation

        // ORDER IS IMPORTANT!
        // 1) stop the scanner
        ResourceFactory.getResourceChangeScannerService().stop();

        // 2) stop the notifier
        //ResourceFactory.getResourceChangeNotifierService().stop();

         // 3) set the system event listener back to the original implementation
        SystemEventListenerFactory.setSystemEventListener(originalSystemEventListener);

        try {
            if(bamProducerPool != null)
                bamProducerPool.close();
        } catch(Exception x) {
            x.printStackTrace();
        }
    }

/******************************************************************************
 *************        StatefulKnowledgeSession Management               *********/
    
    /*
        - load a StatefulKnowledgeSession with an id recently freed during the 'after process completion' event
        - if no available sessions, then make a new StatefulKnowledgeSession
     */
    private StatefulKnowledgeSession getStatefulKnowledgeSession(String processId) {
        StatefulKnowledgeSession ksession = null;
        if(processId != null) {
            int sessionId = sessionPool.getAvailableSessionId();
            if(sessionId > 0) {
                ksession = loadStatefulKnowledgeSession(new Integer(sessionId));
            } else {
                ksession = makeStatefulKnowledgeSession();
            }
            sessionPool.markAsBorrowed(ksession.getId(), processId);
        } else {
            ksession = makeStatefulKnowledgeSession();
        }
        return ksession;
    }

    /*
        -- this method is invoked by numerous methods such as 'completeWorkItem' and 'abortProcessInstance'
        -- seems that there needs to be verification that StatefuleKnowledgeSession object corresponding to the ksession isn't already in use
        -- without verification, there is a possibility that ksession corresponding to this ksessionId could be involved in processing of some other operation
        -- optimistic lock exception could ensue
        -- the kWrapperHash datastructure is a good candidate to use
    */
    private StatefulKnowledgeSession loadStatefulKnowledgeSession(Integer sessionId) {
    	if(kWrapperHash.containsKey(sessionId)) {
            //log.info("loadStatefulKnowledgeSession() found ksession in cache for ksessionId = " +sessionId);
            return kWrapperHash.get(sessionId).ksession;
        }
        
        //0) initialise knowledge base if it hasn't already been done so
        if(kbase == null){
            createKnowledgeBaseViaKnowledgeAgentOrBuilder();
        }

        //1) very important that a unique 'Environment' is created per StatefulKnowledgeSession
        Environment ksEnv = createKnowledgeSessionEnvironment();

        KnowledgeSessionConfiguration ksConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(ksconfigProperties);

        // 2) instantiate new StatefulKnowledgeSession from old sessioninfo
        StatefulKnowledgeSession ksession = JPAKnowledgeService.loadStatefulKnowledgeSession(sessionId, kbase, ksConfig, ksEnv);
        return ksession;
    }

    /*
     *  disposeStatefulKnowledgeSessionAndExtras
     *<pre>
     *- disposes of a StatefulKnowledgeSession object currently in use
     *- NOTE:  can no longer dispose knowledge session within scope of a transaction due to side effects from fix for JBRULES-1880
     *</pre>
     */
    public void disposeStatefulKnowledgeSessionAndExtras(Integer sessionId) {
        try {
            KnowledgeSessionWrapper kWrapper = ((KnowledgeSessionWrapper)kWrapperHash.get(sessionId));
            if(kWrapper == null)
                throw new RuntimeException("disposeStatefulKnowledgeSessionAndExtras() no ksessionWrapper found with sessionId = "+sessionId);
            
            kWrapper.dispose();
            kWrapperHash.remove(sessionId);
        } catch(RuntimeException x) {
            throw x;
        } catch(Exception x){
            throw new RuntimeException(x);
        }
    }

    private void addExtrasToStatefulKnowledgeSession(StatefulKnowledgeSession ksession) {

        // 1) register a configurable WorkItemHandlers with StatefulKnowledgeSession
        this.registerAddHumanTaskWorkItemHandler(ksession);
        this.registerSkipHumanTaskWorkItemHandler(ksession);
        this.registerFailHumanTaskWorkItemHandler(ksession);
        this.registerEmailWorkItemHandler(ksession);
        
        //1.5 register any addition workItemHandlers defined in drools.session.template
        SessionTemplate sTemplate = newSessionTemplate();
        if(sTemplate != null){
            for(Map.Entry<String, ?> entry : sTemplate.getWorkItemHandlers().entrySet()){
                try {
                    WorkItemHandler wHandler = (WorkItemHandler)entry.getValue();
                    ksession.getWorkItemManager().registerWorkItemHandler(entry.getKey(), wHandler);
                } catch(Exception x){
                    throw new RuntimeException("addExtrasToStatefulKnowledgeSession() following exception occurred when registering workItemId = "+entry.getKey()+" : "+x.getLocalizedMessage());
                }
            }
        }
        
            
        // 2)  add agendaEventListener to knowledge session to notify knowledge session of various rules events
        addAgendaEventListener(ksession);

        // 3)  add 'busySessions' ProcessEventListener to knowledgesession to assist in maintaining 'busySessions' state
        final ProcessEventListener busySessionsListener = new ProcessEventListener() {

            /* 
             * these process events are implemented as a 'stack pattern'
             * ie:  afterProcessStarted() event is the last event to be called
             * see org.jbpm.process.instance.ProcessRuntimeImpl.startProcessInstance(long processInstanceId) for details
            */
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                ProcessInstance pInstance = event.getProcessInstance();
                org.drools.definition.process.Process droolsProcess = event.getProcessInstance().getProcess();
                if(sessionPool.isBorrowed(ksession.getId(), pInstance.getProcessId())) {
                    log.info("afterProcessCompleted()\tsessionId :  "+ksession.getId()+" : "+pInstance+" : pDefVersion = "+droolsProcess.getVersion()+" : session to be reused");
                    sessionPool.markAsReturned(ksession.getId());
                } else {
                    log.info("afterProcessCompleted()\tsessionId :  "+ksession.getId()+" : process : "+pInstance+" : pDefVersion = "+droolsProcess.getVersion());
                }
            }

            public void beforeProcessStarted(ProcessStartedEvent event) {
            }

            /* 
                with a process with no wait state, this call-back method will actually get invoked AFTER the 'afterProcessCompleted' call back
                - if parent process, state = 1
                - if subprocess, state = 2
            */
            public void afterProcessStarted(ProcessStartedEvent event) {
                StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                ProcessInstance pInstance = event.getProcessInstance();
                org.drools.definition.process.Process droolsProcess = event.getProcessInstance().getProcess();
                log.info("afterProcessStarted()\tsessionId :  "+ksession.getId()+" : "+pInstance+" : pDefVersion = "+droolsProcess.getVersion());
            }
            public void beforeProcessCompleted(ProcessCompletedEvent event) {
            }
            public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
                if (event.getNodeInstance() instanceof SubProcessNodeInstance) {
                    StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                    SubProcessNodeInstance spNode = (SubProcessNodeInstance)event.getNodeInstance();
                    org.drools.definition.process.Process droolsProcess = event.getProcessInstance().getProcess();
                    if(enableLog)
                        log.info("beforeNodeTriggered()\tsessionId :  "+ksession.getId()+" : sub-process : " + spNode.getNodeName()+" : pid: "+spNode.getProcessInstanceId()+" : pDefVersion = "+droolsProcess.getVersion());
                }
            }
            public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
                if (event.getNodeInstance() instanceof SubProcessNodeInstance) {
                    StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                    org.drools.definition.process.Process droolsProcess = event.getProcessInstance().getProcess();
                    SubProcessNodeInstance spNode = (SubProcessNodeInstance)event.getNodeInstance();
                    if(enableLog)
                        log.info("afterNodeTriggered()\tsessionId :  "+ksession.getId()+" : sub-process : " + spNode.getNodeName()+" : pid: "+spNode.getProcessInstanceId()+" : pDefVersion = "+droolsProcess.getVersion());
                }
            }
            public void beforeNodeLeft(ProcessNodeLeftEvent event) {
            }
            public void afterNodeLeft(ProcessNodeLeftEvent event) {
            }
            public void beforeVariableChanged(ProcessVariableChangedEvent event) {
            }
            public void afterVariableChanged(ProcessVariableChangedEvent event) {
            }
        };
        ksession.addEventListener(busySessionsListener);

        // 4) register TaskCleanUpProcessEventListener
        //   NOTE:  need to ensure that task audit data has been pushed to BAM prior to this taskCleanUpProcessEventListener firing
        if(!StringUtils.isEmpty(taskCleanUpImpl) && taskCleanUpImpl.equals(TaskCleanUpProcessEventListener.class.getName())) {
            TasksAdmin adminObj = jtaTaskService.createTaskAdmin();
            TaskCleanUpProcessEventListener taskCleanUpListener = new TaskCleanUpProcessEventListener(adminObj);
            ksession.addEventListener(taskCleanUpListener);
        }

       
        // 5)  register any other process event listeners specified via configuration
        // TO_DO:  refactor using mvel. ie:  jbpm-gwt/jbpm-gwt-console-server/src/main/resources/default.session.template
        AsyncBAMProducer bamProducer= null;
        if(processEventListeners != null) {
            for(String peString : processEventListeners) {
                try {
                    Class peClass = Class.forName(peString);
                    ProcessEventListener peListener = (ProcessEventListener)peClass.newInstance();
                    if(IBAMService.ASYNC_BAM_PRODUCER.equals(peListener.getClass().getName())){
                        bamProducer = (AsyncBAMProducer)peListener;
       
                        if(bamProducerPool == null) 
                            bamProducerPool = AsyncBAMProducerPool.getInstance();
                    }
                    ksession.addEventListener(peListener);
                } catch(Exception x) {
                    throw new RuntimeException(x);
                }
            }
        }
 
        // 6)  create a kWrapper object with optional bamProducer
        KnowledgeSessionWrapper kWrapper = new KnowledgeSessionWrapper(ksession, bamProducer);
        kWrapperHash.put(ksession.getId(), kWrapper);

        // 7)  add KnowledgeRuntimeLogger as per section 4.1.3 of jbpm5 user manual
        if(enableKnowledgeRuntimeLogger) {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append(System.getProperty("jboss.server.log.dir"));
            sBuilder.append("/knowledgeRuntimeLogger-");
            sBuilder.append(ksession.getId());
            kWrapper.setKnowledgeRuntimeLogger(KnowledgeRuntimeLoggerFactory.newFileLogger(ksession, sBuilder.toString()));
        }

        SingleSessionCommandService ssCommandService = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession)ksession).getCommandService();
    }

    private StatefulKnowledgeSession loadStatefulKnowledgeSessionAndAddExtras(Integer sessionId) {
        StatefulKnowledgeSession ksession = loadStatefulKnowledgeSession(sessionId);
        addExtrasToStatefulKnowledgeSession(ksession);
        return ksession;
    }

    public String dumpSessionStatusInfo() {
        return sessionPool.dumpSessionStatusInfo();
    }

    public String dumpBAMProducerPoolInfo() {
        StringBuilder sBuilder = new StringBuilder("dumpBAMProducerPoolInfo()\n\tNumber Active = ");
        if(bamProducerPool != null) {
            sBuilder.append(bamProducerPool.getNumActive());
            sBuilder.append("\n\tNumber Idle = ");
            sBuilder.append(bamProducerPool.getNumIdle());
        } else {
            sBuilder.append("bamProducerPool is null.  most likely environment is not configured correctly for async logging of bam events from jbpm5 process engine");
        }
        return sBuilder.toString();
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
    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> parameters) {
        StatefulKnowledgeSession ksession = null;
        StringBuilder sBuilder = new StringBuilder();
        Integer ksessionId = null;
        try {
            uTrnx.begin();
            ksession = getStatefulKnowledgeSession(processId);
            ksessionId = ksession.getId();
            addExtrasToStatefulKnowledgeSession(ksession);
            sBuilder.append("startProcessAndReturnId()\tsessionId :  "+ksessionId+" : process = "+processId);
            ProcessInstance pInstance = null;
            if(parameters != null) {
                pInstance = ksession.startProcess(processId, parameters);
            } else {
                pInstance = ksession.startProcess(processId);
            }
            uTrnx.commit();
            Map<String, Object> returnMap = new HashMap<String, Object>();
            returnMap.put(IKnowledgeSessionService.PROCESS_INSTANCE_ID, pInstance.getId());
            returnMap.put(IKnowledgeSessionService.KSESSION_ID, ksessionId);
            disposeStatefulKnowledgeSessionAndExtras(ksessionId);

            uTrnx.begin();
            sessionPool.setProcessInstanceId(ksessionId, pInstance.getId());
            uTrnx.commit();

            sBuilder.append(" : pInstanceId = "+pInstance.getId()+" : now completed");
            log.info(sBuilder.toString());
            return returnMap;
        } catch(RuntimeException x) {
            x.printStackTrace();
            return null;
            //throw x;
        } catch(Exception x) {
            x.printStackTrace();
            return null;
            //throw new RuntimeException(x);
        }
    }

    /**
     *completeWorkItem
     *<pre>
     *- notifies process engine to complete a work item and continue execution of next node in process instance
     *- this method operates within scope of container managed transaction
     *- can no longer dispose knowledge session within scope of this transaction due to side effects from fix for JBRULES-1880
     *- subsequently, it's expected that a client will invoke 'disposeStatefulKnowledgeSessionAndExtras' after this JTA trnx has been committed
     *
     * if ksessionId param is passed, then pInstanceId param can be null
     * otherwise, if ksessionId param is not passed, then pInstanceId param is mandatory
     *</pre>
     */
    public void completeWorkItem(Long workItemId, Map<String, Object> pInstanceVariables, Long pInstanceId, Integer ksessionId) {
        StatefulKnowledgeSession ksession = null;
        try {
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(pInstanceId);
            
            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            ksession.getWorkItemManager().completeWorkItem(workItemId, pInstanceVariables);
        } catch(RuntimeException x) {
            throw x;
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public void signalEvent(String signalType, Object signalValue, Long processInstanceId, Integer ksessionId) {
        StatefulKnowledgeSession ksession = null;
        try {
        	uTrnx.begin();
            
        	// always go to the database to ensure row-level pessimistic lock for each process instance
            ksessionId = sessionPool.getSessionId(processInstanceId);

            
            //due to ksession.dispose() needing to be outside trnx, ksessionId could still be temporarily in kWrapperHash 
        	boolean goodToGo=true;
        	for(int x=0; x < 10; x++){
        		if(kWrapperHash.containsKey(ksessionId)) {
        			log.info("signalEvent() found ksession in cache for ksessionId = " +ksessionId+" :  will sleep");
        			try {Thread.sleep(100);} catch(Exception t){t.printStackTrace();}
        			goodToGo = false;
        		}else {
        			goodToGo = true;
        			break;
        		}
        	}
        	if(!goodToGo)
        		throw new RuntimeException("signalEvent() the following ksession continues to be in use: "+ksessionId);
        	
        	
            ksession = this.loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            if(enableLog)
            	log.info("signalEvent() \n\tksession = "+ksessionId+"\n\tprocessInstanceId = "+processInstanceId+"\n\tsignalType="+signalType+"\n\tsignalValue="+signalValue);
           
            ProcessInstance pInstance = ksession.getProcessInstance(processInstanceId);
            pInstance.signalEvent(signalType, signalValue);
            uTrnx.commit();
            disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        } catch(RuntimeException x) {
            log.error("signalEvent() exception thrown.  signalType = "+signalType+" : pInstanceId = "+processInstanceId+" : ksessionId ="+ksessionId);
            rollbackTrnx();
            throw x;
        }catch(Exception x) {
            log.error("signalEvent() exception thrown.  signalType = "+signalType+" : pInstanceId = "+processInstanceId+" : ksessionId ="+ksessionId);
            rollbackTrnx();
            throw new RuntimeException(x);
        }
    }

    public void abortProcessInstance(Long processInstanceId, Integer ksessionId) {
        StatefulKnowledgeSession ksession = null;
        try {
        	uTrnx.begin();
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(processInstanceId);

            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            ksession.abortProcessInstance(processInstanceId);
            sessionPool.markAsReturned(ksessionId);
            uTrnx.commit();
            disposeStatefulKnowledgeSessionAndExtras(ksessionId);

        } catch(RuntimeException x) {
            rollbackTrnx();
            throw x;
        }catch(Exception x) {
            rollbackTrnx();
            throw new RuntimeException(x);
        } finally {
        }
    }

    public String printActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId) {
        Map<String,Object> vHash = getActiveProcessInstanceVariables(processInstanceId, ksessionId);
        StringBuilder sBuilder = new StringBuilder();
        if(vHash.size() == 0){
            sBuilder.append("no process instance variables for :\n\tprocessInstanceId = ");
            sBuilder.append(processInstanceId);
        }
        for (Map.Entry<?, ?> entry: vHash.entrySet()) {
            sBuilder.append("\n");
            sBuilder.append(entry.getKey());
            sBuilder.append(" : ");
            sBuilder.append(entry.getValue());
        }
        return sBuilder.toString();
    }
    
    public Map<String, Object> getActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId) {
        return getActiveProcessInstanceVariables(processInstanceId, ksessionId, true);
    }
    public Map<String, Object> getActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId, Boolean disposeKsession) {
        StatefulKnowledgeSession ksession = null;
        try {
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(processInstanceId);

            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            ProcessInstance processInstance = ksession.getProcessInstance(processInstanceId);
            if (processInstance != null) {
                Map<String, Object> variables = ((WorkflowProcessInstanceImpl) processInstance).getVariables();
                if (variables == null) {
                    return new HashMap<String, Object>();
                }
                // filter out null values
                Map<String, Object> result = new HashMap<String, Object>();
                for (Map.Entry<String, Object> entry: variables.entrySet()) {
                    if (entry.getValue() != null) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
                return result;
            } else {
                throw new IllegalArgumentException("Could not find process instance " + processInstanceId);
            }
        } catch(Exception x) {
            throw new RuntimeException(x);
        } finally {
            if(ksession != null && disposeKsession)
                disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        }
    }

    public void setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables, Integer ksessionId) {
        setProcessInstanceVariables(processInstanceId, variables, ksessionId, true);
    }
    public void setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables, Integer ksessionId, Boolean disposeKsession) {
        StatefulKnowledgeSession ksession = null;
        try {
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(processInstanceId);

            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            ProcessInstance processInstance = ksession.getProcessInstance(processInstanceId);
            if (processInstance != null) {
                VariableScopeInstance variableScope = (VariableScopeInstance)((org.jbpm.process.instance.ProcessInstance) processInstance).getContextInstance(VariableScope.VARIABLE_SCOPE);
                if (variableScope == null) {
                    throw new IllegalArgumentException("Could not find variable scope for process instance " + processInstanceId);
                }
                for (Map.Entry<String, Object> entry: variables.entrySet()) {
                    variableScope.setVariable(entry.getKey(), entry.getValue());
                }
            } else {
                throw new IllegalArgumentException("Could not find process instance " + processInstanceId);
            }
        } finally {
            if(ksession != null && disposeKsession)
                disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        }
    }

    public void upgradeProcessInstance(long processInstanceId, String processId, Map<String, Long> nodeMapping) {
        StatefulKnowledgeSession ksession = null;
        Integer ksessionId = 0;
        try {
            ksessionId = sessionPool.getSessionId(processInstanceId);
            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            WorkflowProcessInstanceUpgrader.upgradeProcessInstance(ksession, processInstanceId, processId, nodeMapping);
        } finally {
            if(ksession != null)
                disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        }
    }


    

}

class KnowledgeSessionWrapper {
    StatefulKnowledgeSession ksession;
    AsyncBAMProducer bamProducer;
    KnowledgeRuntimeLogger rLogger;

    public KnowledgeSessionWrapper(StatefulKnowledgeSession x, AsyncBAMProducer y) {
        ksession = x;
        bamProducer = y;
    }

    public void dispose() throws Exception {
        if(bamProducer != null)
            bamProducer.dispose();

        if(rLogger != null) {
            rLogger.close();
        }

        ksession.dispose();
    }

    public void setKnowledgeRuntimeLogger(KnowledgeRuntimeLogger x) {
        rLogger = x;
    }
}
