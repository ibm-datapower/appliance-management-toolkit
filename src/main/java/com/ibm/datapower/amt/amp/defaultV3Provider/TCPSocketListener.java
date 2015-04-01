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


package com.ibm.datapower.amt.amp.defaultV3Provider;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.NotificationCatcher;
import com.ibm.datapower.amt.amp.NotificationCatcherResourceException;
import com.ibm.datapower.amt.logging.LoggerHelper;
/**
 * Listens for TCP connections.  When a new connection is initiated, an HTTP
 * handler will be created to process the event. 
 *
 * @see NotificationCatcher
 * @see NotificationCatcherImpl
 * @version SCM ID: $Id: TCPSocketListener.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
public class TCPSocketListener implements Runnable {
    boolean shuttingDown; 
    ServerSocket mySock;

    public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;

    protected final static String CLASS_NAME = TCPSocketListener.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    /**
     * When the Manager is started, the Manager will create a 
     * NotificationCatcherImpl using the NotificationCatcherFactory. The
     * catcher will instantiate the TCPSocketListener telling it which port
     * to use for catching events and whether SSL is used.  SSL is always used
     * in production code, though the manager allows an undocumented interface to 
     * post to an HTTP port instead of HTTPS.
     * 
     * @see NotificationCatcherImpl
     * @throws AMPException if it has problems listening on the socket
     */
    public TCPSocketListener(int port, boolean useSSL) throws AMPException {
        final String METHOD_NAME = "TCPSocketListener"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        shuttingDown = false;

        try {
            if (useSSL) {
                SSLContextCache myCache = SSLContextCache.getInstance();
                SSLServerSocketFactory sslserversocketfactory = myCache.getCustomSSLServerSocketFactory();
                //SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();

                mySock = (SSLServerSocket)sslserversocketfactory.createServerSocket(port);
                // Fix for bugzilla #72456
                //((SSLServerSocket)mySock).setNeedClientAuth(true);
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Listening on HTTPS port " + mySock.getLocalPort()); //$NON-NLS-1$
            }
            else {
                mySock = new ServerSocket(port);
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Listening on HTTP port " + mySock.getLocalPort()); //$NON-NLS-1$
            }
        }
        catch (Exception e) {
        	String message = Messages.getString("wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port));
            NotificationCatcherResourceException e2 = new NotificationCatcherResourceException(message, e,"wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port)); //$NON-NLS-1$
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message, e2); //$NON-NLS-1$
            throw e2;
    	}
    	// now we test accept() to see if it will fail on the first invocation
    	// we set the socket to timeout in a certain number of milliseconds.
    	// if we are thrown a SocketTimeoutException, then we can (hopefully) assume that the SSL/TCP config is fine.
    	// if we get any other type of exception, then there is most likely a configuration error. 
    	int originalTimeout = 0;
    	try{
    		originalTimeout = mySock.getSoTimeout();
    		mySock.setSoTimeout(1);
    		Socket socket = mySock.accept();

    		// in the REALLY rare case that we get a connection before the timeout occurs, 
    		// we should just close the socket and subsequently ignore the request
    		socket.close();
    	}
    	catch (SocketTimeoutException ste){
    		// if we get this type of exception, then things will most likely work.
    		try{
    			// lets put the timeout back to what it was before
    			mySock.setSoTimeout(originalTimeout);
    		}
    		catch(SocketException se){
    			// if we can't reset the timeout on the socket, then shutdown.
    			// we initiate a shutdown by throwing an exception up the stack.
    			String message = Messages.getString("wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port));
                NotificationCatcherResourceException e2 = new NotificationCatcherResourceException(message, se,"wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port)); //$NON-NLS-1$
                logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message, e2); //$NON-NLS-1$
                throw e2;
    		}
    	}
    	catch (Exception e){
    		// if we get any other type of exception, it was a configuration error, 
    		// either we couldn't get or set the timeout value (which is bad), or 
    		// there is an underlying SSL issue as well.

    		// we initiate a shutdown by throwing an exception up the stack.
    		String message = Messages.getString("wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port));
            NotificationCatcherResourceException e2 = new NotificationCatcherResourceException(message, e,"wamt.amp.defaultProvider.TCPSocketListener.errCreateAMPEvent",Integer.toString(port)); //$NON-NLS-1$
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, message, e2); //$NON-NLS-1$
            throw e2;
    	}

    	logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Once the TCPSocketListener is allocated (and the server socket is 
     * listenting, the NotificationCatcherImpl will start a new thread for 
     * accpeting new connections for posted events. This kicks off a new 
     * HttpHandler thread for each received connection (event).
     * 
     * @see NotificationCatcherImpl
     */
    public void run()
    {
        final String METHOD_NAME = "run"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        while(!shuttingDown) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Waiting for more requests."); //$NON-NLS-1$
            try { 
                Socket socket = mySock.accept();
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "New connection from " + //$NON-NLS-1$
                            socket.getInetAddress() +
                            ":" + socket.getPort()); //$NON-NLS-1$

                // for now, just create a thread request, afr to fix
                HttpHandler request = new HttpHandler(socket);
                Thread thread = new Thread(request);
                thread.setDaemon(true);
                thread.start();
            }
            catch(Exception e) {
            	// if we aren't shutting down, then we should log this error.
            	if (!shuttingDown){
            		String message = Messages.getString("wamt.amp.defaultProvider.TCPSocketListener.errAccepting"); //$NON-NLS-1$
                	logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message, e);
            	}
            	// if we are shutting down, then we just quietly ignore it. 
            }
        }
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Socket listener shut down."); //$NON-NLS-1$
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    

    /**
     * The NotificationCatcherImpl will shut down this listener when it is
     * told to shut down.
     * 
     * @see NotificationCatcherImpl
     */
    public void shutdown() {
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        shuttingDown = true;
        try {
            mySock.close();
        } catch(Exception e) {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Error in closing socket ", e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
}

