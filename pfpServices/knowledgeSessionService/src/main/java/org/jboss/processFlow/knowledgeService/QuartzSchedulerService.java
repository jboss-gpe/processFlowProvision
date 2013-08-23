/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.processFlow.knowledgeService;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.common.InternalWorkingMemory;
import org.drools.common.Scheduler.ActivationTimerJobContext;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.persistence.jpa.JpaTimerJobInstance;
import org.drools.runtime.process.ProcessInstance;
import org.drools.time.AcceptsTimerJobFactoryManager;
import org.drools.time.InternalSchedulerService;
import org.drools.time.Job;
import org.drools.time.JobContext;
import org.drools.time.JobHandle;
import org.drools.time.TimerService;
import org.drools.time.Trigger;
import org.drools.time.impl.DefaultJobHandle;
import org.drools.time.impl.DefaultTimerJobFactoryManager;
import org.drools.time.impl.TimerJobFactoryManager;
import org.drools.time.impl.TimerJobInstance;
import org.drools.time.SessionClock;
import org.jbpm.process.instance.timer.TimerManager.ProcessJobContext;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.timer.NamedJobContext;

/**
 * Quartz based <code>GlobalSchedulerService</code> that is configured according
 * to Quartz rules and allows to store jobs in data base. With that it survives 
 * server crashes and operates as soon as service is initialized without session 
 * being active.
 *
 */
public class QuartzSchedulerService implements TimerService, InternalSchedulerService, AcceptsTimerJobFactoryManager, SessionClock  {
    
