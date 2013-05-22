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
import javax.jms.Destination;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.jms.HornetQJMSClient;

import org.apache.log4j.Logger;

public class MessagingUtil {

    // using non-pooled / non-JCA connection factory (ie:  not using java:/JmsXA)
    public static final String CONNECTION_FACTORY_JNDI_NAME="java:/RemoteConnectionFactory";
    private static Logger log = Logger.getLogger("MessagingUtil");

    // 22 Feb 2013
    // all lookups for ConnectionFactory (both in an OSE and non OSE environment should be to local JNDI
    public static ConnectionFactory grabConnectionFactory() throws Exception {
        Context jndiContext = null;
        try {
            jndiContext = new InitialContext();
            return (ConnectionFactory)jndiContext.lookup(CONNECTION_FACTORY_JNDI_NAME);
        }finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

    public static Destination grabDestination(String jndiName) throws Exception {
        Context jndiContext = null;
        try {
            jndiContext = new InitialContext();
            return (Destination)jndiContext.lookup("jms/"+jndiName);
        } catch(javax.naming.NamingException x){
            /* 
                HornetQ JCA RA doesn't support any admin objects for binding HornetQ JMS destinations in the local JNDI namespace 
                    - (https://issues.jboss.org/browse/HORNETQ-908)
                However, can work around this by using the JMS API javax.jms.Session.createQueue(String)
                    - keep in mind that the String passed to createQueue will be the underlying HornetQ name of the destination, not the JNDI entry
            */
            Destination queue = HornetQJMSClient.createQueue(jndiName);
            return queue;
        } catch(Exception x) {
            throw x;
        } finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

}
