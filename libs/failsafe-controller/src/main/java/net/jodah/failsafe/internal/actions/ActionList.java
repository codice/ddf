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

import groovy.lang.Closure;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.jodah.failsafe.Actions.Done;

/**
 * The action list class is used to track failsafe actions registered for a given failsafe
 * execution.
 *
 * @param <R> the result type
 */
public class ActionList<R> implements Done<R> {
  private final Stubber stubber = new Stubber();

  private final Stubber0 stubber0 = new Stubber0();

  private final Stubber1 stubber1 = new Stubber1();

  private final Stubber2 stubber2 = new Stubber2();

  /**
   * List of creator functions to invoke at the time we will populate an expectation to create the
   * expected actions.
   */
  private final Deque<Function<ActionRegistry<R>.Expectation, Action<R>>> actions =
      new LinkedList<>();

  public ActionList() { // nothing else to initialize
  }

  @Override
  public ActionList<R> done() {
    return this;
  }

  /**
   * Adds all specified actions to this list.
   *
   * @param actionList the list of actions to be added to this one
   * @return this list for chaining
   */
  public ActionList<R> add(Done<R> actionList) {
    final ActionList<R> alist = actionList.done();

    if (alist != this) { // to avoid circular references
      actions.addAll(alist.actions);
    }
    return this;
  }

  /**
   * Adds all specified actions to this list.
   *
   * <p>Syntax sugar for Spock users which allows you to write this:
   *
   * <p><code><pre>
   *   doThrow(NullPointerException) + doReturn(true)
   * </pre></code>
   *
   * @param actionList the list of actions to be added to this one
   * @return this list for chaining
   */
  public ActionList<R> plus(Done<R> actionList) {
    return add(actionList);
  }

  /**
   * Registers the specified results to be returned in sequence the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * @param results the results to be returned in sequence or <code>null</code> if a single <code>
   *     null</code> element should be returned
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber2 doReturn(@Nullable R... results) {
    return stubber.doReturn(results);
  }

  /**
   * Indicates to do nothing (returns <code>null</code>) the next time failsafe attempts a retry.
   * Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber2 doNothing() {
    return stubber.doNothing();
  }

  /**
   * Indicates to return nothing (returns <code>null</code>) the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber2 doReturn() {
    return stubber.doReturn();
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
  public ActionList<R>.Stubber2 doThrow(Throwable... throwables) {
    return stubber.doThrow(throwables);
  }

  /**
   * Registers the specified exceptions to be thrown in sequence the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * <p><i>Note:</i> Actual exception objects will be instantiated for the provided classes using a
   * constructor that takes a message string, one that takes a message string and a cause, one that
   * takes a cause, or the default constructor.
   *
   * @param throwables the exceptions classes to be instantiated and thrown in sequence
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>throwables</code> or any of its elements is <code>
   *     null</code> if unable to instantiate any exception classes
   */
  public ActionList<R>.Stubber2 doThrow(Class<? extends Throwable>... throwables) {
    return stubber.doThrow(throwables);
  }

  /**
   * Registers the specified results or exceptions to be returned or thrown in sequence the next
   * time failsafe attempts a retry. Doing so short circuits any actions registered with failsafe by
   * the code under test.
   *
   * @param arguments the results to be returned or the exceptions to be thrown or the exception
   *     classes to be instantiated and thrown in sequence or <code>null</code> if a single <code>
   *     null</code> element should be returned
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if unable to instantiate any exception classes
   */
  public ActionList<R>.Stubber2 doThrowOrReturn(@Nullable Object... arguments) {
    return stubber.doThrowOrReturn(arguments);
  }

