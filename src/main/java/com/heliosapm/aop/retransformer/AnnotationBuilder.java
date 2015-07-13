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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javassist.bytecode.annotation.MemberValue;

/**
 * <p>Title: AnnotationBuilder</p>
 * <p>Description: Fluent style annotation builder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.AnnotationBuilder</code></p>
 */

public class AnnotationBuilder {
	/** A map of named member values keyed by the element type */
	protected final EnumMap<ElementType, Map<String, MemberValue>> members = new EnumMap<ElementType, Map<String, MemberValue>>(ElementType.class);
	/** The indexed meta-data of the annotation type */
	protected final EnumMap<ElementType, Map<String, MemberDef<?>>> index = new EnumMap<ElementType, Map<String, MemberDef<?>>>(ElementType.class);
	/** A set of the member names on the annotation type */
	protected final Set<String> memberNames;

	/**
	 * Creates a new AnnotationBuilder
	 * @param annotationClassName The class name of the annotation to build
	 * @param classLoader The optional class loader
	 * @return an annotation builder
	 */
	public static AnnotationBuilder newBuilder(final String annotationClassName, final ClassLoader classLoader) {
		if(annotationClassName==null || annotationClassName.trim().isEmpty()) throw new IllegalArgumentException("The passed class name was null or empty");
		try {
			@SuppressWarnings("unchecked")
			Class<Annotation> annotationClass = (Class<Annotation>) (classLoader==null ?
					Class.forName(annotationClassName, true, Thread.currentThread().getContextClassLoader()) :
						Class.forName(annotationClassName, true, classLoader));
			return new AnnotationBuilder(annotationClass);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load class [" + annotationClassName + "]", ex);
		}
	}

	/**
	 * Creates a new AnnotationBuilder loading the annotation class using the calling thread's context classloader
	 * @param annotationClassName The class name of the annotation to build
	 * @return an annotation builder
	 */
	public static AnnotationBuilder newBuilder(final String annotationClassName) {
		return newBuilder(annotationClassName, null);
	}
	
	/**
	 * Creates a new AnnotationBuilder for the passed annotation type
	 * @param annotationClass The annotation type
	 * @return an annotation builder
	 */
	public static AnnotationBuilder newBuilder(final Class<? extends Annotation> annotationClass) {
		return new AnnotationBuilder(annotationClass);
	}
	
	
	/**
	 * Creates a new AnnotationBuilder
	 * @param annotationClass The annotation class to build
	 */
	private AnnotationBuilder(final Class<? extends Annotation> annotationClass) {
		for(ElementType et: ElementType.values()) {
			members.put(et, new HashMap<String, MemberValue>());
			index.put(et, new HashMap<String, MemberDef<?>>());
		}
		final Method[] methods = annotationClass.getDeclaredMethods();
		final Set<String> mn = new HashSet<String>(methods.length);
		for(Method m: methods) {
			mn.add(m.getName());
		}
		memberNames = Collections.unmodifiableSet(mn);
		log("Member Names: " + memberNames);
		final MemberDef<?>[] defs = MemberDef.getMemberDefs(annotationClass);
		final Target target = annotationClass.getAnnotation(Target.class);
		if(target!=null) {
			for(ElementType et: target.value()) {
				
			}
		}
	}
	
	public static void main(String[] agrs) {
		log("Quickie Test");
//		AnnotationBuilder ab = newBuilder(SuppressWarnings.class);
		AnnotationBuilder ab = newBuilder("javax.xml.ws.soap.Addressing");
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	protected static class MemberDef<T> {
		/** The member name */
		final String name;
		/** The annotation member type */
		final Class<T> type;
		/** The annotation member default value */
		final T value;
		
		/**
		 * Returns an array of member definitions for the passed annotation type
		 * @param type The annotation type
		 * @return the array of member definitions
		 */
		public static <T extends Annotation> MemberDef<?>[] getMemberDefs(final Class<T> type) {
			final Method[] methods = type.getDeclaredMethods();
			final Set<MemberDef<?>> defs = new LinkedHashSet<MemberDef<?>>(methods.length);
			for(Method method: methods) {
				defs.add(new MemberDef(method.getName(), method.getReturnType(), method.getDefaultValue()));
			}
			return defs.toArray(new MemberDef[methods.length]);
		}
		
		/**
		 * Creates a new MemberDef
		 * @param name The member name
		 * @param type The annotation member type
		 * @param value The annotation member default value
		 */
		public MemberDef(final String name, final Class<T> type, final T value) {
			this.name = name;
			this.type = type;
			this.value = value;
		}
		
		
		
		/**
		 * Returns the member name
		 * @return the name
		 */
		public String getName() {
			return name;
		}



		/**
		 * Returns the annotation member type
		 * @return the annotation member type
		 */
		public Class<T> getType() {
			return type;
		}

		/**
		 * Returns the annotation member value
		 * @return the annotation member value or null if no default is defined
		 */
		public T getValue() {
			return value;
		}

		
		
	}

}
