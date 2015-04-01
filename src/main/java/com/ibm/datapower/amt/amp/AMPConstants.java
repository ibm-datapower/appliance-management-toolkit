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

import java.util.HashMap;

public class AMPConstants {
	private AMPConstants(){
	}
	private static final HashMap<String,int[]> opToCmdMappings;
	
	static {
		opToCmdMappings = new HashMap<String,int[]>();
		
		opToCmdMappings.put("Ping",new int[]{Commands.PING_DEVICE});
		opToCmdMappings.put("GetToken",new int[]{Commands.GET_SAML_TOKEN});
		opToCmdMappings.put("Reboot",new int[]{Commands.REBOOT});
		opToCmdMappings.put("SetFirmware",new int[]{Commands.SET_FIRMWARE_IMAGE}); // multiple methods
		opToCmdMappings.put("Reinitialize",new int[]{});
		opToCmdMappings.put("SecureBackup",new int[]{Commands.BACKUP_DEVICE});
		opToCmdMappings.put("SecureRestore",new int[]{Commands.RESTORE_DEVICE});
		opToCmdMappings.put("WhenDeviceLastChanged",new int[]{});
		opToCmdMappings.put("Quiesce",new int[]{Commands.QUIESCE_DEVICE, Commands.QUIESCE_DOMAIN, Commands.QUIESCE_SERVICE}); // multiple
		opToCmdMappings.put("Unquiesce",new int[]{Commands.UNQUIESCE_DEVICE, Commands.UNQUIESCE_DOMAIN, Commands.UNQUIESCE_SERVICE}); // multiple
		opToCmdMappings.put("GetDeviceInfo",new int[]{Commands.GET_DEVICE_METAINFO});
		opToCmdMappings.put("GetDeviceSettings",new int[]{});
		opToCmdMappings.put("SetDeviceSettings",new int[]{});
		opToCmdMappings.put("GetErrorReport",new int[]{Commands.GET_ERROR_REPORT});
		opToCmdMappings.put("Subscribe",new int[]{Commands.SUBSCRIBE_TO_DEVICE});
		opToCmdMappings.put("Unsubscribe",new int[]{Commands.UNSUBSCRIBE_FROM_DEVICE});
		opToCmdMappings.put("GetDomainList",new int[]{Commands.GET_DOMAIN_LIST});
		opToCmdMappings.put("GetDomainStatus",new int[]{Commands.GET_DOMAIN_STATUS});
		opToCmdMappings.put("GetDomainExport",new int[]{Commands.GET_DOMAIN});
		opToCmdMappings.put("SetDomainExport",new int[]{Commands.SET_DOMAIN}); // multiple
		opToCmdMappings.put("GetDomainConfig",new int[]{});
		opToCmdMappings.put("SetDomainConfig",new int[]{});
		opToCmdMappings.put("DeleteDomain",new int[]{Commands.DELETE_DOMAIN});
		opToCmdMappings.put("StartDomain",new int[]{Commands.START_DOMAIN});
		opToCmdMappings.put("StopDomain",new int[]{Commands.STOP_DOMAIN});
		opToCmdMappings.put("RestartDomain",new int[]{Commands.RESTART_DOMAIN});
		opToCmdMappings.put("GetCryptoArtifacts",new int[]{Commands.GET_KEY_FILENAMES});
		opToCmdMappings.put("SetFile",new int[]{Commands.SET_FILE});
		opToCmdMappings.put("CompareConfig",new int[]{Commands.IS_DOMAIN_DIFFERENT}); // multiple - getDomainDifferences
		opToCmdMappings.put("GetLog",new int[]{});
		opToCmdMappings.put("WAXHNActivate",new int[]{});
		opToCmdMappings.put("GetServiceListFromExport",new int[]{Commands.GET_SERVICE_LIST_FROM_EXPORT_IMAGE, Commands.GET_SERVICE_LIST_FROM_EXPORT_FILE});
		opToCmdMappings.put("GetInterDependentServices",new int[]{Commands.GET_INTERDEPENDENT_SERVICES_FILE, Commands.GET_INTERDEPENDENT_SERVICES_IMAGE});
		opToCmdMappings.put("GetServiceListFromDomain",new int[]{Commands.GET_SERVICE_LIST_FROM_DOMAIN});
		opToCmdMappings.put("StartService",new int[]{Commands.START_SERVICE});
		opToCmdMappings.put("StopService",new int[]{Commands.STOP_SERVICE});
		opToCmdMappings.put("GetReferencedObjects",new int[]{Commands.GET_REFERENCED_OBJECTS});
		opToCmdMappings.put("DeleteService",new int[]{Commands.DELETE_SERVICE});
		opToCmdMappings.put("DeleteFile",new int[]{Commands.DELETE_FILE});
	}
	
	public static final String NONE_AMP = "0.0";
    public static final String AMP_V1_0 = "1.0";
    public static final String AMP_V2_0 = "2.0";
    public static final String AMP_V3_0 = "3.0";
}
