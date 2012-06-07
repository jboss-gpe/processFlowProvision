package org.jboss.processFlow.tasks.event;

import org.jbpm.task.Task;
import org.jbpm.task.event.TaskUserEvent;
import org.jbpm.task.service.ContentData;

/**
 * Task added event
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class TaskAddedEvent extends TaskUserEvent {

    private Task task;
    private ContentData cData;

    /**
     * @param task the <code>task</code> instance in detached state, changes on this object won't be persisted into
     *            database
     * @param cData
     */
    public TaskAddedEvent(Task task, ContentData cData) {
        super(task.getId(), null);
        this.task = task;
        this.cData = cData;
    }

    /**
     * @return the <code>task</code> instance in detached state, changes on this object won't be persisted into database
     */
    public Task getTask() {
        return this.task;
    }

    public ContentData getContentData() {
        return this.cData;
    }
}
