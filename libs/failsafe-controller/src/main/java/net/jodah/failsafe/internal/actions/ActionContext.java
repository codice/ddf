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

import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.function.ContextualCallable;

/**
 * Provides context information about a specific failsafe attempt.
 *
 * @param <R> the result type
 */
public class ActionContext<R> {
  private final FailsafeController<R> controller;

  @SuppressWarnings("unused" /* provided for future development */)
  private final ExecutionContext context;

  private final ContextualCallable<R> callable;

  /**
   * Constructs a new action context.
   *
   * @param controller the current failsafe controller
   * @param context the corresponding failsafe execution context
   * @param callable a reference to the production code that would normally be invoked during this
   *     failsafe attempt
   */
  public ActionContext(
      FailsafeController<R> controller, ExecutionContext context, ContextualCallable<R> callable) {
    this.controller = controller;
    this.context = context;
    this.callable = callable;
  }

  /**
   * Gets the current failsafe controller.
   *
   * @return the current failsafe controller
   */
  public FailsafeController<R> getController() {
    return controller;
  }

  /**
   * Proceeds normally in calling the production code that would normally have been called by
   * failsafe for this attempt.
   *
   * @return the result from the production code
   * @throws Exception if an error occurred while executing the production code
   */
  public R proceed() throws Exception {
    return callable.call(context);
  }
}
