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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.CtClass;
import javassist.bytecode.annotation.IntegerMemberValue;
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
	protected final Map<String, MemberValue> members = new HashMap<String, MemberValue>();
	/** A set of the member names on the annotation type */
	protected final Set<String> memberNames;
	/** The member definitions keyed by the member name */
	final Map<String, MemberDef<?>> defs;
	/** The element types supported by the annotation */
	final Set<ElementType> elementTypes;
	
	
	public static final Set<Class> ALLOWED_TYPES = Collections.unmodifiableSet(new HashSet<Class>(Arrays.asList(
			new Class[]{
					byte.class, byte[].class, boolean.class, boolean[].class, short.class, short[].class, 
					char.class, char[].class, int.class, int[].class, float.class, float[].class, long.class, 
					long[].class, double.class, double[].class,
					String.class, Enum.class, Annotation.class, Class.class,					
					String[].class, Enum[].class, Annotation[].class, Class[].class
			}
	)));
	
	public static final Set<Class> ALLOWED_NON_FINAL_TYPES = Collections.unmodifiableSet(new HashSet<Class>(Arrays.asList(
			new Class[]{
					Enum.class, Annotation.class					
				}
	)));
	
	public static final Map<Class, Class> P2O;
	public static final Map<Class, Class> O2P;
	/** Primitive type to Jvaassist CtClass type decode */
	public static final Map<Class, CtClass> P2J;
	
	static {
		final Map<Class, Class> o2p = new HashMap<Class, Class>();
		final Map<Class, Class> p2o = new HashMap<Class, Class>();
		final Map<Class, CtClass> p2j = new HashMap<Class, CtClass>();
		// =========  full object --> primitive
		o2p.put(java.lang.Byte.class, byte.class);
		o2p.put(java.lang.Boolean.class, boolean.class);
		o2p.put(java.lang.Short.class, short.class);
		o2p.put(java.lang.Integer.class, int.class);
		o2p.put(java.lang.Character.class, char.class);
		o2p.put(java.lang.Float.class, float.class);
		o2p.put(java.lang.Long.class, long.class);
		o2p.put(java.lang.Double.class, double.class);
		o2p.put(java.lang.Byte[].class, byte[].class);
		o2p.put(java.lang.Boolean[].class, boolean[].class);
		o2p.put(java.lang.Short[].class, short[].class);
		o2p.put(java.lang.Character[].class, char[].class);
		o2p.put(java.lang.Integer[].class, int[].class);
		o2p.put(java.lang.Float[].class, float[].class);
		o2p.put(java.lang.Long[].class, long[].class);
		o2p.put(java.lang.Double[].class, double[].class);
		// =========  primitive --> full object
		p2o.put(byte.class, java.lang.Byte.class);
		p2o.put(boolean.class, java.lang.Boolean.class);
		p2o.put(short.class, java.lang.Short.class);
		p2o.put(char.class, java.lang.Character.class);
		p2o.put(int.class, java.lang.Integer.class);
		p2o.put(float.class, java.lang.Float.class);
		p2o.put(long.class, java.lang.Long.class);
		p2o.put(double.class, java.lang.Double.class);
		p2o.put(byte[].class, java.lang.Byte[].class);
		p2o.put(boolean[].class, java.lang.Boolean[].class);
		p2o.put(short[].class, java.lang.Short[].class);
		p2o.put(char[].class, java.lang.Character[].class);
		p2o.put(int[].class, java.lang.Integer[].class);
		p2o.put(float[].class, java.lang.Float[].class);
		p2o.put(long[].class, java.lang.Long[].class);
		p2o.put(double[].class, java.lang.Double[].class);
		// =========  primitive --> CtClass primtive
		p2j.put(byte.class, CtClass.byteType);
		p2j.put(boolean.class, CtClass.booleanType);
		p2j.put(short.class, CtClass.shortType);
		p2j.put(char.class, CtClass.charType);
		p2j.put(int.class, CtClass.intType);
		p2j.put(float.class, CtClass.floatType);
		p2j.put(long.class, CtClass.longType);
		p2j.put(double.class, CtClass.doubleType);
		p2j.put(void.class, CtClass.voidType);		
		O2P = Collections.unmodifiableMap(new HashMap<Class, Class>(o2p));
		P2O = Collections.unmodifiableMap(new HashMap<Class, Class>(p2o));
		P2J = Collections.unmodifiableMap(new HashMap<Class, CtClass>(p2j));
	}
	

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
//		for(ElementType et: ElementType.values()) {
//			members.put(et, new HashMap<String, MemberValue>());
//			index.put(et, new HashMap<String, MemberDef<?>>());
//		}
		final Method[] methods = annotationClass.getDeclaredMethods();
		final Set<String> mn = new HashSet<String>(methods.length);
		for(Method m: methods) {
			mn.add(m.getName());
		}
		memberNames = Collections.unmodifiableSet(mn);
		log("Member Names: " + memberNames);
		defs = MemberDef.getMemberDefs(annotationClass);
		final Target target = annotationClass.getAnnotation(Target.class);
		Set<ElementType> ets = EnumSet.noneOf(ElementType.class);
		if(target!=null) {
			for(ElementType et: target.value()) {
				ets.add(et);
			}
		}
		this.elementTypes = Collections.unmodifiableSet(ets);
	}
	
	/**
	 * Adds a new member name and value to the builder
	 * @param name The member name
	 * @param value The member value
	 * @return 
	 */
	public AnnotationBuilder add(final String name, final int value) {
		validate(name, value);
		members.put(name, new IntegerMemberValue(null, value));
		return this;
	}
	
	public void validate(final String name, final Object value) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("Member name was null or empty");		
		if(!memberNames.contains(name)) throw new IllegalArgumentException("Invalid member name [" + name + "]");
		if(value==null) throw new IllegalArgumentException("The value passed was null for Member name [" + name + "]");
		final Class<?> type = value.getClass();
		for(MemberDef md: defs.values()) {
			if(md.match(name, type)) return;
		}
		throw new IllegalArgumentException("Invalid type [" + value.getClass() + "] for member name [" + name + "]");
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
		/** The annotation member type upped alt */
		final Class<T> otype;
		
		/** The annotation member default value */
		final T value;
		
		/**
		 * Returns an array of member definitions for the passed annotation type
		 * @param type The annotation type
		 * @return the array of member definitions
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Annotation> Map<String, MemberDef<?>> getMemberDefs(final Class<T> type) {			
			final Method[] methods = type.getDeclaredMethods();
			final Map<String, MemberDef<?>> map = new HashMap<String, MemberDef<?>>(methods.length);			
			for(Method method: methods) {
				map.put(method.getName(), new MemberDef(method.getName(), method.getReturnType(), method.getDefaultValue()));
			}
			return Collections.unmodifiableMap(map);
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
			this.otype = P2O.get(type);
		}
		
		/**
		 * Tests the passed member name and value to see if it is a match for this member def
		 * @param name The member name
		 * @param valueType The member value type
		 * @return true for a match, false otherwise
		 */
		public boolean match(final String name, final Class<?> valueType) {
			if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The member name was null or empty");
			if(value==null) throw new IllegalArgumentException("The member type was null");
			if(!this.name.equals(name)) return false;			
			return (valueType.equals(this.type) || valueType.equals(this.otype));
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
