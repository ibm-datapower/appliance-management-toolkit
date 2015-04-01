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

/**
 * This represents the data that would be returned from
 * {@link Commands#deleteService(DeviceContext, String, String, String, ConfigObject[], boolean)}. Object of this class
 * is instantiated automatically by that method.
 *
 *
 */

public class DeleteObjectResult {
	private ConfigObject configObject=null;
	
	private boolean deleted = false;
	private boolean excluded = false;    
	
	private String errorMessage = "";
    
    public static final String COPYRIGHT_2012 = Constants.COPYRIGHT_2012;
 
    /**
     * Construct a new ConfigObjectStatus object. In general this should be invoked
     * only by {@link Commands#deleteService(DeviceContext, String, String, String, ConfigObject[], boolean)}.
     *  
     * @param deleted The deleted indicator
     * @param excluded The excluded indicator
     * @param errorMessage The error message
     */
    public DeleteObjectResult(ConfigObject configObject, boolean deleted, boolean excluded, String errorMessage) {	
    	this.configObject = configObject;
    	this.deleted = deleted;
		this.excluded = excluded;
		this.errorMessage = errorMessage;
    }
   
    /**
	 * Get the name of this object.
	 * @return the String representation of this object's name
	 */
	public String getName() {
		return (this.configObject.getName());
	}
	
	/**
	 * Get the class name of this object.
	 * @return the String representation of this object's class name
	 */
	public String getClassName() {
		return (this.configObject.getClassName());
	}
	
	/**
	 * Get the class display name of this object.
	 * @return the String representation of this object's display name
	 */
    public String getClassDisplayName() {
        return (this.configObject.getClassDisplayName());
    }
        
    /**
     * Get the error message of this object when the delete operation is failed. 
     * @return the String representation of this object for the error message
     */
    public String getErrorMessage() {
        return(this.errorMessage);
    }
        
    /**
     * Get the deleted indicator of this object. 
     * @return true if this object is deleted, false otherwise.
     */
    public boolean getDeleted() {
    	return (this.deleted);
    }
        
    /**
     * Get the excluded indicator of this object.
     * @return true if this object is excluded, false otherwise.
     */
    public boolean getExcluded() {
    	return (this.excluded);
    }    
    
    /**
     * Get a String representation of object for the purpose of debugging or tracing.
     * @return a String representation 
     */
    public String toString() {
        String result = "[DeletedObjectStatus "; //$NON-NLS-1$
        result += ", deleted=" + this.deleted;
        result += ", excluded=" + this.excluded;
        result += ", error message= " + this.errorMessage;
        result += "]"; //$NON-NLS-1$
        
        return(result);
    }
}
