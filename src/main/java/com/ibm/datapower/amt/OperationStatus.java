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

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
/**
 * This class defines valid values tracking the operation status of elements
 * such as domains, and containers to hold these values. It also provides helper
 * methods for aggregation of operation status to higher levels (i.e., a single
 * value for a device or managed set). The color mapping for these states could
 * be "up=green", "partial=yellow", "unknown=grey", "down=red".
 * <p>
 * An OperationStatus has no function. Setting its value does not accomplish
 * anything. The OperationStatus is set by the heartbeat daemon and other tasks
 * that probe the device for the operation status of domains. The
 * OperationStatus is to be used for display only. The manager does not manage the
 * operation status, except for the manually-initiated ability to stop and start
 * domains. The manager only reads the domain status from the device.
 * <p>
 * Inside the device, individual configuration objects (all the way from domains
 * to match rules and below) have an <cite>op-state </cite>, which is an
 * abbreviation for "operation state". The two valid values inside the device
 * for the op-state are "enabled" and "disabled". A "disabled" op-state could be
 * caused by manually disabling a configuration object, or it could be caused by
 * error such as a network port conflict or a missing required file resource for
 * a configuration object. The device also provides the op-state setting for a
 * domain - disabling a domain disables all the services in that domain.
 * <p>
 * AMP provides a way to query the device for the operation status of a domain,
 * which is not the boolean enabled/disabled op-state of the domain
 * configuration object itself, but instead is a rollup of the op-states of all
 * the services in the domain - this means that the rollup of all the services
 * could result in a non-boolean value such as "partial".
 * <p>
 * Although the device native op-state can refer to any configuration object,
 * the OperationStatus refers only to domains. OperationStatus does not
 * apply to firmware or settings.
 * <p>
 * This class will be used both by the clientAPI (task-level API) and the device
 * communication layer (AMP). Consumers of the clientAPI should treat these
 * objects as read-only, and invoke only the gettr methods. The only threads to
 * call the settr methods should be internal threads.
 * <p>
 * @version SCM ID: $Id: OperationStatus.java,v 1.3 2010/08/23 21:20:28 burket Exp $
 * <p>
 */
//* Created on Jun 21, 2006
public class OperationStatus {
    private volatile Enumerated currentStatus = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    /**
     * Create a status object that can hold an enumerated state. There are a
     * finite/constrined set of enumerated state singletons, but you can create
     * an inifinite number of objects to hold the state value.
     * 
     * @param currentStatus the enumerated current status.
     */
    public OperationStatus(Enumerated currentStatus) {
        this.currentStatus = currentStatus; 
    }
    
    /**
     * Set the value of this status object to the new enumerated value. This
     * should be called only by threads internal to the manager. It has the
     * <code>public</code> access modifier because it needs to be invoked from
     * multiple packages.
     * 
     * @param newStatus the enumerated new status
     */
    public void setStatus(Enumerated newStatus) {
        this.currentStatus = newStatus;
    }
    
    /**
     * Retrieve the enumerated current status.
     * 
     * @return the enumerated current status.
     */
    public Enumerated getEnumerated() {
        return(this.currentStatus);
    }
    
    /**
     * Since the status can be one of four values, this method checks if the
     * status has the enumerated "up" value.
     * 
     * @return true if the status is "up", false otherwise
     * @see #isPartiallyUp()
     * @see #isDown()
     * @see #isUnknown()
     */
    public boolean isUp() {
        boolean result = (currentStatus.equals(Enumerated.UP));
        return(result);
    }
    
    /**
     * Since the status can be one of four values, this method checks if the
     * status has the enumerated "partial" value.
     * 
     * @return true if the status is "partial", false otherwise
     * @see #isUp()
     * @see #isDown()
     * @see #isUnknown()
     */
    public boolean isPartiallyUp() {
        boolean result = (currentStatus.equals(Enumerated.PARTIAL));
        return(result);
    }
    
    /**
     * Since the status can be one of four values, this method checks if the
     * status has the enumerated "unknown" value.
     * 
     * @return true if the status is "unknown", false otherwise
     * @see #isUp()
     * @see #isDown()
     * @see #isPartiallyUp()
     */
    public boolean isUnknown() {
        boolean result = (currentStatus.equals(Enumerated.UNKNOWN));
        return(result);
    }
    
