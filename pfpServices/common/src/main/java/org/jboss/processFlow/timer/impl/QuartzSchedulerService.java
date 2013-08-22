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
package org.jboss.processFlow.timer.impl;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.common.InternalWorkingMemory;
import org.drools.common.Scheduler.ActivationTimerJobContext;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.time.AcceptsTimerJobFactoryManager;
import org.drools.time.InternalSchedulerService;
import org.drools.time.Job;
import org.drools.time.JobContext;
import org.drools.time.JobHandle;
import org.drools.time.SelfRemovalJobContext;
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
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.processFlow.knowledgeService.IBaseKnowledgeSession;
import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.timer.NamedJobContext;

/**
 * Quartz based <code>GlobalSchedulerService</code> that is configured according
 * to Quartz rules and allows to store jobs in data base. With that it survives 
 * server crashes and operates as soon as service is initialized without session 
 * being active.
 *
 */
public class QuartzSchedulerService implements TimerService, InternalSchedulerService, AcceptsTimerJobFactoryManager, SessionClock  {
    
    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulerService.class);

    private AtomicLong idCounter = new AtomicLong();
    
    private TimerJobFactoryManager jobFactoryManager = DefaultTimerJobFactoryManager.instance;
    
    // global data shared across all scheduler service instances
    private static Scheduler scheduler;    
    private static AtomicInteger timerServiceCounter = new AtomicInteger();
    private static IBaseKnowledgeSession kSessionProxy;
 
    public QuartzSchedulerService() throws Exception {
        log.info("QuartzSchedulerService()");
        Context jndiContext = new InitialContext();
        kSessionProxy = (IBaseKnowledgeSession)jndiContext.lookup(IBaseKnowledgeSession.BASE_JNDI);
    }
    
    @Override
    public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger) {
        Long id = idCounter.getAndIncrement();
        String jobname = null;
        
        if(scheduler == null)
            this.initScheduler();
        
        if (ctx instanceof ProcessJobContext) {
            ProcessJobContext processCtx = (ProcessJobContext) ctx;
            StatefulKnowledgeSessionImpl wM = (StatefulKnowledgeSessionImpl)processCtx.getKnowledgeRuntime();
            
            jobname = "ProcessJob-"+wM.getId() + "-" + processCtx.getProcessInstanceId() + "-" + processCtx.getTimer().getId();
        } else if (ctx instanceof NamedJobContext) {
            jobname = "NamedJob-"+((NamedJobContext) ctx).getJobName();
        } else if (ctx instanceof ActivationTimerJobContext) {
            InternalWorkingMemory wM =  (InternalWorkingMemory)((ActivationTimerJobContext)ctx).getAgenda().getWorkingMemory();
            int sessionId = wM.getId();
            jobname = "ActivationTimerJob-"+sessionId;
        } else {
            throw new RuntimeException("scheduleJob() unknown jobContext = "+ctx);
        }
        log.info("scheduleJob() jobName = "+jobname+" : nextFireDate = "+trigger.hasNextFireTime());
        
        // check if this scheduler already has such job registered if so there is no need to schedule it again        
        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobname, "jbpm");
        
            if (jobDetail != null) {
                TimerJobInstance timerJobInstance = (TimerJobInstance) jobDetail.getJobDataMap().get("timerJobInstance");
                return timerJobInstance.getJobHandle();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        GlobalQuartzJobHandle quartzJobHandle = new GlobalQuartzJobHandle(id, jobname, "jbpm");
        
        TimerJobInstance jobInstance = this.jobFactoryManager.createTimerJobInstance( job, ctx, trigger, quartzJobHandle, this);
        quartzJobHandle.setTimerJobInstance( (TimerJobInstance) jobInstance );

        internalSchedule(jobInstance);
        
        return quartzJobHandle;
    }

    public void internalSchedule(TimerJobInstance timerJobInstance) {

        GlobalQuartzJobHandle quartzJobHandle = (GlobalQuartzJobHandle) timerJobInstance.getJobHandle();
        // Define job instance
        JobDetail jobq = new JobDetail(quartzJobHandle.getJobName(), quartzJobHandle.getJobGroup(), QuartzJob.class);

        jobq.getJobDataMap().put("timerJobInstance", timerJobInstance);
        
        Date nextFireTime = timerJobInstance.getTrigger().hasNextFireTime();
        // Define a Trigger that will fire "now"
        org.quartz.Trigger triggerObj = new SimpleTrigger(quartzJobHandle.getJobName()+"_trigger", quartzJobHandle.getJobGroup(), nextFireTime);
            
        // Schedule the job with the trigger
        try {
            if (scheduler.isShutdown()) {
                return;
            }
            this.jobFactoryManager.addTimerJobInstance( timerJobInstance );
            JobDetail jobDetail = scheduler.getJobDetail(quartzJobHandle.getJobName(), quartzJobHandle.getJobGroup());
            if (jobDetail == null) {
                scheduler.scheduleJob(jobq, triggerObj);
            } else {
                // need to add the job again to replace existing especially important if jobs are persisted in db
                scheduler.addJob(jobq, true);
                triggerObj.setJobName(quartzJobHandle.getJobName());
                triggerObj.setJobGroup(quartzJobHandle.getJobGroup());
                scheduler.rescheduleJob(quartzJobHandle.getJobName()+"_trigger", quartzJobHandle.getJobGroup(), triggerObj);
            }
            
        } catch (JobPersistenceException e) {
            if (e.getCause() instanceof NotSerializableException) {
                // in case job cannot be persisted, like rule timer then make it in memory
                //internalSchedule(new InmemoryTimerJobInstanceDelegate(quartzJobHandle.getJobName(), ((GlobalTimerService) globalTimerService).getTimerServiceId()));
            } else {
                this.jobFactoryManager.removeTimerJobInstance(timerJobInstance);
                throw new RuntimeException(e);
            }
        } catch (SchedulerException e) {
            this.jobFactoryManager.removeTimerJobInstance(timerJobInstance);
            throw new RuntimeException("Exception while scheduling job", e);
        }
    }

    public synchronized void initScheduler() {
        timerServiceCounter.incrementAndGet();
        
        if (scheduler == null) {            
            try {
                scheduler = StdSchedulerFactory.getDefaultScheduler();            
                scheduler.start();
            } catch (SchedulerException e) {
                throw new RuntimeException("Exception when initializing QuartzSchedulerService", e);
            }
        }
    }

    /*  NOTE:  BRMS5.3.1, as part of disposing this session, will invoke this function
        subsequently, timer will never fire
    */ 
    @Override
    public boolean removeJob(JobHandle jobHandle) {
        GlobalQuartzJobHandle quartzJobHandle = (GlobalQuartzJobHandle) jobHandle;
        return true;
        /*
        try {
           
            boolean removed =  scheduler.deleteJob(quartzJobHandle.getJobName(), quartzJobHandle.getJobGroup());            
            return removed;
        } catch (SchedulerException e) {     
            
            throw new RuntimeException("Exception while removing job", e);
        } catch (RuntimeException e) {
            SchedulerMetaData metadata;
            try {
                metadata = scheduler.getMetaData();
                if (metadata.getJobStoreClass().isAssignableFrom(JobStoreCMT.class)) {
                    return true;
                }
            } catch (SchedulerException e1) {
                
            }
            throw e;
        }
        */
    }


    @Override
    public void shutdown() {
        int current = timerServiceCounter.decrementAndGet();
        /*
        if (scheduler != null && current == 0) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            scheduler = null;
        }
        */
    }
    
    public void forceShutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
