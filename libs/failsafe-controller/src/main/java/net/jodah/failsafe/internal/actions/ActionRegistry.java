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
package net.jodah.failsafe.internal.actions;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.internal.FailsafeContinueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The action registry class is used to track failsafe actions registered with a controller.
 *
 * @param <R> the result type
 */
public class ActionRegistry<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActionRegistry.class);

  private final Stubber stubber = new Stubber();

  private final Stubber0 stubber0 = new Stubber0();

  private final Stubber1 stubber1 = new Stubber1();

  private final Stubber2 stubber2 = new Stubber2();

  private final FailsafeController<R> controller;

  private final Deque<Action<R>> actions = new LinkedList<>();

  private final Deque<Action<R>> processed = new LinkedList();

  private AssertionError shutdownFailure = null;

  public ActionRegistry(FailsafeController<R> controller) {
    this.controller = controller;
  }

  /**
   * Starts registering actions with this registry.
   *
   * @return a stub construct for chaining
   */
  public Stubber whenAttempting() {
    return stubber;
  }

  /**
   * Adds the specified action to this registry.
   *
   * @param action the action to be added
   */
  public void add(Action<R> action) {
    synchronized (controller) {
      failIfShutdown();
      LOGGER.debug("FailsafeController({}): recording action: {}", controller, action);
      actions.addLast(action);
    }
  }

  /**
   * Decorates the previous action with the specified one.
   *
   * @param decorator a function that can be used to decorate the previous action which should
   *     return a replacement action
   * @throws IllegalArgumentException if the decorator fails to create a replacement action
   * @throws java.util.NoSuchElementException if there are no previous actions to decorate
   */
  public void decorate(Function<Action<R>, Action<R>> decorator) {
    synchronized (controller) {
      failIfShutdown();
      final Action<R> previous = actions.removeLast();

      try {
        final Action<R> action = decorator.apply(previous);

        LOGGER.debug(
            "FailsafeController({}): decorating previously recorded action [{}] as: {}",
            controller,
            previous,
            action);
        actions.add(action);
      } catch (IllegalArgumentException e) {
        actions.add(previous);
        throw e;
      }
    }
  }

  /**
   * Verifies if all recorded actions that cannot be left incomplete have been completed.
   *
   * @throws AssertionError if there are recorded actions left to execute
   */
  public void verifyNoMoreActions() {
    synchronized (controller) {
      final List<Action<R>> left =
          actions.stream().filter(a -> !a.canBeLeftIncomplete()).collect(Collectors.toList());

      if (!left.isEmpty()) {
        throw controller.setFailure(
            new AssertionError(
                "too many recorded actions for controller '"
                    + controller.getId()
                    + "'; the following action(s) were not attempted: \r\n\t"
                    + left.stream()
                        .map(Action::currentToString)
                        .collect(Collectors.joining("\r\n\t"))
                    + "\r\n"));
      }
    }
  }

  @SuppressWarnings("squid:S1181" /* purposely catching VirtualMachineError first */)
  public R attempt(ExecutionContext context, Callable<R> callable) throws Exception {
    LOGGER.debug("FailsafeController({}): failsafe is attempting", controller);
    while (true) {
      final ActionContext<R> actionContext = new ActionContext<>(controller, context, callable);
      final Action<R> action = peek();

      try {
        final R r = action.execute(actionContext);

        LOGGER.debug("FailsafeController({}): action {} returned: {}", controller, action, r);
        return r;
      } catch (VirtualMachineError e) {
        throw e;
      } catch (FailsafeContinueException e) { // do nothing and loop back to continue with the next
        LOGGER.debug(
            "FailsafeController({}): action {} indicated to continue with next action",
            controller,
            action);
      } catch (Exception | Error e) {
        LOGGER.debug("FailsafeController({}): action {} threw: {}", controller, action, e, e);
        throw e;
      } finally {
        pop();
      }
    }
  }

  /**
   * Shuts down this registry.
   *
   * @param failure the associated failure to throw back if the registry is used afterward
   */
  public void shutdown(AssertionError failure) {
    synchronized (controller) {
      this.shutdownFailure = failure;
    }
  }

  /**
   * Peeks at the next action to be executed.
   *
   * @return the next action to be executed
   * @throws AssertionError if there are no more actions in the registry (the controller's will be
   *     updated with a corresponding failure)
   */
  private Action<R> peek() {
    synchronized (controller) {
      failIfShutdown();
      final Action<R> action = actions.peek();

      if (action == null) {
        LOGGER.debug("FailsafeController({}): not enough recorded actions", controller);
        throw controller.setFailure(
            new AssertionError(
                "not enough recorded actions for controller '"
                    + controller.getId()
                    + "'; the following "
                    + processed.size()
                    + " action(s) were processed: \r\n\t"
                    + processed
                        .stream()
                        .map(Action::definedToString)
                        .collect(Collectors.joining("\r\n\t"))
                    + "\r\n"));
      }
      LOGGER.debug("FailsafeController({}): next action to execute: {}", controller, action);
      return action;
    }
  }

  /**
   * Pops the next action to be executed if it has completed.
   *
   * @return the next action if it has completed or <code>null</code> if it hasn't or if no actions
   *     are left
   */
  @Nullable
  private Action<R> pop() {
    synchronized (controller) {
      final Action<R> action = actions.peek();

      if ((action != null) && action.hasCompleted()) {
        LOGGER.debug("FailsafeController({}): completed action: {}", controller, action);
        actions.pop();
        processed.add(action);
      }
      return action;
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

  /** Construct used to register a sequence of actions. */
  public class Stubber {
    private Stubber() { // only allow creation from within this class
    }

    /**
     * Completes a sequence of actions for the failsafe controller.
     *
     * @return the failsafe controller for chaining
     */
    public FailsafeController<R> done() {
      failIfShutdown();
      return controller;
    }

    /**
     * Registers the specified results to be returned in sequence the next time failsafe attempts a
     * retry. Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @param results the results to be returned in sequence or <code>null</code> if a single <code>
     *     null</code> element should be returned
     * @return a stub construct for chaining
     */
    public Stubber2 doReturn(@Nullable R... results) {
      add(new DoReturnAction<>(controller, results));
      return stubber2;
    }

    /**
     * Indicates to do nothing (return <code>null</code>) the next time failsafe attempts a retry.
     * Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doNothing() {
      add(new DoNothingAction<>(controller));
      return stubber2;
    }

    /**
     * Registers the specified exceptions to be thrown in sequence the next time failsafe attempts a
     * retry. Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * <p><i>Note:</i> The stack trace of each exception will be re-filled just before being thrown
     * out.
     *
     * @param throwables the exceptions objects to be thrown in sequence
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>throwables</code> or any of its elements is <code>
     *     null</code>
     */
    public Stubber2 doThrow(Throwable... throwables) {
      add(new DoThrowAction<>(controller, throwables));
      return stubber2;
    }

    /**
     * Registers the specified exceptions to be thrown in sequence the next time failsafe attempts a
     * retry. Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * <p><i>Note:</i> Actual exception objects will be instantiated for the provided classes using
     * a constructor that takes a message string, one that takes a message string and a cause, one
     * that takes a cause, or the default constructor.
     *
     * @param throwables the exceptions classes to be instantiated and thrown in sequence
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>throwables</code> or any of its elements is <code>
     *     null</code> if unable to instantiate any exception classes
     */
    public Stubber2 doThrow(Class<? extends Throwable>... throwables) {
      add(new DoThrowAction<>(controller, throwables));
      return stubber2;
    }

    /**
     * Registers the specified results or exceptions to be returned or thrown in sequence the next
     * time failsafe attempts a retry. Doing so short circuits any actions registered with failsafe
     * by the code under test.
     *
     * @param arguments the results to be returned or the exceptions to be thrown or the exception
     *     classes to be instantiated and thrown in sequence or <code>null</code> if a single <code>
     *     null</code> element should be returned
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if unable to instantiate any exception classes
     */
    public Stubber2 doThrowOrReturn(@Nullable Object... arguments) {
      add(new DoThrowOrReturnAction<>(controller, arguments));
      return stubber2;
    }

    /**
     * Indicates to simulate a thread interruption the next time failsafe attempts a retry. Doing so
     * short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doInterrupt() {
      add(new DoInterruptAction<>(controller));
      return stubber2;
    }

    /**
     * Indicates to proceed with calling the production action registered with failsafe normally the
     * next time failsafe attempts a retry.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doProceed() {
      add(new DoProceedAction<>(controller));
      return stubber2;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until the specified latch is
     * notified. After the specified latch has been notified, the controller will move on to the
     * next action and execute it right away.
     *
     * <p>If the specified latch has already been notified then the controller will move on to the
     * next action.
     *
     * @param latch the latch to wait for
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>latch</code> is <code>null</code>
     */
    public Stubber0 doWaitFor(String latch) {
      add(new DoWaitForAction<>(controller, latch));
      return stubber0;
    }

    /**
     * Indicates to notify the specified latch and wake up anybody waiting on it. After executing
     * this action, the controller will move on to the next one and execute it right away.
     *
     * <p>If the specified latch has already been notified then the controller will move on to the
     * next action.
     *
     * @param latch the latch to notify
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>latch</code> is <code>null</code>
     */
    public Stubber0 doNotify(String latch) {
      add(new DoNotifyAction<>(controller, latch));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until it's execution is
     * cancelled. After the execution is cancelled, the controller will simulate an interruption by
     * throwing an {@link InterruptedException} back.
     *
     * <p>If the execution has already been cancelled then the controller will throw the interrupted
     * exception right away.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 doWaitToBeCancelled() {
      add(new DoWaitToBeCancelledAction<>(controller));
      return stubber0;
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber and() {
      failIfShutdown();
      return this;
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber then() {
      failIfShutdown();
      return this;
    }
  }

  /** Construct used to configure the previously registered action. */
  public class Stubber0 extends Stubber {
    private Stubber0() { // only allow creation from within this class
    }

    /**
     * Indicates to conditionally execute the previously registered action.
     *
     * @param condition <code>true</code> to execute the action; <code>false</code> not to execute
     *     it
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(boolean condition) {
      decorate(a -> new OnlyIfAction(a, condition, Boolean.toString(condition)));
      return stubber;
    }

    /**
     * Indicates to conditionally execute the previously registered action.
     *
     * @param condition <code>true</code> to execute the action; <code>false</code> not to execute
     *     it
     * @param info string representation of the condition
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(boolean condition, String info) {
      decorate(a -> new OnlyIfAction(a, condition, info));
      return stubber;
    }

    /**
     * Indicates to conditionally execute the previously registered action. The predicate will be
     * evaluated only the first time the action is executed.
     *
     * @param predicate a predicate that returns <code>true</code> to execute the action; <code>
     *     false</code> not to execute it
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(BooleanSupplier predicate) {
      final boolean condition = predicate.getAsBoolean();

      decorate(a -> new OnlyIfAction(a, predicate, "predicate<?>"));
      return stubber;
    }

    /**
     * Indicates to conditionally execute the previously registered action. The predicate will be *
     * evaluated only the first time the action is executed.
     *
     * @param predicate a predicate that returns <code>true</code> to execute the action; <code>
     *     false</code> not to execute it
     * @param info string representation of the predicate
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(BooleanSupplier predicate, String info) {
      decorate(a -> new OnlyIfAction(a, predicate, info));
      return stubber;
    }
  }

  /** Construct used to configure the previously registered action. */
  public class Stubber1 extends Stubber0 {
    private Stubber1() { // only allow creation from within this class
    }

    /**
     * Indicates to repeat the previously registered action until the specified latch has been
     * notified at which point the controller will move on to the next action and execute it right
     * away.
     *
     * <p>The previous action will be executed at least once even if the latch has already been
     * notified.
     *
     * @param latch the latch to check before repeating the previous action
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>latch</code> is <code>null</code>
     */
    public Stubber0 untilNotifiedFor(String latch) {
      decorate(action -> new UntilNotifiedForAction<>(action, latch));
      return stubber0;
    }

    /**
     * Indicates to repeat the previously registered action until the failsafe execution is
     * cancelled at which point the controller will simulate an interruption by throwing back an
     * {@link InterruptedException}.
     *
     * <p>The previous action will not be executed if the execution was already cancelled.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 untilCancelled() {
      decorate(action -> new UntilCancelledAction<>(action));
      return stubber0;
    }

    /**
     * Indicates to repeat the previously registered action forever (i.e. until the controller is
     * shutdown).
     *
     * @return the failsafe controller for chaining
     */
    public FailsafeController<R> forever() {
      decorate(ForeverAction::new);
      return controller;
    }

    /**
     * Indicates to never execute the previously registered action.
     *
     * @return a stub construct for chaining
     */
    public Stubber never() {
      decorate(NeverAction::new);
      return stubber;
    }
  }

  /** Construct used to configure the previously registered action. */
  public class Stubber2 extends Stubber1 {
    private Stubber2() { // only allow creation from within this class
    }

    /**
     * Indicates to repeat the previously registered action for <code>count</code> times.
     *
     * @param count the number of times to repeat the action
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>count</code> is negative
     */
    public Stubber1 times(int count) {
      decorate(action -> new TimesAction<>(action, count));
      return stubber1;
    }

    /**
     * Indicates to delay the previously registered action for the specified amount of time.
     *
     * @param delay the number of seconds to delay
     * @throws IllegalArgumentException if <code>delay</code> is negative
     */
    public Stubber0 delayedFor(int delay) {
      decorate(action -> new DelayedForAction<>(action, delay));
      return stubber0;
    }
  }
}
