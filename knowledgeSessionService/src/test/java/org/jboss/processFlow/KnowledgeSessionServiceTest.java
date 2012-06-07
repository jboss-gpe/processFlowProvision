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

package org.jboss.processFlow;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.jboss.mx.util.MBeanServerLocator; // lib/jbos-j2se.jar
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ejb3.annotation.Service;
import org.jboss.ejb3.annotation.Management;
import org.jboss.ejb3.annotation.Depends;

import org.apache.log4j.Logger;

@Service (objectName="org.jboss.processFlow.knowledgeService:service=KnowledgeSessionServiceTest")
@Management(IKnowledgeSessionServiceTest.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@Depends ("jboss.jca:name=jbpm-core-cp,service=ManagedConnectionPool")
public class KnowledgeSessionServiceTest implements IKnowledgeSessionServiceTest {

    private static final Logger log = Logger.getLogger("KnowledgeSessionServiceTest");
    MBeanServer mBeanServer = null;
    ObjectName objectName = null;

    public void start() throws Exception {
        log.info("start() just started HornetQ TaskClient");
        mBeanServer = MBeanServerLocator.locateJBoss();
        objectName = new ObjectName("org.jboss.processFlow.knowledgeService:service=KnowledgeSessionService");
    }

    public void stop() throws Exception {
        
    }

    @Override
    public void signalEvent(String processInstanceId, String signalKey, String signalValue) throws Exception {
        mBeanServer.invoke(objectName, "signalEvent", new Object[]{processInstanceId, signalKey, signalValue}, 
                new String[]{"java.lang.String", "java.lang.String", "java.lang.String"});
    }

    @Override
    public void signalExecution(String processInstanceId, String signalValue) throws Exception {
        mBeanServer.invoke(objectName, "signalEvent", new Object[]{processInstanceId, "signal", signalValue}, 
                new String[]{"java.lang.String", "java.lang.String", "java.lang.String"});
    }

    
}
