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

import com.ibm.datapower.amt.Constants;

/**
 * An operation was attempted on two objects, and the objects were incompatible
 * with each other. The operation failed. There are multiple causes for
 * incompatibility, so this is a parent class for other types of incompatibility 
 * exception classes. 
 * <p>
 * 
 * @see "direct known subclasses"
 * @version SCM ID: $Id: IncompatibilityException.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
//* Created on Sep 6, 2007
//* the concrete cause should be a child class of this class.
//* So, you may catch exceptions of this class as a shortcut to catching the
//* individual subclasses, but it is recommended to throw only the subclasses
//* because they identify the specific cause of the incompatibility. It is not
//* recommended to create and throw instances of this specific class, you should
//* use the subclasses instead.
//* <p>
//* As of this writing, incompatibilities will occur between two devices (i.e.,
//* coexisting in the same managed set), or between a device and firmware (i.e.,
//* can the firmware run on the device).
public class IncompatibilityException extends ClientAPIException {

    private static final long serialVersionUID = 2L;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Create an exception with no message and no cause. Please use one of the
     * other constructors to provide as much information about the exception as
     * possible. You really shouldn't be using this zero-argument constructor.
     */
    public IncompatibilityException() {
        super();
    }

    /**
     * Create an exception with a message but with no chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception
     */
    public IncompatibilityException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParms an Object[] that gives the attributes to be used with
     *        msgKey
     */
    public IncompatibilityException(String message, String msgKey,
            Object[] msgParms) {
        super(message, msgKey, msgParms);
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParm an Object that gives the attributes to be used with msgKey
     */
    public IncompatibilityException(String message, String msgKey,
            Object msgParm) {
        super(message, msgKey, msgParm);
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     */
    public IncompatibilityException(String message, String msgKey) {
        super(message, msgKey);
    }

    /**
     * Create an exception with a message and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public IncompatibilityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParms an Object[] that gives the attributes to be used with
     *        msgKey
     */
    public IncompatibilityException(String message, Throwable cause,
            String msgKey, Object[] msgParms) {
        super(message, cause, msgKey, msgParms);
    }

    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParm an Object that gives the attribute to be used with msgKey
     */
    public IncompatibilityException(String message, Throwable cause,
            String msgKey, Object msgParm) {
        super(message, cause, msgKey, msgParm);
    }

    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     */
    public IncompatibilityException(String message, Throwable cause,
            String msgKey) {
        super(message, cause, msgKey);
    }

    /**
     * Create an exception with a chained cause and no message.
     * 
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public IncompatibilityException(Throwable cause) {
        super(cause);
    }

}
