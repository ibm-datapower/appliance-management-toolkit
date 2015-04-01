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



package com.ibm.datapower.amt.clientAPI;

import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Send a signal to some third party that informs them we are about to do an
 * update of a device. We send a signal before the device update, and another
 * signal after the device update. This gives the customer a way to do things
 * like taking a device out of a load balancer rotation. All signals go to a
 * single location as a SOAP POST.
 * <p>
 * The third party is specified by the user, we POST to a user-specified URL and
 * don't care about the response. We just want to tell someone what we are
 * doing.
 * <p>
 * 
 * @version SCM ID: $Id: Signaler.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
public class Signaler {
    private Device device = null;
    private FirmwareVersion firmwareVersion = null;
//    private DomainVersion domainVersion = null;
    private String domainName = null;
    private String destinationURL = null;
    private String destinationSOAPAction = null;
    private int destinationConnectTimeout = 0;
    private int destinationResponseTimeout = 0;
    private int destinationDelayTime = 0;
    
    static private final boolean START = true;
    static private final boolean END = false;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    private static final String CLASS_NAME = Signaler.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Create a new signaler object
     * 
     * @param device 
     * @param firmwareVersion 
     * @param domainVarsion 
     */
    Signaler(Device device, 
            FirmwareVersion firmwareVersion, 
            String domainName) {
//            DomainVersion domainVersion) {
        this.device = device;
        this.firmwareVersion = firmwareVersion;
//        this.domainVersion = domainVersion;
        this.domainName = domainName;

        this.destinationURL = Configuration.get(Configuration.KEY_SIGNAL_URL);
        this.destinationSOAPAction = Configuration.get(Configuration.KEY_SIGNAL_SOAP_ACTION);
        Integer destinationConnectTimeoutInteger = Configuration.getAsInteger(Configuration.KEY_SIGNAL_CONNECT_TIMEOUT);
        this.destinationConnectTimeout = destinationConnectTimeoutInteger.intValue();
        Integer destinationResponseTimeoutInteger = Configuration.getAsInteger(Configuration.KEY_SIGNAL_RESPONSE_TIMEOUT);
        this.destinationResponseTimeout = destinationResponseTimeoutInteger.intValue();
        Integer destinationDelayTimeInteger = Configuration.getAsInteger(Configuration.KEY_SIGNAL_DELAY_TIME);
        this.destinationDelayTime = destinationDelayTimeInteger.intValue();
    }
    

    /**
     * Signal a start event 
     * 
     */
    void sendStart() {
        this.sendCommon(START, null);
    }
    
    /**
     * Signal an end event 
     * 
     */
    void sendEnd(boolean wasSuccessful) {
        if (wasSuccessful) {
            this.sendCommon(END, Boolean.TRUE);
        } else {
            this.sendCommon(END, Boolean.FALSE);
        }
    }
    
    private void sendCommon(boolean isStart, Boolean wasSuccessful) {
        final String METHOD_NAME;
        if (isStart) {
            METHOD_NAME = "sendCommon(start)";  //$NON-NLS-1$
        } else {
            METHOD_NAME = "sendCommon(end)";  //$NON-NLS-1$
        }
        logger.entering(CLASS_NAME, METHOD_NAME, wasSuccessful);
        
        if (this.destinationURL == null || this.destinationURL.length() == 0 ) {
            // don't do it
            return;
        }
        
        try {
            String messageString = this.createDocument(isStart, wasSuccessful);
            int messageLength = messageString.getBytes().length;
            
            // because HttpUrlConnection doesn't support timeouts in 1.4.2, use a plain socket.
            Socket socket = new Socket();
            URL url = new URL(this.destinationURL);
            String hostname = url.getHost();
            int port = url.getPort();
            if (port < 0) {
                port = url.getDefaultPort();
            }
            InetSocketAddress endpoint = new InetSocketAddress(hostname, port);
            socket.connect(endpoint, this.destinationConnectTimeout);
            OutputStream outputStream = socket.getOutputStream();
            final String newline = "\r\n";   //$NON-NLS-1$
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "attempting to post signal message to " + url.toExternalForm());  //$NON-NLS-1$
            
            // send the headers
            outputStream.write("Content-Type: text/xml".getBytes());   //$NON-NLS-1$
            outputStream.write(newline.getBytes());
            outputStream.write("Content-Length: ".getBytes());   //$NON-NLS-1$
            outputStream.write(String.valueOf(messageLength).getBytes());
            outputStream.write(newline.getBytes());
            if (this.destinationSOAPAction != null) {
                outputStream.write("SOAPAction: ".getBytes());   //$NON-NLS-1$
                outputStream.write(this.destinationSOAPAction.getBytes());
                outputStream.write(newline.getBytes());
            }
            
            // send the command
            outputStream.write(newline.getBytes());
            outputStream.write("POST ".getBytes());   //$NON-NLS-1$
            String path = url.getPath();
            outputStream.write(path.getBytes());
            outputStream.write(" HTTP/1.0".getBytes());   //$NON-NLS-1$
            outputStream.write(newline.getBytes());
            
            // send the document
            outputStream.write(messageString.getBytes());
            outputStream.flush();
            // read the response, in case the server cares that it was successfully received by us
            socket.setSoTimeout(this.destinationResponseTimeout);
            InputStream inputStream = socket.getInputStream();
            // block until read one byte and discard it
            inputStream.read();
            // acknowledge and discard all other input.
            // I don't think we need to specifically read the whole message
            socket.shutdownInput();
            
            // close the socket
            outputStream.close();
            inputStream.close();
            socket.close();
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "successfully posted signal message to " + url.toExternalForm());  //$NON-NLS-1$
            
