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

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.AMPException;
import com.ibm.datapower.amt.amp.Notification;
import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A implementation of the Thread to process BackgroundTask items and
 * Notification items that are on their respective queues. Those Task items are
 * placed on the Queues by the clientAPI methods and the NotificationCatcher.
 * The QueueProcess will not lock the respective WorkArea, it will let the Task
 * do that. Since we want each Task to run without aborting and we really don't
 * care about the length of execution, each Task will do a blocking lock
 * acquisition, and the Task runs on the QueueProcessor thread, so it is possible
 * that the QueueProcessor may take a while to get through each Task. There is
 * only one QueueProcessor thread, which is a conscious design decision to avoid
 * lock contention and data synchronization. Since the enqueueing and dequeueing of
 * Tasks is synchronized, there are no explicit Lock objects to coordinate that.
 * If there are no Tasks in any queue, then the QueueProcessor will go into a
 * <code>wait()</code> state until it is notified that a new Task has been
 * queued. Thus, the QueueProcessor thread does not poll the queues.
 * <p>
 * 
 * @see WorkArea
 * @see BackgroundTask
 * @see Notification
 * @version SCM ID: $Id: QueueProcessor.java,v 1.6 2011/04/08 15:12:32 wjong Exp $
 * <p>
 */
//* Created on Sep 19, 2006
class QueueProcessor implements Runnable {
    private boolean shutdownRequested = false;
    // a Collection that I want to reuse instead of creating on each loop
    private Vector workAreas = null;
    private Thread thread = null;
    private String activeMethodName = null;
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = QueueProcessor.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    QueueProcessor() {
        this.workAreas = new Vector();
    }
    
