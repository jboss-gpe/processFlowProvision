package org.jboss.processFlow.rest;

public class FormAuthorityRef {
    public enum Type {
        TASK, PROCESS
    }

    private Type currentType = null;

    private String referenceId;

    public FormAuthorityRef(String referenceId) {
        this.referenceId = referenceId;
        this.currentType = Type.TASK;
    }

    public FormAuthorityRef(String referenceId, Type currentType) {
        this.currentType = currentType;
        this.referenceId = referenceId;
    }

    public Type getType() {
        return this.currentType;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
