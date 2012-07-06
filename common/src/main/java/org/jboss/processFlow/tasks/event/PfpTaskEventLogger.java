package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the task event listener logs all events
 * 
 * @author tanxu
 * @date May 12, 2012
 * @since
 */
public class PfpTaskEventLogger implements PfpTaskEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PfpTaskEventLogger.class);

    public void taskClaimed(TaskClaimedEvent event) {
        logger.info("task {} claimed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskCompleted(TaskCompletedEvent event) {
        logger.info("task {} completed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskFailed(TaskFailedEvent event) {
        logger.info("task {} failed by {}", event.getTaskId(), event.getUserId());
    }

    public void taskSkipped(TaskSkippedEvent event) {
        logger.info("task {} skipped by {}", event.getTaskId(), event.getUserId());
    }

    public void taskDelegated(TaskDelegatedEvent event) {
        logger.info("task {} delegated by {}", event.getTaskId(), event.getUserId());
    }

    public void taskCreated(TaskCreatedEvent event) {
        logger.info("task {} created", event.getTaskId());
    }

    public void taskReleased(TaskReleasedEvent event) {
        logger.info("task {} released by {}", event.getTaskId(), event.getUserId());
    }

    public void taskStarted(TaskStartedEvent event) {
        logger.info("task {} started by {}", event.getTaskId(), event.getUserId());
    }

    public void taskForwarded(TaskForwardedEvent event) {
        logger.info("task {} forwarded by {}", event.getTaskId(), event.getUserId());
    }

    public void taskStopped(TaskStoppedEvent event) {
        logger.info("task {} stopped by {}", event.getTaskId(), event.getUserId());
    }

}
