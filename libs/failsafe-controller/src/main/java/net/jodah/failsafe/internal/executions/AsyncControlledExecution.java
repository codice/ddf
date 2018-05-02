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
package net.jodah.failsafe.internal.executions;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.jodah.failsafe.AsyncFailsafe;
import net.jodah.failsafe.AsyncFailsafeConfigDelegater;
import net.jodah.failsafe.ControlledExecutionException;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.FailsafeFuture;
import net.jodah.failsafe.function.AsyncCallable;
import net.jodah.failsafe.function.AsyncRunnable;
import net.jodah.failsafe.function.CheckedRunnable;
import net.jodah.failsafe.function.ContextualCallable;
import net.jodah.failsafe.function.ContextualRunnable;
import net.jodah.failsafe.internal.ControlledScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines an {@link AsyncFailsafe} which is used to control and track a specific
 * failsafe execution.
 *
 * @param <R> the result type
 */
public class AsyncControlledExecution<R> extends AsyncFailsafeConfigDelegater<R>
    implements ControlledExecution<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncControlledExecution.class);

  private final FailsafeController<R> controller;

  /**
   * Reference to the master {@link AsyncFailsafe} where all requests for more execution should be
   * delegated.
   */
  private final AsyncFailsafe<R> master;

  /**
   * Keeps track of all threads that are currently allocated to help failsafe perform a specific
   * execution. These threads are either asynchronously executing the retry attempt or
   * asynchronously notifying registered listeners.
   */
  private final ControlledScheduler controlledScheduler;

  /** Unique identifier for this execution. */
  private final int id;

  /** Keeps track of when a given execution completed successfully or in error. */
  private boolean completed = false;

  /**
   * Keeps track of the result for the execution. This will be <code>null</code> until the execution
   * completes at which points it might remain <code>null</code> if the result returned is <code>
   * null</code> or if the execution failed.
   */
  @Nullable private R result = null;

  /**
   * Keeps track of the error for the execution. This will be <code>null</code> until the execution
   * completes at which point it will remain <code>null</code> if no error occurred.
   */
  @Nullable private Throwable error = null;

  public AsyncControlledExecution(
      FailsafeController<R> controller, AsyncFailsafe<R> master, int id) {
    this(
        controller,
        master,
        id,
        new ControlledScheduler(
            controller, id, AsyncFailsafeConfigDelegater.getOriginalScheduler(master)));
  }

  private AsyncControlledExecution(
      FailsafeController<R> controller,
      AsyncFailsafe<R> master,
      int id,
      ControlledScheduler scheduler) {
    super(master, scheduler);
    this.controller = controller;
    this.master = master;
    this.controlledScheduler = scheduler;
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public boolean hasCompleted() {
    synchronized (controller) {
      return completed && !controlledScheduler.isActive();
    }
  }

  @Override
  public boolean hasSuccessfullyCompleted() {
    synchronized (controller) {
      return hasCompleted() && (error == null);
    }
  }

  @Nullable
  @Override
  public R getResult() {
    synchronized (controller) {
      return result;
    }
  }

  @Nullable
  @Override
  public R getResultOrThrowError() throws ControlledExecutionException {
    synchronized (controller) {
      if (completed && (error != null)) {
        throw new ControlledExecutionException(error);
      }
      return result;
    }
  }

  @Nullable
  @Override
  public Throwable getError() {
    synchronized (controller) {
      return error;
    }
  }

  @Override
  public boolean isAssociatedWith(Thread thread) {
    return controlledScheduler.isActive(thread);
  }

  @Override
  public void onCompletion(R result, Throwable error) {
    setCompleted(result, error, "listener");
  }

  /**
   * Control the execution of the specified code asynchronously.
   *
   * @param callable the code to control its execution
   * @return a completable future for the code being executed asynchronously
   */
  public CompletableFuture<R> execute(ContextualCallable<CompletableFuture<R>> callable) {
    LOGGER.debug("FailsafeController({} - {}): starting execution", controller, id);
    final CompletableFuture<R> future = super.future(callable);

    // setup a completed stage such that we be notified when failsafe has completed the execution
    // but do not return the resulting future as we do not want that stage to not be called if the
    // production code decides to cancel the future it will be receiving; instead return the future
    // returned by failsafe for executing/controlling the registered retry production code
    future.whenComplete(this::completed);
    return future;
  }

  @Override
  public void shutdown() {
    controlledScheduler.shutdown();
  }

  @Override
  public <T> CompletableFuture<T> future(Callable<CompletableFuture<T>> callable) {
    return master.future(callable);
  }

  @Override
  public <T> CompletableFuture<T> future(ContextualCallable<CompletableFuture<T>> callable) {
    return master.future(callable);
  }

  @Override
  public <T> CompletableFuture<T> futureAsync(AsyncCallable<CompletableFuture<T>> callable) {
    return master.futureAsync(callable);
  }

  @Override
  public <T> FailsafeFuture<T> get(Callable<T> callable) {
    return master.get(callable);
  }

  @Override
  public <T> FailsafeFuture<T> get(ContextualCallable<T> callable) {
    return master.get(callable);
  }

  @Override
  public <T> FailsafeFuture<T> getAsync(AsyncCallable<T> callable) {
    return master.getAsync(callable);
  }

  @Override
  public FailsafeFuture<Void> run(CheckedRunnable runnable) {
    return master.run(runnable);
  }

  @Override
  public FailsafeFuture<Void> run(ContextualRunnable runnable) {
    return master.run(runnable);
  }

  @Override
  public FailsafeFuture<Void> runAsync(AsyncRunnable runnable) {
    return master.runAsync(runnable);
  }

  private <T> void completed(T result, Throwable error) {
    setCompleted((R) result, error, "future");
  }

  private void setCompleted(R result, Throwable error, String from) {
    synchronized (controller) {
      if (!completed) {
        if (LOGGER.isDebugEnabled()) {
          if (error == null) {
            LOGGER.debug(
                "FailsafeController({} - {}): execution completed successfully from {} with: {}",
                controller,
                id,
                from,
                result);

          } else if (error instanceof CancellationException) {
            LOGGER.debug(
                "FailsafeController({} - {}): execution was cancelled from {} with: {}",
                controller,
                id,
                from,
                error,
                error);
          } else {
            LOGGER.debug(
                "FailsafeController({} - {}): execution completed in failure from {} with: {}",
                controller,
                id,
                from,
                error,
                error);
          }
        }
        this.completed = true;
        this.result = result;
        this.error = error;
        if (error instanceof AssertionError) { // let the controller know we saw a test failure
          controller.setFailure((AssertionError) error);
        }
        controller.notifyAll(); // wake up anybody that might be waiting on our completion
      } // else - already marked completed
    }
  }
}
