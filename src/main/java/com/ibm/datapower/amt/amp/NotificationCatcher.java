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

import java.net.URL;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Manager;

/**
 * This is the interface through which the Manager will start and stop the
 * NotificationCatcher. When the Manager is instantiated, it can in turn
 * instantiate a named implementation of the NotificationCatcher. Since the
 * NotificationCatcher holds on to system resources (i.e., network ports) for
 * long periods of time, the NotificationCatcher should offer a
 * <code>shutdown()</code> method that can be invoked by the Manager.
 * Similarly, if the NotificationCatcher needs to perform initialization that
 * must be done outside the constructor, the NotificationCatcher should offer a
 * <code>startup()</code> method that can be invoked by the Manager.
 * <p>
 * The device sends events (Notifications) to the NotificationCatcher. The
 * NotificationCatcher collects them from the network, validates the device's
 * certificate on the HTTPS transport used to deliver the notification, creates
 * Notification objects from the raw message as obtained from the HTTPS
 * transport, and places the Notification in the Manager's queue. The
 * NotificationCatcher should maintain proper order of the Notifications.
 * <p>
 * The NotificationCatcher sends acknowledgements to the device when it
 * successfully receives a notification. The device uses this acknowledgement to
 * guarantee delivery of all notifications to the NotificationCatcher.
 * <p>
 * HTTPS will be used as the transport to deliver the notifications. The
 * notifications will be in CBE format, and wrapped in a SOAP message for
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
 * If the transport fails the integrity test, then the NotificationCatcher
 * should discard the notification and optionally create a log entry.
 * <p>
 * This should be an interface instead of a class because more than one
 * implementation is expected.
 * <p>
 * 
 * @see NotificationCatcherFactory
 * @see Notification
 * @see Commands
 * @version SCM ID: $Id: NotificationCatcher.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
public interface NotificationCatcher {
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$

    /**
     * When the Manager is started, the Manager will invoke this method. The
     * Manager needs a way to start the NotificationCatcher when the Manager
     * starts. After that point, the NotificationCatcher can place Notifications
     * on the Manager's queue. This method will allow the NotificationCatcher to
     * do any initialization to receive events from devices (such as listening
     * on a network socket) after it has been instantiated by the
     * NotificationCatcherFactory.
     * 
     * @see NotificationCatcherFactory
     * @see Manager
     */
    public void startup() throws AMPException;
    
    /**
     * When the Manager is shutdown, the Manager will invoke this method. For
     * any resources that were obtained via the NotificationCatcher's
     * constructor or {@link #startup()}, this method will allow those
     * resources (such as network sockets) to be released when the Manager is
     * shutdown.
     * 
     * @see Manager
     */
    public void shutdown();
    
    /**
     * Get the URL that this NotificationCatcher listens on so Devices know
     * where to post Notifications. This URL is used when a subscription request (
     * {@link Commands#subscribeToDevice(DeviceContext, String, StringCollection, URL)})
     * is sent to a device so that the device knows where to send the
     * notifications (events).
     * 
     * @return the URL that this NotificationCatcher listens on so Devices know
     *         where to post Notifications. This value will be used when sending
     *         subscription requests to Devices. This may be an https or http
     *         url. Typically, the path part of the URL will not be relevant,
     *         the relevant parts are the protocol, hostname, and port.
     */
    public URL getURL();
}
