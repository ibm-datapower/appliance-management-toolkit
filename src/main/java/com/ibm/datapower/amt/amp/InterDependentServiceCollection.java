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

import java.util.ArrayList;
import java.util.List;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.RuntimeService;

/**
 * This represents the data that would be returned only from
 * {@link Commands#getInterDependentServices(DeviceContext, String, byte[], ConfigObject[])},
 * {@link Commands#getInterDependentServices(DeviceContext, String, String, String, ConfigObject[])}.
 * 
 */

public class InterDependentServiceCollection {
	private List<RuntimeService> serviceList = null;
    private List<ConfigObject> objectToBeOverwritten = null;
    private StringCollection fileList = null;

    public static final String COPYRIGHT_2012 = Constants.COPYRIGHT_2012;

    /** 
     * Construct a new InterDependentServiceCollection object which is returned only by
     * {@link Commands#getInterDependentServices(DeviceContext, String, byte[], ConfigObject[]} in general.
     */    
	public InterDependentServiceCollection(List<RuntimeService> serviceList, List<ConfigObject> objectToBeOverwritten, StringCollection fileList) {
    	this.serviceList = serviceList;
    	this.objectToBeOverwritten = objectToBeOverwritten;
		this.fileList = fileList;
    }
    
    /**
     * Get all RuntimeService objects of the interdependent service.
     * @return a RuntimeService array contains all services
     */
    public RuntimeService[] getInterDependentServices() {    	
    	if ( this.serviceList != null ) {
	    	int iSize = this.serviceList.size();
	    	RuntimeService[] service = new RuntimeService[iSize];
	    	
	    	iSize = 0;
	    	for( RuntimeService list: this.serviceList ) {
	    		service[iSize++] = list;
	    	}
	    	return service;
    	}
    	return (new RuntimeService[0]);
    }
    
    /**
     * Get a file list of the interdependent service to be overwritten.
     * @return a StringCollection contains all files
     */
    public StringCollection getFilesToBeOverwritten() {    	
    	if ( this.fileList != null )
    		return this.fileList;
    	return ( new StringCollection());
    }
     
    /**
     * Get the ConfigObject object(s) to be overwritten.
     * @return the ConfigObject array
     */
    public ConfigObject[] getObjectToBeOverwritten() {
    	if ( this.objectToBeOverwritten != null ) {
	    	int iSize = this.objectToBeOverwritten.size();
	    	ConfigObject[] objectInfo = new ConfigObject[iSize];
	    	
	    	iSize = 0;
	    	for( ConfigObject list: this.objectToBeOverwritten ) {
	    		objectInfo[iSize++] = list;
	    	}
	    	return objectInfo;
    	}    	
    	return (new ConfigObject[0]); 
    }     
    
}
