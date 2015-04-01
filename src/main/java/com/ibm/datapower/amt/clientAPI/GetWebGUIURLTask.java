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

import java.net.MalformedURLException;
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
 * A BackgroundTask to get the URL that can be opened in an external browser to
 * launch the regular WebGUI. The benefit of this task is that the user does not
 * need to manually enter the URL into their browser, and the user is
 * automatically logged in to the WebGUI using the stored userid and password
 * from the repository without having to login manually. Also, they may
 * optionally specify the context of a specific domain, which can be considered
 * the "landing domain" instead of assuming they want to land in the
 * <code>default</code> domain after the WebGUI is launched. This task will
 * generate the URL with a one-time-use token that can be consumed by an
 * external browser.
 * <p>
 * @version SCM ID: $Id: GetWebGUIURLTask.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
public class GetWebGUIURLTask extends BackgroundTask {
    private Device device = null;
    private String domainName = null;
    
    private static final String PROTOCOL = "https"; //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = GetWebGUIURLTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    GetWebGUIURLTask(Device device, String uri) {
        super();
        this.device = device;
        this.domainName = uri;
        this.progressContainer.setTotalSteps(this.estimateSteps());
    }
   
    /**
     * Get the target device of the where they will be connected to a WebGUI.
     * 
     * @return the target device of the where they will be connected to a
     *         WebGUI.
     */
    public Device getDevice() {
        return(this.device);
    }
    
    /**
     * Get the name of the domain that the user will automatically be switched
     * into the context of.
     * 
     * @return the name of the domain that the user will automatically be
     *         switched into the context of.
     */
    public String getDomainName() {
        return(this.domainName);
    }

    /**
     * Get a String representation of this BackgroundTask for the purpose of
     * debugging or tracing.
     * 
     * @return a String representation of this BackgroundTask for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        String result = "GetWebGUIURLTask["; //$NON-NLS-1$
        result += "device=" + this.device; //$NON-NLS-1$
        result += ", domainName=" + this.domainName; //$NON-NLS-1$
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
        // don't need to lock since this is a read-only operation

        URL url = null;
        String hostname = null;
        int guiPort = 0;
        try {
            hostname = this.device.getHostname();
            guiPort = this.device.getGUIPort();
            
            Manager manager = Manager.internalGetInstance();
            Commands commands = device.getCommands();
            DeviceContext deviceContext = device.getDeviceContext();
            String token = commands.getSAMLToken(deviceContext, this.domainName);
            
            if ((token != null) && (token.length() > 0)) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "got token successfully"); //$NON-NLS-1$
                url = new URL(PROTOCOL, hostname, guiPort,
                        "/?SAMLart=" + token); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "failed to get token"); //$NON-NLS-1$
                url = new URL("https", hostname, guiPort, "/"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            // there is no lock, so we can commit the result/error immediately
            this.progressContainer.setComplete(url);
            
        } catch (MalformedURLException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("wamt.clientAPI.GetWebGUIURLTask.unexpErr"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
            url = null;
        } catch (DMgrException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("wamt.clientAPI.GetWebGUIURLTask.exceptionWithDev", device.getDisplayName()), e); //$NON-NLS-1$
            // if we got a hostname and port, just skip the token
            if ((hostname != null) && (guiPort != 0)) {
                try {
                    url = new URL(PROTOCOL, hostname, guiPort, "/"); //$NON-NLS-1$
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "Using just hostname and port, skipping token."); //$NON-NLS-1$
                    this.progressContainer.setComplete(url);
                } catch (MalformedURLException e1) {
                    // geez, what's not to like about it?
                    logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                            Messages.getString("wamt.clientAPI.GetWebGUIURLTask.unexpErr"), e1); //$NON-NLS-1$
                    url = null;
                    this.progressContainer.setComplete(url);
                }
            } else {
                this.progressContainer.setError(e);
            }
        } catch (Exception e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            this.progressContainer.setError(e);
        } finally {
            this.cleanup();
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, url);
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
