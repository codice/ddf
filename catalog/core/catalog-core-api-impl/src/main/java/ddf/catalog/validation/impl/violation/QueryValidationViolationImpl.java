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
package ddf.catalog.validation.impl.violation;

import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Map;
import java.util.Objects;

public class QueryValidationViolationImpl implements QueryValidationViolation {

  private Severity severity;

  private String message;

  private Map<String, Object> extraData;

  public QueryValidationViolationImpl(
      final Severity severity, final String message, final Map<String, Object> extraData) {
    this.severity = severity;
    this.message = message;
    this.extraData = extraData;
  }

  @Override
  public Severity getSeverity() {
    return severity;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Map<String, Object> getExtraData() {
    return extraData;
  }

  @Override
  public String toString() {
    return "QueryValidationViolationImpl{"
        + "severity="
        + severity
        + ", message='"
        + message
        + '\''
        + ", extraData="
        + extraData
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryValidationViolationImpl that = (QueryValidationViolationImpl) o;
    return severity == that.severity
        && Objects.equals(message, that.message)
        && Objects.equals(extraData, that.extraData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severity, message, extraData);
  }
}
