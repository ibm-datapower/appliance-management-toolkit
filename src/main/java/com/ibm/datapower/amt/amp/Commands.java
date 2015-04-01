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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.ConfigService;
import com.ibm.datapower.amt.clientAPI.DeletedException;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicy;
import com.ibm.datapower.amt.clientAPI.RuntimeService;

/**
 * A list of high-level commands that the DataPower device should support via
 * SOAP invocation. These are commands sent from a manager to the device. Note
 * that there is additional event communication sent from the device to the
 * manager via {@link Notification}. These commands are long-running methods
 * that contact the device via the network and will block until the device
 * responds that the command has been completed. For the reason of being
 * long-running with the potential for high variability in the completion time,
 * it is recommended that you invoke these methods on a different thread than
 * what may be running an interactive user interface.
 * <p>
 * This communication currently is scoped only for multi-box management. It does
 * not include future requirements for application enablement. This should be an
 * interface instead of a class, because it is expected that there may be more
 * than one AMP client implementation.
 * <p>
 * 
 * @see Notification
 * @version SCM ID: $Id: Commands.java,v 1.5 2011/02/14 19:35:58 wjong Exp $
 */
//* <p>
//* Created on Jun 19, 2006

 /*
  * getDomainDifferences not implemented in first release
  * getSettingsDifferences not implemented in first release
  */
public interface Commands {
    public static final String COPYRIGHT_2009_2012 = Constants.COPYRIGHT_2009_2012;

    static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$
    
    public static final int BACKUP_DEVICE = 1;
    public static final int DELETE_DOMAIN = 2;
    public static final int DELETE_FILE = 3;
    public static final int DELETE_SERVICE = 4;
    public static final int GET_DEVICE_METAINFO = 5;
    public static final int GET_DOMAIN = 6;
    public static final int GET_DOMAIN_DIFFERENCES = 7;
    public static final int GET_DOMAIN_LIST = 8;
    public static final int GET_DOMAIN_STATUS = 9;
    public static final int GET_ERROR_REPORT = 10;
    public static final int GET_INTERDEPENDENT_SERVICES_IMAGE = 11;
    public static final int GET_INTERDEPENDENT_SERVICES_FILE = 12;
    public static final int GET_KEY_FILENAMES = 13;
    public static final int GET_REFERENCED_OBJECTS = 14;
    public static final int GET_SAML_TOKEN = 15;
    public static final int GET_SERVICE_LIST_FROM_DOMAIN = 16;
    public static final int GET_SERVICE_LIST_FROM_EXPORT_IMAGE = 17;
    public static final int GET_SERVICE_LIST_FROM_EXPORT_FILE = 18;
    public static final int IS_DOMAIN_DIFFERENT = 19;
    public static final int PING_DEVICE = 20;
    public static final int QUIESCE_DEVICE = 21;
    public static final int QUIESCE_DOMAIN = 22;
    public static final int QUIESCE_SERVICE = 23;
    public static final int REBOOT = 24;
    public static final int RESTART_DOMAIN = 25;
    public static final int RESTORE_DEVICE = 26;
    public static final int SET_DOMAIN = 27;
    public static final int SET_DOMAIN_BY_SERVICE_IMAGE = 28;
    public static final int SET_DOMAIN_BY_SERVICE_FILE = 29;
    public static final int SET_FILE = 30;
    public static final int SET_FIRMWARE_IMAGE = 31;
    public static final int SET_FIRMWARE_STREAM = 32;
    public static final int SET_FIRMWARE_IMAGE_ACCEPT_LICENSE = 33;
    public static final int SET_FIRMWARE_STREAM_ACCEPT_LICENSE = 34;
    public static final int START_DOMAIN = 35;
    public static final int STOP_DOMAIN = 36;
    public static final int START_SERVICE = 37;
    public static final int STOP_SERVICE = 38;
    public static final int SUBSCRIBE_TO_DEVICE = 39;
    public static final int UNQUIESCE_DEVICE = 40;
    public static final int UNQUIESCE_DOMAIN = 41;
    public static final int UNQUIESCE_SERVICE = 42;
    public static final int UNSUBSCRIBE_FROM_DEVICE = 43;
    
