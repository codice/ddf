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

import groovy.lang.Closure;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import net.jodah.failsafe.function.AsyncCallable;
import net.jodah.failsafe.function.AsyncRunnable;
import net.jodah.failsafe.function.CheckedRunnable;
import net.jodah.failsafe.function.ContextualCallable;
import net.jodah.failsafe.function.ContextualRunnable;
import net.jodah.failsafe.internal.TimelessRetryPolicy;
import net.jodah.failsafe.internal.actions.ActionRegistry;
import net.jodah.failsafe.internal.executions.AsyncControlledExecution;
import net.jodah.failsafe.internal.executions.ControlledExecutionRegistry;
import net.jodah.failsafe.internal.executions.SyncControlledExecution;
import net.jodah.failsafe.internal.monitor.ThreadMonitor;
import net.jodah.failsafe.internal.util.Assert;
import net.jodah.failsafe.util.concurrent.Scheduler;
import net.jodah.failsafe.util.concurrent.Schedulers;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The failsafe controller is the point of entry for creating a test framework for failsafe.
 *
 * <p>A controller supports the creation of a single Failsafe object via one of the {@link
 * #with(CircuitBreaker)} or {@link #with(RetryPolicy)} methods.
 *
 * @param <R> the result type
 */
public class FailsafeController<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FailsafeController.class);

  private static final String FAILSAFE_CONTROLLER_WAS_SHUTDOWN =
      "failsafe controller was shutdown: ";

  private static final String INVALID_NULL_CONDITION = "invalid null condition";

  private static final String NOT_SUPPORTED_YET = "not supported yet";

  private static final String RUNNABLE = "runnable";

  private static final String CALLABLE = "callable";

  private final String id;

  private final ActionRegistry<R> actions;

  private final ControlledExecutionRegistry<R> executions;

  private SyncBuilder sync = null;

  private final Set<String> conditions = new HashSet<>();

  private final ThreadMonitor monitor;

  private AssertionError shutdownFailure = null;

  private AssertionError failure = null;

  /**
   * Creates a new controller for failsafe.
   *
   * @param id an identifier for this controller
   */
  public FailsafeController(String id) {
    this.id = id;
    this.actions = new ActionRegistry<>(this);
    this.executions = new ControlledExecutionRegistry<>(this);
    this.monitor = new ThreadMonitor(this);
  }

  /**
   * Retrieves the controller's identifier.
   *
   * @return the identifier for this controller
   */
  public String getId() {
    return id;
  }

  /**
   * Creates and returns a new SyncFailsafe instance that will perform executions and retries
   * synchronously according to the {@code retryPolicy}.
   *
   * <p>Calling this method is the same as calling <code>control(Failsafe.with(retryPolicy))</code>.
   *
   * @param retryPolicy the retry policy to use
   * @throws NullPointerException if {@code retryPolicy} is null
   * @throws IllegalStateException if any of the <code>retry()</code> methods have been called
   *     already
   */
  public synchronized SyncFailsafe<R> with(RetryPolicy retryPolicy) {
    Assert.state(sync == null, "with() was already called once");
    this.sync = new SyncBuilder(new FailsafeConfig<R, FailsafeConfig<R, ?>>());
    sync.with(retryPolicy);
    return sync;
  }

  /**
   * Creates and returns a new SyncFailsafe instance that will perform executions and retries
   * synchronously according to the {@code circuitBreaker}.
   *
   * <p>Calling this method is the same as calling <code>control(Failsafe.with(circuitBreaker))
   * </code>.
   *
   * @param circuitBreaker the circuit breaker to use
   * @throws NullPointerException if {@code circuitBreaker} is null
   * @throws IllegalStateException if any of the <code>retry()</code> methods have been called
   *     already
   */
  @SuppressWarnings(
      "squid:CommentedOutCodeLine" /* currently not supported, will be uncommented once we support it */)
  public synchronized SyncFailsafe<R> with(CircuitBreaker circuitBreaker) {
    Assert.state(sync == null, "with() was already called once");
    throw new UnsupportedOperationException(FailsafeController.NOT_SUPPORTED_YET);
    //    this.sync = new SyncBuilder(new FailsafeConfig<R, FailsafeConfig<R, ?>>());
    //    sync.with(circuitBreaker);
    //    return sync;
  }

  /**
   * Register expected actions for the next Failsafe execution.
   *
   * @param actionList the list of actions to be executed for the next Failsafe's execution
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> onNextExecution(Actions.Done<R> actionList) {
    actions.add(actionList.done());
    return this;
  }

  /**
   * Register expected actions for the next Failsafe execution.
   *
   * <p>Syntax sugar for Spock users which allows you to write this:
   *
   * <p><code>new FailsafeController('test') >> doReturn(true)</code>
   *
   * @param actionList the list of actions to be executed for the next Failsafe's execution
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> rightShift(Actions.Done<R> actionList) {
    return onNextExecution(actionList);
  }

  /**
   * Register expected actions for the next Failsafe execution.
   *
   * <p>Syntax sugar for Spock users which allows you to write this:
   *
   * <p><code><pre>
   *   new FailsafeController('test').onNextExecution {
   *     doThrow(NullPointerException).then().doReturn(true)
   *   }
   * </pre></code>
   *
   * @param closure a closure for the list of actions to be executed for the next Failsafe's
   *     execution
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> onNextExecution(Closure<Actions.Done<R>> closure) {
    return onNextExecution(closure.call());
  }

  /**
   * Register expected actions for the next Failsafe execution.
   *
   * <p>Syntax sugar for Spock users which allows you to write this:
   *
   * <p><code><pre>
   *   new FailsafeController('test') >> {
   *     doWaitFor('creating').before().returning(true)
   *   }
   * </pre></code>
   *
   * @param closure a closure for the list of actions to be executed for the next Failsafe's
   *     execution
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> rightShift(Closure<Actions.Done<R>> closure) {
    return onNextExecution(closure);
  }

  /**
   * Register expected actions for the next Failsafe executions where each entry correspond to a
   * separate executions.
   *
   * <p>Syntax sugar for Spock users which allows you to write this:
   *
   * <p><code><pre>
   *   new FailsafeController('test') >>> [doReturn(true), doThrow(NullPointerException).then().doReturn(true)]
   * </pre></code>
   *
   * @param actionLists the lists of actions to be executed for the next Failsafe's execution
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> rightShiftUnsigned(List<Actions.Done<R>> actionLists) {
    actionLists.forEach(this::onNextExecution);
    return this;
  }

  /**
   * Syntax sugar.
   *
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> then() {
    return this;
  }

  /**
   * Syntax sugar.
   *
   * @return the failsafe controller for chaining
   */
  public FailsafeController<R> and() {
    return this;
  }

  /**
   * Shuts down testing using this controller.
   *
   * <p>All subsequent failsafe attempts will fail with an interruption and all waits for conditions
   * will be interrupted.
   */
  public synchronized void shutdown() {
    if (shutdownFailure != null) {
      return;
    }
    LOGGER.debug("FailsafeController({}): shutting down", id);
    this.shutdownFailure =
        new AssertionError(FailsafeController.FAILSAFE_CONTROLLER_WAS_SHUTDOWN + id);
    monitor.shutdown();
    actions.shutdown(shutdownFailure);
    executions.shutdown(shutdownFailure);
    notifyAll();
  }

  /**
   * Checks if the controller was shutdown.
   *
   * @return <code>true</code> if the controller was shutdown; <code>false</code> otherwise
   */
  public synchronized boolean isShutdown() {
    return shutdownFailure != null;
  }

  /**
   * Notifies the specified condition.
   *
   * @param condition the condition/latch to be notified
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public synchronized void notify(String condition) {
    checkIfFailed();
    failIfShutdown();
    Validate.notNull(condition, FailsafeController.INVALID_NULL_CONDITION);
    LOGGER.debug("FailsafeController({}): notifying '{}'", this, condition);
    if (conditions.add(condition)) {
      notifyAll();
    }
  }

  /**
   * Notifies the specified condition/latch.
   *
   * @param condition the condition/latch to be notified
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public synchronized void notifyTo(String condition) {
    checkIfFailed();
    failIfShutdown();
    Validate.notNull(condition, FailsafeController.INVALID_NULL_CONDITION);
    LOGGER.debug("FailsafeController({}): notifying to '{}'", this, condition);
    if (conditions.add(condition)) {
      notifyAll();
    }
  }

  /**
   * Waits for the specified condition/latch to be notified.
   *
   * <p>The method returns right away if the specified condition/latch has already been notified.
   *
   * @param condition the condition/latch to wait for
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   * @throws InterruptedException if shutdown or if interrupted while waiting for the specified
   *     condition/latch
   */
  public synchronized void waitFor(String condition) throws InterruptedException {
    checkIfFailed();
    interruptIfShutdown();
    Validate.notNull(condition, FailsafeController.INVALID_NULL_CONDITION);
    LOGGER.debug("FailsafeController({}): waiting for '{}'", this, condition);
    while (!conditions.contains(condition)) {
      wait();
      checkIfFailed();
      interruptIfShutdown();
      LOGGER.debug("FailsafeController({}): '{}' was notified", this, condition);
    }
  }

  /**
   * Waits for the specified condition/latch to be notified.
   *
   * <p>The method returns right away if the specified condition/latch has already been notified.
   *
   * @param condition the condition/latch to wait for
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   * @throws InterruptedException if shutdown or if interrupted while waiting for the specified
   *     condition/latch
   */
  public synchronized void waitTo(String condition) throws InterruptedException {
    checkIfFailed();
    interruptIfShutdown();
    Validate.notNull(condition, FailsafeController.INVALID_NULL_CONDITION);
    LOGGER.debug("FailsafeController({}): waiting to '{}'", this, condition);
    while (!conditions.contains(condition)) {
      wait();
      checkIfFailed();
      interruptIfShutdown();
      LOGGER.debug("FailsafeController({}): '{}' was notified", this, condition);
    }
  }

  /**
   * Checks if a given condition/latch was notified.
   *
   * @param condition the condition/latch to check if it was notified
   * @return <code>true</code> if the condition/latch was notified; <code>false</code> if not
   */
  public synchronized boolean wasNotified(String condition) {
    checkIfFailed();
    failIfShutdown();
    Validate.notNull(condition, FailsafeController.INVALID_NULL_CONDITION);
    return conditions.contains(condition);
  }

  /**
   * Checks if a given condition/latch was notified.
   *
   * @param condition the condition/latch to check if it was notified
   * @return <code>true</code> if the condition/latch was notified; <code>false</code> if not
   */
  public synchronized boolean wasNotifiedTo(String condition) {
    return wasNotified(condition);
  }

  /**
   * Checks if the last execution done by failsafe (if any) was cancelled via its future.
   *
   * @return <code>true</code> if the last failsafe execution was cancelled; <code>false</code>
   *     otherwise
   */
  public boolean wasLastExecutionCancelled() {
    return executions.wasLastExecutionCancelled();
  }

  /**
   * Gets the number of executions done by failsafe.
   *
   * @return the number of executions done by failsafe
   */
  public int getNumExecutions() {
    return executions.size();
  }

  /**
   * Waits for failsafe to complete the current (or next) successful execution. This method will
   * return right away if failsafe has already completed successfully its last execution. If the
   * current execution completes with a failure, then this method will wait for the next execution
   * to start and complete successfully.
   *
   * @return the result from the current (or next) successful completion
   * @throws InterruptedException if shutdown or if interrupted while waiting for a successful
   *     completion
   */
  public R waitForSuccessfulCompletion() throws InterruptedException {
    return executions.waitForSuccessfulCompletion();
  }

  /**
   * Waits for the last failsafe execution to complete (successfully or not). This method will
   * return right away if failsafe has already completed the last execution.
   *
   * @return the result from the last successful completion
   * @throws ControlledExecutionException if the last completion failed
   * @throws InterruptedException if shutdown or if interrupted while waiting for completion
   */
  public R waitForCompletion() throws InterruptedException, ControlledExecutionException {
    return executions.waitForCompletion();
  }

  /**
   * Records the specified test failure for this controller. Once recorded, the controller will stop
   * and start throwing back this failure everywhere.
   *
   * @param failure the test failure to record
   * @return <code>failure</code>
   */
  public synchronized AssertionError setFailure(AssertionError failure) {
    LOGGER.debug("FailsafeController({}): recording failure: ", id, failure, failure);
    if ((this.failure != null) && (this.failure != failure)) {
      this.failure.addSuppressed(failure);
    } else {
      this.failure = failure;
      actions.shutdown(failure);
      executions.setFailure(failure);
    }
    notifyAll();
    return failure;
  }

  /**
   * Waits for all threads and executions to complete and verifies if a failure occurred and if all
   * recorded actions that cannot be left incomplete have been completed. For example actions that
   * are customized with <code>forever()</code>, <code>never()</code>, <code>times(0)</code> are
   * allowed to not complete.
   *
   * @throws AssertionError if a failure occurred while controlling failsafe
   */
  public void verify() {
    LOGGER.debug("FailsafeController({}): verifying no more actions", id);
    shutdown();
    if (failure != null) {
      throw failure;
    }
    actions.verify();
  }

  /**
   * Gets the retry policy associated with the corresponding Failsafe.
   *
   * @return the retry policy associated with the corresponding failsafe
   * @throws IllegalStateException if one of the <code>with()</code> methods has not been called yet
   */
  public RetryPolicy getRetryPolicy() {
    Assert.state(sync != null, "with() has not been called yet");
    return sync.getOriginalRetryPolicy();
  }

  @Override
  public String toString() {
    return id;
  }

  /**
   * Checks if the controller was shutdown and throw back an assertion error if it has.
   *
   * @throws AssertionError if the controller was shutdown
   */
  private synchronized void failIfShutdown() {
    if (shutdownFailure != null) {
      throw shutdownFailure;
    }
  }

  /**
   * Checks if the controller was shutdown and throw back an interrupted exception if it has.
   *
   * @throws InterruptedException if the controller was shutdown
   */
  private synchronized void interruptIfShutdown() throws InterruptedException {
    if (shutdownFailure != null) {
      throw new InterruptedException(shutdownFailure.getMessage());
    }
  }

  /**
   * Checks if a failure was recorded and throw it back.
   *
   * @throws AssertionError if a test failure has been recorded
   */
  private synchronized void checkIfFailed() {
    if (failure != null) {
      throw failure;
    }
  }

  private synchronized void onCompletion(R result, Throwable error) {
    executions.currentExecution().ifPresent(exec -> exec.onCompletion(result, error));
  }

  static <T> ContextualCallable<T> callableOf(final ContextualRunnable runnable) {
    Assert.notNull(runnable, FailsafeController.RUNNABLE);
    return c -> {
      runnable.run(c);
      return null;
    };
  }

  /**
   * This class is used to intercept all failsafe configuration in order to allow us to control
   * them. Any sync or async created later will always delegate configuration to this class as the
   * master which will allow us to keep one copy of the configuration and interceptors.
   */
  class SyncBuilder extends SyncFailsafeConfigDelegater<R> {
    private RetryPolicy originalRetryPolicy = RetryPolicy.NEVER;

    SyncBuilder(FailsafeConfig<R, ?> master) {
      super(master);
      // register a synchronous completion listener to get the results as soon as possible
      // for sync, this will be called from the same thread that is invoking failsafe which is
      // already going to be tracked
      // for async, this will be called from a thread retrieved from the scheduler that is actually
      // executing a given attempt which is also going to be tracked
      onComplete(FailsafeController.this::onCompletion);
    }

    @Override
    public SyncFailsafe<R> with(RetryPolicy retryPolicy) {
      Assert.state(
          originalRetryPolicy == RetryPolicy.NEVER, "A retry policy has already been configured");
      final RetryPolicy newPolicy = new TimelessRetryPolicy(retryPolicy);

      super.retryPolicy = newPolicy;
      this.originalRetryPolicy = retryPolicy;
      return this;
    }

    @Override
    public AsyncFailsafe<R> with(ScheduledExecutorService executor) {
      return new AsyncBuilder(this, Schedulers.of(executor));
    }

    @Override
    public AsyncFailsafe<R> with(Scheduler scheduler) {
      return new AsyncBuilder(this, Assert.notNull(scheduler, "scheduler"));
    }

    @Override
    public <T> T get(Callable<T> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      return get(c -> callable.call());
    }

    @Override
    public <T> T get(ContextualCallable<T> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      final ActionRegistry<R>.Expectation expectation = actions.next();
      final SyncControlledExecution<R> execution = executions.newExecution(this, expectation);

      return (T)
          execution.execute(
              context ->
                  monitor.monitor(
                      () -> expectation.attempt(context, (ContextualCallable<R>) callable)));
    }

    @Override
    public void run(CheckedRunnable runnable) {
      get(Functions.callableOf(runnable));
    }

    @Override
    public void run(ContextualRunnable runnable) {
      get(FailsafeController.callableOf(runnable));
    }

    RetryPolicy getOriginalRetryPolicy() {
      return originalRetryPolicy;
    }
  }

  /**
   * This class is used to intercept all failsafe configuration in order to allow us to control them
   * and delegate them back to the master sync to keep all configurations in sync.
   */
  class AsyncBuilder extends AsyncFailsafeConfigDelegater<R> {
    AsyncBuilder(SyncBuilder sync, Scheduler scheduler) {
      super(sync, scheduler);
    }

    @Override
    public <T> CompletableFuture<T> future(Callable<CompletableFuture<T>> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      return future(c -> callable.call());
    }

    @Override
    public <T> CompletableFuture<T> future(ContextualCallable<CompletableFuture<T>> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      throw new UnsupportedOperationException(FailsafeController.NOT_SUPPORTED_YET);
    }

    @Override
    public <T> CompletableFuture<T> futureAsync(AsyncCallable<CompletableFuture<T>> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      throw new UnsupportedOperationException(FailsafeController.NOT_SUPPORTED_YET);
    }

    @Override
    public <T> FailsafeFuture<T> get(Callable<T> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      return get(c -> callable.call());
    }

    @Override
    public <T> FailsafeFuture<T> get(ContextualCallable<T> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      final ActionRegistry<R>.Expectation expectation = actions.next();
      final AsyncControlledExecution<R> execution = executions.newExecution(this, expectation);
      final CompletableFuture<R> future =
          execution.execute(
              context ->
                  CompletableFuture.completedFuture(
                      monitor.monitor(
                          () -> expectation.attempt(context, (ContextualCallable<R>) callable))));

      return (FailsafeFuture<T>) new FailsafeFutureAdapter<>(this, future);
    }

    @Override
    public <T> FailsafeFuture<T> getAsync(AsyncCallable<T> callable) {
      Assert.notNull(callable, FailsafeController.CALLABLE);
      throw new UnsupportedOperationException(FailsafeController.NOT_SUPPORTED_YET);
    }

    @Override
    public FailsafeFuture<Void> run(CheckedRunnable runnable) {
      Assert.notNull(runnable, FailsafeController.RUNNABLE);
      return get(Functions.callableOf(runnable));
    }

    @Override
    public FailsafeFuture<Void> run(ContextualRunnable runnable) {
      Assert.notNull(runnable, FailsafeController.RUNNABLE);
      return get(FailsafeController.callableOf(runnable));
    }

    @Override
    public FailsafeFuture<Void> runAsync(AsyncRunnable runnable) {
      throw new UnsupportedOperationException(FailsafeController.NOT_SUPPORTED_YET);
    }
  }
}
