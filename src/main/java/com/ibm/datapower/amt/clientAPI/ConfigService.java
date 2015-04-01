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

import com.ibm.datapower.amt.Constants;

/**
 * The ConfigService is to represent the information of service returned from the {@link ServiceConfiguration#getAvailableServices(Device)}. 
 * 
 */
public class ConfigService  {
	private String name = "";
	private String className = "";
	private String classDisplayName = "";	
	private String userComment = "";
	
	public static final String COPYRIGHT_2012 = Constants.COPYRIGHT_2012;
	

	/**
	 *
	 * Construct a new ConfigService object.
	 * 
	 * @param name the name of this ConfigService
	 * @param className the class name of this ConfigService
	 * @param classDisplayName the display name of this ConfigService
	 * @param userComments the user comments of this ConfigService
	 */
	public ConfigService(String name, String className, String classDisplayName, String userComments) {
		this.name = name;
		this.className = className;
		this.classDisplayName = classDisplayName;
		this.userComment = userComments;
	}
	
	/**
	 * Get the name of this ConfigService
	 * @return the String representation of this object's name
	 */
	public String getName() {
		return (this.name);
	}
	
	/**
	 * Get the class name of this ConfigService.
	 * @return the String representation of this object's class name
	 */
	public String getClassName() {
		return (this.className);
	}
	
	/**
	 * Get the class display name of this ConfigService.
	 * @return the String representation of this object's display name
	 */
    public String getClassDisplayName() {
        return (this.classDisplayName);
    }
    
    /**
     * Get the user comment of this ConfigService.
     * @return the String representation of this object's user comments
     */
    public String getUserComment() {
    	return (this.userComment);
    }
    
    /**
     * Get the pre-built String that could be used as a primary key for this
     * object if you need to get it anywhere or put it in a hash collection.
     * Although this method isn't necessary for users of the clientAPI, it may
     * be helpful for them to have so they don't need to implement it
     * themselves. It is used internally within the clientAPI and is exposed
     * here for your convenience.
     * 
     * @return a String that could represent a unique instance of this object.
     *         It use the Name and ClassName as the primary key.
     *         (i.e. Name:Classname)
     */
    public String getPrimaryKey() {
    	return (this.name+":"+this.className);
    }
    
    
    /**
     * Get a String representation of object for the purpose of debugging or tracing.
     * @return a String representation 
     */
    public String toString() {
        String result = "[ConfigService "; //$NON-NLS-1$
        result += "Name=" + this.name; //$NON-NLS-1$
        result += ", ClassName=" + this.className; //$NON-NLS-1$
        result += ", ClassDisplayName=" + this.classDisplayName; //$NON-NLS-1$
        result += ", UserComments=" + this.userComment; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        
        return(result);
    }
}
