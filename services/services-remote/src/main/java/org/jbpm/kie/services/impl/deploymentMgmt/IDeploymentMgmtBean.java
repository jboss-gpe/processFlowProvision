package org.jbpm.kie.services.impl.deploymentMgmt;

import java.util.Map;

import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;

public interface IDeploymentMgmtBean {
    public void deployKJar(IPfpDeploymentUnit dUnit);
    public void undeployKJar(DeploymentUnit dUnit);
    public void start() throws Exception;
    public void stop();
}
