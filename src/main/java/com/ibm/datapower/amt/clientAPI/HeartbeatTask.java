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
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DeviceMetaInfo;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.PingResponse;
import com.ibm.datapower.amt.amp.SubscriptionState;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Implements a Runnable object for executing heartbeats. It has the following
 * logic:
 * <ul>
 * <li>get DeviceMetaInfo
 * <li>check serial number to see if it has changed; if so, this is a big
 * error; if not, proceed
 * <li>check device type to see if it has changed; if so, this is a big error;
 * if not, proceed
 * <li>check firmware level to see if it has changed; if so, this is a
 * significant error; if not, proceed
 * <li>get pingResponse
 * <li>check returned subscription id to see if it has changed; if so, we need
 * to resync the device with the local datastore; if not,
 * proceed
 * <li>get DomainList from device
 * </ul>
 * 
 * @version $Id: HeartbeatTask.java,v 1.6 2011/04/14 16:24:24 lsivakumxci Exp $
 *  
 */
public class HeartbeatTask implements Runnable, Task {
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final String CLASS_NAME = HeartbeatTask.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    private Manager manager = null;
    private Commands commands = null;
    private Device device = null;
    private String subscriptionID = null;
    private MacroProgressContainer macroProgressContainer = null;
    
    /**
     * Constructor for HeartbeatTask. These tasks sit in a HeartbeatQueue, to be executed by a thread
     * from a thread pool which is managed by the HeartbeatQueue object. 
     * 
     * @param device The device we want to send a heartbeat to
     * @param subscriptionID The current subscription id, as set by the Manager instance
     * @throws AMPException The commands library or Manager instance could not be loaded.
     */
    public HeartbeatTask(Device device, String subscriptionID) throws AMPException{
        this.manager = Manager.internalGetInstance();
        this.commands = device.getCommands();
        this.device = device;
        this.subscriptionID = subscriptionID;
        this.macroProgressContainer = new MacroProgressContainer(this);
    }
    
    /**
     * Retrieves the Device object representing the appliance whose "heartbeat" we wish to check
     * @return The Device object representing the appliance whose "heartbeat" we wish to check
     */
    public Device getDevice() {
        return(this.device);
    }
    
