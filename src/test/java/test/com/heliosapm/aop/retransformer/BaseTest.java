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

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * <p>Title: BaseTest</p>
 * <p>Description: Base test class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.aop.retransformer.BaseTest</code></p>
 */

public class BaseTest {
	/** The currently executing test name */
	@Rule public final TestName name = new TestName();

	/**
	 * Prints the test name about to be executed
	 */
	@Before
	public void printTestName() {
		log("\n\t==================================\n\tRunning Test [%s]\n\t==================================\n", name.getMethodName());
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
