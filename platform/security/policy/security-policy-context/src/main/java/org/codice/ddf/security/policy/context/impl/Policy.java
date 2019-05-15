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
package org.codice.ddf.security.policy.context.impl;

import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;

/** Implementation of ContextPolicy for the Policy Manager in this package. */
public class Policy implements ContextPolicy {

  private String contextPath;

  private Collection<String> authenticationMethods;

  private Collection<ContextAttributeMapping> attributeMappings;

  public Policy(
      String contextPath,
      Collection<String> authenticationMethods,
      Collection<ContextAttributeMapping> attributeMappings) {
    this.contextPath = contextPath;
    this.authenticationMethods = authenticationMethods;
    this.attributeMappings = attributeMappings;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public Collection<String> getAuthenticationMethods() {
    return authenticationMethods;
  }

  @Override
  public CollectionPermission getAllowedAttributePermissions() {
    List<KeyValuePermission> perms = new ArrayList<>();
    for (ContextAttributeMapping mapping : attributeMappings) {
      perms.add(mapping.getAttributePermission());
    }
    KeyValueCollectionPermission permissions = new KeyValueCollectionPermission(getContextPath());
    permissions.addAll(perms);
    return permissions;
  }

  @Override
  public Collection<String> getAllowedAttributeNames() {
    List<String> names = new ArrayList<>();
    if (attributeMappings != null && !attributeMappings.isEmpty()) {
      for (ContextAttributeMapping mapping : attributeMappings) {
        names.add(mapping.getAttributeName());
      }
    }
    return names;
  }

  @Override
  public Collection<ContextAttributeMapping> getAllowedAttributes() {
    if (attributeMappings == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(attributeMappings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Context Path: ");
    sb.append(contextPath);
    sb.append(", Authentication Methods: ");
    sb.append(Arrays.toString(authenticationMethods.toArray()));
    sb.append(", AttributeMapping: ");

    for (ContextAttributeMapping attriMap : attributeMappings) {
      sb.append(attriMap.toString());
    }

    return sb.toString();
  }
}
