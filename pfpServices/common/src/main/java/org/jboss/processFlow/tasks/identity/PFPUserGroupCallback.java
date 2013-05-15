package org.jboss.processFlow.tasks.identity;

import java.util.List;
import javax.enterprise.inject.Alternative;

import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;

import org.apache.log4j.Logger;

@Alternative
@Selectable
public class PFPUserGroupCallback implements UserGroupCallback {

    private static Logger log = Logger.getLogger("PFPUserGroupCallback");

    public PFPUserGroupCallback() {
       log.info("PFPUserGroupCallback() .... constructor instantiated"); 
    }

    public boolean existsUser(String userId) {
        return true;
    }
    public boolean existsGroup(String groupId) {
        return true;
    }

    public List<String> getGroupsForUser(String userId, List<String> groupIds, List<String> allExistingGroupIds) {
        return groupIds;
    }
} 
