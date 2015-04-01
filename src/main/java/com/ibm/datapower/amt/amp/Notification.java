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
package com.ibm.datapower.amt.amp;

import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Task;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A POJO representation of an event from a DataPower device.
 * <p>
 * The {@link com.ibm.datapower.amt.clientAPI.Manager} should be notified when
 * the following events occur on a subscribed device:
 * <ul>
 * <li><cite>Save Config </cite> action (may indicate an update to domain(s)
 * and/or a clonable device-specific setting)</li>
 * <li>firmware upgrade/downgrade</li>
 * <li>change in op-state status of a domain</li>
 * <li>impending device restart/reboot (optional)</li>
 * </ul>
 * For an exact list of events that are included in an AMP subscription, please
 * refer to the list in the DataPower firmware source file
 * <code>dp_src/datapower/webgui/Support/ampSubscriptions.xml</code>.
 * <p />
 * When these events occur, the Manager may need to take action, such as
 * deploying the changed Domain/Settings to the rest of the devices in the
 * ManagedSet, update the op-state of a Domain, etc.
 * <p />
 * The NotificationCatcher reads these events from the device, and enqueues them
 * to the Manager for further action. Refer to the {@link NotificationCatcher}
 * javadoc for how the events are sent by the device. The events will be sent
 * from the device to the subscriber using CBE (Common Base Event) format. The
 * <code>NotificationCatcher</code> handles the transport of the event, and
 * the <code>Notification</code> handles the data of the event.
 * <p />
 * Note that for a <cite>Save Config </cite>action, notification is not expected
 * when a running (non-persisted) service configuration object is changed, only
 * when the administrator invokes <cite>Save Config </cite> or if one of the
 * other conditions in the bulleted list occurs.
 * <p />
 * All the values returned from the gettr methods will be extracted from CBE
 * fields.
 * <p />
 * One of the values in the Notification is a sequence number. The sequence
 * number should be a monotomically increasing integer which indicates the
 * sequence of the events. This sequence number is used to verify that events
 * are processed in order and that any missing events are detected. The sequence
 * number is on a per log target basis, which means that the sequence number
 * will be reset (to "0") when a new target is created on the device. When the
 * device creates the events, the sequence number should be in order. However,
 * the events are transmitted from the device asynchronously: there is a linear
 * event buffer in which events are enqueued but they may get transmitted
 * slightly out-of-sequence within a small time window. Whoever reads the
 * Notifications needs to handle out-of-sequence Notifications. If the device's
 * event buffer overflows (which is highly unlikely) then the next serial number
 * will be last serial number plus number of events dropped. So you can have the
 * following seqeunces:
 * <dl>
 * <dt>0 1 2 3 4 ... n
 * <dd>(normal case)
 * <dt>n (n+1) ... 0 1 2 3
 * <dd>(normal roll over or target has been modified)
 * <dt>n (n+1) (n+m) (n+m+1)
 * <dd>(m events got lost because the buffer was full)
 * </dl>
 * <p />
 * 
 * @see NotificationCatcher
 * @see NotificationCatcherFactory
 * @see Commands
 * @version SCM ID: $Id: Notification.java,v 1.5 2010/09/02 16:24:53 wjong Exp $
 */
//* <p />
//* Created on Jun 19, 2006
//* This class needs to implement the <code>Task</code> interface because it
//* gets enqueued to the Manager for background processing, similar to a
//* {@link com.ibm.datapower.amt.clientAPI.BackgroundTask}, which also
//* implements <code>Task</code>.
//* <p />
public class Notification implements Task {
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$
    
    protected final static String CLASS_NAME = Notification.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    private BufferedReader rawReader = null;
    private String deviceSerialNumber = null;
    private int sequenceNumber = 0;   
    private String objectClass = null;
    private String objectName = null;
    private String timestamp = null;  
    private String clientIPAddress = null;

