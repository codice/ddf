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
import javax.annotation.Nullable;

/**
 * This class is used to properly close {@link Closeable}s in case of failures. In case of success,
 * it would disable itself and no longer close the registered {@link Closeable}s.
 *
 * <p>Here is a sample usage of this class:
 *
 * <pre><code>
 *   try (final Closer closer = new Closer()) {
 *     final MyCloseable resource1 = closer.with(new MyCloseable());
 *     final MyCloseable resource2 = closer.with(new MyCloseable());
 *
 *     // do stuff that could potentially throw exceptions out and abort
 *
 *     final MyCloseable resource3 = closer.with(new MyCloseable());
 *
 *     // do more stuff
 *
 *     return closer.returning(new MyManager(resource1, resource2, resource3));
 *   }
 * </code></pre>
 *
 * <p>In the above example, if any errors occurs before the call to {@link #returning(Object)}, the
 * closer will automatically close all registered resources with {@link #with(Closeable)}. Once
 * {@link #returning(Object)} returns, the closer will no longer close any registered resources when
 * the try-with-resources block exits.
 */
public class Closer implements Closeable {
  private final Deque<Closeable> closeables = new ArrayDeque<>(4);

  /**
   * Registers a {@link Closeable} to be later closed if we are unable to return successfully.
   *
   * @param <T> the type of closeable to register
   * @param closeable the closeable to register
   * @return <code>closeable</code> for chaining
   */
  public <T extends Closeable> T with(@Nullable T closeable) {
    if (closeable != null) {
      this.closeables.addFirst(closeable);
    }
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
  public <T> T returning(@Nullable T result) {
    this.closeables.clear();
    return result;
  }

  /**
   * Closes all registered {@link Closeable}s since the creation or since the last call to {@link
   * #returning} whichever came first.
   */
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
