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
package org.codice.ddf.cxf.client.impl;

import ddf.security.audit.SecurityLogger;
import org.codice.ddf.cxf.client.ClientBuilder;
import org.codice.ddf.cxf.client.ClientBuilderFactory;
import org.codice.ddf.cxf.oauth.OAuthSecurity;
import org.codice.ddf.security.jaxrs.SamlSecurity;

public class ClientBuilderFactoryImpl implements ClientBuilderFactory {

  private OAuthSecurity oauthSecurity;

  private SamlSecurity samlSecurity;

  private SecurityLogger securityLogger;

  @Override
  public <T> ClientBuilder<T> getClientBuilder() {
    return new ClientBuilderImpl<>(oauthSecurity, samlSecurity, securityLogger);
  }

  public void setOauthSecurity(OAuthSecurity oauthSecurity) {
    this.oauthSecurity = oauthSecurity;
  }

  public void setSamlSecurity(SamlSecurity samlSecurity) {
    this.samlSecurity = samlSecurity;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