    private String msgId = null;
    // Add new AMP event types here
    private final static String [] msgConstants = new String [] {
        "0x8100006c",  // firmware change applied (but not rebooted) //$NON-NLS-1$
        "0x8100006a",  // op state change down     //$NON-NLS-1$
        "0x8100006b",  // op state change up       //$NON-NLS-1$
        "0x80400003",  // boot scheduled           //$NON-NLS-1$
        "0x81000040",  // domain config saved      //$NON-NLS-1$
        "0x8100003f",  // domain config modified   //$NON-NLS-1$
        "0x54455354"   // test notificiation       //$NON-NLS-1$
    };

    private static int msgIndex=0;
    // declare new AMP event string type here, in order
    private final static String firmwareChange = msgConstants[msgIndex++];
    private final static String opStateChangeDown = msgConstants[msgIndex++];
    private final static String opStateChangeUp = msgConstants[msgIndex++];
    private final static String bootScheduled = msgConstants[msgIndex++];
    private final static String configOfDomainSaved = msgConstants[msgIndex++];
    private final static String configOfDomainModified = msgConstants[msgIndex++];
    private final static String test = msgConstants[msgIndex++];

    /**
     * Constructor for a new Notification object. This will also parse the raw
     * SOAP message into the individual class members that have gettr methods.
     * It is assumed that the NotificationCatcher already verified that the
     * device's SSL certificate used for the HTTPS transport is signed by a
     * trusted certificate authority before invoking this constructor. This
     * construction should be invoked by the NotificationCatcher.
     * 
     * @param rawReader the the BufferedReader that holds the contents of the
     *        SOAP message that was received by the NotificationCatcher
     */
    public Notification(BufferedReader rawReader) {
        this.rawReader = rawReader;
        this.parse();
    }
    
    /**
     * Al alternate constructor for bypassing the NotificationCatcher. This
     * should be used only for internal debugging and testcases.
     * 
     * @param deviceSerialNumber hardware serial number of the device
     * @param sequenceNumber the CBE sequence number for this subscription
     * @param objectClass the name of the device's configuration class of the
     *        object that triggered the notification
     * @param objectName the instance name of the device's configuration object
     *        that triggered the notification
     * @param timestamp the timestamp of when the notification was triggered on
     *        the device
     * @param clientIpaddress IP address of the administrative client that made
     *        the change.
     * @param msgId the AMP event type, which drives the results of the
     *        <code>is*</code> methods. See the private member
     *        <code>msgConstants</code>
     * @deprecated This should be used only for internal debugging and
     *             testcases. It should not be invoked by production code.
     */
    public Notification(String deviceSerialNumber, int sequenceNumber,
            String objectClass, String objectName, String timestamp,
            String clientIpaddress, String msgId) {
        this.deviceSerialNumber = deviceSerialNumber;
        this.sequenceNumber = sequenceNumber;
        this.objectClass = objectClass;
        this.objectName = objectName;
        this.timestamp = timestamp;
        this.clientIPAddress = clientIpaddress;
        this.msgId = msgId;
    }
    
