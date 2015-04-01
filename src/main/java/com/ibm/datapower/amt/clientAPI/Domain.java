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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.AdminStatus;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.OpStatus;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.QuiesceStatus;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.ConfigObject;
import com.ibm.datapower.amt.amp.DeleteObjectResult;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.ReferencedObjectCollection;
import com.ibm.datapower.amt.clientAPI.util.Timeout;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.dataAPI.StoredVersion;
import com.ibm.datapower.amt.dataAPI.local.filesystem.StoredTagImpl;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.amt.soma.SOMACommands;
import com.ibm.datapower.amt.soma.SOMAException;
import com.ibm.datapower.amt.soma.SOMAIOException;
import com.ibm.datapower.amt.soma.Status;

/**
 * A Domain object corresponds to a domain configuration on a DataPower device. 
 * A domain is a configuration partition on a DataPower device, and is the most granular
 * configuration object that the manager uses. The constructor for <code>Domain</code> 
 * should not be used directly. Use the <code>createManagedDomain</code> method on 
 * {@link ManagedSet} or {@link Device} instead. A DataPower device can contain 
 * multiple domains. 
 * <p> 
 * A domain is {@link Versionable} - upon successful 
 * deployment the domain configuration is read back off of the device to create a 
 * {@link DomainVersion} object.
 * <p>
 * The manager has the ability to maintain domain synchronization on Managed Domains. For more 
 * information refer to {@link #setSynchronizationMode(DomainSynchronizationMode)}. 
 * Domain synchronization is disabled by default, to enable it refer to 
 * {@link #setSynchronizationMode(DomainSynchronizationMode)}
 * <p>
 * Domain quiesce happens automatically for devices with firmware 3.8.1 or later. To 
 * change the domain quiesce timeout change the {@link Configuration#KEY_DOMAIN_QUIESCE_TIMEOUT}
 * value in WAMT.properties, or call {@link Domain#setQuiesceTimeout(int)}  
 * <p>
 * The <code>default</code> domain contains some configuration that is unique to 
 * a specific DataPower device.  Ethernet interface configuration is an example. 
 * When managing the <code>default</code> domain for devices with firmware 3.8.1 or later,
 * a deployment policy should be used to filter out configuration that is unique 
 * to a specific device.  The following two filtered configuration rules should 
 * always be used in the deployment policy for the <code>default</code> domain:
 * <ul>
 * <code>*&#47default&#47network&#47interface?Name=.*</code>
 * <br><code>*&#47default&#47system&#47system?Name=.*</code>
 * </ul>
 * Additonal rules can be specified to filter out other configuration if needed.
 * For devices with firmwares prior to 3.8.1, device specific configuration is 
 * always excluded from the <code>default</code> domain configuration.
 * 
 * @see DomainVersion
 * @version SCM ID: $Id: Domain.java,v 1.18 2011/04/27 15:25:49 wjong Exp $
 */
public class Domain implements Persistable, Versionable, Taggable {
    private volatile StoredDomain persistence = null;
    /*
     * All the attributes above this line will be persisted to the datastore.
     */
    
    private boolean checkVersionSynch = false;
    
    private boolean synchDeployFailed = false;
    private boolean compareDomainFailed = false;
    
    // For services
    private Map<String, RuntimeService> serviceMap = null; // Key=primaryKey(objectName:objectClassName) value=Service object

    private int synchRetryCount = 0;
    private static int MAX_SYNCH_RETRY_COUNT;
    private static int DEFAULT_DOMAIN_QUIESCE_VALUE;
    protected final static int DOMAIN_QUIESCE_MIN_VALUE = 60; //seconds    
    
    DomainSynchronizationMode synchMode =  DomainSynchronizationMode.MANUAL;
    
    static final String DEFAULT_DOMAIN_NAME = "default"; //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    static final String SCM_REVISION = "$Revision: 1.18 $"; //$NON-NLS-1$
    
    protected static final String CLASS_NAME = Domain.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
        
        Integer integer1 = Configuration.getAsInteger(Configuration.KEY_DOMAIN_SYNCHRONIZATION_RETRY_MAX);
        MAX_SYNCH_RETRY_COUNT = integer1.intValue();

        Integer integer2 = Configuration.getAsInteger(Configuration.KEY_DOMAIN_QUIESCE_TIMEOUT);
        DEFAULT_DOMAIN_QUIESCE_VALUE = integer2.intValue();
    }

    /**
     * Create a new domain object within the context of a device and add it
     * to the manager. This should not be called directly. Call this method 
     * indirectly from the createManagedDomain in the ManagedSet or Device classes
     * 
     * @param domainName the name of the domain
     * @param device device that this domain will reside on

     * @throws AMPException
     * @throws AlreadyExistsInRepositoryException
     * @throws NotExistException
     * @throws InUseException
     * @throws LockBusyException
     * @throws DatastoreException
     * @throws DeletedException
     * @see Device#createManagedDomain()
     * @see Device#getManagedDomains()
     */
    Domain(String domainName, Device device)
        throws InUseException, NotExistException, AlreadyExistsInRepositoryException, 
               AMPException, LockBusyException, 
               DeletedException, DatastoreException {
        final String METHOD_NAME = "Domain(String, ManagedSet)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        // lock the device. domain operations happen on a per device basis
        device.lockNoWait();
        try {
            // get or construct the StoredDomain instance
            Manager manager = Manager.internalGetInstance();
            Repository repository = manager.getRepository();
            this.persistence = repository.createDomain(device.getStoredInstance(), 
                                                       domainName);
            
            // populate the rest of the non-persisted members

            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.getStoredInstance(), this);

            try {
				setQuiesceTimeout(DEFAULT_DOMAIN_QUIESCE_VALUE);
			} catch (InvalidParameterException e) {
				// we will have handled this scenario before now.
			}
            
            manager.save(Manager.SAVE_UNFORCED);

            //Also make the Deployment Policy persist
            new DeploymentPolicy(this);
            
            	           
        } finally {
	       	device.unlock();
	    }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Create a domain object from persistence 
     *  
     * @param storedDomain - the stored object from persistence
     * @throws DeletedException 
     * 
     */
    Domain(StoredDomain storedDomain) throws AMPException, DeletedException {
        final String METHOD_NAME = "Domain(StoredDomain)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, storedDomain);
        this.persistence = storedDomain;
        
        // set the rest of the non-persisted members: ManagementStatus and OperationStatus

        // can't call this.getManagedSet because it hasn't been added to the PersistenceMapper yet
        PersistenceMapper mapper = PersistenceMapper.getInstance();

        // add it to the mapper
        mapper.add(this.persistence, this);

        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Check to see if this domain is managed, and throw a NotManagedException if not 
     *  
     */
    protected void checkIfManaged() throws DeletedException, NotManagedException {
    	//Check to see if this domain is managed. Throw an UnmanagedException if not.
    	
    	Device device = getDevice();
    	ManagedSet ms = null;
    	
    	if (device != null) { 
    		ms = device.getManagedSet();
    	}
    	
    	if (ms == null) {
			String message = Messages.getString("wamt.clientAPI.Domain.notManagedException", getAbsoluteDisplayName());
			throw new NotManagedException(message,"wamt.clientAPI.Domain.notManagedException");
    	}
    }
    
    /**
     * Trim excess DomainVersions from peristence 
     *  
     */
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
     * Use this method to specify the domain configuration URLSource that should be deployed for this Domain. 
     * <p>
     * <ul>
     * <li>If the synchronization mode for this domain is <code>DomainSynchronizationMode.MANUAL</code> then this URLSource 
     *     will not be deployed until the ProgressContainer returned by {@link #deployConfiguration()} is invoked, or 
     *     if {@link #deploySourceConfigurationAction()} is invoked.</li>
     * <li>If the synchronization mode for this domain is <code>DomainSynchronizationMode.AUTO</code> is set then this method 
     *     will automatically trigger a deploy action.</li> 
     * </ul>
     * NOTE: If domain synchronization is enabled, and a deployment policy is to be deployed along 
     * with this source then the synchronization mode should be set to <code>DomainSynchronizationMode.MANUAL</code> 
     * prior to calling this method and set back to <code>DomainSynchronizationMode.AUTO</code> after calling   
     * {@link #setDeploymentPolicy(URLSource, String, String)}. The domain will be synchronized automatically 
     * after the synchronization mode is set to <code>DomainSynchronizationMode.AUTO</code>
     * 
     * @param sourceConfiguration is a URLSource that points to the configuration that should be 
     *        deployed to the Device for this domain. The domain configuration is not persisted in the 
     *        repository as a DomainVersion until the configuration is successfully deployed
     *        
     * @throws InUseException the device is in a ManagedSet. This method can be
     *         invoked only on a device that is not managed.
     * @throws InvalidParameterException the ProgressContainer is null
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws IOException an error occurred while getting the bytes from the
     *         Blob
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws NotExistException 
     * @throws InvalidParameterException 
     * @throws LockBusyException 
     * @throws DatastoreException 
     * @throws FullException 
     * @throws URISyntaxException 
     * @throws NotManagedException 
     * @throws UndeployableVersionException 
     * @see Domain#setSynchronizationMode(DomainSynchronizationMode)
     * @see Domain#setDeploymentPolicy(URLSource, String, String)
     */
    public void setSourceConfiguration(URLSource sourceConfiguration) throws DatastoreException, DeletedException, URISyntaxException, NotExistException, InvalidParameterException, FullException, NotManagedException  {
    	
    	checkIfManaged();
    	
    	Manager manager = Manager.internalGetInstance();
        StoredDomain domain = this.getStoredInstance();
        if (sourceConfiguration == null) { // reset the URLSource to the initial state (null)
        	setOutOfSynch(false);
        	Repository repository = manager.getRepository();    
            
            domain.setSourceURL(null);
            this.persistence = repository.updateDomain(domain);
            
            manager.save(Manager.SAVE_UNFORCED);
            return;
        } else {
        	if (domain.getSourceURL() == null){
        		//SourceURl has not been set so far. set flag to true
        		setOutOfSynch(true);
        	}
        	else{ 
        		if (!sourceConfiguration.getURL().equals(domain.getSourceURL().getURL())) {
        		   setOutOfSynch(true);
        		}
        	}
        }

        //If the URLSource is a domain version, then we automatically set up the 
    	//deployment policy version to the one with the same version number.
        if (getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)) {

	    	if (sourceConfiguration.hasVersionPath()) {
	    		checkVersionSynch = true;
	    		
	        	DeploymentPolicy deploymentPolicy = getDeploymentPolicy();
         		DeploymentPolicyVersion policyVer = sourceConfiguration.getDeploymentPolicyVersion();
         		
         		if (policyVer == null) {
        			String message = Messages.getString("wamt.clientAPI.Domain.verNotExist");
        			throw new NotExistException(message,"wamt.clientAPI.Domain.verNotExist");
         		}
	    		
	    		if (policyVer.getPolicyType() == DeploymentPolicyType.NONE) {
	    			deploymentPolicy.setPolicyNone();
	    		} else {
	    		    String policyDomain = policyVer.getPolicyDomainName(); 
	    		    String policyName   = policyVer.getPolicyName();
	    		    
	    		    String policyURL = sourceConfiguration.getURL().replaceFirst(URLSource.URI_PATH_DOMAINVER , URLSource.URI_PATH_DEPPOL_VER);
	    		    URLSource urls = new URLSource(policyURL);
	    		    
	    		    deploymentPolicy.setPolicyExport(urls, policyDomain, policyName);
	    		}
	    	}
        }
        
        
        Repository repository = manager.getRepository();    
                
        domain.setSourceURL(sourceConfiguration);
        this.persistence = repository.updateDomain(domain);

        manager.save(Manager.SAVE_UNFORCED);

        if (isSynchEnabled() && getOutOfSynch()) {
            //queue a background task to *synch* the domain - synch allows retry logic to be used
        	enqueueSynch(false);
        }
    }

    /**
     * Push a configuration onto the device that was previously set by calling 
     * {@link #setSourceConfiguration(URLSource)} and {@link #setDeploymentPolicy(URLSource, String, String)}  
     * <p>
     * This method returns a long-running task that is wrapped in a {@link ProgressContainer}. The 
     * {@link ProgressContainer} must be used to trigger the actual execution of this task. 
     * <p>
     * This operation will fail if the domain does not belong to a managed device.
     * <p>
     * Note: To use <code>EXPORT</code> configuration files for DataPower devices on a firmware prior to 4.0.1,
     *  the domain on the device must be created first via the DataPower WebGUI or CLI.  A domain does not need 
     *  to be created first on the device if using a entire domain <code>BACKUP</code> configuration file.
     * 
     * @return the ProgressContainer object for this long running task
     * @throws DeletedException
     * @throws FullException
     * @throws NotManagedException
     * 
     * @see #setSourceConfiguration(URLSource sourceConfiguration)
     *  
     */
    public ProgressContainer deployConfiguration() throws DeletedException, FullException, NotManagedException {
        final String METHOD_NAME = "deployConfiguration"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	checkIfManaged();
    	
    	ProgressContainer progressContainer = null;

    	// Set and deploy a firmware version.
        BackgroundTask backgroundTask = BackgroundTask.createDomainDeployConfigurationTask(this);
        progressContainer = backgroundTask.getProgressContainer();
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        Device device = this.getDevice();
        ManagedSet managedSet = null;
        if (device != null) {
            managedSet = device.getManagedSet();
        }
        if (managedSet == null) 
        {
            manager.enqueue(backgroundTask, manager);        	
        }
        else
        {
            manager.enqueue(backgroundTask, managedSet);        	
        }

        // Go ahead and add the domain name to allDomainNames collection on Device.  
        // If there is a failure to deploy the domain, the next heartbeat task will
        // remove it.
        this.getDevice().addDomainName(this.getName());      
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(progressContainer);
    }
    
    
    /**
     * Push a configuration onto the device that was previously set by calling 
     * {@link #setSourceConfiguration(URLSource)} and {@link #setDeploymentPolicy(URLSource, String, String)}  
     * <p>
     * This is a synchronous call to a long running task. The thread that calls this 
     * method will not return until the task has completed or encounters an error.
     * <p> 
     * This method is made available for direct execution, but note that {@link #deployConfiguration()} 
     * is the preferred method to deploy a domain because it uses a {@link ProgressContainer}. 
     * 
     * @throws InUseException the device is in a ManagedSet. This method can be
     *         invoked only on a device that is not managed.
     * @throws InvalidParameterException the ProgressContainer is null
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws IOException an error occurred while getting the bytes from the
     *         Blob
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws NotExistException 
     * @throws InvalidParameterException 
     * @throws LockBusyException 
     * @throws DatastoreException 
     * @throws FullException 
     * @throws URISyntaxException 
     * @throws UndeployableVersionException 
     * @throws UnsuccessfulOperationException 
     * @throws NotManagedException 
     */
    //* @see Domain#setDesiredVersion(Version)
    protected void deploySourceConfigurationAction() throws InUseException, IOException, AMPException, DeletedException, 
    		NotExistException, InvalidParameterException, LockBusyException, DatastoreException, FullException, 
    		URISyntaxException, UndeployableVersionException, UnsuccessfulOperationException, NotManagedException {
        final String METHOD_NAME = "deploySourceConfigurationAction"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});

    	checkIfManaged();
    	
        Device device = this.getDevice();

        //Send a signal that a domain is about to be deployed 
        boolean wasSuccessful = false;
        Signaler signal = new Signaler(device, null, this.getName());
        signal.sendStart();
        
    	device.lockNoWait();

    	byte[] bytes;
		DeploymentPolicy depPol = null;
		boolean isQuiesced = false;
		try {
			//If checkVersionSynch is true, check to ensure the depPol ver that matches the domain ver

			URLSource sourceConfiguration = getSourceConfiguration();

			if (checkVersionSynch) {
			    int versionNumber = sourceConfiguration.getVersion();
			    DeploymentPolicyVersion policyVer = sourceConfiguration.getDeploymentPolicyVersion();
			    DomainVersion           domainVer = sourceConfiguration.getDomainVersion();
				if (policyVer.getTimestamp().compareTo(domainVer.getTimestamp()) != 0) {
	    			String message = Messages.getString("wamt.clientAPI.Domain.tsMismatch");
	    			throw new UndeployableVersionException(message,"wamt.clientAPI.Domain.tsMismatch");
				}
	    	}
			
			// domain configuration
			Blob source = sourceConfiguration.getBlob(); 
			long sourceLastModified = sourceConfiguration.getLastModified();
			bytes = source.getByteArray();
            Commands commands;
			commands = this.getDevice().getCommands();
            DeviceContext deviceContext;
			deviceContext = device.getDeviceContext();
			
			// deployment policy
			long depPolLastModified = 0; 
			depPol = getDeploymentPolicy();

			if (getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)) {
				depPol.getPolicy();  // retrieve the deployment policy from the URLSource				

				URLSource url =	depPol.getPolicyURLSource();
				if (url!= null){	
					depPolLastModified =  url.getLastModified();
				}
			}
				
			//Quiesce the domain - if quiesce is supported and fails we will not attempt to setDomain
			//other stuff an exception, will of course stop this execution.

			if(isPresentOn(getDevice())){
				quiesce();
				isQuiesced = true;
			}			
			
		    // deploy domain configuration w/deployment policy
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "call setDomain on " + getDevice().getSymbolicName() + " with domain " + getName()); //$NON-NLS-1$
            commands.setDomain(deviceContext, getName(), bytes, depPol);

            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "set synch states to OK"); //$NON-NLS-1$
            setSyncFailed(false);
			setOutOfSynch(false);
            
            Date timeStamp = new Date();
            
            // create versions
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "create deployment policy version"); //$NON-NLS-1$
//			if (getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)) {
	            String policyComment = "Version generated from Domain.deployConfiguration()";            
	            new DeploymentPolicyVersion(depPol,timeStamp,policyComment);
