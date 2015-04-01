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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Credential;
import com.ibm.datapower.amt.DeviceLogger;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.amp.NotificationCatcher;
import com.ibm.datapower.amt.amp.NotificationCatcherFactory;
import com.ibm.datapower.amt.clientAPI.ManagerStatus.Enumerated;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.RepositoryFactory;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.dataAPI.StoredVersion;
import com.ibm.datapower.amt.dataAPI.local.filesystem.StoredTagImpl;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.amt.soma.Status;

/**
 * A singleton that is the root management object that starts and stops all
 * other management objects/daemons. This object should exist for the duration
 * of the management, which basically means "always". When working with
 * the clientAPI, you must start with the Manager object.
 * <p>
 * 
 */
public class Manager extends WorkArea implements Persistable {
    
    /*
     * Do not reference the "options" member directly, except when fetching the
     * Credential. You should be using the methods in the Configuration class to
     * get configuration data, even to get the configuration data that was
     * passed to us in this Map.
     */
    private volatile Map options = null;
    private volatile NotificationCatcher notificationCatcher = null;
    private volatile Repository repository = null;
    private volatile Lock unmanagedDevicesLock = null;
    private volatile String primarySubscriptionId = null;
    private volatile String secondarySubscriptionId = null;
    private volatile QueueProcessor queueProcessor = null;
    private volatile HeartbeatDaemon heartbeatDaemon = null;
    private volatile DomainSynchronizationDaemon domainSynchDaemon = null;    
    private volatile Queue notificationProgresses = null;
    private volatile Queue heartbeatProgresses = null;
    private volatile Queue domainSyncProgresses = null;    
    // don't need backgroundTaskProgresses because client already has those via return value
    private volatile ManagerStatus managerStatus = null;
    private volatile boolean shutdownRequested = false;    
    private volatile static Manager singleton = null;
    
    // Keeps track of active domainSynchronization Tasks started by domain operations, DS daemon or Notifications
    // Before Manager shuts down, it will wait for the active tasks to complete. 
    // Any tasks that complete are immediately removed from the hashtable
    private volatile Hashtable<String, DomainSynchronizationTask> domainSyncTasksInProgress = new Hashtable<String, DomainSynchronizationTask>();
    
    public static final String version = Constants.WAMT_VERSION;
    /**
     * When invoking {@link #save(boolean)}, use this value to indicate that if
     * the persistence has forked (a change in data was made by another vehicle
     * after it was loaded here), that the method should focus on data
     * consistency and fail the save operation.
     * 
     * @see #SAVE_FORCED
     */
    public static final boolean SAVE_UNFORCED = false;
    