    /**
     * After the raw content has been loaded via the constructor.
     * this method can convert the CBE-in-SOAP message into the member fields
     * for this Java object. Then the gettr methods may be called, likely by the
     * Manager. Until this method is invoked, the only other methods which can
     * be invoked on this object are {@link #setContent(String)}, and
     * {@link #isParsed()}. After this method has been invoked, then any of the
     * other methods in this class may be invoked.
     */
    private void parse() {
        final String METHOD_NAME = "parse"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();            
            Document doc = docBuilder.parse(new InputSource(rawReader));
            Node root = doc.getDocumentElement();
            Node cbeRoot = findCbeRoot(root);
            parseCbe(cbeRoot);
        } catch (Exception e) {
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.Notification.exceptionAMPEvent"), e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    Node findCbeRoot(Node node) {
        final String METHOD_NAME = "findCbeRoot"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Node result = null;
        if (node!=null) {
            int type = node.getNodeType();
            if (type==Node.ELEMENT_NODE) {
                if (node.getNodeName().equals("CommonBaseEvent")){ //$NON-NLS-1$
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Found the common base event root."); //$NON-NLS-1$
                    result = node;
                }
                else {
                    NodeList children = node.getChildNodes();
                    int len = children.getLength();
                    for (int i=0; i<len; i++) {
                        Node childNode = (children.item(i));
                        Node found = findCbeRoot(childNode);
                        if (found != null) {
                            result = findCbeRoot(found);
                        }
                    }
                }
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return result;
    }
    
    void parseCbe(Node node) {
        final String METHOD_NAME = "parseCbe"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        NamedNodeMap attrs = node.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) {
            Attr attr = (Attr)attrs.item(i);
            String attrName = attr.getNodeName();
            if (attrName.equals("creationTime")) { //$NON-NLS-1$
                timestamp = attr.getNodeValue();
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "CBE has creation time of " + timestamp); //$NON-NLS-1$
            } else if (attrName.equals("sequenceNumber")) { //$NON-NLS-1$
                sequenceNumber = new Integer(attr.getNodeValue()).intValue();
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "CBE has sequence number of " + sequenceNumber); //$NON-NLS-1$
            }
        }

