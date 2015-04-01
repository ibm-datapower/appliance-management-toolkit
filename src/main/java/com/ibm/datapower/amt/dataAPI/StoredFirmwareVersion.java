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

import java.util.Date;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.clientAPI.Blob;

/**
 * The information that must be maintained and persisted for a firmware level, 
 * See {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion}. <code>StoredFirmwareVersion</code>s can be deployed to managed or 
 * unmanaged devices. A FirmwareVersion can be deployed to all device members
 * that belong to a ManagedSet.   
 * <p>
 * When creating a StoredFirmwareVersion that has a Blob, please read the
 * {@link Blob Blob class javadoc} about verifying the contents are not in a
 * byte array via {@link Blob#hasBytes()}.
 * 
 */
public interface StoredFirmwareVersion extends StoredVersion {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
    /**
     * Gets the version level of this firmware. This value is embedded in the
     * firmware image, so there is no <code>setLevel(String)</code> method. It
     * will be automatically populated in this object by the
     * <code>setImage</code> method.   This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getLevel()}
     * <p>
     * Note: The Local File System implementation persists the level an an attribute  
     * on this version element in the WAMT.repository.xml
     * </p> 
     * @return the firmware version level, for example <code>3.5.1.12</code>.
     */
    public String getLevel();

    /**
     * Gets the manufacture date of this firmware. This value is embedded in the
     * firmware image, so there is no <code>setManufactureDate(Date)</code>
     * method. It will be automatically populated in this object by the
     * <code>setImage</code> method. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getManufactureDate()}
     * <p>
     * Note: The Local File System implementation persists date of manufacture an an attribute  
     * on this version element in the WAMT.repository.xml
     * </p> 
     * @return the date of manufacture of this firmware image
     */
    public Date getManufactureDate();

    /**
     * Gets the StoredFirmware Object that contains this StoredFirmwareVersion. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getFirmware()}
     * 
     * @return he StoredFirmware Object that contains this StoredFirmwareVersion
     */
    public StoredFirmware getFirmware();

    /**
     * Returns the Unique identifier for this StoredFirmwareVersion. 
     * There is no <code>setPrimaryKey</code> exposed since it is managed by the
     * dataAPI implementation. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.FirmwareVersion#getPrimaryKey()} 
     * <p>
     * Note: The Local File System implementation combines the unique identifier 
     * of the containing firmware with the firmware level to generate   
     * the unique identifier of this object. 
     * </p>
     * @return the unique identifier for this object
     */
    String getPrimaryKey();

}
