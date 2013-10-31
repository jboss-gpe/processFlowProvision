package org.jboss.processFlow.consoleIntegration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.errai.bus.server.annotations.Service;
import org.jbpm.console.ng.bd.service.KieSessionEntryPoint;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.services.client.api.RemoteRestSessionFactory;

@Service
@ApplicationScoped
public class RemoteSessionEntryPointImpl implements KieSessionEntryPoint {
    
	private static final String EX_SERVER_URL = "kie.exec.server.rest.base.url";
	private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static Logger log = LoggerFactory.getLogger("RemoteSessionEntryPointImpl");
    
    private URL exServerUrl;
    private String userId;
    private String password;
    
    public RemoteSessionEntryPointImpl() throws MalformedURLException {
        this.exServerUrl = new URL(System.getProperty(EX_SERVER_URL, "http://zareason:8330/kie-jbpm-services/"));
        this.userId = System.getProperty(USER_ID, "jboss");
        this.password = System.getProperty(PASSWORD, "brms");
        log.info("***** RemoteSessionEntryPointImpl() exServerUrl = "+exServerUrl);
    }
    
    private RemoteRestSessionFactory getSessionFactory(String domainId) {
    	return new RemoteRestSessionFactory(domainId, exServerUrl, userId, password);
    }

    @Override
    public long startProcess(String domainId, String processId) {
        return startProcess(domainId, processId, null);
    }

    @Override
    public long startProcess(String domainId, String processId, Map<String, String> params) {
    	KieSession ksession = getSessionFactory(domainId).newRuntimeEngine().getKieSession();
    	ProcessInstance pInstance = ksession.startProcess(processId, (Map)params);
        return pInstance.getId();
    }

    @Override
    public void abortProcessInstance(long processInstanceId) {
        // TODO Auto-generated method stub
    	

    }

    @Override
    public void abortProcessInstances(List<Long> processInstanceIds) {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspendProcessInstance(long processInstanceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void signalProcessInstance(long processInstanceId, String signalName, Object event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void signalProcessInstances(List<Long> processInstanceIds, String signalName, Object event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProcessVariable(long processInstanceId, String variableId, Object value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<String> getAvailableSignals(long processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

}
