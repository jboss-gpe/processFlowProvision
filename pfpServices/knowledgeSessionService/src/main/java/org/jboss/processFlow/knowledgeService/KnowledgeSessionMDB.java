package org.jboss.processFlow.knowledgeService;


import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@MessageDriven
public class KnowledgeSessionMDB implements MessageListener {
    
    //@EJB(lookup="java:global/processFlow-knowledgeSessionService/prodKSessionProxy!org.jboss.processFlow.knowledgeService.IKnowledgeSessionService")
    IKnowledgeSessionService kProxy;
    
    private Logger log = LoggerFactory.getLogger("KSessionHttp");

    @Override
    public void onMessage(Message mObj) {
        try {
            if(!(mObj instanceof ObjectMessage))
                throw new MessageFormatException("onMessage() following message type not supported: "+mObj.getClass().toString());
            ObjectMessage oMessage = (ObjectMessage)mObj;
            String operationType = mObj.getStringProperty(IKnowledgeSessionService.COMPLETE_WORK_ITEM);
            if(StringUtils.isEmpty(operationType))
                throw new MessageFormatException("onMessage() need to include String property with key = "+IKnowledgeSessionService.COMPLETE_WORK_ITEM);
            if(operationType.equals(IKnowledgeSessionService.START_PROCESS_AND_RETURN_ID)){
                String processId = mObj.getStringProperty(IKnowledgeSessionService.PROCESS_ID);
                if(StringUtils.isEmpty(processId))
                    throw new MessageFormatException("onMessage() need to include String property with key = "+IKnowledgeSessionService.PROCESS_ID);
                Map<String, Object> pInstanceVariables = (Map<String,Object>)oMessage.getObject();
                log.info("onMessage() about to startProcessInstance for processId = "+processId);
                Map<String, Object> responseObj = kProxy.startProcessAndReturnId(processId, pInstanceVariables);
                
                //TO-DO:  send this response back to a queue
            }else if(operationType.equals(IKnowledgeSessionService.COMPLETE_WORK_ITEM)){
                
            }else
                throw new MessageFormatException("onMessage() following operationType is not supported: "+operationType);
        } catch(RuntimeException x){
            throw x;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
