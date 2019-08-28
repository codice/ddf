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

public class OauthPluginException extends RuntimeException {

  private final String sourceId;

  private final String redirectUrl;

  private final ErrorType errorType;

  public enum ErrorType {
    NO_AUTH,
    AUTH_SOURCE
  }

  public OauthPluginException(String sourceId) {
    super();
    this.sourceId = sourceId;
    this.redirectUrl = "";
    this.errorType = ErrorType.AUTH_SOURCE;
  }

  public OauthPluginException(String sourceId, String redirectUrl) {
    super();
    this.sourceId = sourceId;
    this.redirectUrl = redirectUrl;
    this.errorType = ErrorType.NO_AUTH;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public ErrorType getErrorType() {
    return errorType;
  }
}
