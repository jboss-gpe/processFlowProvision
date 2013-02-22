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

import org.apache.log4j.Logger;

public class MessagingUtil {

    public static final String CONNECTION_FACTORY_JNDI_NAME="java:/RemoteConnectionFactory";
    public static final String JBOSS_REMOTING_HOST_NAME="org.jboss.remoting.host.name";
    public static final String JBOSS_REMOTING_PORT="org.jboss.remoting.port";
    public static final String IS_HORNETQ_INVM="org.jboss.processFlow.is.hornetq.inVm";

    private static Logger log = Logger.getLogger("MessagingUtil");
    private static boolean isHornetqInVm = true;

    
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

    // 22 Feb 2013
    // until i can figure out how to bind to local jndi a reference to a remote destination, all lookups for destinations will be to that remote hornetq broker
    public static Object grabJMSObject(String jndiName) throws Exception {
        Context jndiContext = null;
        String jbossRemotingHostName = null;
        String jbossRemotingPort = null;
        try {
        	isHornetqInVm = Boolean.parseBoolean(System.getProperty(IS_HORNETQ_INVM, Boolean.TRUE.toString()));
            if(!isHornetqInVm) {
                jbossRemotingHostName = System.getProperty(JBOSS_REMOTING_HOST_NAME);
                if(jbossRemotingHostName == null)
                    throw new RuntimeException("grabJMSObject() system property not set : "+JBOSS_REMOTING_HOST_NAME);
                jbossRemotingPort = System.getProperty(JBOSS_REMOTING_PORT);
                if(jbossRemotingPort == null)
                    throw new RuntimeException("grabJMSObject() system property not set : "+JBOSS_REMOTING_PORT);

                Properties env = new Properties();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
                env.put(Context.PROVIDER_URL, "remote://"+jbossRemotingHostName+":"+jbossRemotingPort);
                jndiContext = new InitialContext(env);
            }else {
                jndiContext = new InitialContext();
            }
            return jndiContext.lookup(jndiName);
        } catch(javax.naming.NamingException x){
        	log.error("grabJMSObject() isHornetqInVm="+isHornetqInVm+" : remotingHostName="+jbossRemotingHostName+" : remotingPort="+jbossRemotingPort+" : jndiName="+jndiName);
        	throw x;
        } finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

}
