package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.entity.*;
import org.jbpm.task.event.TaskEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the task event listener logs all events
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class PfpTaskEventLogger implements TaskEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PfpTaskEventLogger.class);

    public void taskClaimed(TaskUserEvent event) {
        logger.info("task {} claimed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskCompleted(TaskUserEvent event) {
        logger.info("task {} completed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskFailed(TaskUserEvent event) {
        logger.info("task {} failed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskSkipped(TaskUserEvent event) {
        logger.info("task {} skipped by {}", event.getTaskId(), event.getUserId());
    }

    public void taskCreated(TaskUserEvent event) {
        logger.info("task {} created", event.getTaskId());
    }

    public void taskReleased(TaskUserEvent event) {
        logger.info("task {} released by {}", event.getTaskId(), event.getUserId());
    }

    public void taskStarted(TaskUserEvent event) {
        logger.info("task {} started by {}", event.getTaskId(), event.getUserId());
    }

    public void taskForwarded(TaskUserEvent event) {
        logger.info("task {} forwarded by {}", event.getTaskId(), event.getUserId());
    }

    public void taskStopped(TaskUserEvent event) {
        logger.info("task {} stopped by {}", event.getTaskId(), event.getUserId());
    }

}
