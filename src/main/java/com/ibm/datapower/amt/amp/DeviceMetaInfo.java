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

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;

/**
 * This represents the data that would be returned from
 * {@link Commands#getDeviceMetaInfo(DeviceContext)}. Objects of this class
 * are automatically instantiated by that <code>getDeviceMetaInfo</code> method.
 * <p>
 *
 */
public class DeviceMetaInfo {
    private String deviceName = null;
    private String currentAMPVersion = null;
    private String serialNumber = null;
    private ModelType modelType = null;
    private String hardwareOptions = null;
    private int webGUIPort = 0;
    private DeviceType deviceType = null;
    private String firmwareLevel = null;
    private StringCollection featureLicenses = null;
    private int supportedCommands[] = null;
    private String secureBackup = null;
    // AMP port must be specified outside of this, since we use AMP to get this data

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    /**
     * Construct a new DeviceMetaInfo object. In general this should be invoked
     * only by {@link Commands#getDeviceMetaInfo(DeviceContext)}.
     * 
     * @param deviceName name of the device. This is not the hostname nor the IP
     *        address. It is the System Identifier stored in the device. For
     *        more information, refer to {@link #getDeviceName()}.
     * @param serialNumber serial number of the device
     * @param modelType version of the Datapower hardware (9002, 9003, etc).
     * @param guiPort the device's port that accepts WebGUI connections
     * @param deviceType the type of the device
     * @param featureLicenses the features that this device is licensed for
     */
    public DeviceMetaInfo(String deviceName, String serialNumber, String currentAMPVersion,
    				      ModelType modelType, 
                          String hardwareOptions, int guiPort, DeviceType deviceType,
                          String firmwareLevel, StringCollection featureLicenses) {
        this.deviceName = deviceName;
        this.serialNumber = serialNumber;
        this.currentAMPVersion = currentAMPVersion;
        this.modelType = modelType;
        this.hardwareOptions = hardwareOptions;
        this.webGUIPort = guiPort;
        this.deviceType = deviceType;
        this.firmwareLevel = firmwareLevel;
        this.featureLicenses = featureLicenses;
    }
    
    public DeviceMetaInfo(String deviceName, String serialNumber, String currentAMPVersion,
		      ModelType modelType, 
          String hardwareOptions, int guiPort, DeviceType deviceType,
          String firmwareLevel, StringCollection featureLicenses, 
          int supportedCommands[], String secureBackup ) {
		this.deviceName = deviceName;
		this.serialNumber = serialNumber;
		this.currentAMPVersion = currentAMPVersion;
		this.modelType = modelType;
		this.hardwareOptions = hardwareOptions;
		this.webGUIPort = guiPort;
		this.deviceType = deviceType;
		this.firmwareLevel = firmwareLevel;
		this.featureLicenses = featureLicenses;	
		if ( supportedCommands != null )
			this.supportedCommands = supportedCommands.clone();		
		this.secureBackup = secureBackup;
    }
    
    public DeviceMetaInfo(String deviceName, String serialNumber, String currentAMPVersion,
		      ModelType modelType, 
        String hardwareOptions, int guiPort, DeviceType deviceType,
        String firmwareLevel, StringCollection featureLicenses, 
        int supportedCommands[]) {
		this.deviceName = deviceName;
		this.serialNumber = serialNumber;
		this.currentAMPVersion = currentAMPVersion;
		this.modelType = modelType;
		this.hardwareOptions = hardwareOptions;
		this.webGUIPort = guiPort;
		this.deviceType = deviceType;
		this.firmwareLevel = firmwareLevel;
		this.featureLicenses = featureLicenses;
		if ( supportedCommands != null )
			this.supportedCommands = supportedCommands.clone();
  }
    
