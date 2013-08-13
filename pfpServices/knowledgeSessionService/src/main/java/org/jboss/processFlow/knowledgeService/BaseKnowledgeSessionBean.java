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
import java.lang.reflect.Constructor;
import java.net.ConnectException;
import java.util.*;

import javax.transaction.UserTransaction;
import javax.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.*;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.drools.SessionConfiguration;
import org.drools.SystemEventListener;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;
import org.drools.WorkingMemory;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.agent.impl.PrintStreamSystemEventListener;
import org.drools.command.SingleSessionCommandService;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.compiler.PackageBuilder;
import org.drools.core.util.DelegatingSystemEventListener;
import org.drools.definition.process.Process;
import org.drools.definitions.impl.KnowledgePackageImp;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.WorkflowProcess;
import org.drools.definition.process.Node;
import org.drools.event.*;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.io.*;
import org.drools.io.impl.InputStreamResource;
import org.drools.management.DroolsManagementAgent;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.persistence.jpa.JpaJDKTimerService;
import org.drools.persistence.jpa.processinstance.JPAWorkItemManagerFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.process.WorkItemHandler;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.compiler.ProcessBuilderImpl;
import org.jbpm.integration.console.shared.GuvnorConnectionUtils;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.task.service.TaskService;

import org.jboss.processFlow.haTimerService.ITimerServiceManagement;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.util.LogSystemEventListener;
import org.jboss.processFlow.workItem.WorkItemHandlerLifecycle;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;


/**
 *<pre>
 *architecture
 * Drools knowledgeBase management
 *  - this implementation instantiates a single instance of org.drools.KnowledgeBase
 *  - this KnowledgeBase is kept current by interacting with a remote BRMS guvnor service
 *  - note: this KnowledgeBase instance is instantiated the first time any IKnowledgeSessionService operation is invoked
 *  - the KnowledgeBase is not instantiated in a start() method because the BRMS guvnor may be co-located on the same jvm
 *      as this KnowledgeSessionService and may not yet be available (depending on boot-loader order)
 *      
 *
 * WorkItemHandler Management
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
 * processEventListeners
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
 * BAM audit logging
 *  - this implementation leverages a pool of JMS producers to send BAM events to a JMS provider
 *  - a corresponding BAM consumer receives those BAM events and persists to the BRMS BAM database
 *  - it is possible to disable the production of BAM events by NOT including 'org.jboss.processFlow.bam.AsyncBAMProducer' as a value
 *    in the IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS property
 *  - note:  if 'org.jboss.processFlow.bam.AsyncBAMProducer' is not included, then any clients that query the BRMS BAM database will be affected
 *  - an example is the BRMS gwt-console-server
 *      the gwt-console-server queries the BRMS BAM database for listing of active process instances
 *      
 *     
 *  22 Jan 2013:  various performance optimizations and general cleanup contributed by Michal Valach.  thank you!
 */
public class BaseKnowledgeSessionBean {

    public static final String EMF_NAME = "org.jbpm.persistence.jpa";
    public static final String DROOLS_SESSION_CONF_PATH="/META-INF/drools.session.conf";
    public static final String DROOLS_SESSION_TEMPLATE_PATH="drools.session.template.path";
    public static final String DROOLS_WORK_ITEM_HANDLERS = "drools.workItemHandlers";
    public static final String CHANGE_SET_URL = "org.jboss.processFlow.change.set.url";
    
    protected Logger log = Logger.getLogger(BaseKnowledgeSessionBean.class);
    protected String droolsResourceScannerInterval = "30";
    protected boolean enableLog = false;
    protected boolean enableKnowledgeRuntimeLogger = true;
    protected Map<String, Class<?>> programmaticallyLoadedWorkItemHandlers = new HashMap<String, Class<?>>();

    protected KnowledgeBase kbase = null;
    protected KnowledgeAgent kagent = null;
    protected boolean kAgentMonitor = true;
    protected long lastKAgentRefresh;
    protected int kAgentRefreshHours = 12;
    protected SystemEventListener originalSystemEventListener = null;
    protected DroolsManagementAgent kmanagement = null;
    protected GuvnorConnectionUtils guvnorUtils = null;
    protected Properties ksconfigProperties;
    protected String[] processEventListeners;
    protected String guvnorChangeSet;
    
