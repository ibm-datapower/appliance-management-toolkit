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



package com.ibm.datapower.amt.clientAPI;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Capture the management status (i.e., synced, in progress, etc.) for a managed
 * element (firmware) on a device. 
 * <p>
 * A ManagementStatus has no function, it is to be used only for display.
 * Setting a ManagementStatus does not do anything. The ManagementStatus is set
 * while management operations occur. It is
 * to be used as an indicator of management function.
 * <p>
 * This class defines valid values for and provides helper methods for
 * aggregation of management status to higher levels. See the inner class
 * <code>Enumerated</code> for a list of the possible values in this status.
 * When you read through those list of values it should give you a better idea
 * of what this class is trying to do. 
 * <p>
 * @version SCM ID: $Id: ManagementStatus.java,v 1.5 2011/04/14 16:23:54 lsivakumxci Exp $
 * <p>
 */
public class ManagementStatus {
    
    private volatile Enumerated currentStatus = null;
    // an array is needed if this is a rollup.
    private volatile Vector exceptions = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = ManagementStatus.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    /**
     * Create an object to store the ManagementStatus of an item, using the
     * specified initial value.
     * 
     * @param currentStatus the initial management status of the item that is
     *        being managed.
     */
    public ManagementStatus(Enumerated currentStatus) {
        logger.log(Level.FINER, "creating with " + currentStatus); //$NON-NLS-1$
        this.currentStatus = currentStatus; 
        this.exceptions = new Vector();
    }
    
    /**
     * Change the management status of the managed item to the new status. This
     * should be called only by threads internal to the manager. It has the
     * <code>public</code> access modifier because it needs to be invoked from
     * multiple packages.
     * 
     * @param newStatus the new management status of the item that is being
     *        managed.
     */
    public void setStatus(Enumerated newStatus) {
        logger.log(Level.FINER, "setting status to " + newStatus); //$NON-NLS-1$
        this.currentStatus = newStatus;
    }
    
    /**
     * Get the current management status of the item that is being managed.
     * 
     * @return the current management status of the item that is being managed.
     */
    public Enumerated getEnum() {
        return(this.currentStatus);
    }
    
    /**
     * Add an Exception to the ManagementStatus to keep a list of problems that
     * may have been encountered.
     * 
     * @param newException a problem that was encountered during management
     */
    public void addException(Exception newException) {
        logger.log(Level.FINER, "adding exception: " + newException); //$NON-NLS-1$
        this.exceptions.add(newException);
    }
    
    /**
     * Get the list of exceptions that may have been encountered during
     * management.
     * 
     * @return the list of exceptions that may have been encountered during
     *         management.
     */
    public Exception[] getExceptions() {
        Exception[] result = null;
        if ((currentStatus.equalsTo(Enumerated.ERROR)) ||
                (currentStatus.equalsTo(Enumerated.UNREACHABLE))) {
            int size = this.exceptions.size();
            result = new Exception[size];
            for (int i=0; i<size; i++) {
                result[i] = (Exception) this.exceptions.get(i);
            }
        } else {
            result = new Exception[0];
        }
        return(result);
    }
    
    /**
     * This contains the logic to walk through multiple lower-level status
     * values to determine what the aggregated higher-level status value is.
     * After this method completes, the <code>this</code> object contains the
     * higher-level aggregated status, which is dependent on the lower-level
     * status. The rollup status in <code>this</code> is the highest value
     * from the lower-level statuses, which means this object will be
     * overwritten, but the parameters will only be read.
     * 
     * @param lowerLevelStati the lower-level statuses to merge into the
     *        aggregated higher-level status that the <code>this</code> object
     *        represents.
     */
    public void rollupFrom(ManagementStatus[] lowerLevelStati) {
        logger.entering(CLASS_NAME, "rollupFrom"); //$NON-NLS-1$
        // initialize with the lowest seriousness
        this.currentStatus = Enumerated.SYNCED;
        this.exceptions.clear();
        
        for (int i=0; i<lowerLevelStati.length; i++) {
        	if (lowerLevelStati[i] != null){
        		if (lowerLevelStati[i].currentStatus.isMoreSeriousThan(this.currentStatus)) {
        			logger.log(Level.FINE, "found more serious status: " + //$NON-NLS-1$
        					lowerLevelStati[i].currentStatus);
        			this.currentStatus = lowerLevelStati[i].currentStatus;
        		}
        		if ((lowerLevelStati[i].currentStatus.equalsTo(Enumerated.ERROR)) &&
        				(lowerLevelStati[i].exceptions.size() != 0)) {
        			for (int j=0; j<lowerLevelStati[i].exceptions.size(); j++) {
        				Exception exception = (Exception) lowerLevelStati[i].exceptions.get(j); 
        				logger.log(Level.FINE, "adding exception: " + exception); //$NON-NLS-1$
        				this.exceptions.add(exception);
        			}
        		}
        	}
        }
        logger.exiting(CLASS_NAME, "rollupFrom", this.currentStatus); //$NON-NLS-1$
    }
    
