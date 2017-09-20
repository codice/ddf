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
package org.codice.ddf.migration;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MigrationExceptionTest {
  private static final String FORMAT = "test-%s";

  private static final String ARG = "message";

  private static final String MESSAGE = "test-message";

  private static final Throwable CAUSE = new Exception("test-cause");

  private final MigrationException exception = new MigrationException(MESSAGE);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructorWithMessage() throws Exception {
    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.nullValue());
  }

  @Test
  public void testConstructorWithNullMessage() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null message"));

    new MigrationException((String) null);
  }

  @Test
  public void testConstructorWithFormat() throws Exception {
    final MigrationException exception = new MigrationException(FORMAT, ARG);

    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.nullValue());
  }

  @Test
  public void testConstructorWithFormatAndNoArgs() throws Exception {
    final MigrationException exception = new MigrationException(MESSAGE, (Object[]) null);

    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.nullValue());
  }

  @Test
  public void testConstructorWithFormatAndCause() throws Exception {
    final MigrationException exception = new MigrationException(FORMAT + ": %s", ARG, CAUSE);

    Assert.assertThat(
        exception.getMessage(), Matchers.equalTo(MESSAGE + ": " + CAUSE.getMessage()));
    Assert.assertThat(exception.getCause(), Matchers.sameInstance(CAUSE));
  }

  @Test
  public void testConstructorWithFormatAndCauseButNoSpecifierInFormatForIt() throws Exception {
    final MigrationException exception = new MigrationException(FORMAT, ARG, CAUSE);

    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.sameInstance(CAUSE));
  }

  @Test
  public void testConstructorWithNullFormat() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null format message"));

    new MigrationException(null, ARG);
  }

  @Test
  public void testConstructorWithMessageAndCause() throws Exception {
    final MigrationException exception = new MigrationException(MESSAGE, CAUSE);

    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.sameInstance(CAUSE));
  }

  @Test
  public void testConstructorWithCauseAndNullMessage() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null message"));

    new MigrationException(null, CAUSE);
  }

  @Test
  public void testConstructorWithMessageAndNullCause() throws Exception {
    final MigrationException exception = new MigrationException(MESSAGE, (Throwable) null);

    Assert.assertThat(exception.getMessage(), Matchers.equalTo(MESSAGE));
    Assert.assertThat(exception.getCause(), Matchers.nullValue());
  }
}
