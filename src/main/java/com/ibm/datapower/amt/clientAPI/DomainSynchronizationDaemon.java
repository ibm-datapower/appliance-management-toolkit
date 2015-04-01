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

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * <p>Implements a daemon which will synchronize the managed domains if a change is detected on a domain
 * configuration source or deployment policy source. The automatic synchronization will not occur unless the 
 * DomainSynchronizationMode has been
 * set to {@link DomainSynchronizationMode#AUTO}
 * 
 * <p>The daemon creates a new DomainSnchronization task for each managed domain and 
 * puts them on the DomainSynchronizationQueue.  Each DomainSynchronizationTask calls {@link Domain#synch(boolean)} to
 * check for changes that may result in domain configuration deployment if the DomainSynchronizationMode has been
 * set to {@link DomainSynchronizationMode#AUTO}
 * 
 * <p>The DomainSynchronization threads remove the tasks and process them.
 *  
 */
public class DomainSynchronizationDaemon {
    private Manager manager = null;
    private boolean taskIsScheduled = false;
    private DomainSynchronizationQueue domainSynchQueue = null;
    private Timer domainSynchTimer = null;
    private DomainSynchronizationTimerTask domainSynchtask = null;
    private boolean isDaemonFiringFirstTime = true;
    
    private static int threadPoolSize;
    private static long sleepIntervalMS;

    private static final boolean IS_DAEMON = true;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final String CLASS_NAME = DomainSynchronizationDaemon.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    static {
        Integer integer = Configuration.getAsInteger(Configuration.KEY_DOMAIN_SYNCHRONIZATION_THREAD_POOL_SIZE);
        threadPoolSize = integer.intValue();
        integer = Configuration.getAsInteger(Configuration.KEY_DOMAIN_SYNCHRONIZATION_INTERVAL);
        sleepIntervalMS = integer.intValue();
    }
    
    /**
     * Constructor for DomainSynchronizationDaemon class, which does nothing. Users should
     * call the startup method of this class after instantiating the class.
     */
    public DomainSynchronizationDaemon() {
        // empty constructor, refer to startup method
        this.taskIsScheduled = false;
    }
    
    /**
     * Initializes the timer and thread pool objects for the DomainSynchronizationDaemon 
     * mechanism.
     */
    public void startup(){
        
        final String METHOD_NAME = "startup"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        this.manager = Manager.internalGetInstance();
        
        this.domainSynchQueue = new DomainSynchronizationQueue(DomainSynchronizationDaemon.threadPoolSize);
        this.domainSynchTimer = new Timer(IS_DAEMON);
        setIntervalMS(DomainSynchronizationDaemon.sleepIntervalMS);
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                "DomainSynchronizationDaemon started with " + threadPoolSize +  //$NON-NLS-1$
                " threads and at a " + sleepIntervalMS + "ms interval"); //$NON-NLS-1$ //$NON-NLS-2$
        
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Cancels all the scheduled tasks, timers, and stops all threads. Note that
     * any tasks currently being executed will complete before quitting for good.
     */
    public void shutdown(){
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Shutting down DomainSynchronizationt daemon"); //$NON-NLS-1$
        if ((this.domainSynchtask != null) && (this.taskIsScheduled)) {
            this.domainSynchtask.cancel();
            this.taskIsScheduled = false;
        }
        if (this.domainSynchtask != null) {
            this.domainSynchtask = null;
        }
        if (this.domainSynchTimer != null) {
            this.domainSynchTimer.cancel();
            this.domainSynchTimer = null;
        }
        if (this.domainSynchQueue != null) { 
        	this.domainSynchQueue.stopAllThreads();
        	this.domainSynchQueue = null;
        }
    }
    
    /**
     * Changes the scheduled interval between Domain Synchronizations. This change
     * is not persisted after the application exits.
     * 
     * @param newInterval the desired interval between Domain Synchronizations in milliseconds
     */
    void setIntervalMS(long newInterval){
        final String METHOD_NAME = "changeDomainSynchronizationInterval"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "Changing interval between domain synchronizations to " + newInterval); //$NON-NLS-1$

        DomainSynchronizationDaemon.sleepIntervalMS = newInterval;
        /*
         * We can keep the same Timer and schedule other TimerTasks on it. But
         * we can't reschedule the same TimerTask object that was previously
         * cancelled. The TimerTask appears to keep some state data inside it.
         * So we will need to create a new TimerTask and schedule it on the
         * original Timer.
         */
        if ((this.domainSynchtask != null) && (this.taskIsScheduled)) {
            this.domainSynchtask.cancel();
            this.taskIsScheduled = false;
            this.domainSynchtask = null;
        }
        if (this.domainSynchtask == null) {
            this.domainSynchtask = new DomainSynchronizationTimerTask();
            this.taskIsScheduled = false;
        }
        if (!this.taskIsScheduled) {
        	Date now = new Date(System.currentTimeMillis()+1000);
            this.domainSynchTimer.scheduleAtFixedRate(this.domainSynchtask, now, newInterval);
            this.taskIsScheduled = true;
        }
    }
    
    long getIntervalMS() {
        return(DomainSynchronizationDaemon.sleepIntervalMS);
    }
    
    /**
     * Implements a TimerTask that executes the following logic:
     * 
     * get all managed sets
     * get all devices in each managed set
     * for each managed device get all domains
     *   execute(new DomainSynchronizationTask(domain));
     * 
     *
     * @version $Id: DomainSynchronizationDaemon.java,v 1.3 2010/09/02 16:24:52 wjong Exp $
     * 
     */
    class DomainSynchronizationTimerTask extends TimerTask{
        
        private static final String METHOD_NAME = "DomainSynchronizationTimerTask"; //$NON-NLS-1$

		public void run(){
            
            logger.entering(CLASS_NAME, METHOD_NAME);
            Thread.currentThread().setName("DomainSynchronizationTimerTask"); 
         
         
            ManagedSet[] set = manager.getManagedSets();
            int countOfAllAUTOSyncedDomains = 0;
            
            if (set == null){
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "There are no managed domains defined in the manager to synch"); 
            }
            else{
                for (int i = 0; i < set.length; i++){

                	ArrayList<Domain>[] deviceList = null;
					try {					
						deviceList = new ArrayList[set[i].getDeviceMembers().length];
	                    for(int j =0; j<set[i].getDeviceMembers().length; j++ ){ 
	                    	deviceList[j] = new ArrayList<Domain>();
	                    }
						
					} catch (DeletedException e1) {
						// TODO Auto-generated catch block
						logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e1);
					}
                    try{
                    	
                    	Device [] device = set[i].getDeviceMembers();
                    	int num = 0;
                    	for (Device dev: device){                    		
                    		Domain []domains = dev.getManagedDomains();
                    		//allManagedDevices.add(dev);
                  	        
                  	        for (Domain dom: domains){
                  	        	//allManagedDomains.put(dom, dev);

                  	        	if (dom.isSynchEnabled()){
                  	        	    deviceList[num].add(dom);
                      	        	countOfAllAUTOSyncedDomains ++;                  	        	    
                  	        	}
                  	        }               
                            num ++;  
                    	}
                    } catch (DeletedException e) {
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                                "A managed domain has already been deleted.", e); //$NON-NLS-1$
						//e.printStackTrace();
					}
                    boolean notDone = true;
                    int domProcessed =0;
                    while(notDone){
                    	for(ArrayList list: deviceList){   
                    		if(!list.isEmpty()){
                    			Domain domain = (Domain)list.remove(0); domProcessed++;
                    			// If this is the first time daemon is firing, compare the difference in domain configuration
                    			// between device and persisted domains
                    			// Else, just check for out of synch due to Domain and Policy Source time differences,call to set methods  
                    			if (isDaemonFiringFirstTime){
                     			   domainSynchQueue.execute(new DomainSynchronizationTask(domain, true));                    				
                    			}else{
                    			   domainSynchQueue.execute(new DomainSynchronizationTask(domain, false));
                    			}
                 			
                    		}
                    	}
                    	if (domProcessed >= countOfAllAUTOSyncedDomains){
                    		notDone=false;
                    	}
                    }
                    
                }                                           
                
            }
            isDaemonFiringFirstTime = false;
            logger.exiting(CLASS_NAME, METHOD_NAME);
        }
    }   
}
