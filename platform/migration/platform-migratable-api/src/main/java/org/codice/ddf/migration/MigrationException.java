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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Exception that indicates some problem with the migration operation.
 *
 * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
 * migration operation.
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public class MigrationException extends RuntimeException implements MigrationMessage {
  private static final long serialVersionUID = -1023839569683504743L;

  /**
   * Constructs a new migration exception with the specified detail message.
   *
   * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
   * migration operation.
   *
   * @param message the detail message for this exception
   * @throws IllegalArgumentException if <code>message</code> is <code>null</code>
   */
  public MigrationException(String message) {
    super(message);
    Validate.notNull(message, "invalid null message");
  }

  /**
   * Constructs a new migration exception with the specified detail message to be formatted with the
   * specified parameters.
   *
   * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
   * migration operation. All {@link Throwable} arguments are formatted using their {@link
   * Throwable#getMessage()} representation.
   *
   * @param format the format string for the detail message for this exception (see {@link
   *     String#format})
   * @param args the arguments to the format message (if the last argument provided is a {@link
   *     Throwable} it will automatically be initialized as the cause for the exception)
   * @throws IllegalArgumentException if <code>format</code> is <code>null</code>
   */
  public MigrationException(String format, @Nullable Object... args) {
    super(
        String.format(
            MigrationException.validateNotNull(format, "invalid null format message"),
            MigrationException.sanitizeThrowables(args)));
    if ((args != null) && (args.length > 0)) {
      final Object a = args[args.length - 1];

      if (a instanceof Throwable) {
        initCause((Throwable) a);
      }
    }
  }

  /**
   * Constructs a new migration exception with the specified detail message and cause.
   *
   * <p><i>Note:</i> Detail messages are displayed to the administrator on the console during a
   * migration operation.
   *
   * @param message the detail message for this exception
   * @param cause the cause for this exception
   * @throws IllegalArgumentException if <code>message</code> is <code>null</code>
   */
  @SuppressWarnings(
      "squid:S1905" /* cast required to ensure the array received is null and not an array with a null element */)
  public MigrationException(String message, @Nullable Throwable cause) {
    // in case they were using a format with only a Throwable - leave this ctor as people are
    // familiar with it
    this(
        MigrationException.validateNotNull(message, "invalid null message"),
        (cause != null) ? new Object[] {cause} : (Object[]) null);
  }

  protected MigrationException(MigrationException error) {
    super(error.getMessage(), error);
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from MigrationWarning and MigrationInformation within this package */)
  static Object[] sanitizeThrowables(Object[] args) {
    if ((args == null) || (args.length == 0)) {
      return args;
    }
    final Object[] sargs = new Object[args.length];

    System.arraycopy(args, 0, sargs, 0, args.length);
    for (int i = 0; i < sargs.length; i++) {
      if (sargs[i] instanceof Throwable) {
        // make sure the message doesn't end with a period
        sargs[i] = StringUtils.removeEnd(((Throwable) sargs[i]).getMessage(), ".");
      }
    }
    return sargs;
  }

  private static <T> T validateNotNull(T t, String message) {
    Validate.notNull(t, message);
    return t;
  }
}
