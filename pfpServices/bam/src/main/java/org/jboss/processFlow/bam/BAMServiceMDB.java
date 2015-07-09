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

package org.jboss.processFlow.bam;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.MessageDriven;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;
import javax.transaction.Transaction;

import org.apache.log4j.Logger;

import org.drools.audit.event.LogEvent;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.JPAProcessInstanceDbLog;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

import org.drools.runtime.Environment;
import org.drools.impl.EnvironmentFactory;
import org.drools.runtime.EnvironmentName;

import org.jboss.processFlow.util.MessagingUtil;

@MessageDriven(name = "BAMServiceMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/queue/processFlow.asyncWorkingMemoryLogger"),
})
public class BAMServiceMDB implements MessageListener {

    public static final String LOG_EVENT_TYPE = "logEventType";
    private static final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
    private static final MarshallingConfiguration configuration = new MarshallingConfiguration();
    private static final Logger log = Logger.getLogger("BAMServiceMDB");

    private boolean enableLog = false;

    private @PersistenceUnit(unitName="org.jbpm.bam.jpa") EntityManagerFactory jbpmBamEFactory;

    @PostConstruct
    public void start() throws Exception {
        enableLog = Boolean.parseBoolean(System.getProperty("org.jboss.enableLog", "false"));
    }

    public void onMessage(Message message) {
        ByteArrayInputStream is = null;
        EntityManager persistManager;
        try {
            if(message instanceof BytesMessage) {
                persistManager = jbpmBamEFactory.createEntityManager();
                int logEventType = message.getIntProperty(LOG_EVENT_TYPE);
                if(enableLog)
                    log.info("start() received message with logEventType = "+ logEventType+" : length = "+((BytesMessage)message).getBodyLength());
                byte[] logBytes = new byte[(int)((BytesMessage)message).getBodyLength()];
                ((BytesMessage)message).readBytes(logBytes);
                is = new ByteArrayInputStream(logBytes);

                final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
                unmarshaller.start(Marshalling.createByteInput(is));
                Object bamObj = unmarshaller.readObject();
                unmarshaller.finish();

                if(LogEvent.AFTER_RULEFLOW_COMPLETED == logEventType) {

                    // update the processinstancelog table with the end date
                    Query queryObj = persistManager.createQuery("UPDATE ProcessInstanceLog pLog SET pLog.end = :end WHERE pLog.processInstanceId = :processInstanceId");
                    queryObj.setParameter("end", ((ProcessInstanceLog)bamObj).getEnd());
                    long processInstanceId = ((ProcessInstanceLog)bamObj).getProcessInstanceId();
                    queryObj.setParameter("processInstanceId", processInstanceId);
                    int updated = queryObj.executeUpdate();

                    if(enableLog)
                        log.info("onMessage() following # of records updated for pInstanceId = "+processInstanceId+" : "+updated);
                } else {
                    persistManager.persist(bamObj);
                }
            } else {
                log.error("onMessage() can't process message of type = "+message.getClass());
            }
        } catch(Exception x) {
            x.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception x) { x.printStackTrace(); }
            }
        }
    }
}
