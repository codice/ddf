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
 * Action to never execute a given action each time failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class NeverAction<R> extends RepeatingAction<R> {
  NeverAction(ActionRegistry<R>.Expectation expectation, String name) {
    super(expectation, name);
  }

  private NeverAction(NeverAction<R> action) {
    super(action);
  }

  @Override
  public NeverAction<R> copy() {
    return new NeverAction<>(this);
  }

  @Override
  public boolean hasCompleted() {
    return true; // it never executes
  }

  @Override
  public boolean canBeLeftIncomplete() {
    return true;
  }

  @Override
  public String toString() {
    return action + "." + name + "()";
  }

  @Override
  protected boolean shouldRepeat(ActionContext<R> context) {
    return false; // should never execute
  }
}
