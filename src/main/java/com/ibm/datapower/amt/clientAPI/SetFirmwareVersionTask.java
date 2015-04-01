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
 * A task to set the desired firmware version for a device.  This is the 
 * firmware version that is used by the deploy firmware version task. 
 * <p>
 * @version SCM ID: $Id: SetFirmwareVersionTask.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
 */
//* <p>
//* Created on Jan 7, 2010
//* 
public class SetFirmwareVersionTask extends BackgroundTask {
	Device device = null;
	FirmwareVersion firmwareVersion = null;
	
	public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = SetFirmwareVersionTask.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * 
     * @param targetDevice
     * @param desiredFirmwareVersion
     */
    SetFirmwareVersionTask(Device targetDevice, FirmwareVersion desiredFirmwareVersion) {
  	  super();
  	  device = targetDevice;
  	  firmwareVersion = desiredFirmwareVersion;
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
        String result = "SetFirmwareVersionTask["; //$NON-NLS-1$
        result += "device=" + this.device.getDisplayName(); //$NON-NLS-1$
        if ( this.firmwareVersion != null )
        	result += ", firmwareVersion=" + this.firmwareVersion.getAbsoluteDisplayName(); //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }


    protected int estimateSteps() {
        // one step to set the firmware version
        return(1);
    }
    
    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // TODO: add locking
        try {
            
            try {
                device.setSourceFirmwareVersionAction(firmwareVersion);
                // need to release lock before making update visible, so commit later
                this.progressContainer.setUncommittedComplete();
            }
        	catch (DeletedException e){
    			// Firmware Version was deleted
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        "Exception occurred, Firmware Version was deleted", e); //$NON-NLS-1$
        		this.progressContainer.setUncommittedError(e);            			
        	}
            catch (Exception e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            }
            
        } 
        catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setUncommittedError(e);
        } 
        finally {
            // release device lock?
            this.progressContainer.commit();
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
