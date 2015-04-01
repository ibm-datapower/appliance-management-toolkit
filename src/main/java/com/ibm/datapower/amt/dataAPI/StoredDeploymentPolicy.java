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
import com.ibm.datapower.amt.clientAPI.DeploymentPolicyType;
import com.ibm.datapower.amt.clientAPI.URLSource;

/**
 * The information that must be maintained and persisted for a {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy} 
 * that is used in deploying a managed domain configuration. This includes the policy deployment source, and the policy type. 
 * When using a configuration source, a 
 * domainName and a policy object name serve as an "index" into the source, pointing to the domain, and 
 * associated deployment policy within the configuration source blob. 
 * 
 * <p>It is a container for all the versions of this <code>StoredDeploymentPolicy</code>. Each successful domain
 * deployment should persist a domain version element and a corresponding deployment policy version in the repository. 
 * The persisted domain version number should be the same as the deployment version number.  The version number is used by 
 * the manager to correlate the domain version to the policy version. 
 * </p>
 * @see StoredDeploymentPolicy
 */
public interface StoredDeploymentPolicy extends StoredDeployablePolicy {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$
    
//    /**
//     * Set the StoredDomainVersion that is desired to be deployed on this
//     * StoredDomain. 
//     * 
//     * @param domainVersion
//     *            desired version of the StoredDomain to set
//     * @throws DatastoreException
//     *             there was a problem writing the updated value to the
//     *             datastore
//     */
//    public void setDesiredVersion(StoredDeploymentPolicyVersion domainVersion)
//            throws DatastoreException;

    /**
     * Gets the name of the <code>StoredDomain</code> that contains this <code>StoredDeploymentPolicy</code>. 
     * There is only one DeploymentPolicy persisted for each Domain.
     * This name should be human-consumable.  This is exposed by {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getDomain()}
     * 
     * @return StoredDomain return the containing StoredDomain object.
     * @see StoredDomain
     */
    public StoredDomain getDomain();

    /**
     * Returns the Unique identifier for this StoredDeploymentPolicy. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getPrimaryKey()}.
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation.
     * 
     * @see #getPolicyName()
     * @return  the unique identifier for this object
     */
    String getPrimaryKey();
    
    /**
     * Gets the domain name on the <code>StoredDeploymentPolicy</code>. It is used as an index into
     * the configuration source.  It is called by 
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getPolicyDomainName()}.
     *
     * @return the policy domain name that serves as an "index" into the 
     * configuration source, and points
     * to the domain within the domain configuration blob. 
     *
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicy#setPolicyExport(URLSource, String, String)
     */
    public String getPolicyDomainName();     

    /**
     * Sets the Domain name that is to find the deployment domain within
     *        the configuration {@link URLSource} which will be deployed on the device
     *        
     * @see #getPolicyDomainName()       
     * @param policyDomainName domain name that is used index into the configuration source and point to the 
     * domain within the configuration source. 
     */
    public void setPolicyDomainName(String policyDomainName) ;

    /**
     * Returns the domain configuration source that was persisted on this DeploymentPolicy object. This can be
     * set by invoking 
     * 
     * <p>
     * Note: In the Local File System implementation, the policy domain name is stored as an
     * string attribute on the policydeployment element.
     * </p> 
     * @return  the configuration {@link URLSource} for this StoredDeploymentPolicy
     */    
    public URLSource getPolicyURL();

    /*
     * Set the configuration source on this DeploymentPolicy object. This can be invoked when the manager
     * creates a deployment policy.
     * 
     * @see {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#setPolicyExport(URLSource, String, String)} 
     * @see {@link com.ibm.datapower.amt.clientAPI.Domain#setSourceConfiguration(URLSource)} 
     * @see #setPolicyType(DeploymentPolicyType)
     * @param policyURL is a reference to the location of configuration blob from a device
     */     
    //public void setPolicyURL(URLSource policyURL) ;

    /**
     * Gets the PolicyType on this DeploymentPolicy object.  This data is for internal use and is not 
     * exposed via the clientAPI
     * 
     * 
     * @return the Deployment Policy Type on this object
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#EXPORT 
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#XML
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#NONE
     */
    public DeploymentPolicyType getPolicyType();

    /*
     * Get the PolicyType on this DeploymentPolicy object
     * 
     * @see #setPolicyType(DeploymentPolicyType)
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#EXPORT
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#XML
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#NONE     *
     */
    //public void setPolicyType(DeploymentPolicyType policyType);
    
    /**
     * Returns the policy name that is used along with the domain name to 
     * index into the configuration source and point to the 
     * deployment policy and associated domain within the configuration source.     
     * 
     * @return the policy name set on this StoredDeploymentPolicy to index into the
     * persisted configuration source
     */    
    public String getPolicyName();
    
    /**
     * Persists the timestamp of the deployed policy source at the time of domain configuration deployment.
     * This persisted timestamp can be used later to compare with timestamp on the policy source to determine
     * if the policy source has changed.  If the policy source has a more recent timestamp than the
     * persisted timestamp, the manager may synchronize the domain.  This is used internally by the clientAPI.
     *
     * @param  synchDate time stamp on the configuration source file
     * @see #getLastModifiedOfDeployedSource()
     */      
    public void setLastModifiedOfDeployedSource(long synchDate);   

    /**
     * Gets the "last modified" timestamp of the policy source persisted on this deployment policy object.
     * This retrieved timestamp can be used to determine
     * if the policy source has a more recent timestamp than the persisted timestamp.  If the policy source has a more recent timestamp than the
     * persisted timestamp, the manager may synchronize the domain if other conditions are also met. This is for internal use only.
     *
     * @see #setLastModifiedOfDeployedSource(long)
     * @return  timestamp on the StoredDeploymentPolicy
     */     
    public long getLastModifiedOfDeployedSource();
    
    /**
     * Gets the DeploymentPolicyVersion object with the specified version number. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicy#getVersion(int)} 
     * 
     * @return  StoredDeploymentPolicyVersion if found, otherwise return 
     */     
    public StoredDeploymentPolicyVersion getVersion (int versionNumber);

}
