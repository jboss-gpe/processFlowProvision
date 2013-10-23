package org.jboss.processFlow.consoleIntegration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.errai.bus.server.annotations.Service;
import org.jbpm.console.ng.bd.service.KieSessionEntryPoint;

@Service
@ApplicationScoped
public class RemoteSessionEntryPointImpl implements KieSessionEntryPoint {
    
    private static Logger log = LoggerFactory.getLogger("RemoteSessionEntryPointImpl");
    
    public RemoteSessionEntryPointImpl() {
        log.info("***** RemoteSessionEntryPointImpl()");
    }

    @Override
    public long startProcess(String domainId, String processId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long startProcess(String domainId, String processId, Map<String, String> params) {
        // TODO Auto-generated method stub
        return 0;
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
