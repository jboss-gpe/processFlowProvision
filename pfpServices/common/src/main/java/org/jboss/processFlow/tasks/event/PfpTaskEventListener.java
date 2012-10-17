package org.jboss.processFlow.tasks.event;

import org.drools.event.process.ProcessEventListener;
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
     * Event triggered when new task added.
     * <p>
     * <b>IMPORTANT</b>: since the
     * {@link HumanTaskService#addTask(org.jbpm.task.Task, org.jbpm.task.service.ContentData)} marked as
     * <code>TransactionAttributeType.NOT_SUPPORTED</code>, the business data in the knowledgeSession JTA transaction is
     * invisible at this point. <br/>
     * As a workaround, you can catch similar event in
     * {@link ProcessEventListener#afterNodeTriggered(org.drools.event.process.ProcessNodeTriggeredEvent)}
     * </p>
     * 
     * @param event
     */
    void taskAdded(TaskAddedEvent event);

    /**
     * Event triggered when the task is unlocked, which is released to the group
     * 
     * @param event
     */
    void taskReleased(TaskReleasedEvent event);

    /**
     * Event triggered when the task is started, which is transitive state
     * 
     * @param event
     */
    void taskStarted(TaskStartedEvent event);

    /**
     * Event triggered when the task is delegated to others
     * 
     * @param event
     */
    void taskDelegated(TaskDelegatedEvent event);
}
