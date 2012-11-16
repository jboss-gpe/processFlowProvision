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

/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.processFlow.console.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.definition.process.Node;
import org.drools.definition.process.NodeContainer;
import org.drools.definition.process.Process;
import org.drools.definition.process.WorkflowProcess;
import org.jboss.bpm.console.client.model.ActiveNodeInfo;
import org.jboss.bpm.console.client.model.DiagramInfo;
import org.jboss.bpm.console.client.model.DiagramNodeInfo;
import org.jboss.processFlow.bam.IBAMService;
import org.jboss.processFlow.knowledgeService.IKnowledgeSessionService;
import org.jboss.processFlow.knowledgeService.SerializableNodeMetaData;
import org.jboss.processFlow.knowledgeService.SerializableProcessMetaData;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;

/**
 * @author Kris Verlaenen

    JA Bride :  modified until base jbpm5 stops using hibernate.cfg.xml :  which does not leverage our JCA DataSource pools
 */
public class GraphViewerPluginImpl extends org.jbpm.integration.console.graph.GraphViewerPluginImpl {

    private static final int BUFFER_SIZE = 512;
    private static IKnowledgeSessionService ksessionProxy = null;
    private static IBAMService bamProxy = null;
    
    static {
        Context ksessionContext = null;
        Context bamContext = null;
        try {
            Properties jndiProps = new Properties();
            jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            ksessionContext = new InitialContext(jndiProps);
            ksessionProxy = (IKnowledgeSessionService)ksessionContext.lookup(IKnowledgeSessionService.KNOWLEDGE_SESSION_SERVICE_JNDI);

            bamContext = new InitialContext(jndiProps);
            bamProxy = (IBAMService)bamContext.lookup(IBAMService.BAM_SERVICE_JNDI);
        } catch(Exception x) {
            throw new RuntimeException(x);
        } finally {
            try {
                if(ksessionContext != null)
                    ksessionContext.close();
                if(bamContext != null)
                    bamContext.close();
            } catch(Exception y) {
                throw new RuntimeException(y);
            }
        }
    }

    public List<ActiveNodeInfo> getActiveNodeInfo(String processInstanceId) {
        ProcessInstanceLog processInstance = bamProxy.getProcessInstanceLog(Long.valueOf(processInstanceId));
        if (processInstance == null) 
            throw new IllegalArgumentException("Could not find process instance with id = " + processInstanceId);

        Map<String, NodeInstanceLog> nodeInstances = new HashMap<String, NodeInstanceLog>();
        for (NodeInstanceLog nodeInstance: bamProxy.findNodeInstances(Long.valueOf(processInstanceId))) {
            if (nodeInstance.getType() == NodeInstanceLog.TYPE_ENTER) {
                nodeInstances.put(nodeInstance.getNodeInstanceId(), nodeInstance);
            } else {
                nodeInstances.remove(nodeInstance.getNodeInstanceId());
            }
        }
        
        if (!nodeInstances.isEmpty()) {
            List<ActiveNodeInfo> result = new ArrayList<ActiveNodeInfo>();
            for (NodeInstanceLog nodeInstance: nodeInstances.values()) {
                String nodeId = nodeInstance.getNodeId();
                boolean found = false;
                DiagramInfo diagramInfo = getDiagramInfo(processInstance.getProcessId());
                if(diagramInfo != null) {
                    for (DiagramNodeInfo nodeInfo: diagramInfo.getNodeList()) {
                        if (nodeInfo.getName().equals(nodeId)) {
                            result.add(new ActiveNodeInfo(diagramInfo.getWidth(), diagramInfo.getHeight(), nodeInfo));
                            found = true;
                            break;
                        }
                    }
                }else {
                    throw new IllegalArgumentException("Could not find diagramInfo for "+processInstance.getProcessId());
                }
                if (!found) {
                	throw new IllegalArgumentException("Could not find diagram info for: "+ nodeInstance.getNodeName() + " nodeId = "+nodeId+" : process " + processInstance.getProcessId());
                }
            }
            return result;
        }
        return null;
    }

    public DiagramInfo getDiagramInfo(String processId) {
        SerializableProcessMetaData processMeta = ksessionProxy.getProcess(processId);
        if (processMeta == null) {
            return null;
        }

        DiagramInfo result = new DiagramInfo();
        // TODO: diagram width and height?
        result.setWidth(932);
        result.setHeight(541);
        List<DiagramNodeInfo> nodeList = new ArrayList<DiagramNodeInfo>();
        addNodesInfo(nodeList, processMeta, "id=");
        result.setNodeList(nodeList);
        return result;
    }
    
    private void addNodesInfo(List<DiagramNodeInfo> nodeInfos, SerializableProcessMetaData processMeta, String prefix) {
        for (SerializableNodeMetaData node: processMeta.getNodes()) {
            nodeInfos.add(new DiagramNodeInfo(node.getUniqueId(),node.getX(),node.getY(),node.getWidth(),node.getHeight()));
            //if (node instanceof NodeContainer) {
            //   addNodesInfo(nodeInfos, ((NodeContainer) node).getNodes(), prefix + node.getId() + ":");
            //}
        }
    }
}
