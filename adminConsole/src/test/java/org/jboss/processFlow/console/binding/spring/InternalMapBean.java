package org.jboss.processFlow.console.binding.spring;

import java.util.Map;

/**
 * DOCME
 * 
 * @author tanxu
 * @date Feb 15, 2012
 * @since
 */
public class InternalMapBean {

    private Map<String, Object> resultsMap;

    public InternalMapBean() {
    }

    /**
     * @return the resultsMap
     */
    public Map<String, Object> getResultsMap() {
        return resultsMap;
    }

    /**
     * @param resultsMap the resultsMap to set
     */
    public void setResultsMap(Map<String, Object> resultsMap) {
        this.resultsMap = resultsMap;
    }

    @Override
    public String toString() {
        return "ResultBean [resultsMap=" + resultsMap + "]";
    }

}
