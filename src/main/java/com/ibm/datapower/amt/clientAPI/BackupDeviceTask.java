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

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to do a full back up of a DP device.  
 * <p>
 * <p>
 * @version SCM ID: $Id: BackupDeviceTask.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
 * 
 */
// * Created on April 16 , 2010
public class BackupDeviceTask extends BackgroundTask {
	
	public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = BackupDeviceTask.class.getName();
    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {   
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName()); //$NON-NLS-1$
    }
    
	Device device = null;
	URLSource certificateLocation = null;
	URI  backupFileLocation = null;
	String certificateObjectName = null;
	// These values are ON by default
	boolean includeISCSI = true;
	boolean includeRaid = true;
    
	BackupDeviceTask(Device targetDevice, String certObjectName, URLSource certificateLocation, URI backupFileLocation,
			boolean includeISCSIsetting, boolean includeRaidsetting) {
		super();
		device = targetDevice;
		this.backupFileLocation = backupFileLocation;
		this.certificateLocation = certificateLocation;
		this.progressContainer.setTotalSteps(this.estimateSteps());
		this.certificateObjectName = certObjectName;
		this.includeISCSI = includeISCSIsetting;
		this.includeRaid = includeRaidsetting;    	  
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
        //result += ", firmwareVersion=" + this.firmwareVersion.getAbsoluteDisplayName(); //$NON-NLS-1$
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

        try {
            
            try {

            	// backup the device, ? need to set the key?
                device.backUpDeviceAction(this.certificateObjectName, certificateLocation, backupFileLocation, includeISCSI, includeRaid);
                    		
                
                // need to release lock before making update visible, so commit later
                this.progressContainer.setUncommittedComplete();

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
