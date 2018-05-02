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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.function.ContextualCallable;
import net.jodah.failsafe.internal.FailsafeContinueException;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The action registry class is used to track failsafe actions registered with a controller.
 *
 * @param <R> the result type
 */
public class ActionRegistry<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActionRegistry.class);

  private static final String NL_AND_INDENT = String.format("%n\t");

  private final FailsafeController<R> controller;

  private final Deque<Expectation> expectations = new LinkedList<>();

  private final Deque<Expectation> processing = new LinkedList();

  private AssertionError shutdownFailure = null;

  public ActionRegistry(FailsafeController<R> controller) {
    this.controller = controller;
  }

  /**
   * Adds the specified action list as the next expected execution.
   *
   * @param actionList the list of expected actions for the next expected execution
   */
  public void add(ActionList<R> actionList) {
    synchronized (controller) {
      final Expectation expectation = new Expectation(expectations.size() + 1);

      actionList.populate(expectation);
      expectations.add(expectation);
    }
  }

  /**
   * Adds the specified action list as the next <code>count</code> expected executions.
   *
   * @param actionList the list of expected actions for the next expected executions
   * @param count the number of expected executions to register the list of actions with
   * @throws IllegalArgumentException if <code>count</code> is negative
   */
  public void add(ActionList<R> actionList, int count) {
    Validate.isTrue(count >= 0, "count must be greater or equal than 0");
    synchronized (controller) {
      for (int i = 0; i < count; i++) {
        final Expectation expectation = new Expectation(expectations.size() + 1);

        actionList.populate(expectation);
        expectations.add(expectation);
      }
    }
  }

  /**
   * Shuts down this registry.
   *
   * @param failure the associated failure to throw back if the registry is used afterward
   */
  public void shutdown(AssertionError failure) {
    synchronized (controller) {
      this.shutdownFailure = failure;
    }
  }

  /**
   * Verifies if all expected actions that cannot be left incomplete have been completed.
   *
   * @throws AssertionError if there are recorded actions left to execute
   */
  public void verify() {
    synchronized (controller) {
      processing.forEach(Expectation::verify);
      if (!expectations.isEmpty()) {
        throw controller.setFailure(
            new AssertionError(
                String.format(
                    "expecting %d executions for `%s` but only %d occurred; the following were not executed: %n\t%s%n",
                    (expectations.size() + processing.size()),
                    controller,
                    processing.size(),
                    expectations
                        .stream()
                        .map(Expectation::toString)
                        .collect(Collectors.joining(ActionRegistry.NL_AND_INDENT)))));
      }
    }
  }

  /**
   * Gets the next expectation to be executed.
   *
   * @return the next expectation
   * @throws AssertionError if there are not enough expected executions recorded
   */
  public Expectation next() {
    synchronized (controller) {
      final Expectation expectation = expectations.pop();

      if (expectation == null) {
        throw controller.setFailure(
            new AssertionError("not enough expected executions recorded for '" + controller + "'"));
      }
      processing.add(expectation);
      return expectation;
    }
  }

  /** Holds all expected actions for a given Failsafe execution. */
  public class Expectation {
    /** Unique identifier for this expectation. */
    private final int id;

    private final Deque<Action<R>> actions = new LinkedList<>();

    private final Deque<Action<R>> processed = new LinkedList();

    private Expectation(int id) { // prevents creation from outside the class
      this.id = id;
    }

    public FailsafeController<R> getController() {
      return controller;
    }

    /**
     * Gets a unique identifier for this expectation with the associated controller.
     *
     * @return a unique identifier for this expectation
     */
    public int getId() {
      return id;
    }

    /**
     * Verifies if all recorded actions that cannot be left incomplete have been completed.
     *
     * @throws AssertionError if there are recorded actions left to execute
     */
    public void verify() {
      synchronized (controller) {
        final List<Action<R>> left =
            actions.stream().filter(a -> !a.canBeLeftIncomplete()).collect(Collectors.toList());

        if (!left.isEmpty()) {
          throw controller.setFailure(
              new AssertionError(
                  String.format(
                      "too many expected actions for execution '%s - %s'; the following action(s) were not attempted: %n\t%s%n",
                      controller,
                      id,
                      left.stream()
                          .map(Action::currentToString)
                          .collect(Collectors.joining(ActionRegistry.NL_AND_INDENT)))));
        }
      }
    }

    @SuppressWarnings("squid:S1181" /* purposely catching VirtualMachineError first */)
    public R attempt(ExecutionContext context, ContextualCallable<R> callable) throws Exception {
      LOGGER.debug("FailsafeController({} - {}): failsafe is attempting", controller, id);
      while (true) {
        final ActionContext<R> actionContext = new ActionContext<>(controller, context, callable);
        final Action<R> action = peek();

        try {
          final R r = action.execute(actionContext);

          if (r == Action.NOTHING) {
            LOGGER.debug(
                "FailsafeController({} - {}): action {} completed", controller, id, action);
            return null; // nothing is returned back as null and will eventually be ignored
          }
          LOGGER.debug(
              "FailsafeController({} - {}): action {} returned: {}", controller, id, action, r);
          return r;
        } catch (VirtualMachineError e) {
          throw e;
        } catch (
            FailsafeContinueException e) { // do nothing and loop back to continue with the next
          LOGGER.debug(
              "FailsafeController({} - {}): action {} indicated to continue with next action",
              controller,
              id,
              action);
        } catch (Exception | Error e) {
          LOGGER.debug(
              "FailsafeController({} - {}): action {} threw: {}", controller, id, action, e, e);
          throw e;
        } finally {
          pop();
        }
      }
    }

    @Override
    public String toString() {
      return "execution #"
          + id
          + ": \r\n\t\t"
          + actions.stream().map(Action::toString).collect(Collectors.joining("\r\n\t\t"));
    }

    /**
     * Adds the specified action to this registry.
     *
     * @param action the action to be added
     */
    void add(Action<R> action) {
      if (LOGGER.isDebugEnabled()) {
        if (action instanceof RepeatingAction) {
          LOGGER.debug(
              "FailsafeController({} - {}): decorating last recorded action [{}] as: {}",
              controller,
              id,
              ((RepeatingAction<R>) action).getAction(),
              action);
        } else {
          LOGGER.debug("FailsafeController({} - {}): recording action: {}", controller, id, action);
        }
      }
      synchronized (controller) {
        actions.addLast(action);
      }
    }

    /**
     * Removes the last recorded action.
     *
     * @return the last recorded action
     * @throws java.util.NoSuchElementException if there are no actions recorded
     */
    Action<R> removeLast() {
      synchronized (controller) {
        return actions.removeLast();
      }
    }

    /**
     * Peeks at the next action to be executed.
     *
     * @return the next action to be executed
     * @throws AssertionError if there are no more actions in the registry (the controller's will be
     *     updated with a corresponding failure)
     */
    private Action<R> peek() {
      synchronized (controller) {
        failIfShutdown();
        final Action<R> action = actions.peek();

        if (action == null) {
          throw controller.setFailure(
              new AssertionError(
                  String.format(
                      "not enough expected actions for execution '%s - %s'; the following %d action(s) were processed: %n\t%s%n",
                      controller,
                      id,
                      processed.size(),
                      processed
                          .stream()
                          .map(Action::definedToString)
                          .collect(Collectors.joining(ActionRegistry.NL_AND_INDENT)))));
        }
        LOGGER.debug(
            "FailsafeController({} - {}): next action to execute: {}", controller, id, action);
        return action;
      }
    }

    /**
     * Pops the next action to be executed if it has completed.
     *
     * @return the next action if it has completed or <code>null</code> if it hasn't or if no
     *     actions are left
     */
    @Nullable
    private Action<R> pop() {
      synchronized (controller) {
        final Action<R> action = actions.peek();

        if ((action != null) && action.hasCompleted()) {
          LOGGER.debug("FailsafeController({} - {}): completed action: {}", controller, id, action);
          actions.pop();
          processed.add(action);
        }
        return action;
      }
    }

    /**
     * Checks if the registry was shutdown and throw back an assertion error if it has.
     *
     * @throws AssertionError if the registry was shutdown
     */
    private void failIfShutdown() {
      synchronized (controller) {
        if (shutdownFailure != null) {
          throw shutdownFailure;
        }
      }
    }
  }
}
