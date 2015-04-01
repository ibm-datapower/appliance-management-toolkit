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


package com.ibm.datapower.amt.dataAPI.local.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlbeans.XmlOptions;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Credential;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicyType;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.URLSource;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDevice;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPFirmware;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPManagedSet;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPManager;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPManagerDocument;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPTag;
/**
 * The Repository object used for executing persistence operations such as reading and 
 * writing Stored objects to the local file system
 * <p>
 * This implementation provides a public static Repository
 * getInstance() method to return the singleton instance of Repository object. The clientAPI
 * starts up the repository when the Manager is initialized. It obtains an instance of the singleton 
 * Repository in order to make make modifications to persisted data.  
 * <p>
 * The data to be persisted is written to a directory configured by the caller.
 * <p>The Repository objects transforms all the Stored objects to XML Objects before
 * saving them in the local file system.  It also determines the WAMT.Repository.xml and loads all
 * all persisted XMLobjects and transforms them to Java objects.
 * </p>
 * @see com.ibm.datapower.amt.dataAPI.Repository
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(
		justification="<Write to static fields (collectingGarbage $$ inUseBlobFiles) from instance method>",
		value="ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") 
public class RepositoryImpl implements Repository
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;

   private static Credential credential = null;
   private Hashtable allDomains = null;
   private Hashtable allDevices = null;
   private Hashtable allManagedSets = null;
   private Hashtable allFirmware = null;
   private Hashtable allTags = null;
   private int maxVersionsToStore = 0;

   // The corresponding XML object. It is created when this object
   // is being persisted into an XML file.
   private DPManager xmlObject = null;

   // Mapping from XML objects to in-memory objects
   // Used to load repository objects from an XML file
   Hashtable mapXmlObjectsToMemObjects;

   // Garbage collection flag
   // When it is true, the in-use blob files' paths
   // will be saved in the blob files' table
   static private boolean collectingGarbage = false;

   // The collection contains all the in-use blob files' names
   // It will be used for garbage collection
   static private Collection inUseBlobFiles = null;

   private volatile static RepositoryImpl singleton = null;

   //public static Logger TRACE = null;
   //public static MessageLogger MESSAGE = null;

   //private static final String className = "RepositoryImpl";
   private static final String XMI_VERSION = "2.0";
   private static final String XML_ID = "WAMTManager_0";

   static private final String BLOB_FILE_PREFIX = "Blob";
   static private final String BLOB_FILE_SUFFIX = "bin";

   static private final String REPOSITORY_FILE_NAME = "WAMT.repository.xml";
   static private final String LAST_VERSION_REPOSITORY_FILE_NAME = "lastVersion_WAMT.repository.xml";
   static private final String TEMPORARY_REPOSITORY_FILE_NAME = "temp_WAMT.repository.xml";
   static private final String REPOSITORY_DIRECTORY_PROPERTY_NAME = "RepositoryDirectory";


   // The following values should be loaded from a properties file
   private static String repository_dir = null;

   private static final String WAMT_REPOS_HOME = "WAMT_REPOS_HOME";
   private static final String DEFAULT_WAMTCONFIG_HOME_PROP = "user.home";
   private static final String DEFAULT_WAMTCONFIG_REPOSITORY_DIR = "WAMTRepository";

/*   static {
      initializeLogger();
   }*/
   private static final String CLASS_NAME = RepositoryImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }

   private RepositoryImpl()
   {
      init();
   }

   /*
    * Initialize hash tables for the repository object
    */
   private void init()
   {
	  final String METHOD_NAME = "init";
      allDomains = new Hashtable();
      allDevices = new Hashtable();
      allManagedSets = new Hashtable();
      allFirmware = new Hashtable();    
      allTags = new Hashtable();
           
      if (repository_dir == null){
    	  repository_dir = System.getenv(WAMT_REPOS_HOME);
    	  if (repository_dir == null ){
     		  // Use user.home directory if environment variable is not configured 
    		  repository_dir = System.getProperty(DEFAULT_WAMTCONFIG_HOME_PROP) + File.separator +
    		  DEFAULT_WAMTCONFIG_REPOSITORY_DIR + File.separator;
    		  
    	  }    	  
      }

      if (!repository_dir.endsWith(File.separator)){  			    		  
			  repository_dir = repository_dir + File.separator;		      		  
      }	        
      
      // Create the repository directory if it does not exist
      // Create one directory
      try{
    	  if ((new File(repository_dir)).isDirectory()){
    		  logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,"Repository directory exists: " + repository_dir);                                                         
    		  
    	  }else{
        	  boolean success = (new File(repository_dir)).mkdirs();  
        	  if (success) {
        		  logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,"Created Repository directory: " + repository_dir);                                                        

        	  }else{
        		  logger.logp(Level.SEVERE,CLASS_NAME, METHOD_NAME,"Unable to create Repository Directory " + repository_dir);                                             	  
        	      String message = Messages.getString("wamt.dataAPI.RepositoryFactory.failedToCreateDirectory", repository_dir); 
        	      throw new DatastoreException(message,"DataStoreException");           	          
        	  }    		  
    	  }

      }catch(Exception e){
    	  logger.logp(Level.SEVERE,CLASS_NAME, METHOD_NAME,"Unable to create Repository Directory",e);                                                     
      }
      
      logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,"Repository path " + repository_dir);                                                         

   }

   /**
    * 
    */
   static final RepositoryImpl getInstance()
   {
      if (singleton == null)
          singleton = new RepositoryImpl();
      return singleton;
   }
   
  /**
   * This method must be implemented to return the singleton instance of Repository.  This is
   * invoked when the Manager is first initialized. 
   * 
   * Refer to the package overview for the specific parameters used by this implementation.
   * 
   * @param cred The credential to use for this implementation
   * 
   * <inheritDoc />
   */

  static final public RepositoryImpl getInstance(Credential cred) {
	  final String METHOD_NAME = "init";
      logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"Credential object: " + cred);
      credential = cred;
      repository_dir = (String)credential.getProperty(REPOSITORY_DIRECTORY_PROPERTY_NAME);
      if ( repository_dir != null ) {
    	  System.setProperty(WAMT_REPOS_HOME, repository_dir);
      }
      return getInstance();
  }


   /**
    * @return Returns the repository_dir.
    */
   static String getRepositoryDir()
   {
      return repository_dir;
   }
   /**
    * <p>
    * Note:  The Local File System implementation does not implement this method. To
    * backup the repository, make copies of all the files in configured repository directory. 
    * </p>
    * <inheritDoc /> 
    */
   public void exportAll(OutputStream outputStream)
   {
	   final String METHOD_NAME = "exportAll()";
       logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
    		   "exportAll is not implemented!!!"); //$NON-NLS-1$ 
       return;
   }

   /**
    * <p>
    * Note:  The Local File System implementation does not implement this method. 
    * </p>
    * <inheritDoc /> 
    */
   public void importAll(InputStream inputStream) throws DatastoreException
   {
	   final String METHOD_NAME = "importAll()";
       logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
    		   "importAll is not implemented!!!"); //$NON-NLS-1$ 
       return;

   }

   /**
    * <p>
    * Note: The Local File System implementation adds or changes elements in the WAMT.Repository.xml file during 
    * a save operation. It may also persist separate bin files for deployed firmware versions and domain configurations.
    * This implementation ignores the forceSave parameter.
    * </p>  
    * <inheritDoc /> 
    */
   public void save(boolean forceSave) throws DirtySaveException
   {
      final String METHOD_NAME = "save";
      logger.entering(CLASS_NAME, METHOD_NAME);

      String lastVersionRepositoryFilePath = repository_dir + LAST_VERSION_REPOSITORY_FILE_NAME;
      String tempFilePath = repository_dir + TEMPORARY_REPOSITORY_FILE_NAME;
      String repostoryFilePath = repository_dir + REPOSITORY_FILE_NAME;

      //Save to a temporary file
      save(tempFilePath);

      File repositoryFile = new File(repostoryFilePath);
      File lastVersionRepositoryFile = new File(lastVersionRepositoryFilePath);
      File tempFile = new File(tempFilePath);

      if (lastVersionRepositoryFile.exists())
      {
         if ( !lastVersionRepositoryFile.delete() )
        	 logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Deletes the file: " + lastVersionRepositoryFile.getName() + " failed.");
      }

      // Keep the last version of repository file
      if ( !repositoryFile.renameTo(lastVersionRepositoryFile) )
    	  logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Unable to rename file " + repostoryFilePath);

      // Rename the temporary file to make it be the current repository file
      if ( !tempFile.renameTo(repositoryFile) )
    	  logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Unable to rename file " + tempFilePath);

      logger.exiting(CLASS_NAME, METHOD_NAME);
   }

   /*
    * Persist the in-memory repository objects into an XML file
    *
    */

   private synchronized void save(String repositoryFilePath) throws DirtySaveException
   {
      final String METHOD_NAME = "save(String repositoryFilePath)";
      logger.entering(CLASS_NAME, METHOD_NAME, 
    		  new Object[]{repositoryFilePath});

      // Create the XML document
      DPManagerDocument dpmDoc = DPManagerDocument.Factory.newInstance();
      // delete the old XML objects
      this.deleteXMLObjects();
      // Create an XML object for RepositoryImpl
      DPManager dpManager = dpmDoc.addNewDPManager();

      try
      {
         // Transform the RepositoryImpl object into an XML object
         this.toXMLObject(dpManager);

         File repositoryFile = new File(repositoryFilePath);
         // Save the XML objects into the file, repositoryFile
         dpmDoc.save(repositoryFile, new XmlOptions().setSavePrettyPrint());
      }
      catch(Throwable th)
      {
         //MESSAGE.trap(th);
         //TRACE.throwing(className, METHOD_NAME, th);
         logger.throwing(CLASS_NAME, METHOD_NAME, th);     
    	 String message = Messages.getString("DataStoreException");          
         throw new DirtySaveException(message,"DataStoreException");
      }

      logger.exiting(CLASS_NAME, METHOD_NAME);
   }

   /*
    * Loads the repository objects from a XML file
    */

   void load(String repositoryFilePath) throws DatastoreException
   {
      final String METHOD_NAME = "load";
      logger.entering(CLASS_NAME, METHOD_NAME, repositoryFilePath);
      
      // Initialize the hash tables
      this.init();
      
      File repositoryFile = new java.io.File(repositoryFilePath);

      if (!repositoryFile.exists())
      {
         // The repository file does not exist. Failed to be created by call to init().
         // so we will start with an empty repository.
    	 logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.dataAPI.RepositoryFactory.exInvokeGetInst"),
                 new Object[] {repositoryFilePath}); 
         return;
      }

      try
      {
         // Initialize the mapping table
         this.mapXmlObjectsToMemObjects = new Hashtable();
         // Parse the XML repository file
         DPManagerDocument dpmDoc =
            DPManagerDocument.Factory.parse(repositoryFile);
         // Transform the XML object into a RepositoryImpl object
         fromXMLObject(dpmDoc.getDPManager());
         // delete the mapping table
         this.mapXmlObjectsToMemObjects = null;
      }
      catch(Throwable e)
      {
    	  e.printStackTrace();
         logger.throwing(CLASS_NAME, METHOD_NAME, e);
    	 String message = Messages.getString("DataStoreException");          
         throw new DatastoreException(message,"DataStoreException");
      }

      logger.exiting(CLASS_NAME, METHOD_NAME);
   }

   /**
    * <p>
    * Note: The Local File System implementation adds a device element to the WAMT.repository.xml file during 
    * a save operation to save all information related to the device.
    * </p> 
    * <inheritDoc /> 
    */
   public StoredDevice createDevice(String deviceID, String serialNumber, String name,
         DeviceType deviceType, ModelType modelType, String hostname,
		 String userid,String password, int HLMport, int guiPort, String ampVersion)
         throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME="createDevice(String, String, DeviceType," +
      		"ModelType,String,String,String,int,int)";
      logger.entering(CLASS_NAME,METHOD_NAME);

      StoredDeviceImpl newDevice = new StoredDeviceImpl(allDevices, deviceID, serialNumber,
            name,deviceType,modelType,hostname,userid,password, HLMport, guiPort, ampVersion);

      allDevices.put(newDevice.getPrimaryKey(),newDevice);

      logger.exiting(CLASS_NAME,METHOD_NAME,newDevice);
      return newDevice;
   }
  
   /**
    * <p>
    * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
    * create operation to save all information related to the domain within the 
    * containing device element.
    * </p>  
    * <inheritDoc />  
    */
   public StoredDomain createDomain(StoredDevice device,
             String domainName) throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME = "createDomain";
      logger.entering(CLASS_NAME,METHOD_NAME);

      StoredDomainImpl newDomain = new StoredDomainImpl(allDomains,device ,domainName);

      allDomains.put(newDomain.getPrimaryKey(),newDomain);
      newDomain.updateDevice();

      logger.exiting(CLASS_NAME,METHOD_NAME,newDomain);
      return newDomain;
   }
   
   /* (non-Javadoc)
    * 
    */   
   public StoredDomain updateDomain(StoredDomain domain
           ) throws AlreadyExistsInRepositoryException, DatastoreException
 {
	   final String METHOD_NAME = "updateDomain";
	   logger.entering(CLASS_NAME,METHOD_NAME);

	   StoredDomainImpl newDomain = (StoredDomainImpl)domain;
	   newDomain.updateDevice();
	   allDomains.put(newDomain.getPrimaryKey(),domain);          

	   logger.exiting(CLASS_NAME,METHOD_NAME,newDomain);
	   return newDomain;
 }
   

   /**
    * <p>
    * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
    * create operation to save all information related to the policy version within the 
    * containing deployment policy element.
    * </p>  
    * <inheritDoc />  
    */ 
    
   public StoredDeploymentPolicyVersion createDeploymentPolicyVersion(StoredDeploymentPolicy policy,
           Blob blob, String comment, Date timestamp) throws AlreadyExistsInRepositoryException, DatastoreException
 {
    final String METHOD_NAME = "createDeploymentPolicyVersion";
    logger.entering(CLASS_NAME,METHOD_NAME);
    
    StoredDeploymentPolicyVersionImpl newDeploymentPolicyVersion =
    	 new StoredDeploymentPolicyVersionImpl(policy, blob, comment, timestamp);
    	 
    newDeploymentPolicyVersion.setPolicyName(policy.getPolicyName());
    newDeploymentPolicyVersion.setPolicyDomainName(policy.getPolicyDomainName());    
    newDeploymentPolicyVersion.setPolicyType(policy.getPolicyType());  

    StoredDeploymentPolicyImpl deploymentPolicy = (StoredDeploymentPolicyImpl) policy;
    deploymentPolicy.addVersion(newDeploymentPolicyVersion);
    
    logger.exiting(CLASS_NAME,METHOD_NAME,newDeploymentPolicyVersion);
    return newDeploymentPolicyVersion;

 }

   /** 
   * <p>
   * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
   * create operation to save all information related to the policy version within the 
   * containing domain element.
   * </p> 
   * <inheritDoc />???
   */   
   public StoredDeploymentPolicyImpl createDeploymentPolicy(StoredDomain domain,
           String name, URLSource policyURL, String policyDomainName, DeploymentPolicyType policyType) throws AlreadyExistsInRepositoryException, DatastoreException
 {
    final String METHOD_NAME = "createDeploymentPolicy";
    logger.entering(CLASS_NAME,METHOD_NAME);
    StoredDeploymentPolicyImpl newDeploymentPolicyImpl = null;
    newDeploymentPolicyImpl = (StoredDeploymentPolicyImpl) domain.getDeploymentPolicy();
    if (newDeploymentPolicyImpl == null){
      newDeploymentPolicyImpl =
      new StoredDeploymentPolicyImpl(domain,name);  
  	  newDeploymentPolicyImpl.setPolicyDomainName(policyDomainName);
	  newDeploymentPolicyImpl.setPolicyName(name);    	
	  newDeploymentPolicyImpl.setPolicyType(policyType);
	  newDeploymentPolicyImpl.setPolicyURL(policyURL);      
    }else{
    	newDeploymentPolicyImpl.setPolicyDomainName(policyDomainName);
    	newDeploymentPolicyImpl.setPolicyName(name);    	
    	newDeploymentPolicyImpl.setPolicyType(policyType);
    	newDeploymentPolicyImpl.setPolicyURL(policyURL);
    }
    newDeploymentPolicyImpl.updatePolicyVersions();
    logger.exiting(CLASS_NAME,METHOD_NAME,newDeploymentPolicyImpl);
    return newDeploymentPolicyImpl;
 }   

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getDevice(java.lang.String)
    */
   public StoredDevice getDevice(String serialNumber)
   {
      final String METHOD_NAME = "getDevice";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredDeviceImpl result = null;
      Object o = allDevices.get(serialNumber);
      if ((o !=null) && (o instanceof StoredDeviceImpl))
      {
         result = (StoredDeviceImpl) o;
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,result);
      return result;
   }
   
   /*
    * (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getDeviceBySerialNumber(java.lang.String)
    */
   public StoredDevice[] getDeviceBySerialNumber(String serialNumber)
   {
      final String METHOD_NAME = "getDevice";
      logger.entering(CLASS_NAME,METHOD_NAME);
      
      Vector<StoredDeviceImpl> tmpDevice = new Vector<StoredDeviceImpl>();
      Enumeration e = allDevices.keys();      
      while (e.hasMoreElements())
      {
         Object key = e.nextElement();
         Object value = allDevices.get(key);
         if ((value != null) && (value instanceof StoredDeviceImpl))
         {
            if ( ((StoredDeviceImpl) value).getSerialNumber().equals(serialNumber) ) {
            	tmpDevice.add((StoredDeviceImpl)value);
            }	
         }
      }
      
      int iSize = tmpDevice.size();
      StoredDevice[] resultArray = new StoredDevice[iSize];
      for ( int i=0; i < iSize; i++ ) {
    	  resultArray[i] = tmpDevice.get(i);
      }
      logger.exiting(CLASS_NAME,METHOD_NAME,resultArray);
      return resultArray;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getDevices()
    */
   public StoredDevice[] getDevices()
   {
      final String METHOD_NAME = "getDevices";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredDevice[] resultArray = new StoredDevice[allDevices.size()];
      Enumeration e = allDevices.keys();

      int i = 0;
      while (e.hasMoreElements())
      {
         Object key = e.nextElement();
         Object value = allDevices.get(key);
         if ((value != null) && (value instanceof StoredDeviceImpl))
         {
            resultArray[i++]=(StoredDeviceImpl) value;
         }
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,resultArray);
      return resultArray;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getUnmanagedDevices(com.ibm.datapower.amt.DeviceType)
    */
   public StoredDevice[] getUnmanagedDevices(DeviceType desiredDeviceType)
   {
      final String METHOD_NAME = "getUnmanagedDevices";
      logger.entering(CLASS_NAME,METHOD_NAME);
      Vector matchingDevices = new Vector();
      Enumeration e = allDevices.keys();

      //walk the hashtable looking for elements that are unmanaged and
      //match the desiredDeviceType.

      while (e.hasMoreElements())
      {
         Object key = e.nextElement();
         Object value = allDevices.get(key);
         if ((value != null) && (value instanceof StoredDeviceImpl))
         {
            StoredDeviceImpl thisDevice = (StoredDeviceImpl) value;
            if (thisDevice.getManagedSet()==null)
            {
               if (desiredDeviceType != null)
               {
                  if (thisDevice.getDeviceType().equals(desiredDeviceType))
                     matchingDevices.add(thisDevice);
               }
               else
               {
                  matchingDevices.add(thisDevice);
               }
            }  // else device is managed
         } //else should not occur
      }

      //load matching devices into return array

      StoredDevice[] resultArray = new StoredDevice[matchingDevices.size()];
      Iterator iter = matchingDevices.iterator();
      int i=0;
      while (iter.hasNext())
      {
         resultArray[i++] = (StoredDeviceImpl) iter.next();
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,resultArray);
      return resultArray;
   }
   
   public StoredTag getTag(String name)
   {
      final String METHOD_NAME = "getTag";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredTagImpl result = null;
      Object o = allTags.get(name);
      if ((o !=null) && (o instanceof StoredTagImpl))
      {
         result = (StoredTagImpl) o;
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,result);
      return result;
   }
   
   public StoredTag[] getTags()
   {
      final String METHOD_NAME = "getTags";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredTag[] resultArray = new StoredTag[allTags.size()];
       Enumeration e = allTags.keys();

       int i = 0;
       while (e.hasMoreElements())
       {
          Object key = e.nextElement();
          Object value = allTags.get(key);
          if ((value != null) && (value instanceof StoredTagImpl))
          {
             resultArray[i++]=(StoredTagImpl) value;
          }
       }

      logger.exiting(CLASS_NAME,METHOD_NAME,resultArray);
      return resultArray;
   }
   

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getManagedSet(java.lang.String)
    */
   public StoredManagedSet getManagedSet(String name)
   {
      final String METHOD_NAME = "getManagedSet";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredManagedSetImpl result = null;
      Object o = allManagedSets.get(name);
      if ((o !=null) && (o instanceof StoredManagedSetImpl))
      {
         result = (StoredManagedSetImpl) o;
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,result);
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getManagedSets()
    */
   public StoredManagedSet[] getManagedSets()
   {
      final String METHOD_NAME = "getManagedSets";
      //TRACE.entering(className,METHOD_NAME);
      StoredManagedSet[] resultArray =
         new StoredManagedSet[allManagedSets.size()];
      Enumeration e = allManagedSets.keys();

      int i = 0;
      while (e.hasMoreElements())
      {
         Object key = e.nextElement();
         Object value = allManagedSets.get(key);
         if ((value != null) && (value instanceof StoredManagedSetImpl))
         {
            resultArray[i++]=(StoredManagedSetImpl) value;
         }
      }

      //TRACE.exiting(className,METHOD_NAME,resultArray);
      return resultArray;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getFirmwares()
    */
   public StoredFirmware[] getFirmwares()
   {
      final String METHOD_NAME = "getFirmwares";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredFirmware[] resultArray =
         new StoredFirmware[allFirmware.size()];
      Enumeration e = allFirmware.keys();

      int i = 0;
      while (e.hasMoreElements())
      {
         Object key = e.nextElement();
         Object value = allFirmware.get(key);
         if ((value != null) && (value instanceof StoredFirmwareImpl))
         {
            resultArray[i++]=(StoredFirmwareImpl) value;
         }
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,resultArray);
      return resultArray;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getFirmware(com.ibm.datapower.amt.DeviceType,com.ibm.datapower.amt.ModelType,com.ibm.datapower.amt.StringCollection;)
    */
   public StoredFirmware getFirmware(DeviceType deviceType, ModelType modelType,
         StringCollection strictFeatures, StringCollection nonstrictFeatures)
   {
      final String METHOD_NAME = "getFirmware";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredFirmwareImpl result = null;
      String key = StoredFirmwareImpl.getPrimaryKey(deviceType, modelType, strictFeatures,
            nonstrictFeatures);
      Object o = allFirmware.get(key);
      if ((o !=null) && (o instanceof StoredFirmwareImpl))
      {
         result = (StoredFirmwareImpl) o;
      }

      logger.exiting(CLASS_NAME,METHOD_NAME,result);
      return result;
   }

   /** 
    * <p>
    * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
    * create operation to save all information related to the managed set within the 
    * containing manager element.
    * </p> 
    * <inheritDoc />
    */
   public StoredManagedSet createManagedSet(String name)
         throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME = "createManagedSet";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredManagedSetImpl newManagedSet = new StoredManagedSetImpl(allManagedSets,name);

      allManagedSets.put(newManagedSet.getPrimaryKey(),newManagedSet);

      logger.exiting(CLASS_NAME,METHOD_NAME,newManagedSet);
      return newManagedSet;
   }
   
   public StoredTagImpl createTag(String name, String value)
   throws AlreadyExistsInRepositoryException, DatastoreException
	{
		final String METHOD_NAME = "createTag";
		logger.entering(CLASS_NAME,METHOD_NAME);
		StoredTagImpl newTag = new StoredTagImpl(allTags,name, value);
		
		allTags.put(newTag.getPrimaryKey(),newTag);
		
		logger.exiting(CLASS_NAME,METHOD_NAME,newTag);
		return newTag;
	}

   /** 
     * <p>
     * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
     * create operation to save all information related to the firmware within the 
     * containing manager element. It will also contain firmware version elements for each 
     * level of firmware deployed. 
     * </p>
    * <inheritDoc />
    */
   public StoredFirmware createFirmware(DeviceType deviceType, ModelType modelType,
   		StringCollection strictFeatures, StringCollection nonstrictFeatures)
         throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME = "createFirmware";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredFirmwareImpl newFirmware = new StoredFirmwareImpl(allFirmware,
      		deviceType, modelType, strictFeatures, nonstrictFeatures);

      allFirmware.put(newFirmware.getPrimaryKey(),newFirmware);

      logger.exiting(CLASS_NAME,METHOD_NAME,(Object)newFirmware);
      return newFirmware;
   }

   /** 
    * <p>
    * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
    * create operation to save all information related to the firmware version within the 
    * containing firmware element. 
    * </p>
    * <inheritDoc />
    */
   public StoredFirmwareVersion createFirmwareVersion(
         StoredFirmware versionedObject, Blob blob, String level,
         Date manufactureDate, String userComment, Date timeStamp)
            throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME = "createFirmwareVersion";
      logger.entering(CLASS_NAME,METHOD_NAME);
      StoredFirmwareVersionImpl newFirmwareVersion =
         new StoredFirmwareVersionImpl(versionedObject,blob,
               level,manufactureDate,userComment,timeStamp);

      StoredFirmwareImpl firmware = (StoredFirmwareImpl) versionedObject;
      firmware.addVersion(newFirmwareVersion);

      logger.exiting(CLASS_NAME,METHOD_NAME,(Object)newFirmwareVersion);
      return newFirmwareVersion;
   }

   /** 
     * <p>
     * Note: The Local File System implementation adds an element to the WAMT.repository.xml file during 
     * create operation to save all information related to the StoredDomainVersion within the 
     * containing Domain element.
     * </p> 
    * <inheritDoc />
    */
   public StoredDomainVersion createDomainVersion(StoredDomain versionedObject,
         Blob blob, String userComment, Date timeStamp)
         throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String METHOD_NAME = "createDomainVersion";
      logger.entering(CLASS_NAME,METHOD_NAME);

      StoredDomainVersionImpl newDomainVersion =
         new StoredDomainVersionImpl(versionedObject,blob,userComment,timeStamp);

      StoredDomainImpl domain = (StoredDomainImpl) versionedObject;
      domain.addVersion(newDomainVersion);

      logger.exiting(CLASS_NAME,METHOD_NAME,newDomainVersion);
      return newDomainVersion;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#createSettings(com.ibm.datapower.amt.dataAPI.StoredManagedSet) */
//   public StoredSettings createSettings(StoredManagedSet managedSet) throws AlreadyExistsException,
//   DatastoreException
//   {
//      final String METHOD_NAME = "createSettings";
//      logger.entering(CLASS_NAME,METHOD_NAME);
//      StoredSettingsImpl newSettings = new StoredSettingsImpl(allSettings,managedSet);
//
//      allSettings.put(newSettings.getPrimaryKey(),newSettings);
//
//      logger.exiting(CLASS_NAME,METHOD_NAME,newSettings);
//      return newSettings;
//
//
//   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#createSettingsVersion(com.ibm.datapower.amt.dataAPI.StoredSettings, com.ibm.datapower.amt.clientAPI.Blob, java.lang.String, java.util.Date)
    */
//   public StoredSettingsVersion createSettingsVersion(
//         StoredSettings versionedObject, Blob blob, String userComment,
//         Date timeStamp) throws AlreadyExistsException, DatastoreException
//   {
//      final String METHOD_NAME = "createSettingsVersion";
//      logger.entering(CLASS_NAME,METHOD_NAME);
//      StoredSettingsVersionImpl newSettingsVersion =
//         new StoredSettingsVersionImpl(versionedObject,blob,userComment,timeStamp);
//
//      StoredSettingsImpl settings = (StoredSettingsImpl) versionedObject;
//      settings.addVersion(newSettingsVersion);
//
//      logger.exiting(CLASS_NAME,METHOD_NAME,newSettingsVersion);
//      return newSettingsVersion;
//   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#getMaxVersionsToStore()
    */
   public int getMaxVersionsToStore()
   {
      return this.maxVersionsToStore;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#setMaxVersionsToStore(int)
    */
   public void setMaxVersionsToStore(int maxVersions)
   {
      this.maxVersionsToStore = maxVersions;

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#shutdown()
    * Save the repository to an XML file and remove the hash tables
    * Remove unused blob files
    */
   public void shutdown()
   {
      final String METHOD_NAME = "shutdown";
      logger.entering(CLASS_NAME,METHOD_NAME);

      try
      {
         // Enable garbage collection
         collectingGarbage = true;
         inUseBlobFiles = new HashSet();

         // Save the repository
         save(true);

         // Do garbage collection
         removeUnusedBlobfiles();
      }
      catch(Exception e)
      {
         logger.throwing(CLASS_NAME, METHOD_NAME, e);
      }

      // Delete the hash tables
      this.allDomains = null;
      this.allDevices = null;
      this.allManagedSets = null;
      this.allFirmware = null;
//      this.allSettings = null;
      this.mapXmlObjectsToMemObjects = null;
      inUseBlobFiles = null;

      logger.exiting(CLASS_NAME,METHOD_NAME);
   }

   /*
    * Remove all the unused blob files in the repository directory
    */
   private  void removeUnusedBlobfiles()
   {
      final String METHOD_NAME = "removeUnusedBlobfiles";
      logger.entering(CLASS_NAME,METHOD_NAME);

      File repositoryDir = new File(repository_dir);

      String[] filesInRepostitoryDir = repositoryDir.list();

      for (int i=0; i<filesInRepostitoryDir.length; i++)
      {
         if (filesInRepostitoryDir[i].endsWith("bin"))
         {
            if (!inUseBlobFiles.contains(filesInRepostitoryDir[i]))
            {
               File unusedBlobFile = new File(repository_dir + filesInRepostitoryDir[i]);
               //logger.finer("Removing unused blob file, " + unusedBlobFile.getAbsolutePath());
               logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
            		   "Removing unused blob file, " + unusedBlobFile.getAbsolutePath()); //$NON-NLS-1$               
               if ( !unusedBlobFile.delete() )
            	   logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Unable to delete file " + repository_dir + filesInRepostitoryDir[i]);               
            }
         }
      }

      logger.exiting(CLASS_NAME,METHOD_NAME);
   }

   
/*   private static void initializeLogger()
   {
      DPConfigLoggerManager loggerManager = DPConfigLoggerManager.getInstance();
      TRACE = loggerManager.getLogger(DPConfigLoggerManager.REPOSITORY_COMPONENT);
      MESSAGE = MessageLogger.getLogger(MessageLogger.DPCONFIG_BUNDLE);
   }*/
   
   

   /*
    * @return Returns the allDevices.
    */
   Hashtable getAllDevices()
   {
      return allDevices;
   }
   /*
    * @return Returns the allDomains.
    */
   Hashtable getAllDomains()
   {
      return allDomains;
   }
   
   void setAllDomains(Hashtable allDomains) {
	   this.allDomains = allDomains;
   }
   /*
    * @return Returns the allFirmware.
    */
   Hashtable getAllFirmware()
   {
      return allFirmware;
   }
   /*
    * @return Returns the allManagedSets.
    */
   Hashtable getAllManagedSets()
   {
      return allManagedSets;
   }
   
   
   Hashtable getAllTags()
   {
      return allTags;
   }
   
//   /**
//    * @return Returns the allSettings.
//    */
//   public Hashtable getAllSettings()
//   {
//      return allSettings;
//   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.Repository#startup()
    */
   public void startup() throws DatastoreException
   {
      final String METHOD_NAME = "startup";
      logger.entering(CLASS_NAME,METHOD_NAME);

      // Disable garbage collection
      collectingGarbage = false;
      inUseBlobFiles = null;

      //load("c:\\tmp\\sample_WAMT_XMLbeans.xml");
      //logger.finer("Loading from: " +repository_dir + REPOSITORY_FILE_NAME);
      logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
    		  "Loading from: " +repository_dir + REPOSITORY_FILE_NAME); //$NON-NLS-1$      
      load(repository_dir + REPOSITORY_FILE_NAME);

      logger.exiting(CLASS_NAME,METHOD_NAME);
   }

   /*
    * Delete the corresponding XML object and its descendants
    */
   void deleteXMLObjects()
   {
      final String METHOD_NAME = "deleteXMLObjects";
      logger.entering(CLASS_NAME,METHOD_NAME);

      this.xmlObject = null;

      Enumeration e = allDevices.elements();
      while (e.hasMoreElements())
      {
         ((StoredDeviceImpl)e.nextElement()).deleteXMLObjects();
      }

      e = allFirmware.elements();
      while (e.hasMoreElements())
      {
         ((StoredFirmwareImpl)e.nextElement()).deleteXMLObjects();
      }

      e = allManagedSets.elements();
      while (e.hasMoreElements())
      {
         ((StoredManagedSetImpl)e.nextElement()).deleteXMLObjects();
      }
      
      e = allTags.elements();
      while (e.hasMoreElements())
      {
         ((StoredTagImpl)e.nextElement()).deleteXMLObjects();
      }

      logger.exiting(CLASS_NAME,METHOD_NAME);
   }


   /*
    * Return the corresponding XML object
    */
   DPManager getXMLObject()
   {
      return this.xmlObject;
   }

   /*
    * Transform a RepositoryImpl object into an XML object for persistence
    */
   void toXMLObject(DPManager dpManager) throws DatastoreException
   {
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME,METHOD_NAME, this);

      this.xmlObject = dpManager;

      dpManager.setVersion(XMI_VERSION);
      dpManager.setVersionsStoredLimit(this.maxVersionsToStore);
      dpManager.setId(XML_ID);

      //reset XML object counters
      StoredDeviceImpl.resetXmlObjectNum();
      StoredDomainImpl.resetXmlObjectNum();
      StoredDeploymentPolicyImpl.resetXmlObjectNum();
      StoredDeploymentPolicyVersionImpl.resetXmlObjectNum();
      StoredDomainVersionImpl.resetXmlObjectNum();
      StoredFirmwareImpl.resetXmlObjectNum();
      StoredFirmwareVersionImpl.resetXmlObjectNum();
      StoredManagedSetImpl.resetXmlObjectNum();
      StoredTagImpl.resetXmlObjectNum();
//      StoredSettingsImpl.resetXmlObjectNum();
//      StoredSettingsVersionImpl.resetXmlObjectNum();

      // Devices and firmwares should be saved before saving managedSets
      // because the device members and the desired firmware version of a managedSet
      // have references to the device objects and the firmware version

      // Step 1: transform all the devices into XML objects

      Enumeration e = allDevices.elements();
      while (e.hasMoreElements())
      {
         StoredDeviceImpl dev = (StoredDeviceImpl)e.nextElement();
         DPDevice dpd = dpManager.addNewDevices();
         dev.toXMLObject(dpd);
      }

      // Step 2: transform all the firmwares and their versions
      // into XML objects

      e = allFirmware.elements();
      while (e.hasMoreElements())
      {
         StoredFirmwareImpl fw = (StoredFirmwareImpl)e.nextElement();
         DPFirmware dpf = dpManager.addNewFirmwares();
         fw.toXMLObject(dpf);
      }

      // Step 3: transform all the managed sets and their descendants
      // into XML objects

      e = allManagedSets.elements();
      while (e.hasMoreElements())
      {
         StoredManagedSetImpl mset = (StoredManagedSetImpl)e.nextElement();
         DPManagedSet dpms = dpManager.addNewManagedSets();
         mset.toXMLObject(dpms);
      }
      
   // Step 4: transform all the tags
      // into XML objects

      e = allTags.elements();
      while (e.hasMoreElements())
      {
         StoredTagImpl tag = (StoredTagImpl)e.nextElement();
         if ( tag.isTagged() ) {
	         DPTag dtag = dpManager.addNewTag();
	         tag.toXMLObject(dtag);
         }
         else { // remove it from cache
        	 allTags.remove(tag.getPrimaryKey());
         }
      }
      logger.exiting(CLASS_NAME,METHOD_NAME,dpManager);
   }

   /*
    * Transform an XML object into a RepositoryImpl object
    */
   void fromXMLObject(DPManager dpm) throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME,METHOD_NAME, dpm);

      // Devices and firmwares should be created before creating managedSets
      // because the device members and the desired firmware version of a managedSet
      // have references to the device objects and the firmware versions

      this.maxVersionsToStore = dpm.getVersionsStoredLimit();      
      
      //create devices
      List<DPDevice> dvs = dpm.getDevicesList();
      for(DPDevice d: dvs){
    	  StoredDeviceImpl.fromXMLObject(d);
      }
      
