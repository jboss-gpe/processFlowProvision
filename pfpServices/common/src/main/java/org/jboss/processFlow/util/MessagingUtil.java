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

import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.UDPBroadcastGroupConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;

import org.apache.log4j.Logger;

public class MessagingUtil {

    public static final String CONNECTION_FACTORY_JNDI_NAME="java:/RemoteConnectionFactory";
    public static final String JBOSS_MESSAGING_GROUP_ADDRESS="jboss.messaging.group.address";
    public static final String JBOSS_MESSAGING_GROUP_PORT="jboss.messaging.group.port";
    public static final String IS_HORNETQ_INVM="org.jboss.processFlow.is.hornetq.inVm";

    private static Logger log = Logger.getLogger("MessagingUtil");
    private static boolean isHornetqInVm = true;
    private static String brokerHostName = null;
    private static String brokerPort = null;
    private static DiscoveryGroupConfiguration groupConfiguration = null;

    static{
        isHornetqInVm = Boolean.parseBoolean(System.getProperty(IS_HORNETQ_INVM, Boolean.TRUE.toString()));
        if(!isHornetqInVm) {
            brokerHostName = System.getProperty(JBOSS_MESSAGING_GROUP_ADDRESS);
            if(brokerHostName == null)
                throw new RuntimeException("grabJMSObject() system property not set : "+JBOSS_MESSAGING_GROUP_ADDRESS);
            brokerPort = System.getProperty(JBOSS_MESSAGING_GROUP_PORT);
            if(brokerPort == null)
                throw new RuntimeException("grabJMSObject() system property not set : "+JBOSS_MESSAGING_GROUP_PORT);

            UDPBroadcastGroupConfiguration udpCfg = new UDPBroadcastGroupConfiguration(brokerHostName, Integer.parseInt(brokerPort), null, -1);
            groupConfiguration = new DiscoveryGroupConfiguration(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT, HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT, udpCfg);
        }
    }
    
    // 22 Feb 2013
    // all lookups for ConnectionFactory (both in an OSE and non OSE environment should be to local JNDI
    public static ConnectionFactory grabConnectionFactory() throws Exception {
        Context jndiContext = null;
        try {
            if(!isHornetqInVm) {
                org.hornetq.jms.client.HornetQConnectionFactory hqcFactory = HornetQJMSClient.createConnectionFactoryWithHA(groupConfiguration, JMSFactoryType.QUEUE_CF);
                hqcFactory.setReconnectAttempts(-1);
                return (ConnectionFactory)hqcFactory;
            }else {
                jndiContext = new InitialContext();
                return (ConnectionFactory)jndiContext.lookup(CONNECTION_FACTORY_JNDI_NAME);
            }
        }finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

    public static Destination grabDestination(String jndiName) throws Exception {
        Context jndiContext = null;
        try {
            if(!isHornetqInVm) {
                Destination queue = HornetQJMSClient.createQueue(jndiName);
                return queue;
            }else {
                jndiContext = new InitialContext();
                return (Destination)jndiContext.lookup("jms/"+jndiName);
            }
        } catch(javax.naming.NamingException x){
            log.error("grabJMSObject() isHornetqInVm="+isHornetqInVm+" : remotingHostName="+brokerHostName+" : remotingPort="+brokerPort+" : jndiName="+jndiName);
            throw x;
        } finally {
            if(jndiContext != null)
                jndiContext.close();
        }
    }

}
