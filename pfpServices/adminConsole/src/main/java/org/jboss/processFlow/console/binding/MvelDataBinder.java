package org.jboss.processFlow.console.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.mvel2.DataConversion;
import org.mvel2.MVEL;

/**
 * The dataBinder adapts to MVEL.<br/>
 * This is used by the default dataBinder since it work quite well except the following limition(s):
 * <ul>
 * <li>do NOT support datetime conversion, since MVEL use global registry for the type conversion which could result in
 * unexpected value in complex runtime env</li>
 * </ul>
 * 
 * @author tanxu
 * @date Feb 18, 2012
 * @since
 */
public class MvelDataBinder extends AbstractDataBinder {

    public MvelDataBinder() {
        super();
    }

    @Override
    protected Object doConvert(Class targetType, Object source) {
        return DataConversion.convert(source, targetType);
    }

    @Override
    protected Map<String, Object> doBind(Object target, Map<String, Object> sourceContext) throws Exception {
        for (Map.Entry<String, Object> entry : sourceContext.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            MVEL.setProperty(target, key, value);
        }

        Map newValues = null;
        if (target instanceof Map) {
            newValues = ((Map) target);
        }
        else {
            newValues = getProperties(target);
        }
        return newValues;
    }

    @Override
    protected Map<String, Object> renderContextIfNeeded(Object target, Map<String, Object> sourceContext) {
        HashMap<String, Object> newContext = new HashMap<String, Object>(sourceContext.size());
        if (target instanceof Map) {
            for (Map.Entry<String, Object> sourceEntry : sourceContext.entrySet()) {
                String sourceKey = sourceEntry.getKey();
                if (!isNavigatiableProperty(sourceKey))
                    continue;

                // parse the property name from the sourceKey, for example, a[0].b -> a
                String prop = getProperty(sourceKey);

                // filter the interested context according to the property
                // otherwise, we might get the error if non-used context values exists:
                // "Property referenced in indexed property path is neither an array nor a List nor a Map"
                for (Map.Entry<String, Object> targetEntry : ((Map<String, Object>) target).entrySet()) {
                    if (prop.equals(targetEntry.getKey())) {
                        // deal with map binding from special expression:
                        // for example, (a[0].key=testkey, a[0].value=testvalue) -> a['testky']=testvalue
                        if (targetEntry.getValue() instanceof Map) {
                            // convert a[0].key, a[0].value to map entry
                            if (Pattern.matches(prop + "\\[\\d\\].(key|value)", sourceKey)) {
                                int dotIdx = sourceKey.indexOf('.');
                                String k = (String) sourceContext.get(sourceKey.substring(0, dotIdx) + ".key");
                                Object v = sourceContext.get(sourceKey.substring(0, dotIdx) + ".value");
                                if (k != null && !k.isEmpty() && v != null) {
                                    String newKey = prop + "['" + k + "']";
                                    newContext.put(newKey, v);
                                    continue;
                                }
                            }
                        }

                        newContext.put(sourceKey, sourceEntry.getValue());
                    }
                }
            }
        }
        else {
            newContext.putAll(sourceContext);
        }
        return newContext;
    }

    protected String getProperty(String keyWithToken) {
        int bracketIdx = keyWithToken.indexOf('[');
        int dotIdx = keyWithToken.indexOf('.');

        if (bracketIdx > 0 && dotIdx <= 0) {
            return keyWithToken.substring(0, bracketIdx);
        }
        else if (bracketIdx <= 0 && dotIdx > 0) {
            return keyWithToken.substring(0, dotIdx);
        }
        else if (bracketIdx > 0 && dotIdx > 0) {
            return keyWithToken.substring(0, Math.min(bracketIdx, dotIdx));
        }
        else {
            return keyWithToken;
        }
    }

    @Override
    protected Object renderTargetIfNeeded(Object target, Map<String, Object> sourceContext) {
        return target;
    }

}