    static ManagementStatus[] toManagementStatusArray(Collection managementStatusCollection) {
        // convert an Object[] to a ManagementStatus[]. Can't do a simple cast
        ManagementStatus[] result = new ManagementStatus[managementStatusCollection.size()];
        Iterator iterator = managementStatusCollection.iterator();
        for (int i=0; iterator.hasNext(); i++) {
            ManagementStatus managementStatus = (ManagementStatus) iterator.next();
            result[i] = managementStatus;
        }
        return(result);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "ManagementStatus[" + this.currentStatus.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        return(result);
    }
    
    /**
     * Get a key that represents the description of this object. This key may be
     * used in nls enabled user interfaces.
     * 
     * @return a key that represents the human-readable string that describes
     *         this object.
     */
    public String getDisplayNameKey() {
        return this.currentStatus.descriptionKey;
    }
    
    /**
     * Get a human-readable name that represents this object, using the default
     * locale This name may be used in user interfaces.
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
        String result = Messages.getNonMsgString(this.currentStatus.descriptionKey, locale);
        return(result);
    }
    
    /**
     * The possible values for {@link ManagementStatus}.
     * <p>
     * The values in order from least serious to most serious are:
     * <ul>
     * <li>{@link #SYNCED}
     * <li>{@link #PENDING}
     * <li>{@link #IN_PROGRESS}
     * <li>{@link #UNKNOWN}
     * <li>{@link #ERROR}
     * <li>{@link #UNREACHABLE}
     * </ul>
     */
    public static class Enumerated {
        private int intValue;
        private String description = null;
        private String descriptionKey = null;  //key to access nls enabled descriptions
        
        /**
         * The element is synchronized to the desired values.
         */
        public static final Enumerated SYNCED = new Enumerated(0, "synced", "ManagementStatus.synced"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * An action to perform synchronization is pending (queued to occur),
         * but the worker thread has not started performing it yet.
         */
        public static final Enumerated PENDING = new Enumerated(1, "changes pending", "ManagementStatus.changesPending"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * The element is currently being synchronized.
         */
        public static final Enumerated IN_PROGRESS = new Enumerated(2, "in_progress", "ManagementStatus.inProgress"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * It is not know if the element is synchronized or not. The likely cause
         * here is that the Manager has not yet contacted the device to get
         * its management status.
         */
        public static final Enumerated UNKNOWN = new Enumerated(3, "unknown", "ManagementStatus.unknown"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * An attempt to synchronize the element failed, due to a reason on the
         * device. The device responded, but it responded with an error. 
         */
        public static final Enumerated ERROR = new Enumerated(4, "error", "ManagementStatus.error"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * The device is not responding to management queries or requests.
         */
        public static final Enumerated UNREACHABLE = new Enumerated(5, "unreachable", "ManagementStatus.unreachable"); //$NON-NLS-1$ //$NON-NLS-2$

        private Enumerated(int value, String description, String descriptionKey) {
            this.intValue = value;
            this.description = description;
            this.descriptionKey = descriptionKey;
        }
        
        /**
         * Compare two objects to see if they are equivalent.
         * 
         * @param that the other object to compare to <code>this</code>
         * @return true if the two objects are equivalent, false otherwise
         */
        public boolean equalsTo(Enumerated that) {
            boolean result = (this.intValue == that.intValue);
            return(result);
        }
                
        /**
         * Check if the the parameter has a more serious value than
         * <code>this</code>
         * 
         * @param that the status to check to see if it is more serious than
         *        <code>this</code>. For a ranking of values based on
         *        seriousness, refer to the javadoc at the top of this class.
         * @return true if <code>that</code> is more serious than
         *         <code>this</code>, false otherwise
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
         * Get a String representation of this object for the purpose of
         * debugging or tracing or user interfaces.
         * 
         * @return a String representation of this object for the purpose of
         *         debugging or tracing or user interfaces.
         */
        public String toString() {
            return(this.description);
        }
        
    }
}
