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

import javax.annotation.Nullable;
import net.jodah.failsafe.internal.actions.ActionList;

/**
 * Public point of access for creating an action list when defining expectations for an execution.
 */
public class Actions {
  /**
   * Simple termination point when building a list of actions.
   *
   * @param <R> the result type
   */
  public interface Done<R> {
    /**
     * Completes a sequence of actions for the failsafe controller.
     *
     * @return the action list for chaining
     */
    public ActionList<R> done();
  }

  private Actions() { // prevent instantiation
  }

  /**
   * Registers the specified results to be returned in sequence the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * @param <R> the result type
   * @param results the results to be returned in sequence or <code>null</code> if a single <code>
   *     null</code> element should be returned
   * @return a stub construct for chaining
   */
  public static <R> ActionList<R>.Stubber2 doReturn(@Nullable R... results) {
    return new ActionList<R>().doReturn(results);
  }

  /**
   * Indicates to do nothing (return <code>null</code>) the next time failsafe attempts a retry.
   * Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * @param <R> the result type
   * @return a stub construct for chaining
   */
  public static <R> ActionList<R>.Stubber2 doNothing() {
    return new ActionList<R>().doNothing();
  }

  /**
   * Registers the specified exceptions to be thrown in sequence the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * <p><i>Note:</i> The stack trace of each exception will be re-filled just before being thrown
   * out.
   *
   * @param <R> the result type
   * @param throwables the exceptions objects to be thrown in sequence
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>throwables</code> or any of its elements is <code>
   *     null</code>
   */
  public static <R> ActionList<R>.Stubber2 doThrow(Throwable... throwables) {
    return new ActionList<R>().doThrow(throwables);
  }

  /**
   * Registers the specified exceptions to be thrown in sequence the next time failsafe attempts a
   * retry. Doing so short circuits any actions registered with failsafe by the code under test.
   *
   * <p><i>Note:</i> Actual exception objects will be instantiated for the provided classes using a
   * constructor that takes a message string, one that takes a message string and a cause, one that
   * takes a cause, or the default constructor.
   *
   * @param <R> the result type
   * @param throwables the exceptions classes to be instantiated and thrown in sequence
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>throwables</code> or any of its elements is <code>
   *     null</code> if unable to instantiate any exception classes
   */
  public static <R> ActionList<R>.Stubber2 doThrow(Class<? extends Throwable>... throwables) {
    return new ActionList<R>().doThrow(throwables);
  }

  /**
   * Registers the specified results or exceptions to be returned or thrown in sequence the next
   * time failsafe attempts a retry. Doing so short circuits any actions registered with failsafe by
   * the code under test.
   *
   * @param <R> the result type
   * @param arguments the results to be returned or the exceptions to be thrown or the exception
   *     classes to be instantiated and thrown in sequence or <code>null</code> if a single <code>
   *     null</code> element should be returned
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if unable to instantiate any exception classes
   */
  public static <R> ActionList<R>.Stubber2 doThrowOrReturn(@Nullable Object... arguments) {
    return new ActionList<R>().doThrowOrReturn(arguments);
  }

  /**
   * Indicates to simulate a thread interruption the next time failsafe attempts a retry. Doing so
   * short circuits any actions registered with failsafe by the code under test.
   *
   * @param <R> the result type
   * @return a stub construct for chaining
   */
  public static <R> ActionList<R>.Stubber2 doInterrupt() {
    return new ActionList<R>().doInterrupt();
  }

  /**
   * Indicates to proceed with calling the production action registered with failsafe normally the
   * next time failsafe attempts a retry.
   *
   * @param <R> the result type
   * @return a stub construct for chaining
   */
  public static <R> ActionList<R>.Stubber2 doProceed() {
    return new ActionList<R>().doProceed();
  }

  /**
   * Indicates to block failsafe the next time it attempts a retry until the specified latch is
   * notified. After the specified latch has been notified, the controller will move on to the next
   * action and execute it right away.
   *
   * <p>If the specified latch has already been notified then the controller will move on to the
   * next action.
   *
   * @param <R> the result type
   * @param latch the latch to wait for
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>latch</code> is <code>null</code>
   */
  public static <R> ActionList<R>.Stubber0 waitFor(String latch) {
    return new ActionList<R>().waitFor(latch);
  }

  /**
   * Indicates to notify the specified latch and wake up anybody waiting on it. After executing this
   * action, the controller will move on to the next one and execute it right away.
   *
   * <p>If the specified latch has already been notified then the controller will move on to the
   * next action.
   *
   * @param <R> the result type
   * @param latch the latch to notify
   * @return a stub construct for chaining
   * @throws IllegalArgumentException if <code>latch</code> is <code>null</code>
   */
  public static <R> ActionList<R>.Stubber0 doNotify(String latch) {
    return new ActionList<R>().doNotify(latch);
  }

  /**
   * Indicates to block failsafe the next time it attempts a retry until it's execution is
   * cancelled. After the execution is cancelled, the controller will simulate an interruption by
   * throwing an {@link InterruptedException} back.
   *
   * <p>If the execution has already been cancelled then the controller will throw the interrupted
   * exception right away.
   *
   * @param <R> the result type
   * @return a stub construct for chaining
   */
  public static <R> ActionList<R>.Stubber0 waitToBeCancelled() {
    return new ActionList<R>().waitToBeCancelled();
  }
}
