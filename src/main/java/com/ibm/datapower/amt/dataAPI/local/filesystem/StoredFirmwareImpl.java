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


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotExistInRepositoryException;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPFirmware;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPFirmwareVersion;

/**
 * Persists all Firmware related information. This includes the device type, model type, strict features list, 
 * and non-strict feature list. It acts as a container for all the FirmwareVersions for this device type.
 * The firmware that is deployed to devices is persisted in separate bin files in the local file system. 
 * Below is a sample firmware element which can contain multiple firmware versions
 * <blockquote>
 * <pre> 
 * {@code
 * <firmwares xmi:id="DPFirmware_0" deviceType="XI50" highestVersion="1" modelType="9003" strictFeatures="" nonstrictFeatures="DataGlue;JAXP-API;PKCS7-SMIME;HSM;XG4;Compact-Flash;iSCSI;RaidVolume;LocateLED;AppOpt;MQ;WebSphere-JMS;">
 *   ...
 * </firmwares> 
 * }
 * </pre>
 * </blockquote> 
 * @see com.ibm.datapower.amt.dataAPI.StoredFirmware
 * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion 
 * @see com.ibm.datapower.amt.clientAPI.Firmware
 * @see com.ibm.datapower.amt.clientAPI.FirmwareVersion 
 *  
 */
