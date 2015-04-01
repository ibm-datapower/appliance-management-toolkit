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
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.logging.LoggerHelper;


/**
 * A task to unsubscribe from a device.
 * <p>
 * The device is identified via a DeviceContext object instead of a Device
 * object, because there are scenarios where the Device object is deleted from
 * the Manager immediately after this task is created, and we don't want a race
 * condition of referencing the Device object that may have already been deleted
 * from peristence, which could cause us to lose access to the information
 * needed to issue the AMP command.
 * <p>
 * @version SCM ID: $Id: UnsubscribeTask.java,v 1.4 2011/02/24 16:31:16 jcates Exp $
 */
class UnsubscribeTask extends BackgroundTask {
    private DeviceContext deviceContext = null;
    private Device device = null;
    boolean removOnly = false;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = UnsubscribeTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    UnsubscribeTask(Device device, boolean removeOnly) {
        super();
        this.device = device;
        //this.deviceContext = device.getDeviceContext();
        this.progressContainer.setTotalSteps(this.estimateSteps());
        this.removOnly = removeOnly;
    }
    
    /**
     * Get the DeviceContext which described how to connect to the device.
     * 
     * @return the DeviceContext which described how to connect to the device.
     */
    public DeviceContext getDeviceContext() {
        return(this.deviceContext);
    }

    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "UnsubscribeTask["; //$NON-NLS-1$
        result += "deviceContext=" + this.deviceContext; //$NON-NLS-1$
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
        String hostName = "(unknown)";

        try {
            device.lockNoWait();
            
        	this.deviceContext = device.getDeviceContext();
        	hostName = this.deviceContext.getHostname();
        	
        	if (!this.removOnly && !DeviceType.XC10.equals(device.getDeviceType())) {
        		//defect #9986
        		try {
        			device.unsubscribe(this.progressContainer);
        		} catch (DeviceExecutionException e) {
        			// log it and continue
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                            Messages.getString("ProbExecComm",hostName), e); //$NON-NLS-1$
        		}
        	}
            
            // set synch mode on any existing domains to MANUAL.
            device.setSynchronizationModeForManagedDomains(DomainSynchronizationMode.MANUAL);    
            
            // managed set may be null, if the device has been removed from the managed set            
            ManagedSet ms = device.getManagedSet();
            
            // remove it from the managedSet
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "remove from persistence"); //$NON-NLS-1$
            StoredDevice storedDevice = device.getStoredInstance();
            if (ms != null){
                ms.getStoredInstance().remove(storedDevice);
            }            
            Manager manager = Manager.internalGetInstance();
            manager.save(Manager.SAVE_UNFORCED);
            
            // remove the device's Notification queue            
            if ((ms != null) && (ms.notificationQueue != null)){
            	ms.notificationQueue.clean(ms);  
            }
            
            // there is no lock, so we can commit the result/error immediately
            this.progressContainer.setComplete();

        } catch (InvalidCredentialsException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("ProbCommDev",hostName), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (DeviceExecutionException e) {
            // log it and continue
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("ProbExecComm",hostName), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (AMPIOException e) {
            // log it and continue
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("ProbCommDev",hostName), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (AMPException e) {
            // log it and continue
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("ProbCommDev",hostName), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (LockBusyException e) {
            // log it and continue
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("ProbCommDev",hostName), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        }catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
            
        } finally {
            this.cleanup();
            device.unlock();
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
