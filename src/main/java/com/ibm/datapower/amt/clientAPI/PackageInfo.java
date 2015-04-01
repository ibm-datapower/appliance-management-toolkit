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

import java.lang.reflect.Field;

import com.ibm.datapower.amt.AbstractPackageInfo;
import com.ibm.datapower.amt.ComponentInfo;
import com.ibm.datapower.amt.Constants;

/**
 * Get the SCM information about all the classes in this package.  It is
 * typically invoked only by {@link com.ibm.datapower.amt.ComponentInfo}.
 * 
 * @see AbstractPackageInfo
 */
public class PackageInfo extends AbstractPackageInfo {
	private static PackageInfo instance = null;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Get an instance of this object. This method should be invoked only by
     * {@link ComponentInfo}. This is a public method only because it needs to
     * be invoked by <code>ComponentInfo</code>, which is in a different
     * package.
     * 
     * @return an instance of this object.
     */
    public static PackageInfo getInstance() {
    	if (instance == null) {
    		instance = new PackageInfo();
    	}
    	return(instance);
    }

	private PackageInfo() {
		super();
		// make sure this list is complete for this package
		this.add(AddFirmwareTask.class);
		this.add(AlreadyExistsException.class);
		this.add(BackgroundTask.class);
		this.add(Blob.class);
		this.add(ClientAPIException.class);
        this.add(Configuration.class);
//		this.add(CopyTask.class);
		this.add(DeletedException.class);
		this.add(DeployDomainConfigurationTask.class);
		this.add(DeployFirmwareVersionTask.class);
		this.add(DeploymentPolicy.class);
		this.add(DeploymentPolicyVersion.class);
		this.add(Device.class);
		this.add(DeviceTypeIncompatibilityException.class);
		this.add(Domain.class);
		this.add(DomainSynchronizationDaemon.class);
		this.add(DomainSynchronizationMode.class);
		this.add(DomainSynchronizationQueue.class);
		this.add(DomainSynchronizationTask.class);
		this.add(DomainVersion.class);
        this.add(ExportAllTask.class);
		this.add(FeaturesNotEqualException.class);
		this.add(Firmware.class);
		this.add(FirmwareVersion.class);
		this.add(FullException.class);
		this.add(GetDiffURLTask.class);
		this.add(GetDomainsOperationStatusTask.class);
		this.add(GetWebGUIURLTask.class);
		this.add(HeartbeatDaemon.class);
		this.add(HeartbeatQueue.class);
		this.add(HeartbeatTask.class);
		this.add(IncompatibilityException.class);
		this.add(InUseException.class);
		this.add(InvalidCredentialException.class);
		this.add(InvalidParameterException.class);
		this.add(Lock.class);
		this.add(LockBusyException.class);
		this.add(MacroProgressContainer.class);
		this.add(ManagedSet.class);
		this.add(ManagementStatus.class);
		this.add(Manager.class);
        this.add(ManagerStatus.class);
		this.add(MissingFeaturesInFirmwareException.class);
		this.add(ModelTypeIncompatibilityException.class);
		this.add(NewDeviceTask.class);
		this.add(NewDomainTask.class);
		this.add(NotEmptyException.class);
        this.add(NotExistException.class);
		this.add(PackageInfo.class);
		this.add(Persistable.class);
		this.add(PersistenceMapper.class);
		this.add(ProgressContainer.class);
		this.add(Queue.class);
        this.add(QueueCollection.class);
		this.add(QueueProcessor.class);
        this.add(ReorderableQueue.class);
        this.add(SetFirmwareVersionTask.class);
//        this.add(SetVersionsDirectoryTask.class);
        this.add(ShutdownException.class);
        this.add(Signaler.class);
		this.add(SubscribeTask.class);
		this.add(SubscriptionToAnotherManagerException.class);
		this.add(Task.class);
		this.add(UndeployableVersionException.class);
		this.add(UnlicensedFeaturesInFirmwareException.class);
        this.add(UnsubscribeAllTask.class);
		this.add(UnsubscribeTask.class);
		this.add(UnsupportedVersionException.class);
		this.add(URLSource.class);
		this.add(Version.class);
		this.add(Versionable.class);
		this.add(WorkArea.class);
	}
	
	protected Field getDeclaredField(String fieldName, Class cl) 
	throws SecurityException, NoSuchFieldException {
		return(cl.getDeclaredField(fieldName));
	}
	
	protected Object getValueFromField(Field field) 
	throws IllegalArgumentException, IllegalAccessException {
		return(field.get(null));
	}
	
}
