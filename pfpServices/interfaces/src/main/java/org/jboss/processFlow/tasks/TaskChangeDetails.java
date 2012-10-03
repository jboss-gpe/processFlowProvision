package org.jboss.processFlow.tasks;

import java.util.Date;

import org.jbpm.task.Status;

public class TaskChangeDetails implements java.io.Serializable {
	public static final String TASK_CHANGE_DETAILS = "TASK_CHANGE_DETAILS";
	public static final String NORMAL_COMPLETION_REASON = "TASK COMPLETED NORMALLY";
	public static final String REASON = "reason";
	public static final String TASK_ID = "taskId";
	public static final String NEW_STATUS = "newStatus";
	
	private String reason;
	private long taskId;
	private Status newStatus;

	public Status getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(Status newStatus) {
		this.newStatus = newStatus;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
