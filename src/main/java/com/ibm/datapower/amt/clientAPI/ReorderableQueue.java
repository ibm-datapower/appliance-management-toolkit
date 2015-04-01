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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A FIFO container for objects, it is used both internally specifically for
 * events from the NotificationCatcher. An unfortunate situation exists with the
 * events coming from the device that the sending of events is not in a strict
 * serial order, so it is possible that they may be received out of sequence
 * (i.e., 1, 3, 4, 2). If events are received out of sequence, the
 * QueueProcessor assumes that events were lost and triggers a resync of the
 * whole device. If none of the events are lost but instead are received out of
 * sequence within a nice small time window (the sequence is eventually complete
 * within the time window), the device resync was unnecessary and incurrs
 * significant expense with network traffic, manager processing time, and even
 * perhaps delaying servicing more important items in the work queue.
 * <p>
 * If none of the events are lost but instead are received out of sequence but
 * within a nice small time window, we should reorder the events and present
 * them for retrieval from the queue in order. Only after a sufficient amount of
 * time has elapsed and we have not received the out of sequence events should
 * we surface that issue to the caller. So basically this class will hide out of
 * sequence objects from the caller for a short period of time to create a
 * window in which the missing objects may arrive and then be put in the queue
 * in the correct and complete sequence.
 * <p>
 * For more information about sequence numbers, see {@link Notification}.
 * <p>
 * It is expected that this class will hold only Notification objects. Therefore
 * this does not have a "maximum size" or "removeWait" capability as is in the
 * Queue class.
 * <p>
 * Internal comment: see WorkArea
 * <p>
 * 
 * @see Queue
 * @see QueueCollection
 * @version SCM ID: $Id: ReorderableQueue.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
class ReorderableQueue {
    // these members don't need to be marked volatile because synchronized methods are used to access them
    private Map map = null;
    private int lifetimeCount = 0;
    private Integer lastRetrievedSequenceNumber = null;
    private long windowStartTime = 0;
    private String name = null;
    
    public static final long DEFAULT_WINDOW_SIZE_MS = 5000;
    private static long windowSizeMS = DEFAULT_WINDOW_SIZE_MS;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = ReorderableQueue.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Create a new Queue that has no size limit (the number of objects that may
     * be on this queue at any one time).
     *  
     */
    public ReorderableQueue(String name) {
        this.map = new HashMap();
        this.name = name;
        
        this.lifetimeCount = 0;
        this.lastRetrievedSequenceNumber = null;
        this.windowStartTime = 0;
    }

