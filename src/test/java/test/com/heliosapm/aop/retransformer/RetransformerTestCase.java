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
package test.com.heliosapm.aop.retransformer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import test.com.heliosapm.aop.retransformer.testclasses.Child;
import test.com.heliosapm.aop.retransformer.testclasses.English;
import test.com.heliosapm.aop.retransformer.testclasses.Spanish;

import com.heliosapm.aop.retransformer.Retransformer;

/**
 * <p>Title: RetransformerTestCase</p>
 * <p>Description: Retransformer test cases</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.aop.retransformer.RetransformerTestCase</code></p>
 */

public class RetransformerTestCase extends BaseTest {
	/** The Retransformer instance */
	protected final Retransformer retran;
	
	/**
	 * Creates a new RetransformerTestCase
	 */
	public RetransformerTestCase() {
		try {
			retran = Retransformer.getInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get Retransformer", ex);
		}
	}
	
	/**
	 * Restores the English class
	 */
	@Before
	public void resetEnglish() {
		resetAndValidate();
	}
	
	/**
	 * Resets the English class and validates
	 */
	public void resetAndValidate() {
		retran.restore(English.class);
		final English english = new English();
		Assert.assertEquals("Hello", english.getHello());
		Assert.assertEquals("butterscotch", english.getCandy());
		Assert.assertEquals("spoon", english.getCutlery());
		
	}
	
	/**
	 * Tests the replacement of an external class method replacement from another external class
	 */
	@Test
	public void testExternalClassMockedTransform() {
		final English english = new English();
		Assert.assertEquals("Hello", english.getHello());
		retran.transform(English.class, Spanish.class);
		Assert.assertEquals("Hola", english.getHello());
		resetAndValidate();
	}
	
	/**
	 * Tests the replacement of an internal class method replacement from another internal class
	 */
	@Test
	public void testInnerClassMockedTransform() {
		class EnglishInner {
			public String getHello() {
				return "Hello";
			}
		}
		class SpanishInner {
			@SuppressWarnings("unused")
			public String getHello() {
				return "Hola";
			}
		}
		
		final EnglishInner english = new EnglishInner();
		Assert.assertEquals("Hello", english.getHello());
		retran.transform(EnglishInner.class, SpanishInner.class);
		Assert.assertEquals("Hola", english.getHello());
		resetAndValidate();
	}
	
	/**
	 * Tests the replacement of an external class method replacement from an inner class
	 */
	@Test
	public void testExternalClassMockedInternalTransform() {
		class SpanishInner {
			@SuppressWarnings("unused")
			public String getHello() {
				return "Hola!";
			}
		}
		
		final English english = new English();
		Assert.assertEquals("Hello", english.getHello());
		retran.transform(English.class, SpanishInner.class);
		Assert.assertEquals("Hola!", english.getHello());
		resetAndValidate();
	}
	
	/**
	 * Tests the replacement of an external class method replacement from an inner class
	 */
	@Test
	public void testInnerClassMockedExternalTransform() {
		class EnglishInner {
			public String getHello() {
				return "Hullo";
			}
		}
		
		final EnglishInner english = new EnglishInner();
		Assert.assertEquals("Hullo", english.getHello());
		retran.transform(EnglishInner.class, Spanish.class);
		Assert.assertEquals("Hola", english.getHello());
		resetAndValidate();		
	}
	
	/**
	 * Tests the replacement of an external class method replacement from an anonymous class
	 */
	@Test
	public void testExternalClassMockedAnnonTransform() {		
		final English english = new English();
		Assert.assertEquals("Hello", english.getHello());
		retran.transform(English.class, new Object(){
			@SuppressWarnings("unused")
			public String getHello() {
				return "CAFEBABE";
			}			
		}.getClass());
		Assert.assertEquals("CAFEBABE", english.getHello());
		resetAndValidate();		
	}

	/**
	 * Tests the replacement of an external class method replacement from source
	 */
	@Test
	public void testExternalClassSourceTransform() {
		final English english = new English();
		Assert.assertEquals("Hello", english.getHello());
		final String randomHello = "" + System.currentTimeMillis();
		retran.transform(English.class, "getHello", "{ return \"" + randomHello + "\"; }");
		Assert.assertEquals(randomHello, english.getHello());
		resetAndValidate();
	}
	
