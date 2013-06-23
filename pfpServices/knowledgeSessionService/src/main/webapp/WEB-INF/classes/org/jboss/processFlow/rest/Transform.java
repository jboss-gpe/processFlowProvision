package org.jboss.processFlow.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jbpm.task.I18NText;
import org.jbpm.task.Task;
import org.jbpm.task.query.TaskSummary;

public class Transform {

    public static TaskRef task(TaskSummary task) {
        return new TaskRef(task.getId(), Long.toString(task.getProcessInstanceId()), "", task.getName(), task.getActualOwner() == null ? null : task
                .getActualOwner().getId(), false, false);
    }

    public static TaskRef task(Task task) {
        String name = "";
        for (I18NText text : task.getNames()) {
            if ("en-UK".equals(text.getLanguage())) {
                name = text.getText();
            }
        }
        return new TaskRef(task.getId(), Long.toString(task.getTaskData().getProcessInstanceId()), "", name, task.getTaskData().getActualOwner() == null ? null
                : task.getTaskData().getActualOwner().getId(), false, false);
    }
    
    public static List<TaskContentRef> taskContent(Map<String, Object> taskContent) {
        List<TaskContentRef> taskContentRefList = new ArrayList<TaskContentRef>();
        if (taskContent != null) {
            for (String key : taskContent.keySet()) {
                TaskContentRef taskContentRef = new TaskContentRef();
                taskContentRef.setKey(key);
                taskContentRef.setValue(taskContent.get(key).toString());
                taskContentRefList.add(taskContentRef);
            }
        }
        return taskContentRefList;
    }

}