    /**
     * Obligatory toString implementation for logging purposes
     * @return The string representation of this heartbeat task
     */
    public String toString() {
        String result = "HeartbeatTask["; //$NON-NLS-1$
        result += "device=" + this.device; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    private void saveToRepository(Manager manager){
        
        final String METHOD_NAME = "saveToRepository"; //$NON-NLS-1$
        // fix for 59079
        try{
            manager.save(Manager.SAVE_UNFORCED);
        }
        catch(DirtySaveException e1){
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "The heartbeat thread was unable to save changes to the repository, " + //$NON-NLS-1$
                    "since there were other outstanding changes that could not be saved.", e1); //$NON-NLS-1$
        }
        catch(DatastoreException e2){
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "The heartbeat thread was unable to save changes to the repository " + //$NON-NLS-1$
                    "due to an error with the datastore.", e2); //$NON-NLS-1$
        }
    }

    /**
     * The core logic of the heartbeat mechanism resides in this method. 
     * 
     * It has the following
     * logic:
     * <ul>
     * <li>get DeviceMetaInfo
     * <li>check serial number to see if it has changed; if so, this is a big
     * error; if not, proceed
     * <li>check device type to see if it has changed; if so, this is a big error;
     * if not, proceed
     * <li>check firmware level to see if it has changed; if so, this is a
     * significant error; if not, proceed
     * <li>get pingResponse
     * <li>get DomainList from device
     * </ul>
     * 
     */
    public void run(){
        
        final String METHOD_NAME = "run (Thread " + Thread.currentThread().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        ProgressContainer getDeviceMetaInfoProgressContainer = new ProgressContainer(this);
        ProgressContainer pingProgressContainer = new ProgressContainer(this);
        
        try{
            manager.addHeartbeatProgress(this.macroProgressContainer);
            
            // figure out whether this device is managed or not
            boolean isManaged = false;
            
            // get status objects for firmware and domains
            ManagementStatus firmwareManagementStatus = null;
            ManagementStatus deviceManagementStatus = null;
//            ManagementStatus domainStatus = null;
            
            ManagedSet managedSet = device.getManagedSet();
            if (managedSet != null) {
                isManaged = true;
            }
            
            // if the device is in the middle of reboot, it may not respond to AMP requests.
            // So skip the heartbeat processing, and try again at the next timer pop.
            deviceManagementStatus = device.getManagementStatusOfDevice();
            if ((deviceManagementStatus != null) && 
                    (deviceManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {
                String message = "The managed device " + device.getDisplayName() +  //$NON-NLS-1$
                	" is in progress of being rebooted. Will skip this heartbeat check."; //$NON-NLS-1$
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
                macroProgressContainer.setComplete();
                logger.exiting(CLASS_NAME, METHOD_NAME);                
                return;
            }

            // if the device is in the middle of a firmware upgrade, it may not respond to AMP requests.
            // So skip the heartbeat processing, and try again at the next timer pop.
            firmwareManagementStatus = device.getManagementStatusOfFirmware();
            if ((firmwareManagementStatus != null) && 
                    (firmwareManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {
                String message = "The firmware for managed device " + device.getDisplayName() +  //$NON-NLS-1$
                	" is in progress of being updated. Will skip this heartbeat check."; //$NON-NLS-1$
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
                macroProgressContainer.setComplete();
                //fix for 59079
                //saveToRepository(manager);
                logger.exiting(CLASS_NAME, METHOD_NAME);
                
                return;
            }

            // Both managed and unmanaged devices need to be checked to verify serial number and device type
            
            // since the getDeviceMetaInfo step is implemented here without an 
            // embedded ProgressContainer, create a nested ProgressContainer here
            
            getDeviceMetaInfoProgressContainer.setTotalSteps(1);
            macroProgressContainer.addNested(getDeviceMetaInfoProgressContainer);
            
            getDeviceMetaInfoProgressContainer.incrementCurrentStep(1, "wamt.clientAPI.HeartbeatTask.chkDevMetaData_txt"); //$NON-NLS-1$
            DeviceMetaInfo deviceMetaInfo = commands.getDeviceMetaInfo(device.getDeviceContext());
            getDeviceMetaInfoProgressContainer.setComplete();
            
            device.isDeviceReachable = true;
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "Serial Number in repository = " + device.getSerialNumber() + "; Serial Number returned from device = " + deviceMetaInfo.getSerialNumber()); //$NON-NLS-1$ //$NON-NLS-2$
            if (!device.getSerialNumber().equalsIgnoreCase(deviceMetaInfo.getSerialNumber())){
                // serial number doesnt match; let's log this severe error (noting that the device should be deleted and recreated)
                // Also, lets set the device as unreachable in its status. 
                
                firmwareManagementStatus = device.getManagementStatusOfFirmware();
                if (firmwareManagementStatus != null)
                    firmwareManagementStatus.setStatus(ManagementStatus.Enumerated.UNREACHABLE);
                else {
                    // firmwareManagementStatus shouldnt be null - log this severe error
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "The managagement status of the firmware for the device " + device.getSymbolicName() + " are not defined"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                
//                // Domain management status not currently supported 
//                if (isManaged == true){
//                    Domain[] domainArray = device.getManagedDomains();
//                    for (int i = 0; i < domainArray.length; i++){
//                        domainStatus = device.getManagementStatusOfDomain(domainArray[i]);
//                        if (domainStatus != null){
//                            domainStatus.setStatus(ManagementStatus.Enumerated.UNREACHABLE);
//                        }
//                        else {
//                            // domainStatus should not be null - log this severe ERROR
//                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
//                                    "The management status of the managed domain \"" + domainArray[i].getName() + "\" for the device " +  //$NON-NLS-1$ //$NON-NLS-2$
//                                    device.getSymbolicName() + " are not defined"); //$NON-NLS-1$
//                        }
//                    }
//                }
                // If Device status is unreachable, set the domain status to UNKNOWN
                setDomainStatusToUnknown();                
                
                // log a message that the serial numbers didnt match, and that the device entry must be deleted
                // and recreated before proceeding.
                Object[] args = new Object[] {device.getHostname(), Integer.toString(device.getHLMPort()), device.getSymbolicName()};
                String message = Messages.getString("wamt.clientAPI.HeartbeatTask.serialNumberMismatch", args); //$NON-NLS-1$
 
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
                Exception e = new Exception(message);
                macroProgressContainer.setError(e);
                //fix for 59079
                //saveToRepository(manager);
                logger.exiting(CLASS_NAME, METHOD_NAME);
                
                return;
            }
            //serial number matches, proceed
            
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "Device Type in repository = " + device.getDeviceType().toString() + "; Device Type returned from device = " + deviceMetaInfo.getDeviceType().toString()); //$NON-NLS-1$ //$NON-NLS-2$
            if (!device.getDeviceType().equals(deviceMetaInfo.getDeviceType())){
                // device type doesnt match; lets log this sever error
                // Also, lets set the device as unreachable in its status. 
                
                firmwareManagementStatus = device.getManagementStatusOfFirmware();
                if (firmwareManagementStatus != null)
                    firmwareManagementStatus.setStatus(ManagementStatus.Enumerated.UNREACHABLE);
                else {
                    // firmwareManagementStatus shouldnt be null - log this severe error
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "The managagement status of the firmware for the device " + device.getSymbolicName() + " are not defined"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                
                // If Device status is unreachable, set the status of managed domains to UNKNOWN
                setDomainStatusToUnknown(); 
//                if (isManaged == true){
//                    Domain[] domainArray = device.getManagedDomains();
//                    for (int i = 0; i < domainArray.length; i++){
//                        domainStatus = device.getManagementStatusOfDomain(domainArray[i]);
//                        if (domainStatus != null){
//                            domainStatus.setStatus(ManagementStatus.Enumerated.UNREACHABLE);
//                        }
//                        else {
//                            // domainStatus should not be null - log this FINER ERROR
//                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
//                                    "The managagement status of the managed domain \"" + domainArray[i].getName() + "\" for the device " +  //$NON-NLS-1$ //$NON-NLS-2$
//                                    device.getSymbolicName() + " are not defined"); //$NON-NLS-1$
//                        }
//                    }
//                }
                
                // log a message that the serial numbers didnt match, and that the device entry must be deleted
                // and recreated before proceeding.
                Object[] args = new Object[] {device.getHostname(), Integer.valueOf(device.getHLMPort()), device.getSymbolicName()};
                String message =
                    Messages.getString("wamt.clientAPI.HeartbeatTask.devTypMismatch", args); //$NON-NLS-1$
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
                Exception e = new Exception(message);
                
                macroProgressContainer.setError(e);
                //fix for 59079
                //saveToRepository(manager);
                logger.exiting(CLASS_NAME, METHOD_NAME);
                
                return;
            }
            // all meta info we check match (serial number, firmware rev, device type); 
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "The firmware level running on device \"" + device.getSymbolicName() + "\" is " //$NON-NLS-1$ //$NON-NLS-2$
                    + deviceMetaInfo.getFirmwareLevel());
            // update the value in the device POJO to what is currently on the device.
            device.setActualFirmwareLevel(deviceMetaInfo.getFirmwareLevel());
            
            // check that the firmware meets the minimum level
            Object[] args = new Object[] {device.getDisplayName(), MinimumFirmwareLevel.MINIMUM_LEVEL };
            // Do not check for XC10 devices
            if (!DeviceType.XC10.equals(device.getDeviceType()) &&!FirmwareVersion.meetsMinimumLevel(device.getActualFirmwareLevel())) {
                String message = Messages.getString("wamt.clientAPI.HeartbeatTask.fwLevelDoesNotMatch", args); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                try {
                    manager.forceRemove(device);
                    UnsupportedVersionException e = new UnsupportedVersionException(message,"wamt.clientAPI.HeartbeatTask.fwLevelDoesNotMatch", args);
                    macroProgressContainer.setError(e);
                } catch (DMgrException e) {
                    macroProgressContainer.setError(e);
                }
                //fix for 59079
                // this is the one place where the HeartbeatTask can make a change to the repository
                saveToRepository(manager);
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            
            //firmware level matches, continue.  Set firmwareManagmentStatus
            
            firmwareManagementStatus = device.getManagementStatusOfFirmware();
            if (firmwareManagementStatus != null)
                firmwareManagementStatus.setStatus(ManagementStatus.Enumerated.SYNCED);
            else {
                // firmwareManagementStatus shouldnt be null - log this severe error
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "The managagement status of the firmware for the device " + device.getSymbolicName() + " are not defined"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            //ping response and set of domain operational status is OK, continue.  
            StringCollection domainList = new StringCollection(); 
            if(!DeviceType.XC10.equals(device.getDeviceType())){
            	String[] listOfDomains = commands.getDomainList(device.getDeviceContext());
            
            	// for all devices, lets just update the domain list
            	domainList = new StringCollection(listOfDomains);
            	device.setAllDomainNames(domainList);
            }
            
            if (isManaged == true){
                // we only want to ping managed devices
                pingProgressContainer.setTotalSteps(1);
                macroProgressContainer.addNested(pingProgressContainer);

                Commands commands = null;
                
                if(DeviceType.XC10.equals(device.getDeviceType())){
                	commands = device.getXC10Commands();
                }else{
                	commands = device.getV1Commands();	
               	}
                
                pingProgressContainer.incrementCurrentStep(1, "wamt.clientAPI.HeartbeatTask.chkDevMetaData_txt"); //$NON-NLS-1$
                PingResponse pingResponse = commands.pingDevice(device.getDeviceContext(),subscriptionID);
                pingProgressContainer.setComplete();
                
                if(!DeviceType.XC10.equals(device.getDeviceType())){
                	//check state of subscription on device
                	if (pingResponse.getSubscriptionState() != SubscriptionState.ACTIVE){
                		// subscription is not active; lets sync the device and log it
                		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "The ping subscription is out of sync for device \"" + device.getSymbolicName()  //$NON-NLS-1$
                            + "\"; issuing subscribe command"); //$NON-NLS-1$
                    
                		// regain the lost subscription
                		try {
                			device.subscribe(null);
                		} catch (SubscriptionToAnotherManagerException e) {
                			// since we are doing subscription when it's not active
                			// it's not possible it's subscribed by another manager
                			// so just eat the exception here
                		}
                    
                    	macroProgressContainer.setComplete();
                    	//fix for 59079
                    	//saveToRepository(manager);
                    	logger.exiting(CLASS_NAME, METHOD_NAME);
                	} else {
                		// subscription is active
                	}
                	// update operational status for all managed domains in a managed device

                	Domain[] domainArray = device.getManagedDomains();
                	for (int i = 0; i < domainArray.length; i++){

                		ProgressContainer probeDomainProgressContainer = new ProgressContainer(this);                    	
                		probeDomainProgressContainer.setTotalSteps(1);
                		macroProgressContainer.addNested(probeDomainProgressContainer);
                		args = new Object[] {domainArray[i].getAbsoluteDisplayName(), this.device.getDisplayName()};                   
                		probeDomainProgressContainer.incrementCurrentStep(1, "wamt.clientAPI.GetDomainsOperationStatusTask.probingStat_txt", args); //$NON-NLS-1$
                    	try{
                    		// Not only update the domain status, but also for service status
                    		if( domainList.contains(domainArray[i].getName()) ){
                    			domainArray[i].refresh();
                			}
                    		 
                    	}catch(Exception e){
                    		probeDomainProgressContainer.setError(e);
                    	}
                    	probeDomainProgressContainer.setComplete();
                	}
                }
            }
            
                                   
            macroProgressContainer.setComplete();
            //fix for 59079
            //saveToRepository(manager);
            logger.exiting(CLASS_NAME, METHOD_NAME);
            return;
            
        }
        catch (DeletedException e){
            // This means that the manager itself has been deleted, or a managed set or device has been deleted since we started.
            // Lets just log this, and punt, and hope that the state has settled by the time the next heartbeat is scheduled for
            // the device.
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "A defined resource (managed set or device) has been deleted and therefore the heartbeat cannot be completed",e); //$NON-NLS-1$
            if (getDeviceMetaInfoProgressContainer != null){
                if (!getDeviceMetaInfoProgressContainer.isComplete()){
                    getDeviceMetaInfoProgressContainer.setError(e);
                }
            }
            else if (pingProgressContainer != null){
                if (!pingProgressContainer.isComplete()){
                    pingProgressContainer.setError(e);
                }
            }
            else {
                macroProgressContainer.setError(e);
            }
        }
        catch (AMPIOException e){
            // the device could not be reached due to some IO error. Set all device status to UNREACHABLE, log, and quit.
            try{
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "The device " + device.getSymbolicName() + " could not be reached to complete a heartbeat.",e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            catch (DeletedException ex){
                //do nothing?
            }
            
            device.isDeviceReachable = false;
            
            ManagementStatus status = device.getManagementStatusOfFirmware();
            if (status != null)
                status.setStatus(ManagementStatus.Enumerated.UNREACHABLE);
            else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "The management status of the firmware was null",e); //$NON-NLS-1$
            }
            
            this.setDomainStatusToUnknown();
//            try{
//                ManagedSet managedSet = device.getManagedSet();
//                
//                if (managedSet == null){
//                    //this device is not managed; skip this processing
//                    //see bug 57804 in cs tracker
//                    
//                    //fix for 59079
//                    //saveToRepository(manager);
//                    
//                }
//                else {
//                    Domain[] domainArray = device.getManagedDomains();
//                    
//                    for (int i = 0; i < domainArray.length; i++){
//                        device.setManagementStatusOfDomain(domainArray[i],ManagementStatus.Enumerated.UNREACHABLE);
//                    }
//                }
//            }
//            catch (DeletedException ex){
//                String message = Messages.getString("wamt.clientAPI.HeartbeatTask.resDeleted");  //$NON-NLS-1$
//                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, ex);
//            }
//            finally{
                //fix for 59079 
                //saveToRepository(manager);
                
                if (getDeviceMetaInfoProgressContainer != null){
                    if (!getDeviceMetaInfoProgressContainer.isComplete()){
                        getDeviceMetaInfoProgressContainer.setError(e);
                    }
                }
                else if (pingProgressContainer != null){
                    if (!pingProgressContainer.isComplete()){
                        pingProgressContainer.setError(e);
                    }
                }
                else {
                    macroProgressContainer.setError(e);
                }
                
//            }
        }
        catch (InvalidCredentialsException e){
            // the device could not be reached due to some IO error. Set all device status to UNREACHABLE, log, and quit.
            try{
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "The credentials specified for device " + device.getSymbolicName() + " are incorrect",e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            catch (DeletedException ex){
                //do nothing?
            }
            
            device.isDeviceReachable = false;
            
            device.setManagementStatusOfFirmware(ManagementStatus.Enumerated.ERROR);
            
            this.setDomainStatusToUnknown();
//            try{
//                ManagedSet managedSet = device.getManagedSet();
//                
//                if (managedSet != null){
//                    Domain[] domainArray = device.getManagedDomains();
//                    
//                    for (int i = 0; i < domainArray.length; i++){
//                        device.setManagementStatusOfDomain(domainArray[i],ManagementStatus.Enumerated.ERROR);
//                    }
//                }
//            }
//            catch (DeletedException ex){
//                String message = Messages.getString("wamt.clientAPI.HeartbeatTask.resDeleted");  //$NON-NLS-1$
//                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, ex);
//            }
//            finally{
                //fix for 59079 
                //saveToRepository(manager);
                
                if (getDeviceMetaInfoProgressContainer != null){
                    if (!getDeviceMetaInfoProgressContainer.isComplete()){
                        getDeviceMetaInfoProgressContainer.setError(e);
                    }
                }
                else if (pingProgressContainer != null){
                    if (!pingProgressContainer.isComplete()){
                        pingProgressContainer.setError(e);
                    }
                }
                else {
                    macroProgressContainer.setError(e);
                }
//            }
        }
        catch (DeviceExecutionException e){
            // the device could not be reached due to some IO error - log, and quit.
            try{
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "There was an error executing the hearbeat on device " + device.getSymbolicName(),e); //$NON-NLS-1$
            }
            catch (DeletedException ex){
                //do nothing?
            }
            
            device.isDeviceReachable = false;
            
            device.setManagementStatusOfFirmware(ManagementStatus.Enumerated.ERROR);

            this.setDomainStatusToUnknown();
            
//            try{
//                ManagedSet managedSet = device.getManagedSet();
//                
//                if (managedSet != null){
//                    Domain[] domainArray = device.getManagedDomains();
//                    
//                    for (int i = 0; i < domainArray.length; i++){
//                        device.setManagementStatusOfDomain(domainArray[i],ManagementStatus.Enumerated.ERROR);
//                    }
//                }
//            }
//            catch (DeletedException ex){
//                String message = Messages.getString("wamt.clientAPI.HeartbeatTask.resDeleted");  //$NON-NLS-1$
//                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, ex);
//            }
//            finally{
                //fix for 59079 
                //saveToRepository(manager);
                
                if (getDeviceMetaInfoProgressContainer != null){
                    if (!getDeviceMetaInfoProgressContainer.isComplete()){
                        getDeviceMetaInfoProgressContainer.setError(e);
                    }
                }
                else if (pingProgressContainer != null){
                    if (!pingProgressContainer.isComplete()){
                        pingProgressContainer.setError(e);
                    }
                }
                else {
                    macroProgressContainer.setError(e);
                }
//            }
        }
        catch (AMPException e){
            String message = "An error occured during the execution of AMP commands"; //$NON-NLS-1$
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message, e);
            
            device.isDeviceReachable = false;
            
            this.setDomainStatusToUnknown();
            
            if (getDeviceMetaInfoProgressContainer != null){
                if (!getDeviceMetaInfoProgressContainer.isComplete()){
                    getDeviceMetaInfoProgressContainer.setError(e);
                }
            }
            else if (pingProgressContainer != null){
                if (!pingProgressContainer.isComplete()){
                    pingProgressContainer.setError(e);
                }
            }
            else {
                macroProgressContainer.setError(e);
            }
        }              
    }
       
    void setDomainStatusToUnknown() {
    	// If Device status is unreachable, set the domain operational status to UNKNOWN
    	final String METHOD_NAME = "setDomainStatusToUnknown"; 	
    	try{
    		boolean isManaged = false;
            ManagedSet managedSet = device.getManagedSet();
            if (managedSet != null) {
                isManaged = true;
            }
    		if (isManaged == true){
    			Domain[] domainArray = device.getManagedDomains();
    			for (int i = 0; i < domainArray.length; i++){
    				DomainStatus domainStatus = device.getDomainStatus(domainArray[i]);
    				if (domainStatus != null ){
    					domainStatus.setStatus(DomainStatus.UNKNOWN_DOMAIN_STATUS);
    				}
    				else {
    					// domainStatus should not be null - log this severe ERROR
    					logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
    							"The status of the managed domain \"" + domainArray[i].getName() + "\" for the device " +  //$NON-NLS-1$ //$NON-NLS-2$
    							device.getSymbolicName() + " are not defined"); //$NON-NLS-1$
    				}
    			}
    		}                
    	}catch (DeletedException ex){    		
    		String message = "The Domain, ManagedSet or Device has been deleted:";  //$NON-NLS-1$
    		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, ex);
	    }catch (InvalidParameterException ipe){    		
			String message = "The invalid domain:";  //$NON-NLS-1$
			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, ipe);
		}
    }    
}
