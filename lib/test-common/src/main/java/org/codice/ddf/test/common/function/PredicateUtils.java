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
package org.codice.ddf.test.common.function;

import java.util.function.Predicate;
import java.util.stream.Stream;

/** This class provides utilities for working with predicates. */
public class PredicateUtils {
  private PredicateUtils() {}

  /**
   * Returns a new predicate that will report <code>true</code> if all provided predicates report
   * <code>true</code> for the tested argument.
   *
   * @param <T> the type of object being tested
   * @param predicates the set of predicates to test with
   * @return a new predicate that expects all given predicates to return <code>true</code> for the
   *     tested argument in order to return <code>true</code>
   */
  public static <T> Predicate<T> allOf(Predicate<T>... predicates) {
    return n -> Stream.of(predicates).allMatch(p -> p.test(n));
  }

  /**
   * Returns a new predicate that will report <code>true</code> if any of the provided predicates
   * report <code>true</code> for the tested argument.
   *
   * @param <T> the type of object being tested
   * @param predicates the set of predicates to test with
   * @return a new predicate that expects any given predicates to return <code>true</code> for the
   *     tested argument in order to return <code>true</code>
   */
  public static <T> Predicate<T> anyOf(Predicate<T>... predicates) {
    return n -> Stream.of(predicates).anyMatch(p -> p.test(n));
  }
}
