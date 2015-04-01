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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A task to deploy the desired configuration for services.
 * <p>
 */
public class DeployServiceConfigurationTask extends BackgroundTask {
	ServiceDeployment svcDeployment = null;
	
	public static final String COPYRIGHT_2012_2013 = Constants.COPYRIGHT_2012_2013;
    
    protected static final String CLASS_NAME = DeployServiceConfigurationTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    private Domain domain = null;
    

    DeployServiceConfigurationTask(Domain domain, ServiceDeployment svcDeployment) {
    	  super();
    	  this.domain = domain;
    	  this.svcDeployment = svcDeployment;
    	  this.progressContainer.setTotalSteps(this.estimateSteps());
    }

	/**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = CLASS_NAME; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

	protected int estimateSteps() {
		return(1);
    }
    
    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // TODO: add lock device
        try {            
            try {
            	// deploy services
            	Object [] args = new Object[2];
                args[0] = this.domain.getName();
                args[1] = this.domain.getDevice();
                this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.DeployServiceConfigurationTask.deploySource", args);
                svcDeployment.deployServiceSourceConfigurationAction();
                // need to release lock before making update visible, so commit later
                this.progressContainer.setUncommittedComplete();

            } 
            catch (Exception e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
                this.progressContainer.setUncommittedError(e);
            }            
        } 
        catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setUncommittedError(e);
        }
        finally {
            // TODO: release locks
            this.progressContainer.commit();
            this.cleanup();
        }        
        logger.exiting(CLASS_NAME, METHOD_NAME);    	
    }
}
