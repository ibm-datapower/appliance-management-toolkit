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

/**
 * The interface establishes an object hierarchy that includes BackgroundTasks
 * and Notifications. A ProgressContainer, MacroProgressContainer, and some
 * queues work with Tasks.
 * <p>
 * This class does not do anything other than act as a parent for the subclasses.
 * All the funcion and definition is in the subclasses.
 * <p>
 * If you are looking at the internals of the clientAPI, please also look at
 * the class <code>QueueProcessor</code>.
 * <p>
 * 
 * @see ProgressContainer
 * @see MacroProgressContainer
 * @version SCM ID: $Id: Task.java,v 1.2 2010/08/23 21:20:27 burket Exp $
 */
public interface Task {

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;
    
    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
}
