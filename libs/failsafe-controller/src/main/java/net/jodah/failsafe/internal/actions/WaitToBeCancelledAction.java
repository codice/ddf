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

/**
 * Action to wait for failsafe's execution to be cancelled.
 *
 * @param <R> the result type
 */
public class WaitToBeCancelledAction<R> extends Action<R> {
  private boolean executed = false;

  WaitToBeCancelledAction(ActionRegistry<R>.Expectation expectation, String name) {
    super(expectation, name);
  }

  private WaitToBeCancelledAction(WaitToBeCancelledAction<R> action) {
    super(action);
  }

  @Override
  public WaitToBeCancelledAction<R> copy() {
    return new WaitToBeCancelledAction<>(this);
  }

  @Override
  @SuppressWarnings("squid:S2142" /* interruption is re-thrown after failsafe is cancelled */)
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          synchronized (controller) {
            this.executed = true;
            boolean interrupted = false;

            try {
              while (!controller.wasLastExecutionCancelled()) {
                try {
                  controller.wait();
                } catch (InterruptedException e) {
                  // this means we internally got interrupted which either means we are about to
                  // discover that we were cancelled or we were shutdown or again a failure occurred
                  // so just loop back to check
                  interrupted = true;
                }
              }
            } finally {
              if (interrupted) {
                Thread.currentThread().interrupt();
              }
            }
          }
          throw FailsafeContinueException.INSTANCE; // move on to the next one
        });
  }

  @Override
  public boolean hasCompleted() {
    return executed;
  }

  @Override
  public boolean canBeLeftIncomplete() {
    return executed;
  }

  @Override
  public String toString() {
    return name + "()";
  }
}
