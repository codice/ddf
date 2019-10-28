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
package ddf.security.claims.impl;

import ddf.security.claims.ClaimsParameters;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

public class ClaimsParametersImpl implements ClaimsParameters {

  private Principal principal;

  private Set<Principal> roles;

  private Map<String, Object> additionalProperties;

  public ClaimsParametersImpl(
      Principal principal, Set<Principal> roles, Map<String, Object> additionalProperties) {
    this.principal = principal;
    this.roles = roles;
    this.additionalProperties = additionalProperties;
  }

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  @Override
  public Set<Principal> getRoles() {
    return roles;
  }

  @Override
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }
}
