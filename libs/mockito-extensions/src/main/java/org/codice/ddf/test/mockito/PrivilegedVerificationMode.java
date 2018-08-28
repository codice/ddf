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

import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.mockito.Incubating;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.internal.verification.api.VerificationDataInOrder;
import org.mockito.internal.verification.api.VerificationInOrderMode;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.Location;
import org.mockito.invocation.MatchableInvocation;
import org.mockito.verification.VerificationMode;

/**
 * Class that extends Mockito's {@link VerificationMode} to verify that a mocked method has been
 * called within a {@code AccessController#doPrivileged()} block.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
@Incubating
public class PrivilegedVerificationMode implements VerificationMode, VerificationInOrderMode {

  private final VerificationMode mode;

  private PrivilegedVerificationMode(VerificationMode mode) {
    this.mode = mode;
  }

  /**
   * Wraps another {@link VerificationMode} instance and ensures all {@code verify()} calls from the
   * specified mode were made inside a {@code AccessController.doPrivileged()} block.
   *
   * @param mode {@link VerificationMode} instance to wrap with a privileged check
   * @return wrapped {@link VerificationMode} instance
   */
  public static VerificationMode privileged(VerificationMode mode) {
    return new PrivilegedVerificationMode(mode);
  }

  /**
   * Returns a {@link VerificationMode} object that ensures a single {@code verify} code was made
   * inside a {@code AccessController.doPrivileged()} block.
   *
   * @return wrapped {@link VerificationMode} instance
   */
  public static VerificationMode privileged() {
    return new PrivilegedVerificationMode(times(1));
  }

  @Override
  public void verify(VerificationData verificationData) {
    mode.verify(verificationData);

    verify(verificationData.getTarget(), verificationData.getAllInvocations());
  }

  @Override
  public void verifyInOrder(VerificationDataInOrder verificationDataInOrder) {
    if (!(mode instanceof VerificationInOrderMode)) {
      throw new MockitoAssertionError(mode.toString() + " cannot be used in ordered mode");
    }

    ((VerificationInOrderMode) mode).verifyInOrder(verificationDataInOrder);

    verify(verificationDataInOrder.getWanted(), verificationDataInOrder.getAllInvocations());
  }

  @Override
  public VerificationMode description(String description) {
    return VerificationModeFactory.description(this, description);
  }

  private void verify(MatchableInvocation wanted, List<Invocation> invocations) {

    if (invocations.isEmpty()) {
      return;
    }

    Location location = invocations.get(0).getLocation();
    Field stackTraceHolderField = getStackTraceHolderField(wanted, location);

    if (invocations
        .stream()
        .filter(wanted::matches)
        .flatMap(i -> getStackTraceElements(stackTraceHolderField, wanted, i))
        .noneMatch(this::isDoPrivilegedCall)) {
      throw new MockitoAssertionError(wanted + " not called in a doPrivileged block");
    }
  }

  private Stream<StackTraceElement> getStackTraceElements(
      Field stackTraceHolderField, MatchableInvocation wanted, Invocation invocation) {
    try {
      return Arrays.stream(
          ((Throwable) stackTraceHolderField.get(invocation.getLocation())).getStackTrace());
    } catch (IllegalAccessException e) {
      throw getMockitoImplementationChangedException(wanted);
    }
  }

  private Field getStackTraceHolderField(MatchableInvocation wanted, Location location) {
    if (!(location instanceof LocationImpl)) {
      throw getMockitoImplementationChangedException(wanted);
    }

    try {
      Field stackTraceHolderField = location.getClass().getDeclaredField("stackTraceHolder");
      stackTraceHolderField.setAccessible(true);
      return stackTraceHolderField;
    } catch (NoSuchFieldException e) {
      throw getMockitoImplementationChangedException(wanted);
    }
  }

  private boolean isDoPrivilegedCall(StackTraceElement e) {
    return e.getClassName().equals(AccessController.class.getName())
        && e.getMethodName().startsWith("doPrivileged");
  }

  private MockitoAssertionError getMockitoImplementationChangedException(
      MatchableInvocation wanted) {
    return new MockitoAssertionError(
        "Failed to verify that "
            + wanted
            + " was called in a doPrivileged block, most likely because Mockito's"
            + " internal implementation has changed.");
  }
}
