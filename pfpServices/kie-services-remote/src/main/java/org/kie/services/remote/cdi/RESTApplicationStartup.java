package org.kie.services.remote.cdi;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * triggers org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events
 * captured in org.kie.services.remote.cdi.RuntimeManagerManager
 */
@Singleton
@Startup
public class RESTApplicationStartup {
	
	public static final String DEPLOYMENT_ID = "org.kie.services.remote.cdi.deployment.id";
	public static final String VFS_PATH = "org.kie.services.remote.cdi.vfs.path";
	
	@Inject
    @Vfs
    private DeploymentService deploymentService;
	
	private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();
	
	private String deploymentId = "general";
	private String vfsPath = "process/"+deploymentId;
	
	private Logger log = LoggerFactory.getLogger("RESTApplicationStartup");
	
	@PostConstruct
	public void start() {
		deploymentId = System.getProperty(DEPLOYMENT_ID, deploymentId);
		vfsPath = System.getProperty(VFS_PATH, vfsPath);
		System.out.println("jeff");
		log.info("start() deploymentId = {} : vfsPath = {}", deploymentId, vfsPath);
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
