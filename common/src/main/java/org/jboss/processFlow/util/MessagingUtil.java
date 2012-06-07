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

import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.jms.ConnectionFactory;

public class MessagingUtil {

    public static final String HORNETQ="HORNETQ";
    public static final String MRG="MRG";

    /* attempts to lookup JMS ConnectionFactory from either :
        1)  jndi from remote Hornetq provider
        2)  local jndi as provided by jboss-qpid project for Red Hat Messaging
    */ 
    public static ConnectionFactory grabConnectionFactory() throws Exception {
        String cFactoryName = System.getProperty("org.jboss.processFlow.messaging.connectionFactory");
        if(cFactoryName == null)
            throw new RuntimeException("must set a value for system property:  org.jboss.processFlow.messaging.connectionFactory");

        return (ConnectionFactory)grabJMSObject(cFactoryName);
    }

    /* attempts to lookup a generic JMS object from either :
        1)  jndi from remote Hornetq provider
        2)  local jndi as provided by jboss-qpid project for Red Hat Messaging
    */ 
    public static Object grabJMSObject(String jndiName) throws Exception {
        Context jndiContext = null;
        try {
            // determine whether to use HornetQ or Red Hat Messaging/Qpid
            String htHandlerType = System.getProperty("org.jboss.processFlow.messagingProvider");
            if(htHandlerType == null)
                throw new RuntimeException("static() need to set system property : org.jboss.processFlow.messagingProvider");

            if(htHandlerType.equals(HORNETQ)) {
                // Grab reference to JMS Connection Factory via Remote HornetQ JNDI
                Properties jndiProps = new Properties();
                String pUrlString = "jnp://"+System.getProperty("hornetq_ip")+":1599";
                jndiProps.put(Context.PROVIDER_URL, pUrlString);
                jndiContext = new InitialContext(jndiProps);
            } else {
                // if using Red Hat Messaging ... then reference to AMQConnectionFactory will be in local JBoss JNDI
                jndiContext = new InitialContext();
            }
            return jndiContext.lookup(jndiName);
        } finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

}
