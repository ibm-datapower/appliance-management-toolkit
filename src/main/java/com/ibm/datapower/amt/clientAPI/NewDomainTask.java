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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A BackgroundTask to begin management of a domain. 
 * <p>
 * @version SCM ID: $Id: NewDomainTask.java,v 1.4 2010/09/02 16:24:51 wjong Exp $
 */
public class NewDomainTask extends BackgroundTask {
    private String domainName = null;
    private Device device = null;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = NewDomainTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    NewDomainTask(String domainName, Device device) { 
        super();
        this.domainName = domainName;
        this.device = device;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
    
    /**
     * Get the name of the domain to manage.
     * 
     * @return the name of the domain to manage.
     */
    public String getDomainName() {
        return(this.domainName);
    }
    
    /**
     * Get the Device where the domain to be
     * managed.
     * 
     * @return the Device that has the domain to
     *         be managed.
     */
    public Device getDevice() {
        return(this.device);
    }
    
    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "NewDomainTask["; //$NON-NLS-1$
        result += "domainName=" + this.domainName; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }
    
    protected int estimateSteps() {
        int steps = 0;

        // one AMP command to the device to retreive the initial version,
        steps++;
        // then two steps to deploy to the device
        steps++;
        steps++;
        return(steps);
    }
    
    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, 
                new Object[] {this.domainName, this.device});
        
        Domain domain = null;
        
        try {
        	device.lockWait();
            DomainVersion domainVersion = 
//                DomainVersion.createInstanceAndSync(this.domainName, 
//                        null, this.device, this.progressContainer);
                DomainVersion.createInstanceAndSync(this.domainName, 
                        this.device, this.progressContainer);
            domain = domainVersion.getDomain();

            Manager manager = Manager.internalGetInstance();
            manager.save(Manager.SAVE_UNFORCED);

            // need to release lock before making update visible, so commit later
            this.progressContainer.setUncommittedComplete(domain);

        } catch (DMgrException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
            this.progressContainer.setUncommittedError(e);
        } catch (IOException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.NewDomainTask.ioPorb"), e); //$NON-NLS-1$
            this.progressContainer.setUncommittedError(e);
        } catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setUncommittedError(e);
        } finally {
            device.unlock();
            this.progressContainer.commit();
            this.cleanup();
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

//    protected boolean affectsFirmware() {
//        return false;
//    }
//
//    protected boolean affectsDomains() {
//        return true;
//    }
//
//    protected String getSingleAffectedDomain() {
//        return this.domainName;
//    }
//
//    protected Device getSingleAffectedDevice() {
//        return null;
//    }
    
}
