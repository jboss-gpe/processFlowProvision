package org.jboss.processFlow.tasks.handlers;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.task.Status;
import org.apache.log4j.Logger;
import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.tasks.TaskChangeDetails;
import org.jboss.processFlow.workItem.WorkItemHandlerLifecycle;

/**
 * PFPSkipTaskHandler
 * 
 * places task in a status of "Obsolete" and continues work flow execution of "task" branch
 * executes kSessionProxy.completeWorkItem(...) so as to continue execution of "signaled" branch
 * adds a process instance variable of:  ITaskService.TASK_STATUS,  Status.Obsolete.name()
 * intended for use in an existing JTA transaction
 */
public class PFPSkipTaskHandler extends BasePFPTaskHandler implements WorkItemHandlerLifecycle {

    public static final Logger log = Logger.getLogger("PFPSkipTaskHandler");

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        TaskChangeDetails changeDetails = (TaskChangeDetails)workItem.getParameter(TaskChangeDetails.TASK_CHANGE_DETAILS);
        if(changeDetails == null)
                throw new RuntimeException("executeWorkItem() must supply a workItem parameter of : "+TaskChangeDetails.TASK_CHANGE_DETAILS);

        long taskId = changeDetails.getTaskId();
        String reason = changeDetails.getReason();
        changeDetails.setNewStatus(Status.Obsolete);
        
        // places task in a status of "Obsolete" and continues work flow execution of "task" branch
        taskProxy.skipTask(taskId, ITaskService.ADMINISTRATOR, workItem.getParameters());
        
        // continue execution of "signaled" branch
        kSessionProxy.completeWorkItem(workItem.getId(), workItem.getParameters(), workItem.getProcessInstanceId(), ksessionId);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.error("abortWorkItem() workItemId = "+workItem.getName() +" + workItemName = "+workItem.getName());
    }
}
