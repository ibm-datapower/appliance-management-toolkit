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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.AbstractFactory;
import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Credential;
import com.ibm.datapower.amt.DMgrException;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Factory for creating/manufacturing the Repository object.
 * 
 */
public class RepositoryFactory extends AbstractFactory {

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = RepositoryFactory.class
            .getName();

    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    private RepositoryFactory() throws DMgrException {
        // don't call the constructor
        super();
    }

    /**
     * Returns an instance of the repository. A Repository is a singleton, so this
     * will return a reference to the instance. The caller must have valid
     * credentials to access the repository.
     * 
     * @param implementationClassName
     *            name of the class that implements the Repository interface.
     * @param credential
     *            credential which indicate that the caller is authorized to
     *            access the repository
     * @return a reference to the repository
     */
    public static Repository getRepository(String implementationClassName,
            Credential credential) throws DatastoreException {
        final String METHOD_NAME = "getRepository(String, Credential)"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, implementationClassName);
        Repository repository = null;
        Object[] args = new Object[1];
        args[0] = credential;
        try {
            repository = (Repository) getUntypedInstance(
                    implementationClassName, Repository.class, "getInstance", //$NON-NLS-1$
                    args, SINGLETON);
        } catch (DMgrException e) {
        	Object[] params = {RepositoryFactory.class.getName(),implementationClassName};
            String msg = Messages.getString("wamt.dataAPI.RepositoryFactory.exInvokeGetInst",params); //$NON-NLS-1$
            DatastoreException de = new DatastoreException(msg, e, "wamt.dataAPI.RepositoryFactory.exInvokeGetInst",params);
            logger.throwing(CLASS_NAME, METHOD_NAME, de);
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, msg, e);
            throw (de);
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return (repository);
    }
}
