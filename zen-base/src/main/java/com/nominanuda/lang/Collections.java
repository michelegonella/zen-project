/*
 * Copyright 2008-2011 the original author or authors.
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
package com.nominanuda.lang;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Collections {
	public final static String[] EMPTY_STR_ARR = new String[0];
	
	public static <K, V> Map<V, K> invert(Map<K, V> m) {
		try {
			@SuppressWarnings("unchecked")
			Map<V, K> res = m.getClass().newInstance();
			Set<Entry<K, V>> entrySet = m.entrySet();
			for (Entry<K, V> e : entrySet) {
				res.put(e.getValue(), e.getKey());
			}
			if (entrySet.size() != res.keySet().size()) {
				throw new IllegalArgumentException(
						"map values are not distinct");
			}
			return res;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static <T> List<T> reversedClone(List<T> list) {
		try {
			@SuppressWarnings("unchecked")
			List<T> l2 = list.getClass().newInstance();
			l2.addAll(list);
			java.util.Collections.reverse(l2);
			return l2;
		} catch (Exception e) {
			return null;
		}
	}

	@SafeVarargs
	public static <T> T[] array(T... ts) {
		return ts;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Collection<T> coll, Class<? super T> cl) {
		T[] arr = (T[]) java.lang.reflect.Array.newInstance(cl, coll.size());
		int idx = 0;
		for (T el : coll) {
			arr[idx++] = el;
		}
		return arr;
	}

	@SafeVarargs
	public static <T> List<T> fixedList(T... ts) {
		return Arrays.asList(array(ts));
	}

	public static <T> List<T> linkedList() {
		return new LinkedList<T>();
	}

	@SafeVarargs
	public static <T> LinkedList<T> linkedList(T... ts) {
		LinkedList<T> res = new LinkedList<T>();
		for (T item : ts) {
			res.add(item);
		}
		return res;
	}

	public static <T> HashSet<T> hashSet() {
		return new HashSet<T>();
	}

	@SuppressWarnings("unchecked")
	public static <T> HashSet<T> hashSet(Object... ts) {
		return (HashSet<T>)buildSet(HashSet.class, ts);
	}
	@SuppressWarnings("unchecked")
	public static <T> LinkedHashSet<T> linkedHashSet(Object... ts) {
		return (LinkedHashSet<T>)buildSet(LinkedHashSet.class, ts);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Set<T> buildSet(Class<? extends Set> sclass, Object...ts) {
		Set<? super Object> s;
		try {
			s = sclass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for(Object t : ts) {
			s.add(t);
		}
		return (Set<T>)s;
	}

	@SuppressWarnings("unchecked")
	public static <K,V,T extends Map<K,V>> Map<K,V> linkedHashMap(Object... members) throws RuntimeException {
		return buildMap(LinkedHashMap.class, members);
	}

	@SuppressWarnings("unchecked")
	public static <K,V,T extends Map<K,V>> Map<K,V> hashMap(Object... members) throws RuntimeException {
		return buildMap(HashMap.class, members);
	}

	@SuppressWarnings("unchecked")
	public static <K,V,T extends Map<K,V>> Map<K,V> buildMap(Class<T> mclass, Object... members) throws RuntimeException {
		int len = members.length;
		if(len % 2 != 0) {
			throw new IllegalArgumentException("odd number of arguments");
		}
		Map<K,V> m;
		try {
			m = mclass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for(int i = 0; i < len; i += 2) {
			m.put((K)members[i], (V)members[i+1]);
		}
		return m;
	}

	public static <K,V,T extends Map<K,V>> Map<K,V> entriesToMap(Class<T> mclass, Iterable<Entry<K, V>> entries) throws RuntimeException {
		Map<K,V> m;
		try {
			m = mclass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for(Entry<K, V> e : entries) {
			m.put(e.getKey(), e.getValue());
		}
		return m;
	}
	public static boolean nullOrEmpty(Collection<?> coll) {
		return coll == null || 0 == coll.size();
	}

	public static <T> Set<T> emptySet() {
		return java.util.Collections.emptySet();
	}
	
	public static <T> Iterable<T> emptyIterable() {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return java.util.Collections.emptyIterator();
			}
		};
	}

	public static <T> List<T> asList(Iterable<T> iterable) {
		if (Check.notNull(iterable) instanceof List<?>) {
			return (List<T>)iterable;
		} else {
			List<T> l = new LinkedList<T>();
			for (T t : iterable) {
				l.add(t);
			}
			return l;
		}
	}

	public static <T> boolean find(T needle, Iterable<? extends T> haystack) {
		for (T x : haystack) {
			if (needle.equals(x)) {
				return true;
			}
		}
		return false;
	}

	public static <T> boolean find(T needle, T[] haystack) {
		for (T x : haystack) {
			if (needle.equals(x)) {
				return true;
			}
		}
		return false;
	}

	public static <X,Y> List<Y> mapToList(Collection<X> l, Function<X, Y> mapper) {
		return l.stream().map(mapper).collect(Collectors.toList());
	}
}