  /**
   * Indicates to simulate a thread interruption the next time failsafe attempts a retry. Doing so
   * short circuits any actions registered with failsafe by the code under test.
   *
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber2 doInterrupt() {
    return stubber.doInterrupt();
  }

  /**
   * Indicates to proceed with calling the production action registered with failsafe normally the
   * next time failsafe attempts a retry.
   *
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber2 doProceed() {
    return stubber.doProceed();
  }

  /**
   * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
   * executing this action, the controller will move on to the next one and execute it right away.
   *
   * <p>If the specified condition/latch has already been notified then the controller will move on
   * to the next action.
   *
   * @param condition the condition/latch to notify
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public ActionList<R>.Stubber0 doNotify(String condition) {
    return stubber.doNotify(condition);
  }

  /**
   * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
   * executing this action, the controller will move on to the next one and execute it right away.
   *
   * <p>If the specified condition/latch has already been notified then the controller will move on
   * to the next action.
   *
   * @param condition the condition/latch to notify
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public ActionList<R>.Stubber0 doNotifyTo(String condition) {
    return stubber.doNotifyTo(condition);
  }

  /**
   * Indicates to block failsafe the next time it attempts a retry until the specified
   * condition/latch is notified. After the specified condition/latch has been notified, the
   * controller will move on to the next action and execute it right away.
   *
   * <p>If the specified condition/latch has already been notified then the controller will move on
   * to the next action.
   *
   * @param condition the condition/latch to wait for
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public ActionList<R>.Stubber0 waitFor(String condition) {
    return stubber.waitFor(condition);
  }

  /**
   * Indicates to block failsafe the next time it attempts a retry until the specified
   * condition/latch is notified. After the specified condition/latch has been notified, the
   * controller will move on to the next action and execute it right away.
   *
   * <p>If the specified condition/latch has already been notified then the controller will move on
   * to the next action.
   *
   * @param condition the condition/latch to wait for
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
   */
  public ActionList<R>.Stubber0 waitTo(String condition) {
    return stubber.waitTo(condition);
  }

  /**
   * Indicates to block failsafe the next time it attempts a retry until its execution is cancelled.
   * After the execution is cancelled, the controller will simulate an interruption by throwing an
   * {@link InterruptedException} back.
   *
   * <p>If the execution has already been cancelled then the controller will throw the interrupted
   * exception right away.
   *
   * @return a stub construct for chaining
   */
  public ActionList<R>.Stubber0 waitToBeCancelled() {
    return stubber.waitToBeCancelled();
  }

  /**
   * Called to populate this action list into the specified expectation.
   *
   * @param expectation the expectation to be populated with this list of action
   */
  synchronized void populate(ActionRegistry<R>.Expectation expectation) {
    actions.stream().map(a -> a.apply(expectation)).forEach(expectation::add);
  }

  /**
   * Adds an action creator to this list. The creator will be consulted later when it is time to
   * populate an expectation.
   *
   * @param creator a function to create the action later
   */
  private synchronized void add(Function<ActionRegistry<R>.Expectation, Action<R>> creator) {
    actions.add(creator);
  }

  /** Construct used to register a sequence of actions. */
  public class Stubber implements Done<R> {
    private Stubber() { // only allow creation from within this class
    }

    @Override
    public ActionList<R> done() {
      return ActionList.this;
    }

    /**
     * Adds all specified actions to this list.
     *
     * <p>Syntax sugar for Spock users which allows you to write this:
     *
     * <p><code><pre>
     *   doThrow(NullPointerException) + doReturn(true)
     * </pre></code>
     *
     * @param actionList the list of actions to be added to this one
     * @return this list for chaining
     */
    public ActionList<R> plus(Done<R> actionList) {
      return add(actionList);
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
      add(expectation -> new DoReturnAction<>(expectation, "doReturn", results));
      return stubber2;
    }

    /**
     * Registers the specified results to be returned in sequence the next time failsafe attempts a
     * retry. Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @param results the results to be returned in sequence or <code>null</code> if a single <code>
     *     null</code> element should be returned
     * @return a stub construct for chaining
     */
    public Stubber2 returning(@Nullable R... results) {
      add(expectation -> new DoReturnAction<>(expectation, "returning", results));
      return stubber2;
    }

