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

import java.util.Set;

import com.ibm.datapower.amt.dataAPI.AlreadyExistsInRepositoryException;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.dataAPI.DirtySaveException;

public interface Taggable {

	/**
	 * <p>
	 * Add a new tag.
	 * <p>
	 * <ul>
	 * <li>The tag name is required, parameter value can be null, empty string or a string.</li>
	 * <li>The value can be null which is different to an empty string that can be explicitly set</li>
	 * <li>The combination of name and value uniquely identifies a tag per resource</li>
	 * <li>Different resources (Device or Domain) can have the same tag name and value</li> 
	 * <li>Adding a tag with an existing name to a resource with a different value adds a second tag</li>
	 * <li>Adding a tag with an existing name to a resource with the same value does not add a second tag and does not error</li>
	 * </ul>
	 * 
	 * @param name the tag name
	 * @param value the tag value
	 */
	public abstract void addTag(String name, String value) throws DeletedException, AlreadyExistsInRepositoryException, DatastoreException, InvalidParameterException;

	/**
	 * <p>
	 * Remove the exactly matching tag. no error if you try to remove a tag that isn't there
	 * 
	 * @param name the tag name
	 * @param value the tag value
	 */
	public abstract void removeTag(String name, String value) throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException;

	/**
	 * <p>
	 * Remove all tags with the provided name. no error if you try to remove a tag that isn't there
	 * 
	 * @param name the tag name
	 */
	public abstract void removeTag(String name) throws DeletedException, DirtySaveException, DatastoreException, InvalidParameterException;

	/**
	 * <p>
	 * Remove all the tags from the resource. No error if none exist
	 */
	public abstract void removeTags() throws DeletedException, DirtySaveException, DatastoreException;

	/**
	 * <p>
	 * Get all of the tag names on the resource. return an empty set if no tags have been added
	 * 
	 * @return the set of tag name
	 */
	public abstract Set<String> getTagNames() throws DeletedException;

	/**
	 * <p>
	 * Get the values for a given tag name. return an empty set if the resource hasn't been tagged with the requested tag name
	 * 
	 * @param name the tag name
	 * 
	 * @return the set of tag name
	 */
	public abstract Set<String> getTagValues(String name) throws DeletedException, InvalidParameterException;

}
