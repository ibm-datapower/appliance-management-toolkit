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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A QueueCollection is needed to sort all the Notifications in a ManagedSet
 * into the proper sequence for processing by the QueueProcessor. There is a
 * ReorderableQueue per device, since notifications from devices may be received
 * slightly out-of-sequence and they need to be reordered on a device basis. See
 * <code>ReorderableQueue</code> for more information on that topic. There is one
 * QueueCollection per ManagedSet, since the QueueProcessor wants to process
 * notifications from the ManagedSet as a whole, so we need to aggregate all the
 * device queues into a single virtual queue that the QueueProcessor sees. This
 * class does that aggregation / virtualization.
 * <p>
 * Although the notifications on individual device queues may be reordered, the
 * QueueCollection attempts to keep track of the sequence of which devices had
 * notifications. So although the device notifications may not be retrieved in
 * the same order they were received, we do make an attempt at approximating
 * that. It is an approximation because device A's notifications may be waiting
 * for reordering while device B's notifications are ready even though device
 * A's notifications arrived first. The notifications from the ready device will
 * be returned, but we will check for readiness in the arrival order. First come
 * and first ready, then first served.
 * <p>
 * Only the NotificationCatcher will be adding notifications to the device
 * queues, and only the QueueProcessor will be removing them.
 * <p>
 * Internal note: see <code>ReorderableQueue</code> and <code>QueueProcessor</code>
 * 
 */
public class QueueCollection {

    Map deviceQueues = null;
    Vector arrivalSequence = null;
    
    protected static final String CLASS_NAME = QueueCollection.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    QueueCollection() {
        deviceQueues = new HashMap();
        arrivalSequence = new Vector();
    }
   
    /**
     * Add an item to one of the queues in the collection. The specific queue in
     * which this item will be added will be determined by the device
     * serialNumber parameter. If the device queue does not yet exist, it will
     * be created. Within the context of the manager, this should get called only by
     * WorkArea.internalEnqueue().
     * 
     * @param sequenceNumber
     *            the notification sequence number. This is unique per device,
     *            and should increment monotomically.
     * @param notification
     *            the Notification object to be enqueued.
     * @param serialNumber
     *            the serial number of the device from which this Notification
     *            originated.
     */
    synchronized void add(int sequenceNumber, Notification notification,
            String serialNumber) {
        // first, find the correct ReorderableQueue. This may be for a new device.
        ReorderableQueue deviceQueue = 
            (ReorderableQueue) this.deviceQueues.get(serialNumber);
        if (deviceQueue == null) {
            // this is a new device for us. Create a queue for it
            deviceQueue = new ReorderableQueue(serialNumber);
            this.deviceQueues.put(serialNumber, deviceQueue);
        }
        deviceQueue.add(sequenceNumber, notification);
        // indicate the order of which queue had an notification
        arrivalSequence.add(deviceQueue);
    }
    
    /**
     * Check to see if any of the ReorderableQueues have any items ready.
     * 
     * @return true if any of the device queues have a Notification ready to be
     *         read. Otherwise, false.
     */
    synchronized boolean isEmpty() {
        // don't look at the sequence Vector, look at the ReorderableQueues
        // themselves, because they may be masking out-of-sequence objects.
        boolean result = true;
        Collection queues = this.deviceQueues.values();
        Iterator iterator = queues.iterator();
        while (iterator.hasNext()) {
            ReorderableQueue queue = (ReorderableQueue) iterator.next();
            if (!queue.isEmpty()) {
                result = false;
            }
        }
        return(result);
    }
    
    /**
     * Check to see if any of the ReorderableQueues are hiding any items.
     * 
     * @return true if any of the device queues are hiding a Notification
     *         because it is out-of-sequence but the time window for getting the
     *         missing sequence numbers hasn't expired yet.
     */
    synchronized boolean isHidingItems() {
        boolean result = false;
        Collection queues = this.deviceQueues.values();
        Iterator iterator = queues.iterator();
        while (iterator.hasNext()) {
            ReorderableQueue queue = (ReorderableQueue) iterator.next();
            if (queue.isHidingItems()) {
                result = true;
            }
        }
        return(result);
    }
    
    /**
     * Remove an item from the first ready queue. First come and first ready,
     * then first served.
     * 
     * @return an item which was previoulsy enqueued. May be null if no queues
     *         have an item ready.
     */
    synchronized ReorderableQueue.SequencedObject removeQuick() {
        // go through the vector to see which ReorderableQueues might have
        // an object ready, put retrieve the object only if it is ready to
        // be pulled.
        ReorderableQueue.SequencedObject result = null;
        for (int i=0; i<this.arrivalSequence.size(); i++) {
            ReorderableQueue queue = (ReorderableQueue) this.arrivalSequence.get(i);
            if (!queue.isEmpty()) {
                try {
                    result = queue.removeQuick();
                    this.arrivalSequence.remove(i);
                    break;
                } catch (NotExistException e) {
                    result = null;
                    continue;
                }
            }
        }
        return(result);
    }
    
    /**
     * Remove any queues in the collection which are no longer needed, because a
     * device is no longer in the specified managed set.
     * 
     * @param managedSet
     *            the managed set to which this queue collection belongs.
     */
    synchronized void clean(ManagedSet managedSet) {
        Set seen = new HashSet();
        try {
            Device[] devices = managedSet.getDeviceMembers();
            for (int i=0; i<devices.length; i++) {
                String serialNumber = devices[i].getSerialNumber();
                if (serialNumber != null) {
                    ReorderableQueue deviceQueue = 
                        (ReorderableQueue) this.deviceQueues.get(serialNumber);
                    if (deviceQueue != null) {
                        seen.add(serialNumber);
                    }
                }
            }
        } catch (DeletedException e) {
            // the managedSet was deleted, so don't mark any seen
        }
        Set keySet = this.deviceQueues.keySet();
        String[] keys = new String[keySet.size()];
        keys = (String[]) keySet.toArray(keys);
        keySet=null;
        for (int i=0; i<keys.length; i++) {
            String serialNumber = keys[i];
            if (!seen.contains(serialNumber)) {
                ReorderableQueue deviceQueue = (ReorderableQueue) this.deviceQueues.remove(serialNumber);
                while (this.arrivalSequence.contains(deviceQueue)) {
                    this.arrivalSequence.remove(deviceQueue);
                }
            }
        }
    }

    /**
     * See {@link WorkArea#setReorderingWindowSizeMS(long)}.
     * 
     * @param ms
     *            the window size in milliseconds.
     */
    static void setReorderingWindowSizeMS(long ms) {
        ReorderableQueue.setWindowSizeMS(ms);
    }

    static long getReorderingWindowSizeMS() {
        return(ReorderableQueue.getWindowSizeMS());
    }
}
