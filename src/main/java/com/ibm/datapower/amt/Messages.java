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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Class for accessing NLS (National Language Support) enabled messages in the
 * IBM Appliance Management Toolkit.
 * 
 *
 */
//* Created on Jul 2, 2007
public class Messages {
    
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    static final String SCM_REVISION = "$Revision: 1.4 $"; //$NON-NLS-1$
    
    public static final String BUNDLE_NAME = "com.ibm.datapower.amt.messages"; //$NON-NLS-1$
    public static final String NONMSG_BUNDLE_NAME = "com.ibm.datapower.amt.text"; //$NON-NLS-1$

    protected static final String CLASS_NAME = Messages.class.getName();

    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    private static String prefix = "WAMT";
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    private Messages() {
    }
    
    public static void setMessagePrefix(String inPrefix) {
    	prefix = inPrefix;
    }
    
    /**
	 * Returns an String from a bundle.
	 * 
	 * @param bundle
	 *            The bundle to get the string from
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    public static String getString(ResourceBundle bundle, String key) {
        String METHOD_NAME = "getString(ResourceBundle bundle, String key)"; //$NON-NLS-1$
        String msg = ""; //$NON-NLS-1$
        try {
            msg = prefix + bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Exception caught", e); //$NON-NLS-1$
            msg = "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return msg;
    }

    /**
	 * Return a String from the message bundle.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    public static String getString(String key) {
        return getString(ResourceBundle.getBundle(BUNDLE_NAME),key);
    }

    /**
	 * Return a String from the message bundle, based on a locale.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    public static String getString(String key, Locale locale) {
        return getString(ResourceBundle.getBundle(BUNDLE_NAME, locale),key);
    }

    /**
	 * Return a String from the message bundle, based on a locale, with
	 * message parameters substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @param args
	 *            The array of parameters to be substituted in the message.
	 * @return The string from the bundle with the message parameters
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getString(String key, Locale locale,
            Object[] args) {
        String msg = getString(key, locale);
        return MessageFormat.format(msg, args);
    }
    
    /**
	 * Return a String from the message bundle, using the default locale,
	 * with message parameters substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param args
	 *            The array of parameters to be substituted in the message.
	 * @return The string from the bundle with the message parameters
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getString(String key, 
            Object[] args) {
        String msg = getString(key);
        return MessageFormat.format(msg, args);
    }

    /**
	 * Return a String from the message bundle, based on a locale, with a
	 * single message parameter substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @param arg
	 *            The single parameter to be substituted in the message.
	 * @return The string from the bundle with the message parameter
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getString(String key, Locale locale, Object arg) {
        Object[] args = { arg };
        return getString(key, locale, args);
    }
    
    /**
	 * Return a String from the message bundle, using the default locale,
	 * with a single message parameter substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param arg
	 *            The single parameter to be substituted in the message.
	 * @return The string from the bundle with the message parameter
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getString(String key, Object arg) {
        Object[] args = { arg };
        return getString(key, args);
    }
    
    /**
	 * Return a String from the non-message text bundle.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    public static String getNonMsgString(String key) {
        return getTextMsgString(ResourceBundle.getBundle(NONMSG_BUNDLE_NAME),key);
    }

    /**
	 * Return a String from the non-message text bundle, based on a locale.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    public static String getNonMsgString(String key, Locale locale) {
        return getTextMsgString(ResourceBundle.getBundle(NONMSG_BUNDLE_NAME, locale),key);
    }

    /**
	 * Return a String from the non-message text bundle, based on a locale,
	 * with text parameters substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @param args
	 *            The array of parameters to be substituted in the text.
	 * @return The string from the bundle with the text parameters
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getNonMsgString(String key, Locale locale,
            Object[] args) {
        String msg = getNonMsgString(key, locale);
        return MessageFormat.format(msg, args);
    }
   
    /**
	 * Return a String from the non-message text bundle, using the default
	 * locale, with text parameters substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param args
	 *            The array of parameters to be substituted in the text.
	 * @return The string from the bundle with the text parameters
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getNonMsgString(String key,
            Object[] args) {
        String msg = getNonMsgString(key);
        return MessageFormat.format(msg, args);
    }

    /**
	 * Return a String from the non-message text bundle, based on a locale,
	 * with a single text parameter substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param locale
	 *            The locale for the string to be returned.
	 * @param arg
	 *            The single parameter to be substituted in the text.
	 * @return The string from the bundle with the text parameter
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getNonMsgString(String key, Locale locale, Object arg) {
        Object[] args = { arg };
        return getNonMsgString(key, locale, args);
    }
    
    /**
	 * Return a String from the non-message text bundle, using the default
	 * locale, with a single text parameter substituted.
	 * 
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @param arg
	 *            The single parameter to be substituted in the text.
	 * @return The string from the bundle with the text parameter
	 *         substituted, or error text if the key is not found
	 *         in the bundle.
	 */
    public static final String getNonMsgString(String key, Object arg) {
        Object[] args = { arg };
        return getNonMsgString(key, args);
    }
    
    /*
	 * Returns the explanation or useraction from a bundle without
	 * any prefix information
	 * 
	 * @param key
	 *            The key used to access the string in the bundle
	 * @param locale
	 *             The locale for the string to be returned.
	 *            
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    static String getStringNoPrefix(String key, Locale locale) {
        String METHOD_NAME = "getString(String key)"; //$NON-NLS-1$
        String msg = ""; //$NON-NLS-1$
        ResourceBundle bundle = null;
        if (locale!= null){
        	bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        }
        else{
        	bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        }
        try {
            msg = bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Exception caught", e); //$NON-NLS-1$
            msg = "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return msg;
    }
    
    /*
	 * Returns an String from the non-message text bundle.  For internal use only.
	 * 
	 * @param bundle
	 *            The bundle to get the string from - text.properties
	 * @param key
	 *            The key used to access the string in the bundle.
	 * @return The string from the bundle, or error text if the key
	 *         is not found in the bundle.
	 */
    private static String getTextMsgString(ResourceBundle bundle, String key) {
        String METHOD_NAME = "getString(ResourceBundle bundle, String key)"; //$NON-NLS-1$
        String msg = ""; //$NON-NLS-1$
        try {
            msg = bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                    "Exception caught", e); //$NON-NLS-1$
            msg = "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return msg;
    }

}
