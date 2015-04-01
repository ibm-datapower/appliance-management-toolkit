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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.ConfigObject;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.amp.DeviceExecutionException;
import com.ibm.datapower.amt.amp.InterDependentServiceCollection;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * The ServiceDeployment is used to check the inter-dependent service before the deployment, and is used to deploy the service source configuration to device. 
 * Objects of this class are automatically instantiated when
 *  {@link Domain#createServiceDeployment(ServiceConfiguration, URLSource, String, String, boolean)} or 
 *  {@link Domain#createServiceDeployment(ServiceConfiguration, boolean)} is invoked.
 * <p> 
 * The {@link InterDependentServiceCollection} object is created when this object is instantiated, 
 * so the returns of methods 
 * {@link ServiceDeployment#getFilesToBeOverwritten()}, 
 * {@link ServiceDeployment#getInterDependentServices()}, and 
 * {@link ServiceDeployment#getObjectsToBeOverwritten()} are cached at the same time. 
 * Consequently, those returns could be a slight difference when the service is deploying to device.
 */
public class ServiceDeployment {
	
	protected static final String CLASS_NAME = ServiceDeployment.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;

	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}
	
	private Domain domain;
	private ServiceConfiguration svcConfig = null;	
	private DeploymentPolicyService depPol = null;
	private boolean importAllFiles = false;
	
	private InterDependentServiceCollection depService = null;	
	
	/*
	 * Construct object and try to get the InterDependentSvcCollection object
	 */
	ServiceDeployment(Domain domain, ServiceConfiguration svcConfig, DeploymentPolicyService depPol, boolean importAllFiles) 
		throws DeletedException, AMPException, NotExistException, InUseException, InvalidParameterException, IOException {
		final String METHOD_NAME = "ServiceDeployment"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
		this.domain = domain;
		this.svcConfig = svcConfig;
		this.depPol = depPol;
		this.importAllFiles = importAllFiles;
		
		byte[] byteArray = svcConfig.getURLSource().getBlob().getByteArray();
		if ( this.isPresentOn(domain.getDevice()) ) {
			// Try to get the InterDependentServiceCollection if domain is on the device
			this.getInterDependentSvcCollection(domain, byteArray);
		}
	}	
		
	ServiceDeployment(Domain domain, ServiceConfiguration svcConfig, boolean importAllFiles) 
		throws DeletedException, AMPException, NotExistException, InUseException, InvalidParameterException, IOException {
		this.domain = domain;
		this.svcConfig = svcConfig;
		this.importAllFiles = importAllFiles;
		
		byte[] byteArray = svcConfig.getURLSource().getBlob().getByteArray();
		if ( this.isPresentOn(domain.getDevice()) ) {
			// Try to get the InterDependentServiceCollection if domain is on the device
			this.getInterDependentSvcCollection(domain, byteArray);
		}
	}	

	/**
	 * Get all interdependent service(s) when this class is instantiated.
	 * @return the interdependent service(s)
	 * 
	 * @throws IOException 
	 * @throws AMPException 
	 * @throws InvalidParameterException 
	 * @throws InUseException 
	 * @throws NotExistException 
	 * @throws DeletedException 
	 */
	public RuntimeService[] getInterDependentServices() 
		throws DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException, IOException {
		if ( this.depService != null ) {
			return ( this.depService.getInterDependentServices() );
		}
		// try to getInterDependentServices via amp call again, because the domain could be deployed to device after this class is instantiated.
		if ( this.isPresentOn(domain.getDevice()) ) {
			byte[] byteArray = svcConfig.getURLSource().getBlob().getByteArray();
			this.getInterDependentSvcCollection(this.domain, byteArray);
			
			if ( this.depService != null ) // if not null
				return ( this.depService.getInterDependentServices() );
			else // no dependent Service
				return ( new RuntimeService[0]);
		}
		return (new RuntimeService[0]);
	}
	
	/**
	 * Get a file list to be overwritten.
	 * @return the file list to be overwritten
	 * 
	 * @throws IOException 
	 * @throws AMPException 
	 * @throws InvalidParameterException 
	 * @throws InUseException 
	 * @throws NotExistException 
	 * @throws DeletedException 
	 */
	public StringCollection getFilesToBeOverwritten()
		throws DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException, IOException {
		if ( this.importAllFiles ) // just return empty StringCollection, because user wants to import all files.
			return (new StringCollection());
		
		if ( this.depService != null ) {
			return ( this.depService.getFilesToBeOverwritten() );
		}		
		// try to getInterDependentServices via amp call again, because the domain could be deployed to device after this class is instantiated
		if ( this.isPresentOn(domain.getDevice()) ) {
			byte[] byteArray = svcConfig.getURLSource().getBlob().getByteArray();
			this.getInterDependentSvcCollection(this.domain, byteArray);
					
			if ( this.depService != null ) // if not null
				return ( this.depService.getFilesToBeOverwritten() );
			else // no dependent Service
				return (new StringCollection());
		}
		return (new StringCollection()); 
	}
	
	/**
	 * Get ConfigObject(s) to be overwritten.
	 * @return the ConfigObject array to be overwritten.
	 * 
	 * @throws IOException 
	 * @throws AMPException 
	 * @throws InvalidParameterException 
	 * @throws InUseException 
	 * @throws NotExistException 
	 * @throws DeletedException 
	 */
	public ConfigObject[] getObjectsToBeOverwritten()
		throws DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException, IOException {
		if ( this.depService != null ) {
			return ( this.depService.getObjectToBeOverwritten() );
		}
		// try to getInterDependentServices via amp call again, because the domain could be deployed to device after this class is instantiated
		if ( this.isPresentOn(domain.getDevice()) ) {
			byte[] byteArray = svcConfig.getURLSource().getBlob().getByteArray();
			this.getInterDependentSvcCollection(this.domain, byteArray);
			
			if ( this.depService != null ) // if not null
				return ( this.depService.getObjectToBeOverwritten() );
			else  // no dependent Service
				return (new ConfigObject[0]);
		}
		return (new ConfigObject[0]);
	}
	
	/**
	 * Push a service source configuration onto the device.
	 * @return the ProgressContainer object for this long running task
	 */
	public ProgressContainer deployServiceConfiguration() throws DeletedException, FullException, NotManagedException {
		final String METHOD_NAME = "deployServiceConfiguration"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
		ProgressContainer progressContainer = null;

    	// Set and deploy a firmware version.
        BackgroundTask backgroundTask = BackgroundTask.createServiceDeployConfigurationTask(this.domain, this);
        progressContainer = backgroundTask.getProgressContainer();
        
        // Enqueue background task
        Manager manager = Manager.internalGetInstance();
        Device device = this.domain.getDevice();
        ManagedSet managedSet = null;
        if (device != null) {
            managedSet = device.getManagedSet();
        }
        if (managedSet == null) {
            manager.enqueue(backgroundTask, manager);        	
        }
        else {
            manager.enqueue(backgroundTask, managedSet);        	
        }
                
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return (progressContainer);
	}
	
	protected void deployServiceSourceConfigurationAction() 
		throws DeletedException, AMPException, NotExistException, InUseException, InvalidParameterException, IOException,
		UnsuccessfulOperationException, LockBusyException, NotManagedException, NotEmptyException, DatastoreException {
		final String METHOD_NAME = "deploySourceConfigurationAction"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        this.domain.checkIfManaged();		
		Device device = this.domain.getDevice();
		
		//Send a signal that a domain is about to be deployed 
        boolean wasSuccessful = false;
        Signaler signal = new Signaler(device, null, this.domain.getName());
        signal.sendStart();
        
    	device.lockNoWait();
		
    	byte[] bytes;
    	boolean isQuiesced = false;
		try {
			//Quiesce the domain - if quiesce is supported and fails we will not attempt to setDomain
			//other stuff an exception, will of course stop this execution.
			if(isPresentOn(this.domain.getDevice())){
				this.domain.quiesce();
				isQuiesced = true;
			}			
		    // deploy service configuration
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "call setDomain on " + device.getSymbolicName() + " with domain " + this.domain.getName()); //$NON-NLS-1$
            
			Commands commands = device.getCommands();
			bytes = this.svcConfig.getURLSource().getBlob().getByteArray();
			
			// retrieve the deployment policy from the URLSource, if the deployment policy is set
			if ( this.depPol != null ) { 
				this.depPol.getPolicy();
			}
			
			ConfigService[] confService = this.svcConfig.getServicesForDeployment();
			if ( confService == null || confService.length == 0 ) { // no service specified, deploy all services in domain				
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "call setDomain to deploy all services on " + 
	            			device.getSymbolicName() + " with domain " + this.domain.getName()); //$NON-NLS-1$
	            // Try to get and set all available services
	            ConfigService[] configServices = svcConfig.getAvailableServices(device);
	            if ( configServices != null && configServices.length != 0 ) {
		            svcConfig.setServicesForDeployment(configServices);
		            ConfigObject[] coList = new ConfigObject[configServices.length];
					for ( int i=0; i < configServices.length; i++ ) {
						coList[i] = new ConfigObject(configServices[i].getName(), configServices[i].getClassName(), 
										configServices[i].getClassDisplayName(), configServices[i].getUserComment());
					}
					commands.setDomainByService(device.getDeviceContext(), this.domain.getName(), coList, bytes, this.depPol, true);
					//commands.setDomain(device.getDeviceContext(), this.domain.getName(), bytes, this.depPol);
	            }
			}
			else { // deploy services specified
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "call setDomain to deploy services specified on " + 
            			device.getSymbolicName() + " with domain " + this.domain.getName()); //$NON-NLS-1$
				ConfigObject[] coList = new ConfigObject[confService.length];
				for ( int i=0; i < confService.length; i++ ) {
					coList[i] = new ConfigObject(confService[i].getName(), confService[i].getClassName(), 
							confService[i].getClassDisplayName(), confService[i].getUserComment());
				}
				commands.setDomainByService(device.getDeviceContext(), this.domain.getName(), coList, bytes, this.depPol, this.importAllFiles);
			}					
            wasSuccessful = true;            
            this.domain.unquiesce();
            isQuiesced = false;
		}  catch (IOException e) {			
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (AMPException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);			
			throw e;
		} catch (DeletedException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (NotExistException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} catch (InvalidParameterException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;		
		} catch (UnsuccessfulOperationException e) {
			logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			throw e;
		} finally {
			//Fix for #10200: [3663] Deploy configuration fails if done via local file or URL option, 
            //						yet still wipes the existing config
			if ( isQuiesced )
				this.domain.unquiesce(); // Need to unquiesce the domain here in case the exception is thrown
			// cleanup			
            bytes = null;
            
            // release lock 
        	device.unlock();        	
            signal.sendEnd(wasSuccessful);
		}		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/*
	 * Try to get the InterDependentServiceCollection
	 */
	private void getInterDependentSvcCollection(Domain domain, byte[] byteArray) throws DeletedException, AMPException {
		final String METHOD_NAME = "getInterDependentSvcCollection"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
		
		Device device = domain.getDevice();
		Commands commands = device.getCommands();
		DeviceContext deviceContext = device.getDeviceContext();
		
		try {
			ConfigService[] svcConfigList = svcConfig.getServicesForDeployment();
			ConfigObject[] coList = null;
			if ( svcConfigList != null ) {
				coList = new ConfigObject[svcConfigList.length];
				for ( int i=0; i < svcConfigList.length; i++ ) {
					coList[i] = new ConfigObject(svcConfigList[i].getName(), svcConfigList[i].getClassName(), 
												svcConfigList[i].getClassDisplayName(), svcConfigList[i].getUserComment());
				}
			}
			InterDependentServiceCollection IDSC_amp = commands.getInterDependentServices( deviceContext, domain.getName(), byteArray, coList);
			// Fix [12010] getInterDependentServices() does not contain all necessary data:
			// The return from commands.getInterDependentServices does not contain other information like domain and ReferencedObject				
			ArrayList<RuntimeService> serviceList = new ArrayList<RuntimeService>();			
			RuntimeService[] rsv = IDSC_amp.getInterDependentServices();			
			for ( RuntimeService rsv_New : rsv) {
				// set the domain
				serviceList.add(new RuntimeService(domain, rsv_New));
			}
			// Set the ConfigObjectOverwritten
			ArrayList<ConfigObject> configObjectOverwritten = new ArrayList<ConfigObject>();
			ConfigObject[] co = IDSC_amp.getObjectToBeOverwritten();
			for ( ConfigObject co_tmp : co ) {
				configObjectOverwritten.add(co_tmp);
			}
			depService = new InterDependentServiceCollection(serviceList, configObjectOverwritten, IDSC_amp.getFilesToBeOverwritten());
			
		}catch ( DeviceExecutionException e ) { // Amp call fail
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
		}catch ( AMPException e ) {
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
		} 
	}
	
	/*
	 * To check if the domain of this service belongs to is present on device
	 */
	private boolean isPresentOn(Device device) throws DeletedException, AMPException {
        final String METHOD_NAME = "isPresentOn"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, device});
        // use AMP to test the Domain version
        boolean result = false;
        
        Commands commands = this.domain.getDevice().getCommands();
        DeviceContext deviceContext = device.getDeviceContext();
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "invoking getDomainList"); //$NON-NLS-1$
        String[] existingDomainNames = commands.getDomainList(deviceContext);

        String flattenedList = ""; //$NON-NLS-1$
        String thisDomainName = this.domain.getName();
        StringBuffer buf = new StringBuffer(flattenedList);
        for (int i=0; i<existingDomainNames.length; i++) {
            if (existingDomainNames[i].equals(thisDomainName)) {
                result = true;
            }
            if (flattenedList.length() > 0) {
            	buf.append(", "); //$NON-NLS-1$
            }
            buf.append(existingDomainNames[i]); //flattenedList += existingDomainNames[i];
        }
        flattenedList = buf.toString();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, flattenedList);
        logger.exiting(CLASS_NAME, METHOD_NAME, Boolean.valueOf(result));
        return(result);
    }
}