    /**
     * Since the status can be one of four values, this method checks if the
     * status has the "down" value.
     * 
     * @return true if the status is "down", false otherwise
     * @see #isUp()
     * @see #isPartiallyUp()
     * @see #isUnknown()
     */
    public boolean isDown() {
        boolean result = (currentStatus.equals(Enumerated.DOWN));
        return(result);
    }
    
    /**
     * This contains the logic to walk through multiple lower-level status
     * values to determine what the aggregated higher-level status value is. The
     * "this" object is where the aggregated higher-level status is stored.
     * <p>
     * A bottom-level element will have status of either up or down or unknown.
     * A higher-level status is defined as an aggregation of multiple
     * lower-level status items. If all the lower-level status are "up", then
     * the higher-level status is "up" also. If all the lower-level status are
     * "down", then the higher-level status is "down" also. If all the
     * lower-level status are "unknown", then the higher-level status is also
     * "unknown". If there is a mix of "up" and "down" and possibly "unknown"
     * status in the lower-level, then the higher-level status is "partial". The
     * color mapping for these states could be "up=green", "partial=yellow",
     * "unknown=grey", "down=red".
     * 
     * @param lowerLevelStati the lower-level status items to merge into the
     *        aggregated higher-level status that the <code>this</code> object
     *        represents.
     */
    public void rollupFrom(OperationStatus[] lowerLevelStati) {
        boolean hasUp = false;
        boolean hasPartial = false;
        boolean hasUnknown = false; 
        boolean hasDown = false;
        if (lowerLevelStati.length == 0) {
            hasUnknown = true;
        }
        for (int i=0; i<lowerLevelStati.length; i++) {
            if (lowerLevelStati[i].currentStatus.equals(Enumerated.UP)) {
                hasUp = true;
            } else if (lowerLevelStati[i].currentStatus.equals(Enumerated.PARTIAL)) {
                hasPartial = true;
            } else if (lowerLevelStati[i].currentStatus.equals(Enumerated.UNKNOWN)) {
                hasUnknown = true;
            } else if (lowerLevelStati[i].currentStatus.equals(Enumerated.DOWN)) {
                hasDown = true;
            }
        }
        if (hasUp && !hasPartial && !hasDown && !hasUnknown) {
            // everything is up
            this.setStatus(Enumerated.UP);
        } else if (hasDown && !hasPartial && !hasUp && !hasUnknown) {
            // everything is down
            this.setStatus(Enumerated.DOWN);
        } else if (hasUnknown && !hasPartial && !hasDown && !hasUp) {
            // everything is unknown
            this.setStatus(Enumerated.UNKNOWN);
        } else {
            this.setStatus(Enumerated.PARTIAL);
        }
    }
    
