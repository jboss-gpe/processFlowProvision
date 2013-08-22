/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.processFlow.haTimerService;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.Remote;

import org.jboss.logging.Logger;


@Remote(ITimerServiceManagement.class)
@Singleton
public class TimerServiceManagementBean implements ITimerServiceManagement {
    private static Logger LOGGER = Logger.getLogger(TimerServiceManagementBean.class);
    
    @Resource
    private TimerService timerService;

    @Timeout
    public void handleTimeout(Timer timer) {
        LOGGER.info("scheduler() the following has timedout =" + timer.getInfo());
    }

    public void createIntervalTimer(long initialDuration, long intervalDuration, Serializable info) {
        TimerConfig tConfig = new TimerConfig(info, true);
        timerService.createIntervalTimer(initialDuration, intervalDuration, tConfig);
    }

    public void createSingleActionTimer(Date expiration, Serializable info){
        TimerConfig tConfig = new TimerConfig(info, true);
        timerService.createSingleActionTimer(expiration, tConfig);
    }
    
    public String sanityCheck(){
        if(timerService != null)
            return "sweet!  timerService != null";
        else
            return "oh-no!  timerService == null";
    }
    
    public void stop() {
        LOGGER.info("stop() Stop all existing HASingleton timers");
        for (Timer timer : timerService.getTimers()) {
            LOGGER.trace("Stop HASingleton timer: " + timer.getInfo());
            timer.cancel();
        }
    }
}