        NodeList children = node.getChildNodes();
        int childrenLen = children.getLength();
        for (int i=0; i<childrenLen; i++) {
            Node childNode = (children.item(i));
            int childType = childNode.getNodeType();
            if (childType==Node.ELEMENT_NODE) {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Processing CBE children."); //$NON-NLS-1$
                if (childNode.getNodeName().equals("extendedDataElements")){ //$NON-NLS-1$
                    // domain, client IP addr live in here
                    logger.logp(Level.FINEST, CLASS_NAME, "parseCbe", "Found extended data elements."); //$NON-NLS-1$ //$NON-NLS-2$
                    attrs = childNode.getAttributes();
                    int attrslen = attrs.getLength();
                    for (int j=0; j<attrslen; j++) { // loop thru attribs
                        Attr attr = (Attr)attrs.item(j);
                        String attrName = attr.getNodeName();
                        String attrValue = attr.getNodeValue();
                        if (attrName.equals("name")) { // better be name //$NON-NLS-1$
                            NodeList grandChildren = childNode.getChildNodes();
                            int grandChildrenLen = grandChildren.getLength();
                            for (int k=0; k<grandChildrenLen; k++) {
                                Node grandChildNode = (grandChildren.item(k));
                                int grandChildType = grandChildNode.getNodeType();
                                if (grandChildType==Node.ELEMENT_NODE) {
                                    if (attrValue.equals("ClientIP")) { //$NON-NLS-1$
                                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found a client IP."); //$NON-NLS-1$
                                        setClientIP(grandChildNode);
                                    }
                                }
                            }
                        }
                    }
                }
                if (childNode.getNodeName().equals("msgDataElement")){ //$NON-NLS-1$
                    NodeList grandChildren = childNode.getChildNodes();
                    int grandChildrenLen = grandChildren.getLength();
                    for (int k=0; k<grandChildrenLen; k++) {
                        Node grandChildNode = (grandChildren.item(k));
                        int grandChildType = grandChildNode.getNodeType();
                        if (grandChildType==Node.ELEMENT_NODE) {
                            setEventType(grandChildNode);
                        }
                    }
                }

                if (childNode.getNodeName().equals("sourceComponentId")){ //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found the source id."); //$NON-NLS-1$
                    attrs = childNode.getAttributes();
                    int attrslen = attrs.getLength();
                    for (int j=0; j<attrslen; j++) { // loop thru attribs
                        Attr attr = (Attr)attrs.item(j);
                        String attrName = attr.getNodeName();
                        if (attrName.equals("location")) {  //$NON-NLS-1$
                            deviceSerialNumber = attr.getNodeValue();
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found serial number "+deviceSerialNumber); //$NON-NLS-1$
                        } 
                        else if (attrName.equals("subComponent")) {  //$NON-NLS-1$
                            objectName = attr.getNodeValue();
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found object name " +objectName); //$NON-NLS-1$
                        }
                        else if (attrName.equals("componentType")) {  //$NON-NLS-1$
                            objectClass = attr.getNodeValue();
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found object class " +objectClass); //$NON-NLS-1$
                        }
                    }
                }
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    void setEventType(Node node) {
        final String METHOD_NAME = "setEventType"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        if (node.getNodeName().equals("msgId")){ //$NON-NLS-1$
            NodeList children = node.getChildNodes();
            int childrenLen = children.getLength();
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Found a msg ID with "+childrenLen+" children."); //$NON-NLS-1$ //$NON-NLS-2$
            for (int i=0; i<childrenLen; i++) {
                Node childNode = (children.item(i));
                int childType = childNode.getNodeType();
                if (childType==Node.TEXT_NODE) {
                    msgId = childNode.getNodeValue();
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Got this value for msg ID: "+msgId); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isFirmwareChange "+isFirmwareChange()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isSaveConfigOfDomain "+isSaveConfigOfDomain()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isModifiedConfigOfDomain "+isModifiedConfigOfDomain()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isSaveConfigOfSettings "+isSaveConfigOfSettings()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isModifiedConfigOfSettings "+isModifiedConfigOfSettings()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isOpStateChangeUp "+isOpStateChangeUp()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isOpStateChangeDown "+isOpStateChangeDown()); //$NON-NLS-1$
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "For isBootScheduled "+isBootScheduled()); //$NON-NLS-1$
                }
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    void setClientIP(Node node) {
        final String METHOD_NAME = "setClientIP"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        if (node.getNodeName().equals("values")){ //$NON-NLS-1$
            NodeList children = node.getChildNodes();
            int childrenLen = children.getLength();
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Found a client IP value with "+childrenLen+" children."); //$NON-NLS-1$ //$NON-NLS-2$
            for (int i=0; i<childrenLen; i++) {
                Node childNode = (children.item(i));
                int childType = childNode.getNodeType();
                if (childType==Node.TEXT_NODE) {
                    clientIPAddress = childNode.getNodeValue();
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Got this value for client IP: "+clientIPAddress); //$NON-NLS-1$
                }
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get the serialNumber of the device that triggered the notification.
     * 
     * @return the serialNumber of the device that triggered the notification.
     *         This serialNumber should match the return value of
     *         {@link DeviceMetaInfo#getSerialNumber()}, after it has been
     *         retrieved via {@link Commands#getDeviceMetaInfo(DeviceContext)}.
     */
    public String getDeviceSerialNumber() {
        return(this.deviceSerialNumber);
    }
    
    /**
     * Get the CBE sequence number for this subscription.
     * 
     * @return the CBE sequence number for this subscription. This is a
     *         monotomically increasing integer. A gap in this number from
     *         previous notifications indicates lost notifications from the
     *         device, or reception of notifications in a different order than
     *         they were sent from the device. The sequence number will reset to
     *         zero upon a device reboot. Also, a subscription state that is in
     *         error or has been lost indicates lost notifications from the
     *         device.
     */
    public int getSequenceNumber() {
        return(this.sequenceNumber);
    }


    /**
     * Check if the notification was triggered by a "Save Config" action on the
     * device that persists the domain.
     * 
     * @return true if the notification was triggered by a "Save Config" action
     *         that affected a domain, false otherwise.
     */
    public boolean isSaveConfigOfDomain() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(configOfDomainSaved);
        }
        return(result);
    }
    
    /**
     * Check if the notification was triggered by a domain modification on the
     * device that persists the domain.
     * 
     * @return true if the notification was triggered by a domain modification
     *         that affected a domain, false otherwise.
     */
    public boolean isModifiedConfigOfDomain() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(configOfDomainModified);
        }
        return(result);
    }
    
    /**
     * Check if the notification was triggered by a "Save Config" action on the
     * device that persisted the clonable device-specific settings.
     * 
     * @return true if the notification was triggered by a "Save Config" action
     *         that affected the settings, false otherwise.
     */
    public boolean isSaveConfigOfSettings() {
        boolean result = false;
        if (msgId != null && objectName != null) {
            if (msgId.equals(configOfDomainSaved) &&
                objectName.equals("default")) //$NON-NLS-1$
                result = true;
        }
        return(result);
    }

    
    /**
     * Check if the notification was triggered by a modification of 
     * device that persisted the clonable device-specific settings.
     * 
     * @return true if the notification was triggered by a modification
     *         that affected the settings, false otherwise.
     */
    public boolean isModifiedConfigOfSettings() {
        boolean result = false;
        if (msgId != null && objectName != null) {
            if (msgId.equals(configOfDomainModified) &&
                objectName.equals("default")) //$NON-NLS-1$
                result = true;
        }
        return(result);
    }
    
    /**
     * Check if the notification was triggered by a change in an op-state of a
     * domain that made it up now.
     * 
     * @return true if the notification was triggered by a domain op-state
     *         change that made it up, false otherwise.
     */
    public boolean isOpStateChangeUp() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(opStateChangeUp);
        }
        return result;
    }
    
    /**
     * Check if the notification was triggered by a change in an op-state of a
     * domain that made it down now.
     * 
     * @return true if the notification was triggered by a domain op-state
     *         change that made it down, false otherwise.
     */
    public boolean isOpStateChangeDown() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(opStateChangeDown);
        }
        return result;
    }

