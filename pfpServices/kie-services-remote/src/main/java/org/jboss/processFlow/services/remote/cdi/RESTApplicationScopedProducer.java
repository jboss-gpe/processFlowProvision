package org.jboss.processFlow.services.remote.cdi;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.DeploymentUnit.RuntimeStrategy;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceDotFileImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RESTApplicationScopedProducer {
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
    
    
    private IOService vfsIOService;
    private List<DeploymentUnit> dUnits;
    
    @Produces
    public UserGroupCallback produceSelectedUserGroupCallback() {
        return userGroupCallback;
    }
    
    @Produces
    public EntityManagerFactory getEntityManagerFactory() {
        return jbpmCoreEMF;
    }
    
    @Produces
    @Named("deploymentUnits")
    public List<DeploymentUnit> getDeploymentUnits() throws Exception {
    	
    	if(dUnits == null ) {
    		dUnits = new CopyOnWriteArrayList<DeploymentUnit>();
    		
    		Map<String, Map<String, Object>> deployments = DeployUnitParser.getParsedJsonConfig();
    		//To-Do:  parse and iterate through a JSON based file 
    		for(Entry<String, Map<String, Object>> deployment : deployments.entrySet()) {
    			DeploymentUnit dUnit = null;
    			if(DeployUnitParser.GIT.equals(deployment.getKey()))
    				dUnit = createGitDeploymentUnit(deployment.getValue());
    			else if(DeployUnitParser.LOCAL_FILE_SYSTEM.equals(deployment.getKey()))
    				dUnit = this.createLocalFileDeploymentUnit(deployment.getValue());
    			else if(DeployUnitParser.KMODULE.equals(deployment.getKey()))
    				dUnit = this.createKModuleDeploymentUnit(deployment.getValue());
    			else
    				throw new Exception("getDeploymentUnits() unknown deployment type: "+deployment.getKey());
    
    			dUnits.add(dUnit);
    		}
    	}
    	return dUnits;
    }
    
    private VFSDeploymentUnit createLocalFileDeploymentUnit(Map<String, Object> details){
    	RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf((String)details.get(DeployUnitParser.KSESSION_STRATEGY));
    	StringBuilder sBuilder = new StringBuilder();
    	String dId = (String)details.get(DeployUnitParser.DEPLOYMENT_ID);
    	
    	// needs to be prefixed with "file:///"  .... will default to JGITFileSystemProvider
    	// setting repositoryScheme to "file" does not seem to trigger use of SimpleFileSystemProvider
    	String rFolder = "file://"+(String)details.get(DeployUnitParser.REPO_FOLDER);  
    	
    	String rAlias = (String)details.get(DeployUnitParser.REPO_ALIAS);
    	sBuilder.append("createLocalFileDeploymentUnit() creating localFile deploymentUnit with \n\tdeploymentId = ");
    	sBuilder.append(dId);
    	sBuilder.append("\n\trepoFolder = ");
    	sBuilder.append(rFolder);
    	sBuilder.append("\n\trepoAlias = ");
    	sBuilder.append(rAlias);
    	sBuilder.append("\n\tksessionStrategy = ");
    	sBuilder.append(ksessionStrategy.toString());
    	log.info(sBuilder.toString());
    	VFSDeploymentUnit vfsUnit = new VFSDeploymentUnit(dId, rAlias, rFolder);
    	vfsUnit.setStrategy(ksessionStrategy);
    	return vfsUnit;
    }
    
    private VFSDeploymentUnit createGitDeploymentUnit(Map<String, Object> details){
    	RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf((String)details.get(DeployUnitParser.KSESSION_STRATEGY));
    	StringBuilder sBuilder = new StringBuilder();
    	String dId = (String)details.get(DeployUnitParser.DEPLOYMENT_ID);
    	String rFolder = (String)details.get(DeployUnitParser.REPO_FOLDER);
    	String rAlias = (String)details.get(DeployUnitParser.REPO_ALIAS);
    	sBuilder.append("createGitDeploymentUnit() creating git deploymentUnit with \n\tdeploymentId = ");
    	sBuilder.append(dId);
    	sBuilder.append("\n\trepoFolder = ");
    	sBuilder.append(rFolder);
    	sBuilder.append("\n\trepoAlias = ");
    	sBuilder.append(rAlias);
    	sBuilder.append("\n\tksessionStrategy = ");
    	sBuilder.append(ksessionStrategy.toString());
    	log.info(sBuilder.toString());
    	VFSDeploymentUnit vfsUnit = new VFSDeploymentUnit(dId, rAlias, rFolder);
    	vfsUnit.setStrategy(ksessionStrategy);
    	vfsUnit.setRepositoryScheme(DeployUnitParser.GIT);
    	return vfsUnit;
    }
    
    
    private KModuleDeploymentUnit createKModuleDeploymentUnit(Map<String, Object> details) {
    	String ARTIFACT_ID = "test-module";
        String GROUP_ID = "org.jbpm.test";
        String VERSION = "1.0.0-SNAPSHOT";

    	KModuleDeploymentUnit kUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, "KBase-test", "ksession-test");
    	return kUnit;
    }
    
    /* 
     * required for ioStrategy field in:  
       1)  org.jbpm.shared.services.impl.VFSFileServiceImpl    
       2)  org.jboss.processFlow.services.remote.cdi.RESTApplicationStartup
    */
    @Produces
    @Named("ioStrategy")
    public IOService getIOService() {
    	try{
    		if(vfsIOService == null) {

    			// TO-DO:  investigate the details between all of these ??
    			vfsIOService = new IOServiceDotFileImpl();
    			//vfsIOService = new IOServiceIndexedImpl(indexEngine, DublinCoreView.class, VersionAttributeView.class, OtherMetaView.class)
    			//vfsIOService = vfsIOServiceClusterImpl(service, clusterServiceFactory);
    			//vfsIOService = new IOServiceNio2WrapperImpl();
    		}
    		return vfsIOService;
    	}catch(Exception x){
    		throw new RuntimeException(x);
    	}
    }
}
