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
 * This class provides a set of enumerated values for the model type, as opposed
 * to using String values. You should use the public fields and the methods
 * included here.
 * <p>
 * The model describes the hardware version of the appliance. Also be aware that
 * there may be more ModelTypes than the enums listed here, as this class
 * supports creation of ModelTypes not known apriori. The enums are here only
 * as a convenience. Be sure to use the {@link #equals(ModelType)} method and
 * not the <code>==</code> operator when comparing for equality.
 * <p>
 * The descriptions in this class were NOT NLS enabled, because they are
 * unlikely to ever be internationalized. The descriptions are copied directly
 * from the device metadata, see
 * {@link com.ibm.datapower.amt.amp.Commands#getDeviceMetaInfo(com.ibm.datapower.amt.amp.DeviceContext)}.
 * <p>
 * 
 * @see com.ibm.datapower.amt.clientAPI.ModelTypeIncompatibilityException
 * @version SCM ID: $Id: ModelType.java,v 1.3 2011/03/15 15:57:47 wjong Exp $
 * <p>
 * 
 */
//* Created on Oct 30, 2006
public class ModelType {
    private String description = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$

    /**
     * A model that is a 9001.
     */
    public static final ModelType TYPE_9001 = new ModelType("9001"); //$NON-NLS-1$

    /**
     * A model that is a 9002.
     */
    public static final ModelType TYPE_9002 = new ModelType("9002"); //$NON-NLS-1$

    /**
     * A model that is a 9003.
     */
    public static final ModelType TYPE_9003 = new ModelType("9003"); //$NON-NLS-1$

    /**
     * A model that is a 9235.
     */
    public static final ModelType TYPE_9235 = new ModelType("9235"); //$NON-NLS-1$

    /**
     * A model that is a 7198.
     */
    // 9005 1U
    public static final ModelType TYPE_7198 = new ModelType("7198"); //$NON-NLS-1$

    /**
     * A model that is a 7199.
     */
    // 9005 2U
    public static final ModelType TYPE_7199 = new ModelType("7199"); //$NON-NLS-1$

    /**
     * A special model type used ONLY for firmware that indicates the firmware
     * supports a 7198 or 7199. Firmware appears to be binary compatible across
     * the 7198 or 7199.
     */
    public static final ModelType TYPE_9005 = new ModelType("9005"); //$NON-NLS-1$

    /**
     * A special model type used ONLY for firmware that indicates the firmware
     * supports a 9001 or 9002. Firmware appears to be binary compatible across
     * the 9001 and 9002.
     */
    public static final ModelType TYPE_OTHER = new ModelType("9002 or 9001"); //$NON-NLS-1$
    
    /**
     * A model that is a virtual appliance that will specifically run on VMWare
     */
    public static final ModelType TYPE_5725 = new ModelType("5725"); //$NON-NLS-1$
    
    /**
     * A model that is a generic virtual appliance that will run on any hypervisor
     */
    public static final ModelType TYPE_Virtual = new ModelType("virtual"); //$NON-NLS-1$
    
    /**
     * A model that is 4195
     */
    public static final ModelType TYPE_4195 = new ModelType("4195"); //$NON-NLS-1$

    /**
     * A model that is 9006
     */
    public static final ModelType TYPE_9006 = new ModelType("9006"); //$NON-NLS-1$
    
    /**
     * A model that is 8436
     */
    public static final ModelType TYPE_8436 = new ModelType("8436"); //$NON-NLS-1$

    /**
     * Create a new ModelType object. The use of {@link #fromString(String)} is
     * preferred, because it will attempt to reuse objects for known Strings.
     * 
     * @param description
     *            the text identifier of the model type, such as "9003".
     */
    public ModelType(String description) {
        String cleanDescription = cleanString(description);
        this.description = cleanDescription;
    }

    /**
     * Compare two ModelType objects to see if they are equal. 
     * In the context of this method, "equal" means exactly the same.
     * Even though a 9235 is compatible with a 9003, it is not equal. 
     * If you want to check for compatibility instead of being exactly
     * the same, then you should use {@link #isCompatibleWith(ModelType)}.
     * 
     * @param that
     *            the other ModelType object to compare to <code>this</code>.
     * @return true if the objects are equivalent, false otherwise
     * @see #isCompatibleWith(ModelType)
     */
    public boolean equals(Object that) {
        if (!(that instanceof ModelType)) return false;
        
        boolean result = false;
        if (this.description.equals(TYPE_OTHER.description)) {
            result = ((((ModelType)that).description.equals(TYPE_OTHER.description)) ||
                    (((ModelType)that).description.equals(TYPE_9001.description)) ||
                    (((ModelType)that).description.equals(TYPE_9002.description)));
        } else if (this.description.equals(TYPE_9001.description)) {
            result = ((((ModelType)that).description.equals(TYPE_OTHER.description)) ||
                    (((ModelType)that).description.equals(TYPE_9001.description)));
        } else if (this.description.equals(TYPE_9002.description)) {
            result = ((((ModelType)that).description.equals(TYPE_OTHER.description)) ||
                    (((ModelType)that).description.equals(TYPE_9002.description)));
        } else {
            // neither is TYPE_OTHER...do straight comparison
            result = this.description.equals(((ModelType)that).description);
        }
        return (result);
    }
   
    public int hashCode(){
        return this.description.hashCode();
    }
    
    /**
     * Check if two ModelType objects are compatible with each other. This 
     * is used to check if the firmware ModelType can be deployed to this Device.
     * <p>
     * For the most part, "compatible" means {@link #equals(ModelType)}.
     * However, a 9003 is not equal to a 9235, but they are compatible.
     * 
     * @param that
     *            other ModelType object to compare to <code>this</code>.
     * @return true if the objects are compatible with each other, false
     *         otherwise
     * @see #equals(ModelType)
     */
    public boolean isCompatibleWith(ModelType that) {
        boolean result = false;
        if (this.equals(that)) {
            // always compatible with same
            result = true;
        } else if (this.equals(TYPE_9001) || this.equals(TYPE_9002)) {
            // 9001 or 9002 are compatible with each other
            result = (that.equals(TYPE_9001) || that.equals(TYPE_9002));
        } else if (this.equals(TYPE_9003) || this.equals(TYPE_9235) || (that.equals(TYPE_4195)) ) {
            // 9003, 9235 and 4195 are compatible with each other
            result = (that.equals(TYPE_9003) || 
            		(that.equals(TYPE_9235)  || (that.equals(TYPE_4195)) ));
        } else if (this.equals(TYPE_7198) || this.equals(TYPE_7199) || this.equals(TYPE_9005)) {
            // 7198 and 7199 and 9005 are compatible with each other
            result = (that.equals(TYPE_7198) ||
            		(that.equals(TYPE_7199)) ||
            		(that.equals(TYPE_9005)));
        } else if ( this.equals(TYPE_5725)) {
            // 7198 and 7199 and 9005 are compatible with each other
            result = true;
        } else if( this.equals(TYPE_9006) || this.equals(TYPE_8436)) {
        	// 9006 and 8436 are compatible
        	result = that.equals(TYPE_9006) ||
        			that.equals(TYPE_8436);
        } else {
            // assume unknown is newer and compatible with 9005.
            // assume is good as long as it's not a previous model
            result = (!that.equals(TYPE_9001) &&
            		!that.equals(TYPE_9002) &&
            		!that.equals(TYPE_9003) && 
            		!that.equals(TYPE_9235));
        }
        return(result);
    }

    /**
     * Get a human-readable String representation of this object. This is more
     * for debugging than for end-user consumption.
     * 
     * @return a human-readable String representation of this object
     * @see #getDisplayName()
     */
    public String toString() {
        String result = "ModelType[" + this.description + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        return (result);
    }

    /**
     * Get a String description of this object that is suitable for display to
     * an end user.
     * <p>
     * This was NOT NLS enabled because the ModelType descriptions are
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
     * Get a ModelType instance based on the String representation of the
     * instance returned by getDisplayName
     * 
     * @param modelTypeString
     *            A String previously returned by ModelType.getDisplayName()
     * @return An instance of ModelType that would return the value
     *         ModelTypeString if getDisplayName was called on it. null will be
     *         returned if a matching instance can't be found.
     */
    public static ModelType fromString(String modelTypeString) {
        String modelTypeStringFormatted = modelTypeString.trim();
        // TYPE_OTHER contains alphabetic characters, thus the IgnoreCase.
        // firmware headers actually say "other"
        if (modelTypeStringFormatted.equalsIgnoreCase("other")) { //$NON-NLS-1$
            return TYPE_OTHER;
        } else if (modelTypeStringFormatted.equalsIgnoreCase(TYPE_OTHER.getDisplayName())) {
            return TYPE_OTHER;
        } else if (modelTypeStringFormatted.equalsIgnoreCase(TYPE_Virtual.getDisplayName())) {
            return TYPE_5725;
        } else if (modelTypeStringFormatted.equals(TYPE_7198.getDisplayName())) {
            return TYPE_7198;
        } else if (modelTypeStringFormatted.equals(TYPE_7199.getDisplayName())) {
            return TYPE_7199;
        } else if (modelTypeStringFormatted.equals(TYPE_9005.getDisplayName())) {
            return TYPE_9005;
        } else if (modelTypeStringFormatted.equals(TYPE_9235.getDisplayName())) {
            return TYPE_9235;
        } else if (modelTypeStringFormatted.equals(TYPE_9003.getDisplayName())) {
            return TYPE_9003;
        } else if (modelTypeStringFormatted.equals(TYPE_9002.getDisplayName())) {
            return TYPE_9002;
        } else if (modelTypeStringFormatted.equals(TYPE_9001.getDisplayName())) {
            return TYPE_9001;
        } else if (modelTypeStringFormatted.equals(TYPE_5725.getDisplayName())) {
            return TYPE_5725;
        } else if (modelTypeStringFormatted.equals(TYPE_9006.getDisplayName())) {
        	return TYPE_9006;
        } else {
            ModelType result = new ModelType(modelTypeString);
            return(result);
        }
    }

    private static String cleanString(String dirtyString) {
        // check for null, trim whitespace, make upper case
        String d1 = null;
        if (dirtyString == null) {
            d1 = "";
        } else {
            d1 = dirtyString;
        }
        String d2 = d1.trim();
        String d3 = d2.toUpperCase();
        return(d3);
    }
}
