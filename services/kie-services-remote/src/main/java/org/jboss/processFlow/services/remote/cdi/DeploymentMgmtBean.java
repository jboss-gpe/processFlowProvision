package org.jboss.processFlow.services.remote.cdi;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.jboss.processFlow.services.remote.cdi.IPfpDeploymentUnit.ProcessEnginePersistenceType;
import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.api.DeploymentUnit.RuntimeStrategy;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.commons.io.FileSystemType;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.file.FileSystem;
import org.kie.commons.java.nio.fs.jgit.JGitFileSystemProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.commons.io.FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE;

/*
 purpose:  Create and trigger org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events.
    - there are two types of Deploy events:
        1)  import org.jbpm.kie.services.impl.KModuleDeploymentUnit
        2)  import org.jbpm.kie.services.impl.PfpVFSDeploymentUnit; 
    - These events are captured by org.kie.services.remote.cdi.RuntimeManagerManager (which is the entry point into the BPMS "Execution Server")
        - the Execution Server subsequently uses these Deploy events to instantiate and populate corresponding kieBase objects via a KieContainer
        - org.kie.api.runtime.KieContainer is the container for all the KieBases of a given KieModule

    - Note:  
        - KieContainer, KModule and KieBase objects are all created via use of either KModuleDeploymentUnit and/or PfpVFSDeploymentUnit
        - PfpVFSDeploymentUnit(s)
            - KnowledgeSession strategy is defined in this DeploymentUnit as per:  kie.deployments.json
            - workItemHandler mappings are registered with a kieSession via:
               1)  org.jbpm.kie.services.impl.VfsMVELWorkItemHandlerProducer
        - KModuleDeploymentUnit(s)
             - both ksession strategy and workItemHandler mappings are included in kJar's META-INF/kmodule.xml

    - NOTE:  regarding workItemHandler mapping registration, can also :
        1)  define mapping in:  WEB-INF/classes/META-INF/CustomWorkItemHandlers.conf
        2)  define programmatically via:  org.drools.core.command.runtime.process.RegisterWorkItemHandlerCommand
*/
@ApplicationScoped
public class DeploymentMgmtBean implements IDeploymentMgmtBean {

    public static final String GIT_ENV_PROP_DEST_PATH = "out-dir";
    private static Logger log = LoggerFactory.getLogger("DeploymentMgmtBean");
    
    @Inject
    @Vfs  // org.jboss.processFlow.services.remote.cdi.PfpVFSDeploymentService extends org.jbpm.kie.services.impl.VFSDeploymentService
    private DeploymentService vfsService;
    
    @Inject
    @Kjar // org.jboss.processFlow.services.remote.cdi.PfpKModuleDeploymentService extends org.jbpm.kie.services.impl.KModuleDeploymentService
    private DeploymentService kjarService;
    
    private List<DeploymentUnit> dUnits = new CopyOnWriteArrayList<DeploymentUnit>();
    
    @Inject
    @Named("ioStrategy")
    private IOService ioService;
    
    public static enum LocalFileSystemType implements FileSystemType {
        LOCAL_FILE_SYSTEM;
        
        public String toString() {
            return "LOCAL_FILE_SYSTEM";
        }
    }

