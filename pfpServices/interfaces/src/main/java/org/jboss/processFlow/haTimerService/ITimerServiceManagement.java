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

import java.util.Date;

import javax.ejb.TimerConfig;

public interface ITimerServiceManagement {
    
    public static final String TIMER_SERVICE_MANAGEMENT_JNDI = "ejb:/processFlow-haTimerService//TimerServiceManagementBean!org.jboss.processFlow.haTimerService.ITimerServiceManagement";
    public static final String TIMER_SERVICE = "org.jboss.processFlow.ejb.ClusteredSingletonTimerService";

    void createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig);
    void createSingleActionTimer(Date expiration, TimerConfig timerConfig);
    void stop();
    String sanityCheck();

}
