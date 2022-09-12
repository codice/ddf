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
package org.codice.ddf.test.common.xmlunit;

/** This class provides additional support when testing along with XMLUnit 2.0 matchers. */
public class XMLUnitSupport {
  private XMLUnitSupport() {}

  /**
   * Adapts a Java 8 predicate to an XMLUnit predicate one.
   *
   * @param <T> the type of the input to the predicate
   * @param predicate the predicate to adapt
   * @return a corresponding XMLUnit predicate
   */
  public static <T> org.xmlunit.util.Predicate<T> adapt(java.util.function.Predicate<T> predicate) {
    return predicate::test;
  }
}
