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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * An implementation of a re-entrant lock that can be either blocking (like
 * <code>synchronized</code>) or non-blocking (fail-fast). The purpose of this 
 * implementation is to provide a lock to write methods with the ability to test 
 * if another thread already owns the lock. That is not available with the
 * <code>synchronized</code> keyword.
 * <p>
 * All updates (writes) to anything in an object that uses <code>Lock</code> must 
 * invoke <code>lockNoWait()</code>, whether done by the clientAPI or
 * internally via a background thread. Each object that uses <code>Lock</code> has 
 * its own instance of a <code>Lock</code> object, these instances of 
 * <code>Lock</code> operate independently of each other (e.g., calling lockNoWait()
 * on one device does not prevent using (locking) another device on another thread.
 * <p>
 * The intention is for callers of the clientAPI to invoke only short-running
 * tasks. If the lock exists when the clientAPI is invoked (for example, because
 * a separate thread already has it locked for a short running or long running
 * task), the clientAPI method which does not own the lock will fail with an
 * {@link LockBusyException}.
 * <p>
 * Anything long-running should be queued for execution by a background thread
 * (see {@link ProgressContainer}). If the background execution queue is already
 * full (which is not the default behavior), then a FullException will be
 * thrown. (For more information about this circumstance, see
 * {@link FullException}.) Instead of failing fast, the background execution
 * thread should block until the lock is available, since there is no user
 * interface penalty when a background thread blocks for a long period.
 * <p>
 * <strong>Locking design: </strong>
 * <p>
 * The following may seem unnecessarily complicated (i.e., why not just use
 * <code>synchronized</code> ?), but there are some unusual requirements:
 * <ul>
 * <li>Act as a wrapper for a persistent datastore. When items in the datastore
 * are deleted, the Object wrapper should no longer be used. Java does not have
 * a strict destructor like C++ does. 
 * <li>Trying to acquire a busy lock should fail-fast instead of block.
 * <li>Public methods must be short-running.
 * </ul>
 * Best practices for using Lock:
 * <p>
 * The public clientAPI methods must not be long-running. If there is a task
 * that requires a long run, then it should be queued for another thread to
 * execute. This is because the UI (consumer of the clientAPI) must be
 * responsive and not block for indeterminate periods. The UI could be any
 * client (i.e., servlet, or a rich client). 
 * <p>
 * Locks are fail-fast. If a thread tries to acquire a lock that is already held
 * by another thread, the thread trying to acquire the busy lock will fail
 * without blocking or waiting. This means that callers should expect to either
 * acquire the lock quickly, or receive an exception quickly. If a caller
 * receives an exception while trying to acquire a lock, it may (at the caller's
 * discretion) retry the lock acquisition at a later time or abort the entire
 * operation. [Note: requests for locks via the clientAPI should fail-fast.
 * However, daemons can wait for locks to be acquired, so this class provides a
 * <code>lockWait()</code> method that will block until the lock is available.
 * So this class needs to support both fail-fast acquisition and blocking
 * acquisition.]
 * <p>
 * If the thread is unable to obtain all the locks it requires, it should fail
 * and release the locks it was able to obtain up until the failure point.
 * <p>
 * Objects that implement the <code>Persistable</code> interface can be
 * persisted to a datastore using the dataAPI. Those objects can be deleted from
 * the datastore. If you try to use an object that has been deleted from the
 * datastore and you still have a reference to that object, it will throw a
 * DeletedException. This is necessary because the datastore has the notion of a
 * delete or destructor, but Java objects do not have the notion of a destructor
 * that can be immediately invoked such as in C++. It is not considered
 * acceptable to access a persistable Java object that was deleted from the
 * datastore. If this occurs, an exception will be thrown. Because the clientAPI
 * has a lot of persistence, a lot of the clientAPI methods will throw a
 * DeletedException.
 * <p>
 * All the Lock methods are <code>synchronized</code>, meaning that access is
 * serialized around the lock, not serializing around the work areas directly.
 * <p>
 * 
 * @version SCM ID: $Id: Lock.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 */
//* Created on Sep 11, 2006
/* BEGIN INTERNAL COMMENTS
 * An implementation of a re-entrant lock that can be either blocking (like
 * <code>synchronized</code>) or non-blocking (fail-fast). Normally a class
 * in java.util.concurrent.locks would be used, but we are trying to limit
 * ourselves to J2SE 1.4.2. The purpose of this implementation is to provide a
 * lock to write methods with the ability to test if another
 * thread already owns the lock. That is not available with the
 * <code>synchronized</code> keyword.
 * <p>
 * All updates (writes) to anything in the ManagedSet must invoke
 * <code>ManagedSet.lockNoWait()</code>, whether done by the clientAPI or
 * internally via a background thread. However, reads of anything in the
 * ManagedSet may be done concurrently, even while a ManagedSet is locked and a
 * write is in process. Locks are available for each ManagedSet independently.
 * <p>
 * The intention is for callers of the clientAPI to invoke only short-running
 * tasks. If the lock exists when the clientAPI is invoked (for example, because
 * a separate thread already has it locked for a short running or long running
 * task), the clientAPI method which does not own the lock will fail with an
 * {@link LockBusyException}.
 * <p>
 * Anything long-running should be queued for execution by a background thread
 * (see {@link BackgroundTask}). If the background execution queue is already
 * full (which is not the default behavior), then a FullException will be
 * thrown. (For more information about this circumstance, see
 * {@link FullException}.) Instead of failing fast, the background execution
 * thread should block until the lock is available, since there is no user
 * interface penalty when a background thread blocks for a long period.
 * <p>
 * <strong>Locking design: </strong>
 * <p>
 * The following may seem unnecessarily complicated (i.e., why not just use
 * <code>synchronized</code> ?), but there are some unusual requirements:
 * <ul>
 * <li>Act as a wrapper for a persistent datastore. When items in the datastore
 * are deleted, the Object wrapper should no longer be used. Java does not have
 * a strict destructor like C++ does. But we also don't want to encounter
 * unchecked exceptions like NullPointerException.
 * <li>Can't use the nice concurrency utilities in J2SE 5. We are stuck with
 * J2SE 1.4.2.
 * <li>Trying to acquire a busy lock should fail-fast instead of block.
 * <li>Public methods must be short-running.
 * </ul>
 * Now I'll explain why things are.
 * <p>
 * The locking implementation must stay within J2SE 1.4.2, we are not able to
 * use the concurrency utilities in J2SE 5. This is for two reasons, the first
 * being deployment in WAS. WAS 6.1 supports J2SE 5, but earlier versions of WAS
 * support only J2SE 1.4.2. Because objects must be serialized when invoked
 * remotely as admin tasks (JMX), they must be serialized to/from a form that is
 * bytecode compatible with the local JRE. If an admin task is being invoked
 * remotely on a pre-6.1 WAS server then the objects must be bytecode compatible
 * with J2SE 1.4.2. The second reason is that it consumers of the clientAPI such
 * as Tivoli might not support a J2SE 5 JRE. At the current time, J2SE 1.4.2 is
 * the common Java stack.
 * <p>
 * The public clientAPI methods must not be long-running. If there is a task
 * that requires a long run, then it should be queued for another thread to
 * execute. This is because the UI (consumer of the clientAPI) must be
 * responsive and not block for indeterminate periods. The UI could be an ISC
 * client (i.e., servlet) or a rich client (i.e., Tivoli). Note: the only
 * exceptions to this rule are the Device.[get|set]Unmanaged* methods.
 * <p>
 * If a long-running method is already queued, we debated on whether we should
 * let another long running method be queued to the same work area (ManagedSet,
 * Firmwares/FirmwareVersions, or unmanged Devices) or not. Basically the
 * question is should the queue size be limited to 1 or not. The driver for this
 * question was to let the current long-running task finish changing the system
 * state, so that decisions can be made by the caller based on data that does
 * not have changes pending. However, we felt that the side effect of having a
 * daemon prevent all clientAPI tasks from being queued was too harsh, so we
 * decided to let the default queue size be unbounded. For users who want to be
 * more conservative and limit the queue size to one, the Manager will support
 * that by specifying in its constructor options
 * {@link Manager#OPTION_TASK_QUEUE_SIZE}<code> = new Integer(1)</code>
 * <p>
 * Locks are fail-fast. If a thread tries to acquire a lock that is already held
 * by another thread, the thread trying to acquire the busy lock will fail
 * without blocking or waiting. This means that callers should expect to either
 * acquire the lock quickly, or receive an exception quickly. If a caller
 * receives an exception while trying to acquire a lock, it may (at the caller's
 * discretion) retry the lock acquisition at a later time or abort the entire
 * operation. [Note: requests for locks via the clientAPI should fail-fast.
 * However, daemons can wait for locks to be acquired, so this class provides a
 * <code>lockWait()</code> method that will block until the lock is available.
 * So this class needs to support both fail-fast acquisition and blocking
 * acquisition.]
 * <p>
 * Locks are write locks. This is to prevent multiple writers from updating a
 * single object simultaneously. A reader of the object does not need to acquire
 * a lock to read the object. This means that it is possible that the reader may
 * be getting stale data from the object. This is considered acceptable. If a
 * reader is performing a write operation on another object based on the
 * readable contents of this object, then the reader should acquire a write lock
 * on the object it is reading.
 * <p>
 * Individual low-level objects are not locked. Locks are performed on an entire
 * ManagedSet. Different ManagedSets may be locked and unlocked independently of
 * each other. There are other items outside the scope of a ManagedSet, namely
 * unmanaged Devices and Firmware/FirmwareVersion. Thus there will an additional
 * single lock for all the unmanaged Devices, and an additional single lock to
 * cover all the Firmware and FirmwareVersion objects. So the total number of
 * locks will be 2 + numberOfManagedSets.
 * <p>
 * To prevent deadlock situations, locks should be obtained in the following
 * order:
 * <ol>
 * <li>unmanaged Devices
 * <li>Firmware/FirmwareVersion
 * <li>ManagedSet
 * <li>Device
 * </ol>
 * If the thread is unable to obtain all the locks it requires, it should fail
 * and release the locks it was able to obtain up until the failure point.
 * <p>
 * Simple setter methods on objects should not require a lock. This is considered
 * acceptable. Because there is a permanently running thread to verify that the
 * devices are in the desired state, the desired state will be reached
 * eventually.
 * <p>
 * Objects that implement the <code>Persistable</code> interface can be
 * persisted to a datastore using the dataAPI. Those objects can be deleted from
 * the datastore. If you try to use an object that has been deleted from the
 * datastore and you still have a reference to that object, it will throw a
 * DeletedException. This is necessary because the datastore has the notion of a
 * delete or destructor, but Java objects do not have the notion of a destructor
 * that can be immediately invoked such as in C++. It is not considered
 * acceptable to access a persistable Java object that was deleted from the
 * datastore. If this occurs, an exception will be thrown. Because the clientAPI
 * has a lot of persistence, a lot of the clientAPI methods will throw a
 * DeletedException.
 * <p>
 * All the Lock methods are <code>synchronized</code>, meaning that access is
 * serialized around the lock, not serializing around the work areas directly.
 * <p>
 * 
 * @version SCM ID: $Id: Lock.java,v 1.4 2010/09/02 16:24:52 wjong Exp $
 * END INTERNAL COMMENTS 
 */

public class Lock {
    // don't need to mark these members as volatile because "synchronized" 
    // is used to access them
    private String name = null;
    private Thread owner = null; 
    private int count = 0;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = Lock.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * Create a mutex lock. It is initially in the unlocked state. You must use
     * one of the lock acquisition methods ({@link #lockWait()},
     * {@link #lockNoWait()}) to trigger the lock to prevent other threads from
     * acquiring it.
     * 
     * @param name a human-readable name for the lock. This name will appear in
     *        log and exception messages and in the method {@link #toString()}.
     */
    public Lock(String name) {
        this.name = name;
    }

    /**
     * Check if the lock is available for acquisition by this thread. This
     * method should never block.
     * 
     * @return true if the lock is available for acquisition and is not
     *         currently held by another thread, false otherwise. Since this
     *         lock is reentrant, it will return true if the current thread
     *         already holds this lock.
     */
    synchronized public boolean isAvailable() {
        final String METHOD_NAME = "isAvailable (" + this + " on thread " + Thread.currentThread().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        logger.entering(CLASS_NAME, METHOD_NAME);
        boolean result = false;
        if (this.owner == null) {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "lock has no owner"); //$NON-NLS-1$
            result = true;
        } else {
            Thread thisThread = Thread.currentThread();
            if (this.owner == thisThread) {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "lock is owned by this thread: " + //$NON-NLS-1$
                        Thread.currentThread().getName());
                result = true;
            } else {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                        "lock is owned by another thread: " + this.owner.getName() + //$NON-NLS-1$
                        ": current thread: " + Thread.currentThread().getName()); //$NON-NLS-1$
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return(result);
    }

    /**
     * Acquire the lock, even if it requires blocking until another thread
     * releases this lock. Because blocking may occur, this method may take an
     * indeterminate amount of time to run. Since the lock is reentrant, this
     * method will return immediately if the current thread already holds the
     * lock. Usually only background tasks will attempt to acquire locks using
     * this blocking method, since background tasks aren't sensitive to being
     * long-running.
     */
    synchronized public void lockWait() {
        final String METHOD_NAME = "lockWait (" + this + " on thread " + Thread.currentThread().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        logger.entering(CLASS_NAME, METHOD_NAME);
        boolean successful = false;
        while (!successful) {
            try {
                while (!this.isAvailable()) {
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                            Thread.currentThread().getName() + " waiting" + //$NON-NLS-1$
                            " for lock " + this); //$NON-NLS-1$
                    this.wait();
                }
                this.lockNoWait();
                successful = true;
            } catch (InterruptedException e) {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        Thread.currentThread().getName() + " interrupted: " + e); //$NON-NLS-1$
                Thread.currentThread().interrupt();
            } catch (LockBusyException e) {
                // just wait again until it is available
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    /**
     * Try to acquire the lock, but fail fast if the lock is held by another
     * thread. Since this lock is reentrant, this method will return immediately
     * successfully if the current thread already holds the lock. If the lock is
     * held by another thread, this method will throw a
     * <code>LockBusyException</code> immediately. This method does not block,
     * so it should return (successfully or with an exception) quickly. Usually
     * most clientAPI invocations from a user interface will use this method,
     * since the user interface is sensitive to long-running methods on its
     * threads.
     * 
     * @throws LockBusyException the lock is held by another thread. Since this
     *         method is fail-fast instead of blocking, this indicates failure
     *         to obtain the lock. You can try to acquire the lock again at any
     *         time using this method or {@link #lockWait()}.
     */
    synchronized public void lockNoWait() throws LockBusyException {
        final String METHOD_NAME = "lockNoWait (" + this + " on thread " + Thread.currentThread().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        logger.entering(CLASS_NAME, METHOD_NAME); 
        if (this.isAvailable()) {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "lock is available"); //$NON-NLS-1$
            Thread thisThread = Thread.currentThread();
            if (this.owner != thisThread) {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "setting lock owner");  //$NON-NLS-1$
                this.owner = thisThread;
            } else {
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "already own lock");  //$NON-NLS-1$
            }
            this.count++;
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "new reentrant count: " + this.count);  //$NON-NLS-1$
        } else {
        	Object[] args = new Object[] {Thread.currentThread().getName(), this.toString()};
            String message = Messages.getString("wamt.clientAPI.Lock.lockBusy", args);   //$NON-NLS-1$ 
            LockBusyException e = new LockBusyException(message,"wamt.clientAPI.Lock.lockBusy", args);
            /*
             * since the QueueProcessor is usually the one who has the lock when
             * we get into this contention situation, it might be helpful for
             * debug reasons to note what the QueueProcessor is currently doing.
             */
            Manager manager = Manager.internalGetInstance();
            String queueProcessorActiveMethodName = manager.getQueueProcessorActiveMethodName();
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "Just created a LockBusyException and am about to throw it. FYI: QueueProcessor active method: " + queueProcessorActiveMethodName);
            throw(e);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Release the current lock, or decrement the reentrancy counter. If this
     * lock has been acquired by the current thread only once (meaning that the
     * reentrancy counter equals 1), then release this lock so that it may be
     * acquired by any thread. If the reentrancy count is greater than 1, then
     * decrement the reentrancy counter by 1 and maintain ownership of this
     * lock. This method should never block.
     */
    synchronized public void unlock() {
        final String METHOD_NAME = "unlock (" + this + " on thread " + Thread.currentThread().getName() + ")";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // do nothing if not locked
        if (this.count == 0) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "reentrant count is 0 before release: " + this);  //$NON-NLS-1$
            return;
        }
        // check that you own it
        Thread thisThread = Thread.currentThread();
        if (this.owner != thisThread) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                    "incorrect ownernship: should be " + this.owner.getName() +  //$NON-NLS-1$
                    " but you are " + thisThread.getName());  //$NON-NLS-1$
            return;
        }
        if (this.count > 0) {
            this.count--;
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, 
                    "decrementing reentrant count, new value: " + this.count);  //$NON-NLS-1$
        }
        if (this.count == 0) {
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                    "ownership released, notifying all");  //$NON-NLS-1$
            this.owner = null;
            this.notifyAll();
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */
    public String toString() {
        return("Lock[name=" + this.name + ", owner=" + owner +    //$NON-NLS-1$ //$NON-NLS-2$
                ", reentrant_count=" + this.count + "]");   //$NON-NLS-1$ //$NON-NLS-2$
    }
}
