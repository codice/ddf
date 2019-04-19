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

import org.apache.shiro.authc.AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseAuthenticationToken implements AuthenticationToken {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseAuthenticationToken.class);

  private boolean useWssSts = false;

  /**
   * Represents the account identity submitted during the authentication process.
   *
   * <p>
   *
   * <p>Most application authentications are username/password based and have this object represent
   * a username. However, this can also represent the DN from an X509 certificate, or any other
   * unique identifier.
   *
   * <p>
   *
   * <p>Ultimately, the object is application specific and can represent any account identity (user
   * id, X.509 certificate, etc).
   */
  protected Object principal;

  /**
   * Represents the credentials submitted by the user during the authentication process that
   * verifies the submitted Principal account identity.
   *
   * <p>
   *
   * <p>Most application authentications are username/password based and have this object represent
   * a submitted password.
   *
   * <p>
   *
   * <p>Ultimately, the credentials Object is application specific and can represent any credential
   * mechanism.
   */
  protected Object credentials;

  public BaseAuthenticationToken(Object principal, Object credentials) {
    this.principal = principal;
    this.credentials = credentials;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  protected void setCredentials(Object o) {
    this.credentials = o;
  }

  public boolean isUseWssSts() {
    return useWssSts;
  }

  public void setUseWssSts(boolean useWssSts) {
    this.useWssSts = useWssSts;
  }

  /**
   * Returns the credentials as an XML string suitable for injecting into a STS request. This
   * default behavior assumes that the credentials actually are stored in their XML representation.
   * If a subclass stores them differently, it is up to them to override this method.
   *
   * @return String containing the XML representation of this token's credentials
   */
  public String getCredentialsAsXMLString() {
    String retVal = "";
    if (getCredentials() != null) {
      retVal = getCredentials().toString();
    } else {
      LOGGER.debug("Credentials are null - unable to create XML representation.");
    }

    return retVal;
  }
}
