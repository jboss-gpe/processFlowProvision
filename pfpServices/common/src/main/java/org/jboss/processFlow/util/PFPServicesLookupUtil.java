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

import org.jboss.processFlow.tasks.ITaskService;
import org.jboss.processFlow.bam.IBAMService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;

public class PFPServicesLookupUtil {

    private static Logger log = Logger.getLogger("PFPServicesLookupUtil");
    private static Object lockObj = new Object();

    private static IKnowledgeSessionService kSessionProxy = null;
    private static IBAMService bamProxy = null;
    private static ITaskService taskProxy = null;

    public static IKnowledgeSessionService getKSessionProxy() {
        if(kSessionProxy == null)
            init();
        return kSessionProxy;
    }
    public static ITaskService getTaskProxy() {
        if(taskProxy == null)
            init();
        return taskProxy;
    }
    public static IBAMService getBamProxy() {
        if(bamProxy == null)
            init();
        return bamProxy;
    }

    private static void init() {
        if(taskProxy == null){
            synchronized(lockObj){
                if(taskProxy != null)
                    return;

                Context jndiContext = null;
                boolean colocatedPfpServices = true;
                try {

                    Properties jndiProps = new Properties();
                    if(System.getProperty("org.jboss.processFlow.colocated.pfp.services") != null)
                       colocatedPfpServices = Boolean.parseBoolean(System.getProperty("org.jboss.processFlow.colocated.pfp.services"));
                    if(!colocatedPfpServices){
                        jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
                        log.info("init() pfpServices are not colocated in same JVM.  will set following jndi prop to org.jboss.ejb.client.naming : "+Context.URL_PKG_PREFIXES);
                    } else {
                        log.info("init() pfpServices colocated in same JVM.  no additional naming configs needed");
                    }

                    jndiContext = new InitialContext(jndiProps);
                    taskProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
                    kSessionProxy = (IKnowledgeSessionService)jndiContext.lookup((IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI));
                    bamProxy = (IBAMService)jndiContext.lookup(IBAMService.BAM_SERVICE_JNDI);

                } catch(Exception x) {
                        throw new RuntimeException(x);
                }finally {
                    try {
                        if(jndiContext != null)
                            jndiContext.close();
                    }catch(Exception x){
                        x.printStackTrace();
                    }
                }
            }
        }
    }
                    

}
