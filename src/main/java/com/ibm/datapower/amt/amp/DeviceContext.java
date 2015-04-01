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
 * This provides the information/context necessary to identify a particular
 * DataPower device for communicating with it, and have the authentication or
 * other data necessary to connect to it. This should provide the context to
 * invoke methods in {@link Commands}.
 * <p>
 * 
 * @version SCM ID: $Id: DeviceContext.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* <p>
//* Created on Jun 21, 2006
//* <p>
public class DeviceContext {
    String hostname = null;
    int ampPort = 0;
    String userid = null;
    String password = null;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Create a new DeviceContext object. The following parameters are required.
     * 
     * @param hostname the hostname or IP address of the device
     * @param ampPort the device's port to connect to for AMP commands (XML
     *        Management Interface that has the AMP endpoint enabled). AMP is
     *        the Appliance Management Protocol.
     * @param userid the device's administrative userid to execute AMP commands
     * @param password the password for the above administrative userid
     */
    public DeviceContext(String hostname, int ampPort, String userid, String password) {
        this.hostname = hostname;
        this.ampPort = ampPort;
        this.userid = userid;
        this.password = password;
    }
    
    /**
     * Get the device's hostname or IP address from this context.
     * 
     * @return the device's hostname
     */
    public String getHostname() {
        return(hostname);
    }
    
    /**
     * Get the device's port for AMP commands.
     * 
     * @return the port number for AMP
     */
    public int getAMPPort() {
        return(ampPort);
    }
    
    /**
     * Get the device's administrative userid from this context.
     * 
     * @return the device's administrative userid. This userid can be used to
     *         both read and change the device's configuration.
     * @see #getPassword()
     */
    public String getUserId() {
        return(userid);
    }
    
    /**
     * Get the device's password for the administrative userid id from this
     * context.
     * 
     * @return the device's password for the administrative userid. This
     *         password can be used with the administrative userid to both read
     *         and change the device's configuraiton.
     * @see #getUserId()
     */
    public String getPassword() {
        return(password);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * and tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging and tracing.
     */
    public String toString() {
        String result = "[DeviceContext: hostname=" + this.hostname + //$NON-NLS-1$
        ", ampPort=" + this.ampPort + //$NON-NLS-1$
        ", userid=" + this.userid + //$NON-NLS-1$
        ", password(length)=" + this.password.length() + " ]"; //$NON-NLS-1$ //$NON-NLS-2$
        return(result);
    }
}
