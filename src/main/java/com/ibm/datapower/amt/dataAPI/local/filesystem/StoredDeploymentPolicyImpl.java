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
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicyType;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.URLSource;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotEmptyInRepositoryException;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeployablePolicy;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeploymentPolicyVersion;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomain;


/**
 * The deployment policy information includes the domain configuration blob, the policy type, the policy source, and the associated domain name.
 * It is a container for all the deployed versions of this StoredDeploymentPolicy. Below is a sample of a persisted deployment policy.
 * <blockquote>
 * <pre> 
 * {@code
 * <deploymentPolicy xmi:id="DPPolicy_0" highestVersion="1" policyName="lathas" SynchDate="1276806718906" SourceURL="file:///C:/DataPower2010/DatapowerTest/lathasdomain.zip" policyType="EXPORT" domainName="lathas">
 *     ...
 * </deploymentPolicy>
 * }
 * </pre>
 * </blockquote>
 * @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy
 */
public class StoredDeploymentPolicyImpl implements StoredDeploymentPolicy
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;
   
   private StoredDomainImpl domain = null;;
   private StoredDeploymentPolicy deploymentPolicy = null;
   private int highestVersion = 0;
   private URLSource policyURL = null;
   private String policyDomainName = null;
   private String policyName = null;
   private DeploymentPolicyType policyType = null;
   private Hashtable versions = null;
   private long synchDate =0;   
  
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.

   
   private DPDeployablePolicy xmlObject = null;
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPPolicy";
   private static final String CLASS_NAME = StoredDeploymentPolicyImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }   
   
   

   StoredDeploymentPolicyImpl(StoredDomain sdomain,
             String policyName) throws AlreadyExistsInRepositoryException, DatastoreException
   {
      versions = new Hashtable();

      if (sdomain instanceof StoredDomainImpl)
          this.domain = (StoredDomainImpl) sdomain;
      this.policyName = policyName;  
      if ( this.domain != null )
    	  this.domain.setDeploymentPolicy(this);

      
   }

   /* (non-Javadoc)
    * @see @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy#delete()
    */
   public void delete() throws DatastoreException, NotEmptyInRepositoryException
   {
      final String METHOD_NAME = "delete";
      logger.entering(CLASS_NAME,METHOD_NAME);
      domain.removeDeploymentPolicy(this);
      logger.exiting(CLASS_NAME,METHOD_NAME);
     
   }

   /**
    * <p>
    * Note: In the Local File System implementation, the policy name is stored as an
    * attribute on the deploymentPolicy element.
    * </p> 
    * <inheritDoc />
    */
   public String getPolicyName()
   {
      
      return this.policyName;
   }
   

   void setPolicyName(String policyName)
   {
      
      this.policyName = policyName;
   }   

   /**
    * <p>
    * Note: The Local File System implementation combines the containing domain identifier
    * with the policy name on this <code>StoredDeploymentPolicy</code> as the unique identifier
    * of this object. 
    * </p> 
    * <inheritDoc />
    */
   public String getPrimaryKey()
   {

	  return this.domain.getPrimaryKey()+ ":" + this.getPolicyName();      
   }


   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy#getDomain()
    */
   public StoredDomain getDomain()
   {
      return this.domain;
   }
   

   /**
    * Adds a new StoredDomainVervion to this StoredDomain
    * 
    * @param version
    * @throws AlreadyExistsInRepositoryException
    */
   void addVersion(StoredDeploymentPolicyVersion version) throws AlreadyExistsInRepositoryException
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
   
   /*
    * Remove a StoredDomainVersion from this StoredDomain
    * @param version
    * @throws AlreadyExistsInRepositoryException
    */
   void removeVersion(StoredDeploymentPolicyVersion version) 
   {
      final String METHOD_NAME = "removeVersion";
      logger.entering(CLASS_NAME,METHOD_NAME,version);
      
      versions.remove(version.getPrimaryKey());
      
      logger.exiting(CLASS_NAME,METHOD_NAME);
   }
   
   /**
    * Updates the StoredDeploymentPolicyVersion keys if needed.
    * 
    * @return int Number of version entries that were updated
    */
   protected int updatePolicyVersions() {
      final String METHOD_NAME = "updatePolicyVersions";
      logger.entering(CLASS_NAME,METHOD_NAME);

      int result = 0;
      Enumeration ev = this.versions.keys();
      while (ev.hasMoreElements()) { // process each version element
	      String versionKey = (String) ev.nextElement();
		  StoredDeploymentPolicyVersion aVersion = (StoredDeploymentPolicyVersion) versions.get(versionKey);
		  String aKey = aVersion.getPrimaryKey();
		  if (!versionKey.equals(aKey)) { // versions key is wrong 
			  aVersion = (StoredDeploymentPolicyVersion) versions.remove(versionKey);
			  versions.put(aKey, aVersion);
			  result++;
		      logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "updated version " + aVersion +
		    		  " from key " + versionKey + " to " + aKey);
		  } // end, versions key is wrong
      } // end, process each version element
      logger.exiting(CLASS_NAME,METHOD_NAME);
      return result;
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
    * @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy#getVersion (int versionNumber)
    */   
   public StoredDeploymentPolicyVersion getVersion (int versionNumber)
   {
	  StoredDeploymentPolicyVersion result = null;
	  StoredDeploymentPolicyVersion[] versions  = this.getVersions();
	  for(StoredDeploymentPolicyVersion version: versions){
		  if (version.getVersionNumber() == versionNumber){
			  result = version;
		  }
	  }
      return result;
   }
   
   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#getVersions()
    */
   public StoredDeploymentPolicyVersion[] getVersions()
   {
      StoredDeploymentPolicyVersion[] result = new StoredDeploymentPolicyVersion[this.versions.size()];
      Enumeration e = this.versions.elements();
      int i = 0;
      while (e.hasMoreElements()) 
      {  
         Object o = e.nextElement();
         if (o instanceof StoredDeploymentPolicyVersion)
         {
            result[i++] = (StoredDeploymentPolicyVersion) o;
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

   /* (non-Javadoc)
    * @see com.ibm.datapower.amt.dataAPI.StoredDeployableConfiguration#setHighestVersion(int)
    */
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
   DPDeployablePolicy getXMLObject()
   {
      return this.xmlObject;
   }

   /* 
    * Transform a StoredDeploymentPolicyImpl object into an XML object for persistence  
    */ 
   void toXMLObject(DPDeployablePolicy dpPolicy, DPDomain dpDomain) throws DatastoreException
   {
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME,METHOD_NAME, this);          

      this.xmlObject = dpPolicy;
      dpPolicy.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      
      dpPolicy.setHighestVersion(this.highestVersion);
      if (this.policyName != null ){
         dpPolicy.setPolicyName(this.policyName);
      }
      
      dpPolicy.setSynchDate(this.synchDate);
      
      if (this.policyURL != null){
    	  dpPolicy.setSourceURL(this.policyURL.getURL());
      }
      if (this.policyType !=null){  
    	  
    	  switch(this.policyType)
    	  {
    	      // Use the type's enumeration values to set the attribute value.
    	      case EXPORT:
    	    	  dpPolicy.setPolicyType(DPDeployablePolicy.PolicyType.EXPORT);   	    	  
    	          break;
/*    	      case REFERENCE:
    	    	  dpPolicy.setPolicyType(DPDeployablePolicy.PolicyType.REFERENCE);
    	          break;*/
    	      case XML:
    	    	  dpPolicy.setPolicyType(DPDeployablePolicy.PolicyType.XML);
    	          break;
    	      case NONE:
    	    	  dpPolicy.setPolicyType(DPDeployablePolicy.PolicyType.NONE);
    	          break;
    	      default:
    	          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", this.policyType.toString());
      			  throw new DatastoreException(message,"wamt.dataAPI.Repository.typeNotSupp");
    	  } 	  	      	  
      }
      if (this.policyDomainName!= null){
    	  dpPolicy.setDomainName(this.policyDomainName);
      }
    
      // transform all the versions
      Vector<DPDeploymentPolicyVersion> v = new Vector();
      Enumeration e = versions.elements();
      int highVersionNum = 0;
      while (e.hasMoreElements())
      {
         StoredDeploymentPolicyVersionImpl dv = (StoredDeploymentPolicyVersionImpl)e.nextElement();
         if (dv.getVersionNumber()> highVersionNum){
        	 highVersionNum = dv.getVersionNumber();
         }
         DPDeploymentPolicyVersion dpdv = DPDeploymentPolicyVersion.Factory.newInstance();
         dv.toXMLObject(dpdv,dpDomain);
         v.addElement(dpdv);
      }
      dpPolicy.setVersionsArray(
            (DPDeploymentPolicyVersion[])(v.toArray(new DPDeploymentPolicyVersion[v.size()])));

      // If all domains have been deleted reset counter
      if (versions.size() < 1) {
    	  this.setHighestVersion(0);
          dpPolicy.setHighestVersion(this.highestVersion);
      }else{
    	  this.setHighestVersion(highVersionNum);
          dpPolicy.setHighestVersion(this.highestVersion);    	  
      }
                  
      logger.exiting(CLASS_NAME,METHOD_NAME,dpPolicy);
   }
   
   /*
    * Transform an XML object into a StoredDomainImpl object
    */
   static void fromXMLObject(DPDeployablePolicy dpd, StoredDeploymentPolicy policyObj)
   throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{dpd, policyObj});
      
      StoredDeploymentPolicyImpl  policy = (StoredDeploymentPolicyImpl)policyObj;
      
      policy.setPolicyDomainName(dpd.getDomainName());
      String url =dpd.getSourceURL();
      if(url != null){
    	  try{
    		  policy.setPolicyURL(new URLSource(url));
    	  } catch (URISyntaxException e) {
  			  String message = Messages.getString("wamt.dataAPI.Repository.badURL", url);
			  throw new DatastoreException(message,"wamt.dataAPI.Repository.badURL", e);
    	  }
      }
    	
      policy.setHighestVersion(dpd.getHighestVersion());
      policy.setLastModifiedOfDeployedSource(dpd.getSynchDate());
	  
      if (dpd.getPolicyType() !=null){  
    	  
    	  switch(dpd.getPolicyType().intValue())
    	  {
    	      // Use the type's enumeration values to set the attribute value.
    	      case DPDeployablePolicy.PolicyType.INT_EXPORT:
    	    	  policy.setPolicyType(DeploymentPolicyType.EXPORT);   	    	  
    	          break;
/*    	      case DPDeployablePolicy.PolicyType.INT_REFERENCE:
    	    	  policy.setPolicyType(DeploymentPolicyType.REFERENCE);   	    	  
    	          break;*/
    	      case DPDeployablePolicy.PolicyType.INT_XML:
    	    	  policy.setPolicyType(DeploymentPolicyType.XML);   	    	  
    	          break;
    	      case DPDeployablePolicy.PolicyType.INT_NONE:
    	    	  policy.setPolicyType(DeploymentPolicyType.NONE);   	    	  
    	          break;
    	      default:
    	          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", dpd.getPolicyType().toString());
      			  throw new DatastoreException(message,"wamt.dataAPI.Repository.typeNotSupp");
    	  } 	  	      	  
      }	  
      policy.setPolicyName(dpd.getPolicyName()); 
      //DPDeploymentPolicyVersion[] dfvs = dpd.getVersionsArray();
      List <DPDeploymentPolicyVersion> dfvs = dpd.getVersionsList();
      

      for (int i = 0; i < dfvs.size(); i++)
      {
    	  StoredDeploymentPolicyVersionImpl sv =StoredDeploymentPolicyVersionImpl.fromXMLObject
    	  (dfvs.get(i), policy); 
    	  policy.addVersion(sv);
      }
      
      logger.exiting(CLASS_NAME, METHOD_NAME, policy);  
   }
   /**
    * <p>
    * Note:  For the Local File System implementation, the policy domain name is stored as an
    * attribute on the deploymentPolicy element.
    * </p> 
    * 
    * <inheritDoc />
    */    
   public String getPolicyDomainName() {
	   return policyDomainName;
   }

   public void setPolicyDomainName(String policyDomainName) {
	   this.policyDomainName = policyDomainName;
   }

   /* (non-Javadoc)
    * @see @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy#getPolicyURL()
    */
   public URLSource getPolicyURL() {
	   return policyURL;
   }

   void setPolicyURL(URLSource policyURL) {
	   this.policyURL = policyURL;
   }

   /**
    * 
    * <p>
    * Note: In the Local File System implementation, the policy type is stored as an
    * attribute on the deploymentPolicy element.
    * </p>
    * <inheritDoc />
    */
   public DeploymentPolicyType getPolicyType() {
	   return policyType;
   }


   void setPolicyType(DeploymentPolicyType policyType) {
	   this.policyType = policyType;
   }
   
   /**
    * 
     * <p>
     * Note: In the Local File System implementation, the synchDate is stored as an
     * attribute on the deploymentPolicy element.
     * </p> 
    * <inheritDoc />
    */  
   public void setLastModifiedOfDeployedSource(long synchDate) {
		// TODO Auto-generated method stub
		  this.synchDate = synchDate;
	   }   

   /* (non-Javadoc)
    * @see @see com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy#getLastModifiedOfDeployedSource()
    */   
   public long getLastModifiedOfDeployedSource() {
		// TODO Auto-generated method stub
		return this.synchDate;
  }
  



   
}
