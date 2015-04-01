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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.OperationStatus;
import com.ibm.datapower.amt.amp.DomainStatus;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A <code>MacroProgressContainer</code> is a container for multiple 
 * ProgressContainers. This class is typically used for <code>ManagedSet</code> 
 * operations, since a <code>ManagedSet</code> typical contains multiple 
 * <code>Device</code> objects, and long running operations performed on 
 * <code>Device</code> are wrapped in a {@link ProgressContainer}. 
 * <p>
 * The execution of <code>MacroProgressContainer</code> is considered to have an 
 * error if any of the individual {@link ProgressContainer} tasks have an error. 
 * The execution of a <code>MacroProgressContainer</code> is considered to be complete 
 * only when all of <code>ProgressContainer</code> tasks it contains objects are complete.
 * <p>
 * A <code>MacroProgressContainer</code> may find an exception in any of its nested
 * <code>ProgressContainer</code> objects, or it may have an exception itself. A
 * <code>MacroProgressContainer</code> can wait for all the nested <code>ProgressContainer</code>
 * objects to be complete, and it must wait for itself to be marked complete. However, a
 * <code>MacroProgressContainer</code> doesn't have any steps or step descriptions itself,
 * those come only from the nested <code>ProgressContainer</code> objects.
 * <p>
 * 
 * @see ProgressContainer
 * @version SCM ID: $Id: MacroProgressContainer.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
/* BEGIN INTERNAL COMMENTS
 * A container for multiple ProgressContainers that can be used to represent
 * macro tasks. A macro task could be described as a collection of single tasks.
 * The user interface may want to track progress of macro tasks, and that is
 * done via the class. Examples of macro tasks where this class is used is the
 * {@link HeartbeatTask}, and {@link Manager#shutdown()}.
 * <p>
 * The macro task is considered to have an error if any of the single tasks have
 * an error. A macro task is considered to be complete only when all the single
 * tasks are complete.
 * <p>
 * A MacroProgressContainer may find an exception in any of the nested
 * ProgressContainers, or it may have an exception itself. A
 * MacroProgressContainer can wait for all the nested ProgressContainers to be
 * complete, and it must wait for itself to be marked complete. However, a
 * MacroProgressContainer doesn't have any steps or step descriptions itself,
 * those come only from the nested ProgressContainers.
 * <p>
 * 
 * @see ProgressContainer
 * @version SCM ID: $Id: MacroProgressContainer.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 * END INTERNAL COMMENTS
 */
public class MacroProgressContainer {
    private Task task = null;
    private Vector nestedProgressContainers = null;
    private boolean isComplete = false;
    private Exception error = null;
    private boolean hasUpdate = false;
    private int sleepInterval = 0;
    private Object correlator = null;
    private Hashtable failedTasks = null;
    
    /*
     * I would have liked to just reuse ProgressContainer and create subtasks,
     * but there are just too many problems with return completion states and
     * error states. Not all of the ProgressContainer methods make sense in a
     * MacroProgressContainer. A MacroProgressContainer needs to have different
     * behavior than a ProgressContainer. Thus it is a different class.
     */
    
    /**
     * The default time for the sleep interval while polling for update or
     * completion.
     * 
     * @see #setSleepInterval(int)
     */
    public static final int DEFAULT_SLEEP_MS = 200;
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = MacroProgressContainer.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    MacroProgressContainer(Task task) {
        this.task = task;
        this.nestedProgressContainers = new Vector();
        this.sleepInterval = DEFAULT_SLEEP_MS;
    }
    
    synchronized void addNested(ProgressContainer progressContainer) {
        this.nestedProgressContainers.add(progressContainer);
    }
    
    synchronized void setComplete() {
        // nested ProgressContainers still may be running in background
        this.isComplete = true;
        this.hasUpdate = true;
    }
    
    synchronized void setError(Exception error) {
        this.error = error;
        this.hasUpdate = true;
    }
    
    /**
     * Gets the interval between checks through all the nested
     * ProgressContainers for any that may have had an update. For more
     * information about this, see {@link #setSleepInterval(int)}.
     * 
     * @return the interval in milliseconds that it sleeps before checking all
     *         the nested ProgressContainers for any that may be updated.
     * @see #setSleepInterval(int)
     * @see #waitForUpdate()
     * @see #waitForEnd()
     */
    synchronized public int getSleepInterval() {
        return(this.sleepInterval);
    }
    
