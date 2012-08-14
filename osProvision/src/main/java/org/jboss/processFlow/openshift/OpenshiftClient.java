package org.jboss.processFlow.openshift;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientResponse;

/*
 * see :  https://openshift.redhat.com/community/sites/default/files/documents/OpenShift-2.0-REST_API_Guide-en-US.pdf
 */
public interface OpenshiftClient {

    // equivalent :  curl -k -X GET https://openshift.redhat.com/broker/rest/domains --user "<openShiftUserId>:<openShiftPassword>"    
    @GET
    @Path("domains")
    @Produces("application/json")
    ClientResponse<String> getDomains();
   
 
    // curl -k -X POST https://openshift.redhat.com/broker/rest/domains/ --user "<openShiftUserId>:<openShiftPassword>" --data "id=<openShiftUserId>" 
    @POST
    @Path("domains")
    @Consumes("text/plain")
    @Produces("application/json")
    ClientResponse<String> createDomain(@QueryParam("id") String id);


    /* 
        curl -k -X POST https://openshift.redhat.com/broker/rest/domains/<openShiftUserId>/applications --user "<openShiftUserId>:<openShiftPassword>" \
          --data "name=brmsWebs&cartridge=jbossas-7&scale=false&gear_profile=medium"    
    */
    @POST
    @Path("domains/{domainId: /*}/applications")
    @Consumes("test/plain")
    @Produces("application/json")
    ClientResponse<String> createApp(@PathParam("domainId")String domainId, @QueryParam("name") String name, @QueryParam("cartridge")String cartridge, @QueryParam("scale")String scale, @QueryParam("gear_profile")String gearProfile);
  
    /*
        curl -k -X POST https://openshift.redhat.com/broker/rest/domains/<openShiftUserId>/applications/brmsWebs/cartridges \
            --user "<openShiftUserId>:<openShiftPassword>" --data "cartridge=postgresql-8.4"        
    */ 
    @POST
    @Path("domains/{domainId: /*}/applications/{appId: /*}/cartridges")
    @Consumes("test/plain")
    @Produces("application/json")
    ClientResponse<String>addCartridge(@PathParam("domainId")String domainId,
    									@PathParam("appId")String appId,
    									@QueryParam("cartridge") String cartridgeType);

}
