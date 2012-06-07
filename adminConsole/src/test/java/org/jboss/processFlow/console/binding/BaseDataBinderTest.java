package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.processFlow.console.binding.DataBinderManager;
import org.jboss.processFlow.console.binding.IDataBinder;
import org.jboss.processFlow.console.task.AttachmentInfo;
import org.junit.Test;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 18, 2012
 * @since
 */
public class BaseDataBinderTest {

    @Test
    public void testDataBindingTask() throws Exception {
        DataBinderManager mgr = new DataBinderManager();
        IDataBinder binder = mgr.getDataBinder("DataBindingTask");
        assertNotNull(binder);

        HashMap values = new HashMap();
        values.put("USERACTION", "RELEASE");
        values.put("BUSINESSPROCID", 1); // which not privided in the bean factory
        values.put("ATTACHMENTS[0].attachSequence", "1");
        values.put("ATTACHMENTS[0].attachStatus", "var_status");
        values.put("ATTACHMENTS[0].attachmentID", "var_attachmentID");
        values.put("FIELDVALUES[0].key", "testkey1");
        values.put("FIELDVALUES[0].value", "testvalue1");
        values.put("FIELDVALUES[1].key", "testkey2");
        values.put("FIELDVALUES[1].value", "testvalue2");

        binder.bind(values);

        assertEquals("RELEASE", values.get("USERACTION"));
        // BUSINESSPROCID is not provided by the bean factory, but should keep as well
        assertEquals(1, values.get("BUSINESSPROCID"));

        List attachmentList = (List) values.get("ATTACHMENTS");
        System.out.println("ATTACHMENTS: " + attachmentList);
        assertNotNull(attachmentList);
        assertEquals(1, attachmentList.size());

        AttachmentInfo info = (AttachmentInfo) attachmentList.get(0);
        assertEquals("var_attachmentID", info.getAttachmentID());
        assertEquals("var_status", info.getAttachStatus());
        assertEquals(1L, info.getAttachSequence());

        Map fieldValueMap = (Map) values.get("FIELDVALUES");
        System.out.println("FIELDVALUES: " + fieldValueMap);
        assertNotNull(fieldValueMap);
        assertEquals(2, fieldValueMap.size());
    }

    @Test
    public void testUa0001() throws Exception {
        DataBinderManager mgr = new DataBinderManager();
        IDataBinder binder = mgr.getDataBinder("UA0001");
        assertNotNull(binder);

        HashMap values = new HashMap();
        values.put("USERACTION", "RELEASE");
        values.put("BUSINESSPROCID", 1); // which not privided in the bean factory
        values.put("ATTACHMENTS[0].attachSequence", "1");
        values.put("ATTACHMENTS[0].attachStatus", "var_status");
        values.put("ATTACHMENTS[0].attachmentID", "var_attachmentID");
        // values.put("ATTACHMENTS[0].attachmentDate", "15/02/2012");

        binder.bind(values);
        System.out.println(values);

        assertEquals("RELEASE", values.get("USERACTION"));
        // BUSINESSPROCID is not provided by the bean factory, but should keep as well
        assertEquals(1, values.get("BUSINESSPROCID"));

        List attachmentList = (List) values.get("ATTACHMENTS");
        assertNotNull(attachmentList);
        assertEquals(1, attachmentList.size());

        AttachmentInfo info = (AttachmentInfo) attachmentList.get(0);
        assertEquals("var_attachmentID", info.getAttachmentID());
        assertEquals("var_status", info.getAttachStatus());
        assertEquals(1L, info.getAttachSequence());
    }

    @Test
    public void testErr001() throws Exception {
        DataBinderManager mgr = new DataBinderManager();
        IDataBinder binder = mgr.getDataBinder("ERR001");
        assertNotNull(binder);

        HashMap values = new HashMap();

        values.put("fieldValues['IMAGE_UID']", "1");
        values.put("fieldValues['SCANLOCATION']", "1");
        values.put("fieldValues['BUSINESSPROCID']", 0);
        values.put("fieldValues['MAINCASEID']", 18378);

        values.put("fieldTypes['IMAGE_UID']", "string");
        values.put("fieldTypes['SCANLOCATION']", "string");
        values.put("fieldTypes['BUSINESSPROCID']", "integer");
        values.put("fieldTypes['MAINCASEID']", "integer");

        binder.bind(values);
        System.out.println(values);

        Map fieldValues = (Map) values.get("fieldValues");
        assertNotNull(fieldValues);
        assertEquals(4, fieldValues.size());

        Map fieldTypes = (Map) values.get("fieldTypes");
        assertNotNull(fieldTypes);
        assertEquals(4, fieldTypes.size());
    }

}
