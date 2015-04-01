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
import com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode;
import com.ibm.datapower.amt.clientAPI.URLSource;

/**
 * <p>It represents a domain on a DataPower device and holds all information that must be 
 * maintained and persisted for a {@link com.ibm.datapower.amt.clientAPI.Domain}.  This includes the Domain name, the device the domain resides on, synchronization 
 * mode, configuration source and the associated deployment policy.
 * It is a container for all the {@link com.ibm.datapower.amt.dataAPI.StoredDomainVersion}s 
 * deployed on this domain, as well as the {@link com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy} 
 * and {@link com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion} 
 * used in the domain deployment</p>
 * 
 */
public interface StoredDomain extends StoredDeployableConfiguration {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /**
     * Gets the name of this <code>StoredDomain</code>. This name should be human-consumable.
     * The name is immutable.
     *
     * @see #getPrimaryKey()
     * @return the name of this StoredDomain
     */
    public String getName();

    /**
     * Returns the Device on which this <code>StoredDomain</code> resides.  This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getDevice()}
     *
     * @return the StoredDevice object
     * 
     */
    public StoredDevice getDevice();

    /**
     * Returns the Unique identifier for this StoredDomain. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getPrimaryKey()}
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation. 
     *      
     * @return the unique identifier for this object
     */
    String getPrimaryKey();
    
    /* Called internally  in dataAPI
     * Remove the specified Deployment Policy contained in this domain from the repository. This is
     * invoked by {@link com.ibm.datapower.dataAPI.StoredDeploym}
     * 
     * <p>
     * Note: The name combined with the <code>Device</code> serialNumber is used as the primary key of
     * this object in the repository. 
     * </p>
     * 
     * @param policy
     *            the policy to remove
     * @throws DatastoreException
     *             there was a problem updating datastore
     */
    // public void removeDeploymentPolicy(StoredDeploymentPolicy policy) throws DatastoreException;
  
    
    /* Called internally  in dataAPI
     * Set the specified Deployment Policy on this domain.
     * 
     * @param deploymentPolicy
     *            the deployment policy to set for this domain
     * @throws DatastoreException
     *             there was a problem updating datastore
     */    
    //public void setDeploymentPolicy(StoredDeploymentPolicy deploymentPolicy) throws DatastoreException;
    
    /**
     * Gets the Deployment Policy on this StoredDomain. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getDeploymentPolicy()}
     * 
     * @return the deployment policy for this domain
     * @see com.ibm.datapower.amt.clientAPI.Domain#setDeploymentPolicy(URLSource, String, String)     
     */     
    public StoredDeploymentPolicy getDeploymentPolicy();

    /**
     * Sets the Configuration source on this StoredDomain. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)}
     * 
     * @param urlSource indicates the source for the configuration to be deployed to the Domain
     * @see com.ibm.datapower.amt.clientAPI.Domain#deployConfiguration()      
     * @see com.ibm.datapower.amt.clientAPI.Domain#setDeploymentPolicy(URLSource, String, String)     
     */
    
    public void setSourceURL(URLSource urlSource) ;

    /**
     * Gets the configuration source location where the configuration is stored. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getSourceConfiguration() } and 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)} 
     * 
     * @return the URLSource for this domain configuration source
     */
    public URLSource getSourceURL() ;

    
    /**
     * Sets the Synchronization Mode on this StoredDomain to AUTO or MANUAL. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#setSynchronizationMode(DomainSynchronizationMode)}
     * 
     * @param synchMode a value of AUTO indicates the domain will be synchronized automatically 
     * @see com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode#AUTO
     * @see com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode#MANUAL
     */    
	public void setSynchMode(DomainSynchronizationMode synchMode);

	
    /**
     * Gets the Synchronization Mode on this StoredDomain. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getSynchronizationMode()}
     * 
     * @return synchronization mode persisted on the StoredDomain
     * 
     * @see com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode#AUTO
     * @see com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode#MANUAL
     */
	public DomainSynchronizationMode getSynchMode();
	
    /**
     * Internal use only.
     * Sets the time (in milliseconds) to record the timestamp of the domain configuration source
     * when it is deployed. This timestamp is used to check if the timestamp of source has changed
     * since the last deployment.  If the <code>DomainSynchronizationMode</code>
     * is set to {@link DomainSynchronizationMode#AUTO}, the manager periodically checks for a 
     * change in timestamp of the configuration source.  If a change is detected, it automatically
     * synchronizes the domain configuration by performing a redeploy. See {@link com.ibm.datapower.amt.clientAPI.Domain#setSynchronizationMode(DomainSynchronizationMode)}
     *  
     * 
     * @param synchDate time in milliseconds when the domain configuration is deployed and 
     * persisted in the repository 
     * 
     * @see #getLastModifiedOfDeployedSource()
     */    
    public void setLastModifiedOfDeployedSource(long synchDate); 
  
