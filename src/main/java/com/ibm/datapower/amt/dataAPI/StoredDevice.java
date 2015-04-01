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

import java.net.URI;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.URLSource;

/**
 * <p>All device related information that must be  maintained and persisted for a physical 
 * DataPower device, i.e {@link com.ibm.datapower.amt.clientAPI.Device}.
 * This includes the device serialnumber, hostname, GUIPOrt, HLMPort, Hostname,  Model, 
 * Symbolic Name, and Quiesce time out.
 * </p>
 * <p>It is the container for StoredDomains persisted in the repository.
 * </p>
 */
public interface StoredDevice extends Persistable{

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /**
     * A unique identifier for this StoredDevice in the repository which can be used
     * as a primary key to retrieve and store the data.
     * 
     * 
     * @return the the unique identifier for this object
     */
    String getPrimaryKey();

    /**
     * Gets the symbolic name of this StoredDevice. This name should be
     * human-consumable and may be used in user interfaces. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Device#getSymbolicName()}to retrieve the symbolic name.
     * 
     * @return the device Name
     * @see #setSymbolicName(String)
     */
    public String getSymbolicName();

    /**
     * Sets the symbolic name of this StoredDevice. This name is designed to be
     * human-consumable. This name must be unique but it is mutable. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Device#setSymbolicName(String)}to set this symbolic name.
     * 
     * 
     * @param name
     *            the device name
     * @see #getSymbolicName()
     * @throws AlreadyExistsInRepositoryException
     *             the symbolic name specified is not unique
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     */
    public void setSymbolicName(String name) throws AlreadyExistsInRepositoryException,
            DatastoreException;

    /**
     * Gets the serial number of this device. The serial number is a unique value
     * hard coded inside the device, and is the same serial number that appears
     * in the WebGUI. The serial number is the primary key of this object in the
     * repository. The serial number is immutable, thus there is no set method.
     * 
     * 
     * This is invoked by {@link com.ibm.datapower.amt.clientAPI.Device#getSerialNumber()} 
     * to retrieve the serial number of a persisted <code>Device</code> object.
     * 
     * @return the device's embedded serial number
     */
    public String getSerialNumber();

    /**
     * Gets the hostname or IP address of this device. Users of the clientAPI can use 
     * {@link com.ibm.datapower.amt.clientAPI.Device#getHostname()} 
     * to retrieve the hostname of a persisted <code>Device</code> object.

     * 
     * @return the device's hostname or IP address
     * @see #setHostname(String)
     */
    public String getHostname();

    /**
     * Sets the hostname or IP address of this device. This value is used when
     * the repository attempts to communicate with the device.  Users of the clientAPI can use 
     * {@link com.ibm.datapower.amt.clientAPI.Device#setHostname(String)} 
     * to set the hostname a <code>StoredDevice</code> object.
     * 
     * @param hostname
     *            the new hostname or IP address for this device *
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     * @see #getHostname()
     */
    public void setHostname(String hostname) throws DatastoreException;

    /**
     * Gets the current AMP version supported by this device. 
     * The AMP version is the string representation of the numeric version.
     * The AMP version supported is determined by the firmware deployed on the device.
     *
     * Users of the clientAPI users can use 
     * {@link com.ibm.datapower.amt.clientAPI.Device#getCurrentAMPVersion()} 
     * to retrieve the supported AMP version from a <code>StoredDevice</code> object.
     * 
     * @return the device's current AMP version
     */
    public String getCurrentAMPVersion();

    /**
     * Sets the current AMP version supported by this device. 
     * The AMP version is the string representation of the numeric version.
     * The AMP version supported is determined by the firmware.
     * 
     * @param ampVersion
     *        the amp Version currently supported by the DataPower device
     * 
     */
    public void setCurrentAMPVersion(String ampVersion) throws DatastoreException;

    /**
     * Gets an array of the managed domains in this managed set. This is a convenience
     * method used internally within the clientAPI and users can invoke it by calling {@link com.ibm.datapower.amt.clientAPI.Device#getManagedDomains()}.
     * If the device is not managed or has does not have any domains then no domains will be returned. 
     * 
     * <p>Domains cannot exist outside of a containing device object. 
     * </p>
     * 
     * @return an array of StoredDomain objects
     */
    public StoredDomain[] getManagedDomains();

    /**
     * Gets the domain from the repository that has the specified name. This is a convenience
     * method used internally within the clientAPI and this information can also be accessed by 
     * invoking {@link com.ibm.datapower.amt.clientAPI.Device#getManagedDomain(String)} 
     * 
     * 
     * @param domainName
     * @return the StoredDomain object
     */
    public StoredDomain getManagedDomain(String domainName);

