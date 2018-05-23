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

import java.util.concurrent.ExecutorService;
import net.jodah.failsafe.event.ContextualResultListener;
import net.jodah.failsafe.function.CheckedBiConsumer;
import net.jodah.failsafe.function.CheckedBiFunction;
import net.jodah.failsafe.function.CheckedConsumer;
import net.jodah.failsafe.internal.monitor.MonitoredContextualResultListener;

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
    this.master = master;
    // copy all config from master
    super.retryPolicy = master.retryPolicy;
    super.circuitBreaker = master.circuitBreaker;
    super.fallback = master.fallback;
    super.listeners = master.listeners;
    // make sure we use the master registry while ensuring it was initialized
    super.listenerRegistry = master.registry();
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

  // override all async listeners in order to make them monitorable
  @Override
  public SyncFailsafe<R> onAbortAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    return super.onAbort(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onAbortAsync(
      CheckedConsumer<? extends Throwable> listener, ExecutorService executor) {
    return super.onAbort(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onAbortAsync(
      ContextualResultListener<? extends R, ? extends Throwable> listener,
      ExecutorService executor) {
    return super.onAbort(new MonitoredContextualResultListener<>(listener, executor));
  }

  @Override
  public SyncFailsafe<R> onCompleteAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    return super.onComplete(
        new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onCompleteAsync(
      ContextualResultListener<? extends R, ? extends Throwable> listener,
      ExecutorService executor) {
    return super.onComplete(new MonitoredContextualResultListener<>(listener, executor));
  }

  @Override
  public SyncFailsafe<R> onFailedAttemptAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    return super.onFailedAttempt(
        new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onFailedAttemptAsync(
      CheckedConsumer<? extends Throwable> listener, ExecutorService executor) {
    return super.onFailedAttempt(
        new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onFailedAttemptAsync(
      ContextualResultListener<? extends R, ? extends Throwable> listener,
      ExecutorService executor) {
    return super.onFailedAttempt(new MonitoredContextualResultListener<>(listener, executor));
  }

  @Override
  public SyncFailsafe<R> onFailureAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    return super.onFailure(
        new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onFailureAsync(
      CheckedConsumer<? extends Throwable> listener, ExecutorService executor) {
    return super.onFailure(
        new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onFailureAsync(
      ContextualResultListener<? extends R, ? extends Throwable> listener,
      ExecutorService executor) {
    return super.onFailure(new MonitoredContextualResultListener<>(listener, executor));
  }

  @Override
  public SyncFailsafe<R> onRetriesExceededAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    registry()
        .retriesExceeded()
        .add(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
    return this;
  }

  @Override
  public SyncFailsafe<R> onRetriesExceededAsync(
      CheckedConsumer<? extends Throwable> listener, ExecutorService executor) {
    registry()
        .retriesExceeded()
        .add(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
    return this;
  }

  @Override
  public SyncFailsafe<R> onRetryAsync(
      CheckedBiConsumer<? extends R, ? extends Throwable> listener, ExecutorService executor) {
    return super.onRetry(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onRetryAsync(
      CheckedConsumer<? extends Throwable> listener, ExecutorService executor) {
    return super.onRetry(new MonitoredContextualResultListener<>(Listeners.of(listener), executor));
  }

  @Override
  public SyncFailsafe<R> onRetryAsync(
      ContextualResultListener<? extends R, ? extends Throwable> listener,
      ExecutorService executor) {
    return super.onRetry(new MonitoredContextualResultListener<>(listener, executor));
  }

  @Override
  public SyncFailsafe<R> onSuccessAsync(
      CheckedBiConsumer<? extends R, ExecutionContext> listener, ExecutorService executor) {
    registry()
        .success()
        .add(new MonitoredContextualResultListener<>(Listeners.ofResult(listener), executor));
    return this;
  }

  @Override
  public SyncFailsafe<R> onSuccessAsync(
      CheckedConsumer<? extends R> listener, ExecutorService executor) {
    registry()
        .success()
        .add(new MonitoredContextualResultListener<>(Listeners.ofResult(listener), executor));
    return this;
  }
}
