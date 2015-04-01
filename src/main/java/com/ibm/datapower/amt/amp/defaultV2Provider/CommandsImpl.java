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


package com.ibm.datapower.amt.amp.defaultV2Provider;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.datapower.schemas.appliance.management.x20.Backup;
import com.datapower.schemas.appliance.management.x20.CompareConfigRequestDocument;
import com.datapower.schemas.appliance.management.x20.CompareConfigResponseDocument;
import com.datapower.schemas.appliance.management.x20.CompareResult;
import com.datapower.schemas.appliance.management.x20.ConfigState;
import com.datapower.schemas.appliance.management.x20.CryptoFileName;
import com.datapower.schemas.appliance.management.x20.DeleteDomainRequestDocument;
import com.datapower.schemas.appliance.management.x20.DeleteDomainResponseDocument;
import com.datapower.schemas.appliance.management.x20.DeploymentPolicyConfiguration;
import com.datapower.schemas.appliance.management.x20.File;
import com.datapower.schemas.appliance.management.x20.Firmware;
import com.datapower.schemas.appliance.management.x20.GetCryptoArtifactsRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetCryptoArtifactsResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetDeviceInfoRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetDeviceInfoResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainExportRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainExportResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainListRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainListResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainStatusRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetDomainStatusResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetErrorReportRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetErrorReportResponseDocument;
import com.datapower.schemas.appliance.management.x20.GetTokenRequestDocument;
import com.datapower.schemas.appliance.management.x20.GetTokenResponseDocument;
import com.datapower.schemas.appliance.management.x20.ManagementInterface;
import com.datapower.schemas.appliance.management.x20.ManagementType;
import com.datapower.schemas.appliance.management.x20.OpState;
import com.datapower.schemas.appliance.management.x20.PingRequestDocument;
import com.datapower.schemas.appliance.management.x20.PingResponseDocument;
import com.datapower.schemas.appliance.management.x20.QuiesceRequestDocument;
import com.datapower.schemas.appliance.management.x20.QuiesceResponseDocument;
import com.datapower.schemas.appliance.management.x20.RebootMode;
import com.datapower.schemas.appliance.management.x20.RebootRequestDocument;
import com.datapower.schemas.appliance.management.x20.RebootResponseDocument;
import com.datapower.schemas.appliance.management.x20.RestartDomainRequestDocument;
import com.datapower.schemas.appliance.management.x20.RestartDomainResponseDocument;
import com.datapower.schemas.appliance.management.x20.SecureBackup;
import com.datapower.schemas.appliance.management.x20.SecureBackupRequestDocument;
import com.datapower.schemas.appliance.management.x20.SecureBackupResponseDocument;
import com.datapower.schemas.appliance.management.x20.SecureRestoreRequestDocument;
import com.datapower.schemas.appliance.management.x20.SecureRestoreResponseDocument;
import com.datapower.schemas.appliance.management.x20.SetDomainExportRequestDocument;
import com.datapower.schemas.appliance.management.x20.SetDomainExportResponseDocument;
import com.datapower.schemas.appliance.management.x20.SetFileRequestDocument;
import com.datapower.schemas.appliance.management.x20.SetFileResponseDocument;
import com.datapower.schemas.appliance.management.x20.SetFirmwareRequestDocument;
import com.datapower.schemas.appliance.management.x20.SetFirmwareResponseDocument;
import com.datapower.schemas.appliance.management.x20.StartDomainRequestDocument;
import com.datapower.schemas.appliance.management.x20.StartDomainResponseDocument;
import com.datapower.schemas.appliance.management.x20.Status;
import com.datapower.schemas.appliance.management.x20.StopDomainRequestDocument;
import com.datapower.schemas.appliance.management.x20.StopDomainResponseDocument;
import com.datapower.schemas.appliance.management.x20.SubscribeRequestDocument;
import com.datapower.schemas.appliance.management.x20.SubscribeResponseDocument;
import com.datapower.schemas.appliance.management.x20.SubscriptionStateUrl;
import com.datapower.schemas.appliance.management.x20.SubscriptionTopic;
import com.datapower.schemas.appliance.management.x20.TokenType;
import com.datapower.schemas.appliance.management.x20.UnquiesceRequestDocument;
import com.datapower.schemas.appliance.management.x20.UnquiesceResponseDocument;
import com.datapower.schemas.appliance.management.x20.UnsubscribeRequestDocument;
import com.datapower.schemas.appliance.management.x20.UnsubscribeResponseDocument;
import com.datapower.schemas.appliance.management.x20.SecureBackup.SecureBackupFile;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.QuiesceStatus;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPConstants;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.ConfigObject;
import com.ibm.datapower.amt.amp.DeleteObjectResult;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DeviceMetaInfo;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.ErrorReport;
import com.ibm.datapower.amt.amp.InterDependentServiceCollection;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.NotExistException;
import com.ibm.datapower.amt.amp.PingResponse;
import com.ibm.datapower.amt.amp.ReferencedObjectCollection;
import com.ibm.datapower.amt.amp.SOAPHelper;
import com.ibm.datapower.amt.amp.SOAPHelperFactory;
import com.ibm.datapower.amt.amp.SubscriptionResponseCode;
import com.ibm.datapower.amt.amp.defaultV3Provider.Utils;
import com.ibm.datapower.amt.clientAPI.ConfigService;
import com.ibm.datapower.amt.clientAPI.DeletedException;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicy;
import com.ibm.datapower.amt.clientAPI.Device;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.RuntimeService;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Implements the Commands interface specified in
 * {@link com.ibm.datapower.amt.amp.Commands}. The device has a schema which describes
 * the format and allowed values for the SOAP messages, see
 * store:/app-mgmt-protocol-v2.xsd and store:/app-mgmt-protocol-v2.wsdl.
 * <p>
 * This implementation leverages the Apache XMLBeans generated classes from the
 * AMP schema.
 * 
 * 
 * @see com.ibm.datapower.amt.amp.Commands
 */
//* @version $Id: CommandsImpl.java,v 1.5 2011/01/18 20:46:22 wjong Exp $

public class CommandsImpl implements Commands {
    private SOAPHelper soapHelper;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    final String setFirmwareHeader = "<amp:SetFirmwareRequest xmlns:amp=\"http://www.datapower.com/schemas/appliance/management/"+AMPConstants.AMP_V2_0+"\">\n<amp:Firmware>";  //$NON-NLS-1$
    final String setFirmwareFooter = "</amp:Firmware>\n</amp:SetFirmwareRequest>"; //$NON-NLS-1$

    final byte[] setFirmwareHeaderBytes = setFirmwareHeader.getBytes();
    final byte[] setFirmwareFooterBytes = setFirmwareFooter.getBytes();
    
    private static final String CLASS_NAME = CommandsImpl.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    public CommandsImpl(String soapHelperImplementationClassName) throws AMPException{
        
        final String METHOD_NAME = "CommandsImpl()"; //$NON-NLS-1$
        
        soapHelper = SOAPHelperFactory.getSOAPHelper(soapHelperImplementationClassName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "soapHelper object created"); //$NON-NLS-1$
        
    }
    
    /* 
     * @see com.ibm.datapower.amt.amp.Commands#subscribeToDevice(
     * com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, 
     * com.ibm.datapower.amt.StringCollection, java.net.URL)
     */
    public SubscriptionResponseCode subscribeToDevice(DeviceContext device,
                                                      String subscriptionId, StringCollection topics, URL callback)
        throws InvalidCredentialsException, AMPIOException,
               DeviceExecutionException, AMPException {
        
        final String METHOD_NAME = "subscribeToDevice"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, 
                        new Object[]{device, subscriptionId, topics, callback});
        
        SubscribeRequestDocument requestDoc = 
            SubscribeRequestDocument.Factory.newInstance();
        SubscribeRequestDocument.SubscribeRequest subscribeRequest = 
            requestDoc.addNewSubscribeRequest();
        SubscribeRequestDocument.SubscribeRequest.Subscription subscription = 
            subscribeRequest.addNewSubscription();
        
        subscription.setId(subscriptionId);
        subscription.setURL(callback.toString());
        
        SubscribeRequestDocument.SubscribeRequest.Subscription.Topics requestTopics = 
            subscription.addNewTopics();
        