    /**
     * Sets the interval between checks through all the nested
     * ProgressContainers for any that may have had an update. Since there
     * generally are multiple ProgressContainers involved, there isn't any easy
     * way to block in such a way that {@link #waitForUpdate()} or
     * {@link #waitForEnd()} will return if any of them get an update. Because
     * polling must be used, this is how long <code>waitForUpdate()</code> or
     * <code>waitForEnd()</code> method should wait before checking all the
     * ProgressContainers for update or completion. You should pick a value here
     * that is a good balance between responsiveness and consuming too much CPU
     * doing the poll.
     * 
     * @param ms the interval between checks through all the nested
     *        ProgressContainers for any that may have had an update. If not set
     *        explicitly, the default is {@link #DEFAULT_SLEEP_MS}.
     * @see #getSleepInterval()
     * @see #waitForUpdate()
     * @see #waitForEnd()
     */
    synchronized public void setSleepInterval(int ms) {
        this.sleepInterval = ms;
    }

    /**
     * Block until one of the nested ProgressContainers has an update.
     * You can use this method to "block" in a CPU-friendly way to 
     * wait until there is an update available. Because there is not a good
     * way to truly block until one of the nested ProgressContainers has
     * an update, this method will poll all the nested ProgressContainers
     * to check for updates, then will sleep for the duration specified
     * by {@link #setSleepInterval(int)} until the next check.
     * 
     * @throws InterruptedException this method was interrupted while sleeping
     *         for an update.
     * @see ProgressContainer#waitForUpdate()
     * @see #hasUpdate()
     */
    synchronized public void waitForUpdate() throws InterruptedException {
        boolean anyUpdated = false;
        while (!anyUpdated) {
            if (this.hasUpdate) {
                anyUpdated = true;
            }
            for (int i=0; i<this.nestedProgressContainers.size(); i++) {
                ProgressContainer progressContainer = 
                    (ProgressContainer) this.nestedProgressContainers.get(i);
                if (progressContainer.hasUpdate()) {
                    anyUpdated = true;
                }
                
            }
            if (!anyUpdated) {
            	this.wait(this.sleepInterval); //Thread.sleep(this.sleepInterval);
            }
        }
    }
    
    /**
     * Block until all of the nested ProgressContainers are complete. You can
     * use this method to "block" in a CPU-friendly way to wait until completion
     * is reached. Because there is not a good way to truly block until all of
     * the nested ProgressContainers are complete, this method will poll all the
     * nested ProgressContainers to check for completion, then will sleep for
     * the duration specified by {@link #setSleepInterval(int)} until the next
     * check.
     * 
     * @throws InterruptedException this method was interrupted while sleeping
     *         for an update.
     * @see ProgressContainer#waitForEnd()
     * @see #isComplete()
     */
    synchronized public void waitForEnd() throws InterruptedException {
        boolean allEnded = false;
        while (!allEnded) {
            // assume true unless proved false
            allEnded = true;
            if (!(this.isComplete || this.hasError())) {
                allEnded = false;
            }
            for (int i=0; i<this.nestedProgressContainers.size(); i++) {
                ProgressContainer progressContainer =
                    (ProgressContainer) this.nestedProgressContainers.get(i);
                if (!(progressContainer.isComplete() || progressContainer.hasError())) {
                    allEnded = false;
                }
            }
            if (!allEnded) {                
                this.wait(this.sleepInterval); //Thread.sleep(this.sleepInterval);
            }
        }
    }
    
