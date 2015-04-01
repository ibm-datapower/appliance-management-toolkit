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

package com.ibm.datapower.amt.amp.defaultProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Node;

import com.datapower.schemas.appliance.management.x10.FaultDocument;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPConstants;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.SOAPHelper;
import com.ibm.datapower.amt.amp.defaultCommon.CustomURLConnection;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A collection of methods for sending and receiving SOAP messages to a
 * DataPower device using its AMP interface. This is designed for use with
 * preformed XML documents. Given an XMLBeans XmlObject, this will add the
 * SOAP envelope, establish an SSL connection that meets the DataPower device
 * requirements, execute a SOAP invocation of the request, and return 
 * the content of the SOAP body as a String. 
 * 
 */
//* SCM: $Id: SOAPHelperImpl.java,v 1.6 2011/05/02 16:59:21 wjong Exp $

public class SOAPHelperImpl implements SOAPHelper{
    
    private XmlOptions options = null;
    private SSLContextCache contextCache = null;
    private static final String SOAP_TOP = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n  <soapenv:Body>\n"; //$NON-NLS-1$
    private static final String SOAP_BOTTOM = "\n  </soapenv:Body>\n</soapenv:Envelope>"; //$NON-NLS-1$
    
    private static final byte[] SOAP_TOP_BYTES = SOAP_TOP.getBytes();
    private static final byte[] SOAP_BOTTOM_BYTES = SOAP_BOTTOM.getBytes();
        
    private static final String SOAP_TRANSPORT = "https"; //$NON-NLS-1$
    private static final String SOAP_URI = "/service/mgmt/amp/"+AMPConstants.AMP_V1_0; //$NON-NLS-1$
    private static final String DATAPOWER_AMP_NS = "http://www.datapower.com/schemas/appliance/management/"+AMPConstants.AMP_V1_0; //$NON-NLS-1$
        
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    private static final String CLASS_NAME = SOAPHelperImpl.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    /**
     * Constructor for a SOAPHelper object.
     */
    public SOAPHelperImpl() { 
        contextCache = SSLContextCache.getInstance();
    }
    
    /**
     * Creates an XmlOptions object that enables the XML outputted by XMLBeans to be 
     * pretty-printed, and to have the correct namespace prefix for the AMP WSDL.
     * 
     * @return an XmlOptions object with pretty print enabled and the AMP prefix defined.
     */
    public XmlOptions getOptions(){
        
        /* the first time, create the options; otherwise reuse */
        if (options == null){
            options = new XmlOptions();
            options.setSavePrettyPrint();
            
            HashMap suggestedPrefixes = new HashMap();
            suggestedPrefixes.put(DATAPOWER_AMP_NS, "amp"); //$NON-NLS-1$
            options.setSaveSuggestedPrefixes(suggestedPrefixes);
        }
        
        return options;
    }
    
