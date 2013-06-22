package org.jboss.processFlow.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "taskReference")
public class TaskRef {
    private long id;
    private String processInstanceId;
    private String processId;

    private String name;
    private String assignee = "";

    private boolean isBlocking;
    private boolean isSignalling = true;

    private List<String> outcomes = new ArrayList<String>();

    public enum STATE {
        OPEN, ASSIGNED, CLOSED
    };

    private STATE currentState;

    private List<ParticipantRef> participantUsers = new ArrayList<ParticipantRef>();
    private List<ParticipantRef> participantGroups = new ArrayList<ParticipantRef>();
    
    private List<TaskContentRef> taskContent = new ArrayList<TaskContentRef>();

    private String url;

    private Date dueDate;
    private Date createDate;
    private int priority;

    private String description;

    public TaskRef() {
        initOrUpdateState();
    }

    public TaskRef(long taskId, String processInstanceId, String processId, String taskName, String assignee, boolean blocking, boolean signalling) {
        this.id = taskId;
        this.processInstanceId = processInstanceId;
        this.processId = processId;
        this.name = taskName;
        setAssignee(assignee);
        isBlocking = blocking;
        isSignalling = signalling;

        initOrUpdateState();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        if (null == assignee)
            assignee = "";

        this.assignee = assignee;
        initOrUpdateState();
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    public boolean isSignalling() {
        return isSignalling;
    }

    public void setSignalling(boolean signalling) {
        isSignalling = signalling;
    }

    public List<String> getOutcomes() {
        return outcomes;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<ParticipantRef> getParticipantUsers() {
        return participantUsers;
    }

    public List<ParticipantRef> getParticipantGroups() {
        return participantGroups;
    }

    private void initOrUpdateState() {
        if (assignee == null || assignee.equals("")) {
            currentState = STATE.OPEN;
        } else {
            currentState = STATE.ASSIGNED;
        }
    }

    public void close() {
        if (STATE.ASSIGNED != currentState)
            throw new IllegalArgumentException("Cannot close task in state " + currentState);

        currentState = STATE.CLOSED;
    }

    public String getProcessId() {
        return processId;
    }

    public STATE getCurrentState() {
        return currentState;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "TaskRef{id:" + id + ",state:" + currentState + "}";
    }

    public List<TaskContentRef> getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(List<TaskContentRef> taskData) {
        this.taskContent = taskData;
    }
}
