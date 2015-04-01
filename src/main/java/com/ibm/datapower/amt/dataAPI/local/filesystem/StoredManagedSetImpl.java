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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotEmptyInRepositoryException;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDevice;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPManagedSet;


/**
 * The StoredMangedSet writes and reads all information that must be maintained for ManagedSet including
 * its managed device members. Below is a sample ManagedSet element with a single member.
 * <blockquote>
 * <pre> 
 * {@code
 * <managedSets xmi:id="DPManagedSet_1" name="set1" deviceMembers="DPDevice_0"/>
 * }
 * </pre>
 * </blockquote>
 * <p>
 * Note:  The Local File System implementation adds an element for each ManagedSet that is created.  All it device members are stored as attributes
 * on the Managedset element.
 * It does not contain other elements.
 * </p> 
 */
public class StoredManagedSetImpl implements StoredManagedSet
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;

   private String name = null;
   private Hashtable allManagedSets = null;
   private Hashtable domains = null;
   private Hashtable devices = null;

   // The corresponding XML object. It is created when this object
   // is being persisted into an XML file.
   private DPManagedSet xmlObject = null;
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;

   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String className = "StoredManagedSetImpl";
   private static final String XML_CLASS_NAME = "DPManagedSet";

   private static final String CLASS_NAME = StoredManagedSetImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }
   
   
  StoredManagedSetImpl(Hashtable allManagedSets, String name) throws AlreadyExistsInRepositoryException
   {
      devices = new Hashtable();
      domains = new Hashtable();

      this.name = name;
      this.allManagedSets = allManagedSets;
      if (allManagedSets.containsKey(getPrimaryKey())){

    	  String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey()); 
    	  throw new AlreadyExistsInRepositoryException (message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey() );
      }  
   }

  /**
   * <p>
   * Note:  The Local File System implementation 
   * uses the name as the unique identifier of this object in the
   * repository. 
   * </p>
   * <inheritDoc />
   */
   public String getName()
   {
      return this.name;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#add(com.ibm.datapower.amt.dataAPI.StoredDevice)
    */
   public void add(StoredDevice device)
   {
	  final String METHOD_NAME = "add(StoredDevice)";  
      if (device != null)
      {
         this.devices.put(device.getPrimaryKey(), device);
         if (device instanceof StoredDeviceImpl)
         {
            StoredDeviceImpl storedDevice = (StoredDeviceImpl) device;
            storedDevice.setManagedSet(this);
         }
      }
      else
      {
         //logger.finer("Attempted to add null device");
         logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		 "Attempted to add null device"); 
      }
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#getDeviceMembers()
    */
   public StoredDevice[] getDeviceMembers()
   {
      StoredDevice[] result = new StoredDevice[devices.size()];

      Enumeration e = devices.elements();
      int i=0;
      while (e.hasMoreElements())
      {
         Object o = e.nextElement();
         if (o instanceof StoredDeviceImpl)
         {
            result[i++] = (StoredDeviceImpl) o;
         }
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#remove(com.ibm.datapower.amt.dataAPI.StoredDevice)
    */
   public void remove(StoredDevice device)
   {
      this.devices.remove(device.getPrimaryKey());

      if (device instanceof StoredDeviceImpl)
      {
         StoredDeviceImpl storedDevice = (StoredDeviceImpl) device;
         storedDevice.clearManagedSet();
      }

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#copy(com.ibm.datapower.amt.dataAPI.StoredDomainVersion)
    */
/* 9004   public StoredDomain copy(StoredDomainVersion domainVersion)
         throws NotExistException
   {
      //    TODO implement
      return null;
   }
*/
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredManagedSet#copy(com.ibm.datapower.amt.dataAPI.StoredSettingsVersion)
    */
/* 9004   public StoredSettings copy(StoredSettingsVersion SettingsVersion)
         throws NotExistException
   {
      //TODO implement
      return null;
   }
*/ 
   /**
   * <p>
   * Note:  The Local File System implementation 
   * deletes the ManagedSet element from the WAMT.repository.xml file but any device elements
   * that are not contained inside it are retained and can be accessed by calling 
   * {@link com.ibm.datapower.amt.clientAPI.Manager#getAllDevices()} and
   * {@link com.ibm.datapower.amt.clientAPI.Manager#getDmiLogger()}     
   * </p>
   * </p>
   * <inheritDoc />
   */
   public void delete() throws DatastoreException, NotEmptyInRepositoryException
   {
      final String methodName = "delete";
      logger.entering(CLASS_NAME,methodName);
      this.allManagedSets.remove(this.getPrimaryKey());
      logger.exiting(CLASS_NAME,methodName);

   }

   /**
    * <p>
    * Note:  The Local File System implementation 
    * uses the name of the ManagesSet as the unique identifier for this object. The name is immutable, so there is no
    * <code>setName(String)</code> method.  
    * </p>
    * <inheritDoc />
    */
   public String getPrimaryKey()
   {

      return getName();
   }

   /*
    * Reset the object counter of DPClonableDeviceSettingsVersion
    */
   static void resetXmlObjectNum()
   {
      xmlObjectNum = 0;
   }

   /*
    * Delete the corresponding XML object and its descendants
    */
   void deleteXMLObjects()
   {
      this.xmlObject = null;
/*      if (this.settings != null)
      {
         this.settings.deleteXMLObjects();
      }*/
      Enumeration e = this.domains.elements();
      while (e.hasMoreElements())
      {
         ((StoredDomainImpl)e.nextElement()).deleteXMLObjects();
      }

   }

   /*
    * Return the corresponding XML object
    */
   DPManagedSet getXMLObject()
   {
      return this.xmlObject;
   }

   /*
    * Transform a StoredManagedSetImpl object into an XML object for persistence
    */
   void toXMLObject(DPManagedSet dpmset)
   {
      final String methodName = "toXMLObject";
      logger.entering(CLASS_NAME, methodName, this);

      this.xmlObject = dpmset;
      dpmset.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);

      dpmset.setName(this.name);

      // transform all the device members

      String deviceMembers = "";
      Enumeration e = devices.elements();
      StringBuffer buf = new StringBuffer(deviceMembers);
      while (e.hasMoreElements())
      {
         StoredDeviceImpl dev = (StoredDeviceImpl)e.nextElement();
         DPDevice existingDpd = dev.getXMLObject();
         buf.append(" " + existingDpd.getId()); // deviceMembers += " " + existingDpd.getId();
      }
      deviceMembers = buf.toString();
      dpmset.setDeviceMembers2(deviceMembers.trim());

      logger.exiting(CLASS_NAME,methodName,dpmset);
   }

   /*
    * Transform an XML object into a StoredManagedSetImpl object
    */
   static void fromXMLObject(DPManagedSet dms) throws DatastoreException
   {
      final String methodName = "fromXMLObject";
      logger.entering(CLASS_NAME, methodName, dms);

      StoredManagedSetImpl mset = null;
      RepositoryImpl ri = RepositoryImpl.getInstance();

      mset = (StoredManagedSetImpl) ri.createManagedSet(dms.getName());

      String deviceMembers = dms.getDeviceMembers2();
      if (deviceMembers != null)
      {
         StringTokenizer dmst = new StringTokenizer(deviceMembers);
         while (dmst.hasMoreTokens())
         {
            String device = dmst.nextToken();
            mset.add((StoredDevice) ri.getMapXmlObjectsToMemObjects().get(device));
         }
      }

      logger.exiting(CLASS_NAME, methodName, mset);
   }

}
