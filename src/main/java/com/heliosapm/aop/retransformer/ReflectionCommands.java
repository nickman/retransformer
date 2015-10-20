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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.reflections.Reflections;

/**
 * <p>Title: ReflectionCommands</p>
 * <p>Description: Reflections getter command helper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.ReflectionCommands</code></p>
 */
@SuppressWarnings("rawtypes")
public class ReflectionCommands {

	
	/**
	 * Returns constructors annotated with the specified annotation
	 * @param annotation the annotation to filter by
	 * @return A set of matching constructors
	 */
	public static ReflectionCommand<Constructor> constructors(final Annotation annotation) {
		return new ReflectionCommand<Constructor>() {
			@Override
			public Set<Constructor> invoke(final Reflections reflections) {				
				return reflections.getConstructorsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns constructors annotated with the specified annotation type
	 * @param annotation the annotation type to filter by
	 * @return A set of matching constructors
	 */
	public static ReflectionCommand<Constructor> constructors(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Constructor>() {
			@Override
			public Set<Constructor> invoke(final Reflections reflections) {				
				return reflections.getConstructorsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns constructors with the passed parameter signature
	 * @param types the parameter signature to filter by
	 * @return A set of matching constructors
	 */
	public static ReflectionCommand<Constructor> constructors(final Class<?>...types) {
		return new ReflectionCommand<Constructor>() {
			@Override
			public Set<Constructor> invoke(final Reflections reflections) {				
				return reflections.getConstructorsMatchParams(types);
			}
		};
	}
	
	/**
	 * Returns constructors with any parameters annotated with the specified annotation
	 * @param annotation the parameter annotation to filter by
	 * @return A set of matching constructors
	 */
	public static ReflectionCommand<Constructor> constructorsWithParams(final Annotation annotation) {
		return new ReflectionCommand<Constructor>() {
			@Override
			public Set<Constructor> invoke(final Reflections reflections) {				
				return reflections.getConstructorsWithAnyParamAnnotated(annotation);
			}
		};
	}
	
	/**
	 * Returns constructors with any parameters annotated with the specified annotation type
	 * @param annotation the parameter annotation type to filter by
	 * @return A set of matching constructors
	 */
	public static ReflectionCommand<Constructor> constructorsWithParams(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Constructor>() {
			@Override
			public Set<Constructor> invoke(final Reflections reflections) {				
				return reflections.getConstructorsWithAnyParamAnnotated(annotation);
			}
		};
	}
	
	/**
	 * Returns converter methods that accept instances of the first supplied type and return instances of the second 
	 * @param from The type accepted by the method
	 * @param to The type returned by the method
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methods(final Class<?> from, final Class<?> to) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {		
				final Set<Method> froms = reflections.getMethodsMatchParams(from);
				final Set<Method> tos = reflections.getMethodsReturn(to);
				tos.retainAll(froms);
				return tos;
			}
		};
	}

	/**
	 * Returns fields annotated with the passed annotation 
	 * @param annotation The annotation to filter by
	 * @return A set of matching fields
	 */
	public static ReflectionCommand<Field> fields(final Annotation annotation) {
		return new ReflectionCommand<Field>() {
			@Override
			public Set<Field> invoke(final Reflections reflections) {						
				return reflections.getFieldsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns fields annotated with the passed annotation type
	 * @param annotation The annotation type to filter by
	 * @return A set of matching fields
	 */
	public static ReflectionCommand<Field> fields(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Field>() {
			@Override
			public Set<Field> invoke(final Reflections reflections) {						
				return reflections.getFieldsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns methods annotated with the passed annotation
	 * @param annotation The annotation to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methods(final Annotation annotation) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {						
				return reflections.getMethodsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns methods annotated with the passed annotation type
	 * @param annotation The annotation type to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methods(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {						
				return reflections.getMethodsAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns methods with the matching signature
	 * @param signature The parameter type signature to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methodsSig(final Class<?>... signature) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {						
				return reflections.getMethodsMatchParams(signature);
			}
		};
	}
	
	/**
	 * Returns methods with the matching return type
	 * @param returnType The return type signature to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methodsRet(final Class<?> returnType) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {						
				return reflections.getMethodsReturn(returnType);
			}
		};
	}
	
	/**
	 * Returns methods with any parameters annotated with the specified annotation
	 * @param annotation the parameter annotation to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methodsWithParams(final Annotation annotation) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {				
				return reflections.getMethodsWithAnyParamAnnotated(annotation);
			}
		};
	}
	
	/**
	 * Returns methods with any parameters annotated with the specified annotation type
	 * @param annotation the parameter annotation type to filter by
	 * @return A set of matching methods
	 */
	public static ReflectionCommand<Method> methodsWithParams(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Method>() {
			@Override
			public Set<Method> invoke(final Reflections reflections) {				
				return reflections.getMethodsWithAnyParamAnnotated(annotation);
			}
		};
	}
	
	/**
	 * Returns types that are sub types of the passed type
	 * @param type The type to get sub types of
	 * @return A set of subtypes
	 */
	public static <T> ReflectionCommand<Class<? extends T>> subTypes(final Class<T> type) {
		return new ReflectionCommand<Class<? extends T>>() {			
			@Override
			public Set<Class<? extends T>> invoke(final Reflections reflections) {				
				return reflections.getSubTypesOf(type);
			}
		};
	}
	
	/**
	 * Returns types annotated with the passed annotation
	 * @param annotation The annotation to filter by
	 * @return A set of matching types
	 */
	public static ReflectionCommand<Class<?>> types(final Annotation annotation) {
		return new ReflectionCommand<Class<?>>() {
			@Override
			public Set<Class<?>> invoke(final Reflections reflections) {						
				return reflections.getTypesAnnotatedWith(annotation);
			}
		};
	}
	
	/**
	 * Returns types annotated with the passed annotation type
	 * @param annotation The annotation type to filter by
	 * @return A set of matching types
	 */
	public static ReflectionCommand<Class<?>> types(final Class<? extends Annotation> annotation) {
		return new ReflectionCommand<Class<?>>() {
			@Override
			public Set<Class<?>> invoke(final Reflections reflections) {						
				return reflections.getTypesAnnotatedWith(annotation);
			}
		};
	}


	/**
	 * Returns types annotated with the passed annotation
	 * @param annotation The annotation to filter by
	 * @param honorInherited true to honor the @Inherrited annotations, false otherwise
	 * @return A set of matching types
	 */
	public static ReflectionCommand<Class<?>> types(final Annotation annotation, final boolean honorInherited) {
		return new ReflectionCommand<Class<?>>() {
			@Override
			public Set<Class<?>> invoke(final Reflections reflections) {						
				return reflections.getTypesAnnotatedWith(annotation, honorInherited);
			}
		};
	}
	
	/**
	 * Returns types annotated with the passed annotation type
	 * @param annotation The annotation type to filter by
	 * @param honorInherited true to honor the @Inherrited annotations, false otherwise
	 * @return A set of matching types
	 */
	public static ReflectionCommand<Class<?>> types(final Class<? extends Annotation> annotation, final boolean honorInherited) {
		return new ReflectionCommand<Class<?>>() {
			@Override
			public Set<Class<?>> invoke(final Reflections reflections) {						
				return reflections.getTypesAnnotatedWith(annotation, honorInherited);
			}
		};
	}

	
	/**
	 * Extracts the argument at the provided index, validates that it is an instance of the passed type
	 * and returns it, cast to the appropriate type
	 * @param index The index at which the target object is in the passed argument array
	 * @param type The type to cast to
	 * @param args The array of arguments to extract the object from
	 * @return the cast object
	 */	
	public static <T> T val(final int index, final Class<T> type, final Object...args) {
		if(index < 0 || index > args.length-1) throw new IllegalArgumentException("Invalid index [" + index + "] for arg array of length [" + args.length + "]");
		if(type==null) throw new IllegalArgumentException("The passed type was null");
		final Object o = args[index];
		if(o==null) throw new IllegalArgumentException("Value at index [" + index + "] was null");
		if(type.isInstance(o)) {
			return type.cast(o);
		}
		throw new IllegalArgumentException("Value at index [" + index + "] was not an instance of [" + type.getClass().getName() + "]");
	}
	
	private ReflectionCommands() {}

}
