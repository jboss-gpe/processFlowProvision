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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;

import org.drools.audit.event.LogEvent;
import org.drools.event.process.DefaultProcessEventListener;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;

import org.jboss.processFlow.bam.SubProcessInstanceLog;
import org.jboss.processFlow.bam.HumanTaskLog;

public class AsyncBAMProducer extends DefaultProcessEventListener {

    public static final String LOG_EVENT_TYPE = "logEventType";

    private static final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
    private static final MarshallingConfiguration configuration = new MarshallingConfiguration();

    private BAMProducerWrapper pWrapper;

    static {
        configuration.setVersion(3);
    }

    public void setBAMProducerWrapper(BAMProducerWrapper x) {
        pWrapper = x;
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        try {
            ProcessInstanceLog log = new ProcessInstanceLog(event.getProcessInstance().getId(), event.getProcessInstance().getProcessId());
            sendBAMEvent(LogEvent.BEFORE_RULEFLOW_CREATED, log);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        try {
            ProcessInstanceLog log = new ProcessInstanceLog(event.getProcessInstance().getId(), event.getProcessInstance().getProcessId());
            // Nick: set the end date before pass it to BAM, to make sure the log date is consistent on the same server
            log.setEnd(new Date());
            sendBAMEvent(LogEvent.AFTER_RULEFLOW_COMPLETED, log);
            pWrapper.getSession().commit();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        try {
            NodeInstanceLog log = new NodeInstanceLog(
                    NodeInstanceLog.TYPE_ENTER, 
                    event.getProcessInstance().getId(),
                    event.getProcessInstance().getProcessId(), 
                    String.valueOf(event.getNodeInstance().getId()),
                    getuniqueNodeid(event.getNodeInstance().getNode()), 
                    event.getNodeInstance().getNodeName());
            sendBAMEvent(LogEvent.BEFORE_RULEFLOW_NODE_TRIGGERED, log);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        try {
            if (event.getNodeInstance() instanceof SubProcessNodeInstance) {
                SubProcessInstanceLog log = new SubProcessInstanceLog();
                log.setSubProcessInstanceId(((SubProcessNodeInstance) event.getNodeInstance()).getProcessInstanceId());
                log.setParentProcessInstanceId(event.getProcessInstance().getId());
                log.setSubProcessNodeInstanceId(event.getNodeInstance().getId());

                sendBAMEvent(SubProcessInstanceLog.AFTER_SUBPROCESSINSTANCE_CREATED, log);
            }
            else if (event.getNodeInstance() instanceof HumanTaskNodeInstance) {
                HumanTaskLog log = new HumanTaskLog();
                log.setWorkItemId(((HumanTaskNodeInstance) event.getNodeInstance()).getWorkItemId());
                log.setNodeInstanceId(event.getNodeInstance().getId());
                log.setProcessInstanceId(event.getProcessInstance().getId());

                sendBAMEvent(HumanTaskLog.AFTER_HUMANTASK_CREATED, log);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        try {
            NodeInstanceLog log = new NodeInstanceLog(
                    NodeInstanceLog.TYPE_EXIT, 
                    event.getProcessInstance().getId(),
                    event.getProcessInstance().getProcessId(), 
                    String.valueOf(event.getNodeInstance().getId()),
                    getuniqueNodeid(event.getNodeInstance().getNode()), 
                    event.getNodeInstance().getNodeName());
            sendBAMEvent(LogEvent.BEFORE_RULEFLOW_NODE_EXITED, log);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        String objectToString = event.getNewValue() == null ? "null" : event.getNewValue().toString();
        try {
            VariableInstanceLog log = new VariableInstanceLog(event.getProcessInstance().getId(), event.getProcessInstance().getProcessId(),
                    event.getVariableInstanceId(), event.getVariableId(),
                    objectToString);
            sendBAMEvent(LogEvent.AFTER_VARIABLE_INSTANCE_CHANGED, log);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendBAMEvent(int logEventType, Serializable logObj) throws JMSException, IOException {
        Session sessionObj = pWrapper.getSession();
        BytesMessage bMessage = sessionObj.createBytesMessage();
        if (logObj != null) {
            bMessage.writeBytes(marshall(logObj));
        }
        bMessage.setIntProperty(LOG_EVENT_TYPE, logEventType);

        pWrapper.getProducer().send(bMessage);
    }

    private byte[] marshall(Object obj) throws IOException {
        final Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            marshaller.start(Marshalling.createByteOutput(os));
            marshaller.writeObject(obj);
            marshaller.finish();
            return os.toByteArray();
        }
        finally {
            if (os != null) {
                os.close();
            }
        }
    }


    /**
     * Get the unique id of this <code>node</code>, this method is different from {@link org.drools.definition.process.Node#getId()}
     * which returns the id scoped in its container node.
     * <p>
     * The unique id of this <code>node</code> is the internal id in form of <code>"_${containerId}_${nodeId}"</code>
     * </p>
     * <p>
     * Both the BAM producer and the consumer/audit-trail is expected to invoke this method to make sure the consistency.
     * </p>
     * 
     * @param node
     * @return
     */
    public static String getuniqueNodeid(org.drools.definition.process.Node node) {
        return org.jbpm.bpmn2.xml.XmlBPMNProcessDumper.getUniqueNodeId(node);
    }
}
