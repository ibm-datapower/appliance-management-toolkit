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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A URLSource is a convenience class to access blob objects from various schemes
 * e.g. http, file, device. 
 * <p>
 * Usage:
 * <p/>
 *  To reference a file <br/> 
 *    --> <code>new URLSource("file:///C:/DataPowerData/my-config-source.zip");</code>
 * <p/>
 *  To reference a resource via http <br/>
 *    --> <code>new URLSource("http://host:port/my-config-source.zip");</code>
 * <p/>
 *  To reference domain on a device <br/>
 *    --> <code>new URLSource("device://device-host/myDomain");</code>
 * <p/>
 *  To reference a domain version in the repository <br/>
 *    --> <code>new URLSource("repos://device-host/myDomain/domain-version/2");</code>
 * <p/>
 *  To reference a deployment policy version in the repository <br/>
 *    --> <code>new URLSource("repos://device-host/myDomain/policy-version/2");</code>
 * <p>
 * Note: Using the 'device' scheme or 'repos' scheme requires that a corresponding 
 * {@link Device} object be created in the Manager. The <code>device-host</code> must be identical to 
 * the <code>hostname</code> parameter used in {@link Device#createDevice(String, String, String, String, int)}.
 * Host names must be valid host names per <a href="http://tools.ietf.org/html/rfc952">RFC 952</a>
 * and <a href="http://tools.ietf.org/html/rfc1123">RFC 1123</a>.
 * <p>
 * 
 * @version SCM ID: $Id: URLSource.java,v 1.9 2011/04/06 18:20:03 bschreib Exp $
 * @see Blob
 */
public class URLSource {
	private volatile URI uri = null;
    private boolean hasVersionPath = false; 
    private int     version = 0; 

	public static final String SCHEME_HTTP     = "http";
	public static final String SCHEME_HTTPS    = "https";
	public static final String SCHEME_FILE     = "file";
	public static final String SCHEME_DEVICE   = "device";
	public static final String SCHEME_REPOS    = "repos";

	public static final String URI_PATH_DOMAINVER   = "domain-version";
	public static final String URI_PATH_DEPPOL_VER  = "policy-version";

	public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final Locale en = new Locale("en");
    
    protected static final String CLASS_NAME = URLSource.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
	
    /**
     * Create a new URLSource object with the provided spec (URL).
     * 
     * @param spec - the URL of the resource to be addressed by this URLSource 
     * @see Blob
     * 
     */
	public URLSource(String specs) throws URISyntaxException{
		
		
		String tempSpec =specs; 
		// account for spaces in directory specified
		if (tempSpec.startsWith(SCHEME_FILE)){
			tempSpec = tempSpec.replace(" ", "%20");
			
		}
		
		URI uri = new URI(tempSpec);
		
		boolean badURI = false;
		
		String scheme = uri.getScheme();
		if (scheme!=null) {
			scheme = scheme.toLowerCase(en);
		}
		
		// For all schemes except file, ensure there is a valid host name
		if (!SCHEME_FILE.equals(scheme) && uri.getHost() == null) {
			throw new URISyntaxException(specs, Messages.getString("wamt.clientAPI.URLSource.invalidHost"));
		}
		
		if (SCHEME_DEVICE.equals(scheme)) {
			String path = uri.getPath();
			String [] paths = path.split("/");

			if (paths.length == 2) {
				//This number of paths is OK, proceed	
				uri = normaliseDevice(uri);
			} else {
				badURI = true;
			}
	    } else if (SCHEME_REPOS.equals(scheme)) {
			String path = uri.getPath();
			String [] paths = path.split("/");

			if (paths.length == 4) {
				//The paths[3] value is OK if it does not throw a NumberFormatException
				if ((URI_PATH_DOMAINVER.equalsIgnoreCase(paths[2])) || (URI_PATH_DEPPOL_VER.equalsIgnoreCase(paths[2]))) {
					version = Integer.parseInt(paths[3]);
					hasVersionPath = true;
				} else {
					badURI = true;
				}
				
			} else {
				badURI = true;
			}
	    	
	    }
		
		if (badURI) {
			//TODO - pull "reason" from a resource bundle
			throw new URISyntaxException(specs, "The device URI had an invalid number of paths");
		}
		
		this.uri = uri;
	}
	
	/**
	 * Ensures that a device URI is of the format device://device-host/myDomain converting URIs of
	 * the format device://device-primary-key/myDomain if necessary.
	 *   
	 * @param uri the original URI
	 * @return the normalised URI
	 */
	private URI normaliseDevice(URI uri) {
		
        final String METHOD_NAME = "normaliseDevice"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, uri);
		
		Manager mgr = Manager.internalGetInstance();
		Device[] devices = mgr.getAllDevices();
		String uriHost = uri.getHost().toLowerCase(en);
		
		// Attempt to find device based on hostname
		boolean deviceFound = false;
		for (Device device: devices) {
			try {
				String deviceHost = device.getHostname().toLowerCase(en);
	    		if (deviceHost.contains(uriHost)) {
					logger.finest("Found device with matching hostname " + deviceHost);
					deviceFound = true;
					break;
				}
			} catch (DeletedException e) {
				// Ignore device if deleted
			}
		}
		
		// If device not found based on hostname, search based on primary key
		if (!deviceFound) {
			logger.finest("Device with hostname " + uriHost + " not found - checking primary keys");
			for (Device device: devices) {
				try {
					String devicePrimaryKey = device.getPrimaryKey();
		    		if (devicePrimaryKey.equalsIgnoreCase(uriHost)) {
						deviceFound = true;
						try {
							logger.finest("Normalising primary key based URI " + uri);
							uri = new URI(uri.getScheme(), device.getHostname(), uri.getPath(), uri.getFragment());
							logger.finest("Normalised primary key based URI to " + uri);
						} catch (URISyntaxException e) {
							logger.finest("Failed to normalise URI " + uri + ":" + e.getLocalizedMessage());
							// Fall back to existing URI
						}
						break;
					}
				} catch (DeletedException e) {
					// Ignore device if deleted
				}
			}
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, uri);
		return uri;
	}

	/**
     * Use this method to determine the URL referenced by this object.
     * 
     */
	public String getURL() {
		return this.uri.toString();
	}
	
    /**
     * Use this method to determine the scheme of the URL referenced by this object.
     * @return the scheme (e.g. http, file, etc of the URL) 
     * @see Blob
     * 
     */
	public String getScheme() {
		return this.uri.getScheme();
	}

    /**
     * Use this method to determine if the URLSource refers to a version
     * @return boolean indication that this URLSource points to a version 
     * @see Blob
     * 
     */
	public boolean hasVersionPath() {
		return this.hasVersionPath;
	}

    /**
     * Use this method to determine the version referred to by this URLSource
     * @return int version 
     * @see Blob
     * 
     */
	public int getVersion() {
		return this.version;
	}

    /**
     * Use this method to determine the "last modified" time of the 
     * source referenced by this object. This is useful to compare 
     * persisted time stamps to see if the blob should be retrieved 
     * again
     * 
     * @return long - can be used to build a Date object  
     * @throws IOException 
     * @see Blob
     * @see java.util.Date
     * 
     */
	public long getLastModified() throws IOException {
		
		long lastModified = 0;
		
		String scheme = uri.getScheme();
		if (scheme!=null) {
			scheme = scheme.toLowerCase(en);
		}

		logger.fine("SCHEME=" + scheme);
		
		if (SCHEME_FILE.equals(scheme)) {
			File thisFile = new File(this.uri);
			lastModified = thisFile.lastModified();
		} else if ((SCHEME_HTTP.equals(scheme)) || (SCHEME_HTTPS.equals(scheme))  ) {
			try {
				URLConnection urlConn = uri.toURL().openConnection();
				lastModified = urlConn.getHeaderFieldDate("Last-Modified", 0); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				//It is unlikely that this exception will ever happen 
				//So eat the exception and the method will return 0. This 
				//problem will be noticed at domain deploy time anyway.  
			}
		}
		
		return lastModified;
	}

	/**
     * This method returns a DeploymentPolicyVersion object from the device associated 
     * with this URLSource. If the source is not of the correct scheme or an
     * error occurs, the result is null.
     *  
     * @return the DeploymentPolicyVersion associated with the device at this source. 
     *  
     * 
     * @see DeploymentPolicyVersion
     * 
     */
	protected DeploymentPolicyVersion getDeploymentPolicyVersion() { //throws IOException, DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException{
		return (DeploymentPolicyVersion) getVersionObject(URI_PATH_DEPPOL_VER);
	}

	/**
     * This method returns a DomainVersion object from the device associated 
     * with this URLSource. If the source is not of the correct scheme or an
     * error occurs, the result is null.
     *  
     * @return the DomainVersion associated with the device at this source. 
     *  
     * 
     * @see DomainVersion
     * 
     */
	protected DomainVersion getDomainVersion() { //throws IOException, DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException{
		return (DomainVersion) getVersionObject(URI_PATH_DOMAINVER);
	}

	/**
     * This private method does the work to support getDomainVersion and
     * getDeploymentPolicyVersion. It extracts a Version object from the device associated 
     * with this URLSource. If the source is not of the correct scheme or an
     * error occurs, the result is null.
     * 
     * Note that a device can only be obtained if the URL scheme is SCHEME_DEVICE or SCHEME_REPOS.
     *  
     * @param inType, either URI_PATH_DOMAINVER or URI_PATH_DEPPOL_VER indicating the type of version object.
     * @return the Version Object associated with the device at this source. 
     * 
     */
	private Object getVersionObject(String inType) { //throws IOException, DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException{
		
        final String METHOD_NAME = "getVersionObject"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
  		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "inType " + inType); 
        Object result = null;

        String scheme = uri.getScheme();
		if (scheme != null) {
			scheme = scheme.toLowerCase(en);
		}
		
		// Parse URI data 
		String uriHost = this.uri.getHost();
		String path = this.uri.getPath();
		String [] paths = path.split("/");
		String domainName = paths[1];
		Device device = null;
		logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
				    "URI data : scheme " + scheme +
				    ", uriHost " + uriHost +
				    ", domainName " + domainName +
				    ", type " + paths[2] +
				    ", versionNumber " + paths[3]);
		
		try {
			if (SCHEME_DEVICE.equals(scheme) ||
				SCHEME_REPOS.equals(scheme)) { // scheme types that should provide device
					device = findDevice(uriHost);
				} // end, scheme types 
			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Device " + device);
			if (device != null) { // have a device
				Domain domain = device.getManagedDomain(domainName);
			    int versionNumber = Integer.parseInt(paths[3]);
			    if (URI_PATH_DOMAINVER.equals(inType)) {
					result = (Object) domain.getVersion(versionNumber);
			    }
			    else if (URI_PATH_DEPPOL_VER.equals(inType)) {
					result = (Object) domain.getDeploymentPolicy().getVersion(versionNumber);
			    }
			} // end, have a device
		} catch (Exception e) {
			logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
			result = null;
		}

        logger.exiting(CLASS_NAME, METHOD_NAME);
        return result;
	}
	
    /**
     * This method returns a Blob object from the spec sent to the constructor 
     * (e.g. URL source). This method will B64 encode the source if a file:, http: 
     * or https: scheme is specified in the URL. It will not encode if the URL 
     * scheme is device: or repos: since these are already encoded.
     *  
     * @return the Blob constructed from the source. 
     *  
     * 
     * @see Blob
     * 
     */
	public Blob getBlob() throws IOException, DeletedException, NotExistException, InUseException, InvalidParameterException, AMPException{
		
        final String METHOD_NAME = "getBlob"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        Blob result = null;

        String scheme = uri.getScheme();
		if (scheme!=null) {
			scheme = scheme.toLowerCase(en);
		}
	
		logger.fine("SCHEME=" + scheme);
		
		// NOTE: device object has to exist, since we need a username/password. 
		String uriHost = this.uri.getHost();
		String path = this.uri.getPath();
		String [] paths = path.split("/");
		
		if (SCHEME_FILE.equals(scheme)) {
			File thisFile = new File(this.uri);
			Blob fileBlob = new Blob(thisFile); 
			result = fileBlob.getBase64Encoded();
		} else if (SCHEME_DEVICE.equals(scheme)) {
			Device device = findDevice(uriHost);
			
	    	Blob blob = null;
			try {
		        Commands commands = device.getCommands();
		        DeviceContext deviceContext = device.getDeviceContext();
		        String domainName = paths[1];
		        byte[] bytes = commands.getDomain(deviceContext, domainName);
		        blob = new Blob(bytes);
		        // get rid of one reference to "bytes"
		        bytes = null;
			} catch (AMPException e) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
				//e.printStackTrace();
				throw e;
			} catch (DeletedException e) {
				logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
				//e.printStackTrace();
				throw e;
			}
    		result = blob;
		} else if (SCHEME_REPOS.equals(scheme)) {
			Device device = findDevice(uriHost);
			String domainName = paths[1];
			Domain domain = device.getManagedDomain(domainName);
			String version = paths[3];
			
			if (URI_PATH_DOMAINVER.equalsIgnoreCase(paths[2])) {
			    int versionNumber = Integer.parseInt(version);
				DomainVersion domainVerison = (DomainVersion)domain.getVersion(versionNumber);
				
				if (domainVerison == null) {
					String message = Messages.getString("wamt.clientAPI.Domain.verNotExist");
					throw new NotExistException(message,"wamt.clientAPI.Domain.verNotExist");
				}
				
				result = domainVerison.getBlob();
			} else {
			    int versionNumber = Integer.parseInt(version);
				DeploymentPolicyVersion deploymentPolicyVerison = (DeploymentPolicyVersion)domain.getDeploymentPolicy().getVersion(versionNumber);
				result = deploymentPolicyVerison.getBlob();
			}
		} else {
			URL thisURL = uri.toURL();
			Blob urlBlob = new Blob(thisURL);
			result = urlBlob.getBase64Encoded();
		}
		
        logger.exiting(CLASS_NAME, METHOD_NAME);
		return result;
	}
	
    /**
     * This method returns a Blob object from the spec sent to the constructor 
     * (e.g. URL source). A separate method was needed because getBlob() will B64 
     * encode the file, http and https schemes and firmware images should not be 
     * encoded. 
     * 
     * @return the Blob constructed from the source
     * 
     * @throws MalformedURLException 
     * @throws InvalidParameterException 
     * 
     * @see Blob
     * 
     */
	public Blob getFirmwareBlob() throws MalformedURLException, InvalidParameterException{
		
        final String METHOD_NAME = "getBlob"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        Blob result = null;
        
		String scheme = uri.getScheme();
		if (scheme!=null) {
			scheme = scheme.toLowerCase(en);
		}

		logger.fine("SCHEME=" + scheme);

		if (SCHEME_DEVICE.equals(scheme) || SCHEME_REPOS.equals(scheme)) {
			String message = Messages.getString("wamt.clientAPI.URLSource.invalidSchemeForFW");
			throw new InvalidParameterException(message,"wamt.clientAPI.URLSource.invalidSchemeForFW");
		}

		if (SCHEME_FILE.equals(scheme)) {
			File thisFile = new File(this.uri);
			Blob fileBlob = new Blob(thisFile);
			result = fileBlob;
		} else {
			URL thisURL = uri.toURL();
			Blob urlBlob = new Blob(thisURL);
			result = urlBlob;
		}
		
        logger.exiting(CLASS_NAME, METHOD_NAME);
		return result;
	}

	private Device findDevice(String deviceName) throws DeletedException, NotExistException {
		Device device = null;		
		boolean deviceFound = false;
		
		// If the deviceName is null, let it fall through to throw a NotExistException
		if (deviceName != null) {
			Manager mgr = Manager.internalGetInstance();
			Device[] devices = mgr.getAllDevices();
			deviceName = deviceName.toLowerCase(en);
			for (int i=0; i < devices.length; i++) {
				String objHostName = devices[i].getHostname().toLowerCase(en);
	//			String objDisplayName = devices[i].getDisplayName().toLowerCase(en);
	//			String objSymbolicName = devices[i].getSymbolicName().toLowerCase(en);
				
				//The device specifier can be the hostname, displayname, or symbolicname.
				//if ((objHostName.contains(deviceName)) || (objDisplayName.contains(deviceName)) || (objSymbolicName.contains(deviceName))) {
	    		if (objHostName.contains(deviceName)) {
					device = devices[i];
					deviceFound = true;
					break;
				}
			}
		}
		
		if (deviceFound == false) {
			//Didn't find the device.....
			String message = Messages.getString("wamt.clientAPI.URLSource.noDevice");
			throw new NotExistException(message,"wamt.clientAPI.URLSource.noDevice");
		}
		
		return(device);
		
	}
}
