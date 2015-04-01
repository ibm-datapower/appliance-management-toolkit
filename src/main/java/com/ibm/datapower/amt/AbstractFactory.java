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

package com.ibm.datapower.amt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A common implementation of a class factory, it can be extended to create
 * specific factories. In the javadoc refer to the list of known subclasses for
 * a list of classes where this is used. This abstract factory can be configured
 * to return singletons or create new objects upon each invocation. It uses
 * reflection to obtain and validate any named class. If you wish to use this,
 * look at the methods with the <code>protected</code> access modifier,
 * especially <code>getUntypedInstance</code>. (Note: the default Ant script
 * may be configured to generate javadoc for only public methods and not for
 * protected methods, so the javadoc for <code>getUntypedInstance</code> may
 * not be generated automatically.)
 * <p>
 * <p>
 * @version SCM ID: $Id: AbstractFactory.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 * 
 */
public abstract class AbstractFactory {
	
    /**
     * When the factory is invoked, it should find an existing object and return
     * it. If no object already exists, it should create one and cache it.
     */
    public final static boolean SINGLETON = true;
    /**
     * When the factory is invoked, it should create a new instance of the
     * object even if one already exists.
     */
    public static boolean NON_SINGLETON = false;
    private static Map instanceCache = new HashMap();

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected final static String CLASS_NAME = AbstractFactory.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }
    
    static final String SCM_REVISION = "$Revision: 1.5 $"; //$NON-NLS-1$

    protected AbstractFactory() throws DMgrException {
        final String METHOD_NAME = "AbstractFactory"; //$NON-NLS-1$
        // don't call the constructor
        DMgrException e = new DMgrException(Messages.getString("wamt.AbstractFactory.cannotInvokeConst")); //$NON-NLS-1$
        logger.throwing(CLASS_NAME, METHOD_NAME, e);
        throw(e);
    }

    /**
     * Get an instance of a class that implements the specified
     * <code>parentInterface</code>. Reflection is used to accomplish that,
     * so references to the desired class are defined by names (Strings).
     * 
     * @param implementationClassName
     *            the name of the class that implements
     *            <code>parentInterface</code>. This name should be in the
     *            format that can be used by
     *            {@link Class#forName(java.lang.String)}. This method will
     *            attempt to load the named class. The named class should be on
     *            the JRE's classpath.
     * @param parentInterface
     *            a reference to the interface that the implementation should
     *            implement. This will typically be an interface or an abstract
     *            class, but could also be a class that has been extended. This
     *            parameter will be used to verify that the object returned from
     *            the named implementation's constructor is an
     *            <code>instanceof</code> the parent interface. Then you can
     *            safely cast the returned instance to the interface class
     *            without worrying about a class cast exception.
     * @param getInstanceMethodName
     *            the name of the method that will be invoked to get the
     *            instance of an object if an object does not exist already.
     *            Normally this will be null, in which case the constructor in
     *            the named implementation class will be invoked. Set this to a
     *            non-null value to invoke a specific named method instead of
     *            the constructor to create an object. This method must be a
     *            member of the named implementation class.
     * @param constructorArgs
     *            the arguments that should be used when obtaining the instance.
     *            If <code>getInstanceMethodName</code> is null, these will be
     *            passed to the named implementation's constructor, thus the
     *            name "constructorArgs". If <code>getInstanceMethodName</code>
     *            is not null they will be passed to the method named by
     *            <code>getInstanceMethodName</code>, in which case this
     *            parameter might be better named "getInstanceMethodArgs". A
     *            null value for this parameter (no array) indicates no
     *            arguments should be passed (a zero-argument constructor or
     *            zero-argument getInstanceMethod method). Note that there
     *            should not be any elements in this array with the value
     *            <code>null</code>, as then it becomes impossible to
     *            determine the Class of that constructor argument.
     * @param isSingleton
     *            you want the objects instantiated by this factory to be
     *            treated as singletons and cached so that the same object is
     *            returned on subsequent calls to this method with the same
     *            <code>implementationClassName</code> value. Specifying
     *            <code>false</code> will prevent objects from being cached,
     *            so that the class' constructor or getInstanceMethod is called
     *            on each invocation of the factory. Use the static public
     *            variables {@link #SINGLETON},{@link #NON_SINGLETON}. The
     *            constructor arguments are not part of the cache key, so
     *            invoking this method with different
     *            <code>constructorArgs</code> but with the same
     *            <code>implementationClassName</code> and specifying
     *            <code>true</code>/<code>SINGLETON</code> for this
     *            argument will return the cached instance.
     * @return an instance of the named class. If you specified
     *         <code>true</code>/<code>SINGLETON</code> for the
     *         <code>isSingleton</code> parameter, this return value will also
     *         be cached by this factory, so additional calls to this method
     *         using the same named class will return the same instance instead
     *         of creating additional instances. In the specific factory class
     *         that extends this abstract factory, you are welcome to cast the
     *         returned object to the class <code>parentInterface</code>,
     *         since it has been verified that the object is an
     *         <code>instanceof</code> <code>parentInterface</code>.
     * @throws DMgrException
     *             if there is a problem getting the class, such as the named
     *             class not being found, the named class not having a
     *             constructor/<code>getInstanceMethodName</code> of the
     *             specified arguments, or if the named class' constructor /
     *             <code>getInstanceMethodName</code> throws an exception.
     */
    protected static Object getUntypedInstance(String implementationClassName,
            Class parentInterface, String getInstanceMethodName, Object[] constructorArgs,
            boolean isSingleton) throws DMgrException {
        Object instance = null;

        if (isSingleton) {
            if (!instanceCache.containsKey(implementationClassName)) {
                instance = createInstance(implementationClassName,
                        parentInterface, getInstanceMethodName, constructorArgs);
                instanceCache.put(implementationClassName, instance);
            }
            instance = instanceCache.get(implementationClassName);
        } else {
            instance = createInstance(implementationClassName, parentInterface,
                    getInstanceMethodName, constructorArgs);
        }
        return (instance);
    }

    /**
     * Get an instance of a class that implements the specified parentInterface.
     * This is the same as
     * {@link #getUntypedInstance(String, Class, String, Object[], boolean)},
     * except that it assumes the <code>getInstanceMethodName</code> parameter
     * is null, which effectively assumes that the named class' constructor
     * instead of a special method should be called to instantiate new objects.
     * 
     * @param implementationClassName refer to the documentation of the
     *        <code>implementationClassName</code> parameter of
     *        {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     * @param parentInterface refer to the documentation of the
     *        <code>parentInterface</code> parameter of
     *        {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     * @param constructorArgs refer to the documentation of the
     *        <code>constructorArgs</code> parameter of
     *        {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     * @param isSingleton refer to the documentation of the
     *        <code>isSingleton</code> parameter of
     *        {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     * @return refer to the documentation of the return value of
     *         {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     * @throws DMgrException refer to the <code>throws</code> documentation of
     *         {@link #getUntypedInstance(String, Class, String, Object[], boolean)}.
     */
    protected static Object getUntypedInstance(String implementationClassName,
            Class parentInterface, Object[] constructorArgs, boolean isSingleton)
            throws DMgrException {
        return getUntypedInstance(implementationClassName, parentInterface,
                null, constructorArgs, isSingleton);
    }

    private static Object createInstance(String implementationClassName,
            Class parentClass, String getInstanceMethodName, Object[] constructorArgs)
            throws DMgrException {
        final String METHOD_NAME = "createInstance"; //$NON-NLS-1$
        // this method does most of the work described above

        Object instance = null;

        Class classRef = null;
        try {
            classRef = Class.forName(implementationClassName);
        } catch (ClassNotFoundException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.classNotFound",params); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.classNotFound",params); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (Throwable t) {
            // ugh, there probably is some nasty JRE error, unchecked exception or similar
            String message = Messages.getString("wamt.AbstractFactory.unexpected",implementationClassName); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,t,"wamt.AbstractFactory.unexpected",implementationClassName); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        if (classRef == null) {
            String message = Messages.getString("wamt.AbstractFactory.nullClass");  //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,"wamt.AbstractFactory.nullClass"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }

        Class[] parameterTypes = null;
        if (constructorArgs != null) {
            parameterTypes = new Class[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                if (constructorArgs[i] != null) {
                    parameterTypes[i] = constructorArgs[i].getClass();
                } else {
                    // if you gave me a null arg, I can't get the Class
                    parameterTypes[i] = Object.class;
                }
            }
        }
        if (getInstanceMethodName == null) {
            instance = callConstructor(implementationClassName, constructorArgs, classRef,
                    parameterTypes);
        } else {
            instance = callGetInstanceMethod(implementationClassName, getInstanceMethodName, constructorArgs,  classRef,
                    parameterTypes);
        }
        if (instance == null) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.nullInstance",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,"wamt.AbstractFactory.nullInstance",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }

        if (!(parentClass.isInstance(instance))) {
        	Object[] params = {implementationClassName,parentClass.getName()};
        	String message = Messages.getString("wamt.AbstractFactory.notInstanceOf",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,"wamt.AbstractFactory.notInstanceOf",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }

        return (instance);
    }

    // Invoke a constructor to get the instance
    private static Object callConstructor(String implementationClassName,
            Object[] constructorArgs, Class classRef, Class[] parameterTypes)
            throws DMgrException {
        final String METHOD_NAME = "callConstructor"; //$NON-NLS-1$
        Constructor constructor = null;
        try {
            // look for the constructor
            constructor = classRef.getConstructor(parameterTypes);
        } catch (SecurityException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.secEx",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.secEx",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (NoSuchMethodException caught) {
        	Object[] params = {getPrintableTypes(parameterTypes)};
        	String message = Messages.getString("wamt.AbstractFactory.noConst",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.noConst",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        if (constructor == null) {
        	String message = Messages.getString("wamt.AbstractFactory.nullConst",implementationClassName);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,"wamt.AbstractFactory.nullConst",implementationClassName); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        Object instance = null;
        try {
            instance = constructor.newInstance(constructorArgs);
        } catch (InstantiationException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.callConst",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.callConst",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (IllegalAccessException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.accFail",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.accFail",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (InvocationTargetException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.constThrewEx",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.constThrewEx",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return instance;
    }

    // Invoke a method to get an instance
    private static Object callGetInstanceMethod(String implementationClassName, String getInstanceMethodName,
            Object[] methodArgs, Class classRef, Class[] parameterTypes)
            throws DMgrException {
        final String METHOD_NAME = "callGetInstanceMethod"; //$NON-NLS-1$
        Method method = null;
        try {
            // look for the method
            method =  classRef.getMethod(getInstanceMethodName, parameterTypes);
        } catch (SecurityException caught) {
        	Object[] params = {implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.secEx",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.secEx",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (NoSuchMethodException caught) {
        	Object[] params = {getInstanceMethodName,implementationClassName,getPrintableTypes(parameterTypes)};
        	String message = Messages.getString("wamt.AbstractFactory.noMethod",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.noMethod",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        if (method == null) {
        	Object[] params = {implementationClassName,getInstanceMethodName};
        	String message = Messages.getString("wamt.AbstractFactory.nullMethod",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,"wamt.AbstractFactory.nullMethod",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        Object instance = null;
        try {
            instance = method.invoke(null, methodArgs);
        } catch (IllegalAccessException caught) {
        	Object[] params = {getInstanceMethodName,implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.accFailMet",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.accFailMet",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        } catch (InvocationTargetException caught) {
        	Object[] params = {getInstanceMethodName, implementationClassName};
        	String message = Messages.getString("wamt.AbstractFactory.threwEx",params);
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            DMgrException e = new DMgrException(message,caught,"wamt.AbstractFactory.threwEx",params); //$NON-NLS-1$ //$NON-NLS-2$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw(e);
        }
        return instance;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
    		justification="<For excption message, won't impact the performance>",
    		value="SBSC_USE_STRINGBUFFER_CONCATENATION") 
    private static String getPrintableTypes(Class[] types) {
        String result = "(null)"; //$NON-NLS-1$
        if (types != null) {
            result = ""; //$NON-NLS-1$
            for (int i = 0; i < types.length; i++) {
                if (types[i] == null) {
                    result += "(null object)" + " "; //$NON-NLS-1$  //$NON-NLS-2$
                } else {
                    result += types[i].getName() + " "; //$NON-NLS-1$
                }
            }
        }
        return (result);
    }
}
