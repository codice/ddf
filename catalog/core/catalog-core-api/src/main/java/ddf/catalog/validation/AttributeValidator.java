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
package ddf.catalog.validation;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.report.AttributeValidationReport;
import java.util.Optional;

/**
 * A service that validates a single {@link ddf.catalog.data.Metacard} attribute and provides
 * information if problems exist with the attribute's value.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface AttributeValidator {
  /**
   * Validates a single {@link Attribute}.
   *
   * @param attribute the {@link Attribute} to validate, cannot be null
   * @return an {@link Optional} containing an {@link AttributeValidationReport} if there are
   *     violations, or an empty {@link Optional} if there are no violations
   * @throws IllegalArgumentException if {@code attribute} is null
   */
  Optional<AttributeValidationReport> validate(Attribute attribute);
}
