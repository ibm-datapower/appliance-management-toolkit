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
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to export the contents of the repository and all it's versions. This
 * must be done as a Background task because it may take a long time and will
 * require several wide reaching locks.
 * 
 * <p>
 * @version SCM ID: $Id: ExportAllTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
//* Created on Jun 25, 2007
//* 

public class ExportAllTask extends BackgroundTask {
    private OutputStream exportOutputStream = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = ExportAllTask.class.getName();

    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    ExportAllTask(OutputStream exportOS) {
        super();
        this.exportOutputStream = exportOS;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }

    /**
     * Get the new directory for storing versions that is associated with this
     * BackgroundTask. The repository will be told to start using this directory
     * to store all the version in. The repository may choose to move all the
     * existing versions from their current directory to the new directory.
     * 
     * @return the new directory that will be used to store versions
     */
    public OutputStream getExportOutputStream() {
        return (this.exportOutputStream);
    }

    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "ExportAllTask["; //$NON-NLS-1$
        result += "exportOutputStream=" + this.exportOutputStream; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return (result);
    }

    protected int estimateSteps() {
        int totalSteps = 0;
        totalSteps += 1; // call repository.exportAll
        return (totalSteps);
    }

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, this.exportOutputStream);

        File result = null;
        Manager manager = Manager.internalGetInstance();
        try {
            // lock everything (ManagedSets, Devices, Firmware/FirmwareVersions,
            // unmanaged Devices

            ManagedSet[] managedSets = manager.getManagedSets();
            for (int i = 0; i < managedSets.length; i++) {
                managedSets[i].lockWait();
            }
            
            Device[] devices = manager.getAllDevices();
            for (int i = 0; i < devices.length; i++) {
                devices[i].lockWait();
            }
            
            Firmware.lockWait();
            
            manager.unmanagedDevicesLockWait();
            try {
                this.progressContainer.incrementCurrentStep(1,"wamt.clientAPI.ExportAllTask.exporting_txt"); //$NON-NLS-1$
                manager.getRepository().exportAll(exportOutputStream);

            } finally {
                manager.unmanagedDevicesUnlock();
                Firmware.unlock();
                for (int i = 0; i < devices.length; i++) {
                	devices[i].unlock();
                }
                for (int i = 0; i < managedSets.length; i++) {
                    managedSets[i].unlock();
                }
            }

            // unlock is before setComplete or setError, so can commit progress immediately
            this.progressContainer.setComplete(result);

        } catch (DMgrException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "An exception occurred.", e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (Exception e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "An unchecked error occurred.", e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } finally {
            this.cleanup();
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, result);
    }

//    /**
//     * This task does NOT impact the management status of a device's firmware.
//     */
//    protected boolean affectsFirmware() {
//        return false;
//    }
//
//    /**
//     * This task does NOT impact the management status of a device's domains.
//     */
//    protected boolean affectsDomains() {
//        return false;
//    }
//
//    protected String getSingleAffectedDomain() {
//        return null;
//    }
//
//    protected Device getSingleAffectedDevice() {
//        return null;
//    }

}