    void startup() {
        final String METHOD_NAME = "startup"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "start requested"); //$NON-NLS-1$
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("QueueProcessor"); //$NON-NLS-1$
        this.shutdownRequested = false;
        thread.start();
    }
    
    void shutdown() {
        final String METHOD_NAME = "shutdown"; //$NON-NLS-1$
        this.activeMethodName = METHOD_NAME;

        this.shutdownRequested = true;
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "checking if thread isAlive..."); //$NON-NLS-1$
        while (this.thread.isAlive()) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "attempting to acquire monitor for QueueProcessor...");
            synchronized (this) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "monitor acquired, sending notifyAll...");
                this.notifyAll();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "sleep interrupted");
            }
        }
        this.activeMethodName = null;
    }
    
    String getActiveMethodName() {
        return(this.activeMethodName);
    }
    
    public void run() {
        /*
         * The run() method should not be synchronized because we don't want it
         * holding the object lock while it is doing the long-running execution
         * of the Task. Only the waitForTask() and enqueue() methods should be
         * synchronized. This is because the enqueuing methods are also
         * synchronized, so holding the lock during the long-running Task
         * execution would block other threads from enqueuing new Tasks. We want
         * the other tasks to be able to quickly enqueue.
         */
        final String METHOD_NAME = "run"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                "thread running: " + Thread.currentThread().getName()); //$NON-NLS-1$
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME;
        
        while (true) {
            try {
                // wait for indicator of a Task so we don't suck up all the CPU
                Object object = this.waitForTask();
                if ((object == null) && (!this.shutdownRequested)) {
                    // a Notification is hiding and not ready yet.
                    // so don't wait, but sleep a bit until it is ready.
                    long ms = ReorderableQueue.getWindowSizeMS() / 2;
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                            "received null task (indicates isHiding), sleeping " + ms + "ms");
                    Thread.sleep(ms);
                } else if ((object == null) && (this.shutdownRequested)) { 
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "exiting run() loop because shutdown requested");
                    break;
                } else if (object instanceof BackgroundTask) {
                    BackgroundTask backgroundTask = (BackgroundTask) object;
                    this.process(backgroundTask);
                } else if (object instanceof ReorderableQueue.SequencedObject) {
                    ReorderableQueue.SequencedObject sequencedObject =
                        (ReorderableQueue.SequencedObject) object;
                    this.process(sequencedObject);
                } else {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            "unknown instance from waitForTask()");
                }
            } catch (InterruptedException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                        "thread interrupted.", e); //$NON-NLS-1$
                this.shutdownRequested = true;
            } catch (Exception e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UncheckedException"), e); //$NON-NLS-1$
            } catch (Throwable e) {
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME,
                        Messages.getString("wamt.clientAPI.QueueProcessor.uncaughtEx"), //$NON-NLS-1$
                        e);
            }
        }
        
        if (this.shutdownRequested) {
            /*
             * pull every task from every queue and set an error in the
             * ProgressContainer. Then if another thread is blocking
             * on the ProgressContainer, it won't hang indefinitely,
             * and will know the outcome. This is just a good cleanup.
             */
            
            // the list of workAreas can change, so rebuild the list
            this.determineWorkAreas();
            
            Iterator iterator = this.workAreas.iterator();
            while (iterator.hasNext()) {
                WorkArea workArea = (WorkArea) iterator.next();

                // get all items from the BackgroundTask Queue
                while (!workArea.isBackgroundTaskQueueEmpty()) {
                    Object object = null;
                    try {
                        object = workArea.backgroundTaskQueueRemoveQuick();
                    } catch (NotExistException e1) {
                        object = null;
                    }
                    if ((object != null) && (object instanceof BackgroundTask)) {
                        BackgroundTask backgroundTask = (BackgroundTask) object;
                        // we do want an UnsubscribeAll to run so there is a clean exit
                        if (backgroundTask instanceof UnsubscribeAllTask) {
                            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                    "about to run UnsubscribeAll after shutdown requested");
                            this.process(backgroundTask);
                        } else {
                            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                    "discarding BackgroundTask after shutdown requested: " +
                                    backgroundTask);
                            ProgressContainer progressContainer =
                                backgroundTask.getProgressContainer();
                            // mark the ProgressContainer complete with an exception
                            // LS  Any discarded DomainSynchronization tasks will be
                            // created, based on the flags persisted in the repository, and re-tried
                            // when the Manager comes up next time.
                            String taskName = backgroundTask.getClass().getName();
                            taskName = taskName.substring(taskName.lastIndexOf('.')+1);
                            String message = Messages.getString("wamt.clientAPI.QueueProcessor.shutdown",
                                    taskName);
                            ShutdownException shutdownException =
                                new ShutdownException(message,
                                        "wamt.clientAPI.QueueProcessor.shutdown",
                                        taskName);
                            progressContainer.setError(shutdownException);
                            // clean up the task since we aren't invoking execute()
                            backgroundTask.cleanup();
                        }
                    }
                }

                /*
                 * We don't need to do anything for
                 * ProgressContainers/MacroProgressContainers on the
                 * Notification queue, because the MacroProgressContainers for
                 * Notifications don't get created until the Notification is
                 * processed in process(Notification, boolean). Since that isn't
                 * getting called because we eat the Notification here, then
                 * there is no ProgressContainer/MacroProgressContainer to clean
                 * up. So just pulling it off the queue and discarding it is
                 * sufficient.
                 */
                while (!workArea.isNotificationQueueEmpty()) {
                    workArea.notificationQueueRemoveQuick();
                }
            }
        }
        
        this.activeMethodName = oldMethodName;
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                " QueueProcessor Shutdown Completed");
       
    }
    
    /*
     * Synchronize the queueing of Tasks so we can notify the QueueProcessor
     * when they arrive. If the QueueProcessor doesn't have any work, it will
     * wait(), which is why we need to notify() it of the new work so it can
     * wake up and pull it from a queue and execute it. These enqueue methods
     * should be invoked only by Manager, and the Manager.*enqueue* methods
     * should be the only way to put BackgroundTasks and Notifications on queues
     * for the QueueProcessor. Manager invokes QueueProcessor which invokes
     * WorkArea which invokes QueueCollection which invokes ReorderableQueue.
     * 
     * Even though the methods in ReorderableQueue and QueueCollection are also
     * synchronized on their respective objects, invoking those methods is done
     * via only this class which is also synchronized. So perhaps
     * ReorderableQueue and QueueCollection don't need to have synchronized
     * methods, but it won't hurt (won't be any race conditions because of the
     * common entry point), and it is nicely modular. If you jump into a
     * different entry point than the one listed above, problems might occur.
     */
    
    synchronized void enqueue(Notification notification, WorkArea workArea) {
        workArea.internalEnqueue(notification);
        this.notifyAll();
    }
    
    synchronized void enqueue(BackgroundTask backgroundTask, WorkArea workArea) 
    throws FullException {
        workArea.internalEnqueue(backgroundTask);
        this.notifyAll();
    }
    
    synchronized void privilegedEnqueue(BackgroundTask backgroundTask, WorkArea workArea) {
        workArea.internalPrivilegedEnqueue(backgroundTask);
        this.notifyAll();
    }
    
    /**
     * Pull the next Task from any queue. If there is no available Task, wait.
     * WorkArea does not need to be locked before retrieving the Task because
     * access to the queues is synchronized via <code>this</code>.
     * 
     * @return either a BackgroundTask or a ReorderableQueue.SequencedObject.
     *         This may also return null in the case that (1) there is no Task
     *         ready at the moment, but there is an out-of-sequence Task hiding
     *         in a ReorderableQueue waiting for the missing Tasks to arrive, or
     *         (2) if a shutdown has been requested. So the caller should be
     *         prepared to receive a null value and check
     *         this.shutdownRequested.
     * @throws InterruptedException
     *             the wait was interrupted. That may occur if a shutdown was
     *             requested.
     */
    synchronized private Object waitForTask()
    throws InterruptedException, NotExistException {
        final String METHOD_NAME = "waitForTask";
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME;

        Object result = null;

        // if shutdownRequested, handle that before Tasks
        outer: while ((result == null) && (!this.shutdownRequested)) {
            // the list of workAreas can change while waiting, so rebuild the list
            this.determineWorkAreas();
            // look for BackgroundTask
            Iterator iterator = this.workAreas.iterator();
            inner: while (iterator.hasNext()) {
                WorkArea workArea = (WorkArea) iterator.next();
                
                /*
                 * Note that for each WorkArea, we look for one BackgroundTask,
                 * and if none found then look for one Notification. This has
                 * the effect of giving each WorkArea an equal chance to get one
                 * thing done before going on to the next WorkArea, so that a
                 * single WorkArea with a long queue doesn't hog the system. It
                 * also places BackgroundTasks as a higher priority than
                 * Notifications.
                 */
                
                // look for something on the BackgroundTask Queue
                if ((result == null) && 
                        (!workArea.isBackgroundTaskQueueEmpty()) &&
                        (!this.shutdownRequested)) {
                    Object object = null;
                    // pull the item from the Queue so it can be executed
                    try {
                        object = workArea.backgroundTaskQueueRemoveQuick();
                    } catch (NotExistException e1) {
                        // wow, this shouldn't happen, we just checked
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "BackgroundTask queue was not empty but now it is"); //$NON-NLS-1$
                        // just check again
                    }
                    if (object instanceof BackgroundTask) {
                        logger.log(Level.FINE, 
                                "Popped " +  //$NON-NLS-1$
                                object.toString() +
                                " from BackgroundTask queue: " + //$NON-NLS-1$ 
                                workArea);
                        result = object; 
                        break inner;
                    } else {
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                                "object is not instanceof BackgroundTask: " + object); //$NON-NLS-1$
                        /*
                         * This is a potential deadlock situation, because
                         * we have received a work request that we don't
                         * know how to complete, and the sender is probably
                         * waiting for a result that we will never return to
                         * them. Oh, what to do, what to do?
                         */
                    }
                }
                
                // look for something on the Notification Queue if no result thus far
                if ((result == null) && 
                        (!workArea.isNotificationQueueEmpty()) &&
                        (!this.shutdownRequested)) {
                    // retrieve the queue item
                    ReorderableQueue.SequencedObject sequencedObject = 
                        workArea.notificationQueueRemoveQuick();
                    logger.log(Level.FINE, 
                            "Popped from Notification queue " +  //$NON-NLS-1$ 
                            sequencedObject.toString() + 
                            " from " + workArea); //$NON-NLS-1$
                    result = sequencedObject;
                    break inner;
                }
                
                // if nothing on either queue, then go to next WorkArea
            }
            
            // We looked in BackgroundTaskQueue and NotificationQueue in all WorkAreas.
            
            if (result == null) {
                /*
                 * Check to see if there is a Notification hiding because the
                 * Notification is out-of-sequence. This is a problem because we
                 * were notified when it was added to the queue, but we won't be
                 * able to dequeue and execute it while it is hiding. So if we
                 * go into wait, it will become unhidden when the window time
                 * expires, which is not tied to a notify(). So if we wait() we
                 * may not be notified when it becomes unhidden. So we need to
                 * explicitly check if something is hiding, and then come back
                 * and look again for ready Tasks without going into wait(). We
                 * will signal this condition to the caller by returning a null
                 * instead of a real task.
                 */
                boolean isHidingItems = false;
                iterator = this.workAreas.iterator();
                while (iterator.hasNext()) {
                    WorkArea workArea = (WorkArea) iterator.next();
                    if (workArea.isNotificationQueueHidingItems()) {
                        isHidingItems = true;
                        // return a null to the caller and check later, release lock on this 
                        break outer;
                    }
                }
                /*
                 * If there really isn't anything even hiding, then wait. But
                 * if shutdownRequested, then fall out of this loop and return
                 * null to the caller.
                 */
                if ((!isHidingItems) && !(this.shutdownRequested)) {
                    // just in case we get in something like deadlock, 
                    // go look again in 30 minutes
                    this.wait(1000 * 60 * 30);
                }
            }
        }
        this.activeMethodName = oldMethodName;
        return(result);
    }
    
    private void determineWorkAreas() {
        final String METHOD_NAME = "determineWorkAreas"; //$NON-NLS-1$
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME;

        this.workAreas.clear();
        // walk through each of the ManagedSets
        Manager manager = Manager.internalGetInstance();
        if (manager == null) {
            // Manager hasn't started yet. That's weird.
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, 
                    Messages.getString("wamt.clientAPI.QueueProcessor.managerNull")); //$NON-NLS-1$
            this.activeMethodName = oldMethodName;
            return;
        }
        ManagedSet[] managedSets = manager.getManagedSets();
        for (int i=0; i<managedSets.length; i++) {
            this.workAreas.add(managedSets[i]);
        }
        
        // include the unmanaged Devices queue
        this.workAreas.add(manager);
        
        this.activeMethodName = oldMethodName;
    }
    
    private void process(BackgroundTask backgroundTask) {
        final String METHOD_NAME = "process(BackgroundTask)"; //$NON-NLS-1$
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME + ": " + backgroundTask;  //$NON-NLS-1$

        // execute it. Doesn't get much easier than that.
        backgroundTask.execute();

        this.activeMethodName = oldMethodName;
    }
    
    /**
     * Handle a notification that was queued.
     */
    private void process(ReorderableQueue.SequencedObject sequencedObject) {
        final String METHOD_NAME = "processNotifications"; //$NON-NLS-1$
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME + ": " + sequencedObject; //$NON-NLS-1$

        // retrieve and process the queue item
        Object object = null;
        if (sequencedObject != null) {
            object = sequencedObject.getObject();
        }
        if (object instanceof Notification) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "About to process: " + object.toString()); //$NON-NLS-1$
            Notification notification = (Notification) object;
            boolean isInSequence = sequencedObject.isInSequence();
            this.process(notification, isInSequence);
        } else {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "object is not instanceof Notification: " + object); //$NON-NLS-1$
            // just discard any objects that are malformed
        }

        this.activeMethodName = oldMethodName;
    }

    /**
     * Dispatcher to call the processing for each type of Notification. This is
     * so all the Notification processing doesn't have to be in one gigantic
     * method. This method is where we handle gaps and/or resets in the
     * sequence number.
     * <p>
     * Note that a device may send notifications in a slightly out-of-sequence
     * order, due to queueing of event transmission on the device. We don't care
     * if the notifications are out-of-sequence in the queue, we basically
     * ignore the sequence number. Since all that we do for notifications is
     * update state data (in the case of a domain opstate change) and trigger a
     * device sync (in the case of a device "SaveConfig"), the sequence of the
     * events doesn't matter as long as we process all the events eventually. We
     * don't care if we take a non-optimal path to the correct state, as long as
     * we end up in the correct state. That is how we are able to not care about
     * the notification sequence order.
     * <p>
     * If there are notifications that we never received, then the device
     * subscription should either be in a FAULT state or the subscription will
     * be missing. The HeartbeatDaemon will monitor the device subscription for
     * a FAULT state or missing state, and if that state is detected then the
     * HeartbeatDaemon will trigger a device sync but putting a sync Task on the
     * queue. That is how we are able to not care about gaps in the sequence
     * numbers.
     * <p>
     * So although the notification provides us a sequence number, we don't need
     * to process the notifications in any particular order. HOWEVER, we do need
     * to know if there are gaps in the sequence number. For that purpose
     * notifications are sorted by sequence number (on a per-device basis) via
     * the ReorderableQueue. When we pull a notification from the
     * ReorderableQueue it will tell us if it is in sequence or if there is a
     * gap and/or reset in the sequence numbers. If we see a gap/reset in the
     * sequence numbers, then we should invoke a sync in the foreground on this
     * thread.
     * 
     * @param notification
     * @param isInSequence
     * @throws AMPException 
     */
    private void process(Notification notification, boolean isInSequence) {
        final String METHOD_NAME = "process(Notification)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, 
                new Object[] {notification, Boolean.valueOf(isInSequence)});
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME + ": " + notification + ": " + isInSequence; //$NON-NLS-1$ //$NON-NLS-2$
        
        if (!isInSequence) {     
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                    "encountered out-of-sequence notification from device serial number " +  //$NON-NLS-1$   
                    notification.getDeviceSerialNumber());   
            // start a device sync. Can ignore this notification since sync will handle it.      
            Manager manager = Manager.internalGetInstance();     
            Device device = null;    
            try {    
                device = manager.getDeviceBySerialNumber(notification.getDeviceSerialNumber());      
            } catch (DeletedException e) {   
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                        "unable to get device by serial number, was deleted", e); //$NON-NLS-1$   
                device = null;   
            }    
            if (device == null) {    
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                        "a Device with the serial number " + notification.getDeviceSerialNumber() + //$NON-NLS-1$    
                        " does not exist. Aborting processing of notification."); //$NON-NLS-1$      
            } else {     
                ManagedSet managedSet = null;    
                try {    
                    managedSet = device.getManagedSet();     
                } catch (DeletedException e1) {      
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                            "unable to check if device is in a managed set, managed set deleted", e1); //$NON-NLS-1$      
                    managedSet = null;   
                }    
                if (managedSet == null) {    
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                            device + " is not in a managed set. Ignoring notification."); //$NON-NLS-1$      
                } else {     
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                            "initiating device sync"); //$NON-NLS-1$     
                    MacroProgressContainer macroProgressContainer =      
                        new MacroProgressContainer(notification);    
                    ProgressContainer progressContainer = 
                        new ProgressContainer(notification);   
                    macroProgressContainer.addNested(progressContainer);     
                    manager.addNotificationProgress(macroProgressContainer);     
                    if (progressContainer.hasError()) {      
                        logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,     
                                Messages.getString("wamt.clientAPI.QueueProcessor.syncError",device.getDisplayName()),    
                                    progressContainer.getError());   
                    } else {     
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,     
                                "device sync completed successfully"); //$NON-NLS-1$     
                        progressContainer.setComplete();     
                        macroProgressContainer.setComplete();    
                    }    
                }    
            }    
        } else {     
            // is valid sequence number
//            if (notification.isFirmwareChange()) {
//                this.processFirmwareChange(notification);
//            } else if (notification.isOpStateChangeUp() || notification.isOpStateChangeDown()) {
            if (notification.isOpStateChangeUp() || notification.isOpStateChangeDown()) {
                this.processOpStateChange(notification);
            } else if (notification.isTest()) {
                this.processTest(notification);
            } else {
                // the default domain could contain changed service objects 
                if (notification.isSaveConfigOfDomain()) {
                    this.processSaveConfigOfDomain(notification);
                }
                if (!notification.isSaveConfigOfDomain() && !notification.isSaveConfigOfSettings()) {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                            "ignoring notification because of event code"); //$NON-NLS-1$
                }
            }
        }

        this.activeMethodName = oldMethodName;
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
//    private void processFirmwareChange(Notification notification) {
//        final String METHOD_NAME = "processFirmwareChange"; //$NON-NLS-1$
//        logger.entering(CLASS_NAME, METHOD_NAME);
//        String oldMethodName = this.activeMethodName;
//        this.activeMethodName = METHOD_NAME + ": " + notification; //$NON-NLS-1$
//        
//        // this really is quite straightforward, except for all the try/catch blocks.
//        Manager manager = Manager.internalGetInstance();
//        MacroProgressContainer macroProgressContainer = 
//            new MacroProgressContainer(notification);
//        manager.addNotificationProgress(macroProgressContainer);
//        
//        // get the Device object from the serial number
//        String serialNumber = notification.getDeviceSerialNumber();
//        Device device = null;
//        try {
//            device = manager.getDeviceBySerialNumber(serialNumber);
//        } catch (DeletedException e) {
//            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
//                    "deleted: " + e); //$NON-NLS-1$
//            device = null;
//        }
//        if (device == null) {
//            String message = Messages.getString("wamt.clientAPI.QueueProcessor.devRefNull"); //$NON-NLS-1$
//            Exception e = new NotExistException(message);
//            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message); 
//            macroProgressContainer.setError(e);
//            this.activeMethodName = oldMethodName;
//            return;
//        }
//        
////        System.out.println("**********************************************************");
////        System.out.println("TCB The processFirmwareChange code is DISABLED because the");
////        System.out.println("managed set getDesiredFirmwareVersion() method is obsolete");
////        System.out.println("**********************************************************");
////        
////        // get the ManagedSet for this Device
////        ManagedSet managedSet = null;
////        try {
////            managedSet = device.getManagedSet();
////        } catch (DeletedException e) {
////            // device was deleted. Next.
////            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
////                    "deleted: " + e); //$NON-NLS-1$
////            macroProgressContainer.setError(e);
////            this.activeMethodName = oldMethodName;
////            return;
////        }
////        
////        if (managedSet == null) {
////            // we don't care if an unmanaged device changes its firmware, but
////            // we did already update Device.actualFirmwareVersion
////            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
////                    "device is not in a ManagedSet"); //$NON-NLS-1$
////            macroProgressContainer.setComplete();
////            this.activeMethodName = oldMethodName;
////            return;
////        }
////            
////        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
////                "device is in " + managedSet); //$NON-NLS-1$
////        managedSet.lockWait();
////        try {
////            
////            FirmwareVersion desiredVersion = managedSet.getDesiredFirmwareVersion();
////            if (desiredVersion != null) {
////                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
////                        "Requesting sync of managed set."); //$NON-NLS-1$
////                /*
////                 * Because a firmware-change notification is not a notification
////                 * that the new firmware has already taken effect, but instead
////                 * firmware has been installed but not yet rebooted, and because
////                 * the new firmware level won't be visible until after reboot
////                 * (i.e., 2+ minutes), it is possible that the syncManagedSet
////                 * operation below won't see the new firmware because it hasn't
////                 * finished rebooting yet (especially if we start processing
////                 * this notification immediately after it is received). The
////                 * DeviceMetaInfo in syncManagedSet may report the old firmware
////                 * level. In fact, this is probably likely. When syncManagedSet
////                 * probes the device which notified us of a firmware update, it
////                 * may tell us it is running the old firmware because it hasn't
////                 * finished rebooting. So the syncManagedSet below possibly
////                 * won't detect/deploy the new firmware. Even if we did a
////                 * sleep() here to wait ample time for the new firmware to get
////                 * booted after we receive the notification, we would have the
////                 * managedSet locked for a long time (2+ minutes), and there is
////                 * some variability to how long it takes the device to reboot
////                 * (see FirmwareVersion.modifyDevice), so all in all this could
////                 * get quite complicated. So I'd suggest that the right thing to
////                 * do here is to let the HeartbeatTask detect the changed
////                 * firmware and execute a syncManagedSet there. The
////                 * HeartbeatTask is better equipped to retry while the device is
////                 * down, and not try if we know the device is rebooting a
////                 * firmware upgrade. So if you see this section of code fail to
////                 * detect a new firmware, that is OK, the HeartbeatTask should
////                 * detect it later. If HeartbeatTask fails to detect the new
////                 * firmware, then we are in trouble.
////                 */
////                ProgressContainer progressContainer = new ProgressContainer(notification);
////                macroProgressContainer.addNested(progressContainer);
////                progressContainer.setComplete();
////            } else {
////                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
////                        "desired FirmwareLevel for ManagedSet " + managedSet.getDisplayName() + //$NON-NLS-1$
////                        " is null, so skipping this operation."); //$NON-NLS-1$
////                ProgressContainer progressContainer = new ProgressContainer(notification);
////                macroProgressContainer.addNested(progressContainer);
////                progressContainer.setTotalSteps(1);
////                String newLevel = device.retrieveActualFirmwareLevel(progressContainer);
////                if ((newLevel != null) && (newLevel.length() > 0)) {
////                    device.setActualFirmwareLevel(newLevel);
////                }
////                progressContainer.setComplete();
////            }
////            
////            manager.save(Manager.SAVE_UNFORCED);
////
////            macroProgressContainer.setComplete();
////
////        } catch (DeletedException e) {
////            // I give up. Skip.
////            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
////                    "deleted: " + e); //$NON-NLS-1$
////            macroProgressContainer.setError(e);
////        } catch (DatastoreException e) {
////            // I give up. Skip.
////            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
////                    Messages.getString("DataStoreException"), e); //$NON-NLS-1$
////            macroProgressContainer.setError(e);
////        } catch (AMPException e) {
////            // let the heartbeat daemon try again via the ManagementStatus ERROR
////            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
////                    Messages.getString("DeviceCommProb"), e); //$NON-NLS-1$
////            macroProgressContainer.setError(e);
////        } finally {
////            managedSet.unlock();
////            this.activeMethodName = oldMethodName;
////        }
//        logger.exiting(CLASS_NAME, METHOD_NAME);
//    }
    
    private void processOpStateChange(Notification notification) {
        final String METHOD_NAME = "processOpStateChange"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // this is just a display attribute, so don't worry about locking the ManagedSet
        
        // set up the MacroProgressContainer
        Manager manager = Manager.internalGetInstance();
        ProgressContainer progressContainer = new ProgressContainer(notification);
        progressContainer.setTotalSteps(1);
        MacroProgressContainer macroProgressContainer = new MacroProgressContainer(notification);
        macroProgressContainer.addNested(progressContainer);
        manager.addNotificationProgress(macroProgressContainer);
        
        try {
            progressContainer.incrementCurrentStep(1, "wamt.clientAPI.QueueProcessor.opnStateChange_txt"); //$NON-NLS-1$
            
            // get the Domain name
            String domainName = notification.getObjectName();
            
            // get the Device object
            String serialNumber = notification.getDeviceSerialNumber();
            Device device = manager.getDeviceBySerialNumber(serialNumber);
            if (device == null) {
                // device no longer exists
                String message = Messages.getString("wamt.clientAPI.QueueProcessor.devRefNull"); //$NON-NLS-1$
                Exception e = new NotExistException(message, "wamt.clientAPI.QueueProcessor.devRefNull");
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message); 
                progressContainer.setError(e);
                return;
            }
            
            // get the ManagedSet for this Device
            ManagedSet managedSet = device.getManagedSet();
            
            // ignore if Device is not in a ManagedSet
            if (managedSet == null) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "device is not in a ManagedSet, don't care about OpState change"); //$NON-NLS-1$
                progressContainer.setComplete(); 
                macroProgressContainer.setComplete();
                return;
            }
            
            // get the Domain object, since it is in a ManagedSet
            Domain domain = device.getManagedDomain(domainName);
            
            // ignore if Domain is not in ManagedSet
            if (domain == null) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "domain " + domainName +  //$NON-NLS-1$
                        " is not managed, don't care about OpState change"); //$NON-NLS-1$
                progressContainer.setComplete();
                macroProgressContainer.setComplete();
                return;
            }
            
            /* get the new op state
            OperationStatus.Enumerated newEnum = null;
            if (notification.isOpStateChangeUp()) {
                newEnum = OperationStatus.Enumerated.UP;
            } else {
                newEnum = OperationStatus.Enumerated.DOWN;
            }            
            // set it in the Device
            device.setOperationStatusOfDomain(domain, newEnum); */
            
			domain.refresh();
            
            progressContainer.setComplete();
            macroProgressContainer.setComplete();
            
        } catch (DeletedException e) {
            // if the ManagedSet, Domain, or Device gets deleted, skip this. Next.
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "deleted: " + e); //$NON-NLS-1$
            progressContainer.setError(e);
        } 
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    private void processTest(Notification notification) {
        final String METHOD_NAME = "processTest"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        // set up the MacroProgressContainer
        Manager manager = Manager.internalGetInstance();
        ProgressContainer progressContainer = new ProgressContainer(notification);
        progressContainer.setTotalSteps(1);
        MacroProgressContainer macroProgressContainer = new MacroProgressContainer(notification);
        macroProgressContainer.addNested(progressContainer);
        manager.addNotificationProgress(macroProgressContainer);
        
        progressContainer.incrementCurrentStep(1, "Processing test notification"); //$NON-NLS-1$
        progressContainer.setComplete();
        macroProgressContainer.setComplete();
            
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Process Notification of domain config saved from Device MsgID = 0x81000040
     * This method will also detect any domain deleted on the device  
     */
    private void processSaveConfigOfDomain(Notification notification) {
        final String METHOD_NAME = "processSaveConfigOfDomain"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        String oldMethodName = this.activeMethodName;
        this.activeMethodName = METHOD_NAME + ": " + notification; //$NON-NLS-1$

        Manager manager = Manager.internalGetInstance();
       
        // get the Device object
        String serialNumber = notification.getDeviceSerialNumber();
        Device device = null;
        try {
            device = manager.getDeviceBySerialNumber(serialNumber);
        } catch (DeletedException e) {
            // either the Manager or the Device were deleted. Next.
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "deleted: " + e); //$NON-NLS-1$
            this.activeMethodName = oldMethodName;
            return;
        }
        // skip it if the Device no longer exists
        if (device == null) {
            String message = Messages.getString("wamt.clientAPI.QueueProcessor.devRefNull"); //$NON-NLS-1$            
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, message); 
            this.activeMethodName = oldMethodName;
            return;
        }
        
      
        String domainName = notification.getObjectName();
        
        /*
         * If the default domain changed, one other thing to look out for is the
         * deletion of a managed domain (other than "default". The deletion of
         * a domain triggers an event in the default domain, even though it
         * wasn't the default domain that changed. So check to make sure all the
         * managed domains are present. If any are missing, get the desired
         * version and sync it.
         * 
         * We will also see the same kind of event if the user creates a new
         * domain on the device. Even if it is an unmanaged domain, we would
         * still like it to appear immediately in Device.allDomainNames 
         * 
         * Check if the managed domain is flagged for SynchronizationMode.AUTO. If any change  is
         * detected in the deployed configuration on the device and the stored configuration, then
         * synchNow() deploys the stored configuration. If a LockBusyException is thrown, set the sychFailed 
         * flag so that the DomainSynchronization Daemon will retry it later.         
         */
        if (domainName.equals(Domain.DEFAULT_DOMAIN_NAME)) {
        
            device.lockWait();
            MacroProgressContainer macroProgressContainer = new MacroProgressContainer(notification);
            try {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "checking if any managed domains were deleted"); //$NON-NLS-1$
                Domain[] managedDomains = device.getManagedDomains();
                ProgressContainer progressContainer = new ProgressContainer(notification);
                // assume just the check
                progressContainer.setTotalSteps(2);
                macroProgressContainer.addNested(progressContainer);
                for (int i=0; i<managedDomains.length; i++) {
                    progressContainer.incrementCurrentStep(1,"wamt.clientAPI.QueueProcessor.chkAllManDomains_txt"); //$NON-NLS-1$
                    boolean present = managedDomains[i].isPresentOn(device);
                    if (!present) {
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                                "looks like " + managedDomains[i] +  //$NON-NLS-1$
                                " is missing from " + device +  //$NON-NLS-1$
                                ", will attempt to restore"); //$NON-NLS-1$
                        // Call domain.sysnc(compareMode = true) which will check for difference in domain and 
                        // force synchronization on the domain, if it is marked for DomainSynchronizationMode.AUTO.
//                        try {
							//managedDomains[i].synch(true);
                    	    DomainSynchronizationTask result = new DomainSynchronizationTask(managedDomains[i], true);  
                    	    result.execute();                        	
/*						} catch (LockBusyException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();		
							managedDomains[i].setSyncFailed(true);
			                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
			                "LockBusyException thrown. SyncNow() will be attempted by DomainSychronization Timer later."); //$NON-NLS-1$							
						} catch (NotManagedException e) {
							//e.printStackTrace();		
			                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, 
        			                "NotManagedException thrown."); //$NON-NLS-1$							
						}*/
				        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
				        		"Call Domain.synch(true) since domain has been deleted " + managedDomains[i]);                         
                    }
                }
                
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "refreshing list of all domains on device"); //$NON-NLS-1$
                progressContainer.incrementCurrentStep(1,"wamt.clientAPI.QueueProcessor.refDomainList_txt"); //$NON-NLS-1$
                device.refreshDomainList();
                progressContainer.setComplete();
                
            } catch (AMPException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
                macroProgressContainer.setError(e);
            } catch (DeletedException e) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                        Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
                macroProgressContainer.setError(e);            
 				
			} finally {
              device.unlock();  
            }
        }
        
        Domain domain = null;
        String hostname = null;  
        try {
            domain = device.getManagedDomain(domainName);
            hostname = device.getHostname();        	
        } catch (DeletedException e) {
            // skip it
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "Domain was deleted"); //$NON-NLS-1$
            this.activeMethodName = oldMethodName;
            return;
        }

        /*
         * Determine if any managed domains have this device/domain as source and need sync.
         * Walk thru all managed devices containing a managed domain with the same name.
         * Note that saved domain may not be managed and the domain object may be null at this point.
         * Any exceptions we encounter should be ignored.
         * 
         */        
		String urlString = URLSource.SCHEME_DEVICE+"://"+hostname+"/"+domainName; // URLSource string
		for (ManagedSet loopset : manager.getManagedSets())
		{
			try {
				for (Device loopdevice : loopset.getDeviceMembers())
				{
					// skip the device that triggered the notification
					if (loopdevice.getHostname().equals(hostname)) continue;
        		
					// lock the device
					loopdevice.lockWait();

					try {
						// find a managed domain with the source as this device
						Domain loopdomain = loopdevice.getManagedDomain(domainName);
						if (loopdomain != null && loopdomain.isSynchEnabled())
						{
							if (loopdomain.getSourceConfiguration().getURL().equalsIgnoreCase(urlString))
							{
								// Mark this domain as out of sync since the source has changed.  
								loopdomain.setOutOfSynch(true);
								logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                	        		"Source domain was modified for Domain " + loopdomain.getAbsoluteDisplayName());                
							}
						}
					} catch (AlreadyExistsInRepositoryException e) {
						// ignore and skip to the next device
					} catch (DatastoreException e) {
						// ignore and skip to the next device
					}
					
					// unlock the device
					loopdevice.unlock();
				} // device loop        		
			}catch (DeletedException e){
				// ignore and skip to the next managed set
			}
		} // managed set loop

        // skip this device sync if domain is not managed
        if (domain == null) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    "domain is not a managed domain, ignore: " + domainName); //$NON-NLS-1$
            this.activeMethodName = oldMethodName;
            return;
        }

        // now that we know the ManagedSet, lock it
        device.lockWait(); 
        try {
//            try {
				//domain.synch(true);
        	    DomainSynchronizationTask result = new DomainSynchronizationTask(domain, true);  
        	    result.execute();
/*			} catch (LockBusyException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();		
				domain.setSyncFailed(true);
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                "LockBusyException thrown. SyncNow() will be attempted by DomainSychronization Timer later."); //$NON-NLS-1$							
			}*/
	        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
	        		"Managed domain modified: Create DomainSynchronizationTask for Domain " + domain.getAbsoluteDisplayName());                            
        } catch (Exception e) {
			domain.setSyncFailed(true);//will be attempted by DomainSychronization Timer later."); //$NON-NLS-1$	
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("UnexpectedException"), e); //$NON-NLS-1$
        } finally {
            device.unlock();
            this.activeMethodName = oldMethodName;
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

}
