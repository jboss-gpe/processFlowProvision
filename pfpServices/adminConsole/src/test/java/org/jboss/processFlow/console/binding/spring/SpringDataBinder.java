package org.jboss.processFlow.console.binding.spring;

import java.util.HashMap;
import java.util.Map;

import org.jboss.processFlow.console.binding.AbstractDataBinder;
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
public class SpringDataBinder extends AbstractDataBinder {

    private DefaultFormattingConversionService conversionService;

    public SpringDataBinder() {
        super();
        conversionService = new DefaultFormattingConversionService();
        // set the conversion service to convert the date
        conversionService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
    }

    @Override
    protected Map<String, Object> renderContextIfNeeded(Object target, Map<String, Object> valueContext) {
        HashMap<String, Object> newContext = new HashMap<String, Object>(valueContext.size());
        if (target instanceof InternalMapBean) {
            for (Map.Entry<String, Object> entry : valueContext.entrySet()) {
                String key = entry.getKey();
                if (!isNavigatiableProperty(key))
                    continue;

                // filter the interested context according to the property
                // otherwise, we might get the error if non-used context values exists:
                // "Property referenced in indexed property path is neither an array nor a List nor a Map"
                boolean match = false;
                for (String property : ((InternalMapBean) target).getResultsMap().keySet()) {
                    if (key.startsWith(property)) {
                        match = true;
                        break;
                    }
                }
                if (match == false)
                    continue;

                String newProperty = "resultsMap['"; // "resultsMap" is the filed name of InternalMapBean
                // deal with tokens contain nagivator sign
                int rightBracketIdx = key.indexOf(']');
                if (rightBracketIdx > 0) { // deal with indexed property path
                    String prop = key.substring(0, rightBracketIdx);
                    int leftBracketIdx = prop.indexOf('[');
                    if (leftBracketIdx > 0) {
                        newProperty = newProperty + prop.substring(0, leftBracketIdx) + "']"
                                + prop.substring(leftBracketIdx) + key.substring(rightBracketIdx);
                    }
                    else {
                        newProperty = newProperty + prop.substring(0, leftBracketIdx) + "']"
                                + key.substring(rightBracketIdx);
                    }
                }
                else {
                    int dotIdx = key.indexOf('.'); // deal with normal property path
                    if (dotIdx > 0) {
                        newProperty = newProperty + key.substring(0, dotIdx) + "']" + key.substring(dotIdx);
                    }
                    else {
                        newProperty = newProperty + key + "']";
                    }
                }

                newContext.put(newProperty, entry.getValue());
            }
        }
        else {
            newContext.putAll(valueContext);
        }
        return newContext;
    }

    @Override
    protected Object renderTargetIfNeeded(Object target, Map<String, Object> valueContext) {
        if (target instanceof Map) {
            InternalMapBean bean = new InternalMapBean();
            bean.setResultsMap((Map) target);
            return bean;
        }
        return target;
    }

    @Override
    protected Object doConvert(Class targetType, Object source) {
        return conversionService.convert(source, targetType);
    }

    @Override
    protected Map<String, Object> doBind(Object target, Map<String, Object> context) throws Exception {
        MutablePropertyValues values = new MutablePropertyValues();

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            values.addPropertyValue(entry.getKey(), entry.getValue());
        }

        DataBinder binder = new DataBinder(target);
        binder.setConversionService(conversionService);

        binder.bind(values);

        Map newValues = null;
        if (target instanceof InternalMapBean) {
            newValues = ((InternalMapBean) target).getResultsMap();
        }
        else {
            newValues = getProperties(target);
        }

        return newValues;
    }

}
