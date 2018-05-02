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
 * Action to throw one exception or return one result at a time each failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class DoThrowOrReturnAction<R> extends Action<R> {
  private final List<?> arguments;
  private final List<?> processedArguments;
  private final List<?> current;

  DoThrowOrReturnAction(
      ActionRegistry<R>.Expectation expectation, String name, @Nullable Object... args) {
    super(expectation, name);
    // if args is null then we consider it as a single null element
    this.arguments = (args != null) ? Arrays.asList(args) : Collections.singletonList(null);
    this.processedArguments =
        arguments
            .stream()
            .map(a -> ThrowableSupport.instantiateIfThrowableClass(controller, a))
            .collect(Collectors.toCollection(LinkedList::new));
    this.current = Collections.synchronizedList(new LinkedList<>(this.processedArguments));
  }

  private DoThrowOrReturnAction(DoThrowOrReturnAction<R> action) {
    super(action);
    this.arguments = action.arguments;
    this.processedArguments = action.processedArguments;
    this.current = Collections.synchronizedList(new LinkedList<>(action.processedArguments));
  }

  @Override
  public DoThrowOrReturnAction<R> copy() {
    return new DoThrowOrReturnAction<>(this);
  }

  @Override
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          final Object o;

          synchronized (current) {
            if (current.isEmpty()) {
              throw FailsafeContinueException.INSTANCE; // move on to the next one
            }
            o = current.remove(0);
          }
          if (o instanceof Throwable) {
            ThrowableSupport.sneakyThrow(((Throwable) o).fillInStackTrace());
          }
          return (R) o;
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
    return arguments
        .stream()
        .map(Objects::toString)
        .collect(Collectors.joining(",", name + "(", ")"));
  }
}
