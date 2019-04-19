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
package org.codice.ddf.security.handler.basic;

import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;

/**
 * Checks for basic authentication credentials in the http request header. If they exist, they are
 * retrieved and returned in the HandlerResult.
 */
public class BasicAuthenticationHandler extends AbstractBasicAuthenticationHandler {
  /** Basic type to use when configuring context policy. */
  private static final String AUTH_TYPE = "BASIC";

  public BasicAuthenticationHandler() {
    LOGGER.debug("Creating basic username/token bst handler.");
  }

  protected BaseAuthenticationToken getBaseAuthenticationToken(String username, String password) {
    return new UPAuthenticationToken(username, password);
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }
}
