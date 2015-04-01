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

package com.ibm.datapower.amt.dataAPI;

import com.ibm.datapower.amt.Constants;

/**
 * An interface to identify which classes should be persisted to a repository. The
 * Some Stored objects available with the data API implement this interface.
 * <p>
 * @see StoredDeployableConfiguration
 * @see StoredDevice
 * @see StoredFirmware
 * @see StoredManagedSet
 * @see StoredVersion
 * 
 */
public interface Persistable {
    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.2 $"; //$NON-NLS-1$
    
    /**
     * Returns the Unique identifier for this Persisted object.
     * 
     * @return A string uniquely identifying this persisted object
     */
    String getPrimaryKey();
}
