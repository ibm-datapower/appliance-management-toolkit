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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A ProgressContainer is the vehicle that the manager uses to inform the caller
 * (likely a user presentation) of the progress on a long-running method. Think
 * of this as a rendezvous object. Methods are long-running probably because a
 * device has to be communicated with via AMP. The caller will get notified via
 * this object that there was an update in the progression and can read the new
 * progress point from this object. The caller should block using
 * {@link #waitForUpdate()}, and then retrieve the new data via the gettr
 * methods. Alternatively the caller could poll via {@link #hasUpdate}, and
 * then when it returns true call the gettr methods. Or if you do not need to be
 * informed of interim progress, you could use the method {@link #waitForEnd()}.
 * If there is any return value from the long-running method, it will be
 * available for retrieval from this object. Similarly, if there is any
 * Exception from the long-running method, it will be available for retrieval
 * from this object.
 * <p>
 * Typical usage looks like this:
 * <p>
 * <blockquote>
 * <pre>
 * ProgressContainer progressContainer = longRunningCommandInvocation();
 * // the invocation returns quickly but the long-running work is performed on another thread
 * 
 * while (!progressContainer.isComplete &amp;&amp; !progressContainer.hasError()) {
 *     System.out.println(&quot;Executing step &quot; + progressContainer.getCurrentStep()
 *             + &quot; of &quot; + progressContainer.getTotalSteps() + &quot;: &quot;
 *             + progressContainer.getCurrentStepDescription());
 *     progressContainer.waitForUpdate();
 * }
 * if (progressContainer.hasError()) {
 *     System.out.println(&quot;Task did not complete because of error: &quot;
 *             + progressContainer.getError().toString());
 * } else {
 *     System.out.println(&quot;Task complete.&quot;);
 *     if (progressContainer.getResult() != null) {
 *         System.out.println(&quot;Result: &quot;
 *                 + progressContainer.getResult().toString());
 *     }
 * }
 * </pre>
 * </blockquote>
 * <p>
 * A convenience method, {@link #blockAndTrace(Level)}, is provided so that the
 * above can be accomplished with much less effort:
 * <blockquote>
 * <pre>
 * ProgressContainer progressContainer = longRunningCommandInvocation();
 * progressContainer.blockAndTrace(Level.FINER);
 * </pre>
 * </blockquote>
 * You may also see:
 * <pre>
 * <blockquote>
 * ProgressContainer progressContainer = longRunningCommandInvocation();
 * progressContainer.waitForEnd();
 * </blockquote>
 * </pre>
 * Another common use case is to simply collect the ProgressContainers and
 * periodically check them all for updates, get the updated data, and then
 * display or act on it.
 * <p> 
 * Also available is a <code>correlator</code>, which is a value which can be
 * optionally set and retrieved by the caller to hold a value that may help
 * correlate this background task to other items that the caller cares about.
 * The queue processor does not look at this correlator, it is for use only by
 * the caller. The default value of the correlator is null.
 * <p>
 * This class is NLS-enabled using message keys and arguments.
 * <p>
 * <cite>Internal comment:</cite> The thread that feeds updates to this object
 * should always close this object via one of these methods:
 * <code>setComplete()</code>,<code>setComplete(Object)</code>, or
 * <code>setError(Exception)</code>. Otherwise there will be a circular
 * reference between the task and the ProgressContainer that will prevent
 * garbage collection of both this ProgressContainer and the task that it
 * references.
 * <p>
 * <cite>Internal comment:</cite> This class also supports hiding of the
 * final error/completion until the feeding thread wishes to call
 * <code>commit()</code>. This is helpful when dealing with locks where
 * the lock is released in a <code>finally</code> clause and the
 * <code>commit()</code> invocation can also be placed in that
 * <code>finally</code> clause after the lock release. Note that
 * updates to the current step cannot be hidden, only the final
 * result/error. For more information, see the javadoc for
 * <code>commit()</code> (package-access method).
 * <p>
 * <cite>Internal comment:</cite> Generally only the Task classes will
 * load ProgressContainers with completion and/or error data. So for that
 * reason you won't see ProgressContainers being passed into lower-level
 * methods. For example, if a lower-level method encounters an error
 * condition, it will throw an exception all the way back up to the Task class
 * instead of calling setError(e) itself. In places where you do see 
 * ProgressContainers being passed into lower-level methods, it is only
 * for the lower-level method to increment the current step with information
 * available only to the lower-level method.
 * <p>
 * 
 * @see MacroProgressContainer
 * @version SCM ID: $Id: ProgressContainer.java,v 1.6 2011/03/18 16:24:30 lsivakumxci Exp $
 */
public class ProgressContainer {
    // these members don't need to be marked volatile because synchronized methods are used to access them
    private int totalSteps = 0;
    private int currentStep = 0;
    private String currentStepDescriptionKey = null;
    private Object[] currentStepDescriptionArgs = null;
    private Date currentStepTimestamp = null;
    private boolean isComplete = false;
    private boolean uncommittedIsComplete = false;
    private Object result = null;
    private Object uncommittedResult = null;
    private Exception error = null;
    private Exception uncommittedError = null;
    private boolean hasUpdate = false;
    private Object correlator = null;
    private Task task = null;
    private final Date creationDate;
    private Vector<String> eventList = new Vector<String>();
    
    private static final String NOT_STARTED_MESSAGE_KEY = "wamt.clientAPI.ProgressContainer.notStarted"; //$NON-NLS-1$
    static final boolean IMMEDIATELY_COMMIT = true;
    static final boolean DEFER_COMMIT = false;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = ProgressContainer.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    // methods for use by the manager core only (producer of updates) 
    
    /**
     * Create a new ProgressContainer. This is not public access because
     * ProgressContainers are created automatically when BackgroundTasks are
     * created, so users of the public API should not need to call this
     * constructor directly, the object is created for them.
     */
    ProgressContainer(Task task) {
        this.totalSteps = 0;
        this.currentStep = 0;
        this.currentStepDescriptionKey = NOT_STARTED_MESSAGE_KEY;
        this.currentStepDescriptionArgs = null;
        this.currentStepTimestamp = new Date();
        this.isComplete = false;
        this.uncommittedIsComplete = false;
        this.result = null;
        this.uncommittedResult = null;
        this.error = null;
        this.uncommittedError = null;
        this.hasUpdate = false;
        this.correlator = null;
        this.task = task;
        // just copy the value, no need to create a new instance
        this.creationDate = this.currentStepTimestamp;
    }
    
    synchronized void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
        this.hasUpdate = true;
        this.notifyAll();
    }
    
    synchronized void incrementTotalSteps(int additionalSteps) {
        this.totalSteps += additionalSteps;
        this.hasUpdate = true;
        this.notifyAll();
    }
    
    /**
     * The ProgressContainer can make data available to the consumer in two
     * ways: First, it can happen immediately via {@link #setComplete()},
     * {@link #setComplete(Object)}, or {@link #setError(Exception)}. Normally
     * that is how it would be done. The second way is by hiding the
     * error/completion until the producer is ready to release it via
     * {@link #commit()}. This would be useful if there is an object with a
     * mutex lock, and the producer's lock needs to be released before making
     * the data available and then the consumer expecting that it can lock the
     * same mutex (using the ProgressContainer to coordinate the
     * unlocking/locking). If the consumer is blocking on the ProgressContainer
     * before it attempts to acquire the mutex, then that avoids a race
     * condition between the producer's lock release and the consumer's lock
     * acquisition. If the producer wants to hide the data until calling
     * {@link #commit()}, then the producer should use
     * {@link #setUncommittedComplete()},
     * {@link #setUncomittedComplete(Object)}, and
     * {@link #setUncommittedError(Exception)}. When the producer invokes
     * {@link #commit()}, then the data is made visible to the consumer and the
     * blocked consumer threads are notified of an update. The consumer is not
     * able to determine if the data was sent immediately by the producer or if
     * it was hidden for a while and then released via a commit. Note that other
     * producer methods such as {@link #incrementCurrentStep(int, String)} do
     * not have a delayed commit capability, current step data is always
     * immediately available to the consumer. It is only the final state
     * (complete or error) that can be delayed from the consumer until
     * committed. If more than one of
     * <code>setUncommittedComplete/Complete(Object)/Error(Exception)</code>
     * is called before <code>commit</code>, then the last one wins.
     */
    synchronized void commit() {
        // if previously hiding completion or error, show it now
        final String METHOD_NAME = "commit()"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "About to commit, task: " + this.task); //$NON-NLS-1$
        if (this.isUncommitted()) {
            // copy in all the uncommitted values
            this.isComplete = this.uncommittedIsComplete;
            this.result = this.uncommittedResult;
            this.error = this.uncommittedError;
            // clear the holders of uncommitted data
            this.uncommittedIsComplete = false;
            this.uncommittedResult = null;
            this.uncommittedError = null;
        } else {
            // I'd like for it to be safe to call commit() if not isUncommitted.
            // so don't overwrite values we already have
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Invoked method without uncommitted data, ignoring commit: " + this.task); //$NON-NLS-1$ 
        }
        // update the status data
        this.currentStepTimestamp = new Date();
        this.hasUpdate = true;
        this.correctTotalStepEstimate(METHOD_NAME);
        // now we can notify other blocked threads
        this.notifyAll();
    }
    
    synchronized boolean isUncommitted() {
        boolean result = (this.uncommittedIsComplete || 
                (this.uncommittedError != null));
        return(result);
    }

    synchronized private void correctTotalStepEstimate(String methodName) {
        // handle bad estimates (over/under estimations).
        // log using the method that called us.
        // same action, but different log messages.
        if (this.isComplete) {
            // We want estimated == actual at the end, even if the original estimate was wrong.
            if (this.totalSteps != this.currentStep) {
                logger.logp(Level.FINEST, CLASS_NAME, methodName,
                                "warning: ProgressContainer completed on step " + this.currentStep + //$NON-NLS-1$ 
                                        " which differs from estimate of " + this.totalSteps + //$NON-NLS-1$ 
                                        ", making correction"); //$NON-NLS-1$
                this.totalSteps = this.currentStep;
            }
        } else if (this.error != null) {
            // ending with "on step 3 of 2" looks bad, fix it
            if (this.currentStep > this.totalSteps) {
                logger.logp(Level.FINEST, CLASS_NAME, methodName,
                                "warning: ProgressContainer ended with error on step " + this.currentStep + //$NON-NLS-1$ 
                                        " which is greater than estimate of " + this.totalSteps + //$NON-NLS-1$ 
                                        ", making correction"); //$NON-NLS-1$
                this.totalSteps = this.currentStep;
            }
        } else {
            // incomplete. Having "on step 3 of 2" looks bad, fix it
            if (this.currentStep > this.totalSteps) {
                logger.logp(Level.FINEST, CLASS_NAME, methodName,
                                "warning: ProgressContainer incremented to step " + this.currentStep + //$NON-NLS-1$ 
                                        " which is greater than estimate of " + this.totalSteps + //$NON-NLS-1$ 
                                        ", making correction"); //$NON-NLS-1$
                this.totalSteps = this.currentStep;
            }
        }
    }
    
    synchronized void setComplete() {
        final String METHOD_NAME = "setComplete(noResult)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "About to mark complete (w/o object), task: " + this.task); //$NON-NLS-1$
        this.isComplete = true;
        this.hasUpdate = true;
        this.currentStepTimestamp = new Date();
        this.correctTotalStepEstimate(METHOD_NAME);
        // purge any uncommitted data, just in case
        this.uncommittedIsComplete = false;
        this.uncommittedResult = null;
        this.uncommittedError = null;
        // notify anyone waiting for us
        this.notifyAll();
    }
    
    synchronized void setUncommittedComplete() {
        final String METHOD_NAME = "setUncommittedComplete(noResult)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "About to mark uncommitted complete (w/o object), task: " + this.task); //$NON-NLS-1$
        this.uncommittedIsComplete = true;
        // erase any previous uncommitted values
        this.uncommittedResult = null;
        this.uncommittedError = null;
        // do not notifyAll or hasUpdate or correctTotalStepEstimate, that will be done in commit()
    }

    synchronized void setComplete(Object result) {
        final String METHOD_NAME = "setComplete(Object)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "About to mark complete (with object: " + result +  //$NON-NLS-1$
                "), task: " + this.task); //$NON-NLS-1$
        this.result = result;
        this.isComplete = true;
        this.hasUpdate = true;
        this.currentStepTimestamp = new Date();
        this.correctTotalStepEstimate(METHOD_NAME);
        // purge any uncommitted data, just in case
        this.uncommittedIsComplete = false;
        this.uncommittedResult = null;
        this.uncommittedError = null;
        // notify anyone waiting for us
        this.notifyAll();
    }
    
    synchronized void setUncommittedComplete(Object result) {
        final String METHOD_NAME = "setUncommittedComplete(Object)"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "About to mark uncomitted complete (with object: " + result +  //$NON-NLS-1$
                "), task: " + this.task); //$NON-NLS-1$
        this.uncommittedIsComplete = true;
        this.uncommittedResult = result;
        // erase any previous uncommitted values
        this.uncommittedError = null;
        // do not notifyAll or hasUpdate or correctTotalStepEstimate, that will be done in commit()
    }
    
    synchronized void setError(Exception error) {
        final String METHOD_NAME = "setError"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "Setting error in ProgressContainer for task " + this.task, error);  //$NON-NLS-1$
        this.error = error;
        this.hasUpdate = true;
        this.currentStepTimestamp = new Date();
        this.correctTotalStepEstimate(METHOD_NAME);
        // purge any uncommitted data, just in case
        this.uncommittedIsComplete = false;
        this.uncommittedResult = null;
        this.uncommittedError = null;
        // notify anyone waiting for us
        this.notifyAll();
    }
    
    synchronized void setUncommittedError(Exception error) {
        final String METHOD_NAME = "setUncommittedError"; //$NON-NLS-1$
        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                "Setting error in ProgressContainer for task " + this.task, error);  //$NON-NLS-1$
        this.uncommittedError = error;
        // erase any previous uncommitted values
        this.uncommittedIsComplete = false;
        this.uncommittedResult = null;
        // do not notifyAll or hasUpdate or correctTotalStepEstimate, that will be done in commit()
    }
    
    // NLS-enabled: with key and a single arg
    synchronized void incrementCurrentStep(int additionalSteps, 
            String newCurrentStepDescriptionKey, Object arg) {
        Object[] args = { arg };
        this.incrementCurrentStep(additionalSteps, newCurrentStepDescriptionKey, args);
    }

    // NLS-enabled: with key and no args
    synchronized void incrementCurrentStep(int additionalSteps, 
            String newCurrentStepDescriptionKey) {
        this.incrementCurrentStep(additionalSteps, newCurrentStepDescriptionKey, null);
    }
    
    // NLS-enabled: with key and multiple args
    synchronized void incrementCurrentStep(int additionalSteps, 
            String newCurrentStepDescriptionKey, Object[] newCurrentStepDescriptionArgs) {
        this.currentStepDescriptionKey = newCurrentStepDescriptionKey;
        this.currentStepDescriptionArgs = newCurrentStepDescriptionArgs;
        this.incrementCurrentStep(additionalSteps);
    }
    
    synchronized private void incrementCurrentStep(int additionalSteps) {
        final String METHOD_NAME = "incrementCurrentStep"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "incrementing current step by " + additionalSteps + //$NON-NLS-1$
                " on task " + this.task + //$NON-NLS-1$
                " (was formerly " + this.currentStep + " of " + this.totalSteps + ") " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                " with description: " + getCurrentStepDescription()); //$NON-NLS-1$
        this.currentStep += additionalSteps;
        this.hasUpdate = true;
        this.currentStepTimestamp = new Date();
        this.correctTotalStepEstimate(METHOD_NAME);
        this.eventList.add(getCurrentStepTimestamp() + ","+ getCurrentStep() + ","+ getCurrentStepDescription());
        this.notifyAll();
    }
    
    // methods for the client (consumer of progress information)
    
    /**
     * Block and wait for this object to be updated. The update may be an
     * incremented step, completion, error, or a new number of total steps. Your
     * thread will sleep until there is an update to this object. If you want to
     * probe for an update without blocking, use {@link #hasUpdate()}.
     * <p>
     * If there is not an error and it is not complete, then the update is
     * regarding a current step.
     * 
     * @throws InterruptedException this thread was interrupted while waiting
     *         for an update
     * @see #getCurrentStep()
     * @see #getCurrentStepDescription()
     * @see #isComplete()
     * @see #hasError()
     * @see #hasUpdate()
     */
    synchronized public void waitForUpdate() throws InterruptedException {
        final String METHOD_NAME = "waitForUpdate"; //$NON-NLS-1$
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "thread " + Thread.currentThread().getName() + //$NON-NLS-1$
                " waiting for update to ProgressContainer with task " + this.task); //$NON-NLS-1$
        // there will not be any more updates if complete or has error
        while ((!this.hasUpdate()) && (!this.isComplete()) && (!this.hasError())) {
            this.wait();
        }
        // don't clear the hasUpdate indicator until at least one gettr method is called
    }
    
    /**
     * Block and wait for the object to reach "the end", where "the end" is
     * defined as being complete or ending abnormally with an error. This will
     * not return when the hasUpdate flag is set, it will wait until either
     * normal completion or an error completion.
     * 
     * @throws InterruptedException this thread was interrupted while waiting
     *         for an update
     * @see #waitForUpdate()
     */
    synchronized public void waitForEnd() throws InterruptedException {
        while ((!this.isComplete()) && (!this.hasError())) {
            this.waitForUpdate();
            // clear the update flag, because we care only about completion
            this.hasUpdate = false;
        }
    }
    
    /**
     * A non-blocking method to check if the object has an update. If you want
     * to sleep until a update is available, see {@link #waitForUpdate()}. An
     * update could consist of an increment of the current step, or a change in
     * the estimated total number of steps.
     * <p>
     * Note that this will return false if the object has completed either
     * successfully or with an error. If you wish to check for those conditions,
     * use {@link #isComplete()} or {@link #hasError()}.
     * <p>
     * Note that {@link #waitForUpdate()} will return on these two conditions
     * plus if the object completes either successfully or with an error. So
     * {@link #waitForUpdate()} behaves a bit differently than this method.
     * 
     * @return true if there is an update available, false otherwise
     * @see #waitForUpdate()
     */
    synchronized public boolean hasUpdate() {
        // if it is complete or has an error, there will be no more updates
        boolean result = (this.hasUpdate) && (!this.isComplete) && (this.error == null);
        return(result);
    }

    /**
     * Get the estimated total number of steps for this task. Calling this
     * method will clear the <code>hasUpdate</code> flag for this object.
     * 
     * @return the estimated total number of steps for this task. It is possible
     *         that the estimation may not be completely accurate, so a task may
     *         be complete when the current step is not equal to the total
     *         number of steps. To accurately check if this task is completed,
     *         use {@link #isComplete()}. To accurately check if this task ended
     *         abnormally with an error, use {@link #hasError()}.
     * @see #getCurrentStep()
     * @see #isComplete()
     * @see #hasError()
     */
    synchronized public int getTotalSteps() {
        // clear the hasUpdate flag
        this.hasUpdate = false;
        return(this.totalSteps);
    }
    
    /**
     * Get the task object (usually a BackgroundTask or Notification) that this
     * ProgressContainer is related to. Thus if you have the ProgressContainer,
     * you can use this method to get the Task and then you can probe the Task
     * to see what kind of task it is and get the Task metadata. (This assumes
     * that the task provides public methods that allow it to be probed, which
     * it should.)
     * 
     * @return the task object that this ProgressContainer is related to.
     */
    synchronized public Task getTask() {
        return(this.task);
    }
    
    /**
     * Get the current step number for this task. Calling this method will clear
     * the <code>hasUpdate</code> flag for this object.
     * 
     * @return the current step number for this task. The current step number
     *         should help you estimate how far this task is to approaching
     *         completion, and to provide feedback to the user that this task is
     *         moving forward with progress. For checking if this task is
     *         complete, see the documentation for {@link #isComplete()}.
     * @see #getTotalSteps()
     * @see #getCurrentStepDescription()
     * @see #getCurrentStepTimestamp()
     */
    synchronized public int getCurrentStep() {
        // clear the hasUpdate flag
        this.hasUpdate = false;
        return(this.currentStep);
    }
    
    /**
     * Get the textual description of the current step, resolving the
     * message key with the argument value substitution. Calling this method will
     * clear the <code>hasUpdate</code> flag for this object.
     * 
     * @return the textual description of the current step.
     * @see #getTotalSteps()
     * @see #getCurrentStep()
     * @see #getCurrentStepTimestamp()
     * @see #getCurrentStepDescriptionKey()
     * @see #getCurrentStepDescriptionArgs()
     */
    synchronized public String getCurrentStepDescription() {
        // clear the hasUpdate flag
        this.hasUpdate = false;
        return(Messages.getNonMsgString(this.getCurrentStepDescriptionKey(), 
                this.getCurrentStepDescriptionArgs()));
    }
    
    /**
     * Get the arguments for the description of the current step. Calling this
     * method will clear the <code>hasUpdate</code> flag for this object. This
     * should be used in conjunction with
     * {@link #getCurrentStepDescriptionKey()} if you want the values
     * separately, or just call {@link #getCurrentStepDescription()} which will
     * resolve the message key and substitute the argument values.
     * 
     * @return the arguments to be value substituted in the message indicated by
     *         the key.
     * @see #getCurrentStepDescriptionKey()
     * @see #getCurrentStepDescription()
     */
    public synchronized Object[] getCurrentStepDescriptionArgs() {
        this.hasUpdate = false;
        
        Object[] result = null;
        if ( currentStepDescriptionArgs != null )
        	result = currentStepDescriptionArgs.clone();
        return result;
    }

    /**
     * Get the key for the description of the current step. Calling this method will
     * clear the <code>hasUpdate</code> flag for this object. This should be used
     * in conjunction with {@link #getCurrentStepDescriptionArgs()} if you want
     * the key separately, or just call {@link #getCurrentStepDescription()}
     * which will resolve the message key and substitute the argument values.
     * 
     * @return the message key for the current step description
     * @see #getCurrentStepDescriptionArgs()
     * @see #getCurrentStepDescription()
     */
    public synchronized String getCurrentStepDescriptionKey() {
        this.hasUpdate = false;
        return currentStepDescriptionKey;
    }
    
    /**
     * Get the timestamp of the most recent update to this object. The timestamp
     * will be updated when this object is constructed, a step is updated, this
     * object is marked complete, or a task's exception is attached here to mark
     * an abnormal end. Getting this timestamp does not clear the
     * <code>hasUpdate</code> flag for this object.
     * 
     * @return the timestamp of the most recent update to this object.
     */
    synchronized public Date getCurrentStepTimestamp() {
    	Date result = this.currentStepTimestamp;
        return(result);
    }
    
    /**
     * Check if the task represented by this object completed successfully.
     * 
     * @return true if the task represented by this object completed
     *         successfully. If the task should return a result to the caller,
     *         that object may be retreived using {@link #getResult()}. It will
     *         return false if the task is still in process or if it ended
     *         abnormally with an error. To check if it ended with an error, use
     *         {@link #hasError()}. Calling this method will not clear the
     *         <code>hasUpdate</code> flag for this object.
     * @see #hasError()
     * @see #getResult()
     */
    synchronized public boolean isComplete() {
        return(this.isComplete);
    }
    
    /**
     * If the task was supposed to return a result to the caller, use this
     * method to fetch the result from the task. This method should be called
     * after <code>isComplete</code> returns <code>true</code>. Calling
     * this method will clear the <code>hasUpdate</code> flag for this object.
     * 
     * @return the result from the background task. Not all tasks return
     *         objects, it depends on the task. The returned object will be
     *         populated by the task.
     * @see #isComplete()
     */
    synchronized public Object getResult() {
        // clear the hasUpdate flag
        this.hasUpdate = false;
        return(this.result);
    }
    
    /**
     * Check if the task ended abnormally.
     * 
     * @return true if the task ended abnormally. In this case, you may retrieve
     *         the Exception that cause the task to terminate by using
     *         {@link #getError()}. The <code>isComplete</code> flag will be
     *         set to false. This will return false if the task is still
     *         executing or if it completed successfully. Calling this method
     *         will not clear the <code>hasUpdate</code> flag for this object.
     * @see #getError()
     * @see #isComplete()
     */
    synchronized public boolean hasError() {
        return(this.error != null);
    }
    
    /**
     * Get the Exception that caused the task to end abnormally. This method
     * should not be called until the <code>hasError</code> flag is true.
     * Calling this method will clear the <code>hasUpdate</code> flag for this
     * object.
     * 
     * @return the Exception that caused this task to end abnormally.
     * @see #hasError()
     */
    synchronized public Exception getError() {
        // clear the hasUpdate flag
        this.hasUpdate = false;
        return(this.error);
    }
    
    synchronized void reset() {
        // in case you want to reuse this object to track a different long-running task
        this.totalSteps = 0;
        this.currentStep = 0;
        this.currentStepDescriptionKey = null;
        this.currentStepDescriptionArgs = null;
        this.currentStepTimestamp = null;
        this.isComplete = false;
        this.uncommittedIsComplete = false;
        this.result = null;
        this.uncommittedResult = null;
        this.error = null;
        this.uncommittedError = null;
        this.hasUpdate = false;
        this.correlator = null;
        this.task = null;
        // creationDate member is final 
    }
    
    /**
     * Set the caller-defined correlator for this object. Calling this method
     * will have no effect on the <code>hasUpdate</code> flag for this object.
     * 
     * @param correlator a caller-defined correlation value for this object.
     *        This is a value which can be optionally set and retrieved by the
     *        caller to store a value that may help correlate this background
     *        task to other items that the caller cares about. The queue
     *        processor does not look at this correlator, it is for use only by
     *        the caller. The default value of the correlator is null.
     * @see #getCorrelator()
     */
    synchronized public void setCorrelator(Object correlator) {
        this.correlator = correlator;
    }
    
    /**
     * Get the caller-defined correlator for this object. Calling this method
     * will have no effect on the <code>hasUpdate</code> flag for this object.
     * 
     * @return the caller-defined correlator for this object. See
     *         {@link #setCorrelator(Object)}.
     * @see #setCorrelator(Object)
     */
    synchronized public Object getCorrelator() {
        return(this.correlator);
    }
    
    /**
     * Get the date this object was created. Calling this method will not clear
     * the <code>hasUpdate</code> flag for this object.
     * 
     * @return the date this object was created.
     */
    public Date getCreationDate() {
        /*
         * This method does not need to be <code>synchronized</code> because
         * the creation date never changes after this object is instantiated, so
         * there never is a race condition for this read-only data.
         */
    	Date result = this.creationDate;
        return (result);
    }
    
    /**
     * A convenience method for waiting for the task to complete and printing
     * the progress of each step. It will wait for an update, and for each step
     * print the step number and description. When the task completes
     * successfully it will print that and check if the task returned an object.
     * If the task ends abnormally it will print the Exception that caused the
     * problem.
     * 
     * @param level the log level at which to generate the text messages
     * @throws InterruptedException this thread was interrupted while waiting
     *         for an update
     * @throws Exception if the task ends abnormally with an error, this method
     *         will throw a new Exception with the original Exception in the
     *         "cause" field.
     */
    synchronized public void blockAndTrace(Level level) 
    throws InterruptedException, Exception {
        final String METHOD_NAME = "blockAndTrace"; //$NON-NLS-1$
        while (!this.isComplete && (this.error==null)) {
            // clear the update flag because we are getting the updated data now
            this.hasUpdate = false;
            logger.logp(level, CLASS_NAME, METHOD_NAME,
                    "Executing step " + this.currentStep + //$NON-NLS-1$
                    " of " + this.totalSteps + //$NON-NLS-1$
                    ": " + this.getCurrentStepDescription()); //$NON-NLS-1$
            this.waitForUpdate();
        }
        if (this.error != null) {
            String message = Messages.getString("wamt.clientAPI.ProgressContainer.taskNotComp",this.error); //$NON-NLS-1$
            Exception e = new Exception(message, this.error);
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            logger.logp(level, CLASS_NAME, METHOD_NAME, message);
            throw(e);
        } else {
            String message = "Task complete."; //$NON-NLS-1$
            if (this.result != null) {
                message += " Result: " + this.result; //$NON-NLS-1$
            }
            logger.logp(level, CLASS_NAME, METHOD_NAME, 
                    message);
        }
    }
    
    /**
     * Have this ProgressContainer act as a facade to another ProgressContainer.
     * Or in other words, have this one "follow" that one. This would be used if
     * one long-running method is a composite or or derivation of other
     * long-running methods. For the purpose of this explanation, I will call
     * <code>this</code> the 2nd ProgressContainer and <code>that</code> the
     * 1st ProgressContainer. Even though the 1st one may be partially complete,
     * the 2nd one will look at how many more steps the 1st one has and will
     * follow it along until the 1st one completes. Whatever result or exception
     * that the 1st one finished with, that data will be copied into the 2nd one -
     * this is what I call a "merge". This method will block until the 1st one
     * finishes.
     * 
     * @param that the first ProgressContainer to follow.
     * @param doMergeAtEnd if true, will copy the result/exception data from the
     *        first one into the second one (this one). You would want to do
     *        this if the second one isn't going to follow any more
     *        ProgressContainers.
     * @see #merge(ProgressContainer)
     */
    synchronized public void follow(ProgressContainer that, boolean doMergeAtEnd) {
        final String METHOD_NAME = "follow"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, that);
        int snapshotTotalSteps = that.getTotalSteps();
        int snapshotCurrentStep = that.getCurrentStep();
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                "starting to follow on step " + snapshotCurrentStep + //$NON-NLS-1$
                " of " + snapshotTotalSteps); //$NON-NLS-1$
        this.incrementTotalSteps(snapshotTotalSteps);
        String descriptionKey = that.getCurrentStepDescriptionKey();
        Object[] descriptionArgs = that.getCurrentStepDescriptionArgs();
        this.incrementCurrentStep(snapshotCurrentStep, descriptionKey, descriptionArgs);
        boolean interrupted = false;
        while (!that.isComplete() && !that.hasError() && !interrupted) {
            try {
                that.waitForUpdate();
            } catch (InterruptedException e) {
                interrupted = true;
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "thread interrupted in waitForUpdate()"); //$NON-NLS-1$
            }
            // check for a difference in totalSteps
            if (that.getTotalSteps() != snapshotTotalSteps) {
                int delta = that.getTotalSteps() - snapshotTotalSteps;
                this.incrementTotalSteps(delta);
                snapshotTotalSteps += delta;
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "followee incremented total steps by " + delta); //$NON-NLS-1$
            }
            // check for a difference in currentStep number
            if (that.getCurrentStep() != snapshotCurrentStep) {
                int delta = that.getCurrentStep() - snapshotCurrentStep;
                descriptionKey = that.getCurrentStepDescriptionKey();
                descriptionArgs = that.getCurrentStepDescriptionArgs();
                this.incrementCurrentStep(delta, descriptionKey, descriptionArgs);
                snapshotCurrentStep += delta;
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "followee incremented current step by " + delta); //$NON-NLS-1$
            }
        }

        if (that.hasError()) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "followee reported an error"); //$NON-NLS-1$
        }
        if (that.isComplete()) {
            Object result = that.getResult();
            if (result == null) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "followee reported completion without result"); //$NON-NLS-1$
            } else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                        "followee reported completion with result: " + result); //$NON-NLS-1$
            }
        }
        this.merge(that);
        
        logger.exiting(CLASS_NAME, METHOD_NAME, that.getResult());
    }
    
    /**
     * Merge all the results (both result and exception) of another
     * ProgressContainer into this one. If the other ProgressContainer
     * <code>that</code> has an error or a result, then copy it into this one.
     * 
     * @param that the ProgressContainer from which to copy the exception or
     *        result object from
     * @see #mergeError(ProgressContainer)
     */
    synchronized public void merge(ProgressContainer that) {
        final String METHOD_NAME = "merge"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, that);

        // error
        this.mergeError(that);
        
        // complete
        if (that.isComplete()) {
            Object result = that.getResult();
            if (result == null) {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "merging completion flag from that into this"); //$NON-NLS-1$
                this.setComplete();
            } else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        "merging completion flag and result from that into this"); //$NON-NLS-1$
                this.setComplete(result);
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME, that.result);
    }
    
    /**
     * Merge the error of another ProgressContainer into this one. If the other
     * ProgressContainer <code>that</code> has an error, then copy it into
     * this one.
     * 
     * @param that the ProgressContainer from which to copy the exception from
     */
    synchronized public void mergeError(ProgressContainer that) {
        final String METHOD_NAME = "mergeError"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {this, that});

        if (that.hasError()) {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "merging error from that into this"); //$NON-NLS-1$
            this.setError(that.getError());
        } else {
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                    "that does not have an error"); //$NON-NLS-1$
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, new Object[] {this, that});
    }
    
    /**
     * Create a human-readable String representation of this object.
     * 
     * @return a human-readable String representation of this object
     */
    synchronized public String toString() {
        String resultString = "ProgressContainer["; //$NON-NLS-1$
   
        if (this.isComplete) {
            resultString += "Complete"; //$NON-NLS-1$
            if (this.result != null) {
                resultString += " Result: " + this.result; //$NON-NLS-1$
            }
        } else if (this.error != null) {
            resultString += "Error: " + this.error; //$NON-NLS-1$
        } else {
            resultString += "On step " + this.currentStep +  //$NON-NLS-1$
            " of " + this.totalSteps +  //$NON-NLS-1$
            ": " + this.getCurrentStepDescription(); //$NON-NLS-1$
        }
        resultString += "]"; //$NON-NLS-1$
        return(resultString);
    }
    
    /**
     * Returns a Vector of step descriptions with the most recent at the end of the Vector.
     * 
     * @return the Vector, each element describes a completed step
     * @see #hasError()
     */
    synchronized public Vector getEventList() {               
        return(this.eventList);
    }

}
