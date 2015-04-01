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

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlbeans.impl.util.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * This class represents the deployment policy that is to be applied to a <code>Domain</code> 
 * during deployment. This deployment policy will not reside on the DataPower device after domain 
 * deployment because it is only applied at deploy-time. 
 * <p>
 * This behavior of this class is controlled by the <code>Domain</code> class, and as such, all 
 * of the methods in this class are for internal use only. 
 * <p>
 * A <code>DeploymentPolicy</code> is tightly coupled with a <code>Domain</code>. When 
 * a <code>Domain</code> object is created an empty <code>DeploymentPolicy</code> object is 
 * created within it. The settings for a deployment policy must be specified via the 
 * <code>setDeploymentPolicy(url, policydomain, policyObjectName)</code> method on 
 * {@link com.ibm.datapower.amt.clientAPI.Domain}. Calling <code>setDeploymentPolicy(null, 
 * null, null)</code> on {@link com.ibm.datapower.amt.clientAPI.Domain} 
 * sets the <code>DeploymentPolicy</code> to an empty state.
 * <p>
 * When calling setPolicyExport on <code>Domain</code>, the URLSource is a reference to a configuration blob from a 
 * device, the domainName and policyObjectName serve as an "index" into the blob, pointing to 
 * the domain, and associated deployment policy within the blob. The domainName specified does 
 * not have to match the name of the <code>Domain</code> that owns this 
 * <code>DeploymentPolicy</code> object.    
 * <p>
 *
 * @version SCM ID: $Id: DeploymentPolicy.java,v 1.9 2011/04/12 15:21:01 jcates Exp $
 * @see Domain
 * @see DeploymentPolicyVersion
 */
public class DeploymentPolicy implements Persistable, Versionable {
    private volatile StoredDeploymentPolicy persistence = null;
    /*
     * All the attributes above this line will be persisted to the datastore.
     */
    
    private byte[] byteArray = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    
    protected static final String CLASS_NAME = DeploymentPolicy.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    protected URLSource policyURL = null;
    protected String policyDomainName = null;
    protected String policyObjectName = null;
    protected DeploymentPolicyType policyType = DeploymentPolicyType.NONE;
    protected Domain domain = null;

    /**
     * Create a new deployment policy object
     * @param domain2 
     * @throws DatastoreException 
     * @throws DirtySaveException 
     * @throws DeletedException 
     */
    DeploymentPolicy(Domain domain) throws DeletedException, DirtySaveException, DatastoreException
    {
        final String METHOD_NAME = "DeploymentPolicy()"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        policyType = DeploymentPolicyType.NONE;
        this.domain = domain;
        doPersist();
        logger.exiting(CLASS_NAME, METHOD_NAME);                
    }
    
    DeploymentPolicy(Domain domain, URLSource policyURL, String policyDomainName, String policyObjectName) throws DeletedException, DirtySaveException, DatastoreException {
    	this.policyURL = policyURL;
    	this.policyDomainName = policyDomainName;
    	this.policyObjectName = policyObjectName;
    	this.policyType = DeploymentPolicyType.EXPORT;
    	this.domain = domain;
    }
    
    DeploymentPolicy(StoredDeploymentPolicy storedDeploymentPolicy) {
        final String METHOD_NAME = "DeploymentPolicy(StoredDeploymentPolicy)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, storedDeploymentPolicy);
        
        this.persistence = storedDeploymentPolicy;
        this.policyURL = storedDeploymentPolicy.getPolicyURL();
        this.policyDomainName = storedDeploymentPolicy.getPolicyDomainName();
        this.policyObjectName = storedDeploymentPolicy.getPolicyName();
        
        try {
			this.domain = getDomain();
		} catch (DeletedException e) {
			// This should never happen since this constructor is called from 
			// code reconstructing objects from data store
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
		}
        
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        // add it to the mapper
        mapper.add(this.persistence, this);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }    
     