        final String configuration = SubscriptionTopic.CONFIGURATION.toString();
        final String firmware = SubscriptionTopic.FIRMWARE.toString();
        final String operational = SubscriptionTopic.OPERATIONAL.toString();
        final String all = SubscriptionTopic.X.toString();
        for (int i = 0; i < topics.size(); i++){
            if (topics.get(i).equalsIgnoreCase(configuration))
                requestTopics.addTopic(SubscriptionTopic.CONFIGURATION);
            else if (topics.get(i).equalsIgnoreCase(firmware))
                requestTopics.addTopic(SubscriptionTopic.FIRMWARE);
            else if (topics.get(i).equalsIgnoreCase(operational))
                requestTopics.addTopic(SubscriptionTopic.OPERATIONAL);
            else if (topics.get(i).equalsIgnoreCase(all))
                requestTopics.addTopic(SubscriptionTopic.X);
            else {
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic",topics.get(i));
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic",topics.get(i));
                logger.throwing(CLASS_NAME, METHOD_NAME,e);
                throw e;
            }
        }
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "subscribeRequestDocument created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending subscriptionRequest(" + subscriptionId + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage);        
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        SubscriptionResponseCode result = null;
        /* Parse the request into a SubscribeResponseDocument */
        try{
            SubscribeResponseDocument responseDoc = 
                SubscribeResponseDocument.Factory.parse(responseDocXml);
            
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "Received subscriptionResponse(" + subscriptionId + ") from device "  //$NON-NLS-1$ //$NON-NLS-2$
                        + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "subscribeResponseDocument received"); //$NON-NLS-1$
            
            SubscribeResponseDocument.SubscribeResponse subscriptionResponse = 
                responseDoc.getSubscribeResponse();
            if (subscriptionResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noResponse",params); 
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noResponse",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            SubscriptionStateUrl subState = subscriptionResponse.getSubscriptionState();
            
            if (subState == null){
            	
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noSubState",params); 
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noSubState",params); //$NON-NLS-1$ //$NON-NLS-2$            	
                throw e;
            }
            /* Return appropriate SubscriptionResponseCode from response */
            if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString()))
                result = SubscriptionResponseCode.ACTIVE;
            else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.DUPLICATE_STRING)){
                String originalURL = subState.getURL();
                result = SubscriptionResponseCode.createWithDuplicate(originalURL);
            }
            else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))
                /* according to the device's AMP implementation, it will never
                 * send a NONE in a subscribe response, only in an unsubscribe
                 * response or a ping response. But for the sake of completeness,
                 * set the POJO value just in case this value ever shows up. */
                result = SubscriptionResponseCode.NONE;
            else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString())){
            	Object[] params = {subscriptionId,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errSub",params); 
                DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errSub",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSubDev",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSubDev",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return result;
    }
    
    /* 
     * @see com.ibm.datapower.amt.amp.Commands#unsubscribeFromDevice(
     * com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public void unsubscribeFromDevice(DeviceContext device,
                                      String subscriptionID, StringCollection topics) throws NotExistException,
                                      InvalidCredentialsException, DeviceExecutionException,
                                      AMPIOException, AMPException {
        
        final String METHOD_NAME = "unsubscribeFromDevice"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, 
                        new Object[]{device, subscriptionID, topics});
        
        UnsubscribeRequestDocument requestDoc = 
            UnsubscribeRequestDocument.Factory.newInstance();
        UnsubscribeRequestDocument.UnsubscribeRequest unsubscribeRequest = 
            requestDoc.addNewUnsubscribeRequest();
        UnsubscribeRequestDocument.UnsubscribeRequest.Subscription unsubscription = 
            unsubscribeRequest.addNewSubscription();
        
        unsubscription.setId(subscriptionID);
        
        UnsubscribeRequestDocument.UnsubscribeRequest.Subscription.Topics requestTopics = 
            unsubscription.addNewTopics();
        
        final String configuration = SubscriptionTopic.CONFIGURATION.toString();
        final String firmware = SubscriptionTopic.FIRMWARE.toString();
        final String operational = SubscriptionTopic.OPERATIONAL.toString();
        final String all = SubscriptionTopic.X.toString();
        for (int i = 0; i < topics.size(); i++){
            if (topics.get(i).equalsIgnoreCase(configuration))
                requestTopics.addTopic(SubscriptionTopic.CONFIGURATION);
            else if (topics.get(i).equalsIgnoreCase(firmware))
                requestTopics.addTopic(SubscriptionTopic.FIRMWARE);
            else if (topics.get(i).equalsIgnoreCase(operational))
                requestTopics.addTopic(SubscriptionTopic.OPERATIONAL);
            else if (topics.get(i).equalsIgnoreCase(all))
                requestTopics.addTopic(SubscriptionTopic.X);
            else {
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic",topics.get(i));
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic",topics.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "unsubscribeRequestDocument created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending unsubscriptionRequest(" + subscriptionID + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage);
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a UnsubscribeResponseDocument */
        try{
            UnsubscribeResponseDocument responseDoc = 
                UnsubscribeResponseDocument.Factory.parse(responseDocXml);
            
            UnsubscribeResponseDocument.UnsubscribeResponse unsubscribeResponse = responseDoc.getUnsubscribeResponse();
            if (unsubscribeResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            com.datapower.schemas.appliance.management.x20.SubscriptionState.Enum subState = 
                unsubscribeResponse.getSubscriptionState();
            String subStateString = null;
            if (subState != null)
                subStateString = subState.toString();
            else{
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noUnsubState",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noUnsubState",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString())){
            	Object[] params = {subscriptionID,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errRemSub",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errRemSub",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else if ((subStateString.equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString())) || 
                     (subStateString.equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))) {
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            else{
            	Object[] params = {subscriptionID,device.getHostname()};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.subIdNotExist",params);
            	NotExistException e = new NotExistException(message,"wamt.amp.defaultV2Provider.CommandsImpl.subIdNotExist",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);  
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumUnsub",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumUnsub",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* 
     * @see com.ibm.datapower.amt.amp.Commands#pingDevice(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public PingResponse pingDevice(DeviceContext device, String subscriptionID)
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "pingDevice"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, 
                        new Object[]{device, subscriptionID});
        
        PingRequestDocument requestDoc = PingRequestDocument.Factory.newInstance();
        PingRequestDocument.PingRequest pingRequest = requestDoc.addNewPingRequest();
        
        pingRequest.setSubscriptionID(subscriptionID);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "PingRequestDocument created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending pingRequest(" + subscriptionID + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a PingResponse */
        PingResponse result = null;
        try{
            PingResponseDocument responseDoc = PingResponseDocument.Factory.parse(responseDocXml);
            
            PingResponseDocument.PingResponse pingResponse = responseDoc.getPingResponse();
            if (pingResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noPingResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noPingResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            com.datapower.schemas.appliance.management.x20.SubscriptionState.Enum subState = 
                pingResponse.getSubscriptionState();
            String subStateString = null;
            if (subState != null)
                subStateString = subState.toString();
            else{
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noSubStateInPing",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noSubStateInPing",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            } 
            
            if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString()))
                result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.ACTIVE);
            else if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))
                result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.NONE);
            else if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString()))
                result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.FAULT);
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumPingResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumPingResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower.amt.amp.DeviceContext)
     */
    public DeviceMetaInfo getDeviceMetaInfo(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "getDeviceMetaInfo"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        GetDeviceInfoRequestDocument requestDoc = 
            GetDeviceInfoRequestDocument.Factory.newInstance();
        //GetDeviceInfoRequestDocument.GetDeviceInfoRequest getDeviceInfoRequest = 
            requestDoc.addNewGetDeviceInfoRequest();
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getDeviceInfoRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getDeviceInfoRequest to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a DeviceMetaInfo object */
        try{
            GetDeviceInfoResponseDocument responseDoc = 
                GetDeviceInfoResponseDocument.Factory.parse(responseDocXml);
            GetDeviceInfoResponseDocument.GetDeviceInfoResponse getDeviceInfoResponse = 
                responseDoc.getGetDeviceInfoResponse();
            
            if (getDeviceInfoResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noRespgetDevInfo",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noRespgetDevInfo",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            String deviceName = getDeviceInfoResponse.getDeviceName();
            String currentAMPVersion = Device.getCurrentAMPVersionFromGetDeviceInfoResponse(getDeviceInfoResponse.getCurrentAMPVersion());
            String serialNumber = getDeviceInfoResponse.getDeviceSerialNo();
            
            String responseDeviceType = getDeviceInfoResponse.getDeviceType();
            if ((responseDeviceType == null) || (responseDeviceType.length() == 0)) {
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.devTypNotSpecified",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.devTypNotSpecified",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            com.ibm.datapower.amt.DeviceType deviceType = null;
            deviceType = com.ibm.datapower.amt.DeviceType.fromString(responseDeviceType);
            
            String versionComposite = getDeviceInfoResponse.getFirmwareVersion();
            int periodIndex = versionComposite.indexOf("."); //$NON-NLS-1$
            String firmwareLevel = versionComposite.substring(periodIndex + 1);
            
            ModelType modelType = 
                Device.getModelTypeFromDeviceID(getDeviceInfoResponse.getDeviceID());
            String hardwareOptions =
                Device.getHardwareOptionsFromDeviceID(getDeviceInfoResponse.getDeviceID());
            
            StringCollection featureLicenses = new StringCollection();
            String[] deviceFeaturesArray = getDeviceInfoResponse.getDeviceFeatureArray();
            if (deviceFeaturesArray != null){
                for (int i = 0; i < deviceFeaturesArray.length; i++)
                    featureLicenses.add(deviceFeaturesArray[i]);
            }
            
            int webGUIPort = -1;
            ManagementInterface[] mgmtArray = getDeviceInfoResponse.getManagementInterfaceArray();
            if (mgmtArray != null){
                for (int i = 0; i < mgmtArray.length; i++){
                    if (mgmtArray[i].getType() == ManagementType.WEB_MGMT){
                        try{
                    		webGUIPort = Integer.parseInt(mgmtArray[i].getStringValue());
                    	} catch (NumberFormatException nfe){
                        	String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.invalidWebGUIPort");
                            AMPException e = new AMPException(message,"wamt.amp.defaultProvider.SOAPHelper.invalidWebGUIPort"); //$NON-NLS-1$
                        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                            throw e;
                    	}
                    	
                        break;
                    }
                }
            }
            
            int supportedCommands[] = new int[]{
					Commands.BACKUP_DEVICE,	Commands.DELETE_DOMAIN,	Commands.GET_DEVICE_METAINFO, 
					Commands.GET_DOMAIN, Commands.GET_DOMAIN_DIFFERENCES, Commands.GET_DOMAIN_LIST, 
					Commands.GET_DOMAIN_STATUS, Commands.GET_ERROR_REPORT, Commands.GET_KEY_FILENAMES, 
					Commands.GET_SAML_TOKEN, Commands.IS_DOMAIN_DIFFERENT, Commands.PING_DEVICE, 
					Commands.QUIESCE_DEVICE, Commands.QUIESCE_DOMAIN, Commands.REBOOT, Commands.RESTART_DOMAIN, 
					Commands.RESTORE_DEVICE, Commands.SET_DOMAIN, Commands.SET_FILE, Commands.SET_FIRMWARE_IMAGE, 
					Commands.SET_FIRMWARE_STREAM, Commands.START_DOMAIN, Commands.STOP_DOMAIN, 
					Commands.SUBSCRIBE_TO_DEVICE, Commands.UNSUBSCRIBE_FROM_DEVICE, 
					Commands.UNQUIESCE_DEVICE, Commands.UNQUIESCE_DOMAIN};
            
            DeviceMetaInfo result = new DeviceMetaInfo(deviceName, serialNumber, currentAMPVersion,
                                                       modelType, hardwareOptions, webGUIPort, deviceType, 
                                                       firmwareLevel, featureLicenses, supportedCommands);
            logger.exiting(CLASS_NAME, METHOD_NAME, result);
            return result;
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDevMetaInfo",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseDevMetaInfo",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDevMetaInfo",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDevMetaInfo",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#reboot(com.ibm.datapower.amt.amp.DeviceContext)
     */
    public void reboot(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException, 
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "reboot"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        RebootRequestDocument requestDoc = RebootRequestDocument.Factory.newInstance();
        RebootRequestDocument.RebootRequest rebootRequest = requestDoc.addNewRebootRequest();
        
        rebootRequest.setMode(RebootMode.REBOOT);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "rebootRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending rebootRequest to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a RebootResponse object */
        try{
            RebootResponseDocument responseDoc = RebootResponseDocument.Factory.parse(responseDocXml);
            
            RebootResponseDocument.RebootResponse rebootResponse = responseDoc.getRebootResponse();
            if (rebootResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.rebootNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum rebootStatus = rebootResponse.getStatus();
            if (rebootStatus == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.rebootNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (rebootStatus.equals(Status.OK)){
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            else{
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootError",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.rebootError",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRebootResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseRebootResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumReboot",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumReboot",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getDomainList(com.ibm.datapower.amt.amp.DeviceContext)
     */
    public String[] getDomainList(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "getDomainList"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        GetDomainListRequestDocument requestDoc = 
            GetDomainListRequestDocument.Factory.newInstance();
        //GetDomainListRequestDocument.GetDomainListRequest getDomainListRequest = 
            requestDoc.addNewGetDomainListRequest();
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getDomainListRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getDomainListRequest to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a GetDomainListResponse object */
        try{
            GetDomainListResponseDocument responseDoc = 
                GetDomainListResponseDocument.Factory.parse(responseDocXml);
            GetDomainListResponseDocument.GetDomainListResponse getGetDomainListResponse = 
                responseDoc.getGetDomainListResponse();
            
            if (getGetDomainListResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomainListNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getDomainListNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            String[] result = getGetDomainListResponse.getDomainArray();
            
            logger.exiting(CLASS_NAME, METHOD_NAME, result);
            return result;
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public byte[] getDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "getDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        GetDomainExportRequestDocument requestDoc = 
            GetDomainExportRequestDocument.Factory.newInstance();
        GetDomainExportRequestDocument.GetDomainExportRequest getDomainExportRequest = 
            requestDoc.addNewGetDomainExportRequest();
        
        // set domain name
        getDomainExportRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getDomainExportRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a GetDomainExportResponse object */
        try{
            GetDomainExportResponseDocument responseDoc = 
                GetDomainExportResponseDocument.Factory.parse(responseDocXml);
            GetDomainExportResponseDocument.GetDomainExportResponse getGetDomainExportResponse = 
                responseDoc.getGetDomainExportResponse();
            if (getGetDomainExportResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomListNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getDomListNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Backup backup = getGetDomainExportResponse.getConfig();
            if (backup == null){
                //fix for cs # 59219
                Status.Enum status = getGetDomainExportResponse.getStatus();
                if (status == null){
                	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainElt",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noDomainElt",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else if (status.equals(Status.ERROR)){
                	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errorGetDom",params);
                	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errorGetDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else {
                	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec",params); //$NON-NLS-1$ //$NON-NLS-2$
                	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
            }
            else if (backup.isNil()){
            	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec",params);
            	NotExistException e = new NotExistException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            //byte[] result = backup.getByteArrayValue();
            byte[] result = backup.getStringValue().getBytes();
            logger.exiting(CLASS_NAME, METHOD_NAME, result);
            return result;
            
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInDomainResp",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInDomainResp",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setDomain(com.ibm.datapower.amt.amp.DeviceContext, byte[])
     */
    public void setDomain(DeviceContext device, String domainName, byte[] domainImage, DeploymentPolicy policy)
        throws InvalidCredentialsException,DeviceExecutionException, 
               AMPIOException, AMPException, DeletedException {
        
        final String METHOD_NAME = "setDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        SetDomainExportRequestDocument requestDoc = 
            SetDomainExportRequestDocument.Factory.newInstance();
        SetDomainExportRequestDocument.SetDomainExportRequest setDomainExportRequest = 
            requestDoc.addNewSetDomainExportRequest();

        // Backup image
        Backup image = setDomainExportRequest.addNewConfig();
        image.setStringValue(new String(domainImage));
        image.setDomain(domainName);
        
        // reset domain
        setDomainExportRequest.setResetDomain(true);
        
        // deployment policy
        DeploymentPolicyConfiguration deppol = null;
        if (policy != null){
            switch (policy.getPolicyType()){
            case EXPORT:
            	deppol = setDomainExportRequest.addNewPolicyConfiguration();
                deppol.setStringValue(new String(policy.getCachedBytes()));
                deppol.setPolicyDomainName(policy.getPolicyDomainName());
                deppol.setPolicyObjectName(policy.getPolicyObjectName());
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "setDomainExportRequest(" + domainName + ") " +
                        "deployment policy (" + policy.getPolicyType().name() + "," 
                        + policy.getPolicyDomainName() + "," 
                        + policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$ //$NON-NLS-2$
                break;
            case XML:
            	com.datapower.schemas.appliance.management.x20.DeploymentPolicy xmlpolicy;
            	com.datapower.schemas.appliance.management.x20.DeploymentPolicy.ModifiedConfig modifiedConfig;
            	
            	// Parse the policy
            	String xmlFragment = new String(policy.getCachedBytes());
            	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            	Document doc = null;
            	try{
                	DocumentBuilder db = dbf.newDocumentBuilder();
                	doc = db.parse(new InputSource(new StringReader(xmlFragment)));
            	}
            	catch (ParserConfigurationException exception){
          	    	Object[] params = new Object[] {device.getHostname()};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params); //$NON-NLS-1$ //$NON-NLS-2$
                	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;            		
            	} catch (SAXException exception) {
            		Object[] params = new Object[] {device.getHostname()};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params); //$NON-NLS-1$ //$NON-NLS-2$
                	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
				} catch (IOException exception) {
					Object[] params = new Object[] {device.getHostname()};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy",params); //$NON-NLS-1$ //$NON-NLS-2$
                	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
				}

            	// Walk the nodes and generate the policy in the request
            	Node node = (Node)doc;
            	NodeList policyNode = node.getChildNodes();
            	for (int i=0; i<policyNode.getLength(); i++){
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                            "setDomainExportRequest(" + domainName + ") " +
                            "deployment policy (" + policy.getPolicyType().name() + ":" 
                            + policyNode.item(i).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            		if (policyNode.item(i).getNodeName().equalsIgnoreCase("policy")){
            			xmlpolicy = setDomainExportRequest.addNewPolicy();
            			NodeList configNodes = policyNode.item(i).getChildNodes();
                    	for (int j=0; j<configNodes.getLength(); j++){
                    		if (configNodes.item(j).getNodeName().equalsIgnoreCase("modifiedconfig")){
                                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                                        "setDomainExportRequest(" + domainName + ") " +
                                        "deployment policy (" + policy.getPolicyType().name() + ":" 
                                        + configNodes.item(j).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                    			modifiedConfig = xmlpolicy.addNewModifiedConfig();
                    			NodeList propertyNodes = configNodes.item(j).getChildNodes();
                            	for (int k=0; k<propertyNodes.getLength(); k++){
                            		boolean logProperty = true;
                            		if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("match")){
                                		modifiedConfig.setMatch(propertyNodes.item(k).getTextContent());
                            		}
                            		else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("type")){
                                		modifiedConfig.setType(com.datapower.schemas.appliance.management.x20.PolicyType.CHANGE);
                            		}
                            		else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("property")){
                                		modifiedConfig.setProperty(propertyNodes.item(k).getTextContent());
                            		}
                            		else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("value")){
                                		modifiedConfig.setValue(propertyNodes.item(k).getTextContent());
                            		}
                            		else {
                            			logProperty = false;
                            		}

                            		// log if needed
                            		if (logProperty){
                                        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                                                "setDomainExportRequest(" + domainName + ") " +
                                                "deployment policy (" + policy.getPolicyType().name() + ":" 
                                                + propertyNodes.item(k).getNodeName() + ":"
                                                + propertyNodes.item(k).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                            		}
                            	}                    			
                    		}
                    		else if (configNodes.item(j).getNodeName().equalsIgnoreCase("acceptedconfig")){
                                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                                        "setDomainExportRequest(" + domainName + ") " +
                                        "deployment policy (" + policy.getPolicyType().name() + ":" 
                                        + configNodes.item(j).getNodeName() + ":"
                                        + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                        		xmlpolicy.addAcceptedConfig(configNodes.item(j).getTextContent());
                    		}
                    		else if (configNodes.item(j).getNodeName().equalsIgnoreCase("filteredconfig")){
                                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                                        "setDomainExportRequest(" + domainName + ") " +
                                        "deployment policy (" + policy.getPolicyType().name() + ":" 
                                        + configNodes.item(j).getNodeName() + ":"
                                        + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                        		xmlpolicy.addFilteredConfig(configNodes.item(j).getTextContent());
                    		}
                    	}
            		}
            	}
            	break;
/*            case REFERENCE:
            	setDomainExportRequest.setPolicyObjectName(policy.getPolicyObjectName());
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "setDomainExportRequest(" + domainName + ") " +
                        "deployment policy (" + policy.getPolicyType().name() + "," 
                        + policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$ //$NON-NLS-2$
            	break;*/
            default:
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "setDomainExportRequest(" + domainName + ") " +
                        "deployment policy type (" + policy.getPolicyType().name() + ")is unsupported."); //$NON-NLS-1$ //$NON-NLS-2$
            	break;        	
            }        	
        }
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "setDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending setDomainExportRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;        
        
        /* Parse the request into a SetDomainExportResponse object */
        try{
            SetDomainExportResponseDocument responseDoc = 
                SetDomainExportResponseDocument.Factory.parse(responseDocXml);
            SetDomainExportResponseDocument.SetDomainExportResponse getSetDomainExportResponse = 
                responseDoc.getSetDomainExportResponse();
            
            if (getSetDomainExportResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            // Use as a string to allow different error return codes.
            String status = getSetDomainExportResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.OK.toString())){
                logger.exiting(CLASS_NAME,METHOD_NAME);
                return;
            }
            else{
            	Object[] params = {domainName, device.getHostname(),Integer.toString(device.getAMPPort()), status};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);;
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#deleteDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public void deleteDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "deleteDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        DeleteDomainRequestDocument requestDoc = 
            DeleteDomainRequestDocument.Factory.newInstance();
        DeleteDomainRequestDocument.DeleteDomainRequest deleteDomainRequest = 
            requestDoc.addNewDeleteDomainRequest();
        
        deleteDomainRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "deleteDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending deleteDomainRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a DeleteDomainResponse object */
        try{
            DeleteDomainResponseDocument responseDoc = 
                DeleteDomainResponseDocument.Factory.parse(responseDocXml);
            DeleteDomainResponseDocument.DeleteDomainResponse getDeleteDomainResponse = 
                responseDoc.getDeleteDomainResponse();
            
            if (getDeleteDomainResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.delDomainNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.delDomainNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = getDeleteDomainResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatDelDomain",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noStatDelDomain",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            if (status.equals(Status.OK)){
                logger.exiting(CLASS_NAME,METHOD_NAME);
                return;
            }
            else{
            	Object[] params = {domainName, device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.delDomainFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.delDomainFail",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getDomainStatus(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public DomainStatus getDomainStatus(DeviceContext device, String domainName)
    throws NotExistException, InvalidCredentialsException, 
           DeviceExecutionException, AMPIOException, AMPException {
    
    final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$
    
    logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
    
    GetDomainStatusRequestDocument requestDoc = 
        GetDomainStatusRequestDocument.Factory.newInstance();
    GetDomainStatusRequestDocument.GetDomainStatusRequest getDomainStatusRequest = 
        requestDoc.addNewGetDomainStatusRequest();
    
    getDomainStatusRequest.setDomain(domainName);
    
    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                "getDomainStatusRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
    
    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                "Sending getDomainStatusRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
    
    // Send request to device 
    StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
    Node responseDocXml = soapHelper.call(device, outMessage); 
    
    outMessage.delete(0,outMessage.length());
    outMessage = null;
    requestDoc.setNil();
    requestDoc = null;
    
    // Parse the request into a GetDomainStatusResponse object 
    try{
        GetDomainStatusResponseDocument responseDoc = 
            GetDomainStatusResponseDocument.Factory.parse(responseDocXml);
        GetDomainStatusResponseDocument.GetDomainStatusResponse getGetDomainStatusResponse = 
            responseDoc.getGetDomainStatusResponse();
        
        if (getGetDomainStatusResponse == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomNoResp",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getDomNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$            	
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        GetDomainStatusResponseDocument.GetDomainStatusResponse.Domain domain = 
            getGetDomainStatusResponse.getDomain();
        
        if (domain == null){
            // see cs tracker # 59517 for more explanation about the following code
            
            // if domain isn't here, then we can't have an OpState!
            // therefore, if we get here, and there is no status (or there is one,
            // and its == 'ok'), then throw an exception as this is a REALLY weird case.
            
            // in dp build 137835+, this might return a <status>error</status> to represent
            //   a non existant domain 
            
            Status.Enum status = getGetDomainStatusResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getDomNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            else if (status.equals(Status.ERROR)){
            	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet",params);
            	NotExistException ex = new NotExistException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, ex);
                throw ex;
            }
            else {
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainSpec",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noDomainSpec",params); //$NON-NLS-1$ //$NON-NLS-2$
            	logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        } 
           
        // domain object exists, which means we either have a good message, or a <=3.6.0.3 
        // firmware level
        
        // workaround below for bug in 3.6.0.3 (dp build 137758) 
        OpState.Enum opState = null;
        try{
            opState = domain.getOpState();
        }
        catch (XmlValueOutOfRangeException e){
            // a null opState == non existant domain
        	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet",params);
        	NotExistException ex = new NotExistException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        
        if (opState == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        OperationStatus opStatus;

        if (opState.toString().equalsIgnoreCase(OperationStatus.Enumerated.UP.toString()))
            opStatus = new OperationStatus(OperationStatus.Enumerated.UP);
        else if (opState.toString().equalsIgnoreCase(OperationStatus.Enumerated.DOWN.toString()))
            opStatus = new OperationStatus(OperationStatus.Enumerated.DOWN);
        else if (opState.toString().equalsIgnoreCase(OperationStatus.Enumerated.PARTIAL.toString()))
            opStatus = new OperationStatus(OperationStatus.Enumerated.PARTIAL);
        else if (opState.toString().equalsIgnoreCase(OperationStatus.Enumerated.UNKNOWN.toString()))
            opStatus = new OperationStatus(OperationStatus.Enumerated.UNKNOWN);
        else{
        	Object[] params = {opState.toString(),device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        ConfigState.Enum configState = domain.getConfigState();
        if (configState == null){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        boolean needsSave;
        if (configState.toString().equalsIgnoreCase("modified")) //$NON-NLS-1$
            needsSave = true;
        else if (configState.toString().equalsIgnoreCase("saved")) //$NON-NLS-1$
            needsSave = false;
        else {
        	Object[] params = {configState.toString(),device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        boolean debugState;
        if (domain.getDebugState() == true)
            debugState = true;
        else if (domain.getDebugState() == false)
            debugState = false;
        else {
        	Object[] params = {Boolean.valueOf(domain.getDebugState()),device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidDebugStat",params);
            AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.invalidDebugStat",params); //$NON-NLS-1$ //$NON-NLS-2$
        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        String quiesceState = domain.getQuiesceState();
        QuiesceStatus quiesceStatus = QuiesceStatus.UNKNOWN;  // default            
        if (quiesceState.equals("")) quiesceStatus = QuiesceStatus.NORMAL;
        else if (quiesceState.equals("quiescing")) quiesceStatus = QuiesceStatus.QUIESCE_IN_PROGRESS;
        else if (quiesceState.equals("quiesced")) quiesceStatus = QuiesceStatus.QUIESCED;
        else if (quiesceState.equals("unquiescing")) quiesceStatus = QuiesceStatus.UNQUIESCE_IN_PROGRESS;
        else if (quiesceState.equals("error")) quiesceStatus = QuiesceStatus.ERROR;
                         
        DomainStatus result = new DomainStatus(opStatus,needsSave,debugState,quiesceStatus);
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return result;
    }
    catch (XmlException e){
    	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParsingGetDomStat",params);
        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParsingGetDomStat",params); //$NON-NLS-1$ //$NON-NLS-2$
    	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
        throw ex;
    }
    catch (XmlValueOutOfRangeException e){
    	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomStat",params);
        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomStat",params); //$NON-NLS-1$ //$NON-NLS-2$
    	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
        throw ex;
    }
}
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#startDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public void startDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "startDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        StartDomainRequestDocument requestDoc = 
            StartDomainRequestDocument.Factory.newInstance();
        StartDomainRequestDocument.StartDomainRequest startDomainRequest = 
            requestDoc.addNewStartDomainRequest();
        
        startDomainRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "startDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending startDomainRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a StartDomainResponse object */
        try{
            
            StartDomainResponseDocument responseDoc = 
                StartDomainResponseDocument.Factory.parse(responseDocXml);
            
            StartDomainResponseDocument.StartDomainResponse startDomainResponse = 
                responseDoc.getStartDomainResponse();
            
            if (startDomainResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.startDomNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.startDomNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = startDomainResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.startDomNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.startDomNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.OK)){
                logger.exiting(CLASS_NAME,METHOD_NAME);
                return;
            }
            else {
            	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errStartDom",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errStartDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseStartDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseStartDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInStartDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInStartDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#stopDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public void stopDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "stopDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        StopDomainRequestDocument requestDoc = 
            StopDomainRequestDocument.Factory.newInstance();
        StopDomainRequestDocument.StopDomainRequest stopDomainRequest = 
            requestDoc.addNewStopDomainRequest();
        
        stopDomainRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "stopDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending stopDomainRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a StopDomainResponse object */
        try{
            StopDomainResponseDocument responseDoc = 
                StopDomainResponseDocument.Factory.parse(responseDocXml);
            
            StopDomainResponseDocument.StopDomainResponse stopDomainResponse = responseDoc.getStopDomainResponse();
            
            if (stopDomainResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = stopDomainResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.OK)){
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            else {
            	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errStopDom",params);
                DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errStopDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseStopDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseStopDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumStopDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumStopDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#restartDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public void restartDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "restartDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        RestartDomainRequestDocument requestDoc = 
            RestartDomainRequestDocument.Factory.newInstance();
        RestartDomainRequestDocument.RestartDomainRequest restartDomainRequest = 
            requestDoc.addNewRestartDomainRequest();
        
        restartDomainRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "restartDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending restartDomainRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a RestartDomainResponse object */
        try{
            
            RestartDomainResponseDocument responseDoc = 
                RestartDomainResponseDocument.Factory.parse(responseDocXml);
            
            RestartDomainResponseDocument.RestartDomainResponse restartDomainResponse = 
                responseDoc.getRestartDomainResponse();
            
            if (restartDomainResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restartDomNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.restartDomNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = restartDomainResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatRestartDom",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.noStatRestartDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.OK)){
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            else {
            	Object[] params = {domainName,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errRestartingDom",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.errRestartingDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRestDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseRestDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumRestartDom",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumRestartDom",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setFirmware(com.ibm.datapower.amt.amp.DeviceContext, byte[], boolean)
     */
    public void setFirmware(DeviceContext device, byte[] firmwareImage) 
        throws InvalidCredentialsException,DeviceExecutionException, 
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "setFirmware(byte[]...)"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        SetFirmwareRequestDocument requestDoc = 
            SetFirmwareRequestDocument.Factory.newInstance();
        SetFirmwareRequestDocument.SetFirmwareRequest setFirmwareRequest = 
            requestDoc.addNewSetFirmwareRequest();
        
        Firmware image = setFirmwareRequest.addNewFirmware();
                
        image.setByteArrayValue(firmwareImage);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "setFirmwareRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending setFirmwareRequest to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a SetFirmwareResponse object */
        try{
            SetFirmwareResponseDocument responseDoc = 
                SetFirmwareResponseDocument.Factory.parse(responseDocXml);
            SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = 
                responseDoc.getSetFirmwareResponse();
            
            if (getSetFirmwareResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = getSetFirmwareResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else {
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setFirmware(com.ibm.datapower.amt.amp.DeviceContext, java.io.InputStream, boolean)
     */
    public void setFirmware(DeviceContext device, InputStream inputStream) 
        throws InvalidCredentialsException,DeviceExecutionException, 
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "setFirmware(InputStream...)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, inputStream});
       
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFirmwareRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending setFirmwareRequest to device "+ device.getHostname() +  //$NON-NLS-1$
                    ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        Node responseDocXml = soapHelper.call(device, setFirmwareHeaderBytes, setFirmwareFooterBytes, 
                inputStream); 
        
        /* Parse the request into a SetFirmwareResponse object */
        try{
            SetFirmwareResponseDocument responseDoc = 
                SetFirmwareResponseDocument.Factory.parse(responseDocXml);
            SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = 
                responseDoc.getSetFirmwareResponse();
            
            if (getSetFirmwareResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = getSetFirmwareResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else {
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getKeyFilenames(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public String[] getKeyFilenames(DeviceContext device, String domainName)
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "getKeyFilenames"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[]{device, domainName});
        
        GetCryptoArtifactsRequestDocument requestDoc = 
            GetCryptoArtifactsRequestDocument.Factory.newInstance();
        GetCryptoArtifactsRequestDocument.GetCryptoArtifactsRequest getCryptoArtifactsRequest =
            requestDoc.addNewGetCryptoArtifactsRequest();
        
        getCryptoArtifactsRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getCryptoArtifactsRequest created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getCryptoArtifactsRequest(" + domainName + ") to device "  //$NON-NLS-1$ //$NON-NLS-2$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a GetDomainListResponse object */
        try{
            GetCryptoArtifactsResponseDocument responseDoc = 
                GetCryptoArtifactsResponseDocument.Factory.parse(responseDocXml);
            GetCryptoArtifactsResponseDocument.GetCryptoArtifactsResponse getGetCryptoArtifactsResponse = 
                responseDoc.getGetCryptoArtifactsResponse();
            
            if (getGetCryptoArtifactsResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            GetCryptoArtifactsResponseDocument.GetCryptoArtifactsResponse.CryptoArtifacts artifacts = 
                getGetCryptoArtifactsResponse.getCryptoArtifacts();
            
            if (artifacts == null){
                Status.Enum status = getGetCryptoArtifactsResponse.getStatus();
                if (status == null){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoStat",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else if (status.equals(Status.ERROR)){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNFail",params);
                	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else {
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidResp",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
            }
            
            String domain = artifacts.getDomain();
            
            if (domain == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoDom",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoDom",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (!domain.equalsIgnoreCase(domainName)){
            	Object[] params = {domain,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.diffDomain",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.diffDomain",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            CryptoFileName[] cryptoArray = artifacts.getCryptoFileNameArray();
            
            if ((cryptoArray == null) || (cryptoArray.length == 0))
                return null;
            
            String[] result = new String[cryptoArray.length];
            for (int i = 0; i < result.length; i++)
                result[i] = cryptoArray[i].getStringValue();
            
            logger.exiting(CLASS_NAME, METHOD_NAME, result);
            return result;
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseGetKeyFN",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseGetKeyFN",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidEnum",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setFile(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, byte[])
     */
    public void setFile(DeviceContext device, String domainName,
                        String filenameOnDevice, byte[] contents) throws NotExistException,
                        InvalidCredentialsException, DeviceExecutionException, 
                        AMPIOException, AMPException {
        
        final String METHOD_NAME = "setFile"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME); 
        
        SetFileRequestDocument requestDoc = SetFileRequestDocument.Factory.newInstance();
        SetFileRequestDocument.SetFileRequest setFileRequest = requestDoc.addNewSetFileRequest();
        
        try{
	        /*File file = setFileRequest.addNewFile();
	        file.setByteArrayValue(contents);
	        //file.setStringValue(new String(contents));
	        file.setDomain(domainName);
	        file.setLocation(filenameOnDevice);*/
	        
	        File file = setFileRequest.addNewFile();
			file.setByteArrayValue(contents);
			//file.setStringValue(new String(contents));
			file.setDomain(domainName);
			
			// defect 12668 java.lang.OutOfMemoryError: Java heap space - Very large file upload
			// Check memory size		
			Utils.checkMemorySize(contents, METHOD_NAME);
			
			// find location
			int iLoc = filenameOnDevice.lastIndexOf("/");
			String sLocation = filenameOnDevice.substring(0, iLoc+1);		
			file.setLocation(sLocation);
			// get file name
			String sFileName = filenameOnDevice.substring(iLoc+1);
			file.setName(sFileName);
	        
			// Check memory size		
			Utils.checkMemorySize(contents, METHOD_NAME);
			
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
	                    "setFileRequest created"); //$NON-NLS-1$
	        
	        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
	                    "Sending setFileRequest(" + domainName + ", " + filenameOnDevice +") to device "  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	        
	        /* Send request to device */
	        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
	        Node responseDocXml = soapHelper.call(device, outMessage); 
	        
	        outMessage.delete(0,outMessage.length());
	        outMessage = null;
	        requestDoc.setNil();
	        requestDoc = null;
        
        /* Parse the request into a SetFileResponse object */
        
            SetFileResponseDocument responseDoc = 
                SetFileResponseDocument.Factory.parse(responseDocXml);
            SetFileResponseDocument.SetFileResponse setFileResponse = 
                responseDoc.getSetFileResponse();
            
            if (setFileResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFileNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = setFileResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFileNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (status.equals(Status.OK)){
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return;
            }
            else{
            	Object[] params = {filenameOnDevice,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setFileFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRespSetFile",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.errParseRespSetFile",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFile",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFile",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (Error o ) {
        	Object[] params = { };
        	String message = Messages.getString("wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
            DeviceExecutionException e = new DeviceExecutionException(message, "wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
            throw e; 
        }
    }
    
//    /* (non-Javadoc)
//     * @see com.ibm.datapower.amt.amp.Commands#getClonableDeviceSettings(com.ibm.datapower.amt.amp.DeviceContext)
//     */
//    public byte[] getClonableDeviceSettings(DeviceContext device)
//        throws InvalidCredentialsException, DeviceExecutionException,
//               AMPIOException, AMPException {
//        
//        final String METHOD_NAME = "getClonableDeviceSettings"; //$NON-NLS-1$
//        
//        logger.entering(CLASS_NAME, METHOD_NAME, device);
//        
//        GetDeviceSettingsRequestDocument requestDoc =
//            GetDeviceSettingsRequestDocument.Factory.newInstance();
//        //GetDeviceSettingsRequestDocument.GetDeviceSettingsRequest getDeviceSettingsRequest = 
//            requestDoc.addNewGetDeviceSettingsRequest();
//        
//        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
//                    "getDeviceSettingsRequest created"); //$NON-NLS-1$
//        
//        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
//                    "Sending getDeviceSettingsRequest to device "  //$NON-NLS-1$
//                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
//        
//        /* Send request to device */
//        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
//        Node responseDocXml = soapHelper.call(device, outMessage); 
//        
//        outMessage.delete(0,outMessage.length());
//        outMessage = null;
//        requestDoc.setNil();
//        requestDoc = null;
//        
//        /* Parse the request into a GetDeviceSettingsResponse object */
//        try{
//            GetDeviceSettingsResponseDocument responseDoc = 
//                GetDeviceSettingsResponseDocument.Factory.parse(responseDocXml);
//            GetDeviceSettingsResponseDocument.GetDeviceSettingsResponse getGetDeviceSettingsResponse = 
//                responseDoc.getGetDeviceSettingsResponse();
//            
//            if (getGetDeviceSettingsResponse == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettNoResp",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getSettNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            Export settings = getGetDeviceSettingsResponse.getSettings();
//            if (settings == null){
//                // lets try to get the error status
//                Status.Enum status = getGetDeviceSettingsResponse.getStatus();
//                if (status == null){
//                	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettNoStat",params);
//                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getSettNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
//                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                    throw e;
//                }
//                
//                else if (status.equals(Status.ERROR)){
//                	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettFail",params);
//                	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getSettFail",params); //$NON-NLS-1$ //$NON-NLS-2$
//                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                    throw e;
//                }
//                else {
//                	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettNoSett",params);
//                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getSettNoSett",params); //$NON-NLS-1$ //$NON-NLS-2$
//                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                    throw e;
//                }
//            }
//            
//            //byte[] result = settings.getByteArrayValue();
//            byte[] result = settings.getStringValue().getBytes();
//            logger.exiting(CLASS_NAME, METHOD_NAME);
//            return result;
//        }
//        catch (XmlException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettErrParse",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getSettErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//            throw ex;
//        }
//        catch (XmlValueOutOfRangeException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getSettInvalidEnum",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getSettInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//            throw ex;
//        }
//    }
//    
//    /* (non-Javadoc)
//     * @see com.ibm.datapower.amt.amp.Commands#setClonableDeviceSettings(com.ibm.datapower.amt.amp.DeviceContext, byte[])
//     */
//    public void setClonableDeviceSettings(DeviceContext device, byte[] settingsImage) 
//    		throws InvalidCredentialsException, DeviceExecutionException, 
//    		AMPIOException, AMPException {
//        
//        final String METHOD_NAME = "setClonableDeviceSettings"; //$NON-NLS-1$
//        
//        logger.entering(CLASS_NAME, METHOD_NAME);//, new Object[]{device, settingsImage});
//        
//        SetDeviceSettingsRequestDocument requestDoc =
//            SetDeviceSettingsRequestDocument.Factory.newInstance();
//        SetDeviceSettingsRequestDocument.SetDeviceSettingsRequest setDeviceSettingsRequest = 
//            requestDoc.addNewSetDeviceSettingsRequest();
//        
//        Export export = setDeviceSettingsRequest.addNewSettings();
//        //export.setByteArrayValue(settingsImage);
//        export.setStringValue(new String(settingsImage));
//        
//        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
//                    "setDeviceSettingsRequest created"); //$NON-NLS-1$
//        
//        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
//                    "Sending setDeviceSettingsRequest to device "  //$NON-NLS-1$
//                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
//        
//        /* Send request to device */
//        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
//        Node responseDocXml = soapHelper.call(device, outMessage); 
//        
//        outMessage.delete(0,outMessage.length());
//        outMessage = null;
//        requestDoc.setNil();
//        requestDoc = null;
//        
//        /* Parse the request into a SetDeviceSettingsResponse object */
//        try{
//            SetDeviceSettingsResponseDocument responseDoc = 
//                SetDeviceSettingsResponseDocument.Factory.parse(responseDocXml);
//            SetDeviceSettingsResponseDocument.SetDeviceSettingsResponse getSetDeviceSettingsResponse = 
//                responseDoc.getSetDeviceSettingsResponse();
//            
//            if (getSetDeviceSettingsResponse == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setSettNoResp",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setSettNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            Status.Enum status = getSetDeviceSettingsResponse.getStatus();
//            if (status == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setSettNoStat",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setSettNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            if (status.equals(Status.OK)){
//                logger.exiting(CLASS_NAME, METHOD_NAME);
//                return;
//            }
//            else{
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setSettFail",params);
//            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.setSettFail",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//        }
//        catch (XmlException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setSettErrParse",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.setSettErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//            throw ex;
//        }
//        catch (XmlValueOutOfRangeException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setSettInvalidEnum",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.setSettInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//            throw ex;
//        }
//    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getErrorReport(com.ibm.datapower.amt.amp.DeviceContext)
     */
    public ErrorReport getErrorReport(DeviceContext device) 
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "getErrorReport"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        GetErrorReportRequestDocument requestDoc = 
            GetErrorReportRequestDocument.Factory.newInstance();
        //GetErrorReportRequestDocument.GetErrorReportRequest getErrorReportRequest =
            requestDoc.addNewGetErrorReportRequest();
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getErrorReport created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getErrorReport to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a GetErrorReportResponse object */
        try{
            GetErrorReportResponseDocument responseDoc = 
                GetErrorReportResponseDocument.Factory.parse(responseDocXml);
            GetErrorReportResponseDocument.GetErrorReportResponse getGetErrorReportResponse = 
                responseDoc.getGetErrorReportResponse();
            
            if (getGetErrorReportResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            File file = getGetErrorReportResponse.getErrorReport();
            if (file == null){
                // lets check for the error status
                Status.Enum status = getGetErrorReportResponse.getStatus();
                if (status == null){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoStat",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else if (status.equals(Status.ERROR)){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepFail",params);
                	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else {
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepEmpty",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepEmpty",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
            }
            
            //byte[] stringVal = file.getByteArrayValue();
            byte[] stringVal = null;
            if (file.getStringValue() != null){
                stringVal = file.getStringValue().getBytes();
            }
            
            ErrorReport result = new ErrorReport(file.getDomain(), file.getLocation(), file.getName(), stringVal);
            logger.exiting(CLASS_NAME, METHOD_NAME, result);
            return result;
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepParseErr",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepParseErr",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepInvalidEnum",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getErrRepInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getErrorReport(com.ibm.datapower.amt.amp.DeviceContext)
     */
    public String getSAMLToken(DeviceContext device, String domainName) 
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException {
        
        final String METHOD_NAME = "getSAMLToken"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        GetTokenRequestDocument requestDoc = GetTokenRequestDocument.Factory.newInstance();
        GetTokenRequestDocument.GetTokenRequest getTokenRequest = requestDoc.addNewGetTokenRequest();
        
        // we only support SAML tokens for login to the webGUI currently
        
        getTokenRequest.setType(TokenType.LOGIN_WEB_MGMT);
        getTokenRequest.setUser(device.getUserId());
        getTokenRequest.setPassword(device.getPassword());
        getTokenRequest.setDomain(domainName);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "getToken created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending getToken to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a GetErrorReportResponse object */
        try{
            GetTokenResponseDocument responseDoc = GetTokenResponseDocument.Factory.parse(responseDocXml);
            GetTokenResponseDocument.GetTokenResponse getGetTokenResponse = 
                responseDoc.getGetTokenResponse();
            
            if (getGetTokenResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            String samlToken = getGetTokenResponse.getToken();
            
            if (samlToken == null){
                Status.Enum status = getGetTokenResponse.getStatus();
                if (status == null){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoStat",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else if (status.equals(Status.ERROR)){
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenFail",params);
                	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
                else {
                	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenOkStat",params);
                    AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenOkStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
            }
            
            return samlToken;
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenErrParse",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenInvalidEnum",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.getTokenInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#isDifferent(byte[], byte[], com.ibm.datapower.amt.amp.DeviceContext)
     */
    public boolean isDomainDifferent(String domainName, byte[] configImage1, 
                                     byte[] configImage2, DeviceContext device) throws InvalidCredentialsException,
                                     DeviceExecutionException, AMPIOException, AMPException {
        
        final String METHOD_NAME = "isDomainDifferent"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME); 
                        //new Object[]{domainName, configImage1, configImage2, device});
        
        CompareConfigRequestDocument requestDoc = 
            CompareConfigRequestDocument.Factory.newInstance();
        CompareConfigRequestDocument.CompareConfigRequest compareConfigRequest = 
            requestDoc.addNewCompareConfigRequest();
        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig compareConfig = 
            compareConfigRequest.addNewCompareConfig();
        
        compareConfig.setDomain(domainName);
        
        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.From from = 
            compareConfig.addNewFrom();
        Backup fromImage = from.addNewConfig();
        
        //fromImage.setByteArrayValue(configImage1);
        fromImage.setStringValue(new String(configImage1));
        fromImage.setDomain(domainName);
        
        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.To to = 
            compareConfig.addNewTo();
        if (configImage2 == null)
            to.addNewPersisted();
        else{
            Backup toImage = to.addNewConfig();
            //toImage.setByteArrayValue(configImage2);
            toImage.setStringValue(new String(configImage2));
            toImage.setDomain(domainName);
        }
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "isDomainDifferent created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Sending isDomainDifferent to device "  //$NON-NLS-1$
                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
        
        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 
        
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
        
        /* Parse the request into a SetDeviceSettingsResponse object */
        try{
            CompareConfigResponseDocument responseDoc = 
                CompareConfigResponseDocument.Factory.parse(responseDocXml);
            CompareConfigResponseDocument.CompareConfigResponse getCompareConfigResponse = 
                responseDoc.getCompareConfigResponse();
            
            if (getCompareConfigResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            CompareConfigResponseDocument.CompareConfigResponse.CompareConfig compareConfigResponse = getCompareConfigResponse.getCompareConfig();
            if (compareConfigResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompConf",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompConf",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            String returnedDomain = compareConfigResponse.getDomain();
            if (returnedDomain == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameNull",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameNull",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;   
            }
            if (!returnedDomain.equalsIgnoreCase(domainName)){
            	Object[] params = {domainName, device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameRec",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameRec",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            CompareResult.Enum compareResult = compareConfigResponse.getCompareResult();
            if (compareResult == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompRes",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompRes",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            if (compareResult.toString().equalsIgnoreCase("identical")){ //$NON-NLS-1$
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return false;
            }
            else if (compareResult.toString().equalsIgnoreCase("different")){ //$NON-NLS-1$
                logger.exiting(CLASS_NAME, METHOD_NAME);
                return true;
            }
            else {
            	Object[] params = {compareResult,device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidRes",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidRes",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffErrParse",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        catch (XmlValueOutOfRangeException e){
        	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidEnum",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
//    /* (non-Javadoc)
//     * @see com.ibm.datapower.amt.amp.Commands#isDifferent(byte[], byte[], com.ibm.datapower.amt.amp.DeviceContext)
//     */
//    public boolean isSettingsDifferent(byte[] configImage1, byte[] configImage2,
//                                       DeviceContext device) throws InvalidCredentialsException,
//                                       DeviceExecutionException, AMPIOException, AMPException {
//        
//        final String METHOD_NAME = "isSettingsDifferent"; //$NON-NLS-1$
//        
//        logger.entering(CLASS_NAME, METHOD_NAME); 
//                        //new Object[]{configImage1, configImage2, device});
//        
//        CompareConfigRequestDocument requestDoc = 
//            CompareConfigRequestDocument.Factory.newInstance();
//        CompareConfigRequestDocument.CompareConfigRequest compareConfigRequest = 
//            requestDoc.addNewCompareConfigRequest();
//        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig compareConfig = 
//            compareConfigRequest.addNewCompareConfig();
//        
//        compareConfig.setDomain("default"); //$NON-NLS-1$
//        
//        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.From from = 
//            compareConfig.addNewFrom();
//        Export fromImage = from.addNewSettings();
//        //fromImage.setByteArrayValue(configImage1);
//        fromImage.setStringValue(new String(configImage1));
//        
//        CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.To to = 
//            compareConfig.addNewTo();
//        if (configImage2 == null)
//            to.addNewPersisted();
//        else{
//            Export toImage = to.addNewSettings();
//            //toImage.setByteArrayValue(configImage2);
//            toImage.setStringValue(new String(configImage2));
//        }
//        
//        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
//                    "isSettingsDifferent created"); //$NON-NLS-1$
//        
//        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
//                    "Sending isSettingsDifferent to device "  //$NON-NLS-1$
//                    + device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
//        
//        /* Send request to device */
//        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
//        Node responseDocXml = soapHelper.call(device, outMessage); 
//        
//        outMessage.delete(0,outMessage.length());
//        outMessage = null;
//        requestDoc.setNil();
//        requestDoc = null;
//        
//        /* Parse the request into a SetDeviceSettingsResponse object */
//        try{
//            CompareConfigResponseDocument responseDoc = 
//                CompareConfigResponseDocument.Factory.parse(responseDocXml);
//            CompareConfigResponseDocument.CompareConfigResponse getCompareConfigResponse = 
//                responseDoc.getCompareConfigResponse();
//            
//            if (getCompareConfigResponse == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoResp",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            CompareConfigResponseDocument.CompareConfigResponse.CompareConfig compareConfigResponse = getCompareConfigResponse.getCompareConfig();
//            if (compareConfigResponse == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoCompConf",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoCompConf",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            CompareResult.Enum compareResult = compareConfigResponse.getCompareResult();
//            if (compareConfigResponse == null){
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoCompRes",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffNoCompRes",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//            
//            if (compareResult.toString().equalsIgnoreCase("identical")){ //$NON-NLS-1$
//                logger.exiting(CLASS_NAME, METHOD_NAME);
//                return false;
//            }
//            else if (compareResult.toString().equalsIgnoreCase("different")){ //$NON-NLS-1$
//                logger.exiting(CLASS_NAME, METHOD_NAME);
//                return true;
//            }
//            else {
//            	Object[] params = {compareResult,device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffInvalidCompRes",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffInvalidCompRes",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
//        }
//        catch (XmlException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffErrParse",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//            throw ex;
//        }
//        catch (XmlValueOutOfRangeException e){
//        	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//        	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.settDiffInvalidEnum",params);
//            AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.settDiffInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
//            logger.throwing(CLASS_NAME, METHOD_NAME, e);
//            throw ex;
//        }
//    }
    
    /* (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getDifferences(byte[], byte[], com.ibm.datapower.amt.amp.DeviceContext)
     */
    
    
    public URL getDomainDifferences(String domainName, byte[] configImage1, 
                                    byte[] configImage2, DeviceContext device) throws InvalidCredentialsException,
                                    DeviceExecutionException, AMPIOException, AMPException {
     
        final String METHOD_NAME = "getDomainDifferences"; //$NON-NLS-1$
    
        logger.entering(CLASS_NAME, METHOD_NAME); 
                        //new Object[]{domainName, configImage1, configImage2, device});
            
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getDomainDifferences is not implemented!!!"); //$NON-NLS-1$
         
        /*
     
          CompareConfigRequestDocument requestDoc = 
          CompareConfigRequestDocument.Factory.newInstance();
          CompareConfigRequestDocument.CompareConfigRequest compareConfigRequest = 
          requestDoc.addNewCompareConfigRequest();
          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig compareConfig = 
          compareConfigRequest.addNewCompareConfig();
     
          compareConfig.setDomain(domainName);
     
          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.From from = 
          compareConfig.addNewFrom();
          Backup fromImage = from.addNewConfig();
          fromImage.setByteArrayValue(configImage1);
          fromImage.setDomain(domainName);
     
          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.To to = 
          compareConfig.addNewTo();
          if (configImage2 == null)
          to.addNewPersisted();
          else{
          Backup toImage = to.addNewConfig();
          toImage.setByteArrayValue(configImage2);
          toImage.setDomain(domainName);
          }
     
          logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
          "getDomainDifferences created");
     
          logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
          "Sending getDomainDifferences to device " 
          + device.getHostname() + ":" + device.getAMPPort());
     StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
     Node responseDocXml = soapHelper.call(device, outMessage); 
     
        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;
          //TODO: implement response for getDomainDifferences
          /* AMPException ex = new AMPException("Error parsing setClonableDeviceSettings response from device " + device.getHostname() + ":" + device.getAMPPort(),e);
             logger.throwing(CLASS_NAME, METHOD_NAME, ex);
             throw ex;
             catch (XmlValueOutOfRangeException e){
             AMPException ex = new AMPException("Invalid enumeration in getClonableDeviceSettingsResponse from device " + device.getHostname() + ":" + device.getAMPPort(),e);
             logger.throwing(CLASS_NAME, METHOD_NAME, ex);
             throw ex;
             }
        */
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return null;
    }
      
//    /* (non-Javadoc)
//     * @see com.ibm.datapower.amt.amp.Commands#getDifferences(byte[], byte[], com.ibm.datapower.amt.amp.DeviceContext)
//     */
//    
//    public URL getSettingsDifferences(byte[] configImage1, byte[] configImage2,
//                                      DeviceContext device) throws InvalidCredentialsException,
//                                      DeviceExecutionException, AMPIOException, AMPException {
//     
//        final String METHOD_NAME = "getSettingsDifferences"; //$NON-NLS-1$
//     
//        logger.entering(CLASS_NAME, METHOD_NAME);//, new Object[]{configImage1, configImage2, device});
//    
//        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getSettingsDifferences is not implemented!!!"); //$NON-NLS-1$
//        /*
//          CompareConfigRequestDocument requestDoc = 
//          CompareConfigRequestDocument.Factory.newInstance();
//          CompareConfigRequestDocument.CompareConfigRequest compareConfigRequest = 
//          requestDoc.addNewCompareConfigRequest();
//          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig compareConfig = 
//          compareConfigRequest.addNewCompareConfig();
//     
//          compareConfig.setDomain("default");
//     
//          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.From from = 
//          compareConfig.addNewFrom();
//          Export fromImage = from.addNewSettings();
//          fromImage.setByteArrayValue(configImage1);
//     
//          CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.To to = 
//          compareConfig.addNewTo();
//          if (configImage2 == null)
//          to.addNewPersisted();
//          else{
//          Export toImage = to.addNewSettings();
//          toImage.setByteArrayValue(configImage2);
//          }
//     
//          logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
//          "getSettingsDifferences created");
//     
//          logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
//          "Sending getSettingsDifferences to device " 
//          + device.getHostname() + ":" + device.getAMPPort());
//     StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
//     Node responseDocXml = soapHelper.call(device, outMessage); 
//     
//        outMessage.delete(0,outMessage.length());
//        outMessage = null;
//        requestDoc.setNil();
//        requestDoc = null;
//        
//          //TODO: implement response for getSettingsDifferences
//          * AMPException ex = new AMPException("Error parsing setClonableDeviceSettings response from device " + device.getHostname() + ":" + device.getAMPPort(),e);
//          logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//          throw ex;
//          catch (XmlValueOutOfRangeException e){
//          AMPException ex = new AMPException("Invalid enumeration in getClonableDeviceSettingsResponse from device " + device.getHostname() + ":" + device.getAMPPort(),e);
//          logger.throwing(CLASS_NAME, METHOD_NAME, ex);
//          throw ex;
//          }
//        */
//        logger.exiting(CLASS_NAME, METHOD_NAME);
//        return null;
//    }
    
    /*
     * @throws InvalidParameterException 
     * 
     */    
    public Hashtable backupDevice(DeviceContext device, String cryptoCertificateName, byte[] cryptoImage, String secureBackupDestination,
    		boolean includeISCSI, boolean includeRaid) throws AMPIOException, InvalidCredentialsException, AMPException
    { 
    	final String METHOD_NAME = "backupDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);    
        
        
		// The crypto object name or the crypto certificate location must be specified,both cannot be specified
		if ((cryptoCertificateName != null)&& (cryptoImage !=  null)){
			logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME,
					"Specify the Crypto Object name or the crypto certificates location. Both may not be non-null" );
		}  
		// The crypto object name or the crypto certificate location must be specified,both cannot be null
		if ((cryptoCertificateName == null)&& (cryptoImage ==  null)){
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
					"Specify the Crypto Object name or the crypto certificates location. Both may not be null" );
		}		
        
        List<SecureBackupFile> returnedBackupFilesList = null;
        Hashtable<String,byte[]> backupHashTable = new Hashtable<String,byte[]> ();
        SecureBackupRequestDocument requestDoc = 
        	SecureBackupRequestDocument.Factory.newInstance();
        
        SecureBackupRequestDocument.SecureBackupRequest request = 
        	requestDoc.addNewSecureBackupRequest();
       
        // Remove option to backup ISCSI, if caller specifies a value of false.  It is ON by default.
        if(!includeISCSI){
        	XmlObject  scsi =request.addNewDoNotIncludeiSCSI();
        }
        // Remove option to backup RAID, if caller specifies a value of false.  It is ON by default.       
        if(!includeRaid){
        	XmlObject  raid = request.addNewDoNotIncludeRAID();
        }
        
        // Device.backupDevice() ensures that only one of the 2 is specified - cryptoObjectName or cryptoCertificate.
        // Both will not be null
        if(cryptoCertificateName !=null){
          request.setCryptoCertificateName(cryptoCertificateName);  
        }        
     
        
        if (cryptoImage!=null ){
          request.setCryptoCertificate(cryptoImage); 
        }	
         
        if (secureBackupDestination!=null){ 
           request.setSecureBackupDestination(secureBackupDestination);
        }
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
    	"backupRequest request created"); //$NON-NLS-1$
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		"Sending backup request to device "  //$NON-NLS-1$
        		+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$  
        
        /* Send request to device */

        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        
        Node responseDocXml = soapHelper.call(device, outMessage); 

        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;        
        
        /* Parse the request into a response object */
        try{
        	SecureBackupResponseDocument responseDoc = SecureBackupResponseDocument.Factory.parse(responseDocXml);
        	SecureBackupResponseDocument.SecureBackupResponse backupResponse = 
                responseDoc.getSecureBackupResponse();
             
            if (backupResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.backupNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Status.Enum status = backupResponse.getStatus();
            SecureBackup backup = backupResponse.getSecureBackup();                                                                   
            
            // By design when files are returned by backup, the status is NULL
            if (secureBackupDestination != null){
            	if (status == null ) {
            		Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            		String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupNoStat",params);
            		AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.backupNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
            		logger.throwing(CLASS_NAME, METHOD_NAME, e);
            		throw e;
            	}
            	else if (status.equals(Status.ERROR)){
            		Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            		String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupFail",params);
            		DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.backupFail",params); //$NON-NLS-1$ //$NON-NLS-2$
            		logger.throwing(CLASS_NAME, METHOD_NAME, e);
            		throw e;
            	}
            	
            }
            
            //Retrieve backup files when secureBackupDestination specified is null.  
            //Backup files are expected to be returned when secureBackupDestination is null
            if (secureBackupDestination == null) {
            	if (backup == null){
            		// backup files not returned as expected!
            		Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            		String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupFail",params);
            		DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.backupFail",params); //$NON-NLS-1$ //$NON-NLS-2$
            		logger.throwing(CLASS_NAME, METHOD_NAME, e);
            		throw e;
            	}else{
            		returnedBackupFilesList = backup.getSecureBackupFileList();            	

            		if (returnedBackupFilesList!=null){
            			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"Number of backup files returned = " + returnedBackupFilesList.size());            		
            			java.util.Iterator<SecureBackupFile> it=returnedBackupFilesList.iterator();
            			while(it.hasNext())
            			{
            				SecureBackupFile file=(SecureBackupFile)it.next();
            				backupHashTable.put(file.getName(),file.getStringValue().getBytes());                    	  
            			}
            		}
            	}
            }
            
        }
        catch (XmlException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupErrParse",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.backupErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
        catch (XmlValueOutOfRangeException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupInvalidEnum",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.backupInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }        
    	
    	return backupHashTable;
    }

    
    public void restoreDevice(DeviceContext device, String cryptoCredentialName, boolean validate, URI secureBackupSource, Hashtable<String, byte[]> backupFilesTable)
    throws AMPIOException, InvalidCredentialsException, AMPException
    {
    	final String METHOD_NAME = "restoreDevice"; //$NON-NLS-1$


    	SecureRestoreRequestDocument requestDoc = 
    		SecureRestoreRequestDocument.Factory.newInstance();

    	SecureRestoreRequestDocument.SecureRestoreRequest request = 
    		requestDoc.addNewSecureRestoreRequest();

    	if(cryptoCredentialName !=null){
    		request.setCryptoCredentialName(cryptoCredentialName);
    	}        

    	if (secureBackupSource!=null ){
    		if (secureBackupSource.getScheme().equalsIgnoreCase("file")){
    			SecureBackup backupFiles = readBackUpFiles(backupFilesTable);
    			request.setSecureBackup(backupFiles);

    		}else{
    			request.setSecureBackupSource(secureBackupSource.toString());
    		}
    	}	
    	
    	if(validate){
           request.addNewValidate();
    	}   	   

    	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
    	"backupRequest request created"); //$NON-NLS-1$

    	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
    			"Sending backup request to device "  //$NON-NLS-1$
    			+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$  

    	/* Send request to device */

    	StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 

    	Node responseDocXml = soapHelper.call(device, outMessage); 

    	outMessage.delete(0,outMessage.length());
    	outMessage = null;
    	requestDoc.setNil();
    	requestDoc = null;     

    	/* Parse the request into a response object */
    	try{
    		SecureRestoreResponseDocument responseDoc = SecureRestoreResponseDocument.Factory.parse(responseDocXml);
    		SecureRestoreResponseDocument.SecureRestoreResponse restoreResponse = 
    			responseDoc.getSecureRestoreResponse();

    		if (restoreResponse == null){
    			Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreNoResp",params);
    			AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.restoreNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
    			logger.throwing(CLASS_NAME, METHOD_NAME, e);
    			throw e;
    		}

    		Status.Enum status = restoreResponse.getStatus();


    		if (status == null ){
    			Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreNoStat",params);
    			AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.restoreNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
    			logger.throwing(CLASS_NAME, METHOD_NAME, e);
    			throw e;
    		}
    		else if (status.equals(Status.ERROR)){
    			Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreFail",params);
    			DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.restoreFail",params); //$NON-NLS-1$ //$NON-NLS-2$
    			logger.throwing(CLASS_NAME, METHOD_NAME, e);
    			throw e;
    		}

    	}
    	catch (XmlException e){
    		Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    		String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreErrParse",params);
    		AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.restoreErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
    		logger.throwing(CLASS_NAME, METHOD_NAME, ex);
    		throw ex;
    	}
    	catch (XmlValueOutOfRangeException e){
    		Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
    		String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreInvalidEnum",params);
    		AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.restoreInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
    		logger.throwing(CLASS_NAME, METHOD_NAME, ex);
    		throw ex;
    	}            	        

    	return;
    }
    
    public void quiesceDomain(DeviceContext device, String domain, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException
    {
        final String METHOD_NAME = "quiesceDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
    
        QuiesceRequestDocument requestDoc = 
        	QuiesceRequestDocument.Factory.newInstance();
        
        QuiesceRequestDocument.QuiesceRequest request = 
        	requestDoc.addNewQuiesceRequest();
        
        QuiesceRequestDocument.QuiesceRequest.Domain requestDomain = 
        	request.addNewDomain();        
        requestDomain.setName(domain);
        requestDomain.setTimeout(timeout);

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
        	"quiesce request created"); //$NON-NLS-1$

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		"Sending quiesce request to device "  //$NON-NLS-1$
        		+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 

        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;

        /* Parse the request into a response object */
        try{
            QuiesceResponseDocument responseDoc = QuiesceResponseDocument.Factory.parse(responseDocXml);
            QuiesceResponseDocument.QuiesceResponse quiesceResponse = 
                responseDoc.getQuiesceResponse();

            if (quiesceResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }

            Status.Enum status = quiesceResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
//            else {
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceOkStat",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceOkStat",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
        }
        catch (XmlException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
        catch (XmlValueOutOfRangeException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
    }
    
    public void unquiesceDomain(DeviceContext device, String domain) throws AMPIOException, InvalidCredentialsException, AMPException
    {
        final String METHOD_NAME = "unquiesceDomain"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
    
        UnquiesceRequestDocument requestDoc = 
        	UnquiesceRequestDocument.Factory.newInstance();
        
        UnquiesceRequestDocument.UnquiesceRequest request = 
        	requestDoc.addNewUnquiesceRequest();
        
        UnquiesceRequestDocument.UnquiesceRequest.Domain requestDomain = 
        	request.addNewDomain();        
        requestDomain.setName(domain);

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
        	"unquiesce request created"); //$NON-NLS-1$

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		"Sending unquiesce request to device "  //$NON-NLS-1$
        		+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 

        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;

        /* Parse the request into a response object */
        try{
            UnquiesceResponseDocument responseDoc = UnquiesceResponseDocument.Factory.parse(responseDocXml);
            UnquiesceResponseDocument.UnquiesceResponse unquiesceResponse = 
                responseDoc.getUnquiesceResponse();

            if (unquiesceResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }

            Status.Enum status = unquiesceResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
//            else {
//            	Object[] params = {device.getHostname(),new Integer(device.getAMPPort())};
//            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceOkStat",params);
//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceOkStat",params); //$NON-NLS-1$ //$NON-NLS-2$
//                logger.throwing(CLASS_NAME, METHOD_NAME, e);
//                throw e;
//            }
        }
        catch (XmlException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
        catch (XmlValueOutOfRangeException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
    }

    public void quiesceDevice(DeviceContext device, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException
    {
        final String METHOD_NAME = "quiesceDevice"; //$NON-NLS-1$
        
//







        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "quiesceDevice is not implemented in defaultProvider!"); //$NON-NLS-1$

        QuiesceRequestDocument requestDoc = QuiesceRequestDocument.Factory.newInstance();
        QuiesceRequestDocument.QuiesceRequest request = requestDoc.addNewQuiesceRequest();
        QuiesceRequestDocument.QuiesceRequest.Device requestDevice = request.addNewDevice();
        requestDevice.setTimeout(timeout);

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
        	"quiesce request created"); //$NON-NLS-1$

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		"Sending quiesce request to device "  //$NON-NLS-1$
        		+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 

        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;

        /* Parse the request into a response object */
        try{
            QuiesceResponseDocument responseDoc = QuiesceResponseDocument.Factory.parse(responseDocXml);
            QuiesceResponseDocument.QuiesceResponse quiesceResponse = 
                responseDoc.getQuiesceResponse();

            if (quiesceResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }

            Status.Enum status = quiesceResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
        catch (XmlValueOutOfRangeException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
    }
    
    public void unquiesceDevice(DeviceContext device) throws AMPIOException, InvalidCredentialsException, AMPException
    {
        final String METHOD_NAME = "unquiesceDevice"; //$NON-NLS-1$    	
        // Implemented in wamt.amp.defaultV2Provider 
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "unquiesceDevice is not implemented in defaultV2Provider!"); //$NON-NLS-1$
        
        UnquiesceRequestDocument requestDoc = UnquiesceRequestDocument.Factory.newInstance();
        UnquiesceRequestDocument.UnquiesceRequest request = requestDoc.addNewUnquiesceRequest();
        request.addNewDevice();

        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
        	"unquiesce request created"); //$NON-NLS-1$

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
        		"Sending unquiesce request to device "  //$NON-NLS-1$
        		+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

        /* Send request to device */
        StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions())); 
        Node responseDocXml = soapHelper.call(device, outMessage); 

        outMessage.delete(0,outMessage.length());
        outMessage = null;
        requestDoc.setNil();
        requestDoc = null;

        /* Parse the request into a response object */
        try{
            UnquiesceResponseDocument responseDoc = UnquiesceResponseDocument.Factory.parse(responseDocXml);
            UnquiesceResponseDocument.UnquiesceResponse unquiesceResponse = 
                responseDoc.getUnquiesceResponse();

            if (unquiesceResponse == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }

            Status.Enum status = unquiesceResponse.getStatus();
            if (status == null){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            else if (status.equals(Status.ERROR)){
            	Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            	String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail",params);
            	DeviceExecutionException e = new DeviceExecutionException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch (XmlException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
        catch (XmlValueOutOfRangeException e){
	        Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
	        String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum",params);
	        AMPException ex = new AMPException(message,e,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum",params); //$NON-NLS-1$ //$NON-NLS-2$
	        logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	        throw ex;
        }
    }

    /*
     * Load the encrypted content from files that were previously saved from a
     * Secure Backup call.  This method is called from 
     * 
     * @param backupFilesTable this is a Hashtable whose keys are file names and the values
     * are the corresponding file content.  The information from the table will be loaded into
     * com.datapower.schemas.appliance.management.x20.SecureBackup and sent by AMP to the device on a Secure Restore request. 
     * @return SecureBackup contains multiple SecureBackupFile objects - one for each file used
     * during Secure Restore     
     * @see #restoreDevice
     */
    private SecureBackup  readBackUpFiles(Hashtable<String,byte[]> backupFilesTable) {
    	final String METHOD_NAME = "writeToBackUpFile";

    	if (backupFilesTable == null){
    		return null;
    	}

    	SecureBackupFile sbf = null;
    	SecureBackup sb = SecureBackup.Factory.newInstance();   
    	Enumeration<String> fileNames = backupFilesTable.keys();               

    	while(fileNames.hasMoreElements())
    	{
    		String fileName =(String)fileNames.nextElement();
    		byte[] fileContent = (byte[]) backupFilesTable.get(fileName);

    		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
    				"Adding backup File: " + fileName);

    		try{
    			sbf = sb.addNewSecureBackupFile();
    			sbf.setName(fileName);
    			sbf.setStringValue(new String(fileContent));

    		}catch(Exception e){
    			logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "Exception thrown:", e);
    			//e.printStackTrace();
    		}
    	}   	    	    	    	

    	return sb;
    }
    
	// =========================== New functions for provider V3 ===========================
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getServiceList(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
     */
    public RuntimeService[] getServiceListFromDomain(DeviceContext device, String domainName)
    	throws DeviceExecutionException, AMPIOException, AMPException {
    	
    	final String METHOD_NAME = "getServiceListFromDomain"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getServiceListFromDomain() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
        
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getInterDependentServices(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[])
     */
    public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, String fileDomainName,
    		String fileNameOnDevice, ConfigObject[] objectInfo)
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
    	
    	final String METHOD_NAME = "getInterDependentServices"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getInterDependentServices() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
                
    	throw new UnsupportedOperationException();    	
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getInterDependentServices(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, byte[], com.ibm.datapower.amt.amp.ObjectContext[])
     */
    public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, byte[] packageImage, ConfigObject[] objectArray)
		throws NotExistException, InvalidCredentialsException,DeviceExecutionException, AMPIOException, AMPException {
    	
    	final String METHOD_NAME = "getInterDependentServices"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getInterDependentServices()s is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
                
    	throw new UnsupportedOperationException(); 
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getServiceListFromExport(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String)
     */
    public ConfigService[] getServiceListFromExport(DeviceContext device, String fileDomainName, String fileNameOnDevice)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
    	
    	final String METHOD_NAME = "getServiceListFromExport"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getServiceListFromExport() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
    	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getServiceListFromExport(com.ibm.datapower.amt.amp.DeviceContext, byte[])
     */
    public ConfigService[] getServiceListFromExport(DeviceContext device, byte[] packageImage)
		throws NotExistException, InvalidCredentialsException,DeviceExecutionException, AMPIOException, AMPException {
   	
    	final String METHOD_NAME = "getServiceListFromExport"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getServiceListFromExport() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
    	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#getReferencedObjects(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String)
     */
    public ReferencedObjectCollection getReferencedObjects(DeviceContext device, String domainName, String objectName, String objectClassName)
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
    	final String METHOD_NAME = "getReferencedObjects"; //$NON-NLS-1$    	
        // Implemented in wamt.amp.defaultV3Provider 
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getReferencedObjects() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
    	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#deleteService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String, com.ibm.datapower.amt.amp.ObjectMetaInfo[], boolean)
     */
    public DeleteObjectResult[] deleteService(DeviceContext device, String domainName, String objectName, String objectClassName, 
    			ConfigObject [] excludeObjects, boolean deleteReferencedFiles)
    	throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

    	final String METHOD_NAME = "deleteService"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "deleteService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
    	
    	throw new UnsupportedOperationException();    	
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#quiesceService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[], int)
     */
    public void quiesceService(DeviceContext device, String domain, ConfigObject[] objects, int timeout) 
		throws AMPIOException, InvalidCredentialsException, AMPException {    	

    	final String METHOD_NAME = "quiesceService"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "quiesceService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
    	
    	throw new UnsupportedOperationException();
    }
    
   /*
    * (non-Javadoc)
    * @see com.ibm.datapower.amt.amp.Commands#unquiesceService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[])
    */
    public void unquiesceService(DeviceContext device, String domain, ConfigObject[] objects) 
    	throws AMPIOException, InvalidCredentialsException, AMPException {    	

    	final String METHOD_NAME = "unquiesceService"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "unquiesceService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
        	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#startService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[])
     */
    public void startService(DeviceContext device, String domainName, ConfigObject[] object) 
		throws AMPIOException, InvalidCredentialsException, AMPException {    	

    	final String METHOD_NAME = "startService"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "startService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
        	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#stopService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[])
     */
    public void stopService(DeviceContext device, String domainName, ConfigObject[] objects) 
		throws AMPIOException, InvalidCredentialsException, AMPException {

    	final String METHOD_NAME = "stopService"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "stopService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
        	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setDomainByService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[], byte[], com.ibm.datapower.amt.clientAPI.DeploymentPolicy, boolean)
     */
    public void setDomainByService(DeviceContext device, String domainName, ConfigObject[] objects, byte[] domainImage,
    			DeploymentPolicy policy, boolean ImportAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException {   	

    	final String METHOD_NAME = "setDomainByService"; //$NON-NLS-1$    	
    	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "setDomainByService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
       	
    	throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#setDomainByService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ConfigObject[], java.lang.String, java.lang.String, com.ibm.datapower.amt.clientAPI.DeploymentPolicy, boolean)
     */
    public void setDomainByService(DeviceContext device, String domainName,	ConfigObject[] objects, String fileDomainName, String fileNameOnDevice,
    		DeploymentPolicy policy, boolean importAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException{    	

    	final String METHOD_NAME = "setDomainByService"; //$NON-NLS-1$    	
	    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "setDomainByService() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
	        	
    	throw new UnsupportedOperationException();
	}
    
    public void setFirmware(DeviceContext device, byte[] firmwareImage, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException{    	

    	final String METHOD_NAME = "setFirmware"; //$NON-NLS-1$    	
		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "setFirmware() with parameter acceptLicense is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
		       	
    	throw new UnsupportedOperationException();
	}


	public void setFirmware(DeviceContext device, InputStream inputStream, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {    	

		final String METHOD_NAME = "setFirmware"; //$NON-NLS-1$    	
		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "setFirmware() with parameter acceptLicense is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
			       	
    	throw new UnsupportedOperationException();
	}
    
    /*
     * (non-Javadoc)
     * @see com.ibm.datapower.amt.amp.Commands#deleteFile(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String)
     */
    public void deleteFile(DeviceContext device, String domainName, String fileNameOnDevice)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
    	
    	final String METHOD_NAME = "deleteFile"; //$NON-NLS-1$    	
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "deleteFile() is not implemented in com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl"); //$NON-NLS-1$
        	
    	throw new UnsupportedOperationException();
    }
}
