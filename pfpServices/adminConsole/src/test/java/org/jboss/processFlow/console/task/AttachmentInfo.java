package org.jboss.processFlow.console.task;

import java.io.Serializable;

public class AttachmentInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private long attachSequence;
    private String attachStatus;
    private String attachmentID;

    private String attachmentType;
//    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private String attachmentDate;
    private String docTypeID;

    public long getAttachSequence() {
        return attachSequence;
    }

    public void setAttachSequence(long attachSequence) {
        this.attachSequence = attachSequence;
    }

    public String getAttachStatus() {
        return attachStatus;
    }

    public void setAttachStatus(String attachStatus) {
        this.attachStatus = attachStatus;
    }

    public String getAttachmentID() {
        return attachmentID;
    }

    public void setAttachmentID(String attachmentID) {
        this.attachmentID = attachmentID;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentDate() {
        return attachmentDate;
    }

    public void setAttachmentDate(String attachmentDate) {
        this.attachmentDate = attachmentDate;
    }

    public String getDocTypeID() {
        return docTypeID;
    }

    public void setDocTypeID(String docTypeID) {
        this.docTypeID = docTypeID;
    }

    @Override
    public String toString() {
        return "AttachmentInfo [attachSequence=" + attachSequence + ", attachStatus=" + attachStatus
                + ", attachmentID=" + attachmentID + ", attachmentType=" + attachmentType + ", attachmentDate="
                + attachmentDate + ", docTypeID=" + docTypeID + "]";
    }

}
