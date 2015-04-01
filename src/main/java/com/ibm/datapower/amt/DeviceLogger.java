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


package com.ibm.datapower.amt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.clientAPI.Configuration;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A simple trace logger that accepts trace entries via HTTP and logs to the 
 * appropriate (specified) logger. It is intended to be a centralized log
 * for trace events coming from DataPower appliances deployed in a managed 
 * set. DataPower appliances would set up log targets to point to this 
 * host/port. It spawns 10 worker threads
 * to catch and log events.
 * <p>
 * 
 * @version SCM ID: $Id: DeviceLogger.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 * <p>
 */
public class DeviceLogger extends Thread {
    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    protected final static String CLASS_NAME = DeviceLogger.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }
    private static DeviceLogger singleton = null;

    ServerSocket serverSocket;    
    static int threads = 10;
    private Logger targetLogger;

   /**
     * Create a new DeviceLogger.
     * 
     * @param port the listening TCP (HTTP) port
     * @param myLogger the Logger to which received events will be logged
     */
    public DeviceLogger(int port, Logger myLogger) {
        final String METHOD_NAME = "DeviceLogger"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        try {
            serverSocket = new ServerSocket(port);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger created serverSocket on " + serverSocket.getInetAddress() + ", port=" + port); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger; error creating ServerserverSocket ", e); //$NON-NLS-1$
        }
        targetLogger = myLogger;
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Return the singleton instance of a DeviceLogger given a trace logger.
     * 
     * @param devLogger the trace logger to be used by the DeviceLogger
     * @return the singleton DeviceLogger
     */
    public static DeviceLogger getInstance(Logger devLogger) {
        final String METHOD_NAME = "getInstance"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        Integer integerPort =  Configuration.getAsInteger(Configuration.KEY_DEVICE_LOG_PORT);
        if (integerPort != null) {
            // only start if a port was specified
            int port = integerPort.intValue();
            if (singleton == null) {
            	DeviceLogger singleton_local = new DeviceLogger(port, devLogger); 
            	singleton_local.start();
            	singleton = singleton_local;
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger singleton allocated"); //$NON-NLS-1$
            }
        }
        else {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger not started since no port was specified"); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return singleton;
    }
    
   /**
     * Run the accept thread that will listen for new connections. 
     * This will send this off to a client thread for processing. 
     * 
     * @see DeviceLogger_ClientThread
     */
    public void run() {
        final String METHOD_NAME = "run"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger starting"); //$NON-NLS-1$
        //create the Threads
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(new DeviceLogger_ClientThread(targetLogger), "DeviceLogger Client # " + i); //$NON-NLS-1$
            t.setName("DeviceLogger Client # " + i); //$NON-NLS-1$
            t.setDaemon(true);
            t.start();
        }

        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Started " + threads + " client threads."); //$NON-NLS-1$ //$NON-NLS-2$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger init complete.  Listening for requests...\n\n"); //$NON-NLS-1$

        //Listen for connections
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger accepted connection on port " + serverSocket.getLocalPort()); //$NON-NLS-1$
                DeviceLogger_ClientThread.processRequest(clientSocket);
            }
        } catch (Exception e) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "DeviceLogger; error accepting from ServerSocket;", e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
}
