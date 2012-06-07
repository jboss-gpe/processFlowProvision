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

package org.jboss.processFlow.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries ( {
    @NamedQuery(
        name="FIND_TASKWITHDRAWDATA_BY_COMBO",
        query=  "select tData from TaskWithDrawData tData where tData.signalType = :signalType and tData.serviceLine = :serviceLine and tData.accountNumber = :accountNumber and tData.accountNumber = :accountNumber and tData.brandId = :brandId order by tData.id"   
    )
})
public class TaskWithDrawData implements Externalizable {
    
    public static final String FIND_TASKWITHDRAWDATA_BY_COMBO="FIND_TASKWITHDRAWDATA_BY_COMBO";
    public static final String EVENT_TYPE = "EVENT_TYPE";
    public static final String REGISTER = "REGISTER";
    public static final String SIGNAL = "SIGNAL";
    public static final String WITHDRAW = "WITHDRAW";
    
    public static final String ID = "id";
    public static final String PROC_INSTANCE_ID = "procInstanceId";
    public static final String SIGNAL_TYPE = "signalType";
    public static final String SERVICE_LINE = "serviceLine";
    public static final String ACCOUNT_NUMBER = "accountNumber";
    public static final String BRAND_ID = "brandId";
    public static final String BIZ_PROC_ID = "bizProcId";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long    id;
    private long    procInstanceId;
    private String    signalType;
    private String  serviceLine;
    private String  accountNumber;
    private String  brandId;
    private String  bizProcId;
    
    
    public String getServiceLine() {
        return serviceLine;
    }
    public void setServiceLine(String serviceLine) {
        this.serviceLine = serviceLine;
    }
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    public String getBrandId() {
        return brandId;
    }
    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }
    public String getBizProcId() {
        return bizProcId;
    }
    public void setBizProcId(String bizProcId) {
        this.bizProcId = bizProcId;
    }

    
    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getSignalType() {
        return signalType;
    }
    public void setSignalType(String x) {
        this.signalType = x;
    }
    public long getProcInstanceId() {
        return procInstanceId;
    }
    public void setProcInstanceId(long procInstanceId) {
        this.procInstanceId = procInstanceId;
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong( id );
        out.writeLong( procInstanceId );
        out.writeUTF( signalType );
    }
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        id = in.readLong();
        procInstanceId = in.readLong();
        signalType = in.readUTF();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)procInstanceId;
        result = prime * result + ((signalType == null) ? 0 : signalType.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof TaskWithDrawData) ) return false;
        TaskWithDrawData other = (TaskWithDrawData) obj;
        
        if ( other.procInstanceId !=procInstanceId )
            return false;
        
        if ( signalType ==null ){
            if(other.signalType !=null)
                return false;
        }else if(!signalType.equals(other.signalType))
            return false;
        return true;
    }     
    
    public String toString() {
        return "[" + getClass().getSimpleName() + ":'" + id + ":'" +procInstanceId + "'"+signalType + "']";
    }
    
    
}
