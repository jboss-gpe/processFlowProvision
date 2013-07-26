package org.kie.services.remote.cdi;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;

import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;

@ApplicationScoped
public class RESTApplicationScopedProducer {
	
	@PersistenceUnit(unitName="org.jbpm.persistence.jpa")
	EntityManagerFactory jbpmCoreEMF;
	
	@Inject
    @Selectable
    private UserGroupCallback userGroupCallback;
	
	private IOService vfsIOStrategy;
	
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
		if(vfsIOStrategy == null)
			vfsIOStrategy = new IOServiceNio2WrapperImpl();
		
		return vfsIOStrategy;
		
	}
}
