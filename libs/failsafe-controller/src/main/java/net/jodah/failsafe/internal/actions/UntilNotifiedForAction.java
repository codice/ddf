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
 * Action to repeat a given action each time failsafe makes attempts until a specified latch is
 * notified.
 *
 * @param <R> the result type
 */
public class UntilNotifiedForAction<R> extends RepeatingAction<R> {
  private final Action<R> action;

  private final String latch;

  private boolean notified = false;

  /**
   * Constructs a new action which will repeat the specified action until the specified latch is
   * notified.
   *
   * @param action the action to be repeated
   * @param latch the latch to check after the action has executed to see if it should be repeated
   * @throws IllegalArgumentException if <code>latch</code> if <code>null</code>
   */
  public UntilNotifiedForAction(Action<R> action, String latch) {
    super(action);
    Validate.notNull(latch, "invalid null latch");
    this.action = action;
    this.latch = latch;
  }

  private UntilNotifiedForAction(UntilNotifiedForAction<R> action) {
    super(action);
    this.action = action.action;
    this.latch = action.latch;
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
    return action + ".untilNotifiedFor(" + latch + ")";
  }

  @Override
  protected synchronized boolean shouldRepeat(ActionContext<R> context) {
    this.notified = controller.wasNotified(latch);
    // make sure that we execute the action at least once
    return !notified || (currentCount == 0);
  }
}
