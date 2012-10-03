package org.jboss.processFlow.console.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;

/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 14, 2012
 * @since
 */
public class AttachmentListResultBean {

    private String USERACTION;
    private List<AttachmentInfo> ATTACHMENTS = ListUtils.lazyList(new ArrayList(),
            FactoryUtils.instantiateFactory(AttachmentInfo.class));

    /**
     * @return the uSERACTION
     */
    public String getUSERACTION() {
        return USERACTION;
    }

    /**
     * @return the aTTACHMENTS
     */
    public List<AttachmentInfo> getATTACHMENTS() {
        return ATTACHMENTS;
    }

    /**
     * @param uSERACTION the uSERACTION to set
     */
    public void setUSERACTION(String uSERACTION) {
        USERACTION = uSERACTION;
    }

    /**
     * @param aTTACHMENTS the aTTACHMENTS to set
     */
    public void setATTACHMENTS(List<AttachmentInfo> aTTACHMENTS) {
        ATTACHMENTS = aTTACHMENTS;
    }

    @Override
    public String toString() {
        return "AttachmentListResultBean [USERACTION=" + USERACTION + ", ATTACHMENTS=" + ATTACHMENTS + "]";
    }

}
