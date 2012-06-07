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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The pool persists the knowledgeSession busy/available data structure into the table <code>SessionProcessXref</code>,
 * so that we won't lost the session info after restart or hot-deploy. <br/>
 * 
 * The pool implementation assumes that the one session per process instance strategy applied.
 * <p>
 * Since there are lot of database query/updating, the simple database tuning is create index for the sessionId and processInstanceId respectively (for example, on postgresql):
 * <ul>
 * <li>create index on sessionId: <code>[postgres]$ CREATE UNIQUE INDEX spx_sid_idx ON sessionprocessxref (sessionid);</code></li>
 * <li>create index on processInstanceId: <code>[postgres]$ CREATE UNIQUE INDEX spx_pinstid_idx ON sessionprocessxref (processinstanceid);</code></li>
 * </ul>
 * </p>
 * 
 * @author tanxu
 * @date Mar 5, 2012
 * @since
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JpaKnowledgeSessionPool implements IKnowledgeSessionPool {

    private static final Logger logger = LoggerFactory.getLogger(JpaKnowledgeSessionPool.class);

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_BUSY = 1;

    private EntityManagerFactory emf;
    private LinkedList<Integer> availableSessions = new LinkedList<Integer>();
    transient final ReentrantLock availableSessionsLock = new ReentrantLock();

    public JpaKnowledgeSessionPool() {
        InitialContext jndiContext = null;
        try {
            jndiContext = new InitialContext();
            emf = (EntityManagerFactory) jndiContext.lookup("java:/knowledgeSessionEMF");
        }
        catch (Exception e) {
            logger.error("lookup jbpm emf failed", e);
        }
        finally {
            if (jndiContext != null)
                try {
                    jndiContext.close();
                }
                catch (NamingException e) {
                    e.printStackTrace();
                }
        }

        populateAvailableSessions();
    }

    private void populateAvailableSessions() {
        EntityManager em = emf.createEntityManager();
        try {
            List<Integer> availableSessionIds = em
                    .createQuery("SELECT t.sessionId FROM SessionProcessXref t where t.status=:status")
                    .setParameter("status", STATUS_AVAILABLE).getResultList();
            availableSessions.addAll(availableSessionIds);
            logger.info("size of availableSessions after populate: {}", availableSessions.size());
        }
        finally {
            em.close();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Integer getAvailableSessionId() {
        int sessionId = -1;
        // query in memory for available sessionId rather than query from database
        // 1) to improve the performance
        // 2) to fix the optimistic issue of sessionInfo since it could return the same sessionId if query from database
        if (availableSessions.size() > 0) {
            availableSessionsLock.lock();
            try {
                sessionId = availableSessions.removeFirst();
            }
            finally {
                availableSessionsLock.unlock();
            }
        }

        return sessionId;
    }

    @Override
    public boolean isBorrowed(Integer sessionId, String processId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<SessionProcessXref> sessions = em
                    .createQuery("FROM SessionProcessXref where sessionId=:sessionId and processId=:processId")
                    .setParameter("sessionId", sessionId).setParameter("processId", processId).getResultList();
            return sessions.size() > 0;
        }
        finally {
            em.close();
        }
    }

    @Override
    public void markAsBorrowed(Integer sessionId, String processId) {
        EntityManager em = emf.createEntityManager();
        try {
            int rows = em.createQuery("UPDATE SessionProcessXref SET status=:status WHERE sessionId=:sessionId")
                    .setParameter("status", STATUS_BUSY).setParameter("sessionId", sessionId).executeUpdate();
            if (rows == 0) {
                SessionProcessXref xref = new SessionProcessXref();
                xref.setProcessId(processId);
                xref.setSessionId(sessionId);
                xref.setStatus(STATUS_BUSY);
                em.persist(xref);
            }
        }
        finally {
            em.close();
        }
    }

    @Override
    public void markAsReturned(Integer sessionId) {
        EntityManager em = emf.createEntityManager();
        try {
            int rows = em.createQuery("UPDATE SessionProcessXref SET status=:status WHERE sessionId=:sessionId")
                    .setParameter("status", STATUS_AVAILABLE).setParameter("sessionId", sessionId).executeUpdate();
            if (rows == 0) {
                logger.warn("Failed to return the ksession: sessionId [{}] not found!");
            }
            else {
                availableSessionsLock.lock();
                try {
                    availableSessions.addLast(new Integer(sessionId));
                }
                finally {
                    availableSessionsLock.unlock();
                }
            }
        }
        finally {
            em.close();
        }
    }

    @Override
    public String dumpSessionStatusInfo() {
        StringBuilder sBuilder = new StringBuilder(
                "dumpSessionStatusInfo()\nThe following is a list of sessions that are available for re-use by this knowledgeSessionService :\n");

        EntityManager em = emf.createEntityManager();
        try {
            for (Integer sessionId : availableSessions) {
                sBuilder.append(sessionId);
                sBuilder.append(" ");
            }

            List<Integer> busySessions = em
                    .createQuery("SELECT t.sessionId FROM SessionProcessXref t where t.status=:status")
                    .setParameter("status", STATUS_BUSY).getResultList();
            sBuilder.append("\n\nThe following is a list of sessions that are not yet available for re-use by this knowledgeSessionService :\n");
            for (Integer ksessionId : busySessions) {
                sBuilder.append(ksessionId);
                sBuilder.append(" ");
            }
        }
        finally {
            em.close();
        }

        return sBuilder.toString();
    }

    public void setProcessInstanceId(Integer ksessionId, Long pInstanceId) {
        EntityManager em = emf.createEntityManager();
        try {
            int rows = em
                    .createQuery(
                            "UPDATE SessionProcessXref xref set xref.processInstanceId=:processInstanceId where xref.sessionId=:ksessionId")
                    .setParameter("processInstanceId", pInstanceId).setParameter("ksessionId", ksessionId)
                    .executeUpdate();
            if (rows == 0) {
                logger.warn("Failed to udpate processInstanceId [{}]: sessionId [{}] not found", pInstanceId,
                        ksessionId);
            }
        }
        finally {
            em.close();
        }
    }

    public Integer getSessionId(Long pInstanceId) {
        EntityManager em = emf.createEntityManager();
        try {
            SessionProcessXref xref = (SessionProcessXref) em
                    .createQuery("SELECT xref FROM SessionProcessXref xref where xref.processInstanceId=:pInstanceId")
                    .setParameter("pInstanceId", pInstanceId).getSingleResult();
            return xref.getSessionId();
        }
        finally {
            em.close();
        }
    }
}
