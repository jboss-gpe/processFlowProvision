package org.jboss.processFlow.rest;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wrapper")
public class TaskRefWrapper {
    List<TaskRef> tasks = new ArrayList<TaskRef>();

    public TaskRefWrapper() {
    }

    public TaskRefWrapper(List<TaskRef> tasks) {
        this.tasks = tasks;
    }

    public List<TaskRef> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskRef> tasks) {
        this.tasks = tasks;
    }

    @XmlElement(name = "totalCount")
    public int getTotalCount() {
        return tasks.size();
    }
}
