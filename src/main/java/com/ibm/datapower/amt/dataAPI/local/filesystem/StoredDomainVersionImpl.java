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
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomainVersion;

/**
 * 
 * A version of Domain object persisted in the repository that can be deployed to
 * a DataPower device.  It is contained within the Domain on which it is deployed.  It is persisted
 * when a domain configuration is successfully deployed by the manager. Below is a sample domain version element
 * as well as the deployment policy and deployment version that are also created and persisted with the domain version
 * by the clientAPI.</p>
 * <blockquote>
 * <pre> 
 * {@code
 *   <domains xmi:id="DPDomain_0" highestVersion="1" name="domain1" SourceURL="device://9.42.112.79/domain1" SynchDate="0" OutOfSynch="false" checkVersionSynch="false" quiesceTimeout="60" SyncMode="MANUAL">
 *     <versions xmi:id="DPDomainVersion_0" timeCreatedInManager="1276807490921" userComment="Version generated from Domain.deployConfiguration()" versionImageFileLocation="Blob9074270970331689053.bin" versionNumber="1" xsi:type="dat:DPDomainVersion" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
 *     <deploymentPolicy xmi:id="DPPolicy_0" highestVersion="1" policyName="domain1" SynchDate="1276806718906" SourceURL="file:///C:/DataPower2010/DatapowerTest/domain.zip" policyType="EXPORT" domainName="domain1">
 *       <versions xmi:id="DPPolicyVersion_0" timeCreatedInManager="1276807490921" userComment="Version generated from Domain.deployConfiguration()" versionImageFileLocation="Blob9198776463933681699.bin" versionNumber="1" policyType="EXPORT" domainName="domain1" policyName="domain1"/>
 *     </deploymentPolicy>
 * </domains>
 * }
 * </pre>
 * </blockquote>
 * 
 */
public class StoredDomainVersionImpl implements StoredDomainVersion
{
   /* Standard copyright and build info constants */
   private static final String CR = Constants.COPYRIGHT_2009_2013;
	   
   private String primaryKey = null;
   private StoredDomainImpl domain = null;
   private int versionNumber = 0;
   private Date timestamp = null;
   private String userComment = null;
   private String versionImageFileLocation = null;
   
   // The corresponding XML object. It is created when this object 
   // is being persisted into an XML file.
   private DPDomainVersion xmlObject = null;
   
   // XML object counter for this class. It is used in XML objects' IDs.
   private static int xmlObjectNum = 0;
   private static final String XML_CLASS_NAME = "DPDomainVersion";
   //private static Logger TRACE = RepositoryImpl.TRACE;
   //private static final String CLASS_NAME = "StoredDomainVersionImpl";
   
   private static final String CLASS_NAME = StoredDomainVersionImpl.class.getName();   
   protected final static Logger logger = Logger.getLogger(CLASS_NAME);
   static {
       LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
   }   
   
