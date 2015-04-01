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

import java.util.HashSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotEmptyInRepositoryException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.dataAPI.StoredVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A container for firmware images.
 * <br/>
 * A firmware image for a device is uniquely identified by 5 pieces of information: 
 * <ul>
 * <li>DeviceType (i.e., XI50)</li>
 * <li>ModelType (i.e., 9003)</li> 
 * <li>Strict feature list (i.e., "Tibco-EMS", "MQ", "TAM")</li>
 * <li>Non-strict feature list (i.e., "JAXP-API", "HSM")</li>
 * <li>Firmware level (i.e., "3.6.0.1")</li>
 * </ul>
 * This object (<code>Firmware</code>) represents the first four items in the list. 
 * The last element is represented by {@link FirmwareVersion}. The <code>Firmware</code>
 * object serves as a container for one or more levels (e.g., one or more <code>FirmwareVersion</code> 
 * objects). Note that the firmware levels in more than one <code>Firmware</code> object 
 * may work for a particular device (e.g be deployable because of matching DeviceType, 
 * ModelType, and StrictFeatures), but one may be preferred over the other 
 * because of the non-strict features supported by the firmware.  
 * <p>  
 * <code>Firmware</code> objects are always created by the {@link Manager}. Firmware 
 * objects are automatically created, if necessary, when {@link Manager#addFirmware(Blob, String)}
 * is called. If a matching <code>Firmware</code> already exists in the manager, then a <code>
 * FirmwareVersion</code> is added to the <code>Firmware</code>. An array of all <code>Firmware</code> 
 * objects can be retrieved from the manager by calling {@link Manager#getFirmwares()}. 
 * <p> 
 * It is not possible to export a firmware image from a device, so the Firmware
 * class has a slightly different behavior than Domain. 
 * <p>
 * The versionNumber information from the Version interface doesn't mean
 * much, it is an opaque integer (i.e., "2") that is different than the Level
 * (i.e., "3.6.0.1") in a FirmwareVersion. Almost always when dealing with a
 * FirmwareVersion, you will want to look at {@link #getLevel(String)} instead 
 * of <code>getVersion</code>.
 * <p>
 * The organization of firmware is more complicated that you might assume. Take
 * a deep breath and read on.
 * <p>
 * DataPower firmware is packaged to be optimized for download size, installed
 * memory space, and some other constraints such as licenses (described below).
 * So a single firmware image has the binaries only for one DeviceType, one
 * ModelType, a (mostly) global set of non-strict features, and a specific set
 * of strict features. It is not the case that there is a unified single
 * firmware image that can be applied across a variety of devices.
 * <p>
 * In DataPower firmware, the DeviceType is important because each DeviceType
 * has a different set of capabilities, even if they have similar hardware. So
 * the firmware will be different for each DeviceType. A firmware image is
 * compatible only with the intended DeviceType.
 * <p>
 * Also, the ModelType infers the hardware revision. There are different
 * firmware binaries for different ModelTypes. A firmware image is compatible
 * only with the intended ModelType.
 * <p>
 * DataPower devices may have optional features enabled or disabled at the
 * factory. The enablement of these features is controlled via licenses embedded
 * in the device. Generally, but not always, these features are software-based
 * and do not require any special hardware. Thus, generally, these features are
 * implemented by including a license on the device and embedding extra code
 * (libraries) in the firmware.
 * <p>
 * There are two kinds of features: strict and non-strict. Non-strict features
 * may be distributed and installed to any device, even to a device that is not
 * licensed for that feature. In the case where a device is not licensed for a
 * non-strict feature but firmware for that non-strict feature is installed on
 * the device, the feature is simply disabled on that device. In comparison,
 * strict features may be distributed and installed only to devices that are
 * licensed for that feature. The reason for a feature being designated as
 * strict is usually because it is licensed from a third-party, and the license
 * agreement with the third-party limits distribution only to devices which are
 * licensed. In those cases it is not acceptable based on the license to
 * distribute code for a strict feature to a device that is not licensed for it,
 * even if the strict feature is disabled - the third-parties do not want the
 * code to be resident on the device even if it doesn't run. The manager limits the 
 * notion of features even further because it will not install a firmware that
 * does not contain all of the features (strict and non-strict) that a device
 * is licensed to use.  
 * <p>
 * For a description of how to determine if firmware is compatible with a
 * specific device, see {@link #isCompatibleWith(Device)}.
 * The lowest level of firmware supported a device may be checked with 
 * {@link FirmwareVersion#meetsMinimumLevel(String)}. Arbitrary version checking 
 * can be done with {@link FirmwareVersion#meetsMinimumLevel(String, String)}, and
 * {@link Device#meetsMinimumFirmwareLevel(String)} can be used to see if the 
 * actual firmware deployed to a device meets a specific firmware level requirement.
 * <p>
 * There are two versions of firmware packaging. The older is
 * <code>scrypt</code> or first generation, the newer is <code>scrypt2</code>
 * or next generation. Only the next generation is supported here, as we need
 * certain header information which is present only in the next generation.
 * <p>
 * 
 * @see FirmwareVersion
 * @version SCM ID: $Id: Firmware.java,v 1.6 2011/03/01 22:36:18 lsivakumxci Exp $
 */
public class Firmware implements Persistable, Versionable {
    private volatile StoredFirmware persistence = null;
    /*
     * All the attributes above this line will be persisted to the datastore.
     * Also see all the members in the parent class.
     */
    private static final Lock lock = new Lock("allFirmwareAndFirmwareVersions");
    private static final boolean DONT_LOG = false;
    private static final boolean DO_LOG = true;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = Firmware.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    static void lockWait() {
        final String METHOD_NAME = "lockWait"; //$NON-NLS-1$
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "about to lock firmware"); //$NON-NLS-1$
        Firmware.lock.lockWait();
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "obtained lock for firmware"); //$NON-NLS-1$
    }
    
    static void unlock() {
        final String METHOD_NAME = "unlock"; //$NON-NLS-1$
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "releasing lock for firmware"); //$NON-NLS-1$
        Firmware.lock.unlock();
    }

    /**
     * Create a new Firmware object.
     * 
     * @throws DeletedException
     * @throws LockBusyException
     * @throws DatastoreException
     * @see Manager#addFirmware(Blob)
     */
    Firmware(DeviceType deviceType, ModelType modelType, 
             StringCollection strictFeatures, StringCollection nonstrictFeatures)
        throws AlreadyExistsException, DatastoreException, DeletedException {
        final String METHOD_NAME = 
            "Firmware(DeviceType, ModelType, StringCollection, StringCollection)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME,
                        new Object[] {deviceType, modelType, strictFeatures, nonstrictFeatures});
        Firmware.lockWait();
        try {
            // check that it doesn't already exist
            Manager manager = Manager.internalGetInstance();
            Firmware firmware = manager.getFirmware(deviceType, modelType, 
                                                    strictFeatures, nonstrictFeatures);
            if (firmware != null) {
            	Object[] args = new Object[] {deviceType.getDisplayName(), modelType.getDisplayName(), strictFeatures.getDisplayName(), nonstrictFeatures.getDisplayName()};
                String message = Messages.getString("wamt.clientAPI.Firmware.fwExists", args); //$NON-NLS-1$
                AlreadyExistsException e = new AlreadyExistsException(message,"wamt.clientAPI.Firmware.fwExists", args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            
            // get or construct the StoredFirmware instance
            Repository repository = manager.getRepository();
            try {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "adding to persistence"); //$NON-NLS-1$
                this.persistence = repository.createFirmware(deviceType, modelType, 
                                                             strictFeatures, nonstrictFeatures);
            } catch (com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException e) {
                // convert to a clientAPI.AlreadyExistsException
            	Object[] args = new Object[] {deviceType.getDisplayName(), modelType.getDisplayName(), strictFeatures.getDisplayName(), nonstrictFeatures.getDisplayName()};
                String message = Messages.getString("wamt.clientAPI.Firmware.cannotCreateDup", args); //$NON-NLS-1$
                AlreadyExistsException e1 = new AlreadyExistsException(message, e,"wamt.clientAPI.Firmware.cannotCreateDup");
                logger.throwing(CLASS_NAME, METHOD_NAME, e1);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e1);
            }
            
            // populate the rest of the non-persisted members
            // n/a
            
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);
            
        } finally {
            Firmware.unlock();            
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    Firmware(StoredFirmware storedFirmware) {
        final String METHOD_NAME = "Firmware(StoredFirmware)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Firmware.lockWait();
        try {
            
            this.persistence = storedFirmware;
            
            // populate the rest of the non-persisted members
            // n/a
            
            // add it to the mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);

        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    void trimExcessVersions() {
        final String METHOD_NAME = "trimExcessVersions"; //$NON-NLS-1$
        Manager manager = null;
        int allowed = 0;
        Version[] versions = null;
        int have = 0;
        int highest = 0;
        try {
            manager = Manager.internalGetInstance();
            allowed = manager.getMaxVersionsToStore();
            versions = this.getVersions();
            have = versions.length;
            highest = this.getHighestVersionNumber();
        } catch (DeletedException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("CannotTrim"), e); //$NON-NLS-1$
            return;
        }
        if (have > allowed) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "need to trim, have " + have + //$NON-NLS-1$
                    " > allowed " + allowed + //$NON-NLS-1$
                    ", highest " + highest); //$NON-NLS-1$
            // work from the oldest version to the newest
            for (int i=1; (i<=highest) && (have>allowed); i++) {
                Version version = null;
                try {
                    version = this.getVersion(i);
                } catch (DeletedException e) {
                    // skip this one
                    version = null;
                    // if deleted then we don't really have it
                    have--;
                }
                if (version != null) {
                    try {
//                        if (!version.isInUse()) {
                            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                    "attempting to trim " + version); //$NON-NLS-1$
                            this.remove(version);
                            have--;
//                        }
                    } catch (DMgrException e) {
                        // just skip this one
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                "Unable to check/perform trimming of " + version, e); //$NON-NLS-1$
                    }
                }
            }
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "now have " + have + " versions"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "no versions need to be trimmed, have " + have + //$NON-NLS-1$
                    " <= allowed " + allowed); //$NON-NLS-1$
        }
    }

    /**
     * Get the DeviceType of this Firmware. Firmware is specific to device
     * types. The deviceType is immutable, so there is no
     * <code>setDeviceType</code> method.
     * 
     * @return the device type that this firmware must run on
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public DeviceType getDeviceType() throws DeletedException {
        return(this.getStoredInstance().getDeviceType());
    }

    /**
     * Get the ModelType of this Firmware. Firmware is specific to the model
     * type. The model type is immutable, so there is no
     * <code>setModelType</code> method.
     * 
     * @return the ModelType that this firmware must run on
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public ModelType getModelType() throws DeletedException {
        return(this.getStoredInstance().getModelType());
    }
    
    /**
     * Get all ModelTypes that are compatible with the ModelType of this Firmware. 
     * Firmware is specific to the model type. The model type is immutable, so there is no
     * <code>setModelType</code> method.
     * 
     * @return array of ModelType that are compatible with this firmware.  Returned array will always 
     *         contain the modelType of this firmware, since it will be compatible with itself.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public ModelType [] getCompatibleModelTypes() throws DeletedException {
        ModelType modelTypeOfFirmware = this.getStoredInstance().getModelType();
        HashSet<ModelType> modelTypes  = new HashSet<ModelType>();

        // Iterate thru each model type and add if compatible
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9001)) {
        	modelTypes.add(ModelType.TYPE_9001);            
        } 
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9002)) {
        	modelTypes.add(ModelType.TYPE_9002); 
        } 
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9003)) {
        	modelTypes.add(ModelType.TYPE_9003);
        } 
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9235)) {
        	modelTypes.add(ModelType.TYPE_9235);
        }                   
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9005)){
        	modelTypes.add(ModelType.TYPE_9005);
        }
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_9006)){
        	modelTypes.add(ModelType.TYPE_9006);
        }
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_7198)){
        	modelTypes.add(ModelType.TYPE_7198);
        }
        if (modelTypeOfFirmware.isCompatibleWith(ModelType.TYPE_7199)){
        	modelTypes.add(ModelType.TYPE_7199);
        }

        ModelType [] models = 
        	(ModelType[]) modelTypes.toArray( new ModelType[ modelTypes.size() ] ); 
        return models;
    }    
    
    /**
     * Get the strict features that are in this Firmware. The device you are
     * trying to apply this Firmware to must have exactly the same list of
     * strict licensed features. The strict features are immutable, so there is
     * no <code>setStrictFeatures</code> method.
     * 
     * @return the list of strict features that this firmware has and that the
     *         device must be licensed for
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public StringCollection getStrictFeatures() throws DeletedException {
        return(this.getStoredInstance().getStrictFeatures());
    }
    
    /**
     * Get the non-strict features that are in this Firmware. This Firmware must
     * have all the non-strict features that the device is licensed for.
     * Otherwise the device would not have the code for all the features it is
     * licensed for. The non-strict features are immutable, so there is no
     * <code>setNonstrictFeatures</code> method.
     * 
     * @return the list of non-strict features that this firmware has. This may
     *         be a superset of the licensed non-strict features for the device
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public StringCollection getNonstrictFeatures() throws DeletedException {
        return(this.getStoredInstance().getNonstrictFeatures());
    }
 
    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Get the highest version number of all the FirmwareVersions in this
     * Firmware. You may be more interested in the firmware level (i.e.,
     * "3.6.0.1") than the version number (i.e., 2), so instead consider using
     * {@link #getVersions()}, and {@link FirmwareVersion#getLevel()}.
     * 
     * @return the highest version number of all the FirmwareVersions, i.e., 2
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public int getHighestVersionNumber() throws DeletedException {
        StoredFirmware storedFirmware = this.getStoredInstance();
        int result = storedFirmware.getHighestVersionNumber();
        return(result);
    }
    
    /**
     * Retrieve a reference to the Firmware that has the specified primary key.
     * 
     * @param targetKey the primary key to search for
     * @return the Firmware that has the specified primary key. May return
     *         <code>null</code> if no Firmware with the specified primary key
     *         was found.
     * @see #getPrimaryKey()
     */
    public static Firmware getByPrimaryKey(String targetKey) {
        Firmware result = null;
        Manager manager = Manager.internalGetInstance();
        Firmware[] firmwares = manager.getFirmwares();
        for (int i=0; i<firmwares.length; i++) {
            String key = null;
            try {
                key = firmwares[i].getPrimaryKey();
            } catch (DeletedException e) {
                key = ""; //$NON-NLS-1$
            }
            if (key.equals(targetKey)) {
                result = firmwares[i];
                break;
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
    StoredFirmware getStoredInstance() throws DeletedException {
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
     * @throws NotExistException
     * @throws InvalidParameterException
     * @throws InUseException
     * @throws DeletedException
     * @throws DatastoreException
     * @throws NotEmptyInRepositoryException
     */
    void destroy() throws InUseException, InvalidParameterException, NotEmptyInRepositoryException, DeletedException, DatastoreException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Firmware.lockWait();
        try {
            
            // delete the DomainVersion objects
            Version[] versions = this.getVersions();
            for (int i=0; i<versions.length; i++) {
                try {
                    FirmwareVersion firmwareVersion = (FirmwareVersion) versions[i];
                    this.remove(firmwareVersion);
                } catch (DeletedException e) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                "deleted: " + e); //$NON-NLS-1$
                    // if the Version was deleted, don't worry about it
                }
            }
            
            // delete from persistence
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting from persistence"); //$NON-NLS-1$
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.remove(this.getStoredInstance());
            this.getStoredInstance().delete();
            this.persistence = null;
                        
            // Fix 9933
            Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
            // clear any references
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited by interface */
    public void remove(Version version) throws InUseException, InvalidParameterException, DeletedException, DatastoreException {
        final String METHOD_NAME = "remove"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, version);
        Firmware.lockWait();
        try {
            
            if (!(version instanceof FirmwareVersion)) {
                String message = Messages.getString("wamt.clientAPI.Firmware.noFwVersion", version); //$NON-NLS-1$
                InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Firmware.noFwVersion", version);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw(e);
            }
            FirmwareVersion firmwareVersion = (FirmwareVersion) version;
//            // don't remove it if it is in use by ManagedSet.desiredFirmwareVersion or Device.actualFirmwareVersion
//            if (firmwareVersion.isInUse()) {
//                ManagedSet[] managedSets = firmwareVersion.getUsers();
//                String msNamesString = "";
//                for (int i=0; i<managedSets.length; i++) {
//                    if (i > 0) {
//                        msNamesString += ", "; //$NON-NLS-1$
//                    }
//                    msNamesString += managedSets[i].getDisplayName();
//                }
//                String msgArgs[] = {firmwareVersion.getAbsoluteDisplayName(), msNamesString};
//                String message = Messages.getString("wamt.clientAPI.Firmware.cannotRemoveVer", msgArgs); //$NON-NLS-1$
//                InUseException e = new InUseException(message,"wamt.clientAPI.Firmware.cannotRemoveVer", msgArgs);
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
//                throw(e);
//            }
//            
            // Fix 9933: Still need to destroy first and then save to repository, otherwise, the record won't be changed.
            firmwareVersion.destroy();
            Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);            
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited from interface */
    public Version[] getVersions() throws DeletedException {
        final String METHOD_NAME = "getVersions"; //$NON-NLS-1$
        FirmwareVersion[] result = null;
        StoredVersion[] storedFirmwareVersions = this.getStoredInstance().getVersions();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        Vector firmwareVersions = new Vector();
        for (int i=0; i<storedFirmwareVersions.length; i++) {
            StoredFirmwareVersion storedFirmwareVersion = (StoredFirmwareVersion) storedFirmwareVersions[i];
            FirmwareVersion firmwareVersion = null;
            try {
                firmwareVersion = mapper.getVia(storedFirmwareVersion);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            storedFirmwareVersion + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                firmwareVersion = null;
            }
            if (firmwareVersion != null) {
                firmwareVersions.add(firmwareVersion);
            }
        }
        result = new FirmwareVersion[firmwareVersions.size()];
        for (int i=0; i<firmwareVersions.size(); i++) {
            result[i] = (FirmwareVersion) firmwareVersions.get(i);
        }
        return(result);
    }
    
    /* javadoc inherited from interface */
    /**
     * @deprecated Instead of this method you probably want to use
     *             {@link #getLevel(String)}. A level is a
     *             String value (i.e., "3.6.1.0") which is probably what you are
     *             searching for. The version number in a FirmwareVersion
     *             object will be an opaque int and not anything meaningful to
     *             the user. This method will return a valid value when invoked
     *             with a valid parameter (i.e., 2), but you probably want to
     *             use <code>getLevel</code>.
     */
    public Version getVersion(int targetVersionNumber) throws DeletedException {
        FirmwareVersion result = null;
        StoredFirmwareVersion matchingStoredFirmwareVersion = null;
        StoredVersion[] storedVersions = this.getStoredInstance().getVersions();
        for (int i=0; i<storedVersions.length; i++) {
            if (storedVersions[i].getVersionNumber() == targetVersionNumber) {
                matchingStoredFirmwareVersion = (StoredFirmwareVersion) storedVersions[i];
                break;
            }
        }
        if (matchingStoredFirmwareVersion != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(matchingStoredFirmwareVersion);
        }
        return(result);
    }
    
    /**
     * Get the FirmwareVersion object that corresponds to the requested level.
     * The FirmwareVersion must exist as a Version of this Firmware object.
     * For example it is possible to have a "3.6.0.1" level for XS40 devices
     * but not for XI50 devices - using this method to look for the "3.6.0.1"
     * level within the XI50 Firmware object will return null.
     * 
     * @param targetLevel the FirmwareVersion level to search for, i.e.,
     *        "3.6.0.1"
     * @return the FirmwareVersion object that has the requested level. If no
     *         FirmwareVersion object exists with the requested level, then it
     *         returns <code>null</code>.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public FirmwareVersion getLevel(String targetLevel) throws DeletedException {
        FirmwareVersion result = null;
        StoredFirmwareVersion matchingStoredFirmwareVersion = null;
        StoredVersion[] storedVersions = this.getStoredInstance().getVersions();
        for (int i=0; i<storedVersions.length; i++) {
            StoredFirmwareVersion storedFirmwareVersion = (StoredFirmwareVersion) storedVersions[i];
            if (storedFirmwareVersion.getLevel().equals(targetLevel)) {
                matchingStoredFirmwareVersion = storedFirmwareVersion;
                break;
            }
        }
        if (matchingStoredFirmwareVersion != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(matchingStoredFirmwareVersion);
        }
        return(result);
    }

//    /* javadoc inherited from interface */
//    /**
//     * @deprecated This method should not be used.
//     *             This method will always return <code>null</code> when invoked.
//     */
//    public Version getDesiredVersion() {
//        return null;
//    }
//
//    /* javadoc inherited from interface */
//    /**
//     * @deprecated This method should not be used.
//     *             This method will always throw an
//     *             <code>InvalidParameterException</code> when invoked.
//     */
//    public ProgressContainer setDesiredVersion(Version version) 
//        throws InvalidParameterException {
//        final String METHOD_NAME = "setDesiredVersion"; //$NON-NLS-1$
//        String message = Messages.getString("wamt.clientAPI.Firmware.methodNotAv");
//        InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Firmware.methodNotAv"); //$NON-NLS-1$
//        logger.throwing(CLASS_NAME, METHOD_NAME, e);
//        throw(e);
//    }
    
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
        return("Firmware[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces.
     * 
     * @return a human-readable name that represents this object. For example,
     *         "for device type XS40 and model type 9003 with the strict
     *         features Tibco-EMS, TAM, and with the non-strict features
     *         JAXP-API, HSM"
     */
    public String getDisplayName() {
        String result = null;
        try {
            result = "for device type "; //$NON-NLS-1$
            DeviceType deviceType = this.getDeviceType();
            result += deviceType.getDisplayName();
            
            result += " and model type "; //$NON-NLS-1$
            ModelType modelType = this.getModelType();
            result += modelType.getDisplayName();
            
            result += " with the strict features "; //$NON-NLS-1$
            StringCollection strictFeatures = this.getStrictFeatures();
            result += strictFeatures.getDisplayName();

            result += " and with the non-strict features "; //$NON-NLS-1$
            StringCollection nonstrictFeatures = this.getNonstrictFeatures();
            result += nonstrictFeatures.getDisplayName();
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }

    /**
	 * Test if the Firmware and the Device are compatible, which means that the
	 * Firmware could run on the Device.
	 * <p>
	 * Please review the class javadoc at the top of this file for an
	 * explanation of the 5-tuple that uniquely identifies firmware, including
	 * strict and non-strict features.
	 * <p>
	 * In the examples below, "JAXP-API" and "HSM" are non-strict features, and
	 * "Tibco-EMS" is a strict feature. (There are many more features in real
	 * life than these three.) We will assume that the firmware has the features
	 * ["JAXP-API", "HSM", "Tibco-EMS"] and the device is licensed for the
	 * features ["JAXP-API", "Tibco-EMS"].
	 * <p>
	 * All of the four following conditions must exist for a firmware to be
	 * compatible with a device:
	 * <ol>
	 * <li>the DeviceType of the firmware and the device must match exactly.
	 * For example, <code>XS40=XS40</code></li>
	 * <li>the ModelType of the firmware and the device must match exactly. For
	 * example, <code>9003=9003</code> or <code>other=9002</code></li>
	 * <li>all the features in the firmware must be a superset of the licensed
	 * features of the device. In other words, all the features that the device
	 * is licensed for must be present in the firmware. (Based on the
	 * implementation of
	 * {@link com.ibm.datapower.amt.StringCollection#isSupersetOf(StringCollection) isSupersetOf},
	 * that method evaluates to <code>true</code> if there are zero extra
	 * elements in the sets, meaning that each set has exactly the same elements
	 * as the other set.) The reason for doing this check is that we never want
	 * the code for a licensed feature to be missing from the device, which
	 * would effectively disable the feature. While this makes sense for
	 * non-strict features, it may sound counter-intuitive for strict features
	 * because no extra strict features from the firmware are allowed on the
	 * device. However, because the device isn't able to tell us via AMP which
	 * of its features are strict versus non-strict (we get only a unified list
	 * of the device's feature when we query the device), and when this
	 * condition is combined with the condition below, it all works. For
	 * example,
	 * <code>firmware["JAXP", "HSM", "Tibco-EMS"].isSupersetOf(device["JAXP", "Tibco-EMS"])</code>
	 * </li>
	 * <li>the features of the device must be a superset of the firmware's
	 * strict features. In other words, the device must be licensed for all the
	 * strict features present in the firmware. An attempt to install a firmware
	 * with strict features on a device that is not licensed for that strict
	 * feature will result in a device's failure to install that firmware. Since
	 * we want to prevent users from going down a path that we know ahead of
	 * time will result in failure, we perform that check here to catch it
	 * early. If the firmware has any strict features not present in the device
	 * then this test will fail. For example,
	 * <code>device["JAXP", "Tibco-EMS"].isSupersetOf(firmware["Tibco-EMS"])</code>
	 * </li>
	 * </ol>
	 * <p />
	 * The firmware level (i.e., "3.6.1.2") is not included in the compatibility
	 * test. If the other 4 elements of the 5-tuple indicate that the firmware
	 * is compatible with the device, then any version may be applied to the
	 * device. This is what is meant as upgrading or downgrading firmware.
	 * <p />
	 * The <code>get*</code> methods from the <code>Utilities</code> class
	 * listed as "See Also" below that take a <code>Blob</code> parameter are
	 * where the metadata is retrieved from the firmware images. The Device
	 * constructor is where the metadata is retrieved from the device.
	 * 
	 * @param device
	 *            the device to check for compatibility with this Firmware
	 * @return true if the compatible, false otherwise
	 * @see #getIncompatibilityReason(Device)
	 * @see com.ibm.datapower.amt.amp.Utilities#getFirmwareDeviceType(Blob)
	 * @see com.ibm.datapower.amt.amp.Utilities#getFirmwareModelType(Blob)
	 * @see com.ibm.datapower.amt.amp.Utilities#getNonStrictFirmwareFeatures(Blob)
	 * @see com.ibm.datapower.amt.amp.Utilities#getStrictFirmwareFeatures(Blob)
	 * @see com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower.amt.amp.DeviceContext)
	 */
    public boolean isCompatibleWith(Device device) {
        boolean result = true;
        try {
        	this.assertCompatibility(device);
        } catch (ClientAPIException e) {
        	result = false;
        }
        return(result);
    }
    
    /**
	 * Test if the Firmware and the Device are compatible. This method should be
	 * invoked only within this package, which is why it is not public. Because
	 * it is an internal function, don't cause any incompatibilities to appear
	 * above Level.FINER in the log by using <code>DONT_LOG</code>. This
	 * method is for checking compatibility before a Device object has been
	 * created.
	 * 
	 * @param deviceDeviceType
	 *            the DeviceType of the Device
	 * @param deviceModelType
	 *            the ModelType of the Device
	 * @param deviceFeatures
	 *            all the features of the Device
	 * @return true if this Device is compatible with this Firmware, false
	 *         otherwise
	 */
    boolean isCompatibleWith(DeviceType deviceDeviceType,
    		ModelType deviceModelType, StringCollection deviceFeatures) {
    	boolean result = true;
    	try {
    		this.assertCompatibility(null, deviceDeviceType, deviceModelType, deviceFeatures, DONT_LOG);
    	} catch (ClientAPIException e) {
    		result = false;
    	}
    	return(result);
    }
    
    /**
	 * Gets the text description of the reason why this Firmware is incompatible
	 * with the device of the specified properties. This text may be used in
	 * user interface messages. If the the firmware and device are compatible,
	 * this will return a null String (a null incompatibility means they are
	 * compatible).
	 * 
	 * @param device
	 *            the Device to check for compatibility to this Firmware
	 * @return an explanation why the Firmware is incompatible. Will return null
	 *         if the Firmware is compatible with the Device (a null
	 *         incompatibility means they are compatible).
	 * @see #isCompatibleWith(Device)
	 * @see Device#createDevice(String, String, String, String, int)
	 */
    public String getIncompatibilityReason(Device device) {
    	String result = null;
    	try {
    		this.assertCompatibility(device);
    	} catch (ClientAPIException e) {
    		result = e.getMessage();
    	}
    	return(result);
    }
    
    /**
	 * Assume that the Device is compatible with this Firmware. Otherwise throw
	 * an exception. This is not a standard Java assertion that is disabled by
	 * default, this assertion will always run because it is a regular method -
	 * don't get confused by the "assert" name thinking it is the Java keyword.
	 * 
	 * @param device
	 *            the device to check for compatibility with this firmware
	 * @throws DeletedException
	 * @throws UnlicensedFeaturesInFirmwareException
	 * @throws MissingFeaturesInFirmwareException
	 * @throws ModelTypeIncompatibilityException
	 * @throws DeviceTypeIncompatibilityException
	 * @throws DeletedException
	 *             the device or the firmware has been deleted from persistence
	 * @throws DeviceTypeIncompatibilityException
	 *             the DeviceType of the firmware and the device are not equal
	 * @throws ModelTypeIncompatibilityException
	 *             the ModelType of the firmware and the device are not equal
	 * @throws MissingFeaturesInFirmwareException
	 *             the firmware does not provide the libraries for some of the
	 *             device's features
	 * @throws UnlicensedFeaturesInFirmwareException
	 *             the firmware has additional libraries for strict features
	 *             that the device is not licensed for. It is OK to have extra
	 *             non-strict features, but the firmware can not provide extra
	 *             strict features.
	 */
    void assertCompatibility(Device device)
			throws DeviceTypeIncompatibilityException,
			ModelTypeIncompatibilityException,
			MissingFeaturesInFirmwareException,
			UnlicensedFeaturesInFirmwareException, DeletedException {
		this.assertCompatibility(device, null, null, null, DO_LOG);
    }
    
    private StringCollection removeVersionString(StringCollection features){
    	StringCollection temp = new StringCollection();
        for(int i=0;i<features.size();i++){
      	  String feature = features.get(i);
      	  int index = feature.indexOf('_');
      	  temp.add(index>0 ? feature.substring(0, index) : feature);
        }
        return temp;
    }
    
    /* this is where the real assertion testing takes place, the other methods are just wrappers */
    private void assertCompatibility(Device device, DeviceType deviceDeviceType,
            ModelType deviceModelType, StringCollection deviceFeatures, 
            boolean logAboveFiner)
    throws DeletedException, DeviceTypeIncompatibilityException,
    ModelTypeIncompatibilityException,
    MissingFeaturesInFirmwareException,
    UnlicensedFeaturesInFirmwareException {
		final String METHOD_NAME = "assertCompatibility";
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, device, deviceDeviceType, deviceModelType, deviceFeatures});
        try {
        	if (device != null) {
				deviceDeviceType = device.getDeviceType();
				deviceModelType = device.getModelType();
				deviceFeatures = device.getFeatureLicenses();
        	}

            DeviceType firmwareDeviceType = this.getDeviceType();
            ModelType firmwareModelType = this.getModelType();
            StringCollection firmwareStrictFeatures = removeVersionString(this.getStrictFeatures());
            StringCollection firmwareNonstrictFeatures = removeVersionString(this.getNonstrictFeatures());
            StringCollection firmwareAllFeatures = new StringCollection(firmwareStrictFeatures, firmwareNonstrictFeatures);
            
            if (!(firmwareDeviceType.isCompatibleWith(deviceDeviceType))) {
                // device types need to be compatible
            	String messageKey = null;
            	Object[] args = null;
            	if (device != null) {
            		// if we know the device name, add it
            		messageKey = "wamt.clientAPI.Firmware.devTypeMismatchDev";  //$NON-NLS-1$
            		args = new Object[] {firmwareDeviceType.getDisplayName(),
            				deviceDeviceType.getDisplayName(),
            				device.getDisplayName()};
            	} else {
            		messageKey = "wamt.clientAPI.Firmware.devTypeMismatch";  //$NON-NLS-1$
            		args = new Object[] {firmwareDeviceType.getDisplayName(), 
            			deviceDeviceType.getDisplayName()};
            	}
                String message = Messages.getString(messageKey, args);
                DeviceTypeIncompatibilityException e = new DeviceTypeIncompatibilityException(message,
                        messageKey, args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                if (logAboveFiner) {
                	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, e.getMessage());
                }
                throw(e);
            } else if (!(firmwareModelType.isCompatibleWith(deviceModelType))) {
                // model types need to be compatible (9002==9002, 9003~=9004)
            	String messageKey = null;
            	Object[] args = null;
            	if (device != null) {
            		// if we know the device name, add it
            		messageKey = "wamt.clientAPI.Firmware.modelTypeMismatchDev"; //$NON-NLS-1$
            		args = new Object[] {firmwareModelType.getDisplayName(),
            				deviceModelType.getDisplayName(),
            				device.getDisplayName()};
            	} else {
            		messageKey = "wamt.clientAPI.Firmware.modelTypeMismatch"; //$NON-NLS-1$
            		args = new Object[] {firmwareModelType.getDisplayName(), 
            				deviceModelType.getDisplayName()};
            	}
                String message = Messages.getString(messageKey, args);
                ModelTypeIncompatibilityException e = new ModelTypeIncompatibilityException(message, 
                        messageKey, args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                if (logAboveFiner) {
                	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, e.getMessage());
                }
                throw(e);
            } else if (!isSupersetOf(firmwareAllFeatures, deviceFeatures)) {
                // firmware should supply everything the device needs when RequireSupportInScrypt() returns true
            	String messageKey = null;
            	Object[] args = null;
            	if (device != null) {
            		messageKey = "wamt.clientAPI.Firmware.notAllFeaturesDev"; //$NON-NLS-1$
            		args = new Object[] {firmwareAllFeatures.getDisplayName(), 
                			deviceFeatures.getDisplayName(),
                			device.getDisplayName()};
            	} else {
            		messageKey = "wamt.clientAPI.Firmware.notAllFeatures"; //$NON-NLS-1$
            		args = new Object[] {firmwareAllFeatures.getDisplayName(), 
                			deviceFeatures.getDisplayName()};
            	}
                String message = Messages.getString(messageKey, args);
                MissingFeaturesInFirmwareException e = new MissingFeaturesInFirmwareException(message,
                        messageKey, args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                if (logAboveFiner) {
                	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, e.getMessage());
                }
                throw(e);
            } else if (!(deviceFeatures.isSupersetOf(firmwareStrictFeatures))) {
                // all strict features should be on device
            	StringCollection missing = new StringCollection();
            	for (int i=0; i<firmwareStrictFeatures.size(); i++) {
            		String feature = firmwareStrictFeatures.get(i);
            		if (!deviceFeatures.contains(feature)) {
            			missing.add(feature);
            		}
            	}
            	String messageKey = null;
            	Object[] args = null;
            	if (device != null) {
            		messageKey = "wamt.clientAPI.Firmware.extraFeaturesDev"; //$NON-NLS-1$
            		args =  new Object[] {firmwareStrictFeatures.getDisplayName(), 
                			firmwareNonstrictFeatures.getDisplayName(),
                			device.getDisplayName(),
                			deviceFeatures.getDisplayName(),
                			missing.getDisplayName()};
            	} else {
            		messageKey = "wamt.clientAPI.Firmware.extraFeatures"; //$NON-NLS-1$
            		args = new Object[] {firmwareStrictFeatures.getDisplayName(), 
            			firmwareNonstrictFeatures.getDisplayName(),
            			deviceFeatures.getDisplayName(),
            			missing.getDisplayName()};
            	}
                String message = Messages.getString(messageKey, args);
                UnlicensedFeaturesInFirmwareException e = new UnlicensedFeaturesInFirmwareException(message,
                        messageKey, args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                if (logAboveFiner) {
                	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, e.getMessage());
                }
                throw(e);
            }
        } finally {
        	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
        			"lack of an 'RETURN true' immediately below means incompatible"); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME, Boolean.TRUE);
    }
    
    private boolean isSupersetOf(StringCollection firmwareAllFeatures, StringCollection deviceFeatures) {
    	final String METHOD_NAME = "isSupersetOf";
    	for ( int i=0; i < deviceFeatures.size(); i++ ) {
    		String deviceFeature = deviceFeatures.get(i);
    		if ( RequireSupportInScrypt(deviceFeature) ) {
    			if ( !firmwareAllFeatures.contains(deviceFeature) ) {
    				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Firmware does not contain " + deviceFeature + " in devcice");
    				return (false);
    			}
    		}
    	}
    	return (true);
    }
    
    private boolean RequireSupportInScrypt(String feature) {
    	int index = feature.indexOf('_');
    	feature = index>0 ? feature.substring(0, index) : feature;  
    	    	
    	if ( feature.equals("MQ") )	return true;
    	else if ( feature.equals("TAM")) return true;
    	else if ( feature.equals("DataGlue") ) return true;
    	else if ( feature.equals("JAXP_API") ) return true;
    	else if ( feature.equals("PKCS7_SMIME") ) return true;
    	else if ( feature.equals("SQL_ODBC") ) return true;
    	else if ( feature.equals("Tibco_EMS") ) return true;
    	else if ( feature.equals("WebSphere_JMS") ) return true;
    	else if ( feature.equals("AppOpt") ) return true;
    	else if ( feature.equals("Tibco_RV") ) return true;
    	else if ( feature.equals("DCO") ) return true;
    	
    	return false;
    }
        
}
