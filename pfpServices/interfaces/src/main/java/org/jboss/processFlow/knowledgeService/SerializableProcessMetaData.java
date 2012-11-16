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

class SerializableNodeMetaData implements Serializable {
	public static final String X = "x";
	public static final String Y = "y";
	public static final String HEIGHT="height";
	public static final String WIDTH="width";
	
	public String getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public Integer getX() {
		return x;
	}
	public void setX(Integer x) {
		this.x = x;
	}
	public Integer getY() {
		return y;
	}
	public void setY(Integer y) {
		this.y = y;
	}
	public Integer getHeight() {
		return height;
	}
	public void setHeight(Integer height) {
		this.height = height;
	}
	public Integer getWidth() {
		return width;
	}
	public void setWidth(Integer width) {
		this.width = width;
	}
	private Integer x;
	private Integer y;
	private Integer height;
	private Integer width;
	private String uniqueId;
	
	public SerializableNodeMetaData(Integer x, Integer y, Integer height, Integer width, String uniqueId){
		this.x = x;
		this.y = y;
		this.height = y;
		this.width = width;
		this.uniqueId = uniqueId;
	}
}

