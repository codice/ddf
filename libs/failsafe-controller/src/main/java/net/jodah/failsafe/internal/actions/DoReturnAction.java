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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.jodah.failsafe.internal.FailsafeContinueException;

/**
 * Action to return one result at a time each failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class DoReturnAction<R> extends Action<R> {
  private final List<R> results;

  private final List<R> current;

  DoReturnAction(ActionRegistry<R>.Expectation expectation, String name, @Nullable R... results) {
    super(expectation, name);
    // if results is null then we consider it as a single null element
    this.results = (results != null) ? Arrays.asList(results) : Collections.singletonList(null);
    this.current = Collections.synchronizedList(new LinkedList<>(this.results));
  }

  private DoReturnAction(DoReturnAction action) {
    super(action);
    this.results = action.results;
    this.current = Collections.synchronizedList(new LinkedList<>(action.results));
  }

  @Override
  public DoReturnAction<R> copy() {
    return new DoReturnAction<>(this);
  }

  @Override
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          synchronized (current) {
            if (current.isEmpty()) {
              throw FailsafeContinueException.INSTANCE; // move on to the next one
            }
            return current.remove(0);
          }
        });
  }

  @Override
  public synchronized boolean hasCompleted() {
    return current.isEmpty();
  }

  @Override
  public synchronized boolean canBeLeftIncomplete() {
    return current.isEmpty();
  }

  @Override
  public String currentToString() {
    synchronized (current) {
      return current
              .stream()
              .map(Objects::toString)
              .collect(Collectors.joining(",", name + "(", ") at "))
          + getDefinitionInfo();
    }
  }

  @Override
  public String toString() {
    return results
        .stream()
        .map(Objects::toString)
        .collect(Collectors.joining(",", name + "(", ")"));
  }
}
