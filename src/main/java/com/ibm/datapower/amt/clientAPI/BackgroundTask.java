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

import java.io.OutputStream;
import java.net.URI;
import java.util.Hashtable;

import com.ibm.datapower.amt.Constants;

/**
 * A background task to be executed by a thread outside of the user interface.
 * This other thread does batch processing of queued requests. Each of these
 * queued tasks can take an indeterminate amount of time to complete, since they
 * probably involve network communication to a device or other time-intensive
 * processing. In general, any method that does not return quickly should be
 * implemented as a {@link Task}, be it either a <code>BackgroundTask</code>
 * or a {@link com.ibm.datapower.amt.amp.Notification}. This way the user
 * interface can trust that it won't get blocked for long periods of when it 
 * calls a method.
 * <p>
 * The status of BackgroundTasks is communicated back to the caller via a
 * {@link ProgressContainer}. The caller can check for progress, completion, or
 * errors by looking in the ProgressContainer. The ProgressContainer is
 * instantiated at the same time that the BackgroundTask is instantiated, and
 * there should be a ProgressContainer object for each BackgroundTask object.
 * <p>
 * When a BackgroundTask object is instantiated, it should have all the
 * information it needs to start running. For example, a {@link NewDomainTask}
 * should be instantiated with the name of the domain to be managed, and the
 * device where the domain is located. However, the
 * environment in which it runs may be different than when it was enqueued, due
 * to the side effects of other Tasks that run after it was queued and before it
 * runs. For example, the devices in the ManagedSet may have changed. The
 * BackgroundTask should provide public gettr methods to retrieve this
 * construction-time metadata, such as the domain name and ManagedSet. This
 * metadata can be used to provide information to the user regarding which
 * BackgroundTasks are in the queue. Using the same example, you should expect a
 * NewDomainTask to have a <code>getDomain()</code> method and a
 * <code>getManagedSet()</code> method. Users should not create
 * BackgroundTasks objects themselves, it is done as a side effect of other
 * public methods, such as {@link Device#createManagedDomain(String)}which
 * should also return a ProgressContainer.
 * <p>
 * Each BackgroundTask class holds the implementation to execute the task. Each
 * BackgroundTask class should also estimate how many steps it will take to
 * complete, so that information can appear in the ProgressContainer even before
 * this BackgroundTask starts running. This abstract class also acts as a
 * pseudo-factory to instantiate the concrete background task classes.
 * <p>
 * Once a concrete instance of a BackgroundTasks exists, it should be mapped to
 * a WorkArea in which it will run. Each WorkArea has its own queue, but there
 * is only one thread which services all the WorkAreas. In general a WorkArea
 * maps exactly to a specific ManagedSet, or in the case of unmanaged Devices is
 * be mapped to the Manager. After the BackgroundTasks is created, it is placed
 * on a WorkArea queue via
 * {@link com.ibm.datapower.amt.clientAPI.Manager#enqueue(BackgroundTask, WorkArea)}.
 * The system may be configured to be more deterministic, meaning to not allow
 * users to submit new BackgroundTasks when there is a Task already in the queue
 * for a particular WorkArea - the maximum queue size is 1. This prevents users
 * from submitting changes when there is still outstanding Tasks to run, meaning
 * that the environment may change between when the BackgroundTask is submitted
 * and when it is run. This configuration is controlled via
 * {@link Manager#OPTION_TASK_QUEUE_SIZE}. However, the default is to have a
 * queue size greater than 1, which effectively disables this behavior, and
 * users are allowed to submit new BackgroundTasks while other Tasks are still
 * outstanding.
 * <p>
 * Because there is a circular reference between the BackgroundTask and
 * ProgressContainer that needs to be broken after the BackgroundTask is
 * completed (by nulling the reference from the BackgroundTask to the
 * ProgressContainer), it is recommended that you get a reference to the
 * ProgressContainer from the BackgroundTask via
 * <code>getProgressContainer()</code> before you enqueue it to the WorkArea,
 * just in case the task completes very quickly before you have a chance to
 * retrieve the ProgressContainer. You should always be able to get the
 * BackgroundTask from the ProgressContainer. The circular reference needs to be
 * broken to enable objects for garbage collection. This breaking is done in the
 * method <code>cleanup()</code>.
 * <p>
 * <p>
 * @version SCM ID: $Id: BackgroundTask.java,v 1.3 2010/08/23 21:20:27 burket Exp $
 */
