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
import java.util.List;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicyType;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeployablePolicy;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDeploymentPolicyVersion;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomain;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPVersion;

/**
 * <p>Holds all information related to a deployment policy version that can be 
 * used during domain configuration deployment to a DataPower device. The policy
 * name, the domain name, policy type, and  deployment policy image information
 * is persisted in the local file system. Below is a sample of a deployment policy versions element
 * contained within a deploymentPolicy element.
 * 
 * <blockquote>
 * <pre> 
 * {@code
 * <deploymentPolicy xmi:id="DPPolicy_0" highestVersion="1" policyName="lathas" SynchDate="1276806718906" SourceURL="file:///C:/DataPower2010/DatapowerTest/lathasdomain.zip" policyType="EXPORT" domainName="lathas">
 *     <versions xmi:id="DPPolicyVersion_0" timeCreatedInManager="1276807490921" userComment="Version generated from Domain.deployConfiguration()" versionImageFileLocation="Blob9198776463933681699.bin" versionNumber="1" policyType="EXPORT" domainName="lathas" policyName="lathas"/>
 * </deploymentPolicy>
 * }
 * </pre>
 * </blockquote>
 * </p>
 */
public class StoredDeploymentPolicyVersionImpl implements StoredDeploymentPolicyVersion
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;
   	   
   private StoredDeploymentPolicyImpl deploymentPolicy = null;
   private int versionNumber = 0;
   private Date timestamp = null;
   private String userComment = null;
   private String versionImageFileLocation = null;
   private String policyDomainName = null;
   private DeploymentPolicyType policyType = null;
   private String policyName = null;
   
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.
   private DPDeploymentPolicyVersion xmlObject = null;
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPPolicyVersion";
   
   private static final String CLASS_NAME = StoredDeploymentPolicyVersionImpl.class.getName();
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }   
   
   StoredDeploymentPolicyVersionImpl(StoredDeploymentPolicy versionedObject,
         Blob blob, String userComment, Date timeStamp) 
   			throws AlreadyExistsInRepositoryException, DatastoreException
   {
      if (versionedObject != null)
      {
         this.deploymentPolicy = (StoredDeploymentPolicyImpl) versionedObject;
      }
      else
      {
		 String message = Messages.getString("wamt.dataAPI.Repository.invalidVerObj");
		 throw new DatastoreException(message,"wamt.dataAPI.Repository.invalidVerObj");
      }

      if (blob != null)
      {
         this.versionImageFileLocation = RepositoryImpl.saveBlobToFile(blob);
      }
      this.userComment = userComment;
      this.timestamp = timeStamp;
      this.versionNumber = this.deploymentPolicy.getNextVersionNumber();
   
      this.policyName = this.deploymentPolicy.getPolicyName();
      this.policyDomainName = this.deploymentPolicy.getPolicyDomainName();
//      this.policyType = this.deploymentPolicy.getPolicyType();
//      this.deploymentPolicy.addVersion(this);
      
      if (deploymentPolicy.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }

   }

   /* 
    * This constructor is only used to load the object from an XML file
    * into memory. getNextVersionNumber() should not be called in this method.
    */   
   private StoredDeploymentPolicyVersionImpl(StoredDeploymentPolicy versionedObject,
         String versionImageLocation, String userComment, Date timeStamp, int versionNum) 
   			throws AlreadyExistsInRepositoryException, DatastoreException
   {
      if (versionedObject instanceof StoredDeploymentPolicyImpl)
      {
         this.deploymentPolicy = (StoredDeploymentPolicyImpl) versionedObject;
      }
      else
      {
 		 String message = Messages.getString("wamt.dataAPI.Repository.invalidVerObj");
		 throw new DatastoreException(message,"wamt.dataAPI.Repository.invalidVerObj");
      }
      this.versionImageFileLocation = versionImageLocation;
      this.userComment = userComment;
      this.timestamp = timeStamp;
      this.versionNumber = versionNum;
      
      if (deploymentPolicy.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }

   }
   
   /* (non-Javadoc)
    * 
    */
   public String getPrimaryKey()
   {
     
      String result = this.deploymentPolicy.getPrimaryKey() + ":" + 
      	Integer.toString(this.versionNumber);
      return result;
   }

   /* (non-Javadoc)
    * 
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
      
      return this.deploymentPolicy;
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
         return new Blob(new File(RepositoryImpl.getRepositoryDir() + 
               this.versionImageFileLocation));
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
      final String METHOD_NAME = "delete";
      logger.entering(CLASS_NAME,METHOD_NAME);
      this.deploymentPolicy.removeVersion(this);
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
   }

   /* 
    * Return the corresponding XML object 
    */   
   DPDeploymentPolicyVersion getXMLObject()
   {
      return this.xmlObject;
   }

   /* 
    * Transform a DPDomainVersionImpl object into an XML object for persistence  
    */   
   void toXMLObject(DPDeploymentPolicyVersion dpVersion, DPDomain dpDomain) throws DatastoreException
   {
	
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, this);
      
      this.xmlObject = dpVersion;
      dpVersion.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      
      dpVersion.setTimeCreatedInManager(this.timestamp.getTime());
      dpVersion.setUserComment(this.userComment);
      dpVersion.setVersionImageFileLocation(this.versionImageFileLocation);
      dpVersion.setVersionNumber(this.versionNumber);
      
      
      if (this.policyType !=null){  
    	  
    	  switch(this.policyType)
    	  {
    	      // Use the type's enumeration values to set the attribute value.
    	      case EXPORT:
    	    	  dpVersion.setPolicyType(DPDeploymentPolicyVersion.PolicyType.EXPORT);   	    	  
    	          break;
/*    	      case REFERENCE:
    	    	  dpVersion.setPolicyType(DPDeploymentPolicyVersion.PolicyType.REFERENCE);
    	          break;*/
    	      case XML:
    	    	  dpVersion.setPolicyType(DPDeploymentPolicyVersion.PolicyType.XML);
    	          break;
    	      case NONE:
    	    	  dpVersion.setPolicyType(DPDeploymentPolicyVersion.PolicyType.NONE);
    	          break;
    	      default:
    	          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", this.deploymentPolicy.getPolicyType().toString());
      			  throw new DatastoreException(message,"wamt.dataAPI.Repository.typeNotSupp");
    	  } 	  	      	  
      }
      if(this.policyDomainName != null){ 
   	     dpVersion.setDomainName(this.policyDomainName);
      }
      
      if(this.policyName != null) {
        dpVersion.setPolicyName(this.policyName);
      }  
      
      DPVersion domainVersion = null;   
      List<DPVersion> domainsList = dpDomain.getVersionsList();
      for (DPVersion d: domainsList){
    	  if (d.getVersionNumber() == dpVersion.getVersionNumber()){
    		  domainVersion = d;
    	  }
      }     
      
      if (RepositoryImpl.isCollectingGarbage() && this.versionImageFileLocation != null)
      {      
         RepositoryImpl.getinUseBlobFiles().add(this.versionImageFileLocation);
      }
      
      logger.exiting(CLASS_NAME,METHOD_NAME,dpVersion);  
   }
   
   
   /*
    * Transform an XML object into a StoredDomainVersionImpl object
    */
   static StoredDeploymentPolicyVersionImpl fromXMLObject(DPDeploymentPolicyVersion dpdv, StoredDeploymentPolicy policy)
   throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {dpdv, policy});
      StoredDeploymentPolicyVersionImpl dv = null;

      dv = new StoredDeploymentPolicyVersionImpl(
            policy, 
            dpdv.getVersionImageFileLocation(), 
            dpdv.getUserComment(), 
            new Date(dpdv.getTimeCreatedInManager()), 
            dpdv.getVersionNumber());
      
      dv.setPolicyDomainName(dpdv.getDomainName());
      dv.setPolicyName(dpdv.getPolicyName());

      if (dpdv.getPolicyType() !=null){  
    	  
    	  switch(dpdv.getPolicyType().intValue())
    	  {
    	      // Use the type's enumeration values to set the attribute value.
    	      case DPDeployablePolicy.PolicyType.INT_EXPORT:
    	    	  dv.setPolicyType(DeploymentPolicyType.EXPORT);   	    	  
    	          break;
    	      case DPDeployablePolicy.PolicyType.INT_XML:
    	    	  dv.setPolicyType(DeploymentPolicyType.XML);   	    	  
    	          break;
    	      case DPDeployablePolicy.PolicyType.INT_NONE:
    	    	  dv.setPolicyType(DeploymentPolicyType.NONE);   	    	  
    	          break;
    	      default:
    	          String message = Messages.getString("wamt.dataAPI.Repository.typeNotSupp", dpdv.getPolicyType().toString());
      			  throw new DatastoreException(message,"wamt.dataAPI.Repository.typeNotSupp");
    	  } 	  	      	  
      }	
      
      logger.exiting(CLASS_NAME, METHOD_NAME, dv);
      return dv;
   }


   public void setPolicyDomainName(String policyDomain) {
	   this.policyDomainName = policyDomain;	
   }

   public StoredDeploymentPolicy getDeploymentPolicy() {
	   // TODO Auto-generated method stub
	   return this.deploymentPolicy;
   }

   public StoredDomain getDomain() {
	   // TODO Auto-generated method stub
	   return null;
   }

   public DeploymentPolicyType getPolicyType() {
	   return this.policyType;
   }
   
   public void setPolicyName(String policyName) {
	   this.policyName = policyName;	
   }

   public String getPolicyName() {
	   // TODO Auto-generated method stub
	   return this.policyName;
   }

   public void setPolicyType(DeploymentPolicyType policyType) {
	   this.policyType = policyType;
   }

   public String getPolicyDomainName() {
	// TODO Auto-generated method stub
	return this.policyDomainName;
   }

//public String getVersionImageFileLocation() {
//	return versionImageFileLocation;
//}
   



}
