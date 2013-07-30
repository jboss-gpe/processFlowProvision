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
    public static final String GIT_REPO_url = "org.kie.services.git.repo.url";
    
    @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
    EntityManagerFactory jbpmCoreEMF;
    
    @Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
    
    private Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");
    
    private IOService vfsIOService;
    private String gitUser = "jboss";
    private String gitPasswd = "bpms";
    private String remoteGitUrl = "changeMe";
    private String gitUrl = "git://jbpm-local";
    
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
        vfsIOService = new IOServiceNio2WrapperImpl();
        /*try{
            final URI fsURI = URI.create(gitUrl);
            FileSystem fSystem = vfsIOService.getFileSystem(fsURI);
            if(fSystem == null){
                gitUser = System.getProperty(this.GIT_USER, gitUser);
                gitPasswd = System.getProperty(this.GIT_PASSWD, gitPasswd);
                gitUrl = System.getProperty(this.GIT_REPO_url, this.gitUrl);
                log.info("getIOService() using gitUser of {} to create fileSystem with URL of {}", gitUser, gitUrl);
                final Map<String, Object> env = new HashMap<String, Object>();
                env.put( "username", gitUser );
                env.put( "password", gitPasswd);
                env.put( "origin", remoteGitUrl);
                vfsIOService.newFileSystem(fsURI, env, FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE);
            }
        }catch(Exception x){
            throw new RuntimeException(x);
        }*/
        return vfsIOService;
    }
}
