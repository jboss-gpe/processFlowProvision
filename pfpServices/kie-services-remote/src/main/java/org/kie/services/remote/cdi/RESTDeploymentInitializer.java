package org.kie.services.remote.cdi;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;

/*
 * triggers org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events
 * captured in org.kie.services.remote.cdi.RuntimeManagerManager
 */

@ApplicationScoped
public class RESTDeploymentInitializer {
	
	@Inject
    @Vfs
    private DeploymentService deploymentService;
	
	private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();
	
	@PostConstruct
	public void start() {
        DeploymentUnit deploymentUnit = new VFSDeploymentUnit("general", "", "processes/general");
        deploymentService.deploy(deploymentUnit);
	}
	
	@PreDestroy
	public void stop() {
		for (DeploymentUnit unit : units) {
            deploymentService.undeploy(unit);
        }
	}
}