            // sleep for propogation by 3rd party
            Thread.sleep(this.destinationDelayTime);
            
        } catch (Throwable e) {
            // log it and keep going. This should be passive.
            String message = Messages.getString("wamt.clientAPI.Signal.sendErr", this.destinationURL);  //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    private String createDocument(boolean isStart, Boolean wasSuccessful) 
    throws ParserConfigurationException, DeletedException, TransformerException {
        // TODO: this should really be a CBE event instead of a proprietary one
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        
        final String soapEnvNs = "http://schemas.xmlsoap.org/soap/envelope/";  //$NON-NLS-1$
        
        Element envElement = document.createElementNS(soapEnvNs, "soapenv:Envelope");  //$NON-NLS-1$
        document.appendChild(envElement);
        Element bodyElement = document.createElementNS(soapEnvNs, "soapenv:Body");  //$NON-NLS-1$
        envElement.appendChild(bodyElement);

        Element signalElement = document.createElement("signal");  //$NON-NLS-1$
        signalElement.setAttribute("xmlns", "http://www.ibm.com/datapower/wamt/signal/1.0");  //$NON-NLS-1$
        bodyElement.appendChild(signalElement);
        
        Calendar timestamp = GregorianCalendar.getInstance();
        Element timestampElement = document.createElement("timestamp");  //$NON-NLS-1$
        signalElement.appendChild(timestampElement);
        
        Element yearElement = document.createElement("year");  //$NON-NLS-1$
        timestampElement.appendChild(yearElement);
        Text yearText = document.createTextNode(String.valueOf(timestamp.get(Calendar.YEAR)));
        yearElement.appendChild(yearText);
        
        Element monthElement = document.createElement("month");  //$NON-NLS-1$
        timestampElement.appendChild(monthElement);
        Text monthText = document.createTextNode(String.valueOf(timestamp.get(Calendar.MONTH)+1));
        monthElement.appendChild(monthText);
        
        Element dayElement = document.createElement("day");  //$NON-NLS-1$
        timestampElement.appendChild(dayElement);
        Text dayText = document.createTextNode(String.valueOf(timestamp.get(Calendar.DAY_OF_MONTH)));
        dayElement.appendChild(dayText);

        Element hourElement = document.createElement("hour");  //$NON-NLS-1$
        timestampElement.appendChild(hourElement);
        Text hourText = document.createTextNode(String.valueOf(timestamp.get(Calendar.HOUR_OF_DAY)));
        hourElement.appendChild(hourText);

        Element minuteElement = document.createElement("minute");  //$NON-NLS-1$
        timestampElement.appendChild(minuteElement);
        Text minuteText = document.createTextNode(String.valueOf(timestamp.get(Calendar.MINUTE)));
        minuteElement.appendChild(minuteText);

        Element secondElement = document.createElement("second");  //$NON-NLS-1$
        timestampElement.appendChild(secondElement);
        Text secondText = document.createTextNode(String.valueOf(timestamp.get(Calendar.SECOND)));
        secondElement.appendChild(secondText);

        Element millisecondElement = document.createElement("millisecond");  //$NON-NLS-1$
        timestampElement.appendChild(millisecondElement);
        Text millisecondText = document.createTextNode(String.valueOf(timestamp.get(Calendar.MILLISECOND)));
        millisecondElement.appendChild(millisecondText);

        Element zoneElement = document.createElement("zone-offset");  //$NON-NLS-1$
        timestampElement.appendChild(zoneElement);
        Text zoneText = document.createTextNode(String.valueOf(timestamp.get(Calendar.ZONE_OFFSET) / (60*60*1000)));
        zoneElement.appendChild(zoneText);

        Element dstElement = document.createElement("dst-offset");  //$NON-NLS-1$
        timestampElement.appendChild(dstElement);
        Text dstText = document.createTextNode(String.valueOf(timestamp.get(Calendar.DST_OFFSET) / (60*60*1000)));
        dstElement.appendChild(dstText);

        Element managerElement = document.createElement("manager");  //$NON-NLS-1$
        signalElement.appendChild(managerElement);
        Manager manager = Manager.internalGetInstance();
        URL managerURL = manager.getNotificationCatcherURL();
        String managerURLString = managerURL.toExternalForm();
        Text managerText = document.createTextNode(managerURLString);
        managerElement.appendChild(managerText);

        Element phaseElement = null;
        if (isStart) {
            phaseElement = document.createElement("start");  //$NON-NLS-1$
        } else {
            phaseElement = document.createElement("end");  //$NON-NLS-1$
        }
        signalElement.appendChild(phaseElement);
        
        Element delayTimeElement = document.createElement("delayTime");  //$NON-NLS-1$
        delayTimeElement.setAttribute("unit", "ms");  //$NON-NLS-1$  //$NON-NLS-2$
        phaseElement.appendChild(delayTimeElement);
        Text delayTimeNode = document.createTextNode(String.valueOf(this.destinationDelayTime));
        delayTimeElement.appendChild(delayTimeNode);
        
        Element deviceElement = document.createElement("device");  //$NON-NLS-1$
        ManagedSet managedSet = this.device.getManagedSet();
        if (managedSet != null) {
            deviceElement.setAttribute("managedSet", managedSet.getName());  //$NON-NLS-1$
        }
        phaseElement.appendChild(deviceElement);
        
        Element nameElement = document.createElement("name");  //$NON-NLS-1$
        deviceElement.appendChild(nameElement);
        Text nameNode = document.createTextNode(this.device.getSymbolicName());
        nameElement.appendChild(nameNode);
        
        Element hostnameElement = document.createElement("hostname");  //$NON-NLS-1$
        deviceElement.appendChild(hostnameElement);
        Text hostnameNode = document.createTextNode(this.device.getHostname());
        hostnameElement.appendChild(hostnameNode);
        
        if (this.firmwareVersion != null) {
            Element firmwareElement = document.createElement("firmware");  //$NON-NLS-1$
            deviceElement.appendChild(firmwareElement);
            Element levelElement = document.createElement("level");   //$NON-NLS-1$
            firmwareElement.appendChild(levelElement);
            Text levelNode = document.createTextNode(this.firmwareVersion.getLevel());
            levelElement.appendChild(levelNode);
            if (wasSuccessful != null) {
                Element successElement = document.createElement("wasSuccessful");  //$NON-NLS-1$
                firmwareElement.appendChild(successElement);
                Text successNode = document.createTextNode(wasSuccessful.toString());
                successElement.appendChild(successNode);
            }
        }
        
//        if (this.domainVersion != null) {
        if (this.domainName != null) {
            Element domainElement = document.createElement("domain");  //$NON-NLS-1$
            deviceElement.appendChild(domainElement);
            Element domainNameElement = document.createElement("name");  //$NON-NLS-1$
            domainElement.appendChild(domainNameElement);
//            Domain domain = this.domainVersion.getDomain();
//            Text domainNameNode = document.createTextNode(domain.getRelativeDisplayName());
            Text domainNameNode = document.createTextNode(this.domainName);
            domainNameElement.appendChild(domainNameNode);
            Element versionElement = document.createElement("version");   //$NON-NLS-1$
            domainElement.appendChild(versionElement);
//            Text versionNode = document.createTextNode(String.valueOf(this.domainVersion.getVersionNumber()));
            Text versionNode = document.createTextNode(String.valueOf("0"));  //$NON-NLS-1$   "0" indicates no version
            versionElement.appendChild(versionNode);
            if (wasSuccessful != null) {
                Element successElement = document.createElement("wasSuccessful");  //$NON-NLS-1$
                domainElement.appendChild(successElement);
                Text successNode = document.createTextNode(wasSuccessful.toString());
                successElement.appendChild(successNode);
            }
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");  //$NON-NLS-1$
        CharArrayWriter writer = new CharArrayWriter(512);
        transformer.transform(
                new DOMSource(document),
                new StreamResult(writer));
        String result = writer.toString();
        return(result);
    }
    
}