   /* new ObjectMarshallingStrategy[]{
            new TestStrategy(),
            new SerializablePlaceholderResolverStrategy( ClassObjectMarshallingStrategyAcceptor.DEFAULT  )*/
    public void deployVFS(IPfpDeploymentUnit dUnit) {
        String dIdentifier = dUnit.getIdentifier();
        if(vfsService.getDeployedUnit(dIdentifier) == null){
            // there is the potential that a DeployedUnit could still be registered with :
            //   org.jbpm.runtime.manager.impl.AbstractRuntimeManager
            try {
                vfsService.deploy(dUnit);
                log.info("start() just deployed the following VFS dUnit : {}", dIdentifier);
            }catch(Throwable x){
                log.error("start() exception thrown when attempting to deploy the following VFS deploymentUnit : "+dIdentifier);
                x.printStackTrace();
            }
        } else {
            log.error("start() uh-oh .... not going to attempt to start the following VFS dUnit cause already registered : {}", dIdentifier);
        }
    }
    public void deployKJar(IPfpDeploymentUnit dUnit) {
        String dIdentifier = dUnit.getIdentifier();
        if(kjarService.getDeployedUnit(dIdentifier) == null){
            try {

                /* following stack is executed IOT create a new org.kie.api.runtime.KieContainer
                            org.drools.compiler.kie.builder.impl.KieServicesImpl.newKieContainer(KieServicesImpl.java:84)
                            org.jbpm.kie.services.impl.KModuleDeploymentService.deploy(KModuleDeploymentService.java:69)
                            org.jbpm.kie.services.impl.KModuleDeploymentService.deploy()
                            org.jboss.processFlow.services.remote.cdi.RESTApplicationStartup.start(RESTApplicationStartup.java)
                 */
                kjarService.deploy(dUnit);

                log.info("start() just deployed the following Kjar dUnit : {}", dIdentifier);
            }catch(Throwable x){
                log.error("start() exception thrown when attempting to deploy the following KJar deploymentUnit : "+dIdentifier);
                x.printStackTrace();
            }
        }else {
            log.error("start() uh-oh .... not going to attempt to start the following KJar deployment unit cause already registered : {}", dIdentifier);
        }
    }
    public void start() throws Exception {
        log.info("start() org.kie.nio.git.daemon.enabled = "+System.getProperty("org.kie.nio.git.daemon.enabled"));
        
        // ensureDeploymentFileSystemsExist 
        Map<String, Map<String, String>> deployments = DeployUnitParser.reloadParsedJsonConfig();
        for(Entry<String, Map<String, String>> deployment : deployments.entrySet()) {
           Map<String, String> dHash = deployment.getValue();
           this.ensureDeploymentFileSystemExists(deployment.getKey(), dHash);
        }
        
        
        List<DeploymentUnit> localScopedDUnits = this.getDeploymentUnits();
        for(DeploymentUnit dUnit : localScopedDUnits){
            if(dUnit instanceof PfpVFSDeploymentUnit ) {
                deployVFS((PfpVFSDeploymentUnit)dUnit);
                dUnits.add(dUnit);
            }else if (dUnit instanceof KModuleDeploymentUnit){
                deployKJar((PfpKModuleDeploymentUnit)dUnit);
                dUnits.add(dUnit);
            }else{
                log.error("start() unknown DeploymentUnit type: "+dUnit);
            }
        }
    }

    public void undeployKJar(DeploymentUnit dUnit) {
         kjarService.undeploy(dUnit);
         dUnits.remove(dUnit);
    }
    public void undeployVFS(DeploymentUnit dUnit) {
        vfsService.undeploy(dUnit);
        dUnits.remove(dUnit);
    }
    public void stop(){
         for (DeploymentUnit dUnit : dUnits) {
             if(dUnit instanceof PfpVFSDeploymentUnit){
                 if(vfsService.getDeployedUnit(dUnit.getIdentifier()) == null)
                     log.error("stop() uh-oh .... not going to attempt to undeploy the following VFS dUnit which was previously not deployed : {}", dUnit.getIdentifier());
                 else{
                     log.info("stop() about to stop following VFS deployment unit : {}", dUnit.getIdentifier());
                     undeployVFS(dUnit);
                 }
             }else if(dUnit instanceof PfpKModuleDeploymentUnit){
                 if(kjarService.getDeployedUnit(dUnit.getIdentifier()) == null)
                     log.error("stop() uh-oh .... not going to attempt to undeploy the following KModule dUnit which was previously not deployed : {}", dUnit.getIdentifier());
                 else{
                     log.info("stop() about to stop following KModule deployment unit : {}", dUnit.getIdentifier());
                     undeployKJar(dUnit);
                 }
                 
             }else{
                 log.error("stop() unit deploymentUnit type = "+dUnit.getClass().toString());
             }
         }
    }
    
