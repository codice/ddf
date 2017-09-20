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

import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;

/**
 * Class that provides warning statuses for migration operations.
 *
 * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
 * migration operation.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public class MigrationWarning implements MigrationMessage {
  private final String message;

  /**
   * Constructs a new migration warning with the specified detail message.
   *
   * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
   * migration operation.
   *
   * @param message the detail message for this warning
   * @throws IllegalArgumentException if <code>message</code> is <code>null</code>
   */
  public MigrationWarning(String message) {
    Validate.notNull(message, "invalid null message");
    this.message = message;
  }

  /**
   * Constructs a new migration warning with the specified detail message to be formatted with the
   * specified parameters.
   *
   * @param format the format string for the detail message for this warning (see {@link
   *     String#format})
   * @param args the arguments to the format message
   * @throws IllegalArgumentException if <code>format</code> is <code>null</code>
   */
  public MigrationWarning(String format, @Nullable Object... args) {
    Validate.notNull(format, "invalid null format message");
    this.message = String.format(format, MigrationException.sanitizeThrowables(args));
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    return message;
  }
}
