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
import javax.annotation.Nullable;
import net.jodah.failsafe.ControlledExecutionException;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.SyncFailsafe;
import net.jodah.failsafe.SyncFailsafeConfigDelegater;
import net.jodah.failsafe.function.CheckedRunnable;
import net.jodah.failsafe.function.ContextualCallable;
import net.jodah.failsafe.function.ContextualRunnable;
import net.jodah.failsafe.internal.monitor.ThreadMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines an {@link SyncFailsafe} which is used to control and track a specific failsafe
 * execution.
 *
 * @param <R> the result type
 */
public class SyncControlledExecution<R> extends SyncFailsafeConfigDelegater<R>
    implements ControlledExecution<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyncControlledExecution.class);

  private static final String EXECUTION = "execution";

  private final FailsafeController<R> controller;

  /**
   * Reference to the master {@link SyncFailsafe} where all requests for more execution should be
   * delegated.
   */
  private final SyncFailsafe<R> master;

  /**
   * Keeps track of all threads that are currently allocated to help failsafe perform a specific
   * execution. These threads are either asynchronously executing the retry attempt or
   * asynchronously notifying registered listeners.
   */
  private final ThreadMonitor monitor;

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

  public SyncControlledExecution(FailsafeController<R> controller, SyncFailsafe<R> master, int id) {
    super(master);
    this.controller = controller;
    this.master = master;
    this.monitor = new ThreadMonitor(controller, id);
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public boolean hasCompleted() {
    synchronized (controller) {
      return completed && !monitor.isMonitoring();
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
    return monitor.isMonitoring(thread);
  }

  @Override
  public void onCompletion(R result, Throwable error) {
    setCompleted(result, error, "listener");
  }

  /**
   * Control the execution of the specified code synchronously.
   *
   * @param callable the code to control its execution
   * @return the result of the executed code
   * @throws FailsafeException if the {@code callable} fails with a Throwable and the retry policy
   *     is exceeded, or if interrupted while waiting to perform a retry
   * @throws net.jodah.failsafe.CircuitBreakerOpenException if a configured circuit is open
   */
  @SuppressWarnings("squid:S1181" /* Forced to because of monitor() */)
  public R execute(ContextualCallable<R> callable) {
    try {
      return monitor.monitor(() -> executeImpl(callable));
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      // should never happen since super.get() does't throw these but monitor requires it
      final InternalError ie = new InternalError(e);

      setCompleted(null, ie, SyncControlledExecution.EXECUTION);
      throw ie;
    }
  }

  @Override
  public void shutdown() {
    monitor.shutdown();
  }

  @Override
  public <T> T get(Callable<T> callable) {
    return master.get(callable);
  }

  @Override
  public <T> T get(ContextualCallable<T> callable) {
    return master.get(callable);
  }

  @Override
  public void run(CheckedRunnable runnable) {
    master.run(runnable);
  }

  @Override
  public void run(ContextualRunnable runnable) {
    master.run(runnable);
  }

  @SuppressWarnings("squid:S1181" /* Bubbling up VirtualMachineError first */)
  private R executeImpl(ContextualCallable<R> callable) {
    LOGGER.debug("FailsafeController({} - {}): starting execution", controller, id);
    try {
      final R r = super.get(callable);

      setCompleted(r, null, SyncControlledExecution.EXECUTION);
      return r;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (FailsafeException e) {
      setCompleted(
          null, (e.getCause() != null) ? e.getCause() : e, SyncControlledExecution.EXECUTION);
      throw e;
    } catch (RuntimeException | Error e) {
      setCompleted(null, e, SyncControlledExecution.EXECUTION);
      throw e;
    }
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
