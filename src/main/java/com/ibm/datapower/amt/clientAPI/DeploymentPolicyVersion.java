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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlbeans.impl.util.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A class to track the versions of a DeploymentPolicy. There may be multiple
 * DeploymentPolicyVersions for a single DeploymentPolicy. A DeploymentPolicyVersion
 * is created for each successful Domain deployment, even if the DeploymentPolicy 
 * is empty. A DeploymentPolicyVersion is tightly coupled with the {@link DomainVersion}.  
 * If the DomainVersion is deleted, the associated DeploymentPolicyVersion is deleted
 * too.
 * <p />
 * This object (by way of the
 * {@link com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion} member) has the 
 * {@link Blob} that contains the data that represents this deployment policy's
 * configuration. 
 * <p />
  * For more information refer to the javadoc for {@link DeploymentPolicy} and 
 * {@link Version}.
 * <p>
 * <p>
 * 
 * @version SCM ID: $Id: DeploymentPolicyVersion.java,v 1.5 2011/04/18 03:27:17 lsivakumxci Exp $
 * @see DeploymentPolicy
 * @see Domain#remove(Version)
 */
//* Created on Mar 11, 2010
public class DeploymentPolicyVersion implements Version, Persistable {
    private volatile StoredDeploymentPolicyVersion persistence = null;
    /*
     * All the members of this class should be persisted. Please see any
     * class-specific members listed above, and also see all the members in the
     * parent class.
     */
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = DeploymentPolicyVersion.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Instantiate a DeploymentPolicyVersion object.
     * @throws LockBusyException
     * @throws DeletedException
     * @throws NotExistException
     */
    DeploymentPolicyVersion(DeploymentPolicy deploymentPolicy, Date timestamp, String userComment) 
        throws DatastoreException, LockBusyException, DeletedException {
        final String METHOD_NAME = "DeploymentPolicyVersion(DeploymentPolicy...)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, deploymentPolicy);
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredDomain storedDomains = deploymentPolicy.getStoredInstance().getDomain();
        Domain domain = mapper.getVia(storedDomains);
        Device device = domain.getDevice();
        
        //No need to lock, since this operation is protected by the lock acquired in 
        //the domain object code

        //Create a blob from the cached byte array in the deployment policy
        Blob blob = null;
        byte[] bytes = deploymentPolicy.getCachedBytes();
        if (bytes != null) {
            blob = new Blob(bytes);
        }
        
        // no need to check that it doesn't already exist, just create it
        Manager manager = Manager.internalGetInstance();
        Repository repository = manager.getRepository();
        this.persistence = repository.createDeploymentPolicyVersion(deploymentPolicy.getStoredInstance(),
                                                          blob, userComment, timestamp);
        
        // set the rest of the non-persisted members
        
        // add it to the persistence mapper
        mapper = PersistenceMapper.getInstance();
        mapper.add(this.getStoredInstance(), this);
        
        String captureBlob = Configuration.get(Configuration.KEY_CAPTURE_VERSION_BLOB);
        if ((blob != null) && (Boolean.valueOf(captureBlob).booleanValue())) {
            try {
                Date now = new Date();
                long ms = now.getTime();
                String filename = Configuration.getRootDirectory() + 
                    "deploymentPolicyBlob-" + device.getPrimaryKey() + "-" + domain.getName() + "-" + deploymentPolicy.getName() + "-" + ms + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "capturing blob in file " + filename); //$NON-NLS-1$
                // blob is in base64 format
                byte[] encoded = blob.getByteArray();
                byte[] decoded = Base64.decode(encoded);
                encoded = null;
                Blob decodedBlob = new Blob(decoded);
                decoded = null;
                decodedBlob.toFile(filename);
                decodedBlob = null;
            } catch (IOException e) {
                // just eat it
            }
        }
        
