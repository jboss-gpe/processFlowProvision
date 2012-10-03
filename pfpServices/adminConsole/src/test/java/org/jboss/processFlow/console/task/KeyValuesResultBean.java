package org.jboss.processFlow.console.task;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.TransformerUtils;

/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public class KeyValuesResultBean {

    private Map fieldValues = MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer());
    private Map fieldTypes = MapUtils.lazyMap(new HashMap(), TransformerUtils.stringValueTransformer());

    /**
     * @return the fieldValues
     */
    public Map getFieldValues() {
        return fieldValues;
    }

    /**
     * @return the fieldTypes
     */
    public Map getFieldTypes() {
        return fieldTypes;
    }

    @Override
    public String toString() {
        return "KeyValuesResultBean [fieldValues=" + fieldValues + ", fieldTypes=" + fieldTypes + "]";
    }

}