    /**
     * Check if the notification was triggered by an impending reboot or device
     * restart.
     * 
     * @return true if the notification was triggered by an impending reboot or
     *         device restart.
     */
    public boolean isBootScheduled() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(bootScheduled);
        }
        return result;
    }
    
    /**
     * Check if the notification was triggered by a firmware change.
     * 
     * @return true if the notification was triggered by a firmware change.
     */
    public boolean isFirmwareChange() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(firmwareChange);
        }
        return result;
    }
    
    public boolean isTest() {
        boolean result = false;
        if (msgId != null) {
            result = msgId.equals(test);
        }
        return result;
    }
    
    /**
     * Get the class of the configuration object on the device that triggered
     * the notification.
     * 
     * @return the name of the device's configuration class of the object that
     *         triggered the notification.
     */
    public String getObjectClass() {
        return(this.objectClass);
    }

    /**
     * Get the instance name of the configuration on the device that triggered
     * the notification.
     * 
     * @return the instance name of the device's configuration object that
     *         triggered the notification.
     */
    public String getObjectName() {
        return(this.objectName);
    }
    
    /**
     * Get the device's timestamp of when the notification was triggered on the
     * device.
     * 
     * @return the timestamp of when the notification was triggered on the
     *         device. This is the device's timestamp, not the subscriber's
     *         timestamp.
     */
    public String getTimestamp() {
        return(this.timestamp);
    }
   
    /**
     * Get the IP address of the administrative client that made the change on
     * the device that triggered this notification.
     * 
     * @return IP address of the administrative client that made the change. If
     *         the administrator was using the WebGUI, this would be the
     *         browser's IP address. If the administrator was using the CLI,
     *         this would be the ssh/telnet client IP address, or serial port
     *         number if locally attached. If the administrator was using SOMA
     *         or AMP, this would be the web service client IP address.
     */
    public String getClientIPAddress() {
        return(this.clientIPAddress);
    }
    
    /**
     * Get a human-readable String representation of this object.
     * 
     * @return a human-readable String representation of this object
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Notification[");
        result.append("deviceSerialNumber=" + this.deviceSerialNumber);
        result.append(", sequenceNumber=" + this.sequenceNumber);
        result.append(", msgId=" + this.msgId);
        result.append("]");
        return(result.toString());
    }
    
}

