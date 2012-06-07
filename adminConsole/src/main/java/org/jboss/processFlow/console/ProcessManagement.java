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

package org.jboss.processFlow.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.drools.definition.process.Process;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.model.ProcessInstanceRef;
import org.jboss.bpm.console.client.model.ProcessInstanceRef.RESULT;
import org.jboss.bpm.console.client.model.ProcessInstanceRef.STATE;
import org.jbpm.process.audit.ProcessInstanceLog;

public class ProcessManagement implements org.jboss.bpm.console.server.integration.ProcessManagement {

	private CommandDelegate delegate;
	
    public ProcessManagement() {
        delegate = new CommandDelegate();
    }
    
	public List<ProcessDefinitionRef> getProcessDefinitions() {
		List<Process> processes = delegate.getProcesses();
		List<ProcessDefinitionRef> result = new ArrayList<ProcessDefinitionRef>();
		for (Process process: processes) {
			result.add(Transform.processDefinition(process));
		}
		return result;
	}

	public ProcessDefinitionRef getProcessDefinition(String definitionId) {
		Process process = delegate.getProcess(definitionId);
		return Transform.processDefinition(process);
	}

	/**
	 * method unsupported
	 */
	public List<ProcessDefinitionRef> removeProcessDefinition(String definitionId) {
		delegate.removeProcess(definitionId); 
	    return getProcessDefinitions();
	}

	/**
	 * XXX this method is not invoked anywhere.
	 */
	public ProcessInstanceRef getProcessInstance(String instanceId) {
		ProcessInstanceLog processInstance = delegate.getProcessInstanceLog(instanceId);
		return Transform.processInstance(processInstance);
	}

	public List<ProcessInstanceRef> getProcessInstances(String definitionId) {
		List<ProcessInstanceLog> processInstances = delegate.getActiveProcessInstanceLogsByProcessId(definitionId);
		List<ProcessInstanceRef> result = new ArrayList<ProcessInstanceRef>();
		for (ProcessInstanceLog processInstance: processInstances) {
			result.add(Transform.processInstance(processInstance));
		}
		return result;
	}

	public ProcessInstanceRef newInstance(String definitionId) {
		ProcessInstanceLog processInstance = delegate.startProcess(definitionId, null);
		return Transform.processInstance(processInstance);
	}
	
	public ProcessInstanceRef newInstance(String definitionId, Map<String, Object> processVars) {
		ProcessInstanceLog processInstance = delegate.startProcess(definitionId, processVars);
		return Transform.processInstance(processInstance);
	}

	public void setProcessState(String instanceId, STATE nextState) {
		if (nextState == STATE.ENDED) {
			delegate.abortProcessInstance(instanceId);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	public Map<String, Object> getInstanceData(String instanceId) {
		return delegate.getProcessInstanceVariables(instanceId);
	}

	public void setInstanceData(String instanceId, Map<String, Object> data) {
		delegate.setProcessInstanceVariables(instanceId, data);
	}

	
	public void signalExecution(String executionId, String signal) {
		delegate.signalExecution(executionId, signal);
	}

	public void deleteInstance(String instanceId) {
		delegate.abortProcessInstance(instanceId);
	}

	//result means nothing
	public void endInstance(String instanceId, RESULT result) {
		delegate.abortProcessInstance(instanceId);
	}

}
