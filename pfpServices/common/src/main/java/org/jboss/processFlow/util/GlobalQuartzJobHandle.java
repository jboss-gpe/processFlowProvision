package org.jboss.processFlow.util;

// This class will be stored in quartz as part of job detail
// will also be passed to kSessionService as part of signalEvent to process
public class GlobalQuartzJobHandle implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String jobName;
    private String jobGroup;
    private int sessionId;
    private long interval;
    private String timerExpresson;
    private String processId;

    public GlobalQuartzJobHandle() {}

    public GlobalQuartzJobHandle(String name, String group, int sessionId) {
        this.jobName = name;
        this.jobGroup = group;
        this.sessionId = sessionId;
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

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}
}
