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

import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;

/**
 * Action to conditionally execute a given action.
 *
 * @param <R> the result type
 */
public class OnlyIfAction<R> extends RepeatingAction<R> {
  /** Keeps track of wether the condition is dynamic or not */
  private final boolean dynamic;

  @Nullable private final BooleanSupplier predicate;
  private final String info;

  private Boolean currentCondition; // null if never evaluated

  /**
   * Constructs a new action which will conditionally execute the last recorded action from the
   * specified expectation.
   *
   * @param expectation the expectation where to get the last recorded action to be conditionally
   *     executed
   * @param name the name for this action
   * @param condition <code>true</code> to execute the action; <code>false</code> not to execute it
   * @param info string representation of the condition
   */
  public OnlyIfAction(
      ActionRegistry<R>.Expectation expectation, String name, boolean condition, String info) {
    super(expectation, name);
    this.dynamic = false;
    this.predicate = null;
    this.info = info;
    this.currentCondition = condition;
  }

  /**
   * Constructs a new action which will conditionally execute the last recorded action from the
   * specified expectation. The predicate will be evaluated only the first time the action is
   * executed.
   *
   * @param expectation the expectation where to get the last recorded action to be conditionally
   *     executed
   * @param name the name for this action
   * @param predicate a predicate that returns <code>true</code> to execute the action; <code>
   *     false</code> not to execute it
   * @param info string representation of the condition
   */
  OnlyIfAction(
      ActionRegistry<R>.Expectation expectation,
      String name,
      BooleanSupplier predicate,
      String info) {
    super(expectation, name);
    this.dynamic = true;
    this.predicate = predicate;
    this.info = info;
    this.currentCondition = null;
  }

  private OnlyIfAction(OnlyIfAction<R> action) {
    super(action);
    this.dynamic = action.dynamic;
    this.predicate = action.predicate;
    this.info = action.info;
    this.currentCondition = action.dynamic ? null : action.currentCondition;
  }

  @Override
  public OnlyIfAction<R> copy() {
    return new OnlyIfAction<>(this);
  }

  @Override
  public boolean hasCompleted() {
    return true; // will execute a maximum of 1 time only if enabled
  }

  @Override
  public synchronized boolean canBeLeftIncomplete() {
    if (currentCount > 0) { // we were evaluated at least once so all depends on the action
      return super.canBeLeftIncomplete();
    } else if (dynamic && (currentCondition == null)) { // never evaluated yet so do a quick eval
      this.currentCondition = predicate.getAsBoolean();
      // fall-through to check if it is now disable or not
    } // else - it all depends on the predicate's result or if was disabled with a static condition
    return !currentCondition;
  }

  @Override
  public String toString() {
    return action + "." + name + "(" + info + ")";
  }

  @Override
  protected synchronized boolean shouldRepeat(ActionContext<R> context) {
    if (currentCount > 0) { // should never be executed more than once
      return false;
    }
    if (dynamic) {
      this.currentCondition = predicate.getAsBoolean();
    }
    return currentCondition;
  }
}
