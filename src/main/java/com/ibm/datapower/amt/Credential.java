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
package com.ibm.datapower.amt;

import java.util.HashMap;
import java.util.Map;
/**
 * The Credential provides data for access control to the Repository and its contents. 
 * The Credential is basically a data container for a collection of
 * property values. Example properties are userid and passwords. It can also be used to pass
 * the repository directory for the local file system implementation. The
 * Credential is passed to the Manager at startup, which in turn passes it to
 * the RepositoryFactory. The RepositoryFactory passes it to the Repository
 * implementation so that the RepositoryImpl can read these properties and do
 * whatever it wants to validate authentication and authorization for access to
 * the repository. The repository is considered the asset that needs access
 * control, because of the data it contains, which includes device
 * administrative userids and passwords. For access control to the binary executable 
 * jar, we will rely on filesystem access controls. 
 * <p>
 * See the local filesystem package for an example use of the Credential object.
 * 
 * @see com.ibm.datapower.amt.dataAPI.RepositoryFactory#getRepository(String,
 *      Credential)
 * @see com.ibm.datapower.amt.dataAPI.Repository
 * @see com.ibm.datapower.amt.clientAPI.Manager#getInstance(Map)
 * @see com.ibm.datapower.amt.clientAPI.Manager#OPTION_CREDENTIAL
 * @version SCM ID: $Id: Credential.java,v 1.7 2011/01/24 20:25:23 lsivakumxci Exp $
 */
public class Credential {
	
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.7 $"; //$NON-NLS-1$

    private Map map = null;
    
    /**
     * Create an empty credential. Use the {@link #setProperty(String, Object)}
     * method to add information to this credential object. After the credential
     * properties are populated it will be passed to the Manager using
     * OPTION_CREDENTIAL.
     * 
     * @see #setProperty(String, Object)
     * @see com.ibm.datapower.amt.clientAPI.Manager#OPTION_CREDENTIAL
     */
    public Credential() {
        map = new HashMap();
    }
    
    /**
     * Add information to a credential, such as a userid, password, SAML
     * assertion, signed message, etc.
     * 
     * @param name name of the property, such as "password". The list of
     *        recognized names is dependent on the Manager which evaluates this
     *        credential.
     * @param value a value of this property that corresponds with the name.
     * @see #getProperty(String)
     */
    public void setProperty(String name, Object value) {
        map.put(name, value);
    }
    
    /**
     * Get the value of the named property from this credential.
     * 
     * @param name the name of the property to get
     * @return the value of the named property of this credential.
     * @see #setProperty(String, Object)
     */
    public Object getProperty(String name) {
        return(map.get(name));
    }
    
    /*
     * We don't need to add a authenticate() and authorize() method here
     * because the RepositoryImpl will interpret the data inside the
     * Credential. If we added no-op authn/authz methods here then they
     * would need to be overriden by a different class, and this is not
     * a pluggable interface. Just push the authn/authz work to the
     * RepositoryImpl. The getProperty method is required for the
     * RepositoryImpl to perform the authn/authz.
     * 
     */
    
    public String toString() {
    	return map.toString();
    }
    
}
