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
package net.jodah.failsafe.internal.monitor;

import java.util.concurrent.ExecutorService;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.event.ContextualResultListener;
import net.jodah.failsafe.internal.util.Assert;

/**
 * A wrapper class for listeners that are called using an executor service so we can attach a thread
 * monitor once an execution starts.
 *
 * @param <R> the result type
 * @param <F> the expected error
 */
public class MonitoredContextualResultListener<R, F extends Throwable>
    implements ContextualResultListener<R, F> {
  private final ContextualResultListener<R, F> listener;
  private final ExecutorService executor;

  /**
   * Constructs a monitored contextual result listener based on the given one and using the
   * specified executor to call it back later.
   *
   * @param listener the listener to be called later
   * @param executor the executor to use when calling the listener
   * @throws NullPointerException if any of the arguments are <code>null</code>
   */
  public MonitoredContextualResultListener(
      ContextualResultListener<R, F> listener, ExecutorService executor) {
    Assert.notNull(listener, "listener");
    Assert.notNull(executor, "executor");
    this.listener = listener;
    this.executor = executor;
  }

  @SuppressWarnings("squid:S1181" /* bubbling up VirtualMachineError first */)
  @Override
  public void onResult(R result, F failure, ExecutionContext context) throws Exception {
    ThreadMonitor.current()
        .monitor(
            () -> {
              listener.onResult(result, failure, context);
              return null;
            },
            executor);
  }
}
