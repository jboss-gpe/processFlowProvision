package org.jboss.processFlow.ejb;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.drools.time.AcceptsTimerJobFactoryManager;
import org.drools.time.InternalSchedulerService;
import org.drools.time.Job;
import org.drools.time.JobContext;
import org.drools.time.JobHandle;
import org.drools.time.SessionClock;
import org.drools.time.TimerService;
import org.drools.time.Trigger;
import org.drools.time.impl.DefaultJobHandle;
import org.drools.time.impl.DefaultTimerJobFactoryManager;
import org.drools.time.impl.TimerJobFactoryManager;
import org.drools.time.impl.TimerJobInstance;

public class ClusteredSingletonTimerService implements TimerService, SessionClock, InternalSchedulerService, AcceptsTimerJobFactoryManager {

    private AtomicLong                    idCounter = new AtomicLong();
    private TimerJobFactoryManager        jobFactoryManager = DefaultTimerJobFactoryManager.instance;

    public void setTimerJobFactoryManager(TimerJobFactoryManager timerJobFactoryManager) {
        this.jobFactoryManager = timerJobFactoryManager;
    }

    public TimerJobFactoryManager getTimerJobFactoryManager() {
        return this.jobFactoryManager;
    }

    public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger) {
        ZookeeperJobHandle zkJobHandle = new ZookeeperJobHandle(idCounter.getAndIncrement() );
        Date date = trigger.hasNextFireTime();
        if ( date != null ) {
            TimerJobInstance jobInstance = jobFactoryManager.createTimerJobInstance( job,
                                                                                     ctx,
                                                                                     trigger,
                                                                                     zkJobHandle,
                                                                                     this );
            zkJobHandle.setTimerJobInstance( (TimerJobInstance) jobInstance );
            internalSchedule( (TimerJobInstance) jobInstance );

            return zkJobHandle;
        } else {
            return null;
        } 
    }

    public void internalSchedule(TimerJobInstance timerJobInstance) {
        Date date = timerJobInstance.getTrigger().hasNextFireTime();
        Callable<Void> item = (Callable<Void>) timerJobInstance;
    }

    public boolean removeJob(JobHandle jobHandle) {
        jobHandle.setCancel( true );
        jobFactoryManager.removeTimerJobInstance( ((ZookeeperJobHandle) jobHandle).getTimerJobInstance() );
        //this.scheduler.remove( (Runnable) ((JDKJobHandle) jobHandle).getFuture() );
        return true;
    }

    public long getTimeToNextJob() {
        return 0;
    }

    public Collection<TimerJobInstance> getTimerJobInstances() {
        return jobFactoryManager.getTimerJobInstances();
    }

    public static class ZookeeperJobHandle extends DefaultJobHandle implements JobHandle {
        private static final long     serialVersionUID = 510l;

        private ScheduledFuture<Void> future;       

        public ZookeeperJobHandle(long id) {
            super(id);
        }
        
        public ScheduledFuture<Void> getFuture() {
            return future;
        }

        public void setFuture(ScheduledFuture<Void> future) {
            this.future = future;
        }

        @Override
        public long getId() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isCancel() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setCancel(boolean arg0) {
            // TODO Auto-generated method stub
            
        }    
    }

    @Override
    public long getCurrentTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
        
    }
}
