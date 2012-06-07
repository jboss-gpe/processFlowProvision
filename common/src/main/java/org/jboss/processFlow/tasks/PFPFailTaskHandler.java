package org.jboss.processFlow.tasks;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.task.Status;
import org.apache.log4j.Logger;


/**
 * PFPFailTaskHandler
 * 
 * places task in a status of "Failed" and continues work flow execution of "task" branch
 * as per WS-HT specification, section 4.7 ,  task status must already be "InProgress" for this operation to be valid
 * executes kSessionProxy.completeWorkItem(...) so as to continue execution of "signaled" branch
 * expects a workItem parameter keyed by:  TaskChangeDetails.TASK_CHANGE_DETAILS and of type org.jboss.processFlow.TaskChangeDetails
 * sets TaskChangeDetails.newStatus = Status.Failed
 * intended for use in an existing JTA transaction
 */
public class PFPFailTaskHandler extends BasePFPTaskHandler implements WorkItemHandlerLifecycle {

    public static final Logger log = Logger.getLogger("PFPFailTaskHandler");

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    	TaskChangeDetails changeDetails = (TaskChangeDetails)workItem.getParameter(TaskChangeDetails.TASK_CHANGE_DETAILS);
    	if(changeDetails == null)
    		throw new RuntimeException("executeWorkItem() must supply a workItem parameter of : "+TaskChangeDetails.TASK_CHANGE_DETAILS);
    	
    	long taskId = changeDetails.getTaskId();
    	String reason = changeDetails.getReason();
    	changeDetails.setNewStatus(Status.Failed);
    	
    	// places task in a status of "Failed" and continues work flow execution of "task" branch
        taskProxy.failTask(taskId, workItem.getParameters(), null, reason, false);
        
        // executes kSessionProxy.completeWorkItem(...) so as to continue execution of "signaled" branch
        kSessionProxy.completeWorkItem(ksessionId, workItem.getId(), workItem.getParameters());
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.error("abortWorkItem() workItemId = "+workItem.getName() +" + workItemName = "+workItem.getName());
    }
}
