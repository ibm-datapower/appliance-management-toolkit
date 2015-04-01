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

import java.util.Locale;

/**
 * This is the root class for exceptions that are thrown from the DataPower
 * Management Interface. All other exception classes in the manager should be a subclass
 * of this. It is recommended that you use the most specific class whenever
 * possible, so instances of this specific class should be rare. Please
 * instantiate subclasses instead.
 * <p>
 * To support getting an NLS message from the exception, this class has been
 * modified to have NLS message attributes (msgId and msgParams). If these
 * attributes are present, they will be used in returning localized messages
 * (@see #getMessage(Locale) ). The normal constructors that pass messages have
 * been preserved for compatibility uses, but they should be avoided if possible.
 * @version SCM ID: $Id: DMgrException.java,v 1.3 2010/08/23 21:20:28 burket Exp $
 * 
 * <p>
 */
public class DMgrException extends Exception {

    /**
     * Version 1 did not have the NLS members msgKey and msgParms, Version 2
     * does. If you change this value in this class, then you probably want to
     * change the value in all the other classes that extend this one.
     */
    private static final long serialVersionUID = 2L;

    public static final String COPYRIGHT_2009_2010 = Constants.COPYRIGHT_2009_2010;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$

    private String msgKey = null;

    private Object[] msgParms = null;

    /**
     * Create an exception with no message and no cause. Please use one of the
     * other constructors to provide as much information about the exception as
     * possible. You really shouldn't be using this zero-argument constructor.
     */
    public DMgrException() {
        super();
    }

    /**
     * Create an exception with a message but with no chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     */
    public DMgrException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParms an Object[] that gives the attributes to be used with
     *        msgKey
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, String msgKey, Object[] msgParms) {
        super(message);
        this.msgKey = msgKey;
        if ( msgParms != null )
        	this.msgParms = msgParms.clone();
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParm an Object that gives the attributes to be used with msgKey
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, String msgKey, Object msgParm) {
        super(message);
        this.msgKey = msgKey;
        Object[] p = {msgParm};
        this.msgParms = p;
    }

    /**
     * Create an exception with a message and NLS attributes, but with no
     * chained cause.
     * 
     * @param message a string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, String msgKey) {
        super(message);
        this.msgKey = msgKey;
    }

    /**
     * Create an exception with a message and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public DMgrException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParms an Object[] that gives the attributes to be used with
     *        msgKey
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, Throwable cause, String msgKey, Object[] msgParms) {
        super(message, cause);
        this.msgKey = msgKey;
        if ( msgParms != null )
        	this.msgParms = msgParms.clone();
    }
    
    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @param msgParm an Object that gives the attribute to be used with msgKey
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, Throwable cause, String msgKey, Object msgParm) {
        super(message, cause);
        this.msgKey = msgKey;
        Object[] p = {msgParm};
        this.msgParms = p;
    }
    
    /**
     * Create an exception with a message, NLS attributes and a chained cause.
     * 
     * @param message string that provides some explanation/description of the
     *        exception. This message should have already been localized for the
     *        jvm's locale, if possible.
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     * @param msgKey a string giving the key of the NLS enabled message to be
     *        used with the exception.
     * @see #getMessage(Locale)
     */
    public DMgrException(String message, Throwable cause, String msgKey) {
        super(message, cause);
        this.msgKey = msgKey;
    }

    /**
     * Create an exception with a chained cause and no message.
     * 
     * @param cause a reference to another exception that occurred at a lower
     *        level which is the cause for this higher-level exception.
     */
    public DMgrException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Return a message for this exception based on locale. If the NLS
     * attributes (msgKey and msgParms) aren't set, this method will return
     * getLocalizedMessage(), which will probably be the same as getMessage().
     * This method is provided for callers who need to get message that matches
     * the local of their users. This is important, because the manager sends much of
     * its user feedback messages via exceptions.
     * 
     * @param locale The locale to be used for the localizedMessage
     * @return message for this exception based on locale
     */
    public String getMessage(Locale locale) {
        if (this.msgKey != null) {
            if (this.msgParms != null) {
                return Messages.getString(this.msgKey, locale, this.msgParms);
            } else {
                return Messages.getString(this.msgKey, locale);
            }
        } else {
            return getLocalizedMessage(); // probably same as getMessage()
        }
    }
    
    /**
     * Return the explanation text for this exception message
     * @return explanation the text of the explanation specified for this exception
     */    
    public String getMessageExplanation(){
  	   String explanation = "";
        explanation = Messages.getStringNoPrefix(this.msgKey+".explanation", null);       
  	   return explanation;
     }

    /**
     * Return the uesraction text for this exception message
     * @return useraction the text of the user action specified for this exception
     */     
    public String getMessageUseraction(){
   	   String useraction = "";
         useraction = Messages.getStringNoPrefix(this.msgKey+".useraction", null);       
   	   return useraction;
    }
    
    /**
     * Returns the explanation text for this exception message
     * @param locale  the locale for the string to be returned. 
     * @return explanation the text of the message explanation in the bundle
     */
    public String getMessageExplanation(Locale locale){
   	   String explanation = "";
        explanation = Messages.getStringNoPrefix(this.msgKey+".explanation", locale);       
   	   return explanation;
      }

    /**
     * Returns the useraction text for this exception message
     * @param locale  the locale for the string to be returned. 
     * @return useraction the text of the message explanation in the bundle
     */    
    public String getMessageUseraction(Locale locale){
    	String useraction = "";
    	useraction = Messages.getStringNoPrefix(this.msgKey+".useraction", locale);       
    	return useraction;
    }

    
    
}
