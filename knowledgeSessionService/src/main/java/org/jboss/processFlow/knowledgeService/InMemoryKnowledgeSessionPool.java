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

package org.jboss.processFlow.knowledgeService;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The in-memory pool that store the knowledgeSession busy/available data structure in the memory. <br/>
 * <b>Note: upon either a reboot of the server or hot-deploy of the knowledgeSessionService, our 'availableSessions'
 * data structure is cleared out</b>
 * 
 * @author tanxu
 * @date Mar 5, 2012
 * @since
 */
public class InMemoryKnowledgeSessionPool implements IKnowledgeSessionPool {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKnowledgeSessionPool.class);

    private static final int sleepDuration = 500;

    /**
     * this map contains the mapping between ksession and the top level processId.<br/>
     * this is mainly used to tell whether to make the ksession as available afterProcessCompleted()
     */
    private ConcurrentMap<Integer, String> busySessions = new ConcurrentHashMap<Integer, String>();
    private LinkedList<Integer> availableSessions = new LinkedList<Integer>();
    private ConcurrentMap<Long, Integer> procInstSessionMap = new ConcurrentHashMap<Long, Integer>();
    transient final ReentrantLock availableSessionsLock = new ReentrantLock();

    public InMemoryKnowledgeSessionPool() {
    }

    @Override
    public Integer getAvailableSessionId() {
        int sessionId = -1;
        if (availableSessions.size() > 0) {
            int size;
            availableSessionsLock.lock();
            try {
                sessionId = availableSessions.removeFirst();
                size = availableSessions.size();
            }
            finally {
                availableSessionsLock.unlock();
            }

            if (size == 0) {
                log.warn("getStatefulKnowledgeSession() just pulled last session off list .... will sleep for following millis to prevent potential optimistic lock problems with drools SessionInfo : "
                        + sleepDuration);
                try {
                    Thread.sleep(sleepDuration);
                }
                catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }
        return sessionId;
    }

    @Override
    public boolean isBorrowed(Integer sessionId, String processId) {
        if (busySessions.containsKey(sessionId)) {
            if (processId.equals((String) busySessions.get(sessionId))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void markAsBorrowed(Integer sessionId, String processId) {
        busySessions.put(sessionId, processId);
    }

    @Override
    public void markAsReturned(Integer sessionId) {
        busySessions.remove(new Integer(sessionId));
        availableSessionsLock.lock();
        try {
            availableSessions.addLast(new Integer(sessionId));
        }
        finally {
            availableSessionsLock.unlock();
        }
    }

    public String dumpSessionStatusInfo() {
        StringBuilder sBuilder = new StringBuilder(
                "dumpSessionStatusInfo()\nThe following is a list of sessions that are available for re-use by this knowledgeSessionService :\n");
        for (Integer ksessionId : availableSessions) {
            sBuilder.append(ksessionId);
            sBuilder.append(" ");
        }
        sBuilder.append("\n\nThe following is a list of sessions that are not yet available for re-use by this knowledgeSessionService :\n");
        for (Integer ksessionId : busySessions.keySet()) {
            sBuilder.append(ksessionId);
            sBuilder.append(":");
            sBuilder.append(busySessions.get(ksessionId));
            sBuilder.append(" ");
        }
        return sBuilder.toString();
    }

    public void setProcessInstanceId(Integer ksessionId, Long pInstanceId) {
        procInstSessionMap.put(pInstanceId, ksessionId);
    }

    public Integer getSessionId(Long pInstanceId) {
        return procInstSessionMap.get(pInstanceId);
    }
}
