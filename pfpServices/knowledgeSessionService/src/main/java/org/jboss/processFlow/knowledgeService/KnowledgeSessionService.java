/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.jms.*;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceMXBean;
import org.jboss.processFlow.tasks.identity.PFPUserGroupCallback;
import org.jboss.processFlow.util.CMTDisposeCommand;
import org.jboss.processFlow.cdi.TestSingleton;


@Remote(IKnowledgeSessionService.class)
@Local(IBaseKnowledgeSession.class)
@Singleton(name="prodKSessionProxy")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KnowledgeSessionService implements IKnowledgeSession, KnowledgeSessionServiceMXBean {
    
    public static final String EMF_NAME = "org.jbpm.persistence.jpa";
    private Logger log = Logger.getLogger("KnowledgeSessionService");
    
    @javax.annotation.Resource (name="java:/RemoteConnectionFactory")
    ConnectionFactory cFactory;

    @javax.annotation.Resource (name="java:/queue/processFlow.knowledgeSessionQueue")
    private Destination gwDObj;
    
    @PersistenceUnit(unitName=EMF_NAME)
    EntityManagerFactory jbpmCoreEMF;

    @Inject
    private TestSingleton tSingleton;
    
    protected ObjectName objectName;
    protected MBeanServer platformMBeanServer;

    private Connection connectionObj = null;
    private RuntimeEnvironmentBuilder reBuilder = null;
    private RuntimeManager rManager = null;
    private RuntimeEnvironment rEnvironment = null;
    
    @PostConstruct
    public void start() throws Exception {
        try {
            String nameString = "META-INF/Taskorm.xml";
            java.io.InputStream iStream = this.getClass().getClassLoader().getResourceAsStream(nameString);
            if(iStream == null)
                throw new Exception("can not find : "+nameString);
            
            objectName = new ObjectName("org.jboss.processFlow:type="+this.getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);

            connectionObj = cFactory.createConnection();
            

            createRuntimeEnvironmentBuilder();
            
        } catch(Exception x) {
            throw new RuntimeException(x);
        }finally {
        }
    }
    
    @PreDestroy 
    public void stop() throws Exception{
        log.info("stop");
        try {
            rManager.close();
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new RuntimeException("stop() Problem during unregistration of Monitoring into JMX:" + e);
        }
    }
    
    private void createRuntimeEnvironmentBuilder() {
        reBuilder = RuntimeEnvironmentBuilder.getDefault()
            .registerableItemsFactory(new org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory())
            .entityManagerFactory(this.jbpmCoreEMF)
            .userGroupCallback(new PFPUserGroupCallback());
    }
    
    private synchronized void createRuntimeManager() {
        if(rEnvironment != null)
            rEnvironment.close();
        
        if(rManager != null)
            rManager.close();
            
        rEnvironment = reBuilder.get();
        rManager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(rEnvironment);
    }
    
    public void addAssetToRuntimeEnvironment(File processFile){
        reBuilder.addAsset(ResourceFactory.newFileResource(processFile), ResourceType.BPMN2);
        
        this.createRuntimeManager();
    }
    
    public void addAssetToRuntimeEnvironment(Process processObj, Resource resourceObj){
        
    }
    
    public void addProcessToKnowledgeBase(File processFile){
        this.addAssetToRuntimeEnvironment(processFile);
    }
    
    /**
     *startProcessAndReturnId
     *<pre>
     *- this method will block until the newly created process instance either completes or arrives at a wait state
     *- at completion of the process instance (or arrival at a wait state), the StatefulKnowledgeSession will be disposed
     *- bean managed transaction demarcation is used by this method IOT dispose of the ksession *AFTER* the transaction has committed
     *- otherwise, this method will fail due to implementation of JBRULES-1880
     *
     *  will deliver to KSessionManagement via JMS if inbound pInstanceVariables map contains an entry keyed by IKnowledgeSessionService.DELIVER_ASYNC
     *</pre>
     */
    public Map<String, Object> startProcessAndReturnId(String processId, Map<String, Object> pInstanceVariables) {
        if(pInstanceVariables != null && pInstanceVariables.containsKey(IKnowledgeSessionService.DELIVER_ASYNC)) {
            Session sessionObj = null;
            try {
                pInstanceVariables.remove(IKnowledgeSessionService.DELIVER_ASYNC);
                sessionObj = connectionObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer m_sender = sessionObj.createProducer(gwDObj);
                ObjectMessage oMessage = sessionObj.createObjectMessage();
                oMessage.setObject((HashMap<String,Object>)pInstanceVariables);
                oMessage.setStringProperty(IKnowledgeSessionService.OPERATION_TYPE, IKnowledgeSessionService.START_PROCESS_AND_RETURN_ID);
                oMessage.setStringProperty(IKnowledgeSessionService.PROCESS_ID, processId);
                m_sender.send(oMessage);
                Map<String, Object> returnMap = new HashMap<String, Object>();
                returnMap.put(IKnowledgeSession.PROCESS_INSTANCE_ID, new Long(0));
                return returnMap;
            } catch(JMSException x) {
                throw new RuntimeException(x);
            }finally {
                if(sessionObj != null) {
                    try { sessionObj.close(); }catch(Exception x){ x.printStackTrace(); }
                }
            }
        } else {
            KieSession kSession = null;
            try {
                
                kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get()).getKieSession();
                ProcessInstance pInstance = kSession.startProcess(processId, pInstanceVariables);
                
                Map<String, Object> returnMap = new HashMap<String, Object>();
                returnMap.put(IKnowledgeSessionService.PROCESS_INSTANCE_ID, pInstance.getId());
                returnMap.put(IKnowledgeSessionService.PROCESS_INSTANCE_STATE, pInstance.getState());
                returnMap.put(IKnowledgeSessionService.KSESSION_ID, kSession.getId());
                return returnMap;
            }finally {
                kSession.execute(new CMTDisposeCommand());
            }
        }
    }
    
    public void signalEvent(String signalType, Object signalValue, Long processInstanceId) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            kSession.signalEvent(signalType, signalValue, processInstanceId);
        }finally {
            kSession.execute(new CMTDisposeCommand());
        }
    }
    
    /**
     * completeWorkItem
     * <pre>
     * this method will block until the existing process instance either completes or arrives at a wait state
     *
     * will deliver to KSessionManagement via JMS if inbound pInstanceVariables map contains an entry keyed by IKnowledgeSessionService.DELIVER_ASYNC
     * </pre>
     */ 
    public void completeWorkItem(Long workItemId, Map<String, Object> pInstanceVariables, Long pInstanceId) {
        if(pInstanceVariables != null && pInstanceVariables.containsKey(IKnowledgeSessionService.DELIVER_ASYNC)) {
            Session sessionObj = null;
            try {
                pInstanceVariables.remove(IKnowledgeSessionService.DELIVER_ASYNC);
                sessionObj = connectionObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer m_sender = sessionObj.createProducer(gwDObj);
                ObjectMessage oMessage = sessionObj.createObjectMessage();
                oMessage.setObject((HashMap<String,Object>)pInstanceVariables);
                oMessage.setStringProperty(IKnowledgeSessionService.OPERATION_TYPE, IKnowledgeSessionService.COMPLETE_WORK_ITEM);
                oMessage.setLongProperty(IKnowledgeSessionService.WORK_ITEM_ID, workItemId);
                oMessage.setLongProperty(IKnowledgeSessionService.PROCESS_INSTANCE_ID, pInstanceId);
                m_sender.send(oMessage);
            } catch(JMSException x) {
                throw new RuntimeException(x);
            }finally {
                if(sessionObj != null) {
                    try { sessionObj.close(); }catch(Exception x){ x.printStackTrace(); }
                }
            }
        } else {
            KieSession kSession = null;
            try {
                kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(pInstanceId)).getKieSession();
                kSession.getWorkItemManager().completeWorkItem(workItemId, pInstanceVariables);
                kSession.dispose();
            }finally {
                kSession.execute(new CMTDisposeCommand());
            }
        }
    }

    public void upgradeProcessInstance(long processInstanceId, String processId, Map<String, Long> nodeMapping) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            WorkflowProcessInstanceUpgrader.upgradeProcessInstance(kSession, processInstanceId, processId, nodeMapping);
        }finally {
            kSession.execute(new CMTDisposeCommand());
        }
    }

    public void abortProcessInstance(Long processInstanceId) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            kSession.abortProcessInstance(processInstanceId);
        }finally {
            kSession.execute(new CMTDisposeCommand());
        }
    }
    
    @SuppressWarnings("unchecked")
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
            results = (List<ProcessInstanceInfo>)processInstanceQuery.getResultList();
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
    
    public Map<String, Object> getActiveProcessInstanceVariables(Long processInstanceId){
        KieSession kSession = null;
        Map<String, Object> returnMap = new HashMap<String, Object>();
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            ProcessInstance pInstance = kSession.getProcessInstance(processInstanceId, Boolean.TRUE.booleanValue());
            if (pInstance != null) {
                Map<String, Object> variables = ((WorkflowProcessInstanceImpl) pInstance).getVariables();
                if (variables == null) {
                    return returnMap;
                }
                // filter out null values
                for (Map.Entry<String, Object> entry: variables.entrySet()) {
                    if (entry.getValue() != null) {
                        returnMap.put(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                log.error("getActiveProcessInstanceVariables() :  Could not find process instance " + processInstanceId);
            }
        }finally {
            kSession.execute(new CMTDisposeCommand());
        }
        return returnMap;
    }
    
    public void setProcessInstanceVariables(Long processInstanceId, Map<String, Object> variables){
        KieSession kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
        ProcessInstance pInstance = kSession.getProcessInstance(processInstanceId, Boolean.TRUE.booleanValue());
        try {
            if (pInstance != null) {
                VariableScopeInstance variableScope = (VariableScopeInstance)((org.jbpm.process.instance.ProcessInstance) pInstance).getContextInstance(VariableScope.VARIABLE_SCOPE);
                if (variableScope == null) {
                    throw new IllegalArgumentException("Could not find variable scope for process instance " + processInstanceId);
                }
                for (Map.Entry<String, Object> entry: variables.entrySet()) {
                    variableScope.setVariable(entry.getKey(), entry.getValue());
                }
            } else {
                throw new IllegalArgumentException("Could not find process instance " + processInstanceId);
            }
        }finally{
            kSession.execute(new CMTDisposeCommand());
        }
    }
    
    public String printActiveProcessInstanceVariables(Long processInstanceId) {
        Map<String,Object> vHash = getActiveProcessInstanceVariables(processInstanceId);
        if(vHash.size() == 0)
            log.error("printActiveProcessInstanceVariables() no process instance variables for :\n\tprocessInstanceId = "+processInstanceId);
        
        StringWriter sWriter = null;
        try {
            sWriter = new StringWriter();
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.writeValue(sWriter, vHash);
            return sWriter.toString();
        }catch(Exception x){
            throw new RuntimeException(x);
        }finally {
            if(sWriter != null) {
                try { sWriter.close();  }catch(Exception x){x.printStackTrace();}
            }
        }
    }

    @Override
    public String printWorkItemHandlers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void rebuildKnowledgeBaseViaKnowledgeAgent() throws ConnectException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void rebuildKnowledgeBaseViaKnowledgeBuilder() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String printKnowledgeBaseContent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAllProcessesInPackage(String pkgName) throws ConnectException {
        return null;
    }

    @Override
    public String dumpSessionStatusInfo() {
        // TODO Auto-generated method stub
        return null;
    }
}
