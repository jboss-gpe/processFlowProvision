package org.jboss.processFlow.ejb;

import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

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
import org.jboss.processFlow.haTimerService.ITimerServiceManagement;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/*
 * plagarized from org.drools.time.impl.JDKTimerService
 */
public class ClusteredSingletonTimerService implements TimerService, SessionClock, InternalSchedulerService, AcceptsTimerJobFactoryManager {

	private static ITimerServiceManagement timerMgmt;
	private static Logger log = LoggerFactory.getLogger("ClusteredSingletonTimerService");
    private AtomicLong                    idCounter = new AtomicLong();
    private TimerJobFactoryManager        jobFactoryManager = DefaultTimerJobFactoryManager.instance;
    
    
    public ClusteredSingletonTimerService() throws NamingException {
    	Context jndiContext = null;
    	try {
    		Properties jndiProps = new Properties();
    		jndiProps.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
    		jndiContext = new InitialContext(jndiProps);
    		timerMgmt = (ITimerServiceManagement)jndiContext.lookup(ITimerServiceManagement.TIMER_SERVICE_MANAGEMENT_JNDI);
    		log.info("ClusteredSingletonTimerService() found TimerServiceMgmt says : "+timerMgmt.sanityCheck());
    	}finally {
    		if(jndiContext != null)
    			jndiContext.close();
    	}
    }

    public void setTimerJobFactoryManager(TimerJobFactoryManager timerJobFactoryManager) {
        this.jobFactoryManager = timerJobFactoryManager;
    }

    public TimerJobFactoryManager getTimerJobFactoryManager() {
        return this.jobFactoryManager;
    }

    public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger) {
        GlobalJobHandle jobHandle = new GlobalJobHandle(idCounter.getAndIncrement() );
        Date date = trigger.hasNextFireTime();
        if ( date != null ) {
            TimerJobInstance jobInstance = jobFactoryManager.createTimerJobInstance( job,
                                                                                     ctx,
                                                                                     trigger,
                                                                                     jobHandle,
                                                                                     this );
            jobHandle.setTimerJobInstance( (TimerJobInstance) jobInstance );
            internalSchedule( (TimerJobInstance) jobInstance );

            return jobHandle;
        } else {
            return null;
        } 
    }
    
    public void internalSchedule(TimerJobInstance timerJobInstance){
    	Date date = timerJobInstance.getTrigger().hasNextFireTime();
    	Callable<Void> item = (Callable<Void>) timerJobInstance;
    }
    
    
    
    public boolean removeJob(JobHandle jobHandle) {
        jobHandle.setCancel( true );
        jobFactoryManager.removeTimerJobInstance( ((GlobalJobHandle) jobHandle).getTimerJobInstance() );
        //this.scheduler.remove( (Runnable) ((JDKJobHandle) jobHandle).getFuture() );
        return true;
    }

    public long getTimeToNextJob() {
        return 0;
    }

    public Collection<TimerJobInstance> getTimerJobInstances() {
        return jobFactoryManager.getTimerJobInstances();
    }

    public static class GlobalJobHandle extends DefaultJobHandle implements JobHandle {
        private static final long     serialVersionUID = 510l;

        private ScheduledFuture<Void> future;       

        public GlobalJobHandle(long id) {
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
