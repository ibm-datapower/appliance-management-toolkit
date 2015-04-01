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
package com.ibm.datapower.amt.soma;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;

/**
 * The top-level class to identify exceptions that may occur while communicating
 * with DataPower devices. SOMA is the SOAP Configuration Management. You should
 * use the most specific exception class available, so please try to use the
 * subclasses instead of parent classes wherever possible.
 * <p>
 * 
 */
public class SOMAException extends DMgrException {  
	
	private static final long serialVersionUID = 2L;
    
    public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;

    /**
     * Create an exception with no message and no cause. Please use one of the
     * other constructors to provide as much information about the exception as
     * possible. You really shouldn't be using this zero-argument constructor.
     * 
     * @see #SOMAException(String)
     * @see #SOMAException(Throwable)
     * @see #SOMAException(String, Throwable)
     */
    public SOMAException() {
        super();
    }

    /**
     * Create an exception with a message but with no chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception
     * @see #SOMAException(Throwable)
     * @see #SOMAException(String, Throwable)
     */
    public SOMAException(String message) {
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
    public SOMAException(String message, String msgKey, Object[] msgParms) {
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
    public SOMAException(String message, String msgKey, Object msgParm) {
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
    public SOMAException(String message, String msgKey) {
        super(message,msgKey);
    }

    /**
     * Create an exception with a message and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @see #SOMAException(String)
     * @see #SOMAException(Throwable)
     */
    public SOMAException(String message, Throwable cause) {
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
    public SOMAException(String message, Throwable cause, String msgKey, Object[] msgParms) {
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
    public SOMAException(String message, Throwable cause, String msgKey, Object msgParm) {
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
    public SOMAException(String message, Throwable cause, String msgKey) {
        super(message, cause, msgKey);
    }
    /**
     * Create an exception with a chained cause and no message.
     * 
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @see #SOMAException(String)
     * @see #SOMAException(String, Throwable)
     */
    public SOMAException(Throwable cause) {
        super(cause);
    }

}
