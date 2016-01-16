package org.reflections;

import com.google.common.base.Supplier;
import com.google.common.collect.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stores metadata information in multimaps
 * <p>use the different query methods (getXXX) to query the metadata
 * <p>the query methods are string based, and does not cause the class loader to define the types
 * <p>use {@link org.reflections.Reflections#getStore()} to access this store
 */
public class Store {

    private transient boolean concurrent;
    private final Map<String, Multimap<String, String>> storeMap;

    //used via reflection
    @SuppressWarnings("UnusedDeclaration")
    protected Store() {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = false;
    }

    public Store(Configuration configuration) {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = configuration.getExecutorService() != null;
    }

    /** return all indices */
    public Set<String> keySet() {
        return storeMap.keySet();
    }

    /** get or create the multimap object for the given {@code index} */
    public Multimap<String, String> getOrCreate(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            SetMultimap<String, String> multimap =
                    Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(),
                            new Supplier<Set<String>>() {
                                public Set<String> get() {
                                    return Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                                }
                            });
            mmap = concurrent ? Multimaps.synchronizedSetMultimap(multimap) : multimap;
            storeMap.put(index,mmap);
        }
        return mmap;
    }

    /** get the multimap object for the given {@code index}, otherwise throws a {@link org.reflections.ReflectionsException} */
    public Multimap<String, String> get(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            throw new ReflectionsException("Scanner " + index + " was not configured");
        }
        return mmap;
    }

    /** get the values stored for the given {@code index} and {@code keys} */
    public Iterable<String> get(String index, String... keys) {
        return get(index, Arrays.asList(keys));
    }

    /** get the values stored for the given {@code index} and {@code keys} */
    public Iterable<String> get(String index, Iterable<String> keys) {
        Multimap<String, String> mmap = get(index);
        IterableChain<String> result = new IterableChain<String>();
        for (String key : keys) {
            result.addAll(mmap.get(key));
        }
        return result;
    }
    

    /** recursively get the values stored for the given {@code index} and {@code keys}, including keys */
    private Iterable<String> getAllIncluding(String index, Iterable<String> keys, IterableChain<String> result) {
        result.addAll(keys);
        for (String key : keys) {
            Iterable<String> values = get(index, key);
            if (values.iterator().hasNext()) {
                getAllIncluding(index, values, result);
            }
        }
        return result;
    }

    /** recursively get the values stored for the given {@code index} and {@code keys}, not including keys */
    public Iterable<String> getAll(String index, String key) {
        return getAllIncluding(index, get(index, key), new IterableChain<String>());
    }

    /** recursively get the values stored for the given {@code index} and {@code keys}, not including keys */
    public Iterable<String> getAll(String index, Iterable<String> keys) {
        return getAllIncluding(index, get(index, keys), new IterableChain<String>());
    }

    private static class IterableChain<T> implements Iterable<T> {
        private final List<Iterable<T>> chain = Lists.newArrayList();

        private void addAll(Iterable<T> iterable) { chain.add(iterable); }

        public Iterator<T> iterator() { return Iterables.concat(chain).iterator(); }
    }
    
    public Iterable<String> getAny(String index, String...keys) {
        Multimap<String, String> mmap = get(index);
        if(keys.length==0) {
        	return mmap.get("[]");
        }
        
        IterableChain<String> result = new IterableChain<String>();        
        for(String k : mmap.keySet()) {
        	final Set<String> types = splitArray(k);
        	if(!types.isEmpty()) {
        		for(String key: keys) {
        			if(types.contains(key)) {
        				result.addAll(mmap.get(k));
        				break;
        			}
        		}
        	}
        }
        return result;
    }
    
    public Iterable<String> getAll(String index, String...keys) {
        Multimap<String, String> mmap = get(index);
        if(keys.length==0) {
        	return mmap.get("[]");
        }
        IterableChain<String> result = new IterableChain<String>();
        outer:
        for(String k : mmap.keySet()) {
        	final Set<String> types = splitArray(k);
        	if(!types.isEmpty()) {
        		if(types.size() >= keys.length) {
	        		for(String key: keys) {
	        			if(!types.contains(key)) {        				
	        				break outer;
	        			}
	        		}
	        		result.addAll(mmap.get(k));
        		}
        	}
        }
        return result;
    }
    
    
	public static void log(final Object fmt, final Object...args) {
		System.out.println(String.format(fmt.toString(), args));
	}

    
	private static final Set<String> NON_MATCHES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"[]".intern(), "[void]".intern(), "[[]]".intern() 			
	)));
	private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<String>(0));
    
    
	public static Set<String> splitArray(final String v) {
		if(v==null || v.isEmpty() || NON_MATCHES.contains(v)) return EMPTY_SET;
		
		final boolean startWithBrace = v.indexOf('[') == 0;
		if(!startWithBrace) return new HashSet<String>(Arrays.asList(splitString(v, ',')));
		final StringBuilder b = new StringBuilder(v);
		while(b.charAt(0)=='[') {
			b.deleteCharAt(0);
			b.deleteCharAt(b.length()-1);
		}
		return new HashSet<String>(Arrays.asList(splitString(b.toString().trim(), ',')));
	}
	
	  /**
	   * Optimized version of {@code String#split} that doesn't use regexps.
	   * This function works in O(5n) where n is the length of the string to
	   * split.
	   * @param s The string to split.
	   * @param c The separator to use to split the string.
	   * @return A non-null, non-empty array.
	   * <p>Copied from <a href="http://opentsdb.net">OpenTSDB</a>.
	   */
	  public static String[] splitString(final String s, final char c) {
		  
	    final char[] chars = s.toCharArray();
	    int num_substrings = 1;
	    for (final char x : chars) {
	      if (x == c) {
	        num_substrings++;
	      }
	    }
	    final String[] result = new String[num_substrings];
	    final int len = chars.length;
	    int start = 0;  // starting index in chars of the current substring.
	    int pos = 0;    // current index in chars.
	    int i = 0;      // number of the current substring.
	    for (; pos < len; pos++) {
	      if (chars[pos] == c) {	    	
    		result[i++] = new String(chars, start, pos - start);
    		start = pos + 1;
	      }
	    }
	    result[i] = new String(chars, start, pos - start).trim();
	    return result;
	  }
    
    
}
