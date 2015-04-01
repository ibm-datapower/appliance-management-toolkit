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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPConstants;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.CommandFactory;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DeviceMetaInfo;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.PingResponse;
import com.ibm.datapower.amt.amp.SubscriptionResponseCode;
import com.ibm.datapower.amt.clientAPI.util.KeyStoreInfo;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.dataAPI.local.filesystem.StoredTagImpl;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.amt.soma.SOMACommandFactory;
import com.ibm.datapower.amt.soma.SOMACommands;
import com.ibm.datapower.amt.soma.SOMAException;

/**
 * A Java object representation of an DataPower device (appliance). Use the static method
 * {@link #createDevice(String, String, String, String, int)} to  create a <code>Device</code>
 * object. This method will instantiate the object, load metadata from the device, and persist
 * the information via the <code>dataAPI</code>. <code>Device</code> is recreated by the {@link Manager} 
 * at startup. A device is removed from the manager by calling {@link Manager#remove(Device)}.
 * <p>
 * Even though a device, upon creation, is known to the manager, it is unmanaged until it is 
 * added to a ManagedSet via {@link ManagedSet#addDevice(Device)}. A device can belong to one 
 * and only one ManagedSet.
 * <p>
 * A device can have multiple (0..N) domains. But a domain is not managed unless it 
 * is created using the <code>createManagedDomain</code> method on the {@link Device} or
 * {@link ManagedSet} object. Calling <code>createManagedDomain</code> on {@link ManagedSet} 
 * causes a domain object to be created on each device in the managed set. the manager will not
 * perform any operations on an unmanaged domain.
 * <p>
 * Each managed domain has an operational status. The operational status reflects 
 * the op-state of the domain. A device has an operational status, which is the aggregation 
 * of all of the domains. Firmware does not have an operational status. Refer to the 
 * javadoc for {@link com.ibm.datapower.amt.OperationStatus} for
 * an explanation of how the aggregation occurs.
 * <p>
 * Firmware may be updated on a device regardless of its membership in a managed set.
 * <p>
 * The methods in this class that access data that originates from a device are
 * really accessing cached values in the data members of a class instance. They
 * are not invoking the device communication API (AMP) to get the value in real
 * time. Separate threads in the manager will communicate with the device in a
 * background manner to get the data from the device to populate this class
 * instance.
 * <p>
 * 
 * @version SCM ID: $Id: Device.java,v 1.20 2011/04/19 20:36:49 fbarsob Exp $
 */
//* Methods in this class that have package access should be invoked by the
//* manager, not a client.
//* <p>
public class Device implements Persistable, Taggable {
    /*
     * although this object can be persisted to the datastore, only
     * some of the attributes will be persisted. Other attributes will be
     * retrieved dynamically from the device.
     */
    private volatile StoredDevice persistence = null;

    /*
     * Lock for device
     */
    private volatile Lock lock = null;
    
    /*
     * Indicate if a device is managed by a primary manager.
     */
    private volatile boolean isPrimary = true;

    /*
     * All the attributes above this line will be persisted to the datastore.
     * deviceType will be persisted because we want to know the device type even
     * if the device is not managed, in case we are not able to have ManagedSets
     * of heterogeneous deviceTypes - it is important for the user to know the
     * deviceType before they add it to a managedSet. guiPort should be persisted
     * because the administrator may want to use the on-the-glass integration
     * to access the device's WebGUI before it is in a ManagedSet.
     */
    // next few are for domains
    private volatile StringCollection allDomainNames = null;
//    private volatile Map managedDomainsManagementStatus = null; // with key domainName
    //private volatile Map managedDomainsOperationStatus = null;  // with key domainName
    private volatile Map<String, DomainStatus> managedDomainsStatus = null;  // with key domainName
    // next few are for firmware
    private volatile String actualFirmwareLevel = null;
    private volatile ManagementStatus firmwareManagementStatus = null;
    private volatile ManagementStatus deviceManagementStatus = null;
    private volatile String sourceFirmwareVersionKey = null;  // primary key for source firmware for deployment
	private static final String RESTORE_TGZ_EXTENSION = "tgz";
	private static final String RESTORE_MANIFEST_EXTENSION = "xml";
	private static final String RESTORE_FILE_MANIFEST = "backupmanifest.xml";		

    private static int DEFAULT_DEVICE_QUIESCE_VALUE;
    protected final static int DEVICE_QUIESCE_MIN_VALUE = 60; //seconds
    
    private volatile int[] supportedCommands = null;
    
    boolean licenseAccepted = false; // For firmware deployment
    
    private boolean secureBackupSupported = false; // If secure backup is supported
    
    volatile boolean isDeviceReachable = false;

    
    // for AMP
    private volatile DeviceContext cachedDeviceContext = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    static final String SCM_REVISION = "$Revision: 1.20 $"; //$NON-NLS-1$
    
    protected static final String CLASS_NAME = Device.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {        
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
        
        Integer integer2 = Configuration.getAsInteger(Configuration.KEY_DEVICE_QUIESCE_TIMEOUT);
        DEFAULT_DEVICE_QUIESCE_VALUE = integer2.intValue();

    }

    private static final StringCollection ALL_TOPICS = 
        new StringCollection(new String[] {"*"}); //$NON-NLS-1$
    
    /**
     * Create a background task to add a new device to the Manager. You can
     * retrieve the Device from the Manager after the ProgressContainer
     * indicates that this is complete. The device created by this task is an 
     * unmanaged device. The device is managed once it is added to a {@link ManagedSet}.
     * <p>
     * As a user of the clientAPI, you should invoke this method instead of the
     * Device constructor. This is a long-running operation that requires a
     * conversation with the device to fetch the required attributes (i.e.,
     * hardware serial number, device type, etc.) This method will return
     * quickly, and you should monitor the returned ProgressContainer for status
     * and results.
     * 
     * @param symbolicName symbolic (human-readable) name of the device - this 
     *        is an arbitrary name that the caller specifies, and is usually 
     *        a short name, like DP1, DP2, etc. The {@link Manager#getDeviceBySymbolicName(String)} 
     *        method uses this value to find the device. 
     * @param hostname network hostname or IP address of the device. This is the 
     *        value used as an address to establish a connection to the device.
     *        <em>Note:  Hostname must be a valid host name per 
     *        <a href="http://tools.ietf.org/html/rfc952">RFC 952</a> and 
     *        <a href="http://tools.ietf.org/html/rfc1123">RFC 1123</a>.</em>
     * @param userid administrative userid
     * @param password password for the administrative userid
     * @param hlmPort device's network port for HLM (AMP)
     * @return the object that contains the method's progress and result
     *         information. If the ProgressContainer indicates successful
     *         completion, you can retrieve the newly-created Device object
     *         instance from the ProgressContainer.
     * @throws FullException the background task queue is full
     */
    public static ProgressContainer createDevice(String symbolicName, String hostname, 
                                                 String userid, String password, int hlmPort) throws FullException {
        final String METHOD_NAME = "createDevice(String...)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, hostname);
        Manager manager = null;
        ProgressContainer result = null;
        manager = Manager.internalGetInstance();
        
        BackgroundTask backgroundTask = 
            BackgroundTask.createNewDeviceTask(symbolicName, hostname, 
                                               userid, password, hlmPort);
        result = backgroundTask.getProgressContainer();
        manager.enqueue(backgroundTask, manager);
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }
    
    /**
     * Create a new device object in memory with these properties and add it to
     * the manager. This constructor is long running, because it has to contact
     * the device to get critical information including the serial number and
     * device type. This constructor should be invoked only by a
     * background thread.
     * 
     * @param symbolicName the human-consumable symbolic name for this object
     * @param hostname the hostname or IP address of the device
     * @param userid the administrative userid of the device
     * @param password the administrative password of the device
     * @param hlmPort the device port for HLM communications
     * @param progressContainer
     * @throws AMPException
     * @throws com.ibm.datapower.amt.clientAPI.AlreadyExistsException
     * @throws DatastoreException
     * @throws AlreadyExistsInRepositoryException
     * @throws DatastoreException
     * @throws DeletedException
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyStoreException 
     * @throws KeyManagementException 
     * 
     * @see ManagedSet#add(Device, ProgressContainer)
     * @see ManagedSet#getDeviceMembers()
     * @see Manager#getAllDevices()
     * @see Manager#getUnmanagedDevices(DeviceType)
     */
    Device(String symbolicName, String hostname, 
           String userid, String password, int hlmPort, 
           ProgressContainer progressContainer) 
        throws AMPException, AlreadyExistsInRepositoryException, UnsupportedVersionException, DatastoreException, 
        		DeletedException, InvalidDeviceMetaInfoException {
        final String METHOD_NAME = "Device(String...)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, hostname);        
        Manager manager = null;
        manager = Manager.internalGetInstance();
        // this can be a block acquisition because this method should be called only from the queue, not in the foreground
        manager.unmanagedDevicesLockWait();
        //TODO WJJ this lock throws exception when stored instance does not exist: this.lock = new Lock(this.toString());
        this.lock = new Lock(hostname);
        try {
            // need to get the hardware serial number before we can construct StoredDevice
            progressContainer.incrementCurrentStep(1, "wamt.clientAPI.Device.ProbeDevice_txt", hostname); //$NON-NLS-1$
            DeviceContext deviceContext = getDeviceContext(hostname, hlmPort, userid, password);
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "getting DeviceMetaInfo for " + hostname); //$NON-NLS-1$
            
            // Add certificate sent from server to the file (dpcacerts)
        	this.createNewCertFile(deviceContext);
        	
            DeviceMetaInfo metaInfo = getDeviceMetaInfo(deviceContext);
            
            if(metaInfo == null){
            	// Can not get the metainfo here, something wrong with the AMP
            	String message = Messages.getString("wamt.clientAPI.Device.failedGetDeviceMetaInfo");
            	AMPException ae = new AMPException(message);
            	throw ae;
            }else{
            	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "DeviceMetaInfo received: " + metaInfo); //$NON-NLS-1$
            }
            
            // got DeviceMetaInfo successfully, assume the device is reachable
            isDeviceReachable = true;
            
            String serialNumber = metaInfo.getSerialNumber();
            
            // Check for a XC10 firmware problem that sometimes
            // The serial number is 00000000
            if(serialNumber.trim().equals("00000000")){
            	String message = Messages.getString("wamt.clientAPI.Device.invalidSerialNumber"); //$NON-NLS-1$
            	InvalidDeviceMetaInfoException e = new InvalidDeviceMetaInfoException(message, "wamt.clientAPI.Device.invalidSerialNumber"); //$NON-NLS-1$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                // make sure it appears in the log, throwing() is FINER
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw e;
            }
            
            int guiPort = metaInfo.getWebGUIPort();
            DeviceType deviceType = metaInfo.getDeviceType();
            String firmwareLevel = metaInfo.getFirmwareLevel();
            
            // special handling for XC10 firmware level
            int index = firmwareLevel.indexOf('-');
            if(index > 0){
            	firmwareLevel = firmwareLevel.substring(0, index);
            }
            
            ModelType modelType = metaInfo.getModelType();
            StringCollection featureLicenses = metaInfo.getFeatureLicenses();
            String currentAMPVersion = metaInfo.getCurrentAMPVersion(); 
                        
            // check to see if its symbolicName, UID or hostname:amp port already exists
            // We won't use the serial number because it might be the same for virtual appliance
            String deviceID = UUID.randomUUID().toString();
            if (manager.containsDeviceUID(deviceID)) {
                String message = Messages.getString("wamt.clientAPI.Device.DeviceIDExists", deviceID); //$NON-NLS-1$
                AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message, "wamt.clientAPI.Device.DeviceIDExists", deviceID);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                // make sure it appears in the log, throwing() is FINER
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            if (manager.containsDeviceSymbolicName(symbolicName)) {
                String message = Messages.getString("wamt.clientAPI.Device.nameExists",symbolicName); //$NON-NLS-1$
                AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message,"wamt.clientAPI.Device.nameExists",symbolicName);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                // make sure it appears in the log, throwing() is FINER
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            if (manager.containsHostnamePort(hostname+":"+hlmPort)) {
            	Object[] params = { hostname, hlmPort };
                String message = Messages.getString("wamt.clientAPI.Device.HostnamePortExists", params); //$NON-NLS-1$
                AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message, "wamt.clientAPI.Device.HostnamePortExists", params);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                // make sure it appears in the log, throwing() is FINER
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            
            // Do not check XC10 devices
            if (!DeviceType.XC10.equals(deviceType) && !FirmwareVersion.meetsMinimumLevel(firmwareLevel)) {
                Object[] args = {firmwareLevel , symbolicName, hostname, MinimumFirmwareLevel.MINIMUM_LEVEL};
                String message = Messages.getString("wamt.clientAPI.Device.fwNotMinSupport", args);//$NON-NLS-1$
                UnsupportedVersionException e = new UnsupportedVersionException(message,"wamt.clientAPI.Device.fwNotMinSupport", args);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
            
            // get or construct the StoredDevice instance
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "writing Device to persistence"); //$NON-NLS-1$
            Repository repository = manager.getRepository();
            this.persistence = repository.createDevice(deviceID, serialNumber, symbolicName, 
                                                       deviceType, modelType, 
                                                       hostname, userid, password,
                                                       hlmPort, guiPort, currentAMPVersion);
            this.persistence.setFeatureLicenses(featureLicenses);
            
            // Save supported commands in this device object
            this.supportedCommands = metaInfo.getSupportedCommands();
            
            this.actualFirmwareLevel = firmwareLevel;
            
            // do not subscribe to a Device until it is added to a ManagedSet.
            // The heartbeat daemon will talk to unmanaged Devices.
            
            // populate the rest of the non-persisted members
            String domains[] = null;
            
            // Get domain only for DataPower devices
            if(!DeviceType.XC10.equals(deviceType)){
            	domains = getCommands().getDomainList(deviceContext);
            	this.allDomainNames = new StringCollection(domains);
            	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        	"domains on device: " + this.allDomainNames); //$NON-NLS-1$
            }
            