    /**
     * Removes the specified domain from this containing StoredDevice. This does not affect the domain on the device. 
     * This is invoked by {@link com.ibm.datapower.amt.clientAPI.Device#removeManagedDomain(String)}.  It is followed 
     * by logic to delete any child objects contained by the specified <code>StoredDomain</code> object.  It will delete
     * any <code>StoredDomainVersion</code>, <code>StoredDeploymentPolicy</code> and <code>StoredDeploymentPolicyVersion</code>
     * objects contained by the <code>StoredDomain</code> in the repository.
     *  
     * @param domain
     *            the domain to remove
     * @throws DatastoreException
     *             there was a problem updating datastore
     * @see Repository#createDomain(StoredDevice, String)
     */
    public void remove(StoredDomain domain) throws DatastoreException;

    /**
     * Gets the administrative userid persisted on this <code>StoredDevice</code>. It is
     * use internally by the manager and also exposed through {@link com.ibm.datapower.amt.clientAPI.Device#getUserId()}. 
     * 
     * @return the device's administrative userid
     * @see #setUserId(String)
     */
    public String getUserId();

    /**
     * Sets the administrative userid for this device. This value is used when
     * the repository attempts to communicate with the device. It can be invoked
     * using {@link com.ibm.datapower.amt.clientAPI.Device#getUserId()}. 
     * 
     * @param userid
     *            the new administrative userid *
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore *
     * @see #setUserId(String)
     */
    public void setUserId(String userid) throws DatastoreException;

    /**
     * Sets the administrative password for this device. This value is used when
     * the repository attempts to communicate with the device. It can be invoked
     * using {@link com.ibm.datapower.amt.clientAPI.Device#setPassword(String)}. 
     * 
     * @param password
     *            the new administrative password *
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     * @see #getPassword()
     */
    public void setPassword(String password) throws DatastoreException;

    /**
     * Gets the administrative password for this device. This value is used when
     * the repository attempts to communicate with the device. It is invoked
     * from {@link com.ibm.datapower.amt.clientAPI.Device#getDeviceContext()}. The password
     * must be set on the DeviceContext for AMP calls.
     * 
     * 
     * @return password the new administrative password
     */
    public String getPassword();

    /**
     * Gets the device's port number for HLM communication. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#getHLMPort()} to retrieve the
     * information from the repository.
     *
     * @return the device's port number for HLM communication
     * @see #setHLMPort(int)
     */
    public int getHLMPort();

    /**
     * Sets the device's port number for HLM communication. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#getHLMPort()} to set the
     * port number for HLM communication on a <code>StoredDevice</code> and persist it in the repository.
     * 
     * @param hlmPort
     *            the device's port number for HLM communication.
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     * @see #getHLMPort()
     */
    public void setHLMPort(int hlmPort) throws DatastoreException;

    /**
     * Gets the device's port for the WebGUI. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#getGUIPort()} to retrieve the
     * information from the repository.
     * 
     * @return the device's port for the WebGUI
     * @see #setGUIPort(int)
     */
    public int getGUIPort();

    /**
     * Sets the device's port for the WebGUI. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#setGUIPort(int)} to set the
     * port number for the WebGUI on a <code>StoredDevice</code> and persist it in the repository. 
     * 
     * @param guiPort
     *            the device's port for the WebGUI
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     * @see #getGUIPort()
     */
    public void setGUIPort(int guiPort) throws DatastoreException;

    /**
     * Gets the list of Strings that represent the feature licenses for this
     * device. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#getFeatureLicenses()} to retrieve the
     * information from the repository.
     * 
     * @return the list of Strings that represent the feature licenses for this
     *         device, i.e., "MQ", "TAM", etc.
     */
    public StringCollection getFeatureLicenses();

    /**
     * Sets the list of Strings that represent the feature licenses for this
     * device. This is 
     * invoked by  {@link com.ibm.datapower.amt.clientAPI.Device#createDevice(String, String, String, String, int)} to set the
     * feature entitlements for this device and persist it in the repository.
     * 
     * @param featureLicenses
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     * @see #getFeatureLicenses()
     */
    void setFeatureLicenses(StringCollection featureLicenses)
            throws DatastoreException;

    /**
     * If this is a managed device, return the StoredManagedSet that owns this device member.  This is 
     * invoked by extensively by the clientAPI and also from {@link com.ibm.datapower.amt.clientAPI.Device#getManagedSet()} to retrieve the
     * information from the repository.
     *   
     * 
     * @return the StoredManagedSet that the device is a member of. If the
     *         device is not a member of any StoredManagedSet, then this value
     *         will be null.
     * @see StoredManagedSet#add(StoredDevice)
     */
    public StoredManagedSet getManagedSet();

    /**
     * Gets the DeviceType for this device. This is 
     * invoked from {@link com.ibm.datapower.amt.clientAPI.Device#getDeviceType()} to retrieve the
     * information from the repository. The DeviceType is immutable, so there is no
     * setDeviceType()
     * 
     *  
     * @return the device's device type, i.e., "XS35", "XS40", "XI50"
     * 
     */
    public DeviceType getDeviceType();

//    /**
//     * Set the DeviceType for this device.  
//     * 
//     * @param deviceType
//     *            the DeviceType for this device
//     * @throws DatastoreException
//     *             there was a problem writing the updated value to the
//     *             datastore
//     */
//    void setDeviceType(DeviceType deviceType) throws DatastoreException;
    
