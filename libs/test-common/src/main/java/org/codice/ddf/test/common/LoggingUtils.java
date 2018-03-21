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
package org.codice.ddf.test.common;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Logging utilities for testing. */
public class LoggingUtils {
  private LoggingUtils() {
    // Static utility class
  }

  /**
   * Appends the stacktrace of the {@link Throwable} to the {@code message} provided. It is the
   * responsibility of the caller to end their message with a space or newline.
   *
   * @param throwable contains the stacktrace to be logged
   * @param message a descriptive message for the stacktrace
   */
  public static void failWithThrowableStacktrace(Throwable throwable, String message) {
    StringWriter errors = new StringWriter();
    throwable.printStackTrace(new PrintWriter(errors));
    fail(message + errors.toString());
  }
}
