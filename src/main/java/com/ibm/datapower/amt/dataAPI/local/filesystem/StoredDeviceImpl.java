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


import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.xmlbeans.impl.util.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.URLSource;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDevice;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomain;

/**
 * <p>It represents a DataPower device. All device related information that must be persisted for a physical DataPower device.
 * This includes the device serialnumber, hostname, GUIPOrt, HLMPort, Hostname,  Model, 
 * Symbolic Name, and quiesce time out.
 * </p>
 * <p>It is the container for StoredDomains that represent domains on a DataPower device. Below is a sample
 * of a device element
 * <blockquote>
 * <pre> 
 * {@code
 * <devices xmi:id="DPDevice_0" deviceType="XI50" GUIPort="8080" HLMPort="5550" 
 *   Hostname="9.42.112.77" password="password" serialNumber="130018M" userId="admin" 
 *   name="dp10.rtp.raleigh.ibm.com" modelType="9003" currentAMPVersion="2.0" 
 *   quiesceTimeout="60" featureLicenses="JAXP-API;WebSphere-JMS;">
 *   ...
 * </devices>
 * }
 * </pre>
 * </blockquote>
 * </p>
 */
public class StoredDeviceImpl implements StoredDevice
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;

	static class PasswordEncoderDecoder
	{
		// TODO: In future, we can allow client to specify enc algs.  For now, just use a static ones.
		private static String key_algorithm    = "AES";
		private static String digest_algorithm = "MD5";
		private static String default_secretkey_string = "q38Ac9PJ87e8W0rF";
		private static String secretkey_string_property_name = "RepositoryEncryptionPassword";
		private static String byte_encoding    = "US-ASCII";
		private static byte[] pw_salt          = {
			(byte)0x98, (byte)0x37, (byte)0x18, (byte)0xd8,
			(byte)0x4e, (byte)0xc4, (byte)0x4b, (byte)0x83,
			(byte)0x46, (byte)0xb3, (byte)0x1e, (byte)0xa4,
			(byte)0x6f, (byte)0xc7, (byte)0x4d, (byte)0xd8,
		};
		private static int pw_iter_count      = 30;
		private static SecretKeySpec keyspec = null;
		private static Cipher encrypt_cipher = null;
		private static Cipher decrypt_cipher = null;
		
		public PasswordEncoderDecoder() 
		{
			this.init();
		}
		
		private void init()
		{
			try{
				// generate key using a password, salt, and digest
				String secretkey_string = (String) RepositoryImpl.getCredential().getProperty(secretkey_string_property_name);
				if (secretkey_string == null) secretkey_string = default_secretkey_string;				
				byte[] input_bytes = secretkey_string.getBytes();
				int len1 = input_bytes.length;
				int len2 = pw_salt.length;
				int max_length = Math.max(len1, len2);
				byte[] output_bytes = new byte[max_length];
				for (int i=0; i<max_length; i++){
					if (i<len1) output_bytes[i] = input_bytes[i];
					else output_bytes[i] = pw_salt[i];
				}
				MessageDigest digest = MessageDigest.getInstance(digest_algorithm);
				byte[] digest_result = digest.digest(output_bytes);
				for (int i=0; i<pw_iter_count; i++)
				{
					digest_result = digest.digest(digest_result);
				}							
				
				// generate key spec
				keyspec = new SecretKeySpec(digest_result, key_algorithm);								
				
				// generate ciphers
				encrypt_cipher = Cipher.getInstance(key_algorithm);
				encrypt_cipher.init(Cipher.ENCRYPT_MODE, keyspec);
				decrypt_cipher = Cipher.getInstance(key_algorithm);
				decrypt_cipher.init(Cipher.DECRYPT_MODE, keyspec);
			} catch (NoSuchAlgorithmException e) {
				// no log msg for now, should not be able to get here since everything is hardcoded
			} catch (NoSuchPaddingException e) {
				// no log msg for now, should not be able to get here since everything is hardcoded			
			} catch (InvalidKeyException e) {
				// no log msg for now, should not be able to get here since everything is hardcoded
			}
			//} catch (Exception e) {
				// no log msg for now, should not be able to get here since everything is hardcoded
			//}			
		}
		
		public static String encode(String password)
		{
			String encoded_password = null;
			try
			{
				if (password == null)
				{
					encoded_password = "";
				}
				else 
				{
					byte[] bytes = encrypt_cipher.doFinal(password.getBytes());
					encoded_password = new String(Base64.encode(bytes),byte_encoding);
				}				
			} catch (IllegalBlockSizeException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			} catch (BadPaddingException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			} catch (UnsupportedEncodingException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			}
			return encoded_password;
		}
		
		public static String decode(String password)
		{
			String decoded_password = "";
			try
			{
				if (password == null)
				{
					decoded_password = "";
				}
				else 
				{
					byte[] bytes;
						bytes = decrypt_cipher.doFinal(Base64.decode(password.getBytes()));
						decoded_password = new String(bytes,byte_encoding);				
				}
			} catch (IllegalBlockSizeException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			} catch (BadPaddingException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			} catch (UnsupportedEncodingException e) {
				// no msg for now, should not be able to get here since everything is hardcoded
			}			
			return decoded_password;
		}
	}
	
   private String deviceID = null;	
   private String symbolicName = null;
   private String hostname = null;
   private String serialNumber = null;
   private String userID = null;
   private String password = null;
   private String currentAMPVersion = null;
   private int hlmPort = 0;
   private int guiPort = 0;
   private StringCollection featureLicenses = null;
   private StoredManagedSetImpl managedSet = null;   
   private DeviceType deviceType = null;
   private ModelType modelType = null;
   private Hashtable domains = null;
   private Hashtable<String, StoredTag> tags = null;
   private URLSource certificateLocation = null;
   private URI backupFileLocation = null;
   private int quiesceTimeout = 0;
   private static PasswordEncoderDecoder passwordEncoderDecoder = new PasswordEncoderDecoder();
   
   //reference to Repository hashtable containing all devices 
   private Hashtable allDevices = null;
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.
   private DPDevice xmlObject = null;
   
   static final char FEATURE_LICENSES_DELIMITER = ';';
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPDevice_";
   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String CLASS_NAME = "StoredDeviceImpl";

   private static final String CLASS_NAME = StoredDeviceImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }
   
   
   StoredDeviceImpl(Hashtable allDevices,String deviceID, String serialNumber, String name,
         DeviceType deviceType, ModelType modelType, String hostname, String userid,
         String password, int HLMport, int guiPort, String ampVersion) throws AlreadyExistsInRepositoryException
   {
	  this.deviceID = deviceID;
      this.serialNumber = serialNumber;
      this.symbolicName = name;
      this.currentAMPVersion = ampVersion;
      this.deviceType = deviceType;
      this.hostname = hostname;
      this.userID = userid;
      this.password = password;
      this.hlmPort = HLMport;
      this.guiPort = guiPort;
      this.allDevices = allDevices;
      this.modelType = modelType;

      domains = new Hashtable();
      tags = new Hashtable<String, StoredTag>();
      
      if (allDevices.containsKey(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }
      
   }
   /**
   * <p>
   * Note:  The Local File System implementation stores all managed domains elements inside a device element 
   * in the WAMT.repository.xml file. Domains cannot exist outside of a containing device. 
   * </p>
   * <inheritDoc />
   */
   public StoredDomain[] getManagedDomains()
   {
      StoredDomain[] result = new StoredDomain[domains.size()];

      Enumeration e = domains.elements();
      int i=0;
      while (e.hasMoreElements())
      {
         Object o = e.nextElement();
         if (o instanceof StoredDomainImpl)
         {
            result[i++] = (StoredDomainImpl) o;
         }
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#getManagedDomain(java.lang.String)
    */
   public StoredDomain getManagedDomain(String domainName)
   {
      StoredDomainImpl result = null;
      if (domainName != null)
      {
         result = (StoredDomainImpl) this.domains.get(domainName);
      }

      return result;

   }

   /**
    * <p>
    * Note:  The Local File System implementation uses the serialNumber of the DataPower device that this object represents as the primary key. 
    * </p>
    * 
    * <inheritDoc />
    */
   public String getPrimaryKey()
   {	  
	   //return this.getSerialNumber();
	   return this.deviceID;
   }
   /**
   * <p>
   * Note:  The Local File System implementation stores the symbolic name as an attribute on the 'devices' element 
   * in the WAMT.repository.xml file.
   * </p>
    * 
    * <inheritDoc />
    */

   public String getSymbolicName()
   {
      
      return this.symbolicName;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setSymbolicName(java.lang.String)
    */
   public void setSymbolicName(String name) throws AlreadyExistsInRepositoryException,
         DatastoreException
   {
       this.symbolicName = name;

   }
   /**
   * <p>
   * Note:  The Local File System implementation stores the serial number as an attribute on the 'devices' element 
   * in the WAMT.repository.xml file.
   * </p>
   * 
   * <inheritDoc />
   */   
   public String getSerialNumber()
   {
      
      return this.serialNumber;
   }

   /**
    * <p>
    * Note:  The Local File System implementation stores the hostname as an attribute on the devices element 
    * in the WAMT.repository.xml file.
    * </p>
    * 
    * <inheritDoc />
    */ 
   public String getHostname()
   {
      
      return this.hostname;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setHostname(java.lang.String)
    */
   public void setHostname(String hostname) throws DatastoreException
   {
      this.hostname = hostname;

   }

   /**
    * <p>
    * Note:  The Local File System implementation stores the userId as an attribute on the device element 
    * in the WAMT.repository.xml file. 
    * </p>
    * <inheritDoc /> 
    */
   public String getUserId()
   {
      
      return this.userID;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setUserId(java.lang.String)
    */
   public void setUserId(String userid) throws DatastoreException
   {
      this.userID = userid;

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setPassword(java.lang.String)
    */
   public void setPassword(String password) throws DatastoreException
   {
      this.password = password;
   }

   /**
    * <p>
    * Note:  The Local File System implementation stores the password as an attribute on the device element 
    * in the WAMT.repository.xml file. 
    * </p> 
    * <inheritDoc />
    */
   public String getPassword()
   {
      
      return this.password;
   }

   /** 
    * <p>
    * Note:  The Local File System implementation stores the AMP version as an attribute on the 'devices' element 
    * in the WAMT.repository.xml file.
    * </p>
    * <inheritDoc />
    */
   public String getCurrentAMPVersion()
   {
      
      return this.currentAMPVersion;
   }

   /* 
    */
   public void setCurrentAMPVersion(String ampVersion) throws DatastoreException
   {
      this.currentAMPVersion = ampVersion;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#getHLMPort()
    */
   public int getHLMPort()
   {
      
      return this.hlmPort;
   }

   /**
    * 
    * <p>
    * Note:  The Local File System implementation stores the HLM Posrt as an attribute on the device element 
    * in the WAMT.repository.xml file. 
    * </p>  
    * < inheritDoc/>   
    */
   public void setHLMPort(int hlmPort) throws DatastoreException
   {
      this.hlmPort = hlmPort;

   }

   /** 
    * <p>
    * Note:  The Local File System implementation stores the WebGUI port as an attribute on the device element 
    * in the WAMT.repository.xml file. 
    * </p> 
    */
   public int getGUIPort()
   {
      
      return this.guiPort;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setGUIPort(int)
    */
   public void setGUIPort(int guiPort) throws DatastoreException
   {
      this.guiPort = guiPort;

   }

   /** 
    * <p>
    * Note:  The Local File System implementation stores the getFeatureLicenses as an attribute on the device element 
    * in the WAMT.repository.xml file. 
    * </p> 
    * < inheritDoc/>
    */
   public StringCollection getFeatureLicenses()
   {
      
      return this.featureLicenses;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setFeatureLicenses(com.ibm.datapower.amt.StringCollection)
    */
   public void setFeatureLicenses(StringCollection featureLicenses)
         throws DatastoreException
   {
      this.featureLicenses = featureLicenses;

   }

   /*
    * (non-Javadoc)
    * 
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#getModelType()
    */
   public ModelType getModelType() 
   {
      return this.modelType;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setModelType(com.ibm.datapower.amt.ModelType)
    */
   public void setModelType(ModelType modelType)
   {
      this.modelType = modelType;
   }
   
   /** 
     * <p>
     * Note:  The Local File System implementation stores managed device as attribute on the managedSets element 
     * in the WAMT.repository.xml file. 
     * </p> 
    * < inheritDoc/>
    */
   public StoredManagedSet getManagedSet()
   {
      
      return this.managedSet;
   }
   
   /*
    * Non-interface method to add a newly created StoredDomain to this StoredDevice
    * list of managed domains.
    * @param domain
    */
   void add(StoredDomain domain)
   {
      final String METHOD_NAME = "add(StoredDomain)";   
      if (domain != null)
      {
         this.domains.put(domain.getPrimaryKey(),domain);
      }
      else
      {
         //logger.finer("Attempted to add null domain");
          logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		  "Attempted to add null domain"); 
      }
   }

   /**
    * <p>
    * Note:  The Local File System implementation will remove the specified domain element from the WAMT.repository.xml file.  The clientAPI 
    * logic will delete all elements contained within a domain element. 
    * </p> 
    */
   public void remove(StoredDomain domain)
   {
      if (domain != null)
      {
         this.domains.remove(domain.getPrimaryKey());

      }

   }


   /**
    * Non-interface method to set the ManagedSet this device belongs to
    * @param managedSet
    */
   public void setManagedSet(StoredManagedSet managedSet)
   {
     if (managedSet instanceof StoredManagedSetImpl)
        this.managedSet= (StoredManagedSetImpl) managedSet;
   }
   
   /**
    * Non-interface method to clear the ManagedSet this device belongs to
    * 
    */
   void clearManagedSet()
   {
     this.managedSet = null;
   }

   /** 
     * <p>
     * Note:  The Local File System implementation stores the deviceType as an attribute on the devices element 
     * in the WAMT.repository.xml file. 
     * </p>  
   * < inheritDoc/>
   */
   public DeviceType getDeviceType()
   {
      
      return this.deviceType;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#setDeviceType(com.ibm.datapower.amt.DeviceType)
    */
   public void setDeviceType(DeviceType deviceType) throws DatastoreException
   {
      this.deviceType = deviceType;

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#delete()
    */
   public void delete()
   {
      final String methodName = "delete";
      logger.entering(CLASS_NAME,methodName);

      StoredDomain[] domains = this.getManagedDomains();  
      for (int i=0; i < domains.length; i++) { // remove domains from device
          try {
              domains[i].delete();
              // remove tags of domain
              domains[i].removeTags();
		  } 
          catch (Exception e) {
              logger.logp(Level.FINER, CLASS_NAME, methodName, "Unexpected exception thrown", e); 
		  }			
      } // end, removes domain from device

      // Remove all tags of this device
      this.removeTags();
      allDevices.remove(this.getPrimaryKey());
      this.managedSet = null;
      
      logger.exiting(CLASS_NAME,methodName);
      
   }
   
   /* 
    * Reset the object counter of DPDevice 
    */  
   static void resetXmlObjectNum()
   {
      xmlObjectNum = 0;
   }
   
   /* 
    * Delete the corresponding XML object 
    */   
   void deleteXMLObjects()
   {
      this.xmlObject = null;
      Enumeration e = this.domains.elements();
      while (e.hasMoreElements())
      {
         ((StoredDomainImpl)e.nextElement()).deleteXMLObjects();
      }
   }

   /* 
    * Return the corresponding XML object 
    */  
   DPDevice getXMLObject()
   {
      return this.xmlObject;
   }

   /* 
    * Transform a StoredDeviceImpl object into an XML object for persistence  
    */  
   void toXMLObject(DPDevice dpDevice) throws DatastoreException
   {
      final String methodName = "toXMLObject";
      logger.entering(CLASS_NAME, methodName, this);

      this.xmlObject = dpDevice;
      dpDevice.setId(XML_CLASS_NAME + this.deviceID);
      
      dpDevice.setDeviceType(
            this.getDeviceType().getDisplayName()); 
      dpDevice.setGUIPort(this.guiPort);
      dpDevice.setHLMPort(this.hlmPort);
      dpDevice.setHostname(this.hostname);
      if (this.password != null)
      {
    	  // encode password
    	  dpDevice.setPassword(passwordEncoderDecoder.encode(this.password));
      }
      dpDevice.setSerialNumber(this.serialNumber);
      dpDevice.setUserId(this.userID);
      dpDevice.setName(this.symbolicName);
      dpDevice.setModelType(this.modelType.getDisplayName());
      dpDevice.setCurrentAMPVersion(this.currentAMPVersion);
      if (this.certificateLocation != null){
    	  dpDevice.setCertificateLocation(this.certificateLocation.getURL());
      }     
      if (this.backupFileLocation != null){
    	  dpDevice.setBackupFileLocation(this.backupFileLocation.toString());
      }       
      dpDevice.setQuiesceTimeout(this.quiesceTimeout);

      // transform all the domains

      Enumeration e = this.domains.elements();
      while (e.hasMoreElements())
      {
    	  StoredDomainImpl domain = (StoredDomainImpl)e.nextElement();
    	  DPDomain dpdomain = dpDevice.addNewDomains();
    	  domain.toXMLObject(dpdomain);
      }    

      
      if (this.featureLicenses != null)
      {
         try
         {
            dpDevice.setFeatureLicenses(this.featureLicenses
                  .marshalToString(FEATURE_LICENSES_DELIMITER));
         } catch (DMgrException ex)
         {
        	String message = Messages.getString("DataStoreException");  
            throw new DatastoreException(message,"DataStoreException");
         }
      }
    
      logger.exiting(CLASS_NAME,methodName,dpDevice);  
   }

   
   /*
    * Transform an XML object into a StoredDeviceImpl object
    */
   static void fromXMLObject(DPDevice dpd) throws DatastoreException
   {
      final String methodName = "fromXMLObject";
      logger.entering(CLASS_NAME,methodName, dpd);
      RepositoryImpl ri = RepositoryImpl.getInstance();
      
      String deviceID = dpd.getId();
      if ( deviceID == null ) {
    	  deviceID = UUID.randomUUID().toString();
      }
      else {
    	  deviceID = deviceID.replace(XML_CLASS_NAME, "");
      }

      DeviceType deviceType = null;
      if (dpd.getDeviceType() != null)
      {
         deviceType = DeviceType.fromString(dpd.getDeviceType());
      }

      String password = dpd.getPassword();
      if (password != null)
      {
    	  // decode password
    	  password = passwordEncoderDecoder.decode(password);
      }
      else
      {
         password="";
      }
      
      ModelType mdType = ModelType.fromString(dpd.getModelType());
      
      StoredDevice dv = ri.createDevice(deviceID, dpd.getSerialNumber(), dpd.getName(),
            deviceType, mdType, dpd.getHostname(), dpd.getUserId(), password,
            dpd.getHLMPort(), dpd.getGUIPort(), dpd.getCurrentAMPVersion());
      
      dv.setQuiesceTimeout(dpd.getQuiesceTimeout());

      // Save the mapping in the table. The information will be 
      // used while loading the device members 
      // of a managedSet
      ri.getMapXmlObjectsToMemObjects().put(dpd.getId(), dv);

      String fl = dpd.getFeatureLicenses();
      if (fl != null)
      {
         StringCollection licenses = StringCollection.createCollectionFromString(fl, FEATURE_LICENSES_DELIMITER);
         dv.setFeatureLicenses(licenses);
      }
      
      String certURL = dpd.getCertificateLocation();
      if(certURL != null){
    	  try{
    		  dv.setBackupCertificateLocation(new URLSource(certURL));
    	  } catch (URISyntaxException e) {
  			  String message = Messages.getString("wamt.dataAPI.Repository.badURL", certURL);
			  throw new DatastoreException(message,"wamt.dataAPI.Repository.badURL", e);
    	  }
      }      
      
      String backupURL = dpd.getBackupFileLocation();
      if(backupURL != null){
    	  try{
    		  dv.setBackupFileLocation(new URI(backupURL));    	 
    	  } catch (URISyntaxException e) {
  			  String message = Messages.getString("wamt.dataAPI.Repository.badURL", backupURL);
			  throw new DatastoreException(message,"wamt.dataAPI.Repository.badURL", e);
		  }
      }      
      
      List<DPDomain> domainsList = dpd.getDomainsList();
      for (int i = 0; i < domainsList.size(); i++)
      {
         StoredDomainImpl.fromXMLObject(domainsList.get(i), dv); 
      }
      
      logger.exiting(CLASS_NAME,methodName,dv);  
   }

   public URLSource getBackupCertificateLocation() {

	   return this.certificateLocation;
   }

   public URI getBackupFileLocation() {
	   
	   return this.backupFileLocation;
   }

   public void setBackupCertificateLocation(URLSource certificateLocation) {
	  this.certificateLocation = certificateLocation ;

   }

   public void setBackupFileLocation(URI fileLocation) {
      this.backupFileLocation = fileLocation;

   }
   
   public void setQuiesceTimeout(int timeout) {
      this.quiesceTimeout = timeout; 

   }
   
   public int getQuiesceTimeout() {
      return this.quiesceTimeout; 
   }

   
   /*
    * (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDevice#add(com.ibm.datapower.amt.dataAPI.StoredTag)
    */
   public void add(StoredTag tag)
   {
	   final String METHOD_NAME = "add(StoredTag)";  
	   if (tag != null) {
		   this.tags.put(tag.getPrimaryKey(),tag);
	   }
	   else {
	       logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Attempted to add null tag"); 
	   }
   }
   
   public StoredTag[] getTags()
   {
	   StoredTag[] result = new StoredTag[this.tags.size()];

      Enumeration e = this.tags.elements();
      int i=0;
      while (e.hasMoreElements()) {
         Object o = e.nextElement();
         if (o instanceof StoredTagImpl) {
            result[i++] = (StoredTagImpl) o;
         }
      }
      return result;
   }

   public void remove(StoredTag tag) {
	   if (tag != null ) {
		   String tagKey = tag.getPrimaryKey();
		   StoredTag storedTag = this.tags.get(tagKey);
		   storedTag.remove(this);
		   this.tags.remove(tagKey);
	   }
   }
   
   public void removeTags() {
	   if ( this.tags != null ) {	
		   Enumeration e = this.tags.elements();
	      int i=0;
	      while (e.hasMoreElements()) {
	         Object o =  e.nextElement();
	         if (o instanceof StoredTagImpl) {	        
	        	 ((StoredTag)o).remove(this);
	         }
	      }
	      this.tags.clear();
	   }
   }
   
}