   /**
    * When calling setPolicyExport, the URLSource is a reference to the configuration blob, the 
    * domainName and policyObjectName serve as an "index" into the blob, pointing to the domain, and 
    * associated deployment policy within the blob. The domainName specified does not have to match the 
    * name of the <code>Domain</code> that owns this <code>DeploymentPolicy</code> 
    * object.
    * 
    * @param url - is a URLSource that points to a configuration blob.  This parameter is required.
    * @param policyDomain - this is the first piece of information used to find the deployment policy within
    *        the configuration source specified by the URLSource.  This parameter is only required if the 
    *        source is a backup, since a backup may contain multiple domains. 
    * @param policyObjName - this is the second piece of information used to find the deployment policy within
    *        the configuration source specified by the URLSource.  This parameter is required.
    * 
    */    
    public void setPolicyExport(URLSource url, String policyDomain, String policyObjName)  throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException, InvalidParameterException{
        final String METHOD_NAME = "setPolicyExport"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        if (url == null){
        	String message = Messages.getString("wamt.clientAPI.DeploymentPolicy.invalidParameter","Source URL");
        	throw new InvalidParameterException(message,"wamt.clientAPI.DeploymentPolicy.invalidParameter","Source URL");
        }    
        
        if (policyObjName == null){
        	String message = Messages.getString("wamt.clientAPI.DeploymentPolicy.invalidParameter","Policy Name");
        	throw new InvalidParameterException(message,"wamt.clientAPI.DeploymentPolicy.invalidParameter","Policy Name");
        }         
        
    	policyURL = url;
    	policyDomainName = policyDomain;
        policyObjectName = policyObjName;
        policyType = DeploymentPolicyType.EXPORT;  

        doPersist();        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * When calling setPolicyXML, the URLSource is a reference to a deployment policy in XML format.
     * The XML must follow the schema for the deployment-policy type in the AMP protocol.  
     * 
     * Below is an example deployment policy in XML:
     * <p>
     * {@code
     * <Policy>
     *     <ModifiedConfig>
     *        <Match>*&#047;*&#047;wsm&#047;wsm-endpointrewrite?Name=simplyWSP&amp;Property=WSEndpointRemoteRewriteRule/RemoteEndpointPort&amp;Value=12345</Match>
     *        <Type>change</Type>
     *        <Property/>
     *        <Value>12345</Value>
     *     </ModifiedConfig>
     *     <ModifiedConfig>
     *        <Match>*&#047;*&#047;wsm&#047;wsm-endpointrewrite?Name=simplyWSP&amp;Property=WSEndpointRemoteRewriteRule/RemoteEndpointHostname&amp;Value=.*</Match>
     *        <Type>change</Type>
     *        <Property/>
     *        <Value>new.hostname.value</Value>
     *     </ModifiedConfig>
     * </Policy>
     * }
     * 
     * @param url - is a URLSource that points to a deployment policy in XML format.  This parameter is required.
     * 
     */    
    public void setPolicyXML(URLSource url)  throws DeletedException, DatastoreException, InvalidParameterException{
        final String METHOD_NAME = "setPolicyExport"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        if (url == null){
        	String message = Messages.getString("wamt.clientAPI.DeploymentPolicy.invalidParameter","Source URL");
        	throw new InvalidParameterException(message,"wamt.clientAPI.DeploymentPolicy.invalidParameter","Source URL");
        }    
                
    	policyURL = url;
        policyType = DeploymentPolicyType.XML;
        
        doPersist();        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * This method is called when <code>null</code> parameters are passed to 
     * <code>setDeploymentPolicy(null, null, null)</code> in <code>Domain</code>   
     * 
     */    
    public void setPolicyNone()  throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException{
        final String METHOD_NAME = "setPolicyUnknown"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
    	policyURL = null;
    	policyDomainName = null;
        policyObjectName = null;
        policyType = DeploymentPolicyType.NONE;  
        
        doPersist();        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    private void doPersist() throws DeletedException, DirtySaveException, DatastoreException {
        // get or construct the StoredDeploymentPolicy instance
        Manager manager = Manager.internalGetInstance();
        Repository repository = manager.getRepository();
        this.persistence = repository.createDeploymentPolicy(this.domain.getStoredInstance(),this.policyObjectName, this.policyURL, this.policyDomainName,this.policyType);
        
        // add it to the persistence mapper
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.add(this.getStoredInstance(), this);
        manager.save(Manager.SAVE_UNFORCED);        
    }

    /**
     * Retrieves the deployment policy from the URLSource for EXPORT type.
     * Once retrieved, the policy is locally cached in the byteArray.
     * For a policy reference, a byteArray is not needed.
     * This may be a long running and should only be called in the 
     * deploy configuration action. The byteArray should be 
     * dereferenced using once it is no longer needed.
     * @throws IOException 
     * @throws AMPException 
     * @throws InvalidParameterException 
     * @throws InUseException 
     * @throws NotExistException 
     * @throws DeletedException 
     */
    protected void getPolicy() throws DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException, IOException 
    {
    	switch (this.getPolicyType()){
    	case EXPORT:
        	byteArray = policyURL.getBlob().getByteArray();
        	break;
    	case XML:
    		// For XML type, the deployment policy should not be B64 encoded.
        	byteArray = Base64.decode(policyURL.getBlob().getByteArray());
        	break;
        //case REFERENCE:
    	case NONE:
        default:
    		byteArray = null;
    		break;
    	}
        return;
    }

    public byte[] getCachedBytes() {
    	byte[] result = null; 
    	
    	if ( this.byteArray != null )
    		result = this.byteArray.clone();    	
    	return result;
    }

    // Use this to set byteArray to null when it is no longer needed.
    protected void setCachedBytes(byte[] byteArray) {
    	this.byteArray = byteArray;
    }
    
    void trimExcessVersions() {
        final String METHOD_NAME = "trimExcessVersions"; //$NON-NLS-1$
        Manager manager = null;
        int allowed = 0;
        Version[] versions = null;
        int have = 0;
        int highest = 0;
        try {
            manager = Manager.internalGetInstance();
            allowed = manager.getMaxVersionsToStore();
            versions = this.getVersions();
            have = versions.length;
            highest = this.getHighestVersionNumber();
        } catch (DeletedException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("CannotTrim"), e); //$NON-NLS-1$
            return;
        }
        if (have > allowed) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "need to trim, have " + have + //$NON-NLS-1$
                    " > allowed " + allowed + //$NON-NLS-1$
                    ", highest " + highest); //$NON-NLS-1$
            // work from the oldest version to the newest
            for (int i=1; (i<=highest) && (have>allowed); i++) {
                Version version = null;
                try {
                    version = this.getVersion(i);
                } catch (DeletedException e) {
                    // skip this one
                    version = null;
                    // if deleted then we don't really have it
                    have--;
                }
                if (version != null) {
                    try {
//                        if (!version.isInUse()) {
                            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                    "attempting to trim " + version); //$NON-NLS-1$
                            this.remove(version);
                            have--;
//                        }
                    } catch (DMgrException e) {
                        // just skip this one
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                "Unable to check/perform trimming of " + version, e); //$NON-NLS-1$
                    }
                }
            }
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "now have " + have + " versions"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "no versions need to be trimmed, have " + have + //$NON-NLS-1$
                    " <= allowed " + allowed); //$NON-NLS-1$
        }
    }

    /**
     * Returns the deployment policy enum type.
     */
    public DeploymentPolicyType getPolicyType() throws DeletedException
    {    	
        return(this.getStoredInstance().getPolicyType());
    }

    /**
     * Returns the policy URL source.
     * @throws DeletedException 
     */
    public URLSource getPolicyURLSource() throws DeletedException 
    {
        return(this.getStoredInstance().getPolicyURL());
    }

    /**
     * Returns the name of the domain that contains the deployment policy in the blob.
     * @throws DeletedException 
     */
    public String getPolicyDomainName() throws DeletedException 
    {
        return(this.getStoredInstance().getPolicyDomainName());
    }

    /**
     * Returns the name of the deployment policy object to use.
     * @throws DeletedException 
     */
    public String getPolicyObjectName() throws DeletedException 
    {
    	 return(this.getStoredInstance().getPolicyName());
    }

    /**
     * Only used for stored instance.
     */
    public String getName() throws DeletedException {
        
    	String result = null;
    	if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
        	result = "[Deployment Policy is not defined]";
    	}else{
    		result = this.getStoredInstance().getPolicyDomainName();
    	}
    	return result;
    }

    public Domain getDomain() throws DeletedException {
    	 Domain result = null;
    	 StoredDomain storedDomain =  this.getStoredInstance().getDomain();
    	 
    	 if (storedDomain != null){
             PersistenceMapper mapper = PersistenceMapper.getInstance();
             result = mapper.getVia(storedDomain);    		 
    	 }
    	 return result;
    }
    
    /**
     * Trim the specified DeploymentPolicyVersion from the repository. This could be
     * user-initiated or invoked by an agent. It will remove the specified
     * DeploymentPolicyVersion, but will not delete the DeploymentPolicy object.
     * 
     * @param version the version to remove from the repository
     * @throws InvalidParameterException the Version parameter is not a
     *         DeploymentPolicyVersion.
     * @throws NotExistException the specified version is not a member of this
     *         DeploymentPolicy.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DatastoreException there was a problem reading or writing to/from
     *         the persistence repository.
     */
    protected void remove(Version version) throws InvalidParameterException, 
    NotExistException, DeletedException, LockBusyException, DatastoreException {
        final String METHOD_NAME = "remove"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, version);

        //This should always be an instanceof DeploymentPolicyVersion
        if (!(version instanceof DeploymentPolicyVersion)) {
            String message = Messages.getString("wamt.clientAPI.Domain.notDomainVersion", version.getClass().getName()); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Domain.notDomainVersion", version.getClass().getName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        try {
            //No need to lock the device since it is already locked at the domain level

            DeploymentPolicyVersion policyVersion = (DeploymentPolicyVersion) version;
            policyVersion.destroy();
            Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
        } finally {
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
    	
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Part of the Persistable interface.
     * 
     * @return
     * @throws DeletedException
     */
    StoredDeploymentPolicy getStoredInstance() throws DeletedException {
        final String METHOD_NAME = "getStoredInstance"; //$NON-NLS-1$
        if (this.persistence == null) {
        	String message = Messages.getString("NoPersistence");
            DeletedException e = new DeletedException(message,"NoPersistence"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return(this.persistence);
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
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        
        // delete the child DeploymentPolicyVersion objects
        Version[] versions = this.getVersions();
        for (int i=0; i<versions.length; i++) {
        	DeploymentPolicyVersion deploymentPolicyVersion = (DeploymentPolicyVersion) versions[i];
            try {
                this.remove(deploymentPolicyVersion);
            } catch (NotExistException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                            Messages.getString("wamt.clientAPI.Domain.verNotExist"), e); //$NON-NLS-1$
                // fine, just eat the exception
            } catch (DeletedException e) {
                // fine, just eat the exception
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "version is already deleted", e); //$NON-NLS-1$
            }
        }
        
        // delete from persistence
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "deleting from persistence"); //$NON-NLS-1$
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.remove(this.getStoredInstance());
        this.getStoredInstance().delete();
        this.persistence = null;
            
        // clear any references
        
        //Exit
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited from interface */
    public DeploymentPolicyVersion[] getVersions() throws DeletedException {
        final String METHOD_NAME = "getVersions"; //$NON-NLS-1$
        DeploymentPolicyVersion[] result = null;
        StoredVersion[] storedDeploymentPolicyVersions = this.getStoredInstance().getVersions();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        Vector deploymentPolicyVersions = new Vector();
        for (int i=0; i<storedDeploymentPolicyVersions.length; i++) {
            StoredDeploymentPolicyVersion storedDeploymentPolicyVersion = (StoredDeploymentPolicyVersion) storedDeploymentPolicyVersions[i];
            DeploymentPolicyVersion deploymentPolicyVersion = null;
            try {
            	deploymentPolicyVersion = mapper.getVia(storedDeploymentPolicyVersion);
            } catch (DeletedException e) {
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                            Messages.getString("VersionDeleted", storedDeploymentPolicyVersion), e); //$NON-NLS-1$
                deploymentPolicyVersion = null;
            }
            if (deploymentPolicyVersion != null) {
            	deploymentPolicyVersions.add(deploymentPolicyVersion);
            }
        }
        result = new DeploymentPolicyVersion[deploymentPolicyVersions.size()];
        for (int i=0; i<deploymentPolicyVersions.size(); i++) {
            result[i] = (DeploymentPolicyVersion) deploymentPolicyVersions.get(i);
        }
        return(result);
    }

    /* javadoc inherited from interface */
    public DeploymentPolicyVersion getVersion(int targetVersionNumber) throws DeletedException {
    	DeploymentPolicyVersion result = null;
        StoredDeploymentPolicyVersion matchingStoredDeploymentPolicyVersion = null;
        StoredVersion[] storedVersions = this.getStoredInstance().getVersions();
        for (int i=0; i<storedVersions.length; i++) {
            if (storedVersions[i].getVersionNumber() == targetVersionNumber) {
                matchingStoredDeploymentPolicyVersion = (StoredDeploymentPolicyVersion) storedVersions[i];
                break;
            }
        }
        if (matchingStoredDeploymentPolicyVersion != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(matchingStoredDeploymentPolicyVersion);
        }
        return(result);
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
        StoredDeploymentPolicy storedDeploymentPolicy = this.getStoredInstance();
        int result = storedDeploymentPolicy.getHighestVersionNumber();
        return(result);
    }
    
