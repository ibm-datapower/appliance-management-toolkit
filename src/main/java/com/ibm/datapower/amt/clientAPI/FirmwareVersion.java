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
import java.io.InputStream;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.Utilities;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A single instance of a firmware image.
 * <br/>
 * A firmware image for a device is uniquely identified by 5 pieces of information 
 * <ul>
 * <li>DeviceType (i.e., XI50)</li>
 * <li>ModelType (i.e., 9003)</li> 
 * <li>Strict feature list (i.e., "Tibco-EMS", "MQ", "TAM")</li>
 * <li>Non-strict feature list (i.e., "JAXP-API", "HSM")</li>
 * <li>Firmware level (i.e. "3.6.0.1")</li>
 * </ul>
 * The first four items in the list are represented by the {@link Firmware} object, because they 
 * are controlled by device hardware and licensing constraints. The last element is represented 
 * by this object (<code>FirmwareVersion</code>).
 * <p>
 * Although FirmwareVersion has an opaque integer <code>version</code> member
 * (i.e., 2) , it is deprecated in favor of the more meaningful member
 * <code>level</code> (i.e., "3.6.0.1").
 * <p>
 * For more information read the javadoc for the classes {@link Firmware}
 * and {@link Version}.
 * <p>
 * 
 * @see Firmware
 * @version SCM ID: $Id: FirmwareVersion.java,v 1.7 2011/04/18 03:28:45 lsivakumxci Exp $
 */
//* Created on Aug 16, 2006
public class FirmwareVersion implements Version, Persistable {
    private volatile StoredFirmwareVersion persistence = null;
    
    /**
     * The minimum firmware level that can be deployed to any ManagedSets or
     * added to the Manager. You can use this value to warn users what the
     * minimum allowed firmware level is, and it is used internally in the
     * method <code>meetsMinimumLevel</code>.
     * 
     * @see #meetsMinimumLevel(String)
     */
    //public static final String MINIMUM_LEVEL = "3.6.0.4"; //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final String CLASS_NAME = FirmwareVersion.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /*
     * All the attributes above this line will be persisted to the datastore.
     * Also see all the members in the parent class.
     */
    
    /*
     * Since the firmware blob can be shared across multiple ManagedSets, it is
     * not possible to include a parent reference to a single ManagedSet here.
     * The parent "object" is not specific to a ManagedSet. So whenever you want
     * to deploy() this firmware version to a ManagedSet you will have to pass
     * in the ManagedSet as a parameter. This is different than DomainVersion,
     * because they are specific to a Domain which are specific to a ManagedSet.
     */
    
    /**
     * @param object
     * @param versionNumber
     * @param timestamp
     * @param userComment
     * @throws AlreadyExistsException
     * @throws DeletedException
     * @throws AMPException
     * @throws IOException
     */
    FirmwareVersion(Firmware firmware, Date timestamp, String userComment, Blob blob) 
        throws AlreadyExistsException, UnsupportedVersionException, DatastoreException, DeletedException, AMPException, IOException {
        final String METHOD_NAME = "FirmareVersion(with Blob)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, firmware);
        Firmware.lockWait();
        try {

            /*
             * The firmare level is the value we will use here for Version. But we
             * won't have that until we parse the blob. So use a placeholder value
             * until we can parse the blob and get the real value. Since the final
             * value of the versionInt will be one, temporarily pick any number
             * larger than 1.
             */
            String level = FirmwareVersion.getLevelFromBlob(blob);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "found level: " + level); //$NON-NLS-1$
            Date manufactureDate = FirmwareVersion.getManufactureDateFromBlob(blob);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "found manufacture date: " + manufactureDate); //$NON-NLS-1$
            
            // check that the level doesn't already exist
            Version[] versions = firmware.getVersions();
            Object[] args = new Object[] {level, firmware.getDisplayName()};
            for (int i=0; i<versions.length; i++) {
                FirmwareVersion firmwareVersion = (FirmwareVersion) versions[i];
                if (firmwareVersion.getLevel().equals(level)) {
                    String message = Messages.getString("wamt.clientAPI.FirmwareVersion.levelAlreadyExists", args); //$NON-NLS-1$
                    AlreadyExistsException e = new AlreadyExistsException(message,"wamt.clientAPI.FirmwareVersion.levelAlreadyExists", args);
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                    throw(e);
                }
            }
            
            DeviceType deviceType = Utilities.getFirmwareDeviceType(blob);
            