    /**
     * When invoking {@link #save(boolean)}, use this value to indicate that
     * the new values held by this process should overwrite whatever may have
     * been changed by another vehicle.
     * 
     * @see #SAVE_UNFORCED
     */
    public static final boolean SAVE_FORCED = true;
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Commands} interface for AMPv1.
     * 
     * @see Configuration#KEY_COMMANDS_IMPL
     */
    public static final String OPTION_COMMANDS_IMPL = "commandsV1"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating prefix for all messages
     *     * 
     * @see Configuration#KEY_MESSAGE_PREFIX
     */
    public static final String OPTION_MESSAGE_PREFIX = "messagesPrefix"; //$NON-NLS-1$

    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Commands} interface for AMPv2.
     * 
     * @see Configuration#KEY_COMMANDS_V2_IMPL
     */
    public static final String OPTION_COMMANDS_V2_IMPL = "commandsV2"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Commands} interface for AMPv3.
     * 
     * @see Configuration#KEY_COMMANDS_V3_IMPL
     */
    public static final String OPTION_COMMANDS_V3_IMPL = "commandsV3"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Commands} interface for SOMA.
     * 
     * @see Configuration#KEY_COMMANDS_SOMA_IMPL
     */
    public static final String OPTION_COMMANDS_SOMA_IMPL = "commandsSOMA"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Commands} interface for AMPv2.
     * 
     * @see Configuration#KEY_COMMANDS_XC10_IMPL
     */
    public static final String OPTION_COMMANDS_XC10_IMPL = "commandsXC10"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link NotificationCatcher} interface.
     *
     * @see Configuration#KEY_NOTIFICATION_CATCHER_IMPL
     */
    public static final String OPTION_NOTIFICATION_CATCHER_IMPL = "notificationCatcher"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link Repository} interface.
     *
     * @see Configuration#KEY_REPOSITORY_IMPL
     */
    public static final String OPTION_REPOSITORY_IMPL = "repository"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface for AMPv1.
     * 
     * @see Configuration#KEY_SOAP_HELPER_IMPL
     */
    public static final String OPTION_SOAPHELPER_IMPL = "soapHelper"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface for AMPv2.
     * 
     * @see Configuration#KEY_SOAP_HELPER_V2_IMPL
     */
    public static final String OPTION_SOAPHELPER_V2_IMPL = "soapHelperV2"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface for AMPv2.
     * 
     * @see Configuration#KEY_SOAP_HELPER_V3_IMPL
     */
    public static final String OPTION_SOAPHELPER_V3_IMPL = "soapHelperV3"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the class that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface for AMPv2.
     * 
     * @see Configuration#KEY_SOAP_HELPER_V3_IMPL
     */
    public static final String OPTION_SOAPHELPER_SOMA_IMPL = "soapHelperSOMA"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating a reference to an object that implements the
     * {@link Credential} interface and contains the values that will pass
     * authentication and authorization tests to permit the current user to use
     * the Manager. Exactly one of these values must be supplied when invoking
     * that method. For example,
     * <pre>
     *     Credential credential = new Credential();
     *     credential.setProperty("ExampleProperty","ExampleValue");
     *     ...
     *     map.put(Manager.OPTION_CREDENTIAL, credential);
     * </pre>
     *
     * @see Credential
     */
    public static final String OPTION_CREDENTIAL = "credential"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating an Integer object that indicates how big you would
     * like the background task queue to be.
     *
     * @see Configuration#KEY_TASK_QUEUE_SIZE
     */
    public static final String OPTION_TASK_QUEUE_SIZE = "taskQueueSize"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating a Boolean object that indicates if you would like
     * the heartbeat daemon to put ProgressContainers in a queue to be collected
     * by the caller.
     *
     * @see Configuration#KEY_COLLECT_DAEMON_PROGRESSES
     */
    public static final String OPTION_COLLECT_DAEMON_PROGRESSES = "collectDaemonProgresses"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating a Boolean object that indicates if you would like
     * the domain synchronization to put ProgressContainers in a queue to be collected
     * by the caller.
     *
     * @see Configuration#KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES
     */
    public static final String OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES = "collectDomainSynchProgresses"; //$NON-NLS-1$
    
    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating a Boolean object that indicates if you would like
     * the heartbeat activity to not occur, meaning that heartbeating is
     * disabled.
     * 
     * @see Configuration#KEY_HEARTBEAT_DISABLE
     */
    public static final String OPTION_DEBUG_DISABLE_HEARTBEAT = "debugNoHeartbeat"; //$NON-NLS-1$
    
    /**
     */
    public static final String OPTION_DEBUG_DOMAIN_QUIESCE_TIMEOUT = "debugDomainQuiesceTimeout"; //$NON-NLS-1$    
    
    /**
     */
    public static final String OPTION_DEBUG_DEVICE_QUIESCE_TIMEOUT = "debugDeviceQuiesceTimeout"; //$NON-NLS-1$    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating a Boolean object that indicates if you would like
     * the DomainVersion and DeploymentPolicyVersion blobs to be captured to disk
     * in the root directory but outside of the repository.
     * 
     * @see Configuration#KEY_CAPTURE_VERSION_BLOB
     */
    public static final String OPTION_DEBUG_CAPTURE_VERSION_BLOB = "debugCaptureVersionBlob"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of threads that should be in the pool
     * for HeartbeatTasks to run on.
     * 
     * @see Configuration#KEY_HEARTBEAT_THREAD_POOL_SIZE
     */
    public static final String OPTION_HEARTBEAT_THREAD_POOL_SIZE = "heartbeatThreadPoolSize"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of threads that should be in the pool
     * for DomainSynchronization Tasks to run on.
     * 
     * @see Configuration#KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE
     */
    public static final String OPTION_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE = "domainSynchronizationThreadPoolSize"; //$NON-NLS-1$    
    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the IP port number that the NotificationCatcher
     * should listen on for events from devices.
     * 
     * @see Configuration#KEY_NOTIFICATION_CATCHER_IP_PORT
     */
    public static final String OPTION_NOTIFICATION_CATCHER_PORT = "notificationCatcherPort"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of milliseconds in the period that a
     * DomainSynchronization task should wait before probing a device again.
     *
     * @see Configuration#KEY_DOMAIN_SYNCHRONIZATION_INTERVAL
     */
    public static final String OPTION_DOMAIN_SYNCHRONIZATION_INTERVAL = "domainSynchronizationInterval"; //$NON-NLS-1$
   
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of times to retry a failed domain synchronization task. Default = 2
     *
     */
    public static final String OPTION_DOMAIN_SYNCHRONIZATION_RETRY_MAX = "domainSynchronizationRetryMax"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of milliseconds in the period that a
     * HeartbeatTask should wait before probing a device again.
     *
     * @see Configuration#KEY_HEARTBEAT_INTERVAL 
     */
    public static final String OPTION_HEARTBEAT_INTERVAL = "heartbeatInterval"; //$NON-NLS-1$    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the number of milliseconds to monitor each AMP connect
     * request to avoid a hang condition.  
     *
     * @see Configuration#KEY_AMP_CONNECT_TIMEOUT 
     */
    public static final String OPTION_AMP_CONNECT_TIMEOUT = "ampConnectTimeout"; //$NON-NLS-1$    
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the IP address that the NotificationCatcher can be
     * reached at by the device for the sending of events. This is needed only
     * if the Manager is running on a multi-homed computer.
     *
     * @see Configuration#KEY_NOTIFICATION_CATCHER_IP_ADDRESS  
     */
    public static final String OPTION_NOTIFICATION_CATCHER_IP_ADDRESS = "ipAddress"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the interface that the NotificationCatcher can be
     * reached at by the device for the sending of events. This is needed only
     * if the Manager is running on a multi-homed computer.
     * 
     * @see Configuration#KEY_NOTIFICATION_CATCHER_IP_INTERFACE
     */
    public static final String OPTION_NOTIFICATION_CATCHER_IP_INTERFACE = "ipInterface"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the IP port number that the HTTP Listener
     * should listen on for file requests from devices.
     * 
     * @see Configuration#KEY_HTTP_LISTENER_IP_PORT
     */
    public static final String OPTION_HTTP_LISTENER_IP_PORT = "httpListenerPort"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the IP port number that the HTTP Listener
     * should listen on for file requests from devices.
     * 
     * @see Configuration#KEY_HTTP_LISTENER_SOCKET_TIMEOUT
     */
    public static final String OPTION_HTTP_LISTENER_SOCKET_TIMEOUT = "httpListenerSocketTimeout"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the IP address that the HTTP Listener can be
     * reached at by the device for retrieving files. This is needed only
     * if the Manager is running on a multi-homed computer.
     *
     * @see Configuration#KEY_HTTP_LISTENER_IP_ADDRESS 
     */
    public static final String OPTION_HTTP_LISTENER_IP_ADDRESS = "httpListenerIpAddress"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the name of the interface that the HTTP Listener can be
     * reached at by the device for retrieving files. This is needed only
     * if the Manager is running on a multi-homed computer.
     * 
     * @see Configuration#KEY_HTTP_LISTENER_IP_INTERFACE
     */
    public static final String OPTION_HTTP_LISTENER_IP_INTERFACE = "httpListenerIpInterface"; //$NON-NLS-1$

    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating the port for device logs.
     * 
     * @see Configuration#KEY_DEVICE_LOG_PORT
     */
    public static final String OPTION_DEVICE_LOG_PORT = "deviceLogPort"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, use this as the option
     * name when designating if the NotificationCatcher should be listening
     * with a non-SSL server socket.
     *
     * @see Configuration#KEY_NOTIFICATION_CATCHER_NO_SSL
     */
    public static final String OPTION_NOTIFICATION_CATCHER_NO_SSL = "noAmpEventSSL"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, and if you want a
     * third-party SOAP service to be signaled (receive a message) just before
     * and immediately after a change (firmware or domain) is
     * deployed to a device, this would specify the URL of the third-party SOAP
     * service.
     * 
     * @see Configuration#KEY_SIGNAL_URL
     */
    public static final String OPTION_SIGNAL_URL = "signalUrl"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, and if you want a
     * third-party SOAP service to be signaled (receive a message) just before
     * and immediately after a change (firmware or domain) is
     * deployed to a device, this would specify the value for the SOAPAction
     * header of the third-party SOAP service.
     * 
     * @see Configuration#KEY_SIGNAL_SOAP_ACTION
     */
    public static final String OPTION_SIGNAL_SOAP_ACTION = "signalSoapAction"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, and if you want a
     * third-party SOAP service to be signaled (receive a message) just before
     * and immediately after a change (firmware or domain) is
     * deployed to a device, this would specify the connect timeout of the
     * third-party SOAP service.
     * 
     * @see Configuration#KEY_SIGNAL_CONNECT_TIMEOUT
     */
    public static final String OPTION_SIGNAL_CONNECT_TIMEOUT = "signalConnectTimeout"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, and if you want a
     * third-party SOAP service to be signaled (receive a message) just before
     * and immediately after a change (firmware or domain) is
     * deployed to a device, this would specify the timeout of waiting for a
     * response from the third-party SOAP service.
     * 
     * @see Configuration#KEY_SIGNAL_RESPONSE_TIMEOUT
     */
    public static final String OPTION_SIGNAL_RESPONSE_TIMEOUT = "signalResponseTimeout"; //$NON-NLS-1$
    
    /**
     * When first invoking {@link #getInstance(Map)}, and if you want a
     * third-party SOAP service to be signaled (receive a message) just before
     * and immediately after a change (firmware or domain) is
     * deployed to a device, this would specify the sleep time after posting to
     * the third-party SOAP service before continuing on to deploy the change.
     * 
     * @see Configuration#KEY_SIGNAL_DELAY_TIME
     */
    public static final String OPTION_SIGNAL_DELAY_TIME = "signalDelayTime"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as an option 
     * to specify the filename of the key store for the manager to use in it's truststore.
     * The Manager will load all the certificates from the keystore to use as 
     * trusted certificates.
     * 
     * @see Configuration#KEY_TRUSTSTORE_FILENAME
     */    
    public static final String OPTION_TRUSTSTORE_FILENAME = "truststoreFilename"; //$NON-NLS-1$

    /**
     * When first invoking {@link #getInstance(Map)}, use this as an option 
     * to specify the password for the key store specified in {@link Configuration#KEY_TRUSTSTORE_FILENAME}
     * 
     * @see Configuration#KEY_TRUSTSTORE_PASSWORD
     */    
    public static final String OPTION_TRUSTSTORE_PASSWORD = "truststorePassword"; //$NON-NLS-1$
        
    /**
     * The maximum number of versions to store in the repository before the
     * oldest ones are purged from the system. Doing so prevents the repository
     * from growing unbounded over time. This is the default value, it can be
     * changed via {@link #setMaxVersionsToStore(int)}.
     * 
     * @see #setMaxVersionsToStore(int)
     * @see #getMaxVersionsToStore()
     */
    public static final int DEFAULT_MAX_VERSIONS_TO_STORE = 30;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = Manager.class.getName();
    private static String loggerGroupName = "WAMT"; //$NON-NLS-1$
    private static Logger wamtLogger = null;
    private static Logger devLogger = null;
    protected final static Logger clientAPILogger = Logger.getLogger(Manager.class.getPackage().getName());
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        // create the master logger
        String thisPackageName = Manager.class.getPackage().getName();
        int lastPeriod = thisPackageName.lastIndexOf('.'); 
        String wamtName = thisPackageName.substring(0, lastPeriod); // parent package
        wamtLogger = Logger.getLogger(wamtName);
        String deviceLoggerName = wamtName+".deviceLog"; //$NON-NLS-1$
        devLogger = Logger.getLogger(deviceLoggerName);

        LoggerHelper.addLoggerToGroup(wamtLogger, Manager.getLoggerGroupName());
        LoggerHelper.addLoggerToGroup(devLogger, Manager.getLoggerGroupName());
        
        // create a parent logger for this package (clientAPI)
        LoggerHelper.addLoggerToGroup(clientAPILogger, Manager.getLoggerGroupName());
        
        // create a logger for this class per usual
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /*
     * Credential will not be implemented for WAS dataAPI provider (WCCM)
     * and for Tivoli dataAPI provider (XMLBeans). They will rely on host
     * and filesystem access controls.
     */
    
    /**
     * Get an instance of the manager. A Manager is a singleton, so this will
     * return a reference to the instance. If an instance doesn't exist yet, a
     * new instance will be created using the specified options. If an instance
     * already exists, the instance parameter will be ignored. The caller must
     * have valid credentials to access the manager, and pass those credentials
     * in via the options parameter.
     * 
     * @param options a name value pair collection. Valid names in this
     *        collection are listed below, refer to each name javadoc for a
     *        description of the default and valid values. This value may be 
     *        null if all of the default option values   
     *        <ul>
     *        <li>{@link #OPTION_CREDENTIAL}: (optional) credential used to access
     *        the persisted datastore
     *        <li>{@link #OPTION_NOTIFICATION_CATCHER_IMPL}: (optional) name
     *        of the class that implements the AMP 
     *        {@link NotificationCatcher} interface.  
     *        The Manager will instantiate the class and call its startup
     *        method.
     *        See {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_IMPL} Constant 
     *        Field for the default value.
     *        <li>{@link #OPTION_COMMANDS_IMPL}: (optional) name of the class
     *        that implements the AMP 
     *        {@link Commands} interface. The Manager will
     *        instantiate the class and call its startup method.
     *        See {@link Configuration#DEFAULT_COMMANDS_IMPL} Constant 
     *        Field for the default value.
     *        <li>{@link #OPTION_SOAPHELPER_IMPL}: (optional) name of the class
     *        that implements the AMP
     *        {@link com.ibm.datapower.amt.amp.SOAPHelper} interface. The Manager will
     *        instantiate the class and force the Commands engine to use it.
     *        See {@link Configuration#DEFAULT_SOAP_HELPER_IMPL} Constant 
     *        Field for the default value.
     *        <li>{@link #OPTION_REPOSITORY_IMPL}: (optional) name of the
     *        class that implements the dataAPI 
     *        {@link Repository} interface. The Manager will instantiate
     *        the class and call its startup method.
     *        See {@link Configuration#DEFAULT_REPOSITORY_IMPL} Constant 
     *        Field for the default value.
     *        <li>{@link #OPTION_COLLECT_DAEMON_PROGRESSES}: (optional) put
     *        ProgressContainers for the heartbeat daemons in a queue for
     *        retrieval.
     *        <li>{@link #OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES}: (optional) put
     *        ProgressContainers for the domain synchronization daemons in a queue for
     *        retrieval.     *        
     *        <li>{@link #OPTION_TASK_QUEUE_SIZE}: (optional) set the
     *        background task queue to a fixed bounded size.
     *        <li>{@link #OPTION_DEBUG_CAPTURE_VERSION_BLOB}: (optional) store
     *        the blob files on disk for debug purposes.
     *        <li>{@link #OPTION_DEBUG_DISABLE_HEARTBEAT}: (optional) disable
     *        the heartbeat deamon for debug purposes.
     *        <li>{@link #OPTION_MESSAGE_PREFIX}: (optional) set
     *        the message prefix for the messages issued by the manaer 
     *        <li>{@link #OPTION_TRUSTSTORE_FILENAME}: (optional) set
     *        the filename for the manager to use as a truststore.  The manager 
     *        will add all the certificates in the file to it's truststore. 
     *        <li>{@link #OPTION_TRUSTSTORE_PASSWORD}: (optional) set
     *        the password for truststore set in {@link #OPTION_TRUSTSTORE_FILENAME}.
     *        </ul>
     *        
     * @return a reference to the manager. This is the root management object
     *         for the manager.
     * @throws InvalidParameterException one or more of the options in the Map
     *         was missing, or has a value with an invalid class or value.
     * @throws InvalidCredentialException the credential supplied in the Map
     *         parameter did not pass the authentication or authorization tests.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository, or reading from the repository.
     * @throws AMPException there was a problem communicating with the device.
     */
    synchronized public static Manager getInstance(Map options) 
    throws InvalidParameterException, InvalidCredentialException, 
           DatastoreException, AMPException, DeletedException {
        final String METHOD_NAME = "getInstance(Map)";
        logger.entering(CLASS_NAME, METHOD_NAME);
        // get a handle to ourself
        if (singleton == null) {
            // singleton will be set as a side effect of the constructor.
            // May seem a little weird, but makes sense when starting daemons
            try {
            	// If the requestor did not pass an options object, create one
            	// to satisfy the manager
            	if (options == null) {
            		options = new HashMap();
            	}
            	
            	// If the requestor did not pass a credential object in the 
            	// options object, create one to satisfy the manager, an empty 
            	// Credential works with local file systm RepositoryImpl
            	if (!options.containsKey(OPTION_CREDENTIAL)) {
                    Credential credential = new Credential();
                    options.put(Manager.OPTION_CREDENTIAL, credential);
            	}
            	
                // check validity of parameters
                checkOptions(options);
                // merge these parameters with the properties and defaults
                Configuration.loadFromManagerMap(options);
                // create an instance
                new Manager(options);
            } catch (InvalidParameterException e) {
                //clean up partially started instance
                if (singleton != null) { singleton.shutdown(); }
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw (e);
            } catch (DatastoreException e) {
            	e.printStackTrace();   //Keep this printStackTrace so this error can easily be detected on startup 
                //clean up partially started instance
                if (singleton != null) { singleton.shutdown(); }
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw (e);
            } catch (AMPException e) {
                //clean up partially started instance
                if (singleton != null) { singleton.shutdown(); }
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw (e);
            }
            
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(singleton);
    }

    /**
     * Get the status of the Manager.
     * 
     * @return The current status of the manager. See {@link ManagerStatus}for
     *         states and life cycle.
     */
    static synchronized public ManagerStatus getManagerStatus() {
        Manager m = internalGetInstance();
        if (m == null) {
            return ManagerStatus.MANAGER_NOTEXIST;
        } else {
            synchronized (m) {
                // synchronize on instance because instance.shutdown does
                return m.managerStatus;
            }
        }
    }
    
    static Manager internalGetInstance() {
        // a convenience method to catch to exceptions that won't happen if the singleton already exists
        return(singleton);
    }
    
    private static void checkOptions(Map options) throws InvalidParameterException {
        final String METHOD_NAME = "checkOptions"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        if (options == null) {
            String message = Messages.getString("wamt.clientAPI.Manager.noOptOnInstantation"); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.noOptOnInstantation"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        // required options
        if (!options.containsKey(OPTION_CREDENTIAL)) {
            String message = Messages.getString("wamt.clientAPI.Manager.missingOpt",OPTION_CREDENTIAL); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.missingOpt",OPTION_CREDENTIAL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if (!(options.get(OPTION_CREDENTIAL) instanceof Credential)) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotCredential",OPTION_CREDENTIAL); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotCredential",OPTION_CREDENTIAL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        // optional options, since Configuration has default values
        if ((options.get(OPTION_MESSAGE_PREFIX) != null) &&
                (!(options.get(OPTION_MESSAGE_PREFIX) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString", OPTION_MESSAGE_PREFIX );//$NON-NLS-1$ 
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString", OPTION_MESSAGE_PREFIX ); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        
        if ((options.get(OPTION_COMMANDS_IMPL) != null) &&
                (!(options.get(OPTION_COMMANDS_IMPL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString", OPTION_COMMANDS_IMPL );//$NON-NLS-1$ 
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString", OPTION_COMMANDS_IMPL ); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_NOTIFICATION_CATCHER_IMPL) != null) &&
                (!(options.get(OPTION_NOTIFICATION_CATCHER_IMPL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IMPL);//$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IMPL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_REPOSITORY_IMPL) != null) &&
                (!(options.get(OPTION_REPOSITORY_IMPL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_REPOSITORY_IMPL); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_REPOSITORY_IMPL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_SOAPHELPER_IMPL) != null) &&
                (!(options.get(OPTION_SOAPHELPER_IMPL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString", OPTION_SOAPHELPER_IMPL );//$NON-NLS-1$ 
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString", OPTION_SOAPHELPER_IMPL ); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_TASK_QUEUE_SIZE) != null) &&
                (!(options.get(OPTION_TASK_QUEUE_SIZE) instanceof Integer))) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotInteger",OPTION_TASK_QUEUE_SIZE); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotInteger",OPTION_TASK_QUEUE_SIZE); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_COLLECT_DAEMON_PROGRESSES) != null) &&
                (!(options.get(OPTION_COLLECT_DAEMON_PROGRESSES) instanceof Boolean))) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotBoolean",OPTION_COLLECT_DAEMON_PROGRESSES); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotBoolean",OPTION_COLLECT_DAEMON_PROGRESSES); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES) != null) &&
                (!(options.get(OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES) instanceof Boolean))) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotBoolean",OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotBoolean",OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }        
        if ((options.get(OPTION_DEBUG_DISABLE_HEARTBEAT) != null) &&
                (!(options.get(OPTION_DEBUG_DISABLE_HEARTBEAT) instanceof Boolean))) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotBoolean",OPTION_DEBUG_DISABLE_HEARTBEAT); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotBoolean",OPTION_DEBUG_DISABLE_HEARTBEAT); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_DEBUG_CAPTURE_VERSION_BLOB) != null) &&
                (!(options.get(OPTION_DEBUG_CAPTURE_VERSION_BLOB) instanceof Boolean))) {
            String message = Messages.getString("wamt.clientAPI.Manager.optNotBoolean",OPTION_DEBUG_CAPTURE_VERSION_BLOB); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.optNotBoolean",OPTION_DEBUG_CAPTURE_VERSION_BLOB); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        // added the ones below in 1.2
        if ((options.get(OPTION_HEARTBEAT_THREAD_POOL_SIZE) != null) &&
                (!(options.get(OPTION_HEARTBEAT_THREAD_POOL_SIZE) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HEARTBEAT_THREAD_POOL_SIZE); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HEARTBEAT_THREAD_POOL_SIZE); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_NOTIFICATION_CATCHER_PORT) != null) &&
                (!(options.get(OPTION_NOTIFICATION_CATCHER_PORT) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_PORT); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_PORT); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_HEARTBEAT_INTERVAL) != null) &&
                (!(options.get(OPTION_HEARTBEAT_INTERVAL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HEARTBEAT_INTERVAL); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HEARTBEAT_INTERVAL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_AMP_CONNECT_TIMEOUT) != null) &&
                (!(options.get(OPTION_AMP_CONNECT_TIMEOUT) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_AMP_CONNECT_TIMEOUT); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HEARTBEAT_INTERVAL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_NOTIFICATION_CATCHER_IP_ADDRESS) != null) &&
                (!(options.get(OPTION_NOTIFICATION_CATCHER_IP_ADDRESS) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IP_ADDRESS); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IP_ADDRESS); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_NOTIFICATION_CATCHER_IP_INTERFACE) != null) &&
                (!(options.get(OPTION_NOTIFICATION_CATCHER_IP_INTERFACE) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IP_INTERFACE); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_IP_INTERFACE); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_HTTP_LISTENER_IP_PORT) != null) &&
                (!(options.get(OPTION_HTTP_LISTENER_IP_PORT) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_PORT); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_PORT); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_HTTP_LISTENER_IP_ADDRESS) != null) &&
                (!(options.get(OPTION_HTTP_LISTENER_IP_ADDRESS) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_ADDRESS); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_ADDRESS); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_HTTP_LISTENER_IP_INTERFACE) != null) &&
                (!(options.get(OPTION_HTTP_LISTENER_IP_INTERFACE) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_INTERFACE); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_IP_INTERFACE); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_HTTP_LISTENER_SOCKET_TIMEOUT) != null) &&
                (!(options.get(OPTION_HTTP_LISTENER_SOCKET_TIMEOUT) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_SOCKET_TIMEOUT); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_HTTP_LISTENER_SOCKET_TIMEOUT); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_DEVICE_LOG_PORT) != null) &&
                (!(options.get(OPTION_DEVICE_LOG_PORT) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_DEVICE_LOG_PORT); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_DEVICE_LOG_PORT); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        if ((options.get(OPTION_NOTIFICATION_CATCHER_NO_SSL) != null) &&
                (!(options.get(OPTION_NOTIFICATION_CATCHER_NO_SSL) instanceof String))) {
            String message = Messages.getString("wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_NO_SSL); //$NON-NLS-1$$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.OptNotInstOfString",OPTION_NOTIFICATION_CATCHER_NO_SSL); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * 
     * @param options
     * @throws InvalidCredentialException
     * @throws DatastoreException
     * @throws AMPException
     * @throws InvalidParameterException
     */
    private Manager(Map options) throws InvalidCredentialException,
    DatastoreException, AMPException, InvalidParameterException, DeletedException {
        /*
         * options should already have been run through checkOptions(Map) and
         * Configuration.loadFromManagerMap(Map) by the time we get here. That
         * is necessary because we can't do it here because super() needs to be
         * the first statement of this method. We'll take care of that
         * in getInstance(Map).
         */
        super(Configuration.getAsInteger(Configuration.KEY_TASK_QUEUE_SIZE));

        final String METHOD_NAME = "Manager(Map)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        managerStatus = new ManagerStatus(Enumerated.STARTING);
        
        this.options = options;
        
        Manager.singleton = this;
        
        // set the rest of the non-persisted members
        
        String collectDaemonProgresses = 
            Configuration.get(Configuration.KEY_COLLECT_DAEMON_PROGRESSES);
        if (Boolean.valueOf(collectDaemonProgresses).booleanValue()) { 
            this.notificationProgresses = new Queue();
            this.heartbeatProgresses = new Queue();            
        }
        
        String collectDomainSynchProgresses = 
            Configuration.get(Configuration.KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES);
        if (Boolean.valueOf(collectDomainSynchProgresses).booleanValue()) { 
            this.domainSyncProgresses = new Queue();            
        }        

        String prefix = (String)options.get(OPTION_MESSAGE_PREFIX);
        if(prefix != null){
          this.setMessagePrefix(prefix);
        }else{
          this.setMessagePrefix(Configuration.DEFAULT_MESSAGE_PREFIX);	
        }
        
        /*
         * Load up all the cacheable objects from the datastore to the cache
         * upon restart. That seems to be easier than pulling them in piecemeal
         * because we each of our objects use object references instead of name
         * references.
         */ 
        String repositoryClassName = Configuration.get(Configuration.KEY_REPOSITORY_IMPL);
        // can't use Configuration method because Credential was not merged there, 
        // must access Map directly. 
        Credential credential = (Credential) this.options.get(OPTION_CREDENTIAL);
        this.repository = 
            RepositoryFactory.getRepository(repositoryClassName, credential);
        this.repository.startup();
        
        // create the lock for the unmanaged Devices
        this.unmanagedDevicesLock = new Lock("unmanagedDevices"); //$NON-NLS-1$

        // add it to the persistence mapper
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        mapper.add(this.repository, this);
        
        // check to see if maxVersionsToStore has been initialized yet in the datastore.
        // Do this check after it has been added to the persistence mapper.
        if (this.getMaxVersionsToStore() == 0) {
            // initialize it
            this.setMaxVersionsToStore(DEFAULT_MAX_VERSIONS_TO_STORE);
        }
        
        // the daemons below need the Manager to exist in the persistence mapper,
        // so start them after this has been added to the mapper.

        // instantiate and start the Notification catcher
        String catcherClassName = Configuration.get(Configuration.KEY_NOTIFICATION_CATCHER_IMPL);
        this.notificationCatcher = 
            NotificationCatcherFactory.getNotificationCatcher(catcherClassName);
        notificationCatcher.startup();
        
        // generate the primary subscriptionId that can be used for the device to identify the Manager
        this.primarySubscriptionId = "WAMT_Manager_Primary"; //$NON-NLS-1$
        
        // generate the secondary subscription ID here
        this.secondarySubscriptionId = "WAMT_Manager_"; //$NON-NLS-1$
        URL url = getNotificationCatcherURL();
        this.secondarySubscriptionId = this.secondarySubscriptionId + url.getHost().replace('.', '_') + "_" + url.getPort();
        //

        // allocate DeviceLogger if necessary
        DeviceLogger deviceLogger = DeviceLogger.getInstance(devLogger);
        if (deviceLogger == null) {
            // debug statement
        }
        
        // instantiate and start the event processing daemon
        this.queueProcessor = new QueueProcessor();
        this.queueProcessor.startup();
        
        // instantiate and start the heartbeat daemon
        String disableHeartbeat = Configuration.get(Configuration.KEY_HEARTBEAT_DISABLE);
        if (Boolean.valueOf(disableHeartbeat).booleanValue()) {
            // skip the heartbeat daemon
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "disabling heartbeat daemon"); //$NON-NLS-1$
        } else {
            this.heartbeatDaemon = new HeartbeatDaemon();
            this.heartbeatDaemon.startup();
        }
        // LS Note: Delay the instantiate and startup of the Domain Synchronization daemon
        // till any persisted Domains are loaded from repository. Fire daemon immediately.
        // Any domains whose configuration was changed while manager was down will be checked 
        // for differences.  If a difference in deployed configuration is detected, it will 
        // be re-deployed if synch mode is set to AUTO
        
        /*
         * load any persisted data. Do this last because we need locks
         * instantiated, subscriptionID, NotificationCatcher, etc., when this
         * method instantiates Devices, ManagedSets, etc. The
         * loadFromDatastore() method also subscribes to managed devices, syncs
         * ManagedSets, etc.
         */
        this.loadFromDatastore();
        
        /* flag the mapper as initialised - defect 13242 */
        mapper.setInitialised();

        managerStatus.setStatus(Enumerated.UP);
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /////////////////////////////////////////////////////////////////////////////
    // for manager
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Get the parent of all the loggers. With this you can set the Levels
     * of the Loggers, add and remove Handlers, and add and remove resource
     * bundles.
     * 
     * @return the parent of all the loggers.
     */
    public static Logger getDmiLogger() {
        return(wamtLogger);
    }
    
 
    /**
     * Get the logger for distributed device logging.
     * 
     * @return the logger for distributed device logging
     */
    public static Logger getDeviceLogger() {
        return(devLogger);
    }

    /**
     * Get the name of the group that is assigned to all loggers.
     * @return the name of the group that is assigned to all loggers
     */
    public static String getLoggerGroupName() {
        return(loggerGroupName);
    }

    /* javadoc inherited from interface */
    public String getPrimaryKey() {
        return("IBM Appliance Management Toolkit"); //$NON-NLS-1$
    }
    
    /**
     * Get the Manager that has the specified primary key. Since there is only
     * one Manager object anyway, this method is not real helpful, instead it is
     * probably easier to use {@link #getInstance(Map)}.
     * 
     * @param targetKey the Manager that has the specified primary key. If there
     *        is no Manager instance that has this primary key then this method
     *        will return null.
     * @return the Manager with the specified primary key.
     */
    public static Manager getByPrimaryKey(String targetKey) {
        Manager result = null;
        if (singleton == null) {
            result = null;
        } else {
            String key = singleton.getPrimaryKey();
            if (key.equals(targetKey)) {
                result = singleton;
            } else {
                result = null;
            }
        }
        return(result);
    }

    /**
     * Part of the Persistable interface.
     * 
     * @return
     * @throws DeletedException
     */
    Repository getStoredInstance() throws DeletedException {
        String METHOD_NAME = "getStoredInstance"; //$NON-NLS-1$
        if (this.repository == null) {
            String message = Messages.getString("NoPersistence"); //$NON-NLS-1$
            DeletedException e = new DeletedException(message,"NoPersistence");
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return(this.repository);
    }
    
    Repository getRepository() throws DeletedException {
        // will use getRepository instead of getStoredObject
        return(this.getStoredInstance());
    }
    
    /**
     * Do a recursive delete of all the objects in memory and in the datastore.
     * Used only for a complete reset of the Manager for JUnit tests. This should
     * not be called by anything other than the unit test framework (ie, JUnit)
     * @throws NotEmptyException
     * @throws InUseException
     * @throws InvalidParameterException
     * @throws NotEmptyException
     * @throws DatastoreException
     * @throws FullException 
     */
    void destroy() throws InUseException, InvalidParameterException,
    NotEmptyException, DatastoreException, FullException {
        final String METHOD_NAME = "destroy"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        // delete all the ManagedSets, which will also take down Domains
        ManagedSet[] managedSets = this.getManagedSets();
        for (int i=0; i<managedSets.length; i++) {
            boolean deleted = false;
            while (!deleted) {
                try {
                    this.remove(managedSets[i]);
                    deleted = true;
                } catch (LockBusyException e) {
                    // just try again
                }
            }
        }
        
        // delete all the Devices (which now are unmanaged)
        Device[] devices = this.getAllDevices();
        for (int i=0; i<devices.length; i++) {
            boolean deleted = false;
            while (!deleted) {
                try {
                    this.remove(devices[i]);
                    deleted = true;
                } catch (DeletedException e) {
                    // we want it deleted anyway, so ignore
                    deleted = true;
                } catch (NotExistException e) {
                    // we want it deleted anyway, so ignore
                    deleted = true;
                } catch (LockBusyException e) {
                    // just try again
                }
            }
        }
        
        // delete all the Firwmares (none of which should be in use)
        Firmware[] firmwares = this.getFirmwares();
        for (int i=0; i<firmwares.length; i++) {
            try {
                this.remove(firmwares[i]);
            } catch (DeletedException e) {
                // we want it deleted anyway, so ignore
            }
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Even though this method does a lot of work, they are all short-running
     * tasks. So it should be OK that this method is synchronized because it
     * doesn't depend on any other threads, and is relatively short-running. All
     * the heavy lifting is queued as BackgroundTasks.
     * 
     * @throws DatastoreException
     */
    synchronized private void loadFromDatastore() throws AMPException, DatastoreException, DeletedException {
        final String METHOD_NAME = "loadFromDatastore"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        

        if (this.repository == null) {
            String message = Messages.getString("wamt.clientAPI.Manager.nullRefToRepos");
            DatastoreException e = new DatastoreException(message, "wamt.clientAPI.Manager.nullRefToRepos");
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        
        // load Firmware
        StoredFirmware[] storedFirmwares = this.repository.getFirmwares();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    storedFirmwares.length + " StoredFirmwares"); //$NON-NLS-1$
        for (int i=0; i<storedFirmwares.length; i++) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "loading Firmware: " + storedFirmwares[i]); //$NON-NLS-1$
            Firmware firmware = new Firmware(storedFirmwares[i]);
            
            // load FirmwareVersions
            StoredFirmwareVersion[] storedFirmwareVersions =
                storedFirmwares[i].getVersions();
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        storedFirmwareVersions.length + " StoredFirmwareVersions"); //$NON-NLS-1$
            for (int j=0; j<storedFirmwareVersions.length; j++) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,  
                            "loading FirmwareVersion: " + storedFirmwareVersions[j]); //$NON-NLS-1$
                FirmwareVersion firmwareVersion = 
                    new FirmwareVersion(firmware, storedFirmwareVersions[j]);
                if (firmwareVersion == null) {
                    // debug statement
                }
            }
            
        }
        
        // load ManagedSets
        StoredManagedSet[] storedManagedSets = this.repository.getManagedSets();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    storedManagedSets.length + " StoredManagedSets"); //$NON-NLS-1$
        for (int i=0; i<storedManagedSets.length; i++) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "loading ManagedSet: " + storedManagedSets[i]); //$NON-NLS-1$
            ManagedSet managedSet = new ManagedSet(storedManagedSets[i]);
            if (managedSet == null) {
                  // debug statement
            }            
        }
        
        // load Devices
        StoredDevice[] storedDevices = this.repository.getDevices();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    storedDevices.length + " StoredDevices"); //$NON-NLS-1$
        for (int i=0; i<storedDevices.length; i++) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "loading Device: " + storedDevices[i]); //$NON-NLS-1$
            try {
            Device device = new Device(storedDevices[i]);
            if (device == null) {
                // debug statement
            }
            }
            catch(Throwable t) {
            	t.printStackTrace();
            	logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "Error", t);
            }

            // load Domains
            StoredDomain[] storedDomains = storedDevices[i].getManagedDomains();
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        storedDomains.length + " StoredDomains"); //$NON-NLS-1$
            for (int domainIndex=0; domainIndex<storedDomains.length; domainIndex++) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "loading Domain: " + storedDomains[domainIndex]); //$NON-NLS-1$
                Domain domain = new Domain(storedDomains[domainIndex]);
               
               
                // load DomainVersions
                StoredVersion[] storedVersions = storedDomains[domainIndex].getVersions();
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            storedVersions.length + " StoredDomainVersions"); //$NON-NLS-1$
                for (int versionIndex=0; versionIndex<storedVersions.length; versionIndex++) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "loading DomainVersion: " + storedVersions[versionIndex]); //$NON-NLS-1$
                    StoredDomainVersion storedDomainVersion =
                        (StoredDomainVersion) storedVersions[versionIndex];
                    DomainVersion domainVersion = new DomainVersion(storedDomainVersion);
                    if (domainVersion == null) {
                        // debug statement
                    }
                }
                
                StoredDeploymentPolicy deploymentPolicy = storedDomains[domainIndex].getDeploymentPolicy();
                DeploymentPolicy dp = new DeploymentPolicy(deploymentPolicy);
                //load DeploymentPolicyVersion
                StoredDeploymentPolicyVersion [] storedPolicyVersions = deploymentPolicy.getVersions();
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            storedVersions.length + " StoredPolicyVersions"); //$NON-NLS-1$
                for (int versionIndex=0; versionIndex<storedPolicyVersions.length; versionIndex++) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "loading DomainVersion: " + storedPolicyVersions[versionIndex]); //$NON-NLS-1$
                    StoredDeploymentPolicyVersion storedDeploymentPolicyVersion =
                        (StoredDeploymentPolicyVersion) storedPolicyVersions[versionIndex];
                    DeploymentPolicyVersion deploymentPolicyVersion = new DeploymentPolicyVersion(storedDeploymentPolicyVersion);
                    if (deploymentPolicyVersion == null) {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                                "deploymentPolicyVersion is NULL"); //$NON-NLS-1$
 
                    }
                }                
                
                dp.trimExcessVersions();
                // do the trim after everything is loaded
                domain.trimExcessVersions();
            }
        }
        
        // trim excess FirmwareVersions now that the ManagedSets are established
        Firmware[] firmwares = this.getFirmwares();
        for (int i=0; i<firmwares.length; i++) {
            firmwares[i].trimExcessVersions();
        }
        
        /*
         * Now create the BackgroundTasks to do all the long-running work that
         * is part of initialization. This includes getting the opstate of all
         * the managed domains, subscribing to all managed devices, and syncing
         * all managed sets.
         */

        // create a MacroProgressContainer and put it on the Notification queue
        MacroProgressContainer macroProgressContainer = new MacroProgressContainer(null);

        ManagedSet[] managedSets = this.getManagedSets();
        for (int managedSetIndex=0; managedSetIndex<managedSets.length; managedSetIndex++) {
            // walk through all managed devices
            Device[] devices = null;
            try {
                devices = managedSets[managedSetIndex].getDeviceMembers();
            } catch (DeletedException e) {
                devices = new Device[0];
            }
            for (int deviceIndex=0; deviceIndex<devices.length; deviceIndex++) {
            	Device device = devices[deviceIndex];
            	
            	if(!device.getDeviceType().equals(DeviceType.XC10)){
            		// get the operation status of managed domains on the device
            		BackgroundTask backgroundTask =
            			BackgroundTask.createGetDomainsOperationStatusTask(devices[deviceIndex]);
            		WorkArea workArea = managedSets[managedSetIndex];
            		this.privilegedEnqueue(backgroundTask, workArea);
            		ProgressContainer progressContainer = backgroundTask.getProgressContainer();
            		macroProgressContainer.addNested(progressContainer);
            		// Device.trackOperationStatusOfDomain is called in the Domain constructor,
            		// so that is already done.
                
            		// subscribe to device
            		backgroundTask = BackgroundTask.createSubscribeTask(device);
            		workArea = managedSets[managedSetIndex];
            		this.privilegedEnqueue(backgroundTask, workArea);
            		progressContainer = backgroundTask.getProgressContainer();
            		macroProgressContainer.addNested(progressContainer);
            	}
            }
          
        }
        this.addNotificationProgress(macroProgressContainer);
        // the macroProgressContainer won't evaluate to isComplete==true until
        // all the nested ProgressContainers are complete, it is OK to set it now
        macroProgressContainer.setComplete();

        
        // When daemon fires the first time all managed domain will also be checked for difference between
        // deployed domain configuration on the device and the persisted configuration.  This is done because
        // we could have lost notification from device when the manager was down.
        // Subsequent firing of the daemon will not check for configuration differences.
        this.domainSynchDaemon = new DomainSynchronizationDaemon();
        this.domainSynchDaemon.startup();             
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Get the subscription id that this manager uses with devices. Normally
     * this value is used only internally. But if you wish, you can retrieve
     * this value and look it up on the device itself with the WebGUI or CLI to
     * verify that the subscription is active. Subscriptions are used to receive
     * change notifications from the device to the manager.
     * 
     * @return the subscription id that this manager uses with devices.
     */
    public String getSubscriptionId() {
    	return getSubscriptionId(true);
    }
    
    
    public String getSubscriptionId(boolean isPrimary) {
    	if(isPrimary)
    		return primarySubscriptionId;
    	else{
    		return secondarySubscriptionId;
    	}
    }
    
    /**
     * Upon garbage collection, shutdown any other threads we started so the
     * resources held by those threads can be released.
     */
    synchronized protected void finalize() {
        final String METHOD_NAME = "finalize"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        this.shutdown();
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Shutdown the Manager. It will in turn shutdown all the components it
     * started, including daemon threads and subscriptions. The JVM may safely
     * shutdown after this process has completed. This is a long-running method,
     * as it unsubscribes from all managed devices and waits for all the threads
     * to terminate.
     * 
     * @see NotificationCatcher#shutdown()
     * @see Repository#shutdown()
     */
    public void shutdown() {
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        /*
         * Instead of making the whole method synchronized, just synchronize on
         * the smallest critical section possible. This is because other threads
         * (ie, QueueProcessor) may try to invoke synchronized methods in this
         * class (ie, save()). We don't want to get into a deadlock, because
         * this is a long-running method with dependencies on other threads (ie,
         * QueueProcessor).
         */
        boolean doItNow = false;
        synchronized(this) {
            if (!this.shutdownRequested) {
                this.shutdownRequested = true;
                doItNow = true;
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "shutdown request accepted from thread " + //$NON-NLS-1$
                        Thread.currentThread().getName());
            } else {
                // already requested to shut down, discard the request
                doItNow = false;
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "shutdown request ignored, already requested"); //$NON-NLS-1$
            }
        }      
        
        // Before tearing down the infrastructure wait for any synch tasks
        // that are already running
        Hashtable <String, DomainSynchronizationTask> tasks = getDomainSyncTasksInProgress();                     
        boolean done = false;
        long shutdownTime = tasks.size() * 10000; // 10 seconds
        long timeout = 0;
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                "Waiting for tasks to complete. shutdownTime = " + shutdownTime); //$NON-NLS-1$        
           
     // Iterate over the keys in the map
        while(!done && timeout < shutdownTime ){        
            done = true;
        	Iterator it = tasks.entrySet().iterator();
        	while (it.hasNext()) {
        		// Get key
        		Map.Entry entry = (Map.Entry) it.next();
            	DomainSynchronizationTask  value = (DomainSynchronizationTask) entry.getValue();
        		
        		boolean flag = value.getStatus();
        		if (!flag){
        			done = false;
        		}
        	}
        	sleep(1000);
        	timeout = timeout +1000;       
        }
        
        
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                "Waited for tasks to complete. done = " + done+ ", timeout = "+timeout); //$NON-NLS-1$        
        
        tasks.clear(); tasks = null;

        if (doItNow) {
            managerStatus.setStatus(Enumerated.STOPPING);

            // shutdown the heartbeat daemon
            if (this.heartbeatDaemon == null) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "HeartbeatDaemon was not started"); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "Shutting down heartbeatDaemon"); //$NON-NLS-1$
                this.heartbeatDaemon.shutdown();
                this.heartbeatDaemon = null;
            }
            
            // shutdown the domainSynchronization daemon
            if (this.domainSynchDaemon == null) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "DomainSynchronizationDaemon was not started"); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "Shutting down DomainSynchronizationDaemon"); //$NON-NLS-1$
                this.domainSynchDaemon.shutdown();
                this.domainSynchDaemon = null;
            }           
            
            /*
             * Shutdown the notification catcher. We do this before the
             * queueProcessor so the catcher won't be trying to add
             * Notifications to a queue that no longer exists.
             */
            if (this.notificationCatcher == null) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "notification catcher was not started"); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "Shutting down notificationCatcher"); //$NON-NLS-1$
                this.notificationCatcher.shutdown();
                this.notificationCatcher = null;
            }

            // shutdown the queueProcessor
            if (this.queueProcessor == null) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "queueProcessor was not started"); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                        "Shutting down queueProcessor");  //$NON-NLS-1$
                // first, unsubscribe from all managed devices
                BackgroundTask unsubscribeAllTask = BackgroundTask.createUnsubscribeAllTask();
                ProgressContainer progressContainer = unsubscribeAllTask.getProgressContainer();
                this.privilegedEnqueue(unsubscribeAllTask, this);
                // for this task to appear somewhere, we will add a MacroProgressContainer
                // to the notification queue. A little wierd, but the best choice available.
                MacroProgressContainer macroProgressContainer = 
                    new MacroProgressContainer(unsubscribeAllTask);
                macroProgressContainer.addNested(progressContainer);
                this.addNotificationProgress(macroProgressContainer);
                
                /*
                 * Ask the QueueProcessor to shutdown. Upon invocation of
                 * queueProcessor.shutdown(), it will start discarding all tasks
                 * in the queue except UnsubscribeAll. The
                 * queueProcessor.shutdown() method is blocking, so it won't
                 * return here until the queue is empty and it is shutdown and
                 * that thread exits.
                 */
                this.queueProcessor.shutdown();
                this.queueProcessor = null;
                
                // if we get this far, then UnsubscribeAll should be complete.
                macroProgressContainer.setComplete();
            }
           
 
            
            // shutdown the repository datastore, but do a save first
            try {
                if (this.repository == null) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "repository does not exist and was not saved"); //$NON-NLS-1$
                } else {                	
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "save repository"); //$NON-NLS-1$
                    this.save(SAVE_UNFORCED);                	
                }
            } catch (DatastoreException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        Messages.getString("wamt.clientAPI.Manager.DSExceptionDuringSave"), //$NON-NLS-1$
                        e);
            }
            try {
                if (this.repository == null) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "repository was not started"); //$NON-NLS-1$
                } else {                	
                    this.repository.shutdown();
                }
            } catch (DatastoreException e) {
                // trying to take it down anyway
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        Messages.getString("wamt.clientAPI.Manager.ExceptionDuringShutdown"), //$NON-NLS-1$
                        e);
            }
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            mapper.destroy();
            this.repository = null;

            // don't destroy self, as that does a recursive delete through the datastore

            this.setSingleton();//Manager.singleton = null;

            managerStatus.setStatus(Enumerated.DOWN);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    private static void setSingleton() {
    	Manager.singleton = null;
    }
    
    /**
     * Export everything in the manager datastore to the specified OutputStream.
     * This method can be used to backup the manager. This method will not close
     * the OutputStream. The format of this data is in a known interchange
     * format so the manager data can be migrated to another managmement system.
     * <p>
     * This may be a long-running method, depending on how long it takes to
     * acquire all the locks. The locks may be held by other threads doing
     * long-running operations.
     * 
     * @param outputStream
     *            where the exported data will be written
     * @return the ProgressContainer that can be used to determine the status of
     *         this long running task.
     * @see #importAll(InputStream)
     */
    public ProgressContainer exportAll(OutputStream outputStream) 
        throws FullException {
        final String METHOD_NAME = "exportAll"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        BackgroundTask backgroundTask = BackgroundTask.createExportAll(outputStream);
        ProgressContainer result = backgroundTask.getProgressContainer();
        WorkArea workArea = this;
        this.enqueue(backgroundTask, workArea);
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return result;
    }
    
    /**
     * Import to the manager datastore from the specified InputStream. This
     * InputStream should be connected to data that was previously exported.
     * This method will not close the InputStream. The format of this data is in
     * a known interchange format so the manager data can be migrated from
     * another management instance.
     * <p>
     * This should be considered a long-running method, as it may take a while
     * for the daemons to shutdown and restart with the new data.
     * 
     * @param inputStream where to read the previously exported data from
     * @see #exportAll(OutputStream)
     * @throws AMPException there was a problem communicating with a device 
     * @throws IOException (deprecated?)
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     */
    public void importAll(InputStream inputStream)
        throws AMPException, IOException, DatastoreException {
        final String METHOD_NAME = "importAll"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
//caused problems by processing the stream before input
//caller can all ExportImport.isValidImport themselves since they control the stream
//        ZipInputStream zIS = new ZipInputStream(inputStream);
//        if (!ExportImport.isValidImport(zIS)) {
//        	String message = Messages.getString("wamt.dataAPI.ExportImport.badImport"); //$NON-NLS-1$
//            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
//            DatastoreException e = new DatastoreException(message,"wamt.dataAPI.ExportImport.badImport",null); //$NON-NLS-1$
//        	logger.throwing(CLASS_NAME, METHOD_NAME, e);
//        	throw (e);
//        }
        
        // shutdown the daemons, because we will reload everything and
        // change all the objects that they are referencing
        if (this.heartbeatDaemon != null){
        	this.heartbeatDaemon.shutdown();
        }
        
        if (this.domainSynchDaemon != null){
        	this.domainSynchDaemon.shutdown();
        }        
        
        if (this.notificationCatcher != null) {
        	this.notificationCatcher .shutdown();
        }
        if (this.queueProcessor != null) {
        	this.queueProcessor.shutdown();
        }

        // lock everything (ManagedSets, Devices, Firmware/FirmwareVersions, unmanaged Devices
        ManagedSet[] managedSets = this.getManagedSets();
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "need to lock " + managedSets.length + " ManagedSets"); //$NON-NLS-1$ //$NON-NLS-2$

        if (managedSets != null) for (int i=0; i<managedSets.length; i++) {
            try {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,  
                            "about to lock ManagedSet " + i + ": " + managedSets[i].getName()); //$NON-NLS-1$ //$NON-NLS-2$
                managedSets[i].lockWait();
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "ManagedSet appears to have been deleted extremely recently"); //$NON-NLS-1$
            }
        }

        Device[] devices = this.getAllDevices(); 
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                "need to lock " + devices.length + " Devices"); //$NON-NLS-1$ //$NON-NLS-2$
        
        if (devices != null) for (int i=0; i<devices.length; i++) {
            try {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,  
                            "about to lock Device " + i + ": " + devices[i].getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
                devices[i].lockWait();
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "Device appears to have been deleted recently"); //$NON-NLS-1$
            }
        }
        
        Firmware.lockWait();
        this.unmanagedDevicesLockWait();
        
        //backup current config unless something bad happens
        //removed because import implies they did not value the current config
