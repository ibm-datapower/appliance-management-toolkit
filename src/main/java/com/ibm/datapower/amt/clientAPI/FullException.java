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

import java.util.Map;

import com.ibm.datapower.amt.Constants;

/**
 * The container you are trying to add an item to is already at its maximum
 * capacity. Your "add" request was unsuccessful.
 * <p>
 * This is generally used with queues for background tasks. In more technical
 * terms, the queue is already full and the requested background task can not be
 * added to it. The length of the queue is set at the time that the Manager is
 * instantiated via
 * {@link com.ibm.datapower.amt.clientAPI.Manager#getInstance(Map)}. Use the
 * option {@link com.ibm.datapower.amt.clientAPI.Manager#OPTION_TASK_QUEUE_SIZE}.
 * The default value of the task queue size is "unlimited", so in practice this
 * exception should not occur unless the default has been overridden.
 * <p>
 * The idea behind limiting the task queue size is to prevent users from
 * submitting tasks to a queue that already has work outstanding, meaning that
 * the user made a decision to do something to an environment which may be
 * changed before their request is serviced. The queue operates in a FIFO
 * manner. If the user had waited until all the outstanding work was completed,
 * they may have wanted to make a different choice. However noble this idea, in
 * the end we felt it was too limiting, and the manager has the principle of getting to
 * the desired end state even though it may take a non-optimal route to get
 * there. This is why the default value of the task queue size is "unlimited".
 * However, we wanted to give other users the ability to take a more
 * conservative approach if they wished to do so.
 * <p>
 * 
 * @version SCM ID: $Id: FullException.java,v 1.3 2010/08/23 21:20:27 burket Exp $
 */
public class FullException extends ClientAPIException {

    private static final long serialVersionUID = 2L;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    /**
     * Create an exception with no message and no cause. Please use one of the
     * other constructors to provide as much information about the exception as
     * possible. You really shouldn't be using this zero-argument constructor.
     */
    public FullException() {
        super();
    }

    /**
     * Create an exception with a message but with no chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception
     */
    public FullException(String message) {
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
     * @param msgParms an Ojbect[] that gives the attributes to be used with
     *        msgKey
     */
    public FullException(String message, String msgKey, Object[] msgParms) {
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
     * @param msgParm an Ojbect that gives the attributes to be used with msgKey
     */
    public FullException(String message, String msgKey, Object msgParm) {
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
    public FullException(String message, String msgKey) {
        super(message,msgKey);
    }
    
    /**
     * Create an exception with a message and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public FullException(String message, Throwable cause) {
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
     * @param msgParms an Ojbect[] that gives the attributes to be used with
     *        msgKey
     */
    public FullException(String message, Throwable cause, String msgKey, Object[] msgParms) {
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
     * @param msgParm an Ojbect that gives the attribute to be used with msgKey
     */
    public FullException(String message, Throwable cause, String msgKey, Object msgParm) {
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
    public FullException(String message, Throwable cause, String msgKey) {
        super(message, cause, msgKey);
    }

    /**
     * Create an exception with a chained cause and no message.
     * 
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public FullException(Throwable cause) {
        super(cause);
    }

}