    /**
     * Given a requestDocument (as an StringBuffer), this method provides a high-level API
     * to wrap the SOAP envelope, create an SSL connection, send the SOAP
     * request, get the SOAP response, and return the response XML (without the SOAP envelope)
     * 
     * @param device the remote device information contained in a DeviceContext object 
     *   (username, password, hostname, and AMP port number)
     * @param requestDocument a StringBuffer containing the XML request. It will be
     *        translated into an XML document for the SOAP request.
     * 
     * @return a String containing the XML document returned by the SOAP response.
     * @throws AMPIOException an error occurred while communicating with the
     *         DataPower device.
     * @throws InvalidCredentialsException an invalid username/password pair was 
     *    specified in the DeviceContext
     * @throws AMPException an error occured while parsing the SOAP envelope returned
     *         from the device.
     */
    public Node call(DeviceContext device, StringBuffer requestDocument)
        throws AMPIOException, InvalidCredentialsException, AMPException{
        
        final String METHOD_NAME = "call"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
                    
        HttpURLConnection httpConnection = null;
        try{
            
            httpConnection = createConnection(device);
            OutputStream out = httpConnection.getOutputStream();
            
            if (logger.isLoggable(Level.FINEST)){
                StringBuffer sanitizedMsg = removeValuesFromMessage(requestDocument);
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "The outgoing message to " + httpConnection.getURL().toExternalForm()  //$NON-NLS-1$
                        + " is:\n" + SOAP_TOP + sanitizedMsg + SOAP_BOTTOM); //$NON-NLS-1$
                sanitizedMsg.delete(0,sanitizedMsg.length());
                sanitizedMsg = null;
            }
            
            // send the SOAP request to the DataPower device
                        
            out.write(SOAP_TOP_BYTES);
            
            out.write(requestDocument.toString().getBytes());
            
            out.write(SOAP_BOTTOM_BYTES);
                        
            //this could potentially be a huge object; we're done with it, lets get rid of it
            //GC can now get rid of it!
            out.flush();
            out.close();
            requestDocument.delete(0,requestDocument.length());
            requestDocument = null;
                        
            // check for HTTP errors
            checkResponse(httpConnection);
            
            // read in the HTTP response
            InputStream in = httpConnection.getInputStream();
            
            Node body = parseResponse(in, device);
            
            httpConnection.disconnect();
            in.close();
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "removed SOAP envelope from response document"); //$NON-NLS-1$
                       
