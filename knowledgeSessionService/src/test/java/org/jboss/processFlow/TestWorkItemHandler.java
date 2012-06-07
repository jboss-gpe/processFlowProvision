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

package org.jboss.processFlow;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;
import org.apache.log4j.Logger;

import org.jboss.processFlow.tasks.WorkItemHandlerLifecycle;


public class TestWorkItemHandler implements WorkItemHandlerLifecycle {

    private static Logger log = Logger.getLogger("TestWorkItemHandler");

    public TestWorkItemHandler(StatefulKnowledgeSession session) {
        log.info("constructor() session id = "+session.getId());
    }

    public void connect() {
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.info("executeWorkItem() workItem = "+workItem+" :   workItemManager = "+manager);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.info("abortWorkItem() workItem = "+workItem+" :   workItemManager = "+manager);
    }

    public void dispose() {
    }

    @Override
    public void init(StatefulKnowledgeSession ksession) {
        // TODO Auto-generated method stub
        
    }
} 
