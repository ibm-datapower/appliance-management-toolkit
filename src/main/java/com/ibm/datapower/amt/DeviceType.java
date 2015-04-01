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


/**
 * This class provides a set of enumerated values for the device type, as
 * opposed to using String values. You should use the public fields and the
 * methods included here.
 * <p>
 * The device type describes generally the capabilities of the appliance based
 * on the software. In general, the XS40 is a superset of the XS35, and the XI50
 * is a superset of the XS40. Also be aware that there may be more DeviceTypes
 * than the enums listed here, as this class supports creation of DeviceTypes
 * not known apriori. The enums are here only as a convenience. Be sure
 * to use the {@link #equals(DeviceType)} method and not the <code>==</code>
 * operator when comparing for equality.
 * <p>
 * The descriptions in this class were NOT NLS enabled, because they are
 * unlikely to ever be internationalized. The descriptions are copied
 * directly from the device metadata, 
 * see {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower.amt.amp.DeviceContext)}.
 * <p>
 * 
 * @see com.ibm.datapower.amt.clientAPI.DeviceTypeIncompatibilityException
 * @version SCM ID: $Id: DeviceType.java,v 1.4 2011/04/01 15:08:57 wjong Exp $
 */
//* Created on Aug 29, 2006
public class DeviceType {

    private String description = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    static final String SCM_REVISION = "$Revision: 1.4 $";

    /**
     * A device that is an XA35.
     */
    public static final DeviceType XA35 = new DeviceType("XA35"); //$NON-NLS-1$

    /**
     * A device that is an XM70.
     */
    public static final DeviceType XM70 = new DeviceType("XM70"); //$NON-NLS-1$

    /**
     * A device that is an XS40.
     */
    public static final DeviceType XS40 = new DeviceType("XS40"); //$NON-NLS-1$

    /**
     * A device that is an XI50.
     */
    public static final DeviceType XI50 = new DeviceType("XI50"); //$NON-NLS-1$

    /**
     * A device that is an XI52 
     */
    // XI52 is a 9005 2U
    public static final DeviceType XI52 = new DeviceType("XI52"); //$NON-NLS-1$

    /**
     * A device that is an XB50.
     */
    public static final DeviceType XB50 = new DeviceType("XB50"); //$NON-NLS-1$

    /**
     * A device that is an XB52 
     */
    // XB52 is a 9005 2U
    public static final DeviceType XB52 = new DeviceType("XB52"); //$NON-NLS-1$

    /**
     * A device that is an XE82 
     */
    public static final DeviceType XE82 = new DeviceType("XE82"); //$NON-NLS-1$
    
    /**
     * A device that is an XG45
     */
    public static final DeviceType XG45 = new DeviceType("XG45"); //$NON-NLS-1$

    /**
     * A device that is an XG45
     */
    public static final DeviceType XC10 = new DeviceType("XC10"); //$NON-NLS-1$
    
    /**
     * A device that is an IDG
     */
    public static final DeviceType IDG = new DeviceType("IDG"); //$NON-NLS-1$
    
    /**
     * Create a new DeviceType object. The use of {@link #fromString(String)} is
     * preferred, because it will attempt to reuse objects for known Strings.
     * 
     * @param description
     *            the text identifier of the device type, such as "XS40".
     */
    public DeviceType(String description) {
        // make sure not null, trim whitespace and convert to uppercase
        String cleaned = cleanDescription(description);
        this.description = cleaned;
    }

    /**
     * Compare two DeviceType objects to see if they are equivalent.
     * 
     * @param that
     *            the other DeviceType object to compare to "this".
     * @return true if the objects are equivalent, false otherwise
     * @see #isCompatibleWith(DeviceType)
     */    
    public boolean equals(Object that) {
        if (!(that instanceof DeviceType)) return false;
        return (this.description.equalsIgnoreCase(((DeviceType)that).description));
    }
   
    public int hashCode(){
        return this.description.hashCode();
    }
    
    /**
     * Check if two DeviceType objects are compatible with each other. This is 
     * used to determine if a firmware can be deployed to this Device.
     * 
     * @param that
     *            the other DeviceType to compare to "this"
     * @return true if the objects are compatible with each other, false
     *         otherwise
     * @see #equals(DeviceType)
     */
    public boolean isCompatibleWith(DeviceType that) {
        boolean result = false;
        if (this.equals(that)) {
            // always compatible with same
            result = true;
        }
        else if (this.equals(DeviceType.XI50) || this.equals(DeviceType.XI52)){
        	// XI50 is compatible with XI52
        	result = (that.equals(DeviceType.XI50) || 
        			that.equals(DeviceType.XI52));
        }
        else if (this.equals(DeviceType.XB50) || this.equals(DeviceType.XB52)){
        	// XB50 is compatible with XB52
        	result = (that.equals(DeviceType.XB50) || 
        			that.equals(DeviceType.XB52));
        }
        else {
        	result = false;
        }
        return (result);
    }

    /**
     * Get a human-readable String representation of this object. This is more
     * for debugging than for end-user consumption.
     * 
     * @return a human-readable String representation of this object
     * @see #getDisplayName()
     */
    public String toString() {
        String result = "DeviceType[" + this.description + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        return (result);
    }

    /**
     * Get a String description of this object that is suitable for display to
     * an end user.
     * 
     * This was NOT NLS enabled because the DeviceTypes descriptions are
     * constants
     * 
     * @return a String description of this object that is suitable for display
     *         to an end user.
     * @see #toString()
     */
    public String getDisplayName() {
        return (this.description);
    }

    /**
     * Get a DeviceType instance based on the human-readable String
     * representation of the instance.
     * 
     * @param dirtyString
     *            A String previously returned by DeviceType.toString()
     * @return An instance of DeviceType that would return the value
     *         deviceTypeString if toString was called on it. The benefit of
     *         using this method over {@link #DeviceType(String)} is that this
     *         method will attempt to reuse instances if they are available. If
     *         the String does not exist in a known enum, then it will
     *         instantiate a new object.
     */
    public static final DeviceType fromString(String dirtyString) {
        String cleanString = cleanDescription(dirtyString);
        if (cleanString.equals(XI50.getDisplayName())) {
            return XI50;
        } else if (cleanString.equals(XI52.getDisplayName())) {
            return XI52;
        } else if (cleanString.equals(XB50.getDisplayName())) {
            return XB50;
        } else if (cleanString.equals(XB52.getDisplayName())) {
            return XB52;
        } else if (cleanString.equals(XE82.getDisplayName())) {
            return XE82;
        } else if (cleanString.equals(XS40.getDisplayName())) {
            return XS40;
        } else if (cleanString.equals(XA35.getDisplayName())) {
            return XA35;
        } else if (cleanString.equals(XM70.getDisplayName())) {
            return XM70;
        } else if (cleanString.equals(XG45.getDisplayName())) {
            return XG45;
        } else if (cleanString.equals(XC10.getDisplayName())) {
            return XC10;
        } else {
            DeviceType result = new DeviceType(cleanString); 
            return(result);
        }
    }
    
    private static String cleanDescription(String dirtyDescription) {
        // make sure not null, trim whitespace and convert to uppercase
        String d1 = null;
        if (dirtyDescription == null) {
            d1 = "";
        } else {
            d1 = dirtyDescription;
        }
        String d2 = d1.trim();
        String d3 = d2.toUpperCase();
        return(d3);
    }

}
