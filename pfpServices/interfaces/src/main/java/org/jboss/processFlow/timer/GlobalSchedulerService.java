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
package org.jboss.processFlow.timer;

import org.drools.time.InternalSchedulerService;
import org.drools.time.JobHandle;
import org.drools.time.SchedulerService;
import org.drools.time.TimerService;

/**
 * Implementations of these interface are responsible for scheduled jobs in global manner,
 * meaning not knowledge session scoped but global accessible for all the sessions that will
 * be configured to use this <code>GlobalSchedulerService</code>
 *
 */
public interface GlobalSchedulerService extends SchedulerService, InternalSchedulerService {
    
    /**
     * Allows to shutdown the scheduler service
     */
    void shutdown();
    
    JobHandle buildJobHandleForContext(NamedJobContext ctx);
}
