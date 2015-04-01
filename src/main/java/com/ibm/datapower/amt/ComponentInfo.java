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

import java.lang.reflect.Method;

/**
 * A utility to get the SCM information (i.e., CVS revision) of all classes in
 * all packages for this component (IBM Appliance Management Toolkit) 
 * for debug purposes. The SCM information
 * is embedded in each class with the data member "SCM_REVISION". This utility
 * may be also invoked standalone via {@link #main(String[])}, which will print
 * all the information to System.out.
 * <p>
 * You do not need to instantiate an object of this class, all the methods
 * and member variables are static. It will invoke the <code>PackageInfo</code>
 * class in each package.
 * 
 * @see AbstractPackageInfo
 */
public class ComponentInfo {

	private static String[] packageNames;

    // increment the following value for each significant publish
    private static final String PUBLISH_REVISION = "1.2";  //$NON-NLS-1$

    private static final String CLASS_RELATIVE_NAME = "PackageInfo"; //$NON-NLS-1$
	private static final String GET_INSTANCE_NAME = "getInstance"; //$NON-NLS-1$
	private static final String METHOD_NAME = "getInfo"; //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$

	static {
		// this should be a hardcoded list of all the packages in this component
		packageNames = new String[] {
				"com.ibm.datapower.amt", //$NON-NLS-1$
				"com.ibm.datapower.amt.amp", //$NON-NLS-1$
				"com.ibm.datapower.amt.amp.defaultProvider", //$NON-NLS-1$
				"com.ibm.datapower.amt.amp.defaultV2Provider", //$NON-NLS-1$
				"com.ibm.datapower.amt.amp.defaultV3Provider", //$NON-NLS-1$
				"com.ibm.datapower.amt.clientAPI", //$NON-NLS-1$
				"com.ibm.datapower.amt.dataAPI", //$NON-NLS-1$
                "com.ibm.datapower.amt.dataAPI.utils" //$NON-NLS-1$
		};
	}
	
	private ComponentInfo() {
	    // don't call this constructor, the methods are static
	}
	
	/**
     * Print out the SCM information for each package, which in turn prints the
     * SCM information for each class. The result will appear on System.out.
     * This method is normally not used, unless you want to invoke it
     * standalone. Doing so would let you see the SCM information from the
     * command line using only this jar file.
     * 
     * @param args ignored
     */
	public static void main(String[] args) {
		StringBuffer result = walkPackages();
		System.out.println(result);
	}
	
	private static StringBuffer walkPackages() {
		StringBuffer result = new StringBuffer();
		
        result.append("Publish version: ");  //$NON-NLS-1$
        result.append(ComponentInfo.PUBLISH_REVISION);
        
		for (int packageNameIndex=0; packageNameIndex<packageNames.length; packageNameIndex++) {
			if (result.length() > 0) {
				result.append("\n"); //$NON-NLS-1$
			}
			String className = packageNames[packageNameIndex] + "." + CLASS_RELATIVE_NAME; //$NON-NLS-1$
			Class namedClass = null;
			try {
				namedClass = Class.forName(className);
			} catch (Exception e) {
				result.append("  Unable to find class " + className + ": " + e.toString() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				continue;
			}
			
			// get an instance of the concrete implementor of AbstractPackageInfo
			AbstractPackageInfo instance = null;
			try {
				Method getInstanceMethod = namedClass.getMethod(GET_INSTANCE_NAME, null);
				Object object = getInstanceMethod.invoke(null, null);
				if ((object != null) && (object instanceof AbstractPackageInfo)) {
					instance = (AbstractPackageInfo) object;
				} else if (!(object instanceof AbstractPackageInfo)){
					result.append("  Object from " + GET_INSTANCE_NAME + " method is not instance of AbstractPackageInfo\n"); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				} else {
					result.append("  Object from " + GET_INSTANCE_NAME + " method is null\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (Exception e1) {
				result.append("  Unable to get instance of " + namedClass.getName() + //$NON-NLS-1$
				        ": " + e1 + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			// the method implementation may be in a superclass, so look through the hierarchy
			Class specificClass = namedClass;
			Method method = null;
			do {
				try {
					// static methods don't appear in getDeclaredMethod(String), so look at all
					Method[] methods = specificClass.getDeclaredMethods();
					for (int methodIndex=0; methodIndex<methods.length; methodIndex++) {
						if (methods[methodIndex].getName().equals(METHOD_NAME)) {
							method = methods[methodIndex];
						}
					}
				} catch (Exception e) {
					// just eat it
				}
				specificClass = specificClass.getSuperclass();
			} while ((method == null) && (specificClass != null));
			if (method == null) {
				// didn't find it
				result.append("  Unable to find method " + METHOD_NAME +  //$NON-NLS-1$
						" expected in class " + className + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			Object object = null;
			try {
				object = method.invoke(instance, null);
			} catch (Exception e) {
				result.append("  Unable to invoke method " + METHOD_NAME + //$NON-NLS-1$
						" in class " + className + ": " + e.toString() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				continue;
			}

			if (object instanceof String) {
				String stringObject = (String) object;
				result.append(stringObject);
			} else {
				result.append("  Returned value is not instanceof String in class " + //$NON-NLS-1$
						className + "\n"); //$NON-NLS-1$
			}
		}
		result.append("\n"); //$NON-NLS-1$
		return(result);
	}

}
