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

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.amp.NotificationCatcher;
import com.ibm.datapower.amt.amp.NotificationCatcherFactory;
import com.ibm.datapower.amt.amp.Utilities;
import com.ibm.datapower.amt.clientAPI.Configuration;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * This is the way in which the Manager will start and stop a listener for 
 * AMP events. When the Manager is instantiated, it instantiates and starts
 * this NotificationCatcher implementation.
 * Since the
 * NotificationCatcher holds on to system resources (i.e., network ports) for
 * long periods of time, the NotificationCatcher offers a
 * <code>shutdown()</code> method that can be invoked by the Manager.
 * Similarly, if the NotificationCatcher needs to perform initialization that
 * must be done outside the constructor, the NotificationCatcher provides a
 * <code>startup()</code> method that is called by the Manager.
 * <p>
 * The device(s) send(s) AMP events (Notifications) to the 
 * NotificationCatcher by posting XML over HTTPS. The
 * NotificationCatcher collects them from the network, validates the device's
 * certificate on the HTTPS transport used to deliver the notification, creates
 * Notification objects from the raw message as obtained from the HTTPS
 * transport, and places the Notification in the Manager's queue. 
 * The
 * NotificationCatcher maintains proper order of the Notifications (FIFO).
 * <p>
 * The NotificationCatcher sends acknowledgements to the device when it
 * successfully receives a notification. 
 * The device uses this acknowledgement to
 * guarantee delivery of all notifications to the NotificationCatcher.
 * <p>
 * HTTPS will be used as the transport to deliver the notifications. The
 * notifications will be in CBE (XML) format, and wrapped in a SOAP message for
 * delivery over the transport.
 * <p>
 * We need a way to guarantee event message integrity, meaning that the event
 * came from a real device that we are managing, and is not an injection by a
 * malicious 3rd party. If we did not have event integrity then the Manager
 * could be vulnerable to a DoS attack, causing it to refetch a lot of data from
 * the device that is not necessary. The integrity will be performed by using
 * SSL for the transport. The device should present to the NotificationCatcher a
 * certificate that is signed by a certificate authority that the
 * NotificationCatcher trusts. The NotificationCatcher should have a default
 * list of trusted CAs that work with the default certificates in DataPower
 * devices. There should be a way for the customer to configure the
 * NotificationCatcher to recognize a custom list of trusted CAs, which would
 * match the CAs used to sign the device certificates.
 * <p>
 * If the transport fails the integrity test, the NotificationCatcher
 * discards the notification and optionally create a log entry.
 * <p>
 * @see NotificationCatcherFactory
 * @see Notification
 * @see NotificationCatcher
 * @see TCPSocketListener
 * @see Commands
 *
 * @version SCM ID: $Id: NotificationCatcherImpl.java,v 1.9 2011/03/10 21:53:49 wjong Exp $
 */
//* <p> 

public class NotificationCatcherImpl implements NotificationCatcher {
    private boolean useSSL = true;

    private TCPSocketListener listener;
    private Thread thread;
    private static URL url = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected final static String CLASS_NAME = NotificationCatcherImpl.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    /**
     * When the Manager is started, the Manager will invoke this method. The
     * Manager needs a way to start the NotificationCatcher when the Manager
     * starts. After that point, the NotificationCatcher can place Notifications
     * on the Manager's queue. This method will allow the NotificationCatcher to
     * do any initialization after it has been instantiated by the
     * NotificationCatcherFactory, such as creating a TCPSocketListener.
     * 
     * @see NotificationCatcherFactory
     * @see com.ibm.datapower.amt.clientAPI.Manager
     * @throws AMPException
     */
    public void startup() throws AMPException {
        final String METHOD_NAME = "startup"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Start of AMP event catcher requested."); //$NON-NLS-1$
        Integer sslPortInteger = Configuration.getAsInteger(Configuration.KEY_NOTIFICATION_CATCHER_IP_PORT);
        int sslPortValue = sslPortInteger.intValue();
        String noSSLFlagString = Configuration.get(Configuration.KEY_NOTIFICATION_CATCHER_NO_SSL);
        if (noSSLFlagString != null) {
            boolean noSSLFlagValue = Boolean.valueOf(noSSLFlagString).booleanValue();
            useSSL = !noSSLFlagValue;
        }
        listener = new TCPSocketListener(sslPortValue, useSSL);
        thread = new Thread(listener);
        thread.setDaemon(true);
        thread.start();
        thread.setName("TCPSocketListener:" + sslPortValue); //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Started TCP (SSL flag=" + useSSL + ") listener on port "+sslPortValue+"."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * When the Manager is shutdown, the Manager will invoke this method. For
     * any resources that were obtained via the NotificationCatcher's
     * constructor or {@link #startup()}, this method will allow those
     * resources to be released when the Manager is shutdown.
     * @throws InterruptedException
     * 
     * @see com.ibm.datapower.amt.clientAPI.Manager
     */
    public void shutdown() {
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Shut down of AMP event catcher requested."); //$NON-NLS-1$
        //fix for bugzilla #78404, add null ptr checks
        if (listener != null)
        	listener.shutdown();
        if (this.thread != null){
        	while (this.thread.isAlive()) {
        		try {
        			this.thread.interrupt();
        			Thread.sleep(100);
        		} catch (InterruptedException e) {
        			Thread.currentThread().interrupt();
        		}
        	}
        }
        // listener2.shutdown();
        // while (thread2.isAlive()) {
        //     try {
        //         Thread.sleep(1000);
        //     } catch (InterruptedException e) {
        //         Thread.currentThread().interrupt();
        //     }
        // }
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Shut down of AMP event catcher complete."); //$NON-NLS-1$
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    public URL getURL() {
        final String METHOD_NAME = "getURL"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        if (url == null) {
            try {
                // get the local IP address of an interface.
                String ipAddrString = Configuration.get(Configuration.KEY_NOTIFICATION_CATCHER_IP_ADDRESS);
                if (ipAddrString != null && (ipAddrString.length() >0)) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Binding to local IP address " + ipAddrString); //$NON-NLS-1$
                } else {
                    // search for one. Was a particular interface requested?
                    String ipInterface = Configuration.get(Configuration.KEY_NOTIFICATION_CATCHER_IP_INTERFACE);
                    ipAddrString = Utilities.getLocalIPAddress(ipInterface);
                }                
                
                // get the port number
                Integer sslPortInteger = Configuration.getAsInteger(Configuration.KEY_NOTIFICATION_CATCHER_IP_PORT);
                int sslPort = sslPortInteger.intValue();
               	logger.logp(Level.FINER,  CLASS_NAME, METHOD_NAME, "The IP Adress is: " + ipAddrString + " SSL Port: " + sslPort);                 	
                
                // get the path part (which I don't think there is any)
                String path = "/"; //$NON-NLS-1$
                if (ipAddrString == null) {
                	logger.logp(Level.SEVERE,  CLASS_NAME, METHOD_NAME, "Error:  The IP Adress is null!!");                  	
                }                 	
               
                if (useSSL)
                    url = new URL("https", ipAddrString, sslPort, path); //$NON-NLS-1$
                else
                    url = new URL("http", ipAddrString, sslPort, path); //$NON-NLS-1$
            } catch (MalformedURLException e) {
                logger.logp(Level.SEVERE,  CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.NotificationCatcherImpl.exEventUrl"), e);  //$NON-NLS-1$
            } catch (SocketException e) {
                logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.NotificationCatcherImpl.exNwIff"), e); //$NON-NLS-1$
            }
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(url);
    }
}
