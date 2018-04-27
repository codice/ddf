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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Stream;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.internal.FailsafeContinueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines an action that can be executed by failsafe controller when a retry attempt is
 * intercepted.
 *
 * @param <R> the result type
 */
public abstract class Action<R> {
  /**
   * Returned value when nothing is being or needs to be returned (e.g. as a result of a void-based
   * method like in a {@link Runnable}).
   */
  public static final Object NOTHING = "NOTHING";

  /** Set of package prefixes to skip when determining the location where the action is defined */
  private static final Set<String> PACKAGE_PREFIXES =
      ImmutableSet.of(
          FailsafeController.class.getPackage().getName() + ".",
          "org.codehaus.groovy.",
          "sun.",
          "com.sun.",
          "java.",
          "javax.");

  /**
   * Internal interface used to represent the action code to be executed.
   *
   * @param <R> the result type
   */
  @FunctionalInterface
  protected interface ThrowingSupplier<R> {
    @SuppressWarnings("squid:S00112" /* Failsafe uses interfaces that declare Exception */)
    public R get() throws Exception;
  }

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final FailsafeController<R> controller;

  protected final String name;

  private final StackTraceElement stack;

  Action(ActionRegistry<R>.Expectation expectation, String name) {
    this(expectation.getController(), name);
  }

  Action(FailsafeController<R> controller, String name) {
    this.controller = controller;
    this.name = name;
    // find the stack trace element for the test case - this would be the first stack element
    // that is not from a Failsafe Controller class
    this.stack =
        Stream.of(Thread.currentThread().getStackTrace())
            .skip(1) // skip the getStackTrace() method
            .filter(Action::isNotFromAReservedPackage)
            .findFirst()
            .orElseGet(() -> new StackTraceElement("<unknown>", "<unknown>", null, -1));
  }

  Action(Action<R> action) {
    this.controller = action.controller;
    this.name = action.name;
    this.stack = action.stack;
  }

  /**
   * Gets the controller associated with this action.
   *
   * @return the controller assocaited with this action
   */
  public FailsafeController<R> getController() {
    return controller;
  }

  /**
   * Gets information about where the action was defined in the test code.
   *
   * @return a stack trace element corresponding to the location where the action was defined
   */
  public StackTraceElement getDefinitionInfo() {
    return stack;
  }

  /**
   * Creates a copy of this action with its state reset.
   *
   * <p>The default implementation of this method returns this object. This would be useful for case
   * where the action doesn't need to maintain state between executions.
   *
   * @return a copy of this action
   */
  public Action<R> copy() {
    return this;
  }

  /**
   * Executes this action.
   *
   * @param context the context representing the current failsafe attempt
   * @return the result to be returned from the failsafe attempt or {@link #NOTHING} if nothing
   *     should be returned (e.g. as a result of a void-based * method like in a {@link Runnable})
   * @throws FailsafeContinueException if the controller should execute the next recorded action
   *     right away in order to determine the failsafe attempt result
   * @throws Exception if an error occurred while executing the action (this error will be thrown
   *     back to failsafe as the result of the attempt)
   */
  @SuppressWarnings("squid:S00112" /* Failsafe uses interfaces that declare Exception */)
  public abstract R execute(ActionContext<R> context) throws Exception;

  /**
   * Checks if this action has completed. An action object will keep track of its internal state and
   * will be updated each time it is executed. Checking if it has completed after executing it will
   * allow the controller to determine when it needs to move to the next recorded action.
   *
   * <p>The default implementation returns <code>true</code> indicating the action is completed
   * after its first execution.
   *
   * @return <code>true</code> if the last execution resulting in completing this action; <code>
   *     false</code> if it needs to be executed again the next time an attempt is made by failsafe
   */
  public boolean hasCompleted() {
    return true;
  }

  /**
   * Called at the time the controller is asked to verify if there are no more actions to see if
   * this action can be omitted from the list of un-executed actions.
   *
   * @return <code>true</code> if this action can be left partially or not executed; <code>false
   *     </code> otherwise
   */
  public boolean canBeLeftIncomplete() {
    return false;
  }

  /**
   * Useful method to execute the provided supplier with debug traces. This method can be called by
   * subclass to surround its execution with debug statements.
   *
   * @param context the context representing the current failsafe attempt
   * @param info action specific info to add to the debug statements
   * @param supplier the supplier design to perform the action's code which should either return a
   *     result or throw back an error in case of failure
   * @return the result to be returned from the failsafe attempt or {@link #NOTHING} if nothing
   *     should be returned (e.g. as a result of a void-based * method like in a {@link Runnable})
   * @throws FailsafeContinueException if the controller should execute the next recorded action
   *     right away in order to determine the failsafe attempt result
   * @throws Exception if an error occurred while executing the action (this error will be thrown
   *     back to failsafe as the result of the attempt)
   */
  @SuppressWarnings("squid:S1181" /* purposely catching VirtualMachineError first */)
  protected R execute(ActionContext<R> context, String info, ThrowingSupplier<R> supplier)
      throws Exception {
    logger.debug("FailsafeController({}): executing {}{}", controller, this, info);
    try {
      final R r = supplier.get();

      if (logger.isDebugEnabled()) {
        if (r == Action.NOTHING) {
          logger.debug("FailsafeController({}): executed {}", controller, this);
        } else {
          logger.debug("FailsafeController({}): executed {} and returned: {}", controller, this, r);
        }
      }
      return r;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (FailsafeContinueException e) {
      if (hasCompleted()) {
        logger.debug("FailsafeController({}): done executing {}; continuing", controller, this);
      } else {
        logger.debug("FailsafeController({}): looping execution {}", controller, this);
      }
      throw e;
    } catch (Exception | Error e) {
      logger.debug("FailsafeController({}): executing {} and threw: {}", controller, this, e, e);
      throw e;
    }
  }

  /**
   * Gets a string representation of the current state of this action along with information about
   * where it was defined.
   *
   * @return a string representing the current state of this action
   */
  public String currentToString() {
    return toString() + " at " + stack;
  }

  /**
   * Gets a string representation of the defined state of this action along with information about
   * where it was defined.
   *
   * @return a string representing the defined state of this action
   */
  public String definedToString() {
    return toString() + " at " + stack;
  }

  private static boolean isNotFromAReservedPackage(StackTraceElement se) {
    final String clazz = se.getClassName();

    return !Action.PACKAGE_PREFIXES.stream().anyMatch(clazz::startsWith);
  }
}
