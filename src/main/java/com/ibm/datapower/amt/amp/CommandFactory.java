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
 * Gets an instance of an AMP Command implementation. It is expected that there
 * might be more than one implementation. At the time of this writing, there are
 * two implementations:
 * <ul>
 * <li><code>com.ibm.datapower.amt.amp.defaultProvider</code>: uses SOAP
 * messages to talk to a device using the device's XML Management Interface.
 * </li>
 * <li><code>com.ibm.datapower.amt.amp.dummyProvider</code>: simulates a set
 * of devices for management purposes only without any network traffic or real
 * devices. This dummyProvider is for unit testing, the source code may be
 * in a different repository module, and is not included in the product jars.
 * </li>
 * </ul>
 * <p>
 * <p>
 * 
 * @version SCM ID: $Id: CommandFactory.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
public class CommandFactory extends AbstractFactory {
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected final static String CLASS_NAME = CommandFactory.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    private CommandFactory() throws DMgrException {
        super();
    }
    
    /**
     * Get an instance of a class that implements the <code>Commands</code> interface.
     * 
     * @param commandsImplementationClassName the name of the class that implements 
     * 		  the Commands interface. This name should be in the format that can be
     *        used by {@link Class#forName(java.lang.String)}. This method will
     *        attempt to load the named class and invoke its one-argument 
     *        constructor (the SOAPHelper classname). The named class should be on 
     *        the JRE's classpath.
     * @param soapHelperImplementationClassName the name of the class that 
     *        implements the SOAPHelper interface. This name should be in the 
     *        format that can be used by {@link Class#forName(java.lang.String)}. 
     *        This method will attempt to load the named class and invoke its 
     *        zero-argument constructor. The named class should be on the JRE's 
     *        classpath.
     * @return an instance of the named class. This return value will also be
     *         cached by this factory, so additional calls to this method using
     *         the same named class will return the same instance instead of
     *         again calling the constructor of the named class.
     * @throws AMPException if there is a problem getting the class, such as the
     *         named class not being found, the named class not having a
     *         zero-argument constructor, or if the named class' constructor
     *         throws an exception.
     */
    public static Commands getCommands(String commandsImplementationClassName, 
    			String soapHelperImplementationClassName) throws AMPException {
        final String METHOD_NAME = "getCommands"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Commands instance = null;

        try {
            instance = (Commands) getUntypedInstance(commandsImplementationClassName, 
                                  Commands.class, new Object[]{soapHelperImplementationClassName}, SINGLETON);
        } catch (DMgrException caught) {
        	String message = Messages.getString("wamt.amp.CommandFactory.instClass",commandsImplementationClassName);
            AMPException e = new AMPException(message, caught,"wamt.amp.CommandFactory.instClass",commandsImplementationClassName); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(instance);
    }
    
}