    /**
     * Convert a collection to an array. Since the
     * {@link #rollupFrom(OperationStatus[])} method takes an array as a
     * parameter, this method may be helpful for converting from an intermediate
     * collection to a type which you can call that method.
     * 
     * @param operationStatusCollection a collection that contains
     *        OperationStatus objects.
     * @return an array of OperationStatus objects.
     */
    public static OperationStatus[] toOperationStatusArray(Collection operationStatusCollection) {
        // convert an Object[] to a OperationStatus[]. Can't do a simple cast
        OperationStatus[] result = new OperationStatus[operationStatusCollection.size()];
        Iterator iterator = operationStatusCollection.iterator();
        for (int i=0; iterator.hasNext(); i++) {
            OperationStatus operationStatus = (OperationStatus) iterator.next();
            result[i] = operationStatus;
        }
        return(result);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing or for a user interface.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing or for a user interface.
     */
    public String toString() {
        return(this.currentStatus.toString());
    }
    
    /**
     * Get a key that represents the description of the current OperationStatus object. This key may be
     * used in nls enabled user interfaces.
     * 
     * @return a key that represents the human-readable string that describes
     *         this object.
     */
    public String getDisplayNameKey() {
        return this.currentStatus.descriptionKey;
    }
    
    /**
     * Get a human-readable name that represents the current OperationStatus
     * object. This name may be used in user interfaces.
     * 
     * @return a human-readable name that represents this object.
     */
    public String getDisplayName() {
        return getDisplayName(Locale.getDefault());
    }
    
    /**
     * Get a human-readable name that represents the current OperationStatus
     * object. This name may be used in user interfaces.
     * 
     * @param locale
     *            The locale to be used in getting the human-readable name
     * 
     * @return a human-readable name that represents this object.
     */
    public String getDisplayName(Locale locale) {
        String result = Messages.getNonMsgString(this.currentStatus.descriptionKey, locale);
        return(result);
    }
    
    /**
     * This holds the enumerated values that are allowed to be used with
     * {@link OperationStatus}.
     * 
     */
    public static class Enumerated {
        private int intValue;
        private String description = null;
        private String descriptionKey = null;  //key for nls enabled description
        
        /**
         * The item is up and running fine (i.e., "green")
         */
        public static final Enumerated UP = new Enumerated(0, "up", "OperationalStatus.up"); //$NON-NLS-1$ //$NON-NLS-2$
        
        /**
         * The item is partially up but not completely up (i.e., "yellow")
         */
        public static final Enumerated PARTIAL = new Enumerated(1, "partial", "OperationalStatus.partial"); //$NON-NLS-1$ //$NON-NLS-2$
        
        /**
         * The state of the item is not known (i.e., "grey"). This may be
         * because we have not been able to retrieve the state of the item yet
         * or have lost contact with the device.
         */
        public static final Enumerated UNKNOWN = new Enumerated(2, "unknown", "OperationalStatus.unknown"); //$NON-NLS-1$ //$NON-NLS-2$
        
        /**
         * The item is down (i.e., "red")
         */
        public static final Enumerated DOWN = new Enumerated(3, "down", "OperationalStatus.down"); //$NON-NLS-1$ //$NON-NLS-2$
        
        private Enumerated(int value, String description, String descriptionKey) {
            this.intValue = value;
            this.description = description;
            this.descriptionKey = descriptionKey;
        }
        
        /**
         * Check if <code>this</code> is equivalent to <code>that</code>.
         * 
         * @param that the other enumerated value to use in the comparison.
         * @return true if the two enumerated values are equal, false otherwise
         */
        public boolean equals(Object that) {
            if (!(that instanceof Enumerated)) return false;
            return (this.intValue == ((Enumerated)that).intValue);
        }
       
        public int hashCode(){
            return this.intValue;
        }
        
        /**
         * Check if <code>this</code> is considered more serious (worse) than
         * <code>that</code>. The following heirarchy of enumerations is used
         * to rank severity (each of the following expressions evaluate to
         * <code>true</code>):
         * <ul>
         * <li><code>PARTIAL.isMoreSeriousThan(UP)</code></li>
         * <li><code>UNKNOWN.isMoreSeriousThan(PARTIAL)</code></li>
         * <li><code>DOWN.isMoreSeriousThan(UNKNOWN)</code></li>
         * </ul>
         * Or described linearly:
         * <code>UP -&gt; PARTIAL -&gt; UNKNOWN -&gt; DOWN</code>
         * 
         * @param that the other value to be used in the comparison
         * @return true if <code>this</code> is more serious than
         *         <code>that</code>, false otherwise.
         */
        public boolean isMoreSeriousThan(Enumerated that) {
            boolean result = (this.intValue > that.intValue);
            return(result);
        }
        
        /**
         * Get a key that represents the description of  object. This key may be
         * used in nls enabled user interfaces.
         * 
         * @return a key that represents the human-readable string that describes
         *         this object.
         */
        public String getDisplayNameKey() {
            return this.descriptionKey;
        }
        
        /**
         * Get a human-readable name that represents this object. This name may be
         * used in user interfaces.
         * 
         * @return a human-readable name that represents this object.
         */
        public String getDisplayName() {
            return getDisplayName(Locale.getDefault());
        }
        
        /**
         * Get a human-readable name that represents this object. This name may be
         * used in user interfaces.
         * @param locale The locale to be used in getting the human-readable name
         * 
         * @return a human-readable name that represents this object.
         */
        public String getDisplayName(Locale locale) {
            String result = Messages.getNonMsgString(descriptionKey, locale);
            return(result);
        }
        
        /**
         * Get a String representation of this object for the purpose of debugging
         * or tracing or for a user interface.
         * 
         * @return a String representation of this object for the purpose of
         *         debugging or tracing or for a user interface.
         */
        public String toString() {
            return(this.description);
        }
       
    }
}
