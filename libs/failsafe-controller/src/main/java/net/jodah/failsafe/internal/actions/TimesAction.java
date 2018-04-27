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
 * Action to repeat a given action a specified number of times failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class TimesAction<R> extends RepeatingAction<R> {
  private final int count;

  /**
   * Constructs a new action which will repeat the last recorded action from the specified
   * expectation the specified number of times.
   *
   * @param expectation the expectation where to get the last recorded action to repeat
   * @param name the name for this action
   * @param count the number of times to repeat the action
   * @throws IllegalArgumentException if <code>count</code> is negative
   */
  TimesAction(ActionRegistry<R>.Expectation expectation, String name, int count) {
    super(expectation, name);
    Validate.isTrue(count >= 0, "count must be greater or equal than 0");
    this.count = count;
  }

  private TimesAction(TimesAction<R> action) {
    super(action);
    this.count = action.count;
  }

  @Override
  public TimesAction<R> copy() {
    return new TimesAction<>(this);
  }

  @Override
  public synchronized boolean hasCompleted() {
    return (count == 0) || ((currentCount == count) && super.hasCompleted());
  }

  @Override
  public synchronized boolean canBeLeftIncomplete() {
    // if count was 0 then it should never be executed
    // otherwise, we must have gone through all number of attempts configured and on the last one
    // we can check if the action we are repeating can be left uncomplete
    return (count == 0) || ((currentCount == count) && super.canBeLeftIncomplete());
  }

  @Override
  public synchronized String currentToString() {
    return action
        + "."
        + name
        + "("
        + (count - currentCount)
        + ") at "
        + action.getDefinitionInfo();
  }

  @Override
  public String toString() {
    return action + "." + name + "(" + count + ")";
  }

  @Override
  protected synchronized boolean shouldRepeat(ActionContext<R> context) {
    return currentCount < count;
  }
}
