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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.dataAPI.Repository;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicy;
import com.ibm.datapower.amt.dataAPI.StoredDeploymentPolicyVersion;
import com.ibm.datapower.amt.dataAPI.StoredDevice;
import com.ibm.datapower.amt.dataAPI.StoredDomain;
import com.ibm.datapower.amt.dataAPI.StoredDomainVersion;
import com.ibm.datapower.amt.dataAPI.StoredFirmware;
import com.ibm.datapower.amt.dataAPI.StoredFirmwareVersion;
import com.ibm.datapower.amt.dataAPI.StoredManagedSet;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Allows us to map between clientAPI objects and dataAPI objects so that we
 * don't need to keep a duplicate relationship tree.
 * <p>
 * A number of the objects in the clientAPI are persistable. For example,
 * Domain, DomainVersion, Device, etc. For the complete list of persistable
 * classes, refer to the list of classes that implement the interface
 * {@link Persistable}. Most of the data members in those clientAPI classes are
 * persisted to a datastore, but there are some data members that are not. For
 * example, in the Device class, the hostname, admin userid and password, and
 * like members are persisted. But other members like allDomainNames,
 * actualFirmwareLevel, and managedDomainsOperationStatus are not persisted. So
 * instead of duplicating the persisted members in both the clientAPI class
 * <code>Device</code> and the persisted class <code>StoredDevice</code>,
 * the persisted members are only in the persisted class StoredDevice. This
 * leaves only the non-persisted members in the Device class. But for the Device
 * class to access the persisted members, the Device class has a forward
 * reference to the StoredDevice class. Methods like Device.getHostname() simply
 * invoke StoredDevice.getHostname() using that forward reference.
 * <p>
 * Where it gets more complex is in how relationships are stored, or containers
 * managed. For example, a managed Device has a relationship to a ManagedSet and
 * vice versa. It would be possible to store that relationship in the clientAPI
 * with a Device having a reference to a ManagedSet and vice versa, but since
 * that relationship is persisted, it is a duplicate of the StoredDevice having
 * a reference to the StoredManagedSet. So instead of duplicating the references
 * in the clientAPI, relationships are kept only in the persistence layer. This
 * helps us keep a single copy of the relationships, so there aren't multiple
 * copies that would be extra work to maintain and that may get out of sync. So
 * to determine which ManagedSet a Device is in, the Device calls to the
 * StoredDevice to get the related StoredManagedSet. But then we need a backward
 * reference from the StoredManagedSet to the ManagedSet, even though the
 * ManagedSet has a forward reference to the StoredManagedSet because the
 * ManagedSet wraps the StoredManagedSet. That backward reference is the reason
 * why this PersistenceMapper class exists. The PersistenceMapper keeps all the
 * backward references from the Stored* objects to their clientAPI wrapper
 * classes. And the Stored* classes in the persistence layer don't have any
 * explicit knowledge of their wrapping counterparts in the clientAPI. So
 * whenever a persistence object and clientAPI object are created (i.e., Device
 * and StoredDevice), be sure to invoke the <code>add</code> method here so
 * that the backward reference from the persistence object to the clientAPI
 * object is captured here. Invoke the <code>remove</code> method here when
 * objects are deleted.
 * <p>
 * In the HashMap, the key is the dataAPI object and the value is the clientAPI
 * object
 * <p>
 * Should have a Map and corresponding methods here for every class that
 * implements Persistable.
 * <p>
 * 
 * @see Persistable
 * @version SCM ID: $Id: PersistenceMapper.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 * <p>
 */
//* Created on Sep 6, 2006
class PersistenceMapper {
    private volatile static PersistenceMapper singleton = null;
    
    private volatile Map deviceMap = null;
    private volatile Map domainMap = null;
    private volatile Map domainVersionMap = null;
    private volatile Map deploymentPolicyMap = null;
    private volatile Map deploymentPolicyVersionMap = null;
    private volatile Map firmwareMap = null;
    private volatile Map firmwareVersionMap = null;
    private volatile Map managedSetMap = null;
    private volatile Map managerMap = null;
    