//    /* javadoc inherited from interface */
//    public Version getDesiredVersion() throws DeletedException {
//        return(null);
//    }
//
//    /* javadoc inherited from interface */
//    public ProgressContainer setDesiredVersion(Version version) 
//        throws DatastoreException, InvalidParameterException, DeletedException, LockBusyException, FullException {
//      return(null);
//    }

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
        try {    	
            if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
            	result = ("Deployment Policy is not defined");        	
            }else{
            	if (this.getPolicyURLSource()!= null){
            	 result = this.getPolicyDomainName() ;
            	}
            }        

        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is absolute to the context of the
     * system.
     * 
     * @return a human-readable name that represents this object. For example
     * "DeploymentPolicy: URL Source:file:///C:/DP/policydefaultdp12.zip, Policy Domain Name:default, Policy Object Name:policy1 for Domain:domain1 on Device:dp12.raleighnc.ibm.com/9.42.102.11 in Managed Set:set1".
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String dpName = null;
        String domianName = null;
        try {    	
            if(this.getPolicyType().equals(DeploymentPolicyType.NONE)){
            	dpName = ("Deployment Policy is not defined");        	
            }else{
            	if (this.getPolicyURLSource()!= null){
            		dpName = (" URL Source:" + this.getPolicyURLSource().getURL() + 
            	          ", Policy Domain Name:" + this.getPolicyDomainName() +
            	          ", Policy Object Name:" + this.getPolicyObjectName());
            	}
            }        
        	
            //result = this.getName();
        } catch (DeletedException e) {
        	dpName = "[deleted]"; //$NON-NLS-1$
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
    
	//Get the "last modified" timestamp for the deployed deployment policy - this is the timestamp of the 
	//source file that was deployed. this only applies if the source was file/http/https URL scheme.
	protected long getLastModifiedOfDeployedSource() throws DeletedException {
		return this.getStoredInstance().getLastModifiedOfDeployedSource();
	}
	
	protected void setLastModifiedOfDeployedSource(long milli) throws DeletedException {
		this.getStoredInstance().setLastModifiedOfDeployedSource(milli);
	}
}
