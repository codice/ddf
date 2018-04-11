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
 * Action to repeat a given action each time failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class ForeverAction<R> extends RepeatingAction<R> {
  /**
   * Constructs a new action which will repeat the specified action forever.
   *
   * @param action the action to be repeated
   */
  public ForeverAction(Action<R> action) {
    super(action);
  }

  private ForeverAction(ForeverAction<R> action) {
    super(action);
  }

  @Override
  public ForeverAction<R> copy() {
    return new ForeverAction<>(this);
  }

  @Override
  public boolean hasCompleted() {
    return false; // it never completes
  }

  @Override
  public boolean canBeLeftIncomplete() {
    return true; // forever action actions can always be left uncomplete by design
  }

  @Override
  public String toString() {
    return action + ".forever()";
  }

  @Override
  protected boolean shouldRepeat(ActionContext<R> context) {
    return true; // should always repeat
  }
}
