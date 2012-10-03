package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.TransformerUtils;
import org.jboss.processFlow.console.binding.DataBinderManager;
import org.jboss.processFlow.console.binding.MvelDataBinder;
import org.jboss.processFlow.console.task.AttachmentInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mvel2.MVEL;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 8, 2012
 * @since
 */
public class MvelDataBinderTest extends BaseDataBinderTest {

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(DataBinderManager.PROPERTY_DATA_BINDER, MvelDataBinder.class.getName());
    }

    @Test
    public void testBindList() {
        HashMap target = new HashMap();
        List attachmentList = ListUtils
                .lazyList(new ArrayList(), FactoryUtils.instantiateFactory(AttachmentInfo.class));
        target.put("ATTACHMENTS", attachmentList);

        MVEL.setProperty(target, "ATTACHMENTS[0].attachSequence", "1");
        MVEL.setProperty(target, "ATTACHMENTS[0].attachStatus", "var_status");
        MVEL.setProperty(target, "ATTACHMENTS[0].attachmentID", "var_attachmentID");
        MVEL.setProperty(target, "ATTACHMENTS[0].attachmentDate", new Date());

        List actualAttachmentList = (List) target.get("ATTACHMENTS");
        System.out.println("actualAttachmentList: " + actualAttachmentList);
        assertEquals(1, actualAttachmentList.size());
        AttachmentInfo actualAttachmentInfo = (AttachmentInfo) actualAttachmentList.get(0);
        assertEquals(1, actualAttachmentInfo.getAttachSequence());
        assertEquals("var_status", actualAttachmentInfo.getAttachStatus());
        assertEquals("var_attachmentID", actualAttachmentInfo.getAttachmentID());
    }

    @Test
    public void testBindMap() {
        HashMap target = new HashMap();
        target.put("fieldValues", MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer()));
        target.put("fieldTypes", MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer()));

        MVEL.setProperty(target, "fieldValues['IMAGE_UID']", "1");
        MVEL.setProperty(target, "fieldValues['SCANLOCATION']", "1");
        MVEL.setProperty(target, "fieldValues['BUSINESSPROCID']", 0);
        MVEL.setProperty(target, "fieldValues['MAINCASEID']", 18378);

        MVEL.setProperty(target, "fieldTypes['IMAGE_UID']", "string");
        MVEL.setProperty(target, "fieldTypes['SCANLOCATION']", "string");
        MVEL.setProperty(target, "fieldTypes['BUSINESSPROCID']", "integer");
        MVEL.setProperty(target, "fieldTypes['MAINCASEID']", "integer");

        System.out.println(target);

        Map fieldValues = (Map) target.get("fieldValues");
        assertNotNull(fieldValues);
        assertEquals(4, fieldValues.size());

        Map fieldTypes = (Map) target.get("fieldTypes");
        assertNotNull(fieldTypes);
        assertEquals(4, fieldTypes.size());

    }

}
