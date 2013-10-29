package org.jboss.processFlow.deploymentMgmt;

import java.util.List;
import org.kie.internal.task.api.UserGroupCallback;
import org.jbpm.shared.services.cdi.Selectable;
import javax.enterprise.inject.Alternative;

/*
  purpose
    - provides ability to opt out of any jbpm human task UserGroupCallback security

  details
    - in some scenarios, it might be desirable not to by-pass UserGroupCallback security required by jbpm
    - the assumption would be that the invocation to task resources via REST API would already have been authorized by a service upstream of this call
    - 25 Sept 2013
        - probably want to stick with one of the out-of-the-box jbpm UserGroupCallback implementations
        - otherwise, queries that rely on UserGroupCallback in org.jbpm.services.task.identity.UserGroupTaskQueryServiceDecorator
          (such as getTasksAssignedAsPotentialOwnerByStatus(...) will query by an empty list of groupIds
*/
@Alternative
@Selectable
public class NoOpUserGroupCallbackImpl implements UserGroupCallback {

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
