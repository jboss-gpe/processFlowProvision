package org.jboss.processFlow.rest;

public class ParticipantRef {
    private String type;
    private String idRef;
    private boolean isGroup;

    public ParticipantRef() {
    }

    public ParticipantRef(String type, String idRef) {
        this.type = type;
        this.idRef = idRef;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdRef() {
        return idRef;
    }

    public void setIdRef(String idRef) {
        this.idRef = idRef;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
