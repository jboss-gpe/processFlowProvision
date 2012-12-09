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
import javax.jms.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.definition.process.Process;
import org.drools.io.*;
import org.drools.runtime.process.ProcessInstance;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.KnowledgeSessionServiceMXBean;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;

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
public class KnowledgeSessionService implements IKnowledgeSessionService, KnowledgeSessionServiceMXBean {
    
    @Inject
    private IKnowledgeSessionBean kBean;
    
    protected ObjectName objectName;
    protected MBeanServer platformMBeanServer;

    private final String cFactoryName = "/ConnectionFactory";
    private final String gwDObjName = "queue/processFlow.knowledgeSessionQueue";
    private Destination gwDObj = null;
    private Connection connectionObj = null;
    
    @PostConstruct
    public void start() throws Exception {
        Context jndiContext = null;
        try {
            objectName = new ObjectName("org.jboss.processFlow:type="+this.getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);

            jndiContext = new InitialContext();
            ConnectionFactory cFactory = (ConnectionFactory)jndiContext.lookup(cFactoryName);
            connectionObj = cFactory.createConnection();
            gwDObj = (Destination)jndiContext.lookup(gwDObjName);
            jndiContext.close(); 
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
    public String printActiveProcessInstances(Map<String,Object> queryCriteria) {
        return kBean.printActiveProcessInstances(queryCriteria);
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
    public List<ProcessInstanceInfo> getActiveProcessInstances(Map<String, Object> queryCriteria) {
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
     *
     *  will deliver to KSessionManagement via JMS if inbound pInstanceVariables map contains an entry keyed by IKnowledgeSessionService.DELIVER_ASYNC
     *</pre>
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
            return kBean.startProcessAndReturnId(processId, pInstanceVariables);
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
    public void completeWorkItem(Integer ksessionId, Long workItemId, Map<String, Object> pInstanceVariables) {
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
                oMessage.setIntProperty(IKnowledgeSessionService.KSESSION_ID, ksessionId);
                m_sender.send(oMessage);
            } catch(JMSException x) {
                throw new RuntimeException(x);
            }finally {
                if(sessionObj != null) {
                    try { sessionObj.close(); }catch(Exception x){ x.printStackTrace(); }
                }
            }
        } else {
            kBean.completeWorkItem(ksessionId, workItemId, pInstanceVariables);
        }
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
    public void upgradeProcessInstance(long processInstanceId, String processId, Map<String, Long> nodeMapping) {
        kBean.upgradeProcessInstance(processInstanceId, processId, nodeMapping);
    }
}
