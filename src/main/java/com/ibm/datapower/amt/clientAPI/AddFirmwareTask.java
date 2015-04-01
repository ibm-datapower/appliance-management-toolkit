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

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.Utilities;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to add a new firmware file to the repository. As part of this "add",
 * an encoding will take place to put the firmware file in Base64 format.
 * Because this encoding can take several seconds given that the file is several
 * MB in size, this must be done in a BackgroundTask because it is somewhat
 * indeterminate in length and also is too long to run on the user interface
 * thread. The firmware file is stored in Base64 format in the repository so
 * that it needs to be encoded only on the initial repository add instead of
 * each time that it is transmitted to a device, which results in a significant
 * CPU and memory savings.
 * <p>
 * <p>
 * @version SCM ID: $Id: AddFirmwareTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
public class AddFirmwareTask extends BackgroundTask {
    private Blob image = null;
    private String userComment = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = AddFirmwareTask.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    AddFirmwareTask(Blob image, String userComment) {
        super();
        this.image = image;
        this.userComment = userComment;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
    
    /**
     * Get the user comment associated with this firmware file in this
     * BackgroundTask. This user comment will be stored in the repository to
     * annotate this firmware file. This user comment was set as a parameter to
     * the constructor. This gettr method is available to get metadata about
     * this BackgroundTask.
     * 
     * @return the user comment associated with this firmware file
     */
    public String getUserComment() {
        return(this.userComment);
    }
    
    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "AddFirmwareTask["; //$NON-NLS-1$
        result += "image=" + this.image; //$NON-NLS-1$
        result += ", userComment=" + this.userComment; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    protected int estimateSteps() {
        int totalSteps = 0;
        totalSteps += 1;  // parse headers
        totalSteps += 1;  // StoredFirmwareVersion constructor and base64 encode
        return(totalSteps);
    }

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, 
                new Object[] {this.image, this.userComment});
        
        FirmwareVersion result = null;
        Manager manager = Manager.internalGetInstance();
        try {

            Firmware.lockWait();
            try {
                this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.AddFirmwareTask.parsingFw_txt"); //$NON-NLS-1$
                String scryptVersion = Utilities.getFirmwareScryptVersion(image);
                DeviceType deviceType = Utilities.getFirmwareDeviceType(image);
                ModelType modelType = Utilities.getFirmwareModelType(image);
                StringCollection strictFeatures = Utilities.getStrictFirmwareFeatures(image);
                StringCollection nonstrictFeatures = Utilities.getNonStrictFirmwareFeatures(image);
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "probed firmware blob and found" + //$NON-NLS-1$
                        " scrypt version " + scryptVersion + //$NON-NLS-1$
                        " and deviceType " + deviceType + //$NON-NLS-1$
                        " and modelType " + modelType + //$NON-NLS-1$
                        " and strict features " + strictFeatures + //$NON-NLS-1$
                        " and non-strict features " + nonstrictFeatures); //$NON-NLS-1$
                // check to see if this is an old firmware file without the headers we require.
                // scrypt version 1.0 is not supported, only 2.0
                if ((deviceType == null) || 
                		(modelType == null) || 
                		(strictFeatures == null) || 
                		(nonstrictFeatures == null) ||
                		(scryptVersion == null) ||
                		(scryptVersion.equals("1.0"))) { //$NON-NLS-1$
                    String message = Messages.getString("wamt.clientAPI.AddFirmwareTask.notEnufInfoInHeader", image); //$NON-NLS-1$
                    InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.AddFirmwareTask.notEnufInfoInHeader", image);
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                    throw(e);
                }
                
                // get the Firmware object that this FirmwareVersion will be created under
                Firmware firmware = manager.getFirmware(deviceType, modelType, 
                        strictFeatures, nonstrictFeatures);
                if (firmware == null) {
                    // create new
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "did not find existing Firmware, creating a new one"); //$NON-NLS-1$
                    firmware = new Firmware(deviceType, modelType, 
                            strictFeatures, nonstrictFeatures);
                } else {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "found existing firmware: " + firmware); //$NON-NLS-1$
                }

                // create the FirmwareVersion
                this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.AddFirmwareTask.encodingFw_txt"); //$NON-NLS-1$
                result = new FirmwareVersion(firmware, new Date(), userComment, image);
                
            } finally {
                Firmware.unlock();
            }
            
            manager.save(Manager.SAVE_UNFORCED);
            // lock is already released, so we can commit the result/error immediately
            this.progressContainer.setComplete(result);
            
        } catch (DMgrException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "An exception occurred.", e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (IOException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "An I/O occurred.", e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (Exception e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "An unchecked error occurred.", e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } finally {
            this.cleanup();
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
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
