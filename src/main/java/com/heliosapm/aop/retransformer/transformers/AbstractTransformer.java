/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.aop.retransformer.transformers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

/**
 * <p>Title: AbstractTransformer</p>
 * <p>Description: Base transformer class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.transformers.AbstractTransformer</code></p>
 */

public abstract class AbstractTransformer {
	/** Empty index map const */
	public static final Map<CtBehavior, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<CtBehavior, String>(0));
	
	
	public Map<CtBehavior, String> indexSourceMap(final CtClass ct, final Map<String, String> sourceMap) {
		if(sourceMap==null || sourceMap.isEmpty()) return EMPTY_MAP;
		final String ctName = ct.getSimpleName();
		final Map<CtBehavior, String> index = new HashMap<CtBehavior, String>(sourceMap.size());
		for(Map.Entry<String, String> entry: sourceMap.entrySet()) {
			String key = entry.getKey();								
			String behName = null;
			String descriptor = null;
			int ind = key.indexOf(':');
			if(ind==-1) {
				behName = key.trim();
				descriptor = null;
			} else {
				behName = key.substring(0, ind).trim();
				descriptor = key.substring(ind+1).trim();									
			}								
			if(ctName.equals(behName)) {
				// Constructor
			} else {
				// Method
			}
		}
		return index;
	}
	
	/**
	 * Finds a constructor in the passed ctclass
	 * @param ct The ctclass to get the constructor for
	 * @param descriptor The optional descriptor. If not supplied, the first located constructor will be returned.
	 * @return the located constructor
	 * @throws NotFoundException thrown if no matching constructor is found for the passed descriptor
	 */
	public CtConstructor findCtor(final CtClass ct, final String descriptor) throws NotFoundException {
		if(descriptor!=null) return ct.getConstructor(descriptor);
		final LinkedHashSet<CtConstructor> ctors = new LinkedHashSet<CtConstructor>(Arrays.asList(ct.getDeclaredConstructors()));
		Collections.addAll(ctors, ct.getConstructors());
		return ctors.iterator().next();		
	}
	
	/**
	 * Creates a new AbstractTransformerfinal Map<String, String> sourceMap
	 */
	protected AbstractTransformer() {}

}
