package org.jboss.processFlow.services.remote.cdi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.security.Principal;
import java.security.acl.Group;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
//import javax.security.auth.Subject;
//import javax.security.jacc.PolicyContext;

import org.jbpm.kie.services.api.IdentityProvider;

/*
    - required by org.kie.services.remote.rest.TaskResource 
    - not quite sure why it needs it considering it already has an HttpServletRequest
*/
@SessionScoped
public class RESTIdentityProvider implements IdentityProvider, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "testUser";
    }

    @Override
    public List<String> getRoles() {
        List<String> roles = new ArrayList<String>();

        /*
        Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");

        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();

            if (principals != null) {
                    roles = new ArrayList<String>();
                    for (Principal principal : principals) {
                        log.info("principal = "+principal);
                        if (principal instanceof Group  && "Roles".equalsIgnoreCase(principal.getName())) {
                            Enumeration<? extends Principal> groups = ((Group) principal).members();

                            while (groups.hasMoreElements()) {
                                Principal groupPrincipal = (Principal) groups.nextElement();
                                roles.add(groupPrincipal.getName());

                            }
                            break;
                        }

                    }
            }else{
                System.out.println("getRoles() principals == null for "+request.getUserPrincipal().getName());
            }
        }else {
            System.out.println("getRoles() subject == null for "+request.getUserPrincipal().getName());
        }
        */
        return roles;
    }

}
