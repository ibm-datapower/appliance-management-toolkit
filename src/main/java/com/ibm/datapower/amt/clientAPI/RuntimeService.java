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

import com.ibm.datapower.amt.AdminStatus;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.OpStatus;
import com.ibm.datapower.amt.QuiesceStatus;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.ConfigObject;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.NotExistException;
import com.ibm.datapower.amt.amp.ReferencedObjectCollection;
import com.ibm.datapower.amt.clientAPI.util.Timeout;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * The RuntimeService is to represent the service which is created on runtime, such as the return from {@link Domain#getServices()}
 *  or {@link ServiceDeployment#getInterDependentServices()}. Some operations are provided to manipulate it.
 * 
 */
public class RuntimeService {
	private String name = "";
	private String className = "";
	private String classDisplayName = "";	
	private String userComment = "";
	
	private Domain domain = null;
	private Status status = null;
	private int quiesceTimeOut = DEFAULT_SERVICE_QUIESCE_VALUE;	
	protected boolean isDeleted = false;

	protected final static int SERVICE_QUIESCE_MIN_VALUE = 60; // seconds
	private static int DEFAULT_SERVICE_QUIESCE_VALUE;
	
	protected static final String CLASS_NAME = RuntimeService.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	
	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;
	
	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());

		Integer integer = Configuration.getAsInteger(Configuration.KEY_DOMAIN_QUIESCE_TIMEOUT);
		DEFAULT_SERVICE_QUIESCE_VALUE = integer.intValue();
	}
	
	/*
	 * Construct a new ConfigObject object with referenced object and status.
	 * @param name the name of this RuntimeService
	 * @param className the class name of this RuntimeService
	 * @param classDisplayName the display name of this RuntimeService
	 * @param userComments the user comments of this RuntimeService
	 * @param adminState the admin state of this RuntimeService
	 * @param opState the op state of this RuntimeService
	 * @param needsSave true if this object has been modified but not saved to flash, false otherwise.
	 * @param quiesceStatus quiesce Status of this RuntimeService
	 */
	public RuntimeService(String name, String className, String classDisplayName, String userComments, 
			AdminStatus adminState, OpStatus opState, boolean needsSave, QuiesceStatus quiesceStatus) {
		this.name = name;
		this.className = className;
		this.classDisplayName = classDisplayName;
		this.userComment = userComments;		
		this.status = new Status(adminState, opState, needsSave, quiesceStatus);
	}

	/*
	 * Internal used only for Domain class
	 */
	RuntimeService(Domain domain, RuntimeService rtSvc) {
		this.name = rtSvc.getName();
		this.className = rtSvc.getClassName();
		this.classDisplayName = rtSvc.getClassDisplayName();
		this.userComment = rtSvc.getUserComment();		
		
		this.status = new Status(rtSvc.getAdminStatus(), rtSvc.getOpStatus(), rtSvc.getNeedsSave(), rtSvc.getQuiesceStatus());
		if ( domain == null )
			this.isDeleted = true;
		else 
			this.domain = domain;
	}
	
	void setDomain(Domain domain) {
		this.domain = domain;
	}
		
	/**
	 * Get the name of this RuntimeService
	 * @return the String representation of this object's name
	 */
	public String getName() {
		return (this.name);
	}
	
	/**
	 * Get the class name of this RuntimeService.
	 * @return the String representation of this object's class name
	 */
	public String getClassName() {
		return (this.className);
	}
	
	/**
	 * Get the class display name of this RuntimeService.
	 * @return the String representation of this object's display name
	 */
    public String getClassDisplayName() {
        return (this.classDisplayName);
    }
    
    /**
     * Get the user comment of this RuntimeService.
     * @return the String representation of this object's user comments
     */
    public String getUserComment() {
    	return (this.userComment);
    }
	
    /**
     * 	Get the referenced objects and file list of this RuntimeService.
     * @return ReferencedObjectCollection
     */
	public ReferencedObjectCollection getReferencedObjectsAndFiles() { 	
		ReferencedObjectCollection rocRet = null;
   		// Fix 12063: [3715] Poor performance on initialization with large number of domains
       	// Try to get the refererncedObjects till it is needed.
   		if ( this.domain != null ) { // only when domain is not null 
   			String message = "Exceptiont is thrown: was unable to get the referenced objects from AMP call," +
   							 " or received an invalid response from the device";
    		try {
    			// Set referenced object	    	    	
	    		Device device;
				device = this.domain.getDevice();
				rocRet = device.getCommands().getReferencedObjects(device.getDeviceContext(), this.domain.getName(),
													this.getName(), this.getClassName());
    		} catch (DeletedException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getDevice", message); 
			} catch (NotExistException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getReferencedObjects", message);
			} catch (InvalidCredentialsException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getReferencedObjects", message);
			} catch (DeviceExecutionException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getReferencedObjects", message);
			} catch (AMPIOException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getReferencedObjects", message);			
			} catch (AMPException e) {
    			// Catch exception, just print out the warning message
    			logger.logp(Level.FINE, CLASS_NAME, "getReferencedObjects", message);
			}
   		}    	
    	return (rocRet);
	}
	
	/**
     * Get the admin state of this RumtimeService, enabled (AdminState.ENABLED) or disabled (AdminState.DISABLED).
     * @return 
     * 		AdminState.ENABLED if this object's admin state is "enabled", AdminState.DISABLED is "disabled"
     */
    public AdminStatus getAdminStatus() {    	
    	if ( this.status != null ) {
    		return (this.status.getAdminStatus());
    	}
        return AdminStatus.UNKNOWN;
    }

    /**
     * Get the operation state of this RuntimeService, up (OpState.UP) or down (OpState.DOWN) 
     * @return 
     * 		OpState.UP if this object's opState is "up", OpState.DOWN is "down"
     */
    public OpStatus getOpStatus() {
    	if ( this.status != null ) {
    		return (this.status.getOpStatus());
    	}
    	return OpStatus.UNKNOWN;
    }
    
    /**
     * Get a boolean value which indicates if the object has been modified but not saved to flash. 
     * @return 
     * 		true if this object has been modified but not saved to flash, false otherwise.
     */
    boolean getNeedsSave() {
    	if ( this.status != null ) {
    		return(this.status.getNeedsSave());
    	}
        return false;
    }

    /**
     * Get the quiesce State of this RuntimeService. 
     * @return quiesce Status of this RuntimeService
     */
    public QuiesceStatus getQuiesceStatus() {
    	if ( this.status != null )
    		return (this.status.getQuiesceStatus());
    	return QuiesceStatus.UNKNOWN;
    }

	/**
	 * Set the timeout value (in seconds) for checking the status of a service
	 * quiesce or unquiesce operation.
	 * <p>
	 * Quiesce only pertains to Firmware levels 3.8.1.0 or later. Earlier levels
	 * of firmware do not support quiesce so calling this method has no effect.
	 * Note: An exception will not be thrown if you call this method for a
	 * domain on a device that has a firmware level below 3.8.1.0 - so this
	 * value will be available if firmware is ever upgraded.
	 * <p>
	 * If a value of zero is set then the quiesce operation will be initiated on
	 * supported firmware, but the quiesce or unquiesce status will not be
	 * checked. If a nonzero value less than 60 is set, then the value will
	 * automatically be set to a minimum of 60 seconds. Values higher than 60
	 * are OK.
	 * 
	 * @param timeout
	 *            value in seconds
	 * @throws InvalidParameterException
	 */
	public void setQuiesceTimeout(int timeout)
		throws NotExistException, AlreadyExistsInRepositoryException, DatastoreException, InvalidParameterException {

		final String METHOD_NAME = "setQuiesceTimeout"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);

		if (timeout > 0 && timeout < SERVICE_QUIESCE_MIN_VALUE) {
			this.quiesceTimeOut = SERVICE_QUIESCE_MIN_VALUE;
			logger.logp( Level.WARNING, CLASS_NAME,	METHOD_NAME,
					"quiesce timeout value is nonzero and less than" + SERVICE_QUIESCE_MIN_VALUE + ", setting to " + SERVICE_QUIESCE_MIN_VALUE); //$NON-NLS-1$
		}
		else {
			this.quiesceTimeOut = timeout;
		}
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Domain save setQuiesceTimeout: " + timeout, this);
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Quiesce this RuntimeService
	 */
	public void quiesce() throws DeletedException, AMPException, UnsuccessfulOperationException, NotExistException {
		final String METHOD_NAME = "quiesce"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);
		quiesceAction(false);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Unquiesce this RuntimeService
	 */
	public void unquiesce() throws DeletedException, AMPException, UnsuccessfulOperationException, NotExistException {
		final String METHOD_NAME = "unquiesce"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);
		quiesceAction(true);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Start this RuntimeService
	 */
	public void start() throws AMPException, DeletedException, NotExistException {
		final String METHOD_NAME = "start"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);
		startAction(false);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Stop this RuntimeService
	 */
	public void stop() throws AMPException, DeletedException, NotExistException {
		final String METHOD_NAME = "stop"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);
		startAction(true);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
		
	/**
	 * Get domain of this service
	 */
	public Domain getDomain() throws NotExistException {
		final String METHOD_NAME = "getDomain"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		checkDeletedState(METHOD_NAME);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);		
		return (this.domain);
	}
	
	/**
	 * Get a boolean value which indicates if this RuntimeService has been deleted.
	 * @return boolean value to indicate if this Runtime Service is deleted, True meaning it is deleted.
	 */
	public boolean isDeleted() {
		return (this.isDeleted);
	}
	
	protected void setDeleted(boolean bDeleted) {
		this.isDeleted = bDeleted;
	}
	

	// Private Methods
	private void quiesceAction(boolean unquiesce) 
		throws AMPException, DeletedException, UnsuccessfulOperationException, NotExistException {
		final String METHOD_NAME = "quiesceAction"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { this });

		String operation = "quiesce";
		Device device = domain.getDevice();
		DeviceContext dc = device.getDeviceContext();
		Commands commands = device.getCommands();

		try {
			// Quiesce only if the FW version is 5.0.0 or later
			if (device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
				logger.logp( Level.FINER, CLASS_NAME, METHOD_NAME,
					"device meets minimum firmware requirement for quiesce:" + MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM); //$NON-NLS-1$

				String domainName = null;
				try {
					domainName = domain.getName();
				} catch (DeletedException e) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown", e);
				}

				if (domainName != null) {					
					ConfigObject[] configObjects = { new ConfigObject(this.name, this.className, this.classDisplayName, this.userComment) };
					if (unquiesce) {
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "unquiesce"); //$NON-NLS-1$
						commands.unquiesceService(dc, this.domain.getName(), configObjects);
					} else {
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "quiesce"); //$NON-NLS-1$
						commands.quiesceService(dc, domainName, configObjects, this.quiesceTimeOut);
					}

					logger.logp( Level.FINER, CLASS_NAME, METHOD_NAME,
									"quiesce timeout: " + this.quiesceTimeOut); //$NON-NLS-1$
					waitForQuiesceActionComplete(commands, dc, domainName, unquiesce, this.quiesceTimeOut);
					updateStatus();
				}
			} else {
				if (unquiesce) {
					operation = "unquiesce";
				}
				logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "device "
						+ device.getDisplayName()
						+ " does not meet minimum firmware requirement for "
						+ operation + ":"
						+ MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM); //$NON-NLS-1$
			}

		} catch (DeletedException e) {
			// This domain MUST have a device, so eat the deleted exception
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	

	private void waitForQuiesceActionComplete(Commands commands, DeviceContext dc, String domainName, boolean unquiesce, int timeout) 
		throws UnsuccessfulOperationException, DeletedException, AMPException {
		final String METHOD_NAME = "waitForQuiesceActionComplete"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME, "Domain:" + domainName);

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "quiesce timeout" + timeout); //$NON-NLS-1$

		// If quiesceTimeout is zero, then don't wait for the quiesce/unquiesce
		// command to finish
		if (timeout > 0) {
			// Wait for the quiesce to finish
			boolean quiesceCommandFinished = false;

			QuiesceStatus qs = null;

			// Kick off the timeout
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "create timeout object"); //$NON-NLS-1$
			Timeout timer = new Timeout();
			timer.setTimeout(timeout);
			new Thread(timer).start();

			do {
				sleep(2000);

				RuntimeService[] rtSvcList = commands.getServiceListFromDomain(dc, domainName);
				if ( rtSvcList == null ) // in case something wrong and return null when get Service List from domain
					continue;
				for (int i = 0; i < rtSvcList.length; i++) {
					if (rtSvcList[i].getPrimaryKey().equals(this.getPrimaryKey()) ) { // find service, check the quiesce status
						qs = rtSvcList[i].status.getQuiesceStatus();
						break;
					}
				}
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Quiesce Status:" + qs); //$NON-NLS-1$

				if (unquiesce) {
					if (qs.equals(QuiesceStatus.NORMAL)) quiesceCommandFinished = true;
				} else {
					if (qs.equals(QuiesceStatus.QUIESCED)) quiesceCommandFinished = true;
				}

				if (qs.equals(QuiesceStatus.ERROR) || qs.equals(QuiesceStatus.UNKNOWN)) {
					String message = Messages.getString("wamt.clientAPI.Domain.unexpectedQStatus"); //$NON-NLS-1$
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
					throw new UnsuccessfulOperationException(message, "wamt.clientAPI.Domain.unexpectedQStatus"); //$NON-NLS-1$
				}

			} while ((quiesceCommandFinished == false) && (timer.timeoutExpired() == false));

			if (unquiesce == false && quiesceCommandFinished == false) {
				sleep(500);
				qs = commands.getDomainStatus(dc, domainName).getQuiesceStatus();
				if (!qs.equals(QuiesceStatus.NORMAL)) {
					String message = Messages.getString("wamt.clientAPI.RuntimeService.quiesceFailed", this.getClassDisplayName()+":"+this.getName()); //$NON-NLS-1$
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$
					throw new UnsuccessfulOperationException(message,"wamt.clientAPI.RuntimeService.quiesceFailed", this.getClassDisplayName()+":"+this.getName()); //$NON-NLS-1$
				}
			}
			if (unquiesce == true && quiesceCommandFinished == false) {
				sleep(500);
				qs = commands.getDomainStatus(dc, domainName).getQuiesceStatus();
				if (!qs.equals(QuiesceStatus.NORMAL)) {
					String message = Messages.getString("wamt.clientAPI.RuntimeService.unquiesceFailed", this.getClassDisplayName()+":"+this.getName()); //$NON-NLS-1$
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
					throw new UnsuccessfulOperationException(message,"wamt.clientAPI.RuntimeService.unquiesceFailed", this.getClassDisplayName()+":"+this.getName()); //$NON-NLS-1$
				}
			}			
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	private static void sleep(long time) {
		final String METHOD_NAME = "sleep"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// Auto-generated catch block - eat it
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"interrupted from sleep. shutting down?"); //$NON-NLS-1$
		}
	}

	private void startAction(boolean bStop) throws AMPException, DeletedException {
		final String METHOD_NAME = "startAction"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME, "StartActon:"+bStop);

		String operation = "start";
		Device device = domain.getDevice();
		DeviceContext dc = device.getDeviceContext();
		Commands commands = device.getCommands();

		try {
			// Quiesce only if the FW version is 5.0.0 or later
			if (device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
							"device meets minimum firmware requirement for quiesce:" + MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM); //$NON-NLS-1$

				String domainName = null;
				try {
					domainName = domain.getName();
				} catch (DeletedException e) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"Exception thrown", e);
				}

				if (domainName != null) {					
					ConfigObject[] configObjects = { new ConfigObject(this.name, this.className, this.classDisplayName, this.userComment) };
					if (bStop) {
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "stop"); //$NON-NLS-1$
						commands.stopService(dc, this.domain.getName(), configObjects);
					} else {
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "start"); //$NON-NLS-1$
						commands.startService(dc, domainName, configObjects);
					}
					updateStatus();
				}
			} else {
				if (bStop) {
					operation = "stop";
				}
				logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "device " + device.getDisplayName()
						+ " does not meet minimum firmware requirement for " + operation + ":" 
						+ MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM); //$NON-NLS-1$
			}

		} catch (DeletedException e) {
			// This domain MUST have a device, so eat the deleted exception
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Used only by this class, AMP call to get the latest status of this runtime service on device.
	 * @throws AMPException
	 * @throws DeletedException
	 * @throws NotExistException
	 */
	private void updateStatus() throws AMPException, DeletedException, NotExistException  {
		final String METHOD_NAME = "updateStatus"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		Device device = domain.getDevice();
		DeviceContext dc = device.getDeviceContext();
		Commands commands = device.getCommands();		
		try {
			RuntimeService[] rtServices = commands.getServiceListFromDomain(dc, this.domain.getName());
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "Get the latest status of service from " +
					"device " + device.getDisplayName()); //$NON-NLS-1$
	        if ( rtServices != null ) {
		        int iSize = rtServices.length;
		        for ( int i=0; i < iSize; i++ ) {
		        	if ( this.getPrimaryKey().equals(rtServices[i].getPrimaryKey()) ) { // Find it, update the status
		        		this.status.setStatus(rtServices[i].getAdminStatus(), rtServices[i].getOpStatus(),
		        					rtServices[i].getNeedsSave(), rtServices[i].getQuiesceStatus());
		        		break;
		        	}
		        }
	        }
		} catch ( DeletedException e ) {
			// This domain MUST have a device, so eat the deleted exception
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * For internal use, only called by the Domain.java
	 */
	protected void updateStatus(AdminStatus adminState, OpStatus opState, boolean needsSave, QuiesceStatus quiesceStatus) {
		if ( this.status == null ) {
			this.status = new Status(adminState, opState, needsSave, quiesceStatus);
		}
		else  {
			this.status.setStatus(adminState, opState, needsSave, quiesceStatus);
		}
	}
	
	/**
	 * This method is used to check if this runtime service is deleted before running operation, 
	 * it throws exception if this object has been deleted.
	 * @param METHOD_NAME
	 * @throws DeletedException
	 */
	private void checkDeletedState(String METHOD_NAME) throws NotExistException {		
		if (this.isDeleted) { // Check if this service is deleted
        	String message = Messages.getString("wamt.clientAPI.Service.svcDeleted", this.getName());
        	NotExistException e = new NotExistException(message, "wamt.clientAPI.Service.svcDeleted", this.getName()); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
            throw(e);
        }
	}
	
	/**
     * Get the pre-built String that could be used as a primary key for this
     * object if you need to get it anywhere or put it in a hash collection.
     * Although this method isn't necessary for users of the clientAPI, it may
     * be helpful for them to have so they don't need to implement it
     * themselves. It is used internally within the clientAPI and is exposed
     * here for your convenience.
     * 
     * @return a String that could represent a unique instance of this object.
     *         It use the Name and ClassName as the primary key.
     *         (i.e. Name:Classname)
     */
    public String getPrimaryKey() {
    	return (this.name+":"+this.className);
    }
    
    /**
     * Get a String representation of object for the purpose of debugging or tracing
     * @return a String representation 
     */
    public String toString() {
        String result = "[RuntimeService "; //$NON-NLS-1$
        result += "Name=" + this.name; //$NON-NLS-1$
        result += ", ClassName=" + this.className; //$NON-NLS-1$
        result += ", ClassDisplayName=" + this.classDisplayName; //$NON-NLS-1$
        result += ", UserComments=" + this.userComment; //$NON-NLS-1$        
        result += "]"; //$NON-NLS-1$
        
        return(result);
    }
}

class Status {        
	private AdminStatus adminStatus = AdminStatus.DISABLED;
	private OpStatus opStatus = OpStatus.DOWN;
	private boolean needsSave = false;
	private QuiesceStatus quiesceStatus = null;
	
	/**
	 * Construct a new InterDependentServiceCollection object. In general this should be created
     * only by {@link RuntimeService}.
     * 
	 * @param adminStatus true if this object's admin state is "enabled", false is "disabled"
	 * @param opStatus true if this object's opState is "up", false is "down"
	 * @param needsSave true if this object has been modified but not saved to flash, false otherwise.
	 * @param quiesceStatus quiesce Status of config object
	 */
	Status(AdminStatus adminStatus, OpStatus opStatus, boolean needsSave, QuiesceStatus quiesceStatus) {
		this.adminStatus = adminStatus;
		this.opStatus = opStatus;
		this.needsSave = needsSave;
		this.quiesceStatus = quiesceStatus;
	}
	
	 /**
     * Get a boolean value which indicates if the object's admin status is enabled. 
     * @return 
     * 		true if this object's admin state is "enabled", false is "disabled"
     */
    public AdminStatus getAdminStatus() {
        return(this.adminStatus);
    }

    /**
     * Get the operation status of the object. 
     * @return 
     * 		true if this object's opState is "up", false is "down"
     */
    public OpStatus getOpStatus() {
        return(this.opStatus);
    }
    
    /**
     * Get a boolean value which indicates if the object has been modified but not saved to flash. 
     * @return 
     * 		true if this object has been modified but not saved to flash, false otherwise.
     */
    public boolean getNeedsSave() {
        return(this.needsSave);
    }

    /**
     * Get the quiesce Status of the object 
     * @return quiesce Status of this object
     */
    public QuiesceStatus getQuiesceStatus() {
        return (this.quiesceStatus);
    }
    
    /*
     * Set the status of this configobject.
     * @param adminState true if this object's admin state is "enabled", false is "disabled"
     * @param opState true if this object's opState is "up", false is "down"
     * @param needsSave true if this object has been modified but not saved to flash, false otherwise.
     * @param quiesceStatus quiesce Status of this object
     */
    public void setStatus(AdminStatus adminState, OpStatus opState, boolean needsSave, QuiesceStatus quiesceStatus) {
    	this.adminStatus = adminState;
    	this.opStatus = opState;
    	this.needsSave = needsSave;
    	this.quiesceStatus = quiesceStatus;
    }

    /**
     * Get a String representation of object for the purpose of debugging or tracing
     * @return a String representation 
     */
    public String toString() {
        String result = "[RuntimeServiceStatus "; //$NON-NLS-1$
        result += ", adminState=" + this.adminStatus; //$NON-NLS-1$
        result += ", opState=" + this.opStatus; //$NON-NLS-1$
        result += ", configState=" + this.needsSave; //$NON-NLS-1$
        result += ", quiesceState=" + this.quiesceStatus.toString(); //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        
        return(result);
    }
}
