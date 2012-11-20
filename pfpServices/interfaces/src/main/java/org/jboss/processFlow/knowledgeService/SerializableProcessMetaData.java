package org.jboss.processFlow.knowledgeService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SerializableProcessMetaData implements Serializable{
    private static final long serialVersionUID = 1L;
    public SerializableProcessMetaData(String processId, String processName, Long processVersion, String packageName){
        this.packageName = packageName;
        this.processId = processId;
        this.processName = processName;
        this.processVersion = processVersion;
    }
    
    public String getProcessId() {
        return processId;
    }
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    public String getProcessName() {
        return processName;
    }
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    public Long getProcessVersion() {
        return processVersion;
    }
    public void setProcessVersion(Long processVersion) {
        this.processVersion = processVersion;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public List<SerializableNodeMetaData> getNodes() {
        return nodes;
    }

    public void setNodes(List<SerializableNodeMetaData> nodes) {
        this.nodes = nodes;
    }
    private String processId;
    private String processName;
    private Long processVersion;
    private String packageName;
    private List<SerializableNodeMetaData> nodes = new ArrayList();
}

