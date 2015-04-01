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

import com.ibm.datapower.amt.Constants;

/**
 * The information that must be maintained and persisted for a
 * {@link com.ibm.datapower.amt.clientAPI.ManagedSet} 
 * including its managed device members.
 * A <code>StoredManagedSet</code> tracks every <code>StoredDevice</code> that is managed by it.  
 * 
 */
public interface StoredManagedSet  extends Persistable{

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; 
    
    /**
     * Gets the name of this StoredManagedSet. This name should be
     * human-consumable. The name is immutable, so there is no
     * <code>setName(String)</code> method.
     * 
     * @return name of this StoredManagedSet.
     */
    public String getName();

    /**
     * Adds the specified device to the specified managed set. A device cannot be
     * a member of more than one managed set at a time. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.ManagedSet#addDevice(com.ibm.datapower.amt.clientAPI.Device)}
     * 
     * @param device
     *            the device to add
     * @throws DatastoreException
     *             there was a problem updating datastore
     * @see #remove(StoredDevice)
     */
    public void add(StoredDevice device) throws DatastoreException;

    /**
     * Gets the array of devices which are members of this managed set.
     * 
     * @return an array of devices which are members of this managed set.
     * @see #add(StoredDevice) This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.ManagedSet#getDeviceMembers()}
     * 
     * @see #remove(StoredDevice)
     */
    public StoredDevice[] getDeviceMembers();

    /**
     * Removes the specified device from this managed set. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.ManagedSet#removeDevice(com.ibm.datapower.amt.clientAPI.Device)}
     * The removed device is still persisted in the repository though it is no longer managed by this
     * managed set.  You can reverse this by adding the device back to the managed set. 
     * 
     * @param device
     *            the device to remove
     * @throws DatastoreException
     *             there was a problem updating datastore
     * @see #add(StoredDevice)
     * @see #getDeviceMembers()
     */
    public void remove(StoredDevice device) throws DatastoreException;

//    /**
//     * Get the firmware version desired to be deployed to this managed set.
//     * 
//     * @return the version of firmware that is desired to be deployed to the
//     *         managed set
//     * @see Repository#createFirmwareVersion(StoredFirmware, Blob, String, Date, String, Date)
//     */
//    public StoredFirmwareVersion getDesiredFirmware();

//    /**
//     * Set the firmware image that is desired to be deployed to all the devices
//     * in the managed set. The deployment will be done in a rolling fashion,
//     * updating one device at a time before moving on to the next device.
//     * 
//     * @param version
//     *            the version of firmware to desired to be deployed to the
//     *            managed set
//     * @throws DatastoreException
//     *             there was a problem writing the updated value to the
//     *             datastore
//     * @see Repository#createFirmwareVersion(StoredFirmware, Blob, String, Date, String, Date)
//     * @see #getDesiredFirmware()
//     */
//    public void setDesiredFirmware(StoredFirmwareVersion version)
//            throws DatastoreException;

    /**
     * Deletes this StoredManagedSet. This operation will successfully delete the Managed
     * Set but will not delete any of the devices or domain that were managed by this
     * ManagedSet.  They are now considered unmanaged devices and domains.
     *
     * @throws NotEmptyInRepositoryException
     *             the StoredManagedSet contains (Domains) or (Policies
     *             with versions)
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     *  
     */
    public void delete() throws DatastoreException, NotEmptyInRepositoryException;

    /**
     * Internal Use Only<p/>
     * Returns the unique identifier for this StoredManagedSet. 
     * It is used internally  by  {@link com.ibm.datapower.amt.clientAPI.Manager#getAllDevices()}
     * 
     * 
     * @return the unique identifier for this object
     * @see #getName()
     * 
     */
    String getPrimaryKey();

}
