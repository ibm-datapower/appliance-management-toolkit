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

import com.ibm.datapower.amt.Constants;

/**
 * An object which can have multiple {@link Version}s. Please see the list of
 * known implementing classes.
 * <p>
 * In general, the data associated with a version instance will be attached to
 * the classes that implements the Version interface, not this interface.
 * Classes that implement this interface should have a collection of references
 * to the versions, and a way to navigate those versions.
 * <p>
 * 
 * @see Version
 * @version SCM ID: $Id: Versionable.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 * <p>
 */
//* Created on Aug 26, 2006
public interface Versionable {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$

    /**
     * Get all the versions that exist of this Versionable object.
     * <p>
     * This object should implement the <code>Versionable</code> interface,
     * and versions of this object should implement the <code>Version</code>
     * interface.
     * 
     * @return an array of objects which describe all the versions of this
     *         object.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getVersion(int)
     */
    public Version[] getVersions() throws DeletedException;

    /**
     * Get the specified Version of this Versionable object. Versions are
     * specified by a version number, which is a monotomically increasing
     * integer that should be unique across this Versionable object.
     * 
     * @param versionNumber the specified version to get of this object.
     * @return the specified version of this object. May return null if no
     *         version of the specified number exists.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getVersions()
     */
    public Version getVersion(int versionNumber) throws DeletedException;

    /**
     * Remove the specified version of this object from the collection. The
     * specified version should be considered no longer usable and eligible for
     * garbage collection at any time, so there should not be any references
     * left to it. Only versions which are not currently in use may be removed.
     * 
     * @param version the object to remove from the collection
     * @throws InUseException the specified version is currently in use and
     *         cannot be removed.
     * @throws NotExistException the specified version is not a member of this
     *         collection of Versions.
     * @throws InvalidParameterException the specified Version object is of the
     *         wrong concrete class and cannot be in this collection. For
     *         example, this exception would occur if you tried to remove a
     *         FirmwareVersion from a Domain. The correct operation would be to
     *         remove a DomainVersion from a Domain.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem reading or writing to/from
     *         the persistence repository.
     */
//    public void remove(Version version) throws InUseException,
//            NotExistException, InvalidParameterException, LockBusyException,
//            DeletedException, DatastoreException;

}
