package org.jboss.processFlow.console.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.TransformerUtils;
import org.jboss.processFlow.console.binding.DataBinderManager;
import org.jboss.processFlow.console.binding.spring.SpringDataBinder;
import org.jboss.processFlow.console.task.AttachmentInfo;
import org.jboss.processFlow.console.task.AttachmentListResultBean;
import org.jboss.processFlow.console.task.KeyValuesResultBean;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.validation.DataBinder;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 14, 2012
 * @since
 */
public class SpringDataBinderTest extends BaseDataBinderTest {

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(DataBinderManager.PROPERTY_DATA_BINDER, SpringDataBinder.class.getName());
    }

    @Test
    public void testBindListWithGenericType() throws Exception {
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("USERACTION", "RELEASE");
        values.addPropertyValue("ATTACHMENTS[0].attachSequence", "1");
        values.addPropertyValue("ATTACHMENTS[0].attachStatus", "var_status");
        values.addPropertyValue("ATTACHMENTS[0].attachmentID", "var_attachmentID");
        values.addPropertyValue("ATTACHMENTS[0].attachmentDate", "15/02/2012");

        AttachmentListResultBean target = new AttachmentListResultBean();
        DataBinder binder = new DataBinder(target);

        // set the conversion service to convert the date
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
        binder.setConversionService(conversionService);

        binder.bind(values);

        System.out.println(target);

        assertEquals("RELEASE", target.getUSERACTION());

        List attachmentList = target.getATTACHMENTS();
        assertNotNull(attachmentList);
        assertEquals(1, attachmentList.size());

        AttachmentInfo info = (AttachmentInfo) attachmentList.get(0);
        assertEquals("var_attachmentID", info.getAttachmentID());
        assertEquals("var_status", info.getAttachStatus());
        assertEquals(1L, info.getAttachSequence());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        assertEquals("15/02/2012", dateFormat.format(info.getAttachmentDate()));
    }

    @Test
    public void testBindMapNestedList() throws Exception {
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("resultsMap['USERACTION']", "RELEASE");
        values.addPropertyValue("resultsMap['ATTACHMENTS'][0].attachSequence", "1");
        values.addPropertyValue("resultsMap['ATTACHMENTS'][0].attachStatus", "var_status");
        values.addPropertyValue("resultsMap['ATTACHMENTS'][0].attachmentID", "var_attachmentID");
        values.addPropertyValue("resultsMap['ATTACHMENTS'][0].attachmentDate", "15/02/2012");

        org.jboss.processFlow.console.binding.spring.InternalMapBean target = new org.jboss.processFlow.console.binding.spring.InternalMapBean();
        HashMap<String, Object> sourceMap = new HashMap<String, Object>();
        sourceMap.put("MAINCASEID", null);
        sourceMap.put("USERACTION", "INIT");
        sourceMap.put("ATTACHMENTS",
                ListUtils.lazyList(new ArrayList(), FactoryUtils.instantiateFactory(AttachmentInfo.class)));
        target.setResultsMap(sourceMap);

        DataBinder binder = new DataBinder(target);

        // set the conversion service to convert the date
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
        binder.setConversionService(conversionService);

        binder.bind(values);

        System.out.println(target);

        assertEquals("RELEASE", sourceMap.get("USERACTION"));

        List attachmentList = (List) sourceMap.get("ATTACHMENTS");
        assertNotNull(attachmentList);
        assertEquals(1, attachmentList.size());

        AttachmentInfo info = (AttachmentInfo) attachmentList.get(0);
        assertEquals("var_attachmentID", info.getAttachmentID());
        assertEquals("var_status", info.getAttachStatus());
        assertEquals(1L, info.getAttachSequence());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        assertEquals("15/02/2012", dateFormat.format(info.getAttachmentDate()));
    }

    @Test
    public void testBindMapNestedMap() throws Exception {
        MutablePropertyValues values = new MutablePropertyValues();

        values.addPropertyValue("resultsMap['fieldValues']['IMAGE_UID']", "1");
        values.addPropertyValue("resultsMap['fieldValues']['SCANLOCATION']", "1");
        values.addPropertyValue("resultsMap['fieldValues']['BUSINESSPROCID']", 0);
        values.addPropertyValue("resultsMap['fieldValues']['MAINCASEID']", 18378);

        values.addPropertyValue("resultsMap['fieldTypes']['IMAGE_UID']", "string");
        values.addPropertyValue("resultsMap['fieldTypes']['SCANLOCATION']", "string");
        values.addPropertyValue("resultsMap['fieldTypes']['BUSINESSPROCID']", "integer");
        values.addPropertyValue("resultsMap['fieldTypes']['MAINCASEID']", "integer");

        org.jboss.processFlow.console.binding.spring.InternalMapBean target = new org.jboss.processFlow.console.binding.spring.InternalMapBean();
        HashMap<String, Object> sourceMap = new HashMap<String, Object>();
        sourceMap.put("fieldValues", MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer()));
        sourceMap.put("fieldTypes", MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer()));
        target.setResultsMap(sourceMap);

        DataBinder binder = new DataBinder(target);

        // set the conversion service to convert the date
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
        binder.setConversionService(conversionService);

        binder.bind(values);
        System.out.println(target);

        Map fieldValues = (Map) sourceMap.get("fieldValues");
        assertNotNull(fieldValues);
        assertEquals(4, fieldValues.size());

        Map fieldTypes = (Map) sourceMap.get("fieldTypes");
        assertNotNull(fieldTypes);
        assertEquals(4, fieldTypes.size());
    }

    @Test
    public void testBindMap() throws Exception {
        MutablePropertyValues values = new MutablePropertyValues();
        // navigate map with dot
        values.addPropertyValue("fieldValues.IMAGE_UID", "1");
        values.addPropertyValue("fieldValues.SCANLOCATION", "1");
        values.addPropertyValue("fieldValues.BUSINESSPROCID", 0);
        values.addPropertyValue("fieldValues.MAINCASEID", 18378);

        // navigate map with bracket
        values.addPropertyValue("fieldTypes['IMAGE_UID']", "string");
        values.addPropertyValue("fieldTypes['SCANLOCATION']", "string");
        values.addPropertyValue("fieldTypes['BUSINESSPROCID']", "integer");
        values.addPropertyValue("fieldTypes['MAINCASEID']", "integer");

        KeyValuesResultBean target = new KeyValuesResultBean();
        DataBinder binder = new DataBinder(target);

        // set the conversion service to convert the date
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
        binder.setConversionService(conversionService);

        binder.bind(values);

        System.out.println(target);
    }
}
