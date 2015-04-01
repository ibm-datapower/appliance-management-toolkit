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


package com.ibm.datapower.amt.amp.defaultV3Provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.datapower.schemas.appliance.management.x30.AdminState;
import com.datapower.schemas.appliance.management.x30.Backup;
import com.datapower.schemas.appliance.management.x30.BackupFile;
import com.datapower.schemas.appliance.management.x30.CompareConfigRequestDocument;
import com.datapower.schemas.appliance.management.x30.CompareConfigResponseDocument;
import com.datapower.schemas.appliance.management.x30.CompareResult;
import com.datapower.schemas.appliance.management.x30.ConfigObject;
import com.datapower.schemas.appliance.management.x30.ConfigObjects;
import com.datapower.schemas.appliance.management.x30.ConfigState;
import com.datapower.schemas.appliance.management.x30.CryptoFileName;
import com.datapower.schemas.appliance.management.x30.DeleteDomainRequestDocument;
import com.datapower.schemas.appliance.management.x30.DeleteDomainResponseDocument;
import com.datapower.schemas.appliance.management.x30.DeleteFileRequestDocument;
import com.datapower.schemas.appliance.management.x30.DeleteFileResponseDocument;
import com.datapower.schemas.appliance.management.x30.DeleteServiceRequestDocument;
import com.datapower.schemas.appliance.management.x30.DeleteServiceResponseDocument;
import com.datapower.schemas.appliance.management.x30.DeploymentPolicyConfiguration;
import com.datapower.schemas.appliance.management.x30.DetailedConfigObject;
import com.datapower.schemas.appliance.management.x30.DetailedConfigObjects;
import com.datapower.schemas.appliance.management.x30.DeviceType;
import com.datapower.schemas.appliance.management.x30.File;
import com.datapower.schemas.appliance.management.x30.FileLocation;
import com.datapower.schemas.appliance.management.x30.Firmware;
import com.datapower.schemas.appliance.management.x30.GetCryptoArtifactsRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetCryptoArtifactsResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetDeviceInfoRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetDeviceInfoResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainExportRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainExportResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainListRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainListResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainStatusRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetDomainStatusResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetErrorReportRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetErrorReportResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetInterDependentServicesRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetInterDependentServicesResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetReferencedObjectsRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetReferencedObjectsResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetServiceListFromDomainRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetServiceListFromDomainResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetServiceListFromExportRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetServiceListFromExportResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetTokenRequestDocument;
import com.datapower.schemas.appliance.management.x30.GetTokenResponseDocument;
import com.datapower.schemas.appliance.management.x30.ManagementInterface;
import com.datapower.schemas.appliance.management.x30.ManagementType;
import com.datapower.schemas.appliance.management.x30.OpState;
import com.datapower.schemas.appliance.management.x30.PingRequestDocument;
import com.datapower.schemas.appliance.management.x30.PingResponseDocument;
import com.datapower.schemas.appliance.management.x30.QuiesceRequestDocument;
import com.datapower.schemas.appliance.management.x30.QuiesceResponseDocument;
import com.datapower.schemas.appliance.management.x30.RebootMode;
import com.datapower.schemas.appliance.management.x30.RebootRequestDocument;
import com.datapower.schemas.appliance.management.x30.RebootResponseDocument;
import com.datapower.schemas.appliance.management.x30.RestartDomainRequestDocument;
import com.datapower.schemas.appliance.management.x30.RestartDomainResponseDocument;
import com.datapower.schemas.appliance.management.x30.SecureBackup;
import com.datapower.schemas.appliance.management.x30.SecureBackupRequestDocument;
import com.datapower.schemas.appliance.management.x30.SecureBackupResponseDocument;
import com.datapower.schemas.appliance.management.x30.SecureRestoreRequestDocument;
import com.datapower.schemas.appliance.management.x30.SecureRestoreResponseDocument;
import com.datapower.schemas.appliance.management.x30.SetDomainExportRequestDocument;
import com.datapower.schemas.appliance.management.x30.SetDomainExportResponseDocument;
import com.datapower.schemas.appliance.management.x30.SetFileRequestDocument;
import com.datapower.schemas.appliance.management.x30.SetFileResponseDocument;
import com.datapower.schemas.appliance.management.x30.SetFirmwareRequestDocument;
import com.datapower.schemas.appliance.management.x30.SetFirmwareResponseDocument;
import com.datapower.schemas.appliance.management.x30.StartDomainRequestDocument;
import com.datapower.schemas.appliance.management.x30.StartDomainResponseDocument;
import com.datapower.schemas.appliance.management.x30.StartServiceRequestDocument;
import com.datapower.schemas.appliance.management.x30.StartServiceResponseDocument;
import com.datapower.schemas.appliance.management.x30.StopDomainRequestDocument;
import com.datapower.schemas.appliance.management.x30.StopDomainResponseDocument;
import com.datapower.schemas.appliance.management.x30.StopServiceRequestDocument;
import com.datapower.schemas.appliance.management.x30.StopServiceResponseDocument;
import com.datapower.schemas.appliance.management.x30.SubscribeRequestDocument;
import com.datapower.schemas.appliance.management.x30.SubscribeResponseDocument;
import com.datapower.schemas.appliance.management.x30.SubscriptionStateUrl;
import com.datapower.schemas.appliance.management.x30.SubscriptionTopic;
import com.datapower.schemas.appliance.management.x30.TokenType;
import com.datapower.schemas.appliance.management.x30.UnquiesceRequestDocument;
import com.datapower.schemas.appliance.management.x30.UnquiesceResponseDocument;
import com.datapower.schemas.appliance.management.x30.UnsubscribeRequestDocument;
import com.datapower.schemas.appliance.management.x30.UnsubscribeResponseDocument;
import com.datapower.schemas.appliance.management.x30.GetInterDependentServicesResponseDocument.GetInterDependentServicesResponse.FilesToBeOverwritten;
import com.datapower.schemas.appliance.management.x30.GetReferencedObjectsResponseDocument.GetReferencedObjectsResponse.Files;
import com.datapower.schemas.appliance.management.x30.SecureBackup.SecureBackupFile;
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
import com.ibm.datapower.amt.clientAPI.ConfigService;
import com.ibm.datapower.amt.clientAPI.DeletedException;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicy;
import com.ibm.datapower.amt.clientAPI.Device;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.RuntimeService;
import com.ibm.datapower.amt.clientAPI.UnsuccessfulOperationException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Implements the Commands interface specified in
 * {@link com.ibm.datapower.amt.amp.Commands}. The device has a schema which
 * describes the format and allowed values for the SOAP messages, see
 * store:/app-mgmt-protocol-v2.xsd and store:/app-mgmt-protocol-v2.wsdl.
 * <p>
 * This implementation leverages the Apache XMLBeans generated classes from the
 * AMP schema.
 * 
 * 
 * @see com.ibm.datapower.amt.amp.Commands
 */
public class CommandsImpl implements Commands {
	private SOAPHelper soapHelper;

	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;

	final String setFirmwareHeader = "<amp:SetFirmwareRequest xmlns:amp=\"http://www.datapower.com/schemas/appliance/management/" + AMPConstants.AMP_V3_0 + "\">\n<amp:Firmware>"; //$NON-NLS-1$
	final String setFirmwareFooter = "</amp:Firmware>\n</amp:SetFirmwareRequest>"; //$NON-NLS-1$

	final byte[] setFirmwareHeaderBytes = setFirmwareHeader.getBytes();
	final byte[] setFirmwareFooterBytes = setFirmwareFooter.getBytes();

