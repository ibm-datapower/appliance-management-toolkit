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
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to deploy the desired firmware version for a device.  To set the 
 * firmware version, use the set firmware version task. 
 * <p>
 * <p>
 */
//* Created on Jan 7, 2010
//* 
public class DeployFirmwareVersionTask extends BackgroundTask {
	Device device = null;
	
	public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private boolean acceptLicense = false;
    
    protected static final String CLASS_NAME = DeployFirmwareVersionTask.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

	DeployFirmwareVersionTask(Device targetDevice) {
    	  super();
    	  device = targetDevice;
    	  this.progressContainer.setTotalSteps(this.estimateSteps());    	  
    }
	
	DeployFirmwareVersionTask(Device targetDevice, boolean acceptLicense) {
  	  super();
  	  device = targetDevice;
  	  this.progressContainer.setTotalSteps(this.estimateSteps());
  	  this.acceptLicense = acceptLicense;
  }

	/**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = CLASS_NAME; //$NON-NLS-1$
        result += "[device=" + this.device.getDisplayName(); //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

	protected int estimateSteps() {
		return(1);
    }
    
    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME); 
        
        // TODO: add lock device
        try {
            
            try {
            	// deploy firmware version
                //this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.v2_DeployFirmwareVersionTask.deploySourceFV"); //$NON-NLS-1$
            	device.deploySourceFirmwareVersionAction(this.progressContainer);
                
                // need to release lock before making update visible, so commit later
                this.progressContainer.setUncommittedComplete();

            }
            catch (DeviceExecutionException dee){
            	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        "Exception occurred, License not accepted", dee); //$NON-NLS-1$
        		this.progressContainer.setUncommittedError(dee);
            }
        	catch (DeletedException e){
    			// Firmware Version was deleted
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        "Exception occurred, Firmware Version was deleted", e); //$NON-NLS-1$
        		this.progressContainer.setUncommittedError(e);            			
        	}
        	catch (UnsupportedVersionException uve){
        		logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        "Exception occurred, Unsupported firmware downgrade", uve); //$NON-NLS-1$
        		
        		this.progressContainer.setUncommittedError(uve);
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
            // TODO: release locks
            this.progressContainer.commit();
            this.cleanup();
            device.licenseAccepted = false;
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
    }

//	protected boolean affectsFirmware() {
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
