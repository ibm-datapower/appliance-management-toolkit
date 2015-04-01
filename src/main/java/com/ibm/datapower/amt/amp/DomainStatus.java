/**
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.datapower.amt.amp;

import com.ibm.datapower.amt.AdminStatus;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.QuiesceStatus;

/**
 * A container to hold the multiple objects returned from
 * {@link Commands#getDomainStatus(DeviceContext, String)}.
 * <p>
 * @version SCM ID: $Id: DomainStatus.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
public class DomainStatus {
	AdminStatus adminState = AdminStatus.UNKNOWN;
    OperationStatus operationStatus = new OperationStatus(OperationStatus.Enumerated.UNKNOWN);
    boolean needsSave = false;
    boolean debugState = false;
    QuiesceStatus quiesceStatus = QuiesceStatus.UNKNOWN;
    
    public static final String COPYRIGHT_2009_2012 = Constants.COPYRIGHT_2009_2012;
    
    public static final DomainStatus UNKNOWN_DOMAIN_STATUS = new DomainStatus(AdminStatus.UNKNOWN, new OperationStatus(OperationStatus.Enumerated.UNKNOWN),
			false, false, QuiesceStatus.UNKNOWN);

    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$

    /**
     * Create an instance of the DomainStatus. This constructor should be
     * invoked only by {@link Commands#getDomainStatus(DeviceContext, String)}.
     * 
     * @param operationStatus the operation status of the domain
     * @param needsSave true if the domain has been modified but not saved to
     *        flash, false otherwise
     * @param debugState true if the domain has debugging/troubleshooting
     *        enabled, false otherwise
     */
    // This constructor is for V1/V2 provider
    public DomainStatus(OperationStatus operationStatus, boolean needsSave, boolean debugState, QuiesceStatus quiesceStatus) {
        this.operationStatus = operationStatus;
        this.needsSave = needsSave;
        this.debugState = debugState;
        this.quiesceStatus = quiesceStatus;
    }
    
    public DomainStatus(AdminStatus adminState, OperationStatus operationStatus, boolean needsSave, boolean debugState, QuiesceStatus quiesceStatus) {
        this.adminState = adminState;
    	this.operationStatus = operationStatus;
        this.needsSave = needsSave;
        this.debugState = debugState;
        this.quiesceStatus = quiesceStatus;
    }
    
    /**
     * Get the admin state of domain, enabled (AdminState.ENABLED) or disabled (AdminState.DISABLED).
     * 
     * @return AdminStatus.ENABLED if this object's admin state is "enabled", AdminStatus.DISABLED is "disabled", 
     *  AdminStatus.UNKNOWN if the domain is not on the device. 
     */
    public AdminStatus getAdminStatus() {
    	return (this.adminState);
    }
    
    /**
     * Get the operation status of this domain as it had been previously fetched
     * by {@link Commands#getDomainStatus(DeviceContext, String)}.
     * 
     * @return the operation status of this domain
     */
    public OperationStatus getOperationStatus() {
        return(this.operationStatus);
    }
    
    /**
     * Get the quiesce status of this domain as it had been previously fetched
     * by {@link Commands#getDomainStatus(DeviceContext, String)}.
     * 
     * @return the quiesce status of this domain
     * 
     */
    public QuiesceStatus getQuiesceStatus() {
        return(this.quiesceStatus);
    }
    
    /**
     * Get a boolean value which indicates if this domain has been modified but
     * not saved to flash.
     * 
     * @return true if this domain has been modified but not saved to flash,
     *         false otherwise.
     */
    public boolean getNeedsSave() {
        return(this.needsSave);
    }
    
    /**
     * Get a boolean value which indicates if this domain has debug or
     * troubleshooting enabled.
     * 
     * @return true if this domain has debug or troubleshooting enabled, false
     *         otherwise.
     */
    public boolean getDebugState() {
        return(this.debugState);
    }
    
    /**
     * Update the domain status
     * @param newStatus new domain status
     */
    public void setStatus(DomainStatus newStatus) {
    	this.adminState = newStatus.adminState;
    	this.operationStatus = newStatus.operationStatus;
        this.needsSave = newStatus.needsSave;
        this.debugState = newStatus.debugState;
        this.quiesceStatus = newStatus.quiesceStatus;
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "[DomainStatus " +  this.operationStatus + //$NON-NLS-1$
            " needsSave=" + this.needsSave + " debugState=" + this.debugState + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return(result);
    }
}
