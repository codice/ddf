/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration;

import java.io.Serializable;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;

/**
 * A HashMap based Dictionary for use with OSGI methods requiring a Dictionary
 *
 * @param <K>
 * @param <V>
 */
public class DictionaryMap<K, V> extends Dictionary<K, V> implements Map<K, V>, Serializable {
  private HashMap<K, V> map;

  public DictionaryMap(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
  }

  public DictionaryMap(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
  }

  public DictionaryMap() {
    map = new HashMap<>();
  }

  public DictionaryMap(Map<? extends K, ? extends V> m) {
    map = new HashMap<>(m);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Enumeration<K> keys() {
    return IteratorUtils.asEnumeration(map.keySet().iterator());
  }

  @Override
  public Enumeration<V> elements() {
    return IteratorUtils.asEnumeration(map.values().iterator());
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  public static <K, V> DictionaryMap<K, V> of(K k1, V v1) {
    return fromEntries(new Pair[] {Pair.of(k1, v1)});
  }

  public static <K, V> DictionaryMap<K, V> of(K k1, V v1, K k2, V v2) {
    return fromEntries(new Pair[] {Pair.of(k1, v1), Pair.of(k2, v2)});
  }

  public static <K, V> DictionaryMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return fromEntries(new Pair[] {Pair.of(k1, v1), Pair.of(k2, v2), Pair.of(k3, v3)});
  }

  private static <K, V> DictionaryMap<K, V> fromEntries(Pair<K, V>[] pairs) {
    DictionaryMap<K, V> map = new DictionaryMap<>();
    for (Pair<K, V> pair : pairs) {
      map.put(pair.key, pair.value);
    }
    return map;
  }

  private static class Pair<K, V> {
    K key;
    V value;

    Pair(K k, V v) {
      this.key = k;
      this.value = v;
    }

    static <K, V> Pair of(K k, V v) {
      return new Pair(k, v);
    }
  }
}