            logger.exiting(CLASS_NAME, METHOD_NAME);
            return body;
        }
        catch (IOException e){
            Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params);
            AMPIOException ex = new AMPIOException(message,e,"wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /**
     * Given a requestDocument (as an combination of header bytes, an InputStream containing the blob, and footer bytes), 
     * this method provides a high-level API to wrap the SOAP envelope, create an SSL connection, send the SOAP
     * request, get the SOAP response, and return the response XML (without the SOAP envelope)
     * 
     * The inputStream must represent base64 encoded bytes!!!
     * 
     * @param device the remote device information contained in a DeviceContext object 
     *   (username, password, hostname, and AMP port number)
     * @param header the bytes before the blob in the message
     * @param footer the bytes after the blob in the message
     * @param requestDocument an InputStream containing the XML request. It will be
     *        translated into an XML document for the SOAP request.
     * 
     * @return a Node containing the XML document returned by the SOAP response.
     * @throws AMPIOException an error occurred while communicating with the
     *         DataPower device.
     * @throws InvalidCredentialsException an invalid username/password pair was 
     *    specified in the DeviceContext
     * @throws AMPException an error occured while parsing the SOAP envelope returned
     *         from the device.
     */
    public Node call(DeviceContext device, byte[] header, byte[] footer, InputStream requestDocument)
        throws AMPIOException, InvalidCredentialsException, AMPException{
        
        final String METHOD_NAME = "call"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME);
                    
        HttpURLConnection httpConnection = null;
        try{
            
            httpConnection = createConnection(device);
            OutputStream out = httpConnection.getOutputStream();
            if (logger.isLoggable(Level.FINEST)){
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "The outgoing message to " + httpConnection.getURL().toExternalForm() 
                        + " is:\n" + SOAP_TOP + new String(header) + "BLOB REMOVED FOR LOGGING PURPOSES" + new String(footer) + SOAP_BOTTOM);
            }
                        
            // send the SOAP request to the DataPower device
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Adding SOAP envelope to request document"); //$NON-NLS-1$
              
            out.write(SOAP_TOP_BYTES);
            
            out.write(header,0,header.length);
            
            synchronized(requestDocument){
                synchronized(out){
                    
                    byte[] b = new byte[4096];
                    while(true){
                        int bytesRead = requestDocument.read(b);
                        if (bytesRead == -1) break;
                        out.write(b,0,bytesRead);
                    }
                }
            }
            requestDocument.close();
            
            out.write(footer,0,footer.length);
            
            out.write(SOAP_BOTTOM_BYTES);
            
            out.flush();
            out.close();
            out = null;
            
            // check for HTTP errors
            checkResponse(httpConnection);
            
            // read in the HTTP response
            InputStream in = httpConnection.getInputStream();
        
            Node body = parseResponse(in, device);
            
            httpConnection.disconnect();
            httpConnection = null;
            
            in.close();
            in = null;
            
            requestDocument.close();
            requestDocument = null;
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "removed SOAP envelope from response document"); //$NON-NLS-1$

            logger.exiting(CLASS_NAME, METHOD_NAME);
            return body;
        }
        catch (IOException e){
            Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params);
            AMPIOException ex = new AMPIOException(message,e,"wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
       
    /**
     * Removes the SOAP envelope and body tags from the response message
     * 
     * @param responseMessage The SOAP envelope received from the DP device
     */
    private Node parseResponse(InputStream responseMessage, DeviceContext device) 
        throws AMPException, InvalidCredentialsException, AMPIOException{

        final String METHOD_NAME = "parseResponse"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
                
        try{
            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.addHeader("Content-Type", "text/xml; charset=UTF-8");
            SOAPMessage msg = MessageFactory.newInstance().createMessage(mimeHeaders, responseMessage);
            /* Explicitly enumerate the Envelope and Body class files used here to resolve
             * the class name clash, since they are in org.apache.soap from 
             * com.ibm.ws.runtime_6-1-0.jar */
            
            if (logger.isLoggable(Level.FINEST)){
                StringWriter writer = new StringWriter();
                writer.append(msg.toString());
                StringBuffer responseStringBuffer = removeValuesFromMessage(writer.getBuffer());
                
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "We received the following message from " +  //$NON-NLS-1$
                        device.getHostname() + ":" + device.getAMPPort() + " :\n" +  responseStringBuffer); //$NON-NLS-1$ //$NON-NLS-2$
                                
                responseStringBuffer.delete(0,responseStringBuffer.length());
                responseStringBuffer = null;
                writer.flush();
                writer = null;
            }
            
            SOAPBody body = msg.getSOAPBody();
            
            if (body == null){
                Object[] params = {device.getHostname()};
                String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.bodyNotSchemaValid",params);
                AMPException e = new AMPException(message,"wamt.amp.defaultProvider.SOAPHelper.bodyNotSchemaValid",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            Node node = body.getFirstChild();
                        
            try {
                FaultDocument faultDoc = FaultDocument.Factory.parse(node);
                com.datapower.schemas.appliance.management.x10.Fault.Enum fault = faultDoc.getFault();
                
                if (fault == null){
                    // this is a weird corner case... what does this mean?
                    // lets just punt this to the commandsImpl code.
                    logger.exiting(CLASS_NAME, METHOD_NAME);
                    return node;
                }
                
                if (fault.equals(com.datapower.schemas.appliance.management.x10.Fault.AUTHENTICATION_FAILURE)){
                    Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
                    String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.invalidUNPass",params);
                    InvalidCredentialsException e = new InvalidCredentialsException(message,"wamt.amp.defaultProvider.SOAPHelper.invalidUNPass",params); //$NON-NLS-1$ //$NON-NLS-2$

                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw e;
                }
            }
            catch (XmlException e){
                // if we get an exception here, it means there was no fault. 
                // squelch it, and just return the body text.
            }
            logger.exiting(CLASS_NAME, METHOD_NAME);
            return node;
            
        }
        catch (SOAPException e){
            Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.errParsingSOAPEnv",params);
            AMPException ex = new AMPException(message,e,"wamt.amp.defaultProvider.SOAPHelper.errParsingSOAPEnv",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;   
        }
        catch (IOException e){
            Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params);
            AMPIOException ex = new AMPIOException(message,e,"wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
    }
    
    /**
     * Create the HttpURLConnection to use for sending and receiving the SOAP
     * envelopes
     * 
     * @param httpConnection the active connection to the device
     * @throws IOException an incorrect content type or response code was
     *    received from the device.
     */
    private HttpURLConnection createConnection(DeviceContext device) 
        throws AMPIOException {
        
        final String METHOD_NAME = "createConnection"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        
        HttpURLConnection httpConnection = null;  
        try{
            String authDecoded = device.getUserId() + ":" + device.getPassword(); //$NON-NLS-1$
            String authHeader = "Basic " + Utils.encodeBase64(authDecoded.getBytes()); //$NON-NLS-1$
            
            URL url = new URL(SOAP_TRANSPORT, device.getHostname(), 
                              device.getAMPPort(), SOAP_URI);
            
            CustomURLConnection customURLConn =  new CustomURLConnection(url);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "AMP connect timeout is set to " + customURLConn.getConnectLimit());
            URLConnection connection = customURLConn.openConnection();
            
            if ( connection == null ) {
                String message = "Upon attempting the HTTPS Connection to " +  //$NON-NLS-1$
                                device.getHostname() + " , there was an unknown error in the SSL Handshake."; //$NON-NLS-1$
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$                
                AMPIOException ex = new AMPIOException(message);
                logger.throwing(CLASS_NAME, METHOD_NAME, ex);
                throw ex;
            }
            
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);
            
            httpConnection = (HttpURLConnection) connection;
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "connection to "  //$NON-NLS-1$
                        + httpConnection.getURL().toExternalForm() + " was successful"); //$NON-NLS-1$
            
            httpConnection.setRequestMethod("POST"); //$NON-NLS-1$
            httpConnection.setUseCaches(false);
            httpConnection.setDefaultUseCaches(false);
            httpConnection.setRequestProperty("Content-Type","text/xml"); //$NON-NLS-1$ //$NON-NLS-2$
            httpConnection.setRequestProperty("Authorization", authHeader); //$NON-NLS-1$
            
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            HostnameVerifier hostnameVerifierIgnore = new HostnameVerifierIgnore();
            httpsConnection.setHostnameVerifier(hostnameVerifierIgnore);
            httpsConnection.setDefaultUseCaches(false);
            httpsConnection.setUseCaches(false);
            
            SSLSocketFactory customSSLSocketFactory = contextCache.getCustomSSLSocketFactory();
            httpsConnection.setSSLSocketFactory(customSSLSocketFactory);
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "attempting to connect to " + httpConnection.getURL().toExternalForm()); //$NON-NLS-1$
            
            try {
                customURLConn.connect();
            } 
            catch (SSLHandshakeException e) {
                if ((e.getMessage().indexOf("unknown certificate") != -1) || //$NON-NLS-1$
                    (e.getMessage().indexOf("No trusted certificate") != -1)) { //$NON-NLS-1$
                    String msg = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.importCert",e.getMessage()); //$NON-NLS-1$
                    SSLHandshakeException e1 = new SSLHandshakeException(msg);
                    e1.setStackTrace(e.getStackTrace());
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "Upon attempting the HTTPS Connection to " +  //$NON-NLS-1$
                                httpConnection.getURL().toExternalForm() + " , the JSSE was " //$NON-NLS-1$
                                + "unable to find a trusted root certificate authority.",e1); //$NON-NLS-1$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e1);
                    throw(e1);
                } 
                else {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                                "Upon attempting the HTTPS Connection to " +  //$NON-NLS-1$
                                httpConnection.getURL().toExternalForm() + " , there was an" //$NON-NLS-1$
                                + " unknown error in the SSL Handshake.",e); //$NON-NLS-1$
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw(e);
                }
            }
        }
        catch(IOException e){
            // Log the exception details for debugging purposes:
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "Upon attempting the HTTPS Connection to " +  //$NON-NLS-1$
                    SOAP_TRANSPORT + "://" + device.getHostname() +":" + device.getAMPPort() + //$NON-NLS-1$ //$NON-NLS-2$
                    SOAP_URI + " , there was an error:", e); //$NON-NLS-1$

            Object[] params = {device.getHostname(),Integer.toString(device.getAMPPort())};
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ioExConnect",params); 
            //Pass in the IOException as the cause when creating the AMPIOException. This serves two
            // purposes: 1. We can make decisions based on the cause. 2. The cause appears in the logs when
            // the exception is logged.
            AMPIOException ex = new AMPIOException(message, e, "wamt.amp.defaultV2Provider.SOAPHelper.ioExConnect",params);
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        logger.exiting(CLASS_NAME, METHOD_NAME, httpConnection);
        return (httpConnection);
    }
    
    /**
     * Check the HttpURLConnection for basic errors after reading the AMP
     * response (SOAP message)
     * 
     * @param httpConnection the active connection to the device
     * @throws IOException an incorrect content type or response code was
     *    received from the device.
     */
    private void checkResponse(HttpURLConnection httpConnection) throws AMPIOException {
        
        final String METHOD_NAME = "checkResponse"; //$NON-NLS-1$
        
        logger.entering(CLASS_NAME, METHOD_NAME, httpConnection);
        
        try{
            int responseCode = httpConnection.getResponseCode();
            
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "responseCode from "  //$NON-NLS-1$
                        + httpConnection.getURL().toExternalForm() + " = " + responseCode); //$NON-NLS-1$
            
            String responseUncode = httpConnection.getResponseMessage();
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "responseUncode from "  //$NON-NLS-1$
                        + httpConnection.getURL().toExternalForm() + " = " + responseUncode); //$NON-NLS-1$
            
            String contentType = httpConnection.getContentType();
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "ContentType from "  //$NON-NLS-1$
                        + httpConnection.getURL().toExternalForm() + " = " + contentType); //$NON-NLS-1$
            
            if (!contentType.startsWith("text/xml")) { //$NON-NLS-1$
                String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ampRespNotvalid",contentType);
                AMPIOException e = new AMPIOException(message,"wamt.amp.defaultProvider.SOAPHelper.ampRespNotvalid",contentType); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
            
            InputStream in = null;
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                in = httpConnection.getErrorStream();
                StringBuffer responseStringBuffer = new StringBuffer();
                byte buffer[] = new byte[512];
                int bytesRead = 0;
                while ((bytesRead = in.read(buffer)) > 0) {
                    String temp = new String(buffer, 0, bytesRead);
                    responseStringBuffer.append(temp);
                }
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "ResponseString from "  //$NON-NLS-1$
                            + httpConnection.getURL().toExternalForm() + " = "  //$NON-NLS-1$
                            + responseStringBuffer);
                
                Object[] params = {Integer.toString(responseCode),Integer.toString(HttpURLConnection.HTTP_OK),httpConnection.getURL().toExternalForm()};
                String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.respCodeNotEq",params);
                AMPIOException e = new AMPIOException(message,"wamt.amp.defaultProvider.SOAPHelper.respCodeNotEq",params); //$NON-NLS-1$ //$NON-NLS-2$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw e;
            }
        }
        catch(IOException e){
            String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.ioExComm");                  
            AMPIOException ex = new AMPIOException(message,e,"wamt.amp.defaultProvider.SOAPHelper.ioExComm");
            logger.throwing(CLASS_NAME, METHOD_NAME, ex);
            throw ex;
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * 
     * Prints messages that are sent or received without printing large blobs (domain configs, device settings, files, firmware images, and error reports)
     * 
     * @param message The original message
     * 
     * @return editedMessage The original message with blobs replaced with "BLOB REMOVED FOR LOGGING PURPOSES"
     */
    private StringBuffer removeValuesFromMessage(StringBuffer message){
        
        /*
         * The 6 strings we need to search for 
         * <amp:Config domain="">
         * <amp:Firmware>
         * <amp:File domain="" location="" name="">
         * <amp:Settings>
         * <amp:ErrorReport domain="" location="" name="">
         * <amp:Password>
         * 
         * note the spaces in Firmware, File, ErrorReport - these are due to attributes in the tag, where as Config and Settings do not have them
         */
        HashMap<String,String[]> stringsToBeFound = new HashMap<>();
        
        stringsToBeFound.put("<amp:Firmware>", //$NON-NLS-1$
                new String[] {"</amp:Firmware>", //$NON-NLS-1$
                              "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}); //$NON-NLS-1$
         stringsToBeFound.put("<amp:File ", //$NON-NLS-1$
                new String[] {"</amp:File>", //$NON-NLS-1$
                             "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}); //$NON-NLS-1$
         stringsToBeFound.put("<amp:ErrorReport ", //$NON-NLS-1$
                new String[] {"</amp:ErrorReport>", //$NON-NLS-1$
                             "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}); //$NON-NLS-1$        
         stringsToBeFound.put("<amp:Config ", //$NON-NLS-1$
                new String[] {"</amp:Config>", //$NON-NLS-1$
                             "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}); //$NON-NLS-1$
         stringsToBeFound.put("<amp:Settings>", //$NON-NLS-1$
                new String[] {"</amp:Settings>", //$NON-NLS-1$
                             "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}); //$NON-NLS-1$
         stringsToBeFound.put("<amp:Password>", //$NON-NLS-1$
                new String[] {"</amp:Password>", //$NON-NLS-1$
                             "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.passwordRemoved")+"]"}); //$NON-NLS-1$ 
                        
        StringBuffer editedMessage = new StringBuffer();
        editedMessage.append(message);
        int startPos = 0, onePos = 0, endPos = 0, marker = 0;
        Iterator<Map.Entry<String, String[]>> startTags = stringsToBeFound.entrySet().iterator();
        while (startTags.hasNext()){
        
            Map.Entry<String, String[]> entry = startTags.next();
            String beginString = entry.getKey();
            String[] endReplacementStringPair = entry.getValue();
            
             do{
                startPos = editedMessage.indexOf(beginString,marker);
                if (startPos != -1){
                    // a config element exists
                    // find the > character
                    onePos = editedMessage.indexOf(">",startPos); //$NON-NLS-1$
                    endPos = editedMessage.indexOf(endReplacementStringPair[0],onePos);
                    editedMessage = editedMessage.replace(onePos+1,endPos,endReplacementStringPair[1]);
                    marker = endPos;
                }
            }while (startPos != -1);
            
        }
        // for policy-configuration
        String beginString = "<amp:Policy-Configuration ";
        if ( editedMessage.indexOf(beginString) > 0) {
            marker = 0;            
            String[] endReplacementStringPair = new String[] {"</amp:Policy-Configuration>", //$NON-NLS-1$
                    "["+Messages.getString("wamt.amp.defaultProvider.SOAPHelper.blobsRemoved")+"]"}; //$NON-NLS-1$;            
            startPos = editedMessage.indexOf(beginString,marker);
            if (startPos != -1){
                onePos = editedMessage.indexOf(">",startPos); //$NON-NLS-1$
                endPos = editedMessage.indexOf(endReplacementStringPair[0],onePos);
                editedMessage = editedMessage.replace(onePos+1,endPos,endReplacementStringPair[1]);
            }
        }

        return editedMessage;
    }
    
    /**
     * A hostname verifier for SSL that can handle DataPower's server certificate
     * that doesn't match its hostname.
     * 
     */
    public static class HostnameVerifierIgnore implements HostnameVerifier {
        
        public HostnameVerifierIgnore() {
            // nothing to do here
        }
        
        /**
         * This method implements what is needed for the HostnameVerifier
         * interface, and ignores the mismatch of the server's hostname and the
         * info in the server's certificate. It does not do any special
         * processing, it just returns "true".
         */
        public boolean verify(String hostname, SSLSession session) {
            // don't do anything, ignore the parameters
            return true;
        }
    }
}