    /**
     * Get the serial number of this device.
     * 
     * @return the serial number of this device. This will be the device's
     *         hardware serial number, not the IP address. This can be treated
     *         as an opaque value. It is guaranteed to be unique. It will be
     *         used to correlate the source of notifications and to act as a
     *         primary key for each device.
     * @see Notification#getDeviceSerialNumber()
     */
    public String getSerialNumber() {
        return(this.serialNumber);
    }

    /**
     * Get the name of the device.
     * 
     * @return the name of the device. This is not the hostname nor the IP
     *         address. It is also independent from
     *         {@link com.ibm.datapower.amt.clientAPI.Device#getSymbolicName()}.
     *         It is the System Identifier in the System Settings, which is a
     *         symbolic name used for functions like SNMP and is stored on the
     *         device.
     */
    public String getDeviceName() {
        return(this.deviceName);
    }
    
    /**
     * Get the version of the Datapower hardware
     * 
     * @return the version of the Datapower hardware
     */
    public ModelType getModelType(){
        return(this.modelType);
    }
    
    /**
     * Get the options configured in the Datapower hardware
     * 
     * @return the options configured in the Datapower hardware
     */
    public String getHardwareOptions(){
        return(this.hardwareOptions);
    }
    
    /**
     * Get the port that accepts WebGUI connections.
     * If it is equal to -1, the WebGUI is not enabled on the device 
     * (or wasn't returned by AMP)
     * 
     * @return the port number that accepts WebGUI connections
     */
    public int getWebGUIPort() {
        return(this.webGUIPort);
    }

    /**
     * Get the type of the device.
     * 
     * @return the type of the device
     */
    public DeviceType getDeviceType() {
        return(this.deviceType);
    }
    
    /**
     * Get the level of the firmware that is currently running on the device.
     * 
     * @return the level of the firmware that is currently running on the
     *         device. This may not be the same as the level desired by the
     *         Manager.
     */
    public String getFirmwareLevel() {
        return(this.firmwareLevel);
    }

    /**
     * Get the list features that this device is licensed for.
     * If this is null, there are no feature licenses on the device (or none
     * were returned from the AMP call). 
     * 
     * @return the list of features that this device is licensed for. Each
     *         feature will be a single String.
     */
    public StringCollection getFeatureLicenses() {
        return(this.featureLicenses);
    }
    
    /**
     * Get the list of commands that this device supports.
     * If this is null, there are no commands supported on the device (or none
     * were returned from the AMP call). 
     * 
     * @return the list of commands that this device supports. Each
     *         commands will be a single String.
     */
    public int[] getSupportedCommands() {
    	int[] result = null;
    	if ( this.supportedCommands != null ) 
    		result = supportedCommands.clone();
        return result;
    }
      
    /**
     * Get the current AMP version.
     * 
     * @return the current AMP version that this device supports.
     */
    public String getCurrentAMPVersion() {
        return(this.currentAMPVersion);
    }
    
    /**
     * Get the status of secure backup.
     * 
     * @return the status of secure backup (enabled/disabled).
     */
    public String getSecurebackup() {
        return(this.secureBackup);
    }

    /**
     * Get a String representation of this object for the purpose of debugging
     * and tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging and tracing.
     */
    public String toString() {
        String result = "[DeviceMetaInfo "; //$NON-NLS-1$
        result += "deviceName=" + this.deviceName; //$NON-NLS-1$
        result += ", currentAMPVersion=" + this.currentAMPVersion; //$NON-NLS-1$
        result += ", serialNumber=" + this.serialNumber; //$NON-NLS-1$
        result += ", modelType=" + this.modelType; //$NON-NLS-1$
        result += ", hardwareOptions=" + this.hardwareOptions; //$NON-NLS-1$
        result += ", webGUIPort=" + this.webGUIPort; //$NON-NLS-1$
        result += ", deviceType=" + this.deviceType; //$NON-NLS-1$
        result += ", firmwareLevel=" + this.firmwareLevel; //$NON-NLS-1$
        result += ", featureLicenses=" + this.featureLicenses; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }
}
