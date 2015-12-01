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
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
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

//TODO: Remove the import and thus the dependency on the 'common' project.
import org.jboss.processFlow.util.MessagingUtil;

/**
    - single-threaded MessageListener that consumes BAM log objects and persists to jbpm_bam database
    - chose not to implement this as a MDB because as of now (9 Nov 2011) qpid client still does not include a supported JTA resource adapter
 */
@Remote(IBAMService.class)
@Singleton
@Startup
public class BAMService implements IBAMService, MessageListener {

    public static final String LOG_EVENT_TYPE = "logEventType";
    private static final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
    private static final MarshallingConfiguration configuration = new MarshallingConfiguration();
    private static final Logger log = Logger.getLogger("BAMService");
    private static int batchSize = 100;

    private Connection connectObj = null;
    private Session sessionObj = null;
    private boolean enableLog = false;
    private EntityManager persistManager;
    private int batchCount = 0;
    private Environment bamEnv = null;
    private Object lockObj = new Object();

    private @PersistenceUnit(unitName="org.jbpm.bam.jpa") EntityManagerFactory jbpmBamEFactory;
    private @Resource UserTransaction uTrnx;
    private @Resource(name="java:/TransactionManager") TransactionManager tMgr;

    @javax.annotation.Resource(name="java:/RemoteConnectionFactory")
    private ConnectionFactory cFactory;

    @javax.annotation.Resource(name="java:/queue/processFlow.asyncWorkingMemoryLogger")
    private Destination queue;

    @PostConstruct
    public void start() throws Exception {
        connectObj = cFactory.createConnection();
        connectObj.setExceptionListener(new ExceptionListener() {
            public void onException(final JMSException e) {
                log.error("start() JMSException = "+e.getLocalizedMessage());
            }
        });

        sessionObj = connectObj.createSession(true, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer mConsumer = sessionObj.createConsumer(queue);
        mConsumer.setMessageListener(this);
        connectObj.start();

        if(System.getProperty("hibernate.jdbc.batch_size") != null)
            batchSize = Integer.parseInt(System.getProperty("hibernate.jdbc.batch_size"));

        log.info("start() batch size = "+batchSize);

        persistManager = jbpmBamEFactory.createEntityManager();

        bamEnv = EnvironmentFactory.newEnvironment();
        bamEnv.set(EnvironmentName.ENTITY_MANAGER_FACTORY, jbpmBamEFactory);

        /**
         *  similar to HumanTaskService, need to suspend JTA trnx 
         *  using a RESOURCE_LOCAL EntityManagerFactory and subsequently ... org.jbpm.process.audit.JPAProcessInstanceDbLog will want to set it's own trnx boundaries
         */
        try {
            Transaction suspendedTrnx = tMgr.suspend();
            JPAProcessInstanceDbLog.setEnvironment(bamEnv);
            tMgr.resume(suspendedTrnx);
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public ProcessInstanceLog getProcessInstanceLog(Long processInstanceId) {
        return JPAProcessInstanceDbLog.findProcessInstance(processInstanceId);
    }

    public List<ProcessInstanceLog> getProcessInstanceLogsByProcessId(String processId) {
        return JPAProcessInstanceDbLog.findProcessInstances(processId);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<ProcessInstanceLog> getActiveProcessInstanceLogsByProcessId(String processId) {
        return JPAProcessInstanceDbLog.findActiveProcessInstances(processId);
    }

    public List<NodeInstanceLog> findNodeInstances(Long processInstanceId) {
        return JPAProcessInstanceDbLog.findNodeInstances(processInstanceId);
    }

    public void onMessage(Message message) {
        ByteArrayInputStream is = null;
        try {
            if(message instanceof BytesMessage) {
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

                if(persistManager == null)
                    persistManager = jbpmBamEFactory.createEntityManager();
                
                if (logEventType == SubProcessInstanceLog.AFTER_SUBPROCESSINSTANCE_CREATED
                        || logEventType == HumanTaskLog.AFTER_HUMANTASK_CREATED) {
                    persistManager.persist(bamObj);

                    // always flush transaction when the subprocessinstance or human task created
                    // to reflect the changes on audit trail
                    flushBam();
                    log.info("onMessage() following # of records insert for " + bamObj);
                } else if(LogEvent.AFTER_RULEFLOW_COMPLETED == logEventType) {
                    // 1)  make sure that eManager used for bulk inserts is flushed prior to updating
                    flushBam();

                    // 2)  update the processinstancelog table with the end date
                    uTrnx.begin();
                    EntityManager eManager = jbpmBamEFactory.createEntityManager();
                    Query queryObj = eManager.createQuery("UPDATE ProcessInstanceLog pLog SET pLog.end = :end WHERE pLog.processInstanceId = :processInstanceId");
                    queryObj.setParameter("end", ((ProcessInstanceLog)bamObj).getEnd());
                    long processInstanceId = ((ProcessInstanceLog)bamObj).getProcessInstanceId();
                    queryObj.setParameter("processInstanceId", processInstanceId);
                    int updated = queryObj.executeUpdate();
                    uTrnx.commit();

                    log.info("onMessage() following # of records updated for pInstanceId = "+processInstanceId+" : "+updated);
                } else {
                    persistManager.persist(bamObj);
                    batchCount++;

                    //4)  incase batchSize # of BAM events are ready to be persisted, flush & clear bulk insert entity manager
                    if(batchCount % batchSize == 0) {
                        flushBam();
                    }
                }
            } else {
                log.error("onMessage() can't process message of type = "+message.getClass());
            }
        } catch(Exception x) {
            try {
                uTrnx.rollback();
            } catch(Exception y) { y.printStackTrace(); }
            throw new RuntimeException(x);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception x) { x.printStackTrace(); }
            }
        }
    }

    public void flushBam() throws Exception {
        synchronized(lockObj) {
            uTrnx.begin();
            persistManager.joinTransaction();
            persistManager.flush();  // This saves any changes made
            persistManager.clear();  // This makes sure that any returned entities are no longer attached to this entity manager/persistence context
            uTrnx.commit();
            
            batchCount = 0;
            persistManager.close();
            persistManager = null;

            sessionObj.commit();
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        log.info("stop()");
        
        flushBam();
        
        if(sessionObj != null) {
            sessionObj.commit();
            sessionObj.close();
        }
        if(connectObj != null)
            connectObj.close();
    }
}