    public void ensureDeploymentFileSystemExists(String deploymentType, Map<String, String> dHash) { 
        if(DeployUnitParser.LOCAL_FILE_SYSTEM.equals(deploymentType)) {
            String fileUri = dHash.get(DeployUnitParser.REPO_FOLDER);
            URI fsURI = URI.create(fileUri);
            try{
                //SimpleFileSystemProvider.newFileSystem() seems to be unimplemented ... so instead will just java.io.* to ensure filesystem directory exists
                //otherwise, the SimpleFileSystemProvider will subsequently complain that the directory doesn't exist
                //final Map<String, Object> env = new HashMap<String, Object>();
                //ioService.newFileSystem(fsURI, env, LocalFileSystemType.LOCAL_FILE_SYSTEM);
                File sFile = new File(fileUri);
                if(!sFile.exists())
                    sFile.mkdirs();
            }catch(Exception x){
                x.printStackTrace();
            }
        }else if(DeployUnitParser.GIT.equals(deploymentType)) {
            URI fsURI = URI.create("git://"+dHash.get(DeployUnitParser.REPO_ALIAS));

            FileSystem fSystem = ioService.getFileSystem(fsURI);
            if(fSystem == null){
                String gitUser = dHash.get(DeployUnitParser.GIT_USER);
                String gitPasswd = dHash.get(DeployUnitParser.GIT_PASSWD);
                String remoteGitUrl = dHash.get(DeployUnitParser.GIT_REMOTE_REPO_URL);
                String gitOutDir = dHash.get(DeployUnitParser.GIT_OUT_DIR);
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append("\n\tdeploymentId = "+dHash.get(DeployUnitParser.DEPLOYMENT_ID));
                sBuilder.append("\n\tgitUser = "+gitUser);
                sBuilder.append("\n\torigin = "+remoteGitUrl);
                sBuilder.append("\n\tgitOutDir = "+gitOutDir);
                final Map<String, Object> env = new HashMap<String, Object>();
                env.put( JGitFileSystemProvider.USER_NAME, gitUser );
                env.put( JGitFileSystemProvider.PASSWORD, gitPasswd);
                if(remoteGitUrl == null || remoteGitUrl.equals(""))
                    throw new RuntimeException("ensureDeployFileSystemsExist() remoteGitUrl can not be null for deploymendId: "+dHash.get(DeployUnitParser.DEPLOYMENT_ID) );
                env.put( JGitFileSystemProvider.GIT_DEFAULT_REMOTE_NAME, remoteGitUrl);
                if(gitOutDir != null && !gitOutDir.equals(""))
                    env.put(GIT_ENV_PROP_DEST_PATH, gitOutDir);
                log.info("ensureDeploymentFileSystemsExist() will clone remote git repo to local as per:  {}", sBuilder.toString());
                ioService.newFileSystem(fsURI, env, BOOTSTRAP_INSTANCE);
            }else {
                log.warn("ensureDeploymentFileSystemsExist() following FileSystem already created: {} : scheme = {}", fsURI.toString(), fSystem.provider().getScheme());
            }
        }else{
            log.warn("ensureDeploymentFileSystemsExist() no need to ensure file system exists for type "+deploymentType);
        }
    }
    
