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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicyType;
import com.ibm.datapower.amt.clientAPI.Domain;
import com.ibm.datapower.amt.clientAPI.URLSource;
import com.ibm.datapower.amt.dataAPI.local.filesystem.StoredTagImpl;

/**
 * The object used for executing persistence operations such as retrieving and 
 * saving Stored objects available with the dataAPI
 * <p>
 * The implementor of this interface must provide a public static Repository
 * getInstance() method to return the singleton instance of Repository.  This is
 * invoked when the Manager is initialized. 
 * <p>
 * For the methods that return multiple objects in the form of arrays, for
 * example {@link #getFirmwares()}, if there are no items in the collection
 * then the method should return an array of zero elements instead of a null
 * array reference. By doing this the caller can easily use a <code>for</code>
 * loop to iterate the array without worrying about a NullPointerException. This
 * applies to all the classes in this package, not just this class.
 * 
 * @see com.ibm.datapower.amt.clientAPI.Manager
 * @see StoredManagedSet
 * @see StoredDevice
 * @see StoredDomain
 * @see StoredFirmware
 */
public interface Repository {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /////////////////////////////////////////////////////////////////////////////
    // for repository
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Export everything in the repository to the specified
     * OutputStream. This method can be used to backup the repository. This
     * method will not close the OutputStream. The format of this data is in a
     * known interchange format so the repository data can be migrated to
     * another management system. The clientAPI method {@link com.ibm.datapower.amt.clientAPI.Manager#exportAll(OutputStream)}
     * calls an implementation of this interface to perform the export of the data in the repository.
     * @param outputStream
     *            where the exported data will be written
     * @see #importAll(InputStream)
     *  @throws DatastoreException
     *             there was a problem exporting the repository
     */
    public void exportAll(OutputStream outputStream) throws DatastoreException;

    /**
     * Import to the repository from the specified InputStream. This
     * InputStream should be connected to data that was previously exported.
     * This method will not close the InputStream. The format of this data is in
     * a known interchange format so the repository data can be migrated from
     * another management system.The clientAPI method {@link com.ibm.datapower.amt.clientAPI.Manager#importAll(InputStream)}
     * calls an implementation of this interface to perform the export of the data in the repository.
     * 
     * @param inputStream
     *            where to read the previously exported data from
     * @see #exportAll(OutputStream)
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */
    public void importAll(InputStream inputStream) throws DatastoreException;

    /**
     * Save the changes that have been made to Stored objects to the repository
     * The clientAPI method {@link com.ibm.datapower.amt.clientAPI.Manager#save(boolean)}
     * calls an implementation of this interface to save the data in the repository.
     *   
     * @param forceSave
     *            If true, do not throw DirtySaveException, even if other changes have
     *            been saved to the repository while the unsaved changes were
     *            taking place.
     * @throws DirtySaveException
     *             Other changes have been saved to the repository while changes
     *             were taking place in our copy. 
     * @throws DatastoreException
     *             there was a problem updating the Data store.
     */
    public void save(boolean forceSave) throws DirtySaveException,
            DatastoreException;

    /**
     * Create a new device object in the repository with these properties. The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Device} calls an implementation of this
     * interface to save the data in the repository.
     * 
     * 
     * @param serialNumber
     *            the serialNumber for this device
     * @param name
     *            the human-consumable symbolic name for this object
     * @param deviceType
     *            the device type of the device
     * @param modelType
     *            the model type of the device
     * @param hostname
     *            the host name or IP address of the device
     * @param userid
     *            the administrative user id of the device
     * @param password
     *            the administrative password of the device
     * @param HLMport
     *            the device port for AMP communications
     * @param guiPort
     *            the device port for the WebGUI
     * @param ampVersion
     *            the device's current AMP version
     * @return a new StoredDevice object
     * 
     * @throws AlreadyExistsInRepositoryException
     *             The device already exists
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     * 
     * @see StoredManagedSet#add(StoredDevice)
     * @see Repository#getDevice(String)
     * @see StoredDevice#delete()
     */
    public StoredDevice createDevice(String deviceID, String serialNumber, String name,
            DeviceType deviceType, ModelType modelType, String hostname,
            String userid, String password, int HLMport, int guiPort, String ampVersion)
            throws AlreadyExistsInRepositoryException, DatastoreException;

    /**
     * Create a new domain object in the repository. The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Device#createManagedDomain(String)} 
     * calls an implementation of this interface to save the newly created 
     * domain data in the repository.
     * 
     * 
     * @param device
     *            The StoredDevice that contains this StoredDomain
     * @param domainName
     *            The name of this StoredDomain
     * @return a new StoredDomain object
     * @see StoredDomain#delete()
     * @throws AlreadyExistsInRepositoryException
     *             a StoredDomain of domainName already exists in managedSet
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */
    public StoredDomain createDomain(StoredDevice device,
            String domainName) throws AlreadyExistsInRepositoryException,
            DatastoreException;
    

    /**
     * Creates a new policy version object in the repository. The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#deployConfiguration()} 
     * will create a new domain version and a new policy version.  It will invoke 
     * an implementation of this interface to save the newly created 
     * policy version data in the repository.
     * 
     * 
     * @param policy
     *            The StoredDeploymentPolicyVersion object
     * @param blob
     *            The policy blob that will be deployed
     * @param comment
     *            The user provided comment for the deployment policy object 
     * @param timestamp
     *            creation timestamp                      
     * @return a new StoredDeploymentPolicyVersion object
     * @see StoredDeploymentPolicyVersion#delete()
     * @throws AlreadyExistsInRepositoryException
     *             a StoredDomain of domainName already exists in managedSet
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */
    public StoredDeploymentPolicyVersion createDeploymentPolicyVersion(StoredDeploymentPolicy policy,
            Blob blob, String comment, Date timestamp) throws AlreadyExistsInRepositoryException, DatastoreException;
  
    
    /**
     * Creates a new policy object in the repository. The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#deployConfiguration()} 
     * will create a new domain version and a new policy version.  It will invoke 
     * an implementation of this interface to save the newly created 
     * policy version data in the repository.
     *
     * @param domain 
     *            The stored Domain object
     * @param policyObjectName - this is the first piece of information used to find the deployment policy within
     *        the URLSource specified              
     * @param url - is a URLSource that points to a configuration containing the policy to be used 
     *        during deployment. 
     * @param policyDomainName - this is the second piece of information used to find the deployment domain within
     *        the URLSource specified
     * @param type - the policy type used during configuration deployment                      
     *                                           
     * @return a new StoredDeploymentPolicy object
     * 
     * @throws AlreadyExistsInRepositoryException
     *             a StoredDomain of domainName already exists in managedSet
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     *             
     * @see StoredDeploymentPolicy#delete() 
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#EXPORT
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#XML   
     */    
    public StoredDeploymentPolicy createDeploymentPolicy(StoredDomain domain, String policyObjectName,
    		URLSource url, String policyDomainName, DeploymentPolicyType type) throws AlreadyExistsInRepositoryException, DatastoreException;

    public StoredTagImpl createTag(String name, String value)
    	throws AlreadyExistsInRepositoryException, DatastoreException;
    
    /**
     * Retrieves a device from the repository that has the specified serialNumber. This is
     * a convenience method that is invoked from the The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#getDeviceBySerialNumber(String)} 
     *
     * @param serialNumber
     *            serialNumber of the device to retrieve from the repository
     * @return the StoredDevice object
     */
    public StoredDevice getDevice(String serialNumber);
    
    /**
     * Retrieves all devices from the repository that has the specified serialNumber. 
     *
     * @param serialNumber
     *            serialNumber of the device to retrieve from the repository
     * @return the array of StoredDevice object
     */
    public StoredDevice[] getDeviceBySerialNumber(String serialNumber);

    /**
     * Retrieves all known devices in the repository. The returned Devices may be managed or unmanaged, i.e 
     * they may or may not be members of a ManagedSet. This is
     * a convenience method that is invoked from the The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#getAllDevices()} 
     * 
     * @return an array of Devices
     */
    public StoredDevice[] getDevices();

//    /**
//     * Gets the list of known devices in the Repository that are not managed i.e. not assigned to
//     * a StoredManagedSet. Additionally, it may filter the list to include only
//     * devices of the desired deviceType.
//     * 
//     * @param desiredDeviceType
//     *            if set to null, this method will return all unmanaged devices
//     *            of any deviceType. If set to a non-null value, this method
//     *            will return all unmanaged devices that match only this desired
//     *            deviceType.
//     * @return a list of unmanaged Devices, meaning devices that are not members
//     *         of any managedSet.
//     */
//    public StoredDevice[] getUnmanagedDevices(DeviceType desiredDeviceType);

    /////////////////////////////////////////////////////////////////////////////
    // for managed sets
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the existing StoredManagedSet that has the specified name from the
     * repository. This is
     * is invoked from the The clientAPI 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#getManagedSet(String)}   
     * 
     * 
     * @param name
     *            name of the StoredManagedSet as stored in the repository
     * @return a reference to the StoredManagedSet object that is populated from
     *         the repository
     */

    public StoredManagedSet getManagedSet(String name);

    /**
     * Gets all the ManagedSets in the repository. This is
     * a convenience method that is invoked internally in many places from the The clientAPI 
     * and is also call from {@link com.ibm.datapower.amt.clientAPI.Manager#getManagedSets()} 
     * 
     * @return a array of ManagedSets
     */
    public StoredManagedSet[] getManagedSets();
    
    
    public StoredTag getTag(String name);
    
    
    public StoredTag[] getTags();

    /////////////////////////////////////////////////////////////////////////////
    // for firmware
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Gets all the firmwares that are in the repository. This is
     * a convenience method that is invoked internally in many places within the
     * clientAPI and also from public API  
     * {@link com.ibm.datapower.amt.clientAPI.Manager#getFirmwares()} 
     * 
     * @return a array of StoredFirmware object
     * @see "useCases section 4.3, 4.4"
     */
    public StoredFirmware[] getFirmwares();

    /**
     * Gets an existing firmware with the specified attributes from the repository. This is
     * a convenience method that is invoked internally in many places within the
     * clientAPI.  
     * 
     * @param deviceType
     *            the deviceType the firmware supports
     * @param modelType
     *            the modelType the firmware supports
     * @param strictFeatures
     *            The strict features supported by this firmware.
     * @param nonstrictFeatures
     *            The nonstrict features supported by this firmware.
     * @return a firmware object populated by the repository
     */
    public StoredFirmware getFirmware(DeviceType deviceType,
            ModelType modelType, StringCollection strictFeatures,
            StringCollection nonstrictFeatures);

    /**
     * Creates a new StoredManagedSet object in the repository. This is
     * invoked by   
     * {@link com.ibm.datapower.amt.clientAPI.ManagedSet#ManagedSet(String)} to
     * create and persist a new managed set in the repository.
     * 
     * @param name
     *            a human-consumable symbolic name for this StoredManagedSet
     * @return a new StoredManagedSet object.
     * @throws AlreadyExistsInRepositoryException
     *             a StoredManagedSet of name already exists
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */
    public StoredManagedSet createManagedSet(String name)
            throws AlreadyExistsInRepositoryException, DatastoreException;

    /**
     * Creates a new StoredFirmware object in the repository unless a matching Firmware 
     * already exists in the manager. 
     * Each <code>Firmware</code> has attributes that uniquely identify it. These attributes include
     * DeviceType, ModelType, Strict feature list, and Non-strict features 
     * See {@link com.ibm.datapower.amt.clientAPI.Firmware}. 
     * It also creates a new StoredFirmwareVersion object whose attributes are determined
     * from the firmware image passed in on the 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#addFirmware(Blob, String)} call. 
     * 
     * @param deviceType
     *            The type of device this firmware must run on.
     * @param modelType
     *            The model type this firmware must run on.
     * @param strictFeatures
     *            The strict features supported by this firmware.
     * @param nonstrictFeatures
     *            The nonstrict features supported by this firmware.
     * 
     * @return a new firmware object. *
     * @throws AlreadyExistsInRepositoryException
     *             a StoredFirmware of name already exists
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */
    public StoredFirmware createFirmware(DeviceType deviceType,
            ModelType modelType, StringCollection strictFeatures,
            StringCollection nonstrictFeatures) throws AlreadyExistsInRepositoryException,
            DatastoreException;

    /**
     * Creates a new StoredFirmwareVersion object in the repository. It stores the Firmware
     * level (i.e. "3.6.0.1"), a reference to the firmware image file created from the 
     * binary image for this firmware added to the manager.
     * See {@link com.ibm.datapower.amt.clientAPI.Firmware} and {@link com.ibm.datapower.amt.clientAPI.Version}. 
     * The StoredFirmwareVersion attributes (level) are determined
     * from the firmware image passed in on the 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#addFirmware(Blob, String)} call.  
     * <p>
     * When creating a StoredFirmwareVersion that has a Blob, please read the
     * {@link Blob Blob class javadoc} about verifying the contents are not in a
     * byte array via {@link Blob#hasBytes()}.
     * @param versionedObject
     *            a reference to the StoredFirmware object that this
     *            StoredVersion object is a version of.
     * @param blob
     *            a reference to the Blob that contains the binary image for
     *            this firmware image as down loaded from the manufacturer's web
     *            site
     * @param level
     *            The level of the firmware extracted from the blob
     * @param manufactureDate
     *            The date the firmware was created. Extracted from the blob.
     * @param userComment
     *            The user's comment for the action that is creating the
     *            StoredFirmware version
     * @param timeStamp
     *            The time to record for the creation of the
     *            StoredFirmwareVersion
     * @return a StoredFirmwareVersion object populated by the repository
     * @throws AlreadyExistsInRepositoryException
     *             A firmware StoredVersion already exists for this image.
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             
     */

    public StoredFirmwareVersion createFirmwareVersion(
            StoredFirmware versionedObject, Blob blob, String level,
            Date manufactureDate, String userComment, Date timeStamp)
            throws AlreadyExistsInRepositoryException, DatastoreException;

    /**
     * Creates a new StoredDomainVersion object in the repository when the clientAPI
     * deploys a new domain source configuration from {@link com.ibm.datapower.amt.clientAPI.Domain#deployConfiguration()}.
     * The binary image for the domain configuration is also persisted. 
     * 
     * <p>
     * When creating a StoredDomainVersion that has a Blob, please read the
     * {@link Blob Blob class javadoc} about verifying the contents are not in a
     * byte array via {@link Blob#hasBytes()}.
     * 
     * 
     * @param versionedObject
     *            a reference to the StoredDomain object that this StoredVersion
     *            object is a version of.
     * @param blob
     *            a reference to the Blob that contains the binary image for
     *            this StoredDomainVersion
     * @param userComment
     *            The user's comment for the action that is creating the
     *            StoredDomainVersion
     * @param timeStamp
     *            The time to record for the creation of the StoredDomainVersion
     * @return a StoredDomainVersion object populated by the repository
     * @throws AlreadyExistsInRepositoryException
     *             A StoredDomain StoredVersion already exists for this image.
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     */

    public StoredDomainVersion createDomainVersion(
            StoredDomain versionedObject, Blob blob, String userComment,
            Date timeStamp) throws AlreadyExistsInRepositoryException, DatastoreException;

    /**
     * Gets the maximum number of versions of any one object that the repository
     * will try to keep. The {@link com.ibm.datapower.amt.clientAPI.Manager#getMaxVersionsToStore()}
     * uses this interface to query the maximum versions that can currently be persisted.
     * This is the value set in {@link #setMaxVersionsToStore(int)}.  This allows the version
     * data for objects to be trimmed by users when they want to purge their older data.
     *
     * 
     * @return the maximum number of versions of any one object to keep in the
     *         repository. This will likely apply to StoredFirmwareVersions,
     *         StoredSettingsVersions, and StoredDomainVersions. If this value
     *         is not initialized (previously set to a specific value via
     *         {@link #setMaxVersionsToStore(int)}) then it should return 0 so
     *         that the Manager can set it to a usable default.
     * @see #setMaxVersionsToStore(int)
     */
    public int getMaxVersionsToStore();

    /**
     * Sets the maximum number of versions of any one object that should be kept
     * in the repository. This is used to trim the repository to a reasonable
     * size. If any object has more than maxVersions in the repository, then the
     * repository will automatically delete the oldest versions until this
     * threshold is no longer exceeded. The repository will not delete any
     * versions that are currently desired versions, even if that results in
     * storing more than maxVersions.
     * 
     * @param maxVersions
     *            the maximum number of versions of any one object to keep in
     *            the repository. For example, is set to 4 then the repository
     *            will limit itself to keeping 4 versions of firmware, 4
     *            settings of each managedSet, and 4 versions of each domain in
     *            each managedSet. This applies to all versioned objects:
     *            firmware, domains, and settings. A value that is less than 1
     *            (0 or negative) means that there is no limit and the
     *            repository will never delete versions.
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             repository
     * @see #getMaxVersionsToStore()
     */
    public void setMaxVersionsToStore(int maxVersions)
            throws DatastoreException;
    
//    /**
//     * Gets the directory where the versions will be stored.
//     * 
//     * @return the directory where the versions will be stored.
//     * @see #setVersionsDirectory(java.io.File)
//     */
//    public File getVersionsDirectory();
//    
//    /**
//     * Sets the directory where the versions created by the manager will be
//     * stored. It is up the repository implementation to decide if it needs to move
//     * the current versions from the current directory to the new directory. 
//     * 
//     * @param newDirectory
//     *            the new directory where the versions will be stored.
//     * @throws DatastoreException
//     * @see #getMaxVersionsToStore()
//     * 
//     */
//
//    public void setVersionsDirectory(File newDirectory)
//            throws DatastoreException;

    /**
     * Starts up the Repository. This instance of Repository is available for use
     * after startup is invoked. 
     * This invoked by {@link com.ibm.datapower.amt.clientAPI.Manager#getInstance(java.util.Map)}
     * 
     * @throws DatastoreException
     *             there was a problem working with the repository
     * @see #shutdown()
     */
    public void startup() throws DatastoreException;

    /**
     * Shuts down the Repository. This instance of Repository should no longer be
     * used after shutdown is invoked.  
     * This invoked by {@link com.ibm.datapower.amt.clientAPI.Manager#shutdown()}
     * 
     * @throws DatastoreException
     *             there was a problem working with the repository
     * @see #startup()
     */
    public void shutdown() throws DatastoreException;
    
    /**
     * Updates an existing domain object. Domain object attributes values can be modified by
     * calling the following methods: 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)},
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setQuiesceTimeout(int)}, and
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setSynchronizationMode(com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode)}.
     * These changes have to be persisted in the repository.
     * 
     * 
     * @throws AlreadyExistsInRepositoryException
     *             A StoredDomain StoredVersion already exists for this image.
     * @throws DatastoreException
     *             there was a problem working with the repository
     *             
     * @see Domain#setDeploymentPolicy(URLSource, String, String)
     * @see Domain#setQuiesceTimeout(int)
     * @see Domain#setSynchronizationMode(com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode)
     * 
     */    
    public StoredDomain updateDomain(StoredDomain domain
    ) throws AlreadyExistsInRepositoryException, DatastoreException;

}