    /**
     * Gets the ModelType for this device. A device ModelType is immutable, so there is no need for a 
     * setModelType().  This is in invoked from {@link com.ibm.datapower.amt.clientAPI.Device#getModelType()} to retrieve the
     * information from the repository.
     * 
     * @return the ModelType for this device
     */
    public ModelType getModelType();

//    /**
//     * Set the ModelType for this device
//     * 
//     * @param modelType
//     *            the ModelType for this device
//     * @throws DatastoreException
//     *             there was a problem writing the updated value to the
//     *             datastore
//     */
//    void setModelType(ModelType modelType) throws DatastoreException;

    /**
     * Deletes the persisted instance of this StoredDevice. This is in invoked from 
     * {@link com.ibm.datapower.amt.clientAPI.Manager#remove(com.ibm.datapower.amt.clientAPI.Device)} 
     * If this StoredDevice
     * is managed by a <code>StoredManagedSet</code>,i.e it is a device member of a <code>StoredManagedSet</code>,
     * it will be removed from that StoredManagedSet before it is deleted.  Also all stored objects contained within
     * the <code>StoredManagedSet</code> such as <code>StoredDomain</code>, <code>DomainVersion</code>, 
     * <code>StoredDeploymentPolicy</code>, <code>StoredDeploymentPolicyVersion</code>, and <code>StoredTag<code> 
     * are also deleted because they do not exist without the containing <code>StoredDevice</code> object.
     *  
     * @throws DatastoreException
     *             there was a problem updating datastore
     */
    public void delete() throws DatastoreException;

    /**
     * Stores the location for the backup file from a Device.  This information is stored when  
     * {@link com.ibm.datapower.amt.clientAPI.Device#backup(String, URLSource, URI, boolean, boolean)}
     *  is invoked to backup up a managed or unmanaged device.  
     * 
     * @param fileLocation indicates location of back up files for a secure backup operation
     */    
	public void setBackupFileLocation(URI fileLocation);
	
    /**
     * Gets the location where the backup files are to be saved for a secure backup operation.
     *  
     * 
     * @return location which holds location of backed up files that can be later used
     * during the restore operation on the device
     */  	
	public URI getBackupFileLocation();	
	
    /**
     * Stores the location of the certificate file to be used in the secure backup operation. This information is 
     * stored when  
     * {@link com.ibm.datapower.amt.clientAPI.Device#backup(String, URLSource, URI, boolean, boolean)}
     *  is invoked to backup up a managed or unmanaged device.  
     * 
     * @param certificatesLocation indicates location of back up certificates that can be later used
     * during the restore operation on the device
     */  
	public void setBackupCertificateLocation(URLSource certificatesLocation);
	
    /**
     * Gets the location of the certificate file to be used in a secure backup operation on a device
     * 
     * @return the location which holds the certificate file that can be later used
     * during the restore operation on the device
     */ 	
	public URLSource getBackupCertificateLocation();
	
    /**
     * Sets the timeout value for quiescing a Device before performing a firmware update on the device. This is invoked 
     * by {@link com.ibm.datapower.amt.clientAPI.Device#setQuiesceTimeout(int)}
     * 
     * @param timeout value in sec
     */    
    public void setQuiesceTimeout(int timeout);
    
    /**
     * Gets the persisted timeout value for quiescing the device. This is invoked 
     * by {@link com.ibm.datapower.amt.clientAPI.Device#getQuiesceTimeout()}  
     * @return timeout value in sec
     */    
    public int getQuiesceTimeout();

    /**
     * Add a tag to the device. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Device#addTag(String, String)} to set the
     * tag on a <code>StoredTag</code> and persist it in the repository.
     *    
     * @param tag StoreTag
     */
    public void add(StoredTag tag);
    
    /**
     * Gets the tags for this device. This is invoked from 
     * {@link com.ibm.datapower.amt.clientAPI.Device#getTagNames()},
     * {@link com.ibm.datapower.amt.clientAPI.Device#getTagValues(String)},
     * {@link com.ibm.datapower.amt.clientAPI.Device#removeTag(String)} and 
     * {@link com.ibm.datapower.amt.clientAPI.Device#removeTag(String, String)},
     *  to retrieve the information from the repository.
     *  
     * @return a array of StoredTags
     */
    public StoredTag[] getTags();
    
    /**
     * Remove the tags for this device. This is invoked from 
     *  {@link com.ibm.datapower.amt.clientAPI.Device#removeTag(String, String)} and 
     *  {@link com.ibm.datapower.amt.clientAPI.Device#removeTag(String)}
     *  
     * @param tag the StoredTag
     * @throws DatastoreException
     */
    public void remove(StoredTag tag) throws DatastoreException;
    
    /**
     * Remove all tags for this device. This is invoked from 
     * {@link com.ibm.datapower.amt.clientAPI.Device#removeTags()}
     */
    public void removeTags();
}