    /**
     * Check if any of the nested ProgressContainers has an update.
     * 
     * @return true if any of the nested ProgressContainers has an update, false
     *         otherwise.
     * @see #waitForUpdate()
     * @see ProgressContainer#hasUpdate()
     */
    synchronized public boolean hasUpdate() {
        boolean anyUpdated = false;
        if (this.hasUpdate) {
            anyUpdated = true;
        }
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            if (progressContainer.hasUpdate()) {
                anyUpdated = true;
            }
        }
        return(anyUpdated);
    }

    /**
     * Get the total number of steps across all the nested ProgressContainers.
     * This should be the sum of the total tasks in each ProgressContainer.
     * 
     * @return the total number of steps across all the nested
     *         ProgressContainers.
     * @see ProgressContainer#getTotalSteps()
     * @see #getCurrentStep()
     */
    synchronized public int getTotalSteps() {
        int allSteps = 0;
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            allSteps += progressContainer.getTotalSteps();
        }
        return(allSteps);
    }
    
    /**
     * Get the Task for which this MacroProgressContainer has been created. With
     * this Task object, you can retrieve metadata about this macro task.
     * 
     * @return the Task for which this MacroProgressContainer has been created.
     * @see ProgressContainer#getTask()
     */
    synchronized public Task getTask() {
        return(this.task);
    }
    
    /**
     * Get the current step which indicates the progress across the total number
     * of steps in this MacroContainer. This should be the sum of the current
     * steps across all the nested ProgressContainers.
     * 
     * @return the current step which indicates the progress across the total
     *         number of steps in this MacroContainer.
     * @see #getTotalSteps()
     * @see ProgressContainer#getCurrentStep()
     * @see #getCurrentStepDescription()
     */
    synchronized public int getCurrentStep() {
        int allCompletedSteps = 0;
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            allCompletedSteps += progressContainer.getCurrentStep();
        }
        return(allCompletedSteps);
    }
    
    /**
     * Get the description of the most recently updated step across all the
     * nested ProgressContainers.
     * 
     * @return the description of the most recently updated step across all the
     *         nested ProgressContainers.
     * @see #getCurrentStep()
     * @see ProgressContainer#getCurrentStepDescription()
     */
    synchronized public String getCurrentStepDescription() {
        // find the most recently updated step description
        String description = null;
        Date latestTimestamp = null;
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            Date thisTimestamp = progressContainer.getCurrentStepTimestamp();
            if ((latestTimestamp == null) || (thisTimestamp.after(latestTimestamp))) {
                latestTimestamp = thisTimestamp;
                description = progressContainer.getCurrentStepDescription();
            }
        }
        return(description);
    }
    
    /**
     * Check if this MacroProgressContainer is complete. For this to be true,
     * all the nested ProgressContainers must be complete, and this
     * MacroProgressContainer must have been marked complete also.
     * 
     * @return true if this MacroProgressContainer is complete, false otherwise.
     * @see ProgressContainer#isComplete()
     * @see #waitForEnd()
     */
    synchronized public boolean isComplete() {
        // everything must be complete, even this object
        boolean allComplete = true;
        if (!this.isComplete) {
            allComplete = false;
        }
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            if (!progressContainer.isComplete()) {
                allComplete = false;
            }
        }
        return(allComplete);
    }
    
    /**
     * Check if any of the nested ProgressContainers or this
     * MacroProgressContainer has an error set.
     * 
     * @return true if any of the nested ProgressContainers or this
     *         MacroProgressContainer has an error set, false otherwise.
     * @see ProgressContainer#hasError()
     * @see #getError()
     */
    synchronized public boolean hasError() {
        boolean anyError = false;
        if (this.error != null) {
            anyError = true;
        }
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            if (progressContainer.hasError()) {
                anyError = true;
            }
        }
        return(anyError);
    }
    
    /**
     * Return the Exception from any of the nested ProgressContainers or from
     * this MacroProgressContainer.
     * 
     * @return Exception from any of the nested ProgressContainers or from this
     *         MacroProgressContainer. If no error has been encountered, this
     *         value will be null. If this MacroProgressContainer has an error,
     *         that value will be returned before checked the nested
     *         ProgressContainers. If more than one nested ProgressContainer has
     *         an error, the first one found will be returned.
     * @see #hasError()
     * @see ProgressContainer#getError()
     */
    synchronized public Exception getError() {
        // just find the first one
        if (this.error != null) {
            return(this.error);
        }
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            Exception nestedError = progressContainer.getError();
            if (nestedError != null) {
                return(nestedError);
            }
        }
        return(null);
    }
    
    /**
     * Set the correlator for this MacroProgressContainer. This is a value which
     * can be optionally set and retrieved by the caller to store a value that
     * may help correlate this task to other items that the caller cares about.
     * The manager does not look at this correlator, it is for use only by the caller.
     * The default value of the correlator is null.
     * 
     * @param correlator a user-defined value. It does not affect the correlator
     *        for any of the nested ProgressContainers.
     * @see #getCorrelator()
     * @see ProgressContainer#setCorrelator(Object)
     */
    synchronized public void setCorrelator(Object correlator) {
        this.correlator = correlator;
    }
    
    /**
     * Get the correlator for this MacroProgressContainer. The correlator is a
     * non-functional place for the caller to attach any object they want. For
     * more details, see {@link #setCorrelator(Object)}.
     * 
     * @return the correlator for this MacroProgressContainer.
     * @see #setCorrelator(Object)
     * @see ProgressContainer#getCorrelator()
     */
    synchronized public Object getCorrelator() {
        return(this.correlator);
    }
    
    /**
     * Get a String representation of this object for the purpose of debugging
     * or tracing.
     * 
     * @return a String representation of this object for the purpose of
     *         debugging or tracing.
     */

    synchronized public String toString() {
        String result = "MacroProgressContainer["; //$NON-NLS-1$
        
        if (this.isComplete) {
            result += "Complete"; //$NON-NLS-1$
        } else if (this.error != null) {
            result += "Error: " + this.error.toString(); //$NON-NLS-1$
        } else {
            result += "On step " + this.getCurrentStep() +  //$NON-NLS-1$
            " of " + this.getTotalSteps() +  //$NON-NLS-1$
            ": " + this.getCurrentStepDescription(); //$NON-NLS-1$
        }
        
        StringBuffer buf = new StringBuffer(result);
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            ProgressContainer progressContainer =
                (ProgressContainer) this.nestedProgressContainers.get(i);
            buf.append(" " + progressContainer.toString());//result += " " + progressContainer.toString(); //$NON-NLS-1$
        }
        result = buf.toString();
        result += "]"; //$NON-NLS-1$
        return(result);
    }
    
    /**
     * Returns the nested ProgressContainers for any returned values or errors.
     *  
     */
    synchronized public ProgressContainer[] getProgressContainers() {
        ProgressContainer [] pg = new ProgressContainer[nestedProgressContainers.size()];
        for (int i=0; i<this.nestedProgressContainers.size(); i++) {
            pg[i] = (ProgressContainer) this.nestedProgressContainers.get(i);
        }
        return pg;
    }

	public void setFailedTasks(Hashtable failedTasks) {
		// this.failedTasks = failedTasks;		
		if ( failedTasks != null ) {
			this.failedTasks = new Hashtable();
	    	Iterator iterator = failedTasks.entrySet().iterator();
	    	while (iterator.hasNext()) {
	    	     Map.Entry entry = (Map.Entry)iterator.next();
	    	     if ( entry != null ) {
	    	         this.failedTasks.put(entry.getKey(), entry.getValue());
	    	     }
	    	}
		}		
	}

	public Hashtable getFailedTasks() {
		Hashtable result = new Hashtable();		
		// Copy Hashtable for Findbugs
		Iterator iterator = failedTasks.entrySet().iterator();
        while (iterator.hasNext()){        
        	Map.Entry entry = (Map.Entry) iterator.next();        	
        	result.put(entry.getKey(), entry.getValue());
        }		
		return result;
	}
	
    /**
     * A convenience method for waiting for the tasks to complete and printing
     * the progress of each step for each nested ProgressConatiner. It will wait 
     * for an update, and for each step in each of the nested ProgressConatiners
     * print the step number and description. When the each task completes
     * successfully it will print that.
     * If any of the task end abnormally it will print the Exception that caused the
     * problem.
     * None of the nested ProgressContainers return objects using this method.
     * 
     * @param level the log level at which to generate the text messages
     * @throws InterruptedException this thread was interrupted while waiting
     *         for an update
     * @throws Exception if the task ends abnormally with an error, this method
     *         will throw a new Exception with the original Exception in the
     *         "cause" field.
     */
    synchronized public void blockAndTrace(Level level) 
     {
        final String METHOD_NAME = "blockAndTrace"; //$NON-NLS-1$
        ProgressContainer [] pcs = this.getProgressContainers();
        
        for (ProgressContainer p:  pcs){
        	try{
        	p.blockAndTrace(level);
        	}catch(InterruptedException interrupted){
                String message = Messages.getString("wamt.clientAPI.ProgressContainer.taskNotComp",interrupted); //$NON-NLS-1$
                logger.logp(level, CLASS_NAME, METHOD_NAME, message);        		
        	}catch(Exception exception){
                String message = Messages.getString("wamt.clientAPI.ProgressContainer.taskNotComp",exception); //$NON-NLS-1$
                logger.logp(level, CLASS_NAME, METHOD_NAME, message);         		
        	}
        }
        
        
    }
}
