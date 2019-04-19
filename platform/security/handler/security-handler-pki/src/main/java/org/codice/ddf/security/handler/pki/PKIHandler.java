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
package org.codice.ddf.security.handler.pki;

import java.security.cert.X509Certificate;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;

/**
 * Handler for PKI based authentication. X509 chain will be extracted from the HTTP request and
 * converted to a BinarySecurityToken.
 */
public class PKIHandler extends AbstractPKIHandler {
  /** PKI type to use when configuring context policy. */
  private static final String AUTH_TYPE = "PKI";

  public PKIHandler() {
    super();
    LOGGER.debug("Creating PKI handler.");
  }

  @Override
  protected BaseAuthenticationToken extractAuthenticationInfo(X509Certificate[] certs) {
    return tokenFactory.getTokenFromCerts(certs);
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }
}