            if(deviceType.equals(DeviceType.XC10)){
            	// no check for XC10 devices so far
            }else if (!meetsMinimumLevel(level)) { 
            	// check that the version meets the minimum level for DataPower devices
                args = new Object[] {level, MinimumFirmwareLevel.MINIMUM_LEVEL};
                String message = Messages.getString("wamt.clientAPI.FirmwareVersion.fwLevelNotSupported", args); //$NON-NLS-1$
                UnsupportedVersionException e = new UnsupportedVersionException(message,"wamt.clientAPI.FirmwareVersion.fwLevelNotSupported", args);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);           
            }
            
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "Adding firmware version " + level +  //$NON-NLS-1$
                        " to " + firmware); //$NON-NLS-1$
            Manager manager = Manager.internalGetInstance();
            Repository repository = manager.getRepository();
            
            Blob tempBlob = null;
            
            if(deviceType.equals(DeviceType.XC10)){
            	tempBlob = new Blob(blob.getByteArray());
            }else{
                /*
                 * encode it in base64 format before putting it in the repository,
                 * because doing the base64 encoding is expensive on such as large
                 * file. Then when it comes time to deploy this firmware, we won't
                 * need to do the base64 encoding at that time to insert it in the
                 * AMP SOAP message because the encoding was already performed.
                 * We only have to do this for DataPower devices.
                 */
            	tempBlob = blob.getBase64Encoded();
            }
            // get rid of one reference to "blob"
            blob = null;
            
            this.persistence = repository.createFirmwareVersion(firmware.getStoredInstance(),
            													tempBlob, 
                                                                level, manufactureDate, 
                                                                userComment, timestamp);
            
            // set the rest of the non-persisted members
            // n/a
            
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.getStoredInstance(), this);
            
            // check if any versions need to be trimmed AFTER this one is added.
            firmware.trimExcessVersions();
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    FirmwareVersion(Firmware firmware, StoredFirmwareVersion storedFirmwareVersion) {
        final String METHOD_NAME = "FirmwareVersion(StoredFirmwareVersion)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Firmware.lockWait();
        try {
            
            /*
             * Normally we would call Firmware.trimExcessVersions() here, but let's
             * wait until everything is loaded and referenced so we can properly
             * figured out what the desired versions are before starting to look for
             * things to trim. Since this is called only via Manager.loadDatastore,
             * then do the invocation there.
             */
            
            this.persistence = storedFirmwareVersion;

            // initialize rest of non-persisted members
            // n/a
            
            // add to PersistenceMapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);
            
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get the version level of this firmware. This is what should be used in
     * place of {@link Version#getVersionNumber()}. This value is embedded in
     * the firmware image header, so there is no
     * <code>setVersionNumber(String)</code> method. It will be automatically
     * populated by the method {@link Manager#addFirmware(Blob, String)}.
     * 
     * @return the firmware version level, for example "3.6.0.1"
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see Version#getVersionNumber()
     */
    public String getLevel() throws DeletedException {
        return(this.getStoredInstance().getLevel());
    }
    
    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Check if a specified firmware level meets the minimum supported level.
     * Because the manager uses AMP to manage a device, and 
     * the firmware contains the AMP implementation for the
     * device, and AMP was implemented in firmware level "3.6.0.4". 
     * <p>
     * This method can be used to verify that this FirmwareVersion has a fully
     * working AMP implementation. Otherwise the manager could be locked out from
     * managing the device. The manager should deny any request to deploy firmware to
     * devices that does not meet the minimum supported level.
     * <p>
     * 
     * @param levelToTest the level to test against the minimum supported firmware
     *        level.
     * @return true if this firmware meets or exceeds the minimum supported level, false
     *         otherwise.
     *
     */
//    * The minimum firmware level is specified in {@link MinimumFirmwareLevel#MINIMUM_LEVEL}. This
//    * check is performed by comparing the level of this firmware to the known
//    * minimum level.
    public static boolean meetsMinimumLevel(String levelToTest) {
    	return meetsMinimumLevel(MinimumFirmwareLevel.MINIMUM_LEVEL, levelToTest);
    }
    
    /**
     * Check if a specified firmware level meets the minimum supported level. 
     * This check is performed by comparing the level of this firmware to the known
     * minimum level. This is useful for checking to see if the device has a firmware 
     * level greater than or equal to a level required by a particular feature.
     * 
     * @param minimumLevel the minimum level to test against for a supported firmware
     *        level.
     * @param levelToTest the level to test against minimumLevel. This is usually the current 
     *        level of the device, but can be specified arbitrarily. 
     * @return true if this firmware meets or exceeds the minimum supported level, false
     *         otherwise.
     *  @see Device#meetsMinimumFirmwareLevel(String)
     */
    public static boolean meetsMinimumLevel(String minimumLevel, String levelToTestIn) {
        final String METHOD_NAME = "meetsMinimumLevel"; //$NON-NLS-1$
        if (minimumLevel == null || levelToTestIn == null){
        	return false;
        }
        
        // Format of the input strings are w.x.y.z
        // I am assuming here that only numerics and periods will be meaningful in a level String
        Boolean result = null;
        String levelToTest = levelToTestIn;

        // Search and replace string for equivalent firmware levels (i.e. Edge 1.0.0.x)
        floop: for (int i=0; i<MinimumFirmwareLevel.EQUIVALENT_FIRMWARE_LEVELS.length; i=i+2){
        	if (levelToTest.startsWith(MinimumFirmwareLevel.EQUIVALENT_FIRMWARE_LEVELS[i])){
        		levelToTest = MinimumFirmwareLevel.EQUIVALENT_FIRMWARE_LEVELS[i+1];
        		break floop;
        	}
        }

        // Comparison algorithm
        StringTokenizer minimumTokenizer = new StringTokenizer(minimumLevel, "."); //$NON-NLS-1$
        StringTokenizer thatTokenizer = new StringTokenizer(levelToTest, "."); //$NON-NLS-1$
        while (minimumTokenizer.hasMoreTokens() || thatTokenizer.hasMoreTokens()) {
            // get the next token. Assume a 0 in the short one if the other level String is longer
            String minimumToken = null;
            if (minimumTokenizer.hasMoreTokens()) {
                minimumToken = minimumTokenizer.nextToken();
            } else {
                minimumToken = "0"; //$NON-NLS-1$
            }
            String thatToken = null;
            if (thatTokenizer.hasMoreTokens()) {
                thatToken = thatTokenizer.nextToken();
            } else {
                thatToken = "0"; //$NON-NLS-1$
            }
            // remove any non-digits
            String minimumTokenClean = minimumToken.replaceAll("\\D", ""); //$NON-NLS-1$ //$NON-NLS-2$
            String thatTokenClean = thatToken.replaceAll("\\D", ""); //$NON-NLS-1$ //$NON-NLS-2$
            // convert the String digit to an int
            int minimumInt = Integer.parseInt(minimumTokenClean);
            int thatInt = Integer.parseInt(thatTokenClean);
            if (thatInt > minimumInt) {
                result = Boolean.TRUE;
                break;
            } else if (thatInt < minimumInt) {
                result = Boolean.FALSE;
                break;
            }
        }
        if (result == null) {
            if (!minimumTokenizer.hasMoreTokens() && !thatTokenizer.hasMoreTokens()) {
                // did both tokenizers run out? If so, then equal values
                result = Boolean.TRUE;
            } else {
                // hmm, shouldn't abort the loop above without a result.
                // Just assume the values are different.
                result = Boolean.FALSE;
            }
        }
        return(result.booleanValue());
    }
    
    /**
     * Retrieve a reference to the FirmwareVersion that has the specified primary key.
     * 
     * @param targetKey the primary key to search for
     * @return the Firmware that has the specified primary key. May return
     *         <code>null</code> if no Firmware with the specified primary key
     *         was found.
     * @see #getPrimaryKey()
     */
    public static FirmwareVersion getByPrimaryKey(String targetKey) {
        FirmwareVersion result = null;
        Manager manager = Manager.internalGetInstance();
        Firmware[] firmwares = manager.getFirmwares();
    outermost: for (int firmwareIndex=0; firmwareIndex<firmwares.length; firmwareIndex++) {
        Version[] versions = null;
        try {
            versions = firmwares[firmwareIndex].getVersions();
        } catch (DeletedException e1) {
            versions = new Version[0];
        }
        for (int versionIndex=0; versionIndex<versions.length; versionIndex++) {
            FirmwareVersion firmwareVersion = (FirmwareVersion) versions[versionIndex];
            String key = null;
            try {
                key = firmwareVersion.getPrimaryKey();
            } catch (DeletedException e) {
                key = ""; //$NON-NLS-1$
            }
            if (key.equals(targetKey)) {
                result = firmwareVersion;
                break outermost;
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
    StoredFirmwareVersion getStoredInstance() throws DeletedException {
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
     * @throws DeletedException
     * @throws DatastoreException
     */
    void destroy() throws DeletedException, DatastoreException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        Firmware.lockWait();
        try {
            
            // delete child objects: not applicable

            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting from persistence"); //$NON-NLS-1$
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.remove(this.getStoredInstance());
            this.getStoredInstance().delete();
            this.persistence = null;
            
            // clear any references
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Get the manufacture date of this firmware. This value is embedded in the
     * firmware image header, so there is no
     * <code>setManufactureDate(Date)</code> method. It will be automatically
     * populated by the method {@link Manager#addFirmware(Blob, String)}.
     * 
     * @return the data of manufacture of this firmware image
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Date getManufactureDate() throws DeletedException {
        return(this.getStoredInstance().getManufactureDate());
    }
    
    /* javadoc inherited from interface */
    public Versionable getVersionedObject() throws DeletedException {
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredFirmware storedFirmware = this.getStoredInstance().getFirmware();
        Firmware firmware = mapper.getVia(storedFirmware);
        return(firmware);
    }

    /**
     * The same as {@link #getVersionedObject()}, but casts the return value to
     * a <code>Firmare</code> class.
     * 
     * @return the Firmware object that this is a version of
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Firmware getFirmware() throws DeletedException {
        return((Firmware) this.getVersionedObject());
    }
    
    private static String getLevelFromBlob(Blob blob) throws AMPException, DeletedException, IOException {
        // is static because is called from constructor
        String result = null;
        result = Utilities.getFirmwareLevel(blob);
        return(result);
    }
    
    private static Date getManufactureDateFromBlob(Blob blob) throws DeletedException, IOException {
        // is static because is called from constructor
        Date result = null;
        result = Utilities.getFirmwareManufactureDate(blob);
        return(result);
    }

    /* javadoc inherited from interface */
    /**
     * @deprecated Although this method will return a valid version number
     *             (i.e., 2), you probably want to use the more meaningful
     *             method {@link #getLevel()}. This method should work and is
     *             supported, but probably isn't the value you are looking for.
     *             It is anticipated that a user interface would reference a
     *             FirmwareVersion via the level instead of versionNumber.
     */
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
                    "setting userComment on " + this + " to: " + userComment); //$NON-NLS-1$ //$NON-NLS-2$
        this.getStoredInstance().setUserComment(userComment);
        Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);        
    }

    /**
     * Get the Blob for this FirmwareVersion. The Blob for a FirmwareVersion
     * is the firmware file (the "scrypt2" file).
     * <p>
     * Beware that the content inside the Blob is base64 encoded. This is
     * different behavior than DomainVersion. The reason for
     * this is documented in
     * {@link Commands#setFirmware(DeviceContext, InputStream)}.
     * 
     * @return the Blob for this FirmwareVersion. The content is base64 encoded.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
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
        return("FirmwareVersion[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Get a human-readable name that represents this object, within the context
     * of a specific Firmware. This name may be used in user interfaces.
     * 
     * @return a human-readable name that represents this object. This is the
     *         level member of this object. For example, "3.6.1.0".
     * @see #getAbsoluteDisplayName()
     */
    public String getRelativeDisplayName() {
        String result = null;
        try {
            result = this.getLevel();
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object, within the context
     * of the system. This name may be used in user interfaces.
     * 
     * @return a human-readable name that represents this object. This includes
     *         the display name of the parent Firmware object. For example,
     *         "3.6.0.1 of firmware for device type XS40 and model type 9003
     *         with the strict features Tibco-EMS, TAM, and with the non-strict
     *         features JAXP-API, HSM".
     * @see #getRelativeDisplayName()
     * @see Firmware#getDisplayName()
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String versionName = this.getRelativeDisplayName();
        String firmwareName = null;
        try {
            Firmware firmware = this.getFirmware();
            firmwareName = firmware.getDisplayName();
        } catch (DeletedException e) {
            firmwareName = "[deleted]"; //$NON-NLS-1$
        }
        result = versionName + " of firmware " + firmwareName; //$NON-NLS-1$
        return(result);
    }

//    /**
//     * Internal use only.
//     * 
//     * @return an array of managed sets. This array may have zero elements.
//     * @throws DeletedException this object has been deleted from the persisted
//     *         repository. The referenced object is no longer valid. You should
//     *         not be using a reference to this object.
//     * @see #isInUse()
//     */
//    // returns an array of ManagedSets that have this FirmwareVersion as their desired version
//    public ManagedSet[] getUsers() throws DeletedException {
//        Vector<ManagedSet> users = new Vector<ManagedSet>();
//        Manager manager = Manager.internalGetInstance();
//        ManagedSet[] managedSets = manager.getManagedSets();
//        for (int i=0; i<managedSets.length; i++) {
//            if (managedSets[i].getDesiredFirmwareVersion() == this) {
//                users.add(managedSets[i]);
//            }
//        }
//        ManagedSet[] result = (ManagedSet[])users.toArray();
//        return(result);
//    }
//    
//    /**
//     * Internal use only.
//     * 
//     * @return true or false
//     */
//    //always false, because FirmwareVersion is not set at the ManagedSet level any more.
//    public boolean isInUse() throws DeletedException {
//        return(false);
//    }
}