public abstract class BackgroundTask implements Task {
    
    /*
     * When a concrete child of BackgroundTask is implemented it should:
     *  - implement the abstract methods.
     *  - in the constructor, invoke
     *        this.progressContainer.setTotalSteps(this.estimateSteps());
     *  - provide public gettr methods for the values used on the constructor,
     *        so that users can peek at this task's context.
     *  - invoke this.progressContainer.setComplete() or
     *        this.progressContainer.setError()
     *  - have a finally clause that calls this.cleanup()
     *  - catch any unchecked Exceptions and log them, because it is very bad if
     *        the QueueProcessor thread dies.
     */
    
    protected volatile ProgressContainer progressContainer = null;
    protected volatile String fromThread = null;
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$
    
    protected BackgroundTask() {
        /*
         * creation of a BackgroundTask also creates a ProgressContainer. In the
         * BackgroundTask constructor it should estimate the total number of steps and
         * invoke ProgressContainer.setTotalSteps()
         */
        this.progressContainer = new ProgressContainer(this);
        
        // for debug, keep track of which thread created the task
        this.fromThread = Thread.currentThread().getName();
        
        /*
         * in each derived class constructor, don't forget to put a call like:
         * this.progressContainer.setTotalSteps(this.estimateSteps()); because
         * the class-specific parameters are probably needed for the estimation,
         * and the class-specific parameters aren't available in super(). 
         */
    }
    
    static BackgroundTask createNewDeviceTask(String symbolicName, String hostname, 
            String userid, String password, int hlmPort) {
        BackgroundTask result = new NewDeviceTask(symbolicName, hostname, userid, password, hlmPort);
        return(result);
    }
    
    static BackgroundTask createGetWebGUIURLTask(Device device, String domainName) {
        BackgroundTask result = new GetWebGUIURLTask(device, domainName);
        return(result);
    }
    
    static BackgroundTask createGetDiffURLTask(Version object1, Version object2) {
        BackgroundTask result = new GetDiffURLTask(object1, object2);
        return(result);
    }
    
//    static BackgroundTask createCopyTask(Version object, Device destinationDevice) {
//        BackgroundTask result = new CopyTask(object, destinationDevice);
//        return(result);
//    }

    static BackgroundTask createNewDomainTask(String domainName, Device device) {
        BackgroundTask result = new NewDomainTask(domainName, device);
        return(result);
    }

    static BackgroundTask createUnsubscribeTask(Device device, boolean removeOnly) {
        BackgroundTask result = new UnsubscribeTask(device, removeOnly);
        return(result);
    }

    static BackgroundTask createUnsubscribeAllTask() {
        BackgroundTask result = new UnsubscribeAllTask();
        return(result);
    }
    
    static BackgroundTask createSubscribeTask(Device device) {
        BackgroundTask result = new SubscribeTask(device);
        return(result);
    }

    static BackgroundTask createGetDomainsOperationStatusTask(Device device) {
        BackgroundTask result = new GetDomainsOperationStatusTask(device);
        return(result);
    }

    static BackgroundTask createAddFirmwareTask(Blob image, String userComment) {
        BackgroundTask result = new AddFirmwareTask(image, userComment);
        return(result);
    }
    
//    static BackgroundTask createSetVersionsDirectory(File newDirectory) {
//        BackgroundTask result = new SetVersionsDirectoryTask(newDirectory);
//        return(result);
//    }
//    
    static BackgroundTask createExportAll(OutputStream exportOS) {
        BackgroundTask result = new ExportAllTask(exportOS);
        return(result);
    }

    static BackgroundTask createSetFirmwareVersionTask(Device targetDevice, FirmwareVersion desiredFirmwareVersion) {
        BackgroundTask result = new SetFirmwareVersionTask(targetDevice, desiredFirmwareVersion);
        return(result);
    }    

    static BackgroundTask createDeployFirmwareVersionTask(Device targetDevice) {
        BackgroundTask result = new DeployFirmwareVersionTask(targetDevice);
        return(result);
    }
    
    static BackgroundTask createBackupDeviceTask(Device targetDevice, String certObjectName,  URLSource certificateLocation, URI backupFileLocation,
    		boolean includeISCSI, boolean includeRaid) {
        BackgroundTask result = new BackupDeviceTask(targetDevice, certObjectName, certificateLocation, backupFileLocation,includeISCSI, includeRaid);
        return(result);
    }    
    
