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
import com.ibm.datapower.amt.clientAPI.Blob;

/**
 * <p>Information for a version of a Firmware, Domain or Deployment Policy that must be  maintained and persisted. 
 * This information can be used to deploy firmware or domain configuration to a DataPower device. The user 
 * of the dataAPI is responsible for pruning excess or older versions that are no longer useful.
 * </p>The following interfaces extend <code>StoredVersion</code>:
 * <ul>
 * <li><code>StoredFirmWareVersion</code></li>
 * <li><code>StoredDomainVersion</code></li>
 * <li><code>StoredDeploymentPolicyVersion</code></li>
 * </ul>
 * <p>
 * </p>
 * 
 */
public interface StoredVersion extends Persistable {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /**
     * Gets the version number that this StoredVersion object represents.
     * StoredVersion numbers are immutable in the StoredVersion object, and are
     * incremented automatically by the repository when a new StoredVersion
     * object is created, so there is no <code>setVersionNumber(int)</code>
     * method. The combination of version number and Object reference forms the
     * primary key for a StoredVersion object in the repository.
     * 
     * @return the version number that this StoredVersion object represents.
     */
    public int getVersionNumber();

    /**
     * Gets a reference to the object that this StoredVersion describes.
     * 
     * @return a reference to either a StoredFirmware, StoredDomain, or
     *         StoredSettings object.
     */
    public Object getVersionedObject();

    /**
     * Gets the timestamp that signifies when the StoredVersion was created in
     * the repository. This value is set automatically by the repository, so
     * there is no <code>setTimestamp(Date)</code> method. The timestamp is accessed from
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#getTimestamp()}, 
     *  {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getTimestamp()}, and
     *   {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getTimestamp()} 
     * 
     * @return the timestamp of the StoredVersion creation
     */
    public Date getTimestamp();

    /**
     * Gets the user comment that corresponds to this version. This is where the
     * user can attach a String to the StoredVersion that helps describe the
     * version in their terms. The comment is can be obtained by calling 
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#getUserComment()}, 
     *  {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getUserComment()},
     *   {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getUserComment()} 
     * 
     * @return the user comment associated with this version
     * @see #setUserComment(String)
     */
    public String getUserComment();

    /**
     * This method is used to set the user comment that can be later retrieved
     * via {@link #getUserComment()}. Calling this method will cause the
     * comment to be written to the repository. The comment is set by invoking
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#setUserComment(String)}, 
     *  {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#setUserComment(String)},
     *   {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#setUserComment(String)} 
     * 
     * @param comment
     *            the user-defined comment to attach to this StoredVersion. It
     *            may have reference to a new function, bug fix, change
     *            management or problem management ticket, etc. This is a
     *            freeform field of unlimited length.
     * @see #getUserComment()
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     */
    public void setUserComment(String comment) throws DatastoreException;

    /**
     * Gets the binary image for this version.  This is used internally by the clientAPI when
     * deploying domain source configuration or firmware to a DataPower device.  The blob can 
     * also be accessed by invoking
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#getBlob()}, 
     * {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getBlob()},
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getBlob()} 
     * The image is set by the clientAPI and store by the repository, so
     * there is no <code>setBlob</code> method.
     * 
     * @return the blob
     */
    public Blob getBlob();

    /**
     * Deletes the version from the repository. Will remove the version from it's
     * container and delete the Blob for this StoredVersion.  This is invoked internally by the manager
     * to remove a version. 
     *  
     * 
     * @throws DatastoreException
     *             there was a problem deleting the instance in the
     *             datastore
     *  
     */
    public void delete() throws DatastoreException;

}
