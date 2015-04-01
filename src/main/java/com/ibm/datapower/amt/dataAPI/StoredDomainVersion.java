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
package com.ibm.datapower.amt.dataAPI;

import java.io.OutputStream;

import com.ibm.datapower.amt.Constants;

/**
 * Information that must be  maintained and persisted for a version of a Domain object in the repository. 
 * {@link com.ibm.datapower.amt.dataAPI.StoredDomainVersion}s can be
 * deployed to a DataPower device.
 * <p>
 * When creating a StoredDomainVersion that has a Blob, please read the
 * {@link com.ibm.datapower.amt.clientAPI.Blob Blob class javadoc} about
 * verifying the contents are not in a byte array via
 * {@link com.ibm.datapower.amt.clientAPI.Blob#hasBytes()}.
 * 
 */
public interface StoredDomainVersion extends StoredVersion {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Unique identifier for this StoredDomainVersion. This is invoked by 
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#getPrimaryKey()}.
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation. 
     *  
     * 
     * @return the unique identifier for this object
     */
    String getPrimaryKey();

    /**
     * Get the <code>StoredDomain</code> that contains this version. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.DomainVersion#getDomain()}
     * 
     * @return StoredDomain object
     * 
     */
    public StoredDomain getDomain();
    
    /**
     * Generate a Domain Configuration Deployment Report.  The implementation of this
     * interface is optional.  
     *
     * 
     * @param outputStream that will be used to write the report
     * 
     */
    public void recordDomainVersion(OutputStream outputStream);
    
    

}
