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
package org.codice.ddf.spatial.ogc.wps.process.api;

import java.net.HttpURLConnection;

public class WpsException extends RuntimeException {
  private final String message;

  private final String exceptionCode;

  private final String locator;

  private final int httpStatus;

  public WpsException(String message, String exceptionCode, String locator) {
    this(message, exceptionCode, locator, HttpURLConnection.HTTP_BAD_REQUEST);
  }

  public WpsException(String message, String exceptionCode, String locator, int httpStatus) {
    this.message = message;
    this.exceptionCode = exceptionCode;
    this.locator = locator;
    this.httpStatus = httpStatus;
  }

  public String getExceptionCode() {
    return exceptionCode;
  }

  public String getLocator() {
    return locator;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
