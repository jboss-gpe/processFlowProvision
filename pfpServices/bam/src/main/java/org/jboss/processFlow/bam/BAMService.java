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

import org.drools.runtime.Environment;
import org.drools.impl.EnvironmentFactory;
import org.drools.runtime.EnvironmentName;

import org.jboss.processFlow.util.MessagingUtil;

@Remote(IBAMService.class)
@Singleton
@Startup
public class BAMService implements IBAMService {

    private static final Logger log = Logger.getLogger("BAMService");
    private @PersistenceUnit(unitName="org.jbpm.bam.jpa") EntityManagerFactory jbpmBamEFactory;
    private @Resource(name="java:/TransactionManager") TransactionManager tMgr;

    @PostConstruct
    public void start() throws Exception {
        log.info("start()");

        Environment bamEnv = EnvironmentFactory.newEnvironment();
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

    @PreDestroy
    public void stop() throws Exception {
        log.info("stop()");
    }
}
