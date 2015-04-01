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

import org.apache.xmlbeans.impl.util.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Internal use only
 * <p/>
 * A class to track the versions of a Domain. There may be multiple
 * DomainVersions for a single Domain. 
 * <p />
 * This object (by way of the
 * {@link com.ibm.datapower.amt.dataAPI.StoredDomainVersion} member) has the 
 * {@link Blob} that contains the data that represents this domain's
 * configuration. 
 * <p />
 * If the domain is not the domain named "default", then
 * the Blob is a generic domain export, the same you would get via the
 * WebGUI. 
 * <p />
 * For more information refer to the javadoc for {@link Domain} and 
 * {@link Version}.
 * <p>
 * 
 * @see Domain
 * @version SCM ID: $Id: DomainVersion.java,v 1.7 2011/04/18 03:28:18 lsivakumxci Exp $
 */
//* <p>
//* Created on Aug 16, 2006
public class DomainVersion implements Version, Persistable {
    private volatile StoredDomainVersion persistence = null;
    /*
     * All the members of this class should be persisted. Please see any
     * class-specific members listed above, and also see all the members in the
     * parent class.
     */

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = DomainVersion.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Instantiate a DomainVersion object. This should be called by the Manager,
     * not by the client. 
     * @throws LockBusyException
     * @throws DeletedException
     * @throws NotExistException
     */
    DomainVersion(Domain domain, Date timestamp,
                  String userComment, Blob blob) 
        throws DatastoreException, LockBusyException, DeletedException {
        final String METHOD_NAME = "DomainVersion(Domain...)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, domain);
        Device device = domain.getDevice();
        device.lockWait();
        try {
            // no need to check that it doesn't already exist, just create it
            Manager manager = Manager.internalGetInstance();
            Repository repository = manager.getRepository();
            this.persistence = repository.createDomainVersion(domain.getStoredInstance(),
                                                              blob, userComment, timestamp);
            
            // set the rest of the non-persisted members
            
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.getStoredInstance(), this);
            
            String captureBlob = Configuration.get(Configuration.KEY_CAPTURE_VERSION_BLOB);
            if (Boolean.valueOf(captureBlob).booleanValue()) {
                try {
                    Date now = new Date();
                    long ms = now.getTime();
                    String filename = Configuration.getRootDirectory() + 
                        "domainBlob-" + device.getPrimaryKey() + "-" + domain.getName() + "-" + ms + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                "capturing blob in file " + filename); //$NON-NLS-1$
                    // blob is in base64 format
                    byte[] encoded = blob.getByteArray();
                    byte[] decoded = Base64.decode(encoded);
                    encoded = null;
                    Blob decodedBlob = new Blob(decoded);
                    decoded = null;
                    decodedBlob.toFile(filename);
                    decodedBlob = null;
                } catch (IOException e) {
                    // just eat it
                }
            }
            
