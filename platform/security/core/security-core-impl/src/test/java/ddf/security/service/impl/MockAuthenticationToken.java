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
package ddf.security.service.impl;

import org.apache.shiro.authc.AuthenticationToken;

/** Mock Authentication Token used for testing out the security manager */
public class MockAuthenticationToken implements AuthenticationToken {

  private static final long serialVersionUID = 1L;

  @Override
  public Object getPrincipal() {
    return null;
  }

  @Override
  public Object getCredentials() {
    return null;
  }
}
