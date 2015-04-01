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
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A BackgroundTask to unsubscribe from every managed device across all managed sets.
 * This will likely be done as part of a Manager shutdown.
 * <p>
 * @version SCM ID: $Id: UnsubscribeAllTask.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
 */
//* <p>
class UnsubscribeAllTask extends BackgroundTask {

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = UnsubscribeAllTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    UnsubscribeAllTask() {
        super();
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
    
    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "UnsubscribeAllTask["; //$NON-NLS-1$
        result += "fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    protected int estimateSteps() {
        int numManagedDevices = 0;
        Manager manager = Manager.internalGetInstance();
        ManagedSet[] managedSets = manager.getManagedSets();
        for (int managedSetIndex=0; managedSetIndex<managedSets.length; managedSetIndex++) {
            try {
                Device[] devices = managedSets[managedSetIndex].getDeviceMembers();
                for (int deviceIndex=0; deviceIndex<devices.length; deviceIndex++) {
                    numManagedDevices++;
                }
            } catch (DeletedException e) {
                // skip this managed set
            }
        }
        return(numManagedDevices);
    }

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        try {
            Exception previousException = null;
            Manager manager = Manager.internalGetInstance();
            ManagedSet[] managedSets = manager.getManagedSets();
            for (int managedSetIndex=0; managedSetIndex<managedSets.length; managedSetIndex++) {
                try {
                    Device[] devices = managedSets[managedSetIndex].getDeviceMembers();
                    for (int deviceIndex=0; deviceIndex<devices.length; deviceIndex++) {
                    	if(!DeviceType.XC10.equals(devices[deviceIndex].getDeviceType())){
                    		try {
                    			// don't need to obtain a lock, as it isn't changing readable attributes
                    			devices[deviceIndex].unsubscribe(this.progressContainer);
                    		} catch (InvalidCredentialsException e3) {
                    			String hostName = devices[deviceIndex].getHostname();
                    			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    					Messages.getString("ProbCommDev",hostName), e3); //$NON-NLS-1$
                    			previousException = e3;
                    		} catch (DeviceExecutionException e3) {
                    			// log it and continue;
                    			String hostName = devices[deviceIndex].getHostname();
                    			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    					Messages.getString("ProbExecComm",hostName), e3); //$NON-NLS-1$
                    		} catch (AMPIOException e3) {
                    			// log it and continue
                    			String hostName = devices[deviceIndex].getHostname();
                    			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    					Messages.getString("ProbCommDev",hostName), e3); //$NON-NLS-1$
                    		} catch (DeletedException e3) {
                    			// this device is already gone, don't worry about it
                    			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    					"deleted:", e3); //$NON-NLS-1$
                    		} catch (AMPException e3) {
                    			// log it and continue
                    			String hostName = devices[deviceIndex].getHostname();
                    			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    					Messages.getString("ProbCommDev",hostName), e3); //$NON-NLS-1$
                    		}
                    	}
                    }
                } catch (DeletedException e2) {
                    // skip this managed set
                }
            }

            // there is no lock, so we can commit the result/error immediately
            if (previousException == null) {
                this.progressContainer.setComplete();
            } else {
                this.progressContainer.setError(previousException);
            }
            
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
