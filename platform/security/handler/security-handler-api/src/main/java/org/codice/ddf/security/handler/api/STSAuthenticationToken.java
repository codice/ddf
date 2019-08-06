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
package org.codice.ddf.security.handler.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class STSAuthenticationToken extends BaseAuthenticationToken {

  private static final Logger LOGGER = LoggerFactory.getLogger(STSAuthenticationToken.class);

  public STSAuthenticationToken(Object principal, Object credentials, String ip) {
    super(principal, credentials, ip);
  }

  /**
   * Returns the credentials as an XML string suitable for injecting into a STS request. This
   * default behavior assumes that the credentials actually are stored in their XML representation.
   * If a subclass stores them differently, it is up to them to override this method.
   *
   * @return String containing the XML representation of this token's credentials
   */
  @Override
  public String getCredentialsAsString() {
    String retVal = "";
    if (getCredentials() != null) {
      retVal = getCredentials().toString();
    } else {
      LOGGER.debug("Credentials are null - unable to create XML representation.");
    }

    return retVal;
  }
}
