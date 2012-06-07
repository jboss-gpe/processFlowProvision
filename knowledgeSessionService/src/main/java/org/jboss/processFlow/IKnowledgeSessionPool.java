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

/**
 * The interface to manage the pool of knowledge session id
 * 
 * @author tanxu
 * @date Mar 4, 2012
 * @since
 */
public interface IKnowledgeSessionPool {

    /**
     * Return any availabed sessionId, which has been returned to the pool after the process instance completed
     * 
     * @return the available sessionId, or <code>-1</code> if no available sessionId
     */
    Integer getAvailableSessionId();

    /**
     * Tell whether the specified <code>sessionId</code> is borrowed (aka. is busy/used by one process instance)
     * 
     * @param sessionId the sessionId
     * @param processId the root processId
     * @return
     */
    boolean isBorrowed(Integer sessionId, String processId);

    /**
     * Mark the speicified <code>sessionId</code> as borrowed in the pool. A new entry will be created if no one exists
     * in the pool
     * 
     * @param sessionId the sessionId
     * @param processId the root processId
     */
    void markAsBorrowed(Integer sessionId, String processId);

    /**
     * Mark the speicfied <code>sessionId</code> as returned/available/freed in the pool.
     * 
     * @param sessionId the sessionId
     */
    void markAsReturned(Integer sessionId);

    /**
     * dump the internal status for debugging.
     * 
     * @return the dump string
     */
    String dumpSessionStatusInfo();

    /**
     * associate a ksessionid with a process instance id
     */
    void setProcessInstanceId(Integer ksessionId, Long pInstanceId);

    /**
     * return the ksessionId corresponding to an active processInstanceId
     *
     * @param processInstanceId of active process instance
     * @return ksessionId
     */
    Integer getSessionId(Long pInstanceId);
}
