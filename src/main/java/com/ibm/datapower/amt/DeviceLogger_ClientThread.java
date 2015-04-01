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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A thread that will read events from an accepted socket and log them. All
 * new sockets get put on a shared pool from which client threads pull.
 * <p>
 * 
 * @version SCM ID: $Id: DeviceLogger_ClientThread.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 * <p>
 */
public class DeviceLogger_ClientThread extends Thread {
    static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    protected final static String CLASS_NAME = DeviceLogger_ClientThread.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    protected int threadNbr = 0;
    protected final static List pool = new LinkedList();
    private Logger targetLogger = null;
    
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

   /**
     * Create a new DeviceLogger_ClientThread.
     * 
     * @param myLogger the Logger to which received events will be logged
     */
    DeviceLogger_ClientThread(Logger myLogger) {
        final String METHOD_NAME = "DeviceLogger_ClientThread"; //$NON-NLS-1$
        threadNbr++;
        targetLogger = myLogger;
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, Thread.currentThread().getName() + " created"); //$NON-NLS-1$
    }
		
    /**
     * Run the client thread that will take a new connection from a shared pool
     * and pull an event
     * from it to log.
     * 
     */
    public void run() {
        final String METHOD_NAME = "run"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, Thread.currentThread().getName() + " starting"); //$NON-NLS-1$

        Socket clientSocket;
        int contentLength = 0;
        boolean keepAlive = false;
        while (true) {
            synchronized (pool) {
                while (pool.isEmpty()) {
                    try {
                        pool.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                clientSocket = (Socket) pool.remove(0);
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "ClientThread " + Thread.currentThread().getName() + " got new request"); //$NON-NLS-1$ //$NON-NLS-2$
					
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String reqHeaders = getRequestHeaders(in);
                    contentLength = getContentLength(reqHeaders);
                    keepAlive = getKeepAlive(reqHeaders);
                    String reqBody = getRequestBody(in, contentLength);
						
                    DeviceLogger_ClientThread.processResponse(clientSocket, reqBody);
                } catch (Exception e) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Error Processing Request ", e); //$NON-NLS-1$
                } finally {
                    try {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "keepAlive = " + keepAlive); //$NON-NLS-1$
                        if (!keepAlive) {
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Closing clientSocket"); //$NON-NLS-1$
                            clientSocket.close();
                        }
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Request Done"); //$NON-NLS-1$
                    } catch (IOException e1) {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Error closing Request ", e1); //$NON-NLS-1$
                    }
                }
            }
        }
    }

    /**
     * Process an inbound socket by adding to the shared pool.
     * 
     * @param clientSocket the Socket that has just been accepted
     */
    public static void processRequest(Socket clientSocket) {
        final String METHOD_NAME = "processRequest"; //$NON-NLS-1$

        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, Thread.currentThread().getName()+ " recieved request"); //$NON-NLS-1$

        synchronized (pool) {
            try {
                pool.add(pool.size(), clientSocket);
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "request added to pool"); //$NON-NLS-1$
                pool.notifyAll();
            } catch (IndexOutOfBoundsException e) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, Thread.currentThread().getName() + " IndexOOB adding " + pool.size()); //$NON-NLS-1$
            }
        }
    }


    /**
     * Send a response to the client that the event was logged
     * 
     * @param clientSocket the Socket over which to send the response
     * @param reqBody not used
     */
    public static void processResponse(Socket clientSocket, String reqBody) {
        final String METHOD_NAME = "processResponse"; //$NON-NLS-1$
        try {
            OutputStream raw = new BufferedOutputStream(clientSocket.getOutputStream());
            Writer out = new OutputStreamWriter(raw);
            out.write("HTTP/1.0 200\r\nContent-Type: text/xml\r\n\r\n"); //$NON-NLS-1$
            out.close();
        } catch (IOException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Error Processing Response", e); //$NON-NLS-1$
        }
    }

    /**
     * Extract the keep alive value from the request headers. 
     * 
     * @param sReqHeaders the request headers
     * @return the boolean keepalive value
     */
    protected static boolean getKeepAlive(String sReqHeaders) {
        boolean keepAlive = (sReqHeaders.toLowerCase().indexOf("connection: keep-alive") > -1) ? true : false; //$NON-NLS-1$
        return keepAlive;
    }

    /**
     * Extract the content length from the request headers. 
     * 
     * @param sReqHeaders the request headers
     * @return the content length
     */
    protected static int getContentLength(String sReqHeaders) {
        final String METHOD_NAME = "getContentLength"; //$NON-NLS-1$
        int clPos = sReqHeaders.indexOf("Content-Length:"); //$NON-NLS-1$
        int clEnd = 0;
        int contentLength = 0;

        if (clPos > -1) {
            clPos++;
            clEnd = clPos += "Content-Length:".length(); //$NON-NLS-1$
            // logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "clPos=" + clPos + ", clEnd=" + clEnd);
			
            try {
                for (char c1 = sReqHeaders.charAt(clEnd); clEnd < sReqHeaders
                         .length(); c1 = sReqHeaders.charAt(clEnd)) {
                    if (Character.isDigit(c1)) {
                        clEnd++;
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Exception find end of contentLength " + e.getMessage()); //$NON-NLS-1$
            }
            contentLength = Integer.parseInt(sReqHeaders.substring(clPos, clEnd));
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "contentLength=" + contentLength); //$NON-NLS-1$
        }
        return contentLength;
    }

    protected static String getRequestHeaders(BufferedReader in) {
        final String METHOD_NAME = "getRequestHeaders"; //$NON-NLS-1$
        int CR = 13;
        int LF = 10;

        boolean CR_Found = false;
        boolean CRLF_Found = false;
        boolean contigiousCRLF_Found = false;
        int c = 0;

        StringBuffer sbufReqHeaders = new StringBuffer();
        String sReqHeaders = null;

        while (contigiousCRLF_Found == false && c != -1) {
            try {
                c = in.read();
                sbufReqHeaders.append((char) c);
            } catch (IOException e) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Error Reading Request Headers",e); //$NON-NLS-1$
                e.getMessage();
            }
            // logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, System.currentTimeMillis() + " Read " + c + " = " + (char) c);
            if (c == CR) {
                CR_Found = true;
            } else {
                if (c == LF && CR_Found) {
                    if (CRLF_Found) {
                        contigiousCRLF_Found = true;
                    } else {
                        CRLF_Found = true;
                    }
                } else {
                    CRLF_Found = false;
                    CR_Found = false;
                }
            }
        }
        sReqHeaders = sbufReqHeaders.toString();
        return sReqHeaders;
    }

    protected String getRequestBody(BufferedReader in, int contentLength) {
        final String METHOD_NAME = "getRequestBody"; //$NON-NLS-1$
        int c = 0;
        StringBuffer sbufReqBody = new StringBuffer();

        try {
            if (contentLength > 0) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Fetching request body via content length"); //$NON-NLS-1$
                for (int i = 0; i < contentLength; i++) {
                    c = in.read();
                    if (c == -1) {
                        break;
                    } else {
                        sbufReqBody.append((char) c);
                    }
                }
            } else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Fetching request body via read"); //$NON-NLS-1$
                while (true) {
                    c = in.read();
                    // logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, System.currentTimeMillis() + " Read " + c + " = " + (char) c);
                    if (c == -1) {
                        break;
                    } else {
                        sbufReqBody.append((char) c);
                    }
                }
            }
        } catch (IOException e) {
 //jg changed to logp & no nls enablement           System.out.print(Messages.getString("wamt.DeviceLogger_ClientThread.errorReadReq", e.getLocalizedMessage())); //$NON-NLS-1$
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Error Reading Request Body " + e.getMessage()); //$NON-NLS-1$
            e.printStackTrace();
        }
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Request logged\n" + sbufReqBody.toString()); //$NON-NLS-1$
//jg changed to no nls enablement        targetLogger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.DeviceLogger_ClientThread.reqLogged", sbufReqBody.toString())); //$NON-NLS-1$
        targetLogger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "Request logged\n"+sbufReqBody.toString()); //$NON-NLS-1$
        return sbufReqBody.toString();
    }
}