    /**
     * Indicates to do nothing (return <code>null</code>) the next time failsafe attempts a retry.
     * Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doNothing() {
      add(expectation -> new DoNothingAction<>(expectation, "doNothing"));
      return stubber2;
    }

    /**
     * Indicates to do nothing (return <code>null</code>) the next time failsafe attempts a retry.
     * Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doReturn() {
      add(expectation -> new DoNothingAction<>(expectation, "doReturn"));
      return stubber2;
    }

    /**
     * Indicates to do nothing (return <code>null</code>) the next time failsafe attempts a retry.
     * Doing so short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 returning() {
      add(expectation -> new DoNothingAction<>(expectation, "returning"));
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
      add(expectation -> new DoThrowAction<>(expectation, "doThrow", throwables));
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
    public Stubber2 throwing(Throwable... throwables) {
      add(expectation -> new DoThrowAction<>(expectation, "throwing", throwables));
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
      add(expectation -> new DoThrowAction<>(expectation, "doThrow", throwables));
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
    public Stubber2 throwing(Class<? extends Throwable>... throwables) {
      add(expectation -> new DoThrowAction<>(expectation, "throwing", throwables));
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
      add(expectation -> new DoThrowOrReturnAction<>(expectation, "doThrowOrReturn", arguments));
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
    public Stubber2 throwingOrReturning(@Nullable Object... arguments) {
      add(
          expectation ->
              new DoThrowOrReturnAction<>(expectation, "throwingOrReturning", arguments));
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
    public Stubber2 doReturnOrThrow(@Nullable Object... arguments) {
      add(expectation -> new DoThrowOrReturnAction<>(expectation, "doReturnOrThrow", arguments));
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
    public Stubber2 returningOrThrowing(@Nullable Object... arguments) {
      add(
          expectation ->
              new DoThrowOrReturnAction<>(expectation, "returningOrThrowing", arguments));
      return stubber2;
    }

    /**
     * Indicates to simulate a thread interruption the next time failsafe attempts a retry. Doing so
     * short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doInterrupt() {
      add(expectation -> new DoInterruptAction<>(expectation, "doInterrupt"));
      return stubber2;
    }

    /**
     * Indicates to simulate a thread interruption the next time failsafe attempts a retry. Doing so
     * short circuits any actions registered with failsafe by the code under test.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 interrupting() {
      add(expectation -> new DoInterruptAction<>(expectation, "interrupting"));
      return stubber2;
    }

    /**
     * Indicates to proceed with calling the production action registered with failsafe normally the
     * next time failsafe attempts a retry.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 doProceed() {
      add(expectation -> new DoProceedAction<>(expectation, "doProceed"));
      return stubber2;
    }

    /**
     * Indicates to proceed with calling the production action registered with failsafe normally the
     * next time failsafe attempts a retry.
     *
     * @return a stub construct for chaining
     */
    public Stubber2 proceeding() {
      add(expectation -> new DoProceedAction<>(expectation, "proceeding"));
      return stubber2;
    }

    /**
     * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
     * executing this action, the controller will move on to the next one and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to notify
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 doNotify(String condition) {
      add(expectation -> new DoNotifyAction(expectation, "doNotify", condition));
      return stubber0;
    }

    /**
     * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
     * executing this action, the controller will move on to the next one and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to notify
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 notifying(String condition) {
      add(expectation -> new DoNotifyAction(expectation, "notifying", condition));
      return stubber0;
    }

    /**
     * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
     * executing this action, the controller will move on to the next one and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to notify
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 doNotifyTo(String condition) {
      add(expectation -> new DoNotifyAction(expectation, "doNotifyTo", condition));
      return stubber0;
    }

    /**
     * Indicates to notify the specified condition/latch and wake up anybody waiting on it. After
     * executing this action, the controller will move on to the next one and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to notify
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 notifyingTo(String condition) {
      add(expectation -> new DoNotifyAction(expectation, "notifyTo", condition));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until the specified
     * condition/latch is notified. After the specified condition/latch has been notified, the
     * controller will move on to the next action and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to wait for
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 waitFor(String condition) {
      add(expectation -> new WaitForAction<>(expectation, "waitFor", condition));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until the specified
     * condition/latch is notified. After the specified condition/latch has been notified, the
     * controller will move on to the next action and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to wait for
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 waitingFor(String condition) {
      add(expectation -> new WaitForAction<>(expectation, "waitingFor", condition));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until the specified
     * condition/latch is notified. After the specified condition/latch has been notified, the
     * controller will move on to the next action and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to wait for
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 waitTo(String condition) {
      add(expectation -> new WaitForAction<>(expectation, "waitTo", condition));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until the specified
     * condition/latch is notified. After the specified condition/latch has been notified, the
     * controller will move on to the next action and execute it right away.
     *
     * <p>If the specified condition/latch has already been notified then the controller will move
     * on to the next action.
     *
     * @param condition the condition/latch to wait for
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 waitingTo(String condition) {
      add(expectation -> new WaitForAction<>(expectation, "waitingTo", condition));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until it execution is
     * cancelled. After the execution is cancelled, the controller will move on to the next action
     * and execute it right away.
     *
     * <p>If the execution has already been cancelled then the controller will will move on to the
     * next action.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 waitToBeCancelled() {
      add(expectation -> new WaitToBeCancelledAction<>(expectation, "waitToBeCancelled"));
      return stubber0;
    }

    /**
     * Indicates to block failsafe the next time it attempts a retry until it execution is
     * cancelled. After the execution is cancelled, the controller will move on to the next action
     * and execute it right away.
     *
     * <p>If the execution has already been cancelled then the controller will will move on to the
     * next action.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 waitingToBeCancelled() {
      add(expectation -> new WaitToBeCancelledAction<>(expectation, "waitingToBeCancelled"));
      return stubber0;
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber and() {
      return this;
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber then() {
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
      return onlyIf(condition, Boolean.toString(condition));
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
      add(expectation -> new OnlyIfAction<>(expectation, "onlyIf", condition, info));
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
      return onlyIf(predicate, "predicate<?>");
    }

    /**
     * Indicates to conditionally execute the previously registered action. The predicate will be
     * evaluated only the first time the action is executed.
     *
     * @param predicate a predicate that returns <code>true</code> to execute the action; <code>
     *     false</code> not to execute it
     * @param info string representation of the predicate
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(BooleanSupplier predicate, String info) {
      add(expectation -> new OnlyIfAction<>(expectation, "onlyIf", predicate, info));
      return stubber;
    }

    /**
     * Indicates to conditionally execute the previously registered action. The predicate will be
     * evaluated only the first time the action is executed.
     *
     * <p>This method makes it nice to use in with Spock.
     *
     * @param closure a closure that returns <code>true</code> to execute the action; <code>
     *     false</code> not to execute it
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(Closure<Boolean> closure) {
      return onlyIf(closure, "closure<?>");
    }

    /**
     * Indicates to conditionally execute the previously registered action. The closure will be
     * evaluated only the first time the action is executed.
     *
     * <p>This method makes it nice to use in with Spock.
     *
     * @param closure a closure that returns <code>true</code> to execute the action; <code>
     *     false</code> not to execute it
     * @param info string representation of the closure
     * @return a stub construct for chaining
     */
    public Stubber onlyIf(Closure<Boolean> closure, String info) {
      return onlyIf(closure::call, info);
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 before() {
      return this;
    }

    /**
     * Syntax sugar.
     *
     * @return a stub construct for chaining
     */
    public Stubber0 but() {
      return this;
    }
  }

