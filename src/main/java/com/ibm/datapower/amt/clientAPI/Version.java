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

import java.util.Date;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.dataAPI.DatastoreException;


/**
 * A version of an object that is {@link Versionable}. A Versionable object can
 * have a collection of Versions. For example, a Domain object can have a
 * collection of DomainVersions. The data associated with a particular version
 * is stored in the Version object instead of the Versionable object.
 * <p>
 * A Version should have several attributes.
 * <ul>
 * <li>the data associated with that version. This may also be known as the
 * snapshot. In the case of the manager, that data will likely be in the form of a
 * {@link Blob}.
 * <li>a version number. The version number is a monotomically increasing
 * integer that starts from "1". The version number should be treated as an
 * opaque identifier and you should not derive any meaning from it, although it
 * is likely that the version numbers will be sequential based on the order in
 * which they were created.</li>
 * <li>a timestamp of when the Version was created. This is when the Version
 * was created in the repository, not when the data associated with that Version was
 * created on the device.</li>
 * <li>a user comment. This is an annotation which a user can attach to a
 * Version that can contain any description they want. The user comment may have
 * a default value, and the user can modify the comment later at their
 * convenience.</li>
 * </ul>
 * Please see the list of known implementing classes.
 * <p>
 * During debug and troubleshooting, it may be helpful to obtain a copy of the
 * {@link Blob} as returned from {@link #getBlob()} if there is a question if
 * the Blob attached to this version is different from what is persisted on the
 * device and how it interacts in
 * {@link com.ibm.datapower.amt.amp.Commands#isDomainDifferent(String, byte[], byte[], com.ibm.datapower.amt.amp.DeviceContext)}
 * Beware that the Blob file may be Base64 encoded in the repository, depending
 * on the implementation of {@link com.ibm.datapower.amt.amp.Commands} and
 * {@link com.ibm.datapower.amt.dataAPI.Repository} that is being used, the
 * default implementation does store the Blob as Base64 encoded, so you will
 * need to Base64 decode the file before using it in the WebGUI. For a Blob in:
 * <ul>
 * <li>{@link DomainVersion} other than the domain named "default": that Blob
 * is the same as if you had created a "Backup Zip Bundle" from the WebGUI. So
 * you can use the WebGUI "Compare Configuration" operation on that Blob file.
 * First do a Base64 decode and then give the filename a ".zip" extension and
 * attach it on one side on the Comparison. On the other side of the Comparison
 * select "Persisted Configuration".
 * <li><code>DomainVersion</code> in the default domain: since the Settings
 * were stripped out of the export of this domain compared to what you would get
 * from the WebGUI's export, you will need to unzip this file and pull out the
 * inner file named "export.xml". Then in the WebGUI "Compare Configuration"
 * select "XML Configuration" and attach the "export.xml" file on one side, and
 * on the other side of the Comparison select "Persisted Configuration".
 * <li>{@link FirmwareVersion}: there is no direct comparison for firmware
 * files, simply use the firmware level String from the running device via
 * {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower.amt.amp.DeviceContext)}
 * or from the firmware file via {@link FirmwareVersion#getLevel()}.
 * </ul>
 * <p>
 * 
 * @see Versionable
 * @version SCM ID: $Id: Version.java,v 1.3 2010/08/23 21:20:27 burket Exp $
 */
public interface Version {
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /**
     * Get the version number that this Version object represents. Version
     * numbers are immutable in the Version object, and are incremented
     * automatically by the Manager when a new Version object is created, so
     * there is no <code>setVersionNumber(int)</code> method. The combination
     * of version number and Object reference forms the primary key for a
     * Version object in the Manager.
     * 
     * @return the version number that this Version object represents.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public int getVersionNumber() throws DeletedException;
    
    /**
     * Get a reference to the object that this Version is a version of. For
     * example, calling <code>DomainVersion.getVersionedObject()</code> should
     * return a <code>Domain</code>, which is the parent object. This should
     * be a reminder for the subclass to implement the method getDomain() 
     * or getFirmware() that would return a typed object instead
     * of a generic one. It isn't expected that a caller would actually use this
     * method.
     * 
     * @return a reference to either a Firmware, Domain, or DeploymentPolicy object.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Versionable getVersionedObject() throws DeletedException;
    
    /**
     * Get the timestamp that signifies when the Version was created in the
     * Manager. This value is set automatically by the Manager, so there
     * is no <code>setTimestamp(Date)</code> method.
     * 
     * @return the timestamp of the Version creation
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Date getTimestamp() throws DeletedException;
    
    /**
     * Get the user comment that corresponds to this Version. This is where the
     * user can attach a String to the Version that helps describe the version
     * in their terms.
     * 
     * @return the user comment associated with this version
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setUserComment(String)
     */
    public String getUserComment() throws DeletedException;
    
    /**
     * This is the method to set the user comment that can be later retrieved
     * via {@link #getUserComment()}. Calling this method will cause the
     * comment to be written to the persisted datastore.
     * 
     * @param userComment the user-defined comment to attach to this Version. It may
     *        have reference to a new function, bug fix, change management or
     *        problem management ticket, etc. This is a freeform field of
     *        unlimited length.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getUserComment()
     */
    public void setUserComment(String userComment) throws DatastoreException, 
    LockBusyException, DeletedException;

    /**
     * Get the Blob that is attached to this Version. The Blob could be very
     * large, FirmwareVersion blobs can be 20MB. The blob should have lazy
     * instantiation, meaning that it isn't loaded from the datastore until this
     * method is invoked.
     * 
     * @return the blob for this version
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Blob getBlob() throws DeletedException;
    
//    /**
//     * Checks to see if this Version is in use, meaning that it is the desired
//     * Version of the parent Versionable object. This Version may not be removed
//     * from the parent Versionable object while this Version is in use,
//     * irregardless of the value of {@link Manager#getMaxVersionsToStore()}.
//     * 
//     * @return true if this version is in use, false otherwise.
//     * @throws DeletedException this object has been deleted from the persisted
//     *         repository. The referenced object is no longer valid. You should
//     *         not be using a reference to this object.
//     */
//    public boolean isInUse() throws DeletedException;
    
}
