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

package org.jboss.processFlow.console.forms;

import java.net.URL;

import javax.activation.DataHandler;

import org.jboss.bpm.console.server.plugin.FormAuthorityRef;
import org.jboss.bpm.console.server.plugin.FormDispatcherPlugin;

import org.jbpm.integration.console.forms.ProcessFormDispatcher;

// JA Bride :  modified until base jbpm5 stops using hibernate.cfg.xml that don't leverage our JCA DataSource pools
public class FormDispatcherComposite implements FormDispatcherPlugin {

	private FormDispatcherPlugin taskDispatcher;
	private FormDispatcherPlugin processDispatcher;

	public FormDispatcherComposite() {
		this.taskDispatcher = new TaskFormDispatcher();
		this.processDispatcher = new ProcessFormDispatcher();
	}

	public URL getDispatchUrl(FormAuthorityRef ref) {
		switch (ref.getType()) {
			case TASK:
				return taskDispatcher.getDispatchUrl(ref);
			case PROCESS:
				return processDispatcher.getDispatchUrl(ref);
			default:
				throw new IllegalArgumentException("Unknown authority type:" + ref.getType());
		}
	}

	public DataHandler provideForm(FormAuthorityRef ref) {
		switch (ref.getType()) {
			case TASK:
				return taskDispatcher.provideForm(ref);
			case PROCESS:
				return processDispatcher.provideForm(ref);
			default:
				throw new IllegalArgumentException("Unknown authority type:" + ref.getType());
		}
	}

}
