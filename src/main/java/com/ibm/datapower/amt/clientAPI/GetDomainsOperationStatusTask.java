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



package com.ibm.datapower.amt.clientAPI;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;


/**
 * A BackgroundTask to get the {@link com.ibm.datapower.amt.OperationStatus}
 * of each managed domain on this device.
 * <p>
 * @version SCM ID: $Id: GetDomainsOperationStatusTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
public class GetDomainsOperationStatusTask extends BackgroundTask {
    private Device device = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = GetDomainsOperationStatusTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    GetDomainsOperationStatusTask(Device device) {
        super();
        this.device = device;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
    
    /**
     * Get the device that will be probed for the OperationStatus
     * of each managed domain.
     * 
     * @return the device that will be probed.
     */
    public Device getDevice() {
        return(this.device);
    }

    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "GetDomainsOperationStatusTask["; //$NON-NLS-1$
        result += "device=" + this.device; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }
    
    protected int estimateSteps() {
        int steps = 0;
        Domain[] domains = null;
        if (device != null) {
            try {
                domains = device.getManagedDomains();
            } catch (DeletedException e1) {
                domains = new Domain[0];
            }
            steps += domains.length;
        }
        return(steps);
    }
    
    // TODO: what else am I missing in the Stored* constructors?

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        try {
            
            // get OperationStatus of each managed domain in each managed set
            
            if (device == null) {
            	if ( this.progressContainer != null )
            		this.progressContainer.incrementCurrentStep(1,"wamt.clientAPI.GetDomainsOperationStatusTask.devNotInMs_txt", "null"); //$NON-NLS-1$
            } else {
                Domain[] domains = null;
                try {
                    domains = device.getManagedDomains();
                } catch (DeletedException e1) {
                    domains = new Domain[0];
                }
                for (int i=0; i<domains.length; i++) {
                    try {
                    	Object[] args = new Object[] {domains[i].getAbsoluteDisplayName(), this.device.getDisplayName()};
                        this.progressContainer.incrementCurrentStep(1,"wamt.clientAPI.GetDomainsOperationStatusTask.probingStat_txt", args);  //$NON-NLS-1$                                
                        this.device.probeDomainStatus(domains[i]);
                    } catch (DeletedException e2) {
                        // skip it
                    }
                }
            }
            
            // there is no lock, so we can commit the result/error immediately
            this.progressContainer.setComplete();
            
        } catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } finally {
            this.cleanup();
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

//    protected boolean affectsFirmware() {
//        return false;
//    }
//
//    protected boolean affectsDomains() {
//        return false;
//    }
//
//    protected String getSingleAffectedDomain() {
//        return null;
//    }
//
//    protected Device getSingleAffectedDevice() {
//        return null;
//    }
}
