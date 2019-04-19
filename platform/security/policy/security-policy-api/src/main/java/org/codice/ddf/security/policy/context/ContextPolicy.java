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
package org.codice.ddf.security.policy.context;

import ddf.security.permission.CollectionPermission;
import java.util.Collection;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;

/** Represents the policy for a given web context. */
public interface ContextPolicy {

  /**
   * Setting that can be used in a header or attribute to signify that the policy on that current
   * message does not require authentication.
   */
  String NO_AUTH_POLICY = "org.codice.ddf.security.policy.no_authentication";

  /**
   * Returns the context path that this policy covers.
   *
   * @return context path
   */
  String getContextPath();

  /**
   * Returns a {@link java.util.Collection} of authentication methods
   *
   * @return authentication methods
   */
  Collection<String> getAuthenticationMethods();

  /**
   * Returns a {@link ddf.security.permission.CollectionPermission} object built from the attribute
   * mappings.
   *
   * @return permissions
   */
  CollectionPermission getAllowedAttributePermissions();

  /**
   * Returns a {@link java.util.Collection} of attribute names.
   *
   * @return all attribute names used in this policy
   */
  Collection<String> getAllowedAttributeNames();

  /**
   * Returns a{@link java.util.Collection} of attributes
   *
   * @return all attributes the policy uses
   */
  Collection<ContextAttributeMapping> getAllowedAttributes();
}
