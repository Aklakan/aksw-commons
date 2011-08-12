package org.aksw.commons.collections;

import java.util.*;

/**
 * User: raven
 * Date: 4/17/11
 * Time: 12:36 AM
 */
public class CollectionUtils {

    /**
     * Given an iterable A whose elements are iterables, this method will return the first
     * element of A.
     * If no such element exist, an empty iterable is returned rather than null.
     * 
     *
     * @param iterable
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T extends Iterable<S>> Iterable<S> safeGetFirst(Iterable<T> iterable) {
        Iterator<T> it = iterable.iterator();

        return it.hasNext() ? it.next() : Collections.<S>emptySet();
    }

    public static <T> List<List<T>> chunk(Iterable<T> col, int batchSize)
    {
        List<List<T>> result = new ArrayList<List<T>>();

        List<T> chunk = new ArrayList<T>();

        Iterator<T> it = col.iterator();
        while(it.hasNext()) {
            chunk.add(it.next());

            if(chunk.size() >= batchSize || !it.hasNext()) {
                result.add(chunk);

                if(it.hasNext())
                    chunk = new ArrayList<T>();
            }
        }

        return result;
    }

	public static <T> Set<T> asSet(Collection<T> c)
    {
        return (c instanceof Set) ? (Set<T>)c : new HashSet<T>(c);
    }
 
	/** Transforms an array into a Hashset. @author Konrad Höffner */
	static <T> Set<T> asSet(T[] a)
	{
		Set<T> s = new HashSet<T>();
		for(T e:a) {s.add(e);}
		return s;
	}

	/**Compares elements based on the order of their values in a given map.  
	 * Based on <a href="http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java"/>
	 * Stack Overflow: How to sort a Map<Key, Value> on the values in Java?<a/> .
	 * @author Konrad Höffner */
	static class ValueComparator<S,T extends Comparable<T>> implements Comparator<S>
	{
		Map<S,T> base;
		public ValueComparator(Map<S, T> base) {this.base = base;}

		@Override
		public int compare(S s, S t) {return base.get(s). compareTo(base.get(t));}
	}

}