        // get rid of one reference to "blob"
        blob = null;
        // check if any versions need to be trimmed AFTER this one is added,
        // otherwise the count of total versions already saved will not be one short.
        deploymentPolicy.trimExcessVersions();        
            
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    DeploymentPolicyVersion(StoredDeploymentPolicyVersion storedDeploytmentPolicyVersion) {
        final String METHOD_NAME = "DeploymentPolicyVersion(StoredDeploymentPolicyVersion)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        /*
         * Normally we would call DeploymentPolicy.trimExcessVersions() here, but let's
         * wait until everything is loaded and referenced so we can properly
         * figured out what the desired versions are before starting to look for
         * things to trim. Since this is called only via Manager.loadDatastore,
         * then do the invocation there.
         */
        
        this.persistence = storedDeploytmentPolicyVersion;
        
        // get the rest of the non-persisted members
        // n/a

        // add it to the persistence mapper
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.add(this.persistence, this);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited from interface */
    public Versionable getVersionedObject() throws DeletedException {
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredDeploymentPolicy storedDeploymentPolicy = this.getStoredInstance().getDeploymentPolicy();
        DeploymentPolicy deploymentPolicy = mapper.getVia(storedDeploymentPolicy);
        return(deploymentPolicy);
    }
    
    /**
     * Get the DeploymentPolicy that this object is a Version of. This is the same as
     * {@link #getVersionedObject()}, except that it returns an object with
     * type <code>DeploymentPolicy</code> instead of the parent type
     * <code>Versionable</code>.
     * 
     * @return the DeploymentPolicy that this object is a Version of.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public DeploymentPolicy getDeploymentPolicy() throws DeletedException {
        return((DeploymentPolicy) this.getVersionedObject());
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Retrieve a reference to the DeploymentPolicyVersion that has the specified primary
     * key.
     * 
     * @param targetKey the primary key to search for
     * @return the DeploymentPolicyVersion that has the specified primary key. May return
     *         <code>null</code> if no DeploymentPolicyVersion with the specified
     *         primary key was found.
     * @see #getPrimaryKey()
     */
    public static DeploymentPolicyVersion getByPrimaryKey(String targetKey) throws DeletedException{
        DeploymentPolicyVersion result = null;
        Manager manager = Manager.internalGetInstance();
        Device[] devices = manager.getAllDevices();
        
    outermost: for (int deviceIndex=0; deviceIndex<devices.length; deviceIndex++) {
        Domain[] domains = null;
        try {
            domains = devices[deviceIndex].getManagedDomains();
        } catch (DeletedException e) {
            domains = new Domain[0];
        }
        for (int domainIndex=0; domainIndex<domains.length; domainIndex++) {
        	
        	DeploymentPolicy deploymentPolicy = null;
        	deploymentPolicy = domains[domainIndex].getDeploymentPolicy();
        	
        	Version[] versions = null;
            try {
                versions = deploymentPolicy.getVersions();
            } catch (DeletedException e2) {
                versions = new Version[0];
            }
            for (int versionIndex=0; versionIndex<versions.length; versionIndex++) {
                DeploymentPolicyVersion deploymentPolicyVersion = (DeploymentPolicyVersion) versions[versionIndex];
                String key = null;
                try {
                    key = deploymentPolicyVersion.getPrimaryKey();
                } catch (DeletedException e1) {
                    key = ""; //$NON-NLS-1$
                }
                if (key.equals(targetKey)) {
                    result = deploymentPolicyVersion;
                    break outermost;
                }
            }
        }
    }
        return(result);
    }
    
    /**
     * Part of the Persistable interface.
     * 
     * @return
     * @throws DeletedException
     */
    StoredDeploymentPolicyVersion getStoredInstance() throws DeletedException {
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
     * Part of the Persistable interface.
     * @throws LockBusyException
     * @throws DatastoreException
     * @throws DeletedException
     */
    void destroy() throws LockBusyException, DeletedException, DatastoreException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);

        //No need to lock because the lock was already acquired in the Domain object code
        try {
        // delete child objects: not applicable
            
        // delete from persistence
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.remove(this.getStoredInstance());
        this.getStoredInstance().delete();
        this.persistence = null;
            
            // clear any references
        } catch (DeletedException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "has already been deleted"); //$NON-NLS-1$
            // object has already been deleted, just eat the exception
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited from interface */
    public int getVersionNumber() throws DeletedException {
        return(this.getStoredInstance().getVersionNumber());
    }

    /* javadoc inherited from interface */
    public Date getTimestamp() throws DeletedException {
        return(this.getStoredInstance().getTimestamp());
    }

    /* javadoc inherited from interface */
    public String getUserComment() throws DeletedException {
        return(this.getStoredInstance().getUserComment());
    }

    /* javadoc inherited from interface */
    public void setUserComment(String userComment) throws DeletedException, DatastoreException, DirtySaveException {
        final String METHOD_NAME = "setUserComment"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting userComment: " + userComment); //$NON-NLS-1$
        this.getStoredInstance().setUserComment(userComment);
        Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
    }

    /* javadoc inherited from interface */
    public Blob getBlob() throws DeletedException {
        return(this.getStoredInstance().getBlob());
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
        return("DomainVersion[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is relative within the context of
     * a Domain within a ManagedSet. For example "3".
     * 
     * @return a human-readable name that represents this object. This includes the version number.
     */
    public String getRelativeDisplayName() {
        String result = null;
        try {
            int number = this.getVersionNumber();
            result = " Version:" + String.valueOf(number);
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces. This name is absolute within the context of
     * the whole system. 
     * 
     * @return a human-readable name that represents this object.Example:
     * "DeploymentPolicyVersion :  Version:1, URL Source:file:///C:/DP/policydefaultdp12.zip, Policy Domain Name:default, Policy Object Name:policy1 for Domain:domain1 on Device:dp12.dp.rtp.raleigh.ibm.com/9.42.101.11 in Managed Set:set1,Domain:domain1 on Device:dp12.raleighnc.ibm.com/9.42.102.72 in Managed Set:set1"
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String deviceName = null;
        String deploymentPolicyName = null;
        String deploymentPolicyVersionName = this.getRelativeDisplayName();
        try {
            DeploymentPolicy deploymentPolicy = this.getDeploymentPolicy();
            deploymentPolicyName = deploymentPolicy.getAbsoluteDisplayName();
            //deviceName = deploymentPolicy.getDomain().getDevice().getManagedSet().getDisplayName();
        } catch (DeletedException e) {
        	deploymentPolicyName = "[deleted]"; //$NON-NLS-1$
            deviceName = "[unavailable]"; //$NON-NLS-1$
        }
        result = deploymentPolicyVersionName+"," + deploymentPolicyName;
        return(result);
    }

//    /* javadoc inherited from interface */
//    public boolean isInUse() throws DeletedException {
//        return(false);
//    }


	public String getPolicyDomainName()throws DeletedException {
		// TODO Auto-generated method stub
		return this.getStoredInstance().getPolicyDomainName();
	}

	public DeploymentPolicyType getPolicyType()throws DeletedException {
		// TODO Auto-generated method stub
		return this.getStoredInstance().getPolicyType();
	}
	
	public String getPolicyName()throws DeletedException {
		// TODO Auto-generated method stub
		return this.getStoredInstance().getPolicyName();
	}
    
}
