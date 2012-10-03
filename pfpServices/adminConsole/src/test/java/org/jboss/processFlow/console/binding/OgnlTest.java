package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.jboss.processFlow.console.task.AttachmentInfo;
import org.junit.Test;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 8, 2012
 * @since
 */
public class OgnlTest {

    @Test
    public void test() throws OgnlException {
        HashMap target = new HashMap();

        List<AttachmentInfo> attachmentList = ListUtils.lazyList(new ArrayList<AttachmentInfo>(), FactoryUtils.instantiateFactory(AttachmentInfo.class));
        target.put("attachmentList", attachmentList);

        HashMap values = new HashMap();
        values.put("attachmentList[0].attachSequence", "1");
        values.put("attachmentList[0].attachStatus", "var_status");
        values.put("attachmentList[0].attachmentID", "var_attachmentID");

        Ognl.setValue("attachmentList[0].attachSequence", values, target, "1");
        Ognl.setValue("attachmentList[0].attachStatus", values, target, "var_status");
        Ognl.setValue("attachmentList[0].attachmentID", values, target, "var_attachmentID");

        List actualAttachmentList = (List) target.get("attachmentList");
        System.out.println("actualAttachmentList: " + actualAttachmentList);
        assertEquals(1, actualAttachmentList.size());
        AttachmentInfo actualAttachmentInfo = (AttachmentInfo) actualAttachmentList.get(0);
        assertEquals(1, actualAttachmentInfo.getAttachSequence());
        assertEquals("var_status", actualAttachmentInfo.getAttachStatus());
        assertEquals("var_attachmentID", actualAttachmentInfo.getAttachmentID());
    }

  
}
