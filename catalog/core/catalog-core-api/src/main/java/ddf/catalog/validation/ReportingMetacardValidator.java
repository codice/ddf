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

import ddf.catalog.data.Metacard;
import ddf.catalog.validation.report.MetacardValidationReport;
import java.util.Optional;

/**
 * Validates a {@link Metacard} and provides information via a report if problems exist with the
 * {@link Metacard}'s data.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface ReportingMetacardValidator {
  /**
   * Validates a {@link Metacard}.
   *
   * @param metacard the {@link Metacard} to validate, cannot be null
   * @return an {@link Optional} containing a {@link MetacardValidationReport} if there are
   *     violations, or an empty {@link Optional} if there are no violations
   * @throws IllegalArgumentException if {@code metacard} is null
   */
  Optional<MetacardValidationReport> validateMetacard(Metacard metacard);
}
