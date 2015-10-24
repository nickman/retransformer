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

import javassist.CtClass;

/**
 * <p>Title: ITransformer</p>
 * <p>Description: Defines a <a href="">Javassist</a> {@link CtClass} transformer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.transformers.ITransformer</code></p>
 * @param <T> The type of the transformer's directive set
 */

public interface ITransformer<T> {
	/**
	 * Indicates if this transformer is strict, meaning it will fail if any of its directives fail.
	 * Otherwise the transformer is non-strict and will ignore failed directives
	 * @return true if this transformer is strict, false otherwise
	 */
	public boolean isStrict();
	
	/**
	 * Invokes a transformer
	 * @param ct The CtClass to transform
	 * @param tc The transform context
	 * @param directiveSet The type specific transform directives
	 * @return the [possibly] transformed CtClass
	 */
	public CtClass transform(final CtClass ct, final TransformContext tc, final T directiveSet);
}