//            this.managedDomainsManagementStatus = new HashMap();
            this.managedDomainsStatus = new HashMap<String, DomainStatus>();
            this.firmwareManagementStatus = new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN);
            this.deviceManagementStatus = new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN);
            
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);
            
            try {
    			setQuiesceTimeout(DEFAULT_DEVICE_QUIESCE_VALUE);
    		} catch (InvalidParameterException e) {
    			// we will have handled this scenario before now.
    		}
    		
    		if(metaInfo.getSecurebackup()!=null && metaInfo.getSecurebackup().equals("enabled"))
    			secureBackupSupported = true;
    		else
    			secureBackupSupported = false;
            
            /*
             * since creating Devices is a long-running task, do not call
             * progressContainer.setComplete(this) here. It should be done only
             * in the NewDeviceTask class.
             */
        } finally {
            manager.unmanagedDevicesUnlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Create a new device object in memory from a storedDevice
     *
     */
    Device(StoredDevice storedDevice) {
        final String METHOD_NAME = "Device(StoredDevice)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, storedDevice.getPrimaryKey());
        Manager manager = Manager.internalGetInstance();
        manager.unmanagedDevicesLockWait();
        this.lock = new Lock(this.toString());
        try {
            
            this.persistence = storedDevice;
            
            // populate the rest of the non-persisted members
            // this.persisted.featureLicenses was set on other constructor.
            
            // this.actualFirmwareLevel is set in this.refreshCachedInfo() below.

            // this.allDomainNames is loaded in this.refreshCachedInfo() below.
            
//            this.managedDomainsManagementStatus = new HashMap();
            this.managedDomainsStatus = new HashMap<String, DomainStatus>();
            this.firmwareManagementStatus = new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN);
            this.deviceManagementStatus = new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN);
            
            // add it to the persistence mapper
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.add(this.persistence, this);
            
            try {
            	// Add certificate sent from server to the file (dpcacerts)                
                this.createNewCertFile(this.getDeviceContext());
                this.refreshCachedInfo();
            } catch (DeletedException e1) {
                // shouldn't happen, we are just constructing it
                String message = "Internal error: this should not happen"; //$NON-NLS-1$
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (AMPException e1) {
                // let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.ampCommError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (DatastoreException e1) {
                // skip this one, let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.RepError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            }
        } finally {
            manager.unmanagedDevicesUnlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get the managed Domains on this Device
     * 
     * @return an array of the Domains that are managed. If no managed domains exist on the 
     *         device, or if this is an unmanaged device, then this array will have 
     *         zero elements.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Domain[] getManagedDomains() throws DeletedException {
    	
    	Domain[] result = null;
    	
    	if (getManagedSet() == null) {
    		result = new Domain[0];	
    	} else {
    		
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            StoredDomain[] storedDomains = this.getStoredInstance().getManagedDomains();
            Vector domains = new Vector();
            for (int i=0; i<storedDomains.length; i++) {
                Domain domain = mapper.getVia(storedDomains[i]);
                if (domain != null) {
                    domains.add(domain);
                }
            }
            result = new Domain[domains.size()];
            for (int i=0; i<domains.size(); i++) {
                result[i] = (Domain) domains.get(i);
            }
    	}
        return(result);
    }

    /**
     * Create a managed domain on this Device.  This <code>Device</code> must be 
     * a member of a {@link ManagedSet} to call this method, otherwise a 
     * {@link NotManagedException} is thrown. 
     * 
     * Note that for a new domain, i.e. a domain that does not yet exist on the physical device, 
     * the domain does not get created on the physical device until the configuration is deployed.
     * Additionally, no DomainVersion objects will be created until the domain is deployed to the 
     * device. Domain synchronization can be enabled, but synchronization will not 
     * occur until {@link Domain#setSourceConfiguration(URLSource)} and optionally
     * {@link Domain#setDeploymentPolicy(URLSource, String, String)} are called. 
     * <p>
     * If the domainName specified matches an unmanaged domain on the <code>Device</code> the 
     * managed domain object will still be created. Deploying the new {@link Domain} 
     * will overwrite the existing domain.   
     *  
     * <p>
     * Managed domains should be deleted (unmanaged) via <code>removeManagedDomain(String)</code> 
     * on the <code>Device</code> or <code>ManagedSet</code> object.   
     * 
     * @param domainName the name of the managed domain to be created 
     * @return the Domain object
     * @throws AlreadyExistsInRepositoryException the managed domain already exists on this device. 
     * @throws AMPException a communication problem occurred while creating the managed domain
     * @throws DatastoreException a occurred persisting the domain object
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws InUseException 
     * @throws LockBusyException Unable to obtains a lock on the device
     * @throws NotExistException The device does not exist
     * @throws NotManagedException The device is not managed
     * 
     * @see Domain
     * @see ManagedSet#createManagedDomain(String, URLSource, URLSource, String, String)
     */
    public Domain createManagedDomain(String domainName) throws AlreadyExistsInRepositoryException, DeletedException, NotManagedException, DatastoreException, LockBusyException, AMPException, NotExistException, InUseException {
    	
        final String METHOD_NAME = "createManagedDomain(String)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {domainName, this});

        if (this.getManagedSet() == null) {
        	String notManaged = Messages.getString("wamt.clientAPI.FirmwareVersion.devNotInMs", getDisplayName()); //$NON-NLS-1$
    		throw new NotManagedException(notManaged,"wamt.clientAPI.FirmwareVersion.devNotInMs", getDisplayName());
    	}
        
        logger.exiting(CLASS_NAME, METHOD_NAME);

        Domain domain = new Domain(domainName, this);
       	this.probeDomainStatus(domain);
       	
        return domain;
    }
    
    /**
     * Check if a device is reachable. By reachable, it means that the device
     * must be able to respond to an application-level requests correctly. 
     * Incorrect user ID or password, for example, will make the device unreachable 
     * even the device is up and running.  
     * Note that is is not a real time status. It will only be updated when a device is 
     * created, loaded from repository or heart-beat task is executed for managed devices. 
     * 
     */
    public boolean isDeviceReachable(){
    	return isDeviceReachable;
    }
    
    /**
     * Delete a domain on a device. This will only work for an unmanagedDomain, if a managed 
     * domain is to be deleted on a device, then the {@link Domain} object must first be 
     * destroyed by calling {@link Device#removeManagedDomain(String)}
     * 
     * @param domainName - the domain to be deleted
     * @throws DeletedException 
     * @throws InvalidParameterException - thrown if the domain specified is still managed
     * @throws AMPException 
     */
    public void deleteDomain(String domainName) throws DeletedException, InvalidParameterException, AMPException {
        final String METHOD_NAME = "deleteDomain(String)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {domainName, this});

        //Check if null was passed in as the name, croak if it was
    	if (domainName == null) {
			String message = Messages.getString("wamt.clientAPI.DeploymentPolicy.invalidParameter","domainName");
			throw new InvalidParameterException(message,"wamt.clientAPI.DeploymentPolicy.invalidParameter");
    	}
    	
    	Domain[] domains = getManagedDomains();

        //Check if the specified domain is managed, croak if it is
    	for (int i=0; i < domains.length; i++) {
    		if (domainName.equals(domains[i].getName())) {
    			
    			String message = Messages.getString("wamt.clientAPI.Device.cantDelDom",domainName);
    			throw new InvalidParameterException(message,"wamt.clientAPI.Device.cantDelDom");
    		}
    	}
    	
    	//Send a signal, since a domain is about to be deleted.
    	boolean wasSuccessful = false;
        Signaler signal = new Signaler(this, null, domainName);
        signal.sendStart();

        //Delete the domain
        try {
        	DeviceContext dc = this.getDeviceContext();
            Commands commands = this.getCommands();
            commands.deleteDomain(dc, domainName);
            wasSuccessful = true;
        } finally {
            signal.sendEnd(wasSuccessful);
        }
    	
        // Update the allDomainNames collection.  
        // Check if the name is in the list. If so, remove it
        if (this.allDomainNames.contains(domainName)){
            this.allDomainNames.remove(domainName);
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Stop managing a domain on the device. This will destroy the domain 
     * object in the manager and remove it from persistence but leave the device as-is.
     * 
     * @param domainName the domain to stop managing
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws NotEmptyException there was a problem deleting the domain
     * @throws InUseException there was a problem deleting the domain
     * @throws InvalidParameterException there was a problem deleting the domain
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws NotExistException 
     * @throws NotEmptyException
     *  
     */
    public void removeManagedDomain(String domainName) throws DeletedException, NotExistException, LockBusyException, DatastoreException, InUseException, InvalidParameterException, NotEmptyException  {
        final String METHOD_NAME = "removeManagedDomain(String)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {domainName, this});

        Domain domain = this.getManagedDomain(domainName);
        if(domain == null){
        	String message = Messages.getString("NoPersistence");
        	NotExistException e = new NotExistException(message,"NoPersistence"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        
        this.lockWait();
        try {
            this.untrackOperationStatusOfDomain(domain);    
            
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting from persistence"); //$NON-NLS-1$
            StoredDomain storedDomain = domain.getStoredInstance();
            this.getStoredInstance().remove(storedDomain);  //removes from stored Device
            Manager manager = Manager.internalGetInstance();
            
            domain.destroy();
            //domain.destroy deleted the stored instance
            manager.save(Manager.SAVE_UNFORCED);
        } finally {
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }    
    
    /**
     * Get the managed Domain with the specified domain name on this device.
     * 
     * @param targetDomainName the name of the target domain.
     * @return the managed Domain which has the specified domain name. If the
     *         targetDomainName is not managed or is not found, this method will
     *         return null.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public Domain getManagedDomain(String targetDomainName) throws DeletedException {
    	// Not supported for XC10
    	try {
			if(getDeviceType().equals(DeviceType.XC10)){
				throw new UnsupportedOperationException();
			}
		} catch (DeletedException e) {
		}
		
        Domain result = null;
        Domain[] domains = this.getManagedDomains();
        for (int i=0; i<domains.length; i++) {
            if (domains[i].getName().equals(targetDomainName)) {
                result = domains[i];
                break;
            }
        }
        return(result);
    }
    
    /**
     * Internal use only
     * 
     * @throws AMPException
     * @throws DeletedException
     */
    protected void refreshDomainList() throws AMPException, DeletedException {
        final String METHOD_NAME = "refreshDomainList"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        
        Commands commands = this.getCommands();
        DeviceContext deviceContext = this.getDeviceContext();
        
        String[] allDomainsArray = commands.getDomainList(deviceContext);
        // refresh domain status
        for ( String strDomain: allDomainsArray ) {
        	Domain domain = this.getManagedDomain(strDomain);
        	if ( domain != null ) { 
        		// only for managed domain. 
        		this.probeDomainStatus(domain);
        	}
        }

        StringCollection allDomainsCollection = new StringCollection(allDomainsArray);
        
        // defect #12892 - need to remove the domain tag if the domain does not exist on the appliance
        if ( this.allDomainNames != null ) { // only for domain existed
	        for ( int i=0; i < this.allDomainNames.size(); i++ ) {
	        	String domainName = this.allDomainNames.get(i);
	        	if ( !allDomainsCollection.contains(domainName) ) { // that domain is removed from the device
	        		Domain domain = this.getManagedDomain(domainName);
	            	if ( domain != null ) {
	            		try { // try to remove all tags
							domain.removeTags();
						} catch (DirtySaveException e) {
							logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
							//e.printStackTrace();
						} catch (DatastoreException e) {
							logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
							//e.printStackTrace();
						}
	            	}
	        	}
	        }
        }
        this.allDomainNames = allDomainsCollection;
        logger.exiting(CLASS_NAME, METHOD_NAME, allDomainsCollection);
    }
    
    /**
     * Internal use only
     * Get Device MetaInfo by trying out all versions of Commands Implementations
     * We are not sure which version of protocol is available now
     * so try out all the possibilities
     */
    private DeviceMetaInfo getDeviceMetaInfo(DeviceContext deviceContext) 
    throws InvalidCredentialsException, AMPException {
    	final String METHOD_NAME = "getDeviceMetaInfo"; //$NON-NLS-1$
    	Commands commands = null;
    	DeviceMetaInfo metaInfo = null;

    	if(deviceContext.getAMPPort() == 22){
    		// it's possible that the device is a XC10
    		// try XC10 first
    		try {
				commands = getXC10Commands();
				metaInfo = commands.getDeviceMetaInfo(deviceContext);
				
				logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,"Using XC10 Provider"); //$NON-NLS-1$
				
				return metaInfo;
    		} catch (InvalidCredentialsException ice){
    			throw ice;
    		} catch (AMPException e) {
				// Failed to get XC10 provider, it's probably not an XC10
				// Go on to try DataPower providers
    			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Exception thrown when trying to retrieve XC10 device meta info" ,e);
			}
    	}
    	
		
    	// How to handle InvalidCredentialsException for DataPower devices ?
		try {
			commands = getV3Commands();
			metaInfo = commands.getDeviceMetaInfo(deviceContext);
			
			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,"Using AMP v3.0"); //$NON-NLS-1$
		} catch (AMPException e1) {
			// If it was a ConnectException, it's probably not a DataPower
    		Throwable cause = e1.getCause();
    		if (cause != null && cause instanceof ConnectException) {
				throw e1;
    		}
    		
    		try {
	        	// Try V2 commands if V3 failed.
        		commands = getV2Commands();
        		metaInfo = commands.getDeviceMetaInfo(deviceContext);
        		
        		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,"Using AMP v2.0"); //$NON-NLS-1$
        	} catch (AMPException e2 ) {
        		// Try V1 commands if V2 failed.
        		try {
        			commands = getV1Commands();
        			metaInfo = commands.getDeviceMetaInfo(deviceContext);
        			
        			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,"Using AMP v1.0"); //$NON-NLS-1$
        		} catch (AMPException e3) {
        			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Exception thrown when trying to retrieve device meta info using AMP v1" ,e3);
        			
        			throw e3;
        		} 	
        	}		    		
		}

    	return metaInfo;
    }
    
    Commands getV1Commands() throws AMPException {
        String commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_IMPL);
        String soapHelperClassNameImpl = Configuration.get(Configuration.KEY_SOAP_HELPER_IMPL);
        Commands commands = CommandFactory.getCommands(commandsClassNameImpl,
        			soapHelperClassNameImpl);
        
        return commands;
    }
    
    Commands getV2Commands() throws AMPException {
        String commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_V2_IMPL);
        String soapHelperClassNameImpl = Configuration.get(Configuration.KEY_SOAP_HELPER_V2_IMPL);
        Commands commands = CommandFactory.getCommands(commandsClassNameImpl,
        			soapHelperClassNameImpl);
        return commands;
    }
    
    Commands getV3Commands() throws AMPException {
        String commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_V3_IMPL);
        String soapHelperClassNameImpl = Configuration.get(Configuration.KEY_SOAP_HELPER_V3_IMPL);
        Commands commands = CommandFactory.getCommands(commandsClassNameImpl,
        			soapHelperClassNameImpl);
        return commands;
    }
    
    Commands getXC10Commands() throws AMPException {
        String commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_XC10_IMPL);
        Commands commands = CommandFactory.getCommands(commandsClassNameImpl, "");
        return commands;
    }
    
    /**
     * Internal use only
     */
    private void refreshCachedInfo() throws AMPException, DatastoreException, DeletedException {
        final String METHOD_NAME = "refreshCachedInfo"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);

        DeviceContext deviceContext = this.getDeviceContext();
        DeviceMetaInfo metaInfo = null;
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                "getDeviceMetaInfo for " + this.getPrimaryKey()); //$NON-NLS-1$
        
        metaInfo = getDeviceMetaInfo(deviceContext);
        
        if(metaInfo == null){
        	// Can not get the metainfo here, something wrong with the AMP
        	String message = Messages.getString("wamt.clientAPI.Device.failedGetDeviceMetaInfo");
        	AMPException ae = new AMPException(message);
        	throw ae;
        }else{
        	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "DeviceMetaInfo received: " + metaInfo); //$NON-NLS-1$
        }
        
        // got DeviceMetaInfo successfully, assume the device is up
        isDeviceReachable = true;
        
        // assume that serialNumber, deviceType, will not change
        
        // update featureLicenses                
        StringCollection featureLicenses = metaInfo.getFeatureLicenses();        
        if ( !this.getFeatureLicenses().equals(featureLicenses) ) {
        	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "discovered updated feature licenses: " + featureLicenses); //$NON-NLS-1$
        	this.getStoredInstance().setFeatureLicenses(featureLicenses);
        }
        
        // update guiPort
        int guiPort = metaInfo.getWebGUIPort();
        if (this.getGUIPort() != guiPort) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "discovered updated WebGUI port: " + guiPort); //$NON-NLS-1$
            this.getStoredInstance().setGUIPort(guiPort);
        }
        
        // update AMP version
        String ampVersion = metaInfo.getCurrentAMPVersion();
        if (this.getCurrentAMPVersion() != ampVersion) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "discovered updated current AMP version: " + ampVersion); //$NON-NLS-1$
            this.getStoredInstance().setCurrentAMPVersion(ampVersion);
        }
        
        // update actualFirmwareLevel
        String newFirmwareLevel = metaInfo.getFirmwareLevel();
        // special handling for XC10 firmware level
        int index = newFirmwareLevel.indexOf('-');
        if(index > 0){
        	newFirmwareLevel = newFirmwareLevel.substring(0, index);
        }
        String oldFirmwareLevel = this.getActualFirmwareLevel();
        if (!newFirmwareLevel.equals(oldFirmwareLevel)) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "discovered updated firmware level, " + //$NON-NLS-1$
                        "was " + oldFirmwareLevel + " now " + newFirmwareLevel); //$NON-NLS-1$ //$NON-NLS-2$
            this.setActualFirmwareLevel(newFirmwareLevel);
        }
        
        // save supported commands in this device object
        this.supportedCommands = metaInfo.getSupportedCommands();
        
        if(metaInfo.getSecurebackup()!=null && metaInfo.getSecurebackup().equals("enabled"))
			secureBackupSupported = true;
        else
        	secureBackupSupported = false;
        
        // update allDomainNames
        if(!DeviceType.XC10.equals(metaInfo.getDeviceType())){
        	this.refreshDomainList();
    	}
        
        ManagedSet managedSet = this.getManagedSet();
        // update Status of managed domains
        if (managedSet != null) {
        	if(!DeviceType.XC10.equals(metaInfo.getDeviceType())){
        		Domain[] domains = getManagedDomains();
        		for (int i=0; i<domains.length; i++) {
        			this.probeDomainStatus(domains[i]);
        		}
        	}
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get a list of operations that are supported on this device.
     * 
     * @return a list of ManagementOperations objects
     */
    public ManagementOperations[] getSupportedOperations(){
    	ArrayList<ManagementOperations> opList = new ArrayList<ManagementOperations>();
    	
    	if(isCommandSupported(Commands.SET_DOMAIN))
    		opList.add(ManagementOperations.DOMAIN_CONFIG_MANAGEMENT);
    	if(isCommandSupported(Commands.SET_FIRMWARE_STREAM))
    		opList.add(ManagementOperations.FIRMWARE_UPDATE);
    	if(isCommandSupported(Commands.BACKUP_DEVICE) && isCommandSupported(Commands.RESTORE_DEVICE))
    		opList.add(ManagementOperations.BACKUP_RESTORE);
    	if(isCommandSupported(Commands.GET_SERVICE_LIST_FROM_DOMAIN))
    		opList.add(ManagementOperations.SERVICE_CONFIG_MANAGEMENT);
    	if(isCommandSupported(Commands.QUIESCE_DEVICE) && isCommandSupported(Commands.UNQUIESCE_DEVICE))
    		opList.add(ManagementOperations.DEVICE_QUIESCE_UNQUIESCE);
    	if(isCommandSupported(Commands.QUIESCE_DOMAIN) && isCommandSupported(Commands.UNQUIESCE_DOMAIN))
    		opList.add(ManagementOperations.DOMAIN_QUIESCE_UNQUIESCE);
    	
    	return opList.toArray(new ManagementOperations[]{});
    }
    
    /**
     * Check if a command is supported on this device
     */
    private boolean isCommandSupported(int command){
    	if(supportedCommands != null){
    		for(int cmd : supportedCommands){
    			if( command == cmd){
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Internal use only
     */
    void recoverFromLostSubscription(ProgressContainer progressContainer) throws DeletedException, AMPException, DatastoreException {
        try {
            this.subscribe(progressContainer);
        } catch (SubscriptionToAnotherManagerException e) {
            this.handleSubscriptionToAnotherManager(progressContainer);
            progressContainer.setError(e);
        }
        
        progressContainer.incrementCurrentStep(1, "wamt.clientAPI.Device.refreshDevCache_txt"); //$NON-NLS-1$
        this.refreshCachedInfo();
    }
    
    /**
     * Internal use only
     */
    void handleSubscriptionToAnotherManager(ProgressContainer progressContainer) {
        final String METHOD_NAME = "handleSubscriptionToAnotherManager"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        // if this device is not in a managed set, do nothing
        ManagedSet managedSet = null;
        try {
            managedSet = this.getManagedSet();
        } catch (DeletedException e) {
            managedSet = null;
        }
        if (managedSet == null) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "Device is not in a Managed Set: " + this); //$NON-NLS-1$
            return;
        }
        
        // Switch to use secondary subscription ID
        isPrimary = false;
        try {
			subscribe(progressContainer);
			
			progressContainer.setComplete();
		} catch (DeletedException e) {
			progressContainer.setError(e);
		} catch (SubscriptionToAnotherManagerException e) {
			// there is already some other manager subscribed as secondary
			// with the same host and port number 
			// nothing can be done here. Just fail it.
			progressContainer.setError(e);
		} catch (AMPException e) {
			// subscription failed
			progressContainer.setError(e);
		}

        if(progressContainer.hasError()){
        	managedSet.lockWait();  //lock managedSet since the device is to be removed from the MS
        	try {
        		try {
        			managedSet.removeDeviceWithoutUnsubscribe(this);
        		} catch (DMgrException e) {
        			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            	"unable to remove device " + this + //$NON-NLS-1$
                            	" from Managed Set " + managedSet + " but will continue.", e); //$NON-NLS-1$ //$NON-NLS-2$
        		}
        	} finally {
        		managedSet.unlock();
        	}
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Internal use only
     */
    private DeviceContext getDeviceContext(String hostname, int hlmPort,
                                           String userid, String password) {
        final String METHOD_NAME = "getDeviceContext(String...)"; //$NON-NLS-1$
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "creating DeviceContext from parms"); //$NON-NLS-1$
        this.cachedDeviceContext = 
            new DeviceContext(hostname, hlmPort, userid, password);
        return(this.cachedDeviceContext);
    }

    /**
     * Returns the DeviceContext for this device, necessary for AMP calls.
     * 
     * @throws AMPException
     * @throws DeletedException
     */
    public DeviceContext getDeviceContext() throws DeletedException {
        final String METHOD_NAME = "getDeviceContext()"; //$NON-NLS-1$
        if (this.cachedDeviceContext == null) {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "creating DeviceContext from Device"); //$NON-NLS-1$
            this.cachedDeviceContext = 
                new DeviceContext(this.getStoredInstance().getHostname(), 
                                  this.getStoredInstance().getHLMPort(),
                                  this.getStoredInstance().getUserId(),
                                  this.getStoredInstance().getPassword());
        }
        return(this.cachedDeviceContext);
    }
    
    /**
     * Get the unique ID of this device, which is its hardware serial number.
     * The serial number is a unique value hardcoded inside the device, and is
     * the same serial number that appears in the WebGUI. You can not set the
     * serial number, it is automatically loaded from the device when it is
     * added to the Manager. This ID is the primary key of this object in the
     * manager. The ID is immutable, so there is no public
     * <code>setID(String)</code> method.
     * 
     * @return the device's unique hardware serial number. Can be null if the
     *         device ID hasn't been set yet, which can occur before the device
     *         has been added to the manager.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public String getSerialNumber() throws DeletedException {
        return(this.getStoredInstance().getSerialNumber());
    }
    
    /* javadoc inherited from interface */
    /**
     * Returns the primary key of this device as a String
     */
    public String getPrimaryKey() throws DeletedException {
        return(this.getStoredInstance().getPrimaryKey());
    }
    
    /**
     * Retrieve a reference to the device that has the specified primary key.
     * 
     * Internal use only
     * 
     * @param targetKey the primary key to search for
     * @return the device that has the specified primary key. May return
     *         <code>null</code> if no device with the specified primary key
     *         was found.
     * @see #getPrimaryKey()
     */
    public static Device getByPrimaryKey(String targetKey) {
        Device result = null;
        Manager manager = Manager.internalGetInstance();
        Device[] devices = manager.getAllDevices();
        for (int i=0; i<devices.length; i++) {
            String key = null;
            try {
                key = devices[i].getPrimaryKey();
            } catch (DeletedException e) {
                key = ""; //$NON-NLS-1$
            }
            if (key.equals(targetKey)) {
                result = devices[i];
                break;
            }
        }
        return(result);
    }
    
    /**
     * Part of the Persistable interface.
     * 
     * Internal use only
     * 
     * @return a reference to the persisted object
     * @throws DeletedException
     */
    StoredDevice getStoredInstance() throws DeletedException {
        final String METHOD_NAME = "getStoredInstance"; //$NON-NLS-1$
        if (this.persistence == null) {
        	String message = Messages.getString("wamt.clientAPI.Device.devDeleted");
            DeletedException e = new DeletedException(message,"wamt.clientAPI.Device.devDeleted"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
            throw(e);
        }
        return(this.persistence);
    }
    
    /**
     * Destroy the device object and all of its children.
     * 
     * Part of the Persistable interface.  
     * 
     * @throws NotExistException
     * @throws InUseException
     * @throws InvalidParameterException
     * @throws NotEmptyException
     * @throws DatastoreException
     * @throws LockBusyException
     * @throws DeletedException
     * @throws FullException 
     */
    void destroy() throws NotExistException, InUseException, InvalidParameterException, 
    NotEmptyException, LockBusyException, DatastoreException, DeletedException, FullException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Manager manager = Manager.internalGetInstance();
        manager.unmanagedDevicesLockNoWait();

        // get the lock on this Device and then we can destroy everything in it.
        this.lockWait();
        try {
            
            // delete child objects: domains since they don't live outside this device
            Domain[] domains = this.getManagedDomains();
            for (int i=0; i<domains.length; i++) {
                this.removeManagedDomain(domains[i].getName());
            }
            
            // delete from persistence
            ManagedSet managedSet = null;
            try {
                managedSet = this.getManagedSet();
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "managed set deleted for device: " + this, e); //$NON-NLS-1$
                managedSet = null;
            }
            if (managedSet != null) {
                try {
                    managedSet.removeDevice(this);
                } catch (NotExistException e) {
                    // that's ok, we wanted it to go away anyway
                } 
            }
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.remove(this.getStoredInstance());
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "deleting device from persistence"); //$NON-NLS-1$
            this.getStoredInstance().delete();
            this.persistence = null;
            
            // clear any references
        } finally {
            manager.unmanagedDevicesUnlock();
            this.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get the symbolic name of this Device. This name is designed to be
     * human-consumable.
     * 
     * @return the device Name
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setSymbolicName(String)
     */
    public String getSymbolicName() throws DeletedException {
        return(this.getStoredInstance().getSymbolicName());
    }
    
    /**
     * Set the symbolic name of this Device. This name is designed to be
     * human-consumable. This name must be unique, but it is mutable. The 
     * symbolic name is typically set when the method 
     * createDevice(String, String, String, String, int) is called
     * 
     * @param name the device name
     * @see #getSymbolicName()
     * @throws AlreadyExistsInRepositoryException the symbolic name specified is not unique
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public void setSymbolicName(String name) 
        throws AlreadyExistsInRepositoryException, AlreadyExistsInRepositoryException, DeletedException, DatastoreException {
        final String METHOD_NAME = "setSymbolicName"; //$NON-NLS-1$
        Manager manager = Manager.internalGetInstance();
        
        Device targetDevice = null;
        
        try {
        	// Try to get the device of the same name from manager
			targetDevice = manager.getDeviceBySymbolicName(name);
		} catch (DeletedException e1) {
			// device does not exist, just ignore it
		}
        
		// It won't be a problem if setting a same name to an existing device
        if (!this.equals(targetDevice) && manager.containsDeviceSymbolicName(name)) {
            String message = Messages.getString("wamt.clientAPI.Device.nameExists",name); //$NON-NLS-1$
            AlreadyExistsInRepositoryException e = new AlreadyExistsInRepositoryException(message,"wamt.clientAPI.Device.nameExists",name);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            // make sure it appears in the log, throwing() is FINER
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting symbolicName on device " + this + " to " + name); //$NON-NLS-1$ //$NON-NLS-2$
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setSymbolicName(name);       
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Get the hostname or IP address of this device.
     * 
     * @return the device's hostname or IP address
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setHostname(String)
     */
    public String getHostname() throws DeletedException {
        return(this.getStoredInstance().getHostname());
    }
    
    /**
     * Set the hostname or IP address of this device. This value is used when
     * the manager attempts to communicate with the device via AMP.
     * 
     * @param hostname the new hostname or IP address for this device
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getHostname()
     */
    public void setHostname(String hostname) throws DeletedException, DatastoreException, AlreadyExistsException {
        final String METHOD_NAME = "setHostname"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting hostname on device " + this + " to " + hostname); //$NON-NLS-1$ //$NON-NLS-2$
        Manager manager = Manager.internalGetInstance();
        // Check hostname:port if exists
        if ( manager.containsHostnamePort(hostname+":"+this.getHLMPort()) ) {
        	Object[] args = new Object[] {hostname, this.getHLMPort()};
        	String message = Messages.getString("wamt.clientAPI.Device.HostnamePortExists", args); //$NON-NLS-1$
            AlreadyExistsException e = new AlreadyExistsException(message,"wamt.clientAPI.Device.HostnamePortExists", args);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setHostname(hostname);
        this.cachedDeviceContext = null;
		         
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Get the administrative userid of this device.
     * 
     * @return the device's administrative userid.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setUserId(String)
     */
    public String getUserId() throws DeletedException {
        return(this.getStoredInstance().getUserId());
    }

    /**
     * Set the administrative userid for this device. This value is used when
     * the manager attempts to communicate with the device via AMP, so it should
     * have administrative privileges on the device.
     * 
     * @param userid the new administrative userid
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getUserId()
     */
    public void setUserId(String userid) throws DeletedException, DatastoreException {
        final String METHOD_NAME = "setUserId"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting userid on device " + this + " to " + userid); //$NON-NLS-1$ //$NON-NLS-2$
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setUserId(userid);
        this.cachedDeviceContext = null;
		Manager manager = Manager.internalGetInstance();         
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Invoked by the manager for AMP.
     * 
     * @return the administrative password
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setPassword(String)
     */
    String getPassword() throws DeletedException {
        return(this.getStoredInstance().getPassword());
    }
    
    /**
     * Set the password for the administrative userid for this device. This
     * value is used when the manager attempts to communicate with the device
     * via AMP. For security reasons, there is no public
     * <code>getPassword()</code> method.
     * 
     * @param password the new administrative password
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public void setPassword(String password) throws DeletedException, DatastoreException {
        final String METHOD_NAME = "setPassword"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting password on device " + this +  //$NON-NLS-1$
                    " to a new value of length " + password.length()); //$NON-NLS-1$
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setPassword(password);
        this.cachedDeviceContext = null;
		Manager manager = Manager.internalGetInstance();         
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Get the device's port number for AMP (HLM) communication.
     * 
     * @return the device's port number for AMP (HLM) communication
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setHLMPort(int)
     */
    public int getHLMPort() throws DeletedException {
        return(this.getStoredInstance().getHLMPort());
    }
    
    /**
     * Set the device's port number to which the AMP (HLM) client will attempt
     * to connect. This is generally the device's XML Management Interface that
     * has the AMP endpoint enabled.
     * 
     * @param hlmPort the device's port number for AMP (HLM) communication.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getHLMPort()
     */
    public void setHLMPort(int hlmPort) throws DeletedException, DatastoreException, AlreadyExistsException {
        final String METHOD_NAME = "setHLMPort"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting hlmPort on device " + this + " to " + hlmPort); //$NON-NLS-1$ //$NON-NLS-2$
		Manager manager = Manager.internalGetInstance();
        // Check hostname:port if exists
        if ( manager.containsHostnamePort(this.getHostname()+":"+hlmPort) ) {
        	Object[] args = new Object[] {this.getHostname(), hlmPort};
        	String message = Messages.getString("wamt.clientAPI.Device.HostnamePortExists", args); //$NON-NLS-1$
            AlreadyExistsException e = new AlreadyExistsException(message,"wamt.clientAPI.Device.HostnamePortExists", args);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }        
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setHLMPort(hlmPort);
        this.cachedDeviceContext = null;         
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Get the device's port for the WebGUI.
     * 
     * @return the device's port for the WebGUI
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #setGUIPort(int)
     */
    public int getGUIPort() throws DeletedException {
        return(this.getStoredInstance().getGUIPort());
    }
    
    /**
     * Set the device's port for the WebGUI. This value may be used for
     * automatically launching an external web browser to the device's WebGUI.
     * This value is automatically populated when a device is added to the
     * manager by probing the device cached info, so you do not need to invoke this
     * method unless there is a special reason to do so.
     * 
     * @param guiPort the device's port for the WebGUI
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getGUIPort()
     */
    public void setGUIPort(int guiPort) throws DeletedException, DatastoreException {
        final String METHOD_NAME = "setGUIPort"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting guiPort on device "  + this + " to " + guiPort); //$NON-NLS-1$ //$NON-NLS-2$
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setGUIPort(guiPort);
		Manager manager = Manager.internalGetInstance();         
		manager.save(Manager.SAVE_UNFORCED);     
    }
    
    /**
     * Get the device type. DeviceType is immutable, so there is no
     * setDeviceType method.
     * 
     * @return the device's device type, i.e., "XS35", "XS40", "XI50"
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public DeviceType getDeviceType() throws DeletedException {
        return(this.getStoredInstance().getDeviceType());
    }
    
    /**
     * Returns the current AMP version supported by this device. 
     * The AMP version is the string representation of the numeric version.
     * The AMP version supported is determined by the firmware.
     * @return String
     * @throws DeletedException
     */
    public String getCurrentAMPVersion() throws DeletedException {
        return(this.getStoredInstance().getCurrentAMPVersion());
    }
    
    /**
     * Get the device's model type. ModelType is immutable, so there is no
     * setModelType method.
     * 
     * @return the device's model type, i.e., "9003", etc.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public ModelType getModelType() throws DeletedException {
        return(this.getStoredInstance().getModelType());
    }
    
    /**
     * Get the list of Strings that represent the feature entitlements (licensed
     * features) for this device. This list is a union of the licensed strict
     * and non-strict features.
     * 
     * @return the list of Strings that represent the feature entitlements for
     *         this device, i.e., "MQ", "TAM", "Tibco-EMS", etc.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public StringCollection getFeatureLicenses() throws DeletedException {
        return(this.getStoredInstance().getFeatureLicenses());
    }
    
    /**
     * Set the list of Strings that represent the feature entitlements for this
     * device.
     * 
     * @param featureLicenses the list of Strings that represent the feature
     *        entitlements for this device, i.e., "MQ", "TAM", "Tibco-EMS", etc.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getFeatureLicenses()
     */
    void setFeatureLicenses(StringCollection featureLicenses) throws DeletedException, DatastoreException {
        final String METHOD_NAME = "setFeatureLicenses"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting feature licenses on device " + this + " to " + featureLicenses); //$NON-NLS-1$ //$NON-NLS-2$
        // don't worry about locking on this field, it's simple
        this.getStoredInstance().setFeatureLicenses(featureLicenses);
		Manager manager = Manager.internalGetInstance();         
		manager.save(Manager.SAVE_UNFORCED);     
    }

    /**
     * Return this device's {@link ManagedSet} membership.
     * 
     * @return the ManagedSet this device's ManagedSet. If the device is
     *         not a member of any ManagedSet, then this value will be null.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see ManagedSet#addDevice(Device)
     * @see "useCases section 4.2"
     */
    public ManagedSet getManagedSet() throws DeletedException {
        ManagedSet result = null;
        StoredManagedSet storedManagedSet = this.getStoredInstance().getManagedSet();
        if (storedManagedSet != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(storedManagedSet);
        }
        return(result);
    }

    /* 
     * setMembership is not needed here, because that is taken care of
     * via Manager.add(Device)
     */
    
    /**
     * Get a list of all the domains on the specified device, including those
     * that are not managed. Managed domains should be deleted (unmanaged) via
     * {@link ManagedSet#removeManagedDomain(String)}. This list is cached from the
     * device from the last query.
     * 
     * @return a List of Strings representing all the domains on this device
     * @see ManagedSet#removeManagedDomain(String)
     */
    public StringCollection getAllDomainNames() {
        return(this.allDomainNames);
    }
    
    /**
     * To be invoked by the heartbeat daemon.
     * 
     * Internal use only
     * 
     * @param newNames
     */
    void setAllDomainNames(StringCollection newNames) {
        this.allDomainNames = newNames;
        
    }
    
    /**
     * To be invoked by Domain.deployConfiguration()
     * 
     * Internal use only
     * 
     * @param newNames
     */
    void addDomainName(String name) {
    	if (!this.allDomainNames.contains(name)){
            this.allDomainNames.add(name);
    	}
    }    
    
////    /** 
////     * TODO wjong see bug 27900, mgmt status is part of post 381 cleanup work
////     * Get the management status of a managed domain.
////     * 
////     * @param domain the domain to get the management status of
////     * @return the management status of a managed domain.
////     * @throws DeletedException this object has been deleted from the persisted
////     *         repository. The referenced object is no longer valid. You should
////     *         not be using a reference to this object.
////     * @see #getOperationStatusOfDomain(Domain)
////     */
//    public ManagementStatus getManagementStatusOfDomain(Domain domain) 
//    throws DeletedException {
//        
//        final String METHOD_NAME = "getManagementStatusOfDomain"; //$NON-NLS-1$
//        logger.entering(CLASS_NAME, METHOD_NAME, domain);
//
//        // get the stored value for this domain on this device
//        ManagementStatus current = 
//            (ManagementStatus) this.managedDomainsManagementStatus.get(domain.getName());
//        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "current management status: " + current.toString()); //$NON-NLS-1$
//        // don't change "current"
//        ManagementStatus result = new ManagementStatus(current.getEnum());
//        
//        // check if managed
//        ManagedSet managedSet = this.getManagedSet();
//        if (managedSet != null) {
//            // is managed, so now peek into the BackgroundTask Queue to see what is pending
//            Collection collection = new Vector();
//            for (int i=0; i<managedSet.backgroundTaskQueueSize(); i++) {
//                Object object = managedSet.backgroundTaskQueuePeek(i);
//                if ((object != null) && (object instanceof BackgroundTask)) {
//                    BackgroundTask backgroundTask = (BackgroundTask) object;
//                    if (backgroundTask.affectsDomains()) {
//                        String singleAffectedDomain = backgroundTask.getSingleAffectedDomain();
//                        Device singleAffectedDevice = backgroundTask.getSingleAffectedDevice();
//                        if ((singleAffectedDomain == null) ||
//                            (singleAffectedDomain.equals(domain.getName()))) {
//                            if ((singleAffectedDevice == null) ||
//                                (singleAffectedDevice == this)) {
//                                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
//                                        "found queued PENDING effect: " + backgroundTask); //$NON-NLS-1$
//                                collection.add(new ManagementStatus(ManagementStatus.Enumerated.PENDING));
//                            }
//                        }
//                    }
//                }
//            }
//            // do a rollup of all of them
//            collection.add(current);
//            ManagementStatus[] managementStatii = ManagementStatus.toManagementStatusArray(collection);
//            result.rollupFrom(managementStatii);
//        }
//        logger.exiting(CLASS_NAME, METHOD_NAME, result);
//        return(result);
//    }
//    
//    void setManagementStatusOfDomain(Domain domain, ManagementStatus.Enumerated newEnum)
//        throws DeletedException {
//        final String METHOD_NAME = "setManagementStatusOfDomain"; //$NON-NLS-1$
//        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
//                    "for device " + this +  //$NON-NLS-1$
//                    " setting ManagementStatus of domain " + domain + " to " + newEnum); //$NON-NLS-1$ //$NON-NLS-2$
//        if (!this.managementStatusIsTracked(domain)) {
//            this.trackManagementStatusOfDomain(domain);
//        }
//        ManagementStatus currentStatus = 
//            (ManagementStatus) this.managedDomainsManagementStatus.get(domain.getName());
//        currentStatus.setStatus(newEnum);
//    }
//    
//    void trackManagementStatusOfDomain(Domain domain) throws DeletedException {
//        final String METHOD_NAME = "trackManagementStatusOfDomain"; //$NON-NLS-1$
//        // check that Domain exists
//        StoredDomain storedDomain = domain.getStoredInstance();
//        if (storedDomain == null) {
//            // debug statement
//        }
//        // get the object
//        if (!this.managedDomainsManagementStatus.containsKey(domain.getName())) {
//            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
//                        "adding ManagementStatus object for domain " + domain + //$NON-NLS-1$
//                        " in " + this); //$NON-NLS-1$
//            this.managedDomainsManagementStatus.put(
//                        domain.getName(), 
//                        new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN));
//        } else {
//            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
//                        "already have ManagementStatus object for domain " + domain + //$NON-NLS-1$
//                        " in " + this); //$NON-NLS-1$
//        }
//    }
//    
//    void untrackManagementStatusOfDomain(Domain domain) throws DeletedException {
//        final String METHOD_NAME = "untrackManagementStatusOfDomain"; //$NON-NLS-1$
//        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
//                    "removing ManagementStatus object for domain " + domain + //$NON-NLS-1$
//                    " in " + this); //$NON-NLS-1$
//        this.managedDomainsManagementStatus.remove(domain.getName());
//    }
//    
//    boolean managementStatusIsTracked(Domain domain) throws DeletedException {
//        if (this.managedDomainsManagementStatus.containsKey(domain.getName())) {
//            return(true);
//        } else {
//            return(false);
//        }
//    }

    /**
     * Get the operation status of a managed domain.
     * 
     * @param domain the domain to get the operation status of
     * @return the operation status of a managed domain
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     */
    public OperationStatus getOperationStatusOfDomain(Domain domain) 
        throws DeletedException {
        OperationStatus result = 
            (OperationStatus) this.managedDomainsStatus.get(domain.getName()).getOperationStatus();
        return(result);
    }
    
    /**
     * Sets the operation status of the specified domain.
     * 
     * @param domain the domain to set the operation status of This method
     *        copies the value, it does not point to the parameter object.
     * @param newStatus the container of the new status value
     * @throws NotExistException
     * @throws DeletedException
     * @see #getOperationStatusOfDomain(Domain)
     */
    void setDomainStatus(Domain domain, DomainStatus newStatus)
        throws DeletedException {
        final String METHOD_NAME = "setDomainStatus"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting Status of domain " + domain + //$NON-NLS-1$
                    " to " + newStatus + " in " + this); //$NON-NLS-1$ //$NON-NLS-2$
        if (!this.StatusIsTracked(domain)) {
            this.trackStatusOfDomain(domain);
        }
        DomainStatus status = (DomainStatus) this.managedDomainsStatus.get(domain.getName());
        status.setStatus(newStatus);
        
    }
    
    void trackStatusOfDomain(Domain domain) throws DeletedException {
        final String METHOD_NAME = "trackStatusOfDomain"; //$NON-NLS-1$
        // check that Domain exists
        StoredDomain storedDomain = domain.getStoredInstance();
        if (storedDomain == null) {
            // debug statement
        }
        // get the object
        if (!this.managedDomainsStatus.containsKey(domain.getName())) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "adding DomainStatus object for domain " + domain + //$NON-NLS-1$
                        " in " + this); //$NON-NLS-1$

            this.managedDomainsStatus.put(domain.getName(), DomainStatus.UNKNOWN_DOMAIN_STATUS);
        } else {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "already have DomainStatus object for domain " + domain + //$NON-NLS-1$
                        " in " + this); //$NON-NLS-1$
        }

    }
    
    void untrackOperationStatusOfDomain(Domain domain) throws DeletedException {
        final String METHOD_NAME = "untrackOperationStatusOfDomain"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "removing OperationStatus object for domain " + domain + //$NON-NLS-1$
                    " in " + this); //$NON-NLS-1$
        this.managedDomainsStatus.remove(domain.getName());
    }
    
    boolean StatusIsTracked(Domain domain) throws DeletedException {
        if (this.managedDomainsStatus.containsKey(domain.getName())) {
            return(true);
        } else {
            return(false);
        }
    }

    void probeDomainStatus(Domain domain) throws DeletedException {
        final String METHOD_NAME = "probeOperationStatusOfDomain"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, domain});
        
        Commands commands = null;
        DomainStatus domainStatus = null;
       
        try {
			commands = this.getCommands();
		} catch (AMPException ae) {
			domainStatus = DomainStatus.UNKNOWN_DOMAIN_STATUS;
       		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Failed to get Commands object. Domain status not updated; probeStatusOfDomain(domain) failed. HeartbeatTask task will try again.");
		}
     
        if(commands != null){
        	DeviceContext deviceContext = this.getDeviceContext();
        
        	try {
        		domainStatus = commands.getDomainStatus(deviceContext, domain.getName());
        	} catch (com.ibm.datapower.amt.amp.NotExistException e) {
        		// The domain doesn't exist on the device, so set the status to "unknown".
        		// This may happen if the device was just added to the ManagedSet and
        		// doesn't already have the domain .
        		domainStatus = DomainStatus.UNKNOWN_DOMAIN_STATUS;
       		
        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Domain does not exist on device. Domain status not updated; probeOperationStatusOfDomain(domain) failed. HeartbeatTask task will try again.");
        	} catch (AMPException ae){
        		domainStatus = DomainStatus.UNKNOWN_DOMAIN_STATUS;
        		
        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Failed to get domain status. Domain status not updated; probeOperationStatusOfDomain(domain) failed. HeartbeatTask task will try again.");
        	}
        }
        
        this.managedDomainsStatus.put(domain.getName(), domainStatus);
        logger.exiting(CLASS_NAME, METHOD_NAME, domainStatus);
    }

    /**
     * Get the firmware level that is currently deployed to this device. In
     * general, all the devices in a managedSet should have the same level of
     * firmware, but situations may cause this to be false. This should
     * reflect the actual firmware that is currently deployed to this device. 
     * The returned value was cached from the device from the last query.
     * 
     * @return the firmware level that is currently deployed to this device
     */
    public String getActualFirmwareLevel() {
        return(this.actualFirmwareLevel);
    }
    
    /**
     * Compare a specific firmware level (represented as a string) to the 
     * the firmware level that is currently deployed to this device. This is 
     * useful for checking to see if the device has a firmware level greater 
     * than or equal to a level required by a particular feature.
     * 
     * @param minimumLevel The firmware level required by a certain feature
     * @return a boolean result from the comparison. If the device's firmware is 
     *         greater than or equal to minimumLevel then the return value is true, otherwise 
     *         the return value is false.
     */
    public boolean meetsMinimumFirmwareLevel(String minimumLevel) {
    	String actualFirmwareLevel = getActualFirmwareLevel();
    	if (minimumLevel == null || actualFirmwareLevel == null){
    		// this can occur when manager cannot connect to device
    		return false;
    	}
        return FirmwareVersion.meetsMinimumLevel(minimumLevel, getActualFirmwareLevel());
    }
    
    /**
     * Set the firmware version that is deployed to this device.
     * 
     * @param newLevel the firmware version that is currently deployed to this
     *        device
     * @see #getActualFirmwareLevel()
     */
    protected void setActualFirmwareLevel(String newLevel) {
        final String METHOD_NAME = "setActualFirmwareLevel"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting actualFirmwareLersion of " + this + //$NON-NLS-1$
                    " to " + newLevel); //$NON-NLS-1$
        this.actualFirmwareLevel = newLevel;

    }
    
    /**
     * Retrieve the actual firmware level from the device instead of trusting the 
     * cached copy. This is a synchronous call that goes out to the device, so 
     * it may take a while.
     * 
     * @param progressContainer
     * @return
     * @throws AMPException
     * @throws DeletedException
     * @throws DatastoreException
     */
    private String retrieveActualFirmwareLevel(ProgressContainer progressContainer) 
        throws AMPException, DeletedException, DatastoreException {
        final String METHOD_NAME = "retrieveActualFirmwareLevel"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        String result = null;

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "getDeviceMetaInfo to " + this); //$NON-NLS-1$
        progressContainer.incrementCurrentStep(1,"wamt.clientAPI.Device.detFWonDev_txt",this.getDisplayName()); //$NON-NLS-1$
        Commands commands = this.getCommands();
        DeviceContext deviceContext = this.getDeviceContext();
        DeviceMetaInfo deviceMetaInfo = commands.getDeviceMetaInfo(deviceContext);
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "received DeviceMetaInfo: " + deviceMetaInfo); //$NON-NLS-1$
        
        result = deviceMetaInfo.getFirmwareLevel();
        
        // cache it in the device too
        this.setActualFirmwareLevel(result);
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return(result);
    }
    
    /**
     * Get the management status of the firmware. Firmware does not have an
     * operational status, only management status.
     * 
     * @return the management status of the firmware.
     */
    public ManagementStatus getManagementStatusOfFirmware() {
        // check the locally stored value
        ManagementStatus current = this.firmwareManagementStatus;
        ManagementStatus result = current;
        
//        // check if managed
//        ManagedSet managedSet = null;
//        try {
//            managedSet = this.getManagedSet();
//        } catch (DeletedException e) {
//            managedSet = null;
//        }
//        if (managedSet != null) {
//            // is managed, so now peek into the BackgroundTask Queue to see what is pending
//            Collection collection = new Vector();
//            for (int i=0; i<managedSet.backgroundTaskQueueSize(); i++) {
//                Object object = managedSet.backgroundTaskQueuePeek(i);
//                if ((object != null) && (object instanceof BackgroundTask)) {
//                    BackgroundTask backgroundTask = (BackgroundTask) object;
//                    if (backgroundTask.affectsFirmware()) {
//                        Device singleAffectedDevice = backgroundTask.getSingleAffectedDevice();
//                        if ((singleAffectedDevice == null) ||
//                            (singleAffectedDevice == this)) {
//                            collection.add(new ManagementStatus(ManagementStatus.Enumerated.PENDING));
//                        }
//                    }
//                }
//            }
//            // do a rollup of all of them
//            collection.add(current);
//            ManagementStatus[] managementStatii = ManagementStatus.toManagementStatusArray(collection);
//            // don't change "current"
//            result = new ManagementStatus(current.getEnum());
//            result.rollupFrom(managementStatii);
//        }
        return(result);
    }
    
    /**
     * Get the management status of the device. The management status is for the device reboot
     * 
     * @return the management status of the device.
     */
    public ManagementStatus getManagementStatusOfDevice() {
        // check the locally stored value       
        return(this.deviceManagementStatus);
    }
    
    /**
     * Set the management status of the firmware.
     * 
     * @param newStatus the new management status of the firmware
     * @see #getManagementStatusOfFirmware()
     */
    protected void setManagementStatusOfFirmware(ManagementStatus.Enumerated newEnum) {
        final String METHOD_NAME = "setManagementStatusOfFirmware"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting ManagementStatus of firmware of " + this + //$NON-NLS-1$
                    " to " + newEnum); //$NON-NLS-1$
        this.firmwareManagementStatus.setStatus(newEnum);
        
    }
    
    protected void setManagementStatusOfDevice(ManagementStatus.Enumerated newEnum) {
        final String METHOD_NAME = "setManagementStatusOfDevice"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting ManagementStatus of device of " + this + //$NON-NLS-1$
                    " to " + newEnum); //$NON-NLS-1$
        this.deviceManagementStatus.setStatus(newEnum);
        
    }
    

