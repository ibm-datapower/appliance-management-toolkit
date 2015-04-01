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



import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.DomainSynchronizationMode;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.URLSource;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotEmptyInRepositoryException;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.dataAPI.StoredVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeployableConfiguration;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeployablePolicy;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomain;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomainVersion;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPVersion;

/**
 * 
 * <p>The information that must be persisted for a StoredDomain on a DataPower
 * device. This includes the Domain name, the device the domain resides on, synchronization mode,
 * configuration source and the associated deployment policy.
 * It is a container for all the StoredDomainVersions that are persisted in the
 * local file system. When 
 * a domain is created an empty deploymentpolicy object is also
 * created and persisted.  Below is a sample domain element</p>
 * <blockquote>
 * <pre> 
 * {@code
 * <domains xmi:id="DPDomain_0" highestVersion="1" name="lathas" SourceURL="device://9.42.112.79/latha" SynchDate="0" OutOfSynch="false" checkVersionSynch="false" quiesceTimeout="60" SyncMode="MANUAL">
 *   ...
 * </domains>
 * }
 * </pre>
 * </blockquote>
 */

public class StoredDomainImpl implements StoredDomain
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;
	   
   private String domainName = null;
   

   private StoredDeviceImpl device = null;
   private StoredDeploymentPolicyImpl  deploymentPolicyImpl = null;
   private int highestVersion = 0;
   
   private Hashtable allDomains = null; 

   private Hashtable versions = null;
   private URLSource sourceURL = null;
   private URLSource deployedSourceURL = null;   
   private DomainSynchronizationMode synchMode = DomainSynchronizationMode.MANUAL;
   private long synchDate =0;
   private boolean outOfSynch = false;
   //private boolean checkVersionSynch = false;   
   private int quiesceTimeout = 0;
 
   private Hashtable<String, StoredTag> tags = null;
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.
   private DPDomain xmlObject = null;
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPDomain";
   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String className = "StoredDomainImpl";
   private static final String CLASS_NAME = StoredDomainImpl.class.getName();   
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }   
   
   
//TCB   public StoredDomainImpl(Hashtable allDomains, StoredManagedSet managedSet,
//TCB	         String domainName) throws AlreadyExistsException
     public StoredDomainImpl(Hashtable allDomains, StoredDevice device,
             String domainName) throws AlreadyExistsInRepositoryException
   {	 
      versions = new Hashtable();
      this.deploymentPolicyImpl = null;
      if (device instanceof StoredDeviceImpl)
          this.device = (StoredDeviceImpl) device;
      this.domainName = domainName;
      if ( allDomains != null ) {
    	  //this.allDomains = allDomains;
    	  this.setAllDomains(allDomains);
          
    	  if (allDomains.containsKey(getPrimaryKey())){
    	    String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());  
    	    throw new AlreadyExistsInRepositoryException (message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
    	  }
      }
      tags = new Hashtable<String, StoredTag>();
   }
     
   private void setAllDomains(Hashtable allDomains) {
	   this.allDomains = allDomains;
   }

   /* (non-Javadoc)
    */
   public void addDomainPolicy(StoredDeploymentPolicyImpl domainPolicy)
         throws DatastoreException
   {
	   
    this.deploymentPolicyImpl = domainPolicy;

   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDomain#delete()
    */
   public void delete() throws DatastoreException, NotEmptyInRepositoryException
   {
      final String METHOD_NAME = "delete";
      logger.entering(CLASS_NAME,METHOD_NAME);
      device.remove(this);
      this.allDomains.remove(this.getPrimaryKey());
      RepositoryImpl.getInstance().setAllDomains(this.allDomains);
      removeTags();
      logger.exiting(CLASS_NAME,METHOD_NAME);
     
   }

   /**
    * <p>
    * Note: The Local File System implementation combines the domain name with the serial number of the containing <code>StoredDevice</code> 
    * as the unique identifier of this object. 
    * </p>
    * <inheritDoc />
    */
   public String getName()
   {

	   return this.domainName;
   }
   /**
    * <p>
    * Note:  The Local File System uses The name of this <code>StoredDomain</code> 
    * combined with the <code>Device</code> primaryKey as the the unique identifier for this object
    * </p>
    * <inheritDoc />
    */
   public String getPrimaryKey()
   {
      return this.getDevice().getPrimaryKey() + ":" + this.getName();
   }


   /**
   * <p>
   * Note: In the Local File System implementation the domain element is contained 
   * within the device element on which the domain resides.
   * </p>
   * <inheritDoc /> 
   */
   public StoredDevice getDevice()
   {
      return this.device;
   }
   
   
   /**
    * Non-interface method to add this Domain to a DeviceImpls list of 
    * managed domains.
    * 
    */
   public void updateDevice()
   {
      device.add(this);
   }

   /**
    * Add a new StoredDomainVervion to this StoredDomain
    * 
    * @param version
    * @throws AlreadyExistsInRepositoryException
    */
   public void addVersion(StoredDomainVersion version) throws AlreadyExistsInRepositoryException
   {
      final String METHOD_NAME = "addVersion";
      logger.entering(CLASS_NAME,METHOD_NAME,version);
      
      if (versions.containsKey(version.getPrimaryKey()))
      {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", version.getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", version.getPrimaryKey());
      }
      else{
         versions.put(version.getPrimaryKey(),version);

      }         
      
      logger.exiting(CLASS_NAME,METHOD_NAME);
   }
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */
   void removeVersion(StoredDomainVersion version) 
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
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */
   public StoredVersion[] getVersions()
   {
      StoredDomainVersion[] result = new StoredDomainVersion[this.versions.size()];
      Enumeration e = this.versions.elements();
      int i = 0;
      while (e.hasMoreElements()) 
      {  
         Object o = e.nextElement();
         if (o instanceof StoredDomainVersion)
         {
            result[i++] = (StoredDomainVersion) o;
         }
      }
      return(result);
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getHighestVersionNumber()
    */
   public int getHighestVersionNumber()
   {
      
      return this.highestVersion;
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

//   /* (non-Javadoc)
//    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#setHighestVersion(int)
//    */
   void setHighestVersion(int newHighestVersionNumber)
         throws DatastoreException
   {
      this.highestVersion = newHighestVersionNumber;

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
      Enumeration e = versions.elements();
      while (e.hasMoreElements())
      {
         ((StoredDomainVersionImpl)e.nextElement()).deleteXMLObjects();
      }

   }

   /* 
    * Return the corresponding XML object 
    */ 
   DPDomain getXMLObject()
   {
      return this.xmlObject;
   }

   /* 
    * Transform a StoredDomainImpl object into an XML object for persistence  
    */ 
   void toXMLObject(DPDomain dpDomain)throws DatastoreException
   {
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME,METHOD_NAME, this);

      this.xmlObject = dpDomain;
      dpDomain.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      
      dpDomain.setHighestVersion(this.highestVersion);
      dpDomain.setName(this.domainName);
      if (this.sourceURL != null){
    	  dpDomain.setSourceURL(this.sourceURL.getURL());
      }
                       
      dpDomain.setSynchDate(this.synchDate);
      dpDomain.setOutOfSynch(this.outOfSynch);
      //dpDomain.setCheckVersionSynch(this.checkVersionSynch);
      dpDomain.setQuiesceTimeout(this.quiesceTimeout);
    	  
      switch(this.synchMode)
      {
      // Use the type's enumeration values to set the attribute value.
      case AUTO:
    	  dpDomain.setSyncMode(DPDeployableConfiguration.SyncMode.AUTO);
    	  break;
      case MANUAL:
    	  dpDomain.setSyncMode(DPDeployableConfiguration.SyncMode.MANUAL);
    	  break;
      default:
          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", this.synchMode.toString());
	      throw new DatastoreException(message, "wamt.dataAPI.Repository.typeNotSupp");
      } 	  	      	  

      
      // transform all the versions
      Vector v = new Vector();
      Enumeration e = versions.elements();
      int highVersionNum = 0;
      while (e.hasMoreElements())
      {
         StoredDomainVersionImpl dv = (StoredDomainVersionImpl)e.nextElement();
         if (dv.getVersionNumber()> highVersionNum){
        	 highVersionNum = dv.getVersionNumber();
         }
         DPDomainVersion dpdv = DPDomainVersion.Factory.newInstance();
         dv.toXMLObject(dpdv);
         v.addElement(dpdv);
      }
      dpDomain.setVersionsArray(
            (DPDomainVersion[])(v.toArray(new DPDomainVersion[v.size()])));

      if (this.deploymentPolicyImpl!=null){
    	 DPDeployablePolicy dpPolicy = DPDeployablePolicy.Factory.newInstance(); 
         this.deploymentPolicyImpl.toXMLObject(dpPolicy,dpDomain);          
         dpDomain.setDeploymentPolicy(dpPolicy);
         //System.out.println(dpDomain.toString());
      }
      
      
               
      // If all domains have been deleted reset counter
      if (versions.size() <1) {
    	  this.setHighestVersion(0);
          dpDomain.setHighestVersion(this.highestVersion);
      }else{
    	  this.setHighestVersion(highVersionNum);
          dpDomain.setHighestVersion(this.highestVersion);    	  
      }
      
      
      
      logger.exiting(CLASS_NAME,METHOD_NAME,dpDomain);
   }
   
   /*
    * Transform an XML object into a StoredDomainImpl object
    */
   static void fromXMLObject(DPDomain dpd, StoredDevice device)
   throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{dpd, device});
      
      StoredDomainImpl  domain = null;
      RepositoryImpl ri = RepositoryImpl.getInstance();

      domain = (StoredDomainImpl)ri.createDomain(device, dpd.getName());
      domain.setHighestVersion(dpd.getHighestVersion());
      domain.setSynchDate(dpd.getSynchDate());
      domain.setOutOfSynch(dpd.getOutOfSynch());
      //domain.setCheckVersionSynch(dpd.getCheckVersionSynch());
      domain.setQuiesceTimeout(dpd.getQuiesceTimeout());
      
      // Save the mapping in the table. The information will be 
      // used while loading the domain members of a tag
      ri.getMapXmlObjectsToMemObjects().put(dpd.getId(), domain);
      
      String sourceURL = dpd.getSourceURL();
      if(sourceURL != null){
    	  try{
    		  domain.setSourceURL(new URLSource(sourceURL));
    	  } catch (URISyntaxException e) {
  			  String message = Messages.getString("wamt.dataAPI.Repository.badURL", sourceURL);
			  throw new DatastoreException(message,"wamt.dataAPI.Repository.badURL", e);
    	  }
      }           


      
      if (dpd.getSyncMode() !=null){  
    	  
    	  switch(dpd.getSyncMode().intValue())
    	  {
    	      // Use the type's enumeration values to set the attribute value.
    	      case DPDeployableConfiguration.SyncMode.INT_AUTO:
    	    	  domain.setSynchMode(DomainSynchronizationMode.AUTO);   	    	  
    	          break;
    	      case DPDeployableConfiguration.SyncMode.INT_MANUAL:
    	    	  domain.setSynchMode(DomainSynchronizationMode.MANUAL);    	    	  
    	          break;
    	      default:
    	          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", dpd.getSyncMode().toString());
      			  throw new DatastoreException(message,"wamt.dataAPI.Repository.typeNotSupp");
    	  } 	  	      	  
      }
      
      List<DPVersion> dfvs = dpd.getVersionsList();
      for (DPVersion v: dfvs)
      {
         StoredDomainVersionImpl sv = StoredDomainVersionImpl.fromXMLObject(
               (DPDomainVersion) v, domain);
         domain.addVersion(sv);
      }
      
      DPDeployablePolicy policy = dpd.getDeploymentPolicy();
     

      StoredDeploymentPolicyImpl sp = new StoredDeploymentPolicyImpl((StoredDomain)domain,policy.getPolicyName());      
      StoredDeploymentPolicyImpl.fromXMLObject(
    		  (DPDeployablePolicy) policy, sp);
      domain.addDomainPolicy(sp);  
      
      
      logger.exiting(CLASS_NAME, METHOD_NAME, domain);  
   }

   void setSynchDate(long synchDate) {
	  this.synchDate = synchDate;
   }

   long getSynchDate() {
		return this.synchDate;
   }
   
   /* (non-Javadoc)
    * 
    */   
   public void setSourceURL(URLSource urlSource) {
	   this.sourceURL = urlSource;
   }      

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */   
   public URLSource getSourceURL() {
	   return this.sourceURL;
   }

   void removeDeploymentPolicy(StoredDeploymentPolicy deploymentPolicy) throws DatastoreException {
	   final String METHOD_NAME = "removeDeploymentPolicy";
	   logger.entering(CLASS_NAME,METHOD_NAME,deploymentPolicy.toString());

	   //this.allDeploymentPolicy.remove(deploymentPolicy.getPrimaryKey());
	   this.deploymentPolicyImpl = null;

	   logger.exiting(CLASS_NAME,METHOD_NAME);


   }
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.DeployableConfiguration#setHighestVersion(int)
    */
   void setDeploymentPolicy(StoredDeploymentPolicy deploymentPolicy) throws DatastoreException {
	   
	      if (deploymentPolicy != null) //&& (deploymentPolicy instanceof StoredDeploymentPolicy))
	   
              this.deploymentPolicyImpl = (StoredDeploymentPolicyImpl)deploymentPolicy;
   }

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */   
   public StoredDeploymentPolicy getDeploymentPolicy() {
          return (StoredDeploymentPolicy)this.deploymentPolicyImpl;	   
              
   }

   public DomainSynchronizationMode getSynchMode() {
	   // TODO Auto-generated method stub
	   return this.synchMode;
   }

   public void setSynchMode(DomainSynchronizationMode synchMode) {
	   // TODO Auto-generated method stub
       this.synchMode = synchMode;
   }
   
   /**
    * <p>
    * Note:  The Local File System persists time as an attribute on domains element. 
    * </p> 
    * <inheritDoc /> 
    */
   
   public void setLastModifiedOfDeployedSource(long synchDate) {
		// TODO Auto-generated method stub
		  this.synchDate = synchDate;
	   }   

  public long getLastModifiedOfDeployedSource() {
		// TODO Auto-generated method stub
		return this.synchDate;
  }

  public boolean getOutOfSynch() {
	  return outOfSynch;
  }

  public void setOutOfSynch(boolean outOfSynch) {
	  this.outOfSynch = outOfSynch;
  }

