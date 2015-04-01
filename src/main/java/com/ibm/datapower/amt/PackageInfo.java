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

import java.lang.reflect.Field;

/**
 * Get the SCM information about all the classes in this package.  It is
 * typically invoked only by {@link com.ibm.datapower.amt.ComponentInfo}.
 * 
 * @see AbstractPackageInfo
 */
class PackageInfo extends AbstractPackageInfo {
	private static PackageInfo instance = null;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Get an instance of this object. This method should be invoked only by
     * {@link ComponentInfo}. This is a public method only because it needs to
     * be invoked by <code>ComponentInfo</code>, which is in a different
     * package.
     * 
     * @return an instance of this object.
     */
    public static PackageInfo getInstance() {
    	if (instance == null) {
    		instance = new PackageInfo();
    	}
    	return(instance);
    }

	private PackageInfo() {
		super();
		// make sure this list is complete for this package
		this.add(AbstractFactory.class);
		this.add(AbstractPackageInfo.class);
		this.add(ComponentInfo.class);
		this.add(Constants.class);
		this.add(Credential.class);
        this.add(DeviceLogger_ClientThread.class);
        this.add(DeviceLogger.class);
		this.add(DeviceType.class);
		this.add(DMgrException.class);
        this.add(Messages.class);
		this.add(ModelType.class);
		this.add(OperationStatus.class);
		this.add(PackageInfo.class);
		this.add(StringCollection.class);
		// TODO: need to add DeviceLogger_ClientThread and DeviceLogger
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
