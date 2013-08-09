package org.jboss.processFlow.services.remote.cdi;


import java.io.File;
import java.net.URI;
import java.util.List;
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
import org.kie.commons.io.FileSystemType.Bootstrap;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceDotFileImpl;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.commons.io.FileSystemType;
import org.kie.commons.java.nio.file.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RESTApplicationScopedProducer {
	
	public static final String DEPLOYMENT_ID = "org.jboss.processFlow.vfs.deployment.id";
    public static final String VFS_PATH = "org.jboss.processFlow.vfs.repository.folder";
    public static final String VFS_ALIAS = "org.jboss.processFlow.vfs.repository.alias";
    public static final String VFS_SCHEME = "org.jboss.processFlow.vfs.repository.scheme";
    public static final String KSESSION_STRATEGY = "org.jboss.processFlow.cdi.ksession.strategy";
    
    public static final String GIT_USER = "org.kie.services.git.user";
    public static final String GIT_PASSWD = "org.kie.services.git.passwd";
    public static final String GIT_REPO_URL = "org.kie.services.git.repo.url";
    public static final String GIT_REMOTE_REPO_URL = "org.kie.services.git.remote.repo.url";
    
    private static String deploymentId = System.getProperty(DEPLOYMENT_ID, "please set org.jboss.processFlow.vfs.deployment.id system property");
    private static String vfsPath = System.getProperty(VFS_PATH, null);
    private static String remoteGitUrl = "https://github.com/guvnorngtestuser1/jbpm-console-ng-playground.git";
    private static RuntimeStrategy ksessionStrategy = DeploymentUnit.RuntimeStrategy.PER_PROCESS_INSTANCE;
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
    
    private Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");
    
    private IOService vfsIOService;
    private String gitUser = "jboss";
    private String gitPasswd = "bpms";
    private String gitUrl = "git://jbpm-local";
    private URI fsURI = null;
    
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
    public List<DeploymentUnit> getDeploymentUnits() {
    	List<DeploymentUnit> dUnits = new CopyOnWriteArrayList<DeploymentUnit>();
    	VFSDeploymentUnit vfsUnit = createVfsDeploymentUnit();
    	//KModuleDeploymentUnit kModule = this.createKModuleDeploymentUnit();
    	dUnits.add(vfsUnit);
    	return dUnits;
    }
    
    private VFSDeploymentUnit createVfsDeploymentUnit(){
    	String vfsAlias = System.getProperty(VFS_ALIAS, null);
    	String vfsScheme = System.getProperty(this.VFS_SCHEME, null);
    	ksessionStrategy = RuntimeStrategy.valueOf(System.getProperty(this.KSESSION_STRATEGY, this.ksessionStrategy.toString()));
    	StringBuilder sBuilder = new StringBuilder();
    	sBuilder.append("start() creating deploymentUnit with \n\tdeploymentId = ");
    	sBuilder.append(deploymentId);
    	sBuilder.append("\n\tvfsPath = ");
    	sBuilder.append(vfsPath);
    	sBuilder.append("\n\tvfsAlias = ");
    	sBuilder.append(vfsAlias);
    	sBuilder.append("\n\tvfsScheme = ");
    	sBuilder.append(vfsScheme);
    	sBuilder.append("\n\tksessionStrategy = ");
    	sBuilder.append(ksessionStrategy.toString());
    	log.info(sBuilder.toString());
    	VFSDeploymentUnit vfsUnit = new VFSDeploymentUnit(deploymentId, vfsAlias, vfsPath+deploymentId);
    	vfsUnit.setStrategy(ksessionStrategy);
    	if(vfsScheme != null && !vfsScheme.equals(""))
    		vfsUnit.setRepositoryScheme(vfsScheme);
    	return vfsUnit;
    }
    
    private KModuleDeploymentUnit createKModuleDeploymentUnit() {
    	String ARTIFACT_ID = "test-module";
        String GROUP_ID = "org.jbpm.test";
        String VERSION = "1.0.0-SNAPSHOT";

    	KModuleDeploymentUnit kUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, "KBase-test", "ksession-test");
    	return kUnit;
    }


    // required for ioStrategy field in:  org.jbpm.shared.services.impl.VFSFileServiceImpl    
    @Produces
    @Named("ioStrategy")
    public IOService getIOService() {
        try{
            if(vfsIOService == null) {
            	
            	// what's the difference between IOServiceNio2WrapperImpl and IOServiceDotFileImpl
            	//vfsIOService = new IOServiceDotFileImpl();
            	vfsIOService = new IOServiceNio2WrapperImpl();
            	
                // if vfsPath is a file system path than need to make sure that is already exists
            	// org.kie.commons.java.nio.fs.file.SimpleFileSystemProvider will not auto-create that directory
            	File vfsFile = new File(vfsPath + deploymentId);
            	if(!vfsFile.exists()){
            		log.info("getIOService() creating file system path: {}", vfsFile.getAbsolutePath());
            		vfsFile.mkdirs();
            	}else{
            		log.info("getIOService() file path already created : {}", vfsFile.getAbsolutePath());
            	}
    
            /*        
            if(fsURI == null)
                fsURI = URI.create(gitUrl);
            
            FileSystem fSystem = vfsIOService.getFileSystem(fsURI);
            if(fSystem == null){
                gitUser = System.getProperty(this.GIT_USER, gitUser);
                gitPasswd = System.getProperty(this.GIT_PASSWD, gitPasswd);
                gitUrl = System.getProperty(this.GIT_REPO_URL, this.gitUrl);
                remoteGitUrl = System.getProperty(this.GIT_REMOTE_REPO_URL, remoteGitUrl);
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append("\n\t gitUser = "+gitUser);
                sBuilder.append("\n\tgitUrl = "+gitUrl);
                sBuilder.append("\n\tremoteGitUrl = "+remoteGitUrl);
                log.info("getIOService() {}", sBuilder.toString());
                final Map<String, Object> env = new HashMap<String, Object>();
                env.put( "username", gitUser );
                env.put( "password", gitPasswd);
                env.put( "origin", remoteGitUrl);
                vfsIOService.newFileSystem(fsURI, env, FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE);
            }else {
                log.debug("getIOService() following FileSystem already created: {}", this.gitUrl);
            }
            */
            }
        }catch(Exception x){
            throw new RuntimeException(x);
        }
        return vfsIOService;
    }
}
