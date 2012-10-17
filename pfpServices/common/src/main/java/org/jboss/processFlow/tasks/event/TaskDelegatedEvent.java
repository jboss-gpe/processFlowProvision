package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.TaskUserEvent;

/**
 * Task delegated event
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class TaskDelegatedEvent extends TaskUserEvent {

    private String targetUserId;

    public TaskDelegatedEvent() {
    }

    public TaskDelegatedEvent(long taskId, String userId, String targetUserId) {
        super(taskId, userId);
        this.targetUserId = targetUserId;
    }

    public String getTargetUserId() {
        return this.targetUserId;
    }
}