            // get rid of one reference to "blob"
            blob = null;
            // check if any versions need to be trimmed AFTER this one is added,
            // otherwise the count of total versions already saved will not be one short.
            domain.trimExcessVersions();
        } finally {
        	device.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    DomainVersion(StoredDomainVersion storedDomainVersion) {
        final String METHOD_NAME = "DomainVersion(StoredDomainVersion)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        /*
         * Normally we would call Domain.trimExcessVersions() here, but let's
         * wait until everything is loaded and referenced so we can properly
         * figured out what the desired versions are before starting to look for
         * things to trim. Since this is called only via Manager.loadDatastore,
         * then do the invocation there.
         */
        
        this.persistence = storedDomainVersion;
        
        // get the rest of the non-persisted members
        // n/a

        // add it to the persistence mapper
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.add(this.persistence, this);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    static DomainVersion createInstanceFromDevice(Domain domain,
                                                  String userComment, 
                                                  Device device, 
                                                  ProgressContainer progressContainer) 
        throws AMPException, DatastoreException, 
               LockBusyException, DeletedException {
        final String METHOD_NAME = "createInstanceFromDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, domain);
        DomainVersion result = null;
        device.lockWait();
        try {
            Object[] args = new Object[] {domain.getRelativeDisplayName(), device.getDisplayName()};
            progressContainer.incrementCurrentStep(1,"wamt.clientAPI.DomainVersion.domExists_txt", args); //$NON-NLS-1$
            boolean found = domain.isPresentOn(device);
            if (!found) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        domain + " does not exist on " + device); //$NON-NLS-1$
                result = null;
                
            } else {
            	args = new Object[] {domain.getRelativeDisplayName(), device.getDisplayName()};
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "getting byte[]"); //$NON-NLS-1$
                progressContainer.incrementCurrentStep(1,"wamt.clientAPI.DomainVersion.retreivingDomain_txt", args); //$NON-NLS-1$
                Commands commands =  device.getCommands();
                DeviceContext deviceContext = device.getDeviceContext();
                byte[] bytes = commands.getDomain(deviceContext, domain.getName());
                Blob blob = new Blob(bytes);
                // get rid of one reference to "bytes"
                bytes = null;
                result = new DomainVersion(domain, new Date(), userComment, blob);
            }
        } finally {
        	device.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }

    /* javadoc inherited from interface */
    public Versionable getVersionedObject() throws DeletedException {
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredDomain storedDomain = this.getStoredInstance().getDomain();
        Domain domain = mapper.getVia(storedDomain);
        return(domain);
    }
    
    /**
     * Get the Domain that this object is a Version of. This is the same as
     * {@link #getVersionedObject()}, except that it returns an object with
     * type <code>Domain</code> instead of the parent type
     * <code>Versionable</code>.
     * 
     * @return the Domain that this object is a Version of.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Domain getDomain() throws DeletedException {
        return((Domain) this.getVersionedObject());
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Retrieve a reference to the DomainVersion that has the specified primary
     * key.
     * 
     * @param targetKey the primary key to search for
     * @return the DomainVersion that has the specified primary key. May return
     *         <code>null</code> if no DomainVersion with the specified
     *         primary key was found.
     * @see #getPrimaryKey()
     */
    public static DomainVersion getByPrimaryKey(String targetKey) {
        DomainVersion result = null;
        Manager manager = Manager.internalGetInstance();
        //TCB ManagedSet[] managedSets = manager.getManagedSets();
        Device[] devices = manager.getAllDevices();
        
    outermost: for (int deviceIndex=0; deviceIndex<devices.length; deviceIndex++) {
        Domain[] domains = null;
        try {
            domains = devices[deviceIndex].getManagedDomains();
        } catch (DeletedException e) {
            domains = new Domain[0];
        }
        for (int domainIndex=0; domainIndex<domains.length; domainIndex++) {
            Version[] versions = null;
            try {
                versions = domains[domainIndex].getVersions();
            } catch (DeletedException e2) {
                versions = new Version[0];
            }
            for (int versionIndex=0; versionIndex<versions.length; versionIndex++) {
                DomainVersion domainVersion = (DomainVersion) versions[versionIndex];
                String key = null;
                try {
                    key = domainVersion.getPrimaryKey();
                } catch (DeletedException e1) {
                    key = ""; //$NON-NLS-1$
                }
                if (key.equals(targetKey)) {
                    result = domainVersion;
                    break outermost;
                }
            }
        }
    }
        return(result);
    }
    
    /**
     * Part of the Persistable interface.
     * 
     * @return
     * @throws DeletedException
     */
    StoredDomainVersion getStoredInstance() throws DeletedException {
        final String METHOD_NAME = "getStoredInstance"; //$NON-NLS-1$
        if (this.persistence == null) {
        	String message = Messages.getString("NoPersistence");
            DeletedException e = new DeletedException(message,"NoPersistence"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return(this.persistence);
    }
    
    /**
     * Part of the Persistable interface.
     * @throws LockBusyException
     * @throws DatastoreException
     * @throws DeletedException
     */
    void destroy() throws LockBusyException, DeletedException, DatastoreException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        Domain domain = this.getDomain();
        Device device = domain.getDevice();
        try {
        	device.lockWait();
        	
            // delete child objects: not applicable
            
            // delete from persistence
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.remove(this.getStoredInstance());
            this.getStoredInstance().delete();
            this.persistence = null;
            
            // clear any references
        } catch (DeletedException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "has already been deleted"); //$NON-NLS-1$
            // object has already been deleted, just eat the exception
        } finally {
        	device.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /*
     * This method should be used only by NewDomainTask.
     */
//    /*
//     * This method should be used only by CopyTask and NewDomainTask, and serves
//     * as common code for both of them. For NewDomainTask, the sourceDevice
//     * will be null.
//     */
//TCB    This method has changed a LOT, basically a complete rewrite --> static DomainVersion createInstanceAndSync 
    static DomainVersion createInstanceAndSync(String domainName, 
//            Device sourceDevice,
            Device destinationDevice,
            ProgressContainer progressContainer)
    throws InUseException, NotExistException, InvalidCredentialsException, 
    DeviceExecutionException, AMPIOException, DeletedException, 
    LockBusyException, AMPException, DatastoreException, IOException {
        final String METHOD_NAME = "createInstanceAndSync"; //$NON-NLS-1$
        
        Domain destinationDomain = null;
        DomainVersion destinationDomainVersion = null;
//        Domain sourceDomain = null;
//        DomainVersion sourceDomainVersion = null;
        
        destinationDevice.lockWait();
        try {
            
            //TCB hack workaround Device[] destinationDevices = destinationManagedSet.getDeviceMembers();
        	Device[] destinationDevices = {destinationDevice};
//            if (sourceDevice == null) {
                // new management of existing domain
                StringCollection allDomains = destinationDevice.getAllDomainNames();
                if (!allDomains.contains(domainName)) {
                	Object[] args = new Object[] {domainName, destinationDevice.getDisplayName()};
                    String message = Messages.getString("wamt.clientAPI.DomainVersion.domainNotPresent", args);  //$NON-NLS-1$
                    NotExistException e = new NotExistException(message,"wamt.clientAPI.DomainVersion.domainNotPresent", args);
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                    throw(e);
                } else {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "The domain " + domainName + " is present in the device");     //$NON-NLS-1$ //$NON-NLS-2$
                }
//            } else {
//            	// perform copy from one managed set to another
//                sourceDomain = sourceDevice.getManagedDomain(domainName);
//                if (sourceDomain == null) {
//                	Object[] args = new Object[] {domainName, sourceDevice.getDisplayName()};
//                    String message = Messages.getString("wamt.clientAPI.DomainVersion.domainNotInMs", args); //$NON-NLS-1$
//                    NotExistException e = new NotExistException(message,"wamt.clientAPI.DomainVersion.domainNotInMs", args);
//                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
//                    throw(e);
//                } else {
//                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
//                            "The domain " + domainName + " is present in the source device"); //$NON-NLS-1$ //$NON-NLS-2$
//                }
//            }
            
            // make sure the Domain exists before attempting to create DomainVersion
            destinationDomain = destinationDevice.getManagedDomain(domainName);
            
            if (destinationDomain == null) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "Creating new domain " + domainName); //$NON-NLS-1$
                //TCB destinationDomain = new Domain(domainName, destinationManagedSet);
                destinationDomain = new Domain(domainName, destinationDevice);
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "Created new domain " + destinationDomain); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "Domain already exists in destination device"); //$NON-NLS-1$
            }
            
            // start tracking the ManagementStatus and DomainStatus
            for (int i=0; i<destinationDevices.length; i++) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "Setting domain status in device " + destinationDevices[i]); //$NON-NLS-1$
                destinationDevices[i].trackStatusOfDomain(destinationDomain);
                destinationDevices[i].setDomainStatus(destinationDomain, DomainStatus.UNKNOWN_DOMAIN_STATUS);
            }
            
            // create the DomainVersion
