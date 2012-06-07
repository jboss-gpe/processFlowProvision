package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.TaskUserEvent;

/**
 * Task released/unlock event
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class TaskReleasedEvent extends TaskUserEvent {

    public TaskReleasedEvent() {
    }

    public TaskReleasedEvent(long taskId, String userId) {
        super(taskId, userId);
    }
}
