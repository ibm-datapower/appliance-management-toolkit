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
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.logging.LoggerHelper;


/**
 * A BackgroundTask to subscribe to a specific Device. A subscription will
 * enable the device to send change Notifications to the Manager.
 * <p>
 * 
 * @version SCM ID: $Id: SubscribeTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 * <p>
 */
//* Created on Sep 13, 2006
public class SubscribeTask extends BackgroundTask {
    private Device device = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = SubscribeTask.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    SubscribeTask(Device device) {
        super();
        this.device = device;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
    
    /**
     * Get the device to which we will subscribe.
     * 
     * @return the device to which we will subscribe.
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
        String result = "SubscribeTask["; //$NON-NLS-1$
        result += "device=" + this.device; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }
    
    protected int estimateSteps() {
        int steps = 1;
        return(steps);
    }

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        try {
            try {
                this.device.subscribe(this.progressContainer);
                
                // there is no lock, so we can commit the result/error immediately
                this.progressContainer.setComplete();
                
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "deleted: " + e); //$NON-NLS-1$
                this.progressContainer.setError(e);
            } catch (AMPException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,  
                        Messages.getString("wamt.clientAPI.SubscribeTask.probSubscribing",device.getDisplayName()), e);
                this.progressContainer.setError(e);
            } catch (SubscriptionToAnotherManagerException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.SubscribeTask.subToAnotherMan",this.device.getDisplayName()), e);
                //this.progressContainer.setError(e);
                device.handleSubscriptionToAnotherManager(progressContainer);
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
