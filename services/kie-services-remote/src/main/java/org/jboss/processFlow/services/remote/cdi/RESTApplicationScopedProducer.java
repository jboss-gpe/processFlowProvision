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

import org.apache.commons.lang.StringUtils;
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

/**
  provides beans that are injected into various other jbpm services at start-up to include :
    1)  EntityManagerFactory
    2)  VFS based IOService
    3)  UserGroupCallback
    4)  List<DeploymentUnit>
        - a configurable JSON data file is parsed of one or more 'deployments' that the jbpm engine should use from which to build a kieBase
        - these deployments can be of type:  simpleFile, git of kjar
 */

@ApplicationScoped
public class RESTApplicationScopedProducer {
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");

    private IOService vfsIOService;
    private List<DeploymentUnit> dUnits;
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
    
    
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
            
            Map<String, Map<String, String>> deployments = DeployUnitParser.getParsedJsonConfig();
            //To-Do:  parse and iterate through a JSON based file 
            for(Entry<String, Map<String, String>> deployment : deployments.entrySet()) {
                DeploymentUnit dUnit = null;
                if(DeployUnitParser.GIT.equals(deployment.getKey())){
                    validateDeploymentUnitFields(deployment);
                    dUnit = createGitDeploymentUnit(deployment.getValue());
                } else if(DeployUnitParser.LOCAL_FILE_SYSTEM.equals(deployment.getKey())) {
                    validateDeploymentUnitFields(deployment);
                    dUnit = this.createLocalFileDeploymentUnit(deployment.getValue());
                } else if(DeployUnitParser.KJAR.equals(deployment.getKey())) {
                    validateDeploymentUnitFields(deployment);
                    dUnit = this.createKModuleDeploymentUnit(deployment.getValue());
                } else
                    throw new Exception("getDeploymentUnits() unknown deployment type: "+deployment.getKey());
    
                dUnits.add(dUnit);
            }
        }
        return dUnits;
    }
    
    private VFSDeploymentUnit createLocalFileDeploymentUnit(Map<String, String> details){
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        StringBuilder sBuilder = new StringBuilder();
        
        // needs to be prefixed with "file:///"  .... will default to JGITFileSystemProvider
        // setting repositoryScheme to "file" does not seem to trigger use of SimpleFileSystemProvider
        String rFolder = "file://"+details.get(DeployUnitParser.REPO_FOLDER);  
        
        String rAlias = details.get(DeployUnitParser.REPO_ALIAS);
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
    
    private VFSDeploymentUnit createGitDeploymentUnit(Map<String, String> details){
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        String rFolder = details.get(DeployUnitParser.REPO_FOLDER);
        String rAlias = details.get(DeployUnitParser.REPO_ALIAS);
        StringBuilder sBuilder = new StringBuilder();
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
    
    
    private KModuleDeploymentUnit createKModuleDeploymentUnit(Map<String, String> details) {
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        String groupId = details.get(DeployUnitParser.GROUP_ID);
        String artifactId = details.get(DeployUnitParser.ARTIFACT_ID);
        String version = details.get(DeployUnitParser.VERSION);

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("createKModuleDeploymentUnit() creating KJar deploymentUnit with \n\tdeploymentId = ");
        sBuilder.append(dId);
        sBuilder.append("\n\tgroupId = ");
        sBuilder.append(groupId);
        sBuilder.append("\n\tartifactId = ");
        sBuilder.append(artifactId);
        sBuilder.append("\n\tversion = ");
        sBuilder.append(version);
        sBuilder.append("\n\tksessionStrategy = ");
        sBuilder.append(ksessionStrategy.toString());
        log.info(sBuilder.toString());

        KModuleDeploymentUnit kUnit = new KModuleDeploymentUnit(groupId, artifactId, version, "KBase-test", "ksession-test");
        kUnit.setStrategy(ksessionStrategy);
        return kUnit;
    }
    
    private void validateDeploymentUnitFields(Entry<String, Map<String, String>> deployment) throws Exception {
        Map<String, String> details = deployment.getValue();
        if(!(deployment.getKey().equals(DeployUnitParser.KJAR)) && StringUtils.isEmpty(details.get(DeployUnitParser.DEPLOYMENT_ID)))
            throw new Exception("Deployment Unit needs a property of : "+DeployUnitParser.DEPLOYMENT_ID);
        
        String kSessionStrategy = details.get(DeployUnitParser.KSESSION_STRATEGY);
        if(StringUtils.isEmpty(kSessionStrategy))
            throw new Exception("All Deployment Units to specify a ksession strategy: "+DeployUnitParser.KSESSION_STRATEGY);
        
        try {
            RuntimeStrategy.valueOf(kSessionStrategy);
        } catch (IllegalArgumentException ex) {  
            throw new Exception("following ksession strategy is not valid: "+kSessionStrategy+" for deploymentId: "+details.get(DeployUnitParser.DEPLOYMENT_ID));
        }
    }
    
}