/*      DPDevice[] dvs = dpm.getDevicesList();
      for (int i=0; i<dvs.length; i++)
      {
         StoredDeviceImpl.fromXMLObject(dvs[i]);
      }*/

      //create firmwares
      List<DPFirmware> dfvs = dpm.getFirmwaresList();
      for(DPFirmware fw: dfvs){
    	  StoredFirmwareImpl.fromXMLObject(fw);
      }      
/*      DPFirmware[] dfvs = dpm.getFirmwaresArray();
      for (int i=0; i<dfvs.length; i++)
      {
         StoredFirmwareImpl.fromXMLObject(dfvs[i]);
      }*/

      //create managedsets
      List<DPManagedSet> dmsets = dpm.getManagedSetsList();
      for(DPManagedSet ms: dmsets){
    	  StoredManagedSetImpl.fromXMLObject(ms);
      }  
      
      //create Tags
      List<DPTag> dtags = dpm.getTagList();
      for(DPTag tag: dtags){
    	  StoredTagImpl.fromXMLObject(tag);
      }
/*      DPManagedSet[] dmsets= dpm.getManagedSetsArray();
      for (int i=0; i<dmsets.length; i++)
      {
         StoredManagedSetImpl.fromXMLObject(dmsets[i]);
      }*/

      logger.exiting(CLASS_NAME,METHOD_NAME, this);
   }

   /*
    * Save the blob data into a file under the repostory directory
    * return the file name
    */
   @edu.umd.cs.findbugs.annotations.SuppressWarnings(
			justification="<clean up stream or resource>",
			value="OBL_UNSATISFIED_OBLIGATION") 
   static final String saveBlobToFile(Blob blob) throws DatastoreException
   {
      final String METHOD_NAME = "saveBlobToFile";
      logger.entering(CLASS_NAME,METHOD_NAME, blob);

      File f = null;

      try
      {
         File reposDir = new File(repository_dir);
         f = File.createTempFile(BLOB_FILE_PREFIX, "." + BLOB_FILE_SUFFIX, reposDir);

         FileChannel out = new FileOutputStream(f).getChannel();
         if (blob.hasBytes())
         {
            out.write(ByteBuffer.wrap(blob.getByteArray()));
         } else
         {
            FileChannel in = ((FileInputStream) blob.getInputStream())
                  .getChannel();
            out.transferFrom(in, 0, in.size());
         }
         out.close();
      } catch (IOException e)
      {
         logger.throwing(CLASS_NAME, METHOD_NAME, e);
         if (f != null) {
            if ( !f.delete() ) { //try to clean up file we created
            	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Sets the last-modified time of the file or directory failed.");
            }
         }
    	 String message = Messages.getString("DataStoreException");          
         throw new DatastoreException(message,"DataStoreException");
      }

      logger.exiting(CLASS_NAME,METHOD_NAME, f.getName());
      return f.getName();
   }


   static Collection getinUseBlobFiles()
   {
      return inUseBlobFiles;
   }

   static boolean isCollectingGarbage()
   {
      return collectingGarbage;
   }

   protected static Credential getCredential()
   {
	   return credential;
   }

   Hashtable getMapXmlObjectsToMemObjects()
   {
      return mapXmlObjectsToMemObjects;
   }
   /**
    * Gets the directory where the versions will be stored.
    *
    * @return the directory where the versions will be stored.
    * @see #setVersionsDirectory(java.io.File)
    */
/*   public File getVersionsDirectory()
   {
	   File temp = null;
	   final String METHOD_NAME = "exportAll()";
       logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
    		   "getVersionsDirectory!!!"); //$NON-NLS-1$ 
       return;
	   return(temp);
   }*/

//   /*
//    * Sets the directory where the versions created by the manager will be
//    * stored. It is up the repository implementation to decide if it needs to move
//    * the current versions from the current directory to the new directory.
//    *
//    * @param newDirectory
//    *            the new directory where the versions will be stored.
//    * @throws DatastoreException
//    * @see #getMaxVersionsToStore()
//    *
//    */
//
//   public void setVersionsDirectory(File newDirectory)
//           throws DatastoreException
//   {
//	// TODO: Implement this (9004 support)
//   }
//
//
//
//public String getRepository_dir() {
//	return repository_dir;
//}
   
   
}

