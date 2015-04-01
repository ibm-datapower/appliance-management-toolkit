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


package com.ibm.datapower.amt.amp.defaultCommon;

import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.InterruptedIOException;

import com.ibm.datapower.amt.clientAPI.Configuration;

/**
 * The CustomURLConnection wrappers a URLConnection with the ability to timeout
 * the invocation of connect(). If the target device is listening at the correct
 * port, but unable to complete the connection, then the manager may hang. Rather than
 * hang, the CustomURLConnection will timeout with InterrupedIOException.
 * Invoke #setConnectLimit(int milliseconds) followed by #connect() and the
 * connection request will be timed out after the connect limit is reached.
 *  
 */
public class CustomURLConnection extends URLConnection {

    private URLConnection conn        = null;
    private int msPoll                = 100;  // default to 100 ms
    private int msLimit               = -1;
    private int msElapsed             = 0;

    /**
     * Construct a CustomURLConnection, taking a URL as input. This is necessary in
     * order to construct the base class.
     * 
     */
    public CustomURLConnection(URL inUrl) {
    	super(inUrl);
    	setConnectLimit(Configuration.getAsInteger(Configuration.KEY_AMP_CONNECT_TIMEOUT));    
    }

    /**
     * Used the base class URL to get an open connection and return the URLConnection.
     * The returned URLConnection should be used to set options before the call to connect().
     * 
     */
    public URLConnection openConnection() throws IOException {
    	try {
        	conn = url.openConnection();
    	} catch (IOException e) {
    		throw e;
    	}
    	return conn;
    }
    
    /**
     * Set a time limit on the connection. The input value is in milliseconds.
     * Make sure that the poll value is less than the connect limit.
     * 
     */
    public void setConnectLimit(int inTime) {
        if (inTime > 0) { // must be valid
            msLimit = inTime;
        }
        else {
        	msLimit = new Integer(Configuration.DEFAULT_AMP_CONNECT_TIMEOUT);
        }
        if (msLimit < msPoll) { // adjust msPoll to be less than msLimit
            msPoll = (msLimit > 1 ? msLimit - 1 : 1);
        }
    }
    
    /**
     * Get the time limit set for connect.
     * 
     */
    public int getConnectLimit() {
    	return msLimit;
    }
    
    /**
     * Return the amount of time that the connect request has been monitored.
     * If the connect completes, this will be the time it took to complete the
     * connect (to a granularity of msPoll). If the connect does not complete,
     * it is effectively the connect timeout value.
     * 
     */
    public int getConnectElapsed() { return msElapsed; }

    /**
     * Request a connect on the URLConnection and monitor for it to complete.
     * 
     * The actual connect request is passed to the (private) ConnectionThread thread.
     * Then loop to monitor for (a) the connect to complete, or (b) the connect
     * request takes an IOException. An IOException is passed on to the caller.
     * If neither of these events occurs before the connect limit is reached,
     * throw InterruptedIOException.
     * 
     */
    public void connect() throws InterruptedIOException, IOException {
    	
        ConnectionThread thd = new ConnectionThread(conn);
        thd.start();

        // Loop to watch for thd.isConnected().
        // If msElapsed time is exceeded, throw InterruptedIOException.
        while (!thd.isConnected()) {
        	if (thd.hasError()) {
        		throw thd.getError();
        	}
        	else {
        		try {
           			Thread.sleep(msPoll);
        		} catch (InterruptedException ex) {
        			throw new InterruptedIOException("Connect timeout failed at " + (msElapsed - msPoll) + " ms");
       			}
        		
        		msElapsed += msPoll;
        		if (msElapsed > msLimit) {
        			throw new InterruptedIOException("Connect timeout after " + (msElapsed - msPoll) + " ms");
        		}
        	}
        }

        // All is well, the connect succeeded.
        return;
    }

    /**
     * Private class to do the actual connect, in case it blocks.
     * This thread is monitored (polled) by the owning CustomURLConnection class.
     */
    static class ConnectionThread extends Thread {

        volatile URLConnection conn = null;
        volatile boolean connected = false;
        volatile IOException excep = null;

        public ConnectionThread(URLConnection inConn) {
            conn = inConn;
        }

        public boolean isConnected() {
            return connected;
        }

        public boolean hasError() {
            return (excep != null);
        }

        public IOException getError() {
            return excep;
        }

        public void run() {
        	int msElapsed = 0;
            try {
                conn.connect();
                connected = true;
            } catch (IOException ex) {
                excep = ex;
            }
            return;
        }        

    } // end, ConnectionThread class

} // end, CustomURLConnection class