//        this.save(SAVE_UNFORCED); //make sure we get current changes in backup
//        File tmpBackup = File.createTempFile(
//                "backupBeforeImport", ".zip");
//        //tmpBackup.deleteOnExit();
//        FileOutputStream tmpBackupOS = new FileOutputStream(tmpBackup);
//        this.repository.exportAll(tmpBackupOS);
        
        // destroy everything before we import, so it imports into an empty space
        try {
            this.destroy();

            this.repository.importAll(inputStream);
            // an importAll only loads the datastore, it doesn't create the clientAPI
            // objects. Let's do that now.
            this.loadFromDatastore();
            this.save(SAVE_UNFORCED);
//            tmpBackup.delete();
        } catch (ClientAPIException e) {
            // shouldn't happen
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
        } finally {
            this.unmanagedDevicesUnlock();
            Firmware.unlock();
            for (int i=0; i<devices.length; i++) {
                devices[i].unlock();
            }
            for (int i=0; i<managedSets.length; i++) {
                managedSets[i].unlock();
            }
        }
        // restart the daemons
        this.notificationCatcher.startup();
        this.queueProcessor.startup();
        this.heartbeatDaemon.startup();
//        LS Disable DomainSynch Daemon till unit test is completed  
        this.domainSynchDaemon.startup();
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Explicitly persist the Manager's content to a datastore. 
     * <p>
     * Some circumstances should cause the data to be saved automatically,
     * otherwise the data should be saved only when this method is invoked by
     * the clientAPI consumer. Automatic saves should happen when changes are
     * triggered as the result of a long-running method, or methods invoked by
     * daemon threads instead of the clientAPI consumer:
     * <ul>
     * <li>data is modified by a background thread instead of via the
     * clientAPI. For example, the heartbeat daemon changes a persistable value.
     * In this case, the background thread should automatically save the data
     * because the clientAPI consumer didn't explicitly drive the change and
     * even isn't aware that a change occurred - it was detected on a background
     * thread.
     * <li>a call to the clientAPI changes data, but the change occurs in the
     * background as the result of a long-running method instead of in the
     * foreground as a short-running method. For example,
     * <code>Manager.remove(ManagedSet)</code> runs in the foreground as a
     * short-running method and does not automatically save, but
     * <code>Manager.addFirmware</code> runs in the background as a
     * long-running method and automatically does a save. It would be difficult
     * for the clientAPI consumer to know when to explicitly invoke
     * <code>save()</code> because changes are occurring in the background over
     * a indeterminate time period.
     * </ul>
     * <p>
     * Beware that the automatic save will save <strong>all </strong> the
     * repository data, not just the subset of data that may have changed due to
     * a long-running process. For this reason, it is difficult to "rollback"
     * changes by not saving them and then restarting the Manager without a save
     * invocation. There may have been a long-running task that triggered all
     * the repository to be saved. 
     * <p>
     * This method should be synchronized because we don't want multiple threads
     * to be invoking it at the same time with slight time offsets, because they
     * would be overwriting each other on the single disk storage destination,
     * which may result in a mangled persisted store.
     * 
     * @param force
     *            the data to persistent storage even if the modified objects
     *            were based on original values that were already modified by
     *            another party. Valid values are {@link #SAVE_UNFORCED},
     *            {@link #SAVE_FORCED}
     * @throws DirtySaveException
     *             another vehicle changed the data in the repository after it
     *             was loaded here, so the data has forked. You did not ask for
     *             the save to be forced, so this exception is thrown to make
     *             you aware of the data inconsistency without overwriting what
     *             is already in the repository.
     * @throws DatastoreException
     *             there was a problem persisting this value to the repository
     *             or reading it from the repository.
     * @see #SAVE_UNFORCED
     * @see #SAVE_FORCED
     */
    synchronized public void save(boolean force) 
    throws DirtySaveException, DatastoreException {
        final String METHOD_NAME = "save"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, Boolean.valueOf(force));
        
        /*
         * Do not attempt to get all the locks, because it is unlikely they will
         * available, and we cannot afford to wait because that would make this
         * method a long-running task. Just go with what we have now.
         */

        this.repository.save(force);

        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Gets the maximum number of versions of any one object that the manager
     * will try to keep. This is the value set in
     * {@link #setMaxVersionsToStore(int)}.
     * 
     * @return the maximum number of versions of any one object to keep in the
     *         manager
     * @see #setMaxVersionsToStore(int)
     * @see #DEFAULT_MAX_VERSIONS_TO_STORE
     */
    public int getMaxVersionsToStore() {
        return(this.repository.getMaxVersionsToStore());
    }
    
    /**
     * Set the maximum number of versions of any one object that should be kept
     * in the manager. This is used to trim the datastore to a reasonable size.
     * If any object has more than maxVersions in the manager, then the manager
     * will automatically delete the oldest versions until this threshold is no
     * longer exceeded. The manager will not delete any versions that are
     * currently deployed, even if that results in storing more than
     * maxVersions. This prevents the repository from growing unbounded over a
     * period of time.
     * 
     * @param maxVersions the maximum number of versions of any one object to
     *        keep in the manager. For example, is set to 4 then the manager
     *        will limit itself to keeping 4 versions of firmware
     *        and 4 versions of each domain in each managedSet.
     *        This applies to all versioned objects: firmware, domains, and
     *        deployment policies. A value that is less than 1 (0 or negative) means that
     *        there is no limit and the manager will never delete versions.
     * @throws InvalidParamterException the maximum number of versions to store
     *         must be greater than or equal to one
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     * @see #getMaxVersionsToStore()
     */
    public void setMaxVersionsToStore(int maxVersions) 
        throws InvalidParameterException, DatastoreException {
        final String METHOD_NAME = "setMaxVersionsToStore"; //$NON-NLS-1$
        if (maxVersions < 1) {
            String message = Messages.getString("wamt.clientAPI.Manager.badMaxVersions",Integer.toString(maxVersions)); //$NON-NLS-1$
            InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.Manager.badMaxVersions",Integer.toString(maxVersions)); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message); 
            throw(e);
        }
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "set maxVersionsToStore=" + maxVersions); //$NON-NLS-1$
        this.repository.setMaxVersionsToStore(maxVersions);
    }
    
//    /**
//     * Gets the directory where the versions created by the manager will be stored.
//     * 
//     * @return the directory where the versions created by the manager will be stored.
//     * @see #setVersionsDirectory(java.io.File)
//     */
//    public File getVersionsDirectory() {
//        return(this.repository.getVersionsDirectory());
//    }
//    
//    /**
//     * Sets the directory where the versions created by the manager will be
//     * stored. It is up the repository implementation to decide if it needs to move
//     * the current versions from the current directory to the new directory. 
//     * 
//     * @param newDirectory
//     *            the new directory where the versions will be stored.
//     * @return the ProgressContainer that can be used to determine the status of
//     *         this long running task.
//     * @throws InvalidParameterException
//     * @throws DatastoreException
//     * @see #getMaxVersionsToStore()
//     * 
//     */
//
//    public ProgressContainer setVersionsDirectory(File newDirectory)
//            throws InvalidParameterException, FullException {
//        final String METHOD_NAME = "setVersionsDirectory"; //$NON-NLS-1$
//        logger.entering(CLASS_NAME, METHOD_NAME, newDirectory);
//        //take any errors on creating directory before creating long running task
//        if (! newDirectory.exists()) {
//            newDirectory.mkdir();
//        }
//        String newDirectoryAbsoluteFileName = newDirectory.getAbsolutePath();
//        if (! newDirectory.isDirectory()) {
//            String s = Messages.getString("wamt.clientAPI.Manager.notADirectory", newDirectoryAbsoluteFileName); //$NON-NLS-1$
//            InvalidParameterException e = new InvalidParameterException(s, Messages.getString("wamt.clientAPI.Manager.notADirectory"), newDirectoryAbsoluteFileName); //$NON-NLS-1$
//            logger.throwing(CLASS_NAME, METHOD_NAME, e);
//            throw (e);
//        }
//        BackgroundTask backgroundTask = BackgroundTask.createSetVersionsDirectory(newDirectory);
//        ProgressContainer result = backgroundTask.getProgressContainer();
//        WorkArea workArea = this;
//        this.enqueue(backgroundTask, workArea);
//        logger.exiting(CLASS_NAME, METHOD_NAME, result);
//        return result;
//    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String primaryKey = null;
        primaryKey = this.getPrimaryKey();
        return("Manager[" + primaryKey + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /////////////////////////////////////////////////////////////////////////////
    // for devices
    /////////////////////////////////////////////////////////////////////////////
    boolean containsHostnamePort(String hostnamePort) {
        boolean result = false;
        Device[] allDevices = this.getAllDevices();
        for (int i=0; i<allDevices.length; i++) {
            try {
                if (hostnamePort.equals(allDevices[i].getHostname() + ":" + allDevices[i].getHLMPort())) {
                    result = true;
                }
            } catch (DeletedException e) {
                // a single device was deleted, just skip it
            }
        }
        return(result);
    }
    
    boolean containsDeviceUID(String UID) {
        boolean result = false;
        Device[] allDevices = this.getAllDevices();
        for (int i=0; i<allDevices.length; i++) {
            try {
            	String uid = allDevices[i].getPrimaryKey();
                if (uid.equals(UID)) {
                    result = true;
                }
            } catch (DeletedException e) {
                // a single device was deleted, just skip it
            }
        }
        return(result);
    }
    
    boolean containsDeviceSerialNumber(String targetSerialNumber) {
        boolean result = false;
        Device[] allDevices = this.getAllDevices();
        for (int i=0; i<allDevices.length; i++) {
            try {
                if (allDevices[i].getSerialNumber().equals(targetSerialNumber)) {
                    result = true;
                }
            } catch (DeletedException e) {
                // a single device was deleted, just skip it
            }
        }
        return(result);
    }
    
    boolean containsDeviceSymbolicName(String targetSymbolicName) {
        boolean result = false;
        Device[] allDevices = this.getAllDevices();
        for (int i=0; i<allDevices.length; i++) {
            try {
                if (allDevices[i].getSymbolicName().equals(targetSymbolicName)) {
                    result = true;
                }
            } catch (DeletedException e) {
                // a single device was deleted, just skip it
            }
        }
        return(result);
    }
    
    /**
     * Gets the list of all known devices in the manager, independent of being
     * assigned to a ManagedSet.
     * 
     * @return a list of all Devices that are defined in the Manager.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see #getUnmanagedDevices(DeviceType, ModelType, StringCollection)
     */
    public Device[] getAllDevices() {
        final String METHOD_NAME = "getAllDevices"; //$NON-NLS-1$
        StoredDevice[] storedDevices = this.repository.getDevices();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        Vector devices = new Vector();
        for (int i=0; i<storedDevices.length; i++) {
            Device device = null;
            try {
                device = mapper.getVia(storedDevices[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            storedDevices[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                device = null;
            }
            if (device != null) {
                devices.add(device);
            }
        }
        Device[] result = new Device[devices.size()];
        for (int i=0; i<devices.size(); i++) {
            result[i] = (Device) devices.get(i);
        }
        return(result);
    }
    
    /**
     * Gets the list of known devices in the manager that are not assigned to a
     * ManagedSet. Additionally, it may filter the list to include only devices
     * of the desired DeviceType, ModelType, and/or feature list.
     * 
     * @param desiredDeviceType
     *            if set to null, this method will return unmanaged devices of
     *            any DeviceType. If set to a non-null value, this method will
     *            return all unmanaged devices that are compatible with this
     *            desired DeviceType. See
     *            {@link DeviceType#isCompatibleWith(DeviceType)} for a
     *            description of compatibility. This parameter may be combined
     *            with the other parameters to perform a logical AND.
     * @param desiredModelType
     *            if set to null, this method will return unmanaged devices of
     *            any ModelType. If set to a non-null value, this method will
     *            return all unmanaged devices that are compatible with this
     *            desired ModelType. See
     *            {@link ModelType#isCompatibleWith(ModelType)} for a
     *            description of compatibility. This parameter may be combined
     *            with the other parameters to perform a logical AND.
     * @param desiredFeatures
     *            if set to null, this method will return unmanaged devices of
     *            any feature list. If set to a non-null value, this method will
     *            return all unmanaged devices that match only this desired
     *            feature list. This parameter may be combined with the other
     *            parameters to perform a logical AND.
     * @return a list of unmanaged Devices, meaning devices that are not members
     *         of any managedSet. This list be may filtered by using the above
     *         parameters.
     * @throws DeletedException
     *             this object has been deleted from the persisted repository.
     *             The referenced object is no longer valid. You should not be
     *             using a reference to this object.
     * @see #getAllDevices()
     */
    public Device[] getUnmanagedDevices(DeviceType desiredDeviceType,
            ModelType desiredModelType, StringCollection desiredFeatures) 
    throws DeletedException {
        Device[] allDevices = this.getAllDevices();
        Vector unmanagedDevices = new Vector();
        for (int i=0; i<allDevices.length; i++) {
            Device device = allDevices[i];
            if (device.getManagedSet() == null) {
                if (((desiredDeviceType == null) || (device.getDeviceType().isCompatibleWith(desiredDeviceType))) &&
                        ((desiredModelType == null) || (device.getModelType().isCompatibleWith(desiredModelType))) &&
                        ((desiredFeatures == null) || device.getFeatureLicenses().equals(desiredFeatures))) {
                    unmanagedDevices.add(device);
                }
            }
        }
        
        Device[] result = new Device[unmanagedDevices.size()];
        Iterator iterator = unmanagedDevices.iterator();
        for (int i=0; iterator.hasNext(); i++) {
            Device device = (Device) iterator.next();
            result[i] = device;
        }
        return(result);
    }

    /**
     * @deprecated
     * Get a Device that has a particular hardware serial number. This method
     * does not care if the Device is a member of a ManagedSet or not.
     * 
     * @param targetSerialNumber the hardware serial number to search for.
     * @return a Device that has a particular hardware serial number. This
     *         method may return null if no device has this hardware serial
     *         number.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see Device#getSerialNumber()
     */
    public Device getDeviceBySerialNumber(String targetSerialNumber) throws DeletedException {
    	Device[] result = this.getDevicesBySerialNumber(targetSerialNumber);
    	
    	if ( result.length > 0 ) 
    		return result[0];
        return null;
    }
    
    /**
     * Get all Devices that have a particular hardware serial number. 
     * This method does not care if the Device is a member of a ManagedSet or not.
     * 
     * @param targetSerialNumber the hardware serial number to search for.
     * @return a array of Device contains all devices that have a particular hardware serial number. 
     *         This method may return zero array if no device has this hardware serial
     *         number.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see Device#getSerialNumber()
     */
    public Device[] getDevicesBySerialNumber(String targetSerialNumber) throws DeletedException {
        Device[] resultArray = null;
        Vector <StoredDevice> result = new Vector<StoredDevice>(); 
        StoredDevice[] storedDevice = this.repository.getDevices();
        for ( int i=0; i < storedDevice.length; i++ ) {
        	if ( storedDevice[i].getSerialNumber().equals(targetSerialNumber) ) {
        		result.add(storedDevice[i]);
        	}
        }        
        
        int iSize = result.size();
        if ( iSize > 0 ) {
        	resultArray = new Device[iSize];
        	for ( int i=0; i < iSize; i++ ) {
        		PersistenceMapper mapper = PersistenceMapper.getInstance();
        		resultArray[i] = mapper.getVia(result.get(i));
        	}
        }
        else {
        	resultArray = new Device[0];
        }
        return(resultArray);
    }
    

    /**
     * Get a Device that has a particular symbolic name. This method does not
     * care if the Device is a member of a ManagedSet or not.
     * 
     * @param targetSymbolicName the symbolic name to look for.
     * @return a Device that has a particular symbolic name. This method may
     *         return null if no device has this symbolic name.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see Device#getSymbolicName()
     */
    public Device getDeviceBySymbolicName(String targetSymbolicName) throws DeletedException {
        Device result = null;
        Device[] devices = this.getAllDevices();
        for (int i=0; i<devices.length; i++) {
            if (devices[i].getSymbolicName().equals(targetSymbolicName)) {
                result = devices[i];
                break;
            }
        }
        return(result);
    }
    
    /**
     * Removes a device from the manager. Also removes it from a ManagedSet if
     * it is a member. 
     * 
     * @param device Device to remove from the Manager.
     * @throws NotEmptyException (deprecated)
     * @throws NotExistException
     * @throws InUseException if it is the last Device
     *         you must first remove it from the ManagedSet via
     *         {@link ManagedSet#removeDevice(Device)}.
     * @throws InvalidParameterException there was a problem removing the Device
     *         from its ManagedSet
     * @throws NotEmptyException (deprecated)
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws FullException the background task queue is full
     * @see ManagedSet#removeDevice(Device)
     */
    public void remove(Device device) 
        throws NotEmptyException, NotExistException, InUseException,
               InvalidParameterException, NotEmptyException,
               DatastoreException, LockBusyException, DeletedException, FullException {
        final String METHOD_NAME = "remove(Device)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, device);
        this.unmanagedDevicesLockNoWait();
        try {
            ManagedSet managedSet = device.getManagedSet();
              if (managedSet == null) {
                  logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                          "device is not a member of a ManagedSet"); //$NON-NLS-1$
              }
            device.destroy();
            this.save(SAVE_UNFORCED);
        } finally {
            this.unmanagedDevicesUnlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Remove a device from the manager even if it is in use (has managed
     * domains and/or is a member of a managed set). This should get called only
     * if the device is subscribed to another manager instance, or is running
     * firmware below the supported minimum level as detected by the
     * HeartbeatTask.
     * 
     * @param device the device to remove
     * @throws AMPException
     * @throws DeletedException
     * @throws InUseException
     * @throws InvalidParameterException
     * @throws NotEmptyException
     * @throws DeletedException
     * @throws DatastoreException
     * @throws NotExistException
     * @throws FullException 
     */
    void forceRemove(Device device) 
    throws AMPException, NotExistException, InUseException, InvalidParameterException, 
    NotEmptyException, DeletedException, DatastoreException, FullException {
        final String METHOD_NAME = "forceRemove"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, device);

        ManagedSet managedSet = null;
        try {
            managedSet = device.getManagedSet();
        } catch (DeletedException e) {
            // the device has already been deleted?
            managedSet = null;
        }
        if (managedSet != null) {
            managedSet.forceRemove(device);
        }
        
        unmanagedDevicesLock.lockWait();
        try {
            String message = Messages.getString("wamt.clientAPI.Manager.removingDevFromRep",device.getDisplayName()); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            try {
                this.remove(device);
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "device removal successful"); //$NON-NLS-1$
            } catch (LockBusyException e) {
                // we already have the lock, so this shouldn't happen
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
            }
        } finally {
            unmanagedDevicesLock.unlock();
        }

        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    void unmanagedDevicesLockWait() {
        this.unmanagedDevicesLock.lockWait();
    }
    
    void unmanagedDevicesLockNoWait() throws LockBusyException {
        this.unmanagedDevicesLock.lockNoWait();
    }

    void unmanagedDevicesUnlock() {
        this.unmanagedDevicesLock.unlock();
    }
    
    /**
     * Get the existing ManagedSet that has the specified name from the manager.
     * 
     * @param name name of the ManagedSet as stored in the manager
     * @return a reference to the ManagedSet object that has the specified name.
     *         If no ManagedSet has the specified name, this method may return
     *         null.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     * @see ManagedSet#getName()
     */
    public ManagedSet getManagedSet(String name) 
        throws DatastoreException, DeletedException {
        ManagedSet result = null;
        StoredManagedSet storedManagedSet = this.repository.getManagedSet(name);
        if (storedManagedSet != null) {
            PersistenceMapper mapper = PersistenceMapper.getInstance();
            result = mapper.getVia(storedManagedSet);
        }
        return(result);
    }

    /**
     * Get a list of all the ManagedSets in the manager.
     * 
     * @return a list of all ManagedSets. If there are no ManagedSets defined
     *         yet, this array may have zero elements.
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see "useCases section 4.2"
     */
    public ManagedSet[] getManagedSets() {
        final String METHOD_NAME = "getManagedSets"; //$NON-NLS-1$
        ManagedSet[] result = null;
        // get only the ManagedSets that exist in the PersistenceMapper, because
        // we may be in the middle of loading them via the dataAPI now.
        Vector collection = new Vector();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        StoredManagedSet[] storedManagedSets = this.repository.getManagedSets();
        for (int i=0; i<storedManagedSets.length; i++) {
            ManagedSet managedSet = null;
            try {
                managedSet = mapper.getVia(storedManagedSets[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            storedManagedSets[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                managedSet = null;
            }
            if (managedSet != null) {
                collection.add(managedSet);
            }
        }
        result = new ManagedSet[collection.size()];
        for (int i=0; i<collection.size(); i++) {
            result[i] = (ManagedSet) collection.get(i);
        }
        return(result);
    }
    
    /**
     * Remove an existing ManagedSet from the Manager. The ManagedSet is deleted and any devices
     * that were members of the ManagedSet are no longer managed.  The device and all the contained domains
     * are still persisted in the repository.  They can be added to another ManagedSet if needed.
     * 
     * @param managedSet the managedSet to remove from the Manager
     * @throws NotEmptyException the managed set still has managed domains in
     *         it. A ManagedSet must not have any managed domains if you wish to
     *         remove it.
     * @throws NotEmptyException there was a problem deleting the ManagedSet
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     * @throws InvalidParameterException there was a problem deleting the
     *         ManagedSet
     * @throws InUseException there was a problem deleting the ManagedSet
     * @throws LockBusyException the lock for the requested object is held by
     *         another thread. The acquisition of the lock was attempted as
     *         fail-fast instead of block. Try again later when the lock is
     *         available.
     * @throws FullException 
     * @see "useCases section 4.2"
     */
    public void remove(ManagedSet managedSet) 
        throws NotEmptyException, InUseException, InvalidParameterException, 
               DatastoreException, LockBusyException, FullException {
        final String METHOD_NAME = "remove(ManagedSet)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        managedSet.lockWait();
        
        try {
            managedSet.destroy();
        } catch (DeletedException e) {
            // the ManagedSet has already been deleted
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "deleted:", e); //$NON-NLS-1$
        } finally {
            managedSet.unlock();
        }
        this.save(SAVE_UNFORCED);
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    boolean containsManagedSetName(String targetName) {
        boolean result = false;
        ManagedSet[] allManagedSets = this.getManagedSets();
        for (int i=0; i<allManagedSets.length; i++) {
            try {
                if (allManagedSets[i].getName().equals(targetName)) {
                    result = true;
                }
            } catch (DeletedException e) {
                // this ManagedSet was deleted, so skip it
            }
        }
        return(result);
    }

    /////////////////////////////////////////////////////////////////////////////
    // for domains
    /////////////////////////////////////////////////////////////////////////////
    
    // n/a

    /////////////////////////////////////////////////////////////////////////////
    // for firmware
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Create a BackgroundTask to add a new firmware image to the manager. It
     * will automatically create a FirmwareVersion object for this Blob, and
     * will also automatically create a Firmware object if necessary to hold
     * this FirmwareVersion. This needs to run as a BackgroundTask because the
     * Blob is converted to a base64 encoded value, and that may take some time
     * for 20MB of data. The BackgroundTask should have the FirmwareVersion in
     * the ProgressContainer when it completes successfully.
     * 
     * @param image the firmware binary image to add
     * @param userComment a human-readable string that describes this image
     * @return a new ProgressContainer object to track the progress of this
     *         BackgroundTask and from which to retrieve the newly created
     *         FirmwareVersion object. From the FirmwareVersion object you can
     *         get the Firmware object, if desired.
     * @see Firmware#remove(Version)
     * @see FirmwareVersion#getFirmware()
     * @throws FullException the queue for background processing is already full
     *         and this item was not queued.
     * @see "useCases section 4.3, 4.4"
     */
    public ProgressContainer addFirmware(Blob image, String userComment) 
    throws FullException { 
        final String METHOD_NAME = "addFirmware"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {image, userComment});
        
        BackgroundTask backgroundTask = BackgroundTask.createAddFirmwareTask(image, 
                userComment);
        ProgressContainer result = backgroundTask.getProgressContainer();
        WorkArea workArea = this;
        this.enqueue(backgroundTask, workArea);
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return(result);
    }
    
    /**
     * Get a list of all the Firmware objects that are in the manager.
     * 
     * @return a list of Firmware object
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @see Firmware#getVersions()
     * @see "useCases section 4.3, 4.4"
     */
    public Firmware[] getFirmwares() {
        final String METHOD_NAME = "getFirmwares"; //$NON-NLS-1$
        Vector selectedFirmwares = new Vector();
        StoredFirmware[] storedFirmwares = this.repository.getFirmwares();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        for (int i=0; i<storedFirmwares.length; i++) {
            Firmware firmware = null;
            try {
                firmware = mapper.getVia(storedFirmwares[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            storedFirmwares[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                firmware = null;
            }
            if (firmware != null) {
                selectedFirmwares.add(firmware);
            }
        }

        Firmware[] result = new Firmware[selectedFirmwares.size()];
        for (int i=0; i<selectedFirmwares.size(); i++) {
            result[i] = (Firmware) selectedFirmwares.elementAt(i);
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "adding this to the result set: " + result[i]); //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * For a specified device profile (deviceType, modelType, strictFeatures,
     * nonStrictFeatures), get the Firmware object that would match that device
     * profile. When reading a firmware image Blob, this method determines which
     * Firmware that the newly-created FirmwareVersion should go under.
     * 
     * @param deviceType the device type that the firmware should be applicable
     *        to
     * @param modelType the model type of the device that the firmware should be
     *        applicable to
     * @param strictFeatures the strict features of the device that the firmware
     *        should be applicable to
     * @param nonstrictFeatures the non-strict features of the device that the
     *        firmware should be applicable to
     * @return a Firmware object that should correspond to the specified device
     *         profile/attributes. If no Firmware object exists yet for that
     *         device profile, then this method will return null.
     */
    public Firmware getFirmware(DeviceType deviceType, ModelType modelType,
                                StringCollection strictFeatures, StringCollection nonstrictFeatures) {
        final String METHOD_NAME = "getFirmware"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME,
                        new Object[] {deviceType, modelType, strictFeatures, nonstrictFeatures});
        Firmware result = null;
        
        StoredFirmware[] storedFirmwares = this.repository.getFirmwares();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        for (int i=0; i<storedFirmwares.length; i++) {
            Firmware firmware = null;
            try {
                firmware = mapper.getVia(storedFirmwares[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            storedFirmwares[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                firmware = null;
            }
            try {
                if ((firmware != null) && 
                    (firmware.getDeviceType().isCompatibleWith(deviceType)) && 
                    (firmware.getModelType().isCompatibleWith(modelType)) &&
                    (firmware.getStrictFeatures().equals(strictFeatures)) &&
                    (firmware.getNonstrictFeatures().equals(nonstrictFeatures))) {
                    result = firmware;
                    break;
                }
            } catch (DeletedException e) {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                            "Came across a deleted firmware while looping through the set, continuing.", //$NON-NLS-1$
                            e);
            }
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return(result);
    }
    
    /**
     * Get the best Firmware that is suitable for the specified deviceType,
     * modelType, and device features. The Firmware object must already exist.
     * Because the device can't tell us which features are strict and which are
     * non-strict, and because it is OK if the firmware provides extra
     * non-strict features, there is not a simple 1:1 mapping from the device's
     * metadata to a single firmware file. So because there could be multiple
     * firmware files that are a candidate for the device, approximate that 1:1
     * mapping by select a "best" Firmware. This method is used when we have a
     * device and are trying to determine which Firmware and/or FirmwareVersion
     * would be applicable for it.
     * 
     * @param deviceType the type of device that you want to find a Firmware
     *        object for
     * @param modelType the device's model type that you want to find a Firmware
     *        object for
     * @param deviceFeatures the device's features (both strict and non-strict)
     *        that you want to find a Firmware object for
     * @param level if you are looking for a specific firmware level, specify it
     *        here and discovered Firmwares that don't already have this level
     *        won't be included. If you aren't looking for a specific firmware
     *        level, just set this parameter to null.
     * @return a Firmware object that is suitable for the deviceType, modelType,
     *         and deviceFeatures indicated. If a level was specified, it will
     *         return a Firmware object that already has a FirmwareVersion of
     *         the specified level.
     */
    public Firmware getBestFirmware(DeviceType deviceType, ModelType modelType,
                                    StringCollection deviceFeatures, String level) {
        final String METHOD_NAME = "getBestFirmware"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME,
                        new Object[] {deviceType, modelType, deviceFeatures, level});
        Firmware result = null;
        
        // build a collection of candidate Firmware objects
        ArrayList<Firmware> candidates = new ArrayList<Firmware>();
        StoredFirmware[] storedFirmwares = this.repository.getFirmwares();
        PersistenceMapper mapper = PersistenceMapper.getInstance();
        for (int i=0; i<storedFirmwares.length; i++) {
            Firmware firmware = null;
            try {
                firmware = mapper.getVia(storedFirmwares[i]);
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            storedFirmwares[i] + 
                            " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
                firmware = null;
            }
            if ( firmware != null && firmware.isCompatibleWith(deviceType, modelType, deviceFeatures)) {
                if (level == null) {
                    // we don't care about the level
                	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                			"don't care about level, adding as candidate"); //$NON-NLS-1$
                    candidates.add(firmware);
                } else {
                    // we know we want a particular level and it is discovered, 
                    // so work to make sure that version is present for consolidation purposes
                    FirmwareVersion firmwareVersion = null;
                    try {
                        firmwareVersion = firmware.getLevel(level);
                    } catch (DeletedException e1) {
                        firmwareVersion = null;
                    }
                    if (firmwareVersion != null) {
                    	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    			"care about level and found it, adding as candidate"); //$NON-NLS-1$
                        candidates.add(firmware);
                    } else {
                    	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    			"care about level and didn't find it, not adding as candidate"); //$NON-NLS-1$
                    }
                }
            }
        }
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "found " + candidates.size() + " candidate(s)."); //$NON-NLS-1$ //$NON-NLS-2$
        
        // now pick the best candidate
        for (int i=0; i<candidates.size(); i++) {
            Firmware candidate = (Firmware) candidates.get(i);
        
            // if none found yet, grab this one as a default
            if (result == null) {
                result = candidate;
            }
            
            // otherwise "best" is defined as the most non-strict features
            int numCandidateFeatures = 0;
            int numResultFeatures = 0;
            
            try {
                numCandidateFeatures = candidate.getNonstrictFeatures().size();
            } catch (DeletedException e) {
                // assume zero
                numCandidateFeatures = 0;
            }
            
            try {
                numResultFeatures = result.getNonstrictFeatures().size();
            } catch (DeletedException e) {
                // assume zero
                numResultFeatures = 0;
            }
            
            if (numCandidateFeatures > numResultFeatures) {
                result = candidate;
            }
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME, result);
        return(result);
    }

    /**
     * Remove a Firmware object from the Manager. This will also remove all
     * FirmwareVersion objects which are under this Firmware.
     * 
     * @param firmware the Firmware object to remove. The child FirmwareVersion
     *        objects will be automatically found and removed from the Manager
     *        also.
     * @throws InUseException one of the FirmwareVersion objects in this
     *         Firmware is a desired version for a ManagedSet.
     * @throws InvalidParameterException there was a problem removing a
     *         FirmwareVersion
     * @throws DeletedException this object has been deleted from the persisted
     *         repository. The referenced object is no longer valid. You should
     *         not be using a reference to this object.
     * @throws DatastoreException there was a problem persisting this value to
     *         the repository or reading it from the repository.
     */
    public void remove(Firmware firmware) throws InUseException, InvalidParameterException, DeletedException, DatastoreException {
        final String METHOD_NAME = "remove(Firmware)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, firmware);
        Firmware.lockWait();
        try {
            
            // remove each of the versions first
            Version[] versions = firmware.getVersions();
            for (int i=0; i<versions.length; i++) {
                firmware.remove(versions[i]);
            }
            try {
                firmware.destroy();
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "deleted: " + e); //$NON-NLS-1$
                // the firmware was already deleted
            }
        } finally {
            Firmware.unlock();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /////////////////////////////////////////////////////////////////////////////
    // for notifications
    /////////////////////////////////////////////////////////////////////////////
    
    /**
     * Enqueue a Notification object to be processed by the Manager. This
     * Notification object is a change event from the device and was created by
     * the NotificationCatcher because we are subscribed to the device.
     * Generally only the NotificationCatcher should be calling this method.
     * User interfaces should not be calling this method.
     * The processing is performed in the background by the QueueProcessor.
     * 
     * @param notification the notification which was received and parsed by the
     *        NotificationCatcher.
     * @param workArea the workArea that the device is in. There is one queue
     *        per work area.
     */
    public void enqueue(Notification notification, WorkArea workArea) {
        final String METHOD_NAME = "enqueue(Notification)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "enqueuing Notification " + notification + //$NON-NLS-1$
                    " to " + workArea); //$NON-NLS-1$
        this.queueProcessor.enqueue(notification, workArea);
    }
    
    /**
     * Enqueue a BackgroundTask object to be processed by the Manager. This
     * BackgroundTask object may have been created by the caller of the
     * clientAPI, or it may have been created internally as the result of a
     * heartbeat or other internal processing. Generally only internal threads
     * should be calling this method. User interfaces should not be calling this
     * method. The processing is performed in the background by the
     * QueueProcessor.
     * 
     * @param backgroundTask the background task which needs to be performed.
     * @param workArea the workArea that the device is in. There is one queue
     *        per work area.
     * @throws FullException the queue for the background tasks for the work
     *         area is already full. The background task was not queued. Try
     *         again later when there are fewer background tasks on the queue of
     *         this work area.
     */
    public void enqueue(BackgroundTask backgroundTask, WorkArea workArea) throws FullException {
        final String METHOD_NAME = "enqueue(BackgroundTask)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "enqueuing (non-privileged) BackgroundTask " + backgroundTask + //$NON-NLS-1$
                    " to workArea " + workArea); //$NON-NLS-1$
        this.queueProcessor.enqueue(backgroundTask, workArea);
    }
    
    void privilegedEnqueue(BackgroundTask backgroundTask, WorkArea workArea) {
        final String METHOD_NAME = "privilegedEnqueue(BackgroundTask)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "enqueuing (privileged) BackgroundTask " + backgroundTask + //$NON-NLS-1$
                    " to workArea " + workArea); //$NON-NLS-1$
        this.queueProcessor.privilegedEnqueue(backgroundTask, workArea);
    }

    String getQueueProcessorActiveMethodName() {
        return(this.queueProcessor.getActiveMethodName());
    }
    
    /**
     * Get the URL that the NotificationCatcher will use when subscribing to
     * Devices in ManagedSets. This can be used to help the user verify that the
     * Manager's correct IPaddress/IPinterface is being used when the Manager
     * subscribes to Devices in ManagedSets.
     * 
     * @return the URL that the NotificationCatcher will use when subscribing to
     *         Devices in ManagedSets
     */
    public URL getNotificationCatcherURL() {
        if (this.notificationCatcher == null) {
            return(null);
        } else {
            return(this.notificationCatcher.getURL());
        }
    }
    
    /**
     * Get the sleep interval of the heartbeat daemon in milliseconds.
     * 
     * @return the sleep interval of the heartbeat daemon in milliseconds.
     * @see #setHeartbeatDaemonSleepMS(long)
     */
    public long getHeartbeatDaemonSleepMS() {
        return(this.heartbeatDaemon.getIntervalMS());
    }
    
    /**
     * Set the sleep interval of the heartbeat daemon. This change is not persisted after the application exits.
     * 
     * @param ms the sleep interval of the heartbeat daemon in milliseconds.
     */
    public void setHeartbeatDaemonSleepMS(long ms) {
    	
    	if(this.heartbeatDaemon == null){
    		final String METHOD_NAME = "setHeartbeatDaemonSleepMS";
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, 
                "HeartbeatDaemon was not started or has been disabled"); //$NON-NLS-1$
            return;
    	}
    	
        this.heartbeatDaemon.setIntervalMS(ms);
    }
    
    /**
     * Change the sleep interval for the DomainSynchronization daemon. This changes the scheduled interval 
     * between firing of the DomainSynchronization daemon. This change is not persisted after the application exits.
 
     * 
     * @param ms the sleep interval of the DomainSynchronization daemon in milliseconds.
     */
    public void setDomainSynchronizationDaemonSleepMS(long ms) {
        this.domainSynchDaemon.setIntervalMS(ms);
    }    
    
    /**
     * Get the sleep interval of the DomainSynchronization daemon in milliseconds.
     * 
     * @return the sleep interval of the DomainSynchronization daemon in milliseconds.
     * @see #setDomainSynchronizationDaemonSleepMS(long)
     * @see Domain#setSynchronizationMode(DomainSynchronizationMode)
     */
    public long getDomainSynchronizationDaemonSleepMS() {
        return(this.domainSynchDaemon.getIntervalMS());
    }
    
    /**
     * This should not be used except by JUnit test cases.
     * 
     * @return a reference to the HeartbeatDaemon
     * @see #getHeartbeatDaemonSleepMS()
     * @see #setHeartbeatDaemonSleepMS(long)
     */
    HeartbeatDaemon internalGetHeartbeatDaemon() {
        return(this.heartbeatDaemon);
    }
    
    void addNotificationProgress(MacroProgressContainer macroProgressContainer) {
        if (this.notificationProgresses != null) {
            try {
                this.notificationProgresses.add(macroProgressContainer);
            } catch (FullException e) {
                // shouldn't happen, since queue is created with no size limit 
            }
        }
    }
    
    /**
     * Get a reference to the Queue that holds the MacroProgressContainers
     * created by Notification tasks. This is so you can see what tasks are
     * queued for future execution, are in progress, and have run historically.
     * By default, this queue is not created and ProgressContainers are not
     * added here. To use this feature, in {@link #getInstance(Map)}, you must
     * specify the option {@link #OPTION_COLLECT_DAEMON_PROGRESSES}, and use
     * the object value {@link Boolean#TRUE}. If you specify that option, you
     * are responsible for removing MacroProgressContainers from this Queue,
     * otherwise the Queue will grow unbounded over the lifetime of the
     * NotificationCatcher daemon and consume system resources.
     * 
     * @return a reference to the Queue that holds MacroProgressContainers
     *         created by Notification tasks.
     */
    //* @see Queue
    //* @see #getHeartbeatProgresses()
    //* @see BackgroundTask
    public Queue getNotificationProgresses() {
        return(this.notificationProgresses);
    }
        
    void addHeartbeatProgress(MacroProgressContainer macroProgressContainer) {
        if (this.heartbeatProgresses != null) {
            try {
                this.heartbeatProgresses.add(macroProgressContainer);
            } catch (FullException e) {
                // shouldn't happen, since queue is created with no size limit
            }
        }
    }
    
    void addDomainSyncProgress(ProgressContainer progressContainer) {
        if (this.domainSyncProgresses != null) {
            try {
                this.domainSyncProgresses.add(progressContainer);
            } catch (FullException e) {
                // shouldn't happen, since queue is created with no size limit
            }
        }
    }    
        
    boolean getShutdownStatus() {
        return(this.shutdownRequested);
    }
    
    /**
     * Get a reference to the Queue that holds the ProgressContainers
     * created by the domain synchronization daemon. This is so you can see what the
     * DomainSynchronizationDaemon is doing and has discovered. By default, this queue is
     * not created and ProgressContainers are not added here. To use this
     * feature, in {@link #getInstance(Map)}, you must specify the option
     * {@link #OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES}, and use the object value
     * {@link Boolean#TRUE}. If you specify that option, you are responsible
     * for removing the ProgressContainers from this Queue, otherwise the
     * Queue will grow unbounded over the lifetime of the DomainSynchronizationDaemon and
     * consume system resources.
     * 
     * @return a reference to the Queue that holds ProgressContainers
     *         created by the DomainSynchronizationDaemon.
     */
    //* @see Queue
    //* @see #getNotificationProgresses()
    //* @see BackgroundTask
    public Queue getDomainSynchronizaionProgresses() {
        return(this.domainSyncProgresses);
    }    

    /**
     * Get a reference to the Queue that holds the MacroProgressContainers
     * created by the heartbeat daemon. This is so you can see what the
     * HeartbeatDaemon is doing and has discovered. By default, this queue is
     * not created and MacroProgressContainers are not added here. To use this
     * feature, in {@link #getInstance(Map)}, you must specify the option
     * {@link #OPTION_COLLECT_DAEMON_PROGRESSES}, and use the object value
     * {@link Boolean#TRUE}. If you specify that option, you are responsible
     * for removing the MacroProgressContainers from this Queue, otherwise the
     * Queue will grow unbounded over the lifetime of the HeartbeatDaemon and
     * consume system resources.
     * 
     * @return a reference to the Queue that holds MacroProgressContainers
     *         created by the HeartbeatDaemon.
     */
    //* @see Queue
    //* @see #getNotificationProgresses()
    //* @see BackgroundTask
    public Queue getHeartbeatProgresses() {
        return(this.heartbeatProgresses);
    }
    private void setMessagePrefix(String prefix){
    	Messages.setMessagePrefix(prefix);
    }
    
    /**
     * Generate a report that will capture the domain configuration deployments completed successfully.
     * @param filePath location to write the report that will be generated. 
     * @param fileName The base name of the file to be written
     * The filename will be appended with the current timestamp. 
     * Example fileName: "ReportMaint1" will
     * be named like ReportMaint12010-04-22_03-25-13.txt - <br/> Note the ".txt" extension is always added.  
     * @throws InvalidParameterException 
     * @throws IOException 
     * @throws DatastoreException 
     * 
     */
    public void generateReport(String filePath, String fileName) throws IOException, InvalidParameterException, DatastoreException{
    	// Save the Domain Configuration Deployment Report
        final String METHOD_NAME = "generateReport"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, filePath + ":" + fileName); 
        
        if (this.repository == null) {
            String message = Messages.getString("wamt.clientAPI.Manager.nullRefToRepos");
            DatastoreException e = new DatastoreException(message, "wamt.clientAPI.Manager.nullRefToRepos");
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }

        //Is filepath null?
        if (filePath == null) {
        	filePath = ".";
        }
        
        if (!filePath.endsWith(File.separator)) {
        	filePath = filePath + File.separator;
        }
        
        //Is filename null?
        if (fileName == null) {
        	fileName = "ApplianceManagerReport"; 
        }
        
        //Create the dir if it does not already exist.
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "filePath=" + filePath);
        File reportDirectory = new File(filePath);
    	if (!reportDirectory.exists()){
    		if ( !reportDirectory.mkdir() ) {
    			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Unable to create directory " + filePath);
    		}	
    	}
    	
    	String deploymentReportName = null; 
    	File deploymentReportFile = null;
    	OutputStream reportFileOutputStream = null;
    	
        try {
    	    deploymentReportName = filePath + fileName + getDate()+ "_" + getTime()+".txt";     
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "deploymentReportName=" + deploymentReportName);
    	    deploymentReportFile = new File(deploymentReportName);

    	    // Create file if it does not exist
    	    boolean success = deploymentReportFile.createNewFile();
			
    	    if (success) {
    	        // File did not exist and was created
    	        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
    	      		  " New Domain Configuration Deployment Report File created: " + deploymentReportName); //$NON-NLS-1$      

    	    } else {
    	        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
      	      		  "Domain Configuration Deployment Report File already exists : " + deploymentReportName); //$NON-NLS-1$      

    	    }
    	    
    	    reportFileOutputStream = new FileOutputStream(deploymentReportFile,true);    	    
    	} catch (IOException e) {
	        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
    	      		  "Unable to create or open a Deployment Report File: " + deploymentReportName); //$NON-NLS-1$      
    		throw e;
    	}
    	
	    String reportTimeStamp = this.getDate()+ " " + this.getTime();
		String reportHeader = "============== DP Report Generation Time : "+ reportTimeStamp + " ==============";  
		try {
			reportFileOutputStream.write(reportHeader.getBytes());     			
			reportFileOutputStream.write(getLineSeparator().getBytes());    
			reportFileOutputStream.write(getLineSeparator().getBytes());   				
	    
    		StoredManagedSet[] sets = repository.getManagedSets();
    		for(StoredManagedSet ms: sets){
    			StoredDevice[] devices =ms.getDeviceMembers();
				String msHeader = "============== Managed Set : "+ ms.getName() + " ==============";  
				reportFileOutputStream.write(msHeader.getBytes());
				reportFileOutputStream.write(getLineSeparator().getBytes());				
    			for (StoredDevice dv: devices){
    				reportFileOutputStream.write(getLineSeparator().getBytes());     				
    				String deviceHeader = "  Device - "+ dv.getSymbolicName();
    				reportFileOutputStream.write(deviceHeader.getBytes());
    				reportFileOutputStream.write(getLineSeparator().getBytes());    
    				reportFileOutputStream.write(getLineSeparator().getBytes());     				
    				StoredDomain[] domains = dv.getManagedDomains();
    				for (StoredDomain dom: domains){
    					StoredVersion[] versions = dom.getVersions();
    					for(StoredVersion ver: versions){
    						StoredDomainVersion v = (StoredDomainVersion)ver;
    						
    						v.recordDomainVersion(reportFileOutputStream);
    					}
    				}
    			}
    		}
		} finally {
			reportFileOutputStream.close();
		}

    }
    
    
    
  final static String getDate(  )   {   
          DateFormat df = new SimpleDateFormat( "yyyy-MM-dd" ) ;   
          df.setTimeZone( TimeZone.getTimeZone( "GMT" )  ) ;   
          return ( df.format( new Date(  )  )  ) ;   
      }   
  final static String getTime(  )   {   
          DateFormat df = new SimpleDateFormat( "hh-mm-ss" ) ;   
          df.setTimeZone ( TimeZone.getTimeZone ( "GMT" )  ) ;                          
          return ( df.format( new Date(  )  )  ) ;   
      }
   
   public String getLineSeparator(){
	   String lineSeparator = System.getProperty("line.separator");  
	   if ((lineSeparator == null) || (lineSeparator.length() == 0)) {
		   lineSeparator = "\n"; 
	   }   
	   return lineSeparator;
   }
   
   /**
    * Returns the version of this WAMT image as a String
    * @return
    */
   public String getVersion() {
	   return this.version;	   
   }
   
   // Add a DS task that has been created for processing
   public void addDomainSyncTaskInProgress(String key, DomainSynchronizationTask value) {
	   this.domainSyncTasksInProgress.put(key, value);	   
   }
   
   // Remove a DS task that had been created because it has completed processing
   public void removeDomainSyncTaskInProgress(String task) {
	   this.domainSyncTasksInProgress.remove(task);
   }  
   
   // Return Hashtable that tracks active DS tasks 
   public Hashtable <String, DomainSynchronizationTask> getDomainSyncTasksInProgress() {
	   Hashtable <String, DomainSynchronizationTask> result = new Hashtable <String, DomainSynchronizationTask>();
	   Set<String> allKeys = this.domainSyncTasksInProgress.keySet();
	   for ( String key: allKeys ) {
		   DomainSynchronizationTask value = this.domainSyncTasksInProgress.get(key);
		   result.put(key, value);
	   }
	   
	   return result;
   }
   
   private void sleep(long time) {
       final String METHOD_NAME = "sleep"; //$NON-NLS-1$
       logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});

       try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// Auto-generated catch block - eat it
           logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                   "interrupted from sleep. shutting down?"); //$NON-NLS-1$
		}
   }

   // To get the StoredTag instance
   StoredTagImpl getStoredTag(String name, String value) {
	   StoredTagImpl result = null;
	   StoredTag[] storedTags = this.repository.getTags();
       for (int i=0; i<storedTags.length; i++) {
    	   String tagName = storedTags[i].getName();
    	   String tagValue = storedTags[i].getValue();
    	   if ( tagName.equals(name) && tagValue.equals(value) ) {
    		   result = (StoredTagImpl) storedTags[i];
    		   break;
    	   }
       }
       return result;
   }
   
   /**
    * Get all unique tag names, return an empty set if no tags have been added
    * 
    * @return a set contains all tag names which were added before
    */
   public Set<String> getTagNames() {
	   final String METHOD_NAME = "getTagNames"; //$NON-NLS-1$
	   logger.entering(CLASS_NAME, METHOD_NAME);
	   
	   Set<String> result = new HashSet<String>();
       // get only the Tags that exist in the PersistenceMapper, because
       // we may be in the middle of loading them via the dataAPI now.
       StoredTag[] storedTags = this.repository.getTags();
       for (int i=0; i<storedTags.length; i++) {
    	   result.add(storedTags[i].getName());
       }             
       
       logger.exiting(CLASS_NAME, METHOD_NAME, result);
	   return (result);
   }
   
  /**
   * Get all unique tag values for a given tag name. return an empty set if no tags have been added for the given name
   *
   * @param name tag name
   * @return a set contains all tag value by the specific tag name which were added before
   * @throws InvalidParameterException 
   */
   public Set<String> getTagValues(String name) throws InvalidParameterException {
	   final String METHOD_NAME = "getTagValues"; //$NON-NLS-1$
	   logger.entering(CLASS_NAME, METHOD_NAME);
	   
	   if ( name == null || name.length() == 0 ) { // name is required
		   String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
		   throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
       }
	   
	   Set<String> result = new HashSet<String>();
	   StoredTag[] storedTags = this.repository.getTags();
       for (int i=0; i<storedTags.length; i++) {
    	   String tagName = storedTags[i].getName();
    	   if ( tagName.equals(name))
    		   result.add(storedTags[i].getValue());
       } 
	   
	   logger.exiting(CLASS_NAME, METHOD_NAME, result);
	   return result;	
   }
   
   private Set<Taggable> getTagAction(StoredTag storedTag ) {
	   final String METHOD_NAME = "getTagAction"; //$NON-NLS-1$
	   logger.entering(CLASS_NAME, METHOD_NAME);
	   
	   StoredDevice[] storedDevices = storedTag.getDeviceMembers();
	   PersistenceMapper mapper = PersistenceMapper.getInstance();
	   Set<Taggable> result = new HashSet<Taggable>();

	   // Get all tagged device
	   //for ( int j=0; j < storedDevices.length; j++ ) {
	   for ( StoredDevice storedDevice : storedDevices ) {	   
		   Device device = null;
		   try {
			   device = mapper.getVia(storedDevice);
		   } catch (DeletedException e) {
			   logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
					   storedDevice + " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
		       device = null;
		   }
		   if (device != null) {
			   result.add(device);
		   }
	   }
	   // Get all tagged domain
	   StoredDomain[] storedDomains = storedTag.getDomainMembers();
	   for ( StoredDomain storeDomain : storedDomains ) {
		   Domain domain = null;
		   try {
			   domain = mapper.getVia(storeDomain);
		   } catch (DeletedException e) {
			   logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
					   storeDomain + " has been deleted from mapper but not from datastore"); //$NON-NLS-1$
			   domain = null;
		   }
		   if (domain != null) {
			   result.add(domain);
		   }
	   }
	   logger.exiting(CLASS_NAME, METHOD_NAME, result);
	   return (result);
	}
   
   /**
    * Get all tagged resources by tag name. return an empty set if no tags have been added for the given name
    *
    * @param name the tag name
    * 
    * @return a set contains all tagged resources 
    * @throws InvalidParameterException 
    */
   public Set<Taggable> getTaggedByName(String name) throws InvalidParameterException {
	   final String METHOD_NAME = "getTaggedByName(" + name + "}"; //$NON-NLS-1$
	   logger.entering(CLASS_NAME, METHOD_NAME);
	   
	   if ( name == null || name.length() == 0 ) { // name is required
		   String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
		   throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
       }	   
	   
	   Set<Taggable> result = new HashSet<Taggable>();
	   
	   StoredTag[] storedTags = this.repository.getTags();
       for (int i=0; i<storedTags.length; i++) {
    	   if ( storedTags[i].getName().equals(name)) {
    		   result.addAll(this.getTagAction(storedTags[i]));
    	   }    		   
       }	   
	   logger.exiting(CLASS_NAME, METHOD_NAME, result);
	   
	   return result;
   }
 
   /**
    * Get all resources tagged with a given name and value. return an empty set if there are no matches
    * 
    * @param name the tag name
    * @param value the tag value
    * 
    * @return a set contains all resources tagged with a given name and value
    * @throws InvalidParameterException 
    */
   public Set<Taggable> getTaggedByNameValue(String name, String value) throws InvalidParameterException {
	   final String METHOD_NAME = "getTaggedByNameValue(" + name + "," + value + ")"; //$NON-NLS-1$
	   logger.entering(CLASS_NAME, METHOD_NAME);
	   
	   if ( name == null || name.length() == 0 ) { // name is required
		   String message = Messages.getString("wamt.clientAPI.Tag.invalidParameter");
		   throw new InvalidParameterException(message, "wamt.clientAPI.Tag.invalidParameter");
       }
	   if ( value == null ) {
		   value = "null";
	   }
	   
	   Set<Taggable> result = new HashSet<Taggable>();
	   
	   StoredTag[] storedTags = this.repository.getTags();	   
       for (int i=0; i<storedTags.length; i++) {
    	   if ( storedTags[i].getName().equals(name) && storedTags[i].getValue().equals(value)) {
    		   result.addAll(this.getTagAction(storedTags[i]));
    	   }    		   
       }	   
	   logger.exiting(CLASS_NAME, METHOD_NAME, result);
	   
	   return result;
   }

}
