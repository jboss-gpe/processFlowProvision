package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.TaskUserEvent;

/**
 * Task started event
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class TaskStartedEvent extends TaskUserEvent {

    public TaskStartedEvent() {
    }

    public TaskStartedEvent(long taskId, String userId) {
        super(taskId, userId);
    }
}
