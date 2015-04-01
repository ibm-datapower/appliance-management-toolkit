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

package com.ibm.datapower.amt.amp.defaultV2Provider;

import java.lang.reflect.Field;

import com.ibm.datapower.amt.AbstractPackageInfo;
import com.ibm.datapower.amt.Constants;

/**
 * Get the SCM information about all the classes in this package.  It is
 * typically invoked only by {@link com.ibm.datapower.amt.ComponentInfo}.
 * 
 * @see AbstractPackageInfo
 */
public class PackageInfo extends AbstractPackageInfo {
	private static PackageInfo instance = null;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    public static PackageInfo getInstance() {
    	if (instance == null) {
    		instance = new PackageInfo();
    	}
    	return(instance);
    }

	private PackageInfo() {
		super();
		// make sure this list is complete for this package
		this.add(CommandsImpl.class);
		this.add(HttpHandler.class);
		this.add(NotificationCatcherImpl.class);
		this.add(PackageInfo.class);
		this.add(SOAPHelperImpl.class);
		this.add(SSLContextCache.class);
		this.add(TCPSocketListener.class);
		this.add(Utils.class);
	}
	
	protected Field getDeclaredField(String fieldName, Class cl) 
	throws SecurityException, NoSuchFieldException {
		return(cl.getDeclaredField(fieldName));
	}

	protected Object getValueFromField(Field field) 
	throws IllegalArgumentException, IllegalAccessException {
		return(field.get(null));
	}
	
}
