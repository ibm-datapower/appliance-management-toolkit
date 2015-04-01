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
import java.util.Vector;

/**
 * Get the SCM (source code management, i.e., CVS metadata) information about all
 * the classes in a package for debug purposes. It is expected that each package
 * will have a class that is a concrete implementation (extends) this
 * one. Given a collection of Classes, in each Class look for a data member
 * "SCM_REVISION" which should be a String and return it. The
 * <code>getInfo()</code> method will be invoked by {@link ComponentInfo}.
 * The <code>ComponentInfo</code> class will query the concrete
 * implementations of <code>AbstractPackageInfo</code> in all the packages so
 * you can see all the SCM_REVISION strings of all the classes in all the
 * packages.
 * <p>
 * When extending this class for a particular package, you need to do only the
 * following:
 * <ul>
 * <li>implement a constructor that builds the list of classes via repeated
 * calls to the method <code>add(Class)</code>.
 * <li>implement the abstract methods
 * <code>getDeclaredField(String, Class)</code> and
 * <code>getValueFromField(Field)</code>.
 * </ul>
 * 
 * @see ComponentInfo
 */
public abstract class AbstractPackageInfo {
	protected Vector classes = null;
	
	/**
	 * The name of the field that should exist in every class that contains
	 * the String value of the SCM information for that class. The specified
	 * field should have package access, it needs to be more than private
	 * but public isn't necessary.
	 */
	public static final String FIELD_NAME = "SCM_REVISION"; //$NON-NLS-1$
	
    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    public AbstractPackageInfo() {
    	this.classes = new Vector();
    }
    
    protected void add(Class cl) {
    	this.classes.add(cl);
    }
    
    /**
	 * Common code for all subclasses to build the text that represents the SCM
	 * information for all the classes in this package.
	 * 
	 * @return a text representation of the SCM information for all the classes
	 *         in this package
	 */
	String getInfo() {
		StringBuffer resultBuffer = new StringBuffer();

		String packageName = null;
		if (this.classes.size() > 0) {
			Class cl = (Class) this.classes.get(0);
			packageName = cl.getPackage().getName();
		}
		resultBuffer.append("Package " + packageName + " (" + this.classes.size() + " classes): "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		for (int i=0; i<this.classes.size(); i++) {
			// look for the revision data member

			Field field = null;
			Class cl = (Class) this.classes.get(i);
			try {
				field = this.getDeclaredField(FIELD_NAME, cl);
			} catch (Exception e) {
				resultBuffer.append("\n  (problem getting member in class " //$NON-NLS-1$
						+ cl.getName() + ": " + e); //$NON-NLS-1$
				field = null;
				continue;
			}
			Object object = null;
			if (field == null) {
				resultBuffer.append("\n  (unable to find member in class " + cl.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				try {
					object = this.getValueFromField(field);
				} catch (IllegalArgumentException e) {
					object = null;
				} catch (IllegalAccessException e) {
					object = null;
				}
				if ((object != null) && (object instanceof String)) {
					String value = (String) object;
					resultBuffer.append("\n  "); //$NON-NLS-1$
					String name = cl.getName();
					resultBuffer.append("  "); //$NON-NLS-1$
					resultBuffer.append(name);
					resultBuffer.append(": "); //$NON-NLS-1$
					resultBuffer.append(value);
				} else if (object == null) {
					resultBuffer.append("\n  (null member in class " + cl.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (!(object instanceof String)) {
					resultBuffer.append("\n  (member is not instanceof String in class " + cl.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					resultBuffer.append("\n  (unknown problem with member in class " + cl.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		String result = resultBuffer.toString();
		return(result);
	}

	/**
	 * This method is needed because the invocation of Class.getDeclaredField
	 * needs to be done in a child class that it in the same package. Running
	 * Class.getDeclaredField in this class would not be allowed to access any
	 * fields with package-level access that are in a different package.
	 * 
	 * @param fieldName
	 *            the name of the Field to get from the Class
	 * @param cl
	 *            the Class from which to get the Field
	 * @return the Field represented by the name
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	abstract protected Field getDeclaredField(String fieldName, Class cl)
	throws SecurityException, NoSuchFieldException;
	
	/**
	 * This method is needed because the invocation of Field.get needs to be
	 * done in a child class that is in the same package. Running Field.get in
	 * this class would not be allow allowed to access any fields with
	 * package-level access that are in a different package.
	 * 
	 * @param field
	 *            the Field from which to get the value
	 * @return the value of the field
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	abstract protected Object getValueFromField(Field field)
	throws IllegalArgumentException, IllegalAccessException;
}
