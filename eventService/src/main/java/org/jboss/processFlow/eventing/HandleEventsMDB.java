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

package org.jboss.processFlow.eventing;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.ejb3.annotation.AspectDomain;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.ejb3.annotation.Depends;
import org.jboss.mx.util.MBeanServerLocator;

import org.jboss.processFlow.domain.TaskWithDrawData;

@MessageDriven(activationConfig = {
// properties can be found in:  org.hornetq.ra.inflow.HornetQActivationSpec
// These activation config properties can be used to over-ride properties of '<inbound-resourceadapter>' in ra.xml of JCA resource adapter of JMS Provider
    @ActivationConfigProperty(propertyName="destination", propertyValue="queue/processFlow.eventQueue"),
    @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
    @ActivationConfigProperty(propertyName="maxSession", propertyValue="5"),    // max # of MDB sessions in pool
    @ActivationConfigProperty(propertyName="minSession", propertyValue="1")

})
@TransactionManagement(TransactionManagementType.CONTAINER)
@AspectDomain("Message Driven Bean") // referenced in deploy/ejb3-interceptors-aop.xml
public class HandleEventsMDB implements MessageListener {

    public static final String EVENTING_EMF = "java:/eventingEMF";
    private static final String EVENT_COMMAND = "EventCommand";
    private static final String PROCESS_ID = "ProcessID";
    private static final String PARAMETERS = "Parameters";
    private static Logger log = Logger.getLogger("HandleEventsMDB");
    private static ObjectMapper jacksonMapper = new ObjectMapper();
    private EntityManagerFactory emf = null;
    private static ObjectName knowledgeSessionObjName = null;
    private static MBeanServer mBeanServer = null;
    


    @PostConstruct
    public void postConstruct() throws javax.management.MalformedObjectNameException, NamingException {
        
        Context jndiContext = new InitialContext();
        emf = (EntityManagerFactory)jndiContext.lookup(EVENTING_EMF);
        if (knowledgeSessionObjName == null)
        	knowledgeSessionObjName = new ObjectName("org.jboss.processFlow.knowledgeService:service=KnowledgeSessionService");
        if (mBeanServer == null)
        	mBeanServer = MBeanServerLocator.locateJBoss();
        log.info("postConstruct() emf = "+emf);
    }

    public HandleEventsMDB() {
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        try {
           if (message instanceof TextMessage) {
                TextMessage tMessage = (TextMessage)message;
                String text = tMessage.getText();
                TaskWithDrawData withdrawObj = jacksonMapper.readValue(text, TaskWithDrawData.class);
                log.info("onMessage()  messageId = "+tMessage.getJMSMessageID()+" :  redelivered = "+tMessage.getJMSRedelivered()+" : text = "+text);
                String eventType = tMessage.getStringProperty(TaskWithDrawData.EVENT_TYPE);
                if(TaskWithDrawData.REGISTER.equals(eventType)) {
                    register(withdrawObj);
                } else if (TaskWithDrawData.SIGNAL.equals(eventType)) {
                    signal(withdrawObj);
                } else {
                    throw new Exception("onMessage() message with following eventId not supported : "+eventType);   
                }
           } else if (message instanceof ObjectMessage) {
               ObjectMessage oMessage = (ObjectMessage)message;
               String eventCommand = oMessage.getStringProperty(EVENT_COMMAND);
               String processId = oMessage.getStringProperty(PROCESS_ID);
               if (processId == null || processId.length() == 0) {
            	   // throw checked exception
               }
               Object objectParameter = oMessage.getObject();
               Map mapParameter = null;
               if (objectParameter != null && objectParameter instanceof Map) {
            	   mapParameter = (Map)objectParameter;
               } else {
            	   // throw checked exception
               }
            	   
               if ("StartProcess".equals(eventCommand)) {
            	   try {
            		   long processInstanceId = (Long)mBeanServer.invoke(knowledgeSessionObjName, "startProcessAndReturnId",
                           new Object[]{processId, mapParameter},
                           new String[]{"java.lang.String", "java.util.Map"}
                       );
            		   log.info("run() just created processInstance with id = "+processInstanceId);
            	   } catch(Exception ex) {
            		   throw new RuntimeException(ex);
            	   }
               } else {
                    log.error("onMessage() unidentified event command : "+eventCommand);
               }            	   
           } else {
                throw new RuntimeException("onMessage() eventService will not accept message of type = "+message.getClass());
           }
        } catch(Throwable x) {
            // if this is a redelivered message, will send to DLQ
            try {
                if (message.getJMSRedelivered()) {
                    log.error("onMessage() "+ message.getJMSMessageID()+ " threw RuntimeException again on re-delivered message ... will send to DLQ");
                    // TO-DO : send message to DLQ for error handling processing and audit
                } else {
                    x.printStackTrace();
                }
            } catch (JMSException j) {
                j.printStackTrace();
            }
        }
    }

    private void register(TaskWithDrawData withdrawObj) throws Exception {
        EntityManager eventsManager = null;
        try {
            eventsManager = emf.createEntityManager();
            eventsManager.persist(withdrawObj);
        } finally {
            if(eventsManager != null)
                eventsManager.close();
        }
    }
    
    private void signal(TaskWithDrawData withdrawObj) throws Exception {
        EntityManager eventsManager = null;
        try {
            eventsManager = emf.createEntityManager();
            //1.  query for TaskWithDrawData processInstanceId
            //TO-DO:  need to better understand the query to pull out process instances
            Query qObj = eventsManager.createNamedQuery(TaskWithDrawData.FIND_TASKWITHDRAWDATA_BY_COMBO);
            qObj = qObj.setParameter(TaskWithDrawData.SIGNAL_TYPE, withdrawObj.getSignalType());
            qObj = qObj.setParameter(TaskWithDrawData.SERVICE_LINE, withdrawObj.getServiceLine());
            qObj = qObj.setParameter(TaskWithDrawData.ACCOUNT_NUMBER, withdrawObj.getAccountNumber());
            qObj = qObj.setParameter(TaskWithDrawData.BRAND_ID, withdrawObj.getBrandId());
            
            List withdrawObjList = (List)qObj.getResultList();
            log.info("signal() withdrawObjList size = "+withdrawObjList.size());
            Iterator withdrawObjIterator = withdrawObjList.iterator();
            while(withdrawObjIterator.hasNext()){
                TaskWithDrawData tObj = (TaskWithDrawData)withdrawObjIterator.next();
                
                //2.  signal process
                mBeanServer.invoke(knowledgeSessionObjName, "signalEvent", 
                        new Object[]{String.valueOf(tObj.getProcInstanceId()), tObj.getSignalType(), tObj.getSignalType()},
                        new String[]{"java.lang.String", "java.lang.String", "java.lang.String"});
            }
        }finally {
            if(eventsManager != null)
                eventsManager.close();
        }
    }
}
