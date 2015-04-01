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

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Capture the status of the Manager.
 * <p>
 * A ManagerStatus has no function, it is to be used only for display. Setting a
 * ManagerStatus does not do anything, other than record the current status of
 * the manager. The ManagerStatus is set during the life cycle of each Manager
 * instance.
 * <p>
 * This class defines valid values for the Manager's status. See the inner class
 * <code>Enumerated</code> for a list of the possible values in this status.
 * When you read through those list of values it should give you a better idea
 * of what this class is trying to do. Consumers of the clientAPI should treat
 * these objects as read-only and invoke only the getter methods.
 * <p>
 * There should be an instance of this object for each Manager.
 * <p>
 * <p>
 * The <strong>intended</strong> use of ManagerStatus provides for the
 * following Manager life cycle.
 * <ol>
 * <li>inital state: down
 * <li>starting: Manager has started to initialize
 * <li>up: Manager has completed initialization.  
 * <li>stopping: Manager is shutting down. Will return to
 * down after the shutdown completes. 
 * </ol>
 * @version SCM ID: $Id: ManagerStatus.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
//* <p>
//* Created on Sep 09, 2007
//* 
public class ManagerStatus {
    
    private volatile Enumerated currentStatus = null;
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
    protected static final String CLASS_NAME = ManagerStatus.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }
    
    public final static ManagerStatus MANAGER_NOTEXIST = new ManagerStatus(Enumerated.DOWN);

    /**
     * Create an object to store the ManagerStatus of an item, using the
     * specified initial value.
     * 
     * @param currentStatus the initial management status of the item that is
     *        being managed.
     */
    public ManagerStatus(Enumerated currentStatus) {
        logger.log(Level.FINER, "creating with " + currentStatus); //$NON-NLS-1$
        this.currentStatus = currentStatus; 
    }
    
    /**
     * Change the status of the Manager item to the new status. This
     * should be called only by methods internal to the manager. 
     * 
     * @param newStatus the new status of the Manager instance
     */
   void setStatus(Enumerated newStatus) {
        logger.log(Level.FINER, "setting status to " + newStatus); //$NON-NLS-1$
        this.currentStatus = newStatus;
    }
    
    /**
     * Get the current status of the Manager.
     * 
     * @return the ccurrent status of the Manager.
     */
    public Enumerated getEnum() {
        return(this.currentStatus);
    }
    
//    /**
//     * Add an Exception to the ManagerStatus to keep a list of problems that
//     * may have been encountered.
//     * 
//     * @param newException a problem that was encountered during management
//     */
//    public void addException(Exception newException) {
//        logger.log(Level.FINER, "adding exception: " + newException); //$NON-NLS-1$
//        this.exceptions.add(newException);
//    }
//    
//    /**
//     * Get the list of exceptions that may have been encountered during
//     * management.
//     * 
//     * @return the list of exceptions that may have been encountered during
//     *         management.
//     */
//    public Exception[] getExceptions() {
//        Exception[] result = null;
//        if ((currentStatus.equals(Enumerated.ERROR)) ||
//                (currentStatus.equals(Enumerated.UNREACHABLE))) {
//            int size = this.exceptions.size();
//            result = new Exception[size];
//            for (int i=0; i<size; i++) {
//                result[i] = (Exception) this.exceptions.get(i);
//            }
//        } else {
//            result = new Exception[0];
//        }
//        return(result);
//    }
    

    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "ManagerStatus[" + this.currentStatus.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
     * The possible values for {@link ManagerStatus}.
     * <p>
     * The values in order from least serious to most serious are:
     * <ul>
     * <li>{@link #DOWN}
     * <li>{@link #STARTING}
     * <li>{@link #STOPPING}
     * <li>{@link #UP}
     * </ul>
     */
    public static class Enumerated {
        private int intValue;
        private String description = null;
        private String descriptionKey = null;  //key to access nls enabled descriptions
        
        /**
         * The Manager instance has not been started or has been shutdown
         */
        public static final Enumerated DOWN = new Enumerated(0, "down", "ManagerStatus.down"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * The Manager has started initializing itself
         */
        public static final Enumerated STARTING = new Enumerated(1, "starting", "ManagerStatus.starting"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * The Manager has completed its initialization.  The deamons may still be doing startup related work with the managed devices, etc.
         */
        public static final Enumerated UP = new Enumerated(2, "up", "ManagerStatus.up"); //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * The Manager is stopping its work.  The next Management status will be down.
         */
        public static final Enumerated STOPPING= new Enumerated(3, "stopping", "ManagerStatus.stopping"); //$NON-NLS-1$ //$NON-NLS-2$
 
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
