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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.BufferedReader;
import java.lang.reflect.Constructor;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;

import javax.persistence.*;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.drools.SessionConfiguration;
import org.drools.SystemEventListenerFactory;
import org.drools.SystemEventListener;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.core.util.DelegatingSystemEventListener;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.reteoo.ReteooRuleBase;
import org.drools.WorkingMemory;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.agent.impl.PrintStreamSystemEventListener;
import org.drools.command.SingleSessionCommandService;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.compiler.PackageBuilder;
import org.drools.definition.process.Process;
import org.drools.definitions.impl.KnowledgePackageImp;
import org.drools.definition.KnowledgePackage;
import org.drools.event.*;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.io.*;
import org.drools.io.impl.InputStreamResource;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.management.DroolsManagementAgent;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.persistence.jpa.JpaJDKTimerService;
import org.drools.persistence.jpa.processinstance.JPAWorkItemManagerFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemHandler;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;
import org.jbpm.compiler.ProcessBuilderImpl;
import org.jbpm.integration.console.shared.GuvnorConnectionUtils;
import org.jbpm.task.admin.TaskCleanUpProcessEventListener;
import org.jbpm.task.admin.TasksAdmin;

import org.jboss.processFlow.bam.IBAMService;
import org.jboss.processFlow.bam.AsyncBAMProducerPool;
import org.jboss.processFlow.bam.AsyncBAMProducer;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceMXBean;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.workItem.WorkItemHandlerLifecycle;
import org.jboss.processFlow.util.LogSystemEventListener;
import org.jboss.processFlow.PFPBaseService;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;

/**
 *<pre>
 *currently, this is the only implementation of org.jboss.processFlow.knowledgeService.IKnowledgeSessionService
 *
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
@Local(IBaseKnowledgeSessionService.class)
@Singleton(name="prodKSessionProxy")
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KnowledgeSessionService extends PFPBaseService implements IKnowledgeSessionService, KnowledgeSessionServiceMXBean {

    private static final String EMF_NAME = "org.jbpm.persistence.jpa";
    public static final String DROOLS_SESSION_CONF_PATH="/META-INF/drools.session.conf";
    public static final String DROOLS_SESSION_TEMPLATE_PATH="drools.session.template.path";
    private static final String DROOLS_WORK_ITEM_HANDLERS = "drools.workItemHandlers";
    
    private ConcurrentMap<Integer, KnowledgeSessionWrapper> kWrapperHash = new ConcurrentHashMap<Integer, KnowledgeSessionWrapper>();
    private Logger log = Logger.getLogger(KnowledgeSessionService.class);
    private String droolsResourceScannerInterval = "30";
    private boolean enableLog = false;
    private boolean enableKnowledgeRuntimeLogger = true;
    private Map<String, Class> programmaticallyLoadedWorkItemHandlers = new HashMap<String, Class>();

    private KnowledgeBase kbase = null;
    private SystemEventListener originalSystemEventListener = null;
    private DroolsManagementAgent kmanagement = null;
    private GuvnorConnectionUtils guvnorUtils = null;
    private AsyncBAMProducerPool bamProducerPool=null;
    private Properties ksconfigProperties;
    private IKnowledgeSessionPool sessionPool;
    private String[] processEventListeners;
    private String guvnorChangeSet;
    private ObjectName objectName;
    private MBeanServer platformMBeanServer;
    private Properties guvnorProps;
    private String taskCleanUpImpl;
    private String templateString;
    private boolean sessionTemplateInstantiationAlreadyBombed = false;
    

    private @PersistenceUnit(unitName=EMF_NAME)  EntityManagerFactory jbpmCoreEMF;
    private @javax.annotation.Resource UserTransaction uTrnx;
    private @javax.annotation.Resource(name="java:/TransactionManager") TransactionManager tMgr;

/******************************************************************************
 **************        Singleton Lifecycle Management                     *********/
    @PostConstruct
    public void start() throws Exception {
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
        // JA Bride :  completely plagarized from David Ward in his org.jboss.internal.soa.esb.services.rules.DroolsResourceChangeService implementation

        // ORDER IS IMPORTANT!
        // 1) stop the scanner
        ResourceFactory.getResourceChangeScannerService().stop();

        // 2) stop the notifier
        //ResourceFactory.getResourceChangeNotifierService().stop();

         // 3) set the system event listener back to the original implementation
        SystemEventListenerFactory.setSystemEventListener(originalSystemEventListener);

        if(bamProducerPool != null)
            bamProducerPool.close();

        try {
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new RuntimeException("Problem during unregistration of Monitoring into JMX:" + e);
        }


    }

    
    
    
    

