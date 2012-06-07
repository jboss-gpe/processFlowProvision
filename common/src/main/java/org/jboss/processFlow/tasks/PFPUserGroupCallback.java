package org.jboss.processFlow.tasks;

import java.util.List;
import org.jbpm.task.identity.UserGroupCallback;

public class PFPUserGroupCallback implements UserGroupCallback {

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