    /**
     * Add an object to the queue. The object is placed in the queue in order
     * determined by the integer value of the sequenceNumber. Higher
     * sequenceNumber values are placed toward the tail end of the FIFO queue.
     * Within the context of the manager, this should get called only by
     * QueueCollection.add().
     * 
     * @param sequenceNumber
     *            the sequenceNumber of the object. The value should be
     *            monotomically increasing by 1 for each item placed in the
     *            queue. It does not matter what the starting value is. If there
     *            are gaps in the sequenceNumber then a removeQuick() may cause
     *            an OutOfSequenceException later if the gaps are not filled by
     *            that time.
     * @param object
     *            Object to be placed on the Queue
     */
    synchronized public void add(int sequenceNumber, Object object) {
        final String METHOD_NAME = "add"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "Incoming object for queue " + this.name + //$NON-NLS-1$
                    " with pre-add size " + this.map.size() + //$NON-NLS-1$
                    " with lifetimeCount " + this.lifetimeCount + //$NON-NLS-1$
                    " from thread " + Thread.currentThread().getName() +  //$NON-NLS-1$
                    ", adding sequenceNumber: " + sequenceNumber +  //$NON-NLS-1$
                    " and object: " + object); //$NON-NLS-1$
        Integer key = Integer.valueOf(sequenceNumber);
        if (this.map.containsKey(key)) {
            // shouldn't happen, but log it and keep going
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Duplicate sequence number " + sequenceNumber + //$NON-NLS-1$
                    " on queue " + this.name);  //$NON-NLS-1$
        }
        this.map.put(key, object);
        this.lifetimeCount++;
        if (this.windowStartTime == 0) {
            // even though this is set on retrieval and not add,
            // the very first add should start the window in case a retrieval
            // request hasn't happened yet.
            Date nowTimestamp = new Date();
            this.windowStartTime = nowTimestamp.getTime();
        }
    }
    
    /**
     * Check if the queue is hiding items it has, because the next expected
     * sequence number hasn't arrived yet, and the window timer also hasn't
     * expired (we haven't given up waiting yet for the missing sequence
     * number(s)).
     * 
     * @return true if there are items in the queue but the next expected
     *         sequence number is missing, and the window time has not yet
     *         expired. It will return false if there are no items in the queue,
     *         or if the next expected sequence number is present, or if the
     *         window time has expired.
     */
    synchronized public boolean isHidingItems() {
        final String METHOD_NAME = "isHidingItems";
        boolean result = false;
        if (this.map.isEmpty()) {
            // we ain't got nothin'
            result = false;
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "map is empty");
        } else {
            Date nowTimestamp = new Date();
            long nowTime = nowTimestamp.getTime();
            if (this.isReadyForPop(nowTime)) {
                // window has expired or items are in sequence, so we don't hide items now
                result = false;
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "isReadyForPop == true");
            } else {
                // is the next one in sequence?
                Integer expectedKey = this.computeNextKeyToRetrieve();
                if ((!this.map.containsKey(expectedKey)) || (this.hasLowerSequenceNumber())) {
                    result = true;
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "doesn't contain key or has lower sequence number");
                } else {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "contains key and not has lower sequence number");
                }
            }
        }
        return(result);
    }
    
    /**
     * Remove an object from the queue. It will not block, even if the queue is
     * not ready to return any new items.
     * <p>
     * "Ready to return a new item" means there is at least one object in the
     * queue and the one of the following conditions:
     * <ul>
     * <li>the item at the front of the queue is the next expected sequence
     * number.
     * <li>the item at the front of the queue is not the next expected sequence
     * number, but the window for receiving the missing has expired.
     * </ul>
     * If this is the first invocation (first object removed from the queue),
     * then it is assumed that the object with the lowest sequence number is
     * really the first expected object. If the first object(s) arrives late in
     * the queue (after the first <code>removeQuick()</code> already removed
     * other objects with higher sequence numbers), then the caller won't know
     * those objects are missing until they arrive in the queue with a sequence
     * number lower than what is expected.
     * 
     * @return a container for the object removed from the queue plus some
     *         metadata. The oldest object (lowest sequence number) will be
     *         removed from the queue and returned to the caller. If the queue
     *         is not ready to return a new item, a NotExistException will be
     *         thrown. If the queue is ready to return a new item but it is out
     *         of sequence, the lack of sequence continuity will be indicated as
     *         <code>SequencedObject.isInSequence()</code> and the object at the
     *         front of the queue will be returned.
     * @throws NotExistException
     *             There are no items in the queue. If the item at the front of
     *             the queue is not the expected next sequence number, and the
     *             window has not yet expired, this method will pretend there
     *             are no items in the queue and throw this exception.
     */
    synchronized public SequencedObject removeQuick() throws NotExistException {
        final String METHOD_NAME = "removeQuick"; //$NON-NLS-1$
        SequencedObject result = null;
        Date nowTimestamp = new Date();
        long nowTime = nowTimestamp.getTime();
        if (this.windowStartTime == 0) {
            this.windowStartTime = nowTime;
        }
        
        if (this.isReadyForPop(nowTime)) {
            // is the next one in sequence?
            Integer expectedKey = this.computeNextKeyToRetrieve();
            // expectedKey should never be null if isReadyForPop()
            Integer lowestKey = this.getLowestKey();
            // lowestKey should never be null if isReadyForPop()
            // check that expected key is the lowest key (look for latecomers)
            if ((this.map.containsKey(expectedKey)) && (!this.hasLowerSequenceNumber())) {
                // is in sequence, return it
                Object object = this.pop(expectedKey, nowTime);
                result = new SequencedObject(expectedKey.intValue(), true, object);
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "object on " + this.name +  //$NON-NLS-1$
                        " is in sequence: " + expectedKey);  //$NON-NLS-1$
            } else {
                // is out of sequence, return object with lowest sequence number (key)
                if (lowestKey != null) {
                    // found the lowest, return it
                    Object object = this.pop(lowestKey, nowTime);
                    result = new SequencedObject(lowestKey.intValue(), false, object);
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                            "object on " + this.name +                     //$NON-NLS-1$
                            " out of sequence, expected " + expectedKey +  //$NON-NLS-1$
                            " but found " + lowestKey);                    //$NON-NLS-1$
                } else {
                    // this is strange, we just checked that size() > 0
                    String message = Messages.getString("wamt.clientAPI.Queue.queueEmpty", Thread.currentThread().getName());
                    NotExistException e = new NotExistException(message, "wamt.clientAPI.Queue.queueEmpty", Thread.currentThread().getName());
                    logger.throwing(CLASS_NAME, METHOD_NAME, e);
                    throw(e);
                }
            }
        } else {
            // not ready for pop
            String message = Messages.getString("wamt.clientAPI.Queue.queueEmpty", Thread.currentThread().getName());
            NotExistException e = new NotExistException(message, "wamt.clientAPI.Queue.queueEmpty", Thread.currentThread().getName());
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        
        return(result);
    }
    
    synchronized private Integer getLowestKey() {
        // find lowest sequence number (key)
        Set keys = this.map.keySet();
        Iterator iterator = keys.iterator();
        Integer lowestKey = null;
        while (iterator.hasNext()) {
            Integer aKey = (Integer) iterator.next();
            if ((lowestKey == null) || (aKey.intValue() < lowestKey.intValue())) {
                lowestKey = aKey;
            }
        }
        return(lowestKey);
    }
    
    synchronized private Integer computeNextKeyToRetrieve() {
        // it may be null if one hasn't been retrieved yet
        if (this.lastRetrievedSequenceNumber != null) {
            int nextSequenceNumber = this.lastRetrievedSequenceNumber.intValue() + 1;
            return(Integer.valueOf(nextSequenceNumber));
        } else {
            return(this.getLowestKey());
        }
    }
    
    synchronized private Object pop(Integer key, long time) {
        // this method will also take care of all the side effects, so they are in one place
        final String METHOD_NAME = "pop"; //$NON-NLS-1$

        Object object = this.map.remove(key);
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "thread " + Thread.currentThread().getName() +               //$NON-NLS-1$
                " popping from " + this.name +                               //$NON-NLS-1$
                " last sequenceNumber " + this.lastRetrievedSequenceNumber + //$NON-NLS-1$
                " this sequenceNumber " + key.intValue() +                   //$NON-NLS-1$
                " removed from queue: " + object);                           //$NON-NLS-1$
        this.lastRetrievedSequenceNumber = key;
        this.windowStartTime = time;
        return(object);
    }
    
    synchronized private boolean isReadyForPop(long nowTime) {
        final String METHOD_NAME = "isReadyForPop"; 
        boolean result = false;
        if (this.windowStartTime == 0) {
            this.windowStartTime = nowTime;
        }
        if (nowTime - this.windowStartTime > ReorderableQueue.windowSizeMS) {
            // window expired: are there no objects in queue?
            if (this.map.size() == 0) {
                result = false;
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "window expired, no objects in queue");
            } else {
                result = true;
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "window expired, objects in queue");
            }
        } else {
            // window not expired
            // in sequence?
            Integer expectedKey = this.computeNextKeyToRetrieve();
            if (expectedKey == null)  {
                // this queue has never contained an object
                result = false;
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "this queue has never contained an object");
            } else if ((this.map.containsKey(expectedKey)) && (!this.hasLowerSequenceNumber())) {
                // in sequence, go ahead and return it
                result = true;
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "contains expected key and doesn't have lower");
            } else {
                // not in sequence, fake an empty queue so the caller will try again later
                result = false;
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "queue " + this.name +                                     //$NON-NLS-1$
                        " has expected key " + this.map.containsKey(expectedKey) + //$NON-NLS-1$
                        ", may have lower, faking empty queue");                   //$NON-NLS-1$
            }
        }
        return(result);
    }
    
    synchronized private boolean hasLowerSequenceNumber() {
        // a latecomer arrived in the queue with a seqnum lower than
        // (or a duplicate of) what was already previously retrieved
        boolean result = false;
        if (this.lastRetrievedSequenceNumber == null) {
            result = false;
        } else {
            Integer lowest = this.getLowestKey();
            if (lowest.intValue() <= this.lastRetrievedSequenceNumber.intValue()) {
                result = true;
            } else {
                result = false;
            }
        }
        return(result);
    }
    
    /**
     * Check if the Queue is empty.
     * 
     * @return true is the Queue has objects in it, false if the Queue is empty
     *         or if the Queue has a gap in the sequence number before the
     *         windowSizeMS timeout expires.
     */
    synchronized public boolean isEmpty() {
        boolean result = false;
        Date nowTimestamp = new Date();
        long nowTime = nowTimestamp.getTime();
        if (this.isReadyForPop(nowTime)) {
            result = false;
        } else {
            result = true;
        }
        return(result);
    }
    
    /**
     * Print the contents of all the objects in the Queue. It will invoke each
     * object's <code>toString()</code> method.
     */
    synchronized public String toString() {
        String result = "ReorderableQueue[numObjects=" + this.map.size(); //$NON-NLS-1$
        Set keys = this.map.keySet();
        Iterator iterator = keys.iterator();
        StringBuffer buf = new StringBuffer(result);
        while (iterator.hasNext()) {
            Integer key = (Integer) iterator.next();
            buf.append(", " + key.intValue() + ": " + this.map.get(key).toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        result = buf.toString();
        result += ", lifetimeCount=" + this.lifetimeCount; //$NON-NLS-1$
        result += ", currentSize=" + this.map.size(); //$NON-NLS-1$
        result += "]"; //$NON-NLS-1$
        return(result);
    }

    synchronized public static long getWindowSizeMS() {
        return(ReorderableQueue.windowSizeMS);
    }

    synchronized public static void setWindowSizeMS(long windowMs) {
        ReorderableQueue.windowSizeMS = windowMs;
    }

    /**
     * A simple container to retrieve multiple pieces of data (object + metadata)
     * from a ReorderableQueue. This is simply the queued object plus some metadata
     * regarding the object.
     * <p> 
     * 
     */
//    * Created on Jan 8, 2008
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
    		justification="Seems to be a Findbugs false positive - this inner " +
    				"class *is* static, isn't it?",
    		value="SIC_INNER_SHOULD_BE_STATIC")
    static class SequencedObject {
        private int sequenceNumber = 0;
        private boolean isInSequence = false;
        private Object object = null;
        
        SequencedObject(int sequenceNumber, boolean isInSequence, Object object) {
            this.sequenceNumber = sequenceNumber;
            this.isInSequence = isInSequence;
            this.object = object;
        }
 
        public int getSequenceNumber() {
            return(this.sequenceNumber);
        }
        
        public boolean isInSequence() {
            return(this.isInSequence);
        }

        public Object getObject() {
            return(this.object);
        }

    }

}
