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
package ddf.catalog.validation.violation;

import java.util.Map;

/**
 * Describes a violation of a constraint on query.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface QueryValidationViolation {
  /**
   * Describes the severity of a validation violation. An error is a violation severe enough to
   * cause validation to fail, and a warning is a less severe violation unrelated to validation
   * failure.
   */
  enum Severity {
    WARNING,
    ERROR
  }

  /**
   * Returns the severity of the violation. Cannot return null.
   *
   * @return the severity
   */
  Severity getSeverity();

  /**
   * Returns a message describing the violation. Cannot return null.
   *
   * @return the message
   */
  String getMessage();

  /**
   * Returns supplemental information about the violation. This is structured data that is more
   * easily parsable than the message. Cannot return null.
   *
   * @return a map of supplemental information
   */
  Map<String, Object> getExtraData();
}
