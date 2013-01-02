package org.jboss.processFlow.knowledgeService;


import java.io.File;
import java.io.Serializable;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;


import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MessageDriven(name="KnowledgeSessionMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue="java:/queue/processFlow.knowledgeSessionQueue")
})
public class KnowledgeSessionMDB implements MessageListener {
    
    private static Connection connectObj = null;
    private Logger log = LoggerFactory.getLogger("KnowledgeSessionMDB");
    private static String jbossNodeName;
    
    @EJB(lookup=IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI)
    IKnowledgeSessionService kProxy;
    
    @Resource MessageDrivenContext mCtx;
    @Resource(name="java:/ConnectionFactory") ConnectionFactory cFactory;
    
    @PostConstruct
    void init() throws JMSException{
        if(connectObj == null){
            connectObj = cFactory.createConnection();
            log.info("init()  connectObj = "+connectObj);
            jbossNodeName = System.getProperty("jboss.node.name");
        }
    }

    @Override
    public void onMessage(Message mObj) {
        try {
            if(!(mObj instanceof ObjectMessage))
                throw new MessageFormatException("onMessage() following message type not supported: "+mObj.getClass().toString());
            ObjectMessage oMessage = (ObjectMessage)mObj;
            String operationType = mObj.getStringProperty(IKnowledgeSessionService.OPERATION_TYPE);
            if(StringUtils.isEmpty(operationType))
                throw new MessageFormatException("onMessage() need to include String property with key = "+IKnowledgeSessionService.OPERATION_TYPE);
            Integer ksessionId = null;
            if(mObj.propertyExists(IKnowledgeSessionService.KSESSION_ID))
                ksessionId = mObj.getIntProperty(IKnowledgeSessionService.KSESSION_ID);
            Long pInstanceId = null;
            if(mObj.propertyExists(IKnowledgeSessionService.PROCESS_INSTANCE_ID))
                pInstanceId = mObj.getLongProperty(IKnowledgeSessionService.PROCESS_INSTANCE_ID);
            
            // ADD PROCESS TO KNOWLEDGE BASE
            if(operationType.equals(IKnowledgeSessionService.ADD_PROCESS_TO_KNOWLEDGE_BASE)){
                File bpmnFile = (File)oMessage.getObject();
                if(bpmnFile == null)
                    throw new MessageFormatException("onMessage() need to include Object property with key = "+IKnowledgeSessionService.BPMN_FILE);
                kProxy.addProcessToKnowledgeBase(bpmnFile);
                
               
            // START PROCESSS AND RETURN ID    
            }else if(operationType.equals(IKnowledgeSessionService.START_PROCESS_AND_RETURN_ID)){
                String processId = mObj.getStringProperty(IKnowledgeSessionService.PROCESS_ID);
                if(StringUtils.isEmpty(processId))
                    throw new MessageFormatException("onMessage() need to include String property with key = "+IKnowledgeSessionService.PROCESS_ID);
                Map<String, Object> pInstanceVariables = (Map<String,Object>)oMessage.getObject();
                log.info("onMessage() about to startProcessInstance for processId = "+processId);
                Map<String, Object> responseFromKProxy = kProxy.startProcessAndReturnId(processId, pInstanceVariables);
                if(mObj.getJMSReplyTo() != null){
                    Session producerSession = null;
                    try {
                        producerSession = connectObj.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        Message mResponse = producerSession.createObjectMessage((Serializable) responseFromKProxy);
                        mResponse.setJMSCorrelationID(mObj.getJMSCorrelationID());
                        mResponse.setStringProperty(IKnowledgeSessionService.NODE_ID, jbossNodeName);
                        MessageProducer producer = producerSession.createProducer(null);
                        producer.send(mObj.getJMSReplyTo(), mResponse);
                    }finally{
                        if(producerSession != null)
                            producerSession.close();
                    }
                }
                
            
            //COMPLETE WORK ITEM
            }else if(operationType.equals(IKnowledgeSessionService.COMPLETE_WORK_ITEM)){
                Map<String, Object> pInstanceVariables = (Map<String,Object>)oMessage.getObject();
                Long workItemId = mObj.getLongProperty(IKnowledgeSessionService.WORK_ITEM_ID);
                if(workItemId == 0)
                    throw new MessageFormatException("onMessage() need to include Int property with key = "+IKnowledgeSessionService.WORK_ITEM_ID);
                if(pInstanceId == 0L)
                    throw new MessageFormatException("onMessage() need to include Long property with key = "+IKnowledgeSessionService.PROCESS_INSTANCE_ID);
                kProxy.completeWorkItem(workItemId, pInstanceVariables, pInstanceId, ksessionId);
                
            
                
            //SIGNAL EVENT
            }else if(operationType.equals(IKnowledgeSessionService.SIGNAL_EVENT)){
                String type = mObj.getStringProperty(IKnowledgeSessionService.SIGNAL_TYPE);
                if(StringUtils.isEmpty(type))
                    throw new MessageFormatException("onMessage() need to include String property with key = "+IKnowledgeSessionService.SIGNAL_TYPE);
                Object eventData = oMessage.getObject();
                if(eventData == null)
                    throw new MessageFormatException("onMessage() need to include Object property in body of message");
                if(pInstanceId == 0L)
                    throw new MessageFormatException("onMessage() need to include Long property with key = "+IKnowledgeSessionService.PROCESS_INSTANCE_ID);
                kProxy.signalEvent(type, eventData, pInstanceId, ksessionId);
                
                
            }else
                throw new MessageFormatException("onMessage() following operationType is not supported: "+operationType);
        } catch(RuntimeException x){
            throw x;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