  /** Construct used to configure the previously registered action. */
  public class Stubber1 extends Stubber0 {
    private Stubber1() { // only allow creation from within this class
    }

    /**
     * Indicates to repeat the previously registered action until the specified condition/latch has
     * been notified at which point the controller will move on to the next action and execute it
     * right away.
     *
     * <p>The previous action will be executed at least once even if the condition/latch has already
     * been notified.
     *
     * @param condition the condition/latch to check before repeating the previous action
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 untilNotifiedFor(String condition) {
      add(expectation -> new UntilNotifiedForAction<>(expectation, "untilNotifiedFor", condition));
      return stubber0;
    }

    /**
     * Indicates to repeat the previously registered action until the specified condition/latch has
     * been notified at which point the controller will move on to the next action and execute it
     * right away.
     *
     * <p>The previous action will be executed at least once even if the condition/latch has already
     * been notified.
     *
     * @param condition the condition/latch to check before repeating the previous action
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>condition</code> is <code>null</code>
     */
    public Stubber0 untilNotifiedTo(String condition) {
      add(expectation -> new UntilNotifiedForAction<>(expectation, "untilNotifiedTo", condition));
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
      add(expectation -> new UntilCancelledAction<>(expectation, "untilCancelled"));
      return stubber0;
    }

    /**
     * Indicates to repeat the previously registered action forever (i.e. until the controller is
     * shutdown).
     *
     * @return the action list for chaining
     */
    public ActionList<R> forever() {
      add(expectation -> new ForeverAction<>(expectation, "forever"));
      return ActionList.this;
    }

    /**
     * Indicates to never execute the previously registered action.
     *
     * @return a stub construct for chaining
     */
    public Stubber never() {
      add(expectation -> new NeverAction<>(expectation, "never"));
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
      add(expectation -> new TimesAction<>(expectation, "times", count));
      return stubber1;
    }

    /**
     * Indicates to delay the previously registered action for the specified amount of time.
     *
     * @param delay the number of seconds to delay
     * @return a stub construct for chaining
     * @throws IllegalArgumentException if <code>delay</code> is negative
     */
    public Stubber0 delayedFor(int delay) {
      add(expectation -> new DelayedForAction<>(expectation, "delayedFor", delay));
      return stubber0;
    }
  }
}