//                e.printStackTrace();
            }
            scheduler = null;
        }
    }

    public static class GlobalQuartzJobHandle extends GlobalJobHandle {
        
        private static final long     serialVersionUID = 510l;
        private String jobName;
        private String jobGroup;
       
        public GlobalQuartzJobHandle(long id, String name, String group) {
            super(id);
            this.jobName = name;
            this.jobGroup = group;
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
    
    }
    
    public static class GlobalJobHandle extends DefaultJobHandle implements JobHandle{

        private static final long     serialVersionUID = 510l;

        public GlobalJobHandle(long id) {
            super(id);
        }

        public long getTimerId() {
            JobContext ctx = this.getTimerJobInstance().getJobContext();
            if (ctx instanceof SelfRemovalJobContext) {
                ctx = ((SelfRemovalJobContext) ctx).getJobContext();
            }
            return ((ProcessJobContext)ctx).getTimer().getId();
        }

        public int getSessionId() {
            JobContext ctx = this.getTimerJobInstance().getJobContext();
            if (ctx instanceof SelfRemovalJobContext) {
                ctx = ((SelfRemovalJobContext) ctx).getJobContext();
            }
            if (ctx instanceof ProcessJobContext) {
                //return ((ProcessJobContext)ctx).getSessionId();
                return -1;
            }

            return -1;
        }

    }

    
    public static class QuartzJob implements org.quartz.Job {

        public QuartzJob() {
            log.info("QuartzJob()");   
        }

        public void execute(JobExecutionContext quartzContext) throws JobExecutionException {
            log.info("execute() quartzContext = "+quartzContext);
            kSessionProxy.processJobExecutionContext(quartzContext);
        }
    }
    
    public static class InmemoryTimerJobInstanceDelegate implements TimerJobInstance, Serializable, Callable<Void> {
        
        private static final long serialVersionUID = 1L;
        private String jobname;
        private String timerServiceId;
        private transient TimerJobInstance delegate;
        
        public InmemoryTimerJobInstanceDelegate(String jobName, String timerServiceId) {
            this.jobname = jobName;
            this.timerServiceId = timerServiceId;
        }
        
        @Override
        public JobHandle getJobHandle() {
            findDelegate();
            return delegate.getJobHandle();
        }

        @Override
        public Job getJob() {
            findDelegate();
            return delegate.getJob();
        }

        @Override
        public Trigger getTrigger() {
            findDelegate();
            return delegate.getTrigger();
        }

        @Override
        public JobContext getJobContext() {
            findDelegate();
            return delegate.getJobContext();
        }
        
        protected void findDelegate() {
            if (delegate == null) {
                /*
                Collection<TimerJobInstance> timers = ((AcceptsTimerJobFactoryManager)TimerServiceRegistry.getInstance().get(timerServiceId))
                .getTimerJobFactoryManager().getTimerJobInstances();
                for (TimerJobInstance instance : timers) {
                    if (((GlobalQuartzJobHandle)instance.getJobHandle()).getJobName().equals(jobname)) {
                        delegate = instance;
                        break;
                    }
                }
                */
            }
        }

        @Override
        public Void call() throws Exception {
            findDelegate();
            return ((Callable<Void>)delegate).call();
        }
        
    }

    public JobHandle buildJobHandleForContext(NamedJobContext ctx) {
        return new GlobalQuartzJobHandle(-1, ctx.getJobName(), "jbpm");
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
