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

import java.net.URL;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;

/**
 * Provides enumerated values and helper methods for the response from the
 * device of the subscription request. This is very similar to {@link SubscriptionState},
 * this class has an additional state <code>DUPLICATE</code> with a corresponding
 * URL of the subscription which already exists on the device.
 * <p>
 * An instance of this class is returned from a subscription request, so you
 * do not need to create it explicity, you need to use only the gettr methods. 
 * <p>
 * 
 * @see Commands#subscribeToDevice(DeviceContext, String, StringCollection, URL)
 * @version SCM ID: $Id: SubscriptionResponseCode.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
//* Created on Sep 5, 2006
public class SubscriptionResponseCode {
    private int intValue;
    private String description = null;
    private String duplicateURL = null;
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * The requested subscription has been established on the device.
     * 
     * @see #NONE
     * @see #FAULT
     * @see #createWithDuplicate(String)
     * @see SubscriptionState#ACTIVE
     */
    public static final SubscriptionResponseCode ACTIVE = new SubscriptionResponseCode(0, "active"); //$NON-NLS-1$
    
    /**
     * The requested subscription does not exist on the device. This
     * value is likely to be encountered when unsubscribing from a
     * device where the device has lost the subscription or the subscription
     * id is not valid. Or it may be encountered when a device is pinged
     * (the subscription id is included in the ping request), and the
     * ping response indicates that the subscription does not exist on 
     * the device.
     * 
     * @see #ACTIVE
     * @see #FAULT
     * @see #createWithDuplicate(String)
     * @see SubscriptionState#NONE
     */
    public static final SubscriptionResponseCode NONE = new SubscriptionResponseCode(1, "none"); //$NON-NLS-1$
    
    /**
     * The requested subscription is a duplicate of one that already exists.
     * 
     * @see #ACTIVE
     * @see #NONE
     * @see #createWithDuplicate(String)
     * @see SubscriptionState#FAULT
     */
    public static final SubscriptionResponseCode FAULT = new SubscriptionResponseCode(2, "fault"); //$NON-NLS-1$
    
    /**
     * A String to test for when checking if the subscription request is a duplicate
     * (the device already has a NotificationCatcher subscribed to it).
     */
    public static final String DUPLICATE_STRING = "duplicate"; //$NON-NLS-1$
    
    private SubscriptionResponseCode(int value, String description) {
        this.intValue = value;
        this.description = description;
        this.duplicateURL = null;
    }
    
    /**
     * The requested subscription is a duplicate of one that already exists on
     * the device. The subscription request failed and the original callback URL
     * was not overwritten. The callback URL of the original subscription can be
     * captured here for messages and problem diagnosis.
     * <p>
     * Because this class needs to be thread-safe, this particular response can
     * not be a singleton like the others.
     * 
     * @see #ACTIVE
     * @see #NONE
     * @see #FAULT
     * @see #isDuplicate()
     */
    public static SubscriptionResponseCode createWithDuplicate(String url) {
        SubscriptionResponseCode result = new SubscriptionResponseCode(9, "duplicate"); //$NON-NLS-1$
        result.duplicateURL = url;
        return(result);
    }
    
    /**
     * Check if the response indicates that the subscription request was
     * detected as a duplicate.
     * 
     * @return true if it was a duplicate, false otherwise.
     */
    public boolean isDuplicate() {
        return(this.intValue == 9);
    }
    
    /**
     * Compare two objects to see if they are equivalent. You can use this
     * method when testing for equality with ACTIVE, NONE, or FAULT. If you
     * want to test for a duplicate, use isDuplicate().
     * 
     * @param that the other object to compare to "this"
     * @return true if the two objects are equivalent, false otherwise
     * @see #ACTIVE
     * @see #NONE
     * @see #FAULT
     * @see #isDuplicate()
     */
    public boolean equals(Object that) {
        if (!(that instanceof SubscriptionResponseCode)) return false;
        return (this.intValue == ((SubscriptionResponseCode)that).intValue);
    }
   
    public int hashCode(){
        return this.intValue;
    }

    /**
     * Getter for duplicate URL string
     * 
     * @return the duplicate URL string
     */
    public String getDuplicateURL(){
        return duplicateURL;
    }
    
    /**
     * Setter for duplicate URL string
     * 
     * @param duplicateURL the duplicate URL string to be set
     */
    public void setDuplicateURL(String duplicateURL){
        this.duplicateURL = duplicateURL;
    }
    
    /**
     * Get a human-readable String representation of this object.
     * 
     * @return a human-readable String representation of this object
     */
    public String toString() {
        return(this.description);
    }

}
