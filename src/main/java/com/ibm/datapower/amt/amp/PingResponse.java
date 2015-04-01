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
 * This is the object that will be returned from
 * {@link Commands#pingDevice(DeviceContext, String)}. It will be constructed
 * by that method, you need to use only the gettr methods.
 * <p>
 * 
 * @see Commands#pingDevice(DeviceContext, String)
 * @version SCM ID: $Id: PingResponse.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
public class PingResponse {
    private SubscriptionState subscriptionState = null;
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$

    
    /**
     * Create a new PingResponse object. In general, this should be invoked only
     * by {@link Commands#pingDevice(DeviceContext, String)}, so you do not
     * need to invoke this explicitly.
     * 
     * @param subscriptionState the state of the subscription to the device
     */
    public PingResponse(SubscriptionState subscriptionState) {
        this.subscriptionState = subscriptionState;
    }
    
    /**
     * Get the state of the subscription on the device. Please see the further
     * documentation in the javadoc for {@link SubscriptionState}.
     * 
     * @return the state of the subscription on the device
     */
    public SubscriptionState getSubscriptionState() {
        return(subscriptionState);
    }

}
