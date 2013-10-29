package org.jboss.processFlow.deploymentMgmt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.security.Principal;
import java.security.acl.Group;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;

import org.jbpm.kie.services.api.IdentityProvider;

/*
    - required by org.kie.services.remote.rest.TaskResource 
    - not quite sure why it needs it considering it already has an HttpServletRequest
    - BPMS Human Task / REST API functionality also needs UserGroupCallback
        - seems that some re-factoring is in order to consolidate on all user/role mapping lookups here and not use UserGroupCallback
*/
@SessionScoped
public class RESTIdentityProvider implements IdentityProvider, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String SIMPLE_PRINCIPAL="class org.jboss.security.SimplePrincipal";

    @Override
    public String getName() {
        
        String callerPrincipalName = "Unable to locate callerPrincipal";
        try {
            Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
            Set<Principal> principals = subject.getPrincipals();
            for (Principal principal : principals) {
                if(principal.getClass().toString().equals(SIMPLE_PRINCIPAL)){
                    callerPrincipalName = ((org.jboss.security.SimplePrincipal)principal).getName();
                }
            }
            return callerPrincipalName;
        }catch(Exception x){
            throw new RuntimeException();
        }
        
    }

    @Override
    public List<String> getRoles() {
        List<String> roles = new ArrayList<String>();

        Subject subject = null;
        try {
            subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
        }catch(Exception x){
            throw new RuntimeException();
        }

        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();

            if (principals != null) {
                    roles = new ArrayList<String>();
                    for (Principal principal : principals) {
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
                System.out.println("getRoles() principals == null for "+subject);
            }
        }else {
            System.out.println("getRoles() subject == null ");
        }
        return roles;
    }

}
