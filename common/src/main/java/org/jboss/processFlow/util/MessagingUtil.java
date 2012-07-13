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

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.jms.ConnectionFactory;

import org.apache.log4j.Logger;

public class MessagingUtil {

    private static Logger log = Logger.getLogger("MessagingUtil");

    public static ConnectionFactory grabConnectionFactory() throws Exception {
        String cFactoryName = System.getProperty("org.jboss.processFlow.messaging.connectionFactory");
        if(cFactoryName == null) {
            log.warn("system property not set :  org.jboss.processFlow.messaging.connectionFactory ... will set to default:  ConnectionFactory");
            cFactoryName="ConnectionFactory";
        }

        return (ConnectionFactory)grabJMSObject(cFactoryName);
    }

    public static Object grabJMSObject(String jndiName) throws Exception {
        Context jndiContext = null;
        try {
                jndiContext = new InitialContext();
                return jndiContext.lookup(jndiName);
        } finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

}
