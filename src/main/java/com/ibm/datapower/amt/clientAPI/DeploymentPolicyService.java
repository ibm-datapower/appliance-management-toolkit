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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * 
 */
public class DeploymentPolicyService extends DeploymentPolicy implements Persistable, Versionable {
    public static final String COPYRIGHT_20012_2013 = Constants.COPYRIGHT_2012_2013;
        
    protected static final String CLASS_NAME = DeploymentPolicy.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
        
    DeploymentPolicyService(Domain domain, URLSource policyURL, String policyDomainName, String policyObjectName) throws DeletedException, DirtySaveException, DatastoreException{
    	super(domain, policyURL, policyDomainName, policyObjectName);
    } 
     
    /**
     * Returns the deployment policy enum type.
     */
    public DeploymentPolicyType getPolicyType() {    	
        return(this.policyType);
    }

    /**
     * Returns the policy URL source.
     * @throws DeletedException 
     */
    public URLSource getPolicyURLSource()  {
        return(this.policyURL);
    }

    /**
     * Returns the name of the domain that contains the deployment policy in the blob.
     * @throws DeletedException 
     */
    public String getPolicyDomainName()   {
        return(this.policyDomainName);
    }

    /**
     * Returns the name of the deployment policy object to use.
     */
    public String getPolicyObjectName()  {
    	 return(this.policyObjectName);
    }

    /**
     * Only used for stored instance.
     */
    public String getName() throws DeletedException {        
    	String result = null;
    	if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
        	result = "[Deployment Policy is not defined]";
    	}else{
    		result = this.getPolicyDomainName();
    	}
    	return result;
    }

    public Domain getDomain() throws DeletedException {    	 
    	 return this.domain;
    }    
    
    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return( this.domain.getDevice().getSerialNumber()+":"+this.domain.getPrimaryKey()+":"+this.getPolicyObjectName());
    }
    
    /**
     * Part of the persistable interface.
     * @throws InvalidParameterException
     * @throws InUseException
     * @throws DatastoreException
     * @throws NotEmptyException
     * @throws LockBusyException
     */
    void destroy() throws InUseException, InvalidParameterException, 
    NotEmptyException, DatastoreException, LockBusyException, DeletedException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, METHOD_NAME + "() is not supported in this release"); //$NON-NLS-1$    	
    	throw new UnsupportedOperationException();
    }

    /* javadoc inherited from interface */
    public DeploymentPolicyVersion[] getVersions()  {
        final String METHOD_NAME = "getVersions"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, METHOD_NAME + "() is not supported in this release"); //$NON-NLS-1$    	
    	throw new UnsupportedOperationException();
    }

    /* javadoc inherited from interface */
    public DeploymentPolicyVersion getVersion(int targetVersionNumber) throws DeletedException {
    	final String METHOD_NAME = "getVersion"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, METHOD_NAME + "() is not supported in this release"); //$NON-NLS-1$    	
    	throw new UnsupportedOperationException();
    }
    
    /**
     * Get the highest version number of all the DeploymentPolicyVersions in this DeploymentPolicy.
     * 
     * @return the highest version number of all the DeploymentPolicyVersions.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public int getHighestVersionNumber() throws DeletedException {
    	final String METHOD_NAME = "getHighestVersionNumber"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, METHOD_NAME + "() is not supported in this release"); //$NON-NLS-1$    	
    	throw new UnsupportedOperationException();
    }

    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String primaryKey = null;
        try {
            primaryKey = this.getPrimaryKey();
        } catch (DeletedException e) {
            primaryKey = "[deleted]"; //$NON-NLS-1$
        }

        return("Domain[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is relative to the context of a
     * Device.
     * 
     * @return a human-readable name that represents this object. For example,
     *         "domain1".
     */
    public String getRelativeDisplayName() {
        String result = null;
           	
        if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
          	result = ("Deployment Policy is not defined");        	
        }else{
           	if (this.getPolicyURLSource()!= null){
           		result = this.getPolicyDomainName() ;
           	}
        }
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is absolute to the context of the system.
     * 
     * @return a human-readable name that represents this object. For example
     * "DeploymentPolicy: URL Source:file:///C:/DP/policydefaultdp12.zip, 
     * Policy Domain Name:default, Policy Object Name:policy1 for Domain:domain1 on Device:dp12.raleighnc.ibm.com/9.42.102.11 in Managed Set:set1".
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String dpName = null;
        String domianName = null;
          	
        if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
        	dpName = ("Deployment Policy is not defined");        	
        }else{
          	if (this.getPolicyURLSource()!= null){
           		dpName = (" URL Source:" + this.getPolicyURLSource().getURL() + 
           	          ", Policy Domain Name:" + this.getPolicyDomainName() +
           	          ", Policy Object Name:" + this.getPolicyObjectName());
           	}
        }
        try {
            Domain domian = this.getDomain();
            domianName = domian.getAbsoluteDisplayName();
        } catch (DeletedException e) {
        	domianName = "[deleted]"; //$NON-NLS-1$
        }
        result = dpName + " for " + domianName; //$NON-NLS-1$
        return(result);
    }
}
