package org.jboss.processFlow.tasks.event;

import java.util.Iterator;

import org.jbpm.task.Task;
import org.jbpm.task.event.*;
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
}