public class StoredFirmwareImpl implements StoredFirmware
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;

   private DeviceType deviceType = null;
   private int highestVersionNumber = 0;
   private ModelType modelType = null;;
   private StringCollection strictFeatures = null;
   private StringCollection nonstrictFeatures = null;

   private Hashtable allFirmware = null;
   private Hashtable versions = null;
   
   private static final char FEATURES_DELIMITER = ';';
   
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.
   private DPFirmware xmlObject = null;
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPFirmware";
   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String CLASS_NAME = "StoredFirmwareImpl";
   
   private static final String CLASS_NAME = StoredFirmwareImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }   
   
   
   StoredFirmwareImpl(Hashtable allFirmware, DeviceType deviceType,
         ModelType modelType, StringCollection strictFeatures,
         StringCollection nonstrictFeatures) throws AlreadyExistsInRepositoryException
   {
      versions = new Hashtable();
      this.deviceType = deviceType;
      this.allFirmware = allFirmware;
      this.modelType = modelType;
      this.strictFeatures = strictFeatures;
      this.nonstrictFeatures = nonstrictFeatures;
      
      if (allFirmware.containsKey(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }
      
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#getDeviceType()
    */
   public DeviceType getDeviceType()
   {
      return this.deviceType;
   }

   /*
    * Add a new StoredFirmwareVervion to this StoredFirmware
    * 
    * @param version
    * @throws AlreadyExistsInRepositoryException
    */
   void addVersion(StoredFirmwareVersion version) throws AlreadyExistsInRepositoryException
   {
      final String METHOD_NAME = "addVersion";
      logger.entering(CLASS_NAME,METHOD_NAME,version);
      
      if (versions.containsKey(version.getPrimaryKey()))
      {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", version.getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", version.getPrimaryKey());
      }
      else
         versions.put(version.getPrimaryKey(),version);
      
      logger.exiting(CLASS_NAME,METHOD_NAME);
   }
   
   /*
    * Remove a StoredFirmwareVersion from this StoredFirmware
    * @param version
    * @throws AlreadyExistsInRepositoryException
    */
   void removeVersion(StoredFirmwareVersion version) 
   {
      final String METHOD_NAME = "removeVersion";
      logger.entering(CLASS_NAME,METHOD_NAME,version);
      
      versions.remove(version.getPrimaryKey());
      
      logger.exiting(CLASS_NAME,METHOD_NAME);
   }
   
   boolean checkIfVersionExists(String versionKey)
   {
      boolean result = false;
      if (versions.containsKey(versionKey))
         result=true;
      else
         result=false;
      return result;
   }
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#getModelType()
    */
   public ModelType getModelType()
   {
      return this.modelType;
   }
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#getStrictFeatures()
    */
   public StringCollection getStrictFeatures()
   {
      return this.strictFeatures;
   }
   
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#getNonStrictFeatures()
    */
   public StringCollection getNonstrictFeatures()
   {
      return this.nonstrictFeatures;
   }
   
   /* (non-Javad
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */
   public StoredFirmwareVersion[] getVersions()
   {
      StoredFirmwareVersion[] result = new StoredFirmwareVersion[this.versions.size()];
      Enumeration e = this.versions.elements();
      int i = 0;
      while (e.hasMoreElements()) 
      {  
         Object o = e.nextElement();
         if (o instanceof StoredFirmwareVersion)
         {
            result[i++] = (StoredFirmwareVersion) o;
         }
      }
      return(result);
   }

  

   
   /**
    * Generate a new version number
    * @return
    */
   protected int getNextVersionNumber() throws DatastoreException
   {
      int result;
      result = this.getHighestVersionNumber() + 1;
      this.setHighestVersion(result);
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#setHighestVersion(int)
    */
   void setHighestVersion(int newHighestVersion)
   {
      this.highestVersionNumber = newHighestVersion;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#getPrimaryKey()
    */
   public String getPrimaryKey()
   {
      return getPrimaryKey(this.deviceType, this.modelType, this.strictFeatures, this.nonstrictFeatures);
   }

   /* (non-Javadoc)
    * Create a key based on device type, model type and features
    */
   static String getPrimaryKey(DeviceType deviceType, ModelType modelType,
         StringCollection strictFeatures, StringCollection nonstrictFeatures) 
   {
      String key = null;
      key = deviceType.getDisplayName() + ":";
      key += modelType.getDisplayName() + ":";
      try {
          key += strictFeatures.marshalToString(FEATURES_DELIMITER);
          key += ":" + nonstrictFeatures.marshalToString(FEATURES_DELIMITER);
      } catch (DMgrException e) {
          //ignore errors from getFeatures().marshalToString
      }
      return key;
   }
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#remove(com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion)
    */
   public void remove(StoredFirmwareVersion firmwareVersion)
         throws NotExistInRepositoryException
   {
      final String METHOD_NAME = "remove";
      logger.entering(CLASS_NAME,METHOD_NAME);
      logger.exiting(CLASS_NAME,METHOD_NAME);

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmware#delete()
    */
   public void delete()
   {
      final String METHOD_NAME = "delete";
      logger.entering(CLASS_NAME,METHOD_NAME);
      allFirmware.remove(this.getPrimaryKey());
      logger.exiting(CLASS_NAME,METHOD_NAME);
      
   }

   /* 
    * Reset the object counter of DPClonableDeviceSettingsVersion 
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
      Enumeration e = versions.elements();
      while (e.hasMoreElements())
      {
         ((StoredFirmwareVersionImpl)e.nextElement()).deleteXMLObjects();
      }

   }

   /* 
    * Return the corresponding XML object 
    */  
   DPFirmware getXMLObject()
   {
      return this.xmlObject;
   }
   
   /* 
    * Transform a StoredFirmwareImpl object into an XML object for persistence  
    */     
   void toXMLObject(DPFirmware dpFirmware) throws DatastoreException
   {
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME,METHOD_NAME, this);
      
      this.xmlObject = dpFirmware;
      dpFirmware.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      
      dpFirmware.setDeviceType(this.getDeviceType().getDisplayName()); 
      dpFirmware.setHighestVersion(this.highestVersionNumber);
      dpFirmware.setModelType(this.modelType.getDisplayName());
      try
      {
         if (this.strictFeatures != null)
         {         
            dpFirmware.setStrictFeatures(this.strictFeatures.marshalToString(FEATURES_DELIMITER));
         }
         if (this.nonstrictFeatures != null)
         {         
            dpFirmware.setNonstrictFeatures(this.nonstrictFeatures.marshalToString(FEATURES_DELIMITER));
         }
      }
      catch(DMgrException e)
      {
    	 String message = Messages.getString("DataStoreException"); 
         throw new DatastoreException(message,"DataStoreException");
      }
      
      // transform all the versions  

      Enumeration e = versions.elements();
      while (e.hasMoreElements())
      {
         StoredFirmwareVersionImpl fv = (StoredFirmwareVersionImpl)e.nextElement();
         DPFirmwareVersion dpfv = dpFirmware.addNewVersions();
         fv.toXMLObject(dpfv);
      }
      
      logger.exiting(CLASS_NAME,METHOD_NAME,dpFirmware);
   }
      
   /*
    * Transform an XML object into a StoredFirmwareImpl object
    */
   static void fromXMLObject(DPFirmware df) throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, df);
      
      StoredFirmwareImpl sf = null;
      RepositoryImpl ri = RepositoryImpl.getInstance();

      ModelType mdType = ModelType.fromString(df.getModelType());
      StringCollection strictFT = null;
      if (df.getStrictFeatures() != null)
      {      
         strictFT = StringCollection.createCollectionFromString(df.getStrictFeatures(), FEATURES_DELIMITER);
//       //d20711 if feature list is empty, return new StringCollection with empty Vector
         if ((strictFT.size() == 1) && (strictFT.get(0).equals("")))
            strictFT = new StringCollection();
         
      }
      StringCollection nonstrictFT = null;
      if (df.getNonstrictFeatures() != null)
      {      
         nonstrictFT = StringCollection.createCollectionFromString(df.getNonstrictFeatures(), FEATURES_DELIMITER);
         //d20711 if feature list is empty, return new StringCollection with empty Vector
         if ((nonstrictFT.size() == 1) && (nonstrictFT.get(0).equals("")))
            nonstrictFT = new StringCollection();
      }
      
      // A firmware's DeviceType can't be null, so we don't check nullpointer here.
      sf = (StoredFirmwareImpl) ri.createFirmware(DeviceType.fromString(df
            .getDeviceType()), mdType, strictFT, nonstrictFT);
      sf.setHighestVersion(df.getHighestVersion());

      //DPFirmwareVersion[] dfvs = df.getVersionsArray();
      List <DPFirmwareVersion> dfvs = df.getVersionsList();
      for (int i = 0; i < dfvs.size(); i++)
      {
         sf.addVersion(StoredFirmwareVersionImpl.fromXMLObject(dfvs.get(i), sf));
      }

      logger.exiting(CLASS_NAME, METHOD_NAME, sf);  
   }
   
   public int getHighestVersionNumber()
   {
      
      return this.highestVersionNumber;
   }
}
