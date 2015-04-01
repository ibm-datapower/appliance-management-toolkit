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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Credential;
import com.ibm.datapower.amt.DeviceLogger;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.CommandFactory;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.NotificationCatcher;
import com.ibm.datapower.amt.amp.NotificationCatcherFactory;
import com.ibm.datapower.amt.logging.LoggerHelper;

// BEGIN INTERNAL COMMENTS 
//     Some of this information is irrelevant and confusing from a customer 
//     perspective so it is exempt from the JavaDoc.
// * Rollup all configuration data for writing and reading. Configuration can be
// * specified only at startup time, there are no general setter methods for this
// * class. However, other classes may provide their own setter method, for
// * example, <code>Manager.setMaxVersionsToStore(int)</code>, but that is
// * considered outside the scope of this class. Configuration can be set via a
// * properties file, or it can be specified on the <code>Map</code> on the
// * <code>Manager</code> constructor, or it can fall to a default value which
// * is hardcoded here. When reading configuration data, use this class' methods
// * outlined below, not <code>ConfigHelper.getProperty(key)</code> or
// * <code>Manager.getOption(key)</code>.
// * <p>
// * We read from all three "config spaces" and layer it with this class as a
// * top-level space that merges the first three. The properties space is created
// * by reading the properties file, the options space is created naturally by the
// * clientAPI consumer (as they pass in the Map to
// * <code>Manager.getInstance</code>), and this class creates the default
// * space.
// * <p>
// * When fetching a value via this class, there is a preference order. First it
// * will check if a value was specified via the properties file, as that gives us
// * opportunity in the field to override values without a recompile. The
// * properties file is read only once, at startup. Subsequent changes to the
// * properties file will not take effect until a restart. If there is not an
// * explicit property with a non-null value, it will then look in the Manager
// * options. The Manager options are read only once, when the Manager singleton
// * is instantiated. Subsequent changes to the Manager options Map will not take
// * effect until a restart, at which time the clientAPI consumer would need to
// * rebuild the options Map anyway. If there is not an explicit Manager option
// * with a non-null value, it will then use a default value. (Note that a default
// * value may be null.) This approach also enforces that the coverage of all the
// * config spaces are equal - this means that a configuration item can be set by
// * any config space (property, option, or default). This was not true
// * previously.
// * <p>
// * Whenever you want configuration data, fetch it via this class instead of
// * calling <code>ConfigHelper.getProperty()</code> or
// * <code>Manager.getOptions()</code>. For example, a get looks like:
// * <ul>
// * <li><code>Configuration.get(Configuration.KEY_COMMANDS_IMPL)</code></li>
// * </ul>
// * Note that the key parameter is an identifier instead of a String literal.
// * This is to help minimize typographical errors. If the value of thie
// * configuration item originated from the properties file, Manager option, or
// * default value, the above method will do the correct thing. Please use the
// * form above.
// * <p>
// * The following two forms must not be used, consider them deprecated or no
// * longer available. They do not merge all the configuration spaces and do not
// * use fallbacks.
// * <ul>
// * <li><code>Manager.getOption(Manager.OPTION_COMMANDS_IMPL)</code></li>
// * <li><code>ConfigHelper.getProperty("commandsImpl")</code></li>
// * </ul>
// * <p>
// * Note that the Manager options Map takes Objects instead of just Strings. When
// * the Manager option Objects are merged into this class, they are converted to
// * Strings. For example, if the Manager option Map contained a Boolean, it would
// * be stored in this class after invoking its toString() method, which will
// * yield a String of "true" or "false". So beware of this when fetching values
// * that were loaded from the Manager. Also note that if a Credential object is
// * in the Manager options Map, it will not be transferred at all into this
// * class. A Credential object is considered secret and is not readable outside
// * of the Manager.
// * <p>
// * If a properties file does not exist, and the environment variable specified
// * by {@link #ENV_VAR_NAME} is set, then a default properties file will be
// * created. It is expected that <code>ENV_VAR_NAME</code> will not be set for
// * use with WAS/ISC. It will have only the non-hidden items, each item will
// * include the default value, and each item will be commented out. The name of
// * the properties file for both reading and default creation is specified in
// * {@link #PROPERTIES_FILE_RELATIVE_NAME}, relative to the directory specified
// * by the environment variable specified by {@link #ENV_VAR_NAME}.
// * <p>
// * This class will likely be used only within the Manager. A consumer of the clientAPI
// * will not need this class, unless they want to peek at a configuration
// * value. If you want to add a new configuration item, please review the
// * comments in this source file.
//END INTERNAL COMMENTS 
/**
 * This class loads all of the configuration data for the manager. 
 * Configuration can be specified only at startup time, there are no 
 * setter methods in this class to change configuration values after the 
 * <code>Manager</code> object has been instantiated. All configuration properties
 * have default values, which can be overridden by key/value pairs in a 
 * properties file, which may be overridden by key/value pairs in the <code>Map</code> 
 * object passed to the constructor for <code>Manager</code>.
 * <p>
 * Configuration reads from all three "config spaces". The Map object takes priority 
 * over the properties file, and the properties file takes priority over the
 * default values, as shown in the list below:
 * <ul>  
 *    <li>Options specified by the <code>Map</code> used in the <code>Manager</code> constructor</li>
 *    <li>Options specified in the configuration file</li>
 *    <li>Default values for options</li>
 * </ul>
 * The IBM Appliance Management Toolkit properties file is named WAMT.properties. 
 * The properties file location is specified by the WAMT_CONFIGURATION_HOME java system property.<br/>
 * Example:<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-DWAMT_CONFIGURATION_HOME=C:\WAMT_CONFIG<br/>
 * &nbsp;&nbsp;Would cause the manager to expect a file with this name in this path<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;C:\WAMT_CONFIG\WAMT.properties
 * <br/>
 * <br/>
 * The properties in the file should be organized as a list of key=value pairs, with one 
 * key/value pair per line. The keys can be determined by inspecting the Constant Field Values 
 * for the configuration (look for constants KEY_ for the property name and DEFAULT_ for 
 * the default value for each property. Valid values vary based on the property. 
 */
public class Configuration {
    
    private static ItemCollection itemCollection = null;
    private static String rootDirectory = null;
    private static boolean isRootDirectoryEnvVarSet = false;
    private static boolean isPropertiesLoaded = false;
    private static boolean isOptionsLoaded = false;

    protected final static String CLASS_NAME = Configuration.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    

    /**
     * The name of the environment variable that has a value which indicates the
     * manager's home directory. The properties file will be looked for in this
     * directory. If a properties file is not found in this directory and this
     * variable has a non-null value, a default properties file will be
     * generated. The name of the properties file is specified by
     * {@link #PROPERTIES_FILE_RELATIVE_NAME}.
     */
    public static final String ENV_VAR_NAME = "WAMT_CONFIGURATION_HOME"; //$NON-NLS-1$
    
    /**
     * The name of the properties file, relative to the directory specified
     * by {@link #ENV_VAR_NAME}.
     */
    private static final String PROPERTIES_FILE_RELATIVE_NAME =
        "WAMT.properties" ; //$NON-NLS-1$ //$NON-NLS-2$
    
    /*
     * These are the canonical keys that consumers/readers use. Each should
     * start with "KEY_". Consumers/readers should not use Manager.OPTIONS* or
     * ConfigHelper.getProperty. Make sure there is a correlating entry in
     * Manager.OPTIONS* The values of these strings are the property names in
     * the properties file, so the values matter for backward compatibility.
     * 
     * If you want to add a new configuration item, you'll need to:
     *   - create a corresponding KEY_ in this class
     *   - create a corresponding DEFAULT_ in this class
//     *   - create a corresponding VISIBILITY_ in this class
     *   - create a corresponding OPTIONS_ in the Manager class
     *   - add the appropriate validation to Manager.checkOptions()
     *   - in the static block in this class, add an invocation to "addNewItem" with
     *        the above parameters KEY_, DEFAULT_.
//     *        the above parameters KEY_, DEFAULT_, VISIBILITY_.
//     * That's all!
     */

