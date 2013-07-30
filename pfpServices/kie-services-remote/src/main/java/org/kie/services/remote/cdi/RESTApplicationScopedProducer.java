package org.kie.services.remote.cdi;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;

import org.kie.commons.io.FileSystemType.Bootstrap;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.commons.io.FileSystemType;
import org.kie.commons.java.nio.file.FileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RESTApplicationScopedProducer {
    
    public static final String GIT_USER = "org.kie.services.git.user";
    public static final String GIT_PASSWD = "org.kie.services.git.passwd";
    public static final String GIT_REPO_URL = "org.kie.services.git.repo.url";
    public static final String GIT_REMOTE_REPO_URL = "org.kie.services.git.remote.repo.url";
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
    
    private Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");
    
    private IOService vfsIOService;
    private String gitUser = "jboss";
    private String gitPasswd = "bpms";
    private String remoteGitUrl = "https://github.com/guvnorngtestuser1/jbpm-console-ng-playground.git";
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
    @Named("ioStrategy")
    public IOService getIOService() {
        try{
        	if(vfsIOService == null)
        	    vfsIOService = new IOServiceNio2WrapperImpl();
        	
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
            	log.info("getIOService() following FileSystem already created: {}", this.gitUrl);
            }
        }catch(Exception x){
            throw new RuntimeException(x);
        }
        return vfsIOService;
    }
}
