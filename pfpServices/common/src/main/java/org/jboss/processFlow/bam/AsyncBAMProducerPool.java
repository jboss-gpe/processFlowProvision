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

package org.jboss.processFlow.bam;

import javax.jms.*;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import org.jboss.processFlow.util.MessagingUtil;

public final class AsyncBAMProducerPool implements PoolableObjectFactory {

    private static Logger log = Logger.getLogger("AsyncBAMProducerPool");
    private static AsyncBAMProducerPool singleton = null;
    private static Connection connectObj;
    private static Destination dQueue;

    private GenericObjectPool producerPool;

    private AsyncBAMProducerPool() {
    }

    public synchronized static AsyncBAMProducerPool getInstance() {
        if(singleton == null) {
            // 0)  grab JMS objects
            try {
                ConnectionFactory cFactory = MessagingUtil.grabConnectionFactory();
                connectObj = cFactory.createConnection();
                dQueue = (Destination)MessagingUtil.grabJMSObject(IBAMService.BAM_QUEUE);
            } catch(Exception x) {
                throw new RuntimeException(x);
            }

            // 1)  create singleton of this class 
            singleton = new AsyncBAMProducerPool();

            // 2)  create a FIFO pool to manage 'PoolWrapper' objects
            GenericObjectPool producerPool = new GenericObjectPool(singleton);

            // 3)  populate with AsyncBAMProducerPool
            singleton.setProducerPool(producerPool);

            // 4)  set settings on pool
            int poolMaxIdle = 10;
            if(System.getProperty("org.jboss.processFlow.bam.AsyncBAMProducerPool.poolMaxIdle") != null)
                poolMaxIdle = Integer.parseInt(System.getProperty("org.jboss.processFlow.bam.AsyncBAMProducerPool.poolMaxIdle"));
            producerPool.setMaxIdle(poolMaxIdle);
            producerPool.setMaxActive(-1);
            producerPool.setLifo(false);
            log.info("getInstance() just created AsyncBAMProducerPool ");
        }
        return singleton;
    }

    private void setProducerPool(GenericObjectPool x) {
        this.producerPool = x;
    }

    public BAMProducerWrapper borrowObject() throws Exception {
        return (BAMProducerWrapper)producerPool.borrowObject();
    }

    public void returnObject(BAMProducerWrapper pWrapper) throws Exception {
        producerPool.returnObject(pWrapper);
    }

    public void close() throws Exception {
        log.info("close() ");
        if(producerPool != null)
            producerPool.close();
        producerPool = null;

        if(connectObj != null)
            connectObj.close();

        singleton = null;
    }

    public int getNumActive() {
        return producerPool.getNumActive();
    }

    public int getNumIdle() {
        return producerPool.getNumIdle();
    }

    //Instances returned from this method should be in the same state as if they had been activated
    //They will not be activated before being served by the pool
    public Object makeObject() throws JMSException {
        log.info("makeObject() ");
        Session pSession = connectObj.createSession(true, Session.AUTO_ACKNOWLEDGE);
        return new BAMProducerWrapper(pSession, dQueue);
    }

    // (re)initialize a instance from the pool
    // this method is invoked directly after makeObject() or when previously passivated object is being again borrowed from the pool
    public void activateObject(Object obj) throws Exception {
        //log.info("passivateObject() ");
    }

    public void passivateObject(Object obj) throws Exception {
        //log.info("passivateObject() ");
        Session pSession = ((BAMProducerWrapper)obj).getSession();
        pSession.commit();
    }
 
     // Ensures that the instance is safe to be returned by the pool. Returns false if obj should be destroyed
     public boolean validateObject(Object obj) {
         log.info("destroyObject() ");
         return true;
     }

    // invoked on every instance when it is being 'dropped' from the pool
     public void destroyObject(Object obj) throws Exception{
         log.info("destroyObject() ");
     }
}

class BAMProducerWrapper {
    Session sessionObj;
    MessageProducer producerObj;

    BAMProducerWrapper(Session sessionObj, Destination dQueue) throws JMSException {
        this.sessionObj = sessionObj;
        this.producerObj = sessionObj.createProducer(dQueue);
        this.producerObj.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public Session getSession() {
        return sessionObj;
    }
    public MessageProducer getProducer() {
        return producerObj;
    }

    public void destroy() throws JMSException {
        producerObj.close();
        sessionObj.close();
    }
}
