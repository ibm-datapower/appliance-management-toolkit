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
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 */
/**
 * Implements a Runnable object for executing Domain Synchronization. It call the domain.synch() to check if 
 * a domain is in synch with configuration source.  If it is not in synch and the domain SynchMode is set to
 * AUTO (automatic synchronization), the source configuration is deployed to the domain.
 */
public class DomainSynchronizationTask extends BackgroundTask { //implements Runnable, Task {
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    private static final String CLASS_NAME = DomainSynchronizationTask.class.getName();    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
        
    private Domain domain = null;
    private boolean compareMode = false;
    private Device device = null; 
    // Indicates if the task is active
    private boolean isDone = false;
    private String uniqueTaskIdentifier = "";
    
    public DomainSynchronizationTask(Domain domain, Boolean compareMode) { 
        String METHOD_NAME = "DomainSynchronizationTask";
    	try {
			this.device = domain.getDevice();
		} catch (DeletedException e) {
			// TODO Auto-generated catch block
			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "Exception thrown" ,e);
		}
        this.domain = domain;
        this.compareMode = compareMode;
  	    this.progressContainer.setTotalSteps(this.estimateSteps());   
  	    this.progressContainer.setCorrelator(this.device);
  	    this.isDone = false;
  	    this.uniqueTaskIdentifier = String.valueOf(System.currentTimeMillis()) + domain.getAbsoluteDisplayName(); 

  	    Manager manager = Manager.internalGetInstance();
        manager.addDomainSyncProgress(this.progressContainer);
    }    
    
   
    /**
     * Obligatory toString implementation for logging purposes
     * @return The string representation of this DomainSychronization task
     */
    public String toString() {
    	String METHOD_NAME  ="toString";
        String result = "";
		try {
			result = "DomainSynchronizationTask: " + this.domain.getName() + "," + this.device.toString();
		} catch (DeletedException e) {
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    "Delete Exception occurred on domain.", e); //TODO LS Message
	
			//e.printStackTrace();
		} 
        return(result);
    }

    /**
     * Lock the Device - lockNoWait()
     * Call domain.synch(CompareMode = false) 
     * If LockBusyException is caught then wait for 1 sec and put the task back on the DomainSynchronizationQueue
     * 
     * If the manager is in the process of shutting down then, then stop putting new tasks on the DS queue
     */
    public void execute(){
        
        final String METHOD_NAME = "execute (Thread " + Thread.currentThread().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
        Manager mn = Manager.internalGetInstance();
        
        try{
        	// Set up to call domain,sync().  Task may be created by Domain "Set" methods, QueueProcesssor Notification of Domain is saved,
        	// or the DS daemon
        	device = domain.getDevice();           
            //device.lockNoWait();   willl be locked by domain.synch()
      	    mn.addDomainSyncTaskInProgress(this.uniqueTaskIdentifier,this);
            Object [] args = new Object[2];
            args[0] = this.domain.getName();
            args[1] = this.domain.getDevice(); 
          
            if(domain.synch(compareMode)){
            	if (this.progressContainer != null){
          	       this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.DeployDomainConfigurationTask.deploySource", args);
            	}
            }else{
            	if (this.progressContainer != null){
            	   this.progressContainer.incrementCurrentStep(1, "wamt.clientAPI.DeployDomainConfigurationTask.syncNotNeccesary",args[1]);
            	}
            }

			if (progressContainer != null){
				   progressContainer.setComplete();
			}else{				
				if (mn.getShutdownStatus()){
					// By this point the ProgressContainer on the task has been cleared
				   logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, ("sync() Complete. Manager Shutting Down: " + mn.getShutdownStatus()));
				}else{
					// some other problem occurred with ProgressContainer.  
					// eat it - can't do anything with PC
				}
			}
        }catch(LockBusyException e){

        	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
        			"Put the synchronization task back on the queue for domain " + domain + " with a delay of 1 sec");
      	
     		
        		//Put this task back on the queue	
            	sleep(1000);        
        		if (DomainSynchronizationQueue.getDomainSynchronizationQueueInstance()!= null){
        			DomainSynchronizationQueue.getDomainSynchronizationQueueInstance().execute(this);
        		}else{
        			logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
        			"DomainSynchronizationQueue had not been instaniated when domain.sync() was called."); //TODO LS Message					
        		}

	            
        }catch(Exception e){
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    "Exceptions occurred when domain.sync() was called.", e); //TODO LS Message
				//e.printStackTrace();
				if (progressContainer != null){
					progressContainer.setError(e);
				}											
        					
        } finally {
        	//device.unlock();         	
        	// If manager is shutting down set the isDone to true.  This marks the task as completed and no further attempt will be made
        	// to run it and it will be discarded.  The Progress container gets cleared once the shuts down starts and is not a reliable way to indicate completion.          
			this.isDone = true; 
			// Once shutdown starts do not modify the domainSyncTasksInProgress Hashtable in Manager
			// Any remaining tasks in the Hashtable will be cleared in Manager.shutdown().
			if(!mn.getShutdownStatus()){
				mn.removeDomainSyncTaskInProgress(this.uniqueTaskIdentifier); 	
				//System.out.println("task removed from DomainSyncTaskInProgress " + this.toString() +
				//		", isDone: " + this.isDone);
			}	
            //this.cleanup();  	Do not clean up, this task may have been put back on the queue
			// due to Lock Busy exception
        }
    }
    
    public String getDomainName(){
    	String METHOD_NAME = "getDomainName";
    	String name = "";
    	try {
			name = domain.getName();
		} catch (DeletedException e) {
			logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Exception thrown:", e);
		}
		return name;
    }
    
    public boolean getStatus(){
		return this.isDone;
    }

    public void setStatus(boolean status){
		this.isDone = status;
    }
	protected int estimateSteps() {
		return(1);
    }
    
//    protected boolean affectsFirmware() {
//        return false;
//    }
//
//    protected boolean affectsDomains() {
//        return false;
//    }
//
//    protected String getSingleAffectedDomain() {
//        return null;
//    }
//
//    protected Device getSingleAffectedDevice() {
//        return this.device;
//    }
    
    
    private void sleep(long time) {
		final String METHOD_NAME = "sleep";    	
    	try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

			// Auto-generated catch block - eat it
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Interrupted Exception may have been caused by Domain synchronization threads shutting down"); //TODO LS Message			
		
		}
    }    
    


    
}