    protected Properties guvnorProps;
    protected String taskCleanUpImpl;
    protected String templateString;
    private int sessionTemplateInstantiationAttempts = 0;
    private Object templateStringLockObj = new Object();
    private Object sessionTemplateInstantiationLock = new Object();
    
    /* static variable because :
     *   1)  TaskService is a thread-safe object
     *   2)  TaskService is needed for both :
     *     - PFP HumanTaskService           :   functions using a jta enable entity manager for human task functionality
     *     - PFP KnowledgeSessionService    :   needed to instantiate TasksAdmin object and register with knowledgeSession
     */
    protected static TaskService jtaTaskService;
    

    protected @PersistenceUnit(unitName=EMF_NAME)  EntityManagerFactory jbpmCoreEMF;
    protected @javax.annotation.Resource UserTransaction uTrnx;
    
    protected void start() throws Exception{
        /*  - set KnowledgeBase properties
         *  - the alternative to this programmatic approach is a 'META-INF/drools.session.conf' on the classpath
         */
         ksconfigProperties = new Properties();
         ksconfigProperties.put("drools.commandService", SingleSessionCommandService.class.getName());
         ksconfigProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
         ksconfigProperties.setProperty( "drools.workItemManagerFactory", JPAWorkItemManagerFactory.class.getName() );
         ksconfigProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
         
         if(System.getProperty("org.jboss.processFlow.drools.resource.scanner.interval") != null)
             droolsResourceScannerInterval = System.getProperty("org.jboss.processFlow.drools.resource.scanner.interval");
         
         taskCleanUpImpl = System.getProperty(IKnowledgeSessionService.TASK_CLEAN_UP_PROCESS_EVENT_LISTENER_IMPL);

         String timerService = System.getProperty("drools.timerService", JpaJDKTimerService.class.getName());
         ksconfigProperties.setProperty( "drools.timerService", timerService);
         
         
         guvnorUtils = new GuvnorConnectionUtils();
         enableLog = Boolean.parseBoolean(System.getProperty("org.jboss.enableLog", "TRUE"));
         
         if(System.getProperty(IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS) != null)
             processEventListeners = System.getProperty(IKnowledgeSessionService.SPACE_DELIMITED_PROCESS_EVENT_LISTENERS).split("\\s");

         kAgentRefreshHours = Integer.parseInt(System.getProperty("org.jboss.processFlow.kAgentRefreshHours", "12"));
         kAgentMonitor = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.kAgentMonitor", "TRUE"));
         
         enableKnowledgeRuntimeLogger = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.statefulKnowledge.enableKnowledgeRuntimeLogger", "TRUE"));
    
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
    
         StringBuilder logBuilder = new StringBuilder();
         logBuilder.append("start() ksession props as follows :\n\tdrools guvnor scanner interval = ");
         logBuilder.append(droolsResourceScannerInterval);
         logBuilder.append("\n\ttimerService = ");
         logBuilder.append(timerService);
         logBuilder.append("\n\ttaskCleanUpImpl = ");
         logBuilder.append(taskCleanUpImpl);
         logBuilder.append("\n\tenableLog = ");
         logBuilder.append(enableLog);
         logBuilder.append("\n\tkAgentMonitor = ");
         logBuilder.append(kAgentMonitor);
         logBuilder.append("\n\tkAgentRefreshHours = ");
         logBuilder.append(kAgentRefreshHours);
         log.info(logBuilder.toString());
    
    }
    
    @PreDestroy
    public void destroy() throws Exception {
        if (kagent != null) {
            kagent.dispose();
            kagent = null;
        }
    }
    

/******************************************************************************
 * *************        Drools KnowledgeBase Management               *********/
    