    /**
     * Internal use only. 
     * Gets the time stamp (in milliseconds) on the domain source Configuration when it 
     * was last deployed. This timestamp is used to perform automatic domain synchronization
     * when the <code>DomainSynchronizationMode</code>
     * is set to {@link DomainSynchronizationMode#AUTO}. See {@link #setLastModifiedOfDeployedSource(long)}
     * for details of how the timestamp is used for domain synchronization.  
     * 
     * @return time in milliseconds
     * @see #setLastModifiedOfDeployedSource(long)
     */ 
    public long getLastModifiedOfDeployedSource();
    
    /**
     * Internal Use Only
     * <p/> 
     * Gets the value of the OutOfSynch attribute on this StoreDomain.  A value of TRUE indicates that
     * domain on the device is out of synch with the source configuration last deployed and
     * persisted in the repository. The value is TRUE if users invoke any "set" method on Domain 
     * that causes the domain configuration to get out of synch.
     * 
     * @return a value of true indicates the domain is not synchronized
     * @see com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)
     * @see com.ibm.datapower.amt.clientAPI.Domain#setDeploymentPolicy(URLSource,String,String)
     * @see com.ibm.datapower.amt.clientAPI.Domain#setSynchronizationMode(DomainSynchronizationMode)
     */ 
    public boolean getOutOfSynch(); 

    /**
     * Internal use only
     * Sets the value of the OutOfSynch attribute on the StoreDomain.  The value is set to TRUE
     * when set methods are called on the <code>Domain</code> and the
     * domain on the device becomes out of synch with the configuration last deployed and
     * persisted in the repository. This is set to FALSE when the domain is re-synched making
     * the device domain configuration the same as the deployed domain configuration
     * 
     * @param outOfSynch the value of true indicates the domain is not synchronized.
     * @see com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)
     * @see com.ibm.datapower.amt.clientAPI.Domain#setDeploymentPolicy(URLSource,String,String)
     * @see com.ibm.datapower.amt.clientAPI.Domain#setSynchronizationMode(DomainSynchronizationMode)

     */ 
    public void setOutOfSynch(boolean outOfSynch);
    
//    /**
//     * Get the value of the checkVersionSynch attribute on the Domain.  A value of TRUE indicates that
//     * domain settings have changed since last deployment and the domain is out of synch with the configuration 
//     * last deployed and persisted in the repository. 
//     * 
//     * @return a a value of true or false
//     */ 
//    public boolean getCheckVersionSynch(); 
//
//    /**
//     * Set the value of the checkVersionSynch attribute on the Domain.  A value of TRUE indicates that
//     * domain settings have changed since last deployment and the domain is out of synch with the configuration 
//     * last deployed and persisted in the repository
//     * 
//     * @param outOfSynch a value of TRUE indicates the domain is not synchronized 
//     * @see DomainSynchronizationMode#AUTO
//     * @see DomainSynchronizationMode#MANUAL
//     */ 
//    public void setCheckVersionSynch(boolean outOfSynch);
//    
    /**
     * Sets the timeout value for quiescing a domain on the DataPower device before configuration deployment.
     * This is invoked 
     * from {@link com.ibm.datapower.amt.clientAPI.Domain#setQuiesceTimeout(int)}.  The time is used to 
     * This timeout value (in seconds) is used for checking the status of a domain 
     * quiesce or unquiesce operation. The quiesce operation is used on firmware versions 3.8.1 or higher to stop a 
     * domain before deploying source configuration or updating it.
     *
     * @param timeout value in seconds
     * @see #getQuiesceTimeout()
     */    
    public void setQuiesceTimeout(int timeout);
    
    /**
     * Gets the persisted timeout value for quiescing a domain on the DataPower device. This is invoked 
     * from {@link com.ibm.datapower.amt.clientAPI.Domain#getQuiesceTimeout}  
     * @return timeout value in seconds
     * @see #setQuiesceTimeout(int)
     */    
    public int getQuiesceTimeout();
        
    /**
     * Add a tag to the domain. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Domain#addTag(String, String)} to set the
     * tag on a <code>StoredTag</code> and persist it in the repository.
     *    
     * @param tag StoreTag 
     * @param tag
     */
    public void add(StoredTag tag);
    
    /**
     Gets the tags for this domain. This is invoked from 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getTagNames()},
     * {@link com.ibm.datapower.amt.clientAPI.Domain#getTagValues(String)},
     * {@link com.ibm.datapower.amt.clientAPI.Domain#removeTag(String)} and 
     * {@link com.ibm.datapower.amt.clientAPI.Domain#removeTag(String, String)},
     *  to retrieve the information from the repository.
     *  
     * @return a array of StoredTags
     */
    public StoredTag[] getTags();
        
    /**
     * Remove the tags for this domain. This is invoked from 
     *  {@link com.ibm.datapower.amt.clientAPI.Domain#removeTag(String, String)} and 
     *  {@link com.ibm.datapower.amt.clientAPI.Domain#removeTag(String)}
     *  
     * @param tag the StoredTag
     * @throws DatastoreException
     */
    public void remove(StoredTag tag) throws DatastoreException;
    
    /**
     * Remove all tags for this domain. This is invoked from 
     * {@link com.ibm.datapower.amt.clientAPI.Device#removeTags()}
     */
    public void removeTags();
    	
}
