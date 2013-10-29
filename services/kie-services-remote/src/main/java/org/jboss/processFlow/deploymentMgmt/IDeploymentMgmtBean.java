package org.jboss.processFlow.deploymentMgmt;

import java.util.Map;

import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;

public interface IDeploymentMgmtBean {
    public void deployVFS(IPfpDeploymentUnit dUnit);
    public void deployKJar(IPfpDeploymentUnit dUnit);
    public void undeployVFS(DeploymentUnit dUnit);
    public void undeployKJar(DeploymentUnit dUnit);
    public void ensureDeploymentFileSystemExists(String deploymentType, Map<String, String> dHash);
    public void start() throws Exception;
    public void stop();
}
