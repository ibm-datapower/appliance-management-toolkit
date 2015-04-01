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

package com.ibm.datapower.amt.soma.defaultProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlbeans.impl.util.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;
/**
 * Implements utility functions used in the default provider SOMA client.
 * Provides conversion to/from Base64 Encoding (see RFC 3548).
 * 
 */

public class Utils {
    
    public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;
            
    private static final String CLASS_NAME = SOMACommandsImpl.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}

    public static String encodeBase64(byte[] inputArray){

        return new String(Base64.encode(inputArray));
    
    }

    public static byte[] decodeBase64(byte[] inputArray){

        return Base64.decode(inputArray);

    }
    
    public static void checkMemorySize(byte[] contents, String METHOD_NAME) throws DeviceExecutionException{
    	// Check memory size
		long maxMem = Runtime.getRuntime().maxMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();

		logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, 
					"Byte size = " + contents.length + "(" + contents.length/1024/1024 + "MB)" +
					" Free size = " + freeMem + "(" + freeMem/1024/1024 + "MB)" +
					" Maxium size = " + maxMem + "(" + maxMem/1024/1024 + "MB)" +
					" In used = " + + totalMem + "(" + totalMem/1024/1024 + "MB)");
		
		if ( freeMem < contents.length*2.6 ) { // 2.6 is an experimental number, might not be precise
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Available memory " + freeMem + "(" + freeMem/1024/1024  + 
					"MB) is not enough to continue" ); //$NON-NLS-1$
			Object[] params = { };
			String message = Messages.getString("wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
			DeviceExecutionException e = new DeviceExecutionException(message, "wamt.clientAPI.Domain.outOfMemory", params); //$NON-NLS-1$
			throw e;
		}
    }
}
