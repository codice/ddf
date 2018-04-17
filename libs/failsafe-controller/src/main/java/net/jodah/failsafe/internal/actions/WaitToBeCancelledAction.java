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
 * Action to wait for failsafe's execution to be cancelled.
 *
 * @param <R> the result type
 */
public class WaitToBeCancelledAction<R> extends Action<R> {
  WaitToBeCancelledAction(ActionRegistry<R>.Expectation expectation) {
    super(expectation);
  }

  @Override
  @SuppressWarnings("squid:S2142" /* interruption is re-thrown after failsafe is cancelled */)
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          synchronized (controller) {
            while (!controller.wasLastExecutionCancelled()) {
              try {
                controller.wait();
              } catch (InterruptedException e) {
                // this means we internally got interrupted which either means we are about to
                // discover that we were cancelled or we were shutdown or again a failure occurred
                // so just loop back to check
              }
            }
          }
          throw new InterruptedException("'" + controller + "' was cancelled");
        });
  }

  @Override
  public boolean hasCompleted() {
    return controller.wasLastExecutionCancelled();
  }

  @Override
  public String toString() {
    return "waitToBeCancelled()";
  }
}