	public static final String TIMER_JOB_INSTANCE = "timerJobInstance";
	public static final String JOB_GROUP = "jbpm";
    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulerService.class);

    // Quartz Scheduler
    private static Scheduler scheduler;
    
    // For purposes of PER_PROCESS_INSTANCE architecture, will never populate that object
    // subsequently, the session will not include the job trigger
    // the job trigger will always be managed by the Quartz scheduler
    private static TimerJobFactoryManager jobFactoryManager = DefaultTimerJobFactoryManager.instance;
   
    // used to signal the process instance
    private static IBaseKnowledgeSession kSessionProxy;
    
    private static AtomicLong idCounter = new AtomicLong();
    
    
    public static void start() throws Exception{
    	Context jndiContext;
    	jndiContext = new InitialContext();
    	kSessionProxy = (IBaseKnowledgeSession)jndiContext.lookup(IBaseKnowledgeSession.BASE_JNDI);
    	scheduler = StdSchedulerFactory.getDefaultScheduler();            
    	scheduler.start();
    }
    
    public static void stop() throws Exception {
    	scheduler.shutdown();
    	scheduler = null;
    }
    
    public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger) {
        String jobname = null;
        int sessionId = 0;
        if (ctx instanceof ProcessJobContext) {
            ProcessJobContext processCtx = (ProcessJobContext) ctx;
            StatefulKnowledgeSessionImpl wM = (StatefulKnowledgeSessionImpl)processCtx.getKnowledgeRuntime();
            sessionId = wM.getId();
            jobname = "ProcessJob-"+processCtx.getProcessInstanceId() + "-" + processCtx.getTimer().getId();
        } else if (ctx instanceof NamedJobContext) {
            jobname = "NamedJob-"+((NamedJobContext) ctx).getJobName();
        } else if (ctx instanceof ActivationTimerJobContext) {
            InternalWorkingMemory wM =  (InternalWorkingMemory)((ActivationTimerJobContext)ctx).getAgenda().getWorkingMemory();
            sessionId = wM.getId();
            jobname = "ActivationTimerJob-"+sessionId;
        } else {
            throw new RuntimeException("scheduleJob() unknown jobContext = "+ctx);
        }
        log.info("scheduleJob() jobName = "+jobname+" : nextFireDate = "+trigger.hasNextFireTime());
        
        // check if this scheduler already has such job registered if so there is no need to schedule it again        
        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobname, JOB_GROUP);
            
            String[] jobNames = scheduler.getJobNames(JOB_GROUP);
            if(jobNames.length > 0){
            	for(String name : jobNames){
            		log.info("scheduleJob()  existing job detail name = "+name);
            	}
            }
        
            if (jobDetail != null) {
                TimerJobInstance timerJobInstance = (TimerJobInstance) jobDetail.getJobDataMap().get("timerJobInstance");
                return timerJobInstance.getJobHandle();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        Long id = idCounter.getAndIncrement();
        GlobalQuartzJobHandle quartzJobHandle = new GlobalQuartzJobHandle(id, jobname, JOB_GROUP, sessionId);
        
        TimerJobInstance jobInstance = this.jobFactoryManager.createTimerJobInstance( job, ctx, trigger, quartzJobHandle, this);
        quartzJobHandle.setTimerJobInstance( (TimerJobInstance) jobInstance );

        internalSchedule(jobInstance);
        
        return quartzJobHandle;
    }

    public void internalSchedule(TimerJobInstance timerJobInstance) {

        GlobalQuartzJobHandle quartzJobHandle = (GlobalQuartzJobHandle) timerJobInstance.getJobHandle();
        // Define job instance
        JobDetail jdetail = new JobDetail(quartzJobHandle.getJobName(), quartzJobHandle.getJobGroup(), QuartzJob.class);

        jdetail.getJobDataMap().put(TIMER_JOB_INSTANCE, timerJobInstance);
        
        Date nextFireTime = timerJobInstance.getTrigger().hasNextFireTime();
        // Define a Trigger that will fire "now"
        org.quartz.Trigger triggerObj = new SimpleTrigger(quartzJobHandle.getJobName()+"_trigger", quartzJobHandle.getJobGroup(), nextFireTime);
            
        try {
        	// triggers are not to be stored in drools sessions
            //this.jobFactoryManager.addTimerJobInstance( timerJobInstance );
        	
            if (null == scheduler.getJobDetail(quartzJobHandle.getJobName(), quartzJobHandle.getJobGroup())) {
                scheduler.scheduleJob(jdetail, triggerObj);
            } else {
                // need to add the job again to replace existing especially important if jobs are persisted in db
                scheduler.addJob(jdetail, true);
                triggerObj.setJobName(quartzJobHandle.getJobName());
                triggerObj.setJobGroup(quartzJobHandle.getJobGroup());
                scheduler.rescheduleJob(quartzJobHandle.getJobName()+"_trigger", quartzJobHandle.getJobGroup(), triggerObj);
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    

    /*  NOTE:  BRMS5.3.1, as part of disposing of a session, this function will be invoked.
        Subsequently, timer will never fire.
        Will move this functionality to a 'deleteJob' function that is invoked when needed
    */ 
    @Override
    public boolean removeJob(JobHandle jobHandle) {
        return true;
    }
    
    public static boolean deleteJob(GlobalQuartzJobHandle jobHandle) {
    	try {
    		return scheduler.deleteJob(jobHandle.getJobName(), jobHandle.getJobGroup());            
    	} catch (Exception x) {
    		throw new RuntimeException(x);
    	}
    }

    public void shutdown() { 
    	//log.warn("shutdown() function not valid");
    }
    
    public void forceShutdown() {
    	//log.warn("forceShutdown() function not valid");
    }
    public synchronized void initScheduler() {
    	// will handle this in start() function
    }
    
    public static class QuartzJob implements org.quartz.Job {

        public QuartzJob() {}

        public void execute(JobExecutionContext qContext) throws JobExecutionException {
            int pState = kSessionProxy.processJobExecutionContext(qContext);
            JpaTimerJobInstance timerJInstance = (JpaTimerJobInstance)(qContext.getMergedJobDataMap().get(QuartzSchedulerService.TIMER_JOB_INSTANCE));
            GlobalQuartzJobHandle jHandle = (GlobalQuartzJobHandle)timerJInstance.getJobHandle();
            if(ProcessInstance.STATE_COMPLETED == pState){
            	boolean dSuccess = deleteJob(jHandle);
            	log.info("execute() just signaled.  pState = "+pState+" : jobName = "+jHandle.getJobName()+" : successful deletion = "+dSuccess);
            }else {
            	log.info("execute() just signaled.  pState = "+pState+" : jobName = "+jHandle.getJobName());
            }
        }
    }
   
    public static class GlobalQuartzJobHandle extends DefaultJobHandle implements java.io.Serializable {
        
        private String jobName;
        private String jobGroup;
        private int sessionId;
       
        public GlobalQuartzJobHandle(long id, String name, String group, int sessionId) {
            super(id);
            this.jobName = name;
            this.jobGroup = group;
            this.sessionId = sessionId;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getJobGroup() {
            return jobGroup;
        }

        public void setJobGroup(String jobGroup) {
            this.jobGroup = jobGroup;
        }
        public int getSessionId(){
        	return sessionId;
        }
    }

    public JobHandle buildJobHandleForContext(NamedJobContext ctx) {
        return new GlobalQuartzJobHandle(-1, ctx.getJobName(), "jbpm", 0);
    }

    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    @Override
    public long getTimeToNextJob() {
        return 0;
    }

    @Override
    public Collection<TimerJobInstance> getTimerJobInstances() {
        return jobFactoryManager.getTimerJobInstances();
    }

    @Override
    public TimerJobFactoryManager getTimerJobFactoryManager() {
        return this.jobFactoryManager;
    }

    @Override
    public void setTimerJobFactoryManager(TimerJobFactoryManager arg0) {
        this.jobFactoryManager= arg0;
    }
}
