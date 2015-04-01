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

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Implements a heartbeat mechanism to monitor the status of devices and 
 * report severe inconsistencies in device definitions to the Manager via
 * creating appropriate BackgroundTasks. 
 * 
 * 
 */
//* @version $Id: HeartbeatDaemon.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
public class HeartbeatDaemon {
    private Manager manager = null;
    private boolean taskIsScheduled = false;
    private HeartbeatQueue heartbeatQueue = null;
    private Timer heartbeatTimer = null;
    private HeartbeatTimerTask heartbeatTimerTask = null;
    
    private static int threadPoolSize;
    private static long sleepIntervalMS;

    private static final boolean IS_DAEMON = true;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final String CLASS_NAME = HeartbeatDaemon.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    static {
        Integer integer = Configuration.getAsInteger(Configuration.KEY_HEARTBEAT_THREAD_POOL_SIZE);
        threadPoolSize = integer.intValue();
        integer = Configuration.getAsInteger(Configuration.KEY_HEARTBEAT_INTERVAL);
        sleepIntervalMS = integer.intValue();
    }
    
    /**
     * Constructor for HeartbeatDaemon class, which does nothing. Users should
     * call the startup method of this class after instantiating the class.
     */
    public HeartbeatDaemon() {
        // empty constructor, refer to startup method
        this.taskIsScheduled = false;
    }
    
    /**
     * Initializes the timer and thread pool objects for the heartbeat 
     * mechanism.
     */
    public void startup(){
        
        final String METHOD_NAME = "startup"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        this.manager = Manager.internalGetInstance();
        
        this.heartbeatQueue = new HeartbeatQueue(HeartbeatDaemon.threadPoolSize);
        this.heartbeatTimer = new Timer(IS_DAEMON);
        setIntervalMS(HeartbeatDaemon.sleepIntervalMS);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                "Heartbeat Daemon started with " + threadPoolSize +  //$NON-NLS-1$
                " threads and at a " + sleepIntervalMS + "ms interval"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Cancels all the scheduled tasks, timers, and stops all threads. Note that
     * any tasks currently being executed will complete before quitting for good.
     */
    public void shutdown(){
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Shutting down heartbeat daemon"); //$NON-NLS-1$
        if ((this.heartbeatTimerTask != null) && (this.taskIsScheduled)) {
            this.heartbeatTimerTask.cancel();
            this.taskIsScheduled = false;
        }
        if (this.heartbeatTimerTask != null) {
            this.heartbeatTimerTask = null;
        }
        if (this.heartbeatTimer != null) {
            this.heartbeatTimer.cancel();
            this.heartbeatTimer = null;
        }
        if (this.heartbeatQueue != null) { 
        	this.heartbeatQueue.stopAllThreads();
        	this.heartbeatQueue = null;
        }
    }
    
    /**
     * Changes the scheduled interval between heartbeats. This change
     * is not persisted after the application exits.
     * 
     * @param newInterval the desired interval between heartbeats in milliseconds
     */
    void setIntervalMS(long newInterval){
        final String METHOD_NAME = "setIntervalMS"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "Changing interval between heartbeats to " + newInterval); //$NON-NLS-1$

        HeartbeatDaemon.sleepIntervalMS = newInterval;
        /*
         * We can keep the same Timer and schedule other TimerTasks on it. But
         * we can't reschedule the same TimerTask object that was previously
         * cancelled. The TimerTask appears to keep some state data inside it.
         * So we will need to create a new TimerTask and schedule it on the
         * original Timer.
         */
        if ((this.heartbeatTimerTask != null) && (this.taskIsScheduled)) {
            this.heartbeatTimerTask.cancel();
            this.taskIsScheduled = false;
            this.heartbeatTimerTask = null;
        }
        if (this.heartbeatTimerTask == null) {
            this.heartbeatTimerTask = new HeartbeatTimerTask();
            this.taskIsScheduled = false;
        }
        if (!this.taskIsScheduled) {
            this.heartbeatTimer.scheduleAtFixedRate(this.heartbeatTimerTask, 0, newInterval);
            this.taskIsScheduled = true;
        }
    }
    
    long getIntervalMS() {
        return(HeartbeatDaemon.sleepIntervalMS);
    }
    
    /**
     * Implements a TimerTask that executes the following logic:
     * 
     * get all devices in manager
     * for each device in manager
     *   execute HeartbeatTask(device)
     * 
     *
     * @version $Id: HeartbeatDaemon.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
     * 
     */
    class HeartbeatTimerTask extends TimerTask{
        
        private static final String METHOD_NAME = "HeartbeatTimerTask.run"; //$NON-NLS-1$
        public void run(){
            
            logger.entering(CLASS_NAME, METHOD_NAME);
            Thread.currentThread().setName("HeartbeatTimer"); //$NON-NLS-1$
            
            // manager references the member in the outer class
            Device[] devices = manager.getAllDevices();
            
            if (devices == null){
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "There are no devices defined in the manager to heartbeat"); //$NON-NLS-1$
            }
            else{
                for (int i = 0; i < devices.length; i++){
                	Device device = devices[i];
                    String subscriptionID = manager.getSubscriptionId(device.isPrimary());
                    
                    try{
                        heartbeatQueue.execute(new HeartbeatTask(device, subscriptionID));
                    }
                    catch (AMPException e){
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                "There was an error retrieving the Commands library when the heartbeat task is created for device " + device.toString()); //$NON-NLS-1$
                    }
                }
            }
            
            logger.exiting(CLASS_NAME, METHOD_NAME);
        }
    }   
}
