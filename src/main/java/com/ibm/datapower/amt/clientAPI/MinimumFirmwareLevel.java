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

public class MinimumFirmwareLevel {
    // Minimum firmware level required on a device to support Full Backup and Restore
	public static final String MINIMUM_FW_LEVEL_FOR_BACKUP = "3.8.1";	
	public static final String MINIMUM_FW_LEVEL_FOR_RESTORE = "3.8.1";
	// Minimum firmware level required on a device to support Domain Quiesce	
    public static final String MINIMUM_FW_LEVEL_FOR_QUIESCE = "3.8.1";
    // Minimum firmware level required on a device to support a deployment policy at domain deploy-time    
    public static final String MINIMUM_FW_LEVEL_FOR_DEPLOYMENT_POLICY = "3.8.1";
    // Minimum firmware level required on a device for the manager to work properly
    public static final String MINIMUM_LEVEL = "3.6.0.4"; 
    // Minimum firmware level required on a device to support Service Level Configuration Management    
    public static final String MINIMUM_FW_LEVEL_FOR_SLCM = "5.0.0";
    
    
    
    // License Feature for zGryphon
    public static final String  DP_ZGRPYPHON_LICENSE_FEATURE = "zBX";

    // For mapping of equivalent firmware levels 
    // This is a hack but necessary due to Edge renumbering.
    static final String[] EQUIVALENT_FIRMWARE_LEVELS =
    	// Each equivalent firmware entry is a pair of two strings.
    	// 1st string is used to match actual level, 2nd string is the replacement string.
    	{"1.0.0.","4.0.0.0", // Edge 1.0.0.x is equiv to 4.0.0.x 
    	}; 

}
