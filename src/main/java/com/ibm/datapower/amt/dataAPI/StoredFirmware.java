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
package com.ibm.datapower.amt.dataAPI;

import java.util.Date;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Blob;

/**
 * The information related to {@link com.ibm.datapower.amt.clientAPI.Firmware} that must be  maintained and 
 * persisted for a firmware that can
 * be deployed to a device. This includes the device type, model type, strict features list, 
 * and non-strict feature list. 
 * It acts as a container for different levels of firmware, i.e. FirmwareVersion objects for a specific 
 * device type.
 * 
 */
public interface StoredFirmware extends Persistable {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
    /**
     * Gets the deviceType of this StoredFirmware. StoredFirmware is specific to
     * a combination of device type + model type + features. The deviceType is
     * immutable, so there is no <code>setDeviceType</code> method.  This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getDeviceType() }
     * <p>
     * Note:  The Local File System implementation persists device type as an attribute on firmwares element in the
     * WAMT.repository.xml file. 
     * </p> 
     * 
     * @return the device type that this firmware must run on
     */
    public DeviceType getDeviceType();

    /**
     * Gets the modelType of this StoredFirmware. StoredFirmware is specific to a
     * combination of device type + model type + features. The deviceType is
     * immutable, so there is no <code>setModelType</code> method. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getModelType() }
     * <p>
     * Note:  The Local File System implementation persists device model type as an attribute on firmwares element in the
     * WAMT.repository.xml file. 
     * </p>
     * 
     * @return the device type that this firmware must run on
     */
    public ModelType getModelType();

    /**
     * Gets the StringCollection that represents the strict features supported by
     * this firmware. If a Firmware supports a strict feature, it can
     * <strong>ONLY </strong> be installed on devices having a license for that
     * feature.
     * <p>
     * StoredFirmware is specific to a combination of device type + model type +
     * strictfeatures + nonstrictFeatures. The strictFeatures attribute is
     * immutable, so there is no <code>setStrictFeatures</code> method. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getStrictFeatures() }
     * <p>
     * Note:  The Local File System implementation persists the collection of strict features as an attribute on firmwares element in the
     * WAMT.repository.xml file. 
     * </p> 
     * @return the StringCollection that represent the strict features supported
     *         by this firmware. i.e., "MQ", "TAM", etc.
     */
    public StringCollection getStrictFeatures();

    /**
     * Gets the StringCollection that represents the nonstrict features supported
     * by this firmware. If a Firmware supports a nonstrict feature, it can be
     * installed on a device irrespective of whether that device has a licence
     * for that feature. 
     * <p>
     * StoredFirmware is specific to a combination of device type + model type +
     * strictfeatures + nonstrictFeatures. The nonstrictFeatures attribute is
     * immutable, so there is no <code>setNonstrictFeatures</code> method. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getNonstrictFeatures() }
     * <p>
     * Note:  The Local File System implementation persists the collection of non-strict features as an attribute on firmwares element in the
     * WAMT.repository.xml file. 
     * </p>  
     * @return the StringCollection that represent the nonstrict features
     *         supported by this firmware. i.e., 
     *         "DataGlue;JAXP-API;PKCS7-SMIME;HSM;XG4;Compact-Flash;iSCSI;RaidVolume;LocateLED;AppOpt;MQ;WebSphere-JMS;"
     */
    public StringCollection getNonstrictFeatures();

    /**
     * Returns all the FirmwareVersions contained by this StoredFirmware object. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getVersions()}
     * 
     * @return the FirmwareVersions contained by this StoredFirmware object
     */
    public StoredFirmwareVersion[] getVersions();

    /**
     * Gets the highest version number that has EVER been used for a version of
     * this object. Used to create unique identifiers for the versions of this
     * object. It is OK for this number to wrap as long as it doesn't create
     * duplicated identifiers among the current set of versions. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getHighestVersionNumber() }
     * 
     * <p>
     * Note:  The Local File System implementation persists the highest version number as an attribute on firmwares element in the
     * WAMT.repository.xml file. 
     * </p> 
     * 
     * @return The highest version number that has EVER been for a version of
     *         this object
     */
    public int getHighestVersionNumber();

    /*
     * Set the highest version number that has EVER been used for a version of
     * this object. Used to create unique identifiers for the versions of this
     * object. It is OK for this number to wrap as long as it doesn't create
     * duplicated identifiers among the current set of versions. 
     * 
     * @param newHighestVersion
     *            The highest version number that has EVER been for a version of
     *            this object
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     */
    //public void setHighestVersion(int newHighestVersion) throws DatastoreException;

    /**
     * Returns the unique identifier for this StoredFirmware.  This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#getPrimaryKey() }. 
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation. 
     * <p>
     * Note: The Local File System implementation combines the device type, model type, strict features and non-strict features  
     * as the unique identifier of this object. 
     * </p>
     * 
     * 
     * @return the unique identifier for this object
     */
    String getPrimaryKey();

    /**
     * Removes the specifed StoredFirmwareVersion from this StoredFirmware. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Firmware#remove(com.ibm.datapower.amt.clientAPI.Version) }.
     * 
     * @param firmwareVersion
     *            the StoredFirmwareVersion to remove
     * @throws NotExistInRepositoryException
     *             the symbolic name specified is not unique
     *             
     * @see Repository#createFirmwareVersion(StoredFirmware, Blob, String, Date, String, Date)
     */
    public void remove(StoredFirmwareVersion firmwareVersion)
            throws NotExistInRepositoryException;

    /**
     * Deletes the persisted instance of this StoredFirmware. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Manager#remove(com.ibm.datapower.amt.clientAPI.Firmware) } which
     * also contains the logic to removes any contained FirmwareVersions.  
     * 
     * @throws NotEmptyInRepositoryException
     *             the Firmware contains Firmware versions
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     *  
     */
    public void delete() throws NotEmptyInRepositoryException, DatastoreException;

}