	/**
	 * Tests that local field access is replaced with method replacement if the field is final, but not source.
	 * If the field is not final, then field access is replaced.
	 */
	@Test
	public void testNonReplacedFieldAccess() {
		final English english = new English();
		Assert.assertEquals("spoon", english.getCutlery());
		retran.transform(English.class, Spanish.class);
		Assert.assertEquals("Hola", english.getHello());
		Assert.assertEquals("cuchara", english.getCutlery());  // swapping the method also swaps the field value since it's final
		resetAndValidate();		
		retran.transform(English.class, "getCutlery", "{ return this.cutlery; }");
		Assert.assertEquals("Hello", english.getHello());
		Assert.assertEquals("spoon", english.getCutlery());  // compiling a new method does NOT swap the field value
		resetAndValidate();		
		retran.transform(English.class, Spanish.class);
		Assert.assertEquals("Hola", english.getHello());
		Assert.assertEquals("cuchara", english.getCutlery());
		Assert.assertEquals("butterscotch", english.getCandy());   // compiling a new method does NOT swap the field value since it was not final
	}

	/**
	 * Tests that a private method can be transformed
	 */
	@Test
	public void testPrivateMethodTransform() {
		final English english = new English();
		Assert.assertEquals("English", english.getLanguage());
		retran.transform(English.class, new Object(){
			@SuppressWarnings("unused")
			private String language() {   //  <--- the replacement must be private too
				return "CAFEBABE";
			}			
		}.getClass());
		Assert.assertEquals("CAFEBABE", english.getLanguage());
		resetAndValidate();		
	}

	/**
	 * Cannot change the access level of a method in a retransform, but body will be updated
	 * @throws Exception Thrown on reflection failure
	 */
	@Test
	public void testAccessLevelChangeReTransform() throws Exception {
		final English english = new English();
		Assert.assertEquals("English", english.getLanguage());
		retran.transform(English.class, new Object(){
			@SuppressWarnings("unused")
			public String language() {    // changing method to public from private won't work
				return "CAFEBABE";
			}			
		}.getClass());
		Assert.assertEquals("CAFEBABE", english.getLanguage());
		Method m = English.class.getDeclaredMethod("language");
		Assert.assertTrue(Modifier.isPrivate(m.getModifiers()));
		
		retran.transform(English.class, "language", "{ return \"Foo\"; }");
		Assert.assertEquals("Foo", english.getLanguage());
		m = English.class.getDeclaredMethod("language");
		Assert.assertTrue(Modifier.isPrivate(m.getModifiers()));
		
		resetAndValidate();		
	}
	
	/**
	 * Cannot change the signature of a method in a retransform
	 */
	@Test(expected=VerifyError.class)
	public void testFailOnMethodSignatureChange() {
		final English english = new English();
		Assert.assertEquals("English", english.getLanguage());
		retran.transform(English.class, new Object(){
			@SuppressWarnings("unused")
			public int language() {    // changing return type will fail
				return 5;
			}			
		}.getClass());
	}

	/**
	 * Modifying return value using parameters
	 */
	@Test
	public void testParamManipulation() {
		final class Joiner {
			public String join(final int x, final long q, final String s) {
				return String.format("%s-%s-%s", x, q, s);
			}
		}
		final Joiner joiner = new Joiner();
		Assert.assertEquals("5-1024-Foo", joiner.join(5, 1024L, "Foo"));
		// Javassist is sticky about autoboxing and varargs, so:
		// 1. We need to pass the String.format arguments as an Object[] instance
		// 2. We need to use the ($w) expression to explicitly cast the primitives to the full object counterparts. 
		retran.transform(Joiner.class, "join", "{return String.format(\"%s-%s-%s\", new Object[]{$3, ($w)$2, ($w)$1});}");		
		Assert.assertEquals("Foo-1024-5", joiner.join(5, 1024L, "Foo"));
		retran.transform(Joiner.class, "join", "{return String.format(\"%s-%s-%s\", new Object[]{($w)$2, $3, ($w)$1});}");
		Assert.assertEquals("1024-Foo-5", joiner.join(5, 1024L, "Foo"));
	}
	
	/**
	 * Implicit replacement of a parent class declared method
	 */
	@Test
	public void testParentMethodReplacement() {
		Child child = new Child();
		Assert.assertEquals(7168, child.doOp(1024, 2048, 4096));
		// Javassist is sticky about for loops, so we need to iterate on the array
		retran.transform(Child.class, "op", "{long t = 1; for(int i = 0; i < $1.length; i++) { t *= $1[i]; } return t;}");
		Assert.assertEquals(8589934592L, child.doOp(1024, 2048, 4096));
	}
	
	
	/*
	 * By Source
	 * Ext class
	 * Inner class
	 * Anon class
	 * Params example
	 * Returns example
	 * Local Field Access
	 * Instr Provider
	 * @MethodIgnore
	 */
}
