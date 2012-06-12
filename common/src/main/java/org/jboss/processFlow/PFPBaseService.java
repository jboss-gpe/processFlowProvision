package org.jboss.processFlow;

import org.jbpm.task.service.TaskService;

public class PFPBaseService {

    /* static variable because :
     *   1)  TaskService is a thread-safe object
     *   2)  TaskService is needed for both :
     *     - PFP HumanTaskService           :   pretty much every function requires a handle to TaskService
     *     - PFP KnowledgeSessionService    :   needed to instantiate TasksAdmin object and register with knowledgeSession
     */
    protected static TaskService taskService;
}
