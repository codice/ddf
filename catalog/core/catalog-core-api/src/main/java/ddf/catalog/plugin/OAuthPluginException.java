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
package ddf.catalog.plugin;

/** Exception thrown when an user doesn't have the appropriate OAuth tokens to federate */
public class OAuthPluginException extends RuntimeException {

  private final String sourceId;

  private final ErrorType errorType;

  private final String url;

  public enum ErrorType {
    NO_AUTH(401),
    AUTH_SOURCE(412);

    private int statusCode;

    ErrorType(int statusCode) {
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }

  public OAuthPluginException(String sourceId, String url, ErrorType errorType) {
    super();
    this.sourceId = sourceId;
    this.url = url;
    this.errorType = errorType;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getUrl() {
    return url;
  }

  public ErrorType getErrorType() {
    return errorType;
  }
}
