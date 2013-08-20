package org.jboss.processFlow.services.remote.cdi;

import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;


/*
 * Purpose :
 *   - start a new JTA transaction on REST operations that change state:  POST, PUT and DELETE 
 *
 *
 * Overview :
 *   - the following two JAX-RS entry points are expecting container managed transactions (CMT)
 *       1)  org.kie.services.remote.rest.RuntimeResource
 *       2)  org.kie.services.remote.rest.TaskResource
 *   - when these JAX-RS entry points are bundled in kie-wb, CMT is provided OOB by CDI container and SEAM transactions
 *   - because this Openshift cartridge is guaranteed to run in jboss eap, CMT can be provided by default app server mechanism
 *   - no need to load in SEAM transaction functionality
 */
@Provider
@ServerInterceptor
public class TransactionPreProcessInterceptor extends BaseInterceptor implements PreProcessInterceptor{

    @Override
    public ServerResponse preProcess(HttpRequest hRequest, ResourceMethod rMethod) throws Failure, WebApplicationException {
        Set<String> httpMethods = rMethod.getHttpMethods();
        if(!httpMethods.contains(this.HTTP_GET)) {
            try {
                tMgr.begin();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}

class BaseInterceptor {
    
    protected static final String HTTP_GET    = "GET";
    protected static TransactionManager tMgr = null;
    
    static{
        try {
            Context jndiContext = new InitialContext();
            tMgr = (TransactionManager)jndiContext.lookup("java:/TransactionManager");
        }catch(Exception x){
            throw new RuntimeException(x);
        }
    }
}
