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
import javax.inject.Inject;
import javax.jms.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceMXBean;
import org.jboss.processFlow.util.MessagingUtil;
/**
 *<pre>
 *notes on Transactions
 *  - most publicly exposed methods in this singleton assumes a container managed trnx demarcation of REQUIRED
 *  - in some methods, bean managed transaction demarcation is used IOT dispose of the ksession *AFTER* the transaction has committed
 *  - otherwise, the method will fail due to implementation of JBRULES-1880
 *</pre>
 */
@Remote(IKnowledgeSessionService.class)
@Local(IBaseKnowledgeSession.class)
@Singleton(name="prodKSessionProxy")
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KnowledgeSessionService implements IKnowledgeSession, KnowledgeSessionServiceMXBean {
    
    public static final String EMF_NAME = "org.jbpm.persistence.jpa";
    private Logger log = Logger.getLogger("KnowledgeSessionService");
    
    @Inject
    private RuntimeManagerFactory rmFactory;
    
    @javax.annotation.Resource (name=MessagingUtil.CONNECTION_FACTORY_JNDI_NAME)
    ConnectionFactory cFactory;
    
    @PersistenceUnit(unitName=EMF_NAME)
    EntityManagerFactory jbpmCoreEMF;
    
    protected ObjectName objectName;
    protected MBeanServer platformMBeanServer;

    private final String gwDObjName = "jms/processFlow.knowledgeSessionQueue";
    private Destination gwDObj = null;
    private Connection connectionObj = null;
    private String sessionMgmtStrategy = IKnowledgeSessionService.DEFAULT_PER_PINSTANCE;
    private RuntimeManager rManager = null;
    
    @PostConstruct
    public void start() throws Exception {
        try {
            objectName = new ObjectName("org.jboss.processFlow:type="+this.getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);

            connectionObj = cFactory.createConnection();
            gwDObj = (Destination)MessagingUtil.grabJMSObject(gwDObjName);
            
        } catch(Exception x) {
            throw new RuntimeException(x);
        }finally {
        }
    }
    
    @PreDestroy 
    public void stop() throws Exception{
        log.info("stop");
        try {
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new RuntimeException("stop() Problem during unregistration of Monitoring into JMX:" + e);
        }
    }
    
    public void addAssetToRuntimeEnvironment(File processFile){
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
                dispose(kSession);
            }
        }
    }
    
    public void signalEvent(String signalType, Object signalValue, Long processInstanceId) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            kSession.signalEvent(signalType, signalValue, processInstanceId);
        }finally {
            dispose(kSession);
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
                dispose(kSession);
            }
        }
    }

    public void upgradeProcessInstance(long processInstanceId, String processId, Map<String, Long> nodeMapping) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            WorkflowProcessInstanceUpgrader.upgradeProcessInstance(kSession, processInstanceId, processId, nodeMapping);
        }finally {
            dispose(kSession);
        }
    }

    public void abortProcessInstance(Long processInstanceId) {
        KieSession kSession = null;
        try {
            kSession = rManager.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId)).getKieSession();
            kSession.abortProcessInstance(processInstanceId);
        }finally {
            dispose(kSession);
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
            dispose(kSession);
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
            dispose(kSession);
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

    private void dispose(KieSession kSession){
        kSession.dispose();
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
