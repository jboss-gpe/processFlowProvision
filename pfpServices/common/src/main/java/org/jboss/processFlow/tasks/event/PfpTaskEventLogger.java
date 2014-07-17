package org.jboss.processFlow.tasks.event;

import org.jbpm.task.event.TaskClaimedEvent;
import org.jbpm.task.event.TaskCompletedEvent;
import org.jbpm.task.event.TaskFailedEvent;
import org.jbpm.task.event.TaskSkippedEvent;
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

    @Override
    public void taskClaimed(TaskClaimedEvent event) {
        logger.info("task {} claimed by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskCompleted(TaskCompletedEvent event) {
        logger.info("task {} completed by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskFailed(TaskFailedEvent event) {
        logger.info("task {} failed by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskSkipped(TaskSkippedEvent event) {
        logger.info("task {} skipped by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskDelegated(TaskDelegatedEvent event) {
        logger.info("task {} delegated by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskAdded(TaskAddedEvent event) {
        logger.info("task {} added", event.getTaskId());
    }

    @Override
    public void taskReleased(TaskReleasedEvent event) {
        logger.info("task {} released by {}", event.getTaskId(), event.getUserId());
    }

    @Override
    public void taskStarted(TaskStartedEvent event) {
        logger.info("task {} started by {}", event.getTaskId(), event.getUserId());
    }

	@Override
	public void taskExited(TaskExitedEvent event) {
		logger.info("task {} exited by {}", event.getTaskId(), event.getUserId());
	}

}
