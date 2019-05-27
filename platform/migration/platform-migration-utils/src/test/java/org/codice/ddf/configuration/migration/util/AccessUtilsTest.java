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
package org.codice.ddf.configuration.migration.util;

import java.io.IOException;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.codice.ddf.util.function.ThrowingSupplier;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class AccessUtilsTest {

  private static final String RESULT = "test.result";

  private static final IOException IO_EXCEPTION = new IOException("testing");

  private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("testing");

  private static final Error ERROR = new Error("testing");

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testDoPrivilegedSupplierWithNullAction() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doPrivileged((ThrowingSupplier) null);
  }

  @Test
  public void testDoPrivilegedSupplier() throws Exception {
    final String result = AccessUtils.doPrivileged(() -> RESULT);

    Assert.assertThat(result, Matchers.equalTo(RESULT));
  }

  @Test
  public void testDoPrivilegedSupplierThrowingException() throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Object, Exception>doPrivileged(
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoPrivilegedSupplierThrowingRuntimeException() throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Object, Exception>doPrivileged(
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoPrivilegedSupplierThrowingError() throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Object, Exception>doPrivileged(
        () -> {
          throw ERROR;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierWithNullActionAndTrueCondition()
      throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doConditionallyPrivileged(true, (ThrowingSupplier) null);
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierAndTrueCondition() throws Exception {
    final String result = AccessUtils.doConditionallyPrivileged(true, () -> RESULT);

    Assert.assertThat(result, Matchers.equalTo(RESULT));
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingExceptionAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingRuntimeExceptionAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingErrorAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw ERROR;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierWithNullActionAndFalseCondition()
      throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doConditionallyPrivileged(false, (ThrowingSupplier) null);
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierAndFalseCondition() throws Exception {
    final String result = AccessUtils.doConditionallyPrivileged(false, () -> RESULT);

    Assert.assertThat(result, Matchers.equalTo(RESULT));
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingExceptionAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingRuntimeExceptionAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedSupplierThrowingErrorAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Object, Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw ERROR;
        });
  }

  @Test
  public void testDoPrivilegedRunnableWithNullAction() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doPrivileged((ThrowingRunnable) null);
  }

  @Test
  public void testDoPrivilegedRunnable() throws Exception {
    final ThrowingRunnable<Exception> ACTION = Mockito.mock(ThrowingRunnable.class);

    AccessUtils.doPrivileged(ACTION);

    Mockito.verify(ACTION).run();
  }

  @Test
  public void testDoPrivilegedRunnableThrowingException() throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Exception>doPrivileged(
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoPrivilegedRunnableThrowingRuntimeException() throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Exception>doPrivileged(
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoPrivilegedRunnableThrowingError() throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Exception>doPrivileged(
        () -> {
          throw ERROR;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableWithNullActionAndTrueCondition()
      throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doConditionallyPrivileged(true, (ThrowingRunnable) null);
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableAndTrueCondition() throws Exception {
    final ThrowingRunnable<Exception> ACTION = Mockito.mock(ThrowingRunnable.class);

    AccessUtils.doConditionallyPrivileged(true, ACTION);

    Mockito.verify(ACTION).run();
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingExceptionAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingRuntimeExceptionAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingErrorAndTrueCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Exception>doConditionallyPrivileged(
        true,
        () -> {
          throw ERROR;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableWithNullActionAndFalseCondition()
      throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null action"));

    AccessUtils.doConditionallyPrivileged(false, (ThrowingRunnable) null);
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableAndFalseCondition() throws Exception {
    final ThrowingRunnable<Exception> ACTION = Mockito.mock(ThrowingRunnable.class);

    AccessUtils.doConditionallyPrivileged(false, ACTION);

    Mockito.verify(ACTION).run();
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingExceptionAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(IO_EXCEPTION));

    AccessUtils.<Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw IO_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingRuntimeExceptionAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(RUNTIME_EXCEPTION));

    AccessUtils.<Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw RUNTIME_EXCEPTION;
        });
  }

  @Test
  public void testDoConditionallyPrivilegedRunnableThrowingErrorAndFalseCondition()
      throws Exception {
    thrown.expect(Matchers.sameInstance(ERROR));

    AccessUtils.<Exception>doConditionallyPrivileged(
        false,
        () -> {
          throw ERROR;
        });
  }
}
