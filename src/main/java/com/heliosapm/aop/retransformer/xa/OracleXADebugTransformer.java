package com.heliosapm.aop.retransformer.xa;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

import javassist.ClassClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class OracleXADebugTransformer {
	/** The singleton instance */
	private static volatile OracleXADebugTransformer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The callable statements in normal mode */
	public static final Map<String, String> javaXaCalls;
	/** The callable statements in debug mode */
	public static final Map<String, String> javaDebugXaCalls;
	
	/** The white space trimmed JAVA_XA call prefix */
	public static final String JAVA_XA_CALL_PREFIX = "BEGIN?:=JAVA_XA.";
	/** The debug JAVA_XA procedure name */
	public static final String JAVA_XA_DEBUG = "DEBUG_JAVA_XA.";
	/** The original JAVA_XA procedure name */
	public static final String JAVA_XA = "JAVA_XA.";
	/** The length of the original JAVA_XA procedure name */
	public static final int JAVA_XA_LEN = JAVA_XA.length();
	
	private static final Map<Class<?>, CtClass> PRIMITIVES;
	private static final Map<String, CtClass> PRIMITIVENAMES;
	
	static {
		final Map<String, String> calls = new HashMap<String, String>(12);
		final Map<String, String> debugCalls = new HashMap<String, String>(12);
		
		calls.put("xa_start_816", "begin ? := JAVA_XA.xa_start(?,?,?,?); end;");
		calls.put("xa_start_post_816", "begin ? := JAVA_XA.xa_start_new(?,?,?,?,?); end;");
		calls.put("xa_end_816", "begin ? := JAVA_XA.xa_end(?,?); end;");
		calls.put("xa_end_post_816", "begin ? := JAVA_XA.xa_end_new(?,?,?,?); end;");
		calls.put("xa_commit_816", "begin ? := JAVA_XA.xa_commit (?,?,?); end;");
		calls.put("xa_commit_post_816", "begin ? := JAVA_XA.xa_commit_new (?,?,?,?); end;");
		calls.put("xa_prepare_816", "begin ? := JAVA_XA.xa_prepare (?,?); end;");
		calls.put("xa_prepare_post_816", "begin ? := JAVA_XA.xa_prepare_new (?,?,?); end;");
		calls.put("xa_rollback_816", "begin ? := JAVA_XA.xa_rollback (?,?); end;");
		calls.put("xa_rollback_post_816", "begin ? := JAVA_XA.xa_rollback_new (?,?,?); end;");
		calls.put("xa_forget_816", "begin ? := JAVA_XA.xa_forget (?,?); end;");
		calls.put("xa_forget_post_816", "begin ? := JAVA_XA.xa_forget_new (?,?,?); end;");

		debugCalls.put("xa_start_816", "begin ? := DEBUG_JAVA_XA.xa_start(?,?,?,?); end;");
		debugCalls.put("xa_start_post_816", "begin ? := DEBUG_JAVA_XA.xa_start_new(?,?,?,?,?); end;");
		debugCalls.put("xa_end_816", "begin ? := DEBUG_JAVA_XA.xa_end(?,?); end;");
		debugCalls.put("xa_end_post_816", "begin ? := DEBUG_JAVA_XA.xa_end_new(?,?,?,?); end;");
		debugCalls.put("xa_commit_816", "begin ? := DEBUG_JAVA_XA.xa_commit (?,?,?); end;");
		debugCalls.put("xa_commit_post_816", "begin ? := DEBUG_JAVA_XA.xa_commit_new (?,?,?,?); end;");
		debugCalls.put("xa_prepare_816", "begin ? := DEBUG_JAVA_XA.xa_prepare (?,?); end;");
		debugCalls.put("xa_prepare_post_816", "begin ? := DEBUG_JAVA_XA.xa_prepare_new (?,?,?); end;");
		debugCalls.put("xa_rollback_816", "begin ? := DEBUG_JAVA_XA.xa_rollback (?,?); end;");
		debugCalls.put("xa_rollback_post_816", "begin ? := DEBUG_JAVA_XA.xa_rollback_new (?,?,?); end;");
		debugCalls.put("xa_forget_816", "begin ? := DEBUG_JAVA_XA.xa_forget (?,?); end;");
		debugCalls.put("xa_forget_post_816", "begin ? := DEBUG_JAVA_XA.xa_forget_new (?,?,?); end;");		
		
		javaXaCalls = Collections.unmodifiableMap(calls);
		javaDebugXaCalls = Collections.unmodifiableMap(debugCalls);
		
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
	
	protected final ClassPool classPool = new ClassPool();
	protected final CtClass stringCtClass;
	protected final Instrumentation instr;
	protected final Set<ClassPath> addedClassPaths = new HashSet<ClassPath>();
	
	private static final AtomicLong tempClassSerial = new AtomicLong(0L);

	/**
	 * Acquires and returns the OracleXADebugTransformer singleton instance
	 * @return the OracleXADebugTransformer singleton instance
	 */
	public static OracleXADebugTransformer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new OracleXADebugTransformer();
				}
			}
		}
		return instance;
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
	
	
	/**
	 * Creates a new OracleXADebugTransformer
	 */
	private OracleXADebugTransformer() {
		try {
			classPool.appendSystemPath();
			stringCtClass = classPool.get(String.class.getName());
			instr = LocalAgentInstaller.getInstrumentation();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Indicates if the class with the passed class name is instrumented
	 * @param className The name of the class to test
	 * @return true if instrumented, false otherwise
	 */
	public boolean isInstrumented(final String className) {
		if(className==null || className.trim().isEmpty()) throw new IllegalArgumentException("The passed class name was null or empty");
		try {
			final Class<?> clazz = Class.forName(className);
			return clazz.getAnnotation(Instrumented.class)!=null;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load class [" + className + "]", ex);
		}
	}
	
	
	protected CtClass insertBehavior(final CtClass existing, final String className, final String behaviorName, final CharSequence code, final Class<?>...signature) throws ClassNotFoundException, NotFoundException {
		if(className==null || className.trim().isEmpty()) throw new IllegalArgumentException("The passed class name was null or empty");
		if(behaviorName==null || behaviorName.trim().isEmpty()) throw new IllegalArgumentException("The passed behavior name was null or empty");
		if(code==null || code.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed code was null or empty");
		final CtClass ctClazz;
		final Class<?> clazz = Class.forName(className.trim());
		if(existing!=null){
			ctClazz = existing; 
		} else {
			CtClass tmp = getCtClass(clazz);
			if(tmp==null) {
				final ClassPath cp = new LoaderClassPath(clazz.getClassLoader());
				addedClassPaths.add(cp);
				classPool.appendClassPath(cp);
				tmp = getCtClass(clazz);
			}
			if(tmp==null) throw new NotFoundException("Failed to load CtClass for [" + clazz.getName() + "]");
			ctClazz = tmp;
		}
		//protected Map<CtClass, Set<CtBehavior>> getMatchedBehaviors(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		
		
		return null;
	}
	
	/**
	 * Returns the signature for a behavior of the passed name with a signature matching the passed type arguments
	 * @param name The behavior name
	 * @param returnType The return type if the behavior is a method
	 * @param typeArgs An class array of types matching the intended signature
	 * @return the behavior descriptor
	 */
	public static String getBehaviorDescriptor(final String name, final Class<?> returnType, final Class<?>...typeArgs) {
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
	 * Matches a constructor name and optional descriptor
	 * @param descriptor The optional descriptor
	 * @param klass The class to inspect
	 * @return the matched ctor
	 */
	protected static CtConstructor matchCtor(final String descriptor, final CtClass klass) {
		if(descriptor==null) {
			final CtConstructor[] ctors = klass.getConstructors();
			if(ctors.length!=1) throw new RuntimeException("Class [" + klass.getName() + "] has multiple ctors and no descriptor was supplied");
			return ctors[0];
		} else {
			try {
				return klass.getConstructor(descriptor);
			} catch (NotFoundException e) {
				throw new RuntimeException("Failed to find ctor for [" + klass.getName() + "] with descriptor [" + descriptor + "]");
			}
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
	 * Finds the matched behaviors 
	 * @param targetClass The class to inspect and traverse from
	 * @param failOnNotFound true to fail on not found, false otherwise
	 * @param sourceMap The map of behaviors names/descriptors and sources
	 * @return A map of sets of target methods keyed by the class they are declared in
	 */
	protected Map<CtClass, Set<CtBehavior>> getMatchedBehaviors(final Class<?> targetClass, final boolean failOnNotFound, final Map<String, String> sourceMap) {
		try {
			final Map<CtClass, Set<CtBehavior>> actualTargets = new HashMap<CtClass, Set<CtBehavior>>();
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
				CtBehavior matched = null;
				try {
					if(ctor) {
						matched = matchCtor(descriptor, targetCtClass);
					} else {
						matched = matchMethod(bname, descriptor, targetCtClass);
					}
				} catch (Exception ex) {
					if(failOnNotFound) throw ex;
				}
				Set<CtBehavior> behaviors = actualTargets.get(matched.getDeclaringClass());
				if(behaviors==null) {
					behaviors = new LinkedHashSet<CtBehavior>();
					actualTargets.put(matched.getDeclaringClass(), behaviors);
				}
				behaviors.add(matched);				
			}
			return actualTargets;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	protected CtClass getCtClass(final Class<?> clazz) {
		return getCtClass(clazz.getName());
	}
	
	protected CtClass getCtClass(final String className) {
		try {
			return classPool.get(className);
		} catch (Exception ex) {
			return null;
		}
	}
	
	
	public static final String redirect(final String call) {
		if(call==null) return call;
		if(call.replace(" ", "").toUpperCase().indexOf(JAVA_XA_CALL_PREFIX)==0) {
			final int index = call.toUpperCase().indexOf(JAVA_XA);
			return new StringBuilder(call).replace(index, index + JAVA_XA_LEN, JAVA_XA_DEBUG).toString();
		}
		return call;
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void main(String[] args) {
		log("redirect test");
		for(Map.Entry<String, String> entry : javaXaCalls.entrySet()) {
			final String key = entry.getKey();
			final String prod = entry.getValue();
			final String transformed = javaDebugXaCalls.get(key);
			log("[" + prod + "] transformed to [" + transformed + "]");
		}
		
	}
	
}


/*
variable jvmrmaction varchar2(30)
execute :jvmrmaction := 'FULL_REMOVAL';
@@jvmrmxa

-- --------------------------------
-- Create the Package
-- --------------------------------

create or replace package DEBUG_JAVA_XA authid current_user as
-- create or replace package DEBUG_JAVA_XA as

   function xa_start (xid_bytes IN RAW, timeout IN NUMBER, 
                      flag IN NUMBER, status OUT NUMBER) 
   return RAW;

   function xa_start_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW,
                          timeout IN NUMBER, flag IN NUMBER)
   return number;

   function xa_end (xid_bytes IN RAW, flag IN NUMBER) 
   return number;

   function xa_end_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW,
                        flag IN NUMBER) 
   return number;

   function xa_commit (xid_bytes IN RAW, commit IN NUMBER, stateout OUT NUMBER)
   return number;

   function xa_commit_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW, 
                           commit IN NUMBER)
   return number;

   function xa_rollback (xid_bytes IN RAW, stateout OUT NUMBER) 
   return number;

   function xa_rollback_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number;

   function xa_forget (xid_bytes IN RAW, stateout OUT NUMBER) 
   return number;

   function xa_forget_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number;

   function xa_prepare (xid_bytes IN RAW, stateout OUT NUMBER) 
   return number;       

   function xa_prepare_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number;       

   function xa_doTwophase (isFinal IN NUMBER, inBytes IN long RAW) 
   return number;

   function xa_thinTwophase (inBytes IN long RAW) 
   return number;

   pragma restrict_references(default, RNPS, WNPS, RNDS, WNDS, trust);

end;
/

REM -------------------------
REM Create the body
REM -------------------------

--  sys.dbms_system.ksdwrt(2, '-- Start Message --');
--  sys.dbms_system.ksdwrt(2, 'Back Trace:' || dbms_utility.format_error_backtrace);


create or replace package body DEBUG_JAVA_XA as

   function xa_start (xid_bytes IN RAW, timeout IN NUMBER, flag IN NUMBER, status OUT NUMBER) 
   return RAW as BEGIN
      return JAVA_XA.xa_start(xid_bytes, timeout, flag, status);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_start error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_start;


   function xa_start_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW,
                          timeout IN NUMBER, flag IN NUMBER)
   return number as BEGIN
      return JAVA_XA.xa_start_new(formatId, gtrid, bqual, timeout, flag);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_start_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_start_new;

   function xa_end (xid_bytes IN RAW, flag IN NUMBER) 
   return number as BEGIN
      return JAVA_XA.xa_end(xid_bytes, flag);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_end error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_end;

   function xa_end_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW,
                        flag IN NUMBER) 
   return number as BEGIN
      return JAVA_XA.xa_end_new(formatId, gtrid, bqual, flag);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_end_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_end_new;


   function xa_commit (xid_bytes IN RAW, commit IN NUMBER, stateout OUT NUMBER)
   return number as BEGIN
      return JAVA_XA.xa_commit(xid_bytes, commit, stateout);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_commit error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_commit;

   function xa_commit_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW, 
                           commit IN NUMBER)
   return number as BEGIN
      return JAVA_XA.xa_commit_new(formatId, gtrid, bqual, commit);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_commit_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_commit_new;


   function xa_rollback (xid_bytes IN RAW, stateout OUT NUMBER) 
   return number as BEGIN
      return JAVA_XA.xa_rollback(xid_bytes, stateout);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_rollback error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_rollback;

   function xa_rollback_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number as BEGIN
      return JAVA_XA.xa_rollback_new(formatId, gtrid, bqual);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_rollback_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_rollback_new;


   function xa_forget ( xid_bytes IN RAW, stateout OUT NUMBER) 
   return number as BEGIN
      return JAVA_XA.xa_forget(xid_bytes, stateout);   
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_forget error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_forget;

   function xa_forget_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number as BEGIN
      return JAVA_XA.xa_forget_new(formatId, gtrid, bqual);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_forget_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_forget_new;

   function xa_prepare (xid_bytes IN RAW, stateout OUT NUMBER) 
   return number as BEGIN
      return JAVA_XA.xa_prepare(xid_bytes, stateout);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_prepare error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_prepare;

   function xa_prepare_new (formatId IN NUMBER, gtrid IN RAW, bqual  IN RAW)
   return number as BEGIN
      return JAVA_XA.xa_prepare_new(formatId, gtrid, bqual);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_prepare_new error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_prepare_new;

   function xa_doTwophase (isFinal IN NUMBER, inBytes IN LONG RAW)
     return number as BEGIN 
      return JAVA_XA.xa_doTwophase(isFinal, inBytes);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_doTwophase error: ' || dbms_utility.format_error_backtrace);
         raise;
   END xa_doTwophase;

    function xa_thinTwophase (inBytes IN LONG RAW)
     return number as BEGIN 
      return JAVA_XA.xa_thinTwophase(inBytes);
      EXCEPTION  WHEN OTHERS THEN
         sys.dbms_system.ksdwrt(2, '[JXA] xa_thinTwophase error: ' || dbms_utility.format_error_backtrace);
         raise;
     END xa_thinTwophase;

end;
/

create public synonym DEBUG_JAVA_XA for DEBUG_JAVA_XA;
grant execute on DEBUG_JAVA_XA to public ;


 */