package org.jboss.processFlow.util;

// This class will be stored in quartz as part of job detail
// will also be passed to kSessionService as part of signalEvent to process
public class GlobalQuartzJobHandle implements java.io.Serializable {

    private long id;
    private String jobName;
    private String jobGroup;
    private int sessionId;
    private long interval;
    private String timerExpresson;

    public GlobalQuartzJobHandle() {}

    public GlobalQuartzJobHandle(long id, String name, String group, int sessionId) {
        this.id = id;
        this.jobName = name;
        this.jobGroup = group;
        this.sessionId = sessionId;
    }
    public long getId() {
        return id;
    }
    public String getJobName() {
        return jobName;
    }
    public String getJobGroup() {
        return jobGroup;
    }
    public int getSessionId(){
        return sessionId;
    }
    public void setInterval(long x){
        this.interval = x;
    }
    public long getInterval(){
        return interval;
    }
    public void setTimerExpression(String x){
        this.timerExpresson    = x;
    }
    public String getTimerExpression(){
        return this.timerExpresson;
    }
}