    private List<DeploymentUnit> getDeploymentUnits() throws Exception {

        List<DeploymentUnit> localScopedDUnits = new ArrayList<DeploymentUnit>();
        Map<String, Map<String, String>> deployments = DeployUnitParser.getParsedJsonConfig();
        //To-Do:  parse and iterate through a JSON based config file 
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

            localScopedDUnits.add(dUnit);
        }
        return localScopedDUnits;
    }
    
    // see:  org.kie.commons.java.nio.fs.file.SimpleFileSystemProvider 
    private VFSDeploymentUnit createLocalFileDeploymentUnit(Map<String, String> details){
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        ProcessEnginePersistenceType engineType = null;
        String engineTypeString = details.get(DeployUnitParser.ENGINE_TYPE);
        if(engineTypeString != null)
            engineType = ProcessEnginePersistenceType.valueOf(engineTypeString);
        else
            engineType = ProcessEnginePersistenceType.JPA;
        
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
        sBuilder.append("\n\tengineType = ");
        sBuilder.append(engineType);
        log.info(sBuilder.toString());
        PfpVFSDeploymentUnit vfsUnit = new PfpVFSDeploymentUnit(dId, rAlias, rFolder);
        vfsUnit.setStrategy(ksessionStrategy);
        vfsUnit.setProcessEnginePersistenceType(engineType);
        return vfsUnit;
    }
    
    // see:  org.kie.commons.java.nio.fs.jgit.JGitFileSystemProvider 
    private VFSDeploymentUnit createGitDeploymentUnit(Map<String, String> details){
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        ProcessEnginePersistenceType engineType = null;
        String engineTypeString = details.get(DeployUnitParser.ENGINE_TYPE);
        if(engineTypeString != null)
            engineType = ProcessEnginePersistenceType.valueOf(engineTypeString);
        else
            engineType = ProcessEnginePersistenceType.JPA;
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
        sBuilder.append("\n\tengineType = ");
        sBuilder.append(engineType);
        log.info(sBuilder.toString());
        PfpVFSDeploymentUnit vfsUnit = new PfpVFSDeploymentUnit(dId, rAlias, rFolder);
        vfsUnit.setStrategy(ksessionStrategy);
        vfsUnit.setProcessEnginePersistenceType(engineType);
        vfsUnit.setRepositoryScheme(DeployUnitParser.GIT);
        return vfsUnit;
    }
    
    
    private KModuleDeploymentUnit createKModuleDeploymentUnit(Map<String, String> details) {
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        String groupId = details.get(DeployUnitParser.GROUP_ID);
        String artifactId = details.get(DeployUnitParser.ARTIFACT_ID);
        String version = details.get(DeployUnitParser.VERSION);
        String kbaseName = details.get(DeployUnitParser.KBASE_NAME);
        String ksessionName = details.get(DeployUnitParser.KSESSION_NAME);
        ProcessEnginePersistenceType engineType = null;
        String engineTypeString = details.get(DeployUnitParser.ENGINE_TYPE);
        if(engineTypeString != null)
            engineType = ProcessEnginePersistenceType.valueOf(engineTypeString);
        else
            engineType = ProcessEnginePersistenceType.JPA;

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
        sBuilder.append("\n\tkieBase name = ");
        sBuilder.append(kbaseName);
        sBuilder.append("\n\tkieSession name = ");
        sBuilder.append(ksessionName);
        sBuilder.append("\n\tengineType = ");
        sBuilder.append(engineType);
        log.info(sBuilder.toString());

        PfpKModuleDeploymentUnit kUnit = new PfpKModuleDeploymentUnit(groupId, artifactId, version);
        if (StringUtils.isNotEmpty(kbaseName)) {
            kUnit.setKbaseName(kbaseName);
        }
        if (StringUtils.isNotEmpty(ksessionName)) {
            kUnit.setKsessionName(ksessionName);
        }
        
        // confused about kmodule.xml schema:  https://github.com/droolsjbpm/droolsjbpm-knowledge/blob/master/kie-api/src/main/resources/org/kie/api/kmodule.xsd
        // ksessionType can only be "stateless" or "stateful" ?? .... what about SINGLETON or PER_PROCESS_INSTANCE ??
        kUnit.setStrategy(ksessionStrategy);
        kUnit.setProcessEnginePersistenceType(engineType);
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
