package org.kie.services.remote.util;

import java.util.List;
import javax.enterprise.inject.Alternative;

import org.jbpm.shared.services.cdi.Selectable;
import org.kie.internal.task.api.UserGroupCallback;

@Alternative
@Selectable
public class RESTUserGroupCallback implements UserGroupCallback {

    public RESTUserGroupCallback() {
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
