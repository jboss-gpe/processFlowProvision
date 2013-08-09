package org.jboss.processFlow.services.remote.cdi;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Named;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class RESTApplicationStartup {
	private static Logger log = LoggerFactory.getLogger("RESTApplicationStartup");
    
    @Inject
    @Vfs
    private DeploymentService deploymentService;
    
    @Inject
    @Named("deploymentUnits")
    private List<DeploymentUnit> dUnits;
    
    
    /*
     * Triggers org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events.
     * These events are captured by org.kie.services.remote.cdi.RuntimeManagerManager.
     */
    @PostConstruct
    public void start() throws Exception{
    	
    	for(DeploymentUnit dUnit : dUnits){
    		if(deploymentService.getDeployedUnit(dUnit.getIdentifier()) == null){
    			// there is the potential that a DeployedUnit could still be registered with :
    			//   org.jbpm.runtime.manager.impl.AbstractRuntimeManager
    		    deploymentService.deploy(dUnit);
    		} else {
    			log.error("start() uh-oh .... not going to attempt to start the following dUnit cause already registered : {}", dUnit.getIdentifier());
    		}
    	}
    }
    
    @PreDestroy
    public void stop() {
    	for (DeploymentUnit dUnit : dUnits) {
    		if(deploymentService.getDeployedUnit(dUnit.getIdentifier()) == null)
    			log.error("stop() uh-oh .... not going to attempt to undeploy the following dUnit which was previously not deployed : {}", dUnit.getIdentifier());
    		else{
    			log.info("stop() about to stop following deployment unit : {}", dUnit.getIdentifier());
    			deploymentService.undeploy(dUnit);
    			dUnits.remove(dUnit);
    		}
    	}
    }
}
