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

/**
 * Action to repeat a given action each time failsafe makes attempts until failsafe's execution is
 * cancelled.
 *
 * @param <R> the result type
 */
public class UntilCancelledAction<R> extends RepeatingAction<R> {
  private boolean cancelled = false;

  UntilCancelledAction(ActionRegistry<R>.Expectation expectation, String name) {
    super(expectation, name);
  }

  private UntilCancelledAction(UntilCancelledAction<R> action) {
    super(action);
  }

  @Override
  public UntilCancelledAction<R> copy() {
    return new UntilCancelledAction<>(this);
  }

  @Override
  public synchronized boolean hasCompleted() {
    return cancelled;
  }

  @Override
  public synchronized boolean canBeLeftIncomplete() {
    return cancelled;
  }

  @Override
  public String toString() {
    return action + "." + name + "()";
  }

  @Override
  protected synchronized boolean shouldRepeat(ActionContext<R> context) {
    this.cancelled = controller.wasLastExecutionCancelled();
    return !cancelled;
  }
}
