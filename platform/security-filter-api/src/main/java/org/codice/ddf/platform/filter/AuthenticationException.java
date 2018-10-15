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
package org.codice.ddf.platform.filter;

public class AuthenticationException extends Exception {
  private Throwable rootCause;

  public AuthenticationException() {}

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable rootCause) {
    super(message, rootCause);
    this.rootCause = rootCause;
  }

  public AuthenticationException(Throwable rootCause) {
    super(rootCause);
    this.rootCause = rootCause;
  }

  public Throwable getRootCause() {
    return this.rootCause;
  }
}