    // critical that each StatefulKnowledgeSession have its own JPA 'Environment'
    protected Environment createKnowledgeSessionEnvironment() {
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, jbpmCoreEMF);
        return env;
    }
    
    protected void checkKAgentAndBaseHealth() {
        long kAgentRefreshLapsedTime = System.currentTimeMillis() - lastKAgentRefresh;
        long kAgentRefreshMillis = 1000*60*kAgentRefreshHours;
        if(kbase == null || (kAgentMonitor && (kAgentRefreshLapsedTime > kAgentRefreshMillis))){
            log.info("checkKAgentAndBaseHealth() will now refresh kbase and kagent.  lapsed time "+kAgentRefreshLapsedTime+" is greater than "+kAgentRefreshMillis);
            kbase = null;
            createKnowledgeBaseViaKnowledgeAgentOrBuilder();
        }
    }
    
    public void createKnowledgeBaseViaKnowledgeAgentOrBuilder() {
        try {
            this.createKnowledgeBaseViaKnowledgeAgent();
        }catch(ConnectException x){
            log.warn("createKnowledgeBaseViaKnowledgeAgentOrBuilder() can not create a kbase via a kagent due to a connection problem with guvnor ... will now create kbase via knowledgeBuilder");
            rebuildKnowledgeBaseViaKnowledgeBuilder();
        }
    }

    public void createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder() {
        try {
            this.createKnowledgeBaseViaKnowledgeAgent(true);
        }catch(ConnectException x){
            log.warn("createOrRebuildKnowledgeBaseViaKnowledgeAgentOrBuilder() can not create a kbase via a kagent due to a connection problem with guvnor ... will now create kbase via knowledgeBuilder");
            rebuildKnowledgeBaseViaKnowledgeBuilder();
        }
    }
    
    public void rebuildKnowledgeBaseViaKnowledgeAgent() throws ConnectException{
        this.createKnowledgeBaseViaKnowledgeAgent(true);
    }
    protected void createKnowledgeBaseViaKnowledgeAgent() throws ConnectException{
        this.createKnowledgeBaseViaKnowledgeAgent(false);
    }

    // only one knowledgeBase object is needed and is shared amongst all StatefulKnowledgeSessions
    // needs to be invoked AFTER guvnor is available (obviously)
    // setting 'force' parameter to true re-creates an existing kbase
    protected synchronized void createKnowledgeBaseViaKnowledgeAgent(boolean forceRefresh) throws ConnectException{
        log.info("createOrRebuildKnowledgeBaseViaKnowledgeAgent() forceRefresh = "+forceRefresh);
        if(kbase != null && !forceRefresh)
            return;
        
        if(kagent != null) {
            kagent.dispose();
            kagent = null;
        }

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
        
        // Do not start change set notifications
        Integer droolsResourceScannerIntervalValue = -1;
        try {
            droolsResourceScannerIntervalValue = Integer.valueOf(droolsResourceScannerInterval);
        } catch (NumberFormatException nfe) {
            log.error("DroolsResourceScannerInterval is not an integer: " + droolsResourceScannerInterval, nfe);
        }
        if (droolsResourceScannerIntervalValue > 0) {
            sconf.setProperty( "drools.resource.scanner.interval", droolsResourceScannerInterval);
            ResourceFactory.getResourceChangeScannerService().configure( sconf );
            ResourceFactory.getResourceChangeScannerService().start();
            ResourceFactory.getResourceChangeNotifierService().start();
        }
        
        KnowledgeAgentConfiguration aconf = KnowledgeAgentFactory.newKnowledgeAgentConfiguration(); // implementation = org.drools.agent.impl.KnowledgeAgentConfigurationImpl

        /*  - incremental change set processing enabled
            - will create a single KnowledgeBase and always refresh that same instance
        */
        aconf.setProperty("drools.agent.newInstance", "false");
        if (droolsResourceScannerIntervalValue < 0) {
            aconf.setProperty("drools.agent.scanResources", Boolean.FALSE.toString());
            aconf.setProperty("drools.agent.scanDirectories", Boolean.FALSE.toString());
            aconf.setProperty("drools.agent.monitorChangeSetEvents", Boolean.FALSE.toString());
        }


        /*  -- Knowledge Agent provides automatic loading, caching and re-loading of resources
            -- the knowledge agent can update or rebuild this knowledge base as the resources it uses are changed
        */
        kagent = KnowledgeAgentFactory.newKnowledgeAgent("Guvnor default", aconf);
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
        lastKAgentRefresh = System.currentTimeMillis();
        log.info("createKnowledgeBaseViaKnowledgeAgent() just refreshed kBase via knowledgeAgent");
    }
    
    /*
     * intention of this function is to create a knowledgeBase without a strict dependency on guvnor
     * will still query guvnor for packages but will continue on even if problems communicating with guvnor exists
     * this function could be of use in those scenarios where guvnor is not accessible
     * knowledgeBase can subsequently be populated via one of the addProcessToKnowledgeBase(....) functions
     * in all cases, the knowledgeBase created by this function will NOT be registered with a knowledgeAgent that receives updates from guvnor
     */
    public void rebuildKnowledgeBaseViaKnowledgeBuilder() {
        guvnorProps = new Properties();
        try {
            KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            if(guvnorUtils.guvnorExists()) {
                guvnorProps.load(BaseKnowledgeSessionBean.class.getResourceAsStream("/jbpm.console.properties"));
                StringBuilder guvnorSBuilder = new StringBuilder();
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_PROTOCOL_KEY));
                guvnorSBuilder.append("://");
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_HOST_KEY));
                guvnorSBuilder.append("/");
                guvnorSBuilder.append(guvnorProps.getProperty(GuvnorConnectionUtils.GUVNOR_SUBDOMAIN_KEY));
                String guvnorURI = guvnorSBuilder.toString();
                List<String> packages = guvnorUtils.getPackageNames();
                if(packages.size() > 0){
                    for(String pkg : packages){
                        GuvnorRestApi guvnorRestApi = new GuvnorRestApi(guvnorURI);
                        try {
                            InputStream binaryPackage = guvnorRestApi.getBinaryPackage(pkg);
                            kbuilder.add(new InputStreamResource(binaryPackage), ResourceType.PKG);
                            guvnorRestApi.close();
                        } catch(java.io.IOException y) {
                            log.error("rebuildKnowledgeBaseViaKnowledgeBuilder() returned following exception when querying package = "+pkg+" : "+y);
                        }
                    }
                }else {
                    log.warn("rebuildKnowledgeBaseViaKnowledgeBuilder() no packages returned from Guvnor");
                }
            }else if(StringUtils.isNotEmpty(System.getProperty(this.CHANGE_SET_URL))){
                String changeSetUrl = System.getProperty(this.CHANGE_SET_URL);
                InputStream iStream = null;
                try{
                    iStream = new FileInputStream(changeSetUrl);
                    Resource rObj = new InputStreamResource(iStream);
                    kbuilder.add(rObj, ResourceType.PKG);
                }finally{
                    if(iStream != null)
                        iStream.close();
                }
            }else {
                throw new RuntimeException("rebuildKnowledgeBaseViaKnowledgeBuilder() guvnor does not exist and the following property is null: "+this.CHANGE_SET_URL);
            }
            kbase = kbuilder.newKnowledgeBase();
            log.info("rebuildKnowledgeBaseViaKnowledgeBuilder() just created kbase via KnowledgeBase");
        }catch(RuntimeException x){
            throw x;
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }
   
    // compile a process into a package and add it to the knowledge base 
    public void addProcessToKnowledgeBase(Process processObj, Resource resourceObj) {
        checkKAgentAndBaseHealth();
       
        PackageBuilder packageBuilder = new PackageBuilder();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl( packageBuilder );
        processBuilder.buildProcess( processObj, resourceObj);

        List<KnowledgePackage> kpackages = new ArrayList<KnowledgePackage>();
        kpackages.add( new KnowledgePackageImp( packageBuilder.getPackage() ) );
        kbase.addKnowledgePackages(kpackages);
        log.info("addProcessToKnowledgeBase() just added process obj to the kbase with Id : "+processObj.getId());
    }

    public void addProcessToKnowledgeBase(File bpmnFile) {
        checkKAgentAndBaseHealth();

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newFileResource(bpmnFile), ResourceType.BPMN2);
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        if( !kbuilder.hasErrors() ) {
            log.info("addProcessToKnowledgeBase() just added the following bpmn2 process definition to the kbase: "+bpmnFile.getName());
        } else {
            log.error("addProcessToKnowledgeBase() following errors occurred when adding bpmn2 process definition : "+bpmnFile.getName()+"\n\t"+kbuilder.getErrors().toString() );
        }
    }
    
    public String getAllProcessesInPackage(String pkgName) throws ConnectException{
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
        checkKAgentAndBaseHealth();

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("guvnor changesets:\n\t");
       
        if(guvnorChangeSet != null) 
            sBuilder.append(guvnorChangeSet);
        else
            sBuilder.append("not yet created by knowledgeAgent");

        Collection<KnowledgePackage> kPackages = kbase.getKnowledgePackages();
        if(kPackages != null && kPackages.size() > 0) {
            sBuilder.append("\nprintKnowledgeBaseContent()\n\t"); 
            for(KnowledgePackage kPackage : kPackages){
                Collection<Process> processes = kPackage.getProcesses();
                if(processes.size() == 0){
                    sBuilder.append("\n\tpackage = "+kPackage.getName()+" : no process definitions found ");
                }else {
                    for (Process process : processes) {
                        sBuilder.append("\n\tpackage = "+kPackage.getName()+" : pDef= " + process.getId()+" : pDefVersion= "+process.getVersion());
                    }
                }
            }
        } else {
            sBuilder.append("\n\nNo Packages found in kbase");
        }
        sBuilder.append("\n");
        return sBuilder.toString();
    }
    
    protected SessionTemplate newSessionTemplate() {
        //looking for session.templte on local file system
        if(templateString == null){
            synchronized(templateStringLockObj){
                if(templateString == null){
                    String droolsSessionTemplatePath = System.getProperty(DROOLS_SESSION_TEMPLATE_PATH);
                    if(StringUtils.isNotEmpty(droolsSessionTemplatePath)){
                        File droolsSessionTemplate = new File(droolsSessionTemplatePath);
                        if(!droolsSessionTemplate.exists()) {
                            log.error("newSessionTemplate() drools session template not found at : "+droolsSessionTemplatePath);
                            sessionTemplateInstantiationAttempts = -1;
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
            }
        }
        if(sessionTemplateInstantiationAttempts == -1)
            return null;
        if(sessionTemplateInstantiationAttempts == 0){
            synchronized(sessionTemplateInstantiationLock){
                if(sessionTemplateInstantiationAttempts == -1)
                    return null;
                return parseSessionTemplateString();
            }
        }
        return parseSessionTemplateString();
    }
    private SessionTemplate parseSessionTemplateString() {
        ParserConfiguration pconf = new ParserConfiguration();
        pconf.addImport("SessionTemplate", SessionTemplate.class);
        ParserContext context = new ParserContext(pconf);
        Serializable s = MVEL.compileExpression(templateString.trim(), context);
        try {
            SessionTemplate sTemplate = (SessionTemplate)MVEL.executeExpression(s);
            sessionTemplateInstantiationAttempts = 1;
            return sTemplate;
        }catch(Throwable x){
            sessionTemplateInstantiationAttempts = -1;
            x.printStackTrace();
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
    
    
    protected void registerWorkItemHandler(StatefulKnowledgeSession ksession, String serviceTaskName, WorkItemHandlerLifecycle handler) {
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
    
    protected void registerAddHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession) {
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
    protected void registerSkipHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession){
        try {
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(ITaskService.SKIP_TASK);
            WorkItemHandlerLifecycle handler = (WorkItemHandlerLifecycle)workItemHandlerClass.newInstance();
            registerWorkItemHandler(ksession, ITaskService.SKIP_TASK, handler);
            handler.init(ksession);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
    protected void registerFailHumanTaskWorkItemHandler(StatefulKnowledgeSession ksession){
        try {
            Class workItemHandlerClass = programmaticallyLoadedWorkItemHandlers.get(ITaskService.FAIL_TASK);
            WorkItemHandlerLifecycle handler = (WorkItemHandlerLifecycle)workItemHandlerClass.newInstance();
            registerWorkItemHandler(ksession, ITaskService.FAIL_TASK, handler);
            handler.init(ksession);
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    protected void registerEmailWorkItemHandler(StatefulKnowledgeSession ksession) {
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
    protected void addAgendaEventListener(Object ksession) {
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
    
    protected StatefulKnowledgeSession makeStatefulKnowledgeSession() {
        // 1) instantiate a KnowledgeBase via query to guvnor or kbuilder
        createKnowledgeBaseViaKnowledgeAgentOrBuilder();

        // 2) very important that a unique 'Environment' is created per StatefulKnowledgeSession
        Environment ksEnv = createKnowledgeSessionEnvironment();

        // Nick: always instantiate new ksconfig to make it threadlocal to bypass the ConcurrentModificationExcepotion
        KnowledgeSessionConfiguration ksConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(ksconfigProperties);

        // 3) instantiate StatefulKnowledgeSession
        //    make synchronize because under heavy load, appears that underlying SessionInfo.update() breaks with a NPE
        StatefulKnowledgeSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, ksConfig, ksEnv);
        return ksession;
    }  
    
    
    /******************************************************************************
    *************              Process Definition Management              *********/
        public List<SerializableProcessMetaData> retrieveProcesses() throws Exception {
            checkKAgentAndBaseHealth();
            List<SerializableProcessMetaData> result = new ArrayList<SerializableProcessMetaData>();
            for (KnowledgePackage kpackage: kbase.getKnowledgePackages()) {
                for(Process processObj : kpackage.getProcesses()){
                    result.add(getProcess(processObj.getId()));
                }
            }
            log.info("getProcesses() # of processes = "+result.size());
            return result;
        }

        public SerializableProcessMetaData getProcess(String processId) {
            checkKAgentAndBaseHealth();
            Process processObj = kbase.getProcess(processId);
            Long pVersion = 0L;
            if(!StringUtils.isEmpty(processObj.getVersion())) {
                try {
                    pVersion = Long.parseLong(processObj.getVersion());
                } catch(NumberFormatException x) {
                    log.error("getProcess() processId = "+processId+" : process versions must be of type long. the following is invalid: "+processObj.getVersion());
                }
            }
            SerializableProcessMetaData spObj = new SerializableProcessMetaData(processObj.getId(), processObj.getName(), pVersion, processObj.getPackageName());
            if (processObj instanceof org.drools.definition.process.WorkflowProcess) {
                Node[] nodes = ((WorkflowProcess)processObj).getNodes();
                addNodesInfo(spObj.getNodes(), nodes, "id=");
            }
            return spObj;
        }
        private void addNodesInfo(List<SerializableNodeMetaData> snList, Node[] nodes, String prefix) {
            for(Node nodeObj : nodes) {
                // JA Bride:  AsyncBAMProducer has been modified from stock jbpm5 to persist the "uniqueNodeId" in the jbpm_bam database
                //  (as opposed to persisting just the simplistic nodeId)
                //  will need to invoke same functionality here to calculate 'uniqueNodeId' 
                String uniqueId = org.jbpm.bpmn2.xml.XmlBPMNProcessDumper.getUniqueNodeId(nodeObj);
                SerializableNodeMetaData snObj = new SerializableNodeMetaData(
                        (Integer)nodeObj.getMetaData().get(SerializableNodeMetaData.X),
                        (Integer)nodeObj.getMetaData().get(SerializableNodeMetaData.Y),
                        (Integer)nodeObj.getMetaData().get(SerializableNodeMetaData.HEIGHT),
                        (Integer)nodeObj.getMetaData().get(SerializableNodeMetaData.WIDTH),
                        uniqueId                                                      
                        );
                snList.add(snObj);
                if (nodeObj instanceof NodeContainer) {
                    addNodesInfo(snObj.getNodes(), ((NodeContainer)nodeObj).getNodes(), prefix + nodeObj.getId() + ":");
                }
            }
        }
    
    public void removeProcess(String processId) {
        throw new UnsupportedOperationException();
    }

    public List<ProcessInstanceInfo> getActiveProcessInstances(Map<String, Object> queryCriteria) {
         EntityManager psqlEm = null;
         List<ProcessInstanceInfo> results = null;
         StringBuilder sqlBuilder = new StringBuilder();
         sqlBuilder.append("FROM ProcessInstanceInfo p ");
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
             throw new RuntimeException(x);
         }
     }
    
    public String printActiveProcessInstances(Map<String,Object> queryCriteria){
        List<ProcessInstanceInfo> pInstances = getActiveProcessInstances(queryCriteria);
        StringBuffer sBuffer = new StringBuffer();
        if(pInstances != null){
            sBuffer.append("\npInstanceId\tprocessId");
            for(ProcessInstanceInfo pInstance: pInstances){
                sBuffer.append("\n"+pInstance.getId()+"\t"+pInstance.getProcessId());
            }
            sBuffer.append("\n");
        }else{
            sBuffer.append("\nno active process instances found\n");
        }
        return sBuffer.toString();
    }
    
    private static final List<Integer> NON_ROLLBACK_TX = Arrays.asList(new Integer[]{
            javax.transaction.Status.STATUS_NO_TRANSACTION,
            javax.transaction.Status.STATUS_ROLLEDBACK
    });
    protected void rollbackTrnx() {
        try {
            if(!NON_ROLLBACK_TX.contains(uTrnx.getStatus())) {
                uTrnx.rollback();
            }
        } catch(Exception e) {
            log.error(e.getMessage() + " - at: " + (e.getStackTrace() == null ? null : e.getStackTrace()[0]));
        }
    }
}