/******************************************************************************
 * *************        Drools KnowledgeBase Management               *********/
    
    // critical that each StatefulKnowledgeSession have its own JPA 'Environment'
    private Environment createKnowledgeSessionEnvironment() {
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, jbpmCoreEMF);
        return env;
    }
    
    public void createKnowledgeBaseViaKnowledgeAgentOrBuilder() {
    	try {
    		this.createKnowledgeBaseViaKnowledgeAgent();
    	}catch(ConnectException x){
    		log.warn("createKnowledgeBaseViaKnowledgeAgentOrBuilder() can not create a kbase via a kagent due to a connection problem with guvnor ... will now create kbase via knowledgeBuilder");
    		rebuildKnowledgeBaseViaKnowledgeBuilder();
    	}
    }
    
    public void rebuildKnowledgeBaseViaKnowledgeAgent() throws ConnectException{
        this.createKnowledgeBaseViaKnowledgeAgent(true);
    }
    private void createKnowledgeBaseViaKnowledgeAgent() throws ConnectException{
        this.createKnowledgeBaseViaKnowledgeAgent(false);
    }

    // only one knowledgeBase object is needed and is shared amongst all StatefulKnowledgeSessions
    // needs to be invoked AFTER guvnor is available (obviously)
    // setting 'force' parameter to true re-creates an existing kbase
    private synchronized void createKnowledgeBaseViaKnowledgeAgent(boolean force) throws ConnectException{
        if(kbase != null && !force)
            return;

        // investigate:  List<String> guvnorPackages = guvnorUtils.getBuiltPackageNames();
        // http://ratwateribm:8080/jboss-brms/org.drools.guvnor.Guvnor/package/org.jboss.processFlow/test-pfp-snapshot

        if(!guvnorUtils.guvnorExists()) {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append(guvnorUtils.getGuvnorProtocol());
            sBuilder.append("://");
            sBuilder.append(guvnorUtils.getGuvnorHost());
            sBuilder.append("/");
            sBuilder.append(guvnorUtils.getGuvnorSubdomain());
            sBuilder.append("/rest/packages/");
            throw new ConnectException("createKnowledgeBase() cannot connect to guvnor at URL : "+sBuilder.toString()); 
        }

        // for polling of guvnor to occur, the polling and notifier services must be started
        ResourceChangeScannerConfiguration sconf = ResourceFactory.getResourceChangeScannerService().newResourceChangeScannerConfiguration();
        sconf.setProperty( "drools.resource.scanner.interval", droolsResourceScannerInterval);
        ResourceFactory.getResourceChangeScannerService().configure( sconf );
        ResourceFactory.getResourceChangeScannerService().start();
        ResourceFactory.getResourceChangeNotifierService().start();
        
        KnowledgeAgentConfiguration aconf = KnowledgeAgentFactory.newKnowledgeAgentConfiguration(); // implementation = org.drools.agent.impl.KnowledgeAgentConfigurationImpl

        /*  - incremental change set processing enabled
            - will create a single KnowledgeBase and always refresh that same instance
        */
        aconf.setProperty("drools.agent.newInstance", "false");

        /*  -- Knowledge Agent provides automatic loading, caching and re-loading of resources
            -- the knowledge agent can update or rebuild this knowledge base as the resources it uses are changed
        */
        KnowledgeAgent kagent = KnowledgeAgentFactory.newKnowledgeAgent("Guvnor default", aconf);
        StringReader sReader = guvnorUtils.createChangeSet();
        try {
            guvnorChangeSet = IOUtils.toString(sReader);
            sReader.close();
        }catch(Exception x){
            x.printStackTrace();
        }
        
        kagent.applyChangeSet(ResourceFactory.newByteArrayResource(guvnorChangeSet.getBytes()));

        /*  - set KnowledgeBase as instance variable to this mbean for use throughout all functionality of this service
            - a knowledge base is a collection of compiled definitions, such as rules and processes, which are compiled using the KnowledgeBuilder
            - the knowledge base itself does not contain instance data, known as facts
            - instead, sessions are created from the knowledge base into which data can be inserted and where process instances may be started
            - creating the knowledge base can be heavy, whereas session creation is very light :  http://blog.athico.com/2011/09/small-efforts-big-improvements.html
            - a knowledge base is also serializable, allowing for it to be stored
        */
        kbase = kagent.getKnowledgeBase();
    }
    
    public void rebuildKnowledgeBaseViaKnowledgeBuilder() {
        guvnorProps = new Properties();
        try {
            KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            if(guvnorUtils.guvnorExists()) {
                guvnorProps.load(KnowledgeSessionService.class.getResourceAsStream("/jbpm.console.properties"));
                StringBuilder guvnorSBuilder = new StringBuilder();
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_PROTOCOL_KEY));
                guvnorSBuilder.append("://");
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_HOST_KEY));
                guvnorSBuilder.append("/");
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_SUBDOMAIN_KEY));
                String guvnorURI = guvnorSBuilder.toString();
                List<String> packages = guvnorUtils.getPackageNames();
                for(String pkg : packages){
                    GuvnorRestApi guvnorRestApi = new GuvnorRestApi(guvnorURI);
                    InputStream binaryPackage = guvnorRestApi.getBinaryPackage(pkg);
                    kbuilder.add(new InputStreamResource(binaryPackage), ResourceType.PKG);
                    guvnorRestApi.close();
                }
            }
            kbase = kbuilder.newKnowledgeBase();
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }
   
    // compile a process into a package and add it to the knowledge base 
    public void addProcessToKnowledgeBase(Process processObj, Resource resourceObj) {
        if(kbase == null)
            rebuildKnowledgeBaseViaKnowledgeBuilder();
       
        PackageBuilder packageBuilder = new PackageBuilder();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl( packageBuilder );
        processBuilder.buildProcess( processObj, resourceObj);

        List<KnowledgePackage> kpackages = new ArrayList<KnowledgePackage>();
        kpackages.add( new KnowledgePackageImp( packageBuilder.getPackage() ) );
        kbase.addKnowledgePackages(kpackages);
        log.info("addProcessToKnowledgeBase() just added the following bpmn2 process definition to the kbase: "+processObj.getId());
    }

    public void addProcessToKnowledgeBase(File bpmnFile) {
        if(kbase == null)
            rebuildKnowledgeBaseViaKnowledgeBuilder();

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newFileResource(bpmnFile), ResourceType.BPMN2);
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        log.info("addProcessToKnowledgeBase() just added the following bpmn2 process definition to the kbase: "+bpmnFile.getName());
    }
    
    public String getAllProcessesInPackage(String pkgName){
        List<String> processes = guvnorUtils.getAllProcessesInPackage(pkgName);
        StringBuilder sBuilder = new StringBuilder("getAllProcessesInPackage() pkgName = "+pkgName);
        if(processes.isEmpty()){
        	sBuilder.append("\n\n\t :  not processes found");
        	return sBuilder.toString();
        }
        for(String pDef : processes){
            sBuilder.append("\n\t");
            sBuilder.append(pDef);
        }
        return sBuilder.toString();
    }
    
    public String printKnowledgeBaseContent() {
        if(kbase == null)
            throw new RuntimeException("printKnowledgeBaseContent() ... kbase has not yet been instantiated");

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("guvnor changesets\n\t");
        
        sBuilder.append(guvnorChangeSet);

        Collection<KnowledgePackage> kPackages = kbase.getKnowledgePackages();
        for(KnowledgePackage kPackage : kPackages){
            Collection<Process> processes = kPackage.getProcesses();
            if(processes.size() == 0){
                sBuilder.append("\n\tpackage = "+kPackage.getName()+" : no process definitions found ");
            }else {

                sBuilder.append("\nprintKnowledgeBaseContent()\n\t"); 
                for (Process process : processes) {
                    sBuilder.append("\n\tpackage = "+kPackage.getName()+" : process definition = " + process.getId());
                }
            }
        }
        sBuilder.append("\n");
        return sBuilder.toString();
    }
    
    private SessionTemplate newSessionTemplate() {
    	if(sessionTemplateInstantiationAlreadyBombed)
    		return null;
    	
    	if(templateString == null){
    		String droolsSessionTemplatePath = System.getProperty(DROOLS_SESSION_TEMPLATE_PATH);
    		if(StringUtils.isNotEmpty(droolsSessionTemplatePath)){
    			File droolsSessionTemplate = new File(droolsSessionTemplatePath);
    			if(!droolsSessionTemplate.exists()) {
    				throw new RuntimeException("newSessionTemplate() drools session template not found at : "+droolsSessionTemplatePath);
    			}else {
    				FileInputStream fStream = null;
    				try {
    					fStream = new FileInputStream(droolsSessionTemplate);
    					templateString = IOUtils.toString(fStream);

    				}catch(IOException x){
    					x.printStackTrace();
    				}finally {
    					if(fStream != null) {
    						try {fStream.close(); }catch(Exception x){x.printStackTrace();}
    					}
    				}
    			}
    		}else {
    			throw new RuntimeException("newSessionTemplate() following property must be defined : "+DROOLS_SESSION_TEMPLATE_PATH);
    		}
    	}
    	ParserConfiguration pconf = new ParserConfiguration();
    	pconf.addImport("SessionTemplate", SessionTemplate.class);
    	ParserContext context = new ParserContext(pconf);
    	Serializable s = MVEL.compileExpression(templateString.trim(), context);
    	try {
    		return (SessionTemplate)MVEL.executeExpression(s);
    	}catch(Throwable x){
    		sessionTemplateInstantiationAlreadyBombed = true;
    		log.error("newSessionTemplate() following exception thrown \n\t"+x.getLocalizedMessage()+"\n : with session template string = \n\n"+templateString);
    		return null;
    	}
    }

    
    
    

