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
 * Action to do nothing when failsafe makes an attempt.
 *
 * @param <R> the result type
 */
public class DoNothingAction<R> extends Action<R> {
  DoNothingAction(ActionRegistry<R>.Expectation expectation, String name) {
    super(expectation, name);
  }

  @Override
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(context, "", () -> (R) Action.NOTHING);
  }

  @Override
  public String toString() {
    return name + "()";
  }
}
