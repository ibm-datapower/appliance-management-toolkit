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


package com.ibm.datapower.amt.clientAPI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.logging.LoggerHelper;


/**
 * The ServiceConfiguration is created for the services in the source configuration to be deployed.
 * @see ServiceDeployment
 */
public class ServiceConfiguration {	
	protected static final String CLASS_NAME = ServiceConfiguration.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;

	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}	
	
	private URLSource urlSource = null;
	private Vector<ConfigService> allConfigSvc = null;
	private ConfigService[] configSvcDeploy = null;
	private static final Locale en = new Locale("en");

	/**	 
	 * Construct a ServiceConfiguration with the URL of source configuration. 
	 * The schemes of url only support "file:///", "http://", "https://" and "device://" in this release. 
	 * URISyntaxException exception is thrown if the urlSource is not valid.
	 * 
	 * @param urlSource is a URLSource that points to the configuration that should be deployed to the Device for service deployment 
	 * 
	 * @throws URISyntaxException
	 */
	public ServiceConfiguration(URLSource urlSource) throws URISyntaxException {
		//For checking urlSource, some codes are the same as in URLSource()		
		String SCHEME_HTTP     = "http";
		String SCHEME_HTTPS    = "https";
		String SCHEME_FILE     = "file";
		String SCHEME_DEVICE   = "device";		
		
		String urlString = urlSource.getURL();
		URI uri = new URI(urlString);
		
		boolean badURI = false;		
		String scheme = uri.getScheme();
		if ( scheme != null ) {
			scheme = scheme.toLowerCase(en);
		}
		// Only support "file:///", "http://", "https://" and "device://"
		if ( !SCHEME_FILE.equals(scheme) && SCHEME_HTTP.equals(scheme) && SCHEME_HTTPS.equals(scheme) && SCHEME_DEVICE.equals(scheme) ) {
			throw new URISyntaxException(scheme, Messages.getString("wamt.clientAPI.URLSource.invalid"));
		}
		// For all schemes except file, ensure there is a valid host name
		if ( !SCHEME_FILE.equals(scheme) && uri.getHost() == null ) {
			throw new URISyntaxException(scheme, Messages.getString("wamt.clientAPI.URLSource.invalidHost"));
		}		
		if (SCHEME_DEVICE.equals(scheme)) {
			String path = uri.getPath();
			String [] paths = path.split("/");
			if (paths.length == 2) {
				//This number of paths is OK, proceed	
			} else {
				badURI = true;
			}
	    }
		if (badURI) {
			//TODO - pull "reason" from a resource bundle
			throw new URISyntaxException(urlString, "The device URI had an invalid number of paths");
		}
		this.urlSource = urlSource;
		this.allConfigSvc = new Vector<ConfigService>();
	}	

	/**
	 * Get all available services in the Service Configuration for deployment. 
	 * The result are cached for later invocation {@link #setServicesForDeployment(ConfigService[])}
	 * <p>
	 * Note: The firmware version of parameter device must be 5.0.0 or higher.
	 * 
	 * @param device The device is used to get the available service in the source configuration
	 * 
	 * @return a ConfigService array contains all available service(s) to deploy 
	 * 
	 * @throws AMPException
	 * @throws DeletedException
	 * @throws UnsuccessfulOperationException
	 * @throws IOException
	 * @throws DeletedException
	 * @throws NotExistException
	 * @throws InUseException
	 * @throws InvalidParameterException
	 */
	public ConfigService[] getAvailableServices(Device device) throws AMPException, DeletedException, UnsuccessfulOperationException, 
		IOException, DeletedException, NotExistException, InUseException, InvalidParameterException {
		final String METHOD_NAME = "getAvailableServices"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
		ConfigService[] result = null;
		
		// Only if the FW version is 5.0.0 or later
        if (!device.meetsMinimumFirmwareLevel(MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM)) {
        	Object[] args = new Object[] {device.getSymbolicName(), MinimumFirmwareLevel.MINIMUM_FW_LEVEL_FOR_SLCM, METHOD_NAME }; //$NON-NLS-1$
        	String message = Messages.getString("wamt.clientAPI.Device.unsupportedFWLevel", args); //$NON-NLS-1$
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            
            throw new UnsuccessfulOperationException(new UnsupportedVersionException(message, "wamt.clientAPI.Device.unsupportedFWLevel", args));
		}
        
        Commands commands = device.getCommands();
        try {
        	result = commands.getServiceListFromExport(device.getDeviceContext(), this.urlSource.getBlob().getByteArray());
        	if ( result != null ) {            	
            	for ( ConfigService configService : result ) {            		
            		this.allConfigSvc.add(configService);
            	}
            }
        } catch ( NotExistException e ) {
        	throw e;
        }  
        // Set all available services by default
        this.configSvcDeploy = result;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
		return result;
	}
	
	/**
	 * Set the services to be deployed. 
	 * The parameter services must be the subset of services returned from {@link ServiceConfiguration#getAvailableServices(Device)},
	 * meaning the null is not allowed. 
	 * <p>
	 * By default, all services in the source configuration (returned from {@link ServiceConfiguration#getAvailableServices(Device)} ) 
	 * will be deployed if this method is not called.
	 * 
	 * @param services A service array contains service(s) to be deployed
	 * @see #getServicesForDeployment()
	 */
	public void setServicesForDeployment(ConfigService[] services) throws NotExistException, InvalidParameterException {
		final String METHOD_NAME = "setServicesForDeployment"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
		// Check the services
		if ( services == null ) {
			String message = Messages.getString("wamt.clientAPI.Service.invalidConfigSvc");
			throw new InvalidParameterException(message,"wamt.clientAPI.Service.invalidConfigSvc");
		}
		// Check if the services exist
		for ( ConfigService cs : services ) {
			if ( !this.allConfigSvc.contains(cs) ) {
				String message = Messages.getString("wamt.clientAPI.Service.svcNotExist", cs.getName());
				throw new NotExistException(message,"wamt.clientAPI.Service.svcNotExist", cs.getName());
			}
		}
		if ( services != null )
			this.configSvcDeploy = services.clone();
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Get the services to be deployed. 
	 * The result is the same as the {@link #getAvailableServices(Device)} if
	 *  {@link ServiceConfiguration#setServicesForDeployment(ConfigService[])} was not invoked.
	 * 
	 * @return a ConfigService array contains service(s) to be deployed 
	 * @see #setServicesForDeployment(ConfigService[])
	 */
	public ConfigService[] getServicesForDeployment() {
		final String METHOD_NAME = "getServicesForDeployment"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        ConfigService[] result = null;
        
        if ( this.configSvcDeploy != null )
        	result = this.configSvcDeploy.clone();
        
        logger.exiting(CLASS_NAME, METHOD_NAME);        
		return (result);
	}
	
	URLSource getURLSource() {
		return this.urlSource;
	}
}
