/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.processFlow.tasks;

import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.eventmessaging.EventResponseHandler;
import org.jbpm.eventmessaging.Payload;
import org.jbpm.task.*;
import org.jbpm.task.event.TaskCompletedEvent;
import org.jbpm.task.event.TaskEvent;
import org.jbpm.task.event.TaskFailedEvent;
import org.jbpm.task.event.TaskSkippedEvent;
import org.jbpm.task.service.responsehandlers.AbstractBaseResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * this class is registered with the org.jbpm.task.event.TaskEventSupport object managed by org.jbpm.task.service.TaskService
 * afterwards, with a task skipped or failed event, execute() method of this class is invoked
 */    
public class TaskEventHandler extends AbstractBaseResponseHandler implements EventResponseHandler {

    public static final Logger log = LoggerFactory.getLogger("TaskEventHandler");

    public void execute(Payload payload) {
        TaskEvent event = ( TaskEvent ) payload.get();
        if (event instanceof TaskSkippedEvent){
            throw new RuntimeException("please implement TaskSkippedEvent");
        } else if (event instanceof TaskFailedEvent) {
            throw new RuntimeException("please implement TaskFailedEvent");
        } else {
            throw new RuntimeException("execute() following event not implemented : "+event);
        }
    }
        
    // JA Bride:  will set this to true since registering this handler with every 'executeWorkItem' invocation
    // - determines whether this responseHandler will be removed from the 'responseHandlers' data-structure in org.jbpm.task.service.TaskClientHandler
    public boolean isRemove() {
        return false;
    }
}

