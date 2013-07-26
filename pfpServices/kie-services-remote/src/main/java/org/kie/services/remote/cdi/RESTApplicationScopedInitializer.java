package org.kie.services.remote.cdi;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;

/*
 * triggers org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events
 * captured in org.kie.services.remote.cdi.RuntimeManagerManager
 */

@ApplicationScoped
public class RESTApplicationScopedInitializer {
	
	@PersistenceUnit(unitName="org.jbpm.persistence.jpa")
	EntityManagerFactory jbpmCoreEMF;
	
	@Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
	
	@Inject
    @Vfs
    private DeploymentService deploymentService;
	
	private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();
	
	@PostConstruct
	public void start() {
        DeploymentUnit deploymentUnit = new VFSDeploymentUnit("general", "", "processes/general");
        deploymentService.deploy(deploymentUnit);
	}
	
	@Produces
    public UserGroupCallback produceSelectedUserGroupCallback() {
        return userGroupCallback;
    }
	
	@Produces
	public EntityManagerFactory getEntityManagerFactory() {
	    return jbpmCoreEMF;
	}
	
	@PreDestroy
	public void stop() {
		for (DeploymentUnit unit : units) {
            deploymentService.undeploy(unit);
        }
	}
}