    /**
     * The number of domain synchronization threads that should exist to service all managed domains.
     * <br>
     * matching option key name:
     * {@link Manager#OPTION_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE} <br>
     * default value: {@link Configuration#DEFAULT_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE}
//  * @see HeartbeatDaemon
    public static final String KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE = "DomainSynchronizationThreadPoolSize"; //$NON-NLS-1$
    
    /**
     * Period in milliseconds between which a domain synchronization daemon should fire.  <br>
     * matching option key name: {@link Manager#OPTION_DOMAIN_SYNCHRONIZATION_INTERVAL} <br>
     * default value: {@link Configuration#DEFAULT_DOMAIN_SYNCHRONIZATION_INTERVAL} <br>
     * 
     */
//  * visibility: {@link Configuration#v}
//  * @see HeartbeatDaemon
    public static final String KEY_DOMAIN_SYNCHRONIZATION_INTERVAL = "DomainSynchronizationInterval"; //$NON-NLS-1$
    
    
    /**
     * Mow many times to retry a domain synch before disabling automatic synch.  <br>
     * matching option key name: {@link Manager#OPTION_DOMAIN_SYNCHRONIZATION_RETRY_MAX} <br>
     * default value: {@link Configuration#DEFAULT_DOMAIN_SYNCHRONIZATION_RETRY_MAX} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_DOMAIN_SYNCHRONIZATION_RETRY_MAX}
//  * @see HeartbeatDaemon
    public static final String KEY_DOMAIN_SYNCHRONIZATION_RETRY_MAX = "DomainSynchronizationRetryMax"; //$NON-NLS-1$
    
    
    /**
     * The number of heart beat threads that should exist to service all devices.
     * <br>
     * matching option key name:
     * {@link Manager#OPTION_HEARTBEAT_THREAD_POOL_SIZE} <br>
     * default value: {@link Configuration#DEFAULT_HEARTBEAT_THREAD_POOL_SIZE} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_HEARTBEAT_THREAD_POOL_SIZE}
//  * @see HeartbeatDaemon
    public static final String KEY_HEARTBEAT_THREAD_POOL_SIZE = "HeartbeatThreadPoolSize"; //$NON-NLS-1$
    
    /**
     * Period in milliseconds between which a HeartbeatTask should fire for each
     * device. <br>
     * matching option key name: {@link Manager#OPTION_HEARTBEAT_INTERVAL} <br>
     * default value: {@link Configuration#DEFAULT_HEARTBEAT_INTERVAL} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_HEARTBEAT_INTERVAL}
//  * @see HeartbeatDaemon
    public static final String KEY_HEARTBEAT_INTERVAL = "HeartbeatInterval"; //$NON-NLS-1$
    
    /**
     * Period in milliseconds to monitor the AMP connect request.<br>
     * If the connect request times out, the connection is terminated and an <br> 
     * exception is thrown. The specified value must be a positive integer.<br>
     * Otherwise, the {@link Configuration#DEFAULT_AMP_CONNECT_TIMEOUT} value is used.<br>
     * matching option key name: {@link Manager#OPTION_AMP_CONNECT_TIMEOUT} <br>
     * default value: {@link Configuration#DEFAULT_AMP_CONNECT_TIMEOUT} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_AMP_CONNECT_TIMEOUT}
    public static final String KEY_AMP_CONNECT_TIMEOUT = "AMPConnectTimeout"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.Commands} interface. <br>
     * matching option key name: {@link Manager#OPTION_COMMANDS_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_COMMANDS_IMPL} <br>
     *      
     * @see Commands
     * @see CommandFactory#getCommands(String, String)
     */
//  * visibility: {@link Configuration#VISIBILITY_COMMANDS_IMPL}
    public static final String KEY_COMMANDS_IMPL = "commandsImpl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.Commands} interface. <br>
     * matching option key name: {@link Manager#OPTION_COMMANDS_V2_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_COMMANDS_V2_IMPL} <br>
     *      
     * @see Commands
     * @see CommandFactory#getCommands(String, String)
     */
//  * visibility: {@link Configuration#VISIBILITY_COMMANDS_V2_IMPL}
    public static final String KEY_COMMANDS_V2_IMPL = "commandsV2Impl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.Commands} interface. <br>
     * matching option key name: {@link Manager#OPTION_COMMANDS_V3_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_COMMANDS_V3_IMPL} <br>
     *      
     * @see Commands
     * @see CommandFactory#getCommands(String, String)
     */
//  * visibility: {@link Configuration#VISIBILITY_COMMANDS_V3_IMPL}
    public static final String KEY_COMMANDS_V3_IMPL = "commandsV3Impl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.soma.SOMACommands} interface. <br>
     * matching option key name: {@link Manager#OPTION_COMMANDS_SOMA_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_COMMANDS_SOMA_IMPL} <br>
     *      
     * @see Commands
     * @see CommandFactory#getCommands(String, String)
     */
//  * visibility: {@link Configuration#VISIBILITY_COMMANDS_V3_IMPL}
    public static final String KEY_COMMANDS_SOMA_IMPL = "SOMACommandsImpl"; //$NON-NLS-1$
    

    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.Commands} interface. <br>
     * matching option key name: {@link Manager#OPTION_COMMANDS_V3_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_COMMANDS_V3_IMPL} <br>
     *      
     * @see Commands
     * @see CommandFactory#getCommands(String, String)
     */
//  * visibility: {@link Configuration#VISIBILITY_COMMANDS_V3_IMPL}
    public static final String KEY_COMMANDS_XC10_IMPL = "commandsXC10Impl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface. <br>
     * matching option key name: {@link Manager#OPTION_SOAPHELPER_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_SOAP_HELPER_IMPL} <br>
     * 
     * @see com.ibm.datapower.amt.amp.SOAPHelper
     * @see com.ibm.datapower.amt.amp.SOAPHelperFactory#getSOAPHelper(String)
     */
//  * visibility: {@link Configuration#VISIBILITY_SOAP_HELPER_IMPL}
    public static final String KEY_SOAP_HELPER_IMPL = "soapHelperImpl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface. <br>
     * matching option key name: {@link Manager#OPTION_SOAPHELPER_V2_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_SOAP_HELPER_V2_IMPL} <br>
     * 
     * @see com.ibm.datapower.amt.amp.SOAPHelper
     * @see com.ibm.datapower.amt.amp.SOAPHelperFactory#getSOAPHelper(String)
     */
//  * visibility: {@link Configuration#VISIBILITY_SOAP_HELPER_V2_IMPL}
    public static final String KEY_SOAP_HELPER_V2_IMPL = "soapHelperV2Impl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface. <br>
     * matching option key name: {@link Manager#OPTION_SOAPHELPER_V3_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_SOAP_HELPER_V3_IMPL} <br>
     * 
     * @see com.ibm.datapower.amt.amp.SOAPHelper
     * @see com.ibm.datapower.amt.amp.SOAPHelperFactory#getSOAPHelper(String)
     */
//  * visibility: {@link Configuration#VISIBILITY_SOAP_HELPER_V3_IMPL}
    public static final String KEY_SOAP_HELPER_V3_IMPL = "soapHelperV3Impl"; //$NON-NLS-1$    
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.SOAPHelper} interface. <br>
     * matching option key name: {@link Manager#OPTION_SOAPHELPER_SOMA_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_SOAP_HELPER_SOMA_IMPL} <br>
     * 
     * @see com.ibm.datapower.amt.amp.SOAPHelper
     * @see com.ibm.datapower.amt.amp.SOAPHelperFactory#getSOAPHelper(String)
     */
//  * visibility: {@link Configuration#VISIBILITY_SOAP_HELPER_V3_IMPL}
    public static final String KEY_SOAP_HELPER_SOMA_IMPL = "soapHelperSomaImpl"; //$NON-NLS-1$
    
    /**
     * Class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.dataAPI.Repository} interfaces. <br>
     * matching option key name: {@link Manager#OPTION_REPOSITORY_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_REPOSITORY_IMPL} <br>
     * 
     * @see com.ibm.datapower.amt.dataAPI.Repository
     * @see com.ibm.datapower.amt.dataAPI.RepositoryFactory#getRepository(String, Credential)
     */
//  * visibility: {@link Configuration#VISIBILITY_REPOSITORY_IMPL}
    public static final String KEY_REPOSITORY_IMPL = "repositoryImpl"; //$NON-NLS-1$
    
    /**
     * class name to instantiate that implements the
     * {@link com.ibm.datapower.amt.amp.NotificationCatcher} interface. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_IMPL} <br>
     * default value: {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_IMPL} <br>
     * 
     * @see NotificationCatcher
     * @see NotificationCatcherFactory#getNotificationCatcher(String)
     */
//  * visibility: {@link Configuration#VISIBILITY_NOTIFICATION_CATCHER_IMPL}
    public static final String KEY_NOTIFICATION_CATCHER_IMPL = "notificationCatcherImpl"; //$NON-NLS-1$ 

    /**
     * Port for the NotificationCatcher to listen on for events. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_PORT} <br>
     * default value: {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_IP_PORT} <br>
     * 
     * @see NotificationCatcher#getURL()
     */
//  * visibility: {@link Configuration#VISIBILITY_NOTIFICATION_CATCHER_IP_PORT}
    public static final String KEY_NOTIFICATION_CATCHER_IP_PORT = "SSLPort"; //$NON-NLS-1$
    
    /**
     * IP address that the manager will listen on for events from the device, this is
     * the IP address part of the notification subscription URL. This would be
     * used if you have more than one IP interface on the computer running the
     * Manager. You can use {@link #KEY_NOTIFICATION_CATCHER_IP_INTERFACE}
     * instead if desired. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_IP_ADDRESS} <br>
     * default value:
     * {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_IP_ADDRESS} <br>
     * 
     * @see NotificationCatcher#getURL()
     */
//  * visibility:
//  * {@link Configuration#VISIBILITY_NOTIFICATION_CATCHER_IP_ADDRESS}
    public static final String KEY_NOTIFICATION_CATCHER_IP_ADDRESS = "IPAddress";  //$NON-NLS-1$
    
    /**
     * Name of IP interface that the manager will listen on for events from the device,
     * this is the interface whose IP address is used in the notification
     * subscription URL. This would be used if you have more than one IP
     * interface on the computer running the Manager. On a Windows platform,
     * this should be in shortname form, i.e. "eth1". You can use
     * {@link #KEY_NOTIFICATION_CATCHER_IP_ADDRESS} instead if desired. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_IP_INTERFACE} <br>
     * default value:
     * {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_IP_INTERFACE} <br>
     * 
     * @see NotificationCatcher#getURL()
     */
//  * visibility:
//  * {@link Configuration#VISIBILITY_NOTIFICATION_CATCHER_IP_INTERFACE}
    public static final String KEY_NOTIFICATION_CATCHER_IP_INTERFACE = "IPInterface";  //$NON-NLS-1$
    
    /**
     * The NotificationCatcher should listen with a cleartext socket connection
     * instead of an SSL socket connection. The subscription (log target) on the
     * device needs to have a matching SSL enablement. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_NO_SSL} <br>
     * default value: {@link Configuration#DEFAULT_NOTIFICATION_CATCHER_NO_SSL} <br>
     * 
     * @see NotificationCatcher
     */
//  * visibility: {@link Configuration#VISIBILITY_NOTIFICATION_CATCHER_NO_SSL}
    public static final String KEY_NOTIFICATION_CATCHER_NO_SSL = "NoAMPEventSSL"; //$NON-NLS-1$
    
    /**
     * Port for the HTTPListener to listen on for HTTP file requests. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_PORT} <br>
     * default value: {@link Configuration#DEFAULT_HTTP_LISTENER_IP_PORT} <br>
     */
//  * visibility: {@link Configuration#VISIBILITY_HTTP_LISTENER_IP_PORT}
    public static final String KEY_HTTP_LISTENER_IP_PORT = "HTTPListenerPort"; //$NON-NLS-1$
    
    /**
     * IP address that the HTTPListener will listen on for HTTP file requests 
     * from the device, this is the IP address part of the file URL. This would be
     * used if you have more than one IP interface on the computer running the
     * Manager. You can use {@link #KEY_HTTP_LISTENER_IP_INTERFACE}
     * instead if desired. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_IP_ADDRESS} <br>
     * default value:
     * {@link Configuration#DEFAULT_HTTP_LISTENER_IP_ADDRESS} <br>
     */
//  * visibility:
//  * {@link Configuration#VISIBILITY_HTTP_LISTENER_IP_ADDRESS}
    public static final String KEY_HTTP_LISTENER_IP_ADDRESS = "HTTPListenerIPAddress";  //$NON-NLS-1$
    
    /**
     * IP address that the HTTP Listener will listen on for HTTP file requests
     * from the device, this is the interface whose IP address is used in the 
     * file URL. This would be used if you have more than one IP
     * interface on the computer running the Manager. On a Windows platform,
     * this should be in shortname form, i.e. "eth1". You can use
     * {@link #KEY_HTTP_LISTENER_IP_ADDRESS} instead if desired. <br>
     * matching option key name:
     * {@link Manager#OPTION_NOTIFICATION_CATCHER_IP_INTERFACE} <br>
     * default value:
     * {@link Configuration#DEFAULT_HTTP_LISTENER_IP_INTERFACE} <br>
     */
//  * visibility:
//  * {@link Configuration#VISIBILITY_HTTP_LISTENER_IP_INTERFACE}
    public static final String KEY_HTTP_LISTENER_IP_INTERFACE = "HTTPListenerIPInterface";  //$NON-NLS-1$

    /**
     * The interval in seconds since the HTTP listener is created 
     * in which HTTP listener will time out if there is no incoming 
     * connection.  
     * {@link Manager#OPTION_HTTP_LISTENER_SOCKET_TIMEOUT} <br>
     * default value:
     * {@link Configuration#DEFAULT_HTTP_LISTENER_SOCKET_TIMEOUT} <br>
     */
//  * visibility:
//  * {@link Configuration#VISIBILITY_HTTP_LISTENER_SOCKET_TIMEOUT}
    public static final String KEY_HTTP_LISTENER_SOCKET_TIMEOUT = "HTTPListenerSocketTimeout";  //$NON-NLS-1$
    
    /**
     * The port number for {@link com.ibm.datapower.amt.DeviceLogger} to listen
     * on. <br>
     * matching option key name: {@link Manager#OPTION_DEVICE_LOG_PORT} <br>
     * default value: {@link Configuration#DEFAULT_DEVICE_LOG_PORT} <br>
     * 
     * @see DeviceLogger
     */
//  * visibility: {@link Configuration#VISIBILITY_DEVICE_LOG_PORT}
    public static final String KEY_DEVICE_LOG_PORT = "DeviceLogPort"; //$NON-NLS-1$
    
    /**
     * Maximum number of Tasks that can exist on any queue in the
     * QueueProcessor. This value should be null if you want there to be no
     * limit. <br>
     * matching option key name: {@link Manager#OPTION_TASK_QUEUE_SIZE} <br>
     * default value: {@link Configuration#DEFAULT_TASK_QUEUE_SIZE} <br>
     * 
     * @see FullException
     */
//  * visibility: {@link Configuration#VISIBILITY_TASK_QUEUE_SIZE}
    public static final String KEY_TASK_QUEUE_SIZE = "taskQueueSize"; //$NON-NLS-1$
    
    /**
     * The daemons (QueueProcessor and HeartbeatDaemon) should create
     * ProgressContainers and place them on a queue for retrieval by the user
     * interface. The ProgressContainers provide visibility/status in to what
     * the QueueProcessor and HeartbeatDaemon are doing, and may be the only
     * visible indication of errors from automated tasks such as the
     * HeartbeatDaemon which are not user-initiated. If you select "false", you
     * will not have the ability to show heartbeat activity or display errors
     * that may occur during the heartbeat activity (although the heartbeats
     * will still occur and be processed). If you select "true", you must remove
     * the ProgressContainers from the queue, or else the queue will grow
     * unbounded and consume resources that will accumulate. <br>
     * matching option key name:
     * {@link Manager#OPTION_COLLECT_DAEMON_PROGRESSES} <br>
     * default value: {@link Configuration#DEFAULT_COLLECT_DAEMON_PROGRESSES}
     * <br>
     * 
     * @see Manager#getNotificationProgresses()
     * @see Manager#getHeartbeatProgresses()
     */
//  * visibility: {@link Configuration#VISIBILITY_COLLECT_DAEMON_PROGRESSES}
    public static final String KEY_COLLECT_DAEMON_PROGRESSES = "collectDaemonProgresses"; //$NON-NLS-1$
    
    /**
     * The DomainSynchronizationDaemon creates
     * ProgressContainers and places them on a queue for retrieval by the user
     * interface. The ProgressContainers provide visibility/status in to what
     * the DomainSynchronizationDaemon is doing, and may be the only
     * visible indication of errors from automated tasks which are not user-initiated. 
     * If you select "false", you will not have the ability to show activity or display errors
     * that may occur during the domain synchronization activity (although the synchronization
     * will still occur and be processed). If you select "true", you must remove
     * the ProgressContainers from the queue, or else the queue will grow
     * unbounded and consume resources that will accumulate. <br>
     * matching option key name:
     * {@link Manager#OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES} <br>
     * default value: {@link Configuration#DEFAULT_COLLECT_DOMAIN_SYNCH_PROGRESSES}
     * <br>
     * 
     * @see Manager#getNotificationProgresses()
     * @see Manager#getHeartbeatProgresses()
     */
    public static final String KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES = "collectDomainSynchProgresses"; //$NON-NLS-1$
        
    
    /**
     * Disable the Heartbeat daemon and its tasks from running. This should be
     * done only for debug purposes. <br>
     * matching option key name: {@link Manager#OPTION_DEBUG_DISABLE_HEARTBEAT}
     * <br>
     * default value: {@link Configuration#DEFAULT_HEARTBEAT_DISABLE} <br>
     */
//  * visibility: {@link Configuration#VISIBILITY_HEARTBEAT_DISABLE}
    public static final String KEY_HEARTBEAT_DISABLE = "HeartbeatDisable"; //$NON-NLS-1$
    
    /**
     * This value sets the default timeout value (in seconds) for domain quiesce operations. 
     * <br>
     * default value: {@link Configuration#DEFAULT_DOMAIN_QUIESCE_TIMEOUT} <br>
     */
//  * matching option key name: {@link Manager#OPTION_DOMAIN_QUIESCE_TIMEOUT}
//  * visibility: {@link Configuration#VISIBILITY_DOMAIN_QUIESCE_TIMEOUT}
    public static final String KEY_DOMAIN_QUIESCE_TIMEOUT = "DomainQuiesceTimeout"; //$NON-NLS-1$
    
    /**
     * This value sets the default timeout value (in seconds) for device quiesce operations. 
     * <br>
     * default value: {@link Configuration#DEFAULT_DEVICE_QUIESCE_TIMEOUT} <br>
     */
    public static final String KEY_DEVICE_QUIESCE_TIMEOUT = "DeviceQuiesceTimeout"; //$NON-NLS-1$
    
    /**
     * Disable the domain synchronization daemon and its tasks from running. This should be
     * done only for debug purposes. <br>
     * matching option key name: {@link Manager#OPTION_DEBUG_DOMAIN_SYNCHRONIZATION_DISABLE}
     * <br>
     * default value: {@link Configuration#DEFAULT_DOMAIN_SYNCHRONIZATION_DISABLE} <br>
     */
//  * visibility: {@link Configuration#VISIBILITY_DOMAIN_SYNCHRONIZATION_DISABLE}
    //public static final String KEY_DOMAIN_SYNCHRONIZATION_DISABLE = "DomainSynchronizationDisable"; //$NON-NLS-1$
    
    /**
     * When a <code>DomainVersion</code> or <code>DeploymentPolicyVersion</code> is
     * created, serialize it to the local filesystem. These simple blobs can be
     * inspected offline. This would be done only for debug purposes. <br>
     * matching option key name:
     * {@link Manager#OPTION_DEBUG_CAPTURE_VERSION_BLOB} <br>
     * default value: {@link Configuration#DEFAULT_CAPTURE_VERSION_BLOB} <br>
     * 
     * @see Configuration#getRootDirectory()
     */
//  * visibility: {@link Configuration#VISIBILITY_CAPTURE_VERSION_BLOB}
    public static final String KEY_CAPTURE_VERSION_BLOB = "debugCaptureVersionBlob"; //$NON-NLS-1$
    
    /**
     * The URL of a third-party SOAP service to be signaled immediately before
     * and after a change (firmware or domain) is deployed to a
     * device. The signaler is helpful in graceful shutdown of the device from a
     * load balancer. If this value is null there there will be no signaling.<br>
     * matching option key name: {@link Manager#OPTION_SIGNAL_URL} <br>
     * default value: {@link Configuration#DEFAULT_SIGNAL_URL} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_SIGNAL_URL}
    public static final String KEY_SIGNAL_URL = "signalUrl"; //$NON-NLS-1$
    
    /**
     * The value for the SOAPAction header of a third-party SOAP service to be
     * signaled immediately before and after a change (firmware or
     * domain) is deployed to a device. The signaler is helpful in graceful
     * shutdown of the device from a load balancer. If this value is null there
     * there will be no SOAPAction header.<br>
     * matching option key name: {@link Manager#OPTION_SIGNAL_SOAP_ACTION} <br>
     * default value: {@link Configuration#DEFAULT_SIGNAL_SOAP_ACTION} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_SIGNAL_SOAP_ACTION}
    public static final String KEY_SIGNAL_SOAP_ACTION = "signalSoapAction"; //$NON-NLS-1$
    
    /**
     * The connect timeout in milliseconds to a third-party SOAP service to be
     * signaled immediately before and after a change (firmware or
     * domain) is deployed to a device. The signaler is helpful in graceful
     * shutdown of the device from a load balancer. If this value is "0" then
     * the usual network timeout will apply.<br>
     * matching option key name: {@link Manager#OPTION_SIGNAL_CONNECT_TIMEOUT}
     * <br>
     * default value: {@link Configuration#DEFAULT_SIGNAL_CONNECT_TIMEOUT} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_SIGNAL_CONNECT_TIMEOUT}
    public static final String KEY_SIGNAL_CONNECT_TIMEOUT = "signalConnectTimeout"; //$NON-NLS-1$
    
    /**
     * The timeout in milliseconds to wait for a response from a third-party
     * SOAP service to be signaled immediately before and after a change
     * (firmware or domain) is deployed to a device. The signaler is
     * helpful in graceful shutdown of the device from a load balancer.<br>
     * matching option key name: {@link Manager#OPTION_SIGNAL_RESPONSE_TIMEOUT}
     * <br>
     * default value: {@link Configuration#DEFAULT_SIGNAL_RESPONSE_TIMEOUT} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_SIGNAL_RESPONSE_TIMEOUT}
    public static final String KEY_SIGNAL_RESPONSE_TIMEOUT = "signalResponseTimeout"; //$NON-NLS-1$
    
    /**
     * The time in milliseconds to sleep after signaling a third-party SOAP
     * service before and after a change (firmware or domain) is
     * deployed to a device. The signaler is helpful in graceful shutdown of the
     * device from a load balancer. This delay time will be helpful in
     * performing actions after the third-party SOAP service has received the
     * signal. <br>
     * matching option key name: {@link Manager#OPTION_SIGNAL_DELAY_TIME} <br>
     * default value: {@link Configuration#DEFAULT_SIGNAL_DELAY_TIME} <br>
     * 
     */
//  * visibility: {@link Configuration#VISIBILITY_SIGNAL_DELAY_TIME}
    public static final String KEY_SIGNAL_DELAY_TIME = "signalDelayTime"; //$NON-NLS-1$

    /**
     * The filename of the key store for the manager to use in it's truststore.
     * The Manager will load all the certificates from the keystore to use as 
     * trusted certificates. <br>
     * matching option key name: {@link Manager#OPTION_TRUSTSTORE_FILENAME} <br>
     * default value: {@link Configuration#DEFAULT_TRUSTSTORE_FILENAME} <br>
     */
//  * visibility: {@link Configuration#VISIBILITY_TRUSTSTORE_FILENAME}
    public static final String KEY_TRUSTSTORE_FILENAME = "truststoreFilename"; //$NON-NLS-1$

    /**
     * The password for the key store specified in {@link Configuration#KEY_TRUSTSTORE_FILENAME} <br>
     * matching option key name: {@link Manager#OPTION_TRUSTSTORE_PASSWORD} <br>
     * default value: {@link Configuration#DEFAULT_TRUSTSTORE_PASSWORD} <br>
     */
//  * visibility: {@link Configuration#VISIBILITY_TRUSTSTORE_PASSWORD}
    public static final String KEY_TRUSTSTORE_PASSWORD = "truststorePassword"; //$NON-NLS-1$
    
    // default Item values for each of the above keys
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HEARTBEAT_THREAD_POOL_SIZE} 
     */
    public static final String DEFAULT_HEARTBEAT_THREAD_POOL_SIZE = "5";                                                                //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HEARTBEAT_INTERVAL} 
     */
    public static final String DEFAULT_HEARTBEAT_INTERVAL = "300000";                                                                   //$NON-NLS-1$    
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_AMP_CONNECT_TIMEOUT} 
     */
    public static final String DEFAULT_AMP_CONNECT_TIMEOUT = "10000";                                                                   //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE}. Default is 5
     */
    public static final String DEFAULT_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE = "5";   //5                                                             //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_INTERVAL} units are in milliseconds. Default is 600000 milliseconds
     */
    public static final String DEFAULT_DOMAIN_SYNCHRONIZATION_INTERVAL = "600000";   

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_RETRY_MAX}. Default is 2. 
     */
    public static final String DEFAULT_DOMAIN_SYNCHRONIZATION_RETRY_MAX = "3";   
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COMMANDS_IMPL} 
     */
    public static final String DEFAULT_COMMANDS_IMPL = "com.ibm.datapower.amt.amp.defaultProvider.CommandsImpl";                        //$NON-NLS-1$
 
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_MESSAGE_PREFIX} 
     */
    public static final String DEFAULT_MESSAGE_PREFIX = "WAMT";                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COMMANDS_V2_IMPL} 
     */
    public static final String DEFAULT_COMMANDS_V2_IMPL = "com.ibm.datapower.amt.amp.defaultV2Provider.CommandsImpl";                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COMMANDS_V3_IMPL} 
     */
    public static final String DEFAULT_COMMANDS_V3_IMPL = "com.ibm.datapower.amt.amp.defaultV3Provider.CommandsImpl";                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COMMANDS_SOMA_IMPL} 
     */
    public static final String DEFAULT_COMMANDS_SOMA_IMPL = "com.ibm.datapower.amt.soma.defaultProvider.SOMACommandsImpl";                        //$NON-NLS-1$

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COMMANDS_XC10_IMPL} 
     */
    public static final String DEFAULT_COMMANDS_XC10_IMPL = "com.ibm.datapower.amt.xc10.defaultProvider.CommandsImpl";                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SOAP_HELPER_IMPL} 
     */
    public static final String DEFAULT_SOAP_HELPER_IMPL = "com.ibm.datapower.amt.amp.defaultProvider.SOAPHelperImpl";                   //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SOAP_HELPER_V2_IMPL} 
     */
    public static final String DEFAULT_SOAP_HELPER_V2_IMPL = "com.ibm.datapower.amt.amp.defaultV2Provider.SOAPHelperImpl";                   //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SOAP_HELPER_V3_IMPL} 
     */
    public static final String DEFAULT_SOAP_HELPER_V3_IMPL = "com.ibm.datapower.amt.amp.defaultV3Provider.SOAPHelperImpl";                   //$NON-NLS-1$    
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SOAP_HELPER_SOMA_IMPL} 
     */
    public static final String DEFAULT_SOAP_HELPER_SOMA_IMPL = "com.ibm.datapower.amt.soma.defaultProvider.SOAPHelperImpl";                   //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_REPOSITORY_IMPL} 
     */
    public static final String DEFAULT_REPOSITORY_IMPL = "com.ibm.datapower.amt.dataAPI.local.filesystem.RepositoryImpl";                   //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IMPL} 
     */
    public static final String DEFAULT_NOTIFICATION_CATCHER_IMPL = "com.ibm.datapower.amt.amp.defaultProvider.NotificationCatcherImpl"; //$NON-NLS-1$ 
        
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_PORT} 
     */
    public static final String DEFAULT_NOTIFICATION_CATCHER_IP_PORT = "5555";                                                           //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_ADDRESS}
     */
    public static final String DEFAULT_NOTIFICATION_CATCHER_IP_ADDRESS = null;                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_INTERFACE} 
     */
    public static final String DEFAULT_NOTIFICATION_CATCHER_IP_INTERFACE = null;                                                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_NO_SSL}
     */
    public static final String DEFAULT_NOTIFICATION_CATCHER_NO_SSL = "false";                                                           //$NON-NLS-1$

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_ADDRESS} 
     */
    public static final String DEFAULT_HTTP_LISTENER_IP_PORT = "5556";                                                           //$NON-NLS-1$

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_SOCKET_TIMEOUT} 
     */
    public static final String DEFAULT_HTTP_LISTENER_SOCKET_TIMEOUT = "20";                                                           //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_INTERFACE}
     */
    public static final String DEFAULT_HTTP_LISTENER_IP_ADDRESS = null;                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_PORT} 
     */
    public static final String DEFAULT_HTTP_LISTENER_IP_INTERFACE = null;                                                        //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DEVICE_LOG_PORT} 
     */
    public static final String DEFAULT_DEVICE_LOG_PORT = null;                                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_TASK_QUEUE_SIZE} 
     */
    public static final String DEFAULT_TASK_QUEUE_SIZE = null;                                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COLLECT_DAEMON_PROGRESSES} 
     */
    public static final String DEFAULT_COLLECT_DAEMON_PROGRESSES = "false";                                                             //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES} 
     */
    public static final String DEFAULT_COLLECT_DOMAIN_SYNCH_PROGRESSES = "false";                                                             //$NON-NLS-1$
        
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_HEARTBEAT_DISABLE} 
     */
    public static final String DEFAULT_HEARTBEAT_DISABLE = "false";                                                                     //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_QUIESCE_TIMEOUT} 
     */
    public static final String DEFAULT_DOMAIN_QUIESCE_TIMEOUT = "60"; //seconds                                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_QUIESCE_TIMEOUT} 
     */
    public static final String DEFAULT_DEVICE_QUIESCE_TIMEOUT = "60"; //seconds                                                                          //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_DISABLE} 
     */
    //public static final String DEFAULT_DOMAIN_SYNCHRONIZATION_DISABLE = "false";     
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_CAPTURE_VERSION_BLOB} 
     */
    public static final String DEFAULT_CAPTURE_VERSION_BLOB = "false"; //$NON-NLS-1$

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SIGNAL_URL} 
     */
    public static final String DEFAULT_SIGNAL_URL = null;
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SIGNAL_SOAP_ACTION} 
     */
    public static final String DEFAULT_SIGNAL_SOAP_ACTION = null;
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SIGNAL_CONNECT_TIMEOUT} 
     */
    public static final String DEFAULT_SIGNAL_CONNECT_TIMEOUT = "10000"; //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SIGNAL_RESPONSE_TIMEOUT} 
     */
    public static final String DEFAULT_SIGNAL_RESPONSE_TIMEOUT = "10000"; //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_SIGNAL_DELAY_TIME} 
     */
    public static final String DEFAULT_SIGNAL_DELAY_TIME = "1000"; //$NON-NLS-1$
    
    /**
     * Default value for the configuration item specified by
     * {@link #KEY_TRUSTSTORE_FILENAME} 
     */
    public static final String DEFAULT_TRUSTSTORE_FILENAME = null; //$NON-NLS-1$

    /**
     * Default value for the configuration item specified by
     * {@link #KEY_TRUSTSTORE_PASSWORD} 
     */
    public static final String DEFAULT_TRUSTSTORE_PASSWORD = null; //$NON-NLS-1$
    
    /**
     * Prefix to be added to the message numbers logged by the manager
     * {@link com.ibm.datapower.amt.amp.Commands} interface. <br>
     * matching option key name: {@link Manager#OPTION_MESSAGE_PREFIX} <br>
     * default value: {@link Configuration#DEFAULT_MESSAGE_PREFIX} <br>
     *      
     */
    public static final String KEY_MESSAGE_PREFIX = "messagesPrefix"; //$NON-NLS-1$
    
    
    // visibility of each of the above keys
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HEARTBEAT_THREAD_POOL_SIZE}.
     */
    private static final boolean VISIBILITY_HEARTBEAT_THREAD_POOL_SIZE = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HEARTBEAT_INTERVAL}.
     */
    private static final boolean VISIBILITY_HEARTBEAT_INTERVAL = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_AMP_CONNECT_TIMEOUT}.
     */
    private static final boolean VISIBILITY_AMP_CONNECT_TIMEOUT = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE}.
     */
    private static final boolean VISIBILITY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_INTERVAL}.
     */
    private static final boolean VISIBILITY_DOMAIN_SYNCHRONIZATION_INTERVAL = true;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_RETRY_MAX}.
     */
    private static final boolean VISIBILITY_DOMAIN_SYNCHRONIZATION_RETRY_MAX = true;
    
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COMMANDS_IMPL}.
     */
    private static final boolean VISIBILITY_COMMANDS_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SOAP_HELPER_IMPL}.
     */
    private static final boolean VISIBILITY_SOAP_HELPER_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COMMANDS_V2_IMPL}.
     */
    private static final boolean VISIBILITY_COMMANDS_V2_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COMMANDS_V3_IMPL}.
     */
    private static final boolean VISIBILITY_COMMANDS_V3_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COMMANDS_SOMA_IMPL}.
     */
    private static final boolean VISIBILITY_COMMANDS_SOMA_IMPL = false;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COMMANDS_XC10_IMPL}.
     */
    private static final boolean VISIBILITY_COMMANDS_XC10_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SOAP_HELPER_V2_IMPL}.
     */
    private static final boolean VISIBILITY_SOAP_HELPER_V2_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SOAP_HELPER_V3_IMPL}.
     */
    private static final boolean VISIBILITY_SOAP_HELPER_V3_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SOAP_HELPER_SOMA_IMPL}.
     */
    private static final boolean VISIBILITY_SOAP_HELPER_SOMA_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_REPOSITORY_IMPL}.
     */
    private static final boolean VISIBILITY_REPOSITORY_IMPL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IMPL}.
     */
    private static final boolean VISIBILITY_NOTIFICATION_CATCHER_IMPL = false; 

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_PORT}.
     */
    private static final boolean VISIBILITY_NOTIFICATION_CATCHER_IP_PORT = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_ADDRESS}.
     */
    private static final boolean VISIBILITY_NOTIFICATION_CATCHER_IP_ADDRESS = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_IP_INTERFACE}.
     */
    private static final boolean VISIBILITY_NOTIFICATION_CATCHER_IP_INTERFACE = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_NOTIFICATION_CATCHER_NO_SSL}.
     */
    private static final boolean VISIBILITY_NOTIFICATION_CATCHER_NO_SSL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_ADDRESS}.
     */
    private static final boolean VISIBILITY_HTTP_LISTENER_IP_ADDRESS = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_INTERFACE}.
     */
    private static final boolean VISIBILITY_HTTP_LISTENER_IP_INTERFACE = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_IP_PORT}.
     */
    private static final boolean VISIBILITY_HTTP_LISTENER_IP_PORT = true;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HTTP_LISTENER_SOCKET_TIMEOUT}.
     */
    private static final boolean VISIBILITY_HTTP_LISTENER_SOCKET_TIMEOUT = true;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DEVICE_LOG_PORT}.
     */
    private static final boolean VISIBILITY_DEVICE_LOG_PORT = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_TASK_QUEUE_SIZE}.
     */
    private static final boolean VISIBILITY_TASK_QUEUE_SIZE = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COLLECT_DAEMON_PROGRESSES}.
     */
    private static final boolean VISIBILITY_COLLECT_DAEMON_PROGRESSES = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES}.
     */
    private static final boolean VISIBILITY_COLLECT_DOMAIN_SYNCH_PROGRESSES = false;    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_HEARTBEAT_DISABLE}.
     */
    private static final boolean VISIBILITY_HEARTBEAT_DISABLE = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_QUIESCE_TIMEOUT}.
     */
    private static final boolean VISIBILITY_DOMAIN_QUIESCE_TIMEOUT = false;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_QUIESCE_TIMEOUT}.
     */
    private static final boolean VISIBILITY_DEVICE_QUIESCE_TIMEOUT = false;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_DOMAIN_SYNCHRONIZATION_DISABLE}.
     */
    //public static final boolean VISIBILITY_DOMAIN_SYNCHRONIZATION_DISABLE = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_CAPTURE_VERSION_BLOB}.
     */
    private static final boolean VISIBILITY_CAPTURE_VERSION_BLOB = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SIGNAL_URL}.
     */
    private static final boolean VISIBILITY_SIGNAL_URL = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SIGNAL_SOAP_ACTION}.
     */
    private static final boolean VISIBILITY_SIGNAL_SOAP_ACTION = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SIGNAL_CONNECT_TIMEOUT}.
     */
    private static final boolean VISIBILITY_SIGNAL_CONNECT_TIMEOUT = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SIGNAL_RESPONSE_TIMEOUT}.
     */
    private static final boolean VISIBILITY_SIGNAL_RESPONSE_TIMEOUT = false;
    
    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_SIGNAL_DELAY_TIME}.
     */
    private static final boolean VISIBILITY_SIGNAL_DELAY_TIME = false;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_TRUSTSTORE_FILENAME}.
     */
    private static final boolean VISIBILITY_TRUSTSTORE_FILENAME = true;

    /**
     * Customer visibility of the configuration item specified by
     * {@link #KEY_TRUSTSTORE_PASSWORD}.
     */
    private static final boolean VISIBILITY_TRUSTSTORE_PASSWORD = true;

    private Configuration() {
        // don't call the constructor, everything is static
    }
    
    static {
        /*
         * Invoking any method in this class should trigger this static block to
         * be executed. This block will first create the mapping of all the
         * names (canonical/property name, option name), and then it will
         * load the list of properties values. The list of Manager map values
         * will get loaded later, as the Manager is instantiated (see
         * Manager.getInstance()).
         */
        final String METHOD_NAME = "static"; //$NON-NLS-1$
        
        itemCollection = new ItemCollection();

        // visible items
        
        addNewItem(Configuration.KEY_HEARTBEAT_THREAD_POOL_SIZE, 
                Manager.OPTION_HEARTBEAT_THREAD_POOL_SIZE, 
                Configuration.DEFAULT_HEARTBEAT_THREAD_POOL_SIZE,
                Configuration.VISIBILITY_HEARTBEAT_THREAD_POOL_SIZE);
        
        addNewItem(Configuration.KEY_HEARTBEAT_INTERVAL,
                Manager.OPTION_HEARTBEAT_INTERVAL,
                Configuration.DEFAULT_HEARTBEAT_INTERVAL,
                Configuration.VISIBILITY_HEARTBEAT_INTERVAL);
        
        addNewItem(Configuration.KEY_AMP_CONNECT_TIMEOUT,
                Manager.OPTION_AMP_CONNECT_TIMEOUT,
                Configuration.DEFAULT_AMP_CONNECT_TIMEOUT,
                Configuration.VISIBILITY_AMP_CONNECT_TIMEOUT);
        
        addNewItem(Configuration.KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE, 
                Manager.OPTION_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE, 
                Configuration.DEFAULT_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE,
                Configuration.VISIBILITY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE);
        
        addNewItem(Configuration.KEY_DOMAIN_SYNCHRONIZATION_INTERVAL,
                Manager.OPTION_DOMAIN_SYNCHRONIZATION_INTERVAL,
                Configuration.DEFAULT_DOMAIN_SYNCHRONIZATION_INTERVAL,
                Configuration.VISIBILITY_DOMAIN_SYNCHRONIZATION_INTERVAL);

        addNewItem(Configuration.KEY_DOMAIN_SYNCHRONIZATION_RETRY_MAX,
                Manager.OPTION_DOMAIN_SYNCHRONIZATION_RETRY_MAX,
                Configuration.DEFAULT_DOMAIN_SYNCHRONIZATION_RETRY_MAX,
                Configuration.VISIBILITY_DOMAIN_SYNCHRONIZATION_RETRY_MAX);
        
        addNewItem(Configuration.KEY_HTTP_LISTENER_IP_PORT,
                Manager.OPTION_HTTP_LISTENER_IP_PORT,
                Configuration.DEFAULT_HTTP_LISTENER_IP_PORT,
                Configuration.VISIBILITY_HTTP_LISTENER_IP_PORT);
        
        addNewItem(Configuration.KEY_HTTP_LISTENER_IP_INTERFACE,
                Manager.OPTION_HTTP_LISTENER_IP_INTERFACE,
                Configuration.DEFAULT_HTTP_LISTENER_IP_INTERFACE,
                Configuration.VISIBILITY_HTTP_LISTENER_IP_INTERFACE);
        
        addNewItem(Configuration.KEY_HTTP_LISTENER_IP_ADDRESS,
                Manager.OPTION_HTTP_LISTENER_IP_ADDRESS,
                Configuration.DEFAULT_HTTP_LISTENER_IP_ADDRESS,
                Configuration.VISIBILITY_HTTP_LISTENER_IP_ADDRESS);
        
        addNewItem(Configuration.KEY_HTTP_LISTENER_SOCKET_TIMEOUT,
                Manager.OPTION_HTTP_LISTENER_SOCKET_TIMEOUT,
                Configuration.DEFAULT_HTTP_LISTENER_SOCKET_TIMEOUT,
                Configuration.VISIBILITY_HTTP_LISTENER_SOCKET_TIMEOUT);
        
        addNewItem(Configuration.KEY_NOTIFICATION_CATCHER_IP_PORT,
                Manager.OPTION_NOTIFICATION_CATCHER_PORT,
                Configuration.DEFAULT_NOTIFICATION_CATCHER_IP_PORT,
                Configuration.VISIBILITY_NOTIFICATION_CATCHER_IP_PORT);
        
        addNewItem(Configuration.KEY_NOTIFICATION_CATCHER_IP_ADDRESS,
                Manager.OPTION_NOTIFICATION_CATCHER_IP_ADDRESS,
                Configuration.DEFAULT_NOTIFICATION_CATCHER_IP_ADDRESS,
                Configuration.VISIBILITY_NOTIFICATION_CATCHER_IP_ADDRESS);
        
        addNewItem(Configuration.KEY_NOTIFICATION_CATCHER_IP_INTERFACE,
                Manager.OPTION_NOTIFICATION_CATCHER_IP_INTERFACE,
                Configuration.DEFAULT_NOTIFICATION_CATCHER_IP_INTERFACE,
                Configuration.VISIBILITY_NOTIFICATION_CATCHER_IP_INTERFACE);

        // hidden items (don't appear in generated properties file)

        // AMPv1
        addNewItem(Configuration.KEY_COMMANDS_IMPL,
                Manager.OPTION_COMMANDS_IMPL,
                Configuration.DEFAULT_COMMANDS_IMPL,
                Configuration.VISIBILITY_COMMANDS_IMPL);
        
        addNewItem(Configuration.KEY_SOAP_HELPER_IMPL,
                Manager.OPTION_SOAPHELPER_IMPL,
                Configuration.DEFAULT_SOAP_HELPER_IMPL,
                Configuration.VISIBILITY_SOAP_HELPER_IMPL);
        
        // AMPv2
        addNewItem(Configuration.KEY_COMMANDS_V2_IMPL,
                Manager.OPTION_COMMANDS_V2_IMPL,
                Configuration.DEFAULT_COMMANDS_V2_IMPL,
                Configuration.VISIBILITY_COMMANDS_V2_IMPL);
        
        addNewItem(Configuration.KEY_SOAP_HELPER_V2_IMPL,
                Manager.OPTION_SOAPHELPER_V2_IMPL,
                Configuration.DEFAULT_SOAP_HELPER_V2_IMPL,
                Configuration.VISIBILITY_SOAP_HELPER_V2_IMPL);
        // AMPv3
        addNewItem(Configuration.KEY_COMMANDS_V3_IMPL,
                Manager.OPTION_COMMANDS_V3_IMPL,
                Configuration.DEFAULT_COMMANDS_V3_IMPL,
                Configuration.VISIBILITY_COMMANDS_V3_IMPL);
        
        addNewItem(Configuration.KEY_SOAP_HELPER_V3_IMPL,
                Manager.OPTION_SOAPHELPER_V3_IMPL,
                Configuration.DEFAULT_SOAP_HELPER_V3_IMPL,
                Configuration.VISIBILITY_SOAP_HELPER_V3_IMPL);
        
        // SOMA
        addNewItem(Configuration.KEY_COMMANDS_SOMA_IMPL,
                Manager.OPTION_COMMANDS_SOMA_IMPL,
                Configuration.DEFAULT_COMMANDS_SOMA_IMPL,
                Configuration.VISIBILITY_COMMANDS_SOMA_IMPL);
        
        addNewItem(Configuration.KEY_SOAP_HELPER_SOMA_IMPL,
                Manager.OPTION_SOAPHELPER_SOMA_IMPL,
                Configuration.DEFAULT_SOAP_HELPER_SOMA_IMPL,
                Configuration.VISIBILITY_SOAP_HELPER_SOMA_IMPL);
        
        // XC10
        addNewItem(Configuration.KEY_COMMANDS_XC10_IMPL,
                Manager.OPTION_COMMANDS_XC10_IMPL,
                Configuration.DEFAULT_COMMANDS_XC10_IMPL,
                Configuration.VISIBILITY_COMMANDS_XC10_IMPL);
        
        // Repository
        addNewItem(Configuration.KEY_REPOSITORY_IMPL,
                Manager.OPTION_REPOSITORY_IMPL,
                Configuration.DEFAULT_REPOSITORY_IMPL,
                Configuration.VISIBILITY_REPOSITORY_IMPL);
        
        addNewItem(Configuration.KEY_NOTIFICATION_CATCHER_IMPL,
                Manager.OPTION_NOTIFICATION_CATCHER_IMPL,
                Configuration.DEFAULT_NOTIFICATION_CATCHER_IMPL,
                Configuration.VISIBILITY_NOTIFICATION_CATCHER_IMPL);
        
        addNewItem(Configuration.KEY_NOTIFICATION_CATCHER_NO_SSL,
                Manager.OPTION_NOTIFICATION_CATCHER_NO_SSL,
                Configuration.DEFAULT_NOTIFICATION_CATCHER_NO_SSL,
                Configuration.VISIBILITY_NOTIFICATION_CATCHER_NO_SSL);
        
        addNewItem(Configuration.KEY_TASK_QUEUE_SIZE,
                Manager.OPTION_TASK_QUEUE_SIZE,
                Configuration.DEFAULT_TASK_QUEUE_SIZE,
                Configuration.VISIBILITY_TASK_QUEUE_SIZE);
        
        addNewItem(Configuration.KEY_DEVICE_LOG_PORT,
                Manager.OPTION_DEVICE_LOG_PORT,
                Configuration.DEFAULT_DEVICE_LOG_PORT,
                Configuration.VISIBILITY_DEVICE_LOG_PORT);
        
        addNewItem(Configuration.KEY_COLLECT_DOMAIN_SYNCH_PROGRESSES,
                Manager.OPTION_COLLECT_DOMAIN_SYNCH_PROGRESSES,
                Configuration.DEFAULT_COLLECT_DOMAIN_SYNCH_PROGRESSES,
                Configuration.VISIBILITY_COLLECT_DOMAIN_SYNCH_PROGRESSES);
        
        addNewItem(Configuration.KEY_COLLECT_DAEMON_PROGRESSES,
                Manager.OPTION_COLLECT_DAEMON_PROGRESSES,
                Configuration.DEFAULT_COLLECT_DAEMON_PROGRESSES,
                Configuration.VISIBILITY_COLLECT_DAEMON_PROGRESSES);        
        
        addNewItem(Configuration.KEY_HEARTBEAT_DISABLE,
                Manager.OPTION_DEBUG_DISABLE_HEARTBEAT,
                Configuration.DEFAULT_HEARTBEAT_DISABLE,
                Configuration.VISIBILITY_HEARTBEAT_DISABLE);
        
        addNewItem(Configuration.KEY_DOMAIN_QUIESCE_TIMEOUT,
                Manager.OPTION_DEBUG_DOMAIN_QUIESCE_TIMEOUT,
                Configuration.DEFAULT_DOMAIN_QUIESCE_TIMEOUT,
                Configuration.VISIBILITY_DOMAIN_QUIESCE_TIMEOUT);        
        
        addNewItem(Configuration.KEY_DEVICE_QUIESCE_TIMEOUT,
                Manager.OPTION_DEBUG_DEVICE_QUIESCE_TIMEOUT,
                Configuration.DEFAULT_DEVICE_QUIESCE_TIMEOUT,
                Configuration.VISIBILITY_DEVICE_QUIESCE_TIMEOUT);        
        
/*        addNewItem(//Configuration.KEY_DOMAIN_SYNCHRONIZATION_DISABLE,
                //Manager.OPTION_DEBUG_DISABLE_DOMAIN_SYNCHRONIZATION,
                Configuration.DEFAULT_DOMAIN_SYNCHRONIZATION_DISABLE,
                Configuration.VISIBILITY_DOMAIN_SYNCHRONIZATION_DISABLE);*/        
        
        addNewItem(Configuration.KEY_CAPTURE_VERSION_BLOB,
                Manager.OPTION_DEBUG_CAPTURE_VERSION_BLOB,
                Configuration.DEFAULT_CAPTURE_VERSION_BLOB,
                Configuration.VISIBILITY_CAPTURE_VERSION_BLOB);

        addNewItem(Configuration.KEY_SIGNAL_URL,
                Manager.OPTION_SIGNAL_URL,
                Configuration.DEFAULT_SIGNAL_URL,
                Configuration.VISIBILITY_SIGNAL_URL);

        addNewItem(Configuration.KEY_SIGNAL_SOAP_ACTION,
                Manager.OPTION_SIGNAL_SOAP_ACTION,
                Configuration.DEFAULT_SIGNAL_SOAP_ACTION,
                Configuration.VISIBILITY_SIGNAL_SOAP_ACTION);

        addNewItem(Configuration.KEY_SIGNAL_CONNECT_TIMEOUT,
                Manager.OPTION_SIGNAL_CONNECT_TIMEOUT,
                Configuration.DEFAULT_SIGNAL_CONNECT_TIMEOUT,
                Configuration.VISIBILITY_SIGNAL_CONNECT_TIMEOUT);

        addNewItem(Configuration.KEY_SIGNAL_RESPONSE_TIMEOUT,
                Manager.OPTION_SIGNAL_RESPONSE_TIMEOUT,
                Configuration.DEFAULT_SIGNAL_RESPONSE_TIMEOUT,
                Configuration.VISIBILITY_SIGNAL_RESPONSE_TIMEOUT);

        addNewItem(Configuration.KEY_SIGNAL_DELAY_TIME,
                Manager.OPTION_SIGNAL_DELAY_TIME,
                Configuration.DEFAULT_SIGNAL_DELAY_TIME,
                Configuration.VISIBILITY_SIGNAL_DELAY_TIME);

        addNewItem(Configuration.KEY_TRUSTSTORE_FILENAME, 
                Manager.OPTION_TRUSTSTORE_FILENAME, 
                Configuration.DEFAULT_TRUSTSTORE_FILENAME,
                Configuration.VISIBILITY_TRUSTSTORE_FILENAME);

        addNewItem(Configuration.KEY_TRUSTSTORE_PASSWORD, 
                Manager.OPTION_TRUSTSTORE_PASSWORD, 
                Configuration.DEFAULT_TRUSTSTORE_PASSWORD,
                Configuration.VISIBILITY_TRUSTSTORE_PASSWORD);

        // Credential is not included because it is not a String and
        // we don't want anyone else reading it

        // determine the value for rootDirectory and isRootDirectoryEnvVarSet
        rootDirectory = System.getProperty(ENV_VAR_NAME);
        if (rootDirectory == null) {
            isRootDirectoryEnvVarSet = false;
            File currentDir = new File(""); //$NON-NLS-1$
            rootDirectory = currentDir.getAbsolutePath();
            String msg = "Environment variable " + ENV_VAR_NAME + //$NON-NLS-1$
                " not set, using " + rootDirectory;  //$NON-NLS-1$
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, msg);
            // under WAS we only use the env variable to set properties for testing
        } else {
            isRootDirectoryEnvVarSet = true;
        }
        // make sure a trailing separator (ie, "/") was specified on rootDir
        if (rootDirectory.endsWith(File.separator)) {
            // go with it
        } else {
            rootDirectory = rootDirectory + File.separator;
        }

        // load the properties file and merge the data into our ItemCollection
        loadProperties();
        
        // load the Manager options later, the Manager should give us that
    }
    
    static private void addNewItem(String canonicalKey, String optionKey, 
            String defaultValue, boolean hidden) {
        Item item = new Item(canonicalKey, optionKey, 
                null, null, defaultValue, hidden);
        itemCollection.add(item);
    }
    
    private static void loadProperties() {
        final String METHOD_NAME = "loadProperties"; //$NON-NLS-1$
        if (!isPropertiesLoaded) {
            Properties properties = new Properties();
            File propertiesFile = new File(rootDirectory + PROPERTIES_FILE_RELATIVE_NAME);
            // load from it if it exists
            if (propertiesFile.exists()) { 
            	InputStream inputStream = null;
                try {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "found existing properties file");  //$NON-NLS-1$
                    inputStream = new FileInputStream(propertiesFile);
                    properties.load(inputStream);                    
                } catch (IOException e) {
                    logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, 
                            Messages.getString("wamt.ConfigHelper.notOpenProp",  //$NON-NLS-1$ 
                                    propertiesFile.getAbsolutePath()), e);
                } finally {
                	try {
						inputStream.close();
					} catch (IOException e) {
						logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, 
	                            Messages.getString("wamt.ConfigHelper.notOpenProp",  //$NON-NLS-1$ 
	                                    propertiesFile.getAbsolutePath()), e);
					}
                }
            } else {
                // create it if it doesn't exist
                if (isRootDirectoryEnvVarSet) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "creating default properties file"); //$NON-NLS-1$
                    // only if environment variable was set...
                    // prevents creating the file under WAS
                    createPropertiesFile(propertiesFile);
                } else {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "no properties file found, not creating one"); //$NON-NLS-1$
                }
            }

            // now that properties are loaded/created, merge the values into our maps
            Set propertyKeys = properties.keySet();
            Iterator iterator = propertyKeys.iterator();
            while (iterator.hasNext()) {
                String propertyKey = (String) iterator.next();
                String value = properties.getProperty(propertyKey);
                if (value == null) {
                    // don't bother with null values
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                            "skipping null value for property " + propertyKey); //$NON-NLS-1$
                    continue;
                }
                // now set the propertyValue
                Item item = itemCollection.getByCanonical(propertyKey);
                if (item != null) {
                    // since it's a properties file, remove leading and trailing space
                    String cleanValue = value.trim();
                    item.propertyValue = cleanValue;
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                            "loading from property " + propertyKey + //$NON-NLS-1$
                            " with value " + value); //$NON-NLS-1$


                    // Put all config value checks and corrections here.
                    
                    // The quiesce timeout can be zero, but if it is not zero, and less than 60
                    // ten the value must be changed to 60 because the dvice will not respect a 
                    // value less than 60.
                    if (propertyKey.equals(KEY_DOMAIN_QUIESCE_TIMEOUT)) {
                        int timeout = Configuration.getAsInteger(Configuration.KEY_DOMAIN_QUIESCE_TIMEOUT).intValue();
                        if (timeout > 0 && timeout < 60) {
                            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                                    "property " + propertyKey + //$NON-NLS-1$
                                    " nonzero and less than 60, setting to 60"); //$NON-NLS-1$

                            item.propertyValue = "60"; //$NON-NLS-1$
                        }
                    }

                    if (propertyKey.equals(KEY_DEVICE_QUIESCE_TIMEOUT)) {
                        int timeout = Configuration.getAsInteger(Configuration.KEY_DEVICE_QUIESCE_TIMEOUT).intValue();
                        if (timeout > 0 && timeout < 60) {
                            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                                    "property " + propertyKey + //$NON-NLS-1$
                                    " nonzero and less than 60, setting to 60"); //$NON-NLS-1$

                            item.propertyValue = "60"; //$NON-NLS-1$
                        }
                    }
                } else {
                    // skip if not a known property name
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                            "skipping unknown property name " + propertyKey); //$NON-NLS-1$
                }
            }
            
            isPropertiesLoaded = true;
        }
    }
    
    private static void createPropertiesFile(File file) {
        final String METHOD_NAME = "createPropertiesFile";  //$NON-NLS-1$ 
        // write out the properties file
        String comment = "# "; //$NON-NLS-1$
        String delimiter = " = "; //$NON-NLS-1$
        String lineSeparator = System.getProperty("line.separator");  //$NON-NLS-1$
        if ((lineSeparator == null) || (lineSeparator.length() == 0)) {
            lineSeparator = "\n"; //$NON-NLS-1$
        }
        OutputStream outputStream = null;
        try { 
            outputStream = new FileOutputStream(file);

            // add a header comment
            outputStream.write(comment.getBytes()); outputStream.flush();
            outputStream.write(comment.getBytes());
            outputStream.write("properties file for the manager".getBytes());
            outputStream.write(lineSeparator.getBytes());
            
            Set propertyKeys = itemCollection.canonicalMap.keySet();
            Iterator iterator = propertyKeys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                if (!Configuration.isPropertyVisible(key)) {
                    // don't include the hidden properties
                    continue;
                }
                String value = Configuration.getPreferredByProperty(key);
                if (value == null) {
                    // properties without default values
                    outputStream.write(comment.getBytes());
                    outputStream.write(key.getBytes());
                    outputStream.write(delimiter.getBytes());
                    // there is no value, these default to null
                    outputStream.write(lineSeparator.getBytes());
                } else {
                    // properties with default values
                    outputStream.write(comment.getBytes());
                    outputStream.write(key.getBytes());
                    outputStream.write(delimiter.getBytes());
                    outputStream.write(value.getBytes());
                    outputStream.write(lineSeparator.getBytes());
                }
            }
        } catch (Exception e) {
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("wamt.ConfigHelper.notWriteProp",  //$NON-NLS-1$ 
                            file.getAbsolutePath()), e);
        } finally {
        	try {
				outputStream.close();
			} catch (IOException e) {
				logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, 
	                    Messages.getString("wamt.ConfigHelper.notWriteProp",  //$NON-NLS-1$ 
	                            file.getAbsolutePath()), e);			}
        }
    }

    /**
	 * Get the preferred value (property, then option, then default) from the
	 * configuration using the canonical name key. For example,
	 * <code>Configuration.getAsInteger(Configuration.KEY_TASK_QUEUE_SIZE)</code>.
	 * This method may be more helpful than {@link #get(String)} in places where
	 * you want an int or an Integer and don't want to do your own checking for
	 * a <code>NumberFormatException</code>. As long as there is a non-null
	 * and parseable default value for this item, and the key refers to a valid
	 * item, you can trust that this will not return null and thus you don't
	 * need to check for that.
	 * 
	 * @param canonicalKey
	 *            the canonical name of the configuration item. The canonical
	 *            names are the public String members of this class that start
	 *            with "KEY_", such as <code>KEY_COMMANDS_IMPL</code>.
	 * @return the preferred value which parses successfully into an Integer. If
	 *         a value, such as a property, is set to a non-parseable value
	 *         (i.e., the String "2q" from the properties file can not be
	 *         converted to an Integer), then this method will check the next
	 *         preferred value (i.e., see if the value from the Manager options
	 *         can be converted to an Integer). If both the properties value and
	 *         the option value are null or non-parseable, then it will convert
	 *         the default value to an Integer and return it. Even after all
	 *         this, the return value may still be null, if the default value is
	 *         null.
	 * @see #get(String)
	 * @see #set(String, String)
	 */
	static public Integer getAsInteger(String canonicalKey) {
	    final String METHOD_NAME = "getAsInteger"; //$NON-NLS-1$
	    Integer result = null;
	    Item item = itemCollection.getByCanonical(canonicalKey);
	    if (item != null) {
	        // put these values in the preferred order, we'll pick the 1st one that works
	        String[] values = {item.propertyValue, item.optionValue, item.defaultValue};
	        for (int i=0; (i<values.length) && (result==null); i++) {
	            if (values[i] != null) {
	                try {
	                    result = new Integer(values[i]);
	                } catch (NumberFormatException e) {
	                    result = null;
	                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
	                            "The configuration item named " + canonicalKey +       //$NON-NLS-1$
	                            " had String value '" + values[i] + "'" +              //$NON-NLS-1$  //$NON-NLS-2$
	                            " at level " + i +                                     //$NON-NLS-1$
	                            " and the attempt to parse it as an Integer failed."); //$NON-NLS-1$
	                }
	            }
	        }
	    } else {
	        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
	                "warning: request for non-existing item with key " + canonicalKey); //$NON-NLS-1$
	    }
	    return(result);
	}

	static void loadFromManagerMap(Map managerMap) {
        // this should be invoked only by the Manager as the first thing it does

        if (!isOptionsLoaded) {
            // walk through each of the Map entries to find the matching Item
            Set keys = managerMap.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (!(object instanceof String)) {
                    // hey, what are you giving me here?
                    continue;
                }
                String optionKey = (String) object;
                if (optionKey.equals(Manager.OPTION_CREDENTIAL)) {
                    // we don't want the credential to ever leave the Manager
                    continue;
                }
                object = managerMap.get(optionKey);
                if (object == null) {
                    // don't bother with null values
                    continue;
                }
                String value = null;
                if (object instanceof Credential) {
                    continue;
                } else if (object instanceof String) {
                    value = (String) object;
                } else {
                    value = object.toString();
                }
                // now set the optionValue
                Item item = itemCollection.getByOption(optionKey);
                if (item != null) {
                    // accept only trusted key names
                    item.optionValue = value;
                }
            }
            isOptionsLoaded = true;
        }
    }
    
    /**
     * Get the root directory for th manager. This value is defined in the System property
     * (environment variable) specified by {@link #ENV_VAR_NAME}. If that
     * System property has not been defined, then the value returned will be the
     * current directory and a log message at Level.FINE may be generated.
     * <p>
     * This method is for internal use only.
     * <p>
     * 
     * @return the root directory. If a trailing separator (ie, "/" on Linux) is
     *         not included in the value when it was specified, a trailing
     *         separator will be added.
     */
    public static String getRootDirectory() {
        return(rootDirectory);
    }

    /**
     * Get the preferred value (property, then option, then default) from the
     * configuration using the canonical name key. For example,
     * <code>Configuration.get(Configuration.KEY_COMMANDS_IMPL)</code>
     * 
     * @param canonicalKey
     *            the canonical name of the configuration item. The canonical
     *            names are the public String members of this class that start
     *            with "KEY_", such as <code>KEY_COMMANDS_IMPL</code>.
     * @return the preferred value. This may be null, if the default value is
     *         null and there is no specific property or option value. If this
     *         was originally a non-String object from the Manager options, it
     *         was converted into a String by invoking its
     *         <code>toString()</code> method.
     * @see #getAsInteger(String)
     * @see #set(String, String)
     */
    static public String get(String canonicalKey) {
        final String METHOD_NAME = "get"; //$NON-NLS-1$
        String result = null;
        Item item = itemCollection.getByCanonical(canonicalKey);
        if (item != null) {
            result = item.getPreferredValue();
        } else {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "warning: request for non-existing item with key " + canonicalKey); //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Set the option value for the configuration item with the specific
     * canonical key. Because this sets the option value, it can still be
     * overridden with a properties value. For example,
     * <code>Configuration.set(Configuration.KEY_TASK_QUEUE_SIZE, "9")</code>
     * 
     * @param canonicalKey
     *            the canonical name of the configuration item. The canonical
     *            names are the public String members of this class that start
     *            with "KEY_", such as <code>KEY_COMMANDS_IMPL</code>.
     * @param value
     *            the value for this configuration item.
     * @see #get(String)
     * @see #getAsInteger(String)
     */
    static private void set(String canonicalKey, String value) {
        final String METHOD_NAME = "set"; //$NON-NLS-1$
        Item item = itemCollection.getByCanonical(canonicalKey);
        if (item != null) {
            item.optionValue = value;
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "setting " + canonicalKey +     //$NON-NLS-1$
                    " option value to " + value);   //$NON-NLS-1$
        } else {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "warning: request to set non-existing item with key " + canonicalKey);  //$NON-NLS-1$
        }
    }
    
    /**
     * Get the preferred value (property, then option, then default) from the
     * configuration using the ConfigHelper property name key. For example,
     * <code>Configuration.getPreferredByProperty(Configuration.KEY_COMMANDS_IMPL)</code>
     * 
     * @param propertyKey
     *            the property name of the configuration item. The property
     *            names are the public String members of the
     *            Configuration.ConfigHelperLabels class.
     * @return the preferred value. This may be null, if the default value is
     *         null and there is no specific property or option value. If this
     *         was originally a non-String object from the Manager options, it
     *         was converted into a String by invoking its
     *         <code>toString()</code> method.
     */
    static private String getPreferredByProperty(String propertyKey) {
        String result = null;
        Item item = itemCollection.getByCanonical(propertyKey);
        if (item != null) {
            result = item.getPreferredValue();
        }
        return(result);
    }
    
    static private boolean isPropertyVisible(String propertyKey) {
        boolean result = true;
        Item item = itemCollection.getByCanonical(propertyKey);
        if (item != null) {
            result = item.isVisible;
        }
        return(result);
    }
    
    static private class Item {
        private String canonicalKey = null;
        private String optionKey = null; 
        private String propertyValue = null;
        private String optionValue = null;
        private String defaultValue = null;
        private boolean isVisible = false;
        
        Item(String canonicalKey, String optionKey,
                String propertyValue, String optionValue, String defaultValue,
                boolean isVisible) {
            this.canonicalKey = canonicalKey;
            this.optionKey = optionKey;
            this.propertyValue = propertyValue;
            this.optionValue = optionValue;
            this.defaultValue = defaultValue;
            this.isVisible = isVisible;
        }
        
        String getPreferredValue() {
            String result = null;
            if (this.propertyValue != null) {
                result = this.propertyValue;
            } else if (this.optionValue != null) {
                result = this.optionValue;
            } else {
                result = this.defaultValue;
            }
            return(result);
        }
        
        boolean getIsVisible() {
            return(this.isVisible);
        }
    }
    
    static private class ItemCollection {
        private Map canonicalMap = null;
        private Map optionMap = null;
        
        ItemCollection() {
            canonicalMap = new HashMap();
            optionMap = new HashMap();
        }
        
        void add(Item item) {
            this.canonicalMap.put(item.canonicalKey, item);
            this.optionMap.put(item.optionKey, item);
        }

        Item getByCanonical(String canonicalKey) {
            Item result = (Item) this.canonicalMap.get(canonicalKey);
            return(result);
        }
        
        Item getByOption(String optionKey) {
            Item result = (Item) this.optionMap.get(optionKey);
            return(result);
        }
        
    }
}
