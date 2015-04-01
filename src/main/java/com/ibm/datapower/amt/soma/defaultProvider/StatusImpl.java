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
package com.ibm.datapower.amt.soma.defaultProvider;

import java.util.HashMap;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.soma.Status;

public class StatusImpl implements Status {
	private StringCollection names = new StringCollection();
	private HashMap<String, Object> hashMap = new HashMap<String, Object>();
	
	public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;
	
	public StatusImpl() {
	}
	
	public StringCollection getNames() {
		return this.names;
	}

	public Object getValue(String name) {		
		return hashMap.get(name);
	}
	
	void add(String name, Object object) {
		this.names.add(name);
		this.hashMap.put(name, object);
	}
}
