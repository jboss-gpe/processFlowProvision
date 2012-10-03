package org.jboss.processFlow.console.binding;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.jboss.processFlow.console.task.AttachmentInfo;
import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;


/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 9, 2012
 * @since
 */
public class SpelTest {

    @Test
    public void test() {
        HashMap rootObject = new HashMap();
        List<AttachmentInfo> attachmentList = ListUtils.lazyList(new ArrayList<AttachmentInfo>(), FactoryUtils.instantiateFactory(AttachmentInfo.class));
        rootObject.put("attachmentList", attachmentList);

        EvaluationContext context = new StandardEvaluationContext(rootObject);
        ExpressionParser parser = new SpelExpressionParser();

        List list = parser.parseExpression("['attachmentList']").getValue(context, List.class);
        System.out.println("['attachmentList'] ==> " + list);
        assertNotNull(list);
        list = parser.parseExpression("#root['attachmentList']").getValue(context, List.class);
        System.out.println("#root['attachmentList'] ==> " + list);
        assertNotNull(list);
        list = parser.parseExpression("#root.?[key=='attachmentList']").getValue(context, List.class);
        System.out.println("#root.?[key=='attachmentList'] ==> " + list);
        assertNotNull(list);
        // list = parser.parseExpression("attachmentList").getValue(context, List.class);
        // System.out.println(list);
        // assertNotNull(list);

        parser.parseExpression("#root['attachmentList'][0]").setValue(context, new AttachmentInfo());
        // parser.parseExpression("attachmentList[0].attachSequence").setValue(context, "1");

        List actualAttachmentList = (List) rootObject.get("attachmentList");
        System.out.println("actualAttachmentList: " + actualAttachmentList);
        // assertEquals(1, actualAttachmentList.size());
        // AttachmentInfo actualAttachmentInfo = (AttachmentInfo) actualAttachmentList.get(0);
        // assertEquals(1, actualAttachmentInfo.getAttachSequence());
    }

    @Test
    public void test2() {
        ArrayList<AttachmentInfo> rootObject = new ArrayList<AttachmentInfo>();

        EvaluationContext context = new StandardEvaluationContext(rootObject);
        ExpressionParser parser = new SpelExpressionParser();

        parser.parseExpression("#root[0]").setValue(context, new AttachmentInfo());
        // parser.parseExpression("attachmentList[0].attachSequence").setValue(context, "1");

        List actualAttachmentList = rootObject;
        System.out.println("actualAttachmentList: " + actualAttachmentList);
        // assertEquals(1, actualAttachmentList.size());
        // AttachmentInfo actualAttachmentInfo = (AttachmentInfo) actualAttachmentList.get(0);
        // assertEquals(1, actualAttachmentInfo.getAttachSequence());
    }

}
