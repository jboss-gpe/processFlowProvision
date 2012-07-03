package org.jboss.processFlow.tasks.event;

import org.drools.event.process.ProcessEventListener;
import org.jboss.processFlow.tasks.HumanTaskService;
import org.jbpm.task.event.TaskEventListener;

/**
 * The task lifecycle hook extends the jbpm5 {@link TaskEventListener}, provides more task events
 * 
 * @author tanxu
 * @date May 11, 2012
 * @since
 */
public interface PfpTaskEventListener extends TaskEventListener {

    /**
     * Event triggered when the task is delegated to others
     * 
     * @param event
     */
    void taskDelegated(TaskDelegatedEvent event);
}
