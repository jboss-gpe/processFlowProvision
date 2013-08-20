package org.jboss.processFlow.services.remote.cdi;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Named;

import org.jbpm.kie.services.api.DeploymentService;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.kie.commons.io.FileSystemType;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.file.FileSystem;
import org.kie.commons.java.nio.file.FileSystemAlreadyExistsException;

import static org.kie.commons.io.FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE;

import org.kie.commons.java.nio.fs.jgit.JGitFileSystemProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class RESTApplicationStartup {
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationStartup");
    
    @Inject
    @Vfs
    private DeploymentService vfsService;
    
    @Inject
    @Kjar
    private DeploymentService kjarService;
    
    
    @Inject
    @Named("deploymentUnits")
    private List<DeploymentUnit> dUnits;
    
    @Inject
    @Named("ioStrategy")
    private IOService ioService;
    
    public static enum LocalFileSystemType implements FileSystemType {
        LOCAL_FILE_SYSTEM;
        
        public String toString() {
            return "LOCAL_FILE_SYSTEM";
        }
    }
    
    /*
     * Triggers org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events.
     * These events are captured by org.kie.services.remote.cdi.RuntimeManagerManager.
     */
    @PostConstruct
    public void start() throws Exception {
        this.ensureDeploymentFileSystemsExist();
        
        for(DeploymentUnit dUnit : dUnits){
            String dIdentifier = dUnit.getIdentifier();
            if(dUnit instanceof VFSDeploymentUnit ) {
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
            }else if (dUnit instanceof KModuleDeploymentUnit){
                if(kjarService.getDeployedUnit(dIdentifier) == null){
                    try {
                        kjarService.deploy(dUnit);
                        log.info("start() just deployed the following Kjar dUnit : {}", dIdentifier);
                    }catch(Throwable x){
                        log.error("start() exception thrown when attempting to deploy the following KJar deploymentUnit : "+dIdentifier);
                        x.printStackTrace();
                    }
                }else {
                    log.error("start() uh-oh .... not going to attempt to start the following KJar deployment unit cause already registered : {}", dIdentifier);
                }
            }else{
                throw new Exception("start() unknown DeploymentUnit type: "+dUnit);
            }
        }
    }
    
    @PreDestroy
    public void stop() {
        for (DeploymentUnit dUnit : dUnits) {
            if(vfsService.getDeployedUnit(dUnit.getIdentifier()) == null)
                log.error("stop() uh-oh .... not going to attempt to undeploy the following dUnit which was previously not deployed : {}", dUnit.getIdentifier());
            else{
                log.info("stop() about to stop following deployment unit : {}", dUnit.getIdentifier());
                vfsService.undeploy(dUnit);
                dUnits.remove(dUnit);
            }
        }
    }
    
    private void ensureDeploymentFileSystemsExist() throws Exception {
        Map<String, Map<String, String>> deployments = DeployUnitParser.getParsedJsonConfig();
        for(Entry<String, Map<String, String>> deployment : deployments.entrySet()) {
            Map<String, String> dHash = deployment.getValue();
            if(DeployUnitParser.LOCAL_FILE_SYSTEM.equals(deployment.getKey())) {
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
            }else if(DeployUnitParser.GIT.equals(deployment.getKey())) {
                URI fsURI = URI.create("git://"+dHash.get(DeployUnitParser.REPO_ALIAS));

                FileSystem fSystem = ioService.getFileSystem(fsURI);
                if(fSystem == null){
                    String gitUser = dHash.get(DeployUnitParser.GIT_USER);
                    String gitPasswd = dHash.get(DeployUnitParser.GIT_PASSWD);
                    String localGitUrl = dHash.get(DeployUnitParser.GIT_LOCAL_REPO_URL);
                    String remoteGitUrl = dHash.get(DeployUnitParser.GIT_REMOTE_REPO_URL);
                    String gitOutDir = dHash.get(DeployUnitParser.GIT_OUT_DIR);
                    StringBuilder sBuilder = new StringBuilder();
                    sBuilder.append("\n\tdeploymentId = "+dHash.get(DeployUnitParser.DEPLOYMENT_ID));
                    sBuilder.append("\n\tlocalGitUrl = "+localGitUrl);
                    sBuilder.append("\n\tgitUser = "+gitUser);
                    sBuilder.append("\n\torigin = "+remoteGitUrl);
                    sBuilder.append("\n\tgitOutDir = "+gitOutDir);
                    final Map<String, Object> env = new HashMap<String, Object>();
                    env.put( JGitFileSystemProvider.USER_NAME, gitUser );
                    env.put( JGitFileSystemProvider.PASSWORD, gitPasswd);
                    if(remoteGitUrl == null || remoteGitUrl.equals(""))
                        throw new Exception("ensureDeployFileSystemsExist() remoteGitUrl can not be null for deploymendId: "+dHash.get(DeployUnitParser.DEPLOYMENT_ID) );
                    env.put( JGitFileSystemProvider.GIT_DEFAULT_REMOTE_NAME, remoteGitUrl);
                    if(gitOutDir != null && !gitOutDir.equals(""))
                        env.put(JGitFileSystemProvider.GIT_ENV_PROP_DEST_PATH, gitOutDir);
                    log.info("ensureDeploymentFileSystemsExist() will clone remote git repo to local as per:  {}", sBuilder.toString());
                    ioService.newFileSystem(fsURI, env, BOOTSTRAP_INSTANCE);
                }else {
                    log.warn("ensureDeploymentFileSystemsExist() following FileSystem already created: {} : scheme = {}", fsURI.toString(), fSystem.provider().getScheme());
                }
            }else{
                log.warn("ensureDeploymentFileSystemsExist() no need to ensure file system exists for type {}"+deployment.getKey());
            }
        }
    }
}
