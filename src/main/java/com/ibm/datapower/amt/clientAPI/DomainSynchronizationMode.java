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


/**
 * <p>DomainSynchronizationMode is an enumeration used to determine the synchronization
 * policy that will used for managed domains.  
 * 
 * <p>The default is MANUAL. Configuration deployment is done manually. The administrator 
 * is responsible for manually scheduling the Domain deploy configuration task.
 * <p>With the AUTO mode, configuration deployment is synchronized automatically.
 * 
 * @see ManagedSet#setSynchronizationModeForDomain(String, DomainSynchronizationMode)
 * @see Domain#setSynchronizationMode(DomainSynchronizationMode)
 */
public enum DomainSynchronizationMode {
	AUTO,
	MANUAL
}
