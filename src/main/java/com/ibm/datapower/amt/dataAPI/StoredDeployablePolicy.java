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
 * 
 * The information related to a {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy} that must be managed and persisted when
 * domain configuration is deployed to a DataPower device. The information
 * relates to the versions of a deployment policy used in domain configuration deployment. 
 * The highest version is the latest deployment policy version used in domain deployment. This interface
 * is extended by <code>StoredDeploymentPolicy</code>.
 * 
 */
public abstract interface StoredDeployablePolicy extends Persistable{

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
//    /**
//     * Get the StoredVersion that is desired to be deployed on the
//     * StoredDeployableConfiguration in the StoredManagedSet. 
//     * 
//     * @return version the desired version
//     */
//    public StoredDeploymentPolicyVersion getDesiredVersion();

    /**
     * Gets the versions of the specified StoredDeployablePolicy from the repository.
     * This is 
     * invoked from {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getVersions()}
     * 
     * 
     * @return the Versions of the specified StoredDeployablePolicy. The
     *         array is in chronological order with the oldest version first in
     *         the array.
     */
    public StoredDeploymentPolicyVersion[] getVersions();

    /**
     * Gets the highest version number that has EVER been used for a version of
     * this object. Used to create unique identifiers for the versions of this
     * object. It is OK for this number to wrap as long as it doesn't create
     * duplicated identifiers among the current set of versions. 
     * The version number is 
     * calculated and maintained by the repository and there is no
     * <code>setHighestVersion</code>.
     * This is
     * called by {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getHighestVersionNumber()}
     * 
     * @return The highest version number that has EVER been for a version of
     *         this object
     */
    public int getHighestVersionNumber();

//    /**
//     * Set the highest version number that has EVER been used for a version of
//     * this object. Used to create unique identifiers for the versions of this
//     * object. It is OK for this number to wrap as long as it doesn't create
//     * duplicated identifiers among the current set of versions.
//     * 
//     * @param newHighestVersionNumber
//     *            The highest version number that has EVER been for a version of
//     *            this object *
//     * @throws DatastoreException
//     *             there was a problem writing the updated value to the
//     *             datastore
//     */
//    public void setHighestVersion(int newHighestVersionNumber)
//            throws DatastoreException;;

    /**
     * Deletes this StoredDeployablePolicy. The
     * <code>StoredDeploymentPolicy</code> will be removed from the <code>StoredDomain</code>
     * that contains it.  The logic to create and remove the <code>StoredDeploymentPolicy</code> is
     * handled by the <code>clientAPI</code> and is not visible via the clientAPI.
     * 
     * This is invoked from {@link com.ibm.datapower.amt.clientAPI.Device#removeManagedDomain(String)}
     * 
     * <p>
     * Note:  The Local File System implementation 
     * clears the StoredDeploymetPolicy object and removes the element from the WAMT.repository.xml file.
     * It does not remove any contained elements.  The logic to remove the contained elements is
     * handled in the <code>clientAPI</code>.
     * </p>  
     * 
     * @throws NotEmptyInRepositoryException
     *             the StoredDomain contained versions
     * @throws DatastoreException
     *             there was a problem writing the updated value to the
     *             datastore
     *  
     */
    public void delete() throws DatastoreException, NotEmptyInRepositoryException;

}
