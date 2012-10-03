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

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * JPA entity to link the sub process instance with the subprocess node instance
 * 
 * @date Jan 14, 2012
 * @since
 */
@Entity
public class SubProcessInstanceLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private long subProcessInstanceId;
    private long parentProcessInstanceId;
    private long subProcessNodeInstanceId;

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the subProcessInstanceId
     */
    public long getSubProcessInstanceId() {
        return subProcessInstanceId;
    }

    /**
     * @param subProcessInstanceId the subProcessInstanceId to set
     */
    public void setSubProcessInstanceId(long subProcessInstanceId) {
        this.subProcessInstanceId = subProcessInstanceId;
    }

    /**
     * @return the parentProcessInstanceId
     */
    public long getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    /**
     * @param parentProcessInstanceId the parentProcessInstanceId to set
     */
    public void setParentProcessInstanceId(long parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    /**
     * @return the subProcessNodeInstanceId
     */
    public long getSubProcessNodeInstanceId() {
        return subProcessNodeInstanceId;
    }

    /**
     * @param subProcessNodeInstanceId the subProcessNodeInstanceId to set
     */
    public void setSubProcessNodeInstanceId(long subProcessNodeInstanceId) {
        this.subProcessNodeInstanceId = subProcessNodeInstanceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (parentProcessInstanceId ^ (parentProcessInstanceId >>> 32));
        result = prime * result + (int) (subProcessInstanceId ^ (subProcessInstanceId >>> 32));
        result = prime * result + (int) (subProcessNodeInstanceId ^ (subProcessNodeInstanceId >>> 32));
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
        SubProcessInstanceLog other = (SubProcessInstanceLog) obj;
        if (id != other.id)
            return false;
        if (parentProcessInstanceId != other.parentProcessInstanceId)
            return false;
        if (subProcessInstanceId != other.subProcessInstanceId)
            return false;
        if (subProcessNodeInstanceId != other.subProcessNodeInstanceId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SubProcessInstanceLog [subProcessInstanceId=" + subProcessInstanceId + ", parentProcessInstanceId="
                + parentProcessInstanceId + ", subProcessNodeInstanceId=" + subProcessNodeInstanceId + "]";
    }

}
