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
 * Implements a queue for pending heartbeats that need to be processed. This
 * class contains a pool of threads that are used to perform the heartbeats.
 * 
 * @version $Id: HeartbeatQueue.java,v 1.3 2010/09/02 16:24:51 wjong Exp $
 * 
 */
public class HeartbeatQueue {
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
        
    private static final String CLASS_NAME = HeartbeatQueue.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    private final int numThreads;
    private final PoolWorker[] threads;
    private final LinkedList queue;
    
    /**
     * Constructor for HeartbeatQueue. This method creates and starts the actual threads which poll
     * the queue for when a HeartbeatTask arrives.  
     * 
     * @param numThreads The number of threads to create in the thread pool.
     **/
    
    public HeartbeatQueue(int numThreads) {
        
        final String METHOD_NAME = "HeartbeatQueue"; //$NON-NLS-1$
        
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Starting " + numThreads + " heartbeat threads"); //$NON-NLS-1$ //$NON-NLS-2$
        this.numThreads = numThreads;
        queue = new LinkedList();
        threads = new PoolWorker[numThreads];

        this.startThreads(numThreads);
    }
    
    private void startThreads(int numThreads) {
    	for (int i=0; i < numThreads; i++) {
            threads[i] = new PoolWorker(i);
            threads[i].setDaemon(true);
            threads[i].start();
        }
    }

    /**
     * Stops all threads from processing any furter heartbeat requests.
     * Any heartbeat request that is currently being processed by a thread
     * will complete before that thread ceases to work.
     *
     */
    public void stopAllThreads(){
        final String METHOD_NAME = "stopAllThreads"; //$NON-NLS-1$
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "stopping all threads for Heartbeats"); //$NON-NLS-1$
        
        for (int i = 0; i < numThreads; i++) {
            threads[i].stopThread();
            threads[i].interrupt();
        }
        // fix for 59347
        for (int i = 0; i < numThreads; i++) {
            while (threads[i].isAlive()) {
                logger.log(Level.FINEST, 
                           "waiting for thread to finish: " + threads[i].getName()); //$NON-NLS-1$
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.log(Level.FINEST, "sleep interrupted"); //$NON-NLS-1$
                    Thread.currentThread().interrupt();
                }
            }
            logger.log(Level.FINEST, "thread has finished " + threads[i].getName()); //$NON-NLS-1$
        }
    }
    
    /**
     * Adds the HeartbeatTask to the internal queue so that the next available
     * thread will take it off the queue for processing. 
     * 
     * @param r the HeartbeatTask to be processed
     */
    public void execute(Runnable r) {
        
        final String METHOD_NAME = "execute"; //$NON-NLS-1$
        
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "adding HeartbeatTask to HeartbeatQueue"); //$NON-NLS-1$
        
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
    private class PoolWorker extends Thread {
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
         * Thread executing this method will remove a HeartbeatTask off of the 
         * internal queue, and attempt to process it.
         */
        public void run() {
            Runnable heartbeatTask;
            final String METHOD_NAME = "PoolWorker.run"; //$NON-NLS-1$
            
            Thread.currentThread().setName("HeartbeatQueue PoolWorker " + this.number); //$NON-NLS-1$
            
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
                            Thread.currentThread().interrupt();    
                        }
                    }
                    if (!run) {
                        return;
                    }
                    // something's on the queue, lets get it and process it
                    heartbeatTask = (Runnable) queue.removeFirst();
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "removing HeartbeatTask from Heartbeat Queue"); //$NON-NLS-1$
                }

                // run the heartbeat task
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "running HeartbeatTask"); //$NON-NLS-1$
                heartbeatTask.run();
            }
        }
    }
}

