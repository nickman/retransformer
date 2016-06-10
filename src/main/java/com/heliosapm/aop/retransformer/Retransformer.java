package com.heliosapm.aop.retransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

import javassist.ByteArrayClassPath;
import javassist.ClassClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
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
 * <p>Description: The retransformation singleton</p> 
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
	public static final String AGENT_INSTALLER_CLASS = "com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller";
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
		// Lastly, use tools wrapper
		instr = LocalAgentInstaller.getInstrumentation();
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
	
	public synchronized void transformInsert(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		
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
			final Set<Class<?>> transformTargets = new HashSet<Class<?>>();
			transformer = newClassFileTransformer(targetClass, failOnNotFound, sourceMap, transformTargets);
			instrumentation.addTransformer(transformer, true);			
			instrumentation.retransformClasses(transformTargets.toArray(new Class[transformTargets.size()]));
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
	
	private static final AtomicLong tempClassSerial = new AtomicLong(0L);
	private static final Map<Class<?>, CtClass> PRIMITIVES;
	private static final Map<String, CtClass> PRIMITIVENAMES;
	
	static {
		final Map<Class<?>, CtClass> p = new HashMap<Class<?>, CtClass>();
		final Map<String, CtClass> ps = new HashMap<String, CtClass>();
		p.put(byte.class, CtClass.byteType);
		p.put(boolean.class, CtClass.booleanType);
		p.put(char.class, CtClass.charType);
		p.put(short.class, CtClass.shortType);
		p.put(int.class, CtClass.intType);
		p.put(float.class, CtClass.floatType);
		p.put(long.class, CtClass.longType);
		p.put(double.class, CtClass.doubleType);
		p.put(void.class, CtClass.voidType);

		ps.put(byte.class.getName(), CtClass.byteType);
		ps.put(boolean.class.getName(), CtClass.booleanType);
		ps.put(char.class.getName(), CtClass.charType);
		ps.put(short.class.getName(), CtClass.shortType);
		ps.put(int.class.getName(), CtClass.intType);
		ps.put(float.class.getName(), CtClass.floatType);
		ps.put(long.class.getName(), CtClass.longType);
		ps.put(double.class.getName(), CtClass.doubleType);
		ps.put(void.class.getName(), CtClass.voidType);		

		PRIMITIVES = Collections.unmodifiableMap(p);
		PRIMITIVENAMES = Collections.unmodifiableMap(ps);
	}
	
	/**
	 * Returns the method signature for a method of the passed name with a signature matching the passed type arguments
	 * and a void return type
	 * @param name The method name
	 * @param typeArgs An object array of types matching the intended signature
	 * @return the method descriptor
	 */
	public static String getMethodDescriptorFromValues(final String name, final Object...typeArgs) {
		return getMethodDescriptorFromValues(name, null, typeArgs);
	}
	
	/**
	 * Returns the method signature for a method of the passed name with a signature matching the passed type arguments
	 * and a void return type
	 * @param name The method name
	 * @param returnType A value of the return type class. Assumed to be void if null.
	 * @param typeArgs An object array of types matching the intended signature
	 * @return the method descriptor
	 */
	public static String getMethodDescriptorFromValues(final String name, final Object returnType, final Object...typeArgs) {
		return getMethodDescriptorFromValues(name, returnType==null ? null : returnType.getClass(), typeArgs);
	}
	
	
	/**
	 * Returns the method signature for a method of the passed name with a signature matching the passed type arguments
	 * @param name The method name
	 * @param returnType The return type of the method
	 * @param typeArgs An object array of types matching the intended signature
	 * @return the method descriptor
	 */
	public static String getMethodDescriptorFromValues(final String name, final Class<?> returnType, final Object...typeArgs) {
		final Class[] sig = new Class[typeArgs.length];
		for(int i = 0; i < typeArgs.length; i++) {
			sig[i] = typeArgs[i].getClass();
		}
		return getMethodDescriptor(name, returnType, sig);
		
	}
	
	
	/**
	 * Returns the method signature for a method of the passed name with a signature matching the passed type arguments
	 * @param name The method name
	 * @param returnType The return type of the method
	 * @param typeArgs An class array of types matching the intended signature
	 * @return the method descriptor
	 */
	public static String getMethodDescriptor(final String name, final Class<?> returnType, final Class<?>...typeArgs) {
		if(name==null) throw new IllegalArgumentException("Passed method name was null");
		try {
			final ClassPool cp = new ClassPool();
			cp.appendSystemPath();
			final CtClass tmpClazz = cp.makeInterface("TempX" + tempClassSerial.incrementAndGet());
			final CtClass[] params = new CtClass[typeArgs.length];
			for(int i = 0; i < typeArgs.length; i++) {
				params[i] = lookup(typeArgs[i], cp);
			}
			final CtMethod ctm = new CtMethod(returnType==null ? CtClass.voidType : lookup(returnType, cp), name, params, tmpClazz);
			return ctm.getSignature();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get descriptor for method [" + name + Arrays.deepToString(typeArgs) + "]", ex);
		}		
	}
	
	/**
	 * Looks up the CtClass for the passed type in the passed classpool
	 * @param type The type to lookup
	 * @param cp The classpool to look up in
	 * @return the CtClass for the passed type
	 */
	public static final CtClass lookup(final Class<?> type, final ClassPool cp) {
		if(type==null) throw new IllegalArgumentException("Passed type was null");
		if(cp==null) throw new IllegalArgumentException("Passed ClassPool was null");
		CtClass c = PRIMITIVES.get(type);
		if(c!=null) return c;
		final ClassLoader cl = type.getClassLoader();
		final ClassPath classPath;
		if(cl!=null) {
			classPath = new LoaderClassPath(cl);
			cp.appendClassPath(classPath);
		} else {
			classPath=null;
		}
		try {
			cp.appendClassPath(classPath);
			return cp.get(type.getName());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to lookup type [" + type.getName() + "]", ex);
		} finally {
			if(classPath!=null) {
				cp.removeClassPath(classPath);
			}
		}
	}

	/**
	 * Creates a new method provided source classfile transformer
	 * @param targetClass The class to transform
	 * @param failOnNotFound If true, any not found method will throw. Otherwise, if some (but not all) methods are not found, they are ignored.
	 * @param sourceMap A map of source code replacements keyed by the method descriptor of the methods to replace
	 * @param transformTargets A set that the actual Java classes that need to be transformed should be written into
	 * @return the transformer
	 */
	ClassFileTransformer newClassFileTransformer(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap, final Set<Class<?>> transformTargets) {
		final Map<CtClass, Set<CtMethod>> actualTargets =  getMatchedMethods(targetClass, failOnNotFound, sourceMap);
		if(actualTargets.isEmpty()) throw new RuntimeException("Failed to match any methods");
		final Map<String, CtClass> internalClassNames = new HashMap<String, CtClass>(actualTargets.size());
		for(CtClass klass: actualTargets.keySet()) {
			internalClassNames.put(internalForm(klass.getName()), klass);
			if(targetClass.getName().equals(klass.getName())) {
				transformTargets.add(targetClass);
			} else {
				try {
					transformTargets.add(Class.forName(klass.getName(), true, targetClass.getSuperclass().getClassLoader()));
				} catch (Exception ex) {
					throw new RuntimeException("Failed to load class [" + klass.getName() + "]", ex);
				}
			}
			
		}
		return new ClassFileTransformer(){
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {				
					if(internalClassNames.containsKey(className)) {
						try {
							log("\n\t================\n\tTransforming [%s]\n\tUsing Source Map\n\t================", binaryForm(className));
							final CtClass targetClazz = internalClassNames.get(className);							
							ClassPool cp = targetClazz.getClassPool();														
							if(loader!=null) {
								cp.appendClassPath(new LoaderClassPath(loader));
							}
							Set<CtMethod> targetMethods = actualTargets.get(targetClazz);
							for(CtMethod targetMethod: targetMethods) {
								
								
								String sourceKey = null;
								String source = null;
								if(targetMethod.getParameterTypes().length>0) {
									sourceKey = targetMethod.getName() + ":" + targetMethod.getSignature();
									source = sourceMap.get(sourceKey);
									if(source == null) {
										// some slacker left off the descriptor because the method name is unique
										sourceKey = targetMethod.getName();
										source = sourceMap.get(sourceKey);
									}
								} else {
									sourceKey = targetMethod.getName();
									source = sourceMap.get(sourceKey);
								}
								
								if(source==null) {
									throw new RuntimeException("Failed to locate source with key [" + sourceKey + "] for method [" + targetMethod.getLongName() + "]");
								}
								
								targetMethod.setBody(source);
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
							loge("Transform for [%s] using source map failed: %s", targetClass.getName(), ex);
							throw new RuntimeException(ex);							
						}
					}
					return classfileBuffer;
			}
		};
	}
	
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}
	
	
	/**
	 * Creates a new method replacement classfile transformer
	 * @param internalFormClassName The class name to transform
	 * @param mockedClass The class containing the mocked template methods to inject into the target class
	 * @return the transformer
	 */
	ClassFileTransformer newClassFileTransformer(final String internalFormClassName, final Class<?> mockedClass) {
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
	
	/**
	 * Finds the matched methods 
	 * @param targetClass The class to inspect and traverse from
	 * @param failOnNotFound true to fail on not found, false otherwise
	 * @param sourceMap The map of method names/descriptors and sources
	 * @return A map of sets of target methods keyed by the class they are declared in
	 */
	protected Map<CtClass, Set<CtBehavior>> getMatchedBehaviors(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		try {
			final Map<CtClass, Set<CtMethod>> actualTargets = new HashMap<CtClass, Set<CtMethod>>();
			final ClassPool classPool = new ClassPool();
			classPool.appendSystemPath();
			classPool.appendClassPath(new ClassClassPath(targetClass));
			final CtClass targetCtClass = classPool.get(targetClass.getName());
			for(Map.Entry<String, String> entry: sourceMap.entrySet()) {
				String key = entry.getKey();								
				String bname = null;
				String descriptor = null;
				int index = key.indexOf(':');
				if(index==-1) {
					bname = key.trim();
					descriptor = null;
				} else {
					bname = key.substring(0, index).trim();
					descriptor = key.substring(index+1).trim();									
				}								
				final boolean ctor = targetClass.getSimpleName().equals(bname); 
				CtMethod matchedMethod = null;
				CtConstructor matchedCtor = null;
				try {
					matchedMethod = matchMethod(methodName, descriptor, targetCtClass);
					// =============================================================================================
					//   Class redefinition !!!
					// =============================================================================================
//					if(!matchedMethod.getDeclaringClass().equals(targetCtClass)) {
//						if(!Modifier.isFinal(matchedMethod.getModifiers())) {
//							matchedMethod = CtNewMethod.copy(matchedMethod, targetCtClass, null);
//							targetCtClass.addMethod(matchedMethod);
//							
//						}
//					}
				} catch (Exception ex) {
					if(failOnNotFound) throw ex;
				}
				Set<CtMethod> methods = actualTargets.get(matchedMethod.getDeclaringClass());
				if(methods==null) {
					methods = new HashSet<CtMethod>();
					actualTargets.put(matchedMethod.getDeclaringClass(), methods);
				}
				methods.add(matchedMethod);				
			}
			return actualTargets;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	/**
	 * Finds the matched methods 
	 * @param targetClass The class to inspect and traverse from
	 * @param failOnNotFound true to fail on not found, false otherwise
	 * @param sourceMap The map of method names/descriptors and sources
	 * @return A map of sets of target methods keyed by the class they are declared in
	 */
	protected Map<CtClass, Set<CtMethod>> getMatchedMethods(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		try {
			final Map<CtClass, Set<CtMethod>> actualTargets = new HashMap<CtClass, Set<CtMethod>>();
			final ClassPool classPool = new ClassPool();
			classPool.appendSystemPath();
			classPool.appendClassPath(new ClassClassPath(targetClass));
			final CtClass targetCtClass = classPool.get(targetClass.getName());
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
				CtMethod matchedMethod = null;
				try {
					matchedMethod = matchMethod(methodName, descriptor, targetCtClass);
					// =============================================================================================
					//   Class redefinition !!!
					// =============================================================================================
//					if(!matchedMethod.getDeclaringClass().equals(targetCtClass)) {
//						if(!Modifier.isFinal(matchedMethod.getModifiers())) {
//							matchedMethod = CtNewMethod.copy(matchedMethod, targetCtClass, null);
//							targetCtClass.addMethod(matchedMethod);
//							
//						}
//					}
				} catch (Exception ex) {
					if(failOnNotFound) throw ex;
				}
				Set<CtMethod> methods = actualTargets.get(matchedMethod.getDeclaringClass());
				if(methods==null) {
					methods = new HashSet<CtMethod>();
					actualTargets.put(matchedMethod.getDeclaringClass(), methods);
				}
				methods.add(matchedMethod);				
			}
			return actualTargets;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	/**
	 * Matches a method name and optional descriptor
	 * @param methodName The method name
	 * @param descriptor The optional descriptor
	 * @param klass The class to inspect
	 * @return the matched method
	 */
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
							// check to see if this is ambig.
							int cnt = 0;
							for(CtMethod ctm: crnt.getMethods()) {
								if(methodName.equals(ctm.getName())) cnt++;
							}
							if(cnt>1) throw new RuntimeException("Method match failed on [" + methodName + "]. Ambiguous methods. Please specify a method descriptor");
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
	
	
	
	/**
	 * Restores transformed classes back to their original form 
	 * @param targetClasses The classes to restore
	 */
	public synchronized void restore(final Class<?>... targetClasses) {
		if(targetClasses==null) throw new IllegalArgumentException("Passed target class was null");
		try {
//			log("\n\t================\n\tRestoring [%s]\n\t================", targetClass.getName());
			instrumentation.retransformClasses(targetClasses);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to restore classes " + Arrays.toString(targetClasses) , e);
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
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
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