    /* flag to suppress misleading trace messages - defect 13242 */
    private volatile boolean initialised = false;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = PersistenceMapper.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    private PersistenceMapper() {
        this.deviceMap = new HashMap(); 
        this.domainMap = new HashMap(); 
        this.domainVersionMap = new HashMap(); 
        this.deploymentPolicyMap = new HashMap(); 
        this.deploymentPolicyVersionMap = new HashMap(); 
        this.firmwareMap = new HashMap(); 
        this.firmwareVersionMap = new HashMap(); 
        this.managedSetMap = new HashMap(); 
        this.managerMap = new HashMap();
    }
    
    static PersistenceMapper getInstance() {
        if (singleton == null) {
            singleton = new PersistenceMapper();
        }
        return(singleton);
    }
    
    void destroy() {
        deviceMap.clear();
        domainMap.clear();
        domainVersionMap.clear();
        deploymentPolicyMap.clear();
        deploymentPolicyVersionMap.clear();
        firmwareMap.clear();
        firmwareVersionMap.clear();
        managedSetMap.clear();
        managerMap.clear();
    }

    private void genericAdd(Object key, Object value, Map map) {
        final String METHOD_NAME = "genericAdd"; //$NON-NLS-1$
        if (key != null) {
            if (value instanceof Persistable) {
                Persistable persistable = (Persistable) value;
                try {
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                                "adding object " + persistable.getClass().getName() + //$NON-NLS-1$
                                " with primaryKey " + persistable.getPrimaryKey() + //$NON-NLS-1$
                                " to map"); //$NON-NLS-1$
                } catch (DeletedException e) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                "adding deleted object " + persistable.getClass().getName() + //$NON-NLS-1$
                                " back into map"); //$NON-NLS-1$
                }
            } else {
                // it should be Persistable, but just in case...
                logger.log(Level.FINE, 
                           "adding non-persistable object " + value.getClass().getName() + //$NON-NLS-1$
                           " to map"); //$NON-NLS-1$
                        
            }
            map.put(key, value);
        } else {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.PersistenceMapper.nullKey")); //$NON-NLS-1$
        }
    }
    
    void add(StoredDevice storedDevice, Device device) {
        this.genericAdd(storedDevice, device, this.deviceMap);
    }
    
    void add(StoredDomain storedDomain, Domain domain) {
        this.genericAdd(storedDomain, domain, this.domainMap);
    }

    void add(StoredDomainVersion storedDomainVersion, DomainVersion domainVersion) {
        this.genericAdd(storedDomainVersion, domainVersion, this.domainVersionMap);
    }

    void add(StoredDeploymentPolicy storedDeploymentPolicy, DeploymentPolicy deploymentPolicy) {
        this.genericAdd(storedDeploymentPolicy, deploymentPolicy, this.deploymentPolicyMap);
    }

    void add(StoredDeploymentPolicyVersion storedDeploymentPolicyVersion, DeploymentPolicyVersion deploymentPolicyVersion) {
        this.genericAdd(storedDeploymentPolicyVersion, deploymentPolicyVersion, this.deploymentPolicyVersionMap);
    }

    void add(StoredFirmware storedFirmware, Firmware firmware) {
        this.genericAdd(storedFirmware, firmware, this.firmwareMap);
    }

    void add(StoredFirmwareVersion storedFirmwareVersion, FirmwareVersion firmwareVersion) {
        this.genericAdd(storedFirmwareVersion, firmwareVersion, this.firmwareVersionMap);
    }

    void add(StoredManagedSet storedManagedSet, ManagedSet managedSet) {
        this.genericAdd(storedManagedSet, managedSet, this.managedSetMap);
    }

    void add(Repository repository, Manager manager) {
        this.genericAdd(repository, manager, this.managerMap);
    }
  
    //////////////////////////////////////////////////////////////////////////////////
    
    private Object genericGetViaKey(Object key, Map map) throws DeletedException {
        final String METHOD_NAME = "genericGetViaKey"; //$NON-NLS-1$
        Object result = null;
        if (key != null) {
            result = map.get(key);
        }
        if (result == null && initialised) {
            String message = Messages.getString("wamt.clientAPI.PersistenceMapper.objDeleted",key);
            DeletedException e = new DeletedException(message,"wamt.clientAPI.PersistenceMapper.objDeleted",key);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        }
        return(result);
    }
    
    Device getVia(StoredDevice storedDevice) throws DeletedException {
        Object object = genericGetViaKey(storedDevice, this.deviceMap);
        return((Device) object);
    }
    
    Domain getVia(StoredDomain storedDomain) throws DeletedException {
        Object object = genericGetViaKey(storedDomain, this.domainMap);
        return((Domain) object);
    }
    
    DomainVersion getVia(StoredDomainVersion storedDomainVersion) throws DeletedException {
        Object object = genericGetViaKey(storedDomainVersion, this.domainVersionMap);
        return((DomainVersion) object);
    }
    
    DeploymentPolicy getVia(StoredDeploymentPolicy storedDeploymentPolicy) throws DeletedException {
        Object object = genericGetViaKey(storedDeploymentPolicy, this.deploymentPolicyMap);
        return((DeploymentPolicy) object);
    }
    
    DeploymentPolicyVersion getVia(StoredDeploymentPolicyVersion storedDeploymentPolicyVersion) throws DeletedException {
        Object object = genericGetViaKey(storedDeploymentPolicyVersion, this.deploymentPolicyVersionMap);
        return((DeploymentPolicyVersion) object);
    }
    
    Firmware getVia(StoredFirmware storedFirmware) throws DeletedException {
        Object object = genericGetViaKey(storedFirmware, this.firmwareMap);
        return((Firmware) object);
    }
    
    FirmwareVersion getVia(StoredFirmwareVersion storedFirmwareVersion) throws DeletedException {
        Object object = genericGetViaKey(storedFirmwareVersion, this.firmwareVersionMap);
        return((FirmwareVersion) object);
    }
    
    ManagedSet getVia(StoredManagedSet storedManagedSet) throws DeletedException {
        Object object = genericGetViaKey(storedManagedSet, this.managedSetMap);
        return((ManagedSet) object);
    }
    
    Manager getVia(Repository repository) throws DeletedException {
        Object object = genericGetViaKey(repository, this.managerMap);
        return((Manager) object);
    }
    
    //////////////////////////////////////////////////////////////////////////////////
    
    private void genericRemove(Object key, Map map) {
        final String METHOD_NAME = "genericRemove"; //$NON-NLS-1$
        Object object = map.get(key);
        if (object instanceof Persistable) {
            Persistable persistable = (Persistable) object;
            try {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "putting " + persistable.getClass().getName() +  //$NON-NLS-1$
                            " with primaryKey " + persistable.getPrimaryKey() + //$NON-NLS-1$
                            " into trash"); //$NON-NLS-1$
            } catch (DeletedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "duplicate attempt to put " + //$NON-NLS-1$
                            object.getClass().getName() + 
                            " with key " + key + " into trash"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "putting " + object.getClass().getName() + //$NON-NLS-1$
                        " with key " + key + " into trash"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        map.remove(key);
    }
    
    void remove(StoredDevice storedDevice) {
        genericRemove(storedDevice, this.deviceMap);
    }
    
    void remove(StoredDomain storedDomain) {
        genericRemove(storedDomain, this.domainMap);
    }

    void remove(StoredDomainVersion storedDomainVersion) {
        genericRemove(storedDomainVersion, this.domainVersionMap);
    }
    
    void remove(StoredDeploymentPolicy storedDeploymentPolicy) {
        genericRemove(storedDeploymentPolicy, this.deploymentPolicyMap);
    }

    void remove(StoredDeploymentPolicyVersion storedDeploymentPolicyVersion) {
        genericRemove(storedDeploymentPolicyVersion, this.deploymentPolicyVersionMap);
    }
    
    void remove(StoredFirmware storedFirmware) {
        genericRemove(storedFirmware, this.firmwareMap);
    }
    
    void remove(StoredFirmwareVersion storedFirmwareVersion) {
        genericRemove(storedFirmwareVersion, this.firmwareVersionMap);
    }
    
    void remove(StoredManagedSet storedManagedSet) {
        genericRemove(storedManagedSet, this.managedSetMap);
    }
    
    void remove(Repository repository) {
        genericRemove(repository, this.managerMap);
    }  
    
    void setInitialised()
    {
    	initialised = true;
    }
}
