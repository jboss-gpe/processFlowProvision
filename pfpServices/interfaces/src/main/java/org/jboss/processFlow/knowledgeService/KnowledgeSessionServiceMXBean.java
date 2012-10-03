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

import java.net.ConnectException;
import java.util.Map;
import java.util.List;

import org.drools.definition.process.Process;
import org.drools.io.Resource;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.process.audit.ProcessInstanceLog;

public interface KnowledgeSessionServiceMXBean {


    /**
     * printWorkItemHandlers
     * <pre>
     * returns a listing of registered workItemHandlers with knowledgeSessions
     * will include workItemHandlers loaded programmatically and via configuration
     * </pre>
     */
    public String printWorkItemHandlers();

    /**
     * refreshKnowledgeBase and knowledgeAgent managed by PFP knowledgeSessionService
     * use in conjunction with various guvnor.* system properties in $JBOSS_HOME/server/<server.config>/deploy/properties-service.xml 
     */
    public void rebuildKnowledgeBaseViaKnowledgeAgent() throws ConnectException;
    public void rebuildKnowledgeBaseViaKnowledgeBuilder();
    /**
     *return a snapshot of all process definitions that the KnowledgeBase is currently aware of
     */
    public String printKnowledgeBaseContent();
    
    /**
     * Uses GuvnorConnectionUtils to query guvnor for 'assets' of a particular package using the following URL convention:
     * <guvnor.protocol>://<guvnor.host>/<guvnorsubdomain>/rest/packages/<guvnor.package>/assets
     *
     */
    public String getAllProcessesInPackage(String pkgName);

    public String                   printActiveProcessInstanceVariables(Long processInstanceId, Integer ksessionId);

    /**
     * returns a snapshot of all KnowledgeSessions and the state that each session is currently in 
     */
    public String                   dumpSessionStatusInfo();

    /**
     * knowledgeSessionService may have a process event listener that sends events asynchroneously to a message broker
     * these events will subsequently be stored in a business activity monitoring data wharehouse for future analysis
     * this function lists the # of active and idle producers from a pool of JMS producers
     */  
    public String                   dumpBAMProducerPoolInfo();
}
