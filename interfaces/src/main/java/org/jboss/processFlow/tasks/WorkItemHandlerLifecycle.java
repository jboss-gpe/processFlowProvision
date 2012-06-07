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

import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.StatefulKnowledgeSession;

/*
    JA Bride:  18 August 2011
        -- an instance of a workItemHandler is associated with an instance of a StatefulKnowledgeSession
        -- when a StatefulKnowledgeSession is disposed, so to should a workItemHandler
        -- currently, there is no mechanism from within the statefulKnowledgeSession to dispose of it workItemHandler when it (the knowledgeSession) is disposed
        -- this 'lifecycle' interface is a temporary work around to allow a knowledge session 'service' to dispose of a work item handler 
            in conjuntion with the disposal of its corresponding knowledgeSession
*/
public interface WorkItemHandlerLifecycle extends WorkItemHandler {
    public void init(StatefulKnowledgeSession ksession);
    public void dispose();
} 
