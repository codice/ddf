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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;
import net.jodah.failsafe.AsyncFailsafe;
import net.jodah.failsafe.ControlledExecutionException;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.SyncFailsafe;
import net.jodah.failsafe.internal.actions.ActionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The controlled execution registry class is used to track failsafe executions.
 *
 * @param <R> the result type
 */
public class ControlledExecutionRegistry<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ControlledExecutionRegistry.class);

  private final FailsafeController<R> controller;

  private final Deque<ControlledExecution<R>> executions = new LinkedList<>();

  private AssertionError shutdownFailure = null;

  private AssertionError failure = null;

  public ControlledExecutionRegistry(FailsafeController<R> controller) {
    this.controller = controller;
  }

  /**
   * Gets the number of executions done by failsafe.
   *
   * @return the number of executions done by failsafe
   */
  public int size() {
    synchronized (controller) {
      return executions.size();
    }
  }

  /**
   * Gets a stream of all executions done or in progress.
   *
   * @return a stream of all executions
   */
  public Stream<ControlledExecution<R>> executions() {
    return executions.stream();
  }

  /**
   * Finds the execution associated with the current thread.
   *
   * @return the execution associated with the current thread or empty if none
   */
  public Optional<ControlledExecution<R>> currentExecution() {
    final Thread thread = Thread.currentThread();

    synchronized (controller) {
      return executions.stream().filter(exec -> exec.isAssociatedWith(thread)).findAny();
    }
  }

  /**
   * Creates a new synchronous controlled execution based on the given failsafe sync with the
   * specified expectation.
   *
   * @param master the master failsafe sync
   * @param expectation the expectation to use for this execution
   * @return a newly created asynchronous controlled execution
   */
  public SyncControlledExecution<R> newExecution(
      SyncFailsafe<R> master, ActionRegistry<R>.Expectation expectation) {
    synchronized (controller) {
      checkIfFailed();
      failIfShutdown();
      final SyncControlledExecution<R> exec =
          new SyncControlledExecution<>(expectation.getController(), master, expectation.getId());

      executions.add(exec);
      controller.notifyAll();
      return exec;
    }
  }

  /**
   * Creates a new asynchronous controlled execution based on the given failsafe async with the
   * specified expectation.
   *
   * @param master the master failsafe async
   * @param expectation the expectation to use for this execution
   * @return a newly created asynchronous controlled execution
   */
  public AsyncControlledExecution<R> newExecution(
      AsyncFailsafe<R> master, ActionRegistry<R>.Expectation expectation) {
    synchronized (controller) {
      checkIfFailed();
      failIfShutdown();
      final AsyncControlledExecution<R> exec =
          new AsyncControlledExecution<>(expectation.getController(), master, expectation.getId());

      executions.add(exec);
      controller.notifyAll();
      return exec;
    }
  }

  /**
   * Waits for failsafe to complete the current (or next) successful execution. This method will
   * return right away if failsafe has already completed successfully its last execution. If the
   * current execution completes with a failure, then this method will wait for the next execution
   * to start and complete successfully.
   *
   * @return the result from the current (or next) successful completion
   * @throws InterruptedException if the controller was shutdown or if interrupted while waiting for
   *     a successful completion
   */
  public R waitForSuccessfulCompletion() throws InterruptedException {
    synchronized (controller) {
      while (true) {
        checkIfFailed();
        interruptIfShutdown();
        final ControlledExecution<R> exec = executions.peekLast();

        if ((exec != null) && exec.hasSuccessfullyCompleted()) {
          return exec.getResult();
        }
        LOGGER.debug("FailsafeController({}): waiting for successful completion", controller);
        controller.wait();
      }
    }
  }

  /**
   * Waits for the last failsafe execution to complete (successfully or not). This method will
   * return right away if failsafe has already completed the last execution.
   *
   * @return the result from the last successful completion
   * @throws ControlledExecutionException if the last completion failed
   * @throws InterruptedException if the controller was shutdown or if interrupted while waiting for
   *     completion
   */
  public R waitForCompletion() throws InterruptedException, ControlledExecutionException {
    synchronized (controller) {
      while (true) {
        checkIfFailed();
        interruptIfShutdown();
        final ControlledExecution<R> exec = executions.peekLast();

        if ((exec != null) && exec.hasCompleted()) {
          return exec.getResultOrThrowError();
        }
        LOGGER.debug("FailsafeController({}): waiting for completion", controller);
        controller.wait();
      }
    }
  }

  /**
   * Checks if the last execution (if any) was cancelled via its future.
   *
   * @return <code>true</code> if the last execution was cancelled; <code>false</code> otherwise
   */
  public boolean wasLastExecutionCancelled() {
    synchronized (controller) {
      // give priority to check if we were cancelled and then check if we failed or were shutdown
      final ControlledExecution<R> exec = executions.peekLast();

      if ((exec != null) && (exec.getError() instanceof CancellationException)) {
        return true;
      }
      checkIfFailed();
      failIfShutdown();
      return false;
    }
  }

  /**
   * Shuts down this registry and all controlled exeuctions.
   *
   * @param failure the associated failure to throw back if the registry is used afterward
   */
  public void shutdown(AssertionError failure) {
    synchronized (controller) {
      if (shutdownFailure == null) {
        this.shutdownFailure = failure;
        if (!executions.isEmpty()) {
          executions.forEach(ControlledExecution::shutdown);
          controller.notifyAll();
        }
      }
    }
  }

  /**
   * Records the specified test failure.
   *
   * @param failure the test failure to record
   */
  public void setFailure(AssertionError failure) {
    synchronized (controller) {
      this.failure = failure;
    }
  }

  /**
   * Checks if the registry was shutdown and throw back an assertion error if it has.
   *
   * @throws AssertionError if the registry was shutdown
   */
  private void failIfShutdown() {
    synchronized (controller) {
      if (shutdownFailure != null) {
        throw shutdownFailure;
      }
    }
  }

  /**
   * Checks if the registry was shutdown and throw back an interrupted exception if it has.
   *
   * @throws InterruptedException if the registry was shutdown
   */
  private void interruptIfShutdown() throws InterruptedException {
    synchronized (controller) {
      if (shutdownFailure != null) {
        throw new InterruptedException(shutdownFailure.getMessage());
      }
    }
  }

  /**
   * Checks if a failure was recorded and throw it back.
   *
   * @throws AssertionError if a test failure has been recorded
   */
  public void checkIfFailed() {
    synchronized (controller) {
      if (failure != null) {
        throw failure;
      }
    }
  }
}
