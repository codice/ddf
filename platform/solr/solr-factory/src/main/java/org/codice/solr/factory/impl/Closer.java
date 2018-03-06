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
package org.codice.solr.factory.impl;

import com.google.common.io.Closeables;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * This class is used to properly close {@link Closeable}s in case of failures. In case of success,
 * it would disables itself and no longer close the registered {@link Closeable}.
 */
public class Closer implements Closeable {
  private final Deque<Closeable> closeables = new ArrayDeque<>(4);

  /** Creates a new closer class. */
  public Closer() {}

  /**
   * Registers a {@link Closeable} to be later closed if we are unable to return successfully.
   *
   * @param <T> the type of closeable to register
   * @param closeable the closeable to register
   * @return <code>closeable</code> for chaining
   */
  public <T extends Closeable> T with(T closeable) {
    this.closeables.addFirst(closeable);
    return closeable;
  }

  /**
   * Indicates we are returning successfully the provided result and that all registered closeables
   * should no longer be closed.
   *
   * @param <T> the type of the returned result
   * @param result the result to be returned
   * @return <code>result</code> for chaining
   */
  public <T> T returning(T result) {
    this.closeables.clear();
    return result;
  }

  @Override
  public void close() {
    while (!closeables.isEmpty()) {
      try {
        Closeables.close(closeables.removeFirst(), true);
      } catch (IOException e) { // exceptions are suppressed above
      }
    }
  }
}
