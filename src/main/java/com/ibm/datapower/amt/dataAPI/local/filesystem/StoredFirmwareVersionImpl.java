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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPFirmwareVersion;



/**
 * A persisted version of a Firmware i.e StoredFirmware. FirmwareVersions can be deployed to managed or 
 * unmanaged devices. A firmwareVersion can be deployed to all device members
 * that belong to a ManagedSet. Below is a sample firmware version uniquely identified by the level, contained within a firmware which
 * is uniquely identified by its attributes: device type, model type, strict featires and non-strict features.
 * <blockquote>
 * <pre> 
 * {@code
 * <firmwares xmi:id="DPFirmware_0" deviceType="XI50" highestVersion="1" modelType="9003" strictFeatures="" nonstrictFeatures="DataGlue;JAXP-API;PKCS7-SMIME;HSM;XG4;Compact-Flash;iSCSI;RaidVolume;LocateLED;AppOpt;MQ;WebSphere-JMS;">
 *   <versions xmi:id="DPFirmwareVersion_0" timeCreatedInManager="1273597277312" userComment="/dev-xi-186298.scrypt2" versionImageFileLocation="Blob302265539053947690.bin" versionNumber="1" manufactureDate="1272586131000" level="3.8.1.0"/>
 * </firmwares> 
 * }
 * </pre>
 * </blockquote>  
 * 
 * @see com.ibm.datapower.amt.dataAPI.StoredFirmware
 * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion
 * @see com.ibm.datapower.amt.clientAPI.Firmware
 * @see com.ibm.datapower.amt.clientAPI.FirmwareVersion
 */
