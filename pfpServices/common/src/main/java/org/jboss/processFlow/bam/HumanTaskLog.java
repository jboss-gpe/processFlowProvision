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

package org.jboss.processFlow.bam;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * JPA entity to link the human task with the human task node instance
 * 
 * @author tanxu
 * @date Jun 21, 2011
 * @since
 */
@Entity
public class HumanTaskLog implements Externalizable {

    public static final int AFTER_HUMANTASK_CREATED = 902;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private long processInstanceId;
    /**
     * the human task node instance id
     */
    private long nodeInstanceId;
    private long workItemId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the processInstanceId
     */
    public long getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * @param processInstanceId the processInstanceId to set
     */
    public void setProcessInstanceId(long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * @return the nodeInstanceId
     */
    public long getNodeInstanceId() {
        return nodeInstanceId;
    }

    /**
     * @param nodeInstanceId the nodeInstanceId to set
     */
    public void setNodeInstanceId(long nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }

    /**
     * @return the workItemId
     */
    public long getWorkItemId() {
        return workItemId;
    }

    /**
     * @param workItemId the workItemId to set
     */
    public void setWorkItemId(long workItemId) {
        this.workItemId = workItemId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (nodeInstanceId ^ (nodeInstanceId >>> 32));
        result = prime * result + (int) (processInstanceId ^ (processInstanceId >>> 32));
        result = prime * result + (int) (workItemId ^ (workItemId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HumanTaskLog other = (HumanTaskLog) obj;
        if (nodeInstanceId != other.nodeInstanceId)
            return false;
        if (processInstanceId != other.processInstanceId)
            return false;
        if (workItemId != other.workItemId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HumanTaskLog [id=" + id + ", processInstanceId=" + processInstanceId + ", nodeInstanceId="
                + nodeInstanceId + ", workItemId=" + workItemId + "]";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(id);
        out.writeLong(processInstanceId);
        out.writeLong(nodeInstanceId);
        out.writeLong(workItemId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readLong();
        processInstanceId = in.readLong();
        nodeInstanceId = in.readLong();
        workItemId = in.readLong();
    }
}
