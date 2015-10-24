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
import java.util.logging.Logger;

import com.heliosapm.aop.retransformer.Instrumented;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * <p>Title: AbstractTransformer</p>
 * <p>Description: Base transformer class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.transformers.AbstractTransformer</code></p>
 * @param <T> The type of the transformer's directive set
 */

public abstract class AbstractTransformer<T> implements ITransformer<T> {
	/** Empty index map const */
	public static final Map<CtBehavior, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<CtBehavior, String>(0));
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	
	/**
	 * Indexes the passed source map by finding the javassist CtBehaviors identified in the keys of the source map
	 * @param ct The CtClass to index from
	 * @param sourceMap The source map to index
	 * @return A map of the source transforms keyed by the javassist CtBehavior it is linked to
	 */
	public Map<CtBehavior, String> indexSourceMap(final CtClass ct, final Map<String, String> sourceMap) {
		return indexSourceMap(false, ct, sourceMap);
	}
	
	/**
	 * Indexes the passed source map by finding the javassist CtBehaviors identified in the keys of the source map
	 * @param ignoreNotFounds true to ignore any failing behavior lookups, false otherwise
	 * @param ct The CtClass to index from
	 * @param sourceMap The source map to index
	 * @return A map of the source transforms keyed by the javassist CtBehavior it is linked to
	 */
	public Map<CtBehavior, String> indexSourceMap(final boolean ignoreNotFounds, final CtClass ct, final Map<String, String> sourceMap) {
		if(sourceMap==null || sourceMap.isEmpty()) return EMPTY_MAP;
		if(ct==null) throw new IllegalArgumentException("The passed CtClass was null");
		final String ctName = ct.getSimpleName();
		final Map<CtBehavior, String> index = new HashMap<CtBehavior, String>(sourceMap.size());
		for(Map.Entry<String, String> entry: sourceMap.entrySet()) {
			try {
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
				final CtBehavior ctBehavior;
				if(ctName.equals(behName)) {
					ctBehavior = findCtor(ct, descriptor);
				} else {
					ctBehavior = findMethod(ct, behName, descriptor);
				}
				index.put(ctBehavior, entry.getValue());
			} catch (Exception ex) {
				if(!ignoreNotFounds) throw new RuntimeException("Failed to process source map for CtClass [" + ct.getName() + "]", ex);
			}
		}
		return index;
	}
	
	/**
	 * Finds a constructor in the passed ctclass
	 * @param ct The ctclass to get the constructor for
	 * @param descriptor The optional descriptor. If not supplied, the first located constructor will be returned.
	 * @return the located constructor
	 */
	public CtConstructor findCtor(final CtClass ct, final String descriptor) {
		if(ct==null) throw new IllegalArgumentException("The passed CtClass was null");
		try {
			if(descriptor!=null) return ct.getConstructor(descriptor);
			final LinkedHashSet<CtConstructor> ctors = new LinkedHashSet<CtConstructor>(Arrays.asList(ct.getDeclaredConstructors()));
			Collections.addAll(ctors, ct.getConstructors());
			return ctors.iterator().next();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to find ctor for [" + ct.getName() + "]", ex);
		}
	}

	/**
	 * Finds a method in the passed ctclass
	 * @param ct The ctclass to get the method for
	 * @param methodName The name of the method to find
	 * @param descriptor The optional descriptor. If not supplied, the first located constructor will be returned.
	 * @return the located constructor
	 */
	public CtMethod findMethod(final CtClass ct, final String methodName, final String descriptor) {
		if(ct==null) throw new IllegalArgumentException("The passed CtClass was null");
		if(methodName==null || methodName.trim().isEmpty()) throw new IllegalArgumentException("The passed methodName was null or empty");
		final String _name = methodName.trim();
		try {
			if(descriptor!=null) return ct.getMethod(_name, descriptor);
			for(CtMethod m: ct.getMethods()) {
				if(_name.equals(m.getName())) return m;
			}
			throw new Exception("No method found");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to find method [" + _name + "] for [" + ct.getName() + "]", ex);
		}
	}
	
	/**
	 * Applies a retransformer annotation to the passed CtClass to indicate that it has been retransformed
	 * @param ct The CtClass to annotate
	 */
	protected void annotate(final CtClass ct) {
		ConstPool constpool = ct.getClassFile().getConstPool();
		AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
		javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(Instrumented.class.getName(), constpool);
		StringMemberValue smv = new StringMemberValue("Source", constpool);
		annot.addMemberValue("mockProvider", smv);
		LongMemberValue timestamp = new LongMemberValue(System.currentTimeMillis(), constpool);
		annot.addMemberValue("instrumentedTime", timestamp);
		attr.addAnnotation(annot);	
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.aop.retransformer.transformers.ITransformer#isStrict()
	 */
	@Override
	public abstract boolean isStrict();
	
	

	
	/**
	 * Creates a new AbstractTransformerfinal Map<String, String> sourceMap
	 */
	protected AbstractTransformer() {}

}
