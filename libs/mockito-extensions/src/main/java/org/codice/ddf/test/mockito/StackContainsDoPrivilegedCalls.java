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
package org.codice.ddf.test.mockito;

import java.security.AccessController;
import java.util.Arrays;
import org.hamcrest.CustomMatcher;
import org.mockito.Incubating;

/**
 * Hamcrest {@link org.hamcrest.CustomMatcher} that can be used to determine a stack contains a
 * certain number of {@code AccessController.doPrivileged()} calls.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
@Incubating
public class StackContainsDoPrivilegedCalls extends CustomMatcher<StackTraceElement[]> {

  private final int times;

  /**
   * Constructor that takes in a number of excepted {@code AccessController.doPrivileged()} calls.
   *
   * @param times excepted number of {@code AccessController.doPrivileged()} calls
   */
  public StackContainsDoPrivilegedCalls(int times) {
    super("Stack contains call to AccessController.doPrivileged");
    this.times = times;
  }

  /**
   * Factory method that returns an instance of {@link StackContainsDoPrivilegedCalls} that expects
   * only one {@code AccessController.doPrivileged()} call on the stack.
   *
   * @return {@link StackContainsDoPrivilegedCalls} custom Hamcrest matcher
   */
  public static StackContainsDoPrivilegedCalls stackContainsDoPrivilegedCall() {
    return new StackContainsDoPrivilegedCalls(1);
  }

  /**
   * Factory method that returns an instance of {@link StackContainsDoPrivilegedCalls} that expects
   * a specific number of {@code AccessController.doPrivileged()} calls on the stack.
   *
   * @return {@link StackContainsDoPrivilegedCalls} custom Hamcrest matcher
   */
  public static StackContainsDoPrivilegedCalls stackContainsDoPrivilegedCalls(int times) {
    return new StackContainsDoPrivilegedCalls(times);
  }

  @Override
  public boolean matches(Object o) {
    return (o instanceof StackTraceElement[])
        && Arrays.stream((StackTraceElement[]) o).filter(this::isDoPrivilegedCall).count() == times;
  }

  private boolean isDoPrivilegedCall(StackTraceElement e) {
    return e.getClassName().equals(AccessController.class.getName())
        && e.getMethodName().startsWith("doPrivileged");
  }
}