/******************************************************************************
 * *************            WorkItemHandler Management               *********/
    
    public String printWorkItemHandlers() { 
        StringBuilder sBuilder = new StringBuilder("Programmatically Loaded Work Item Handlers :");
        for(String name : programmaticallyLoadedWorkItemHandlers.keySet()){
           sBuilder.append("\n\t"); 
           sBuilder.append(name); 
           sBuilder.append(" : "); 
           sBuilder.append(programmaticallyLoadedWorkItemHandlers.get(name)); 
        }
        sBuilder.append("\nWork Item Handlers loaded from drools session template:");
        SessionTemplate sTemplate = newSessionTemplate();
        if(sTemplate != null){
        	for(Map.Entry<?, ?> entry : sTemplate.getWorkItemHandlers().entrySet()){
        		Class wiClass = entry.getValue().getClass();
        		sBuilder.append("\n\t"); 
        		sBuilder.append(entry.getKey()); 
        		sBuilder.append(" : "); 
        		sBuilder.append(wiClass.getClass());
        	}
        }else {
        	sBuilder.append("\n\tsessionTemplate not instantiated ... check previous exceptions");
        }
        sBuilder.append("\nConfiguration Loaded Work Item Handlers :");
        SessionConfiguration ksConfig = (SessionConfiguration)KnowledgeBaseFactory.newKnowledgeSessionConfiguration(ksconfigProperties);
        try {
            Map<String, WorkItemHandler> wiHandlers = ksConfig.getWorkItemHandlers();
            if(wiHandlers.size() == 0) {
                sBuilder.append("\n\t no work item handlers defined");
                Properties badProps = createPropsFromDroolsSessionConf();
                if(badProps == null)
                    sBuilder.append("\n\tunable to locate "+DROOLS_SESSION_CONF_PATH);
                else
                    sBuilder.append("\n\tlocated"+DROOLS_SESSION_CONF_PATH);
            } else {
                for(String name : wiHandlers.keySet()){
                    sBuilder.append("\n\t"); 
                    sBuilder.append(name); 
                    sBuilder.append(" : "); 
                    Class wiClass = wiHandlers.get(name).getClass();
                    sBuilder.append(wiClass); 
                }
            }
        }catch(NullPointerException x){
            sBuilder.append("\n\tError intializing at least one of the configured work item handlers via drools.session.conf.\n\tEnsure all space delimited work item handlers listed in drools.session.conf exist on the classpath");
            Properties badProps = createPropsFromDroolsSessionConf();
            if(badProps == null){
                sBuilder.append("\n\tunable to locate "+DROOLS_SESSION_CONF_PATH);
            } else {
                try {
                    Enumeration badEnums = badProps.propertyNames();
                    while (badEnums.hasMoreElements()) {
                        String handlerConfName = (String) badEnums.nextElement();
                        if(DROOLS_WORK_ITEM_HANDLERS.equals(handlerConfName)) {
                            String[] badHandlerNames = ((String)badProps.get(handlerConfName)).split("\\s");
                            for(String badHandlerName : badHandlerNames){
                                sBuilder.append("\n\t\t");
                                sBuilder.append(badHandlerName);
                                InputStream iStream = this.getClass().getResourceAsStream("/META-INF/"+badHandlerName);
                                if(iStream != null){
                                    sBuilder.append("\t : found on classpath");
                                    iStream.close();
                                } else {
                                    sBuilder.append("\t : NOT FOUND on classpath !!!!!  ");
                                }
                            }
                        }
                    }
                } catch (Exception y) {
                    y.printStackTrace();
                }
            }
        }catch(org.mvel2.CompileException x) {
            sBuilder.append("\n\t located "+DROOLS_SESSION_CONF_PATH);
            sBuilder.append("\n\t however, following ClassNotFoundException encountered when instantiating defined work item handlers : \n\t\t");
            sBuilder.append(x.getLocalizedMessage());
        }
        sBuilder.append("\n"); 
        return sBuilder.toString();
    }
    
    private Properties createPropsFromDroolsSessionConf() {
        Properties badProps = null;
        InputStream iStream = null;
        try {
            iStream = this.getClass().getResourceAsStream(DROOLS_SESSION_CONF_PATH);
            if(iStream != null){
                badProps = new Properties();
                badProps.load(iStream);
                iStream.close();
            }
        } catch(Exception x) {
            x.printStackTrace();
        }
        return badProps; 
    }
    
    private void registerWorkItemHandler(StatefulKnowledgeSession ksession, String serviceTaskName, WorkItemHandlerLifecycle handler) {
        try {
            ksession.getWorkItemManager().registerWorkItemHandler(serviceTaskName, handler);
        } catch(NullPointerException x) {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("registerHumanTaskWorkItemHandler() ********* NullPointerException when attempting to programmatically register workItemHander of type: "+serviceTaskName);
            sBuilder.append("\nthe following is a report of your work item situation: \n\n");
            sBuilder.append(printWorkItemHandlers());
            sBuilder.append("\n");
            log.error(sBuilder);
            throw x;
        }
    }
    
    private void registerAddHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession) {
        try {
            // 1.  instantiate an object and register with this session workItemManager 
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(ITaskService.HUMAN_TASK);
            WorkItemHandlerLifecycle handler = (WorkItemHandlerLifecycle)workItemHandlerClass.newInstance();

            // 2.  register workItemHandler with workItemManager
            registerWorkItemHandler(ksession, ITaskService.HUMAN_TASK, handler);

            // 3).  call init() on newly instantiated WorkItemHandlerLifecycle
            handler.init(ksession);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
    private void registerSkipHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession){
        try {
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(ITaskService.SKIP_TASK);
            WorkItemHandlerLifecycle handler = (WorkItemHandlerLifecycle)workItemHandlerClass.newInstance();
            registerWorkItemHandler(ksession, ITaskService.SKIP_TASK, handler);
            handler.init(ksession);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
    private void registerFailHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession){
        try {
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(ITaskService.FAIL_TASK);
            WorkItemHandlerLifecycle handler = (WorkItemHandlerLifecycle)workItemHandlerClass.newInstance();
            registerWorkItemHandler(ksession, ITaskService.FAIL_TASK, handler);
            handler.init(ksession);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    private void registerEmailWorkItemHandler(StatefulKnowledgeSession ksession) {
        String address = System.getProperty("org.jbpm.workItemHandler.mail.address");
        String port = System.getProperty("org.jbpm.workItemHandler.mail.port");
        String userId = System.getProperty("org.jbpm.workItemHandler.mail.userId");
        String password = System.getProperty("org.jbpm.workItemHandler.mail.password");
        WorkItemHandlerLifecycle handler = null;
        try {
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(IKnowledgeSessionService.EMAIL);
            Class[] classParams = new Class[] {String.class, String.class, String.class, String.class};
            Object[] objParams = new Object[] {address, port, userId, password};
            Constructor cObj = workItemHandlerClass.getConstructor(classParams);
            handler = (WorkItemHandlerLifecycle)cObj.newInstance(objParams);
            registerWorkItemHandler(ksession, IKnowledgeSessionService.EMAIL, handler);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    
    
    
    
    
/******************************************************************************
 * *************    ProcessEventListener Management                  *********/    
    
    // listens for agenda changes like rules being activated, fired, cancelled, etc
    private void addAgendaEventListener(Object ksession) {
        final org.drools.event.AgendaEventListener agendaEventListener = new org.drools.event.AgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory){
            }
            public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory){
            }
            public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) {
            }
            public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) {
            }
            public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) {
            }
            public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) {
            }
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
            }
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
                workingMemory.fireAllRules();
            }
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) {
            }
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event,  WorkingMemory workingMemory) {
            }
        };
        ((StatefulKnowledgeSessionImpl)  ((KnowledgeCommandContext) ((CommandBasedStatefulKnowledgeSession) ksession)
                    .getCommandService().getContext()).getStatefulKnowledgesession() )
                    .session.addEventListener(agendaEventListener);
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


    private StatefulKnowledgeSession makeStatefulKnowledgeSession() {
        // 1) instantiate a KnowledgeBase via query to guvnor or kbuilder
        createKnowledgeBaseViaKnowledgeAgentOrBuilder();

        // 2) very important that a unique 'Environment' is created per StatefulKnowledgeSession
        Environment ksEnv = createKnowledgeSessionEnvironment();

        // what's the difference between KnowledgeSession and KnowledgeBase configuration ??
        // Nick: always instantiate new ksconfig to make it threadlocal bo bapass the ConcurrentModificationExcepotion
        KnowledgeSessionConfiguration ksConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(ksconfigProperties);

        // 3) instantiate StatefulKnowledgeSession
        //    make synchronize because under heavy load, appears that underlying SessionInfo.update() breaks with a NPE
        StatefulKnowledgeSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, ksConfig, ksEnv);
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
            log.info("loadStatefulKnowledgeSession() found ksession in cache for ksessionId = " +sessionId);
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
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
                    try {
                        log.info("afterProcessCompleted trnx status = "+tMgr.getStatus());
                    } catch(Exception x) { x.printStackTrace(); }
                if(sessionPool.isBorrowed(ksession.getId(), pInstance.getProcessId())) {
                    log.info("afterProcessCompleted()\tsessionId :  "+ksession.getId()+" : "+pInstance+" : session to be reused");
                    sessionPool.markAsReturned(ksession.getId());
                } else {
                    log.info("afterProcessCompleted()\tsessionId :  "+ksession.getId()+" : process : "+pInstance);
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
                log.info("afterProcessStarted()\tsessionId :  "+ksession.getId()+" : "+pInstance+" : ");
            }
            public void beforeProcessCompleted(ProcessCompletedEvent event) {
            }
            public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
                if (event.getNodeInstance() instanceof SubProcessNodeInstance) {
                    StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                    SubProcessNodeInstance spNode = (SubProcessNodeInstance)event.getNodeInstance();
                    if(enableLog)
                        log.info("beforeNodeTriggered()\tsessionId :  "+ksession.getId()+" : sub-process : " + spNode.getNodeName()+" : pid: "+spNode.getProcessInstanceId());
                }
            }
            public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
                if (event.getNodeInstance() instanceof SubProcessNodeInstance) {
                    StatefulKnowledgeSession ksession = (StatefulKnowledgeSession)event.getKnowledgeRuntime();
                      SubProcessNodeInstance spNode = (SubProcessNodeInstance)event.getNodeInstance();
                    if(enableLog)
                        log.info("afterNodeTriggered()\tsessionId :  "+ksession.getId()+" : sub-process : " + spNode.getNodeName()+" : pid: "+spNode.getProcessInstanceId());
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
        if(taskCleanUpImpl != null && taskCleanUpImpl.equals(TaskCleanUpProcessEventListener.class.getName())) {
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


        // 8) allow JMX statistics to be gathered on this Stateful Knowledge Session
        //((CommandBasedStatefulKnowledgeSession)ksession).getInternalWorkingMemory();
        //kmanagement.registerKnowledgeSession(((StatefulKnowledgeSessionImpl)ksession).getInternalWorkingMemory());

        SingleSessionCommandService ssCommandService = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession)ksession).getCommandService();
        if(false) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("addExtrasToStatefulKnowledgeSession() extras added to CommandBasedStatefulKnowledgeSession with the following : ");
            logBuilder.append("\n\tksession id = ");
            logBuilder.append(ksession.getId());
            logBuilder.append("\n\tSingleSessionCommandService = "+ssCommandService);
            log.info(logBuilder.toString());
        }
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
*************              Process Definition Management              *********/
    public List<Process> retrieveProcesses() throws Exception {
        List<Process> result = new ArrayList<Process>();
        if(kbase == null)
            createKnowledgeBaseViaKnowledgeAgent();
        for (KnowledgePackage kpackage: kbase.getKnowledgePackages()) {
            result.addAll(kpackage.getProcesses());
        }
        log.info("getProcesses() # of processes = "+result.size());
        return result;
    }

    public Process getProcess(String processId) {
        if(kbase == null)
            createKnowledgeBaseViaKnowledgeAgentOrBuilder();
        return kbase.getProcess(processId);
    }

    public Process getProcessByName(String name) throws Exception {
        if(kbase == null)
            createKnowledgeBaseViaKnowledgeAgent();
        for (KnowledgePackage kpackage: kbase.getKnowledgePackages()) {
            for (Process process: kpackage.getProcesses()) {
                if (name.equals(process.getName())) {
                    return process;
                }
            }
        }
        return null;
    }

    public void removeProcess(String processId) {
        throw new UnsupportedOperationException();
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
     *</pre>
     */
    public void completeWorkItem(Integer ksessionId, Long workItemId, Map<String, Object> pInstanceVariables) {
        StatefulKnowledgeSession ksession = null;
        try {
            ksession = loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            ksession.getWorkItemManager().completeWorkItem(workItemId, pInstanceVariables);
        } catch(RuntimeException x) {
            throw x;
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void signalEvent(String signalType, Object signalValue, Long processInstanceId, Integer ksessionId) {
        StatefulKnowledgeSession ksession = null;
        try {
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(processInstanceId);

            uTrnx.begin();
            ksession = this.loadStatefulKnowledgeSessionAndAddExtras(ksessionId);
            if(enableLog)
                log.info("signalEvent() \n\tksession = "+ksessionId+"\n\tprocessInstanceId = "+processInstanceId+"\n\tsignalType="+signalType+"\n\tsignalValue="+signalValue);
            ProcessInstance pInstance = ksession.getProcessInstance(processInstanceId);
            pInstance.signalEvent(signalType, signalValue);
            uTrnx.commit();
            disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        } catch(RuntimeException x) {
            rollbackTrnx();
            throw x;
        }catch(Exception x) {
            rollbackTrnx();
            throw new RuntimeException(x);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void abortProcessInstance(Long processInstanceId, Integer ksessionId) {
        StatefulKnowledgeSession ksession = null;
        try {
            if(ksessionId == null)
                ksessionId = sessionPool.getSessionId(processInstanceId);

            uTrnx.begin();
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

    public List<ProcessInstance> getActiveProcessInstances(Map<String, Object> queryCriteria) {
         EntityManager psqlEm = null;
         List<ProcessInstance> results = null;
         StringBuilder sqlBuilder = new StringBuilder();
         sqlBuilder.append("FROM ProcessInstance p ");
         if(queryCriteria != null && queryCriteria.size() > 0){
             sqlBuilder.append("WHERE ");
             if(queryCriteria.containsKey(IKnowledgeSessionService.PROCESS_ID)){
                 sqlBuilder.append("p.processid = :processId");
             }
         }
         try {
             psqlEm = jbpmCoreEMF.createEntityManager();
             Query processInstanceQuery = psqlEm.createQuery(sqlBuilder.toString());
             if(queryCriteria != null && queryCriteria.size() > 0){
                 if(queryCriteria.containsKey(IKnowledgeSessionService.PROCESS_ID)){
                     processInstanceQuery = processInstanceQuery.setParameter(IKnowledgeSessionService.PROCESS_ID, queryCriteria.get(IKnowledgeSessionService.PROCESS_ID));
                 }
             }
             results = processInstanceQuery.getResultList();
             return results;
         }catch(Exception x) {
             return null;
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
  
    @TransactionAttribute(TransactionAttributeType.NEVER) 
    public Map<String, Object> getActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId) {
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
        	if(ksession != null)
        		disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        }
    }

    public void setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables, Integer ksessionId) {
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
        	if(ksession != null)
        		disposeStatefulKnowledgeSessionAndExtras(ksessionId);
        }
    }
    
    private void rollbackTrnx() {
        try {
            if(uTrnx.getStatus() == javax.transaction.Status.STATUS_ACTIVE)
                uTrnx.rollback();
        }catch(Exception e) {
            e.printStackTrace();
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
/*
        StringWriter sw = new StringWriter();
new Throwable("").printStackTrace(new PrintWriter(sw));
String stackTrace = sw.toString();
System.out.println("stack = "+stackTrace);
*/
    }

    public void setKnowledgeRuntimeLogger(KnowledgeRuntimeLogger x) {
        rLogger = x;
    }
}
