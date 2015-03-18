/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.aop.retransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javassist.ByteArrayClassPath;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * <p>Title: Retransformer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.Retransformer</code></p>
 */

public class Retransformer {
	/** The singleton instance */
	private static volatile Retransformer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The system property name defining a static class/field combo that will provide an {@link Instrumentation} instance.
	 * The format of the value is <b><code>&lt;class-name&gt;/&lt;field-name&gt;</code></b>/
	 */
	public static final String INSTR_PROVIDER_PROP = "instrumentation.provider"; 
	
	/** The local agent installer class name */
	public static final String AGENT_INSTALLER_CLASS = "com.heliosapm.aop.retransformer.LocalAgentInstaller";
	/** The local agent installer method name */
	public static final String AGENT_INSTALLER_METHOD = "getInstrumentation";

	/** The retransformer's instrumentation instance */
	private final Instrumentation instrumentation;
	
	/**
	 * Returns the Retransformer singleton instance
	 * @return the Retransformer singleton instance
	 */
	public static Retransformer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Retransformer();
					log("Created Retransformer");
				}
			}
		}
		return instance;
	}
	
	private Retransformer() {
		// A temporary instrumentation holder
		Instrumentation instr = null;
		// We need to get an Instrumentation instance.
		// First try the Instrumentation Provider
		String classField = System.getProperty(INSTR_PROVIDER_PROP);
		if(classField!=null) {
			String[] frags = classField.replace(" ", "").split("/");
			if(frags.length==2) {
				try {
					Class<?> clazz = Class.forName(frags[0]);
					Field f = clazz.getDeclaredField(frags[1]);
					f.setAccessible(true);
					instr = (Instrumentation)f.get(null);
					if(instr!=null) {
						log("Acquired Instrumentation from provider [%s]", classField);
					}
				} catch (Exception ex) {
					loge("Failed to get Instrumentation from provider [%s]: %s", classField, ex.toString());
				}
			}
		}
		if(instr==null) {
			// that didn't work, so we'll try the LocalAgent installer.
			// the installer requires access to tools.jar, which is not always present.
			// we don't want a static dependency on tools.jar, so we do this reflectively.
			try {
				Class<?> clazz = Class.forName(AGENT_INSTALLER_CLASS);
				Method m = clazz.getDeclaredMethod(AGENT_INSTALLER_METHOD);
				instr = (Instrumentation)m.invoke(null);
			} catch (Throwable t) {
				loge("Failed to get Instrumentation from LocalAgentInstaller: %s", t.toString());
			}
		}
		if(instr==null) {
			// no dice. We can't continue without an Instrumentation instance, so we have to throw.
			throw new RuntimeException("Failed to get an Instrumentation instance");
		}
		// success.... continue;
		instrumentation = instr;
	}
	
	/**
	 * Indicates if the passed class is currently instrumented by the Retransformer
	 * @param clazz The class to test for
	 * @return true if the passed class is currently instrumented, false otherwise
	 */
	public boolean isClassInstrumented(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null");
		return clazz.getAnnotation(Instrumented.class)!=null;
	}

	/**
	 * Executes a mock class method replacement transformation
	 * @param targetClass The target class to transform
	 * @param mockedClass The source of the mocked methods to inject into the target
	 */
	public synchronized void transform(final Class<?> targetClass, Class<?> mockedClass) {
		if(targetClass==null) throw new IllegalArgumentException("Passed target class was null");
		if(mockedClass==null) throw new IllegalArgumentException("Passed mocked class was null");
		String internalFormName = internalForm(targetClass.getName());
		if(isClassInstrumented(targetClass)) {
			//throw new RuntimeException("Class ["+ targetClass.getName() + "] already in instrumented state. Restore first and then and retransform");
			restore(targetClass);
		}
		ClassFileTransformer transformer = null;
		try {
			transformer = newClassFileTransformer(internalFormName, mockedClass);
			instrumentation.addTransformer(transformer, true);
			instrumentation.retransformClasses(targetClass);			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to transform [" + targetClass.getName() + "]", ex);
		} finally {
			if(transformer!=null) {
				instrumentation.removeTransformer(transformer);
			}
		}
	}
	
	/**
	 * Convenience method to execute a single provided source method replacement transformation
	 * @param targetClass The target class to transform
	 * @param methodName The method name to transform
	 * @param source  The source of the transformed method
	 */
	public synchronized void transform(final Class<?> targetClass, final String methodName, final String source) {		
		transform(targetClass, true, Collections.singletonMap(methodName, source));
	}
	
	
	
	/**
	 * Executes a provided source method replacement transformation
	 * @param targetClass The target class to transform
	 * @param failOnNotFound If true, any not found method will throw. Otherwise, if some (but not all) methods are not found, they are ignored.
	 * @param sourceMap A map of source code replacements keyed by the method descriptor of the methods to replace
	 */
	public synchronized void transform(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		if(targetClass==null) throw new IllegalArgumentException("Passed target class was null");
		if(sourceMap==null) throw new IllegalArgumentException("Passed mocked class was null");
		if(sourceMap.isEmpty()) {
			log("WARN: \n\tTransform requested on class [%s] with empty map. \n\tThis is a No Op. \n\tClass will not be instrumented.");
			return;
		}
		if(isClassInstrumented(targetClass)) {
			//throw new RuntimeException("Class ["+ targetClass.getName() + "] already in instrumented state. Restore first and then and retransform");
			restore(targetClass);
		}
		ClassFileTransformer transformer = null;
		try {
			final Class<?>[] transformTarget = new Class[1];
			transformer = newClassFileTransformer(targetClass, failOnNotFound, sourceMap, transformTarget);
			instrumentation.addTransformer(transformer, true);
			log("-----TRANS: %s", transformTarget[0].getName());
			instrumentation.retransformClasses(transformTarget[0]);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to transform [" + targetClass.getName() + "]", ex);
		} finally {
			if(transformer!=null) {
				instrumentation.removeTransformer(transformer);
			}
		}
		
	}
	
	/**
	 * Helper to get the JVM spec signature for the passed method 
	 * @param method The method to get a descriptor for
	 * @return the method descriptor
	 */
	public static String getMethodDescriptor(final Method method) {
		if(method==null) throw new IllegalArgumentException("Passed method was null");
		try {
			ClassPool cp = new ClassPool();
			cp.appendSystemPath();
			cp.appendClassPath(new LoaderClassPath(method.getDeclaringClass().getClassLoader()));
			CtClass ctClass = cp.get(method.getDeclaringClass().getName());
			final Class<?>[] paramTypes = method.getParameterTypes();			
			final CtClass[] ctParams = new CtClass[method.getParameterTypes().length];
			if(paramTypes.length>0) {
				for(int i = 0; i < paramTypes.length; i++) {
					Class<?> paramType = paramTypes[i];
					ctParams[i] = cp.get(paramType.getName());
				}				
			}
			CtMethod ctMethod = ctClass.getDeclaredMethod(method.getName(), ctParams);
			return ctMethod.getSignature();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get descriptor for method [" + method.toGenericString() + "]", ex);
		}
	}

	/**
	 * Creates a new method provided source classfile transformer
	 * @param targetClass The class to transform
	 * @param failOnNotFound If true, any not found method will throw. Otherwise, if some (but not all) methods are not found, they are ignored.
	 * @param sourceMap A map of source code replacements keyed by the method descriptor of the methods to replace
	 * @param transformTarget The class to transform is set in this array, since it could be the passed class, or it could be the parent.
	 * @return the transformer
	 */
	protected ClassFileTransformer newClassFileTransformer(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap, final Class<?>[] transformTarget) {
		final String[] internalFormClassName = new String[1];
		final String binaryName = targetClass.getName();
		return new ClassFileTransformer(){
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				log("Inspecting class [%s]", className);
					if(internalFormClassName[0].equals(className)) {
						try {
							log("\n\t================\n\tTransforming [%s]\n\tUsing Source Map\n\t================", binaryForm(internalFormClassName[0]));
							ClassPool cp = new ClassPool();
							cp.appendSystemPath();
							cp.appendClassPath(new ByteArrayClassPath(binaryName, classfileBuffer));
							if(loader!=null) {
								cp.appendClassPath(new LoaderClassPath(loader));
							}
							CtClass targetClazz = cp.get(binaryName);						 
							int methodCount = 0;
							for(Map.Entry<String, String> entry: sourceMap.entrySet()) {
								String key = entry.getKey();								
								String methodName = null;
								String descriptor = null;
								int index = key.indexOf(':');
								if(index==-1) {
									methodName = key.trim();
									descriptor = null;
								} else {
									methodName = key.substring(0, index).trim();
									descriptor = key.substring(index+1).trim();									
								}								
								String source = entry.getValue();
								CtMethod ctMethod = null;
								try {
									if(descriptor==null) {
										try {
											ctMethod = targetClazz.getDeclaredMethod(methodName);
										} catch (NotFoundException nfe) {
											CtClass crnt = targetClazz;
											while(crnt.getSuperclass()!=null) {
												crnt = crnt.getSuperclass();
												try {
													ctMethod = crnt.getDeclaredMethod(methodName);
													break;
												} catch (NotFoundException nfx) { /* No Op */}
											}
											if(ctMethod==null) throw new NotFoundException("Failed to find method [" + methodName + "]"); 
										}
									} else {
										ctMethod = targetClazz.getMethod(methodName, descriptor);
									}
								} catch (Exception ex) {
									if(failOnNotFound) throw new RuntimeException("Failed to find method [" + key + "] in class [" + binaryName + "]");
								}
								internalFormClassName[0] = internalForm(ctMethod.getDeclaringClass().getName());
								transformTarget[0] = Class.forName(ctMethod.getDeclaringClass().getName(), true, targetClass.getClassLoader());
								ctMethod.setBody(source);
								methodCount++;
							}
							if(methodCount==0) {
								throw new RuntimeException("Failed to replace any methods");
							}
							ConstPool constpool = targetClazz.getClassFile().getConstPool();
							AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
							javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(Instrumented.class.getName(), constpool);
							StringMemberValue smv = new StringMemberValue("Source", constpool);
							annot.addMemberValue("mockProvider", smv);
							LongMemberValue timestamp = new LongMemberValue(System.currentTimeMillis(), constpool);
							annot.addMemberValue("instrumentedTime", timestamp);
							attr.addAnnotation(annot);	
							targetClazz.getClassFile().addAttribute(attr);							
							byte[] byteCode =  targetClazz.toBytecode();
							return byteCode;
							
						} catch (Exception ex) {
							loge("Transform for [%s] using source map failed: %s", binaryName, ex);
							throw new RuntimeException(ex);							
						}
					}
					return classfileBuffer;
			}
		};
	}
	
	
	/**
	 * Creates a new method replacement classfile transformer
	 * @param internalFormClassName The class name to transform
	 * @param mockedClass The class containing the mocked template methods to inject into the target class
	 * @return the transformer
	 */
	protected ClassFileTransformer newClassFileTransformer(final String internalFormClassName, final Class<?> mockedClass) {
		final String binaryName = binaryForm(internalFormClassName);		
		return new ClassFileTransformer(){
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
					if(internalFormClassName.equals(className)) {
						log("\n\t================\n\tTransforming [%s]\n\tUsing [%s]\n\t================", binaryForm(internalFormClassName), mockedClass.getName());
						try {
							ClassPool cp = new ClassPool();
							cp.appendSystemPath();
							cp.appendClassPath(new ByteArrayClassPath(binaryName, classfileBuffer));
							cp.appendClassPath(new LoaderClassPath(mockedClass.getClassLoader()));
							cp.appendClassPath(new ClassClassPath(mockedClass));
							CtClass targetClazz = cp.get(binaryName);
							CtClass mockClazz = cp.get(mockedClass.getName());
//							 
							int methodCount = 0;
							for(CtMethod templateMethod: mockClazz.getDeclaredMethods()) {
								//if(!templateMethod.getDeclaringClass().equals(mockClazz)) continue;
								CtMethod targetMethod = null;
								// TODO:  getAnnotation(clazz) might not be implemented in JBoss 4.3's javassist version
								if(templateMethod.getAnnotation(MethodIgnore.class) != null) continue;
								try {
									targetMethod = targetClazz.getDeclaredMethod(templateMethod.getName(), templateMethod.getParameterTypes());
									targetClazz.removeMethod(targetMethod);
									targetMethod.setBody(templateMethod, null);
									targetClazz.addMethod(targetMethod);
									methodCount++;
								} catch (NotFoundException nfe) {					
								}				
							}
							if(methodCount==0) {
								throw new RuntimeException("Failed to replace any methods");
							}
							ConstPool constpool = targetClazz.getClassFile().getConstPool();
							AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
							javassist.bytecode.annotation.Annotation annot = new javassist.bytecode.annotation.Annotation(Instrumented.class.getName(), constpool);
							StringMemberValue smv = new StringMemberValue(mockedClass.getName(), constpool);
							annot.addMemberValue("mockProvider", smv);
							LongMemberValue timestamp = new LongMemberValue(System.currentTimeMillis(), constpool);
							annot.addMemberValue("instrumentedTime", timestamp);
							attr.addAnnotation(annot);	
							targetClazz.getClassFile().addAttribute(attr);
							
							byte[] byteCode =  targetClazz.toBytecode();
							return byteCode;
						} catch (Exception ex) {
							loge("Transform for [%s] using [%s] failed: %s", binaryName, mockedClass.getName(), ex);
							throw new RuntimeException(ex);
						}
					}
					return classfileBuffer;
			}
		}; 
	}
	
	
	protected static CtMethod matchMethod(final String methodName, final String descriptor, final CtClass klass) {
		CtMethod ctMethod = null;
		try {
			if(descriptor==null) {
				try {
					ctMethod = klass.getDeclaredMethod(methodName);
				} catch (NotFoundException nfe) {
					CtClass crnt = klass;
					while(crnt.getSuperclass()!=null) {
						crnt = crnt.getSuperclass();
						try {
							ctMethod = crnt.getDeclaredMethod(methodName);
							break;
						} catch (NotFoundException nfx) { /* No Op */}
					}
				}
			} else {
				ctMethod = klass.getMethod(methodName, descriptor);
			}
			if(ctMethod==null) throw new NotFoundException("Failed to find method [" + methodName + "]");
			return ctMethod;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

	}
	
//    /**
//     * Appends the descriptor of the given class to the given string buffer.
//     * @param buf the string buffer to which the descriptor must be appended.
//     * @param c the class whose descriptor must be computed.
//     * (Derived from ObjectWeb ASM, modified for use with CtClass)
//     * @author Eric Bruneton  
//     * @author Chris Nokleberg
//     */
//    static void getDescriptor(final CtMethod ctMethod) {
//    	if(ctMethod==null) throw new IllegalArgumentException("The passed CtMethod was null");
//        CtClass d = ctMethod.getReturnType();
//        if (d.isPrimitive()) {
//            if (d == CtClass.charType) {
//                return "I";
//            } else if (d == Void.TYPE) {
//                car = 'V';
//            } else if (d == Boolean.TYPE) {
//                car = 'Z';
//            } else if (d == Byte.TYPE) {
//                car = 'B';
//            } else if (d == Character.TYPE) {
//                car = 'C';
//            } else if (d == Short.TYPE) {
//                car = 'S';
//            } else if (d == Double.TYPE) {
//                car = 'D';
//            } else if (d == Float.TYPE) {
//                car = 'F';
//            } else /* if (d == Long.TYPE) */{
//                car = 'J';
//            }
//            buf.append(car);
//            return;
//        } else if (d.isArray()) {
//            buf.append('[');
//            d = d.getComponentType();
//        } else {
//            buf.append('L');
//            String name = d.getName();
//            int len = name.length();
//            for (int i = 0; i < len; ++i) {
//                char car = name.charAt(i);
//                buf.append(car == '.' ? '/' : car);
//            }
//            buf.append(';');
//            return;
//        }        
//    }
	
	
	/**
	 * Restores a transformed class back to its original form 
	 * @param targetClass The class to restore
	 */
	public synchronized void restore(final Class<?> targetClass) {
		if(targetClass==null) throw new IllegalArgumentException("Passed target class was null");
		try {
//			log("\n\t================\n\tRestoring [%s]\n\t================", targetClass.getName());
			instrumentation.retransformClasses(targetClass);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to restore class [" + targetClass.getName() + "]", e);
		}
	}	
	
	
	
	/**
	 * Converts the passed binary class name to the internal form 
	 * @param name The class name to convert
	 * @return the internal form name of the class
	 */
	public static String internalForm(CharSequence name) {
		return name.toString().replace('.', '/');
	}
	
	/**
	 * Converts the passed internal form class name to the binary name
	 * @param name The class name to convert
	 * @return the binary name of the class
	 */
	public static String binaryForm(CharSequence name) {
		return name.toString().replace('/', '.');
	}
	
	
	/**
	 * Converts the binary name of passed class to the internal form 
	 * @param clazz The class for which the name should be converted
	 * @return the internal form name of the class
	 */
	public static String internalForm(Class<?> clazz) {
		return internalForm(clazz.getName());
	}
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Retransformer r = getInstance();
		class Foo {
			public String printHello() {
				return "Hello";
			}
		}
		class SpanishFoo {
			public String printHello() {
				return "Hola";
			}			
		}
		class FrenchFoo {
			public String printHello() {
				return "Bonjour";
			}			
		}
		final Foo f = new Foo();
		log(f.printHello());
		r.transform(Foo.class, SpanishFoo.class);		
		log("Message:[%s], %s", f.printHello(), new InstrumentedImpl(Foo.class));
		r.restore(Foo.class);
		r.transform(Foo.class, FrenchFoo.class);
		log("Message:[%s], %s", f.printHello(), new InstrumentedImpl(Foo.class));
		log(f.printHello());
		r.restore(Foo.class);
		r.transform(Foo.class, "printHello", "{ return \"hallo\"; }");		
		log("Message:[%s], %s", f.printHello(), new InstrumentedImpl(Foo.class));
		log(f.printHello());
		r.restore(Foo.class);
		log(f.printHello());
	}
	
	/**
	 * <p>Title: InstrumentedImpl</p>
	 * <p>Description: A concrete pojo to represent an @Instrumented annotation instance</p> 
	 * <p><code>com.heliosapm.aop.retransformer.Retransformer.InstrumentedImpl</code></p>
	 */
	public static class InstrumentedImpl {
		/** The annotation instance */
		final Instrumented i;
		/** The [possibly instrumented] class */
		final Class<?> clazz;

		/**
		 * Creates a new InstrumentedImpl
		 * @param clazz The target class
		 */
		public InstrumentedImpl(final Class<?> clazz) {
			if(clazz==null) throw new IllegalArgumentException("Passed class was null");
			this.clazz = clazz;			
			this.i = clazz.getAnnotation(Instrumented.class);
		}
		
		/**
		 * Returns the mock provider class name that provided the injected replacement methods
		 * @return the mock provider class name 
		 */
		public String getMockProvider() {
			return i!=null ? i.mockProvider() : null;
		}
		
		/**
		 * Returns the timestamp of the instrumentation event
		 * @return the timestamp of the instrumentation event
		 */
		public long getInstrumentedTime() {
			return i==null ? -1L : i.instrumentedTime();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("Instrumented [\n\tclass:%s, \n\tmock:%s, \n\ttime:%s\n]", clazz.getName(), getMockProvider(), i==null ? "" : new Date(i.instrumentedTime()));
		}
		
	}
	

	/**
	 * Standard out logger 
	 * @param fmt The message format
	 * @param args The format args
	 */
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}
	
	/**
	 * Standard err logger 
	 * @param fmt The message format
	 * @param args The format args
	 */
	public static void loge(final Object fmt, final Object...args) {
		System.err.println(String.format(fmt.toString(), args));
		if(args.length>0 && (args[args.length-1] instanceof Throwable)) {
			((Throwable)args[args.length-1]).printStackTrace(System.err);
		}
	}
	

}
