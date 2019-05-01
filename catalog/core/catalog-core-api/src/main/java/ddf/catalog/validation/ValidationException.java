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

import java.util.List;
import org.apache.commons.collections.CollectionUtils;

/**
 * Thrown to indicate that a validation operation could not be completed. Provides information in
 * the form of a summary message, a list of error messages, and a list of warnings.
 *
 * @author Michael Menousek
 * @author Shaun Morris
 * @author Ashraf Barakat
 */
public abstract class ValidationException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Constructs a {@code ValidationException} with no detailed message. */
  public ValidationException() {
    super();
  }

  /**
   * Constructs a {@code ValidationException} with a specified summary message of the failure.
   *
   * @param summaryMessage summarizes why the validation operation failed
   */
  public ValidationException(String summaryMessage) {
    super(summaryMessage);
  }

  /**
   * Constructs a {@code ValidationException} with a cause of the failure.
   *
   * @param cause the cause of why the validation operation failed
   */
  public ValidationException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a {@code ValidationException} with a specified summary message and cause of the
   * failure.
   *
   * @param summaryMessage summarizes why the validation operation failed
   * @param cause the cause of why the validation operation failed
   */
  public ValidationException(String summaryMessage, Throwable cause) {
    super(summaryMessage, cause);
  }

  /**
   * @return a list of all error messages that have caused validation to fail. The error message
   *     should be human-readable plain text.
   */
  public abstract List<String> getErrors();

  /**
   * @return a list of warning messages. Warning messages are issues that arose during validation
   *     but did not cause validation to fail. A warning message should be human-readable plain
   *     text.
   */
  public abstract List<String> getWarnings();

  /**
   * Converts this exception into a String representation.
   *
   * @return a human-readable form of this exception
   */
  @Override
  public String toString() {
    StringBuilder messageBuilder = new StringBuilder(super.toString());

    List<String> errors = getErrors();
    if (CollectionUtils.isNotEmpty(errors)) {
      messageBuilder.append(":ERRORS");
      for (String error : errors) {
        messageBuilder.append(":");
        messageBuilder.append(error);
      }
    }

    List<String> warnings = getWarnings();
    if (CollectionUtils.isNotEmpty(warnings)) {
      messageBuilder.append(":WARNINGS");
      for (String warning : warnings) {
        messageBuilder.append(":");
        messageBuilder.append(warning);
      }
    }

    return messageBuilder.toString();
  }
}
