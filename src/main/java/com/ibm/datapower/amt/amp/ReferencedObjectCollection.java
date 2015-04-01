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
import java.util.HashMap;
import java.util.Map;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;

/**
 * This class is used to represent the referenced object returned from the 
 * {@link Commands#getReferencedObjects(DeviceContext, String , String , String )} 
 */

public class ReferencedObjectCollection {
	private HashMap<String, ArrayList<com.ibm.datapower.amt.amp.ConfigObject>> referencedObjectTable;
    private StringCollection fileList = new StringCollection();
    
    private String primaryKey=null;

    public static final String COPYRIGHT_2012 = Constants.COPYRIGHT_2012;

    /** 
     * Construct a new ReferencedObjectCollection object which is returned
     * only by {@link Commands#getReferencedObjects(DeviceContext, String , String , String ) in general.
     *
     */   
    public ReferencedObjectCollection(HashMap<String, ArrayList<com.ibm.datapower.amt.amp.ConfigObject>> referencedObject,	StringCollection fileList, String primaryKey){
    	this.referencedObjectTable = referencedObject;
    	this.fileList = fileList;
    	this.primaryKey = primaryKey;
    }
    
    /**
     * Get all direct ConfigObject(s), use ConfigObject.getReferencedObjects() to get the deeper configobject(s).
     * @return all ConfigObjects 	
     */
    public ConfigObject[] getReferencedObjects() {    	
    	ConfigObject[] result = null;
    	
    	ArrayList<ConfigObject> list = this.referencedObjectTable.get(this.primaryKey);
    	if ( list != null ) {
    		int iSize = list.size();
    		result = new ConfigObject[iSize];
    		for ( int i=0; i < iSize; i++ ) {
    			result[i] = (ConfigObject) list.get(i);
    		}
    	}  	
    	return result;
    }   
    
    /**
     * Get file list in the referenced ConfigObject
     * @return a file list
     */
    public StringCollection getReferencedFiles() {        	
    	return this.fileList;
    }
}