public class StoredFirmwareVersionImpl implements StoredFirmwareVersion
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;

   private String level = null;
   private Date manufactureDate = null;
   private StoredFirmwareImpl storedFirmware = null;
   private int versionNumber = 0;
   private Date timestamp = null;
   private String userComment = null;
   private String versionImageFileLocation = null;

   // The corresponding XML object. It is created when this object
   // is being persisted into an XML file.
   private DPFirmwareVersion xmlObject = null;

   // The XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPFirmwareVersion";
   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String CLASS_NAME = "StoredFirmwareVersionImpl";

   private static final String CLASS_NAME = StoredFirmwareVersionImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }
   
   StoredFirmwareVersionImpl(StoredFirmware versionedObject, Blob blob, String level,
         Date manufactureDate, String userComment, Date timeStamp)
   		 throws AlreadyExistsInRepositoryException,DatastoreException
   {
      if (versionedObject instanceof StoredFirmwareImpl)
      {
         this.storedFirmware = (StoredFirmwareImpl) versionedObject;
      }
      else
      {
 		 String message = Messages.getString("wamt.dataAPI.Repository.invalidVerObj");
		 throw new DatastoreException(message,"wamt.dataAPI.Repository.invalidVerObj");
      }

      // Save the blob data into a file and store the file's
      // absolute path in the property, versionImageFileLocation.
      if (blob != null)
      {
         this.versionImageFileLocation = RepositoryImpl.saveBlobToFile(blob);
      }
      this.level = level;
      this.manufactureDate = manufactureDate;
      this.userComment = userComment;
      this.timestamp = timeStamp;
      this.versionNumber = this.storedFirmware.getNextVersionNumber();

      if (storedFirmware.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }

   }

   /*
    * This constructor is only used to load the object from an XML file
    * into memory. getNextVersionNumber() should not be called in this method.
    */
   private StoredFirmwareVersionImpl(StoredFirmware versionedObject, String versionImageLocation, String level,
         Date manufactureDate, String userComment, Date timeStamp, int versionNum)
   		 throws AlreadyExistsInRepositoryException,DatastoreException
   {
      if (versionedObject instanceof StoredFirmwareImpl)
      {
         this.storedFirmware = (StoredFirmwareImpl) versionedObject;
      }
      else
      {
 		 String message = Messages.getString("wamt.dataAPI.Repository.invalidVerObj");
		 throw new DatastoreException(message, "wamt.dataAPI.Repository.invalidVerObj");
      }

      this.versionImageFileLocation = versionImageLocation;
      this.level = level;
      this.manufactureDate = manufactureDate;
      this.userComment = userComment;
      this.timestamp = timeStamp;
      this.versionNumber = versionNum;

      if (storedFirmware.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion#getLevel()
    */
   public String getLevel()
   {
      return this.level;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion#getManufactureDate()
    */
   public Date getManufactureDate()
   {
	   Date result = this.manufactureDate;
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion#getFirmware()
    */
   public StoredFirmware getFirmware()
   {
      return this.storedFirmware;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion#getPrimaryKey()
    */
   public String getPrimaryKey()
   {
      return getFirmware().getPrimaryKey() + ":" + getLevel();
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#getVersionNumber()
    */
   public int getVersionNumber()
   {
      return this.versionNumber;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#getVersionedObject()
    */
   public Object getVersionedObject()
   {
      return this.storedFirmware;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#getTimestamp()
    */
   public Date getTimestamp()
   {
	   Date result = this.timestamp;
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#getUserComment()
    */
   public String getUserComment()
   {
      return this.userComment;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#setUserComment(java.lang.String)
    */
   public void setUserComment(String comment) throws DatastoreException
   {
      this.userComment = comment;

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#getBlob()
    */
   public Blob getBlob()
   {
      if (this.versionImageFileLocation != null)
      {
         return new Blob(new File(RepositoryImpl.getRepositoryDir()
               + this.versionImageFileLocation));
      }
      else
      {
         return null;
      }
   }


   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredVersion#delete()
    */
   public void delete()
   {
      final String methodName = "delete";
      logger.entering(CLASS_NAME,methodName);
      this.storedFirmware.removeVersion(this);
      // 9933: [3661] WAMT Repository contains firmware images that have been deleted from WAMC
      String sFilePath = RepositoryImpl.getRepositoryDir() + this.versionImageFileLocation;
      File file = new File( sFilePath );
      logger.logp(Level.FINER, CLASS_NAME, methodName, 
   		   "Removing deleted blob file, " + sFilePath, " on disk"); //$NON-NLS-1$
      if ( file.delete() ) {
    	  logger.logp(Level.FINER, CLASS_NAME, methodName, 
   	   		   "Removing deleted blob file, " + sFilePath + " on disk is successful"); //$NON-NLS-1$
      }
      else {
    	  logger.logp(Level.FINER, CLASS_NAME, methodName, 
    	   		   "Removing deleted blob file, " + sFilePath + "on disk is failed"); //$NON-NLS-1$
      }
      logger.exiting(CLASS_NAME,methodName);

   }



   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion#setBlob(com.ibm.datapower.amt.clientAPI.Blob)
    */
/* 9004 public void setBlob(Blob newBlob)
   {
      final String methodName = "setBlob";
      logger.entering(CLASS_NAME,methodName, newBlob);

      try
      {
         if (newBlob != null)
         {
            this.versionImageFileLocation = RepositoryImpl
                  .saveBlobToFile(newBlob);
         }
      } catch (DatastoreException ex)
      {
         logger.throwing(CLASS_NAME, methodName, ex);
      }
      TRACE.exiting(CLASS_NAME,methodName);
   }
9004 */
   /*
    * Reset the object counter of DPFirmwareVersion
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
   }

   /*
    * Return the corresponding XML object
    */
   DPFirmwareVersion getXMLObject()
   {
      return this.xmlObject;
   }

   /*
    * Transform a StoredFirmwareVersionImpl object into an XML object for persistence
    */
   void toXMLObject(DPFirmwareVersion dpVersion)
   {
      final String methodName = "toXMLObject";
      logger.entering(CLASS_NAME,methodName, this);

      this.xmlObject = dpVersion;
      dpVersion.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      // save the timestamp as a long integer
      dpVersion.setTimeCreatedInManager(this.timestamp.getTime());
      dpVersion.setUserComment(this.userComment);
      dpVersion.setVersionImageFileLocation(this.versionImageFileLocation);
      dpVersion.setVersionNumber(this.versionNumber);

      //d19567:  set manufacture date to 0 if discovered on device instead of image.
      if (this.manufactureDate != null)
      {
         dpVersion.setManufactureDate(this.manufactureDate.getTime());
      }
      else
      {
         dpVersion.setManufactureDate(0);
      }
      dpVersion.setLevel(this.level);

      if (RepositoryImpl.isCollectingGarbage() && this.versionImageFileLocation != null)
      {
         RepositoryImpl.getinUseBlobFiles().add(this.versionImageFileLocation);
      }

      logger.exiting(CLASS_NAME,methodName,dpVersion);
   }

   /*
    * Transform an XML object into a StoredFirmwareVersionImpl object
    */
   static StoredFirmwareVersionImpl fromXMLObject(DPFirmwareVersion dpfv, StoredFirmware fw)
   throws AlreadyExistsInRepositoryException, DatastoreException
   {
      final String methodName = "fromXMLObject";
      logger.entering(CLASS_NAME,methodName, new Object[]{dpfv, fw});
      StoredFirmwareVersionImpl sfv  = null;
      RepositoryImpl ri = RepositoryImpl.getInstance();

      sfv = new StoredFirmwareVersionImpl(
            fw,
            dpfv.getVersionImageFileLocation(),
            dpfv.getLevel(),
            new Date(dpfv.getManufactureDate()),
            dpfv.getUserComment(),
            new Date(dpfv.getTimeCreatedInManager()),
            dpfv.getVersionNumber());

      // Save all the firmware versions in the table.
      // The information will be used to load the desired
      // firmware versions of managedSets
      ri.getMapXmlObjectsToMemObjects().put(dpfv.getId(), sfv);
      logger.exiting(CLASS_NAME, methodName, sfv);
      return sfv;
   }

}
