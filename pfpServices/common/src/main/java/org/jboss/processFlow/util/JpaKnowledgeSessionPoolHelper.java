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

package org.jboss.processFlow.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaKnowledgeSessionPoolHelper {

    protected static EntityManagerFactory emf;
    private static Object lockObj = new Object();
    private static final Logger logger = LoggerFactory.getLogger(JpaKnowledgeSessionPoolHelper.class);

    public JpaKnowledgeSessionPoolHelper() {
        if(emf == null) {
            synchronized(lockObj) {
                if(emf != null)
                    return;

                InitialContext jndiContext = null;
                try {
                    jndiContext = new InitialContext();
                    emf = (EntityManagerFactory) jndiContext.lookup("java:/app/knowledgeSessionEMF");
                } catch (Exception e) {
                    logger.error("lookup jbpm emf failed", e);
                } finally {
                    if (jndiContext != null) {
                        try {
                            jndiContext.close();
                        } catch (NamingException e) {}
                    }
                }
            }
        }
    }


    // TO-DO:  This needs to change to support reusablesubprocesses:  more than one processinstance can be associated with the same session at a time
    public void setProcessInstanceId(Integer ksessionId, Long pInstanceId) {
        EntityManager em = emf.createEntityManager();
        try {
            int rows = em
                    .createQuery("UPDATE SessionProcessXref xref set xref.processInstanceId=:processInstanceId where xref.sessionId=:ksessionId")
                    .setParameter("processInstanceId", pInstanceId).setParameter("ksessionId", ksessionId)
                    .executeUpdate();
            if (rows == 0) {
                logger.warn("setProcessInstanceId() Failed to udpate processInstanceId [{}];    sessionId [{}] not found", pInstanceId, ksessionId);
            }
        }
        finally {
            em.close();
        }
    }

}
