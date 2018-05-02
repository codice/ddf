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

import org.apache.commons.lang.Validate;

/**
 * Action to repeat a given action each time failsafe makes attempts until a specified
 * condition/latch is notified.
 *
 * @param <R> the result type
 */
public class UntilNotifiedForAction<R> extends RepeatingAction<R> {
  private final String condition;

  private boolean notified = false;

  /**
   * Constructs a new action which will repeat the last recorded action from the specified
   * expectation until the specified condition/latch is notified.
   *
   * @param expectation the expectation where to get the last recorded action to repeat
   * @param name the name for this action
   * @param condition the condition/latch to check after the action has executed to see if it should
   *     be repeated
   * @throws IllegalArgumentException if <code>condition</code> if <code>null</code>
   */
  UntilNotifiedForAction(ActionRegistry<R>.Expectation expectation, String name, String condition) {
    super(expectation, name);
    Validate.notNull(condition, "invalid null condition");
    this.condition = condition;
  }

  private UntilNotifiedForAction(UntilNotifiedForAction<R> action) {
    super(action);
    this.condition = action.condition;
  }

  @Override
  public UntilNotifiedForAction<R> copy() {
    return new UntilNotifiedForAction<>(this);
  }

  @Override
  public synchronized boolean hasCompleted() {
    // make sure we execute the action at least once
    return notified && (currentCount > 0) && super.hasCompleted();
  }

  @Override
  public synchronized boolean canBeLeftIncomplete() {
    // make sure we start executing the action at least once
    return notified && (currentCount > 0) && super.canBeLeftIncomplete();
  }

  @Override
  public String toString() {
    return action + "." + name + "(" + condition + ")";
  }

  @Override
  protected synchronized boolean shouldRepeat(ActionContext<R> context) {
    this.notified = controller.wasNotified(condition);
    // make sure that we execute the action at least once
    return !notified || (currentCount == 0);
  }
}
