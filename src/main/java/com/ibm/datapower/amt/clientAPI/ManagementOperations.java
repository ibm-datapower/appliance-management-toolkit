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

public enum ManagementOperations {
	DOMAIN_CONFIG_MANAGEMENT,
	SERVICE_CONFIG_MANAGEMENT,
	FIRMWARE_UPDATE,
	BACKUP_RESTORE,
	DEVICE_QUIESCE_UNQUIESCE,
	DOMAIN_QUIESCE_UNQUIESCE
}
