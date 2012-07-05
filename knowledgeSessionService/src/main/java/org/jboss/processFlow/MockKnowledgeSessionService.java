package org.jboss.processFlow;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.drools.definition.process.Process;
import org.drools.io.Resource;
import org.drools.runtime.process.ProcessInstance;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;

@Remote(IKnowledgeSessionService.class)
@Singleton(name="mockKSessionProxy")
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MockKnowledgeSessionService implements IKnowledgeSessionService {
	
	private Logger log = Logger.getLogger(MockKnowledgeSessionService.class);

	@Override
	public Map<String, Object> startProcessAndReturnId(String processId,
			Map<String, Object> parameters) throws Exception {
		log.info("startProcessAndReturnId() processId = "+processId+" : will return empty map");
		return new HashMap<String, Object>();
	}

	@Override
	public void completeWorkItem(Integer ksessionId, Long workItemId,
			Map<String, Object> pInstanceVariables) {
		log.info("completeWorkItem() ksessionId = "+ksessionId+" : workItemId = "+workItemId);

	}

	@Override
	public String printWorkItemHandlers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void signalEvent(String type, Object event, Long processInstanceId,
			Integer ksessionId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void abortProcessInstance(Long processInstanceId, Integer ksessionId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rebuildKnowledgeBaseViaKnowledgeAgent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rebuildKnowledgeBaseViaKnowledgeBuilder() {
		// TODO Auto-generated method stub

	}

	@Override
	public String printKnowledgeBaseContent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAllProcessesInPackage(String pkgName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Process> retrieveProcesses() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addProcessToKnowledgeBase(Process processObj,
			Resource resourceObj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addProcessToKnowledgeBase(File bpmnFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ProcessInstance> getActiveProcessInstances(
			Map<String, Object> queryCriteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Process getProcess(String processId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Process getProcessByName(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeProcess(String processId) {
		// TODO Auto-generated method stub

	}

	@Override
	public String printActiveProcessInstanceVariables(Long processInstanceId,
			Integer ksessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getActiveProcessInstanceVariables(
			Long processInstanceId, Integer ksessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProcessInstanceVariables(Long processInstanceId,
			Map<String, Object> variables, Integer ksessionId) {
		// TODO Auto-generated method stub

	}

	@Override
	public String dumpSessionStatusInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String dumpBAMProducerPoolInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disposeStatefulKnowledgeSessionAndExtras(Integer sessionId) {
		// TODO Auto-generated method stub

	}

}
