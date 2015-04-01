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
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to probe the device to gather the required details (especially the
 * hardware serial number and device type) before a <code>Device</code> object
 * can be constructed.
 * <p>
 * @version SCM ID: $Id: NewDeviceTask.java,v 1.6 2011/04/06 18:20:03 bschreib Exp $
 */
public class NewDeviceTask extends BackgroundTask {
    private String symbolicName = null;
    private String hostname = null;
    private String userid = null;
    private String password = null;
    private int hlmPort = 0;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = NewDeviceTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    NewDeviceTask(String symbolicName, String hostname, 
                  String userid, String password, int hlmPort) {
        super();
        this.symbolicName = symbolicName;
        this.hostname = hostname;
        this.userid = userid;
        this.password = password;
        this.hlmPort = hlmPort;
        this.progressContainer.setTotalSteps(this.estimateSteps());
        // device is not yet in a managed set, so no affected* members need to be set
    }
    
    /**
     * Get the symbolic name of the device that will be passed to the Device
     * constructor.
     * 
     * @return the symbolic name of the device that will be passed to the Device
     *         constructor.
     */
    public String getSymbolicName() {
        return(this.symbolicName);
    }
    
    /**
     * Get the hostname/IPaddress of the device. This will be passed to the
     * Device constructor.
     * 
     * @return the hostname/IPaddress of the device.
     */
    public String getHostname() {
        return(this.hostname);
    }

    /**
     * Get the administrative userid of the device. This will be used to connect
     * to the device and will be passed to the Device constructor.
     * 
     * @return the administrative userid of the device.
     */
    public String getUserid() {
        return(this.userid);
    }

    /**
     * Get the password for the administrative userid of the device. This will
     * be used to connect to the device and will be passed to the Device
     * constructor.
     * 
     * @return the password for the administrative userid of the device.
     */
    public String getPassword() {
        return(this.password);
    }

    /**
     * Get the device's port number that provides the AMP endpoint of the XML
     * Management Interface. (AMP is sometimes known as HLM: High-Level
     * Management). This will be used to connect to the device and will be
     * passed to the Device constructor.
     * 
     * @return the device's port number that provides the AMP endpoint.
     */
    public int getHlmPort() {
        return(this.hlmPort);
    }
    
    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "NewDeviceTask["; //$NON-NLS-1$
        result += "symbolicName=" + this.symbolicName; //$NON-NLS-1$
        result += ", hostname=" + this.hostname; //$NON-NLS-1$
        result += ", userid=" + this.userid; //$NON-NLS-1$
        result += ", length(password)=" + this.password.length(); //$NON-NLS-1$
        result += ", hlmPort=" + this.hlmPort; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    protected int estimateSteps() {
        // just one AMP command to the device
        return(1);
    }
    
    protected void execute() {
        // device starts as unmanaged, so don't need to set its ManagementStatus
        // or OperationStatus objects.
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, hostname);
        this.progressContainer.incrementCurrentStep(0, "wamt.clientAPI.NewDeviceTask.starting_txt"); //$NON-NLS-1$
        Device device = null;
        Manager manager = Manager.internalGetInstance();
        manager.unmanagedDevicesLockWait();
        try {
            
            try {
                // validate the host name
                new URI("http",this.hostname, null, null);               
            	            	
                device = new Device(this.symbolicName,
                                    this.hostname, 
                                    this.userid, this.password,
                                    this.hlmPort, this.progressContainer);            		
               
                manager.save(Manager.SAVE_UNFORCED);
                
                // need to release lock before making update visible, so commit later
                this.progressContainer.setUncommittedComplete(device);

            } catch (UnsupportedVersionException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        	Messages.getString("wamt.clientAPI.NewDeviceTask.devCannotBeAdded"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            } catch (DeletedException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                            Messages.getString("wamt.clientAPI.NewDeviceTask.delEx"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            } catch (AMPException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.NewDeviceTask.devCommEx"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            } catch (DatastoreException e) {
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.NewDeviceTask.probWithDataStore"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            } catch (InvalidDeviceMetaInfoException e) {
                this.progressContainer.setUncommittedError(e);
            } catch (URISyntaxException e) {
            	String message =  Messages.getString("wamt.clientAPI.NewDeviceTask.invalidHost"); 
            	URISyntaxException missingHost = new URISyntaxException(this.hostname, message);
            	message += ": " + this.hostname;
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message, missingHost); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(missingHost);            	
  
            } catch (Exception e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UncheckedException"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            }
            
        } finally {
            manager.unmanagedDevicesUnlock();
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
