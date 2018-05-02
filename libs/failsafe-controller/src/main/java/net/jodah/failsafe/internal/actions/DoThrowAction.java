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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jodah.failsafe.internal.FailsafeContinueException;
import org.apache.commons.lang.Validate;

/**
 * Action to throw one exception at a time each failsafe makes attempts.
 *
 * @param <R> the result type
 */
public class DoThrowAction<R> extends Action<R> {
  private final List<?> arguments;
  private final List<Throwable> throwables;
  private final List<Throwable> current;

  DoThrowAction(ActionRegistry<R>.Expectation expectation, String name, Throwable... throwables) {
    super(expectation, name);
    Validate.notNull(throwables, "invalid null throwables");
    Validate.noNullElements(throwables, "invalid null throwable");
    this.arguments = Arrays.asList(throwables);
    this.throwables = Stream.of(throwables).collect(Collectors.toCollection(LinkedList::new));
    this.current = Collections.synchronizedList(new LinkedList<>(this.throwables));
  }

  DoThrowAction(
      ActionRegistry<R>.Expectation expectation,
      String name,
      Class<? extends Throwable>... throwables) {
    super(expectation, name);
    Validate.notNull(throwables, "invalid null throwables");
    Validate.noNullElements(throwables, "invalid null throwable");
    this.arguments = Arrays.asList(throwables);
    this.throwables =
        Stream.of(throwables)
            .map(t -> ThrowableSupport.instantiate(controller, t))
            .collect(Collectors.toCollection(LinkedList::new));
    this.current = Collections.synchronizedList(new LinkedList<>(this.throwables));
  }

  private DoThrowAction(DoThrowAction<R> action) {
    super(action);
    this.arguments = action.arguments;
    this.throwables = action.throwables;
    this.current = Collections.synchronizedList(new LinkedList<>(action.throwables));
  }

  @Override
  public DoThrowAction<R> copy() {
    return new DoThrowAction<>(this);
  }

  @Override
  public R execute(ActionContext<R> context) throws Exception {
    return super.execute(
        context,
        "",
        () -> {
          final Throwable t;

          synchronized (current) {
            if (current.isEmpty()) {
              throw FailsafeContinueException.INSTANCE; // move on to the next one
            }
            t = current.remove(0);
          }
          ThrowableSupport.sneakyThrow(t.fillInStackTrace());
          throw new InternalError("should not have been reached");
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
              .map(Object::toString)
              .collect(Collectors.joining(",", name + "(", ") at "))
          + getDefinitionInfo();
    }
  }

  @Override
  public String toString() {
    return arguments
        .stream()
        .map(Object::toString)
        .collect(Collectors.joining(",", name + "(", ")"));
  }
}
