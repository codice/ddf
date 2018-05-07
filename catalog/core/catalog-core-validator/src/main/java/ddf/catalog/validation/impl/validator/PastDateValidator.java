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
package ddf.catalog.validation.impl.validator;

import com.google.common.base.Preconditions;
import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Validates an attribute's value(s) against the current date and time, validating that they are in
 * the past.
 *
 * <p>Is capable of validating {@link Date}s.
 */
public class PastDateValidator extends AbstractDateValidator implements AttributeValidator {
  private static final PastDateValidator INSTANCE = new PastDateValidator();

  private PastDateValidator() {}

  public static PastDateValidator getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Validates only the values of {@code attribute} that are {@link Date}s.
   */
  @Override
  public Optional<AttributeValidationReport> validate(final Attribute attribute) {
    Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

    final Date now = Date.from(Instant.now());
    return validate(attribute, (date) -> date.before(now), "must be in the past");
  }
}
