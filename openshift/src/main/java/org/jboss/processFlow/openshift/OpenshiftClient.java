package org.jboss.processFlow.openshift;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public interface OpenshiftClient {
	
	@GET
	@Path("domains")
	@Produces("applications/json")
	Response.Status getDomains();

}
