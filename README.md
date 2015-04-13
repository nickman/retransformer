# retransformer
### {Java Bytecode AOP for people with no time for AOP}
Simple Method Replacement for Java Using Retransforms

Retransformer is intended as a testing tool and allows for the runtime modification of methods in targeted java classes. This can simplify testing procedure by allowing modifications to loaded objects and business classes to stub out functionality where the native functionality may not be available in the test environment.

#### Shutup and get to the point

Ok. Assume a class called English which returns words in English, and the same for Spanish. Here's how to graft Spanish methods into the English class:
```java
import com.heliosapm.aop.retransformer.Retransformer;
.....   
final English english = new English();   
Assert.assertEquals("Hello", english.getHello());   
Retransformer.getInstance().transform(English.class, Spanish.class);   
Assert.assertEquals("Hola", english.getHello());   
// Whaaat ? Restore the english stuff
Retransformer.getInstance().restore(English.class);
```

To build:
 1. Clone this repository:  ```git clone https://github.com/nickman/retransformer.git``` 
 2. Run a maven [3] build:  ```mvn clean install```

#### Back to the scheduled pace.

 
Retransformer uses the Java Instrumentation API to issue retransform requests at runtime. The high level requirements to modify a class via retransforms are:
 * A refererence to a java.lang.intrument.Intsrumentation object. Retransformer provides 2 different ways of acquiring one. 
 * The class must be modifiable. In most cases, application classes will be. System classes loaded from the bootstrap or primordial class loader will usually not be modifiable. The modification cannot change the class schema. This pretty much means that all changes must be tucked inside the body of a method and method signatures, inheritance structures and access levels cannot be changed. 
 
# Benefits
 
There's a few different ways to modify behavior in classes to be tested. A similar style is called [PowerMock](https://code.google.com/p/powermock/). It sort of does the same thing but requires the use of custom JUnit test runners and other bits and pieces. I wrote Retransformer because I thought it was easier to use with less overhead. Having said that, PowerMock has a much larger agenda and does a lot more.
One of the difficult to replicate benefits is that when a class is retransformed, all of its objects change with it, regardless of where they are (as long as they are resident in the JVM and from the same classloader) 
Classes with a large number of methods can be arduous to mock, but retransformations allows you to replace only the ones you need to for the current test. 
 
# Usage
 
Starting with an example from the Retransformer's unit tests, consider the class **English** which provides some key words in English. We will use Retransformer to seamlessly change the returned values to Spanish. There are 2 main ways of doing this:
Provide a reference method (a mock method) from another class which will replace the target 
Provide the source code which will be compiled and replace the target. 
 
### Sample Class (English)
```java
public class English {   
      
    final String cutlery = "spoon";   
    String candy = "butterscotch";   
      
    public String getHello() {   
        return "Hello";   
    }   
      
    public String getCutlery() {   
        return this.cutlery;   
    }   
      
    public String getCandy() {   
        return candy;   
    }   
   
    public String getLanguage() {   
        return language();   
    }   
      
    private String language() {   
        return "English";   
    }   
      
    public String keyPhrase() {   
        return "This is the key";   
    }   
}   
``` 
# Reference Method Replacement
 
To perform this replacement, we'll use a test method called **Spanish** (only partially shown here)

```java
public class Spanish {   
    final String cutlery = "cuchara";   
    String candy = "pulparindo";   
      
    public String getHello() {   
        return "Hola";   
    }   
    // ....<snip>....   
}   
```

We're going to swap the method *getHello()* from Spanish and graft it into English:
 
```java
import com.heliosapm.aop.retransformer.Retransformer;
.....   
final English english = new English();   
Assert.assertEquals("Hello", english.getHello());   
Retransformer.getInstance().transform(English.class, Spanish.class);   
Assert.assertEquals("Hola", english.getHello());   
```

 
In this case, the transform grafted all methods from Spanish  into English. It occurs near-instantly and it takes effect on all objects that are instances of the transformed class that originated from the same classloader.
(One caveat to this, from the javadoc:  ***If a retransformed method has active stack frames, those active frames continue to run the bytecodes of the original method. The retransformed method will be used on new invokes.*** ) Maybe not so much a caveat... I mean that seems to be the best of all possible implementations, if you ask me. 
 
Retransformed classes can be retransformed again (old changes will be discarded) and can be restored back to its original state by calling restore(clazz).  e.g.
 