    /////////////////////////////////////////////////////////////////////////////
    // for subscriptions
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Create a subscription so that the device will inform the
     * <code>callback</code> when a configuration change or related action
     * happens on the specified device. For a list of the events that we should
     * be notified of see {@link Notification}. If an attempt is made to create
     * a duplicate subscription to the same device with the same topic, this
     * will not result in receiving duplicate notifications. Only one manager
     * may be subscribed to a device at a time. To check the status of the
     * subscription, use {@link #pingDevice(DeviceContext, String)}.
     * 
     * @param device the DataPower device that the subscriber wants to monitor
     * @param subscriptionId This id is used later to unsubscribe via
     *        {@link #unsubscribeFromDevice(DeviceContext, String, StringCollection)},
     *        and to check the status of the subscription via
     *        {@link #pingDevice(DeviceContext, String)}. This value should be
     *        unique for each subscriber, so it is up to the subscriber to pick
     *        unique ids that will not have collisions on the device.
     * @param topics a collection of String values indicating the topic(s) on
     *        the device to subscribe to
     * @param callback a URL in the subscriber that should be invoked by the
     *        device to send the notification from the device to the subscriber
     * @return a code which indicates if the subscription was successful.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #unsubscribeFromDevice(DeviceContext, String, StringCollection)
     * @see #pingDevice(DeviceContext, String)
     * @see Notification
     * @see NotificationCatcher
     */
    public SubscriptionResponseCode subscribeToDevice(DeviceContext device, 
                                                      String subscriptionId, StringCollection topics, URL callback)
        throws InvalidCredentialsException, AMPIOException, DeviceExecutionException, 
               AMPException;

    /**
     * Delete a subscription that was created with
     * {@link #subscribeToDevice(DeviceContext, String, StringCollection, URL)}.
     * To check the status of the subscription, use
     * {@link #pingDevice(DeviceContext, String)}.
     * 
     * @param device the DataPower device that the subscriber is monitoring
     * @param subscriptionID the subscription id that was returned from
     *        {@link #subscribeToDevice(DeviceContext, String, StringCollection, URL)}
     * @param topics a collection of String values indicating the topic(s) on
     *        the device to unsubscribe from
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>subscriptionID</code>
     *         does not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #subscribeToDevice(DeviceContext, String, StringCollection, URL)
     * @see #pingDevice(DeviceContext, String)
     */
    public void unsubscribeFromDevice(DeviceContext device, String subscriptionID,
                                      StringCollection topics)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * This is used periodically to send a heartbeat request to a device and
     * verify that it responds, and to check on the subscription state. It will
     * block until a response is received or an error condition occurs (timeout,
     * invalid device credentials, etc).
     * 
     * @param device the DataPower device to query
     * @param subscriptionID the subscription id that was returned from
     *        {@link #subscribeToDevice(DeviceContext, String, StringCollection, URL)}.
     *        This will keep the subscription alive on the device, and it will
     *        enable the device to indicate in a PingResponse any error state
     *        regarding the subscription.
     * @return a {@link PingResponse}, which indicates the state of the
     *         subscription on the device.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see PingResponse
     * @see #getDeviceMetaInfo(DeviceContext)
     */
    public PingResponse pingDevice(DeviceContext device, String subscriptionID)
        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for device info
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Get the high-level information about this device, as listed in
     * {@link DeviceMetaInfo}. This info is unlikely to change frequently. All
     * of the returned data will be treated as read-only.
     * 
     * @param device the DataPower device to query.
     * @return a collection of device attributes
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see DeviceMetaInfo
     * @see PingResponse
     */
    public DeviceMetaInfo getDeviceMetaInfo(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Reboot the referenced device, based on the
     * <code>Shutdown -&gt; Reboot System</code> command in the System Control
     * menu of the WebGUI.
     * 
     * @param device the device to reboot
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #restartDomain(DeviceContext, String)
     */
    public void reboot(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for domains
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Get a list of all the domains on the specified device, even the ones not
     * managed by any external manager.
     * 
     * @param device the DataPower device to query.
     * @return a String array representation of domain names. Each item in this
     *         array should be a single domain name. These same String values
     *         can be used on other methods that require a domain name. The
     *         "default" domain will be included in this list.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     */
    public String[] getDomainList(DeviceContext device)
        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Get a domain from the device into an opaque blob (byte array). In
     * DataPower terms, this is a Backup instead of an Export, as we want all
     * the referenced files via a Backup. If you invoke this method on the
     * "default" domain it will include only the service configuration objects.
     * <p>
     * Note that this does not include any items from the <code>cert:</code>,
     * <code>sharedcert:</code>, or <code>pubcert:</code> filestores. Items
     * in those filestores will not be obtained using this method, as those
     * filestores are considered unexportable via AMP, WebGUI, and CLI. It will
     * be the responsbility of the user to manually copy items in those
     * filestores to each device device from their own external store.
     * 
     * @param device
     *            the DataPower device from which to export the domain
     * @param domainName
     *            the domain on the device to export
     * @return an opaque blob that represents the domain. The code does not care
     *         about the contents of this blob, other than it needs to be in a
     *         form that the device can recognized later in
     *         {@link #setDomain(DeviceContext, String, byte[], DeploymentPolicy) setDomain}.
     *         Note that this blob probably (depending upon the implementation)
     *         is a zip file in the same format as if you had performed a
     *         "Create backup of one or more application domains" from the
     *         WebGUI, and you could also use this content on a "Compare
     *         Configuration" in the WebGUI as a "Backup ZIP Bundle".
     *         Additionally, this content may be Base64 encoded (again,
     *         depending on the implementation).
     * @throws InvalidCredentialsException
     *             the device userid and password supplied inside the
     *             <code>DeviceContext</code> parameter were not accepted by
     *             the device.
     * @throws AMPIOException
     *             an I/O problem occurred while communicating to the device via
     *             the network such as hostname not found, connection refused,
     *             connection timed out, etc. For more information regarding the
     *             problem, please refer to this object's method
     *             {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException
     *             the device had an internal error while executing the command.
     *             That internal device error is not described via the other
     *             exceptions thrown by this method. For more information
     *             regarding the problem, please refer to this object's method
     *             {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException
     *             the specified <code>domainName</code> does not exist on the
     *             device.
     * @throws AMPException
     *             the client was unable to parse the response message, or
     *             received an invalid response from the device.
     * @see #setDomain(DeviceContext, String, byte[], DeploymentPolicy)
     */
    public byte[] getDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Load a domain onto a device using an image that was previously retrived
     * via {@link #getDomain(DeviceContext, String)}. In DataPower terms, this
     * is a Restore instead of an Import, as we want all the referenced files,
     * not only the configuration objects. This can also be used to install
     * domains on a device different from where it was originally retrieved.
     * This method should delete any domain of the same name that already exists
     * on the device, load this domain into the running configuration, start all
     * the services that are in this domain, mark the domain as runnable
     * (op-state up), and persist it to the device's non-volatile storage (save
     * config).
     * <p>
     * If a domain was backed up from a device that has a different firmware
     * version that the device it is being restored/installed to, it is assumed
     * that the target device will handle any changes to the domain backup image
     * that may be necessary for it to work properly on the target device.
     * <p>
     * If the domain is being installed to a different deviceType than it was
     * retrieved from (i.e., XS40), it is assumed that loading the domain to a
     * higher function device (i.e., XI50) will work appropriately. Installing
     * the domain on a lower function device (i.e., XA35) may result in domain
     * services that do not behave as desired. Because of this, the caller also
     * needs to keep track of the deviceType that this domain was exported from.
     * 
     * @param device
     *            the DataPower device to which the domain is restored or
     *            installed
     * @param domainName
     *            the name of the domain being loaded
     * @param domainImage
     *            an opaque blob that represents the domain. This is the same
     *            blob that was obtained from
     *            {@link #getDomain(DeviceContext, String)}, see the javadoc
     *            for that method for more information about this blob.
     * @param policy
     *            a deployment policy to be used when setting the domain
     * @throws InvalidCredentialsException
     *             the device userid and password supplied inside the
     *             <code>DeviceContext</code> parameter were not accepted by
     *             the device.
     * @throws AMPIOException
     *             an I/O problem occurred while communicating to the device via
     *             the network such as hostname not found, connection refused,
     *             connection timed out, etc. For more information regarding the
     *             problem, please refer to this object's method
     *             {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException
     *             the device had an internal error while executing the command.
     *             That internal device error is not described via the other
     *             exceptions thrown by this method. For more information
     *             regarding the problem, please refer to this object's method
     *             {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException
     *             the client was unable to parse the response message, or
     *             received an invalid response from the device.
     * @see #getDomain(DeviceContext, String)
     * @see #deleteDomain(DeviceContext, String)
     */
    public void setDomain(DeviceContext device, String domainName, byte[] domainImage, DeploymentPolicy policy)
        throws InvalidCredentialsException,DeviceExecutionException, 
               AMPIOException, AMPException, DeletedException;
    
    /**
     * Delete a DataPower domain and all the contents of that domain from the
     * specified device.
     * 
     * @param device the DataPower device from which to delete the named domain
     * @param domainName the domain on that device to delete. The
     *        <code>default</code> domain cannot be deleted.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #setDomain(DeviceContext, String, byte[], DeploymentPolicy)
     */
    public void deleteDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Get the domain status (op-state). The domain op-state is a rollup
     * of all the services in the domain. A change in status should trigger a
     * {@link Notification}.
     * 
     * @param device the DataPower device on which the domain resides
     * @param domainName the domain to get the status of
     * @return a contains of status indicators
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see Notification
     * @see #startDomain(DeviceContext, String)
     * @see #stopDomain(DeviceContext, String)
     */
    public DomainStatus getDomainStatus(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Set the domain op-state to "up".
     * 
     * @param device the device in which the domain resides
     * @param domainName the name of the domain to start
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #stopDomain(DeviceContext, String)
     * @see #getDomainStatus(DeviceContext, String)
     */
    public void startDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Set the domain op-state to "down".
     * 
     * @param device the device in which the domain resides
     * @param domainName the name of the domain to stop
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #startDomain(DeviceContext, String)
     * @see #getDomainStatus(DeviceContext, String)
     */
    public void stopDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Restart the referenced domain on the referenced device, based on the
     * action of the same name in the System Control menu of the WebGUI.
     * 
     * @param device the device in which the domain resides
     * @param domainName the name of the domain to restart
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #reboot(DeviceContext)
     */
    public void restartDomain(DeviceContext device, String domainName)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for firmware
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Transmit the specified firmware image into the device, make it the active
     * firmware.
     * <p>
     * If a change in the firmware causes either an existing configuration
     * element to no longer be valid, or a missing configuration element to be
     * required, it is assumed that the target device will handle any changes to
     * the configuration element that may be necessary for it to work properly.
     * <p>
     * A single SOAP method on the device will be used by both this method and
     * {@link #setFirmware(DeviceContext, InputStream)}. This method will
     * return after the firmware has been installed and a reboot is scheduled.
     * 
     * @param device the device to load the firmware onto
     * @param firmwareImage an opaque blob firmware binary image as retrieved
     *        from the DataPower web site or from distributable media
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #setFirmware(DeviceContext, InputStream)
     */
    @Deprecated
    public void setFirmware(DeviceContext device, byte[] firmwareImage)
        throws InvalidCredentialsException, DeviceExecutionException, 
               AMPIOException, AMPException;
    
    /**
     * Same as {@link #setFirmware(DeviceContext, byte[])}, but allows for the
     * large boot image to be specified in in a stream instead of in-memory byte
     * array. The image can be 20MB or larger, so for performance purposes you
     * may not want it in a byte array.
     * <p>
     * A single SOAP method on the device will be used by both this method and
     * {@link #setFirmware(DeviceContext, byte[])}.
     * 
     * @param device the device to load the firmware onto
     * @param inputStream an inputStream to an opaque blob firmware binary image
     *        as retrieved from the DataPower web site or from distributable
     *        media. It is expected that this inputStream references content
     *        that is already base64-encoded. This is a special case not present
     *        on the other AMP commands. The reason for this is that firmware is
     *        such a large object 15-30MB that doing a base64 encode to prepare
     *        it for transmission each time it is transmitted to a device is a
     *        memory-intensive operation that we need to avoid. If it was
     *        unencoded, XMLBeans would create multiple copies of this large
     *        blob, which may blow up the heap size and cause out-of-memory
     *        errors. So to tune for this situation, the firmware blob is stored
     *        in the repository already encoded in base64 format, and thus does
     *        not need to be encoded for transmission to devices.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #setFirmware(DeviceContext, byte[])
     */
    @Deprecated
    public void setFirmware(DeviceContext device, InputStream inputStream)
        throws InvalidCredentialsException, DeviceExecutionException, 
               AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for special key handling
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * For the specified domain, get a list of filenames of items in the special
     * device filestores that are used by services in this domain. This would
     * include the <code>cert:</code>, <code>sharedcert:</code>, and
     * <code>pubcert:</code> filestores. Those device filestores are
     * considered unexportable. This means that items in these filestores will
     * not be included in a domain backup, they need to be installed separately
     * when installing the backup image. This method will enable the application
     * to determine which keys are needed when installing the backup image.
     * Those needed keys could be pulled from an external repository, or the
     * administrator could be prompted to manually supply these key files. This
     * does not export the keys from the device, it only provides their names.
     * This is a nice-to-have, not a requirement.
     * 
     * @param device the DataPower device which has the specified domain
     * @param domainName the domain to check for key filenames
     * @return an array of filenames in the format used by the device, i.e.,
     *         <code>cert:///mycert.pem</code>. These filename should be
     *         usable in the
     *         {@link #setFile(DeviceContext, String, String, byte[])}method
     *         and should include the name of the filestore.
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #setFile(DeviceContext, String, String, byte[])
     */
    public String[] getKeyFilenames(DeviceContext device, String domainName)
        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Send a file to the persistent store on the device. This will likely be
     * used to install a key file to a device. The name of the key file may have
     * been retrieved from {@link #getKeyFilenames(DeviceContext, String)}. But
     * it could be used to install any file to any of the device filestores.
     * 
     * @param device the DataPower device which has the specified domain
     * @param domainName the domain in which to install the file
     * @param filenameOnDevice the name of the file in the format used by the
     *        device, i.e., <code>cert:///mycert.pem</code>
     * @param contents the raw file contents (i.e., no Base64 encoding)
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws NotExistException the specified <code>domainName</code> does
     *         not exist on the device.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     * @see #getKeyFilenames(DeviceContext, String)
     */
    public void setFile(DeviceContext device, String domainName, 
                        String filenameOnDevice, byte[] contents)
        throws NotExistException, InvalidCredentialsException, 
               DeviceExecutionException, AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for settings
    /////////////////////////////////////////////////////////////////////////////
    
//    /**
//     * Get the device-specific settings that are potentially clonable to other
//     * devices. Clonable settings excludes items which must be unique, such as
//     * interface IP address. It also excludes all service configuration objects.
//     * Clonable settings includes device configuration objects that are unique
//     * to the "default" domain (i.e., the "Network" [not including "Ethernet
//     * Interface"], "Management", "Access", and "Systems" sections in the
//     * WebGUI's "Objects" navbar) which can be copied as-is to other devices.
//     * These settings would be returned as an opaque blob.
//     * <p>
//     * This will be used to replicate the device configuration across all the
//     * devices in a managedSet. If a clonable device-specific setting is modified
//     * and persisted inside the device, a notification should be
//     * sent. DataPower configuration objects related to services (can exist in a
//     * domain other than "default") are not considered clonable device settings,
//     * those are domains.
//     * <p>
//     * Note that this does not include the RBM passwords. They are unexportable
//     * for security reasons.
//     * 
//     * @param device the DataPower device from which to get the clonable
//     *        device-specific settings
//     * @return an opaque blob that represents the clonable device-specific
//     *         settings. It needs to be in a form that the device can be
//     *         recognized later in
//     *         {@link #setClonableDeviceSettings(DeviceContext, byte[]) setClonableDeviceSettings}.
//     * @throws InvalidCredentialsException the device userid and password
//     *         supplied inside the <code>DeviceContext</code> parameter were
//     *         not accepted by the device.
//     * @throws AMPIOException an I/O problem occurred while communicating to the
//     *         device via the network such as hostname not found, connection
//     *         refused, connection timed out, etc. For more information
//     *         regarding the problem, please refer to this object's method
//     *         {@link Throwable#getCause() getCause()}.
//     * @throws DeviceExecutionException the device had an internal error while
//     *         executing the command. That internal device error is not
//     *         described via the other exceptions thrown by this method. For
//     *         more information regarding the problem, please refer to this
//     *         object's method {@link Throwable#getMessage() getMessage()}.
//     * @throws AMPException the client was unable to parse the response message,
//     *    or received an invalid response from the device.
//     * @see #setClonableDeviceSettings(DeviceContext, byte[])
//     * @see #getDomain(DeviceContext, String)
//     * @see Notification
//     */
//    public byte[] getClonableDeviceSettings(DeviceContext device)
//        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
//
//    /**
//     * Write the clonable device-specific settings to the specified device using
//     * the blob that was previously obtained from
//     * {@link #getClonableDeviceSettings(DeviceContext)}. This method should
//     * persist the settings to the device's non-volatile storage, and make the
//     * changes immmediately active. This method would be used to rollback the 
//     * device-specific settings to a previous version, or can also be used to 
//     * copy settings from one managed set to another managed set.
//     * <p>
//     * If the image was created from a device that has a different firmware
//     * version that the device it is being set to, it is assumed that the target
//     * device will handle any changes to the image that may be necessary for it
//     * to work properly on the target device. If the image was created from a
//     * different deviceType, it is assumed that the device will also handle any
//     * changes to make the image work properly on the target device.
//     * 
//     * @param device the DataPower device to which the clonable device-specific
//     *        settings are restored or installed
//     * @param settingsImage an opaque blob that represents the settings. This is
//     *        the same blob that was obtained from
//     *        {@link #getClonableDeviceSettings(DeviceContext) getClonableDeviceSettings}.
//     * @throws InvalidCredentialsException the device userid and password
//     *         supplied inside the <code>DeviceContext</code> parameter were
//     *         not accepted by the device.
//     * @throws AMPIOException an I/O problem occurred while communicating to the
//     *         device via the network such as hostname not found, connection
//     *         refused, connection timed out, etc. For more information
//     *         regarding the problem, please refer to this object's method
//     *         {@link Throwable#getCause() getCause()}.
//     * @throws DeviceExecutionException the device had an internal error while
//     *         executing the command. That internal device error is not
//     *         described via the other exceptions thrown by this method. For
//     *         more information regarding the problem, please refer to this
//     *         object's method {@link Throwable#getMessage() getMessage()}.
//     * @throws AMPException the client was unable to parse the response message,
//     *    or received an invalid response from the device.
//     * @see #getClonableDeviceSettings(DeviceContext)
//     */
//    public void setClonableDeviceSettings(DeviceContext device, byte[] settingsImage)
//        throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;

    /////////////////////////////////////////////////////////////////////////////
    // for diff
    /////////////////////////////////////////////////////////////////////////////
    
    // TODO: does the diff need to be performed on a particular device, such as one that has the same firmware level?
    
    /**
     * Compare two configuration images obtained from
     * {@link #getDomain(DeviceContext, String)}, and see if they are
     * equivalent. Because this application does not have any knowledge of the
     * device configuration object model (it is opaque), the diff is performed
     * on the device.
     * <p>
     * See {@link #getDomainDifferences(String, byte[], byte[], DeviceContext)}
     * for an alternate way to visually display the blob differences.
     * 
     * @param domainName
     *            the name of the domain specified in configImage1
     * @param configImage1
     *            an opaque blob obtained from getDomain()
     * @param configImage2
     *            an opaque blob obtained from getDomain() If this parameter is
     *            null, then the device should use its own persisted domain as
     *            the second input to the comparison. If this parameter is not
     *            null, then the comparison can be performed on any device, it
     *            does not require that the device already contain the domain,
     *            or that the device firmware match the firmware used when the
     *            domain was retrieved.
     * @param device
     *            a device on which to execute the comparison
     * @return true if there are differences, false if they are equivalent
     * @throws InvalidCredentialsException
     *             the device userid and password supplied inside the
     *             <code>DeviceContext</code> parameter were not accepted by
     *             the device.
     * @throws AMPIOException
     *             an I/O problem occurred while communicating to the device via
     *             the network such as hostname not found, connection refused,
     *             connection timed out, etc. For more information regarding the
     *             problem, please refer to this object's method
     *             {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException
     *             the device had an internal error while executing the command.
     *             That internal device error is not described via the other
     *             exceptions thrown by this method. For more information
     *             regarding the problem, please refer to this object's method
     *             {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException
     *             the client was unable to parse the response message, or
     *             received an invalid response from the device.
     * @see #getDomainDifferences(String, byte[], byte[], DeviceContext)
     */
    public boolean isDomainDifferent(String domainName, byte[] configImage1, 
                                     byte[] configImage2, DeviceContext device) throws InvalidCredentialsException, 
                                     DeviceExecutionException, AMPIOException, AMPException;
    
    // TODO: does the diff need to be performed on a particular device, such as one that has the same firmware level?
    
//    /**
//     * Compare two configuration images obtained from
//     * {@link #getClonableDeviceSettings(DeviceContext)}, and see if they are
//     * equivalent. Because this application does not have any knowledge of the
//     * device configuration object model (it is opaque), the diff is performed
//     * on the device.
//     * <p>
//     * 
//     * @param configImage1 an opaque blob obtained from getClonableDeviceSettings()
//     * @param configImage2 an opaque blob obtained from getClonableDeviceSettings() 
//     *   If this parameter is null, then the device should use its own 
//     *       persisted settings as the second input to the comparison. If this 
//     *        parameter is not null, then the comparison can be performed on 
//     *        any device, it does not require that the device already contain 
//     *        the settings, or that the device firmware match the firmware used 
//     *        when the settings was retrieved.
//     * @param device a device on which to execute the comparison
//     * @return true if there are differences, false if they are equivalent
//     * @throws InvalidCredentialsException the device userid and password
//     *         supplied inside the <code>DeviceContext</code> parameter were
//     *         not accepted by the device.
//     * @throws AMPIOException an I/O problem occurred while communicating to the
//     *         device via the network such as hostname not found, connection
//     *         refused, connection timed out, etc. For more information
//     *         regarding the problem, please refer to this object's method
//     *         {@link Throwable#getCause() getCause()}.
//     * @throws DeviceExecutionException the device had an internal error while
//     *         executing the command. That internal device error is not
//     *         described via the other exceptions thrown by this method. For
//     *         more information regarding the problem, please refer to this
//     *         object's method {@link Throwable#getMessage() getMessage()}.
//     * @throws AMPException the client was unable to parse the response message,
//     *    or received an invalid response from the device.
//     * @see #getSettingsDifferences(byte[], byte[], DeviceContext)
//     */
//    public boolean isSettingsDifferent(byte[] configImage1, byte[] configImage2, 
//                                       DeviceContext device) throws InvalidCredentialsException, 
//                                       DeviceExecutionException, AMPIOException, AMPException;

    /**
     * Compare two configuration images obtained from
     * {@link #getDomain(DeviceContext, String) getDomainViaBackup}, and return
     * a URL where the differences can be viewed. Because the application does
     * not have any knowledge of the device configuration object model (it is
     * opaque), the diff is performed on the device.
     * <p>
     * Because the blob parameters are basically a ZIP backup (see
     * {@link #getDomain(DeviceContext, String)}, you could also use the WebGUI
     * (Compare Configuration - Backup ZIP Bundle) with these blobs as an
     * alternate way to display the differences.
     * 
     * @param domainName
     *            the name of the domain specified in configImage1
     * @param configImage1
     *            an opaque blob obtained from getDomain()
     * @param configImage2
     *            an opaque blob obtained from getDomain(). If this parameter is
     *            null, then the device should use its own persisted domain as
     *            the second input to the comparison. If this parameter is not
     *            null, then the comparison can be performed on any device, it
     *            does not require that the device already contain the domain,
     *            or that the device firmware match the firmware used when the
     *            domain was retrieved.
     * @param device
     *            a device on which to execute the difference fetch. If
     *            <code>configImage2</code> is not null, then the domain image
     *            does not need to exist on the device.
     * @return a URL where a representation of the differences between the two
     *         configImages is rendered visually without requiring the
     *         application to have knowledge of the device configuration object
     *         model.
     * @throws InvalidCredentialsException
     *             the device userid and password supplied inside the
     *             <code>DeviceContext</code> parameter were not accepted by
     *             the device.
     * @throws AMPIOException
     *             an I/O problem occurred while communicating to the device via
     *             the network such as hostname not found, connection refused,
     *             connection timed out, etc. For more information regarding the
     *             problem, please refer to this object's method
     *             {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException
     *             the device had an internal error while executing the command.
     *             That internal device error is not described via the other
     *             exceptions thrown by this method. For more information
     *             regarding the problem, please refer to this object's method
     *             {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException
     *             the client was unable to parse the response message, or
     *             received an invalid response from the device.
     * @see #isDomainDifferent(String, byte[], byte[], DeviceContext)
     */
    
    public URL getDomainDifferences(String domainName, byte[] configImage1, 
                                    byte[] configImage2, DeviceContext device) 
        throws InvalidCredentialsException, DeviceExecutionException, 
               AMPIOException, AMPException;
    
//    /**
//     * Compare two configuration images obtained from
//     * {@link #getClonableDeviceSettings(DeviceContext) getClonableDeviceSettings}
//     * and return a URL where the differences can be viewed. Because the
//     * application does not have any knowledge of the device configuration
//     * object model (it is opaque), the diff is performed on the device.
//     * 
//     * @param configImage1 an opaque blob obtained from
//     *        getClonableDeviceSettings()
//     * @param configImage2 an opaque blob obtained from
//     *        getClonableDeviceSettings(). If this parameter is null, then the
//     *        device should use its own persisted settings as the second input
//     *        to the comparison. If this parameter is not null, then the
//     *        comparison can be performed on any device, it does not require
//     *        that the device already contain the settings, or that the device
//     *        firmware match the firmware used when the settings was retrieved.
//     * @param device a device on which to execute the difference fetch. If
//     *        <code>configImage2</code> is not null, then the domain image
//     *        does not need to exist on the device.
//     * @return a URL where a representation of the differences between the two
//     *         configImages is rendered visually without requiring the
//     *         application to have knowledge of the device configuration object
//     *         model.
//     * @throws InvalidCredentialsException the device userid and password
//     *         supplied inside the <code>DeviceContext</code> parameter were
//     *         not accepted by the device.
//     * @throws AMPIOException an I/O problem occurred while communicating to the
//     *         device via the network such as hostname not found, connection
//     *         refused, connection timed out, etc. For more information
//     *         regarding the problem, please refer to this object's method
//     *         {@link Throwable#getCause() getCause()}.
//     * @throws DeviceExecutionException the device had an internal error while
//     *         executing the command. That internal device error is not
//     *         described via the other exceptions thrown by this method. For
//     *         more information regarding the problem, please refer to this
//     *         object's method {@link Throwable#getMessage() getMessage()}.
//     * @throws AMPException the client was unable to parse the response message,
//     *         or received an invalid response from the device.
//     * @see #isSettingsDifferent(byte[], byte[], DeviceContext)
//     */
//    
//    public URL getSettingsDifferences(byte[] configImage1, byte[] configImage2,
//                                      DeviceContext device) throws InvalidCredentialsException, 
//                                      DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Retrieve error report stored on the device.
     * 
     * @param device a device on which to retrieve the error report
     * @return an ErrorReport object which lists the domain, location, filename, and
     *    actual bytes of the error report from the device
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     */
    public ErrorReport getErrorReport(DeviceContext device) 
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException;
    
    /**
     * Retrieve a SAML token for automatic login to the WebGUI. This SAML token
     * (artifact) is needed when generating a URL so that an external web
     * browser can automatically logon to the device's WebGUI (do not need to
     * enter administrative userid and password in the web browser).
     * 
     * @param device the deviceContext of the device on which to retrieve the
     *        SAML token from
     * @param domainName the domainName to be forwarded to
     * @return a String containing the SAML token
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     */
    
    public String getSAMLToken(DeviceContext device, String domainName) 
        throws InvalidCredentialsException, DeviceExecutionException,
               AMPIOException, AMPException;

    /**
     * Perform a backup of the entire device.  The purpose is to be able to restore a 
     * device to the same state upon failure.
     *   
     * This includes all configuration, crypto material, and user credentials 
     * from the device.
     * 
     * @param device the deviceContext of the device to perform a backup on
     * @param cryptoCertificateName the cyrpto certificate object name
     * @param cryptoImage the content of the certificate used to encrypt the backup
     * @param secureBackupDestination the location where the backed up files will be stored
     * @return Hashtable with filenames as Key and byte[] as values
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException 
     */    
    public Hashtable backupDevice(DeviceContext device, String cryptoCertificateName, byte[] cryptoImage, String secureBackupDestination
,    		boolean includeISCSI, boolean includeRaid) throws AMPIOException, InvalidCredentialsException, AMPException; 

    /**
     * Restore a device from a backup.
     *   
     * @param device the deviceContext of the device to perform the restore
     * @param cryptoCredentialName the object name of the Crypto Identification Credentials used to decrypt the backup
     * @param secureBackupSource location of the backup files. This may be local: temporary: or ftp
     * @param backupFilesTable a Hashtable with the file names(keys) and contents (values) of the backup file, if the were
     * saved on a file system and have to embedded in the restore request
     * 
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException
     */    
    public void restoreDevice(DeviceContext device, String cryptoCredentialName, boolean validate, URI secureBackupSource, 
    		Hashtable<String,byte[]> backupFilesTable) throws AMPIOException, InvalidCredentialsException, AMPException; 
    
    /**
     * Quiesce a domain.  The purpose is to allow domain modifications or perform 
     * troubleshooting without affecting data traffic.
     *   
     * @param device the deviceContext of the device containing the domain to quiesce
     * @param domain the name of the domain to quiesce
     * @param timeout the amount of time in seconds to wait for the quiesce to complete.
     *   Once the timeout is reached, existing connections may be terminated.  
     *
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException 
     */    
    public void quiesceDomain(DeviceContext device, String domain, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException; 
 
    /**
     * Unquiesce a domain which brings the domain objects to an operationally ready state.
     *   
     * @param device the deviceContext of the device containing the domain to unquiesce
     * @param domain the name of the domain to unquiesce
     * 
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException 
     */    
    public void unquiesceDomain(DeviceContext device, String domain) throws AMPIOException, InvalidCredentialsException, AMPException; 
 
    /**
     * Quiesce a device.  The purpose is to allow firmware modifications or perform 
     * troubleshooting without affecting data traffic.
     *   
     * @param device the deviceContext of the device to quiesce
     * @param timeout the amount of time in seconds to wait for the quiesce to complete.
     *   Once the timeout is reached, existing connections may be terminated.  
     * 
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException 
     */    
    public void quiesceDevice(DeviceContext device, int timeout) throws AMPIOException, InvalidCredentialsException, AMPException; 

    /**
     * Unquiesce a device, which brings the device to an operationally ready state.
     *   
     * @param device the deviceContext of the device to unquiesce
     * 
     * @throws AMPException 
     * @throws InvalidCredentialsException 
     * @throws AMPIOException 
     */    
    public void unquiesceDevice(DeviceContext device) throws AMPIOException, InvalidCredentialsException, AMPException; 
    
    
	// =========================== New functions for provider V3 ===========================    
    /**
     * Getting all the services under that domain together with their status.
     * 
     * @param device the DataPower device to query.
     * @param domainName the domain on that device to get the service List
     * 
     * @return a String array representation of all service. Each item in this
     *         array should be a configObject.
     *         
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     */
    public RuntimeService[] getServiceListFromDomain(DeviceContext device, String domainName)
        throws DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Getting inter-dependent service on device.
     * Given an export/backup package, a domain name and a list of services,
     * return all the services in that domain on the device that are dependent with 
     * any of the dependent configuration objects of the specified services
     * in the export package. The purpose is to check if the services to be 
     * deployed has any interdependencies with services already on device.
     * 
     * @param device the DataPower device to query.
     * @param domainName the domain on that device to get the inter dependent services
     * @param fileDomainName the domain on that device to get the export/backup package 
     * @param fileNameOnDevice the name of export/backup package in the format used by the
     *        device, i.e., <code>temporary:///package.zip</code>
     * @param objectArray the objects context array
     * 
     * @return the InterDependentServiceCollection object
	 * 
	 * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     * @see #getInterDependentServices(DeviceContext, String, byte[], ConfigObject[])
     **/
    public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, String fileDomainName
    		, String fileNameOnDevice, ConfigObject[] objects)
    	throws NotExistException, InvalidCredentialsException,DeviceExecutionException, 
           AMPIOException, AMPException;
    
    /**
     * Getting inter-dependent service on device.
     * Given an export/backup package, a domain name and a list of services,
     * return all the services in that domain on the device that are dependent with
     * any of the dependent configuration objects of the specified services
     * in the export package. The purpose is to check if the services to be deployed has
     * any interdependencies with services already on device.
     * 
     * @param device the DataPower device to get the interDependentServices
     * @param domainName the domain on that device to get the inter dependent services
     * @param packageImage an opaque blob that represents the of export/backup package,
     * 			It is expected that this packageImage content is already base64-encoded.
     * 
     * @return the InterDependentServiceCollection object
	 * 
	 * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     * @see #getInterDependentServices(DeviceContext, String, String, String, ConfigObject[])
     **/
    public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName, byte[] packageImage, ConfigObject[] objectArray)
    	throws NotExistException, InvalidCredentialsException,DeviceExecutionException, 
           AMPIOException, AMPException;
    
    
    /**
     * Getting all services from an export/backup package. Given an export/backup package, 
     * return list of services in that package that are supported on the device. 
     * 
     * @param device the DataPower device to get Service List from export/backup package
     * @param fileDomainName the domain in which the export/backup package is 
     * @param fileNameOnDevice
     * 			the name of export/backup package in the format used by the
     *        	device, i.e., <code>temporary:///package.zip</code>
     * 
     * @return the ConfigObject array contains the services
	 * 
	 * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     * @see #getServiceListFromExport(DeviceContext, byte[])
     **/
    public ConfigService[] getServiceListFromExport(DeviceContext device, String fileDomainName, String fileNameOnDevice)
    	throws NotExistException, InvalidCredentialsException,DeviceExecutionException, 
           AMPIOException, AMPException;
    
    /**
     * Getting all services from an export/backup package. Given an export/backup package, return list of services in that package that are supported on the device. 
     * 
     * @param device the DataPower device to get service list from export/backup package
     * @param packageImage an opaque blob represents the export/backup package, It is expected 
     * 		  that this packageImage content is already base64-encoded.
     * 
     * @return the Service array contains the services
	 * 
	 * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     * @see #getServiceListFromExport(DeviceContext, String, String)
     **/
    public ConfigService[] getServiceListFromExport(DeviceContext device, byte[] packageImage)
    	throws NotExistException, InvalidCredentialsException,DeviceExecutionException, 
           AMPIOException, AMPException;
    
    /**
     * Get the referenced objects
     * 
     * @param device the DataPower device to get referenced objects
     * @param domainName the domain in where the object to get the referenced
     * @param objectName the name of object
     * @param objectClassName the class name of the object
     * 
     * @return the ObjectCollection object contains the detail of referenced objects
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException     
     **/
    public ReferencedObjectCollection getReferencedObjects(DeviceContext device, String domainName, String objectName, String objectClassName)
    	throws NotExistException, InvalidCredentialsException,DeviceExecutionException, 
           AMPIOException, AMPException;
    
    /**
     * Given domain name, service class, service name, delete the service object and all its dependent objects. 
     * If desired, user can specify the object they do NOT want to delete. The response will contain the deleting results for each object to indicate if an object is deleted or excluded
     * 
     * @param device the DataPower device to delete service
     * @param domainName the domain in which the service to be deleted
     * @param objectName name of the service to be deleted
     * @param objectClassName class name of the service to be delted
     * @param excludeObjects Object array to be deleted exclusively
     * @param deleteReferencedFiles a boolean value if delete the referenced files
     * 
     * @return ConfigObjectStatus array contains the detail of sevices to be deleted
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     **/
    public DeleteObjectResult[] deleteService(DeviceContext device, String domainName, String objectName, String objectClassName, 
    		ConfigObject [] excludeObjects, boolean deleteReferencedFiles)
    	throws NotExistException, InvalidCredentialsException, 
           DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Quiesce all the services specified in the request for the domain. One timeout value will be used for all services. 
     *
     * @param device the DataPower device to quiesce the service
     * @param domainName the name of domain in which the services to be quiesced
     * @param objects the service object to be quiesced
     * @param timeout the amount of time in seconds to wait for the quiesce to complete. Once the timeout is reached, existing connections may be terminated.
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     */
    public void quiesceService(DeviceContext device, String domain, ConfigObject[] objects, int timeout) 
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * UnQuiesce all the services specified in the request for the specified domain.
     * 
     * @param device the DataPower device to unquiesce the service
     * @param domainName the name of domain in which the services to be unquiesced
     * @param objects the service objects to be unquiesced
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     */
    public void unquiesceService(DeviceContext device, String domainName, ConfigObject[] objects) 
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Start a service by changing its administrative state to "enabled"
     * 
     * @param device the DataPower device to start the services
     * @param domainName the name of domain in which services to be started
     * @param objects the service objects to be started
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException 
     */
    public void startService(DeviceContext device, String domainName, ConfigObject[] objects) 
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Stop a service by changing its administrative state to "disabled"
     * 
     * @param device the DataPower device to start the service
     * @param domainName the name of domain in which services to be stoped
     * @param objects the service objects to be stoped
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     */
    public void stopService(DeviceContext device, String domainName, ConfigObject[] objects) 
    	throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Given an export/backup package, a domain name and a list of services, 
     * all the dependent configuration objects and files will be deploied to 
     * the specified domain. Objects and files with the same names will be overwritten. 
     * Dependent services should be quiesced separately first.
     * 
     * @param device the DataPower device to which the domain is restored or installed.
     * @param domainName the name of the domain being loaded
     * @param objects services want to be set
     * @param domainImage 
     * 			an opaque blob that represents the domain. This is the same
     *          blob that was obtained from {@link #getDomain(DeviceContext, String)}, see the javadoc
     *          for that method for more information about this blob.
     * @param policy a deployment policy to be used when setting the domain
     * @param importAllFiles a boolean value if import all files in the export/backup package
     * 
     * @throws InvalidCredentialsException     
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * 
     * @see #quiesceService(DeviceContext, String, ConfigObject[], int)
     */
    public void setDomainByService(DeviceContext device, String domainName, ConfigObject[] objects, byte[] domainImage,
    			DeploymentPolicy policy, boolean importAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException;
    
    /**
     * Given an export/backup package, a domain name and a list of services, 
     * all the dependent configuration objects and files will be deploy to the 
     * specified domain. Objects and files with the same names will be overwritten. 
     * Dependent services should be quiesced separately first.
     *  
     * @param device the DataPower device to which the domain is restored or installed.
     * @param domainName the name of the domain to be set
     * @param objects services want to be set
     * @param fileDomainName the domain in which to get the file
     * @param filenameOnDevice the name of the file in the format used by the
     *        device, i.e., <code>temporary:///package.zip</code>     * 			
     * @param policy a deployment policy to be used when setting the domain
     * @param importAllFiles a boolean value if import all files in the export/backup package
     * 
     * @throws InvalidCredentialsException
     * @throws DeviceExecutionException
     * @throws AMPIOException
     * @throws AMPException
     * @throws DeletedException
     */
    public void setDomainByService(DeviceContext device, String domainName,	ConfigObject[] objects, String fileDomainName, String fileNameOnDevice,
    			DeploymentPolicy policy, boolean importAllFiles)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException, DeletedException;
    
    /**
     * Transmit the specified firmware image into the device, make it the active
     * firmware.
     * <p>
     * If a change in the firmware causes either an existing configuration
     * element to no longer be valid, or a missing configuration element to be
     * required, it is assumed that the target device will handle any changes to
     * the configuration element that may be necessary for it to work properly.
     * <p>
     * A single SOAP method on the device will be used by both this method and
     * {@link #setFirmware(DeviceContext, InputStream)}. This method will
     * return after the firmware has been installed and a reboot is scheduled.
     * 
     * @param device the device to load the firmware onto
     * @param firmwareImage an opaque blob firmware binary image as retrieved
     *        from the DataPower web site or from distributable media
     * @param acceptLicense a boolean value if accept license, must be ture to continue
     * 		  the firmware setting.
     * 
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #setFirmware(DeviceContext, InputStream)
     */
    public void setFirmware(DeviceContext device, byte[] firmwareImage, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    
    /**
     * Same as {@link #setFirmware(DeviceContext, byte[], boolean)}, but allows for the
     * large boot image to be specified in in a stream instead of in-memory byte
     * array. The image can be 20MB or larger, so for performance purposes you
     * may not want it in a byte array.
     * <p>
     * A single SOAP method on the device will be used by both this method and
     * {@link #setFirmware(DeviceContext, byte[], boolean)}.
     * 
     * @param device the device to load the firmware onto
     * @param inputStream an inputStream to an opaque blob firmware binary image
     *        as retrieved from the DataPower web site or from distributable
     *        media. It is expected that this inputStream references content
     *        that is already base64-encoded. This is a special case not present
     *        on the other AMP commands. The reason for this is that firmware is
     *        such a large object 15-30MB that doing a base64 encode to prepare
     *        it for transmission each time it is transmitted to a device is a
     *        memory-intensive operation that we need to avoid. If it was
     *        unencoded, XMLBeans would create multiple copies of this large
     *        blob, which may blow up the heap size and cause out-of-memory
     *        errors. So to tune for this situation, the firmware blob is stored
     *        in the repository already encoded in base64 format, and thus does
     *        not need to be encoded for transmission to devices.
     * @param acceptLicense a boolean value if accept license, must be ture to continue
     * 		  the firmware setting.
     * 
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *         or received an invalid response from the device.
     * @see #setFirmware(DeviceContext, byte[])
     */
    public void setFirmware(DeviceContext device, InputStream inputStream, boolean acceptLicense) 
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
    /**
     * Delete a file to the persistent store on the device. 
     * 
     * @param device the DataPower device which has the specified domain
     * @param domainName the domain in which to delete the file
     * @param filenameOnDevice the name of the file in the format used by the
     *        device, i.e., <code>temporary:///package.zip</code>
     * 
     * @throws InvalidCredentialsException the device userid and password
     *         supplied inside the <code>DeviceContext</code> parameter were
     *         not accepted by the device.
     * @throws AMPIOException an I/O problem occurred while communicating to the
     *         device via the network such as hostname not found, connection
     *         refused, connection timed out, etc. For more information
     *         regarding the problem, please refer to this object's method
     *         {@link Throwable#getCause() getCause()}.
     * @throws DeviceExecutionException the device had an internal error while
     *         executing the command. That internal device error is not
     *         described via the other exceptions thrown by this method. For
     *         more information regarding the problem, please refer to this
     *         object's method {@link Throwable#getMessage() getMessage()}.
     * @throws AMPException the client was unable to parse the response message,
     *    or received an invalid response from the device.
     *    
     * @see #getKeyFilenames(DeviceContext, String)
     * @see #setFile(DeviceContext, String, String, byte[])
     */
    public void deleteFile(DeviceContext device, String domainName, String fileNameOnDevice)
		throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException;
    
}
