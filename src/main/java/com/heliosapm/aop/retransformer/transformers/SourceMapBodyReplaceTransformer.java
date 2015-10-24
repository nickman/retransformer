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
package com.heliosapm.aop.retransformer.transformers;

import javassist.CannotCompileException;
import javassist.CtBehavior;

/**
 * <p>Title: SourceMapBodyReplaceTransformer</p>
 * <p>Description: A {@link SourceMapTransformer} that replaces the body of tranformed methods with the text of the source map</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.transformers.SourceMapBodyReplaceTransformer</code></p>
 */

public class SourceMapBodyReplaceTransformer extends SourceMapTransformer {
	/** Public shareable strict instance */
	public static final SourceMapBodyReplaceTransformer STRICT_INSTANCE = new SourceMapBodyReplaceTransformer(true);
	/** Public shareable non-strict instance */
	public static final SourceMapBodyReplaceTransformer INSTANCE = new SourceMapBodyReplaceTransformer(false);

	/**
	 * Creates a new SourceMapBodyReplaceTransformer
	 * @param strict true for a strict transformer, false otherwise
	 */
	public SourceMapBodyReplaceTransformer(final boolean strict) {
		super(strict);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.aop.retransformer.transformers.SourceMapTransformer#transform(javassist.CtBehavior, java.lang.String)
	 */
	@Override
	protected void transform(final CtBehavior behavior, final String source) throws CannotCompileException {
		behavior.setBody(source);
	}

	

}
