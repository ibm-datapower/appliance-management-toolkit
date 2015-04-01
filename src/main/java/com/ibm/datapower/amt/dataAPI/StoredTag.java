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

import com.ibm.datapower.amt.Constants;

/**
 * The information that must be maintained and persisted for a
 * {@link com.ibm.datapower.amt.clientAPI.Device} and {@link com.ibm.datapower.amt.clientAPI.Domain} 
 * including its tagged device and domain members.
 * A <code>StoredTag</code> tracks every <code>StoredDevice</code> and <code>StoreDomain</code> that is tagged by it.  
 * 
 */
public interface StoredTag  extends Persistable{

    public static final String COPYRIGHT_2012 = Constants.COPYRIGHT_2012;


    /**
     * Gets the name of this StoredTag. This name should be human-consumable. 
     * The name is immutable, so there is no <code>setName(String)</code> method.
     * 
     * @return name of this StoredTag.
     */
    public String getName();
    
    /**
     * Gets the value of this StoredTag. This value should be human-consumable. 
     * The value is immutable, so there is no <code>setName(String)</code> method.
     * 
     * @return name of this StoredTag.
     */
    public String getValue();
    

    /**
     * Adds the specified device to this tag. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Device#addTag(String, String)}
     * 
     * @param device the device to add     
     * @see #add(StoredDomain)
     */
    public void add(StoredDevice device);
    
    
    /**
     * Adds the specified domain to this tag. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Domain#addTag(String, String)}
     * 
     * @param domain the domain to add
     * @see #add(StoredDevice)
     */
    public void add(StoredDomain domain);

    /**
     * Gets the array of devices which are members of this tag.
     * 
     * @return an array of devices which are members of this tag.
     * @see #add(StoredDevice) This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Device#getTagNames()}
     * 
     */    
     public StoredDevice[] getDeviceMembers();
     
     /**
      * Gets the array of domains which are members of this tag.
      * 
      * @return an array of devices which are members of this tag
      * @see #add(StoredDomain) This is invoked by
      * {@link com.ibm.datapower.amt.clientAPI.Domain#getTagNames()}
      * 
      */ 
     public StoredDomain[] getDomainMembers();

    /**
     * Removes the specified device from this tagged set. This is invoked by
     * {@link com.ibm.datapower.amt.clientAPI.Device#removeTag(String)}
     * The removed device is still persisted in the repository though it is no longer tagged by this tag.
     * You can reverse this by adding the device back to the tag. 
     * 
     * @param device the device to remove
     * @see #add(StoredDevice)
     * @see #getDeviceMembers()
     */
    public void remove(StoredDevice device);
    
    /**
     * Removes the specified domain from this tagged set. This is invoked by
     * {@link com.ibm.datapower.amt.dataAPI.StoredDomain#remove(StoredTag)} and 
     * {@link com.ibm.datapower.amt.dataAPI.StoredDomain#removeTags()  )}
     * The removed domain is still persisted in the repository though it is no longer tagged by this tag.
     * You can reverse this by adding the domain back to the tag. 
     * 
     * @param domain the domain to remove
     * @see #add(StoredDomain)
     * @see #getDomainMembers()
     */
    public void remove(StoredDomain domain);

}
