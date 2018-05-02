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
package net.jodah.failsafe;

import net.jodah.failsafe.function.CheckedBiFunction;
import net.jodah.failsafe.util.concurrent.Scheduler;

/**
 * This class delegates all configuration methods to a master in addition to the local object; thus
 * allowing another failsafe config to be kept up to date.
 *
 * @param <R> The result type
 */
public class AsyncFailsafeConfigDelegater<R> extends AsyncFailsafe<R> {
  private final FailsafeConfig<R, ?> master;

  public AsyncFailsafeConfigDelegater(FailsafeConfig<R, ?> master, Scheduler scheduler) {
    super(master, scheduler);
    // copy all config from master
    super.retryPolicy = master.retryPolicy;
    super.circuitBreaker = master.circuitBreaker;
    super.fallback = master.fallback;
    super.listeners = master.listeners;
    super.listenerRegistry = master.listenerRegistry;
    this.master = master;
  }

  public Scheduler getOriginalScheduler() {
    if (master instanceof AsyncFailsafeConfigDelegater) {
      return ((AsyncFailsafeConfigDelegater<R>) master).getOriginalScheduler();
    }
    return super.scheduler;
  }

  @SuppressWarnings("squid:S1452" /* this is how Failsafe define it */)
  public FailsafeConfig<R, ?> getMaster() {
    return master;
  }

  @Override
  public AsyncFailsafe<R> with(CircuitBreaker circuitBreaker) {
    master.with(circuitBreaker);
    return super.with(circuitBreaker);
  }

  @Override
  public <T> AsyncFailsafe<R> with(Listeners<T> listeners) {
    master.with(listeners);
    return super.with(listeners);
  }

  @Override
  public AsyncFailsafe<R> with(RetryPolicy retryPolicy) {
    master.with(retryPolicy);
    return super.with(retryPolicy);
  }

  // overriding this withFallback() will catch all other withFallback() methods
  @Override
  public AsyncFailsafe<R> withFallback(
      CheckedBiFunction<? extends R, ? extends Throwable, ? extends R> fallback) {
    master.withFallback(fallback);
    return super.withFallback(fallback);
  }

  // overriding registry() will intercept all listeners adding methods
  @Override
  ListenerRegistry<R> registry() {
    final ListenerRegistry<R> listenerRegistry = master.registry();

    // make sure we use the same registry
    super.listenerRegistry = listenerRegistry;
    return listenerRegistry;
  }

  /**
   * Gets the original scheduler defined for the given {@link AsyncFailsafe} object. This will be
   * its original scheduler if it is a config delegater. Otherwise it will be its defined scheduler.
   *
   * @param async the async for which to retrieve its original scheduler
   * @return the corresponding original scheduler
   */
  public static Scheduler getOriginalScheduler(AsyncFailsafe<?> async) {
    if (async instanceof AsyncFailsafeConfigDelegater) {
      return ((AsyncFailsafeConfigDelegater<?>) async).getOriginalScheduler();
    }
    return async.scheduler;
  }
}
