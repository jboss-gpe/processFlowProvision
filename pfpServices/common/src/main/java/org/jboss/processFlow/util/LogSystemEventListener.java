/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and others contributors as indicated 
 * by the @authors tag. All rights reserved. 
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors. 
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 * 
 * (C) 2005-2010
 */
package org.jboss.processFlow.util;

import org.apache.log4j.Logger;
import org.drools.SystemEventListener;

/**
 * A SystemEventListener that uses log4j.
 * 
 * @author dward at jboss.org
 */
public class LogSystemEventListener implements SystemEventListener
{

    private static final Logger logger = Logger.getLogger(LogSystemEventListener.class);
    
    private static final String message_prefix = "Message [";
    private static final String object_prefix = " Object [";
    private static final String suffix = "]";
    
    public LogSystemEventListener() {}
    
    protected Logger getLogger()
    {
        return logger;
    }
    
    protected String getMessagePrefix()
    {
        return message_prefix;
    }
    
    private final String format(String message)
    {
        return new StringBuilder()
            .append(getMessagePrefix())
            .append(String.valueOf(message))
            .append(suffix)
            .toString();
    }
    
    private final String format(String message, Object object)
    {
        return new StringBuilder()
            .append(getMessagePrefix())
            .append(String.valueOf(message))
            .append(suffix)
            .append(object_prefix)
            .append(String.valueOf(object))
            .append(suffix)
            .toString();
    }

    public final void info(String message)
    {
        getLogger().info(format(message));
    }

    public final void info(String message, Object object)
    {
        getLogger().info(format(message, object));
    }

    public final void warning(String message)
    {
        getLogger().warn(format(message));
    }

    public final void warning(String message, Object object)
    {
        getLogger().warn(format(message, object));
    }
    
    public final void exception(Throwable e)
    {
        getLogger().error(format(e.getMessage()), e);
    }

    public final void exception(String message, Throwable e)
    {
        getLogger().error(format(message), e);
    }

    public final void debug(String message)
    {
        getLogger().debug(format(message));
    }

    public final void debug(String message, Object object)
    {
        getLogger().debug(format(message, object));
    }

}
