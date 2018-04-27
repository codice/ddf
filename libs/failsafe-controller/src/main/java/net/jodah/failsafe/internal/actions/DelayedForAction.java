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

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.Validate;

/**
 * Action to delay a given action for some time each time failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class DelayedForAction<R> extends RepeatingAction<R> {
  private final int delay;

  /**
   * Constructs a new action which will delay the specified action for the specified amount of time.
   *
   * @param expectation the expectation where to get the last recorded action to delay
   * @param name the name for this action
   * @param delay the number of seconds to delay
   * @throws IllegalArgumentException if <code>delay</code> is negative
   */
  DelayedForAction(ActionRegistry<R>.Expectation expectation, String name, int delay) {
    super(expectation, name);
    Validate.isTrue(delay >= 0, "number of seconds must not be negative");
    this.delay = delay;
  }

  @Override
  public boolean hasCompleted() {
    return false; // should execute only once
  }

  @Override
  public String toString() {
    return action + "." + name + "(" + delay + ")";
  }

  @Override
  protected boolean shouldRepeat(ActionContext<R> context) throws InterruptedException {
    if (currentCount == 0) { // should execute only once
      logger.debug("FailsafeController({}): delaying {} seconds", controller, delay);
      Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
      return true;
    }
    return false;
  }
}