	private static final String CLASS_NAME = CommandsImpl.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}

	public CommandsImpl(String soapHelperImplementationClassName) throws AMPException {

		final String METHOD_NAME = "CommandsImpl()"; //$NON-NLS-1$

		soapHelper = SOAPHelperFactory.getSOAPHelper(soapHelperImplementationClassName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "soapHelper object created"); //$NON-NLS-1$

	}

	/*
	 * @see com.ibm.datapower.amt.amp.Commands#subscribeToDevice(
	 * com.ibm.datapower.amt.amp.DeviceContext, java.lang.String,
	 * com.ibm.datapower.amt.StringCollection, java.net.URL)
	 */
	public SubscriptionResponseCode subscribeToDevice(DeviceContext device, String subscriptionId, StringCollection topics, URL callback) 
		throws InvalidCredentialsException,	AMPIOException, DeviceExecutionException, AMPException {

		final String METHOD_NAME = "subscribeToDevice"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, subscriptionId, topics, callback });

		SubscribeRequestDocument requestDoc = SubscribeRequestDocument.Factory.newInstance();
		SubscribeRequestDocument.SubscribeRequest subscribeRequest = requestDoc.addNewSubscribeRequest();
		SubscribeRequestDocument.SubscribeRequest.Subscription subscription = subscribeRequest.addNewSubscription();

		subscription.setId(subscriptionId);
		subscription.setURL(callback.toString());

		SubscribeRequestDocument.SubscribeRequest.Subscription.Topics requestTopics = subscription.addNewTopics();

		final String configuration = SubscriptionTopic.CONFIGURATION.toString();
		final String firmware = SubscriptionTopic.FIRMWARE.toString();
		final String operational = SubscriptionTopic.OPERATIONAL.toString();
		final String all = SubscriptionTopic.X.toString();
		for (int i = 0; i < topics.size(); i++) {
			if (topics.get(i).equalsIgnoreCase(configuration))
				requestTopics.addTopic(SubscriptionTopic.CONFIGURATION);
			else if (topics.get(i).equalsIgnoreCase(firmware))
				requestTopics.addTopic(SubscriptionTopic.FIRMWARE);
			else if (topics.get(i).equalsIgnoreCase(operational))
				requestTopics.addTopic(SubscriptionTopic.OPERATIONAL);
			else if (topics.get(i).equalsIgnoreCase(all))
				requestTopics.addTopic(SubscriptionTopic.X);
			else {
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic", topics.get(i));
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic", topics.get(i));
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "subscribeRequestDocument created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending subscriptionRequest(" + subscriptionId + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		SubscriptionResponseCode result = null;
		/* Parse the request into a SubscribeResponseDocument */
		try {
			SubscribeResponseDocument responseDoc = SubscribeResponseDocument.Factory.parse(responseDocXml);

			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Received subscriptionResponse(" + subscriptionId + ") from device " //$NON-NLS-1$ //$NON-NLS-2$
					+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "subscribeResponseDocument received"); //$NON-NLS-1$

			SubscribeResponseDocument.SubscribeResponse subscriptionResponse = responseDoc.getSubscribeResponse();
			if (subscriptionResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noResponse", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noResponse", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			SubscriptionStateUrl subState = subscriptionResponse.getSubscriptionState();

			if (subState == null) {

				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noSubState", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noSubState", params); //$NON-NLS-1$ //$NON-NLS-2$            	
				throw e;
			}
			/* Return appropriate SubscriptionResponseCode from response */
			if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString()))
				result = SubscriptionResponseCode.ACTIVE;
			else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.DUPLICATE_STRING)) {
				String originalURL = subState.getURL();
				result = SubscriptionResponseCode.createWithDuplicate(originalURL);
			} else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))
				/*
				 * according to the device's AMP implementation, it will never
				 * send a NONE in a subscribe response, only in an unsubscribe
				 * response or a ping response. But for the sake of
				 * completeness, set the POJO value just in case this value ever
				 * shows up.
				 */
				result = SubscriptionResponseCode.NONE;
			else if (subState.getStringValue().equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString())) {
				Object[] params = { subscriptionId, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errSub", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errSub", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSubDev", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSubDev", params); //$NON-NLS-1$ //$NON-NLS-2$
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
	public void unsubscribeFromDevice(DeviceContext device, String subscriptionID, StringCollection topics) throws NotExistException, InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "unsubscribeFromDevice"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, subscriptionID, topics });

		UnsubscribeRequestDocument requestDoc = UnsubscribeRequestDocument.Factory.newInstance();
		UnsubscribeRequestDocument.UnsubscribeRequest unsubscribeRequest = requestDoc.addNewUnsubscribeRequest();
		UnsubscribeRequestDocument.UnsubscribeRequest.Subscription unsubscription = unsubscribeRequest.addNewSubscription();

		unsubscription.setId(subscriptionID);

		UnsubscribeRequestDocument.UnsubscribeRequest.Subscription.Topics requestTopics = unsubscription.addNewTopics();

		final String configuration = SubscriptionTopic.CONFIGURATION.toString();
		final String firmware = SubscriptionTopic.FIRMWARE.toString();
		final String operational = SubscriptionTopic.OPERATIONAL.toString();
		final String all = SubscriptionTopic.X.toString();
		for (int i = 0; i < topics.size(); i++) {
			if (topics.get(i).equalsIgnoreCase(configuration))
				requestTopics.addTopic(SubscriptionTopic.CONFIGURATION);
			else if (topics.get(i).equalsIgnoreCase(firmware))
				requestTopics.addTopic(SubscriptionTopic.FIRMWARE);
			else if (topics.get(i).equalsIgnoreCase(operational))
				requestTopics.addTopic(SubscriptionTopic.OPERATIONAL);
			else if (topics.get(i).equalsIgnoreCase(all))
				requestTopics.addTopic(SubscriptionTopic.X);
			else {
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic", topics.get(i));
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidTopic", topics.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "unsubscribeRequestDocument created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending unsubscriptionRequest(" + subscriptionID + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a UnsubscribeResponseDocument */
		try {
			UnsubscribeResponseDocument responseDoc = UnsubscribeResponseDocument.Factory.parse(responseDocXml);

			UnsubscribeResponseDocument.UnsubscribeResponse unsubscribeResponse = responseDoc.getUnsubscribeResponse();
			if (unsubscribeResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			com.datapower.schemas.appliance.management.x30.SubscriptionState.Enum subState = unsubscribeResponse.getSubscriptionState();
			String subStateString = null;
			if (subState != null)
				subStateString = subState.toString();
			else {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noUnsubState", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noUnsubState", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString())) {
				Object[] params = { subscriptionID, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errRemSub", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errRemSub", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			} else if ((subStateString.equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString())) || (subStateString.equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { subscriptionID, device.getHostname() };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.subIdNotExist", params);
				NotExistException e = new NotExistException(message, "wamt.amp.defaultV2Provider.CommandsImpl.subIdNotExist", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumUnsub", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumUnsub", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#pingDevice(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.lang.String)
	 */
	public PingResponse pingDevice(DeviceContext device, String subscriptionID) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "pingDevice"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, subscriptionID });

		PingRequestDocument requestDoc = PingRequestDocument.Factory.newInstance();
		PingRequestDocument.PingRequest pingRequest = requestDoc.addNewPingRequest();

		pingRequest.setSubscriptionID(subscriptionID);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "PingRequestDocument created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending pingRequest(" + subscriptionID + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a PingResponse */
		PingResponse result = null;
		try {
			PingResponseDocument responseDoc = PingResponseDocument.Factory.parse(responseDocXml);

			PingResponseDocument.PingResponse pingResponse = responseDoc.getPingResponse();
			if (pingResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noPingResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noPingResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			com.datapower.schemas.appliance.management.x30.SubscriptionState.Enum subState = pingResponse.getSubscriptionState();
			String subStateString = null;
			if (subState != null)
				subStateString = subState.toString();
			else {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noSubStateInPing", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noSubStateInPing", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.ACTIVE.toString()))
				result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.ACTIVE);
			else if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.NONE.toString()))
				result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.NONE);
			else if (subStateString.equalsIgnoreCase(SubscriptionResponseCode.FAULT.toString()))
				result = new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.FAULT);
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSubResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumPingResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumPingResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower
	 * .wamt.amp.DeviceContext)
	 */
	public DeviceMetaInfo getDeviceMetaInfo(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDeviceMetaInfo"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		GetDeviceInfoRequestDocument requestDoc = GetDeviceInfoRequestDocument.Factory.newInstance();
		// GetDeviceInfoRequestDocument.GetDeviceInfoRequest
		// getDeviceInfoRequest =
		requestDoc.addNewGetDeviceInfoRequest();

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getDeviceInfoRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getDeviceInfoRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a DeviceMetaInfo object */
		try {
			GetDeviceInfoResponseDocument responseDoc = GetDeviceInfoResponseDocument.Factory.parse(responseDocXml);
			GetDeviceInfoResponseDocument.GetDeviceInfoResponse getDeviceInfoResponse = responseDoc.getGetDeviceInfoResponse();

			if (getDeviceInfoResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noRespgetDevInfo", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noRespgetDevInfo", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String deviceName = getDeviceInfoResponse.getDeviceName();
			String currentAMPVersion = Device.getCurrentAMPVersionFromGetDeviceInfoResponse(getDeviceInfoResponse.getCurrentAMPVersion());
			String serialNumber = getDeviceInfoResponse.getDeviceSerialNo();

			String responseDeviceType = getDeviceInfoResponse.getDeviceType();
			if ((responseDeviceType == null) || (responseDeviceType.length() == 0)) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.devTypNotSpecified", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.devTypNotSpecified", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			com.ibm.datapower.amt.DeviceType deviceType = null;
			deviceType = com.ibm.datapower.amt.DeviceType.fromString(responseDeviceType.toString());

			String versionComposite = getDeviceInfoResponse.getFirmwareVersion();
			int periodIndex = versionComposite.indexOf("."); //$NON-NLS-1$
			String firmwareLevel = versionComposite.substring(periodIndex + 1);

			ModelType modelType = Device.getModelTypeFromDeviceID(getDeviceInfoResponse.getDeviceID());
			String hardwareOptions = Device.getHardwareOptionsFromDeviceID(getDeviceInfoResponse.getDeviceID());

			StringCollection featureLicenses = new StringCollection();
			List<String> dfeatureLicensesArray = getDeviceInfoResponse.getDeviceFeatureList();
			if (dfeatureLicensesArray.size() != 0) {
				for (int i = 0; i < dfeatureLicensesArray.size(); i++) {
					featureLicenses.add(dfeatureLicensesArray.get(i));
				}
			}

			int webGUIPort = -1;
			ManagementInterface[] mgmtArray = getDeviceInfoResponse.getManagementInterfaceArray();
			if (mgmtArray != null) {
				for (int i = 0; i < mgmtArray.length; i++) {
					if (mgmtArray[i].getType() == ManagementType.WEB_MGMT) {
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
			
			//StringCollection deviceOperations = new StringCollection();
			
			// FIXME Change to retrieve operations from device ?
			
			int supportedCommands[] = new int[]{
					Commands.BACKUP_DEVICE,	Commands.DELETE_DOMAIN,	Commands.DELETE_FILE, Commands.DELETE_SERVICE, 
					Commands.GET_DEVICE_METAINFO, Commands.GET_DOMAIN, Commands.GET_DOMAIN_DIFFERENCES,
					Commands.GET_DOMAIN_LIST, Commands.GET_DOMAIN_STATUS, Commands.GET_ERROR_REPORT, 
					Commands.GET_INTERDEPENDENT_SERVICES_FILE, Commands.GET_INTERDEPENDENT_SERVICES_IMAGE, 
					Commands.GET_KEY_FILENAMES, Commands.GET_REFERENCED_OBJECTS, Commands.GET_SAML_TOKEN, 
					Commands.GET_SERVICE_LIST_FROM_DOMAIN,	Commands.GET_SERVICE_LIST_FROM_EXPORT_IMAGE, 
					Commands.GET_SERVICE_LIST_FROM_EXPORT_FILE, Commands.IS_DOMAIN_DIFFERENT, Commands.PING_DEVICE, 
					Commands.QUIESCE_DEVICE, Commands.QUIESCE_DOMAIN, Commands.QUIESCE_SERVICE, 
					Commands.REBOOT, Commands.RESTART_DOMAIN, Commands.RESTORE_DEVICE, Commands.SET_DOMAIN,
					Commands.SET_DOMAIN_BY_SERVICE_FILE, Commands.SET_DOMAIN_BY_SERVICE_IMAGE, Commands.SET_FILE, 
					Commands.SET_FIRMWARE_IMAGE, Commands.SET_FIRMWARE_IMAGE_ACCEPT_LICENSE, Commands.SET_FIRMWARE_STREAM, 
					Commands.SET_FIRMWARE_STREAM_ACCEPT_LICENSE,	Commands.START_DOMAIN, Commands.START_SERVICE, 
					Commands.STOP_DOMAIN, Commands.STOP_SERVICE, Commands.SUBSCRIBE_TO_DEVICE, Commands.UNSUBSCRIBE_FROM_DEVICE, 
					Commands.UNQUIESCE_DEVICE, Commands.UNQUIESCE_DOMAIN, Commands.UNQUIESCE_SERVICE};
			
			String secureBackup = "disabled";
			if ( getDeviceInfoResponse.getSecureBackup() != null )
				secureBackup = getDeviceInfoResponse.getSecureBackup().toString();			

			DeviceMetaInfo result = new DeviceMetaInfo(deviceName, serialNumber, currentAMPVersion, modelType, hardwareOptions, 
					webGUIPort, deviceType, firmwareLevel,
					featureLicenses, supportedCommands, secureBackup);
			
			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDevMetaInfo", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDevMetaInfo", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDevMetaInfo", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDevMetaInfo", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#reboot(com.ibm.datapower.amt.amp
	 * .DeviceContext)
	 */
	public void reboot(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "reboot"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		RebootRequestDocument requestDoc = RebootRequestDocument.Factory.newInstance();
		RebootRequestDocument.RebootRequest rebootRequest = requestDoc.addNewRebootRequest();

		rebootRequest.setMode(RebootMode.REBOOT);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "rebootRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending rebootRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a RebootResponse object */
		try {
			RebootResponseDocument responseDoc = RebootResponseDocument.Factory.parse(responseDocXml);

			RebootResponseDocument.RebootResponse rebootResponse = responseDoc.getRebootResponse();
			if (rebootResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.rebootNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum rebootStatus = rebootResponse.getStatus();
			String rebootStatus = rebootResponse.getStatus();
			if (rebootStatus == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.rebootNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (rebootStatus.equals(Status.OK)){
			if (rebootStatus.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.rebootError", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.rebootError", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRebootResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseRebootResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumReboot", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumReboot", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getDomainList(com.ibm.datapower.amt
	 * .amp.DeviceContext)
	 */
	public String[] getDomainList(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDomainList"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		GetDomainListRequestDocument requestDoc = GetDomainListRequestDocument.Factory.newInstance();
		// GetDomainListRequestDocument.GetDomainListRequest
		// getDomainListRequest =
		requestDoc.addNewGetDomainListRequest();

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getDomainListRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getDomainListRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetDomainListResponse object */
		try {
			GetDomainListResponseDocument responseDoc = GetDomainListResponseDocument.Factory.parse(responseDocXml);
			GetDomainListResponseDocument.GetDomainListResponse getGetDomainListResponse = responseDoc.getGetDomainListResponse();

			if (getGetDomainListResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomainListNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getDomainListNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String[] result = getGetDomainListResponse.getDomainArray();

			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getDomain(com.ibm.datapower.amt.
	 * amp.DeviceContext, java.lang.String)
	 */
	public byte[] getDomain(DeviceContext device, String domainName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		GetDomainExportRequestDocument requestDoc = GetDomainExportRequestDocument.Factory.newInstance();
		GetDomainExportRequestDocument.GetDomainExportRequest getDomainExportRequest = requestDoc.addNewGetDomainExportRequest();

		// set domain name
		getDomainExportRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getDomainExportRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetDomainExportResponse object */
		try {
			GetDomainExportResponseDocument responseDoc = GetDomainExportResponseDocument.Factory.parse(responseDocXml);
			GetDomainExportResponseDocument.GetDomainExportResponse getGetDomainExportResponse = responseDoc.getGetDomainExportResponse();
			if (getGetDomainExportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomListNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getDomListNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			Backup backup = getGetDomainExportResponse.getConfig();
			if (backup == null) {
				// fix for cs # 59219
				// Status.Enum status = getGetDomainExportResponse.getStatus();
				String status = getGetDomainExportResponse.getStatus();
				if (status == null) {
					Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainElt", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noDomainElt", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errorGetDom", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errorGetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} else {
					Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
			} else if (backup.isNil()) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec", params);
				NotExistException e = new NotExistException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noDomainRec", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// byte[] result = backup.getByteArrayValue();
			byte[] result = backup.getStringValue().getBytes();
			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;

		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInDomainResp", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInDomainResp", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#setDomain(com.ibm.datapower.amt.
	 * amp.DeviceContext, byte[])
	 */
	public void setDomain(DeviceContext device, String domainName, byte[] domainImage, DeploymentPolicy policy) throws InvalidCredentialsException, DeviceExecutionException,
			AMPIOException, AMPException, DeletedException {

		final String METHOD_NAME = "setDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		SetDomainExportRequestDocument requestDoc = SetDomainExportRequestDocument.Factory.newInstance();
		SetDomainExportRequestDocument.SetDomainExportRequest setDomainExportRequest = requestDoc.addNewSetDomainExportRequest();

		// Backup image
		Backup image = setDomainExportRequest.addNewConfig();
		image.setStringValue(new String(domainImage));
		image.setDomain(domainName);
		
		// reset domain
		setDomainExportRequest.setResetDomain(true);

		// deployment policy
		DeploymentPolicyConfiguration deppol = null;
		if (policy != null) {			
			switch (policy.getPolicyType()) {
			case EXPORT:
				deppol = setDomainExportRequest.addNewPolicyConfiguration();
				deppol.setStringValue(new String(policy.getCachedBytes()));
				deppol.setPolicyDomainName(policy.getPolicyDomainName());
				deppol.setPolicyObjectName(policy.getPolicyObjectName());
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ","
						+ policy.getPolicyDomainName() + "," + policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case XML:
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy xmlpolicy;
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy.ModifiedConfig modifiedConfig;

				// Parse the policy
				String xmlFragment = new String(policy.getCachedBytes());
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				Document doc = null;
				//try {
				DocumentBuilder db;
				try {
					db = dbf.newDocumentBuilder();
					doc = db.parse(new InputSource(new StringReader(xmlFragment)));
				} catch (ParserConfigurationException exception) {
					Object[] params = new Object[] { device.getHostname() };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} catch (SAXException exception) {
					Object[] params = new Object[] { device.getHostname() };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} catch (IOException exception) {
					Object[] params = new Object[] { device.getHostname() };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;				}	

				// Walk the nodes and generate the policy in the request
				Node node = (Node) doc;
				NodeList policyNode = node.getChildNodes();
				for (int i = 0; i < policyNode.getLength(); i++) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ":"
							+ policyNode.item(i).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					if (policyNode.item(i).getNodeName().equalsIgnoreCase("policy")) {
						xmlpolicy = setDomainExportRequest.addNewPolicy();
						NodeList configNodes = policyNode.item(i).getChildNodes();
						for (int j = 0; j < configNodes.getLength(); j++) {
							if (configNodes.item(j).getNodeName().equalsIgnoreCase("modifiedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								modifiedConfig = xmlpolicy.addNewModifiedConfig();
								NodeList propertyNodes = configNodes.item(j).getChildNodes();
								for (int k = 0; k < propertyNodes.getLength(); k++) {
									boolean logProperty = true;
									if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("match")) {
										modifiedConfig.setMatch(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("type")) {
										modifiedConfig.setType(com.datapower.schemas.appliance.management.x30.PolicyType.CHANGE);
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("property")) {
										modifiedConfig.setProperty(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("value")) {
										modifiedConfig.setValue(propertyNodes.item(k).getTextContent());
									} else {
										logProperty = false;
									}

									// log if needed
									if (logProperty) {
										logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
												+ policy.getPolicyType().name() + ":" + propertyNodes.item(k).getNodeName() + ":" + propertyNodes.item(k).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("acceptedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addAcceptedConfig(configNodes.item(j).getTextContent());
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("filteredconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addFilteredConfig(configNodes.item(j).getTextContent());
							}
						}
					}
				}
				break;
			/*
			 * case REFERENCE:
			 * setDomainExportRequest.setPolicyObjectName(policy.
			 * getPolicyObjectName()); logger.logp(Level.FINEST, CLASS_NAME,
			 * METHOD_NAME, "setDomainExportRequest(" + domainName + ") " +
			 * "deployment policy (" + policy.getPolicyType().name() + "," +
			 * policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$
			 * //$NON-NLS-2$ break;
			 */
			default:
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") "
						+ "deployment policy type (" + policy.getPolicyType().name() + ")is unsupported."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setDomainExportRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a SetDomainExportResponse object */
		try {
			SetDomainExportResponseDocument responseDoc = SetDomainExportResponseDocument.Factory.parse(responseDocXml);
			SetDomainExportResponseDocument.SetDomainExportResponse getSetDomainExportResponse = responseDoc.getSetDomainExportResponse();

			if (getSetDomainExportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Use as a string to allow different error return codes.
			// String status = getSetDomainExportResponse.getStatus();
			String status = getSetDomainExportResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.OK.toString())){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()), status };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#deleteDomain(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.lang.String)
	 */
	public void deleteDomain(DeviceContext device, String domainName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "deleteDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		DeleteDomainRequestDocument requestDoc = DeleteDomainRequestDocument.Factory.newInstance();
		DeleteDomainRequestDocument.DeleteDomainRequest deleteDomainRequest = requestDoc.addNewDeleteDomainRequest();

		deleteDomainRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "deleteDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending deleteDomainRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a DeleteDomainResponse object */
		try {
			DeleteDomainResponseDocument responseDoc = DeleteDomainResponseDocument.Factory.parse(responseDocXml);
			DeleteDomainResponseDocument.DeleteDomainResponse getDeleteDomainResponse = responseDoc.getDeleteDomainResponse();

			if (getDeleteDomainResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.delDomainNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.delDomainNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = getDeleteDomainResponse.getStatus();
			String status = getDeleteDomainResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatDelDomain", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noStatDelDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// if (status.equals(Status.OK)){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.delDomainFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.delDomainFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getDomainStatus(com.ibm.datapower
	 * .wamt.amp.DeviceContext, java.lang.String)
	 */
	public DomainStatus getDomainStatus(DeviceContext device, String domainName) 
		throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDomainStatus"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		GetDomainStatusRequestDocument requestDoc = GetDomainStatusRequestDocument.Factory.newInstance();
		GetDomainStatusRequestDocument.GetDomainStatusRequest getDomainStatusRequest = requestDoc.addNewGetDomainStatusRequest();

		getDomainStatusRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getDomainStatusRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getDomainStatusRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		// Send request to device
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		// Parse the request into a GetDomainStatusResponse object
		try {
			GetDomainStatusResponseDocument responseDoc = GetDomainStatusResponseDocument.Factory.parse(responseDocXml);
			GetDomainStatusResponseDocument.GetDomainStatusResponse getGetDomainStatusResponse = responseDoc.getGetDomainStatusResponse();

			if (getGetDomainStatusResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getDomNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$            	
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			GetDomainStatusResponseDocument.GetDomainStatusResponse.Domain domain = getGetDomainStatusResponse.getDomain();

			if (domain == null) {
				// see cs tracker # 59517 for more explanation about the following code

				// if domain isn't here, then we can't have an OpState!
				// therefore, if we get here, and there is no status (or there  is one,
				// and its == 'ok'), then throw an exception as this is a REALLY weird case.

				// in dp build 137835+, this might return a
				// <status>error</status> to represent
				// a non existant domain

				// Status.Enum status = getGetDomainStatusResponse.getStatus();
				String status = getGetDomainStatusResponse.getStatus();
				if (status == null) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getDomNoStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getDomNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet", params);
					NotExistException ex = new NotExistException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, ex);
					throw ex;
				} else {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomainSpec", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noDomainSpec", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;				
				}
			}

			// domain object exists, which means we either have a good message,
			// or a <=3.6.0.3 firmware level

			// workaround below for bug in 3.6.0.3 (dp build 137758)
			OpState.Enum opState = null;
			try {
				opState = domain.getOpState();
			} catch (XmlValueOutOfRangeException e) {
				// a null opState == non existant domain
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet", params);
				NotExistException ex = new NotExistException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.noDomStatRet", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, ex);
				throw ex;
			}

			if (opState == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params); //$NON-NLS-1$ //$NON-NLS-2$
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
			else {
				Object[] params = { opState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			ConfigState.Enum configState = domain.getConfigState();
			if (configState == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			boolean needsSave;
			if (configState.toString().equalsIgnoreCase("modified")) //$NON-NLS-1$
				needsSave = true;
			else if (configState.toString().equalsIgnoreCase("saved")) //$NON-NLS-1$
				needsSave = false;
			else {
				Object[] params = { configState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			AdminState.Enum adminState = domain.getAdminState();
			com.ibm.datapower.amt.AdminStatus adminEnabled;
			if (adminState.toString().equalsIgnoreCase("enabled")) //$NON-NLS-1$
				adminEnabled = com.ibm.datapower.amt.AdminStatus.ENABLED;
			else if (adminState.toString().equalsIgnoreCase("disabled")) //$NON-NLS-1$
				adminEnabled = com.ibm.datapower.amt.AdminStatus.DISABLED;
			else {
				Object[] params = { adminState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			boolean debugState;
			if (domain.getDebugState() == true)
				debugState = true;
			else if (domain.getDebugState() == false)
				debugState = false;
			else {
				Object[] params = { Boolean.valueOf(domain.getDebugState()), device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidDebugStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidDebugStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String quiesceState = domain.getQuiesceState();
			QuiesceStatus quiesceStatus = QuiesceStatus.UNKNOWN; // default
			if (quiesceState.equals(""))
				quiesceStatus = QuiesceStatus.NORMAL;
			else if (quiesceState.equals("quiescing"))
				quiesceStatus = QuiesceStatus.QUIESCE_IN_PROGRESS;
			else if (quiesceState.equals("quiesced"))
				quiesceStatus = QuiesceStatus.QUIESCED;
			else if (quiesceState.equals("unquiescing"))
				quiesceStatus = QuiesceStatus.UNQUIESCE_IN_PROGRESS;
			else if (quiesceState.equals("error"))
				quiesceStatus = QuiesceStatus.ERROR;

			DomainStatus result = new DomainStatus(adminEnabled, opStatus, needsSave, debugState, quiesceStatus);
			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParsingGetDomStat", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParsingGetDomStat", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomStat", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomStat", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#startDomain(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.lang.String)
	 */
	public void startDomain(DeviceContext device, String domainName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "startDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		StartDomainRequestDocument requestDoc = StartDomainRequestDocument.Factory.newInstance();
		StartDomainRequestDocument.StartDomainRequest startDomainRequest = requestDoc.addNewStartDomainRequest();

		startDomainRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "startDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending startDomainRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a StartDomainResponse object */
		try {

			StartDomainResponseDocument responseDoc = StartDomainResponseDocument.Factory.parse(responseDocXml);

			StartDomainResponseDocument.StartDomainResponse startDomainResponse = responseDoc.getStartDomainResponse();

			if (startDomainResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.startDomNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.startDomNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = startDomainResponse.getStatus();
			String status = startDomainResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.startDomNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.startDomNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.OK)){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errStartDom", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errStartDom", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseStartDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseStartDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInStartDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumInStartDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#stopDomain(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.lang.String)
	 */
	public void stopDomain(DeviceContext device, String domainName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "stopDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		StopDomainRequestDocument requestDoc = StopDomainRequestDocument.Factory.newInstance();
		StopDomainRequestDocument.StopDomainRequest stopDomainRequest = requestDoc.addNewStopDomainRequest();

		stopDomainRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "stopDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending stopDomainRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a StopDomainResponse object */
		try {
			StopDomainResponseDocument responseDoc = StopDomainResponseDocument.Factory.parse(responseDocXml);

			StopDomainResponseDocument.StopDomainResponse stopDomainResponse = responseDoc.getStopDomainResponse();

			if (stopDomainResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = stopDomainResponse.getStatus();
			String status = stopDomainResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.stopDomNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.OK)){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errStopDom", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errStopDom", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseStopDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseStopDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumStopDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumStopDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#restartDomain(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.lang.String)
	 */
	public void restartDomain(DeviceContext device, String domainName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException,
			AMPException {

		final String METHOD_NAME = "restartDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		RestartDomainRequestDocument requestDoc = RestartDomainRequestDocument.Factory.newInstance();
		RestartDomainRequestDocument.RestartDomainRequest restartDomainRequest = requestDoc.addNewRestartDomainRequest();

		restartDomainRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "restartDomainRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending restartDomainRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a RestartDomainResponse object */
		try {

			RestartDomainResponseDocument responseDoc = RestartDomainResponseDocument.Factory.parse(responseDocXml);

			RestartDomainResponseDocument.RestartDomainResponse restartDomainResponse = responseDoc.getRestartDomainResponse();

			if (restartDomainResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restartDomNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.restartDomNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = restartDomainResponse.getStatus();
			String status = restartDomainResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatRestartDom", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noStatRestartDom", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.OK)){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errRestartingDom", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.errRestartingDom", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRestDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseRestDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumRestartDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumRestartDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.datapower.amt.amp.Commands#setFirmware(com.ibm.datapower.amt.amp.DeviceContext, byte[], boolean)
	 */
	public void setFirmware(DeviceContext device, byte[] firmwareImage) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setFirmware(byte[]...)"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		SetFirmwareRequestDocument requestDoc = SetFirmwareRequestDocument.Factory.newInstance();
		SetFirmwareRequestDocument.SetFirmwareRequest setFirmwareRequest = requestDoc.addNewSetFirmwareRequest();

		Firmware image = setFirmwareRequest.addNewFirmware();

		image.setByteArrayValue(firmwareImage);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFirmwareRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setFirmwareRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a SetFirmwareResponse object */
		try {
			SetFirmwareResponseDocument responseDoc = SetFirmwareResponseDocument.Factory.parse(responseDocXml);
			SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = responseDoc.getSetFirmwareResponse();

			if (getSetFirmwareResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = getSetFirmwareResponse.getStatus();
			String status = getSetFirmwareResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.ERROR)){
			if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			} else {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#setFirmware(com.ibm.datapower.amt
	 * .amp.DeviceContext, java.io.InputStream, boolean)
	 */
	public void setFirmware(DeviceContext device, InputStream inputStream) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setFirmware(InputStream...)"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, inputStream });

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFirmwareRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setFirmwareRequest to device " + device.getHostname() + //$NON-NLS-1$
				":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		Node responseDocXml = soapHelper.call(device, setFirmwareHeaderBytes, setFirmwareFooterBytes, inputStream);

		/* Parse the request into a SetFirmwareResponse object */
		try {
			SetFirmwareResponseDocument responseDoc = SetFirmwareResponseDocument.Factory.parse(responseDocXml);
			SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = responseDoc.getSetFirmwareResponse();

			if (getSetFirmwareResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = getSetFirmwareResponse.getStatus();
			String status = getSetFirmwareResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.ERROR)){
			if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			} else {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getKeyFilenames(com.ibm.datapower
	 * .wamt.amp.DeviceContext, java.lang.String)
	 */
	public String[] getKeyFilenames(DeviceContext device, String domainName) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getKeyFilenames"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		GetCryptoArtifactsRequestDocument requestDoc = GetCryptoArtifactsRequestDocument.Factory.newInstance();
		GetCryptoArtifactsRequestDocument.GetCryptoArtifactsRequest getCryptoArtifactsRequest = requestDoc.addNewGetCryptoArtifactsRequest();

		getCryptoArtifactsRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getCryptoArtifactsRequest created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getCryptoArtifactsRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetDomainListResponse object */
		try {
			GetCryptoArtifactsResponseDocument responseDoc = GetCryptoArtifactsResponseDocument.Factory.parse(responseDocXml);
			GetCryptoArtifactsResponseDocument.GetCryptoArtifactsResponse getGetCryptoArtifactsResponse = responseDoc.getGetCryptoArtifactsResponse();

			if (getGetCryptoArtifactsResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			GetCryptoArtifactsResponseDocument.GetCryptoArtifactsResponse.CryptoArtifacts artifacts = getGetCryptoArtifactsResponse.getCryptoArtifacts();

			if (artifacts == null) {
				// Status.Enum status =
				// getGetCryptoArtifactsResponse.getStatus();
				String status = getGetCryptoArtifactsResponse.getStatus();
				if (status == null) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNFail", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNFail", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} else {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidResp", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidResp", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
			}

			String domain = artifacts.getDomain();

			if (domain == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoDom", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNNoDom", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			if (!domain.equalsIgnoreCase(domainName)) {
				Object[] params = { domain, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.diffDomain", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.diffDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
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
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseGetKeyFN", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseGetKeyFN", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.getKeyFNInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#setFile(com.ibm.datapower.amt.amp
	 * .DeviceContext, java.lang.String, java.lang.String, byte[])
	 */
	public void setFile(DeviceContext device, String domainName, String filenameOnDevice, byte[] contents) throws NotExistException, InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setFile"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		SetFileRequestDocument requestDoc = SetFileRequestDocument.Factory.newInstance();
		SetFileRequestDocument.SetFileRequest setFileRequest = requestDoc.addNewSetFileRequest();

		try {
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
			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFileRequest created"); //$NON-NLS-1$
	
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setFileRequest(" + domainName + ", " + filenameOnDevice + ") to device " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	
			/* Send request to device */
			StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
			
			Node responseDocXml = soapHelper.call(device, outMessage);
	
			outMessage.delete(0, outMessage.length());
			outMessage = null;
			requestDoc.setNil();
			requestDoc = null;

		/* Parse the request into a SetFileResponse object */
		
			SetFileResponseDocument responseDoc = SetFileResponseDocument.Factory.parse(responseDocXml);
			SetFileResponseDocument.SetFileResponse setFileResponse = responseDoc.getSetFileResponse();

			if (setFileResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFileNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = setFileResponse.getStatus();
			String status = setFileResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFileNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.OK)){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { filenameOnDevice, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFileFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFileFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseRespSetFile", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseRespSetFile", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFile", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFile", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (Error o ) {
			Object[] params = { };
			String message = Messages.getString("wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
			throw e; 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getErrorReport(com.ibm.datapower.
	 * wamt.amp.DeviceContext)
	 */
	public ErrorReport getErrorReport(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getErrorReport"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		GetErrorReportRequestDocument requestDoc = GetErrorReportRequestDocument.Factory.newInstance();
		// GetErrorReportRequestDocument.GetErrorReportRequest
		// getErrorReportRequest =
		requestDoc.addNewGetErrorReportRequest();

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getErrorReport created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getErrorReport to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetErrorReportResponse object */
		try {
			GetErrorReportResponseDocument responseDoc = GetErrorReportResponseDocument.Factory.parse(responseDocXml);
			GetErrorReportResponseDocument.GetErrorReportResponse getGetErrorReportResponse = responseDoc.getGetErrorReportResponse();

			if (getGetErrorReportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			File file = getGetErrorReportResponse.getErrorReport();
			if (file == null) {
				// lets check for the error status
				// Status.Enum status = getGetErrorReportResponse.getStatus();
				String status = getGetErrorReportResponse.getStatus();
				if (status == null) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepFail", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepFail", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} else {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepEmpty", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepEmpty", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
			}

			// byte[] stringVal = file.getByteArrayValue();
			byte[] stringVal = null;
			if (file.getStringValue() != null) {
				stringVal = file.getStringValue().getBytes();
			}

			ErrorReport result = new ErrorReport(file.getDomain(), file.getLocation(), file.getName(), stringVal);
			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepParseErr", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepParseErr", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getErrRepInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.getErrRepInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.datapower.amt.amp.Commands#getErrorReport(com.ibm.datapower.
	 * wamt.amp.DeviceContext)
	 */
	public String getSAMLToken(DeviceContext device, String domainName) throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getSAMLToken"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		GetTokenRequestDocument requestDoc = GetTokenRequestDocument.Factory.newInstance();
		GetTokenRequestDocument.GetTokenRequest getTokenRequest = requestDoc.addNewGetTokenRequest();

		// we only support SAML tokens for login to the webGUI currently

		getTokenRequest.setType(TokenType.LOGIN_WEB_MGMT);
		getTokenRequest.setUser(device.getUserId());
		getTokenRequest.setPassword(device.getPassword());
		getTokenRequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getToken created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getToken to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetErrorReportResponse object */
		try {
			GetTokenResponseDocument responseDoc = GetTokenResponseDocument.Factory.parse(responseDocXml);
			GetTokenResponseDocument.GetTokenResponse getGetTokenResponse = responseDoc.getGetTokenResponse();

			if (getGetTokenResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String samlToken = getGetTokenResponse.getToken();

			if (samlToken == null) {
				// Status.Enum status = getGetTokenResponse.getStatus();
				String status = getGetTokenResponse.getStatus();
				if (status == null) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenFail", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenFail", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} else {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenOkStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenOkStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
			}

			return samlToken;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.getTokenInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.getTokenInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.datapower.amt.amp.Commands#isDifferent(byte[], byte[],
	 * com.ibm.datapower.amt.amp.DeviceContext)
	 */
	public boolean isDomainDifferent(String domainName, byte[] configImage1, byte[] configImage2, DeviceContext device) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "isDomainDifferent"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		// new Object[]{domainName, configImage1, configImage2, device});

		CompareConfigRequestDocument requestDoc = CompareConfigRequestDocument.Factory.newInstance();
		CompareConfigRequestDocument.CompareConfigRequest compareConfigRequest = requestDoc.addNewCompareConfigRequest();
		CompareConfigRequestDocument.CompareConfigRequest.CompareConfig compareConfig = compareConfigRequest.addNewCompareConfig();

		compareConfig.setDomain(domainName);

		CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.From from = compareConfig.addNewFrom();
		Backup fromImage = from.addNewConfig();

		// fromImage.setByteArrayValue(configImage1);
		fromImage.setStringValue(new String(configImage1));
		fromImage.setDomain(domainName);

		CompareConfigRequestDocument.CompareConfigRequest.CompareConfig.To to = compareConfig.addNewTo();
		if (configImage2 == null)
			to.addNewPersisted();
		else {
			Backup toImage = to.addNewConfig();
			// toImage.setByteArrayValue(configImage2);
			toImage.setStringValue(new String(configImage2));
			toImage.setDomain(domainName);
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "isDomainDifferent created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending isDomainDifferent to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a SetDeviceSettingsResponse object */
		try {
			CompareConfigResponseDocument responseDoc = CompareConfigResponseDocument.Factory.parse(responseDocXml);
			CompareConfigResponseDocument.CompareConfigResponse getCompareConfigResponse = responseDoc.getCompareConfigResponse();

			if (getCompareConfigResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			CompareConfigResponseDocument.CompareConfigResponse.CompareConfig compareConfigResponse = getCompareConfigResponse.getCompareConfig();
			if (compareConfigResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompConf", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompConf", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String returnedDomain = compareConfigResponse.getDomain();
			if (returnedDomain == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameNull", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameNull", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			if (!returnedDomain.equalsIgnoreCase(domainName)) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameRec", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffNameRec", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			CompareResult.Enum compareResult = compareConfigResponse.getCompareResult();
			if (compareResult == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompRes", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffNoCompRes", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			if (compareResult.toString().equalsIgnoreCase("identical")) { //$NON-NLS-1$
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return false;
			} else if (compareResult.toString().equalsIgnoreCase("different")) { //$NON-NLS-1$
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return true;
			} else {
				Object[] params = { compareResult, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidRes", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidRes", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.domDiffInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.datapower.amt.amp.Commands#getDifferences(byte[], byte[],
	 * com.ibm.datapower.amt.amp.DeviceContext)
	 */

	public URL getDomainDifferences(String domainName, byte[] configImage1, byte[] configImage2, DeviceContext device) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDomainDifferences"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		// new Object[]{domainName, configImage1, configImage2, device});

		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "getDomainDifferences is not implemented!!!"); //$NON-NLS-1$

		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return null;
	}

	/*
	 * @throws InvalidParameterException
	 */
	public Hashtable<String, byte[]> backupDevice(DeviceContext device, String cryptoCertificateName, byte[] cryptoImage, String secureBackupDestination, boolean includeISCSI, boolean includeRaid)
			throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "backupDevice"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME);

		// The crypto object name or the crypto certificate location must be
		// specified,both cannot be specified
		if ((cryptoCertificateName != null) && (cryptoImage != null)) {
			logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "Specify the Crypto Object name or the crypto certificates location. Both may not be non-null");
		}
		// The crypto object name or the crypto certificate location must be
		// specified,both cannot be null
		if ((cryptoCertificateName == null) && (cryptoImage == null)) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Specify the Crypto Object name or the crypto certificates location. Both may not be null");
		}

		List<SecureBackupFile> returnedBackupFilesList = null;
		Hashtable<String, byte[]> backupHashTable = new Hashtable<String, byte[]>();
		SecureBackupRequestDocument requestDoc = SecureBackupRequestDocument.Factory.newInstance();

		SecureBackupRequestDocument.SecureBackupRequest request = requestDoc.addNewSecureBackupRequest();

		// Remove option to backup ISCSI, if caller specifies a value of false.
		// It is ON by default.
		if (!includeISCSI) {
			XmlObject scsi = request.addNewDoNotIncludeiSCSI();
		}
		// Remove option to backup RAID, if caller specifies a value of false.
		// It is ON by default.
		if (!includeRaid) {
			XmlObject raid = request.addNewDoNotIncludeRAID();
		}

		// Device.backupDevice() ensures that only one of the 2 is specified -
		// cryptoObjectName or cryptoCertificate.
		// Both will not be null
		if (cryptoCertificateName != null) {
			request.setCryptoCertificateName(cryptoCertificateName);
		}

		if (cryptoImage != null) {
			request.setCryptoCertificate(cryptoImage);
		}

		if (secureBackupDestination != null) {
			request.setSecureBackupDestination(secureBackupDestination);
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "backupRequest request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending backup request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$  

		/* Send request to device */

		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));

		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			SecureBackupResponseDocument responseDoc = SecureBackupResponseDocument.Factory.parse(responseDocXml);
			SecureBackupResponseDocument.SecureBackupResponse backupResponse = responseDoc.getSecureBackupResponse();

			if (backupResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.backupNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = backupResponse.getStatus();
			String status = backupResponse.getStatus();
			SecureBackup backup = backupResponse.getSecureBackup();

			// By design when files are returned by backup, the status is NULL
			if (secureBackupDestination != null) {
				if (status == null) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupNoStat", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.backupNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
				// else if (status.equals(Status.ERROR)){
				else if (status.equalsIgnoreCase("ERROR")) {
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupFail", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.backupFail", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}

			}

			// Retrieve backup files when secureBackupDestination specified is
			// null.
			// Backup files are expected to be returned when
			// secureBackupDestination is null
			if (secureBackupDestination == null) {
				if (backup == null) {
					// backup files not returned as expected!
					Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupFail", params);
					DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.backupFail", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				} else {
					returnedBackupFilesList = backup.getSecureBackupFileList();

					if (returnedBackupFilesList != null) {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Number of backup files returned = " + returnedBackupFilesList.size());
						java.util.Iterator<SecureBackupFile> it = returnedBackupFilesList.iterator();
						while (it.hasNext()) {
							SecureBackupFile file = (SecureBackupFile) it.next();
							backupHashTable.put(file.getName(), file.getStringValue().getBytes());
						}
					}
				}
			}

		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.backupErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.backupInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.backupInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}

		return backupHashTable;
	}

	public void restoreDevice(DeviceContext device, String cryptoCredentialName, boolean validate, URI secureBackupSource, Hashtable<String, byte[]> backupFilesTable)
			throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "restoreDevice"; //$NON-NLS-1$

		SecureRestoreRequestDocument requestDoc = SecureRestoreRequestDocument.Factory.newInstance();

		SecureRestoreRequestDocument.SecureRestoreRequest request = requestDoc.addNewSecureRestoreRequest();

		if (cryptoCredentialName != null) {
			request.setCryptoCredentialName(cryptoCredentialName);
		}

		if (secureBackupSource != null) {
			if (secureBackupSource.getScheme().equalsIgnoreCase("file")) {
				SecureBackup backupFiles = readBackUpFiles(backupFilesTable);
				request.setSecureBackup(backupFiles);

			} else {
				request.setSecureBackupSource(secureBackupSource.toString());
			}
		}

		if (validate) {
			request.addNewValidate();
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "backupRequest request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending backup request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$  

		/* Send request to device */

		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));

		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			SecureRestoreResponseDocument responseDoc = SecureRestoreResponseDocument.Factory.parse(responseDocXml);
			SecureRestoreResponseDocument.SecureRestoreResponse restoreResponse = responseDoc.getSecureRestoreResponse();

			if (restoreResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.restoreNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = restoreResponse.getStatus();
			String status = restoreResponse.getStatus();

			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.restoreNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else if (status.equals(Status.ERROR)){
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.restoreFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.restoreErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.restoreInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.restoreInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}

		return;
	}

	public void quiesceDomain(DeviceContext device, String domain, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "quiesceDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		QuiesceRequestDocument requestDoc = QuiesceRequestDocument.Factory.newInstance();

		QuiesceRequestDocument.QuiesceRequest request = requestDoc.addNewQuiesceRequest();

		QuiesceRequestDocument.QuiesceRequest.Domain requestDomain = request.addNewDomain();
		requestDomain.setName(domain);
		requestDomain.setTimeout(timeout);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "quiesce request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending quiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			QuiesceResponseDocument responseDoc = QuiesceResponseDocument.Factory.parse(responseDocXml);
			QuiesceResponseDocument.QuiesceResponse quiesceResponse = responseDoc.getQuiesceResponse();

			if (quiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = quiesceResponse.getStatus();
			String status = quiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else if (status.equals(Status.ERROR)){
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else {
			// Object[] params = {device.getHostname(),new
			// Integer(device.getAMPPort())};
			// String message =
			// Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceOkStat",params);
			//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.quiesceOkStat",params); //$NON-NLS-1$ //$NON-NLS-2$
			// logger.throwing(CLASS_NAME, METHOD_NAME, e);
			// throw e;
			// }
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	public void unquiesceDomain(DeviceContext device, String domain) throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "unquiesceDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		UnquiesceRequestDocument requestDoc = UnquiesceRequestDocument.Factory.newInstance();

		UnquiesceRequestDocument.UnquiesceRequest request = requestDoc.addNewUnquiesceRequest();

		UnquiesceRequestDocument.UnquiesceRequest.Domain requestDomain = request.addNewDomain();
		requestDomain.setName(domain);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "unquiesce request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending unquiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			UnquiesceResponseDocument responseDoc = UnquiesceResponseDocument.Factory.parse(responseDocXml);
			UnquiesceResponseDocument.UnquiesceResponse unquiesceResponse = responseDoc.getUnquiesceResponse();

			if (unquiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = quiesceResponse.getStatus();
			String status = unquiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else if (status.equals(Status.ERROR)){
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else {
			// Object[] params = {device.getHostname(),new
			// Integer(device.getAMPPort())};
			// String message =
			// Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceOkStat",params);
			//                AMPException e = new AMPException(message,"wamt.amp.defaultV2Provider.CommandsImpl.unquiesceOkStat",params); //$NON-NLS-1$ //$NON-NLS-2$
			// logger.throwing(CLASS_NAME, METHOD_NAME, e);
			// throw e;
			// }
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	public void quiesceDevice(DeviceContext device, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "quiesceDevice"; //$NON-NLS-1$

		//
		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "quiesceDevice is not implemented in defaultProvider!"); //$NON-NLS-1$

		QuiesceRequestDocument requestDoc = QuiesceRequestDocument.Factory.newInstance();
		QuiesceRequestDocument.QuiesceRequest request = requestDoc.addNewQuiesceRequest();
		QuiesceRequestDocument.QuiesceRequest.Device requestDevice = request.addNewDevice();
		requestDevice.setTimeout(timeout);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "quiesce request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending quiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			QuiesceResponseDocument responseDoc = QuiesceResponseDocument.Factory.parse(responseDocXml);
			QuiesceResponseDocument.QuiesceResponse quiesceResponse = responseDoc.getQuiesceResponse();

			if (quiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = quiesceResponse.getStatus();
			String status = quiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else if (status.equals(Status.ERROR)){
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	public void unquiesceDevice(DeviceContext device) throws AMPIOException, InvalidCredentialsException, AMPException {
		final String METHOD_NAME = "unquiesceDevice"; //$NON-NLS-1$    	
		// Implemented in wamt.amp.defaultV2Provider
		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "unquiesceDevice is not implemented in defaultV2Provider!"); //$NON-NLS-1$

		UnquiesceRequestDocument requestDoc = UnquiesceRequestDocument.Factory.newInstance();
		UnquiesceRequestDocument.UnquiesceRequest request = requestDoc.addNewUnquiesceRequest();
		request.addNewDevice();

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "unquiesce request created"); //$NON-NLS-1$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending unquiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			UnquiesceResponseDocument responseDoc = UnquiesceResponseDocument.Factory.parse(responseDocXml);
			UnquiesceResponseDocument.UnquiesceResponse unquiesceResponse = responseDoc.getUnquiesceResponse();

			if (unquiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = unquiesceResponse.getStatus();
			String status = unquiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			// else if (status.equals(Status.ERROR)){
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * Load the encrypted content from files that were previously saved from a
	 * Secure Backup call. This method is called from
	 * 
	 * @param backupFilesTable this is a Hashtable whose keys are file names and
	 * the values are the corresponding file content. The information from the
	 * table will be loaded into
	 * com.datapower.schemas.appliance.management.x30.SecureBackup and sent by
	 * AMP to the device on a Secure Restore request.
	 * 
	 * @return SecureBackup contains multiple SecureBackupFile objects - one for
	 * each file used during Secure Restore
	 * 
	 * @see #restoreDevice
	 */
	private SecureBackup readBackUpFiles(Hashtable<String, byte[]> backupFilesTable) {
		final String METHOD_NAME = "writeToBackUpFile";

		if (backupFilesTable == null) {
			return null;
		}

		SecureBackupFile sbf = null;
		SecureBackup sb = SecureBackup.Factory.newInstance();
		Enumeration<String> fileNames = backupFilesTable.keys();

		while (fileNames.hasMoreElements()) {
			String fileName = (String) fileNames.nextElement();
			byte[] fileContent = (byte[]) backupFilesTable.get(fileName);

			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Adding backup File: " + fileName);

			try {
				sbf = sb.addNewSecureBackupFile();
				sbf.setName(fileName);
				sbf.setStringValue(new String(fileContent));

			} catch (Exception e) {
				logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "Exception thrown:", e);
				// e.printStackTrace();
			}
		}

		return sb;
	}
	
	// =========================== New functions for provider V3 ===========================
	/*
	 * @see com.ibm.datapower.amt.amp.Commands#getServiceListFromDomain(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String)
	 */
	public RuntimeService[] getServiceListFromDomain(DeviceContext device, String domainName) 
		throws DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getServiceListFromDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		GetServiceListFromDomainRequestDocument requestDoc = GetServiceListFromDomainRequestDocument.Factory.newInstance();
		GetServiceListFromDomainRequestDocument.GetServiceListFromDomainRequest getServiceListrequest =
			requestDoc.addNewGetServiceListFromDomainRequest();

		getServiceListrequest.setDomain(domainName);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getServiceListFromDomainRequest created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getServiceListFromDomainRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a GetServiceListFromDomainResponse object */
		try {
			GetServiceListFromDomainResponseDocument responseDoc = GetServiceListFromDomainResponseDocument.Factory.parse(responseDocXml);
			GetServiceListFromDomainResponseDocument.GetServiceListFromDomainResponse getGetServiceListResponse = responseDoc.getGetServiceListFromDomainResponse();

			if (getGetServiceListResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromDomainNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromDomainNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			String status = getGetServiceListResponse.getStatus();
			if ( status != null && !status.equalsIgnoreCase("OK")) { // If return error
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromDomainFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromDomainFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
						
			DetailedConfigObjects configObjects = getGetServiceListResponse.getServices();
			List<DetailedConfigObject> objectArray = configObjects.getObjectList();
			int iObjectSize = objectArray.size();
			RuntimeService[] result = null; // Create ConfigObjectStatus

			if (iObjectSize != 0) {
				result = new RuntimeService[iObjectSize];
				for (int i = 0; i < iObjectSize; i++) {										
					DetailedConfigObject object = objectArray.get(i);
					
					String serviceName = object.getName();
					String className = object.getClassName();
					String displayName = object.getClassDisplayName();
					String userComments = object.getUserComments();
					
					// == Get Admin state ==
					AdminState.Enum adminState = object.getAdminState();
					com.ibm.datapower.amt.AdminStatus adminEnabled;
					if (adminState.toString().equalsIgnoreCase("enabled")) //$NON-NLS-1$
						adminEnabled = com.ibm.datapower.amt.AdminStatus.ENABLED;
					else if (adminState.toString().equalsIgnoreCase("disabled")) //$NON-NLS-1$
						adminEnabled = com.ibm.datapower.amt.AdminStatus.DISABLED;
					else {
						Object[] params = { adminState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
						String message = Messages.getString("wamt.amp.defaultV3Provider.invalidAdminStat.invalidConfigStat", params);
						AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.invalidAdminStat", params); //$NON-NLS-1$ //$NON-NLS-2$
						logger.throwing(CLASS_NAME, METHOD_NAME, e);
						throw e;
					}
					
					// == Get operation state ==
					OpState.Enum opState = object.getOpState();					
					if (opState == null) {
						Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
						String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params);
						AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params); //$NON-NLS-1$ //$NON-NLS-2$
						logger.throwing(CLASS_NAME, METHOD_NAME, e);
						throw e;
					}					
					com.ibm.datapower.amt.OpStatus opStatusUp;
					if (opState.toString().equalsIgnoreCase("up")) //$NON-NLS-1$
						opStatusUp = com.ibm.datapower.amt.OpStatus.UP;
					else if (opState.toString().equalsIgnoreCase("down")) //$NON-NLS-1$
						opStatusUp = com.ibm.datapower.amt.OpStatus.DOWN;
					else {
						Object[] params = { opState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
						String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params);
						AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params); //$NON-NLS-1$ //$NON-NLS-2$
						logger.throwing(CLASS_NAME, METHOD_NAME, e);
						throw e;
					}
					
					// Get Config State
					ConfigState.Enum configState = object.getConfigState();
					if (configState == null) {
						Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
						String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params);
						AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
						logger.throwing(CLASS_NAME, METHOD_NAME, e);
						throw e;
					}
					boolean needsSave;
					if (configState.toString().equalsIgnoreCase("modified")) //$NON-NLS-1$
						needsSave = true;
					else if (configState.toString().equalsIgnoreCase("saved")) //$NON-NLS-1$
						needsSave = false;
					else {
						Object[] params = { configState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
						String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params);
						AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
						logger.throwing(CLASS_NAME, METHOD_NAME, e);
						throw e;
					}
					
					// Get Quiesce State
					String quiesceState = object.getQuiesceState();
					QuiesceStatus quiesceStatus = QuiesceStatus.UNKNOWN; // default
					if (quiesceState.equals(""))
						quiesceStatus = QuiesceStatus.NORMAL;
					else if (quiesceState.equals("quiescing"))
						quiesceStatus = QuiesceStatus.QUIESCE_IN_PROGRESS;
					else if (quiesceState.equals("quiesced"))
						quiesceStatus = QuiesceStatus.QUIESCED;
					else if (quiesceState.equals("unquiescing"))
						quiesceStatus = QuiesceStatus.UNQUIESCE_IN_PROGRESS;
					else if (quiesceState.equals("error"))
						quiesceStatus = QuiesceStatus.ERROR;
					
					result[i] = new RuntimeService(serviceName, className, displayName, userComments, 
									adminEnabled, opStatusUp, needsSave, quiesceStatus);
				}
			}
			logger.exiting(CLASS_NAME, METHOD_NAME, result);
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDomainGetDomList", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumGetDomList", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * called by getInterDependentServices
	 */
	private InterDependentServiceCollection getInterDependentServices_core(DeviceContext device, String domainName, GetInterDependentServicesRequestDocument requestDoc,
			GetInterDependentServicesRequestDocument.GetInterDependentServicesRequest getInterDependentServicesRequest,
			com.ibm.datapower.amt.amp.ConfigObject[] objects) throws DeviceExecutionException, AMPIOException, AMPException {
		
		final String METHOD_NAME = "getInterDependentServices_core"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		ConfigObjects configObjects = getInterDependentServicesRequest.addNewServices();
		if ( objects != null ) {
			int iServiceNum = objects.length;
			ConfigObject[] configObj = new ConfigObject[iServiceNum];
			for (int i = 0; i < iServiceNum; i++) {				
				configObj[i] = ConfigObject.Factory.newInstance();
				if ( objects[i] != null ) { // in case something wrong in object array
					configObj[i].setName(objects[i].getName());
					configObj[i].setClassName(objects[i].getClassName());
				}
			}
			configObjects.setObjectArray(configObj);
		}
	
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getInterDependentServicesRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getInterDependentServicesRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	
		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);
	
		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;
	
		/* Parse the request into a getInterDependentServicesResponse object */
		try {
			GetInterDependentServicesResponseDocument responseDoc = GetInterDependentServicesResponseDocument.Factory.parse(responseDocXml);
			GetInterDependentServicesResponseDocument.GetInterDependentServicesResponse getGetInterDependentServicesResponse = responseDoc.getGetInterDependentServicesResponse();
	
			if (getGetInterDependentServicesResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			// Use as a string to allow different error return codes.			
			String status = getGetInterDependentServicesResponse.getStatus();
			if ( status != null && !status.equalsIgnoreCase("OK")) { // If return error
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			DetailedConfigObjects datailedConfigObjects = getGetInterDependentServicesResponse.getServices();
			if (datailedConfigObjects == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesFail", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getInterDependentServicesFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			
			ArrayList<RuntimeService> serviceList = null;
			List<DetailedConfigObject> detailedConfigObjectArray = datailedConfigObjects.getObjectList();
			if (detailedConfigObjectArray != null ) { // Get Inter dependent configobject
				serviceList = new ArrayList<RuntimeService>();
				int iObjectSize = detailedConfigObjectArray.size();
				if (iObjectSize != 0) {
					for (int i = 0; i < detailedConfigObjectArray.size(); i++) {
						DetailedConfigObject detailObject = detailedConfigObjectArray.get(i);
						String serviceName = detailObject.getName();
						String className = detailObject.getClassName();
						String displayName = detailObject.getClassDisplayName();
						String userComments = detailObject.getUserComments();
											
						// == Get Admin state ==
						AdminState.Enum adminState = detailObject.getAdminState();
						com.ibm.datapower.amt.AdminStatus adminEnabled;
						if (adminState.toString().equalsIgnoreCase("enabled")) //$NON-NLS-1$
							adminEnabled = com.ibm.datapower.amt.AdminStatus.ENABLED;
						else if (adminState.toString().equalsIgnoreCase("disabled")) //$NON-NLS-1$
							adminEnabled = com.ibm.datapower.amt.AdminStatus.DISABLED;
						else {
							Object[] params = { adminState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
							String message = Messages.getString("wamt.amp.defaultV3Provider.invalidAdminStat.invalidConfigStat", params);
							AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.invalidAdminStat", params); //$NON-NLS-1$ //$NON-NLS-2$
							logger.throwing(CLASS_NAME, METHOD_NAME, e);
							throw e;
						}
						
						// == Get operation state ==
						OpState.Enum opState = detailObject.getOpState();					
						if (opState == null) {
							Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
							String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params);
							AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.opnStatNotSpec", params); //$NON-NLS-1$ //$NON-NLS-2$
							logger.throwing(CLASS_NAME, METHOD_NAME, e);
							throw e;
						}					
						com.ibm.datapower.amt.OpStatus opStatusUp;
						if (opState.toString().equalsIgnoreCase("up")) //$NON-NLS-1$
							opStatusUp = com.ibm.datapower.amt.OpStatus.UP;
						else if (opState.toString().equalsIgnoreCase("down")) //$NON-NLS-1$
							opStatusUp = com.ibm.datapower.amt.OpStatus.DOWN;
						else {
							Object[] params = { opState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
							String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params);
							AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidOpnStatGet", params); //$NON-NLS-1$ //$NON-NLS-2$
							logger.throwing(CLASS_NAME, METHOD_NAME, e);
							throw e;
						}
						
						// Get Config State
						ConfigState.Enum configState = detailObject.getConfigState();
						if (configState == null) {
							Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
							String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params);
							AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
							logger.throwing(CLASS_NAME, METHOD_NAME, e);
							throw e;
						}
	
						boolean needsSave;
						if (configState.toString().equalsIgnoreCase("modified")) //$NON-NLS-1$
							needsSave = true;
						else if (configState.toString().equalsIgnoreCase("saved")) //$NON-NLS-1$
							needsSave = false;
						else {
							Object[] params = { configState.toString(), device.getHostname(), Integer.toString(device.getAMPPort()) };
							String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params);
							AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.invalidConfigStat", params); //$NON-NLS-1$ //$NON-NLS-2$
							logger.throwing(CLASS_NAME, METHOD_NAME, e);
							throw e;
						}
						
						// Get Quiesce State
						String quiesceState = detailObject.getQuiesceState();
						QuiesceStatus quiesceStatus = QuiesceStatus.UNKNOWN; // default
						if (quiesceState.equals(""))
							quiesceStatus = QuiesceStatus.NORMAL;
						else if (quiesceState.equals("quiescing"))
							quiesceStatus = QuiesceStatus.QUIESCE_IN_PROGRESS;
						else if (quiesceState.equals("quiesced"))
							quiesceStatus = QuiesceStatus.QUIESCED;
						else if (quiesceState.equals("unquiescing"))
							quiesceStatus = QuiesceStatus.UNQUIESCE_IN_PROGRESS;
						else if (quiesceState.equals("error"))
							quiesceStatus = QuiesceStatus.ERROR;					
		
						RuntimeService service = 
								new RuntimeService(serviceName, className, displayName, userComments,
								adminEnabled, opStatusUp, needsSave, quiesceStatus);
						serviceList.add(service);
					}
				}
			}
	
			// Get config objects to be overwritten
			ArrayList<com.ibm.datapower.amt.amp.ConfigObject> configObjectOverwritten = null;
			DetailedConfigObjects objectsTobeOverwritten = getGetInterDependentServicesResponse.getObjectsToBeOverwritten();
			List<DetailedConfigObject> objectsTobeOverwrittenList = objectsTobeOverwritten.getObjectList();
			if (objectsTobeOverwrittenList != null) {
				configObjectOverwritten = new ArrayList<com.ibm.datapower.amt.amp.ConfigObject>();
				int iObjectSize = objectsTobeOverwrittenList.size();			
				for (int i = 0; i < iObjectSize; i++) {
					String serviceName = objectsTobeOverwrittenList.get(i).getName();
					String className = objectsTobeOverwrittenList.get(i).getClassName();
					String displayName = objectsTobeOverwrittenList.get(i).getClassDisplayName();
					
					configObjectOverwritten.add(new com.ibm.datapower.amt.amp.ConfigObject(serviceName, className, displayName,""));
				}
			}
	
			// for file to be overwritten
			StringCollection fileCollection = null;
			FilesToBeOverwritten filesToBeOverwritten = getGetInterDependentServicesResponse.getFilesToBeOverwritten();
			List<String> fileList = filesToBeOverwritten.getFileList();
			if ( fileList != null ) {
				fileCollection = new StringCollection();
				int iSize = filesToBeOverwritten.sizeOfFileArray();
				for (int i = 0; i < iSize; i++) {
					fileCollection.add(fileList.get(i));
				}
			}
	
			return new InterDependentServiceCollection(serviceList, configObjectOverwritten, fileCollection);
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#getInterDependentServices(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext[])
	 */
	public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, 
													String fileDomain, String fileNameOnDevice, com.ibm.datapower.amt.amp.ConfigObject[] objects)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getInterDependentServices"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		GetInterDependentServicesRequestDocument requestDoc = GetInterDependentServicesRequestDocument.Factory.newInstance();
		GetInterDependentServicesRequestDocument.GetInterDependentServicesRequest getInterDependentServicesRequest = requestDoc.addNewGetInterDependentServicesRequest();

		BackupFile backupFile = getInterDependentServicesRequest.addNewConfigFile();
		backupFile.setDomain(domainName);

		FileLocation fileLocation = FileLocation.Factory.newInstance();
		fileLocation.setDomain(fileDomain);
		// find location
		int iLoc = fileNameOnDevice.lastIndexOf("/");
		String sLocation = fileNameOnDevice.substring(0, iLoc+1);		
		fileLocation.setLocation(sLocation);
		// get file name
		String sFileName = fileNameOnDevice.substring(iLoc+1);
		fileLocation.setName(sFileName);

		backupFile.setFile(fileLocation);
		getInterDependentServicesRequest.setConfigFile(backupFile);

		return getInterDependentServices_core(device, domainName, requestDoc, getInterDependentServicesRequest, objects);		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#getInterDependentServices(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, byte[], com.ibm.datapower.amt.amp.ObjectContext[])
	 */
	public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, byte[] packageImage, 
			com.ibm.datapower.amt.amp.ConfigObject[] objects)
		throws NotExistException, InvalidCredentialsException,DeviceExecutionException, AMPIOException, AMPException {
		
		final String METHOD_NAME = "getInterDependentServices"; //$NON-NLS-1$
		
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		GetInterDependentServicesRequestDocument requestDoc = GetInterDependentServicesRequestDocument.Factory.newInstance();
		GetInterDependentServicesRequestDocument.GetInterDependentServicesRequest getInterDependentServicesRequest = requestDoc.addNewGetInterDependentServicesRequest();
				
		Backup backup = getInterDependentServicesRequest.addNewConfig();
		backup.setDomain(domainName);
		backup.setStringValue(new String(packageImage));		
		getInterDependentServicesRequest.setConfig(backup);
		
		return getInterDependentServices_core(device, domainName, requestDoc, getInterDependentServicesRequest, objects);
	}
	
	/*
	 *  called by getServiceListFromExport
	 */
	private ConfigService[] getServiceListFromExport_core(DeviceContext device, GetServiceListFromExportRequestDocument requestDoc,
			GetServiceListFromExportRequestDocument.GetServiceListFromExportRequest GetServiceListFromExportRequest)
		throws InvalidCredentialsException,	DeviceExecutionException, AMPIOException, AMPException {
		
		final String METHOD_NAME = "getServiceListFromExport_core"; //$NON-NLS-1$
		
		logger.entering(CLASS_NAME, METHOD_NAME);

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);
	
		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;
	
		/* Parse the request into a SetDomainExportResponse object */
		try {
			GetServiceListFromExportResponseDocument responseDoc = GetServiceListFromExportResponseDocument.Factory.parse(responseDocXml);
			GetServiceListFromExportResponseDocument.GetServiceListFromExportResponse getGetServiceListFromExportResponse = responseDoc.getGetServiceListFromExportResponse();
	
			if (getGetServiceListFromExportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			// Use as a string to allow different error return codes.			
			String status = getGetServiceListFromExportResponse.getStatus();
			if ( status != null && !status.equalsIgnoreCase("OK")) { // If return error
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			// Use as a string to allow different error return codes.
			DetailedConfigObjects datailedConfigObjects = getGetServiceListFromExportResponse.getServices();
			if (datailedConfigObjects == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportFail", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getServiceListFromExportFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			List<DetailedConfigObject> detailedConfigObjectArray = datailedConfigObjects.getObjectList();
			ConfigService[] result = null;
	
			int iObjectSize = detailedConfigObjectArray.size();
			if (iObjectSize != 0) {
				result = new ConfigService[iObjectSize];
				for (int i = 0; i < iObjectSize; i++) {
					String serviceName = detailedConfigObjectArray.get(i).getName();
					String className = detailedConfigObjectArray.get(i).getClassName();
					String displayName = detailedConfigObjectArray.get(i).getClassDisplayName();
					String userComments = detailedConfigObjectArray.get(i).getUserComments();
	
					result[i] = new ConfigService(serviceName, className, displayName, userComments);
				}
			}
	
			return result;
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#getServiceListFromExport(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String)
	 */
	public ConfigService[] getServiceListFromExport(DeviceContext device, String fileDomainName, String fileNameOnDevice) 
		throws InvalidCredentialsException,	DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getServiceListFromExport"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		GetServiceListFromExportRequestDocument requestDoc = GetServiceListFromExportRequestDocument.Factory.newInstance();
		GetServiceListFromExportRequestDocument.GetServiceListFromExportRequest GetServiceListFromExportRequest = requestDoc.addNewGetServiceListFromExportRequest();

		// Backup image
		BackupFile backupFile = GetServiceListFromExportRequest.addNewConfigFile();
		backupFile.setDomain(fileDomainName);
		//
		FileLocation fileLocation = FileLocation.Factory.newInstance();
		fileLocation.setDomain(fileDomainName);
		// find location
		int iLoc = fileNameOnDevice.lastIndexOf("/");
		String sLocation = fileNameOnDevice.substring(0, iLoc+1);		
		fileLocation.setLocation(sLocation);
		// get file name
		String sFileName = fileNameOnDevice.substring(iLoc+1);
		fileLocation.setName(sFileName);
		backupFile.setFile(fileLocation);
		GetServiceListFromExportRequest.setConfigFile(backupFile);
		
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getServiceListFromExportRequest(" + fileDomainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getServiceListFromExportRequest(" + fileDomainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		return getServiceListFromExport_core(device, requestDoc, GetServiceListFromExportRequest);		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#getServiceListFromExport(com.ibm.datapower.amt.amp.DeviceContext, byte[])
	 */
	public ConfigService[] getServiceListFromExport(DeviceContext device, byte[] packageImage) 
		throws InvalidCredentialsException,	DeviceExecutionException, AMPIOException, AMPException {
		
		final String METHOD_NAME = "getServiceListFromExport"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		GetServiceListFromExportRequestDocument requestDoc = GetServiceListFromExportRequestDocument.Factory.newInstance();
		GetServiceListFromExportRequestDocument.GetServiceListFromExportRequest GetServiceListFromExportRequest = requestDoc.addNewGetServiceListFromExportRequest();

		Backup backup = GetServiceListFromExportRequest.addNewConfig();
		backup.setDomain("default"); // can be any string, just use the default
		backup.setStringValue(new String(packageImage));
		GetServiceListFromExportRequest.setConfig(backup);
		
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "getServiceListFromExportRequest for byte array is created"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending getServiceListFromExportRequest for byte array to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		return getServiceListFromExport_core(device, requestDoc, GetServiceListFromExportRequest);
	}

	/*
	 * @see com.ibm.datapower.amt.amp.Commands#getGetReferencedObjects(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String)
	 */
	public ReferencedObjectCollection getReferencedObjects(DeviceContext device, String domainName, String objectName, String objectClassName)
		throws InvalidCredentialsException,	DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setDomain"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);

		GetReferencedObjectsRequestDocument requestDoc = GetReferencedObjectsRequestDocument.Factory.newInstance();
		GetReferencedObjectsRequestDocument.GetReferencedObjectsRequest GetReferencedObjectsRequest = requestDoc.addNewGetReferencedObjectsRequest();

		GetReferencedObjectsRequest.setDomain(domainName);
		GetReferencedObjectsRequest.setObjectClass(objectClassName);
		GetReferencedObjectsRequest.setObjectName(objectName);

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a SetDomainExportResponse object */
		try {
			GetReferencedObjectsResponseDocument responseDoc = GetReferencedObjectsResponseDocument.Factory.parse(responseDocXml);
			GetReferencedObjectsResponseDocument.GetReferencedObjectsResponse getGetReferencedObjectsResponse = responseDoc.getGetReferencedObjectsResponse();

			if (getGetReferencedObjectsResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			// Use as a string to allow different error return codes.			
			String status = getGetReferencedObjectsResponse.getStatus();
			if ( status != null && !status.equalsIgnoreCase("OK")) { // If return error
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Use as a string to allow different error return codes.
			DetailedConfigObjects datailedConfigObjects = getGetReferencedObjectsResponse.getObjects();
			if (datailedConfigObjects == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.getReferencedObjectsFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			List<DetailedConfigObject> detailedConfigObjectList = datailedConfigObjects.getObjectList();
			// If there is no referenced object, but is file list
			if (detailedConfigObjectList == null) {// no referenced object  
				// check if there is file list
				Files files = getGetReferencedObjectsResponse.getFiles();
				StringCollection fileCollection = null;
				if (files != null) {
					List<String> fileList = files.getFileList();
					int iSize = fileList.size();
					fileCollection = new StringCollection();
					for (int i = 0; i < iSize; i++) {
						String fileName = fileList.get(i).toString();
						fileCollection.add(fileName);
					}
					return new ReferencedObjectCollection(null, fileCollection, objectName+":"+objectClassName); 
				}
				return null; // nor reference object or file list, return null
			}
			
			// Set referencedObjectTable Table			
			int iObjectSize = detailedConfigObjectList.size();		
			
			// HashMap Key is the name:className of ConfigObject, Value are the referenced Objects
			HashMap<String, ArrayList<com.ibm.datapower.amt.amp.ConfigObject>> referencedObjectTable = 
				new HashMap<String, ArrayList<com.ibm.datapower.amt.amp.ConfigObject>>();

			if (iObjectSize != 0) {
				// First, create the ConfigObject Map Table <String, ConfigObject> to save all ConfigObject
				// key is the name:className of ConfigObject, value is the ConfigObject object 
				HashMap<String, com.ibm.datapower.amt.amp.ConfigObject> referencingObjectTable = 
					new HashMap<String, com.ibm.datapower.amt.amp.ConfigObject>();
				for (int i = 0; i < iObjectSize; i++) {					
					DetailedConfigObject detailedConfigObject = detailedConfigObjectList.get(i);
					// Get Referencing object data
					String name_tmp = detailedConfigObject.getName();
					String className_tmp = detailedConfigObject.getClassName();
					// New a ConfigObject
					com.ibm.datapower.amt.amp.ConfigObject configObject =
				    	new com.ibm.datapower.amt.amp.ConfigObject( name_tmp, className_tmp, detailedConfigObject.getClassDisplayName(), 
				    			detailedConfigObject.getUserComments(), null, detailedConfigObjectList.get(i).getReferencedExternally() );
					// Push to the Map table
					referencingObjectTable.put(name_tmp+":"+className_tmp, configObject);					
				}
				
				// Second, create a Hashmap <Name+":"+className, ArrayList of referenced object> to save the 
				// ConfigObject(key) and all referenced ConfigObjects(value, is a array list) which are referenced by ConfigObject(key)
				for (int i = 0; i < iObjectSize; i++) {
					DetailedConfigObject detailedConfigObject = detailedConfigObjectList.get(i);					
					// Get the referenced object					
					List<ConfigObject> referencedObjects = detailedConfigObject.getReferencedObjectList();
					ArrayList<com.ibm.datapower.amt.amp.ConfigObject> tmp_list = null;
					if ( referencedObjects != null ) {
						int iReferencedSize = referencedObjects.size();
						if (  iReferencedSize > 0 ) {// Got referenced object
							tmp_list = new ArrayList<com.ibm.datapower.amt.amp.ConfigObject>();
							for ( int j=0; j < iReferencedSize; j++ ) {
								ConfigObject configObject = referencedObjects.get(j);
								// Get ConfigObject Key
								String referenced_key = configObject.getName()+":"+configObject.getClassName();
								// Add to ArrayList
								com.ibm.datapower.amt.amp.ConfigObject refConfigObject = referencingObjectTable.get(referenced_key);
								// Fix for PMR13749, the referencing object could be null
								if ( refConfigObject != null )
									tmp_list.add(refConfigObject);
								else
									logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
											"Unable to find the referencing Object of key: " + referenced_key); //$NON-NLS-1$
							}
						}
						// Add to referencedObjectTable
						referencedObjectTable.put(detailedConfigObject.getName()+":"+detailedConfigObject.getClassName(), tmp_list);
					}
				}				
			}
				
			// Set the linkage between each configobject
	        Set priKey = referencedObjectTable.entrySet();
	    	Iterator iterator = priKey.iterator();
	        while (iterator.hasNext()) {
	    		// For each key, get it's referenced objects (arrayList),
	        	Map.Entry entry = (Map.Entry) iterator.next();	        	
	    		ArrayList<com.ibm.datapower.amt.amp.ConfigObject> objectList = (ArrayList<com.ibm.datapower.amt.amp.ConfigObject>)entry.getValue();
	    		if ( objectList != null ) {
		    		// Set the referenced linkage
	    			for ( com.ibm.datapower.amt.amp.ConfigObject co: objectList){	
	    				if ( co != null ) {
	    					String ss = co.getPrimaryKey();
			    			ArrayList<com.ibm.datapower.amt.amp.ConfigObject> tmpList = referencedObjectTable.get(co.getPrimaryKey());
			    			if ( tmpList != null ) {
				    			// Set referenced object
			    				com.ibm.datapower.amt.amp.ConfigObject[] tmp = new com.ibm.datapower.amt.amp.ConfigObject[tmpList.size()];		    				
				    			for ( int j=0; j < tmpList.size(); j++ ) {
				    				tmp[j] = tmpList.get(j);
				    			}
				    			co.setReferencedObject(tmp);
			    			}
	    				}
		    		}	    			
	    		}
	    	}
	        // Set file list			
			Files files = getGetReferencedObjectsResponse.getFiles();
			StringCollection fileCollection = null;
			if (files != null) {
				List<String> fileList = files.getFileList();
				int iSize = fileList.size();
				fileCollection = new StringCollection();
				for (int i = 0; i < iSize; i++) {
					String fileName = fileList.get(i).toString();
					fileCollection.add(fileName);
				}
			}
			
			return new ReferencedObjectCollection(referencedObjectTable, fileCollection, objectName+":"+objectClassName);
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#deleteService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, java.lang.String, java.lang.String, com.ibm.datapower.amt.amp.ConfigObject[], boolean)
	 */
	public DeleteObjectResult[] deleteService(DeviceContext device, String domainName, String objectName, String objectClassName, 
			com.ibm.datapower.amt.amp.ConfigObject[] excludeObjects, boolean deleteReferencedFiles)
			throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "deleteService"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, domainName });

		DeleteServiceRequestDocument requestDoc = DeleteServiceRequestDocument.Factory.newInstance();
		DeleteServiceRequestDocument.DeleteServiceRequest deleteServiceRequest = requestDoc.addNewDeleteServiceRequest();

		deleteServiceRequest.setDomain(domainName);
		deleteServiceRequest.setServiceObjectName(objectName);
		deleteServiceRequest.setServiceObjectClass(objectClassName);
		if (excludeObjects != null) {
			ConfigObjects configObjects = deleteServiceRequest.addNewObjectExclusionList();
			for (int i = 0; i < excludeObjects.length; i++) {
				ConfigObject configObject = configObjects.addNewObject();
				configObject.setName(excludeObjects[i].getName());
				configObject.setClassName(excludeObjects[i].getClassName());
			}
			deleteServiceRequest.setObjectExclusionList(configObjects);
		}
		deleteServiceRequest.setDeleteReferencedFiles(deleteReferencedFiles);

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "deleteServiceRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending deleteServiceRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a DeleteDomainResponse object */
		try {
			DeleteServiceResponseDocument responseDoc = DeleteServiceResponseDocument.Factory.parse(responseDocXml);
			DeleteServiceResponseDocument.DeleteServiceResponse getDeleteServiceResponse = responseDoc.getDeleteServiceResponse();

			if (getDeleteServiceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			
			// Use as a string to allow different error return codes.			
			String status = getDeleteServiceResponse.getStatus();
			if ( status != null && !status.equalsIgnoreCase("OK")) { // If return error
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			DetailedConfigObjects detailedConfigObjects = getDeleteServiceResponse.getDeleteResults();
			DeleteObjectResult[] result = null;

			if (detailedConfigObjects != null) {
				List<DetailedConfigObject> detailedConfigObjectList = detailedConfigObjects.getObjectList();
				int iObjectSize = detailedConfigObjectList.size();
				if (iObjectSize != 0) {
					result = new DeleteObjectResult[iObjectSize];
					for (int i = 0; i < iObjectSize; i++) {
						// Set Object Meta Info
						DetailedConfigObject detailedObject = detailedConfigObjectList.get(i);
						String serviceName = detailedObject.getName();
						String className = detailedObject.getClassName();
						String displayName = detailedObject.getClassDisplayName();
						String userComments = detailedObject.getUserComments();
						com.ibm.datapower.amt.amp.ConfigObject configObject = 
								new com.ibm.datapower.amt.amp.ConfigObject(serviceName, className, displayName, userComments);
												
						String errorMessage = detailedObject.getErrorMessage();		
						result[i] = new DeleteObjectResult(configObject, detailedObject.getDeleted(),
									detailedObject.getExcluded(), errorMessage);
					}
				}
				return result;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseDelDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumDelDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}

	/*
	 * @see com.ibm.datapower.amt.amp.Commands#quiesceService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext, int)
	 */
	public void quiesceService(DeviceContext device, String domainName, com.ibm.datapower.amt.amp.ConfigObject[] objects, int timeout)
		throws AMPIOException, InvalidCredentialsException, AMPException {
		
		final String METHOD_NAME = "quiesceService"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		
		if ( objects == null ) {
			quiesceDomain(device, domainName, timeout);
			return;
		}
		
		QuiesceRequestDocument requestDoc = QuiesceRequestDocument.Factory.newInstance();
		QuiesceRequestDocument.QuiesceRequest request = requestDoc.addNewQuiesceRequest();

		QuiesceRequestDocument.QuiesceRequest.Domain requestDomain = request.addNewDomain();
		requestDomain.setName(domainName);
		requestDomain.setTimeout(timeout);
		ConfigObjects configObjects = requestDomain.addNewServices();
		if ( objects != null ) {
			for ( int i=0; i<objects.length; i++ ) {
				if ( objects[i] != null ) {
					ConfigObject configObject = configObjects.addNewObject();
					configObject.setName(objects[i].getName());
					configObject.setClassName(objects[i].getClassName());
				}
			}
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "quiesce request created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending quiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			QuiesceResponseDocument responseDoc = QuiesceResponseDocument.Factory.parse(responseDocXml);
			QuiesceResponseDocument.QuiesceResponse quiesceResponse = responseDoc.getQuiesceResponse();

			if (quiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = quiesceResponse.getStatus();
			String status = quiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.quiesceServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * @see com.ibm.datapower.amt.amp.Commands#unquiesceService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext)
	 */
	public void unquiesceService(DeviceContext device, String domainName, com.ibm.datapower.amt.amp.ConfigObject[] objects)
	throws AMPIOException, InvalidCredentialsException, AMPException {
		
		final String METHOD_NAME = "unquiesceService"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		
		if ( objects == null ) {
			unquiesceDomain(device, domainName);
			return;
		}

		UnquiesceRequestDocument requestDoc = UnquiesceRequestDocument.Factory.newInstance();
		UnquiesceRequestDocument.UnquiesceRequest request = requestDoc.addNewUnquiesceRequest();
		
		UnquiesceRequestDocument.UnquiesceRequest.Domain requestDomain = request.addNewDomain();
		requestDomain.setName(domainName);
		ConfigObjects configObjects = requestDomain.addNewServices();
		
		for ( int i=0; i < objects.length; i++ ) {
			if ( objects[i] != null ) {
				ConfigObject configObject = configObjects.addNewObject();
				configObject.setName(objects[i].getName());
				configObject.setClassName(objects[i].getClassName());
			}
		}
		
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "unquiesce request created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending unquiesce request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			UnquiesceResponseDocument responseDoc = UnquiesceResponseDocument.Factory.parse(responseDocXml);
			UnquiesceResponseDocument.UnquiesceResponse unquiesceResponse = responseDoc.getUnquiesceResponse();

			if (unquiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String status = unquiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			else if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.unQuiesceServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.unquiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * @see com.ibm.datapower.amt.amp.Commands#startService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext)
	 */
	public void startService(DeviceContext device, String domainName, com.ibm.datapower.amt.amp.ConfigObject[] objects)
		throws AMPIOException, InvalidCredentialsException, AMPException {
		
		final String METHOD_NAME = "startService"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		
		StartServiceRequestDocument requestDoc = StartServiceRequestDocument.Factory.newInstance();
		StartServiceRequestDocument.StartServiceRequest request = requestDoc.addNewStartServiceRequest();

		request.setDomain(domainName);		
		if ( objects != null ) {
			ConfigObjects configObjects = request.addNewServices();
			for ( int i=0; i < objects.length; i++ ) {
				if ( objects[i] != null ) {
					ConfigObject configObject = configObjects.addNewObject();
					configObject.setName(objects[i].getName());
					configObject.setClassName(objects[i].getClassName());
				}
			}
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "startService request created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending startService request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			StartServiceResponseDocument responseDoc = StartServiceResponseDocument.Factory.parse(responseDocXml);
			StartServiceResponseDocument.StartServiceResponse quiesceResponse = responseDoc.getStartServiceResponse();

			if (quiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.startServiceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.startServiceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String status = quiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.startServiceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.startServiceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			else if ( !status.equalsIgnoreCase("OK")) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.startServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.startServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * @see com.ibm.datapower.amt.amp.Commands#stopService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ObjectContext)
	 */
	public void stopService(DeviceContext device, String domainName, com.ibm.datapower.amt.amp.ConfigObject[] objects)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		
		final String METHOD_NAME = "stopService"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		
		StopServiceRequestDocument requestDoc = StopServiceRequestDocument.Factory.newInstance();
		StopServiceRequestDocument.StopServiceRequest request = requestDoc.addNewStopServiceRequest();

		request.setDomain(domainName);		
		if ( objects != null ) {
			ConfigObjects configObjects = request.addNewServices();
			for ( int i=0; i < objects.length; i++ ) {
				if ( objects[i] != null ) {
					ConfigObject configObject = configObjects.addNewObject();
					configObject.setName(objects[i].getName());
					configObject.setClassName(objects[i].getClassName());
				}
			}		
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "stopService request created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending stopService request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a response object */
		try {
			StopServiceResponseDocument responseDoc = StopServiceResponseDocument.Factory.parse(responseDocXml);
			StopServiceResponseDocument.StopServiceResponse quiesceResponse = responseDoc.getStopServiceResponse();

			if (quiesceResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.stopServiceNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.stopServiceNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			String status = quiesceResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.stopServiceNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.stopServiceNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			else if ( !status.equalsIgnoreCase("OK")) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.stopServiceFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.stopServiceFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * 	(non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#setDomainByService(com.ibm.datapower.amt.amp.DeviceContext, java.lang.String, com.ibm.datapower.amt.amp.ConfigObject[], byte[], com.ibm.datapower.amt.clientAPI.DeploymentPolicy, boolean)
	 */
	public void setDomainByService(DeviceContext device, String domainName,	com.ibm.datapower.amt.amp.ConfigObject[] objects, byte[] domainImage,
			DeploymentPolicy policy, boolean importAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException {

		final String METHOD_NAME = "setDomain"; //$NON-NLS-1$
	
		logger.entering(CLASS_NAME, METHOD_NAME);
	
		SetDomainExportRequestDocument requestDoc = SetDomainExportRequestDocument.Factory.newInstance();
		SetDomainExportRequestDocument.SetDomainExportRequest setDomainExportRequest = requestDoc.addNewSetDomainExportRequest();
	
		// Backup image
		Backup image = setDomainExportRequest.addNewConfig();
		image.setStringValue(new String(domainImage));
		image.setDomain(domainName);
		
		// reset domain
		setDomainExportRequest.setResetDomain(false);
	
		// deployment policy
		DeploymentPolicyConfiguration deppol = null;
		if (policy != null) {
			switch (policy.getPolicyType()) {
			case EXPORT:
				deppol = setDomainExportRequest.addNewPolicyConfiguration();
				deppol.setStringValue(new String(policy.getCachedBytes()));
				deppol.setPolicyDomainName(policy.getPolicyDomainName());
				deppol.setPolicyObjectName(policy.getPolicyObjectName());
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ","
						+ policy.getPolicyDomainName() + "," + policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case XML:
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy xmlpolicy;
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy.ModifiedConfig modifiedConfig;
	
				// Parse the policy
				String xmlFragment = new String(policy.getCachedBytes());
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					DocumentBuilder db = dbf.newDocumentBuilder();
					doc = db.parse(new InputSource(new StringReader(xmlFragment)));
				} catch (Exception exception) {
					Object[] params = new Object[] { device.getHostname() };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
	
				// Walk the nodes and generate the policy in the request
				Node node = (Node) doc;
				NodeList policyNode = node.getChildNodes();
				for (int i = 0; i < policyNode.getLength(); i++) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ":"
							+ policyNode.item(i).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					if (policyNode.item(i).getNodeName().equalsIgnoreCase("policy")) {
						xmlpolicy = setDomainExportRequest.addNewPolicy();
						NodeList configNodes = policyNode.item(i).getChildNodes();
						for (int j = 0; j < configNodes.getLength(); j++) {
							if (configNodes.item(j).getNodeName().equalsIgnoreCase("modifiedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								modifiedConfig = xmlpolicy.addNewModifiedConfig();
								NodeList propertyNodes = configNodes.item(j).getChildNodes();
								for (int k = 0; k < propertyNodes.getLength(); k++) {
									boolean logProperty = true;
									if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("match")) {
										modifiedConfig.setMatch(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("type")) {
										modifiedConfig.setType(com.datapower.schemas.appliance.management.x30.PolicyType.CHANGE);
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("property")) {
										modifiedConfig.setProperty(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("value")) {
										modifiedConfig.setValue(propertyNodes.item(k).getTextContent());
									} else {
										logProperty = false;
									}
	
									// log if needed
									if (logProperty) {
										logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
												+ policy.getPolicyType().name() + ":" + propertyNodes.item(k).getNodeName() + ":" + propertyNodes.item(k).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("acceptedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addAcceptedConfig(configNodes.item(j).getTextContent());
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("filteredconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addFilteredConfig(configNodes.item(j).getTextContent());
							}
						}
					}
				}
				break;
			/*
			 * case REFERENCE:
			 * setDomainExportRequest.setPolicyObjectName(policy.
			 * getPolicyObjectName()); logger.logp(Level.FINEST, CLASS_NAME,
			 * METHOD_NAME, "setDomainExportRequest(" + domainName + ") " +
			 * "deployment policy (" + policy.getPolicyType().name() + "," +
			 * policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$
			 * //$NON-NLS-2$ break;
			 */
			default:
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") "
						+ "deployment policy type (" + policy.getPolicyType().name() + ")is unsupported."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}
		// set services
		if ( objects != null ) {
			int iSize = objects.length;
			
			ConfigObjects configObjects = setDomainExportRequest.addNewServices();
			ConfigObject[] configObject = new ConfigObject[iSize];
			for ( int i=0; i < iSize; i++ ) {
				configObject[i] = configObjects.addNewObject();
				if ( objects[i] != null ) { // in case something wrong in object context Array	
					configObject[i].setName(objects[i].getName());
					configObject[i].setClassName(objects[i].getClassName());
				}
			}
			configObjects.setObjectArray(configObject);
		}
		
		setDomainExportRequest.setImportAllFiles(importAllFiles);
	
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$	
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setDomainExportRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	
		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);
	
		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;
	
		/* Parse the request into a SetDomainExportResponse object */
		try {
			SetDomainExportResponseDocument responseDoc = SetDomainExportResponseDocument.Factory.parse(responseDocXml);
			SetDomainExportResponseDocument.SetDomainExportResponse getSetDomainExportResponse = responseDoc.getSetDomainExportResponse();
	
			if (getSetDomainExportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			// Use as a string to allow different error return codes.
			// String status = getSetDomainExportResponse.getStatus();
			String status = getSetDomainExportResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			// if (status.equals(Status.OK.toString())){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()), status };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}
	
	public void setDomainByService(DeviceContext device, String domainName,	com.ibm.datapower.amt.amp.ConfigObject[] objects,
			String fileDomainName, String fileNameOnDevice,
			DeploymentPolicy policy, boolean importAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException {

		final String METHOD_NAME = "setDomain"; //$NON-NLS-1$
	
		logger.entering(CLASS_NAME, METHOD_NAME);
	
		SetDomainExportRequestDocument requestDoc = SetDomainExportRequestDocument.Factory.newInstance();
		SetDomainExportRequestDocument.SetDomainExportRequest setDomainExportRequest = requestDoc.addNewSetDomainExportRequest();
		
		// Backup File
		BackupFile backupFile = setDomainExportRequest.addNewConfigFile();
		backupFile.setDomain(domainName);
		//
		FileLocation fileLocation = FileLocation.Factory.newInstance();
		fileLocation.setDomain(fileDomainName);
		// find location
		int iLoc = fileNameOnDevice.lastIndexOf("/");
		String sLocation = fileNameOnDevice.substring(0, iLoc+1);		
		fileLocation.setLocation(sLocation);
		// get file name
		String sFileName = fileNameOnDevice.substring(iLoc+1);
		fileLocation.setName(sFileName);
		backupFile.setFile(fileLocation);
		setDomainExportRequest.setConfigFile(backupFile);
		
		// reset domain
		setDomainExportRequest.setResetDomain(false);
	
		// deployment policy
		DeploymentPolicyConfiguration deppol = null;
		if (policy != null) {
			switch (policy.getPolicyType()) {
			case EXPORT:
				deppol = setDomainExportRequest.addNewPolicyConfiguration();
				deppol.setStringValue(new String(policy.getCachedBytes()));
				deppol.setPolicyDomainName(policy.getPolicyDomainName());
				deppol.setPolicyObjectName(policy.getPolicyObjectName());
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ","
						+ policy.getPolicyDomainName() + "," + policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case XML:
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy xmlpolicy;
				com.datapower.schemas.appliance.management.x30.DeploymentPolicy.ModifiedConfig modifiedConfig;
	
				// Parse the policy
				String xmlFragment = new String(policy.getCachedBytes());
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					DocumentBuilder db = dbf.newDocumentBuilder();
					doc = db.parse(new InputSource(new StringReader(xmlFragment)));
				} catch (Exception exception) {
					Object[] params = new Object[] { device.getHostname() };
					String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params);
					AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainInvalidPolicy", params); //$NON-NLS-1$ //$NON-NLS-2$
					logger.throwing(CLASS_NAME, METHOD_NAME, e);
					throw e;
				}
	
				// Walk the nodes and generate the policy in the request
				Node node = (Node) doc;
				NodeList policyNode = node.getChildNodes();
				for (int i = 0; i < policyNode.getLength(); i++) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy (" + policy.getPolicyType().name() + ":"
							+ policyNode.item(i).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					if (policyNode.item(i).getNodeName().equalsIgnoreCase("policy")) {
						xmlpolicy = setDomainExportRequest.addNewPolicy();
						NodeList configNodes = policyNode.item(i).getChildNodes();
						for (int j = 0; j < configNodes.getLength(); j++) {
							if (configNodes.item(j).getNodeName().equalsIgnoreCase("modifiedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								modifiedConfig = xmlpolicy.addNewModifiedConfig();
								NodeList propertyNodes = configNodes.item(j).getChildNodes();
								for (int k = 0; k < propertyNodes.getLength(); k++) {
									boolean logProperty = true;
									if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("match")) {
										modifiedConfig.setMatch(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("type")) {
										modifiedConfig.setType(com.datapower.schemas.appliance.management.x30.PolicyType.CHANGE);
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("property")) {
										modifiedConfig.setProperty(propertyNodes.item(k).getTextContent());
									} else if (propertyNodes.item(k).getNodeName().equalsIgnoreCase("value")) {
										modifiedConfig.setValue(propertyNodes.item(k).getTextContent());
									} else {
										logProperty = false;
									}
	
									// log if needed
									if (logProperty) {
										logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
												+ policy.getPolicyType().name() + ":" + propertyNodes.item(k).getNodeName() + ":" + propertyNodes.item(k).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("acceptedconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addAcceptedConfig(configNodes.item(j).getTextContent());
							} else if (configNodes.item(j).getNodeName().equalsIgnoreCase("filteredconfig")) {
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") " + "deployment policy ("
										+ policy.getPolicyType().name() + ":" + configNodes.item(j).getNodeName() + ":" + configNodes.item(j).getTextContent() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
								xmlpolicy.addFilteredConfig(configNodes.item(j).getTextContent());
							}
						}
					}
				}
				break;
			/*
			 * case REFERENCE:
			 * setDomainExportRequest.setPolicyObjectName(policy.
			 * getPolicyObjectName()); logger.logp(Level.FINEST, CLASS_NAME,
			 * METHOD_NAME, "setDomainExportRequest(" + domainName + ") " +
			 * "deployment policy (" + policy.getPolicyType().name() + "," +
			 * policy.getPolicyObjectName() + ") set."); //$NON-NLS-1$
			 * //$NON-NLS-2$ break;
			 */
			default:
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") "
						+ "deployment policy type (" + policy.getPolicyType().name() + ")is unsupported."); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}
		// set services
		if ( objects != null ) {
			int iSize = objects.length;
			
			ConfigObjects configObjects = setDomainExportRequest.addNewServices();
			ConfigObject[] configObject = new ConfigObject[iSize];
			for ( int i=0; i < iSize; i++ ) {
				configObject[i] = configObjects.addNewObject();
				if ( objects[i] != null ) { // in case something wrong in object context Array	
					configObject[i].setName(objects[i].getName());
					configObject[i].setClassName(objects[i].getClassName());
				}
			}
			configObjects.setObjectArray(configObject);
		}
		
		setDomainExportRequest.setImportAllFiles(importAllFiles);
	
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setDomainExportRequest(" + domainName + ") created"); //$NON-NLS-1$ //$NON-NLS-2$	
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setDomainExportRequest(" + domainName + ") to device " //$NON-NLS-1$ //$NON-NLS-2$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	
		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);
	
		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;
	
		/* Parse the request into a SetDomainExportResponse object */
		try {
			SetDomainExportResponseDocument responseDoc = SetDomainExportResponseDocument.Factory.parse(responseDocXml);
			SetDomainExportResponseDocument.SetDomainExportResponse getSetDomainExportResponse = responseDoc.getSetDomainExportResponse();
	
			if (getSetDomainExportResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setDomainNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			// Use as a string to allow different error return codes.
			// String status = getSetDomainExportResponse.getStatus();
			String status = getSetDomainExportResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.noStatSetDomain", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			// if (status.equals(Status.OK.toString())){
			if (status.equalsIgnoreCase("OK")) {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			} else {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()), status };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadDomainFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetDom", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			;
			throw ex;
		}
	}
	
	public void deleteFile(DeviceContext device, String domainName, String fileNameOnDevice)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
	
		final String METHOD_NAME = "stopService"; //$NON-NLS-1$
	
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		DeleteFileRequestDocument deleteFileRequestDoc = DeleteFileRequestDocument.Factory.newInstance();
		DeleteFileRequestDocument.DeleteFileRequest deleteFileRequest = deleteFileRequestDoc.addNewDeleteFileRequest();
	
		FileLocation fileLocation = deleteFileRequest.addNewFile();	
		// file.setByteArrayValue(contents);
		fileLocation.setDomain(domainName);
		// find location
		int iLoc = fileNameOnDevice.lastIndexOf("/");
		String sLocation = fileNameOnDevice.substring(0, iLoc+1);		
		fileLocation.setLocation(sLocation);
		// get file name
		String sFileName = fileNameOnDevice.substring(iLoc+1);
		fileLocation.setName(sFileName);
		
		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "deleteFile request created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending deleteFile request to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$
	
		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(deleteFileRequestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);
	
		outMessage.delete(0, outMessage.length());
		outMessage = null;
		deleteFileRequestDoc.setNil();
		deleteFileRequestDoc = null;
	
		/* Parse the request into a response object */
		try {
			DeleteFileResponseDocument responseDoc = DeleteFileResponseDocument.Factory.parse(responseDocXml);
			DeleteFileResponseDocument.DeleteFileResponse deleteFileResponse = responseDoc.getDeleteFileResponse();
	
			if (deleteFileResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteFileNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteFileNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
	
			String status = deleteFileResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteFileNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteFileNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
			else if ( !status.equalsIgnoreCase("OK")) {
				Object[] params = { domainName, device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.deleteFileFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.deleteFileFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceErrParse", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.quiesceInvalidEnum", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	public void setFirmware(DeviceContext device, byte[] firmwareImage, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setFirmware(byte[]...)"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME);
		
		if ( !acceptLicense ) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params);
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
		}

		
		SetFirmwareRequestDocument requestDoc = SetFirmwareRequestDocument.Factory.newInstance();
		SetFirmwareRequestDocument.SetFirmwareRequest setFirmwareRequest = requestDoc.addNewSetFirmwareRequest();	
		//setFirmwareRequest.addNewAcceptLicense();
		Firmware image = setFirmwareRequest.addNewFirmware();
		image.setByteArrayValue(firmwareImage);				

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFirmwareRequest created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setFirmwareRequest to device " //$NON-NLS-1$
				+ device.getHostname() + ":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		StringBuffer outMessage = new StringBuffer(requestDoc.xmlText(soapHelper.getOptions()));
		Node responseDocXml = soapHelper.call(device, outMessage);

		outMessage.delete(0, outMessage.length());
		outMessage = null;
		requestDoc.setNil();
		requestDoc = null;

		/* Parse the request into a SetFirmwareResponse object */
		try {
			SetFirmwareResponseDocument responseDoc = SetFirmwareResponseDocument.Factory.parse(responseDocXml);
			SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = responseDoc.getSetFirmwareResponse();

			if (getSetFirmwareResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = getSetFirmwareResponse.getStatus();
			String status = getSetFirmwareResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			} else {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.amp.Commands#setFirmware(com.ibm.datapower.amt.amp.DeviceContext, java.io.InputStream, boolean)
	 */
	public void setFirmware(DeviceContext device, InputStream inputStream, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "setFirmware(InputStream...)"; //$NON-NLS-1$
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] { device, inputStream });
		
		if ( !acceptLicense ) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params);
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.setFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, e);
			throw e;
		}

		logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "setFirmwareRequest created"); //$NON-NLS-1$
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Sending setFirmwareRequest to device " + device.getHostname() + //$NON-NLS-1$
				":" + device.getAMPPort()); //$NON-NLS-1$

		/* Send request to device */
		Node responseDocXml = soapHelper.call(device, setFirmwareHeaderBytes, setFirmwareFooterBytes, inputStream);

		/* Parse the request into a SetFirmwareResponse object */
		try {
			SetFirmwareResponseDocument responseDoc = SetFirmwareResponseDocument.Factory.parse(responseDocXml);
			SetFirmwareResponseDocument.SetFirmwareResponse getSetFirmwareResponse = responseDoc.getSetFirmwareResponse();

			if (getSetFirmwareResponse == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoResp", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// Status.Enum status = getSetFirmwareResponse.getStatus();
			String status = getSetFirmwareResponse.getStatus();
			if (status == null) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params);
				AMPException e = new AMPException(message, "wamt.amp.defaultV2Provider.CommandsImpl.setFwNoStat", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			}

			// if (status.equals(Status.ERROR)){
			if (status.equalsIgnoreCase("ERROR")) {
				Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
				String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params);
				DeviceExecutionException e = new DeviceExecutionException(message, "wamt.amp.defaultV2Provider.CommandsImpl.loadFwFail", params); //$NON-NLS-1$ //$NON-NLS-2$
				logger.throwing(CLASS_NAME, METHOD_NAME, e);
				throw e;
			} else {
				logger.exiting(CLASS_NAME, METHOD_NAME);
				return;
			}
		} catch (XmlException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.errParseSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} catch (XmlValueOutOfRangeException e) {
			Object[] params = { device.getHostname(), Integer.toString(device.getAMPPort()) };
			String message = Messages.getString("wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params);
			AMPException ex = new AMPException(message, e, "wamt.amp.defaultV2Provider.CommandsImpl.invalidEnumSetFw", params); //$NON-NLS-1$ //$NON-NLS-2$
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
	}
	
}
