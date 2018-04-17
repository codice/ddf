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

import net.jodah.failsafe.internal.FailsafeContinueException;
import org.apache.commons.lang.Validate;

/**
 * Action to wait a latch to be notified before letting failsafe move on to the next action when it
 * makes an attempt.
 *
 * @param <R> the result type
 */
public class WaitForAction<R> extends Action<R> {
  private final String latch;

  WaitForAction(ActionRegistry<R>.Expectation expectation, String latch) {
    super(expectation);
    Validate.notNull(latch, "invalid null latch");
    this.latch = latch;
  }

  @Override
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          controller.waitFor(latch);
          throw FailsafeContinueException.INSTANCE; // move on to the next one
        });
  }

  @Override
  public boolean hasCompleted() {
    return controller.wasNotified(latch);
  }

  @Override
  public String toString() {
    return "waitFor(" + latch + ")";
  }
}
