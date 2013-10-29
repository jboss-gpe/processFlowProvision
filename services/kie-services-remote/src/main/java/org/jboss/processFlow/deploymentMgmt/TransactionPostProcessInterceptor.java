package org.jboss.processFlow.deploymentMgmt;

import javax.transaction.Status;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;


/*
 * Purpose :
 *     - commit any active transactions just prior to REST response being sent to client
 */
@Provider
@ServerInterceptor
public class TransactionPostProcessInterceptor extends BaseInterceptor implements PostProcessInterceptor{
    
    @Override
    public void postProcess(ServerResponse sResponse) {
        try {
            if(tMgr.getStatus() == Status.STATUS_ACTIVE)
                tMgr.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
