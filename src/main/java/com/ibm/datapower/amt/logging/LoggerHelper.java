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


package com.ibm.datapower.amt.logging;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
/**
 * Simple logging class containing helper methods for logging.
 * 
 */
public class LoggerHelper {
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    private static ArrayList<Logger> registeredLoggers = new ArrayList<Logger>();
    
    protected final static String CLASS_NAME = LoggerHelper.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);

    /**
     * Adds the Logger to the registered list of Loggers.  A specific Logger instance is only added one time.  
     * Any duplicate adds are ignored.
     * 
     * @param logger The Logger to be added.
     * @param description For future use.  Currently not used.
     */
    public static void addLoggerToGroup(Logger logger, String description) {
        // currently ignores myString
        if (!registeredLoggers.contains(logger))
            registeredLoggers.add(logger);
    }
}
