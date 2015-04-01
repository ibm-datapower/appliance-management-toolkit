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

package com.ibm.datapower.amt.xc10.defaultProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.clientAPI.Configuration;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;

public class HTTPListener extends Thread {
	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;
	private static final String CLASS_NAME = HTTPListener.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	
	private ServerSocket socket;
	private InputStream is;
	
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(
    		justification="ignore this warning", value="SC_START_IN_CTOR")
	public HTTPListener(InputStream is) throws IOException {
		Integer portInteger = Configuration.getAsInteger(Configuration.KEY_HTTP_LISTENER_IP_PORT);
		int port = portInteger.intValue();
		
		socket = new ServerSocket(port);
		this.is = is;
		start();
	}
	
	static { 
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}
	
	public void run() {
		final String METHOD_NAME = "run"; //$NON-NLS-1$
		
		Integer timeoutInteger = Configuration.getAsInteger(Configuration.KEY_HTTP_LISTENER_SOCKET_TIMEOUT);
		int timeout = timeoutInteger.intValue();
		
		Socket s;
		
		try {
			socket.setSoTimeout(timeout*1000);
			s = socket.accept();
		} catch (SocketTimeoutException ste) {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"No connection comes in before HTTP Listener timed out.");
			
			return;
		} catch (IOException ioe) {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"HTTP Listener failed to listen on port "+getPort());
			
			return;
		} finally{
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore the exception
			}
		}
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"HTTP Listener received connection from "+s.getRemoteSocketAddress());
		
		try {
			final InputStream sis = s.getInputStream();
			
			new Thread() {
				public void run() {
					try {
						while (sis.read()!=-1);
					} catch (IOException e) {
						// Ignore the exception
					}
				}
			};
			
			OutputStream os = s.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			pw.print("HTTP/1.0 200 OK\r\n");
			pw.print("Content-type: application/octet-stream\r\n");
			pw.print("\r\n");
			pw.flush();
			int total=0;
			byte buffer[] = new byte[16*1024];
			int len;
			while ((len=is.read(buffer,0,buffer.length))!=-1) {
				os.write(buffer,0,len);
				total+=len;
			}
			os.flush();
			os.close();
			s.close();
		} catch (IOException e) {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Error occurred when transferring file." ,e); //$NON-NLS-1$
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			// Ignore the exception
		}
		
		logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"HTTP Listener ended.");
	}
		
	public String getHostPort() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostAddress()+":"+socket.getLocalPort();
	}
	
	public int getPort() {
		return socket.getLocalPort();
	}
}