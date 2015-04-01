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

package com.ibm.datapower.amt.dataAPI.local.filesystem;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.NotEmptyInRepositoryException;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredTag;
import com.ibm.datapower.amt.logging.LoggerHelper;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDevice;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPDomain;
import com.ibm.datapower.lfs.schemas.x70.datapowermgr.DPTag;

public class StoredTagImpl implements StoredTag {
	private static final String CR = Constants.COPYRIGHT_2012_2013;

	private String name = null;
	private String value = null;
	private Hashtable<String, StoredTag> allTags = null;
	private Hashtable<String, StoredDomain> domains = null;
	private Hashtable<String, StoredDevice> devices = null;

	// The corresponding XML object. It is created when this object
	// is being persisted into an XML file.
	private DPTag xmlObject = null;
	// XML object counter for this class. It is used in XML objects' IDs.
	private static int xmlObjectNum = 0;

	// private static Logger TRACE = RepositoryImpl.TRACE;
	// private static final String className = "StoredManagedSetImpl";
	private static final String XML_CLASS_NAME = "DPTag";

	private static final String CLASS_NAME = StoredManagedSetImpl.class.getName();
	protected final static Logger logger = Logger.getLogger(CLASS_NAME);
	static {
		LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
	}

	StoredTagImpl(Hashtable<String, StoredTag> allTags, String name,
			String value) throws AlreadyExistsInRepositoryException {
		devices = new Hashtable<String, StoredDevice>();
		domains = new Hashtable<String, StoredDomain>();

		this.name = name;
		this.value = value;
		this.allTags = allTags;
		if (allTags.containsKey(getPrimaryKey())) {

			logger.logp(Level.FINEST, CLASS_NAME, "StoredTagImpl", 
                     "already have tag name " + name + " in " + this); //$NON-NLS-1$			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.datapower.amt.dataAPI.StoredTag#getDeviceMembers()
	 */
	public StoredDevice[] getDeviceMembers() {
		StoredDevice[] result = new StoredDevice[this.devices.size()];

		Enumeration e = devices.elements();
		int i = 0;
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			if (o instanceof StoredDeviceImpl) {
				result[i++] = (StoredDeviceImpl) o;
			}
		}
		return result;
	}
	
	public StoredDomain[] getDomainMembers() {
		StoredDomain[] result = new StoredDomain[this.domains.size()];

		Enumeration e = domains.elements();
		int i = 0;
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			if (o instanceof StoredDomainImpl) {
				result[i++] = (StoredDomainImpl) o;
			}
		}
		return result;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

	public void remove(StoredDevice device) {
		this.devices.remove(device.getPrimaryKey());

		if (device instanceof StoredDeviceImpl) {
			StoredDeviceImpl storedDevice = (StoredDeviceImpl) device;
			this.devices.remove(storedDevice.getPrimaryKey());
		}
	}
	
	public void remove(StoredDomain domain) {
		this.domains.remove(domain.getPrimaryKey());

		if (domain instanceof StoredDomainImpl) {
			StoredDomainImpl storedDomain = (StoredDomainImpl) domain;
			this.domains.remove(storedDomain.getPrimaryKey());
		}
	}

	public void delete() throws DatastoreException, NotEmptyInRepositoryException {
		final String methodName = "delete";
		logger.entering(CLASS_NAME, methodName);
		this.allTags.remove(this.getPrimaryKey());
		logger.exiting(CLASS_NAME, methodName);
	}

	/**
	 * <p>
	 * Note: The Local File System implementation uses the name of the
	 * ManagesSet as the unique identifier for this object. The name is
	 * immutable, so there is no <code>setName(String)</code> method.
	 * </p>
	 * <inheritDoc />
	 */
	public String getPrimaryKey() {
		return getName()+":"+getValue();
	}

	public void add(StoredDevice device) {
		final String METHOD_NAME = "add(StoredDevice)";
		if (device != null) {
			this.devices.put(device.getPrimaryKey(), device);
			if (device instanceof StoredDeviceImpl) {
				StoredDeviceImpl storedDevice = (StoredDeviceImpl) device;
				storedDevice.add(this);
			}
		} else {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Attempted to add null device");
		}
	}

	public void add(StoredDomain domain) {
		final String METHOD_NAME = "add(StoredDevice)";
		if (domain != null) {
			this.domains.put(domain.getPrimaryKey(), domain);
			if (domain instanceof StoredDomainImpl) {
				StoredDomainImpl storedDomain = (StoredDomainImpl) domain;
				storedDomain.add(this);
			}
		} else {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Attempted to add null domain");
		}
	}
	
	/*
	 * Reset the object counter of DPClonableDeviceSettingsVersion
	 */
	static void resetXmlObjectNum() {
		xmlObjectNum = 0;
	}

	/*
	 * Delete the corresponding XML object and its descendants
	 */
	void deleteXMLObjects() {
		this.xmlObject = null;

		Enumeration e = this.domains.elements();
		while (e.hasMoreElements()) {
			((StoredDomainImpl) e.nextElement()).deleteXMLObjects();
		}

	}

	/*
	 * Return the corresponding XML object
	 */
	DPTag getXMLObject() {
		return this.xmlObject;
	}
	
	/*
	 * Check if this Tag is used by device or domain
	 */
	boolean isTagged() {
		if ( devices.isEmpty() && domains.isEmpty() ) {
			// the tag is not used, don't persist it
			return false;
		}
		return true;
	}

	/*
	 * Transform a StoredManagedSetImpl object into an XML object for
	 * persistence
	 */
	void toXMLObject(DPTag dpTag) {
		final String methodName = "toXMLObject";
		logger.entering(CLASS_NAME, methodName, this);
		
		this.xmlObject = dpTag;
		dpTag.setId(XML_CLASS_NAME + "_" + xmlObjectNum++);

		dpTag.setName(this.name);
		dpTag.setValue(this.value);
				
		String deviceMembers = "";
		Enumeration e = devices.elements();
		StringBuffer buf = new StringBuffer(deviceMembers);
		while (e.hasMoreElements()) {
			StoredDeviceImpl dev = (StoredDeviceImpl) e.nextElement();
			DPDevice existingDpd = dev.getXMLObject();
			if ( existingDpd != null ) // if device exists
				buf.append(" " + existingDpd.getId()); // deviceMembers += " " + existingDpd.getId();
		}
		deviceMembers = buf.toString();
		dpTag.setDeviceMembers(deviceMembers.trim());
		
		String domainMembers = "";
		e = domains.elements();
		buf = new StringBuffer(domainMembers);
		while (e.hasMoreElements()) {
			StoredDomainImpl domain = (StoredDomainImpl) e.nextElement();
			DPDomain existingDpd = domain.getXMLObject();
			if ( existingDpd != null ) // if domain exists
				buf.append(" " + existingDpd.getId()); //domainMembers += " " + existingDpd.getId();
		}
		domainMembers = buf.toString();
		dpTag.setDomainMembers(domainMembers.trim());

		logger.exiting(CLASS_NAME, methodName, dpTag);
	}

	/*
	 * Transform an XML object into a StoredManagedSetImpl object
	 */
	static void fromXMLObject(DPTag dpTag) throws DatastoreException {
		final String methodName = "fromXMLObject";
		logger.entering(CLASS_NAME, methodName, dpTag);

		StoredTagImpl mTag = null;
		RepositoryImpl ri = RepositoryImpl.getInstance();

		mTag = (StoredTagImpl) ri.createTag(dpTag.getName(), dpTag.getValue());

		String deviceMembers = dpTag.getDeviceMembers();
		if (deviceMembers != null) {
			StringTokenizer dmst = new StringTokenizer(deviceMembers);
			while (dmst.hasMoreTokens()) {
				String device = dmst.nextToken();
				mTag.add((StoredDevice) ri.getMapXmlObjectsToMemObjects().get(device));
			}
		}
		
		String domainMembers = dpTag.getDomainMembers();
		if (domainMembers != null) {
			StringTokenizer dmst = new StringTokenizer(domainMembers);
			while (dmst.hasMoreTokens()) {
				String domain = dmst.nextToken();
				mTag.add((StoredDomain) ri.getMapXmlObjectsToMemObjects().get(domain));
			}
		}

		logger.exiting(CLASS_NAME, methodName, mTag);
	}

}
