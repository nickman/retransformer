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
package com.heliosapm.aop.retransformer.transformers;

import java.util.Map;
import java.util.logging.Level;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * <p>Title: SourceMapTransformer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.transformers.SourceMapTransformer</code></p>
 */

public abstract class SourceMapTransformer extends AbstractTransformer<Map<String, String>> {
	
	/** Indicates if this transformer is strict */
	private final boolean strict;
	
	/**
	 * Applies the source map source code to the associated passed behavior
	 * @param behavior The behavior to transform
	 * @param source The source code associated to the target behavior
	 * @throws CannotCompileException thrown if your source can't be compiled, yo.
	 */
	protected abstract void transform(final CtBehavior behavior, final String source) throws CannotCompileException;
	
	
	/**
	 * For each class behaviour encoded in the key of the source map, the corresponding value
	 * string will be set as the body of that behavior in the passed {@link CtClass}. The behavior
	 * encoding which supports Methods ({@link CtMethod}) and Constructors ({@link CtConstructor}) is as follows:<ul>
	 * 	<li>Methods:<b><code>&lt;Method Name&gt;[:&lt;Method Descriptor&gt;]</code></b>. If the descriptor is absent, we will match the parameterless method of the given name.</li>
	 *  <li>Constructors:<b><code>[:&lt;Method Descriptor&gt;]</code></b>. If the descriptor is absent, we will match the parameterless constructor of the given class.</li>
	 * </ul>
	 * @param ct The CtClass to transform
	 * @param sourceMap The source map
	 * @return the [possibly] transformed CtClass
	 */
	@Override
	public CtClass transform(final CtClass ct, final TransformContext tc, final Map<String, String> sourceMap) {
		if(ct==null) throw new IllegalArgumentException("The passed CtClass was null");
		if(sourceMap!=null && !sourceMap.isEmpty()) {
			final Map<CtBehavior, String> indexed = indexSourceMap(ct, sourceMap);
			for(Map.Entry<CtBehavior, String> entry: indexed.entrySet()) {
				try {
					transform(entry.getKey(), entry.getValue());
				} catch (CannotCompileException ex) {
					if(strict) throw new RuntimeException("<STRICT MODE> Failed to transform [" + entry.getKey().getGenericSignature() + "] with source [" + entry.getValue() + "]", ex);
					if(log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "<STRICT MODE> Failed to transform [" + entry.getKey().getGenericSignature() + "] with source [" + entry.getValue() + "]", ex);
					}
				}
			}			
		}
		return ct;
	}
	
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.aop.retransformer.transformers.AbstractTransformer#isStrict()
	 */
	@Override
	public boolean isStrict() {
		return strict;
	}
	
	/**
	 * Creates a new SourceMapTransformer
	 * @param strict true for a strict transformer, false otherwise
	 */
	protected SourceMapTransformer(final boolean strict) {
		this.strict = strict;
	}

}
