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

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.amp.Notification;

/**
 * A container for BackgroundTasks and Notifications that the QueueProcessor
 * will run. This is used to segment the areas affected by BackgroundTasks or
 * Notifications, so that locking can be somewhat granular instead of locking
 * everything and having only a single thread.
 * <p>
 * It is likely that only Manager (for the purpose of unmanaged devices) and
 * ManagedSet will extend this class.
 * <p>
 * 
 * @see QueueProcessor
 * @version SCM ID: $Id: WorkArea.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 * <p>
 */
//* Created on Oct 23, 2006
abstract class WorkArea {
    protected volatile Queue backgroundTaskQueue = null;
    protected volatile QueueCollection notificationQueue = null;
    
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    WorkArea(Integer backgroundTaskQueueSize) {
        // BackgroundTask queue may have a size limit, it is OK to use null here
        this.backgroundTaskQueue = new Queue(backgroundTaskQueueSize);
        // Notification queue has no size limit    
        this.notificationQueue = new QueueCollection();
    }
    
    boolean isBackgroundTaskQueueEmpty() {
        return(this.backgroundTaskQueue.isEmpty());
    }
    
    boolean isNotificationQueueEmpty() {
        return(this.notificationQueue.isEmpty());
    }
    
    boolean isNotificationQueueHidingItems() {
        return(this.notificationQueue.isHidingItems());
    }
    
    int backgroundTaskQueueSize() {
        return(this.backgroundTaskQueue.getSize());
    }

    Object backgroundTaskQueuePeek(int i) {
        return(this.backgroundTaskQueue.peek(i));
    }
    
    ReorderableQueue.SequencedObject notificationQueueRemoveQuick() {
        return(this.notificationQueue.removeQuick());
    }
    
    Object backgroundTaskQueueRemoveQuick() throws NotExistException {
        return(this.backgroundTaskQueue.removeQuick());
    }

    /**
     * Add a BackgroundTask to the queue, subject to the queue being full.
     * BackgroundTasks requested by the UI should go here.
     * <p>
     * <strong>Important Note:</strong> All enqueuing should be done via the
     * QueueProcessor class, so it becomes aware when items are added to a
     * queue. Then we can use a wait/notify model instead of sleep/poll. So the
     * only class that should call these methods is QueueProcessor, which is why
     * the method name is prefixed with "internal". All other classes should
     * call the matching methods in QueueProcessor, else QueueProcessor will
     * never be notified of the new Tasks.
     * 
     * @param backgroundTask
     *            the BackgroundTask to add to the queue to be later handled by
     *            the QueueProcessor thread
     * @throws FullException
     *             if the queue is already full
     * @see QueueProcessor#enqueue(BackgroundTask, WorkArea)
     * @see #internalPrivilegedEnqueue(BackgroundTask)
     * @see #internalEnqueue(Notification)
     */
    void internalEnqueue(BackgroundTask backgroundTask) throws FullException {
        this.backgroundTaskQueue.add(backgroundTask);
    }

    /**
     * Add a BackgroundTask to the queue, even if the queue is considered
     * "full". This should be called only by internal methods.
     * <p>
     * <strong>Important Note:</strong> All enqueuing should be done via the
     * QueueProcessor class, so it becomes aware when items are added to a
     * queue. Then we can use a wait/notify model instead of sleep/poll. So the
     * only class that should call these methods is QueueProcessor, which is why
     * the method name is prefixed with "internal". All other classes should
     * call the matching methods in QueueProcessor, else QueueProcessor will
     * never be notified of the new Tasks.
     * 
     * @param backgroundTask
     *            the BackgroundTask to add to the queue to be later handled by
     *            the QueueProcessor thread
     * @see QueueProcessor#privilegedEnqueue(BackgroundTask, WorkArea)
     * @see #internalEnqueue(BackgroundTask)
     * @see #internalEnqueue(Notification)
     */
    void internalPrivilegedEnqueue(BackgroundTask backgroundTask) {
        this.backgroundTaskQueue.privilegedAdd(backgroundTask);
    }

    /**
     * Add a Notification to the queue.
     * <p>
     * <strong>Important Note:</strong> All enqueuing should be done via the
     * QueueProcessor class, so it becomes aware when items are added to a
     * queue. Then we can use a wait/notify model instead of sleep/poll. So the
     * only class that should call these methods is QueueProcessor, which is why
     * the method name is prefixed with "internal". All other classes should
     * call the matching methods in QueueProcessor, else QueueProcessor will
     * never be notified of the new Tasks.
     * 
     * @param notification
     *            the Notification to add to the queue to be later handled by
     *            the QueueProcessor thread
     * @see QueueProcessor#enqueue(Notification, WorkArea)
     * @see #internalEnqueue(BackgroundTask)
     * @see #internalPrivilegedEnqueue(BackgroundTask)
     */
    void internalEnqueue(Notification notification) {
        this.notificationQueue.add(notification.getSequenceNumber(), 
                notification, notification.getDeviceSerialNumber());
    }
    
    /**
     * Modify the maximum time alloted (window size) for a ReorderableQueue to
     * put items in the correct sequence before it times out waiting for a
     * missing item and returns the first one it has.
     * 
     * @param ms
     *            the windows size in milliseconds. The default value is
     *            specified in ReorderableQueue.DEFAULT_WINDOW_SIZE_MS.
     * @see #getReorderingWindowSizeMS()
     * 
     */
    public static void setReorderingWindowSizeMS(long ms) {
        QueueCollection.setReorderingWindowSizeMS(ms);
    }
    
    /**
     * Get the maximum time allocated for a ReorderableQueue to put items in the
     * correct sequence before it times out waiting for a missing item and
     * returns the first one it has.
     * 
     * @return the window size in milliseconds.
     * @see #setReorderingWindowSizeMS(long)
     */
    public static long getReorderingWindowSizeMS() {
        return(QueueCollection.getReorderingWindowSizeMS());
    }

}
