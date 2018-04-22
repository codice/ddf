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

public abstract class RepeatingAction<R> extends Action<R> {
  /** Action being repeated. */
  protected final Action<R> action;

  /** Action being executed in the current iteration. */
  protected Action<R> current = null;

  /** The number of times the action was repeated. */
  protected int currentCount = 0;

  RepeatingAction(ActionRegistry<R>.Expectation expectation, String name) {
    super(expectation, name);
    this.action = expectation.removeLast();
  }

  RepeatingAction(RepeatingAction<R> action) {
    super(action);
    this.action = action.action;
  }

  public Action<R> getAction() {
    return action;
  }

  @Override
  public final R execute(ActionContext<R> context) throws Exception {
    synchronized (this) {
      if (current == null) {
        if (!shouldRepeat(context)) {
          throw FailsafeContinueException.INSTANCE; // move on to the next one
        }
        this.current = action.copy();
        this.currentCount++;
      }
    }
    return super.execute(
        context,
        "",
        () -> {
          try {
            return current.execute(context);
          } finally {
            if (current.hasCompleted()) {
              synchronized (this) {
                this.current = null;
              }
            }
          }
        });
  }

  @Override
  public boolean hasCompleted() {
    return (current == null);
  }

  @Override
  public boolean canBeLeftIncomplete() {
    return (current == null) || current.canBeLeftIncomplete();
  }

  @Override
  public String currentToString() {
    return toString() + " at " + action.getDefinitionInfo();
  }

  @Override
  public String definedToString() {
    return toString() + " at " + action.getDefinitionInfo();
  }

  /**
   * Checks if the action should be repeated/executed. This method is called before executing
   * whereas {@link #hasCompleted)} is called after.
   *
   * @param context the context representing the current failsafe attempt
   * @return <code>true</code> if the action should be repeated; <code>false</code> otherwise
   * @throws Exception if an error occurred
   */
  @SuppressWarnings("squid:S00112" /* based on Failsafe's API */)
  protected abstract boolean shouldRepeat(ActionContext<R> context) throws Exception;
}
