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

import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A ManagedSet is one of the core objects that callers of the clientAPI will use 
 * to manage DataPower devices. A ManangetSet is a heterogeneous collection of DataPower 
 * devices. The term heterogeneous refers to the hardware and licensing characteristics 
 * of the devices, as well as the domains configured in the managed set (e.g., it is not a 
 * requirement that all devices have identical domain configurations, see 
 * {@link Device#createManagedDomain(String)} for more information on a creating managed 
 * domain on a single device in a ManagedsSet/
 * <p>
 * Devices can be added to a ManagedSet which indicates that they are managed and
 * identifies a particular management scope. When a Device is not a member of a ManagedSet, 
 * it is considered unmanaged.
 * <p>
 * A managed set can have zero or more Devices as members. A device is managed
 * only if it is a member of a ManagedSet. When a device is managed, their
 * managed domain(s) can be are synchronized with a specified domain configuration and
 * deployment policy. See {@link Domain#setSynchronizationMode(DomainSynchronizationMode)}} 
 * for more information about {@link Domain} synchronization.
 * <p>
 * 
 * @version SCM ID: $Id: ManagedSet.java,v 1.10 2011/04/11 02:11:38 lsivakumxci Exp $
 */
public class ManagedSet extends WorkArea implements Persistable {
    private volatile StoredManagedSet persistence = null;
    
    private volatile Lock lock = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = ManagedSet.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Create a new ManagedSet that is empty and add it to the Manager. This new
     * ManagedSet will not have any Device members or managed domains.
     * 
     * @param name a human-consumable symbolic name for this object that will be
     *        for later display of this ManagedSet. This name must be unique
     *        across all ManagedSets in this Manager.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws AlreadyExistsInRepositoryException a ManagedSet with this symbolic name
     *         already exists.
     * @throws DeletedException a required object that this ManagedSet requested
     *         to refer to have been deleted. The new ManagedSet was not
     *         created.
     *
     */
    public ManagedSet(String name) throws AlreadyExistsInRepositoryException, DatastoreException, DeletedException {
        // create the queues
        super(Configuration.getAsInteger(Configuration.KEY_TASK_QUEUE_SIZE));

        final String METHOD_NAME = "ManagedSet(String)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, name);
        Manager manager = Manager.internalGetInstance();
        if (manager.containsManagedSetName(name)) {
            String message = Messages.getString("wamt.clientAPI.ManagedSet.msAlreadyExists", name); //$NON-NLS-1$
            AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message,"wamt.clientAPI.ManagedSet.msAlreadyExists", name);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }

        Repository repository = manager.getRepository();
        this.persistence = repository.createManagedSet(name);
        
        // set the rest of the non-persisted members
        this.lock = new Lock(this.toString());
        this.lockWait();
        try {
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    ManagedSet(StoredManagedSet storedManagedSet) throws AlreadyExistsInRepositoryException {
        super(Configuration.getAsInteger(Configuration.KEY_TASK_QUEUE_SIZE));
        
        final String METHOD_NAME = "ManagedSet(StoredManagedSet)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Manager manager = Manager.internalGetInstance();
        if (manager.containsManagedSetName(storedManagedSet.getName())) {
            String message = Messages.getString("wamt.clientAPI.ManagedSet.msAlreadyExists", storedManagedSet.getName()); //$NON-NLS-1$
            AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message,"wamt.clientAPI.ManagedSet.msAlreadyExists", storedManagedSet.getName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }

        this.persistence = storedManagedSet;
        
        // get the rest of the non-persisted members
        
        // can't call this.toString() until it is added to the mapper
        this.lock = new Lock("ManagedSet[" + this.persistence.getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        
        // add it to the persistence mapper
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.add(this.persistence, this);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Check if the ManagedSet is available for new update commands. If any
     * update to the devices in this ManagedSet is currently running, the
     * ManagedSet will not allow additional updates to be performed in either
     * the foreground or the background.
     * 
     * @return true if there is no write lock on the ManagedSet, false otherwise
     */
    public boolean isAvailableForUpdate() {
        return(this.lock.isAvailable());
    }
    
    void lockNoWait() throws LockBusyException {
        this.lock.lockNoWait();
    }
    
    void lockWait() {
        this.lock.lockWait();
    }
    
    void unlock() {
        this.lock.unlock();
    }
    
    /**
     * Get the name of this ManagedSet. This name should be human-consumable and
     * is the symbolic name used when the constructor was called. The name is
     * the primary key of this object in the manager. The name is immutable, so
     * there is no <code>setName(String)</code> method.
     * 
     * @return the symbolic name of this ManagedSet.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public String getName() throws DeletedException {
        return(this.getStoredInstance().getName());
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }

    /**
     * Get the ManagedSet instance that has the specified primary key.
     * 
     * @param targetKey the key of the target ManagedSet
     * @return the ManagedSet instance that has the specified primary key. If no
     *         ManagedSet exists with this primary key, then it will return
     *         null.
     * @see #getPrimaryKey()
     */
    public static ManagedSet getByPrimaryKey(String targetKey) {
        ManagedSet result = null;
        Manager manager = Manager.internalGetInstance();
        ManagedSet[] managedSets = manager.getManagedSets();
        for (int i=0; i<managedSets.length; i++) {
            String key = null;
            try {
                key = managedSets[i].getPrimaryKey();
            } catch (DeletedException e) {
                key = ""; //$NON-NLS-1$
            }
            if (key.equals(targetKey)) {
                result = managedSets[i];
                break;
            }
        }
        return(result);
    }
    
    StoredManagedSet getStoredInstance() throws DeletedException {
        final String METHOD_NAME = "getStoredInstance"; //$NON-NLS-1$
        if (this.persistence == null) {
        	String message = Messages.getString("NoPersistence");
            DeletedException e = new DeletedException(message, "NoPersistence"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return(this.persistence);
    }
    
    /**
     * Part of the Persistable interface.
     * @throws InUseException
     * @throws InvalidParameterException
     * @throws NotEmptyException
     * @throws DatastoreException
     * @throws DeletedException
     * @throws FullException 
     * @throws LockBusyException
     * @throws AMPException
     * @throws LockBusyException
     * @throws NotExistException
     */
    void destroy() throws DeletedException, InUseException, InvalidParameterException,
    NotEmptyException, DatastoreException, FullException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        // get the lock on this ManagedSet and then we can destroy everything in it.
        this.lockWait();
        try {
            
            // delete child objects
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            
            // delete child objects: remove all the devices, but don't destroy them
            Device[] devices = this.getDeviceMembers();
            for (int i=0; i<devices.length; i++) {
                try {
                    this.removeDevice(devices[i]);
                } catch (NotExistException e) {
                    // just eat it, since this shouldn't happen
                } catch (FullException e) {
					// TODO Auto-generated catch block
					throw e;
				}
            }
            
            // don't destroy the firmware since that exists across ManagedSets.
            // But do get rid of the reference to id. And do it before deleting from persistence
//            if (this.getDesiredFirmwareVersion() != null) {
//                this.unsetDesiredFirmwareVersion();
//            }
            
            // delete from persistence mapper, then unlock, then delete from persistence
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting from persistence mapper"); //$NON-NLS-1$
            mapper.remove(this.getStoredInstance());
            
            // clear any references
        } catch (LockBusyException e) {
            // this should never happen if we are able to get the lock in the first place
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.ManagedSet.unexpProb",this.getDisplayName()), e); //$NON-NLS-1$
        } finally {
            this.unlock();
        }
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "deleting from persistence"); //$NON-NLS-1$
        this.getStoredInstance().delete();
        this.persistence = null;
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get the devices which are members of this ManagedSet.
     * 
     * @return the Devices which are members of this ManagedSet. If this
     *         ManagedSet has no members, then this array may have zero
     *         elements.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #addDevice(Device)
     * @see #removeDevice(Device)
     * @see "useCases section 4.2"
     */
    public Device[] getDeviceMembers() throws DeletedException {
        final String METHOD_NAME = "getDeviceMembers"; //$NON-NLS-1$
        Device[] result = null;
        StoredDevice[] storedDevices = this.getStoredInstance().getDeviceMembers();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        Vector<Device> devices = new Vector<Device>();
        for (int i=0; i<storedDevices.length; i++) {
            Device device = null;
            try {
                device = mapper.getVia(storedDevices[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            storedDevices[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                device = null;
            }
            if (device != null) {
                devices.add(device);
            }
        }
        result = new Device[devices.size()];
        for (int i=0; i<devices.size(); i++) {
            result[i] = (Device) devices.get(i);
        }
        return(result);

    }
    
    /**
     * Adds a device to this managed set.
     * <p>
     * The following actions are performed:
     * - this Manager subscribes to the new device
     * - if synchronization mode is AUTO, this Manager synchronizes the configuration on the device 
     * - the managed set is persisted with the new device.
     * 
     * <p>
     * An exception is thrown if:
     * - the device is already a member of another managed set
     * <p>
     * @param device
     * @return ProgressContainer that wraps the task of adding the device.
     * @throws LockBusyException
     * @throws AlreadyExistsInRepositoryException
     * @throws InUseException
     * @throws InvalidParameterException
     * @throws NotEmptyException
     * @throws DeletedException
     * @throws UndeployableVersionException
     * @throws AMPException
     * @throws DatastoreException
     * @throws DeviceTypeIncompatibilityException
     * @throws ModelTypeIncompatibilityException
     * @throws FeaturesNotEqualException
     * @throws MissingFeaturesInFirmwareException
     * @throws UnlicensedFeaturesInFirmwareException
     */
    public ProgressContainer addDevice(Device device) throws Exception, 
    LockBusyException,
	AlreadyExistsInRepositoryException, InUseException, InvalidParameterException,
	NotEmptyException, DeletedException, UndeployableVersionException,
	AMPException, DatastoreException,
	DeviceTypeIncompatibilityException,
	ModelTypeIncompatibilityException, FeaturesNotEqualException,
	MissingFeaturesInFirmwareException,
	UnlicensedFeaturesInFirmwareException 
	{
    	// TODO: remove InvalidParameterException, NotEmptyException,
    	// NotExistException
    	final String METHOD_NAME = "addDevice(Device)"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {device, this});
    	
    	ProgressContainer result = null;
        Manager manager = Manager.internalGetInstance();
        this.lockNoWait();
        try {
            // check that the device is not already in another managedSet
            ManagedSet alreadyManagedSet = device.getManagedSet();
            if (alreadyManagedSet != null) {
            	Object[] args = new Object[] {device.getDisplayName(), alreadyManagedSet.getDisplayName() };
                String message = Messages.getString("wamt.clientAPI.ManagedSet.devAlreadyInMs", args); //$NON-NLS-1$
                InUseException e = new InUseException(message,"wamt.clientAPI.ManagedSet.devAlreadyInMs", args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            
            // add the device to the managed set and persist it
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"adding device to persistence"); //$NON-NLS-1$
            StoredManagedSet storedManagedSet = this.getStoredInstance();
            StoredDevice storedDevice = device.getStoredInstance();
            storedManagedSet.add(storedDevice);

            // save the repository now. 
            manager.save(Manager.SAVE_UNFORCED);

            // TODO: v2 sync device only if AUTO is on
            //  perhaps always call sync and have sync do subscription
            //BackgroundTask task = null;
            // queue the task to sync to this device
            //task = BackgroundTask.createSyncDeviceTask(device,
            //         SubscriptionState.NONE);

        	if(!DeviceType.XC10.equals(device.getDeviceType())){
        		// Background task to subscribe to the device for non-XC10 devices
        		WorkArea workArea = manager;
        		BackgroundTask backgroundTask = BackgroundTask.createSubscribeTask(device);
        		result = backgroundTask.getProgressContainer();
        		manager.enqueue(backgroundTask, workArea);            
        		//  Background task to subscribe to the device
        		//  Put MacroProgressContainer

        		BackgroundTask backgroundTaskDomainStatus =	BackgroundTask.createGetDomainsOperationStatusTask(device);
        		ProgressContainer progressContainer = backgroundTaskDomainStatus.getProgressContainer();            
        		manager.enqueue(backgroundTaskDomainStatus, workArea);
        		MacroProgressContainer macroProgressContainer = new MacroProgressContainer(backgroundTaskDomainStatus);            
        		macroProgressContainer.addNested(progressContainer);
        		manager.addNotificationProgress(macroProgressContainer);
        	} else {
        		result = new ProgressContainer(null);
        		result.setTotalSteps(1);
        		result.setComplete();
        	}
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    	return(result);
   	}
    /**
     * Remove the specified device from this managed set. This means that the
     * device is no longer managed. No domains or firmware are
     * changed during the execution of this method, everything is left as-is on
     * the device. The device remains defined in the Manager, so you can continue to
     * use the Device object, especially if you want to later add it to another
     * ManagedSet. It is not deleted from the repository.
     * 
     * @param device the device to remove
     * @throws NotExistException the device parameter is not a member of this
     *         ManagedSet
     * @throws AMPException there was a problem communicating with the device.
     * @throws InUseException This error occur if you are
     *         trying to remove the last device from a ManagedSet while there
     *         are still Domains being managed. Because managing a domain is
     *         considered similar to having a service in production, it is
     *         important that the service remain up, and calling during this
     *         situation is considered a deployment error. You must first
     *         unmanage all domains before removing the last device from a
     *         ManagedSet.
     * @throws InvalidParameterException there was a problem cleaning up data
     *         related to this Device. This exception would be thrown from a
     *         lower-level method.
     * @throws NotEmptyException the parameter is a null reference.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws FullException
     * @see #addDevice(Device)
     * @see #getDeviceMembers()
     */
    public ProgressContainer removeDevice(Device device)
    throws NotExistException, InUseException, InvalidParameterException, 
    NotEmptyException, DatastoreException, LockBusyException, DeletedException, FullException {
    	final String METHOD_NAME = "removeDevice(Device)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {device, this});
        ProgressContainer result = null;
        try {
        	this.lockWait();
        	
            // prevent a NullPointerException
            if (device == null) {
                String message = Messages.getString("wamt.clientAPI.ManagedSet.nullRef"); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.nullRef");
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            // check if the device is in the managed set
            if (!this.containsDeviceReference(device)) {
            	Object[] args = new Object[] {device.getDisplayName(), this.getDisplayName()};
                String message = Messages.getString("wamt.clientAPI.ManagedSet.reqDevNotInMs", args); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.reqDevNotInMs", args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }                   
                        
       		// unsubscribe from it //DeviceContext deviceContext =
            device.getDeviceContext();
       		BackgroundTask backgroundTask = BackgroundTask.createUnsubscribeTask(device, false);
       		Manager manager = Manager.internalGetInstance();
       		manager.privilegedEnqueue(backgroundTask, manager);
       		result = backgroundTask.getProgressContainer();
       		// Wait till unsubscribe task completes 
       		try {
       			result.blockAndTrace(Level.FINER);			
       		} catch (Exception e) {
       			// TODO Auto-generated catch block
       			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
       					Messages.getString("ProbCommDev",device.getHostname()),e);
       		}
       		if (result.hasError()) {
       			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
       					Messages.getString("ProbCommDev",device.getHostname()), result.getError());
       		}
        	
            Manager.internalGetInstance().save(Manager.SAVE_UNFORCED); 
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }

    /**
     * Remove the specified device from this managed set without trying to unsubscribe.
     * This method should be used when {@link #addDevice(Device)} fails when trying to 
     * subscribe. The failure can be due to a connectivity problem or when the device 
     * is already subscribed to another manager.
     * 
     * @param device the device to remove
     * @throws NotExistException the device parameter is not a member of this
     *         ManagedSet
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DirtySaveException 
     * 
     * @see #addDevice(Device)
     * @see #getDeviceMembers()
     */
     public ProgressContainer removeDeviceWithoutUnsubscribe(Device device) throws LockBusyException, NotExistException, DeletedException, DirtySaveException, DatastoreException
     {
    	final String METHOD_NAME = "removeDevice(Device)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {device, this});
        ProgressContainer result = null;
        try {
            this.lockNoWait();

            // prevent a NullPointerException
            if (device == null) {
                String message = Messages.getString("wamt.clientAPI.ManagedSet.nullRef"); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.nullRef");
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            // check if the device is in the managed set
            if (!this.containsDeviceReference(device)) {
            	Object[] args = new Object[] {device.getDisplayName(), this.getDisplayName()};
                String message = Messages.getString("wamt.clientAPI.ManagedSet.reqDevNotInMs", args); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.reqDevNotInMs", args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }                   
                        
            // unsubscribe from it //DeviceContext deviceContext = 
            device.getDeviceContext();
            BackgroundTask backgroundTask = BackgroundTask.createUnsubscribeTask(device, true);
            Manager manager = Manager.internalGetInstance();
            manager.privilegedEnqueue(backgroundTask, manager);
            result = backgroundTask.getProgressContainer();
            Manager.internalGetInstance().save(Manager.SAVE_UNFORCED); 
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }

/* Covered by removeDevice(Device)    /**
     * Remove the specified device from this managed set. This means that the
     * device is no longer managed. No domains or firmware are
     * changed during the execution of this method, everything is left as-is on
     * the device. The device remains defined in the Manager, so you can continue to
     * use the Device object, especially if you want to later add it to another
     * ManagedSet.
     * 
     * @param device the device to remove
     * @throws NotExistException the device parameter is not a member of this
     *         ManagedSet
     * @throws AMPException there was a problem communicating with the device.
     * @throws InUseException This error occur if you are
     *         trying to remove the last device from a ManagedSet while there
     *         are still Domains being managed. Because managing a domain is
     *         considered similar to having a service in production, it is
     *         important that the service remain up, and calling during this
     *         situation is considered a deployment error. You must first
     *         unmanage all domains before removing the last device from a
     *         ManagedSet.
     * @throws InvalidParameterException there was a problem cleaning up data
     *         related to this Device. This exception would be thrown from a
     *         lower-level method.
     * @throws NotEmptyException the parameter is a null reference.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #add(Device)
     * @see #getDeviceMembers()
     * @see "useCases section 4.2"
     *//*
    public ProgressContainer remove(Device device)
        throws NotExistException, InUseException,
        InvalidParameterException, NotEmptyException, DatastoreException, LockBusyException, DeletedException {
        final String METHOD_NAME = "remove(Device)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {device, this});
        ProgressContainer result = null;
        try {
            this.lockNoWait();

            // prevent a NullPointerException
            if (device == null) {
                String message = Messages.getString("wamt.clientAPI.ManagedSet.nullRef"); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.nullRef");
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            // check if the device is in the managed set
            if (!this.containsDeviceReference(device)) {
            	Object[] args = new Object[] {device.getDisplayName(), this.getDisplayName()};
                String message = Messages.getString("wamt.clientAPI.ManagedSet.reqDevNotInMs", args); //$NON-NLS-1$
                NotExistException e = new NotExistException(message,"wamt.clientAPI.ManagedSet.reqDevNotInMs", args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            
            Device[] devices = this.getDeviceMembers();
            
            // unsubscribe from it
            DeviceContext deviceContext = device.getDeviceContext();
            BackgroundTask backgroundTask = BackgroundTask.createUnsubscribeTask(deviceContext);
            Manager manager = Manager.internalGetInstance();
            manager.privilegedEnqueue(backgroundTask, manager);
            result = backgroundTask.getProgressContainer();
            
            // remove it from the managedSet
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "remove from persistence"); //$NON-NLS-1$
            StoredDevice storedDevice = device.getStoredInstance();
            this.getStoredInstance().remove(storedDevice);
            
            // remove the device's Notification queue
            this.notificationQueue.clean(this);
            
            // are there no devices left?
            devices = this.getDeviceMembers();
            if (devices.length == 0) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "no devices left in ManagedSet, removing domains"); //$NON-NLS-1$

                // We don't want to delete the Firmware child objects because it could be used by another ManagedSet
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                      "unsetting desiredFirmware"); //$NON-NLS-1$
                this.getStoredInstance().setDesiredFirmware(null);
            }
            manager.save(Manager.SAVE_UNFORCED);
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }*/

    /**
     * This will remove the device from the managed set, even if it is the last device 
     * in the managed set, and even
     * if it still has managed domains.  If it is the last
     * device, then the managed set will be empty. If it is the last device and
     * has managed domains, it will unmanage the domains before leaving the
     * managed set as empty.
     * 
     * @param device the Device for forcibly remove from this ManagedSet
     * @throws AMPException there was a problem communicating with the device.
     * @throws InUseException failed to unmanage domains or remove the device
     *         from the ManagedSet. This exception was thrown from a lower-level
     *         method.
     * @throws InvalidParameterException failed to remove the device or domains
     *         from the ManagedSet. This exception was thrown from a level-level
     *         method.
     * @throws NotEmptyException failed to remove the device or domains from the
     *         ManagedSet. This exception was thrown from a level-level method.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws FullException 
     */
    void forceRemove(Device device) throws AMPException, NotExistException, 
    InUseException, InvalidParameterException, NotEmptyException, DeletedException, 
    DatastoreException, FullException {
        final String METHOD_NAME = "forceRemove"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {device, this});
        // remove device from managedSet
        try {
            // we always lock managed sets before we modify them
            this.lockWait();

            Object[] args = new Object[] {device.getDisplayName(), this.getDisplayName()};
            String message = Messages.getString("wamt.clientAPI.ManagedSet.remDevFromMs", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            try {
                this.removeDevice(device);
            } catch (LockBusyException e) {
                // we already have the lock, so this shouldn't happen
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
            }
            
            // remove the Notification queue
            this.notificationQueue.clean(this);
        
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }


    boolean containsDeviceReference(Device device) throws DeletedException {
        boolean result = false;
        Device[] allDevices = this.getDeviceMembers();
        for (int i=0; i<allDevices.length; i++) {
            if (allDevices[i] == device) {
                result = true;
                break;
            }
        }
        return(result);
    }

    
    /**
     * Get the OperationStatus of this ManagedSet. See the javadoc of the return
     * type for an explanation of what OperationStatus is.
     * 
     * @return the object that represents the operation status of the
     *         ManagedSet. This is a rollup of all operation status across each
     *         device in the ManagedSet. This is defined as a rollup of all
     *         managed devices which in turn includes all managed domains on
     *         each device.
     */
    public OperationStatus getOperationStatus() {
        final String METHOD_NAME = "getOperationStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Vector collection = new Vector();
        Device[] devices = null;
        try {
            devices = this.getDeviceMembers();
        } catch (DeletedException e) {
            // one or more Devices were deleted, shouldn't happen, assume no members
            devices = new Device[0];
        }
        for (int i=0; i<devices.length; i++) {
        	Device device = devices[i];
        	DeviceType deviceType = null;
        	try {
				deviceType = device.getDeviceType();
			} catch (DeletedException e) {
			}
        	if(deviceType != null && !deviceType.equals(DeviceType.XC10)){
        		OperationStatus deviceOperationStatus = device.getRollupOperationStatus();
        		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
        				device + " has " + deviceOperationStatus); //$NON-NLS-1$
        		if (deviceOperationStatus != null) {
        			collection.add(deviceOperationStatus);
        		}
        	}
        }
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                "found " + collection.size() + " items to rollup"); //$NON-NLS-1$ //$NON-NLS-2$
        OperationStatus[] statusArray = OperationStatus.toOperationStatusArray(collection);
        OperationStatus result = new OperationStatus(OperationStatus.Enumerated.UNKNOWN);
        // the UNKNOWN value above will be overwritten by rollupFrom
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "about to rollup"); //$NON-NLS-1$
        result.rollupFrom(statusArray);
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return(result);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = null;
        String name = null;
        try {
            name = this.getPrimaryKey();
        } catch (DeletedException e) {
            name = "[deleted]"; //$NON-NLS-1$
        }
        result = "ManagedSet[" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces.
     * 
     * @return a human-readable name that represents this object.
     */
    public String getDisplayName() {
        String result = null;
        try {
            result = this.getName();
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Set the source firmware level for each Device in this ManagedSet. This method calls 
     * the {@link Device#setSourceFirmwareLevel(String)} method for each device in this managed
     * set. If a suitable firmware level has not been loaded into the {@link Manager} for a 
     * device in the set then the device will be added to the returned Hashtable with a 
     * NotExistException. Bear in mind that the devices in this managed set may be 
     * different Device Types, Model Types, or be licensed for different features, so the 
     * manager should be preloaded with all of the correct firmware images, at the level 
     * specified, for all of the devices in this set.        
     * 
     * @param level desiredFirmwareVersion
     * 
     * @return Hashtable of failed Devices - with Device(key)/Exception(value)
     * 
     * @see Device#setSourceFirmwareLevel(String)
     * @see Manager#addFirmware(Blob, String)
     * @see Firmware
     * @see FirmwareVersion
     *         
     */
    public Hashtable setSourceFirmwareLevel(String level) 
    throws DeletedException, NotExistException
    {
    	final String METHOD_NAME = "setSourceFirmwareLevel"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME, level);

		Hashtable<Device,Exception> failedDevices = new Hashtable<Device,Exception> ();           		      	
		Device[] managedDevices = this.getDeviceMembers();   

		if ( managedDevices.length < 1){
			String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
    		throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
		}

		for (Device device: managedDevices){  	    	    
			try{

				device.setSourceFirmwareLevel(level);
			}catch(DeletedException e){
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, device +  ":" + e.getMessage()); 	
				failedDevices.put(device,e);	   	          
			} catch (NotExistException e) {
				// TODO Auto-generated catch block
				failedDevices.put(device, e);
			} catch (DeviceTypeIncompatibilityException e) {
				// TODO Auto-generated catch block
				failedDevices.put(device,e);	   	          
			} catch (ModelTypeIncompatibilityException e) {
				// TODO Auto-generated catch block
				failedDevices.put(device,e);	   	          
			} catch (MissingFeaturesInFirmwareException e) {
				// TODO Auto-generated catch block
				failedDevices.put(device,e);	   	          
			} catch (UnlicensedFeaturesInFirmwareException e) {
				// TODO Auto-generated catch block
				failedDevices.put(device,e);	   	          
			}
		}  	           	        

		logger.exiting(CLASS_NAME, METHOD_NAME);

		return failedDevices;
      	
    }   

    /**
     * Schedules tasks to deploy the FirmwareVersion to all devices in this managed set.
     * This function is available on a managed set with device members. 
     * 
     * See MacroProgressContainer to check for the results of deployment to each of the devices members
     * 
     * 
     * @return MacroProgressConatiner is a collection of single tasks. It is a container for multiple ProgressContainers that represent
     * each firmware deployment task to a managed device. The user interface can track progress of macro tasks and the sub tasks via the 
     * methods available MacroProgressContainer.
     * <p>The {@link MacroProgressContainer#getProgressContainers()} returns a collection of ProgressContainers.  You can get the correlator
     * on each ProgressContainer with a call to {@link ProgressContainer#getCorrelator()}.  The correlator
     * returned is the Device object associated with ProgressContainer. It can be used to correlate a background task to other items that 
     * the caller cares about.
     * <p>The {@link MacroProgressContainer#getFailedTasks()} returns a Hashtable.  The key is the Device object(key) and the value is the 
     * ProgressContainer generated for that device (value)
     * <p>The {@link MacroProgressContainer#blockAndTrace(Level)}is a convenience method for waiting for the tasks to complete and printing
     * the progress of each step for each nested ProgressConatiner.   
     *     
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws NotExistException the specified firmware version is null
     * 
     * @see MacroProgressContainer      
     */
    public MacroProgressContainer deploySourceFirmwareVersion() 
    throws DeletedException, NotExistException
 {
    	final String METHOD_NAME = "deploySourceFirmwareVersion"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME);
      	
    	MacroProgressContainer macroProgressContainer = new MacroProgressContainer(null);
    	ProgressContainer progressContainer = null;
    	
	    Device[] managedDevices = this.getDeviceMembers();   
        Hashtable<Device,ProgressContainer> devicesFailedDeployment = new Hashtable<Device,ProgressContainer> ();
        
        try {  
        	if ( managedDevices.length < 1){
    			String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
        		throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
        	}
            for (Device device: managedDevices){  	    	    
      	        // deploy firmware will throw exception if not compatible or it has been deleted
      	        try{
      	        	progressContainer = device.deploySourceFirmwareVersion();
      	        	progressContainer.setCorrelator(device);
      	        	macroProgressContainer.addNested(progressContainer);
    	      	}catch(DeletedException e){
    	      		devicesFailedDeployment.put(device, progressContainer);		          
				} catch (FullException e) {
					devicesFailedDeployment.put(device, progressContainer);
				}
            }  	           	        

        } finally {
        }
        
    	logger.exiting(CLASS_NAME, METHOD_NAME);
    	Manager manager = Manager.internalGetInstance();
    	manager.addNotificationProgress(macroProgressContainer); 
        macroProgressContainer.setFailedTasks(devicesFailedDeployment);
        macroProgressContainer.setComplete();
    	return macroProgressContainer;
    }

    /**
     * <p>Deploys the previously set domain configuration to all managed domains in this managed set with the 
     * specified domain name.
     * <p>The domain configuration is previously set by calling {@link #setSourceConfigurationForDomain(String, URLSource)}
     * and {@link #setDeploymentPolicyForDomain(String, URLSource, String, String)}
     * 
     * @param domainName Name of domain to be created
     * @throws DeletedException if it fails to retrieve Device members
     * @return MacroProgressConatiner is a collection of single tasks. It is a container for multiple ProgressContainers that represent
     * each firmware deployment task to a managed device. The user interface can track progress of macro tasks and the sub tasks via the 
     * methods available MacroProgressContainer.
     * <p>The {@link MacroProgressContainer#getProgressContainers()} returns a collection of ProgressContainers.  You can get the correlator
     * on each ProgressContainer with a call to {@link ProgressContainer#getCorrelator()}.  The correlator
     * returned is the Device object associated with ProgressContainer. It can be used to correlate a background task to other items that 
     * the caller cares about.
     * <p>The {@link MacroProgressContainer#getFailedTasks()} returns a Hashtable.  The key is the Device object(key) and the value is the 
     * ProgressContainer generated for that device (value)
     * <p>The {@link MacroProgressContainer#blockAndTrace(Level)}is a convenience method for waiting for the tasks to complete and printing
     * the progress of each step for each nested ProgressConatiner.     
     * 
     * @throws NotExistException there are no devices in this managed set
     * @throws DeletedException  device members have been deleted from the repository	     *
     *         
     * @see #setSourceConfigurationForDomain(String, URLSource)
     *         
     * 
     */
	    public MacroProgressContainer deploySourceConfigurationForDomain(String domainName)
	    throws DeletedException, NotExistException
       //@TODO -- implement deployment policy parm 
	 {
	    	final String METHOD_NAME = "deploySourceConfigurationForDomain"; //$NON-NLS-1$
	        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
	
 	
	    	MacroProgressContainer macroProgressContainer = new MacroProgressContainer(null);
	    	ProgressContainer progressContainer = null;
	    	
		    Device[] managedDevices = this.getDeviceMembers();   
	        Hashtable <Device, ProgressContainer> deploymentFailedDevice = new Hashtable<Device,ProgressContainer>();          		      	
	        Domain domain = null;
	        try {  
	        	if ( managedDevices.length < 1){
	    			String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
	        		throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
	        	}
	            for (Device device: managedDevices){  	    	    
	      	        // retrieve specified domain on each managed device for configuration deployment
	      	        try{
	      	        	domain = device.getManagedDomain(domainName); 
	      	        	if (domain !=null){
	      	        		progressContainer = domain.deployConfiguration();
	          	        	progressContainer.setCorrelator(device);	      	        		
	      	        		macroProgressContainer.addNested(progressContainer);		      	        	
	      	        	}else{
	      	  			    String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
	      	    		    throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
	      	        	}
	      	        }catch(DeletedException dt){
	      	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
	      	        			dt.getMessage()); 	          
	      	        	deploymentFailedDevice.put(device,progressContainer);	      	       		          
	      	        } catch (FullException e) {
						// TODO Auto-generated catch block
	      	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
	      	        			e.getMessage()); 	          
	      	        	deploymentFailedDevice.put(device,progressContainer);	      	       		          

					}catch (NullPointerException e){
						// TODO Auto-generated catch block
	      	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
	      	        			NullPointerException.class.getName());          
	      	        	deploymentFailedDevice.put(device,progressContainer);	      	       		          

					}catch (NotExistException e){
	      	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
	      	        			e.getMessage()); 							
	      	        	deploymentFailedDevice.put(device,progressContainer);	 						

					}catch (NotManagedException e){
	      	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
	      	        			e.getMessage()); 							
	      	        	deploymentFailedDevice.put(device,progressContainer);	 						
					}
	            }  	           	        
	
	        } finally {
	        }
	    	logger.exiting(CLASS_NAME, METHOD_NAME);
	    	Manager manager = Manager.internalGetInstance();
	    	manager.addNotificationProgress(macroProgressContainer); 
	        macroProgressContainer.setFailedTasks(deploymentFailedDevice);
	        macroProgressContainer.setComplete();
	    	return macroProgressContainer;
	    }

	     /**
	     * Creates a domain of the specified name on all devices in this managed set. The domain name
	     * must be specified, all other parameter are optional.  If the optional parameters are not specified
	     * it will create new domain objects of the specified name on all the devices and add them
         * to the manager. This will replace a domain of the same name if it already exists without any warnings. 
	     * 
	     * The minimum firmware level must be satisfied for the operation. See {@link FirmwareVersion#meetsMinimumLevel(String)}
	     * 
	     * @param domainName Name of domain to be created
	     * @param domainSource Source configuration to be deployed on device for the domain.  Optional, must pass in null if not to be used.
	     * @param deploymentPolicySource Policy Source to be used during deployment.  Optional, must pass in null if not to be used.
	     * @param deploymentPolicyDomainName Domain name for the deployment policy source.  Used to locate the domain within the policy source. Only used if deploymentPolicySource is non-null.
	     * @param deploymentPolicyObjectName Name for the deployment policy use to locate the policy within the policy source.  Only used if deploymentPolicySource is non-null.
	     * @return Hashtable of devices on which creation of domain failed. The key is the Device object(key) and the value is the 
	     * Exception generated for that device (value)
	     *  
	     * @throws DeletedException if it fails to retrieve Device members
	     * @throws NotExistException if the resource no longer exists
	     * @throws LockBusyException id it fails to obtain a lock on resources
	     *
	     * @see Domain#deployConfiguration()
	     * @see Domain#setDeploymentPolicy(URLSource, String, String)
	     * @see Domain#setSourceConfiguration(URLSource)        
	     *         
	     * 
	     */
	    public Hashtable<Device,Exception> createManagedDomain(String domainName, URLSource domainSource, 
	    		URLSource deploymentPolicySource, String deploymentPolicyDomainName, String deploymentPolicyObjectName)
	    throws DeletedException, NotExistException, LockBusyException
	    {
	    	final String METHOD_NAME = "createManagedDomain"; //$NON-NLS-1$
	        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
	    	  
	        Hashtable<Device,Exception> failedDevices = new Hashtable<Device,Exception> ();
      		      	
	        Domain domain = null;
	        try { 
	        	this.lockNoWait();
	        	
			    Device[] managedDevices = this.getDeviceMembers(); 	        	
	        	if ( managedDevices.length < 1){
	    			String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
	        		throw new NotExistException(message, "wamt.clientAPI.DomainVersion.msNoMembers");
	        	}
	            for (Device device: managedDevices){  	    	    
	            	// retrieve specified domain on each managed device for configuration deployment
	            	try{
	            		
	            		if(!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)){	            		
	            			if (deploymentPolicySource!=null){
	            				
	            				String message = Messages.getString("wamt.clientAPI.Domain.noDepPolForYou");
	            				MissingFeaturesInFirmwareException e = new MissingFeaturesInFirmwareException(message);
			            		failedDevices.put(device,e ); 	
	            			} else {
			            		domain = device.createManagedDomain(domainName);
			            		if (domainSource != null) {
			            			domain.setSourceConfiguration(domainSource);			            			
			            		}
	            			}
	            			
	            		}else{	
		            		domain = device.createManagedDomain(domainName);
		            		if (domainSource!=null) {
		            			domain.setSourceConfiguration(domainSource);
		            		}
							domain.setDeploymentPolicy(deploymentPolicySource ,deploymentPolicyDomainName, deploymentPolicyObjectName); 
	            		} 
	            	}catch (MissingFeaturesInFirmwareException e){
	            		failedDevices.put(device,e );  	            		
	            	}catch(URISyntaxException e){
	            		failedDevices.put(device,e );  	        	       	  	  	        	        		  	        	        	
	            	}catch(DeletedException e){
	            		failedDevices.put(device,e );  	        	       	  	  	        	        		  	        	        	
	            	} catch (InUseException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device,e );  	
					} catch (AlreadyExistsInRepositoryException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device,e );  	
					} catch (LockBusyException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device,e );  	
					} catch (AMPException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device,e );  	
					} catch (DatastoreException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device,e );  	
					}  catch (InvalidParameterException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					}  catch (FullException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					}  catch (NotManagedException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					}
	            }  	           	        
	
	        } finally {
	        	this.unlock();
	        }
	    	logger.exiting(CLASS_NAME, METHOD_NAME);
	    	return failedDevices;
	    }       
    
    
    
		/**
	     * Removes specified managed domain from all devices in this managed set. The managed domain 
	     * and all child objects such as domain versions,deployment policy and deployment policy version
	     * will be deleted from the device. They will not be available from the repository.
	     * 
	     * @param domainName Name of domain to be removed.  
	     * @throws DeletedException if it fails to retrieve Device members
	     * @return Hashtable of devices on which removal of the specified managed domain failed. The key is the Device object(key) and the value is the 
	     * Exception generated for that device (value)
	     * 	    
	     * @throws DeletedException if it fails to retrieve Device members
	     * @throws NotExistException 
		 * @throws LockBusyException 
	     *
	     *         
	     * 
	     */
	    public Hashtable removeManagedDomain(String domainName)
	    throws DeletedException, NotExistException, LockBusyException
	   //@TODO -- implement deployment policy parm 
	 {
	    	final String METHOD_NAME = "removeManagedDomain"; //$NON-NLS-1$
	        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
	    	
  
	        Hashtable<Device,Exception> failedDevices = new Hashtable<Device,Exception> ();
	        //Domain domain = null;
	        String primaryKey = null;
	        try {
	        	this.lockNoWait();
	        	
			    Device[] managedDevices = this.getDeviceMembers(); 	        	
	        	if ( managedDevices.length < 1){
	    			String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
	        		throw new NotExistException(message, "wamt.clientAPI.DomainVersion.msNoMembers");
	        	}
	            for (Device device: managedDevices){  	    	    
	      	        // retrieve specified domain on each managed device for configuration deployment
	            	try{
	            		device.removeManagedDomain(domainName);
	            		primaryKey = device.getPrimaryKey();
	
	            	}catch(DeletedException e){
	            		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
	            				"Failed to remove domain: " + domainName + " on device: "+ device +  ":" + e.getMessage());
	            		failedDevices.put(device, e);
	            	} catch (InUseException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (InvalidParameterException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (NotEmptyException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (LockBusyException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (DatastoreException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					}catch (NullPointerException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					}catch (NotExistException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					}
	            }  	           	        
	
	        } finally {
	        	this.unlock();
	        }
	    	logger.exiting(CLASS_NAME, METHOD_NAME);
            return failedDevices;
	    }     
		/**
		 * Set the configuration source for the specified domain name if it is is present on any 
		 * of the devices in the managed set.
		 * 
		 * 
		 * @param domainName Name of domain for setting the configuration source
		 * @param configurationSource The configuration source to set
		 * @throws DeletedException if it fails to retrieve Device members
	     * @return Hashtable of devices on which setSourceConfiguration() failed. The key is the Device object(key) and the value is the 
	     * Exception generated for that device (value) on which setSourceConfiguration() failed      
		 * @throws NotExistException 
		 * @throws LockBusyException
		 *
		 * @see Domain#setSourceConfiguration(URLSource)
		 * @see URLSource
		 * @see #deploySourceConfigurationForDomain(String)
		 *         
		 * 
		 */
		public Hashtable setSourceConfigurationForDomain(String domainName, URLSource configurationSource)
		throws DeletedException, NotExistException, LockBusyException
		//@TODO -- implement deployment policy parm 
		{
			final String METHOD_NAME = "SetSourceConfigurationForDomain"; //$NON-NLS-1$
			logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});


			//MacroProgressContainer macroProgressContainer = new MacroProgressContainer(null);
			//ProgressContainer progressContainer = null;

			Device[] managedDevices = this.getDeviceMembers();   
	        Hashtable<Device,Exception> failedDevices = new Hashtable<Device,Exception> ();    	
			Domain domain = null;
			try {
				this.lockNoWait();
				
				if ( managedDevices.length < 1){
					String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
		    		throw new NotExistException(message, "wamt.clientAPI.DomainVersion.msNoMembers");
				}
				for (Device device: managedDevices){  	    	    
					// retrieve specified domain on each managed device for configuration deployment
					try{
						domain = device.getManagedDomain(domainName); 
						if (domain !=  null){
							domain.setSourceConfiguration(configurationSource);		      	        	
						}else{
            				String message = Messages.getString("wamt.clientAPI.ManagedSet.domainDoesNotExist", domainName);
							throw new NotExistException(message,"wamt.clientAPI.ManagedSet.domainDoesNotExist");     	        		
						}
					}catch(DeletedException dt){
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,  
								domainName + " :" + dt.getMessage()); 	          
						failedDevices.put(device,dt );	      	       		          
					}catch(NotExistException e){
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
								domainName + " :"  + device +  ":" + e.getMessage()); 	
						failedDevices.put(device,e );	  	          
					} catch (DatastoreException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device,e );	  
					}catch (NullPointerException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					}catch (URISyntaxException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (InvalidParameterException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (FullException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (NotManagedException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					}
				}  	           	        

			} finally {
				this.unlock();
			}
       
			logger.exiting(CLASS_NAME, METHOD_NAME);

			return failedDevices;
		}	 

		/**
		 * Sets the deployment policy for the specified domain name if it is present on any 
		 * devices in the managed set.
		 * 
		 * @param domainName Name of managed domain for which policy export options will be set  
		 * @param configurationSource The configuration source 
		 * @param policyDomainName name of theDomain that is used as an index into blob that will be deployed on the device
		 * @param policyName name of Deployment Policy which will be used as an index into the blob that will be deployed on a device 
		 * @throws DeletedException if it fails to retrieve Device members
	     * @return Hashtable of devices on which setDeplymentPolicy failed. The key is the Device object(key) and the value is the 
	     * Exception generated for that device (value) on which setDeploymentPolicy failed      
		 * @throws NotExistException    
		 * @throws NotExistException 
		 * @throws LockBusyException 
		 * @throws DeleteException
		 * 
		 * @see Domain#setSourceConfiguration(URLSource)
		 * @see URLSource  
		 *         
		 * 
		 */
		public Hashtable setDeploymentPolicyForDomain(String domainName, URLSource configurationSource, String policyDomainName, String policyName)
		throws DeletedException, NotExistException, LockBusyException
		//@TODO -- implement deployment policy parm 
		{
			final String METHOD_NAME = "setDeploymentPolicyForDomain"; //$NON-NLS-1$
			logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this}); 	

			Device[] managedDevices = this.getDeviceMembers();   
	        Hashtable<Device,Exception> failedDevices = new Hashtable<Device,Exception> ();           		      	
			Domain domain = null;
			try {
				this.lockNoWait();
				
				if ( managedDevices.length < 1){
					String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
		    		throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
				}
				for (Device device: managedDevices){  	    	    
					// retrieve specified domain on each managed device for configuration deployment
					try{

						domain = device.getManagedDomain(domainName);
						if(domain!=null){						
							domain.setDeploymentPolicy(configurationSource, policyDomainName, policyName);      	        				
						}else{
            				String message = Messages.getString("wamt.clientAPI.ManagedSet.domainDoesNotExist", domainName);
							throw new NotExistException(message,"wamt.clientAPI.ManagedSet.domainDoesNotExist");     	        		
						}
					}catch(DeletedException e){
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
								domainName + " :"  + device +  ":" + e.getMessage()); 	
						failedDevices.put(device,e);	   	          
        			}catch(NotExistException e){                       
        				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
  		        				domainName + " :"  + device +  ":" + e.getMessage()); 	
  				        failedDevices.put(device,e );                    	          
					} catch (MissingFeaturesInFirmwareException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device,e);	   	     
					} catch (DirtySaveException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device,e);	   	     
					} catch (DatastoreException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device,e);	   	     
					}catch (NullPointerException e) {
						// TODO Auto-generated catch block
	            		failedDevices.put(device, e);
					} catch (InvalidParameterException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					} catch (FullException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					} catch (NotManagedException e) {
						// TODO Auto-generated catch block
						failedDevices.put(device, e);
					}
				}  	           	        

			} finally {
				this.unlock();
			}

			logger.exiting(CLASS_NAME, METHOD_NAME);

			return failedDevices;
		}
		

		/**
		 * <p>The valid synchronization modes that can be set for a domain are DomainSynchronizationMode.MANUAL and DomainSynchronizationMode.AUTO.</p> 
		 * <p>By default the synchronization mode is set to DomainSynchronizationMode.MANUAL. Configuration deployment must
		 * be scheduled and done manually by an administrator.</p>
		 * <p>When synchronization mode is set to DomainSynchronizationMode.AUTO, domain configuration deployment is 
		 * synchronized automatically. An automatic domain deployment can occur when the following conditions are detected:
		 * <li>The persisted domain configuration differs from the domain on the device</li>
		 * <li>The timestamp of the persisted source files (domain source or deployment policy source) changes 
		 *   since the last deployment</li>
		 * <li>A method call changes the domain source or the effective deployment policy</li>
         * <li>A prior synchronized deploy operation has failed</li> 
		 * 
		 * <p> Set the synchronization mode on all managed domains with the specified domain name 
		 * 
		 * @param  domainName name of domain 
		 * @param  synchMode - The synchronization mode to set for the specified domains in this managed set. 
		 *         synchMode defaults to DomainSynchronizationMode.MANUAL. Valid synchronization 
         *         modes are DomainSynchronizationMode.MANUAL and DomainSynchronizationMode.AUTO
		 * @return Hashtable of failed Domains - with Domain(key)/Exception(value)
		 * @throws DeletedException this object has been deleted from the persisted
		 *         repository. The referenced object is no longer valid. You should
		 *         not be using a reference to this object.
		 * @throws NotExistException 
		 * @throws DeletedException
		 * @throws LockBusyException 
		 * 
		 * @see Domain#setSynchronizationMode(DomainSynchronizationMode)
		 * @see Manager#setDomainSynchronizationDaemonSleepMS(long)
		 */
		// * @see DomainSynchronizationDaemon
		public Hashtable setSynchronizationModeForDomain(String  domainName, DomainSynchronizationMode synchMode) throws DeletedException, NotExistException, LockBusyException {
			final String METHOD_NAME = "setSynchronizationModeForManagedDomains"; //$NON-NLS-1$
			logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this}); 				
			Hashtable<Device,Exception> failedDomains = new Hashtable<Device,Exception> ();           		      	
            Domain domain = null; 
			Device[] managedDevices = this.getDeviceMembers();   
			try {
				this.lockNoWait();
				
				if ( managedDevices.length < 1){
					String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
		    		throw new NotExistException(message,"wamt.clientAPI.DomainVersion.msNoMembers");
				}
				for (Device device: managedDevices){  	    	    
					// retrieve specified domain on each managed device for configuration deployment
					try{

						domain = device.getManagedDomain(domainName);
						if(domain!=null){						
							domain.setSynchronizationMode(synchMode);
						}else{
            				String message = Messages.getString("wamt.clientAPI.ManagedSet.domainDoesNotExist", domainName);
							throw new NotExistException(message, "wamt.clientAPI.ManagedSet.domainDoesNotExist");     	        		
						}
					}catch(DeletedException e){
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
								domainName + " :"  + device +  ":" + e.getMessage()); 	
						failedDomains.put(device,e);	   	          
					} catch (DirtySaveException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device,e);	   	     
					} catch (DatastoreException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device,e);	   	     
					}catch (NullPointerException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device, e);
					} catch (NotExistException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device, e);
					} catch (FullException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device, e);
					} catch (NotManagedException e) {
						// TODO Auto-generated catch block
						failedDomains.put(device, e);
					}
				}  	           	        

			} finally {
				this.unlock();
			}

			logger.exiting(CLASS_NAME, METHOD_NAME);

			return failedDomains;
		}

		/**
		 * Set the timeout value (in seconds) for checking the status of a domain 
         * quiesce or unquiesce operation for the specified domain on all devices members in this managed set
		 * 
		 * @param  domainName Name of domains on which to set the timeout value 
		 * @param  timeout timeout value for checking the status of a quiesce operation
		 * @return Hashtable of failed Devices - with Device(key)/Exception(value)
		 * @throws DeletedException this object has been deleted from the persisted
		 *         repository. The referenced object is no longer valid. You should
		 *         not be using a reference to this object.
		 * @throws NotExistException 
		 * @throws DeletedException
		 * @throws LockBusyException 
		 * @throws InvalidParameterException 
		 * 
		 * @see Domain#setQuiesceTimeout(int)
		 * @see Manager#OPTION_DEBUG_DOMAIN_QUIESCE_TIMEOUT
		 * 
		 */
		public Hashtable setQuiesceTimeoutForDomain(String  domainName, int timeout) throws DeletedException, NotExistException, LockBusyException, InvalidParameterException {
			final String METHOD_NAME = "setQuiesceTimeoutForDomain"; //$NON-NLS-1$
			logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this}); 	
			
	        if (timeout > 0 && timeout < Domain.DOMAIN_QUIESCE_MIN_VALUE) {
	        	timeout = Domain.DOMAIN_QUIESCE_MIN_VALUE;
	            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
	                    "quiesce timeout value is nonzero and less than" + Domain.DOMAIN_QUIESCE_MIN_VALUE + ", setting to " + Domain.DOMAIN_QUIESCE_MIN_VALUE); //$NON-NLS-1$
	        }

			Hashtable<Device,Exception> failedDomains = new Hashtable<Device,Exception> ();           		      	
            Domain domain = null; 
			Device[] managedDevices = this.getDeviceMembers();   

			if ( managedDevices.length < 1){
				String message = Messages.getString("wamt.clientAPI.DomainVersion.msNoMembers");
	    		throw new NotExistException(message, "wamt.clientAPI.DomainVersion.msNoMembers");
			}

			for (Device device: managedDevices){  	    	    
				// retrieve specified domain on each managed device for configuration deployment
				try{

					domain = device.getManagedDomain(domainName);
					if(domain!=null){						
						domain.setQuiesceTimeout(timeout);
					}else{
        				String message = Messages.getString("wamt.clientAPI.ManagedSet.domainDoesNotExist", domainName);
						throw new NotExistException(message, "wamt.clientAPI.ManagedSet.domainDoesNotExist");     	        		
					}
				}catch(DeletedException e){
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
							domainName + " :"  + device +  ":" + e.getMessage()); 	
					failedDomains.put(device,e);	   	          
				} catch (DirtySaveException e) {
					// TODO Auto-generated catch block
					failedDomains.put(device,e);	   	     
				} catch (DatastoreException e) {
					// TODO Auto-generated catch block
					failedDomains.put(device,e);	   	     
				} catch (NotExistException e) {
					// TODO Auto-generated catch block
					failedDomains.put(device, e);
				}
			}  	           	        

			logger.exiting(CLASS_NAME, METHOD_NAME);

			return failedDomains;
		}
}