//            if (sourceDevice == null) {
                progressContainer.incrementCurrentStep(1,"wamt.clientAPI.DomainVersion.loadingInitVer_txt"); //$NON-NLS-1$
                destinationDomainVersion = DomainVersion.createInstanceFromDevice(destinationDomain,
                        "created from " + destinationDevice.getPrimaryKey(),  //$NON-NLS-1$
                        destinationDevice, progressContainer);
//            } else {
//                progressContainer.incrementCurrentStep(1,"wamt.clientAPI.DomainVersion.copyBlob_txt"); //$NON-NLS-1$
//                sourceDomainVersion = (DomainVersion) sourceDomain.getDesiredVersion();
//                Blob blob = sourceDomainVersion.getBlob();
//                Date timestamp = sourceDomainVersion.getTimestamp();
//                String userComment = sourceDomainVersion.getUserComment();
//                destinationDomainVersion = new DomainVersion(destinationDomain, timestamp, userComment, blob);
//            }
            
//            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
//                    "setting " + destinationDomainVersion + " as desired version"); //$NON-NLS-1$ //$NON-NLS-2$
//            destinationDomain.setDesiredVersionNoDeploy(destinationDomainVersion);
            
        } finally {
            destinationDevice.unlock();
        }
        return(destinationDomainVersion);
    }

    /**
     * Generate a background task to get a visual representation of the
     * differences of a domain between two versions. This DomainVersion will be
     * compared to the DomainVersion parameter. The diff will be performed on
     * the device, since the manager has no knowledge of the domain internals and
     * instead treats the DomainVersions' Blob as opaque. Since the diff is
     * performed on the device, this is considered a long-running task and is
     * why it returns a ProgressContainer.
     * 
     * @param that the other DomainVersion to be used for comparison to "this"
     * @return a ProgressContainer so you can monitor this progress of this
     *         long-running task. If the ProgressContainer indicates successful
     *         completion, you can retrieve a URL object from the
     *         ProgressContainer. It is expected that this URL can be opened in
     *         an external web browser that will connect to a predefined point
     *         on a DataPower device that will generate the diff content for the
     *         browser.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem reading or writing to/from
     *         the persistence repository.
     * @throws FullException the queue for the background tasks for the
     *         ManagedSet of the device is already full. The background task was
     *         not queued. Try again later when there are fewer background tasks
     *         on the queue.
     * @see "useCases section 4.8"
     */
    public ProgressContainer diff(DomainVersion that) throws DeletedException, DatastoreException, FullException {
        final String METHOD_NAME = "diff"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, that});
        BackgroundTask backgroundTask = BackgroundTask.createGetDiffURLTask(this, that);
        ProgressContainer result = backgroundTask.getProgressContainer();
        Manager manager = Manager.internalGetInstance();
        Domain domain = this.getDomain();
        Device device = domain.getDevice();
        ManagedSet managedSet = device.getManagedSet();
        manager.enqueue(backgroundTask, managedSet);
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }

    /* javadoc inherited from interface */
    public int getVersionNumber() throws DeletedException {
        return(this.getStoredInstance().getVersionNumber());
    }

    /* javadoc inherited from interface */
    public Date getTimestamp() throws DeletedException {
        return(this.getStoredInstance().getTimestamp());
    }

    /* javadoc inherited from interface */
    public String getUserComment() throws DeletedException {
        return(this.getStoredInstance().getUserComment());
    }

    /* javadoc inherited from interface */
    public void setUserComment(String userComment) throws DeletedException, DatastoreException, DirtySaveException {
        final String METHOD_NAME = "setUserComment"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting userComment: " + userComment); //$NON-NLS-1$
        this.getStoredInstance().setUserComment(userComment);
        Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
    }

    /* javadoc inherited from interface */
    public Blob getBlob() throws DeletedException {
        return(this.getStoredInstance().getBlob());
    }

    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String primaryKey = null;
        try {
            primaryKey = this.getPrimaryKey();
        } catch (DeletedException e) {
            primaryKey = "[deleted]"; //$NON-NLS-1$
        }
        return("DomainVersion[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is relative within the context of
     * a Domain within a ManagedSet. For example "3".
     * 
     * @return a human-readable name that represents this object.
     */
    public String getRelativeDisplayName() {
        String result = null;
        try {
            int number = this.getVersionNumber();
            result = String.valueOf(number);
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is absolute within the context of
     * the whole system. For example, "3 of domain domain1 in managed set set1".
     * 
     * @return a human-readable name that represents this object.
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String deviceName = null;
        String domainName = null;
        result = "Domain Verson:"+ this.getRelativeDisplayName();
        try {
            Domain domain = this.getDomain();
            domainName = domain.getAbsoluteDisplayName();
            Device device = domain.getDevice();
            deviceName = device.getDisplayName();
        } catch (DeletedException e) {
            domainName = "[deleted]"; //$NON-NLS-1$
            deviceName = "[unavailable]"; //$NON-NLS-1$
        }
        
        result = deviceName + "," + domainName;
        return(result);
    }

//    /* javadoc inherited from interface */
//    public boolean isInUse() throws DeletedException {
//        boolean result = false;
//        Domain domain = this.getDomain();
//        DomainVersion desiredVersion = (DomainVersion) domain.getDesiredVersion();
//        if (this == desiredVersion) {
//            result = true;
//        } else {
//            result = false;
//        }
//        return(result);
//    }
    
}