//    /**
//     * Rollup the management status of the firmware and all the
//     * domains on this device, and return that aggregated value. This aggregated
//     * value should represent the management status of the device as a whole.
//     * 
//     * @return the aggregated management status of the firmware and
//     *         all the domains on this device
//     */
//    public ManagementStatus getRollupManagementStatus() {
//        Collection collection = new Vector(managedDomainsManagementStatus.values());
//        collection.add(firmwareManagementStatus);
//        ManagementStatus[] statusArray = ManagementStatus.toManagementStatusArray(collection);
//        ManagementStatus result = new ManagementStatus(ManagementStatus.Enumerated.UNKNOWN);
//        // the UNKNOWN value above will get overwritten by rollupFrom
//        result.rollupFrom(statusArray);
//        return(result);
//    }
    
    /**
     * Rollup the operation status of all the domains on this device, and return
     * that aggregated value.
     * 
     * @return the aggregated operation status of all the domains on this
     *         device. Firmware does not have an operation status. If
     *         there are no domains being managed, then consider the device's
     *         operation status to be "up", because we know everything
     *         ("unknown" doesn't apply) and nothing is down (don't need to
     *         alarm the customer).
     * 
     */
    public OperationStatus getRollupOperationStatus() {
    	// Not supported for XC10
    	try {
			if(getDeviceType().equals(DeviceType.XC10)){
				throw new UnsupportedOperationException();
			}
		} catch (DeletedException e) {
		}    	
    	
    	// Try to get the operationStatus map
    	Map<String, OperationStatus> managedDomainsOperationStatus = new HashMap<String, OperationStatus>();
    	Iterator<Map.Entry<String, DomainStatus>> iter = this.managedDomainsStatus.entrySet().iterator();
    	while (iter.hasNext()) {
    	     Map.Entry<String, DomainStatus> entry = (Map.Entry<String, DomainStatus>) iter.next();
    	     if ( entry != null ) {
    	         String domainName = (String) entry.getKey(); 
    	         OperationStatus opStatus = (OperationStatus) entry.getValue().getOperationStatus();
    	         if ( domainName != null && opStatus != null ) {
    	        	 managedDomainsOperationStatus.put(domainName, opStatus);
    	         }
    	     }
    	}
    	// 
        Collection<OperationStatus> collection = new Vector<OperationStatus>(managedDomainsOperationStatus.values());
        OperationStatus[] statusArray = OperationStatus.toOperationStatusArray(collection);
        // the UNKNOWN value above will get overwritten in rollupFrom()
        OperationStatus result = new OperationStatus(OperationStatus.Enumerated.UNKNOWN);
        if (statusArray.length == 0) {
            // no managed domains, but we will consider the device operation status == up
            result.setStatus(OperationStatus.Enumerated.UP);
        } else {
            // do a rollup of all the managed domains
            result.rollupFrom(statusArray);
        }
        return(result);
    }
    
    /**
     * Create a background task to get the URL of the device's WebGUI. This can
     * be used to direct the user's browser for on-the-glass integration. If the
     * device's administrator userid and password are known, it will also
     * generate and append a SAMLart so that the URL will automatically login
     * the browser to the WebGUI Control Panel. If there is a problem generating
     * the SAMLart then no automatic login to the WebGUI will be performed.
     * Because the login token (SAMLart) is generated on the device, this
     * operation is considered long running, which is why it is queued as a
     * background task.
     * 
     * @param domain if you wish to jump directly to a specific domain in the
     *        WebGUI, include the name of the domain here. If you do not wish to
     *        jump directly to a specific domain in the WebGUI, set this value
     *        to null.
     * @return a ProgressContainer for monitoring this long-running task. When
     *         the task has completed successfully you can retrieve a URL object
     *         from this ProgressContainer.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem reading from the datastore
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws FullException the queue for the background tasks for the
     *         ManagedSet of the device is already full. The background task was
     *         not queued. Try again later when there are fewer background tasks
     *         on the queue.
     * @see "useCases section 4.1"
     */
    public ProgressContainer getWebGUIURL(Domain domain)
        throws DeletedException, DatastoreException, LockBusyException, FullException {
        final String METHOD_NAME = "getWebGUIURL"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        ProgressContainer result = null;
        
        String domainName = null;
        ManagedSet managedSet = null;
        Device device = null;
        if (domain != null) {
            domainName = domain.getName();
            device = this;
        } else {
            domainName = "default"; //$NON-NLS-1$
            device = this;
        }
        
        BackgroundTask backgroundTask = BackgroundTask.createGetWebGUIURLTask(device, domainName);
        result = backgroundTask.getProgressContainer();
        Manager manager = Manager.internalGetInstance();
        // this is a read operation, so don't mind the queue size limit
        
        manager.enqueue(backgroundTask, manager);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
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
        String symbolicName = null;
        try {
            primaryKey = this.getPrimaryKey();
            symbolicName = this.getSymbolicName();
        } catch (DeletedException e) {
            primaryKey = "[deleted]"; //$NON-NLS-1$
        }
        
        String result = "Device["; //$NON-NLS-1$
        result += "primaryKey=" + primaryKey; //$NON-NLS-1$
        result += ", symbolicName=" + symbolicName; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    /**
     * Get a human-readable name that represents this object. This name may be
     * used in user interfaces.
     * 
     * @return a human-readable name that represents this object.
     */
    public String getDisplayName() {
        String result = null;
        try {
            result = this.getSymbolicName() + "/" + this.getHostname(); //$NON-NLS-1$
        } catch (DeletedException e) {
            result = "[deleted]"; //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Internal use only 
     */
    void unsubscribe(ProgressContainer progressContainer)
    throws InvalidCredentialsException, DeviceExecutionException, 
    AMPIOException, AMPException, DeletedException {
    	final String METHOD_NAME = "unsubscribe"; //$NON-NLS-1$
    	DeviceContext deviceContext = this.getDeviceContext();
        logger.entering(CLASS_NAME, METHOD_NAME, deviceContext);
        
        progressContainer.incrementCurrentStep(1, "wamt.clientAPI.Device.unsubDev_txt", deviceContext.getHostname()); //$NON-NLS-1$
        
        Manager manager = Manager.internalGetInstance();
        Commands commands = getCommands();
        String subscriptionId = manager.getSubscriptionId(isPrimary);
        commands.unsubscribeFromDevice(deviceContext, subscriptionId, ALL_TOPICS);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Internal use only 
     */
    void subscribe(ProgressContainer progressContainer) throws AMPException, DeletedException, SubscriptionToAnotherManagerException {
        final String METHOD_NAME = "subscribe"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this);
        if (progressContainer != null)
        	progressContainer.incrementCurrentStep(1,"wamt.clientAPI.Device.subDevt_txt", this.getDisplayName()); //$NON-NLS-1$
        
        Manager manager = Manager.internalGetInstance();
        Commands commands = this.getCommands();
        DeviceContext deviceContext = this.getDeviceContext();
        URL url = manager.getNotificationCatcherURL();
        SubscriptionResponseCode responseCode =
            commands.subscribeToDevice(deviceContext, manager.getSubscriptionId(isPrimary), ALL_TOPICS, url);
        if (responseCode.equals(SubscriptionResponseCode.ACTIVE)) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "subscription is active"); //$NON-NLS-1$
            // great!
        } else if (responseCode.isDuplicate()) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "subscription is duplicate"); //$NON-NLS-1$
            // is it us or someone else?
            if (isSubscriptionToAnotherManager(responseCode.getDuplicateURL())) {
                Object args[] = {this.getDisplayName(),responseCode.getDuplicateURL()};
                String message = Messages.getString("wamt.clientAPI.Device.dupSub",  args); //$NON-NLS-1$ 
                SubscriptionToAnotherManagerException e =
                    new SubscriptionToAnotherManagerException(message,"wamt.clientAPI.Device.dupSub",  args);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                // make sure it appears in the log, throwing() is FINER
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            } else {
                // that's OK if it is us resubscribing on top of ourself
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "Duplicate subscription appears to be us, so not treating it as an error."); //$NON-NLS-1$
            }
        } else if (responseCode.equals(SubscriptionResponseCode.FAULT)) {
            String message = Messages.getString("wamt.clientAPI.Device.subFault",this.getDisplayName()); //$NON-NLS-1$
            DeviceExecutionException e = new DeviceExecutionException(message,"wamt.clientAPI.Device.subFault",this.getDisplayName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        } else if (responseCode.equals(SubscriptionResponseCode.NONE)) {
            String message = Messages.getString("wamt.clientAPI.Device.subInvaildResp",this.getDisplayName()); //$NON-NLS-1$
            DeviceExecutionException e = new DeviceExecutionException(message,"wamt.clientAPI.Device.subInvaildResp",this.getDisplayName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Internal use only 
     */
    boolean isSubscriptionToAnotherManager(String existingCallbackURLString) {
        // eventually this should get the callback URL of the existing subscription
        // to determine if it is us or someone else
        Manager manager = Manager.internalGetInstance();
        URL myCallbackURL = manager.getNotificationCatcherURL();
        String myCallbackURLString = myCallbackURL.toExternalForm();
        
        if (myCallbackURLString.equals(existingCallbackURLString)) {
            return(false);
        } else {
            return(true);
        }
    }

    /**
     * Write a firmware version to this specified device. This is a synchronous call 
     * that will take a long time because not only does it upload a large firmware 
     * image to the device, it waits for a device restart and refreshes the cached 
     * info from the device. This whole scenario can take 7 minutes or more. 
     * 
     * @param fv the firmware version to write to the device
     * @param progressContainer object to receive progress updates
     * @throws AMPException
     * @throws DatastoreException 
     * @throws DeletedException
     * @throws IOException
     * @throws UnsuccessfulOperationException
     * @see Device#setSourceFirmwareVersion(FirmwareVersion)
     * @see Device#deploySourceFirmwareVersion(FirmwareVersion)
     */
    private void setFirmwareAndUpdate(FirmwareVersion fv, ProgressContainer progressContainer) 
        throws AMPException, DatastoreException, DeletedException, IOException, 
        	UnsuccessfulOperationException { 
        final String METHOD_NAME = "setFirmwareAndUpdate"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {fv});
        
        boolean wasSuccessful = false;
        Signaler signal = new Signaler(this, fv, null);
        Manager manager = Manager.internalGetInstance();

        ManagedSet managedSet = null;
        try {
            managedSet = this.getManagedSet();
        } catch (DeletedException e) {
        }
        
        lockWait();
        try {
            
            setManagementStatusOfFirmware(ManagementStatus.Enumerated.IN_PROGRESS);
            signal.sendStart();
            
            //Quiesce the device before updating the firmware
    		if (!getDeviceType().equals(DeviceType.XC10) && meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE)) {
                quiesce();
    		}
            
            Object[] args = new Object[] {this.getDisplayName(), fv.getAbsoluteDisplayName()};
            // use AMP to push the desired firmware to the device
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "loading FirmwareVersion " + fv + //$NON-NLS-1$
                        " to " + this); //$NON-NLS-1$
            progressContainer.incrementCurrentStep(1,"wamt.clientAPI.FirmwareVersion.loadingFwVer_txt", args);   //$NON-NLS-1$                    
            Commands commands = getCommands();
            DeviceContext deviceContext = getDeviceContext();
            InputStream inputStream = fv.getBlob().getInputStream();
           
            if (!getDeviceType().equals(DeviceType.XC10)){
            	// AMP call to create the file temporary://license.accepted before actually upgrade the firmware
            	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        	"create temporary:///license.accepted in default to " + this); //$NON-NLS-1$
            	byte[] uselessData = {100};
            	commands.setFile(this.getDeviceContext(), "default", "temporary:///license.accepted", uselessData);
            }
            
            commands.setFirmware(deviceContext, inputStream);
            
            inputStream.close();
            
            /*
             * Since it will take a few minutes for the device to install the
             * image and then reboot, we should wait here until the device comes
             * back up because there might be other operations queued for it.
             * Doesn't make sense to let those other operations fail when we
             * know the device is going to be non-responsive for the next few
             * minutes. We should also resubscribe to the device.
             */
            
            //The ping command must be from V1 commands, since the V2 ping  
            //cannot correctly ping a V1 device
            Commands v1commands = null;
            if (getDeviceType().equals(DeviceType.XC10)){
            	v1commands = commands;
            }else{
            	v1commands = getV1Commands();
            }
            
            int installSecondsTotal = 150;  // takes about 70-90 via WebGUI
            int retrySecondsInterval = 10;
            int waitMinutesTotal = 5;
            try {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "waiting for device to install the image and start reboot, sleeping " + //$NON-NLS-1$
                            installSecondsTotal + " seconds"); //$NON-NLS-1$
                // start by sleeping for a while so the device can unpack and install the image
                Thread.sleep(installSecondsTotal * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "sleep #1 interrupted"); //$NON-NLS-1$
            }
            // try to ping the device for a total of 5 minutes. Figure out the end time.
            Calendar end = new GregorianCalendar();
            end.add(Calendar.MINUTE, waitMinutesTotal);
            boolean isUp = false;
            while (!Thread.currentThread().isInterrupted() && !isUp) {
                // if we have been trying for >5 minutes then give up
                Calendar now = new GregorianCalendar();
                if (now.after(end)) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "gave up waiting for device to respond after " + //$NON-NLS-1$
                                waitMinutesTotal + " minutes"); //$NON-NLS-1$
                    break;
                }
                try {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                "checking if device is up yet..."); //$NON-NLS-1$
                    PingResponse pingResponse = 
                        v1commands.pingDevice(deviceContext, manager.getSubscriptionId(isPrimary));
                    if (pingResponse != null) {
                        isUp = true;
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                    "looks like device is up now, subscription state = " +  //$NON-NLS-1$
                                    pingResponse.getSubscriptionState());
                        refreshCachedInfo(); //TCB - added this so we get the right AMP commands
                        
                        // Recover lost subscription only if the device is in a managed set
                        if(!getDeviceType().equals(DeviceType.XC10) && managedSet != null) {
                        	recoverFromLostSubscription(progressContainer);
                        }
                    } else {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                "pingResponse is null, sleeping " + //$NON-NLS-1$
                                retrySecondsInterval + " seconds."); //$NON-NLS-1$
                        Thread.sleep(retrySecondsInterval * 1000);
                    }
                } catch (AMPIOException e) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "AMPIOException, looks like device is not up yet, waiting another " + //$NON-NLS-1$
                                retrySecondsInterval + " seconds"); //$NON-NLS-1$
                    try {
                        Thread.sleep(retrySecondsInterval * 1000);
                    } catch (InterruptedException e1) {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                    "sleep #3 interrupted"); //$NON-NLS-1$
                        Thread.currentThread().interrupt();
                    }
                } catch (InterruptedException e) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                "sleep #2 interrupted"); //$NON-NLS-1$
                    Thread.currentThread().interrupt();
                }
            }
            
            if (isUp) {
                setManagementStatusOfFirmware(ManagementStatus.Enumerated.SYNCED);
                wasSuccessful = true;
            } else {
            	args = new Object[] {this.getDisplayName(), fv.getAbsoluteDisplayName()};
                setManagementStatusOfFirmware(ManagementStatus.Enumerated.UNREACHABLE);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.FirmwareVersion.devNoComeUp", args));  //$NON-NLS-1$
            }
        } finally {
            unlock();
            if (!wasSuccessful) {
                // set the ManagementStatus to ERROR
                setManagementStatusOfFirmware(ManagementStatus.Enumerated.ERROR);
            }
            signal.sendEnd(wasSuccessful);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Determine the {@link ModelType} from the String returned from
     * {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(DeviceContext)}.
     * This value is used to populate
     * {@link com.ibm.datapower.amt.amp.DeviceMetaInfo#getModelType()}.
     * 
     * @param deviceID
     *            the DeviceID as returned to us from the device via AMP. This
     *            String may look like "9002-XS40-03" in the case of 9003's and
     *            earlier, or it may look like "923522X" in the case of 9235
     *            (9004) and later.
     * @return the matching ModelType.
     */
    public static ModelType getModelTypeFromDeviceID(String deviceID) {
        ModelType result = null;
        deviceID = deviceID.trim();
        if ( deviceID.equals("VMware")) {        	
        	result = ModelType.TYPE_5725;
        }
        else {
	        int hyphen1Index = deviceID.indexOf('-'); //$NON-NLS-1$
	        int hyphen2Index = deviceID.lastIndexOf('-'); //$NON-NLS-1$
	        if ((hyphen1Index >=0) && (hyphen2Index >= 0)) {
	            // old format (9002-XS40-02)
	            // get the character up to the first hyphen
	            String modelTypeString = deviceID.substring(0, hyphen1Index);
	            result = ModelType.fromString(modelTypeString);
	        } else {
	            // new format (923522X)
	            // get the first 4 characters
	            String modelTypeString = deviceID.substring(0, 4);
	            // try to use the existing objects if possible
	            if (modelTypeString.equalsIgnoreCase(ModelType.TYPE_9235.getDisplayName())) {
	                result = ModelType.TYPE_9235;
	            } else {
	                result = ModelType.fromString(modelTypeString);
	            }
	        }
        }
        return(result);
    }

    /**
     * Determine the descriptive hardware options String from the String
     * returned from
     * {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(DeviceContext)}.
     * This value is used to populate
     * {@link com.ibm.datapower.amt.amp.DeviceMetaInfo#getHardwareOptions()}.
     * 
     * @param deviceID
     *            the DeviceID as returned to us from the device via AMP. This
     *            String may look like "9002-XS40-03" in the case of 9003's and
     *            earlier, or it may look like "923522X" in the case of 9235
     *            (9004) and later.
     * @return the String which describes the hardware options. This will
     *         probably require more mapping to be human-readable.
     */
    public static String getHardwareOptionsFromDeviceID(String deviceID) {
        String result = null;
        deviceID = deviceID.trim();
        int hyphen1Index = deviceID.indexOf('-'); //$NON-NLS-1$
        int hyphen2Index = deviceID.lastIndexOf('-'); //$NON-NLS-1$
        if ((hyphen1Index >=0) && (hyphen2Index >= 0)) {
            // old format (9002-XS40-03)
            // get the characters after the 2nd hyphen
            result = deviceID.substring(hyphen2Index + 1, deviceID.length());
        } else {
            // new format (923522X)
            // get the last three characters
            result = deviceID.substring(deviceID.length()-3, deviceID.length());
        }
        return(result);
    }

    /**
     * Determine the current AMP version 
     * 
     * @param ampVersion
     *            A String containing the supported AMP version such as "1.0", "2.0" or "3.0".
     * @return the String representing the current AMP version for the device.
     *         If the input string was null, current AMP version is defaulted to "1.0".
     */
    //* Based on the String returned from
    //* {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(CurrentAMPVersion)}.    
    //* This value is used to populate
    //* {@link com.ibm.datapower.amt.amp.DeviceMetaInfo#currentAMPVersion()}.
    public static String getCurrentAMPVersionFromGetDeviceInfoResponse(String ampVersion) {
        String result = AMPConstants.AMP_V1_0;  // default
        if (ampVersion != null){
            //if (ampVersion.equalsIgnoreCase(AMPConstants.AMP_V2_0)) 
            	//result = ampVersion;
            //else if (ampVersion.equalsIgnoreCase(AMPConstants.AMP_V3_0)) 
            	result = ampVersion; // Set to 2.0 before v3 provider comes in
        }
        return(result);
    }

    /**
     * <p>An alternative to {@link #setSourceFirmwareVersion(FirmwareVersion)}, this 
     * method accepts a firmware "level" (e.g., "3.8.0.1") instead of a specific 
     * FirmwareVersion object. It looks for the best match in the {@link Manager}. An 
     * appropriate match must have been previously loaded into the <code>Manager</code>, 
     * otherwise a {@link NotExistException} will be thrown.
     * </p>  
     * <p>
     * Set the firmware to be deployed by {@link #deploySourceFirmwareVersion()} 
     * to the best matching firmware version according to 
     * {@link Manager#getBestFirmware(DeviceType, ModelType, StringCollection, String)}.
     * </p>
     * 
     * <p>Firmware management operations are disabled on zBX.  The operation will not be executed.  An informational 
     * message will be logged.
     * <p>
     *  
     * @param desiredLevel
     * @throws DeletedException 
     * @throws NotExistException 
     * @throws UnlicensedFeaturesInFirmwareException 
     * @throws MissingFeaturesInFirmwareException 
     * @throws ModelTypeIncompatibilityException 
     * @throws DeviceTypeIncompatibilityException 
     */
    public void setSourceFirmwareLevel(String desiredLevel) throws DeletedException, NotExistException, DeviceTypeIncompatibilityException, ModelTypeIncompatibilityException, MissingFeaturesInFirmwareException, UnlicensedFeaturesInFirmwareException
    {
    	final String METHOD_NAME = "setSourceFirmwareLevel"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME, desiredLevel);

    	Manager manager = Manager.internalGetInstance();
    	
    	// Check for firmware management operations are enabled on device
    	if(!isFirmwareManagentEnabled()){
     		String message = Messages.getString("wamt.clientAPI.Device.firmwareOperationsDisabled", this.getSymbolicName());
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
			return;
		} 	
    	
    	
    	Firmware fw = manager.getBestFirmware(getDeviceType(), getModelType(), getFeatureLicenses(), desiredLevel);
    	if (fw != null) {
  	        fw.assertCompatibility(this);
        	FirmwareVersion fv = fw.getLevel(desiredLevel);
        	sourceFirmwareVersionKey = fv.getPrimaryKey();    			
    	} else {
        	String message = Messages.getString("wamt.clientAPI.Device.noFWforYou"); //$NON-NLS-1$
    		throw new NotExistException(message,"wamt.clientAPI.Device.noFWforYou");
    	}

    	logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Invoked by the set firmware version task.
     * Sets the source firmware version key.
     * @param desiredFirmwareVersion
     * @return
     * @throws DeletedException
     */
    protected boolean setSourceFirmwareVersionAction(FirmwareVersion desiredFirmwareVersion)
    throws DeletedException
    {
    	try {
    		if (desiredFirmwareVersion==null){    			
    			sourceFirmwareVersionKey = null;
    		}
    		else {
            	sourceFirmwareVersionKey = desiredFirmwareVersion.getPrimaryKey();    			
    		}
    	}finally{    		
    	}
    	return true;
    }

    /**
     * Schedules a task to set the FirmwareVersion for this device.
     * This task is available to a unmanaged device 
     * or a device in a managed set.
     * A subscription is not required to perform this task.
     * 
     * <p>Firmware management operations are disabled on zBX.  The operation will not be executed.  An informational 
     * message will be logged.  
     * <p>
     * 
     * Use Firmware.getIncompatibilityReason to check for failures.
     * @param desiredFirmwareVersion
     * @return  ProgressContainer.  Check for completion and error. 
     */
    public ProgressContainer setSourceFirmwareVersion(FirmwareVersion desiredFirmwareVersion) 
    throws DeletedException, FullException, NotExistException,
		DeviceTypeIncompatibilityException,
		ModelTypeIncompatibilityException,
		MissingFeaturesInFirmwareException,
		UnlicensedFeaturesInFirmwareException {
    	final String METHOD_NAME = "setSourceFirmwareVersion"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME, desiredFirmwareVersion);

    	ProgressContainer result = null;
    	   	
    	
/*    	// check that the firmware version is compatible with this device
    	if (desiredFirmwareVersion == null) {
  	    	Object[] args = new Object[] {this.getDisplayName()};
   	        String message = Messages.getString("wamt.clientAPI.Device.fwVerNull", args); //$NON-NLS-1$
   	        NotExistException e = new NotExistException(message,"wamt.clientAPI.Device.fwVerNull", args);
   	        logger.throwing(CLASS_NAME, METHOD_NAME, e);
   	        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
   	        throw(e);
    	}*/
    	
    	// Check firmware management is enabled 
    	if(!isFirmwareManagentEnabled()){
    		String message = Messages.getString("wamt.clientAPI.Device.firmwareOperationsDisabled", this.getSymbolicName());
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
			result = new ProgressContainer(new SetFirmwareVersionTask(this, desiredFirmwareVersion));
			result.setComplete();
			result.setCorrelator(this);
			result.setTotalSteps(0);
			return result;
		}      	
    	
    	try {    	    
    	    // check the compatibility of the firmware to the device
  	        //Firmware firmware = desiredFirmwareVersion.getFirmware();
  	        // assertCompatibility will throw exception if not compatible
  	        //firmware.assertCompatibility(this);
   	        
   	        // task to set the firmware version   	        
            BackgroundTask backgroundTask = BackgroundTask.createSetFirmwareVersionTask(this,desiredFirmwareVersion);

            result = backgroundTask.getProgressContainer();            
            // Enqueue background task
            Manager manager = Manager.internalGetInstance();
            ManagedSet managedSet = this.getManagedSet();
            if (managedSet != null) 
            {
                manager.enqueue(backgroundTask, managedSet);        	
            }
            else
            {
                manager.enqueue(backgroundTask, manager);        	
            }


    	} finally {
    	}
    	logger.exiting(CLASS_NAME, METHOD_NAME);
    	return result;
    }
    
    /**
     * Returns the source FirmwareVersion that is set for this device. 
     * @return the source FirmwareVersion for the device
     */
    public FirmwareVersion getSourceFirmwareVersion()
    {
    	return FirmwareVersion.getByPrimaryKey(sourceFirmwareVersionKey);
    }

    protected boolean deploySourceFirmwareVersionAction(ProgressContainer progressContainer)
    throws AMPException, DatastoreException, DeletedException, InUseException, IOException, InvalidParameterException, 
    		LockBusyException, UnsuccessfulOperationException, UnsupportedVersionException
    {
    	final String METHOD_NAME = "deploySourceFirmwareVersionAction"; //$NON-NLS-1$
    	// Check acceptLicense
        if ( this.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM) && !this.licenseAccepted ) {
			Object[] params = { this.getDeviceContext().getHostname(), Integer.toString(this.getDeviceContext().getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params); //$NON-NLS-1$
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
		} 
    	
    	FirmwareVersion firmwareVersion = FirmwareVersion.getByPrimaryKey(sourceFirmwareVersionKey);
    	
    	if (firmwareVersion==null){
  	    	Object[] args = new Object[] {this.getDisplayName()};
   	        String message = Messages.getString("wamt.clientAPI.Device.deployFwVerDeleted", args); //$NON-NLS-1$
   	        DeletedException e = new DeletedException(message,"wamt.clientAPI.Device.deployFwVerDeleted", args);
    		throw(e);
    	}
    	
    	// Check to prevent a firmware downgrade problem for XC10
    	if(getDeviceType().equals(DeviceType.XC10)){
    		String fwLevel = firmwareVersion.getLevel();
    		if(fwLevel.startsWith("2.0.")){
    			
    			Object[] args = new Object[] {actualFirmwareLevel, fwLevel};
       	        String message = Messages.getString("wamt.clientAPI.device.unsupportedFWDowngrade", args); //$NON-NLS-1$

    			UnsupportedVersionException uve = new UnsupportedVersionException(message, "wamt.clientAPI.device.unsupportedFWDowngrade", args);
    			
    			throw uve;
    		}
    	}else{
    		// Non-XC10 DataPower devices
    		// Prevent DataPower AMP XML size problem
    		// http://www-01.ibm.com/support/docview.wss?uid=swg1IC77610
    		// Firmware images larger than 136 MB when BASE64 encoded are rejected by AMP SETFIRMWAREREQUEST with no error message.  
    		// And the fix is available on 3.8.0.15, 3.8.1.15, 3.8.2.6, and 4.0.1.3

    		boolean fixed = true;
    		
    		String actualFirmwareLevel = getActualFirmwareLevel();
    		
    		if(actualFirmwareLevel.startsWith("4.0.0.")){
    			// 4.0.0.1
    			fixed = FirmwareVersion.meetsMinimumLevel("4.0.0.1", actualFirmwareLevel);
    		}else if(actualFirmwareLevel.startsWith("4.0.1.")){
    			// 4.0.1.3
    			fixed = FirmwareVersion.meetsMinimumLevel("4.0.1.3", actualFirmwareLevel);
    		}else if(actualFirmwareLevel.startsWith("3.8.2.")){
    			// 3.8.2.6
    			fixed = FirmwareVersion.meetsMinimumLevel("3.8.2.6", actualFirmwareLevel);
    		}else if(actualFirmwareLevel.startsWith("3.8.1.")){
    			// 3.8.1.15
    			fixed = FirmwareVersion.meetsMinimumLevel("3.8.1.15", actualFirmwareLevel);
    		}else if(actualFirmwareLevel.startsWith("3.8.0.")){
    			// 3.8.0.15
    			fixed = FirmwareVersion.meetsMinimumLevel("3.8.0.15", actualFirmwareLevel);
    		}
    		
    		if(!fixed){
    			long size = firmwareVersion.getBlob().getSize();
    			
    			// Use ~ 133MB here since the firmware image has been base64 encoded
    			if( size > 133000000L){
    				// Firmware size too large
    				Object[] params = { this.getDeviceContext().getHostname(), actualFirmwareLevel };
    				String message = Messages.getString("wamt.clientAPI.Device.fwTooLarge", params); //$NON-NLS-1$
    				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.clientAPI.Device.fwTooLarge", params); //$NON-NLS-1$
    				logger.throwing(CLASS_NAME, METHOD_NAME, e);
    				throw e;    				
    			}    			
    		}
    	}
    	
    	try {
    		this.setFirmwareAndUpdate(firmwareVersion, progressContainer);
    	} finally {
    	}
    	return true;
    }
    
     /**
     * Schedules a task to deploy a FirmwareVersion.
     * This task is available to a unmanaged device 
     * or a device in a managed set.
     * A subscription is not required to perform this task.
     * Input firmware version is optional.
     * 
     * Note: All Firmware operations are disabled on zBX. 
     * 
     * @return ProgressContainer
     * @throws DeletedException
     * @throws FullException
     * 
     * @see Device#setSourceFirmwareVersion(FirmwareVersion)
     * @see ManagedSet#setSourceFirmwareLevel(String)
     * 
     */
    //@Deprecated
    public ProgressContainer deploySourceFirmwareVersion() throws DeletedException, FullException {
        final String METHOD_NAME = "deploySourceFirmwareVersion"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	ProgressContainer progressContainer = null;
    	
    	// Check if firmware management operations are enabled
    	if(!isFirmwareManagentEnabled()){
			String message = Messages.getString("wamt.clientAPI.Device.firmwareOperationsDisabled", this.getSymbolicName());
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
			progressContainer = new ProgressContainer(new DeployFirmwareVersionTask(this));
			progressContainer.setComplete();
			progressContainer.setCorrelator(this);
			progressContainer.setTotalSteps(0);
			this.licenseAccepted = false;
			return progressContainer;
    	}     	
    	
    	
    	// Set and deploy a firmware version.
        BackgroundTask backgroundTask = BackgroundTask.createDeployFirmwareVersionTask(this);
        progressContainer = backgroundTask.getProgressContainer();          
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        ManagedSet managedSet = this.getManagedSet();
        if (managedSet != null) 
        {
            manager.enqueue(backgroundTask, managedSet);        	
        }
        else
        {
            manager.enqueue(backgroundTask, manager);        	
        }       

        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(progressContainer);
    }

    
    /**
     * Get the commands implementation to use for this device. 
     */
    public Commands getCommands() throws AMPException {
        final String METHOD_NAME = "getCommands"; //$NON-NLS-1$

        Commands commands = null;
        
        String[] AMPVersions = {AMPConstants.AMP_V3_0, AMPConstants.AMP_V2_0, AMPConstants.AMP_V1_0, AMPConstants.NONE_AMP}; // All AMP versions supported
        StringCollection supportAMPVersion = new StringCollection(AMPVersions);
        
        try{
        	String currentAMPVersion = this.getCurrentAMPVersion();
        	if ( !supportAMPVersion.contains(currentAMPVersion) ) { 
        		// unknown AMP version, set the currentAMPVersion to the latest one
        		currentAMPVersion = AMPVersions[0];
        	}        	
        	
        	if (currentAMPVersion.equalsIgnoreCase(AMPConstants.NONE_AMP)){
        		// Not AMP
        		// For now it's XC10
        		commands = getXC10Commands();
        	}
        	else if (currentAMPVersion.equalsIgnoreCase(AMPConstants.AMP_V3_0)){
                commands = getV3Commands();    		
                
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        " using AMP version "  + AMPConstants.AMP_V3_0); //$NON-NLS-1$ //$NON-NLS-2$
            }
        	else if (currentAMPVersion.equalsIgnoreCase(AMPConstants.AMP_V2_0)){ // assume AMPv2
                commands = getV2Commands();
                
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        " using AMP version "  + AMPConstants.AMP_V2_0); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else { // assume AMPv1
                commands = getV1Commands();
                
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        " using AMP version "  + AMPConstants.AMP_V1_0); //$NON-NLS-1$ //$NON-NLS-2$
            }        	
        } catch (DeletedException e){
        	// assume AMPv1
            commands = getV1Commands();
      	
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    " using AMP version "  + AMPConstants.AMP_V1_0); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return(commands);
    }
    
    /**
     * Get the SOMA commands implementation to use for this device. 
     */
    public SOMACommands getSOMACommands() throws SOMAException {
        String commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_SOMA_IMPL);
        String soapHelperClassNameImpl = Configuration.get(Configuration.KEY_SOAP_HELPER_SOMA_IMPL);        
        return (SOMACommandFactory.getCommands(commandsClassNameImpl, soapHelperClassNameImpl));
    }
    
    /*
     * Get the V1 AMP commands to use for this device. 
     * The V1 method is needed because the V2 pingDevice command 
     * will not work if firmware has been downgraded from AMP V2 to V1
     */
    /*
    protected Commands getCommandsV1() throws AMPException {
        final String METHOD_NAME = "getCommandsV1"; //$NON-NLS-1$

        String commandsClassNameImpl = null;
        String soapHelperClassNameImpl = null;
        Commands commands = null;
    	// force AMPv1
        commandsClassNameImpl = Configuration.get(Configuration.KEY_COMMANDS_IMPL);
        soapHelperClassNameImpl = Configuration.get(Configuration.KEY_SOAP_HELPER_IMPL);
        commands = CommandFactory.getCommands(commandsClassNameImpl, soapHelperClassNameImpl);        	
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                " forced to AMP version "  + AMPConstants.AMP_V1_0); //$NON-NLS-1$ //$NON-NLS-2$
        return(commands);
    }*/

        
    /*
     * lock for device
     */
    void lockNoWait() throws LockBusyException {
        this.lock.lockNoWait();
    }
    
    void lockWait() {
        this.lock.lockWait();
    }
    
    void unlock() {
        this.lock.unlock();
    }
    
    /**
     * Set the Synchronization Mode for the all domains on device. Synchronization  mode defaults to DomainSynchronizationMode.MANUAL. 
     * Valid synchronization modes are DomainSynchronizationMode.MANUAL and DomainSynchronizationMode.AUTO
     * 
     * @param synchMode sSynch Mode to be set on the managed domains for this Device
     * @return Hashtable of failed Domains - with Domain(key)/Exception(value)
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws FullException
     * @throws DeletedException 
     * 
     * @see DomainSynchronizationMode#AUTO
     * @see DomainSynchronizationMode#MANUAL
     * @see ManagedSet#setSynchronizationModeForDomain(String, DomainSynchronizationMode)
     * 
     */
    public Hashtable setSynchronizationModeForManagedDomains(DomainSynchronizationMode synchMode) throws DeletedException, FullException {
        Hashtable<Domain,Exception> failedDomains = new Hashtable<Domain,Exception> ();           		      	

        Domain[] domains = this.getManagedDomains();
        for (int i=0; i<domains.length; i++) {
            try {
				domains[i].setSynchronizationMode(synchMode);
			} catch (DirtySaveException e) {
				// TODO Auto-generated catch block
			    failedDomains.put(domains[i], e);
			} catch (DatastoreException e) {
			    failedDomains.put(domains[i], e);			
			} catch (NotManagedException e) {
			    failedDomains.put(domains[i], e);			
			}

        }
        return failedDomains;
    }
    
	
    /**
     * <p>Takes a backup of a DataPower appliance which can be replicated to a compatible appliance. 
     * A compatible appliance is one that has the identical firmware level and storage space. The main purpose 
     * of this function is disaster recovery, but it can also be used to take the configurations from one 
     * appliance and replicate it on another appliance. This task is available to a unmanaged device 
     * or a device in a managed set. This is a long running task that will be placed on a queue.
     * 
     * <p>For more information about the backup capability in a DataPower appliance see the link to article provided below.</p>
     * 
     * <p> Find information about generating keys and certificates using Crypto Tools and 
     * Crypto Identification Credentials in the infocenter. See links provided below.</p>
     * <p>Use this to backup a device so that its state can be restored if the device fails or becomes inoperable. 
     * A Crypto Certificate is used for the encryption during backup and the corresponding Crypto Identification Credential will be required for
     * later for a successful restore.</p>
     * <br/> 
     * <p>This operation is available on managed and unmanaged devices and must satisfy minimum firmware
     * level requirements.
     * 
     * <p>If secure backup fails to complete successfully, check the system logs on the DataPower appliance for further
     * details.
     * 
     * <p>NOTE: This function is available only if you enabled disaster recovery mode
     * during the initial firmware setup of the appliance. If not enabled, you must
     * reinitialize the appliance and enable disaster recovery. To determine if
     * disaster recovery is available, click Administration -> Device-> System
     * Settings. If the Backup Mode property is set to Secure, disaster recovery is
     * available.
     * <br/> 
     *  
     * @param cryptoCertificateName  certificate object name on the Device. This is the Crypto Certificate used for the encryption
     * The corresponding Crypto Identification Credential will be required for the secure restore.<br/>   
     * @param cryptoImage - the location of the certificate used to encrypt the backup.  Must be file,
     * http or https.  The cryptoCertificate or the certObjectName must be specified.  Both cannot be specified; one of them
     * must be null.<br/> 
     * @param backupDestination is the URI of a directory where the backup files will be written. Supported protocols are "local:", 
     * "temporary:", "ftp:" and "file:".  The location must be a directory, not a file name. All backup will be copied to the destination. 
     * If the specified destination is a "file:", any existing backup files will be overwritten. <br/> 
     * @param includeISCSI A value of "true" will include the iSCSI device data in the secure-backup files. 
     * A value of "false" will omit the iSCSI device data in the secure-backup files.<br/>   
     * @param includeRaid A value of "true" will include the RAID device data in the secure-backup files. 
     * A value of "false" will omit the RAID device data in the secure-backup files.  <br/> 
     * 
     * <p>
     * Sample Usage:
     * <p/>
     *  To backup with cryptoCertificateName<br/> 
     *    --> <code></br>
     *         URI backupFileLocation = new URI  ("local:///Backup");</br>
     *         String certName="myCert";  </br>
     *         ProgressContainer pg1 = device1.backup(certName, null, backupFileLocation, false, false); 
     *         </code>
     * <p/>
     * <p/>
     *  To backup with cryptoImage location specified<br/> 
     *    --> <code>String certificateLocation = new URLSource("file:///c:/l3/myCert-sscert.pem");</br>		
     *        ProgressContainer pg1 = device1.backup(null, certificateLocation , backupFileLocation,false,false); 
     *        </code>
     * </p>   
     * 
     * @see <a href="http://www.ibm.com/developerworks/websphere/library/techarticles/1009_furbee/1009_furbee.html">Secure backup-restore for WebSphere DataPower SOA Appliances, a developerWorks Article</a><br/> 
     * @see <a href="http://publib.boulder.ibm.com/infocenter/wsdatap/v3r8m2/index.jsp?topic=/xi50/webgui_generatingkeyscerts_task.htm">Generating keys and certificates using Crypto Tools</a><br/> 
     * @see <a href="http://publib.boulder.ibm.com/infocenter/wsdatap/v3r8m2/index.jsp?topic=/xi50/idcred_definingidcredentialsobjects_task.htm">Crypto Indentification Credential</a><br/> 
     *   
     *   
     * @return ProgressContainer which will indicate if the task completed successfully  
     * @throws DatastoreException
     * @throws LockBusyException
     * @throws DeletedException
     * @throws FullException
     * @throws InvalidParameterException 
     * @throws URISyntaxException 
     * @throws MissingFeaturesInFirmwareException 
     */
    //See {@link MinimumFirmwareLevel#MINIMUM_FW_LEVEL_FOR_BACKUP}
    public ProgressContainer backup(String cryptoCertificateName, URLSource cryptoImage, URI backupDestination, boolean includeISCSI, boolean includeRaid)
    		throws DatastoreException, LockBusyException, DeletedException,
			FullException, InvalidParameterException, URISyntaxException, MissingFeaturesInFirmwareException{
        final String METHOD_NAME = "backupDevice"; //$NON-NLS-1$
        
        
    	ProgressContainer progressContainer = null;    	
    	boolean ftpLocation = false;
    	
		//Full Backup is only available on FW version 3.8.1 or later
		if (!this.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_BACKUP)){
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
						"device does not meet minimum firmware requirement for Back UP:" + MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_BACKUP);
				String message = Messages.getString("wamt.clientAPI.Device.backupFWLevel");
				throw new MissingFeaturesInFirmwareException(message,"wamt.clientAPI.Device.backupFWLevel");
		}
		
		// The crypto object name or the crypto certificate location must be specified,both cannot be specified
		if ((cryptoCertificateName != null)&& (cryptoImage !=  null)){
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
					"Specify the cryptoImage or the cryptoCertificateName. Both may not be specified" );
			String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","Specify the cryptoImage parameter or the cryptoCertificateName parameter. Both may not be specified.");
			throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","Specify the cryptoImage parameter or the cryptoCertificateName parameter. Both may not be specified.");			
		}
		
		if(cryptoCertificateName == null){
			// The crypto object name and the cryptoCertificate cannot be null.  One of them must be specified.
			if (cryptoImage == null){
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
						"cryptoImage or cryptoCertificateName must be specified." );
				String message = Messages.getString("wamt.clientAPI.DeploymentPolicy.invalidParameter","cryptoCertificateName parameter or cryptoImage parameter must be specified.");
				throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","cryptoCertificateName parameter or cryptoImage parameter must be specified.");

			}
			//Check the scheme of the certificate location if the certificate object name it not specified. The scheme must be file, http or https			
			if (!((cryptoImage.getScheme().equals(URLSource.SCHEME_HTTP))||
					(cryptoImage.getScheme().equals(URLSource.SCHEME_FILE))||					
					(cryptoImage.getScheme().equals(URLSource.SCHEME_HTTPS)))){
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
						"Invalidd backup location for certificates. Must be Http or Https "  + cryptoImage.getURL() );
				String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","cryptoImage parameter must be a URL using file, http or https and must be a non-null value.");
				throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","cryptoImage parameter must be a URL using file, http or https and must be a non-null value.");
			}  
		}

    	//Check the scheme of the backupFileLocation, if it is a "file" then the backup files will be written to the specified direct
		try {
			if (backupDestination !=null){
				if (backupDestination.getScheme()!= null){
					// "local',"temporary", "ftp" are valid schemes for backup File
					if (!((backupDestination.getScheme().equals("local"))||
							(backupDestination.getScheme().equals("ftp"))||
							(backupDestination.getScheme().equals("file"))||      						
							(backupDestination.getScheme().equals("temporary")))){  
						String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","The backupDestination parameter must be a URI using file, local, temporary, or ftp");
						throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","The backupDestination parameter must be a URI using file, local, temporary, or ftp");

					}    				
					if(backupDestination.getScheme().equals("file")){
                        
						File f = new File(backupDestination.getPath());
						// Ensure that a directory name was specified
						if(f.isDirectory()){
							// all is well they specified only directory
						}else{
							String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","backupDestination parameter must specify a directory and not a file name");
							throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","backupDestination parameter must specify a directory and not a file name.");
						}
					} 
				}
			}else{
				String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","The backupDestination parameter must be a URI using file, local, temporary, or ftp");
				throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","The backupDestination parameter must be a URI using file, local, temporary, or ftp");
			}
		} finally{

		}		
		
    	// Backup the device, this will take a while.  		
        BackgroundTask backgroundTask = BackgroundTask.createBackupDeviceTask(this, cryptoCertificateName, cryptoImage, backupDestination, includeISCSI, includeRaid);
        progressContainer = backgroundTask.getProgressContainer();
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        ManagedSet managedSet = this.getManagedSet();
        if (managedSet != null) 
        {
            manager.enqueue(backgroundTask, managedSet);        	
        }
        else
        {
            manager.enqueue(backgroundTask, manager);        	
        }

        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(progressContainer);
    }

    /*
     * Does a secure backup of a device. The device may be managed or unmanaged.
     * 
     * This is a synchronous call to a long running task that is not placed 
     * in the queue for background execution. 
     * 
     * @param certObjectName  certificate object name on the Device. This is the Crypto Certificate used for the encryption
     * The corresponding Crypto Credential will be required for the secure restore.  
     * @param cryptoCertificate - the location of the certificate used to encrypt the backup.  Must be file,
     * http or https.  The cryptoCertificate or the certObjectName must be specified.  Both cannot be specified; one of them
     * must be null.
     * @param backupDestination is the URI of a directory where the backup files will be stored. Supported protocols are "local:", 
     * "temporary:", "ftp:" and "file:".  The location must be a directory, not a file name.
     * @param  include-iscsi A value of "true" is the default. The secure backup operation will include the iSCSI device data in the secure-backup file. 
     * A value of "false" will omit the iSCSI device data in the secure-backup file.  
     * @param  include-raid A value of "true" is the default. The secure backup operation will include the RAID device data in the secure-backup file. 
     * A value of "false" will omit the RAID device data in the secure-backup file.  
     * 
     * @throws InvalidParameterException the ProgressContainer is null
     * @throws AMPException an error occurred while executing the command on the
     *         device
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DirtySaveException 
     * @throws LockBusyException 
     * @throws DatastoreException 
     * @throws URISyntaxException
     * @throws MalformedURLException
     * @throws IOException
     * 
     */
    
	 protected void backUpDeviceAction(String certObjectName, URLSource certificatesLocation, URI backupFileLocation,
			boolean includeISCSI, boolean includeRaid) throws AMPException, 
	    DeletedException, DirtySaveException, URISyntaxException, LockBusyException, MalformedURLException,IOException {
		final String METHOD_NAME = "backUpDeviceAction"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);

		//acquire the lock
		this.lockNoWait();	
		try {					
			
			Commands commands;
			commands = this.getCommands();
			DeviceContext deviceContext;
			deviceContext = this.getDeviceContext();


			byte[] cryptoFile = null;
			if (certificatesLocation!= null){
				Blob blob = certificatesLocation.getFirmwareBlob();
				cryptoFile = blob.getByteArray();				
			}   
			
			String backupPath = null; 
			if(backupFileLocation != null){
				backupPath = backupFileLocation.toString();
				if(backupFileLocation.getScheme().equals("file")){
					backupPath = null;
				}
			}
            
			Hashtable<String,byte[]> backupFiles = commands.backupDevice(deviceContext, certObjectName, cryptoFile, backupPath,includeISCSI, includeRaid);

			try {
				if( backupFileLocation != null && backupFileLocation.getScheme().equals("file")){
                   // copy any returned backup file to specified directory
                    Enumeration<String> fileNames = backupFiles.keys();
                  	File backupDirectory = null;	
                	String backupDir = backupFileLocation.getPath();
                	backupDirectory = new File(backupDir);
                	if (!backupDirectory.exists()){
                		if ( !backupDirectory.mkdir() ) {
                			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "Unable to create backup directory");
                		}
                	}                   
                	
                   while(fileNames.hasMoreElements())
                    {
                   	    
                    	//SecureBackupFile file=(SecureBackupFile)fileNames.nextElement();
                   	    String fileName =fileNames.nextElement();
                   	    byte[] value = backupFiles.get(fileName);
                   	    
                   	    if(value.length == 5){
                   	    	String temp = new String(value);
                   	    	if("ERROR".equals(temp)){
                   	    		Object[] args = new Object[] {fileName};
            		        	String message = Messages.getString("wamt.clientAPI.Device.invalidBackupFile", args); //$NON-NLS-1$
            		            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            		            
            		            throw new AMPException(message, "wamt.clientAPI.Device.invalidBackupFile", args);	
                   	    	}
                   	    }
  
        				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
        						"Saving backup File: " + backupFileLocation.getPath()+ " file: " + fileName);
        				
                    	try{
                    		//System.out.println(file.xmlText());
                    	    this.saveFilesafterDeviceBackupOperation(backupFileLocation,fileName, value);
                    	}catch(Exception e){
                			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
                			//e.printStackTrace();
                    	}
                    } 														
				}
			} finally{
			}

			// persist location information in LFS
			StoredDevice sd = this.getStoredInstance();
			sd.setBackupCertificateLocation(certificatesLocation);
			sd.setBackupFileLocation(backupFileLocation);  
			Manager manager = Manager.internalGetInstance();         
			manager.save(Manager.SAVE_UNFORCED);     
		} catch (AMPException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} catch (DeletedException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;		
		} catch (DirtySaveException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
			throw e;
		} catch (DatastoreException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
		} catch (InvalidParameterException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
		} finally {
			// cleanup
			// release lock 
			this.unlock();
		}

        logger.exiting(CLASS_NAME, METHOD_NAME);
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	
    /**
     * <p>Restores the configuration and secure data from a successful secure backup. The main purpose 
     * of this function is disaster recovery, but it can also be used to take the configurations from one 
     * appliance and replicate it on another applicance. It can be used to restore a device that has been remanufactured 
     * or replaced to its previous state.
     * 
     * <p>This task is available to a unmanaged device 
     * or a device in a managed set. This is a long running task that will be placed on a queue. This operation
     * must also satisfy minimum firmware
     * level requirements.
     * 
     * 
     * <p>For more information about the restore capability in a DataPower appliance see the link to article provided below.</p>
     * 
     * <p> Find information about generating keys and certificates using Crypto Tools and 
     * Crypto Identification Credentials in the infocenter. See links provided below.</p>
     * 
     * <p>If restore fails to complete successfully, check the system logs on the DataPower applicance for further
     * details.
     * 
     * <p>If the device is an "managed device", it will be removed from its managed set on successful completion.
     * The clientAPI user should delete the device object and recreate a new device object, since the earlier
     * object may not reflect the current restored configuration.
     * 
     * <p>Note: When the restore operation completes, the appliance will have an administrator user id of "admin" 
     * and a preset password. The password change must be done through the webGUI or the command line and 
     * cannot be done with the manager. 
     * 
     * <p> The disaster recovery mode will be enabled on the device as a result of the restore operation, if it was not previously enabled.
     * To determine if
     * disaster recovery is available, click Administration -> Device-> System
     * Settings. If the Backup Mode property is set to Secure, disaster recovery is
     * available.
     * 
     * <p> Note: Restore operation is disabled on zBX. </p>
     * 
     * <p>
     * Sample Usage:
     * <p/>
     *  To validate restore with cryptoCredentialName<br/> 
     *    --> <code></br>
     *         URI backupFileLocation = new URI ("local:///Backup");</br>
     *         String cryptoCredentialName = "myCertCID";</br>
     *         ProgressContainer pg3 = device1.restore(cryptoCredentialName, backupFileLocation,true);; 
     *         </code>
     * <p/>
     * <p/>
     *  To perform restore from backup files<br/> 
     *    --> <code></br>
     *         URI backupFileLocation = new URI ("local:///Backup");</br>     *    
     *         String cryptoCredentialName = "myCertCID";</br>
     *         ProgressContainer pg3 = device1.restore(cryptoCredentialName, backupFileLocation,false);; 
     *         </code>
     * </p> 
     * 
     * @param cryptoCredentialName is the object name of the Crypto Identification Credentials which includes the
     * private Crypyto Key and the Certificate. It used to decrypt the backup files.  
     * @param backupSource the location of the backup files which will be used to restore the device configuration.
     * It is the URI of a directory where the backup files are available. Supported protocols are "local:", 
     * "temporary:", "ftp:" and "file:".  The location must be a directory, not a file name. 
     * @param validate if true will check for items such as firmware compatibility, the manifest file's digital signature, 
     * and auxillary storage matches. The Validate option only performs the validation between the backup files 
     * and the appliance to be restored. The restore itself not initiated.
     * @return ProgressContainer that will indicate the success of the task
     * 
     * @see <a href="http://www.ibm.com/developerworks/websphere/library/techarticles/1009_furbee/1009_furbee.html">Secure backup-restore for WebSphere DataPower SOA Appliances, a developerWorks Article</a><br/> 
     * @see <a href="http://publib.boulder.ibm.com/infocenter/wsdatap/v3r8m2/index.jsp?topic=/xi50/webgui_generatingkeyscerts_task.htm">Generating keys and certificates using Crypto Tools</a><br/> 
     * @see <a href="http://publib.boulder.ibm.com/infocenter/wsdatap/v3r8m2/index.jsp?topic=/xi50/idcred_definingidcredentialsobjects_task.htm">Crypto Indentification Credential</a><br/> 
     * 
     * @throws DatastoreException
     * @throws LockBusyException
     * @throws DeletedException
     * @throws FullException
     * @throws InvalidParameterException 
     * @throws URISyntaxException 
     * @throws MissingFeaturesInFirmwareException 
     * @throws IOException 
     * @throws NotEmptyException 
     * @throws InUseException 
     */
    public ProgressContainer restore(String cryptoCredentialName, URI backupSource, boolean validate)
    		throws NotExistException,
			DatastoreException, LockBusyException, DeletedException,
			FullException, InvalidParameterException, URISyntaxException, MissingFeaturesInFirmwareException, IOException, InUseException, NotEmptyException{
        final String METHOD_NAME = "restoreDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	ProgressContainer progressContainer = null;    	
    	boolean ftpLocation = false;
    	
    	// Check if restore operation is enabled on device
    	if(!this.isRestoreEnabled()){
			String message = Messages.getString("wamt.clientAPI.Device.restoreDisabled", this.getSymbolicName());
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
			
			progressContainer = new ProgressContainer(new RestoreDeviceTask(this, cryptoCredentialName, validate, backupSource, null));
			progressContainer.setComplete();
			progressContainer.setCorrelator(this);
			progressContainer.setTotalSteps(0);
			return progressContainer;
    	}
    	
		//Full Backup is only available on FW version is 3.8.1 or later
		if (!this.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_BACKUP)){
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
						"device does not meet minimum firmware requirement for Restore:" + MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_BACKUP);
				String message = Messages.getString("wamt.clientAPI.Device.restoreFWLevel");
				throw new MissingFeaturesInFirmwareException(message,"wamt.clientAPI.Device.restoreFWLevel");
		}    	
    	
   	
		if (cryptoCredentialName == null){
    		String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","cryptoCredentialName parameter must be non-null.");
    		throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","cryptoCredentialName parameter must be non-null.");    			
			
		}
    	try {
    		// TODO What are valid schemes?  file, ftp, http, https
    		if (backupSource.getScheme()!= null){
    			// "local',"temporary", "ftp" are valid schemes for backup File
    			if (!((backupSource.getScheme().equals("file"))||
    	    			(backupSource.getScheme().equals("local"))||
    	    			(backupSource.getScheme().equals("ftp"))||    	    			
    	    			(backupSource.getScheme().equals("temporary")))){  
    	    		String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","The backupSource parameter must be a URI using file, local, temporary, or ftp.");
    	    		throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","The backupSource parameter must be a URI using file, local, temporary, or ftp.");
    			
    			}
				if(backupSource.getScheme().equals("file")){
                    
					File f = new File(backupSource.getPath());
					// Ensure that a directory name was specified
					if(f.isDirectory()){
						// all is well they specified only directory
					}else{
						String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","The backupSource parameter must only specify a directory and not a file name");
						throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","The backupSource parameter must specify a directory and not a file name.");
					}
				}
    		}else{
        		String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","The backupSource parameter must be non-null");
        		throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","The backupSource parameter must be non-null");    			
    		}
		} finally{ 

		}    	  	
		
		// If we are ready to start the restoration process, make the device unmanaged. Skip this if
		// validate == true.
		// If the restore is successful, there is no guarantee that the restored device
		// configuration matches what is persisted in the repository.  If the restore is unsuccessful,
		// we do not want to manage it - it may result in errors.
		
		if (!validate){
			ManagedSet set = this.getManagedSet();
			if (set !=null){
				set.removeDevice(this);
				logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
						this.getSymbolicName() + " has been removed from the ManagedSet: " + set.getName()); //$NON-NLS-1$   

			}		
		}
		
		Hashtable<String, byte[]> backupfilesTable = null;
		try {
			if (backupSource.getScheme().equals("file")){
				backupfilesTable = loadBackUpFilesforRestoreOperation(backupSource);
			}
		} catch (InvalidParameterException e) {
    		throw e; 
		} catch (IOException e) {
           throw e; 
		}
		
        BackgroundTask backgroundTask = BackgroundTask.createRestoreDeviceTask(this, cryptoCredentialName, validate, backupSource, backupfilesTable );
        progressContainer = backgroundTask.getProgressContainer();
        
        
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        ManagedSet managedSet = this.getManagedSet();
        if (managedSet != null) 
        {
            manager.enqueue(backgroundTask, managedSet);        	
        }
        else
        {
            manager.enqueue(backgroundTask, manager);        	
        }

        
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(progressContainer);
    }

    /*
     * 
     * 
     */
    protected void restoreDeviceAction(String credObjectName, URI backupFileLocation, boolean validate, Hashtable<String, byte[]> backupFilesTable) throws AMPException, 
    DeletedException, DirtySaveException, URISyntaxException, IOException, LockBusyException {
    	final String METHOD_NAME = "backUpDeviceAction"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME);	

    	//acquire the lock
    	this.lockNoWait();
    	try {
    		Commands commands;
    		commands = this.getCommands();
    		DeviceContext deviceContext;
    		deviceContext = this.getDeviceContext();	
    		
    		commands.restoreDevice(deviceContext, credObjectName, validate, backupFileLocation, backupFilesTable);        

    	} catch (AMPException e) {
    		// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
    		throw e;
    	} catch (DeletedException e) {
    		// TODO Auto-generated catch block
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			//e.printStackTrace();
    		throw e;		
    	} 
    	finally {
    		// cleanup
    		// release lock 
    		this.unlock();
    	}

    	logger.exiting(CLASS_NAME, METHOD_NAME);

    }    
    
    /*
     * Write the backup files returned by the backupDevice operation to the specified directory
     */
    private void saveFilesafterDeviceBackupOperation(URI backupLocation, String fileName, byte[] backupStringValue) throws IOException{
    	final String METHOD_NAME = "writeToBackUpFile";
    	File backupFile = null;
    	String backupFilename = null;

    	FileOutputStream outputStream = null;
    	try {        	
    		backupFilename = backupLocation.getPath() + File.separator + fileName;       	    

    		backupFile = new File(backupFilename);   	        	    

    		// Create file if it does not exist
    		boolean success = backupFile.createNewFile();  

    		if(!success){
    			// File already exists.  Issue a Warning
    			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, 
    					"A file with this name already exists and will be overwritten: " + backupFilename); //$NON-NLS-1$   
    			if(backupFile.delete()){
    				if ( !backupFile.createNewFile() ) {
    					logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,"Unable to create a new file " + backupFilename);
    				}
    			}
    		}

    		outputStream = new FileOutputStream(backupFile);  
    		outputStream.write(backupStringValue);

    	} catch (IOException e) {
    		logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
    				"Unable to create or open a Backup File: " + backupFilename); //$NON-NLS-1$      
    		throw e;
    	} finally {
    		outputStream.close();
    	}
    	
    }

    /*
     * Loads the information from previously call to backupDevice so that they can be used during a secure restore operation
     */
    private Hashtable<String, byte[]> loadBackUpFilesforRestoreOperation(URI backupLocation) throws IOException, InvalidParameterException
    {
    	final String METHOD_NAME = "readBackUpFiles";

    	Hashtable<String, byte[]> backupFiles = new Hashtable<String, byte[]>();
    	
        File folder = new java.io.File(backupLocation.getPath());
        File[] listOfFiles = folder.listFiles();
        boolean foundManifest = false;

        for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {

            String ext="";
            int mid= listOfFiles[i].getName().lastIndexOf(".");
            ext=listOfFiles[i].getName().substring(mid+1,listOfFiles[i].getName().length());  
            // Load files with extension of .tgz or .xml         
            if (ext.equalsIgnoreCase(RESTORE_TGZ_EXTENSION) || ext.equalsIgnoreCase(RESTORE_MANIFEST_EXTENSION)){            	
        		java.io.File backupFile = new java.io.File(backupLocation.getPath()+ java.io.File.separator + listOfFiles[i].getName());       
        		boolean success = backupFile.createNewFile();     
        		// success == true indicates file does not exist! This is an error, backup files should exist
        		if(success){
        			logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME,
        					"Cannot find file for restore:" + backupLocation.getPath()+ java.io.File.separator + listOfFiles[i].getName());
        			String message = Messages.getString("wamt.clientAPI.Device.invalidParameter","Unable to read backup file "+ backupFile);
        			throw new InvalidParameterException(message,"wamt.clientAPI.Device.invalidParameter","Unable to read backup file " + backupFile);			

        		}
        		if (backupFile.getName().equalsIgnoreCase(RESTORE_FILE_MANIFEST)){
        			foundManifest = true;
        		}
        		backupFiles.put(backupFile.getName(), new Blob(backupFile).getByteArray());   
    			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
    					"Loaded  backup file for restore:" + backupLocation.getPath()+ java.io.File.separator + listOfFiles[i].getName());          	
            }
  
          }
        }
        if (!foundManifest){
			logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME,
					"Could not locate:" + backupLocation.getPath()+ java.io.File.separator + this.RESTORE_FILE_MANIFEST);          	

        }
    	return backupFiles;
    }    

    /**
     * Quiesce all the domains on a device (managed domains and unmanaged domains).
     * This can be used before an operation that affects the entire device, like a firmware update.
     * 
     * @throws AMPException 
     * @throws DeletedException 
     * @throws UnsuccessfulOperationException 
     * 
     */
    public void quiesce() throws AMPException, DeletedException, UnsuccessfulOperationException {
        final String METHOD_NAME = "quiesceDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
        
		if (meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE)) {
			
	        int deviceQuiesceTimeout = getQuiesceTimeout(); 
	
	        Commands commands = getCommands();
	        DeviceContext dc = getDeviceContext();
	        commands.quiesceDevice(dc, deviceQuiesceTimeout);
	        
	        //Check the quiesce status
	        StringCollection allDom = getAllDomainNames();
	        
	        // add extra 2 seconds for each domain
	        long actualDeviceQuiesceTimeout = deviceQuiesceTimeout*1000L + allDom.size() * 2000L; 
	        
			for (int i=0; i < allDom.size(); i++) {
	    		if (actualDeviceQuiesceTimeout < 1000L) { 
	    			String message = Messages.getString("wamt.clientAPI.Device.quiesceFailed");
	    			throw new UnsuccessfulOperationException(message,"wamt.clientAPI.Device.quiesceFailed");
	    		}
	    		
				String domainName = allDom.get(i);
		        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "wait for: " + domainName, this);
		        long startTime = java.lang.System.currentTimeMillis();
		        
	    		Domain.waitForQuiesceActionComplete(commands, dc, domainName, false, (int)(actualDeviceQuiesceTimeout/1000L));
	    		
	    		long timeElapsed = java.lang.System.currentTimeMillis() - startTime;
	    		actualDeviceQuiesceTimeout = actualDeviceQuiesceTimeout - timeElapsed;
	        }
			
			try { // Add by Jason
                this.refreshCachedInfo();
            } catch (DeletedException e1) {
                // shouldn't happen, we are just constructing it
                String message = "Internal error: this should not happen"; //$NON-NLS-1$
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (AMPException e1) {
                // let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.ampCommError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (DatastoreException e1) {
                // skip this one, let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.RepError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            }
		} else {
            Object[] args = new Object[] {getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE, "quiesce" }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
        	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
        	
        	throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
    	}

		logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Unquiesce all the domains on a device (managed domains and unmanaged domains).
     * This method is complimentary to {@link #quiesce()}
     * 
     * @throws AMPException 
     * @throws DeletedException 
     * @throws UnsuccessfulOperationException 
     * 
     */
    public void unquiesce() throws AMPException, DeletedException, UnsuccessfulOperationException {
        final String METHOD_NAME = "unquiesceDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});
        
		if (meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE)) {
        
	        int deviceQuiesceTimeout = 60; 
	
	        Commands commands = getCommands();
	        DeviceContext dc = getDeviceContext();
	        commands.unquiesceDevice(dc);
	        
	        //Check the quiesce status
	        StringCollection allDom = getAllDomainNames();
	        
	        // add extra 2 seconds for each domain
	        long actualDeviceQuiesceTimeout = deviceQuiesceTimeout*1000L + allDom.size() * 2000L; 
	        
			for (int i=0; i < allDom.size(); i++) {
	    		if (actualDeviceQuiesceTimeout < 1000L) {
	    			String message = Messages.getString("wamt.clientAPI.Device.unquiesceFailed");
	    			throw new UnsuccessfulOperationException(message,"wamt.clientAPI.Device.unquiesceFailed");
	    		}
	    		
				String domainName = allDom.get(i);
				long startTime = java.lang.System.currentTimeMillis();
				
	    		Domain.waitForQuiesceActionComplete(commands, dc, domainName, true, (int)(actualDeviceQuiesceTimeout/1000L));
	    		
	    		long timeElapsed = java.lang.System.currentTimeMillis() - startTime;
	    		
	    		actualDeviceQuiesceTimeout = actualDeviceQuiesceTimeout - timeElapsed;
	        }
			try { // Add by Jason
                this.refreshCachedInfo();
            } catch (DeletedException e1) {
                // shouldn't happen, we are just constructing it
                String message = "Internal error: this should not happen"; //$NON-NLS-1$
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (AMPException e1) {
                // let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.ampCommError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            } catch (DatastoreException e1) {
                // skip this one, let the heartbeat daemon make further attempts
                String message = Messages.getString("wamt.clientAPI.Device.RepError"); //$NON-NLS-1$
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e1);
            }
		} else {
        	Object[] args = new Object[] {getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_QUIESCE, "unquiesce" }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
        	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
        	
        	throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
	    }

		logger.exiting(CLASS_NAME, METHOD_NAME);
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
     * This value only pertains to Firmware levels 3.8.1.0 or later. Earlier levels
     * of firmware do not support quiesce.
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
        
        if (timeout > 0 && timeout < DEVICE_QUIESCE_MIN_VALUE) {
        	timeout = DEVICE_QUIESCE_MIN_VALUE;
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                    "quiesce timeout value is nonzero and less than" + DEVICE_QUIESCE_MIN_VALUE + ", setting to " + DEVICE_QUIESCE_MIN_VALUE); //$NON-NLS-1$
        }

        this.getStoredInstance().setQuiesceTimeout(timeout);

		Manager manager = Manager.internalGetInstance();         
		manager.save(Manager.SAVE_UNFORCED);     

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
        		"device save setQuiesceTimeout: " + timeout, this); 
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
	}
	
	boolean isFirmwareManagentEnabled() throws DeletedException{
		boolean result = true;
		if(this.getFeatureLicenses().contains(MinimumFirmwareLevel.DP_ZGRPYPHON_LICENSE_FEATURE)){
			result = false;
		}
        return result;
	}
	
	boolean isRestoreEnabled() throws DeletedException{
		boolean result = true;
		if(this.getFeatureLicenses().contains(MinimumFirmwareLevel.DP_ZGRPYPHON_LICENSE_FEATURE)){
			result = false;
		}
        return result;
	}
	
    /**
     * Check if secure backup is enabled on the device.
     * Thie method is only supported on device with AMP v3.     
     * If it's invoked on a device that does not support AMP v3,
     * UnsupportedOperationException will be thrown.
     *   
     * @throws UnsupportedOperationException 
     */    
	public boolean isSecureBackupSupported(){
		String ampVer = null;
		
		try {
			ampVer = getCurrentAMPVersion();
		} catch (DeletedException e) {
			// ignore
		}
		
		if(ampVer!=null && ampVer.equals("3.0")){
			return secureBackupSupported;			
		}else{
			throw new UnsupportedOperationException();
		}
	}

	/**
     * Check if the device is primary.
     * 
     */ 
	public boolean isPrimary(){
		return isPrimary;
	}

	/**
     * Assert license acceptance for subsequent firmware update. 
     * 
     */ 
	public void acceptLicenseForFirmware() {
		this.licenseAccepted = true;
	}
	
	/**
	 * Get Domain Status on device. 
	 * @return {@link DomainStatus} 
	 * @throws DeletedException 
	 * @throws AMPException 
	 */
	DomainStatus getDomainStatus(Domain domain) throws InvalidParameterException, DeletedException {
		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
		
		if ( domain == null ) { // in case it gets null parameter
			String message = Messages.getString("wamt.clientAPI.domain.invalidParameter");
			InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.domain.invalidParameter"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
            throw(e);
		}		
		String domainName = domain.getName();
		DomainStatus ds = this.managedDomainsStatus.get(domainName);
		
		if ( ds == null  ) { 
			// Not in the managedDomainsStatus Map, check if it is a managed domain
			Domain mgtDomain = this.getManagedDomain(domainName);
			if ( mgtDomain != null ) { 
				// The domain is a managed domain, but not in the managedDomainsStatus Map.
				// Return the unknown satus for the time being, the status will be updated once the probeStatusofDomain() is invoked. 
				ds = DomainStatus.UNKNOWN_DOMAIN_STATUS;
				this.managedDomainsStatus.put(domain.getName(), ds);				
			}
			else { 
				// Not in the managedDomainsStatus Map, nor is the managed domain, meaning the domain could be removed				
				String message = Messages.getString("wamt.clientAPI.domain.domainDeleted");
	            DeletedException e = new DeletedException(message,"wamt.clientAPI.domain.domainDeleted"); //$NON-NLS-1$
	            logger.throwing(CLASS_NAME, METHOD_NAME, e); 
	            throw(e);
			}
        }
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return ds;	
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
        // Add this device to tag
        storedTag.add(this.persistence);        
        this.persistence.add(storedTag);        
        manager.save(Manager.SAVE_UNFORCED);
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Add Tag name:" + name + "Tag value:" + value +
        			" to the device: " + this.getDisplayName());
        
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
        
        StoredTag[] tags = this.getStoredInstance().getTags();
        int iSize = tags.length;
        if ( iSize > 0 ) {
        	StoredDevice storedDevice = this.getStoredInstance();
        	boolean bChange = false;
        	for ( int i=0; i < iSize; i++ ) {
        		if ( tags[i].getName().equals(name) && tags[i].getValue().equals(value)) {
        			storedDevice.remove(tags[i]); //removes stored Device
        			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Remove Tag name:" + name + "Tag value:" + value +
        	        			" from the device: " + this.getDisplayName());
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
		final String METHOD_NAME = "removeTag(" + name + ")"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        if ( name == null || name.length() == 0 ) { // name is required
        	String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
        	throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
        }
        StoredTag[] tags = this.getStoredInstance().getTags();
        int iSize = tags.length;
        if ( iSize > 0 ) {
        	StoredDevice storedDevice = this.getStoredInstance();
        	boolean bChange = false;
        	for ( int i=0; i < iSize; i++ ) {
        		if ( tags[i].getName().equals(name)) {
        			storedDevice.remove(tags[i]); //removes stored Device
        			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Remove Tag name:" + name + 
    	        			" from the device: " + this.getDisplayName());
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
        
        this.getStoredInstance().removeTags();  //removes stored Device
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
    	final String METHOD_NAME = "getTagValues("+ name + ")"; //$NON-NLS-1$
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
     * reboot this device.
     */
    public ProgressContainer reboot() throws DeletedException, FullException {
    	final String METHOD_NAME = "deploySourceFirmwareVersion"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	ProgressContainer progressContainer = null;
    	
    	// Set and deploy a firmware version.
        BackgroundTask backgroundTask = BackgroundTask.createRebootDeviceTask(this);
        progressContainer = backgroundTask.getProgressContainer();          
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        ManagedSet managedSet = this.getManagedSet();
        if (managedSet != null) {
            manager.enqueue(backgroundTask, managedSet);        	
        }
        else {
            manager.enqueue(backgroundTask, manager);        	
        }       

        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(progressContainer);
    }
   
    protected void rebootDeviceAction(ProgressContainer progressContainer) 
    throws AMPException, DatastoreException, DeletedException, IOException,	UnsuccessfulOperationException { 
    	final String METHOD_NAME = "rebootDeviceAction"; //$NON-NLS-1$
    	logger.entering(CLASS_NAME, METHOD_NAME);
    	
    	// Check if the device is under reboot
    	ManagementStatus deviceManagementStatus = getManagementStatusOfDevice();
        if ((deviceManagementStatus != null) && 
                (deviceManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {           
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "The device " + this.getDisplayName() + " is in progress of being rebooted."); //$NON-NLS-1$
            
            Object[] params = { this.getDeviceContext().getHostname(), Integer.toString(this.getDeviceContext().getAMPPort()) };
            String message = Messages.getString("wamt.clientAPI.device.rebootFailed", params); //$NON-NLS-1$
			UnsuccessfulOperationException e = new UnsuccessfulOperationException(message, "wamt.clientAPI.device.rebootFailed", params); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
        }
    	
    	// if the device is in the middle of a firmware upgrade, it may not respond to AMP requests.
        // So skip the heartbeat processing, and try again at the next timer pop.
        ManagementStatus firmwareManagementStatus = getManagementStatusOfFirmware();
        if ((firmwareManagementStatus != null) && 
                (firmwareManagementStatus.getEnum().equalsTo(ManagementStatus.Enumerated.IN_PROGRESS))) {
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "The firmware for managed device " + this.getDisplayName() +
            		" is in progress of being updated."); //$NON-NLS-1$
            
            Object[] params = { this.getDeviceContext().getHostname(), Integer.toString(this.getDeviceContext().getAMPPort()) };
			String message = Messages.getString("wamt.clientAPI.device.rebootFailed", params); //$NON-NLS-1$
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.clientAPI.device.rebootFailed", params); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
        }    	
    
    	boolean wasSuccessful = false;
    	//Signaler signal = new Signaler(this, fv, null);
    	Manager manager = Manager.internalGetInstance();

    	ManagedSet managedSet = null;
    	try {
    		managedSet = this.getManagedSet();
    	} catch (DeletedException e) {
    	}
    
    	lockWait();
    	try {
    		setManagementStatusOfDevice(ManagementStatus.Enumerated.IN_PROGRESS);
    		//signal.sendStart();
        
    		//	Quiesce the device before updating the firmware
    		if (!getDeviceType().equals(DeviceType.XC10)) {
    			quiesce();
    		}
        
    		Object[] args = new Object[] {this.getDisplayName() };
    		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, " reboot " + this); //$NON-NLS-1$
    		progressContainer.incrementCurrentStep(1,"wamt.clientAPI.Device.DeviceReboot_txt", args);   //$NON-NLS-1$                    
        
    		// Use AMP call to reboot the device
    		Commands commands = getCommands();   
    		DeviceContext deviceContext = getDeviceContext(); 
    		commands.reboot(deviceContext);
        
    		/*
    		 * Since it will take a few minutes for the device to reboot, we should wait here until the device comes back up 
    		 * because there might be other operations queued for it.
    		 * Doesn't make sense to let those other operations fail when we know the device is going to be non-responsive for the next few
    		 * minutes. We should also resubscribe to the device.
    		 */
        
    		//The ping command must be from V1 commands, since the V2 ping 
    		//cannot correctly ping a V1 device
    		Commands v1commands = null;
    		if (getDeviceType().equals(DeviceType.XC10)){
    			v1commands = commands;
    		}else{
    			v1commands = getV1Commands();
    		}
        
	        int installSecondsTotal = 150;  // takes about 70-90 via WebGUI
	        int retrySecondsInterval = 10;
	        int waitMinutesTotal = 5;
	        try {
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "waiting for device to reboot, sleeping " + //$NON-NLS-1$
	                        installSecondsTotal + " seconds"); //$NON-NLS-1$
	            // start by sleeping for a while so the device can reboot
	            Thread.sleep(installSecondsTotal * 1000L);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "sleep #1 interrupted"); //$NON-NLS-1$
	        }
	        
	        // try to ping the device for a total of 5 minutes. Figure out the end time.
	        Calendar end = new GregorianCalendar();
	        end.add(Calendar.MINUTE, waitMinutesTotal);
	        
	        boolean isUp = false;
	        while (!Thread.currentThread().isInterrupted() && !isUp) {
	        	// if we have been trying for >5 minutes then give up
	        	Calendar now = new GregorianCalendar();
	        	if (now.after(end)) {
	        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "gave up waiting for device to respond after " + //$NON-NLS-1$
	        					waitMinutesTotal + " minutes"); //$NON-NLS-1$
	        		break;
	        	}
	        	try {
	        		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "checking if device is up yet..."); //$NON-NLS-1$
	        		PingResponse pingResponse = 
	        			v1commands.pingDevice(deviceContext, manager.getSubscriptionId(isPrimary));
	        		if (pingResponse != null) {
	        			isUp = true;
	        			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "looks like device is up now, subscription state = " +  //$NON-NLS-1$
	        						pingResponse.getSubscriptionState());
	        			refreshCachedInfo(); //TCB - added this so we get the right AMP commands
                    
	        			// Recover lost subscription only if the device is in a managed set
	        			if(!getDeviceType().equals(DeviceType.XC10) && managedSet != null) {
	        				recoverFromLostSubscription(progressContainer);
	        			}
	        		} else {
	        			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "pingResponse is null, sleeping " + //$NON-NLS-1$
	        					retrySecondsInterval + " seconds."); //$NON-NLS-1$
	        			Thread.sleep(retrySecondsInterval * 1000);
	        		}
	        	} catch (AMPIOException e) {
	        			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "AMPIOException, looks like device is not up yet, waiting another " + //$NON-NLS-1$
                            retrySecondsInterval + " seconds"); //$NON-NLS-1$
	        			try {
	        				Thread.sleep(retrySecondsInterval * 1000);
	        			} catch (InterruptedException e1) {
	        					logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "sleep #3 interrupted"); //$NON-NLS-1$
	        					Thread.currentThread().interrupt();
	        			}
	        	} catch (InterruptedException e) {
	        			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "sleep #2 interrupted"); //$NON-NLS-1$
	        			Thread.currentThread().interrupt();
	        	}
	        }
        
	        if (isUp) {
	        	setManagementStatusOfDevice(ManagementStatus.Enumerated.SYNCED);
	        	wasSuccessful = true;
	        } else {
	        	args = new Object[] {this.getDisplayName()};//, fv.getAbsoluteDisplayName()};
	        	setManagementStatusOfDevice(ManagementStatus.Enumerated.UNREACHABLE);
	        	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
	        			Messages.getString("wamt.clientAPI.Device.devNoComeUp", args));  //$NON-NLS-1$
	        }
    	} finally {
    		unlock();
    		if (!wasSuccessful) {
    			// set the ManagementStatus to ERROR
    			setManagementStatusOfDevice(ManagementStatus.Enumerated.ERROR);
    		}
    		//signal.sendEnd(wasSuccessful);
    	}
    	logger.exiting(CLASS_NAME, METHOD_NAME);
    }   

    /**
     * Try to add certs sent from appliance to the file
     * @param hostName hostname of appliance
     * @param port post to connect
     * @param password password of certificate file
     * @param certFile the absolute path of certificate file
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws KeyManagementException
     */
    private void createNewCertFile(DeviceContext deviceContext) {
    	final String METHOD_NAME = "createNewCertFile"; 
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        final String errorMsg = "exception is thrown when trying to add new cert to file: " + KeyStoreInfo.keyStoreFile + 
					".\n It might cause the exception of certificate expiration later"; //$NON-NLS-1$
        
        String hostName = deviceContext.getHostname();
        int port = deviceContext.getAMPPort();
        
        InputStream in = null;
        KeyStore ks = null;
        
        try {
        	ks = KeyStore.getInstance(KeyStore.getDefaultType());
        	
        	// try to get the dpacerts
            File file = new File(KeyStoreInfo.keyStoreFile);            
            if ( file.exists() ) { // file exists            	
            	in = new FileInputStream(file);
            	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Loading KeyStore " + file.getAbsolutePath() + "...");//$NON-NLS-1$
            	ks.load(in, KeyStoreInfo.keyStorePassword.toCharArray());
            	in.close();
            }
            else // open a empty key store
            	ks.load(null, null);
        	
	        SSLContext context = SSLContext.getInstance("TLS");
	        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        tmf.init(ks);
	        X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
	        AddTrustManager tm = new AddTrustManager(defaultTrustManager);
	        context.init(null, new TrustManager[]{tm}, null);
	        SSLSocketFactory factory = context.getSocketFactory();
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Opening connection to " + hostName + ":" + port + "...");//$NON-NLS-1$
	        SSLSocket socket = null;
	        try {
	        	socket = (SSLSocket) factory.createSocket(hostName, port);
	        } catch (UnknownHostException e) {
	        	String errMsg = "Opening connection to " + hostName + ":" + port + " failed, unable to get the new certificate.";
	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, errMsg);//$NON-NLS-1$
	        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
	        	return;
	        }
	        
	        socket.setSoTimeout(10000);
	        try {
	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Starting SSL handshake...");//$NON-NLS-1$
	            socket.startHandshake();
	            socket.close();
	            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
	            		"All certificates are already trusted, no need to add cert");//$NON-NLS-1$
	            return;
	        } catch (SSLException e) {
	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
	        			"Certificate is not trusted, need to add cert to file");//$NON-NLS-1$	        	
	        }

	        X509Certificate[] chain = tm.chain;
	        if (chain == null) {
	        	logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Could not obtain server certificate chain");//$NON-NLS-1$
	            return;
	        }
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Server sent " + chain.length + " certificate(s):");//$NON-NLS-1$
	        for (int i = 0; i < chain.length; i++) {
	            X509Certificate cert = chain[i];
	            KeyStoreInfo.printCert(cert);
	        }
	        
	        // Add cert to the trust store
	        for (int i = 0; i < chain.length; i++) {
	        	X509Certificate cert = chain[i];        
	        	String alias = hostName + "-" + (i+1);
	        	ks.setCertificateEntry(alias, cert);
	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
	        			"Added certificate to new keystore using alias '" + alias + "'");//$NON-NLS-1$
	        }
        } catch (NoSuchAlgorithmException e1) {
			// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "NoSuchAlgorithmException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);        
        } catch (KeyStoreException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "KeyStoreException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (CertificateException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "CertificateException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (IOException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "IOException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (KeyManagementException e1 ) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "KeyManagementException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } finally {
        	if ( in != null  ) { 
        		try {
        			in.close();
        		} catch (IOException e1) {
                	// Just log it
        			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "IOException "+ errorMsg); //$NON-NLS-1$
        			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
                }
        	}
        }          
        
        OutputStream out = null;
        try {
        	// Save certificate to file
	        out = new FileOutputStream(KeyStoreInfo.keyStoreFile);
	        ks.store(out, KeyStoreInfo.keyStorePassword.toCharArray()); // Write to file
	        out.close();
        } catch (NoSuchAlgorithmException e1) {
        	logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "NoSuchAlgorithmException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (KeyStoreException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "KeyStoreException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (CertificateException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "CertificateException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } catch (IOException e1) {
        	// Just log it
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "IOException "+ errorMsg); //$NON-NLS-1$
			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        } finally {
        	if ( out != null ) {
        		try {
        			out.close();
        		} catch (IOException e1) {
        			// Just log it
        			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "IOException "+ errorMsg); //$NON-NLS-1$
        			logger.throwing(CLASS_NAME, METHOD_NAME,e1);
        		}
        	}
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
	
    private static class AddTrustManager implements X509TrustManager {
        private final X509TrustManager tm;
        private X509Certificate[] chain;

        AddTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
}