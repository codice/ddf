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
package org.codice.ddf.test.common.mockito;

import static org.mockito.Mockito.times;

import com.google.common.annotations.Beta;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.mockito.exceptions.base.MockitoAssertionError;
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
@Beta
public class PrivilegedVerificationMode implements VerificationMode, VerificationInOrderMode {

  private final VerificationMode mode;

  private PrivilegedVerificationMode(VerificationMode mode) {
    this.mode = mode;
  }

  /**
   * Wraps another {@link VerificationMode} instance and ensures all the expected calls were made
   * inside a {@code AccessController.doPrivileged()} block.
   *
   * @param mode {@link VerificationMode} instance to wrap with a privileged check
   * @return wrapped {@link VerificationMode} instance
   */
  public static VerificationMode privileged(VerificationMode mode) {
    return new PrivilegedVerificationMode(mode);
  }

  /**
   * Returns a {@link VerificationMode} object that ensures a single expected calls was made inside
   * a {@code AccessController.doPrivileged()} block.
   *
   * @return wrapped {@link VerificationMode} instance
   */
  public static VerificationMode privileged() {
    return new PrivilegedVerificationMode(times(1));
  }

  @Override
  public void verify(VerificationData verificationData) {
    String target = verificationData.getTarget().toString();

    mode.verify(verificationData);

    verify(verificationData.getTarget(), verificationData.getAllInvocations());
  }

  @Override
  public void verifyInOrder(VerificationDataInOrder verificationDataInOrder) {
    String target = verificationDataInOrder.getWanted().toString();

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

    if (invocations.size() == 0) {
      return;
    }

    try {
      Location location = invocations.get(0).getLocation();
      Field stackTraceHolderField = getStackTraceHolderField(location);

      for (Invocation invocation : invocations) {
        if (wanted.matches(invocation)) {
          if (getStackTraceElements(stackTraceHolderField, invocation)
              .noneMatch(this::isDoPrivilegedCall)) {
            throw new MockitoAssertionError(
                wanted.toString() + " not called in a doPrivileged block");
          }
        }
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new MockitoAssertionError(
          "Failed to verify that "
              + wanted.toString()
              + " was called in a doPrivileged block, most likely because Mockito's"
              + " VerificationData implementation has changed.");
    }
  }

  private Stream<StackTraceElement> getStackTraceElements(
      Field stackTraceHolderField, Invocation invocation) throws IllegalAccessException {
    return Arrays.stream(
        ((Throwable) stackTraceHolderField.get(invocation.getLocation())).getStackTrace());
  }

  private Field getStackTraceHolderField(Location location) throws NoSuchFieldException {
    Field stackTraceHolderField = location.getClass().getDeclaredField("stackTraceHolder");
    stackTraceHolderField.setAccessible(true);
    return stackTraceHolderField;
  }

  private boolean isDoPrivilegedCall(StackTraceElement e) {
    return e.getClassName().equals(AccessController.class.getName())
        && e.getMethodName().startsWith("doPrivileged");
  }
}
