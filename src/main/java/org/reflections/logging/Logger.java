/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.reflections.logging;

import java.util.concurrent.Callable;
import java.util.logging.Level;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Title: Logger</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.reflections.logging.Logger</code></p>
 */

public class Logger {
	final java.util.logging.Logger log;
	private static final Cache<String, Logger> loggerCache = CacheBuilder.newBuilder()
			.initialCapacity(128)
			.weakValues()
			.build();
	
	public static Logger getLogger(final Class<?> clazz) {
		final String clazzName = clazz.getName();
		try {
			return loggerCache.get(clazzName, new Callable<Logger>(){
				public Logger call() {
					return  new Logger(java.util.logging.Logger.getLogger(clazzName));
				}
			});
		} catch (Exception ex) {
			throw new RuntimeException("Wow. that should never have happened", ex);
		}
	}
	
	/**
	 * Creates a new Logger
	 */
	private Logger(final java.util.logging.Logger log) {
		this.log = log;
	}
	
	public void debug(final String msg) {
		log.log(Level.FINE, msg);
	}
	
	public void warn(final String msg) {
		log.warning(msg);
	}
	
	public void warn(final String msg, final Throwable t) {
		log.log(Level.WARNING, msg, t);
	}
	
	public void debug(final String msg, final Throwable t) {
		log.log(Level.FINE, msg, t);
	}
	
	public void error(final String msg, final Throwable t) {
		log.log(Level.SEVERE, msg, t);
	}
	
	
	public boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}
	
	public boolean isWarnEnabled() {
		return log.isLoggable(Level.WARNING);
	}

	public void info(final String format) {
		log.info(format);
	}
	

}