    static BackgroundTask createRestoreDeviceTask(Device targetDevice, String credObjectName, boolean validate, URI restoreFileLocation, Hashtable backupFilesTable) {
        BackgroundTask result = new RestoreDeviceTask(targetDevice, credObjectName, validate, restoreFileLocation, backupFilesTable);
        return(result);
    }   
    
    static BackgroundTask createDomainDeployConfigurationTask(Domain domain) {
        BackgroundTask result = new DeployDomainConfigurationTask(domain);
        return(result);
    }
    
    static BackgroundTask createServiceDeployConfigurationTask(Domain domain, ServiceDeployment svcDeployment) {
        BackgroundTask result = new DeployServiceConfigurationTask(domain, svcDeployment);
        return(result);
    }
    
    static BackgroundTask createDomainSynchronizationTask(Domain domain, Boolean compareMode) {
        BackgroundTask result = new DomainSynchronizationTask(domain, compareMode);
        return(result);
    }
    
    static BackgroundTask createRebootDeviceTask(Device targetDevice) {
        BackgroundTask result = new RebootDeviceTask(targetDevice);
        return(result);
    }
    
    ProgressContainer getProgressContainer() {
        return(this.progressContainer);
    }

//    /**
//     * Determine if this task will affect the firmware on device(s). This method
//     * is used by {@link Device#getManagementStatusOfFirmware()} to determine if
//     * there any tasks in the queue for the {@link QueueProcessor}.
//     * 
//     * @return true if this task could affect the firmware on the device, false
//     *         otherwise.
//     */
//    abstract protected boolean affectsFirmware();
//    
//    /**
//     * Determine if this task will affect domains on the device(s). This method
//     * is used by {@link Device#getManagementStatusOfDomain(Domain) to determine
//     * if there are any tasks in the queue for the {@link QueueProcessor}.
//     * 
//     * @return true if this task could affect domains on the device, false
//     *         otherwise.
//     */
//    abstract protected boolean affectsDomains();
//    
//    /**
//     * If {@link #affectsDomains()} returns true and only one Domain will be
//     * affected by this task, then get the name of that affected domain.
//     * 
//     * @return the name of the affected domain if the above condition is true,
//     *         otherwise null.
//     */
//    abstract protected String getSingleAffectedDomain();
//    
//    /**
//     * If only one Device will be affected by this task, then get a reference to
//     * the affected device.
//     * 
//     * @return a reference to the affected device if the above condition is
//     *         true, otherwise null.
//     */
//    abstract protected Device getSingleAffectedDevice();

    /**
     * Estimate how many long-running steps are in this task, so that the
     * ProgressContainer can be set to have this number of total steps before the
     * tasks starts running.
     * 
     * @return number of long-running steps in this task
     */
    abstract protected int estimateSteps();
    
    /**
     * Execute the task. This method should not throw any exceptions. Exceptions
     * encountered during the execution should be put in the ProgressContainer
     * via {@link ProgressContainer#setError(Throwable)}. Don't forget to mark
     * the task as done when you exit this method, for example
     * {@link ProgressContainer#setComplete()}, or else the observer thread
     * will wait forever.
     */
    abstract protected void execute();
    
    /**
     * Do any cleanup after this task has been instantiated. This method should
     * get called after the task has executed. It should also get called if the
     * task is aborted or isn't run. Basically, you should call this method when
     * the task is done, either at a normal or abnormal end. The
     * {@link #execute()} method should call this method internally}. If this
     * task's execute() method isn't invoked, then you need to invoke this
     * method explicitly.
     */
    protected void cleanup() {
        /*
         * since the ProgressContainer has a reference to the Task, and the Task
         * has a reference to the ProgressContainer, when we are done with
         * everything there will be a circular reference between these two
         * objects that will prevent garbage collection. Let's break the
         * circular reference. After the Task completes either successfully or
         * with an error, the task no longer needs a reference to the
         * ProgressContainer. But the client may be looking at the
         * ProgressContainer to determine what Task maps to it. So within the
         * Task set the ProgressContainer reference to null.
         */
    	  
    	// For fixing defect 11033: [3667 ] NullPointerException in ManagedSet.removeDevice
    	// "Circular strong references don't necessarily cause memory leaks." from sun's doc
        //this.progressContainer = null;
    }
    
}
