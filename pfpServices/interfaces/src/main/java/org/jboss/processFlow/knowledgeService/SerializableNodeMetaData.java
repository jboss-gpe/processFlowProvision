package org.jboss.processFlow.knowledgeService;

import java.io.Serializable;

public class SerializableNodeMetaData implements Serializable {
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
