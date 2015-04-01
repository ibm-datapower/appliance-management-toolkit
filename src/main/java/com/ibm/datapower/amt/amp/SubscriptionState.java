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
 * Provides enumerated values and helper methods for the state of the
 * subscription as reported by the device. This is similar to
 * {@link SubscriptionResponseCode}, but reports only on the current state, not
 * the response to a state change request.
 * <p>
 * 
 * @see Commands#pingDevice(DeviceContext, String)
 * @version SCM ID: $Id: SubscriptionState.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
//* Created on Sep 5, 2006
public class SubscriptionState {
    private int intValue;
    private String description = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    /**
     * This specified subscription exists on the device, and all
     * notifications sent from the device since the subscription request
     * have been acknowledged by the NotificationCatcher. You should assume
     * that all notifications sent from the device have been received by the
     * NotificationCatcher. If the device was booted after it failed to
     * deliver notifications to the NotificationCatcher, then the
     * subscription should not exist because subscriptions are not persisted
     * across reboots.
     */
    public static final SubscriptionState ACTIVE = new SubscriptionState(0, "active"); //$NON-NLS-1$
    
    /**
     * The specified subscription does not exist. Since subscriptions are not
     * persisted on the device, subscriptions may disappear when the device is
     * rebooted. This is a good indication that the device rebooted since the
     * subscription was requested via
     * {@link Commands#subscribeToDevice(DeviceContext, String, StringCollection, URL)}.
     * You should assume that notifications from the device have been lost.
     */
    public static final SubscriptionState NONE = new SubscriptionState(1, "none"); //$NON-NLS-1$
    
    /**
     * The specified subscription has errors, likely due to notifications
     * that the device attempted to send to the NotificationCatcher, but the
     * NotificationCatcher did not acknowledge them for whatever reason.
     * When the device fails to get an acknowledgement of a notification, it
     * will discard the notification and place the subscription in FAULT
     * state, and prepare to send the next notification. You should assume
     * that notifications from the device have been lost.
     */
    public static final SubscriptionState FAULT = new SubscriptionState(2, "fault"); //$NON-NLS-1$
    
    private SubscriptionState(int value, String description) {
        this.intValue = value;
        this.description = description;
    }
    
    /**
     * Compare two objects to see if they are equivalent.
     * 
     * @param that the other object to compare to "this"
     * @return true if the two objects are equivalent, false otherwise
     * @see #ACTIVE
     * @see #NONE
     * @see #FAULT
     */     
    public boolean equals(Object that) {
        if (!(that instanceof SubscriptionState)) return false;
        return (this.intValue == ((SubscriptionState)that).intValue);
    }
   
    public int hashCode(){
        return this.intValue;
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
