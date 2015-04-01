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
* An error report is an object generated on the DataPower appliance that reports the
* running config, logs, and other pertinent data that can be used in debugging. 
* There is a operation in AMP which supports generating and retrieving an error 
* report from a domain on the device. This class represents the data that would 
* be returned from {@link Commands#getErrorReport(DeviceContext)}. Objects of this class
* are automatically instantiated by that <code>getErrorReport</code> method.
* <p>
*/
//* <p>
public class ErrorReport {
    String domainName;
    String location;
    String name;
    byte[] reportFile;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;


    /**
     * Create a new ErrorReport object. The following parameters are required.
     * 
     * @param domainName the name of the domain from where the file was extracted
     * @param location the directory from where the file was extracted
     * @param name the name of the error report file as stored on the appliance
     * @param reportFile the actual file, represented as a byte[]
     */
    public ErrorReport(String domainName, String location, String name, byte[] reportFile){
        this.domainName = domainName;
        this.location = location;
        this.name = name;
        if ( reportFile != null )
        	this.reportFile = reportFile.clone();
    }
    
    /**
     * Get the name of the domain for which the error report was generated.
     * 
     * @return A domain name from the appliance
     */
    public String getDomainName(){
        return this.domainName;
    }
    
    /**
     * Get the path to the directory on the appliance where the report resides.
     * 
     * @return the location (directory) where the error report file resides
     */
    public String getLocation(){
        return this.location;
    }
    
    /**
     * Get the filename of the error report as it resides on the appliance.
     * 
     * @return the filename of the error report
     */
    public String getName(){
        return this.name;
    }
    
    /**
     * Get the contents of the error report.
     * 
     * @return the error report as a byte array
     */    
    public byte[] getReportFile(){
    	byte[] result = new byte[this.reportFile.length];
        System.arraycopy(this.reportFile, 0, result, 0, this.reportFile.length);
        return result;
    }
}
