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

import java.util.Iterator;
import java.util.Vector;

/**
 * A simple collection of Strings. This is used in multiple classes in the
 * clientAPI package (task-level API) and the deviceCommunication package (AMP).
 * This class is important because it has many helper methods, such as doing
 * comparisons with subsets, non-sorted equality, etc.
 * <p>
 * WARNING: THIS CLASS IS NOT NLS ENABLED.  It should only be used to store "data" 
 * type strings that are dynamically obtained from elsewhere.  Examples would be
 * features from firmware or domain names from a device.
 * <p>
 * @version SCM ID: $Id: StringCollection.java,v 1.3 2010/08/23 21:20:28 burket Exp $
 */
//* Created on Aug 16, 2006
public class StringCollection {
    private Vector list = null;

    /**
     * A representation of any empty collection. This member is available
     * so that many instances of empty collections can all reference this
     * single collection, thus saving memory. The modification methods below
     * such as {@link #add(String)} and {@link #remove(String)} have been
     * written to disallow changes to this singleton.
     */
    public static final StringCollection EMPTY = new StringCollection();

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    static final String SCM_REVISION = "$Revision: 1.3 $"; //$NON-NLS-1$

    /**
     * Create a new collection of Strings. The collection is empty.
     */
    public StringCollection() {
        this.list = new Vector();
    }

