package org.jboss.processFlow.services.remote.cdi;

import java.util.Map;

import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;

public interface IDeploymentMgmtBean {
    public void deployVFS(IPfpDeploymentUnit dUnit);
    public void deployKJar(IPfpDeploymentUnit dUnit);
    public void undeploy(DeploymentUnit dUnit);
    public void ensureDeploymentFileSystemExists(String deploymentType, Map<String, String> dHash);
    public void start() throws Exception;
    public void stop();
}
