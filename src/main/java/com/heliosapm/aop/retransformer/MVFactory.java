/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2015, Helios Development Group and individual contributors
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

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;

/**
 * <p>Title: MVFactory</p>
 * <p>Description: A functional enum for member value types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.aop.retransformer.MVFactory</code></p>
 */

public enum MVFactory implements MemberValueFactory {
	 BYTE(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new ByteMemberValue((Byte)value, pool); }},
	 BOOLEAN(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new BooleanMemberValue((Boolean)value, pool); }},
	 SHORT(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new ShortMemberValue((Short)value, pool); }},
	 CHARACTER(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new CharMemberValue((Character)value, pool); }},
	 INTEGER(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new IntegerMemberValue((Integer)value, pool); }},
	 FLOAT(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new FloatMemberValue((Float)value, pool); }},
	 LONG(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new LongMemberValue((Long)value, pool); }},
	 DOUBLE(){ public MemberValue forValue(final Object value, final ConstPool pool)  { return new DoubleMemberValue((Double)value, pool); }},

	
	
	//     AnnotationMemberValue, ArrayMemberValue, BooleanMemberValue, ByteMemberValue, CharMemberValue, ClassMemberValue, DoubleMemberValue, 
	// 		EnumMemberValue, FloatMemberValue, IntegerMemberValue, LongMemberValue, ShortMemberValue, StringMemberValue
}