    /**
     * Create a new collection of Strings and initialize the collection with the
     * contents of the array.
     * 
     * @param array the initial contents for the collection.
     */
    public StringCollection(String[] array) {
        this.list = new Vector();
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    continue;
                } else if (array[i].length() < 1) {
                    continue;
                }
                this.list.add(array[i]);
            }
        }
    }

    /**
     * Create a new collection of Strings that is a clone. The new collection is
     * a separate collection, but it references all the same String objects of
     * the original (clonable) collection.
     * 
     * @param clonable the original collection to make a copy (clone) of.
     */
    public StringCollection(StringCollection clonable) {
        this.list = new Vector();
        this.list.addAll(clonable.list);
    }

    /**
     * Create a new StringCollection that is a merged instance of two other
     * collections. It will eliminate duplicate values. It does not modify the
     * two original collections (<code>collection1</code> and
     * <code>collection2</code>).
     * 
     * @param collection1 a collection of Strings to be included in the merged
     *        instance
     * @param collection2 a collection of Strings to be included in the merged
     *        instance
     */
    public StringCollection(StringCollection collection1,
            StringCollection collection2) {
        this.list = new Vector();
        if (collection1 != null) {
            Iterator iterator = collection1.list.iterator();
            while (iterator.hasNext()) {
                String element = (String) iterator.next();
                if (!this.contains(element)) {
                    this.add(element);
                }
            }
        }
        if (collection2 != null) {
            Iterator iterator = collection2.list.iterator();
            while (iterator.hasNext()) {
                String element = (String) iterator.next();
                if (!this.contains(element)) {
                    this.add(element);
                }
            }
        }
    }

    /**
     * Get the number of Strings currently in this collection.
     * 
     * @return the number of Strings currently in this collection.
     */
    public synchronized int size() {
        return (this.list.size());
    }

    /**
     * Get the String at the specified index in the collection.
     * 
     * @param index
     *            the index into the collection that locates the desired String
     * @return the String at the specified index in the collection
     */
    public synchronized String get(int index) {
        return ((String) this.list.get(index));
    }

    /**
     * Get a String representation of the collection. Each string is separated
     * by the specified delimiter.
     * 
     * @param delimiter The delimiter for each String in the collection that is
     *       returned in the aggregate string
     * @return all the Strings in the collection seperated by delimiter
     * @throws DMgrException
     */
    public synchronized String marshalToString(char delimiter)
            throws DMgrException {
        String ret_s = ""; //$NON-NLS-1$
        StringBuffer buf = new StringBuffer("");
        for (int i = 0; i < this.list.size(); i++) {
            String s = (String) this.list.get(i);
            if (s.indexOf(delimiter) < 0) {
            	buf.append(s + delimiter); //ret_s = ret_s + s + delimiter;
            } else {
            	Object[] args = new Object[] {s, Character.valueOf(delimiter)};
                throw new DMgrException(Messages.getString("wamt.StringCollection.strDelimiter", args),"wamt.StringCollection.strDelimiter", args); //$NON-NLS-1$
            }
        }
        ret_s = buf.toString();
        return ret_s;
    }

    /**
     * Create a StringCollection instance based on a string created by
     * marshalToString.
     * 
     * @param s The string created by marshalToString
     * @param delimiter The delimiter passed to marshalToString
     * @return an all the Strings in the collection seperated by delimiter
     */
    public static final StringCollection createCollectionFromString(String s,
            char delimiter) {
        char[] ca = { delimiter };
        String regExp = new String(ca);
        String[] sa = s.split(regExp, 0);
        return new StringCollection(sa);
    }

    /**
     * Add a new String into the collection. It is added to the end of the
     * collection.
     * 
     * @param string
     *            the String to add to the collection.
     */
    public synchronized void add(String string) {
        if (this != EMPTY) {
            if (string == null) {
                return;
            } else if (string.length() == 0) {
                return;
            }
            this.list.add(string);
        }
    }

    /**
     * Remove the specified String object from the collection. 
     * 
     * @param string the String object to remove from the collection.
     * @see #contains(String)
     */
    public synchronized void remove(String string) {
        if (this != EMPTY) {
            if (this.contains(string)) {
                this.list.remove(string);
            }
        }
    }

    /**
     * Check if the collection contains the specified String. When looking in
     * the collection for the desired String, the <code>String.equals</code>
     * method is invoked instead of <code>==</code>, which means it will find
     * Strings of the same content even if they are different object instances.
     * 
     * @param target
     *            look for a String in the collection that has this contents
     * @return true if the same contents were found, false otherwise
     */
    public synchronized boolean contains(String target) {
        boolean result = false;
        for (int i = 0; i < this.list.size(); i++) {
            if (this.list.get(i).equals(target)) {
                result = true;
                break;
            }
        }
        return (result);
    }

    /**
     * Check if the two collections have the same content. It will walk through
     * each each String in this collection and verify that the exact String is
     * in the other collection. The two collections may not be a superset or
     * subset of each other, they must contain the same Strings of the same
     * values. When we say "same Strings" it means <code>.equals()</code>
     * equivalence, not <code>==</code> equivalence. Also, sort order of the
     * collections is irrevelant, meaning that equivalency is not dependent on
     * the order of the Strings in the collection.
     * 
     * @param that
     *            the other StringCollection to compare equivalency to
     *            <code>this</code>.
     * @return true if the two collections are equivalent, false otherwise.
     */
    public synchronized boolean equals(Object that) {
    	if(!(that instanceof StringCollection)) return false;
    	
    	StringCollection otherStringCollection = (StringCollection) that;
        /*
         * This method is synchronized because we don't want other threads
         * adding Strings to the collection in the middle of this comparison.
         */
        if (this.size() != otherStringCollection.size()) {
            return (false);
        }
        Iterator iterator = this.list.iterator();
        while (iterator.hasNext()) {
            String value = (String) iterator.next();
            if (!otherStringCollection.contains(value)) {
                return (false);
            }
        }
        return (true);
    }
    
    // Added for Findbugs
    public int hashCode(){
    	int hashCode = 0;
    	for (Object entry : list)
		{
			hashCode += entry.hashCode();
		}
    	return hashCode;
    }

    /**
     * Check if one collection (this) is a superset of another collection
     * (that). In other words, verify that the first collection (this) contains
     * all the elements that are in the second collection (that). This is order
     * independent for both collections. The isSupersetOf test is true if the
     * two collections are equal, meaning that the first collection does not
     * need to have a greater number of elements than the second collection.
     * 
     * @param that
     *            the collection which is a subset of <code>this</code>
     * @return true if the first collection is a superset of the second
     *         collection, false otherwise.
     */
    public synchronized boolean isSupersetOf(StringCollection that) {
        // make sure all elements of that are in this
        Iterator iterator = that.list.iterator();
        while (iterator.hasNext()) {
            String value = (String) iterator.next();
            if (!this.contains(value)) {
                return (false);
            }
        }
        return (true);
    }

    /**
     * Create a human-readable representation of this collection. This is intended
     * more for debugging than consumption by end users.
     * 
     * @return a human-readable representation of this collection.
     * @see #getDisplayName()
     */
    public synchronized String toString() {
        StringBuffer buf = new StringBuffer("StringCollection["); //String result = "StringCollection["; //$NON-NLS-1$
        for (int i = 0; i < this.list.size(); i++) {
            String s = (String) this.list.get(i);
            if (s != null) {
                if (s.length() == 0) {
                    buf.append("(empty)");// result += "(empty)"; //$NON-NLS-1$
                } else {
                    buf.append(s); //result += s;
                }
            } else {
                buf.append("(null)"); //result += "(null)"; //$NON-NLS-1$
            }
            if (i + 1 < this.list.size()) {
                buf.append(",");//result += ","; //$NON-NLS-1$
            }
        }
        buf.append("]");//result += "]"; //$NON-NLS-1$
        
        return (buf.toString());
    }

    static final String getDisplayName_SEPARATOR = ", "; //$NON-NLS-1$
    
    /**
     * Create a human-readable representation of this collection that is
     * intended for consumption by end users.
     * 
     * @return a human-readable representation of this collection that is
     *         intended for consumption by end users.
     * @see #createCollectionFromDisplayName(String)
     */
    public synchronized String getDisplayName() {
        String result = ""; //$NON-NLS-1$
        StringBuffer buf = new StringBuffer("");
        for (int i = 0; i < this.list.size(); i++) {
            String element = (String) this.list.get(i);
            if (buf.length() > 0) {
            	buf.append(getDisplayName_SEPARATOR); //result += getDisplayName_SEPARATOR;
            }
            buf.append(element); //result += element;
        }
        result = buf.toString();

        if (this.list.size() == 0) {
            result = ""; //$NON-NLS-1$
        }
        return (result);
    }

    /**
     * Create a StringCollection instance based on a string created by
     * getDisplayName.
     * 
     * @param s The string created by getDisplayName. Since getDisplayName
     *        separates members by ", ", embedded sequences of ", " in a member
     *        <em>will</em> result in the member being split into 2 members.
     * @return The StringCollection created from s
     * @see #getDisplayName()
     */
    public static final StringCollection createCollectionFromDisplayName(String s) {
        String regExp = getDisplayName_SEPARATOR;
        String[] sa = s.split(regExp, 0);
        return new StringCollection(sa);
    }
}
