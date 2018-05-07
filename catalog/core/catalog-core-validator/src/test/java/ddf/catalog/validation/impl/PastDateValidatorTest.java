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
package ddf.catalog.validation.impl;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.PastDateValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.Test;

public class PastDateValidatorTest {
  private static final PastDateValidator VALIDATOR = PastDateValidator.getInstance();

  @Test
  public void testValidValue() {
    final Instant pastInstant = Instant.now().minus(5, MINUTES);
    validateNoErrors(pastInstant);
  }

  @Test
  public void testInvalidValue() {
    final Instant futureInstant = Instant.now().plus(5, MINUTES);
    validateWithErrors(futureInstant, 1);
  }

  private void validateNoErrors(final Instant instant) {
    final Optional<AttributeValidationReport> reportOptional =
        VALIDATOR.validate(new AttributeImpl("test", Date.from(instant)));
    assertThat(reportOptional.isPresent(), is(false));
  }

  private void validateWithErrors(final Instant instant, final int expectedErrors) {
    final Optional<AttributeValidationReport> reportOptional =
        VALIDATOR.validate(new AttributeImpl("test", Date.from(instant)));
    assertThat(reportOptional.get().getAttributeValidationViolations(), hasSize(expectedErrors));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullAttribute() {
    VALIDATOR.validate(null);
  }
}
