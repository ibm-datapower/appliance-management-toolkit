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

package com.ibm.datapower.amt.clientAPI.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;

/*
 * This class can be used to implement a timeout for an operation that 
 * requires polling to determine completetion
 * <br/><pre>
 *  Example:
 *  	A domain quiesce operation may have a timeout of 60 seconds, and 
 *      the manager may have a polling interval of 2 seconds, but we can't just 
 *      poll the status in a loop (30x) because we don't know how long the 
 *      polling operation takes (could be 20 seconds? don't know), that 
 *      could significantly mess up our timeout. This class can be used to 
 *      timeout in exactly N seconds and set a flag stating the timeout 
 *      condition. The polling code can check the timeoutExpired method to 
 *      see if the specified time has elapsed. 
 *  </pre>
 */
public class Timeout implements Runnable {
    	
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = Timeout.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	static {
	    LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}
    	
	private boolean timeoutExpired = false;
	private int     timeoutValue   = 0;

	/* 
	 * Set the timeout for this timer. Only effective before the run() method id called
	 * 
	 * @param timeout in seconds
	 */
	public void setTimeout(int timeout) {
		this.timeoutValue = timeout;
	}

	/*
	 * Called by Thread.start() to initiate the timeout timer. 
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
        final String METHOD_NAME = "TimeoutPop.run"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this});

        timeoutExpired = false;
    	try {
			Thread.sleep(timeoutValue * 1000);
		} catch (InterruptedException e) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "interrupted from timeout. shutting down?"); //$NON-NLS-1$
		}
    	timeoutExpired = true;
    }
	
	/*
	 * Method to use to see if the timer has elapsed.
	 *  
	 */
	public boolean timeoutExpired() {
		return timeoutExpired;
	}
}
