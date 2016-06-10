package com.heliosapm.aop.retransformer.xa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;

public class OracleXADebugTransformer {
	
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
	}
	
	protected final ClassPool classPool = new ClassPool();
	protected final CtClass stringCtClass;

	/**
	 * Creates a new OracleXADebugTransformer
	 */
	public OracleXADebugTransformer() {
		try {
			classPool.appendSystemPath();
			stringCtClass = classPool.get(String.class.getName());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
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