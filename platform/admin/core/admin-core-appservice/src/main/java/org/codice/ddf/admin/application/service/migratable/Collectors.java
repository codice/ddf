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
package org.codice.ddf.admin.application.service.migratable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * The <code>Collectors</code> class extends on the {@link java.util.stream.Collectors} class to
 * provide additional collectors.
 */
public class Collectors {
  private Collectors() {}

  /**
   * Returns a merge function, suitable for use in {@link Map#merge(Object, Object,
   * java.util.function.BiFunction) Map.merge()} or {@link
   * java.util.stream.Collectors#toMap(Function, Function, BinaryOperator) toMap()}, which always
   * throws {@link IllegalStateException}. This can be used to enforce the assumption that the
   * elements being collected are distinct.
   *
   * @param  <T> the type of input arguments to the merge function
   * @return a merge function which always throw <code>IllegalStateException</code>
   */
  public static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

  /**
   * Returns a {@link Collector} that accumulates elements into a {@link LinkedHashMap} whose keys
   * and values are the result of applying the provided mapping functions to the input elements.
   *
   * @param <T> the type of the input elements
   * @param <K> the output type of the key mapping function
   * @param <U> the output type of the value mapping function
   * @param keyMapper a mapping function to produce keys
   * @param valueMapper a mapping function to produce values
   * @return a {@link Collector} which collects elements into a {@link LinkedHashMap} whose keys and
   *     values are the result of applying mapping functions to the input elements
   */
  @SuppressWarnings(
      "squid:S1452" /* standard way of doing it as per java.util.stream.Collectors.toMap() */)
  public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
    return java.util.stream.Collectors.toMap(
        keyMapper, valueMapper, Collectors.throwingMerger(), LinkedHashMap::new);
  }
}
