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
package ddf.catalog.transformer.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * The standard ForkJoinPoolFactory is incompatible with the OSGi security manager. This one uses
 * one to create standard worker threads to avoid the issue.
 */
public class ForkJoinPoolFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
  public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
    return new StandardForkJoinWorkerThread(pool);
  }

  public static ForkJoinPool getNewForkJoinPool(
      Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
    return new ForkJoinPool(
        Math.min(0x7fff, Runtime.getRuntime().availableProcessors()),
        new ForkJoinPoolFactory(),
        handler,
        asyncMode);
  }

  static class StandardForkJoinWorkerThread extends ForkJoinWorkerThread {

    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    protected StandardForkJoinWorkerThread(ForkJoinPool pool) {
      super(pool);
    }
  }
}
