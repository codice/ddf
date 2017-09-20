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
package org.codice.ddf.util.function;

import org.apache.commons.lang.Validate;

/**
 * The <code>BiThrowingConsumer</code> interface expands on the {@link
 * java.util.function.BiConsumer} interface to provide the ability to throw back exceptions.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <E> the type of exceptions that can be thrown by the operation
 */
@FunctionalInterface
public interface BiThrowingConsumer<T, U, E extends Throwable> {
  /**
   * Performs this operation on the given arguments.
   *
   * @param t the first input argument
   * @param u the second input argument
   * @throws E if an error occurs
   */
  public void accept(T t, U u) throws E;

  /**
   * Returns a composed {@code BiThrowingConsumer} that performs, in sequence, this operation
   * followed by the {@code after} operation. If performing either operation throws an exception, it
   * is relayed to the caller of the composed operation. If performing this operation throws an
   * exception, the {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   * @return a composed {@code BiThrowingConsumer} that performs in sequence this operation followed
   *     by the {@code after} operation
   * @throws IllegalArgumentException if {@code after} is <code>null</code>
   * @throws E if an error occurs
   */
  public default BiThrowingConsumer<T, U, E> andThen(
      BiThrowingConsumer<? super T, ? super U, E> after) throws E {
    Validate.notNull(after, "invalid null after");
    return (l, r) -> {
      accept(l, r);
      after.accept(l, r);
    };
  }
}
