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

/**
 * The related information that must be 
 * maintained and persisted for a {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion}.
 * It is used during domain configuration deployment to a DataPower device.  The 
 * policy name, the domain name, policy type, 
 * and configuration source image information are persisted in the repository. 
 * <p>
 * Note:  The deployment policy version element is contained by a deployment policy element
 * in the WAMT.repository.xml file with the Local File System implementation.
 * </p>
 * <p>
 * The configuration source is a reference to a configuration blob from a device  
 * that should be deployed for this {@link com.ibm.datapower.amt.clientAPI.Domain}. 
 * When using a configuration source, a 
 * domainName and a policy object name serve as an "index" into the source, pointing to the domain, and 
 * associated deployment policy within the configuration source blob. 
 * 
 */
public interface StoredDeploymentPolicyVersion extends StoredVersion {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
    /**
     * Returns Unique identifier for this StoredDeploymentPolicyVersion. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getPrimaryKey()}.
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation. 
     * 
     * <p>
     * Note:  The Local File System implementation uses the unique identifier of the <code>StoredDeploymentPolicy</code> 
     * combined with the version number of this <code>StoredDeploymentPolicyVersion</code>.
     * @see StoredDeploymentPolicy#getPrimaryKey()
     * </p> 
     * 
     * @return the unique identifier for this object
     * @see StoredDeploymentPolicy#getPrimaryKey() 
     */
    String getPrimaryKey();  
    
    /**
     * Returns the persisted <code>StoredDeploymentPolicy</code> that contains this version. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getDeploymentPolicy()}.
     * <p>
     * Note:  For the Local File System implementation, the deploymentpolicy element contains  
     * version elements in the WAMT.repository.xml file.
     * </p>  
     * @return instance of StoredDeploymentPolicy which holds all the related versions
     */

	StoredDeploymentPolicy getDeploymentPolicy();

    /**
     * Gets the domain name on the <code>StoredDeploymentPolicyVersion</code>. It is used as an index into
     * the configuration source.  A version is created with each domain deployment.  It is called by 
     * {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getPolicyDomainName()}.
     * <p>
     * Note:  For the Local File System implementation, the policy domain name is stored as an
     * attribute on the policydeployment version element.
     * </p> 
     * @return the policy domain name that serves as an "index" into the 
     * configuration source, and points
     * to the domain within the domain configuration blob. 
     * 
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicy#setPolicyExport(com.ibm.datapower.amt.clientAPI.URLSource, String, String)
     */
	String getPolicyDomainName();

    /**
     * Gets the DeploymentPolicyType on the <code>StoredDeploymentPolicyVersion</code>. It is used internally
     * and cannot be set by users of the clientAPI. 
     * <p>
     * Note:  The policy type is an enumerated type which is stored as an
     * attribute on the policydeployment version element in Local File System implementation,
     * </p>
     * @return the persisted deployment policy type on the DeploymentPolicyVersion
     * 
     * @see StoredVersion#getVersionedObject()
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#XML
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicyType#EXPORT
     */
	DeploymentPolicyType getPolicyType();

    /**
     * Gets the policy object name persisted on the <code>StoredDeploymentPolicyVersion</code> object.  This is
     * exposed by {@link com.ibm.datapower.amt.clientAPI.DeploymentPolicyVersion#getPolicyName()}.
     * <p>
     * Note:  The policy name is stored as an
     * attribute on the policydeployment version element by the Local File System implementation,
     * </p>
     * @return the policy name that is used along with the domain name to 
     * index into the configuration source and point to the 
     * deployment policy and associated domain within the configuration source. 
     * @see StoredVersion#getVersionedObject()
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicy#setPolicyExport(com.ibm.datapower.amt.clientAPI.URLSource, String, String)
     */
	String getPolicyName();	
	

    /**
     * Sets the policy object name on the <code>StoredDeploymentPolicyVersion</code> object which can be used along 
     * with the configuration source, domain name and policy object name during domain configuration
     * deployment. This is invoked internally by by clientAPI when a domain source configuration is
     * deployed.     
     * <p>
     * Note:  The policy name is stored as an
     * attribute on the policydeployment version element by the Local File System implementation,
     * </p>
     * @param policyName the policy name that is used along with the domain name to 
     * index into the configuration source and points to the domain, and 
     * associated deployment policy within the configuration source.
     * @see com.ibm.datapower.amt.clientAPI.DeploymentPolicy#setPolicyExport(com.ibm.datapower.amt.clientAPI.URLSource, String, String)
     * @see StoredVersion#getVersionedObject()
     * 
     */	
	void setPolicyName(String policyName);	

    /**
     * Sets the Policy Domain name on the <code>StoredDeploymentPolicyVersion</code> object which can be used along 
     * with the configuration source, domain name and policy object name during domain configuration
     * deployment.
     * <p>
     * Note: The policy domain name is stored as an
     * attribute on the policydeployment version element by the Local File System implementation.
     * </p> 
     * @param policyDomain the policy domain name that serves as an "index" into the 
     * configuration source, and points
     * to the domain within the configuration source blob. 
     * @see StoredVersion#getVersionedObject()
     */
	void setPolicyDomainName(String policyDomain);
		
//    /**
//     * Returns the persisted location of the configuration source
//     * @return name of the configuration source or null if there is none
//     * 
//     */	
//	public String getVersionImageFileLocation();
}
