package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import org.jboss.processFlow.console.task.AttachmentInfo;
import org.junit.Test;


/**
 * DOCME
 * TODO once the databinding implemented, need handle the code injection
 * TODO binding date type?
 * TODO how to dynamic insert into list?
 * 
 * @author tanxu
 * @date Jan 27, 2012
 * @since
 */
public class OgnlDataBindingTest {

    @Test
    public void testBindSimple() throws Exception {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        HashMap values = new HashMap();
        values.put("attachSequence", "1");
        values.put("attachStatus", "var_status");
        values.put("attachmentID", "var_attachmentID");
        OgnlContext ognlCtx = new OgnlContext(values);

        setValue("attachSequence", ognlCtx, attachmentInfo);
        setValue("attachStatus", ognlCtx, attachmentInfo);
        setValue("attachmentID", ognlCtx, attachmentInfo);

        System.out.println("attachmentInfo: " + attachmentInfo);
        assertEquals(1, attachmentInfo.getAttachSequence());
        assertEquals("var_status", attachmentInfo.getAttachStatus());
        assertEquals("var_attachmentID", attachmentInfo.getAttachmentID());
    }

    @Test
    public void testBindMap() throws Exception {
        HashMap target = new HashMap();
        ArrayList attachmentList = new ArrayList();
        attachmentList.add(new AttachmentInfo());

        target.put("attachmentList", attachmentList);

        HashMap values = new HashMap();
        values.put("attachmentList[0].attachSequence", "1");
        values.put("attachmentList[0].attachStatus", "var_status");
        values.put("attachmentList[0].attachmentID", "var_attachmentID");
        OgnlContext ognlCtx = new OgnlContext(values);

        setValue("attachmentList[0].attachSequence", ognlCtx, target);
        setValue("attachmentList[0].attachStatus", ognlCtx, target);
        setValue("attachmentList[0].attachmentID", ognlCtx, target);

        List actualAttachmentList = (List) target.get("attachmentList");
        System.out.println("actualAttachmentList: " + actualAttachmentList);
        assertEquals(1, actualAttachmentList.size());
        AttachmentInfo actualAttachmentInfo = (AttachmentInfo) actualAttachmentList.get(0);
        assertEquals(1, actualAttachmentInfo.getAttachSequence());
        assertEquals("var_status", actualAttachmentInfo.getAttachStatus());
        assertEquals("var_attachmentID", actualAttachmentInfo.getAttachmentID());
    }

    public void setValue(String expression, OgnlContext context, Object rootObject) throws OgnlException {
        Ognl.setValue(expression, context, rootObject, context.get(expression));
    }

}
