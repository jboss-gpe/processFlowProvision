package org.jboss.processFlow;

import org.jbpm.task.service.TaskService;

public class PFPBaseService {

    /* static variable because :
     *   1)  TaskService is a thread-safe object
     *   2)  TaskService is needed for both :
     *     - PFP HumanTaskService           :   functions using a jta enable entity manager for human task functionality
     *     - PFP KnowledgeSessionService    :   needed to instantiate TasksAdmin object and register with knowledgeSession
     */
    protected static TaskService jtaTaskService;
}
