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

import java.util.List;

import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;

public interface IBAMService {
    public static final String BAM_SERVICE_JNDI = "ejb:pfp/processFlow-bamService//BAMService!org.jboss.processFlow.bam.IBAMService";
    public static final String BAM_SERVICE_PROVIDER_URL = "org.jboss.processFlow.bam.BAM_SERVICE_PROVIDER_URL";
    public static final String BAM_QUEUE = "java:/queue/processFlow.asyncWorkingMemoryLogger";
    public static final String ASYNC_BAM_PRODUCER="org.jboss.processFlow.bam.AsyncBAMProducer";

    public void flushBam() throws Exception;

    public ProcessInstanceLog       getProcessInstanceLog(Long processInstanceId);
    public List<ProcessInstanceLog> getProcessInstanceLogsByProcessId(String processId);
    public List<ProcessInstanceLog> getActiveProcessInstanceLogsByProcessId(String processId);
    public List<NodeInstanceLog>    findNodeInstances(Long processInstanceId);
}
