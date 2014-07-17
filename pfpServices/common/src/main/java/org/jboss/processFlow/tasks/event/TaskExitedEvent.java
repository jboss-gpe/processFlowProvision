package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.TaskUserEvent;

public class TaskExitedEvent extends TaskUserEvent {
	
	public TaskExitedEvent() {
    }

    public TaskExitedEvent(long taskId, String userId) {
        super(taskId, userId);
    }

}
