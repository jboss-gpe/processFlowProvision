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

public interface OpenshiftClient {
    
    @GET
    @Path("domains")
    @Produces("application/json")
    ClientResponse<String> getDomains();
    
    @POST
    @Path("domains")
    @Consumes("text/plain")
    @Produces("application/json")
    ClientResponse<String> createDomain(@QueryParam("id") String id);
    
    @POST
    @Path("domains/{domainId: /*}/applications")
    @Consumes("test/plain")
    @Produces("application/json")
    ClientResponse<String> createApp(@PathParam("domainId")String domainId, @QueryParam("name") String name, @QueryParam("cartridge")String cartridge, @QueryParam("scale")String scale, @QueryParam("gear_profile")String gearProfile);

}
