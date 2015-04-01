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
package com.ibm.datapower.amt.soma;

import java.util.List;
import java.util.Map;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.amp.DeviceContext;

/**
 * A list of high-level commands that the DataPower device should support via
 * SOAP invocation. These are commands sent from a manager to the device.
 * <p>
 */
public interface SOMACommands {
    public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;
        
    /**
    * Get all SOMA status supported by device
    * @param device the DataPower device to get status
    * @param domainName the name of the domain to get status
    * @return all SOMA status supported by device, the String in the Map is the operation name of SOMA
    * 
    * @throws SOMAException
    * @throws SOMAIOException
    */
    public Map<String, List<Status>> getAllStatus(DeviceContext device, String domainName)
    throws SOMAException, SOMAIOException;
    
    /**
     * Get SOMA status with operation name supported by device
     * @param device the DataPower device to get status
     * @param domainName the name of the domain to get status
     * @param name the name of operation of status
     * @return SOMA status list supported by device
     * 
     * @throws SOMAIOException
     * @throws SOMAException
     */
    List<Status> getStatus(DeviceContext device, String domainName, String OpName)
    throws SOMAIOException, SOMAException;
    
}
