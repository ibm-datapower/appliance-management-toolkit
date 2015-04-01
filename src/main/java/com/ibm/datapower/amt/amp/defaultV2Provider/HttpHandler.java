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


package com.ibm.datapower.amt.amp.defaultV2Provider;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.clientAPI.DeletedException;
import com.ibm.datapower.amt.clientAPI.Device;
import com.ibm.datapower.amt.clientAPI.InvalidCredentialException;
import com.ibm.datapower.amt.clientAPI.InvalidParameterException;
import com.ibm.datapower.amt.clientAPI.ManagedSet;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Handles received AMP events and pushes them to the manager.
 * <p>
 * 
 * @see NotificationCatcherImpl
 * @see Commands
 * @version SCM ID: $Id: HttpHandler.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */

public class HttpHandler implements Runnable {
    final static String CRLF = "\r\n"; //$NON-NLS-1$
    Socket socket;
    InputStream input;
    OutputStream output;
    BufferedReader br;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected final static String CLASS_NAME = HttpHandler.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }


    /**
     * When the TCPSocketListener accepts a new connection, meaning a new
     * AMP even is arriving, it creates a new HttpHandler to handle the 
     * incoming event. It then creates a new thread for HttpHandler to 
     * actually parse and deliver the event.
     * 
     * @see TCPSocketListener
     * @throws IOException if it has problems accesing the new socket
     */
    public HttpHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    /**
     * This new thread actually parses and delivers the thread to the
     * manager (eventually).
     * 
     * @see TCPSocketListener
     */
    public void run()
    {
        final String METHOD_NAME = "run"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        try {
            processEvent();
        }
        catch(IOException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.ioException"), e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * This processes the actual AMP event. It creates a new Notification
     * and enqueues the event to the manager. The Notification object actually
     * parses the AMP event XML.
     * 
     * @see TCPSocketListener
     * @see Notification
     * @throws IOException on error reading from the socket
     */
    private void processEvent() throws IOException
    {
        final String METHOD_NAME = "processEvent"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        boolean keepReading = true;
        boolean invalidEvent = false;
        int bodyLength = 0;
        String firstLine = null;
        String contentTypeLine = "Content-Type: text/html" + CRLF ; //$NON-NLS-1$
        String entityBody = null;
        String contentLengthLine = null;

        String inputLine = br.readLine();

        if ( inputLine != null ) {
	        // grab the first word of the first line
	        StringTokenizer s = new StringTokenizer(inputLine);
	        String temp = s.nextToken(); 
	        if(!temp.equals("POST")) { //$NON-NLS-1$
	            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Unrecognized HTTP method (not a POST): "+inputLine); //$NON-NLS-1$
	            invalidEvent = true;
	        } else {
	            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Recognized HTTP POST)"); //$NON-NLS-1$
	        }
	
	        while(keepReading) {
	        	inputLine = br.readLine();
	        	if ( inputLine != null ) {		            
		            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Read more: "+inputLine); //$NON-NLS-1$
		
		            if(inputLine.equals(CRLF) || inputLine.equals("")) //$NON-NLS-1$
		                keepReading = false;
		            else {
		                s = new StringTokenizer(inputLine);
		                temp = s.nextToken();
		
		                if (temp.compareToIgnoreCase("Content-Type:")==0) { //$NON-NLS-1$
		                    temp = s.nextToken();
		                    if (!temp.startsWith("text/xml")) { //$NON-NLS-1$
		                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Non-xml data received: "+temp); //$NON-NLS-1$
		                        invalidEvent = true;
		                    }
		                }
		
		                if (temp.compareToIgnoreCase("Content-Length:")==0) { //$NON-NLS-1$
		                    temp = s.nextToken();
		                    bodyLength = new Integer(temp).intValue();
		                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Received content length of  "+bodyLength); //$NON-NLS-1$
		                }
		            }
	        	}
	        }
        }

        if (bodyLength > 0){
            char[] buffer = new char [bodyLength];
            int numRead = br.read(buffer, 0, bodyLength);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Read " +numRead+ " bytes"); //$NON-NLS-1$ //$NON-NLS-2$
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Read contents: " +new String(buffer)); //$NON-NLS-1$
            CharArrayReader cr = new CharArrayReader(buffer);
            BufferedReader br2 = new BufferedReader(cr);

            Notification myNotification = new Notification(br2);
            String serialNumber = myNotification.getDeviceSerialNumber();
            if (serialNumber != null) {
                try {
                    // would be nice to use Manager.internalGetInstance, but that isn't visible here
                    Manager manager = Manager.getInstance(null);
                    Device device = manager.getDeviceBySerialNumber(serialNumber);
                    ManagedSet managedSet = null;
                    if (device != null) {
                        managedSet = device.getManagedSet();
                    } else {
                        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.AMPeventFromUnkDev",serialNumber)); //$NON-NLS-1$
                    }
                    if (managedSet != null) {
                        /*
                         * It is important to not filter any Notifications here.
                         * If you can find a ManagedSet in which to enqueue
                         * them, then enqueue them. The reason for this is
                         * because the QueueProcessor is looking for gaps in the
                         * sequence number. If we discard Notifications here and
                         * the QueueProcessor never sees them, then the
                         * QueueProcessor thinks that Notifications from the
                         * device have been lost and it will initiate a Device
                         * sync, which is relatively expensive. So if there are
                         * events which will eventually get discarded, enqueue
                         * them anyway and let the QueueProcessor do the
                         * discarding.
                         */
                        
                        // enqueue Notification for the QueueProcessor to handle
                        manager.enqueue(myNotification, managedSet);
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Queued AMP event from: "+serialNumber); //$NON-NLS-1$
                    } else if (device != null) { 
                        // trace only one thing
                        String message = Messages.getString("wamt.amp.defaultProvider.HttpHandler.notInManagedSet",serialNumber); //$NON-NLS-1$
                        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                    }
                } catch (InvalidParameterException e) {
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.exManagerHandle"), e); //$NON-NLS-1$
                } catch (InvalidCredentialException e) {
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.exManagerHandle"), e); //$NON-NLS-1$
                } catch (DatastoreException e) {
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.exManagerHandle"), e); //$NON-NLS-1$
                } catch (AMPException e) {
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.exManagerHandle"), e); //$NON-NLS-1$
                } catch (DeletedException e) {
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.exDelDevMs"), e); //$NON-NLS-1$
                }
            }
            else { 
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.HttpHandler.malformedCBE")); //$NON-NLS-1$
            }
        }
        else {
            invalidEvent = true;
        }

        // send back a response
        if (invalidEvent) {
            firstLine = "HTTP/1.0 400 Bad Request" + CRLF ; //$NON-NLS-1$
            entityBody = "Error in request." + CRLF; //$NON-NLS-1$
        } else {
            firstLine = "HTTP/1.0 200 OK" + CRLF ; //$NON-NLS-1$
            entityBody = "All good." + CRLF; //$NON-NLS-1$
        }

        int outputLength = entityBody.length();
        contentLengthLine = "Content-Length: " +  //$NON-NLS-1$
            Integer.toString(outputLength) + CRLF;

        output.write(firstLine.getBytes());
        output.write(contentTypeLine.getBytes());
        output.write(contentLengthLine.getBytes());
        output.write(CRLF.getBytes());
        output.write(entityBody.getBytes());

        output.close();
        br.close();
        socket.close();
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
}
