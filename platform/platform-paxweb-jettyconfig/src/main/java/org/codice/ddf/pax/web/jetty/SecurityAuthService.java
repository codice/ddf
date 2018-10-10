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
package org.codice.ddf.pax.web.jetty;

import javax.annotation.Nullable;
import org.eclipse.jetty.security.Authenticator;
import org.ops4j.pax.web.service.AuthenticatorService;

public class SecurityAuthService implements AuthenticatorService {

  @Override
  @Nullable
  public <T> T getAuthenticatorService(String method, Class<T> iface) {
    if (JettyAuthenticator.DDF_AUTH_METHOD.equals(method) && iface.equals(Authenticator.class)) {
      return iface.cast(new JettyAuthenticator());
    }
    return null;
  }
}
