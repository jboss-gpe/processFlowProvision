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

package org.jboss.processFlow.console.forms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;

import org.drools.SystemEventListenerFactory;
import org.jboss.bpm.console.server.plugin.FormAuthorityRef;
import org.jbpm.task.Content;
import org.jbpm.task.I18NText;
import org.jbpm.task.Task;
import org.jbpm.integration.console.forms.AbstractFormDispatcher;

import javax.management.InstanceNotFoundException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;

import org.jboss.processFlow.tasks.ITaskService;

/**
 * @author Kris Verlaenen

JA Bride :  modified to invoke PFP's TaskService
 */
public class TaskFormDispatcher extends AbstractFormDispatcher {

    private static ITaskService taskServiceProxy = null;
    private static Logger log = Logger.getLogger("TackFormDispatcher");

    static {
        Context jndiContext = null;
        try {
            Properties jndiProps = new Properties();
            jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

            jndiContext = new InitialContext(jndiProps);
            taskServiceProxy = (ITaskService)jndiContext.lookup(ITaskService.TASK_SERVICE_JNDI);
        } catch(Throwable x) {
            throw new RuntimeException(x);
        } finally {
            try {
                if(jndiContext != null)
                    jndiContext.close();
            } catch(Exception x) {
                x.printStackTrace();
            }
        }
    }

    public DataHandler provideForm(FormAuthorityRef ref) {
        String englishTaskName = taskServiceProxy.getTaskName(new Long(ref.getReferenceId()), "en-UK");

        InputStream template = getTemplate(englishTaskName);
        if (template == null)
            throw new  RuntimeException("provideForm() unable to locate FreeMarker Template with name = "+englishTaskName);

        // get processInstance --> task variable Map
        Map<?,?> documentContentMap = taskServiceProxy.getTaskContent(new Long(ref.getReferenceId()), true);

        Map<String, Object> renderContext = null; 
        //renderContext.put("content", documentContentMap);
        if(documentContentMap.size() != 0) {
            renderContext = new HashMap<String, Object>();
            for (Map.Entry<?, ?> entry: documentContentMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    renderContext.put((String) entry.getKey(), entry.getValue());
                }else {
                    log.warn("provideForm() processInstance --> task variables includes a non-String variable .. will skip : "+entry.getKey()+" : "+entry.getValue());
                }
            }
        } else {
                log.warn("provideForm() processInstance --> task variables map is empty and will be passed a null rootMap !!!");
        }

            // merge template with process variables
            try {
                return processTemplate(englishTaskName, template, renderContext);
            } catch(RuntimeException x) {
                log.error("provideForm() the following are the contents of the processInstance --> task variables Map being used to create a FreeMarker template :");
                if(renderContext != null && renderContext.size() > 0) {
                    for(Map.Entry<String, Object> entry : renderContext.entrySet()) {
                        log.error("\tkey = "+entry.getKey()+" : value = "+entry.getValue());
                    }
                } else {
                    log.error("provideForm() processInstance --> task variables map is empty and was passed a null rootMap !!!");
                }
                throw x;
            }
    }
}
