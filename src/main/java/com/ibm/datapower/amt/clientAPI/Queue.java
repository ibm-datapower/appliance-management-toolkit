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

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A FIFO container for objects, it is used both internally and externally. It
 * operates in FIFO (first in, first out) fashion.
 * <p>
 * The BackgroundTask objects are placed on the queue as a side effect of
 * calling certain clientAPI public methods (generally those methods
 * return a ProgressContainer object). The Notification objects are placed
 * on the queue by the NotificationCatcher and by the HeartbeatDaemon. The
 * objects are removed from the Queue by the QueueProcessor. Generally there is
 * one Queue (for BackgroundTasks) and one ReorderableQueue (for Notifications)
 * per WorkArea.
 * <p>
 * Internal comment: see WorkArea
 * <p>
 * 
 * <p>
 * @version SCM ID: $Id: Queue.java,v 1.4 2010/09/02 16:24:51 wjong Exp $
 */
//* Created on Sep 19, 2006
public class Queue {
    // these members don't need to be marked volatile because synchronized methods are used to access them
    private Vector vector = null;
    private Integer maxSize = null;
    private int lifetimeCount = 0;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = Queue.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Create a new Queue that has no size limit (the number of objects that may
     * be on this queue at any one time).
     *  
     */
    public Queue() {
        this(null);
    }

    /**
     * Create a new Queue that has a size limit (the number of objects that may
     * be on this queue at any one time).
     * 
     * @param maxSize the maximum number of allowed objects that may be on the
     *        Queue at any time.
     */
    public Queue(Integer maxSize) {
        this.vector = new Vector();
        this.maxSize = maxSize;
    }
    
    /**
     * Add an object to the Queue. The object is placed at the end of the Queue.
     * 
     * @param object Object to be placed on the Queue
     * @throws FullException if this Queue has a maxSize limit (see
     *         {@link #Queue(Integer)}) and adding this object would exceed that
     *         size, this exception will be thrown and the object will not be
     *         added to the Queue.
     */
    synchronized public void add(Object object) throws FullException {
        final String METHOD_NAME = "add"; //$NON-NLS-1$
        if (this.maxSize != null) {
            // check the size
            if (this.vector.size() + 1 > this.maxSize.intValue()) {
            	Object[] params = {Integer.toString(this.vector.size()),this.maxSize};
                String message = Messages.getString("wamt.clientAPI.Queue.full",params); //$NON-NLS-1$
                FullException e = new FullException(message,"wamt.clientAPI.Queue.full",params);
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, message);
                throw(e);
            }
        }
        this.internalAdd(object);
    }

    synchronized void privilegedAdd(Object object) {
        // add without checking the maxsize
        this.internalAdd(object);
    }
    
    synchronized private void internalAdd(Object object) {
        final String METHOD_NAME = "internalAdd"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "Incoming object for queue" + //$NON-NLS-1$
                    " with pre-add size " + this.vector.size() + //$NON-NLS-1$
                    " with lifetimeCount " + this.lifetimeCount + //$NON-NLS-1$
                    " from thread " + Thread.currentThread().getName() +  //$NON-NLS-1$
                    ", adding object: " + object); //$NON-NLS-1$
        this.vector.add(object);
        this.lifetimeCount++;
        this.notifyAll();
    }
    
    /**
     * Remove an object from the queue. It will not block if there are no items
     * on the Queue.
     * 
     * @return the object removed from the Queue. The oldest object will be
     *         removed (FIFO).
     * @throws NotExistException if there are not any items on the Queue to
     *         remove
     * @see #removeWait()
     */
    synchronized public Object removeQuick() throws NotExistException {
        final String METHOD_NAME = "removeQuick"; //$NON-NLS-1$
        Object result = null;
        if (this.vector.size() > 0) {
            result = this.vector.remove(0);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "thread " + Thread.currentThread().getName() + //$NON-NLS-1$
                        " removed from queue: " + result); //$NON-NLS-1$
        } else {
            String message = Messages.getString("wamt.clientAPI.Queue.queueEmpty",Thread.currentThread().getName());
            NotExistException e = new NotExistException(message,"wamt.clientAPI.Queue.queueEmpty",Thread.currentThread().getName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return(result);
    }
    
    /**
     * Remove an object from the queue. If there are no items on the Queue, it
     * will block until there are items to remove.
     * 
     * @return the object removed from the Queue. The oldest object will be
     *         removed (FIFO).
     * @throws InterruptedException if this Thread is interrupted while waiting
     *         for an object to be added to the empty Queue.
     */
    synchronized public Object removeWait() throws InterruptedException {
        final String METHOD_NAME = "removeWait"; //$NON-NLS-1$
        while (this.vector.size() < 1) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "thread " + Thread.currentThread().getName() + //$NON-NLS-1$
                        " waiting for queue to have something"); //$NON-NLS-1$
            this.wait();
        }
        Object result = this.vector.remove(0);
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "thread " + Thread.currentThread().getName() + //$NON-NLS-1$
                    " removed from queue (wait): " + result); //$NON-NLS-1$
        return(result);
    }
    
    /**
     * Check if the Queue is empty.
     * 
     * @return true is the Queue has objects in it, false if the Queue is empty.
     */
    synchronized public boolean isEmpty() {
        boolean result = false;
        if (this.vector.size() > 0) {
            result = false;
        } else {
            result = true;
        }
        return(result);
    }
    
    /**
     * Get the number of objects that are currently in this Queue.
     * 
     * @return the number of objects that are in this Queue. If the Queue is
     *         empty, then it will return 0.
     */
    synchronized public int getSize() {
        return(this.vector.size());
    }
    
    /**
     * Get the maximum allowed size of this Queue.
     * 
     * @return the maximum allowed size of this Queue. If the Queue does not
     *         have a size limit (can be of unlimited size), then this method
     *         will return null.
     */
    synchronized public Integer getMaxSize() {
        return(this.maxSize);
    }
    
    /**
     * Get a reference to the object at the specified place in the Queue. It
     * does not remove the object from the Queue.
     * 
     * @param index the specified place in the Queue. The front of the Queue is
     *        index 0.
     * @return a reference to the object at the specified place in the Queue. If
     *         there is no object at the specified place (i.e., index 0 when the
     *         Queue is empty, or index 6 when there are 2 items in the Queue)
     *         then this method will return null.
     */
    synchronized public Object peek(int index) {
        Object result = null;
        try {
            result = this.vector.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            result = null;
        }
        return(result);
    }
    
    /**
     * Print the contents of all the objects in the Queue. It will invoke each
     * object's <code>toString()</code> method.
     */
    synchronized public String toString() {
        String result = "Queue[numObjects=" + this.vector.size(); //$NON-NLS-1$
        StringBuffer buf = new StringBuffer(result);
        for (int i=0; i<this.vector.size(); i++) {
            buf.append(", " + i + ": " + this.vector.get(i).toString()); //result += ", " + i + ": " + this.vector.get(i).toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        result = buf.toString();
        result += ", lifetimeCount=" + this.lifetimeCount; //$NON-NLS-1$
        result += ", currentSize=" + this.vector.size(); //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

}
