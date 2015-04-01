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
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.Commands;
import com.ibm.datapower.amt.amp.DeviceContext;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A BackgoundTask to get the URL of the content differences of two
 * DomainVersions. It is expected that the URL will be
 * opened in an external browser to display the version differences. This URL
 * should invoke the device's WebGUI.
 * <p>
 * <p>
 * @version SCM ID: $Id: GetDiffURLTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
public class GetDiffURLTask extends BackgroundTask {
    private Version object1 = null;
    private Version object2 = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = GetDiffURLTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    GetDiffURLTask(Version object1, Version object2) {
        super();
        this.object1 = object1;
        this.object2 = object2;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }

    /**
     * Get a reference to the first object that will be used in the comparison.
     * 
     * @return a reference to the first object that will be used in the
     *         comparison.
     */
    public Version getObject1() {
        return(this.object1);
    }
    
    /**
     * Get a reference to the second object that will be used in the comparison.
     * 
     * @return a reference to the second object that will be used in the
     *         comparison.
     */
    public Version getObject2() {
        return(this.object2);
    }

    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "GetDiffURLTask["; //$NON-NLS-1$
        result += "object1=" + this.object1; //$NON-NLS-1$
        result += ", object2=" + this.object2; //$NON-NLS-1$
        result += ", fromThread=" + this.fromThread; //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    protected int estimateSteps() {
        return(1);
    }

    protected void execute() {
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME,
                new Object[] {this.object1, this.object2});
        URL url = null;
        try {
            // don't need to lock since this is a read-only operation
            Manager manager = Manager.internalGetInstance();
            Commands commands = null;
            
            // check to see if this is a Domain diff
            if ((object1 instanceof DomainVersion) && (object2 instanceof DomainVersion)) {
                DomainVersion version1 = (DomainVersion) object1;
                DomainVersion version2 = (DomainVersion) object2;
                Domain domain1 = version1.getDomain();
                Domain domain2 = version2.getDomain();
                // can only compare versions within the same Domain
                if (domain1 != domain2) {
                	Object[] args = new Object[] {domain1.getAbsoluteDisplayName(), domain2.getAbsoluteDisplayName()};
                    String message = Messages.getString("wamt.clientAPI.GetDiffURLTask.cannotCompareVer", args); //$NON-NLS-1$
                    InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.GetDiffURLTask.cannotCompareVer", args);
                    throw(e);
                }
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "requesting URL for DomainVersion diff"); //$NON-NLS-1$
                String domainName = domain1.getName();
                Device device = domain1.getDevice();
                DeviceContext deviceContext = device.getDeviceContext();
                commands = device.getCommands();
                url = commands.getDomainDifferences(domainName, 
                        version1.getBlob().getByteArray(),
                        version2.getBlob().getByteArray(),
                        deviceContext);
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "received URL for diff: " + url); //$NON-NLS-1$
                
            } else {
            	Object[] args = new Object[] {this.object1.getClass().getName(), this.object2.getClass().getName()};
                String message = Messages.getString("wamt.clientAPI.GetDiffURLTask.notVersion", args); //$NON-NLS-1$
                InvalidParameterException e = new InvalidParameterException(message,"wamt.clientAPI.GetDiffURLTask.notVersion", args);
                throw(e);
            }
            
            // there is no lock, so we can commit immediately
            this.progressContainer.setComplete(url);

        } catch (DMgrException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (IOException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("wamt.clientAPI.GetDiffURLTask.probWithFile"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
        } finally {
            this.cleanup();
        }
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

//    protected boolean affectsFirmware() {
//        return false;
//    }
//
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
