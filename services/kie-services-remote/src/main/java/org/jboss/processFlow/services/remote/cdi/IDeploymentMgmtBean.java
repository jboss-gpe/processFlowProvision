package org.jboss.processFlow.services.remote.cdi;

import java.util.Map;

import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;

public interface IDeploymentMgmtBean {
    public void deployVFS(VFSDeploymentUnit dUnit);
    public void deployKJar(KModuleDeploymentUnit dUnit);
    public void undeploy(DeploymentUnit dUnit);
    public void ensureDeploymentFileSystemExists(String deploymentType, Map<String, String> dHash);
    public void start() throws Exception;
    public void stop();
}
