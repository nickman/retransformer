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
package com.heliosapm.aop.retransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;

import javassist.CtClass;

/**
 * <p>Title: RetransformChain</p>
 * <p>Description: Supports the chaining of multiple retransformers to execute a series of sequential retransforms
 * on target classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.RetransformChain</code></p>
 */

public class RetransformChain {
	/** Indicates if we're executing in a chain */
	private static final ThreadLocal<Boolean> inChain = new ThreadLocal<Boolean>();

	/** The Retransformer instance */
	protected final Retransformer retran;
	/** The ct classes for the target transforms keyed by the classes we're going to transform */
	protected final Map<Class<?>, CtClass> targetClasses = new HashMap<Class<?>, CtClass>();
	
	
	/**
	 * Indicates if we're executing in a chain
	 * @return true if we're executing in a chain, false otherwise
	 */
	public static boolean isInChain() {
		final Boolean b = inChain.get();
		return b==null ? false : b.booleanValue();
	}
	
	/**
	 * Returns a new RetransformChain
	 * @return a new RetransformChain
	 */
	public static RetransformChain chain() {
		return new RetransformChain();
	}
	
	
	/**
	 * Creates a new RetransformChain
	 */
	private RetransformChain() {
		retran = Retransformer.getInstance();
	}
	
	/**
	 * Retransforms the passed class with the built retransform chain
	 * @param targetClass The class to transform
	 */
	public void retransform(final Class<?> targetClass) {
		//
	}
	
	public void retransform(final Reflections reflections, final Object...params) {
		
	}
	
	/**
	 * Executes a mock class method replacement transformation
	 * @param mockedClass The source of the mocked methods to inject into the target
	 * @return this chain
	 */
	public RetransformChain transform(final Class<?> mockedClass) {

		return this;
	}
	
	/**
	 * TODO: DOCUMENT ME
	 * Convenience method to execute a single provided source method replacement transformation
	 * @param methodName The method name to transform
	 * @param source  The source of the transformed method
	 * @return this chain
	 */
	public RetransformChain transform(final String methodName, final String source) {		
		transform(true, Collections.singletonMap(methodName, source));
		
		return this;
	}
	
	/**
	 * TODO: DOCUMENT ME
	 * Convenience method to execute a single provided source method replacement transformation
	 * @param methodName The method name to transform
	 * @param argCount The number of arguments in the target method
	 * @param source  The source of the transformed method
	 * @return this chain
	 */
	public RetransformChain transform(final String methodName, final int argCount, final String source) {		
		transform(true, Collections.singletonMap(methodName, source));
		
		return this;
	}
	
	/**
	 * TODO: DOCUMENT ME
	 * Convenience method to execute a single provided source method replacement transformation
	 * @param methodName The method name to transform
	 * @param descriptor The target method signature
	 * @param source  The source of the transformed method
	 * @return this chain
	 */
	public RetransformChain transform(final String methodName, final String descriptor, final String source) {		
		transform(true, Collections.singletonMap(methodName, source));
		
		return this;
	}
	
	/**
	 * Executes a provided source method replacement transformation
	 * @param failOnNotFound If true, any not found method will throw. Otherwise, if some (but not all) methods are not found, they are ignored.
	 * @param sourceMap A map of source code replacements keyed by the method descriptor of the methods to replace
	 * @return this chain
	 */
	public RetransformChain transform(final boolean failOnNotFound, final Map<String, String> sourceMap) {
		
		return this;
	}
	
	/**
	 * Applies an annotation builder to this transform chain
	 * @param annotationBuilder The annotation builder to apply
	 * @return this chain
	 */
	public RetransformChain transform(final AnnotationBuilder annotationBuilder) {
		
		return this;
	}
	
	
	

}