//			}

            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "create domain version"); //$NON-NLS-1$
			String userComment = "Version generated from Domain.deployConfiguration()";            
			new DomainVersion(this, timeStamp, userComment, getConfiguration());
//            setDesiredVersion(newDomainVersion);
			
            //Persist the "lastModified" values for the deployed domain configuration and deployment policy
            setLastModifiedOfDeployedSource(sourceLastModified);
            
            getDeploymentPolicy().setLastModifiedOfDeployedSource(depPolLastModified);

            unquiesce(); // unquiesce and refresh domain 
            isQuiesced = false;
            wasSuccessful = true;
		} catch (IOException e) {			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			//e.printStackTrace();
			throw e;
		} catch (AMPException e) {			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} catch (DeletedException e) {			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			//e.printStackTrace();
			throw e;
		} catch (NotExistException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			//e.printStackTrace();
			throw e;
		} catch (InvalidParameterException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			//e.printStackTrace();
			throw e;
		} catch (LockBusyException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} catch (DatastoreException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} catch (UnsuccessfulOperationException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} finally {
			//Fix for #10200: [3663] Deploy configuration fails if done via local file or URL option, 
            //						yet still wipes the existing config
			if ( isQuiesced ){
				unquiesce(); // Need to unquiesce the domain here in case the exception is thrown
			}
			// cleanup
			if (depPol != null) depPol.setCachedBytes(null);
            bytes = null;            
            
            // release lock 
        	device.unlock();
        	
            signal.sendEnd(wasSuccessful);
		}
		Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Quiesce a domain
     * 
     * The quiece operation is used on firmware versions 3.8.1 or higher to stop a 
     * domain before updating it. 
     *        
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws UnsuccessfulOperationException the quiesce was not successful within the timeout
     *         period, or the device returned an unrecognized quiesce status
     * @throws DeletedException 
     * @throws NotExistException the domain is not present on the device
     *          
     */
    public void quiesce() throws DeletedException, UnsuccessfulOperationException, AMPException, 
    		NotExistException {
        DeviceContext dc = this.getDevice().getDeviceContext();
        quiesceAction(dc, false);
        
        refresh();
    }
    
    /**
     * Unquiesce a domain
     * 
     * The quiece operation is used on firmware versions 3.8.1 or higher to stop a 
     * domain before updating it. 
     *        
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws UnsuccessfulOperationException the quiesce was not successful within the timeout
     *         period, or the device returned an unrecognized quiesce status
     * @throws DeletedException 
     * @throws NotExistException the domain is not present on the device
     *          
     */
    public void unquiesce() throws UnsuccessfulOperationException, DeletedException, AMPException, 
    		NotExistException {
        DeviceContext dc = this.getDevice().getDeviceContext();
        quiesceAction(dc, true);
        
        refresh();
    }
    
    
    /**
     * Quiesce or unquiesce a domain (depends on the value of the boolean unquiesce)
     * 
     * The quiesce operation is used on firmware versions 3.8.1 or higher to stop a 
     * domain before updating it. 
     * @param dc the device context for the device that owns this domain
     * @param unquiesce a boolean that if false causes this method to quiesce when called, and 
     *        if true causes the method to unquiesce.
     *        
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws UnsuccessfulOperationException the quiesce was not successful within the timeout
     *         period, or the device returned an unrecognized quiesce status
     * @throws DeletedException 
     * @throws NotExistException The domain does not exist on the device
     *          
     */
    private void quiesceAction(DeviceContext dc, boolean unquiesce) throws AMPException, UnsuccessfulOperationException, 
    		DeletedException, NotExistException {
        final String METHOD_NAME = "quiesceAction"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
        
        String operation ="quiesce";
        Commands commands = this.getDevice().getCommands();
        boolean isPresent = false;
        
        try {
        	isPresent = isPresentOn(getDevice());
		} catch (DeletedException e1) {
			// Eat it, we'll bail out anyway
		}
        
        try {
        	//Quiesce only if the FW version is 3.8.1 or later
        	if (isPresent) {
				if (getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE)) {
		            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
		                "device meets minimum firmware requirement for quiesce:" + MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE); //$NON-NLS-1$
		
			    	String domainName = null; 
					try {
						domainName = getName();
					} catch (DeletedException e) {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
						//e.printStackTrace();
					}
		
					if (domainName != null) {
				    	if (unquiesce) {
				            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
				                    "unquiesce"); //$NON-NLS-1$
							commands.unquiesceDomain(dc, domainName);
				    	} else {
				            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
		                    		"quiesce"); //$NON-NLS-1$
							commands.quiesceDomain(dc, domainName, getQuiesceTimeout());
				    	}
				    	
			            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
	                    		"quiesce timeout is " + getQuiesceTimeout()); //$NON-NLS-1$
				    	waitForQuiesceActionComplete(commands, dc, domainName, unquiesce, getQuiesceTimeout());
					}
				} else {
					if (unquiesce){
						operation = "unquiesce";
					}
		            Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE, operation }; //$NON-NLS-1$
		        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
		            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
		            
		            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
				}
        	}else {
	            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
	                   "domain " + this.getName() + " is not present on the device: " + getDevice().getSymbolicName()); //$NON-NLS-1$
    			String message = Messages.getString("wamt.clientAPI.Domain.notExist",  getDevice().getSymbolicName());
    			throw new NotExistException(message, "wamt.clientAPI.Domain.notExist",  getDevice().getSymbolicName());
			}
		} catch (DeletedException e) {
			// This domain MUST have a device, so eat the deleted exception
		} catch (DeviceExecutionException e ) {
			// In case the AMP call failed
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    protected static void waitForQuiesceActionComplete(Commands commands, DeviceContext dc, String domainName, boolean unquiesce, int timeout) throws UnsuccessfulOperationException, DeletedException, AMPException {
        final String METHOD_NAME = "waitForQuiesceActionComplete"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, "Domain:" + domainName);

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
        		"quiesce timeout is " + timeout); //$NON-NLS-1$

        // If quiesceTimeout is zero, then don't wait for the quiesce/unquiesce command to finish 
    	if (timeout > 0) {

			//Wait for the quiesce to finish
			boolean quiesceCommandFinished = false;
			
			QuiesceStatus qs = null;
			
			//Kick off the timeout
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "create timeout object"); //$NON-NLS-1$
			Timeout timer = new Timeout();
			timer.setTimeout(timeout);
			new Thread(timer).start();
			
			do {
				try {
					qs = commands.getDomainStatus(dc, domainName).getQuiesceStatus();
				} catch (Exception e) {
					// failed to get domain status, wait for next try
					sleep(2000);
					continue;
				}
				
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
	                    "Quiesce Status:" + qs); //$NON-NLS-1$

		    	if (unquiesce) {
					if (qs.equals(QuiesceStatus.NORMAL))
						quiesceCommandFinished = true;
		    	} else {
					if (qs.equals(QuiesceStatus.QUIESCED))
						quiesceCommandFinished = true;
		    	}
			    
		    	if(!quiesceCommandFinished)
		    		sleep(2000);
		    	
				if (qs.equals(QuiesceStatus.ERROR) || qs.equals(QuiesceStatus.UNKNOWN)) {

					String message = Messages.getString("wamt.clientAPI.Domain.unexpectedQStatus");
		            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$
		            throw new UnsuccessfulOperationException(message,"wamt.clientAPI.Domain.unexpectedQStatus");
				}
			} while ((quiesceCommandFinished == false) && (timer.timeoutExpired() == false));
			
			// quiesce
			if (unquiesce == false && quiesceCommandFinished == false) {
				sleep(500);
				try {
					qs = commands.getDomainStatus(dc, domainName).getQuiesceStatus();
				} catch (Exception e) {
					// failed to get domain status
				}
				if (qs == null || !qs.equals(QuiesceStatus.QUIESCED)) {
					String message = Messages.getString("wamt.clientAPI.Domain.quiesceFailed", domainName); //$NON-NLS-1$
		            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
		            throw new UnsuccessfulOperationException(message,"wamt.clientAPI.Domain.quiesceFailed", domainName); //$NON-NLS-1$
				}
			}

			// unquiesce
			if (unquiesce == true && quiesceCommandFinished == false) {
				sleep(500);
				try {
					qs = commands.getDomainStatus(dc, domainName).getQuiesceStatus();
				} catch (Exception e) {
					// failed to get domain status
				}
				if (qs == null || !qs.equals(QuiesceStatus.NORMAL)) {
					String message = Messages.getString("wamt.clientAPI.Domain.unquiesceFailed", domainName); //$NON-NLS-1$
		            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
		            throw new UnsuccessfulOperationException(message,"wamt.clientAPI.Domain.unquiesceFailed", domainName); //$NON-NLS-1$
				}
			}
    	}

    	logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    private static void sleep(long time) {
        final String METHOD_NAME = "sleep"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// Auto-generated catch block - eat it
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "interrupted from sleep. shutting down?"); //$NON-NLS-1$
		}
    }

    /**
     * Get the domain from the device as a Blob. 
     * This is a synchronous call to a long running task. 
     * The thread that calls this method will not return until the task has completed 
     * or encounters an error. 
     * <p>
     * This method is for internal use only
     * 
     * @return a Blob of the domain as exported from the Device. The Blob is not
     *         persisted in the repository.
     * @throws InUseException the device is in a ManagedSet. This method can be
     *         invoked only on a device that is not managed.
     * @throws NotExistException the specified domain does not exist on the
     *         device.
     * @throws InvalidParameterException the ProgressContainer is null
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws NotManagedException 
     */
    private Blob getConfiguration() 
        throws InUseException, NotExistException, InvalidParameterException,
               AMPException, DeletedException, NotManagedException {
        final String METHOD_NAME = "getConfiguration"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});

    	checkIfManaged();
    	
        Device device = this.getDevice();
    	device.lockWait();
    	Blob blob = null;
		try {
	        Commands commands = this.getDevice().getCommands();
	        DeviceContext deviceContext = device.getDeviceContext();
	        byte[] bytes = commands.getDomain(deviceContext, getName());
	        blob = new Blob(bytes);
	        // get rid of one reference to "bytes"
	        bytes = null;
		} catch (AMPException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "AmpException thrown" ,e);                                                                                  
			//e.printStackTrace();
			throw e;
		} catch (DeletedException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "DeletedException thrown" ,e);                                                                                  
			//e.printStackTrace();
			throw e;
		} finally {
			device.unlock();
		}

        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(blob);
    }

    /**
     * Get the name of this Domain. This name should be human-consumable. The
     * name combined with the ManagedSet name is the primary key of this object
     * in the manager. The name is immutable, so there is no
     * <code>setName(String)</code> method.
     * 
     * @return the name of this domain
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public String getName() throws DeletedException {
        return(this.getStoredInstance().getName());
    }

    /**
     * Get the device that owns this Domain. The relationship between a <code>Domain</code>
     * and a <code>Device</code> is immutable, so there is no
     * <code>setDevice(Device)</code> method.
     */
    public Device getDevice() throws DeletedException {
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredDevice storedDevice = this.getStoredInstance().getDevice();
        Device device = null;
        if (storedDevice != null) {
            device = mapper.getVia(storedDevice);
        }
        return(device);
    }
    
    /**
     * Get the deployment policy owned by this Domain.
     * <p>
     * A {@link DeploymentPolicy} object is created as a member of the <code>Domain</code>
     * when the <code>Domain</code> is created. The {@link DeploymentPolicy} value may be changed
     * by calling {@link #setDeploymentPolicy(URLSource, String, String)} - there is no
     * <code>setDeploymentPolicy(DeploymentPolicy)</code> method.
     */
    public DeploymentPolicy getDeploymentPolicy() throws DeletedException {
        //return(this.deploymentPolicy);
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredDeploymentPolicy storedDeploymentPolicy = this.getStoredInstance().getDeploymentPolicy();
        
        DeploymentPolicy deploymentPolicy = null;
        if (storedDeploymentPolicy != null){
        	deploymentPolicy = mapper.getVia(storedDeploymentPolicy);
        }

        return deploymentPolicy ;    	
    }

    /**
     * Use this method to specify the deployment policy to apply to this domain during deployment. This 
     * deployment policy will NOT be available on the device after a successful deployment. 
     * If the domain source is set to a DomainVersion, the deployment policy is not required to be set. 
     * <ul>
     * <li>If the synchronization mode for this domain is <code>DomainSynchronizationMode.MANUAL</code> then this policy 
     *     will not be deployed until the ProgressContainer returned by {@link #deployConfiguration()} is invoked, or 
     *     if {@link #deploySourceConfigurationAction()} is invoked.</li>
     * <li>If the synchronization mode for this domain is <code>DomainSynchronizationMode.AUTO</code> is set then this method 
     *     will automatically trigger a deploy action.</li> 
     * </ul>
     * NOTE: If domain synchronization is enabled, and a domain configuration is to be deployed along 
     * with this policy then the synchronization mode should be set to <code>DomainSynchronizationMode.MANUAL</code> 
     * prior to calling this method and set back to <code>DomainSynchronizationMode.AUTO</code> after calling   
     * {@link #setDeploymentPolicy(URLSource, String, String)}. The domain will be synchronized automatically 
     * after the synchronization mode is set to <code>DomainSynchronizationMode.AUTO</code>
     * 
     * @param url - is a URLSource that points to a configuration source containing the policy to be used 
     *        during deployment. The deployment policy information is not persisted in the 
     *        repository until the policy is successfully deployed.  This parameter is required.
     * @param policyDomain - this is the first piece of information used to find the deployment policy within
     *        the configuration source specified by the URLSource.  This parameter is only required if the 
     *        source is a backup, since a backup may contain multiple domains. 
     * @param policyObjName - this is the second piece of information used to find the deployment policy within
     *        the configuration source specified by the URLSource.  This parameter is required.
     *        
     * @see #setSynchronizationMode(DomainSynchronizationMode)
     * @see #setSourceConfiguration(URLSource)
     */
    // Set a deployment policy for this domain.
    // url and policyObjName is required.
    // policyDomain is only required if the configuration is a backup.
    public void setDeploymentPolicy(URLSource url, String policyDomain, String policyObjName) throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException, FullException, MissingFeaturesInFirmwareException, NotManagedException {
        final String METHOD_NAME = "setDeploymentPolicy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        checkIfManaged();
    	
        DeploymentPolicy dp = this.getDeploymentPolicy();

		if (!getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)) {
			String message = Messages.getString("wamt.clientAPI.Domain.noDepPolForYou");
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$
			throw new MissingFeaturesInFirmwareException(message, "wamt.clientAPI.Domain.noDepPolForYou");
		}

        //Check to see if the deployment policy being set is different from the one 
        //already set
        boolean dpChanged = false;
        if (url != null) {
        	if (dp.getPolicyURLSource() == null){
        		dpChanged = true;
        	}
        	else{
        		if (!url.getURL().equals(dp.getPolicyURLSource().getURL())) {
        			dpChanged = true;
        		}
        	}
        } else {
        	if (dp.getPolicyURLSource() != null) {
        		dpChanged = true;
        	}
        }
        if (policyDomain != null) {
        	if (!policyDomain.equals(dp.getPolicyDomainName())) {
        		dpChanged = true;
        	}
        } else {
        	if (dp.getPolicyDomainName() != null) {
        		dpChanged = true;
        	}
        }
        if (policyObjName != null) {
        	if (!policyObjName.equals(dp.getPolicyObjectName())) {
        		dpChanged = true;
        	}
        } else {
        	if (dp.getPolicyObjectName() != null) {
        		dpChanged = true;
        	}
        }
        
        if (dpChanged) {
        	setOutOfSynch(true);
        }
        
        if ((url == null) && (policyDomain == null) && (policyObjName == null)) {
        	dp.setPolicyNone();        	
        } else {
            dp.setPolicyExport(url, policyDomain, policyObjName);
        }
        
        checkVersionSynch = false;
        
        if (isSynchEnabled() && getOutOfSynch()) {
            //queue a background task to *synch* the domain - synch allows retry logic to be used
        	enqueueSynch(false);
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Use this method to specify the deployment policy in XML format.  
     * Refer to {@link #setDeploymentPolicy(URLSource, String, String)} 
     * for synchronization behavior as related to deployment policy.
     * 
     * @param url - is a URLSource that points to a configuration source containing the policy to be used 
     *        during deployment. The deployment policy information is not persisted in the 
     *        repository until the policy is successfully deployed.  This parameter is required.
     *        
     * @see #setDeploymentPolicy(URLSource, String, String)
     */
    public void setDeploymentPolicyXML(URLSource url) throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException, FullException, MissingFeaturesInFirmwareException, NotManagedException {
        final String METHOD_NAME = "setDeploymentPolicy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        checkIfManaged();
    	
        DeploymentPolicy dp = this.getDeploymentPolicy();

		if (!getDevice().meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY)) {
			String message = Messages.getString("wamt.clientAPI.Domain.noDepPolForYou");
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$
			throw new MissingFeaturesInFirmwareException(message, "wamt.clientAPI.Domain.noDepPolForYou");
		}

        //Check to see if the deployment policy being set is different from the one 
        //already set
        boolean dpChanged = false;
        if (url != null) {
        	if (dp.getPolicyURLSource() == null){
        		dpChanged = true;
        	}
        	else{
        		if (!url.getURL().equals(dp.getPolicyURLSource().getURL())) {
        			dpChanged = true;
        		}
        	}
        } else {
        	if (dp.getPolicyURLSource() != null) {
        		dpChanged = true;
        	}
        }
        
        if (dpChanged) {
        	setOutOfSynch(true);
        }
        
        if (url == null) {
        	dp.setPolicyNone();        	
        } else {
            dp.setPolicyXML(url);
        }
        
        checkVersionSynch = false;
        
        if (isSynchEnabled() && getOutOfSynch()) {
            //queue a background task to *synch* the domain - synch allows retry logic to be used
        	enqueueSynch(false);
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * 
     * Set configuration synchronization mode on the Domain.
     * <br/>
     * The synchronization mode defaults to <code>DomainSynchronizationMode.MANUAL</code>. 
     * Valid synchronization modes are <code>DomainSynchronizationMode.MANUAL</code> 
     * and <code>DomainSynchronizationMode.AUTO</code>.
     *  <br/>
     * If the domain synchronization mode for a domain is set to <code>DomainSynchronizationMode.AUTO</code> 
     * The manager checks periodically to see if the domain configuration is out of synch with the domain on 
	 * a DataPower device device. 
	 * <p>
	 * The manager is designed to make the following checks:
	 * <ul>
	 * <li>The domain configuration differs from the domain on the device</li>
	 * <li>The timestamp of one of the source files (domain source or deployment policy source) has 
	 *  changed since the last deploy action</li>
	 * <li>For a source configuration from a device, a notification that the configuration has changed.  
	 *  Note that the source device must be managed (e.g. belong to a Managed Set) in order for the 
	 *  manager to receive the notification.</li> 
	 * <li>A "Set" method was called that changed the domain source or the effective deployment policy</li>
	 * <li>A prior synch/deploy operation had failed.</li>
	 * </ul> 
	 * <p>
	 * Conditions that cause the manager to invoke the synchronization logic:
	 * <ol>
	 * <li>Manager Start: When the manager is started it assumes that some managed domains 
	 *     may have been altered on managed devices while it was down. It calls synch(true) for 
	 *     each domain in order to do a diff on that domain (and redeploy if necessary).</li>
	 * <li>Notification: The device has the capability to notify the manager if a domain 
	 *     configuration is saved. A notification is specific to a particular device/domain
	 *     (or the default domain, in the event a domain was removed) so the notification
	 *     will eventually call synch(true) to verify the domain has changed, which will cause 
	 *     redeploy, which causes another notification that causes the manager to validate the 
	 *     domains are now identical.</li> 
	 * <li>Timer event: The timer fires periodically as specified by 
   	 *         <code>ConfigurationDEFAULT_DOMAIN_SYNCHRONIZATION_INTERVAL</code> unless overridden in 
   	 *    WAMT.properties with the value of DomainSynchronizationInterval
	 *    The timer event calls synch(false) because it does not want to always compare deployed
	 *    domains to the source configuration, because the manager should be notified of such changes.</li>
	 * <li>"Set" method changes: If the synchronization mode is DomainSynchronizationMode.AUTO
	 *    then a synch(false) task will be kicked off when setSourceConfuguration(url) or 
	 *    setDeploymentPolicy(url, domain, policy) are called with changes to the Domain configuration. 
	 *    If they are called with identical parameters then the synch() method will not be called</li>  
	 * <li>Enabling synch: If the synchronization mode is set from DomainSynchronizationMode.MANUAL 
	 *    to DomainSynchronizationMode.AUTO then a synch(true) task will be called for the domain.</li>
	 * </ol>
     *  
     * @param synchMode the synchronization mode this device should adhere to
     * @throws FullException
     * @throws DirtySaveException
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem reading or writing to/from
     *         the persistence repository.
     * @throws NotManagedException 
     * @see DomainSynchronizationMode
     */
    public void setSynchronizationMode(DomainSynchronizationMode synchMode) throws DeletedException, DirtySaveException, DatastoreException, FullException, NotManagedException{
        final String METHOD_NAME = "setSynchronizationMode"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // we make sure synchRetryCount is set to 0 (in case we are re-trying after reaching MAX_SYNCH_RETRY_COUNT)
        synchRetryCount = 0;
    	checkIfManaged();
    	
        boolean synchImmediately = false;
        
        this.synchMode = synchMode;
        StoredDomain domain = this.getStoredInstance();
        
        if ((synchMode.equals(DomainSynchronizationMode.AUTO)) && (!domain.getSynchMode().equals(synchMode))) {
        	synchImmediately = true;
        }
        
        domain.setSynchMode(synchMode);
        Manager manager = Manager.internalGetInstance();
        Repository repository = manager.getRepository();
        this.persistence = repository.updateDomain(domain);
        manager.save(Manager.SAVE_UNFORCED);
        
        if (synchImmediately && getOutOfSynch()) {
            //queue a background task to *synch* the domain - synch allows retry logic to be used
        	enqueueSynch(true);
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
    }
    
    /**
     * Trim the specified DomainVersion from the repository. The associated  
     * {@link DeploymentPolicyVersion} is also deleted.  This could be
     * user-initiated or invoked by an agent. It will remove the specified
     * DomainVersion, but will not delete the Domain object.  
     * 
     * @param version the version to remove from the repository
     * @throws InvalidParameterException the Version parameter is not a
     *         DomainVersion.
     * @throws NotExistException the specified version is not a member of this
     *         Domain.
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
    public void remove(Version version) throws InvalidParameterException, 
    NotExistException, DeletedException, LockBusyException, DatastoreException {
        final String METHOD_NAME = "remove"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, version);
        if (!(version instanceof DomainVersion)) {
            String message = Messages.getString("wamt.clientAPI.Domain.notDomainVersion", version.getClass().getName()); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Domain.notDomainVersion", version.getClass().getName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        Device device = this.getDevice();
        try {
            // lock the device
        	device.lockNoWait();

        	//Remove the corresponding deployment policy version too.
        	int versionNumber = version.getVersionNumber();
        	DeploymentPolicy dp = getDeploymentPolicy();
        	DeploymentPolicyVersion dpv = dp.getVersion(versionNumber);
        	if (dpv != null){        	   
        	   dp.remove(dpv);        	   
        	}else{
        		// may have been removed by trimExcesses()		
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "Policy Version already deleted, version: " + versionNumber); //$NON-NLS-1$
        	}
        	
            DomainVersion domainVersion = (DomainVersion) version;
            domainVersion.destroy();
            Manager manager = Manager.internalGetInstance();
            manager.save(Manager.SAVE_UNFORCED);
        } finally {
            device.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    boolean isPresentOn(Device device) throws DeletedException, AMPException {
        final String METHOD_NAME = "isPresentOn"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, device});
        // use AMP to test the Domain version
        boolean result = false;
        
        Commands commands = this.getDevice().getCommands();
        DeviceContext deviceContext = device.getDeviceContext();
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "invoking getDomainList"); //$NON-NLS-1$
        String[] existingDomainNames = commands.getDomainList(deviceContext);

        String flattenedList = ""; //$NON-NLS-1$
        String thisDomainName = this.getName();
        StringBuffer buf = new StringBuffer(flattenedList);
        for (int i=0; i<existingDomainNames.length; i++) {
            if (existingDomainNames[i].equals(thisDomainName)) {
                result = true;
            }
            if (buf.toString().length() > 0) {
                buf.append(", "); // flattenedList += ", "; //$NON-NLS-1$
            }
            buf.append(existingDomainNames[i]); //flattenedList += existingDomainNames[i];
        }
        flattenedList = buf.toString();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, flattenedList);
        logger.exiting(CLASS_NAME, METHOD_NAME, Boolean.valueOf(result));
        return(result);
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Retrieve a reference to the Domain that has the specified primary key.
     * 
     * @param targetKey the primary key to search for
     * @return the Domain that has the specified primary key. May return
     *         <code>null</code> if no Domain with the specified primary key
     *         was found.
     * @see #getPrimaryKey()
     */
    public static Domain getByPrimaryKey(String targetKey) {
        Domain result = null;
        Manager manager = Manager.internalGetInstance();
        Device[] devices = manager.getAllDevices();
    outermost: for (int deviceIndex=0; deviceIndex < devices.length; deviceIndex++) {
        Domain[] domains = null;
        try {
            domains = devices[deviceIndex].getManagedDomains();
        } catch (DeletedException e) {
            domains = new Domain[0];
        }
        for (int domainIndex=0; domainIndex<domains.length; domainIndex++) {
            String key = null;
            try {
                key = domains[domainIndex].getPrimaryKey();
            } catch (DeletedException e1) {
                key = ""; //$NON-NLS-1$
            }
            if (key.equals(targetKey)) {
                result = domains[domainIndex];
                break outermost;
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
    StoredDomain getStoredInstance() throws DeletedException {
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
        Device device = this.getDevice();

        try {
        	device.lockNoWait();
        	
        	//delete the child DeploymentPolicy object
        	// getDeploymentPolicy().destroy();

            // delete the child DomainVersion objects
//            this.unsetDesiredVersion();
            Version[] versions = this.getVersions();
            for (int i=0; i<versions.length; i++) {
                DomainVersion domainVersion = (DomainVersion) versions[i];
                try {
                    this.remove(domainVersion);
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
            
            getDeploymentPolicy().destroy();            
            // delete from persistence
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting from persistence"); //$NON-NLS-1$
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.remove(this.getStoredInstance());
            this.getStoredInstance().delete();
            this.persistence = null;
            
            // clear any references
        } finally {
            device.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /* javadoc inherited from interface */
    public Version[] getVersions() throws DeletedException {
        final String METHOD_NAME = "getVersions"; //$NON-NLS-1$
        DomainVersion[] result = null;
        StoredVersion[] storedDomainVersions = this.getStoredInstance().getVersions();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        Vector domainVersions = new Vector();
        for (int i=0; i<storedDomainVersions.length; i++) {
            StoredDomainVersion storedDomainVersion = (StoredDomainVersion) storedDomainVersions[i];
            DomainVersion domainVersion = null;
            try {
                domainVersion = mapper.getVia(storedDomainVersion);
            } catch (DeletedException e) {
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                            Messages.getString("VersionDeleted", storedDomainVersion), e); //$NON-NLS-1$
                domainVersion = null;
            }
            if (domainVersion != null) {
                domainVersions.add(domainVersion);
            }
        }
        result = new DomainVersion[domainVersions.size()];
        for (int i=0; i<domainVersions.size(); i++) {
            result[i] = (DomainVersion) domainVersions.get(i);
        }
        return(result);
    }

    /* javadoc inherited from interface */
    public Version getVersion(int targetVersionNumber) throws DeletedException {
        DomainVersion result = null;
        StoredDomainVersion matchingStoredDomainVersion = null;
        StoredVersion[] storedVersions = this.getStoredInstance().getVersions();
        for (int i=0; i<storedVersions.length; i++) {
            if (storedVersions[i].getVersionNumber() == targetVersionNumber) {
                matchingStoredDomainVersion = (StoredDomainVersion) storedVersions[i];
                break;
            }
        }
        if (matchingStoredDomainVersion != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(matchingStoredDomainVersion);
        }
        return(result);
    }
    
    /**
     * Get the highest version number of all the DomainVersions in this Domain.
     * 
     * @return the highest version number of all the DomainVersions.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public int getHighestVersionNumber() throws DeletedException {
        StoredDomain storedDomain = this.getStoredInstance();
        int result = storedDomain.getHighestVersionNumber();
        return(result);
    }
    
//    /* javadoc inherited from interface */
//    public Version getDesiredVersion() throws DeletedException {
//        DomainVersion result = null;
//        StoredVersion storedVersion = this.getStoredInstance().getDesiredVersion();
//        StoredDomainVersion storedDomainVersion = (StoredDomainVersion) storedVersion;
//        PersistenceMapper mapper = PersistenceMapper.getInstance();
//        result = mapper.getVia(storedDomainVersion);
//        return(result);
//    }
//
//    /* javadoc inherited from interface */
//    public ProgressContainer setDesiredVersion(Version version) 
//        throws DatastoreException, InvalidParameterException, DeletedException, LockBusyException, FullException {
//        final String METHOD_NAME = "setDesiredVersion"; //$NON-NLS-1$
//        logger.entering(CLASS_NAME, METHOD_NAME, version);
//        if (!(version instanceof DomainVersion)) {
//        	String message = Messages.getString("wamt.clientAPI.Domain.notDomainVer", version.getClass().getName());
//            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Domain.notDomainVer", version.getClass().getName()); //$NON-NLS-1$
//            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
//            throw(e);
//        }
//        Manager manager = Manager.internalGetInstance();
//        ProgressContainer progressContainer = null;
//        Device device = this.getDevice();
//        try {
//            device.lockNoWait();
//            DomainVersion domainVersion = (DomainVersion) version;
//            this.setDesiredVersionNoDeploy(domainVersion);
//            manager.save(Manager.SAVE_UNFORCED);
//        } finally {
//        	device.unlock();
//        }
//        
//        logger.exiting(CLASS_NAME, METHOD_NAME);
//        return(progressContainer);
//    }
    
//    void setDesiredVersionNoDeploy(DomainVersion domainVersion) throws DeletedException, DatastoreException {
//        StoredDomainVersion storedDomainVersion = domainVersion.getStoredInstance();
//        this.getStoredInstance().setDesiredVersion(storedDomainVersion);
//    }
//    
//    void unsetDesiredVersion() throws DeletedException, DatastoreException {
//        final String METHOD_NAME = "unsetDesiredVersion"; //$NON-NLS-1$
//        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
//                    "unsetting desiredVersion for " + this); //$NON-NLS-1$
//        this.getStoredInstance().setDesiredVersion(null);
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
     * used in user interfaces. 
     * 
     * @return a human-readable name that represents this object. For example,
     *         "domain1".
     */
    public String getRelativeDisplayName() {
        String result = null;
        try {
            result = this.getName();
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
     *         "Domain:testDomain1 on Device:dp12.dp.rtp.raleigh.ibm.com/9.42.102.72 in Managed Set:set1".
     */
    public String getAbsoluteDisplayName() {
        String result = null;
        String domainName = this.getRelativeDisplayName();
        String deviceName = null;        
        String msName = null;
        try {
            Device device = this.getDevice();
            deviceName = device.getDisplayName();
            
            ManagedSet ms = device.getManagedSet();
            if (ms == null) {
            	msName = "[unmanaged device]";
            } else {
                msName = device.getManagedSet().getDisplayName();
            }
        } catch (DeletedException e) {
        	deviceName = "[deleted]"; //$NON-NLS-1$
        }

        result = "Domain:" + domainName + " on Device:" + deviceName;
        if (msName!=null){
        	result = result + " in Managed Set:" + msName; //$NON-NLS-1$
        }
        return(result);
    }

	/** 
	 * This method can be used to retrieve the URLSource specified by {@link #setSourceConfiguration(URLSource)}
	 * 
	 * @return URLSource
	 * @throws DeletedException
	 */
    public URLSource getSourceConfiguration() throws DeletedException {
		return this.getStoredInstance().getSourceURL();
	}
	
	/**
	 * This method can be used to determine the current synchronization mode for this Domain
	 * 
	 * @return the domain synchronization mode for this Domain object
	 * @throws DeletedException
	 */
    public DomainSynchronizationMode getSynchronizationMode() throws DeletedException {
		return this.getStoredInstance().getSynchMode();
	}	
	
	//Get the "last modified" timestamp for the deployed domain - this is the timestamp of the 
	//source file that was deployed. this only applies if the source was file/http/https URL scheme.
	private long getLastModifiedOfDeployedSource() throws DeletedException {
		return this.getStoredInstance().getLastModifiedOfDeployedSource();
	}
	
	private void setLastModifiedOfDeployedSource(long milli) throws DeletedException {
		this.getStoredInstance().setLastModifiedOfDeployedSource(milli);
	}
	
	//Check to see if a "set" method was called that caused us to get out of synch
	private boolean getOutOfSynch() throws DeletedException {

		boolean retval = false;
		
		try {
			getDevice().lockWait();
			retval = this.getStoredInstance().getOutOfSynch();
		} finally {
			getDevice().unlock();
		}
		
		return retval;
	}
	
	//Set the status of the "set" method related domain synch.
	//   This is set to true only if a set method determines that a "desired" domain configuration has changed. 
	//   This is set to false when the domain is re-synched making the device domain the same as the deployed domain
	protected void setOutOfSynch(boolean value) throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException {
		
        final String METHOD_NAME = "setOutOfSynch"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        try {
			getDevice().lockWait();
	        StoredDomain domain = this.getStoredInstance();
	        domain.setOutOfSynch(value);
	        Manager manager = Manager.internalGetInstance();
	        Repository repository = manager.getRepository();
	        this.persistence = repository.updateDomain(domain);
	        manager.save(Manager.SAVE_UNFORCED);
		} finally {
			getDevice().unlock();
		}
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
        		"Domain save setOutOfSynch: " + value,this); 
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
	}
	
	/**
	 * This method checks to see if a domain configuration is out of synch with the domain on 
	 * a DataPower device device. This method is responsible for checking the synchronization mode to 
	 * determine if it should proceed with any check or deploy operations. This simplifies the 
	 * logic in any of the various callers, as explained below. See 
	 * {@link #setSynchronizationMode(DomainSynchronizationMode)} to learn how to enable domain 
	 * synchronization.
	 * <p>
	 * This method is for internal use only and should only be used 
	 * in the scenarios outlined below.    
 	 * <p>
	 * The compareDomain parameter controls whether the logic is invoked to compare the 
	 * domain source to the domain on the device, and redeploy if a difference is detected.
	 * <p> 
	 * It is important to note that domain synch operations are locked at a device level 
	 * (e.g. multiple domains cannot be simultaneously synched/delpoyed to the same device). 
	 * Since devices will typically have multiple domains it is very likely that the QueueProcessor 
	 * will get a LockBusyExceptions when it tries to perform a large number of domain synch 
	 * operations (it has multiple worker threads). The way the queue processor handles this is to 
	 * attempt to order the queue in a way to avoid LockBusyExceptions (e.g. enqueue domains across 
	 * devices, instead of all the domains on a device, then the next, etc). If the QueueProcessor 
	 * gets a LockBusyException when starting a task from the queue, it delays 1 second 
	 * and re-queues the task, also giving the effect of reordering the queue  
	 * <p>
	 * This method is written to make 4 types of checks
	 * <ul>
	 * <li>The domain configuration differs from the domain on the device</li>
	 * <li>The timestamp of one of the source files (domain source or deployment policy source) has 
	 *  changed since the last deploy action</li>
	 * <li>A "Set" method was called that changed the domain source or the effective deployment policy</li>
	 * <li>A prior synch/deploy operation had failed.</li>
	 * </ul> 
	 * <p>
	 * Conditions that cause synch(compareDomain) to be called:
	 * <ol>
	 * <li>Manager Start: When the manager is started it assumes that some managed domains 
	 *     may have been altered on managed devices while it was down. It calls synch(true) for 
	 *     each domain in order to do a diff on that domain (and redeploy if necessary).</li>
	 * <li>Notification: The device has the capability to notify the manager if a domain 
	 *     configuration is saved. A notification is specific to a particular device/domain
	 *     (or the default domain, in the event a domain was removed) so the notification
	 *     will eventually call synch(true) to verify the domain has changed, which will cause 
	 *     redeploy, which causes another notification that causes the manager to validate the 
	 *     domains are now identical.</li> 
	 * <li>Timer event: The timer fires periodically as specified by 
   	 *         ConfigurationDEFAULT_DOMAIN_SYNCHRONIZATION_INTERVAL unless overridden in 
   	 *    WAMT.properties with the value of DomainSynchronizationInterval
	 *    The timer event calls synch(false) because it does not want to always compare deployed
	 *    domains to the source configuration, because the manager should be notified of such changes.</li>
	 * <li>"Set" method changes: If the synchronization mode is DomainSynchronizationMode.AUTO
	 *    then a synch(false) task will be kicked off when setSourceConfuguration(url) or 
	 *    setDeploymentPolicy(url, domain, policy) are called with changes to the Domain configuration. 
	 *    If they are called with identical parameters then the synch() method will not be called</li>  
	 * <li>Enabling synch: If the synchronization mode is set from DomainSynchronizationMode.MANUAL 
	 *    to DomainSynchronizationMode.AUTO then a synch(true) task will be called for the domain.</li>
	 * </ol>
     * @param compareDomain - a boolean, if set to true will cause the synch method to compare the 
     *                        domain configuration with the domain on the device 
	 * 
	 */
	protected boolean synch(boolean compareDomain) throws LockBusyException, DeletedException, NotManagedException, UndeployableVersionException {
		final String METHOD_NAME = "synch(boolean)";
        logger.entering(CLASS_NAME, METHOD_NAME);
		
        checkIfManaged();
        
	    boolean doDeployAction = false;
		
		if (isSynchEnabled() &&!deviceRebootUnderway() && !firmwareUpdateUnderway()) {
			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "synch is enabled");

			//lock the device while we try to see if it needs to be synch'ed
			getDevice().lockNoWait();
			
			//Should we check to see if the domain on the device is different from the 
			//one we expect with the URLSource we have persisted?
			if (compareDomain || compareDomainFailed) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "compareDomain:" + compareDomain + " compareDomainFailed:" + compareDomainFailed);

				try {
					if (isDeployedDomainDifferent()) {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "domains are different, deploy");
						doDeployAction = true;
						//System.out.println("compareDomain||compareDomainFailed " + compareDomain+","+compareDomainFailed);
					}
					compareDomainFailed = false;
				} catch (Exception e) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Failed to compare deployed domain", e);
					compareDomainFailed = true;
					//System.out.println("compareDomainFailed " + e);					
				}
			}
			
			if (isSyncFailed()) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, " isSyncFailed:true");
				doDeployAction = true;
				//System.out.println("SynchdeployFailed");
			}
			
			if (getOutOfSynch() == true) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, " getOutOfSynch:true");
				doDeployAction = true;
				//System.out.println("getOutOfSynch is true");				
			}
		
			try {
			
				//Don't bother doing the costly process of checking time stamps if we already have to deploy
				if (doDeployAction != true) {
					try {
						
						if (isDomainSourceTimestampDifferent() || isDeploymentPolicySourceTimestampDifferent()) {
							logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, " time stamps are different");
							doDeployAction = true;
							//System.out.println("isDomainSourceTimestampDifferent or isDeploymentPolicySourceTimestampDifferent is true" +isDomainSourceTimestampDifferent()+"," +isDeploymentPolicySourceTimestampDifferent());	
						}
					} catch (IOException e) {
						synchRetryCount++;
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "IOException thrown" ,e);
						//e.printStackTrace();
					} catch (DeletedException e) {
						//this should never happen, since synch cannot not be called if this object is deleted.
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "DeletedException thrown" ,e);
						//e.printStackTrace();
					}
				}
				
				try {
					if (doDeployAction == true) {
				        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
				        		" Start configuration deployment"); 						
						deploySourceConfigurationAction();
				        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
		        		        " Completed configuration deployment"); 						
						synchRetryCount = 0;
					}
				} catch (LockBusyException e) {
					//The device is busy getting an update, so don't update the retry
					//count in this case, try after it is no longer busy.
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, " device busy, try again later");
					
					if (compareDomain || compareDomainFailed) {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, " compareDomain --> setSynchFailed because of busy device");
						setSyncFailed(true);
						//System.out.println("Lock Busy Exception in Domain");						
					}
				} catch (Exception e) {
					// Something bad happened and we weren't able to synch - we will retry
					// on the next timer hit.
					setSyncFailed(true);
					synchRetryCount++;
					logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Exception thrown in deploy", e);
				}						
				
				if (synchRetryCount >= MAX_SYNCH_RETRY_COUNT) {
					try {
						setSynchronizationMode(DomainSynchronizationMode.MANUAL);
						throw new UndeployableVersionException();
					} catch (UndeployableVersionException e) {
						// Log message and throw exception
						logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "MAX_SYNCH_RETRY_COUNT reached, UndeployableVersionException and DomainSynchronizationMode switched to MANUAL", e);
						throw(e);
					} catch (Exception e) {
						// Swallow other exceptions and we'll try again next time
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Exception thrown disabling domain synchronization", e);
						
					}
				}
			} finally {
				getDevice().unlock();
			}
		}
		
        logger.exiting(CLASS_NAME, METHOD_NAME); 
        return doDeployAction;
	} 
	
	private boolean deviceRebootUnderway() throws DeletedException {
		final String METHOD_NAME = "deviceRebootUnderway()";
        logger.entering(CLASS_NAME, METHOD_NAME);
		
		ManagementStatus deviceManagementStatus = null;
		Device device = getDevice();
		boolean RebootInProgress = false;

        // if the device is in the middle of reboot, it may not respond to AMP requests.
        // So skip the synch processing, and try again at the next timer pop.
		deviceManagementStatus = device.getManagementStatusOfDevice();
        if ((deviceManagementStatus != null) && 
                (deviceManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {
            String message = "The device " + device.getDisplayName() +  //$NON-NLS-1$
            	" is in progress of being rebooted. Will skip this synch check."; //$NON-NLS-1$
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
            RebootInProgress = true;
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, "Device reboot inProgress=" + RebootInProgress);
        return RebootInProgress;
	}
	
	private boolean firmwareUpdateUnderway() throws DeletedException {
		final String METHOD_NAME = "firmwareUpdateUnderway()";
        logger.entering(CLASS_NAME, METHOD_NAME);

		ManagementStatus firmwareManagementStatus = null;
		Device device = getDevice();
		boolean updateInProgress = false;

        // if the device is in the middle of a firmware upgrade, it may not respond to AMP requests.
        // So skip the synch processing, and try again at the next timer pop.
        firmwareManagementStatus = device.getManagementStatusOfFirmware();
        if ((firmwareManagementStatus != null) && 
                (firmwareManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {
            String message = "The firmware for device " + device.getDisplayName() +  //$NON-NLS-1$
            	" is in progress of being updated. Will skip this synch check."; //$NON-NLS-1$
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
            updateInProgress = true;
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, "Firmware update inProgress=" + updateInProgress);
        return updateInProgress;
	}
	
    /**
     * Put the synch on the background queue
     * 
     * @throws FullException 
     * @throws DeletedException
     * @throws FullException
     *  
     */
    private ProgressContainer enqueueSynch(boolean compareMode) throws FullException {
        final String METHOD_NAME = "deployConfiguration"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	ProgressContainer progressContainer = null;

    	// Set and deploy a firmware version.
        BackgroundTask backgroundTask = BackgroundTask.createDomainSynchronizationTask(this, compareMode);
        progressContainer = backgroundTask.getProgressContainer();
        Manager manager = Manager.internalGetInstance();
		try {
	        // Enqueue background task
	        
	        Device device;
				device = this.getDevice();
	        ManagedSet managedSet = null;
	        if (device != null) {
	            managedSet = device.getManagedSet();
	        }
	        //don't enqueue task if manager has started shutting down
	        //task will be discarded and the synch operatioin will be completed when the
	        //manager comes up the next time.
	        //tasks that have already started will be completed.
	        if (!manager.getShutdownStatus()){ 	        
	        	if (managedSet == null) 
	        	{
	        		manager.enqueue(backgroundTask, manager);        	
	        	}
	        	else
	        	{
	        		manager.enqueue(backgroundTask, managedSet);        	
	        	}
	        }
	        
		} catch (DeletedException e) {
			// We should never see this
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeletdException thrown enqueueing synch task", e);
			
		} catch (FullException e) {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "FullException thrown enqueueing synch task", e);
			throw e;
		}

        logger.exiting(CLASS_NAME, METHOD_NAME);
        manager.addDomainSyncProgress(progressContainer);
        return(progressContainer);
        
    }

    private boolean isDeployedDomainDifferent() throws DeletedException, AMPException, IOException {
		final String METHOD_NAME = "isDeployedDomainDifferent";
        logger.entering(CLASS_NAME, METHOD_NAME);
        boolean retVal = false;
		int highest = this.getHighestVersionNumber();
		DomainVersion version = (DomainVersion) this.getVersion(highest);
		if (version!= null){
			Blob savedBlob = version.getBlob();
			Commands commands;
			commands = this.getDevice().getCommands();
			DeviceContext deviceContext;
			deviceContext = this.getDevice().getDeviceContext();
			retVal = commands.isDomainDifferent(this.getName(), savedBlob.getByteArray(), null, deviceContext);
		}
		logger.exiting(CLASS_NAME, METHOD_NAME, "returning:" + retVal);
		return retVal;
	}

	// LS Note: If a domain configuration has never been deployed, it will be deployed when the SourceURl date is
	// compared with the last deployed date which will be 0.
	private boolean isDomainSourceTimestampDifferent() throws DeletedException, IOException {
		final String METHOD_NAME = "isDomainSourceTimestampDifferent";
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        boolean domainOutOfSynch = false;
        
		URLSource sourceConfiguration = getSourceConfiguration();

		long domainLastModified = getLastModifiedOfDeployedSource();
		long domainSourceLastModified = 0;
		if( sourceConfiguration != null){
		   domainSourceLastModified = sourceConfiguration.getLastModified();
		}   
		
		if ((domainSourceLastModified == 0) && (domainLastModified > 0)) {
			//This may have happened because the source file cannot be found
			//System.out.println("Source file not found:" + sourceConfiguration.getURL());
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
	        		"Source file not found:" + sourceConfiguration.getURL()); 							
		} else {
			if (domainLastModified != domainSourceLastModified) { //Condition could be true if 0!=lastModified
				domainOutOfSynch = true;
				//System.out.println("!!!!Domain is out of Synch " + domainLastModified + "," +domainSourceLastModified);
		        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
		        		"Domain is not Synchronized: Domain last modified: "   + domainLastModified + ",Domain Source last modified: " +domainSourceLastModified); 							
			}
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, "returning:" + domainOutOfSynch);
		return domainOutOfSynch;
	}

	private boolean isDeploymentPolicySourceTimestampDifferent() throws DeletedException, IOException {
		final String METHOD_NAME = "isDeploymentPolicySourceTimestampDifferent";
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        boolean deploymentPolicyOutOfSynch = false;

        long depPolLastModified = getDeploymentPolicy().getLastModifiedOfDeployedSource();
		long depPolSourceLastModified = 0;
		URLSource policyURL = getDeploymentPolicy().getPolicyURLSource();
		if (policyURL!=null){
			depPolSourceLastModified = getDeploymentPolicy().getPolicyURLSource().getLastModified();
		}

		if ((depPolSourceLastModified == 0) && (depPolLastModified > 0)) {
			//This may have happened because the source file cannot be found
			//System.out.println("Source file not found:" + sourceConfiguration.getURL());
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
	        		"Source file not found, policy URL" + policyURL); 							
			
		} else {
			if (depPolLastModified != depPolSourceLastModified) {// Condition could be true if 0!=lastModified
				deploymentPolicyOutOfSynch = true;
			}
		}
		logger.exiting(CLASS_NAME, METHOD_NAME, "returning:" + deploymentPolicyOutOfSynch);
		return deploymentPolicyOutOfSynch;
		
	}
	
	protected boolean isSynchEnabled() {
		final String METHOD_NAME = "isDeploymentPolicySourceTimestampDifferent";
        logger.entering(CLASS_NAME, METHOD_NAME);

        boolean synchEnabled = true;
		
		//Check to see if synchronization is enabled for this domain
		try {
			if (!getSynchronizationMode().equals(DomainSynchronizationMode.AUTO)) {
				synchEnabled = false;
			}
		} catch (DeletedException e) {
			synchEnabled = false;
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "DeletedException thrown" ,e);
			//e.printStackTrace();
		}
		
	    logger.exiting(CLASS_NAME, METHOD_NAME, "returning:" + synchEnabled);
		return synchEnabled;
	}
	
	private boolean isSyncFailed() {
		return this.synchDeployFailed;
	}

	/**
	 * Internal use only
	 * 
	 * @param syncFailed
	 */
	protected void setSyncFailed(boolean syncFailed) {
		this.synchDeployFailed = syncFailed;
	} 
	
    /**
     * Get the persisted timeout value when Domain is quiesced.
     *   
     * @return timeout value in seconds
     */    
	public int getQuiesceTimeout() {
        int retval = 0;
        
        try {
        	retval = this.getStoredInstance().getQuiesceTimeout();
		} catch (DeletedException e) {
			// Eat it
		}
        
 		return retval;
	}
	
    /**
     * Set the timeout value (in seconds) for checking the status of a domain 
     * quiesce or unquiesce operation.
     * <p> 
     * Quiesce only pertains to Firmware levels 3.8.1.0 or later. Earlier levels
     * of firmware do not support quiesce so calling this method has no effect. 
     * Note: An exception will not be thrown if you call this method for a domain 
     * on a device that has a firmware level below 3.8.1.0 - so this value will be 
     * available if firmware is ever upgraded. 
     * <p>
     * If a value of zero is set then the quiesce operation will be initiated on supported 
     * firmware, but the quiesce or unquiesce status will not be checked. If a nonzero value 
     * less than 60 is set, then the value will automatically be set to a minimum of 60 seconds. 
     * Values higher than 60 are OK.    
     *   
     * @param timeout value in seconds
     * @throws InvalidParameterException 
     */    
	public void setQuiesceTimeout(int timeout) throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException, InvalidParameterException {
		
        final String METHOD_NAME = "setQuiesceTimeout"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        if (timeout > 0 && timeout < DOMAIN_QUIESCE_MIN_VALUE) {
        	timeout = DOMAIN_QUIESCE_MIN_VALUE;
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                    "quiesce timeout value is nonzero and less than" + DOMAIN_QUIESCE_MIN_VALUE + ", setting to " + DOMAIN_QUIESCE_MIN_VALUE); //$NON-NLS-1$
        }

        StoredDomain domain = this.getStoredInstance();
        domain.setQuiesceTimeout(timeout);
        Manager manager = Manager.internalGetInstance();
        Repository repository = manager.getRepository();
        this.persistence = repository.updateDomain(domain);
        manager.save(Manager.SAVE_UNFORCED);

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
        		"Domain save setQuiesceTimeout: " + timeout, this); 
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
	}
	
	/**
	 * Get all services in this domain. 
	 * This operation is used on firmware versions 5.0.0 or higher.
	 * @return the service array contains all service in this domain
	 */
	public RuntimeService[] getServices() throws DeletedException, UnsuccessfulOperationException {
		final String METHOD_NAME = "getServices"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
    	
        Device device = this.getDevice();
        // Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
        
        RuntimeService[] services = null;
        // Fix 12063: [3715] Poor performance on initialization with large number of domains
    	// Not to get the service(s) till needed. 
        boolean isServiceCreated = true;
        if ( this.serviceMap == null ) {        	
        	// serviceMap is null, try to add service to the map table        
            isServiceCreated = createServiceMap();
        }
        if ( !isServiceCreated ){ 
        	// Create serviceMap failed, assuming no service is found
        	services = new RuntimeService[0];
    	}
        else {
        	// return service array
	        int iSize = this.serviceMap.size();
	        services = new RuntimeService[iSize];
	                       
	        Iterator<Entry<String, RuntimeService>> iter = this.serviceMap.entrySet().iterator();
	        int iIndex=0;
			while (iter.hasNext()) {
			    Map.Entry<String, RuntimeService> entry = (Map.Entry<String, RuntimeService>) iter.next();
			    if ( entry != null ) {
			    	RuntimeService svc = (RuntimeService) entry.getValue();
			    	if ( svc != null ) {
			    		services[iIndex++] = svc;
			    	}
			    }
			}
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);        
        return(services);
	}
	
	/**
	 * Delete a RuntimeService, and return the result of each objects in a RuntimeService.
	 * @param service The service which is returned from {@link #getServices()} to be deleted
 	 * @param excludeObjects The ConfigObject array for ConfigObjects of Service to be excluded (not to be deleted)
	 * @param deleteReferencedFiles  true is to delete referenced files of service
	 * @return
	 * @throws AMPException
	 * @throws DeletedException
	 * @throws UnsuccessfulOperationException
	 */
	public DeleteObjectResult[] deleteService(RuntimeService service, ConfigObject [] excludeObjects, boolean deleteReferencedFiles) 
		throws AMPException, DeletedException, UnsuccessfulOperationException {
		final String METHOD_NAME = "deleteService"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
    	
        Device device = this.getDevice();
        // Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
        
        DeleteObjectResult[] deletedResult = null;
        // Find service in the service map
        String priKey = service.getPrimaryKey();
        RuntimeService rtSvc = (RuntimeService) this.serviceMap.get(priKey);
        if ( rtSvc != null ) {        	
        	// if found, AMP call to remove the service on device and get the result
        	Commands commands = this.getDevice().getCommands();
 	        DeviceContext deviceContext = device.getDeviceContext();
 	        deletedResult = commands.deleteService(deviceContext, this.getName(), service.getName(), 
 	        			service.getClassName(), excludeObjects, deleteReferencedFiles);
 	        rtSvc.setDeleted(true);
        	this.serviceMap.remove(priKey);
        }
        else { // Cannot find it, throw exception
        	String message = Messages.getString("wamt.clientAPI.Service.svcDeleted", service.getName());
            DeletedException e = new DeletedException(message, "wamt.clientAPI.Service.svcDeleted", service.getName()); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME); 
        return deletedResult;
	}
	
	/**
	 * Update the latest status of domain and all services in this domain.
	 * @throws AMPException
	 * @throws DeletedException
	 * @throws UnsuccessfulOperationException
	 */
	public void refresh() throws DeletedException {
		final String METHOD_NAME = "refresh"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        Device device = this.getDevice();
        
        // Refresh the status of domain here, because it is available for non-500 firmware    
        device.probeDomainStatus(this);
        
        // Continue only if the FW version is 5.0.0 or later        
	    if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
	    	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
		    String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
		    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
		           
		    // Skip refreshing service since it's not supported in pre-500 firmwares
		    return;
	    }
		
        // Refresh the status of all services in this domain
		if ( this.serviceMap == null ) {			
			// Service Map has not been created yet because the domain might just be created, try to create it.
			this.createServiceMap();
		} else {	
			RuntimeService[] rtServices = null;
	       	try {
	       		// AMP call to get all service
		       	Commands commands = device.getCommands();
	       		rtServices = commands.getServiceListFromDomain(device.getDeviceContext(), this.getName());
	       	} catch (com.ibm.datapower.amt.amp.NotExistException e) {
        		// The domain doesn't exist on the device, so set the status to "unknown".
        		// This may happen if the device was just added to the ManagedSet and
        		// doesn't already have the domain .
        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Domain does not exist on device. " +
        				"service status is not updated. HeartbeatTask task will try again.");
        		return;
	       	} catch (AMPException ae){
        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Failed to get service status. " +
        				"service status is not updated. HeartbeatTask task will try again.");
        		return;
        	}
	       	
	       	Vector<String> deletedSvcVector = new Vector<String>();
	       	if ( rtServices == null ) { 
	       		// no service is found, move all services in serviceMap to deletedServiMap	       		
		        Iterator<Map.Entry<String,RuntimeService>> iter = serviceMap.entrySet().iterator();
		        while (iter.hasNext()) {
		        	Map.Entry<String, RuntimeService> entry = (Map.Entry<String, RuntimeService>) iter.next();
		        	if ( entry != null ) {
		                deletedSvcVector.add((String) entry.getKey());
		        	}
		        }		        
	       	}
	       	else { // Update service status	  
	       		Map <String, RuntimeService> serviceMap_latest = new HashMap<String, RuntimeService>();
	       		
		       	for ( RuntimeService rtSvc : rtServices ) {
		       		String priKey = rtSvc.getPrimaryKey();
		       		serviceMap_latest.put(priKey, rtSvc); // Save it for checking the removed service later
		       		// Try to find service in the serviceMap
		       		if ( this.serviceMap.containsKey(priKey) ) { 
		       			// found it, update the status
		       			RuntimeService rtService = this.serviceMap.get(priKey);
		       			rtService.updateStatus( rtSvc.getAdminStatus(), rtSvc.getOpStatus(), rtSvc.getNeedsSave(), rtSvc.getQuiesceStatus());
		       		} else { 
		       			// A total new service, add to the servcieMap
		        		// Fix the #12216, need to set the domain of RuntmeService
		        		rtSvc.setDomain(this);
			        	this.serviceMap.put(priKey, rtSvc);		        		
		        	}
		        } // End of for loop   
		       	
		       	// Remove the deleted service in domain
		        Iterator<Map.Entry<String,RuntimeService>> iter = serviceMap.entrySet().iterator();
		        while (iter.hasNext()) {
		        	Map.Entry<String, RuntimeService> entry = (Map.Entry<String, RuntimeService>) iter.next();
		        	if ( entry != null ) {
		        		String sPrimaryKey = (String) entry.getKey();
		                if ( !serviceMap_latest.containsKey(sPrimaryKey) ) { 
		                	// The service is removed, save to deletedSvc StringCollection  		                	
		                	deletedSvcVector.add(sPrimaryKey);
		                }
		        	}
		        }
	       	}
	       	for ( int i=0; i < deletedSvcVector.size(); i++ ) {		        	
	        	String sPrimaryKey = deletedSvcVector.get(i);
		        RuntimeService rtSvc = this.serviceMap.get(sPrimaryKey);
            	rtSvc.setDeleted(true);
            	// Remove it from serviceMap
            	this.serviceMap.remove(sPrimaryKey);
	        }
		}
	}
	
	/*
	 * This method is to add the services in Domain to a Service Map using the amp call when domain is created and refreshed. 
	 * The service map is for later use.
	 */
	private boolean createServiceMap() {
		final String METHOD_NAME = "createServiceMap"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        // Since this method is private, and only is called by getServices() and refresh(). Those have already checked the firmware level,
        // it is not necessary to check the firmware level here again.
                
		boolean isDone = false;		     
    	try {
    		// AMP call to get the service list
    		Device device = this.getDevice();
    		Commands commands = device.getCommands();
    	    RuntimeService[] rtSvc = commands.getServiceListFromDomain(device.getDeviceContext(), this.getName());    	        
    	    if ( rtSvc != null ) {
    	    	int iSize = rtSvc.length;
    	        if ( iSize > 0 ) {	  
    	        	this.serviceMap = new HashMap<String, RuntimeService>();
	    	        for ( int i=0; i < iSize; i++ ) {
	    	        	// Not to set the referenced object till needed
	    	    		// Create runtime service
	    	        	RuntimeService service = new RuntimeService(this, rtSvc[i]);
	    	        	this.serviceMap.put(service.getPrimaryKey(), service);
	    	        }
    	        }
    	        isDone = true;
    	    }
    	    else {	    	        
    	    	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "No service is found in domain:" + this.getName()); //$NON-NLS-1$
    	    }    	        	        
    	} catch (AMPException e) {
    		// Fix defect 11849: Invalid credentials or failing connectivity results in unrecoverable error
    		// suppress the exception 
    		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "AmpException thrown: was unable to parse the response message from AMP call," +
    				" or received an invalid response from the device");   			
    		
    	} catch (DeletedException e) {
    		// Fix defect 11849: Invalid credentials or failing connectivity results in unrecoverable error
    		// suppress the exception 
    		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "DeletedException thrown: was trying to access the device or domain" +
    				" which has been deleted from the persisted datastore");
    	}        
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return isDone;
	}

	/**
	 * 	Get the referenced objects and file list of the service/object in this domain.
	 * @param object
	 * @return
	 */
	ReferencedObjectCollection getReferencedObjects(ConfigObject object) 
		throws AMPException, DeletedException, UnsuccessfulOperationException, NotExistException {
		final String METHOD_NAME = "getReferencedObjects"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        Device device = this.getDevice();
        // Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
        boolean isPresent = false;
        try {
        	// Check if domain present on device
        	isPresent = isPresentOn(getDevice());
		} catch (DeletedException e1) {
			// Eat it, we'll bail out anyway
		}
		
		ReferencedObjectCollection objectCollection = null;
		if ( isPresent ) {				
			Commands commands = device.getCommands();
			DeviceContext deviceContext = device.getDeviceContext();
			objectCollection = commands.getReferencedObjects(deviceContext, this.getName(), 
					object.getName(), object.getClassName());
			
			logger.exiting(CLASS_NAME, METHOD_NAME);
		}
		else {
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
	                   "domain " + this.getName() + " is not present on the device: " + getDevice().getSymbolicName()); //$NON-NLS-1$
			String message = Messages.getString("wamt.clientAPI.Domain.notExistRefObjs",  getDevice().getSymbolicName());
			throw new NotExistException(message, "wamt.clientAPI.Domain.notExistRefObjs",  getDevice().getSymbolicName());
		}
		logger.exiting(CLASS_NAME, METHOD_NAME); 
				
		return objectCollection;
	}
	
	/**
	 * Create the ServiceDeployment object with service configuration and deployment policy to be deployed in this domain. 
	 * Parameter policyURL must be valid to point to the deployment policy. 
	 * Use {@link #createServiceDeployment(ServiceConfiguration, boolean)} instead if no deployment policy is for deployment.
	 * <p>
	 * Note: If {@link ServiceConfiguration#setServicesForDeployment(ConfigService[])} is not invoked, then assume all services 
	 * and files in the source configuration will be deployed, the importAllFiles will be treated as true, meaning all files 
	 * in the source configuration will be deployed to the target domain no matter what the importAllFiles is set.
	 * 
	 * @param svcConfig The ServiceConfiguration object
	 * @param policyURL is a URLSource that points to a configuration source containing the policy to be used 
     *        during deployment. The deployment policy information is not persisted in the 
     *        repository until the policy is successfully deployed.  This parameter is required.        
	 * @param policyDomain this is the first piece of information used to find the deployment policy within
     *        the configuration source specified by the URLSource.  This parameter is only required if the 
     *        source is a backup, since a backup may contain multiple domains. 
	 * @param policyObjName this is the second piece of information used to find the deployment policy within
     *        the configuration source specified by the URLSource.  This parameter is required.
	 * @param importAllFiles true to import all files in a configuration source
	 * 
	 * @return The ServiceDeployment object
	 * 
	 * @see #createServiceDeployment(ServiceConfiguration, boolean)
	 * 
	 * @throws DeletedException
	 * @throws UnsuccessfulOperationException
	 * @throws NotExistException
	 * @throws InUseException
	 * @throws InvalidParameterException
	 * @throws AMPException
	 * @throws IOException
	 * @throws DirtySaveException
	 * @throws DatastoreException
	 * @throws FullException
	 * @throws MissingFeaturesInFirmwareException
	 * @throws NotManagedException
	 */
	public ServiceDeployment createServiceDeployment(ServiceConfiguration svcConfig, URLSource policyURL, String policyDomain, String policyObjName, boolean importAllFiles)
		throws DeletedException, UnsuccessfulOperationException, NotExistException, InUseException, InvalidParameterException, AMPException, IOException,
		DirtySaveException, DatastoreException, FullException, MissingFeaturesInFirmwareException, NotManagedException{
		final String METHOD_NAME = "createServiceDeployment"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        Device device = this.getDevice();
        // Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
         
        // Check svcConfig
        if ( svcConfig == null) {
        	String message = Messages.getString("wamt.clientAPI.Service.invalidConfigSvc");
			throw new InvalidParameterException(message,"wamt.clientAPI.Service.invalidConfigSvc");
        }
        // Check URLSource
        if ( policyURL == null ) { 
        	String message = Messages.getString("wamt.clientAPI.Domain.noDepPolForService");
			throw new NotExistException(message,"wamt.clientAPI.Domain.noDepPolForService");
        }        
        
        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                "URLSource of ploicy is " + policyURL.getURL() ); //$NON-NLS-1$        
        DeploymentPolicyService deployPolicy = new DeploymentPolicyService(this, policyURL, policyDomain, policyObjName);
        // Set deployment policy		
		ServiceDeployment svcDeployment = new ServiceDeployment(this, svcConfig, deployPolicy, importAllFiles);
				
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return svcDeployment;		
	}
	
	/**
	 * Create the ServiceDeployment object with service configuration only, no deployment policy for deployment.
	 * @param svcConfig The ServiceConfiguration object
	 * @param importAllFiles true to import all files in a configuration source
	 * 
	 * @return The ServiceDeployment object
	 * 
	 * @see createServiceDeployment(ServiceConfiguration, URLSource, String, String, boolean)
	 * 
	 * @throws DeletedException
	 * @throws UnsuccessfulOperationException
	 * @throws NotExistException
	 * @throws IOException
	 * @throws InUseException
	 * @throws InvalidParameterException
	 * @throws AMPException
	 */
	public ServiceDeployment createServiceDeployment(ServiceConfiguration svcConfig, boolean importAllFiles) 
		throws DeletedException, UnsuccessfulOperationException, NotExistException, IOException, InUseException, InvalidParameterException, AMPException {
		final String METHOD_NAME = "createServiceDeployment"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        Device device = this.getDevice();
        // Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {getDevice().getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
        
        // Check parameter
        if ( svcConfig == null ) {
        	String message = Messages.getString("wamt.clientAPI.Service.invalidConfigSvc");
			throw new InvalidParameterException(message,"wamt.clientAPI.Service.invalidConfigSvc");
        }
		ServiceDeployment svcDeployment = new ServiceDeployment(this, svcConfig, importAllFiles);		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return svcDeployment;
	}
	
	/**
	 * Get Domain Status
	 * @return {@link DomainStatus} 
	 * @throws DeletedException 
	 * @throws AMPException 
	 */
	DomainStatus getStatus() throws InvalidParameterException, DeletedException {
		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return (this.getDevice().getDomainStatus(this));	
	}
	
	/**
	 * Get AdminStatus of this domain, enabled (AdminState.ENABLED) or disabled (AdminState.DISABLED).
	 * @return {@link AdminStatus } 
	 * @throws InvalidParameterException 
	 * @throws DeletedException 
	 */
	public AdminStatus getAdminStatus() throws InvalidParameterException, DeletedException {
		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return (this.getDevice().getDomainStatus(this)).getAdminStatus();	
	}
	
	/**
	 * Get OpStatus of this domain, up (OpState.UP) or down (OpState.DOWN).
	 * @return {@link OpStatus} 
	 * @throws InvalidParameterException 
	 * @throws DeletedException 
	 */
	public OpStatus getOpStatus() throws InvalidParameterException, DeletedException {
		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        OpStatus result = OpStatus.UNKNOWN;
        
        OperationStatus operationStatus = (this.getDevice().getDomainStatus(this)).getOperationStatus();
        if ( operationStatus.isUp() ) {
        	result = OpStatus.UP;
        }
        else if ( operationStatus.isDown() ) {
        	result = OpStatus.DOWN;
        }
        		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return result;	
	}
	
	/**
	 * Get Quiesce Status of this domain.
	 * @return {@link QuiesceStatus} 
	 * @throws InvalidParameterException 
	 * @throws DeletedException 
	 */
	public QuiesceStatus getQuiesceStatus() throws InvalidParameterException, DeletedException {
		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return (this.getDevice().getDomainStatus(this)).getQuiesceStatus();	
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#addTag(java.lang.String, java.lang.String)
	 */
	public void addTag(String name, String value) 
	throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException, InvalidParameterException {
    	final String METHOD_NAME = "addTag(" + name + "," + value + ")"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        if ( name == null || name.length() == 0 ) { // name is required
        	String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
        	throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
        }
        if ( value == null ) {
        	value = "null";
        }
        
        Manager manager = Manager.internalGetInstance();
        // Try to get the tag if exists
        StoredTagImpl storedTag = manager.getStoredTag(name, value);
        if ( storedTag == null ) { // not exist, create new one
        	Repository repository = manager.getRepository();
        	storedTag = repository.createTag(name, value);
        }
        storedTag.add(this.persistence);
        
        this.persistence.add(storedTag);
        manager.save(Manager.SAVE_UNFORCED);
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Add Tag name:" + name + "Tag value:" + value +
    			" to the domain: " + this.getName());
        
        logger.exiting(CLASS_NAME, METHOD_NAME);		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#removeTag(java.lang.String, java.lang.String)
	 */
	public void removeTag(String name, String value) 
	throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException {
    	final String METHOD_NAME = "removeTag(" + name + "," + value + ")"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        if ( name == null || name.length() == 0 ) { // name is required
        	String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
        	throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
        }
        
        StoredTag[] tags = this.getStoredInstance().getTags();  //removes from stored Device
        int iSize = tags.length;
        if ( iSize > 0 ) {
        	StoredDomain storedDomain = this.getStoredInstance();
        	boolean bChange = false;
        	for ( int i=0; i < iSize; i++ ) {
        		if ( tags[i].getName().equals(name) && tags[i].getValue().equals(value)) {
        			storedDomain.remove(tags[i]);
        			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Remove Tag name:" + name + "Tag value:" + value +
    	        			" from the domain: " + this.getName());
        			bChange = true;
        		}
        	}        	
        	if ( bChange )
        		Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
        }

        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#removeTag(java.lang.String)
	 */
	public void removeTag(String name) 
	throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException {
		final String METHOD_NAME = "removeTag("+name+")"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        if ( name == null || name.length() == 0 ) { // name is required
        	String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
        	throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
        }
        
        StoredTag[] tags = this.getStoredInstance().getTags();  //removes from stored Device
        int iSize = tags.length;
        if ( iSize > 0 ) {
        	StoredDomain storedDomain = this.getStoredInstance();
        	boolean bChange = false;
        	for ( int i=0; i < iSize; i++ ) {
        		if ( tags[i].getName().equals(name)) {
        			storedDomain.remove(tags[i]);
        			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Remove Tag name:" + name + 
    	        			" from the domain: " + this.getName());
        			bChange = true;
        		}
        	}    
        	if ( bChange )
        		Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#removeTags()
	 */
	public void removeTags() throws DeletedException, DirtySaveException, DatastoreException  {
		final String METHOD_NAME = "removeTags()"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        this.getStoredInstance().removeTags();  //removes from stored Device
        Manager.internalGetInstance().save(Manager.SAVE_UNFORCED);

        logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#getTagNames()
	 */
	public Set<String> getTagNames() throws DeletedException {	
		final String METHOD_NAME = "getTagNames()"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
        HashSet<String> result = new HashSet<String>();
        StoredTag[] storedTags = this.getStoredInstance().getTags();
        if (storedTags != null) {
            for (int i=0; i<storedTags.length; i++ ) {
            	if ( storedTags[i] != null )
            		result.add(storedTags[i].getName());
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }
    
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.clientAPI.Taggable#getTagValues(java.lang.String)
	 */
    public Set<String> getTagValues(String name) throws DeletedException, InvalidParameterException {
    	final String METHOD_NAME = "getTagValues("+name+")"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		if ( name == null || name.length() == 0 ) { // name is required
        	String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
        	throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
        }
		
    	HashSet<String> result = new HashSet<String>();
    	StoredTag[] storedTags = this.getStoredInstance().getTags();
        if (storedTags != null) {
            for (int i=0; i<storedTags.length; i++ ) {
            	if ( storedTags[i] != null ) {
            		if ( storedTags[i].getName().equals(name)) 
            			result.add(storedTags[i].getValue());
            	}
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    	return (result);
    }
    
    /**
     * Upload file to device of this domain
     * 
     * @param fileName  the name of the file in the format used by the device, i.e., store:///myfile. 
     * 						The folder must exist in the device, otherwise, the exception is thrown
     * 
     * @param urlSource an URLSource that points to the file that is copied to the Device, 
     * the schemes of url only support "file:///", "http://", "https://". URISyntaxException exception is thrown if the urlSource is not valid.
     * @throws DeletedException 
     * @throws AMPException 
     * @throws IOException 
     * @throws URISyntaxException 
     * @throws NotExistException
     */
    public void uploadFile(String fileName, URLSource urlSource) 
    throws DeletedException, AMPException, IOException, URISyntaxException, NotExistException, UnsuccessfulOperationException {
    	final String METHOD_NAME = "uploadFile"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        //Check URLSource
        final String SCHEME_HTTP     = "http";
    	final String SCHEME_HTTPS    = "https";
    	final String SCHEME_FILE     = "file";
    	
    	String urlString = urlSource.getURL();
		URI uri = new URI(urlString);
		
		String scheme = uri.getScheme();
		// Only support "file:///", "http://", "https://"
		if ( !SCHEME_FILE.equals(scheme) && SCHEME_HTTP.equals(scheme) && SCHEME_HTTPS.equals(scheme) ) {
			throw new URISyntaxException(scheme, Messages.getString("wamt.clientAPI.URLSource.invalid"));
		}
		// For all schemes except file, ensure there is a valid host name
		if ( !SCHEME_FILE.equals(scheme) && uri.getHost() == null ) {
			throw new URISyntaxException(scheme, Messages.getString("wamt.clientAPI.URLSource.invalidHost"));
		}		
        
        try {
	    	byte[] bytes= null;    	
	    	Device device = this.getDevice();
	    	
	    	// Check if domain exists on device
			if ( !isPresentOn(device) ){
				// Domain does not exist, throw exception
	    		logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
		                   "domain " + this.getName() + " is not present on the device: " + getDevice().getSymbolicName()); //$NON-NLS-1$
				String message = Messages.getString("wamt.clientAPI.Domain.notExistRefObjs",  getDevice().getSymbolicName());
				throw new NotExistException(message, "wamt.clientAPI.Domain.notExistRefObjs",  getDevice().getSymbolicName());
	    	}
	          
	    	if (SCHEME_FILE.equals(scheme)) {
				File thisFile = new File(uri);
				Blob fileBlob = new Blob(thisFile);
				bytes = fileBlob.getByteArray();
	    	}
	    	else {
	    		URL thisURL = uri.toURL();
	    		Blob urlBlob = new Blob(thisURL);
	    		bytes = urlBlob.getByteArray();
	    	}
			
	    	// defect 12666 Uploading a large file produces WAMT error WAMT0470E
			// Check size of file, 
	    	// Only 100 MB is allowable for the size of file.
			if( bytes.length > 100000000L){
				// file size is too large
				bytes = null;
				Object[] params = { device.getDeviceContext().getHostname(), "100MB" };
				String message = Messages.getString("wamt.clientAPI.Domain.fileTooLarge", params); //$NON-NLS-1$
				UnsuccessfulOperationException e = new UnsuccessfulOperationException(message, "wamt.clientAPI.Domain.fileTooLarge", params); //$NON-NLS-1$								
				throw e;    				
			}
			Commands commands = device.getCommands();
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
	                "call setFile on " + device.getSymbolicName() + " in domain " + this.getName()); //$NON-NLS-1$
			commands.setFile(device.getDeviceContext(), this.getName(), fileName, bytes);
        }  catch (IOException e) {			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (AMPException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			throw e;
		} catch (UnsuccessfulOperationException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (DeletedException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (NotExistException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * To restart this domain
     * @throws LockBusyException 
     * @throws NotExistException 
     * @throws UnsuccessfulOperationException 
     */
    public void restart() 
    throws DeletedException, AMPException, LockBusyException, UnsuccessfulOperationException, NotExistException {
    	final String METHOD_NAME = "restart"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
    	
        Device device = this.getDevice();
        boolean isQuiesced = false;
    	if ( isPresentOn(getDevice()) ){
			quiesce();
			isQuiesced = true;
		}
    	else {
    		// do nothing, just return
    		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
			        "Unable to restart domain " + this.getName() + " because it is not on " + device.getSymbolicName());
    		return ;
    	}
    	
        device.lockNoWait();
        try {        	
			Commands commands = device.getCommands();			
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
				        "resart domain " + this.getName() + " on " + device.getSymbolicName());			
			commands.restartDomain(device.getDeviceContext(), this.getName());			
        } catch (DeletedException e) {
        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
    	} catch (AMPException e) {
    		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;    	    	
		} finally {
			if ( isQuiesced ){
				unquiesce(); // Need to unquiesce amd refresh the domain here in case the exception is thrown
			}
            // release lock 
        	device.unlock();
		}
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get all status
     * @return all status
     * @throws DeletedException
     * @throws SOMAException
     * @throws SOMAIOException
     * @throws InvalidCredentialsException
     */
    public Map<String, List<Status>> getAllStatus() 
    throws DeletedException, SOMAException, SOMAIOException {
    	final String METHOD_NAME = "getAllStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Map<String, List<Status>> result = null;
    	
		try {
			Device device = this.getDevice();
			SOMACommands commands = device.getSOMACommands();
			result = commands.getAllStatus(device.getDeviceContext(), this.getName());
			if ( result == null ) 
				result = new HashMap<String, List<Status>>();
		} catch (DeletedException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (SOMAIOException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;		
		} catch (SOMAException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, result.size());
    	return result;
    }
    
    /**
     * Get a status
     * @param name The name of operation to get status
     * @return the status list
     * @throws DeletedException
     * @throws SOMAException
     * @throws SOMAIOException
     * @throws InvalidCredentialsException
     */
    public List<Status> getStatus(String name) 
    throws DeletedException, SOMAException, SOMAIOException {    	
    	final String METHOD_NAME = "getStatus"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{name});
        List<Status> result = null;
    	
    	try {
			Device device = this.getDevice();
			SOMACommands commands = device.getSOMACommands();
			result = commands.getStatus(device.getDeviceContext(), this.getName(), name);
			if ( result == null ) 
				result = new ArrayList<Status>();
		} catch (DeletedException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (SOMAIOException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;		
		} catch (SOMAException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME, result.size());
    	return result;
    }
}
