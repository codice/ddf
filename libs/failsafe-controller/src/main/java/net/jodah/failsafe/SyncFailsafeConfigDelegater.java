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

/**
 * This class delegates all configuration methods to a master in addition to the local object; thus
 * allowing another failsafe config to be kept up to date.
 *
 * @param <R> The result type
 */
public class SyncFailsafeConfigDelegater<R> extends SyncFailsafe<R> {
  private final FailsafeConfig<R, ?> master;

  public SyncFailsafeConfigDelegater(FailsafeConfig<R, ?> master) {
    super(master.circuitBreaker);
    // copy all config from master
    super.retryPolicy = master.retryPolicy;
    super.circuitBreaker = master.circuitBreaker;
    super.fallback = master.fallback;
    super.listeners = master.listeners;
    super.listenerRegistry = master.listenerRegistry;
    this.master = master;
  }

  @Override
  public SyncFailsafe<R> with(CircuitBreaker circuitBreaker) {
    master.with(circuitBreaker);
    return super.with(circuitBreaker);
  }

  @Override
  public <T> SyncFailsafe<R> with(Listeners<T> listeners) {
    master.with(listeners);
    return super.with(listeners);
  }

  @Override
  public SyncFailsafe<R> with(RetryPolicy retryPolicy) {
    master.with(retryPolicy);
    return super.with(retryPolicy);
  }

  // overriding this withFallback() will catch all other withFallback() methods
  @Override
  public SyncFailsafe<R> withFallback(
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
}
