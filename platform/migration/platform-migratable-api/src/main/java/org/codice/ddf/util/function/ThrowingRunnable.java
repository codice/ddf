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

/**
 * The <code>ThrowingRunnable</code> interface expands on the {@link Runnable} interface to provide
 * the ability to throw back exceptions.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 *
 * @param <E> the type of exceptions that can be thrown by the command
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
  /**
   * Executes user-defined code.
   *
   * @throws E if an error occurs
   */
  public void run() throws E;
}
