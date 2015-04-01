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

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Implements a queue for pending Domain Synchronizations that need to be processed. This
 * class contains a pool of threads that are used to perform the Domain Synchronization.  
 * 
 */
public class DomainSynchronizationQueue {
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
        
    private static final String CLASS_NAME = DomainSynchronizationQueue.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    private final int numThreads;
    private final PoolWorker[] threads;
    private final LinkedList queue;
    private volatile static DomainSynchronizationQueue singleton = null;
    private boolean shutdownStarted = false; 
    
    // private function added to comply with the criteria of Findbugs
    private static void setSingleton(DomainSynchronizationQueue domainSynchronizationQueue) {
    	DomainSynchronizationQueue.singleton = domainSynchronizationQueue;
    }
    /**
     * Constructor for DomainSynchronizationQueue. This method creates and starts the actual threads which poll
     * the queue for when a DomainSynchronizationTask arrives.  
     * 
     * @param numThreads The number of threads to create in the thread pool.
     * 
     **/
    public DomainSynchronizationQueue(int numThreads) {
        
        final String METHOD_NAME = "DomainSynchronizationQueue"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Starting " + numThreads + " DomainSynchronization threads"); //$NON-NLS-1$ //$NON-NLS-2$
        this.numThreads = numThreads;
        queue = new LinkedList();
        threads = new PoolWorker[numThreads];

        this.startThreads(numThreads);
        
        this.setSingleton(this); // DomainSynchronizationQueue.singleton = this;
    }
    
    private void startThreads(int numThreads) {
    	for (int i=0; i < numThreads; i++) {
            threads[i] = new PoolWorker(i);
            threads[i].setDaemon(true);
            threads[i].start();
        }
    }
    
    synchronized public static DomainSynchronizationQueue getDomainSynchronizationQueueInstance(){
    	if (singleton == null){
    		return null;
    	}
    	return singleton;
    }

    
    /**
     * Stops all threads from processing any further domain synchronization requests.
     * Any synchronization request that is currently being processed by a thread
     * will complete before that thread ceases to work.
     *
     */
    public void stopAllThreads(){
        final String METHOD_NAME = "stopAllThreads"; //$NON-NLS-1$
        
        //logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
        //            "stopping all threads for DomainSynchronization"); //$NON-NLS-1$
        
        shutdownStarted = true;
        
        queue.clear();
        
/*        Vector<DomainSynchronizationTask> tasks = Manager.internalGetInstance().getDomainSyncTasksStarted();                     
        boolean done = false;
        while(!done){
        	done = true;
        	for(int i =0; i < tasks.size(); i++) {
        		DomainSynchronizationTask key = (DomainSynchronizationTask) tasks.get(i);
        		boolean flag = key.getStatus();
        		if (!flag){
        			done = false;
        		}
        	}
        } 
        
        tasks.removeAllElements(); tasks = null;*/
        
        for (int i = 0; i < numThreads; i++) {
            threads[i].stopThread();
            threads[i].interrupt();
        }
        // fix for 59347
        for (int i = 0; i < numThreads; i++) {
            while (threads[i].isAlive()) {
                logger.log(Level.FINEST, 
                           "waiting for thread to finish: " + threads[i].getName()); //$NON-NLS-1$ 
                //System.out.println("Waiting for thread in Queue.stopAllThreads: " +threads[i].getName() );                
                try {
                    Thread.sleep(100);

                } catch (InterruptedException e) {
                    logger.log(Level.FINEST, "sleep interrupted"); //$NON-NLS-1$
                    Thread.currentThread().interrupt();
                    //System.out.println("Interrupted Exception in Queue.stopAllThreads: " +threads[i].getName() );
                }
            }
            logger.log(Level.FINEST, "thread has finished " + threads[i].getName()); //$NON-NLS-1$
        }
    }
    
    /**
     * Adds the DomainSynchronizationTask to the internal queue so that the next available
     * thread will take it off the queue for processing. 
     * 
     * @param r the DomainSynchronization Task to be processed
     */
    public void execute(BackgroundTask r) {
        
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        
        // If Manager is shutting down stop adding task to the DS queue and return 
        Manager manager = Manager.internalGetInstance();
        if (manager.getShutdownStatus()){
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,"Shutdown Started. Clear task:" + r.toString());            
            ((DomainSynchronizationTask)r).setStatus(true);
        	return;
        }  	
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "adding DomainSynchronizationTask to DomainSynchronizationQueue"); //$NON-NLS-1$        
        synchronized(queue) {
            queue.addLast(r);
            queue.notify();
        }
    }
    
    /**
     * Provides the necessary mechanisms for a thread to block waiting on a 
     * task to be placed in the queue, so that it can remove it and run it.
     * 
     */
    private final class PoolWorker extends Thread {
        private int number = 0;
        private boolean run = false;
        
        public PoolWorker(int number) {
            this.number = number;
            this.run = true;
        }
        
        /**
         * Sets boolean flag that thread checks upon each iteration to see if
         * it should stop. 
         */
        public void stopThread(){
            run = false;
        }
        /**
         * Thread executing this method will remove a DomainSynchronizationTAsk off of the 
         * internal queue, and attempt to process it.
         */
        public void run() {
            BackgroundTask domainSynchronizationTask;
            final String METHOD_NAME = "PoolWorker.run"; //$NON-NLS-1$
            
            Thread.currentThread().setName("DomainSynchronizationQueue PoolWorker " + this.number); //$NON-NLS-1$

            //give flag to stop the thread if needed.
            while (run) {
                
                synchronized(queue) {         	
                    // while the queue is empty, wait for a notification
                    // that something is placed on the queue
                    while (queue.isEmpty() && run && !Thread.currentThread().isInterrupted()) {
                        try {
                            queue.wait();
                        }
                        catch (InterruptedException ignored){                        	
                        	//System.out.println("**Thread interrupted while waiting. Queue size: " + queue.size());                     	
                            Thread.currentThread().interrupt();    
                        }
                    }
                    if (!run) {
                        return;
                    }
                    // something's on the queue, lets get it and process it                    
                    domainSynchronizationTask = (BackgroundTask) queue.removeFirst();
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "removing DomainSynchronizationTask from DomainSynchronization Queue"); //$NON-NLS-1$
                }
                
                // run the Domain Synchronization task
//                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "running DomainSynchronizationTask " + domainSynchronizationTask.getSingleAffectedDomain() ); //$NON-NLS-1$
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "running DomainSynchronizationTask " ); //$NON-NLS-1$
                domainSynchronizationTask.execute();              
            }
        }
    }
}