   StoredDomainVersionImpl(StoredDomain versionedObject,
         Blob blob, String userComment, Date timeStamp) 
   			throws AlreadyExistsInRepositoryException, DatastoreException
   {
      if (versionedObject instanceof StoredDomainImpl)
      {
         this.domain = (StoredDomainImpl) versionedObject;
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
      this.versionNumber = this.domain.getNextVersionNumber();
      
      if (domain.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message, "wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }

   }

   /* 
    * This constructor is only used to load the object from an XML file
    * into memory. getNextVersionNumber() should not be called in this method.
    */   
   private StoredDomainVersionImpl(StoredDomain versionedObject,
         String versionImageLocation, String userComment, Date timeStamp, int versionNum) 
   			throws AlreadyExistsInRepositoryException, DatastoreException
   {
      if (versionedObject instanceof StoredDomainImpl)
      {
         this.domain = (StoredDomainImpl) versionedObject;
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
      
      if (domain.checkIfVersionExists(getPrimaryKey())) {
 		 String message = Messages.getString("wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
		 throw new AlreadyExistsInRepositoryException(message,"wamt.dataAPI.Repository.alreadyExists", getPrimaryKey());
      }
   }
   
   /**
    * <p>
    * Note:  The Local File System implementation uses the unique identifier of the <code>StoredDomain</code> 
    * combined with the version number of this <code>StoredDomainVersion</code>.
    * 
    * </p>
    * <inheritDoc />
    */
   public String getPrimaryKey()
   {
     
      String result = this.domain.getPrimaryKey() + ":" + 
      	Integer.toString(this.versionNumber);
      return result;
   }

   /**
    * <p>
    * Note:  In the Local File System implementation the Domain element contains  
    * version elements in the WAMT.repository.xml file.
    * </p> 
    * <inheritDoc />
    */
   public StoredDomain getDomain()
   {

      return this.domain;
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
      
      return this.domain;
   }

   /**
    * <p>
    * Note:  The Local File System implementation 
    * stores the timestamp as an attribute on the version element  
    * </p> 
    * <inheritDoc />
    */
   public Date getTimestamp()
   {
	   Date result = this.timestamp;
	   return result;
   }

   /**
    * <p>
    * Note:  The Local File System implementation 
    * stores the getUserComment as an attribute on the version elements  
    * </p> 
    * <inheritDoc />
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

   /**
    * <p>
    * Note:  The Local File System implementation 
    * stores each image as a separate bin file.  The name of the file is stored as an attribute
    * on the version element in the WAMT.repository.xml file. 
    * </p> 
    * <inheritDoc />
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

   /**
    * <p>
    * Note:  The Local File System implementation 
    * clears the Stored object and removes the version element from the WAMT.repository.xml file.
    * It does not modify any other elements.  The corresponding deployment policy version is deleted
    * by logic in clientAPI.
    * </p> 
    */
   public void delete()
   {
      final String METHOD_NAME = "delete";
      logger.entering(CLASS_NAME,METHOD_NAME);
      this.domain.removeVersion(this);
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
   DPDomainVersion getXMLObject()
   {
      return this.xmlObject;
   }

   /* 
    * Transform a DPDomainVersionImpl object into an XML object for persistence  
    */   
   void toXMLObject(DPDomainVersion dpVersion)
   {
	  
      final String METHOD_NAME = "toXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, this);
      
      this.xmlObject = dpVersion;
      dpVersion.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);
      
      dpVersion.setTimeCreatedInManager(this.timestamp.getTime());
      dpVersion.setUserComment(this.userComment);
      dpVersion.setVersionImageFileLocation(this.versionImageFileLocation);
      dpVersion.setVersionNumber(this.versionNumber);
      
      if (RepositoryImpl.isCollectingGarbage() && this.versionImageFileLocation != null)
      {      
         RepositoryImpl.getinUseBlobFiles().add(this.versionImageFileLocation);
      }          
      
      logger.exiting(CLASS_NAME,METHOD_NAME,dpVersion);  
   }
   /**
     * <p>
     * Note: The Local File System implementation is provided for the convenience of customers 
     * that wish to write their own dataAPI implementation.
     * @see com.ibm.datapower.amt.clientAPI.Manager#generateReport(String, String)
     * </p> 
     * <inheritDoc /> 
    */
   public void recordDomainVersion(OutputStream outputStream) {
	   String METHOD_NAME="recordDomainVersion";
	   //RepositoryImpl repository = 
	   RepositoryImpl.getInstance();

	   String lineSeparator = System.getProperty("line.separator");  //$NON-NLS-1$
	   if ((lineSeparator == null) || (lineSeparator.length() == 0)) {
		   lineSeparator = "\n"; //$NON-NLS-1$
	   }

	   SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
	   formatter.setTimeZone ( TimeZone.getTimeZone ( "GMT" )  ); 
	   String timeCreated = formatter.format(this.timestamp);
	   try {
		   String divider = "  ========================================================================================================================================================================================================";

		   StoredDomain dom = this.getDomain();
		   StoredDeploymentPolicy pol = null;
		   StoredDeploymentPolicyVersion version = null;
		   if (dom != null) {
			   pol = dom.getDeploymentPolicy();
		   }
		   if(pol != null){
			   version =  pol.getVersion(this.versionNumber);
		   }

		   if(dom!=null){
			   String domReport = "  Domain - Name: "+ dom.getName() + ", Source URL: " + dom.getSourceURL().getURL()+
			   ", Synchronization Mode: " + dom.getSynchMode();
			   String report = "  Domain Version - Version: " + this.versionNumber + 
			   ", Time Created: " + timeCreated +
			   ", Version Image FileLocation: " + versionImageFileLocation + 
			   ", Highest Version: " + dom.getHighestVersionNumber();

			   outputStream.write(domReport.getBytes());
			   outputStream.write(lineSeparator.getBytes());
			   //outputStream.write(("Time Created"+ timeCreated).getBytes());
			   //outputStream.write(": ".getBytes());	            
			   outputStream.write(report.getBytes());		
			   outputStream.write(lineSeparator.getBytes());	
			   outputStream.write(lineSeparator.getBytes());	            

			   if (pol!= null){
				   String policyURL = "";	
				   String policyName = "";
				   String policyDomain = "";
				   if (pol.getPolicyURL()!= null){
					   policyURL = pol.getPolicyURL().getURL();
				   }
				   if(pol.getPolicyName()!=null){
					   policyName = pol.getPolicyName();
				   }
				   if(pol.getPolicyDomainName()!=null){
					   policyDomain = pol.getPolicyDomainName();
				   }

				   String policyReport = "  Deployment Policy -"+  " Policy URL: " + policyURL + ", Policy Name: " + policyName +
				   ", Policy Domain Name: "+ policyDomain + ", Highest Version: " + pol.getHighestVersionNumber();

			
					   outputStream.write(policyReport.getBytes());
					   outputStream.write(lineSeparator.getBytes());				
					   //outputStream.write(("  "+ formatter.format(version.getTimestamp())).getBytes());
					   //outputStream.write(": ".getBytes());                
				   if (version!= null){	
					   report = "  Policy Version - Version: "+ version.getVersionNumber() + 
					   ", Time Created: " + formatter.format(version.getTimestamp()) +				   
					   //", Last Synchronization Date: " + version.getTimestamp()+   
					   ", Policy Type: " + version.getPolicyType();// + 
					   //", Version Image FileLocation: " + version.getVersionImageFileLocation();

					   outputStream.write(report.getBytes());		
					   outputStream.write(lineSeparator.getBytes());
				   }else{
					   
				   }
			   }else{
				   String policyReport = "  Deployment Policy - is null";
				   outputStream.write(policyReport.getBytes());
				   outputStream.write(lineSeparator.getBytes());
			   }
		   }else{
			   String domReport = "  Domain - is null";
			   outputStream.write(domReport.getBytes());
			   outputStream.write(lineSeparator.getBytes());
		   }
		   outputStream.write(divider.getBytes());   
		   outputStream.write(lineSeparator.getBytes());		

	   } catch (IOException e) {
		   // TODO Auto-generated catch block
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "IOException thrown:", e);
	   }			
}

/*
    * Transform an XML object into a StoredDomainVersionImpl object
    */
   static StoredDomainVersionImpl fromXMLObject(DPDomainVersion dpdv, StoredDomain domain)
   throws DatastoreException
   {
      final String METHOD_NAME = "fromXMLObject";
      logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {dpdv, domain});
      StoredDomainVersionImpl dv = null;

      dv = new StoredDomainVersionImpl(
            domain, 
            dpdv.getVersionImageFileLocation(), 
            dpdv.getUserComment(), 
            new Date(dpdv.getTimeCreatedInManager()), 
            dpdv.getVersionNumber());

      logger.exiting(CLASS_NAME, METHOD_NAME, dv);
      return dv;
   }
}
