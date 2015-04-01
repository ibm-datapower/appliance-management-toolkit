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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.AMPIOException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.ConfigObject;
import com.ibm.datapower.amt.amp.DeleteObjectResult;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.DeviceMetaInfo;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.amp.ErrorReport;
import com.ibm.datapower.amt.amp.InterDependentServiceCollection;
import com.ibm.datapower.amt.amp.InvalidCredentialsException;
import com.ibm.datapower.amt.amp.NotExistException;
import com.ibm.datapower.amt.amp.PingResponse;
import com.ibm.datapower.amt.amp.ReferencedObjectCollection;
import com.ibm.datapower.amt.amp.SubscriptionResponseCode;
import com.ibm.datapower.amt.amp.Utilities;
import com.ibm.datapower.amt.clientAPI.ConfigService;
import com.ibm.datapower.amt.clientAPI.Configuration;
import com.ibm.datapower.amt.clientAPI.DeletedException;
import com.ibm.datapower.amt.clientAPI.DeploymentPolicy;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.clientAPI.RuntimeService;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class CommandsImpl implements Commands {
	public static void main(String args[]) throws Exception {
		Commands cmds = new CommandsImpl(null);
		DeviceContext dc = new DeviceContext("xc3.rtp.raleigh.ibm.com", 0, "xcadmin", "xcadmin");
		DeviceMetaInfo dmi = cmds.getDeviceMetaInfo(dc);

		System.out.println("[MIKE] DeviceMetaInfo = " + dmi);

		System.out.println("[MIKE] PingDevice result = "+cmds.pingDevice(dc, null));
		System.out.println("[MIKE] PingDevice result = "+cmds.pingDevice(dc, null));
		System.out.println("[MIKE] PingDevice result = "+cmds.pingDevice(dc, null));
		/*
		 * FileInputStream fis = new FileInputStream("/Users/bkmartin/ipv6.scrypt3"); c.setFirmware(dc,fis);
		 */
	}

	// The soap helper parameter is actually not needed, it's for compatbility's purpose only
	public CommandsImpl(String soapHelperImplementationClassName) throws AMPException {
	}

	public static final String COPYRIGHT_2012_2014 = Constants.COPYRIGHT_2012_2014;
	private static final String CLASS_NAME = CommandsImpl.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	
	static { 
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}
	
	int connectTimeout = 0;
	
	private Session connect(DeviceContext device) 
	throws InvalidCredentialsException, AMPIOException, DeviceExecutionException, AMPIOException {
		final String METHOD_NAME = "connect"; //$NON-NLS-1$		
				
		JSch jsch=new JSch();      
		Session session = null;
		ChannelShell channel = null;
		try {
			session = jsch.getSession(device.getUserId(), device.getHostname(), device.getAMPPort());
			if ( session == null ) {
				Object[] params = {device.getHostname(),Integer.valueOf(device.getAMPPort())};
	        	String message = Messages.getString("wamt.amp.defaultV2Provider.SOAPHelper.ioExConnect",params);
				AMPIOException ex = new AMPIOException(message, "wamt.amp.defaultV2Provider.SOAPHelper.ioExConnect",params);
				
				logger.throwing(CLASS_NAME, METHOD_NAME, ex);
				throw ex;
			}
			
			session.setPassword(device.getPassword());  
			session.setConfig("StrictHostKeyChecking", "no");
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "[SSH cliet] connecting to " + device.getHostname() + ":" + device.getAMPPort());
	
			session.connect(connectTimeout);   // making a connection with timeout.		
				
			if (!session.isConnected()){   
	        	logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,"[SSH client] Session not connected!");   
			
				Object[] params = {device.getHostname(),Integer.valueOf(device.getAMPPort())};
	        	String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.invalidUNPass",params);
	        	InvalidCredentialsException ex = new InvalidCredentialsException(message,"wamt.amp.defaultProvider.SOAPHelper.invalidUNPass",params); //$NON-NLS-1$ //$NON-NLS-2$
	        	
	        	logger.throwing(CLASS_NAME, METHOD_NAME, ex);
	            throw ex;
	        }		
			
			channel = (ChannelShell) session.openChannel("shell");
			String lines[] = readToMatch_Shell(channel, "Console>");

			if (lines != null) {
				for (int i = 1; i <= lines.length; i++) {					
					logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"[SSH read line dump "+i+"] "+lines[i-1]);
				}
			} else {
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed");
				DeviceExecutionException dee = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed"); //$NON-NLS-1$
				
				if ( session != null )
		    		session.disconnect();
				throw dee;
			}
		} catch (JSchException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,e.getLocalizedMessage());
			Object[] params = {device.getHostname(),Integer.valueOf(device.getAMPPort())};
        	String message = Messages.getString("wamt.amp.defaultV2Provider.SOAPHelper.ioExConnect",params);
			AMPIOException ex = new AMPIOException(message, "wamt.amp.defaultV2Provider.SOAPHelper.ioExConnect",params);
			
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		} finally {
			if ( channel != null && channel.isConnected() ) {
				channel.disconnect();
			}
		}
		return session;
	}
	
	private String[] readToMatch_Shell(Channel channel, String match) {
		final String METHOD_NAME = "readToMatch_Shell"; //$NON-NLS-1$
		
		String sTmpFileName = "Temp.log";
		ArrayList<String> lines = new ArrayList<String>();
		
		PipedInputStream pipeIn = null;  
	    PipedOutputStream pipeOut = null;  
	    FileOutputStream fileOut = null;
	    BufferedReader br = null;
		
		try {
			pipeIn = new PipedInputStream();  
		    pipeOut = new PipedOutputStream( pipeIn );  
		    fileOut = new FileOutputStream( sTmpFileName );  
		    channel.setInputStream( pipeIn );  
		    channel.setOutputStream( fileOut );		    
		    channel.connect( connectTimeout );
		    try {Thread.sleep( 2000 );} catch (InterruptedException e) {}
		    
		    // Get response lines
		    br = new BufferedReader( new FileReader(sTmpFileName) );		    
		    String sCurrentLine;
		    boolean bMatch = false;
			while ( (sCurrentLine = br.readLine()) != null ) {
				//System.out.println(sCurrentLine);
				lines.add(sCurrentLine);
				if ( sCurrentLine.contains(match))
					bMatch = true;
			}  
			
			if ( bMatch ) {
				return (String[]) lines.toArray(new String[lines.size()]);
			}
		} catch (JSchException e2) {
			
		} catch (IOException e1) {
			
		} finally {
			try {
				if ( pipeOut != null ) pipeOut.close();   
				if ( pipeIn != null ) pipeIn.close();  
				if ( fileOut != null ) fileOut.close(); 			
				if ( br != null ) br.close();
			} catch (IOException e1) {
				
			}
			if ( !new File(sTmpFileName).delete() ) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Delete " + sTmpFileName + "failed");
			}
		}
		return null;
	}
	
	private String[] executeCommand(Session session, String command) 
	throws AMPIOException, DeviceExecutionException {
		final String METHOD_NAME = "executeCommand"; //$NON-NLS-1$	
		ChannelExec channel = null;		
		String[] lines = null;
		
		try {
			channel = (ChannelExec) session.openChannel("exec");
			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "[SSH command sent] " + command); //$NON-NLS-1$
	        channel.setCommand(command + "\n");
	        channel.setErrStream(System.err);  
	          
	        if(!session.isConnected()){   
	        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "[SSH client] Session is not connected!"); //$NON-NLS-1$
	            return null;
	        }   
	        channel.connect(connectTimeout);  
	        while(!channel.isConnected()){  
	            try {  
	            	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "[SSH client] Channel is not connected"); //$NON-NLS-1$
	                Thread.sleep(1000);  
	                channel.connect(connectTimeout);  
	            } catch (InterruptedException e) {
	            	//e.printStackTrace(); 
	            	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"[SSH client] Thread interrupted exception."); //$NON-NLS-1$
	            }  
	        }  
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"[SSH client] Exec channel connected"); //$NON-NLS-1$
	        
	        InputStream in = channel.getInputStream();
	        
	        byte[] tmp=new byte[1024];	        
	        StringBuffer buf = new StringBuffer();	        
	        String responseStr="";
	        while (true) {  
	            while (in.available()>0) {  
	              int i = in.read(tmp, 0, 1024);  
	              if (i<0) break;  
	              buf.append(new String(tmp, 0, i));  
	            }  
	            responseStr = buf.toString();
	            if(channel.isClosed()){  
	            	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "exit-status: "+channel.getExitStatus()); //$NON-NLS-1$ 
	              break;  
	            }  
	            try {Thread.sleep(1000);} catch(Exception ee){}  
	        }  
	          
	        channel.disconnect();  
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,"[SSH client] Exec channel disconnected"); //$NON-NLS-1$  
	        
	        if ( responseStr.length() == 0 ) 
	        	return null;
	                
	        if ( responseStr.contains("\n" ) ) {
	        	lines = responseStr.split("\n");
	        } else if ( responseStr.contains("\r")) {
	        	lines = responseStr.split("\r");
			} else {
				lines = new String[1];
	        	lines[0] = responseStr;
			}	        
		} catch ( JSchException e1) {
			//e1.printStackTrace();
			String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed");
			DeviceExecutionException ex = new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed");
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
			
		} catch ( IOException e2 ) {
	        String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed");
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message); //$NON-NLS-1$            	
			AMPIOException ex = new AMPIOException(message, e2, "wamt.amp.defaultV3Provider.CommandsImpl.readDataFailed");
			logger.throwing(CLASS_NAME, METHOD_NAME, ex);
			throw ex;
		}
		
		return lines; 
    }
	
	private String findMatchingLine(String lines[], String match) {
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains(match))
				return lines[i];
		}

		return null;
	}

	private String getColonValue(String lines[], String key) {
		String line = findMatchingLine(lines, key);
		if (line != null) {
			int i = line.indexOf(key);

			if (i != -1) {
				i += key.length();
				String val = line.substring(i + 1).trim();
				return val;
			}
		}

		return null;
	}	

	public Hashtable backupDevice(DeviceContext device, String cryptoCertificateName, byte[] cryptoImage,
			String secureBackupDestination, boolean includeISCSI, boolean includeRaid) throws AMPIOException,
			InvalidCredentialsException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void deleteDomain(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public DeviceMetaInfo getDeviceMetaInfo(DeviceContext device) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {

		final String METHOD_NAME = "getDeviceMetaInfo"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);

		DeviceMetaInfo dmi = null;
		Session session = null;

		try {
			session = connect(device);
	
			// Send the commands, and Read the results until "Console>" in multiple lines			
			String lines[] = executeCommand(session, "show version");
	
			//dumpLines(lines);
			
			String xc10version = findMatchingLine(lines, "XC10 ");
			
			if (xc10version != null)
				xc10version = xc10version.split(" ")[1];
	
			String serialNumber = getColonValue(lines, "Serial number");
			String modelType = getColonValue(lines, "Machine type");
			ModelType mt = ModelType.fromString(modelType.substring(0, 4));
			DeviceType dt = DeviceType.fromString("XC10");
			String deviceName = modelType + "_" + serialNumber;
			String ampVersion = "0.0";
			String hwOptions = modelType.substring(4);
			StringCollection featureLicenses = new StringCollection();
			
			int supportedCommands[] = new int[]{
					Commands.GET_DEVICE_METAINFO, Commands.PING_DEVICE, 
					Commands.REBOOT, Commands.SET_FIRMWARE_STREAM};
	
			dmi = new DeviceMetaInfo(deviceName, serialNumber, ampVersion, mt, hwOptions, 443, dt, xc10version,
					featureLicenses, supportedCommands);
		} finally {
			if ( session != null )
				session.disconnect();
		}

		return dmi;
	}

	public byte[] getDomain(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public URL getDomainDifferences(String domainName, byte[] configImage1, byte[] configImage2, DeviceContext device)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public String[] getDomainList(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException,
			AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public DomainStatus getDomainStatus(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public ErrorReport getErrorReport(DeviceContext device) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public String[] getKeyFilenames(DeviceContext device, String domainName) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public String getSAMLToken(DeviceContext device, String domainName) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public boolean isDomainDifferent(String domainName, byte[] configImage1, byte[] configImage2, DeviceContext device)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public PingResponse pingDevice(DeviceContext device, String subscriptionID) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		final String METHOD_NAME = "pingDevice"; //$NON-NLS-1$
		// Ignore the subscription ID, for subscription is not supported on XC10
		logger.entering(CLASS_NAME, METHOD_NAME, device);


		Session session = connect(device);		
		session.disconnect();
		
		return new PingResponse(com.ibm.datapower.amt.amp.SubscriptionState.NONE);
	}

	public void quiesceDevice(DeviceContext device, int timeout) throws AMPIOException, InvalidCredentialsException,
			AMPException {
		throw new UnsupportedOperationException();
	}

	public void quiesceDomain(DeviceContext device, String domain, int timeout) throws AMPIOException,
			InvalidCredentialsException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void reboot(DeviceContext device) throws InvalidCredentialsException, DeviceExecutionException,
			AMPIOException, AMPException {
		final String METHOD_NAME = "reboot"; //$NON-NLS-1$

		logger.entering(CLASS_NAME, METHOD_NAME, device);
		
		Session session = null;
		try {
			session = connect(device);		
			String lines[] = executeCommand(session, "device restart");
			String result = findMatchingLine(lines, "Ok");
			executeCommand(session, "exit");
				
			if (result == null){
				String message = Messages.getString("wamt.amp.defaultV3Provider.CommandsImpl.rebootFailed");
				throw new DeviceExecutionException(message, "wamt.amp.defaultV3Provider.CommandsImpl.rebootFailed");
			}
		} finally {
			session.disconnect();
		}
 
	}

	public void restartDomain(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void restoreDevice(DeviceContext device, String cryptoCredentialName, boolean validate,
			URI secureBackupSource, Hashtable<String, byte[]> backupFilesTable) throws AMPIOException,
			InvalidCredentialsException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void setDomain(DeviceContext device, String domainName, byte[] domainImage, DeploymentPolicy policy)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException,
			DeletedException {
		throw new UnsupportedOperationException();
	}

	public void setFile(DeviceContext device, String domainName, String filenameOnDevice, byte[] contents)
			throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException,
			AMPException {
		throw new UnsupportedOperationException();
	}

	public void setFirmware(DeviceContext device, byte[] firmwareImage) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void setFirmware(DeviceContext device, InputStream inputStream) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		final String METHOD_NAME = "setFirmware"; //$NON-NLS-1$		
		Session session = null;		
		
		try {
			session = connect(device);
			String lines[] = executeCommand(session, "show version");
			String result = findMatchingLine(lines, "XC10");
			
			if (result == null){
				String message = Messages.getString("wamt.amp.defaultProvider.SOAPHelper.invalidUNPass");			
				DeviceExecutionException dee = new DeviceExecutionException(message, "wamt.amp.defaultProvider.SOAPHelper.invalidUNPass");
				
				throw dee;
			}
			
			HTTPListener hl = null;
			
			try {
				hl = new HTTPListener(inputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				AMPException ae = new AMPException("Failed to create local HTTP listener.", e);
				throw ae;
			}
			
			// get the local IP address of an interface.
	        String ipAddrString = Configuration.get(Configuration.KEY_HTTP_LISTENER_IP_ADDRESS);
	        
	        if (ipAddrString != null && (ipAddrString.length() >0)) {
	            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Binding to local IP address " + ipAddrString); //$NON-NLS-1$
	        } else {
	            // search for one. Was a particular interface requested?
	            String ipInterface = Configuration.get(Configuration.KEY_HTTP_LISTENER_IP_INTERFACE);
	            
	            try {
					ipAddrString = Utilities.getLocalIPAddress(ipInterface);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					AMPException ae = new AMPException("Failed to get local IP address.", e);
					throw ae;
				}
	        }
				
			lines = executeCommand(session, "file get http://" + ipAddrString +":"+hl.getPort() + "/d.fw d.fw");
			// The actual line would look like "Wrote 27551 bytes to local storage"
			result = findMatchingLine(lines, "Wrote ");
			
			if (result == null){
				DeviceExecutionException dee = new DeviceExecutionException("Unable to transfer firmware to the device.");
				
				throw dee;
			}			
			
			lines = executeCommand(session, "firmware upgrade d.fw");
			result = findMatchingLine(lines, "Upgrading ");
			
			if (result == null) {
				DeviceExecutionException dee = new DeviceExecutionException("Failed to initiate upgrade from the device.");
				
				throw dee;
			}
		} finally {
			if ( session != null )
				session.disconnect();
		}
	}

	public void startDomain(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void stopDomain(DeviceContext device, String domainName) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public SubscriptionResponseCode subscribeToDevice(DeviceContext device, String subscriptionId,
			StringCollection topics, URL callback) throws InvalidCredentialsException, AMPIOException,
			DeviceExecutionException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void unquiesceDevice(DeviceContext device) throws AMPIOException, InvalidCredentialsException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void unquiesceDomain(DeviceContext device, String domain) throws AMPIOException,
			InvalidCredentialsException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void unsubscribeFromDevice(DeviceContext device, String subscriptionID, StringCollection topics)
			throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException,
			AMPException {
		throw new UnsupportedOperationException();
	}

	public void deleteFile(DeviceContext device, String domainName, String fileNameOnDevice)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}



	public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName,
			String fileDomainName, String fileNameOnDevice, ConfigObject[] objects) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public InterDependentServiceCollection getInterDependentServices(DeviceContext device, String domainName,
			byte[] packageImage, ConfigObject[] objectArray) throws NotExistException, InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public ReferencedObjectCollection getReferencedObjects(DeviceContext device, String domainName, String objectName,
			String objectClassName) throws NotExistException, InvalidCredentialsException, DeviceExecutionException,
			AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}



	public ConfigService[] getServiceListFromExport(DeviceContext device, String fileDomainName, String fileNameOnDevice)
			throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException,
			AMPException {
		throw new UnsupportedOperationException();
	}

	public ConfigService[] getServiceListFromExport(DeviceContext device, byte[] packageImage) throws NotExistException,
			InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void quiesceService(DeviceContext device, String domain, ConfigObject[] objects, int timeout)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void setDomainByService(DeviceContext device, String domainName, ConfigObject[] objects, byte[] domainImage,
			DeploymentPolicy policy, boolean importAllFiles) throws InvalidCredentialsException,
			DeviceExecutionException, AMPIOException, AMPException, DeletedException {
		throw new UnsupportedOperationException();
	}

	public void setDomainByService(DeviceContext device, String domainName, ConfigObject[] objects,
			String fileDomainName, String fileNameOnDevice, DeploymentPolicy policy, boolean importAllFiles)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException,
			DeletedException {
		throw new UnsupportedOperationException();
	}

	public void setFirmware(DeviceContext device, byte[] firmwareImage, boolean acceptLicense)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void setFirmware(DeviceContext device, InputStream inputStream, boolean acceptLicense)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void startService(DeviceContext device, String domainName, ConfigObject[] objects)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void stopService(DeviceContext device, String domainName, ConfigObject[] objects)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public void unquiesceService(DeviceContext device, String domainName, ConfigObject[] objects)
			throws InvalidCredentialsException, DeviceExecutionException, AMPIOException, AMPException {
		throw new UnsupportedOperationException();
	}

	public DeleteObjectResult[] deleteService(DeviceContext device, String domainName, String objectName,
			String objectClassName, ConfigObject[] excludeObjects, boolean deleteReferencedFiles)
			throws NotExistException, InvalidCredentialsException, DeviceExecutionException, AMPIOException,
			AMPException {
		// TODO Auto-generated method stub
		return null;
	}

	public RuntimeService[] getServiceListFromDomain(DeviceContext device, String domainName)
			throws DeviceExecutionException, AMPIOException, AMPException {
		// TODO Auto-generated method stub
		return null;
	}
}