```java
final English english = new English();   
Assert.assertEquals("Hello", english.getHello()); // We're speaking English   
Retransformer.getInstance().transform(English.class, Spanish.class);   
Assert.assertEquals("Hola", english.getHello());  // We're speaking Spanish   
Retransformer.getInstance().restore(English.class);   
Assert.assertEquals("Hello", english.getHello()); // We're speaking English again   
```
 
To suppress the grafting of a reference class method, it can be annotated with the @MethodIgnore annotation. e.g.
 
```java
@MethodIgnore  // This method will never be grafted   
public String keyPhrase() {   
    return "Esta es la llave";   
}   
```
 
And an example in action:
 
```java
/**  
 * Tests that @MethodIgnore methods in a mock replacement class are not woven.  
 */   
@Test   
public void testMethodIgnore() {   
    final English english = new English();   
    Assert.assertEquals("Hello", english.getHello());   // We're speaking English   
    Assert.assertEquals("This is the key", english.keyPhrase());   // Still English   
    retran.transform(English.class, Spanish.class);   
    Assert.assertEquals("Hola", english.getHello());    // We're speaking Spanish   
    Assert.assertEquals("This is the key", english.keyPhrase());  // Still in English, keyPhrase not swapped since it was @MethodIgnore   
}   
```
# Inner/Anonymous Reference Method Replacement
 
This Reference Source Replacement method replacement is probably the simplest, but in some cases, replacing a method with another based on a code snippet is useful too, and sometimes preferable. In many cases, you may not even need something as formal an externally defined reference class. Methods can be grafted from inner, and even anonymous classes. In this example (also in the unit tests), we'll switch from English into Anonymous (Java.... ?)
 
```java
final English english = new English();   
Assert.assertEquals("English", english.getLanguage());   // We're speaking English   
retran.transform(English.class, new Object(){   
    private String language() {   
        return "CAFEBABE";   
    }               
}.getClass());   
Assert.assertEquals("CAFEBABE", english.getLanguage()); // We're speaking Java   
```

 
# Source Code Driven Retransforms
 
The premise here is almost the same except instead of supplying a class with methods to graft, we're supplying a method name and the source code. Specifying source code for Retransformer is a little bit different than pure Java, since the javac cleans up some otherwise odd bit here and there. See the appendix for an outline of differences and how to work around. The difference is not dramatic and you should not have any difficulty in understanding working examples.
 
In this example, an inner class called Joiner has a method join which concatenates an int, a long and a string in a specific order. This unit test juggles the order in which this occurs using source retransforms:

```java
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
```
 
These source snippets highlight a few important java source standouts:
 * A method block must start with a **{** and end with a **}**.  (this is an example of the syntactic sugar javac takes care of) 
 * Cross typed variable arguments don't work since nothing is auto-boxed.  (another javac luxury) So rather than calling the vararg pattern as on line 7, we need to create an actual array of objects as on line 11 and 13. 
 * Parameters are referred to by the notation **$#** where **#** is the sequence of the parameter, starting at 1, because **$0** means *this*. 
 * Primitives must be cast in the absence of autoboxing (like Java 1.4.....) but Javassist provides a token for that when casting primitives to their wrapper object types:  (**$w**). 
 
Most of the limitations are outlined well in the Javadoc. Other non-obvious but achievable operations are:
 
 * Automatically replace the parent class's method when transforming the child if the method is defined in the parent. 
 * There's no issue retransforming private methods. Retransformer is very nosy. 
 * Be cautious about local field access.  Basically, if a method returns the value of a local field, and you graft that method, some behavior changes depending on whether or not the field is final. (the compiler  is aware of finality and will optimize certain things, assuming nothing will change) (See test case testNonReplacedFieldAccess) 
 
 
Here's some additional reference:
 
 * [java.lang.intrument](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)  (interesting read, really...) 
 * [Javassist JavaDoc](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/html/index.html)  (javadoc is your friend. Keep it handy. Read it.)
 * [Javassit Tutorial](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/tutorial/tutorial.html) (a bit more readable than the javadoc, and also, the 2nd page is a great reference source for javassist tokens etc.) 


Got a question ?  Found a bug ?  Want a new feature ?  Write me a [ticket](https://github.com/nickman/retransformer/issues/new) !


