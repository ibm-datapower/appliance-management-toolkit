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
package com.ibm.datapower.amt.amp;

import java.util.logging.Logger;

import com.ibm.datapower.amt.AbstractFactory;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;
/**
 * Create an instance of a NotificationCatcher implementation using only the
 * name. Because there may be multiple implementations of NotificationCatchers,
 * and because they should be singletons, this class will help return an
 * instance to the requested implementation.
 * <p>
 * 
 * @see NotificationCatcher
 * @see Notification
 * @see Commands
 * @version SCM ID: $Id: NotificationCatcherFactory.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
//* <p>
public class NotificationCatcherFactory extends AbstractFactory {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
	static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$
    
	protected final static String CLASS_NAME = NotificationCatcherFactory.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    private NotificationCatcherFactory() throws DMgrException {
        // This constructor doesn't need to be invoked by anyone.
        super();
    }
    
    /**
     * Get a reference to a NotificationCatcher that has the specified
     * implementation name.
     * 
     * @param implementationClassName the name of the implementation of the
     *        Notification catcher. The specified implementation should have a
     *        zero-argument constructor.
     * @return a reference to a NotificationCatcher that has the specified
     *         implementation name. It is expected that a NotificationCatcher of
     *         a specific implementation will be a singleton. 
     */
    public static NotificationCatcher getNotificationCatcher(String implementationClassName) throws AMPException {
        final String METHOD_NAME = "getNotificationCatcher"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        NotificationCatcher instance = null;
        
        try {
            instance = (NotificationCatcher) getUntypedInstance(implementationClassName,
                    NotificationCatcher.class, null, SINGLETON);
        } catch (DMgrException caught) {
        	String message = Messages.getString("wamt.amp.NotificationCatcherFactory.instNotificationCatcher",implementationClassName);
            AMPException e = new AMPException(message,caught, "wamt.amp.NotificationCatcherFactory.instNotificationCatcher",implementationClassName); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(instance);
    }
    
}
