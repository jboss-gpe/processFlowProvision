package org.jboss.processFlow.tasks.event;

import java.util.Iterator;

import org.jbpm.task.Task;
import org.jbpm.task.event.TaskEventListener;
import org.jbpm.task.event.TaskEventSupport;
import org.jbpm.task.event.TaskFailedEvent;
import org.jbpm.task.service.ContentData;

/**
 * helper class manages the {@link PfpTaskEventListener} registration, and fire events
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class PfpTaskEventSupport extends TaskEventSupport {

    public PfpTaskEventSupport() {
        super();
    }

    public void fireTaskAdded(final Task task, final ContentData cData) {
        final Iterator<TaskEventListener> iter = getEventListenersIterator();

        if (iter.hasNext()) {
            final TaskAddedEvent event = new TaskAddedEvent(task, cData);

            do {
                TaskEventListener listener = iter.next();
                if (listener instanceof PfpTaskEventListener) {
                    ((PfpTaskEventListener) listener).taskAdded(event);
                }
            }
            while (iter.hasNext());
        }
    }

    public void fireTaskReleased(final long taskId, final String userId) {
        final Iterator<TaskEventListener> iter = getEventListenersIterator();

        if (iter.hasNext()) {
            final TaskReleasedEvent event = new TaskReleasedEvent(taskId, userId);

            do {
                TaskEventListener listener = iter.next();
                if (listener instanceof PfpTaskEventListener) {
                    ((PfpTaskEventListener) listener).taskReleased(event);
                }
            }
            while (iter.hasNext());
        }
    }

    public void fireTaskStarted(final long taskId, final String userId) {
        final Iterator<TaskEventListener> iter = getEventListenersIterator();

        if (iter.hasNext()) {
            final TaskStartedEvent event = new TaskStartedEvent(taskId, userId);

            do {
                TaskEventListener listener = iter.next();
                if (listener instanceof PfpTaskEventListener) {
                    ((PfpTaskEventListener) listener).taskStarted(event);
                }
            }
            while (iter.hasNext());
        }
    }

    public void fireTaskDelegated(final long taskId, final String userId, final String targetUserId) {
        final Iterator<TaskEventListener> iter = getEventListenersIterator();

        if (iter.hasNext()) {
            final TaskDelegatedEvent event = new TaskDelegatedEvent(taskId, userId, targetUserId);

            do {
                TaskEventListener listener = iter.next();
                if (listener instanceof PfpTaskEventListener) {
                    ((PfpTaskEventListener) listener).taskDelegated(event);
                }
            }
            while (iter.hasNext());
        }
    }
    
    public void fireTaskExited(final long taskId, final String userId) {
    	
    	final Iterator<TaskEventListener> iter = getEventListenersIterator();

        if (iter.hasNext()) {
            final TaskExitedEvent event = new TaskExitedEvent(taskId, userId);

            do {
            	TaskEventListener listener = iter.next();
            	if (listener instanceof PfpTaskEventListener) {
            		((PfpTaskEventListener) listener).taskExited(event);
            	}
            } while (iter.hasNext());
        }
    	
    }
}
