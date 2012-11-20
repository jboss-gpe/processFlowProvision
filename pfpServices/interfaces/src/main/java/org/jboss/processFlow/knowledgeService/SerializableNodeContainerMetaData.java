package org.jboss.processFlow.knowledgeService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SerializableNodeContainerMetaData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    Map<Long, SerializableNodeMetaData> nodeMap = new HashMap<Long, SerializableNodeMetaData>();
    
//    SerializableNodeMetaData[] getNodes(){
//        nodeMap.entrySet();    
//    }

    SerializableNodeMetaData getNode(long id){
        return nodeMap.get(id);
    }
}