/*  public boolean getCheckVersionSynch() {
	  return checkVersionSynch;
  }

  public void setCheckVersionSynch(boolean synchValue) {
	  this.checkVersionSynch = synchValue;
  }*/

  public void setQuiesceTimeout(int timeout) {
      this.quiesceTimeout = timeout; 

  }  
  public int getQuiesceTimeout() {
      return this.quiesceTimeout; 

  }

  /*
   * (non-Javadoc)
   * @see com.ibm.datapower.amt.dataAPI.StoredDomain#add(com.ibm.datapower.amt.dataAPI.StoredTag)
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
 
  /*
   * (non-Javadoc)
   * @see com.ibm.datapower.amt.dataAPI.StoredDomain#getTags()
   */
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

  /*
   * (non-Javadoc)
   * @see com.ibm.datapower.amt.dataAPI.StoredDomain#remove(com.ibm.datapower.amt.dataAPI.StoredTag)
   */
  public void remove(StoredTag tag) {
	   if (tag != null ) {
		   String tagKey = tag.getPrimaryKey();
		   StoredTag storedTag = this.tags.get(tagKey);
		   storedTag.remove(this);
		   this.tags.remove(tagKey);
	   }
  }
  
  /*
   * (non-Javadoc)
   * @see com.ibm.datapower.amt.dataAPI.StoredDomain#removeTags()
   */
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
