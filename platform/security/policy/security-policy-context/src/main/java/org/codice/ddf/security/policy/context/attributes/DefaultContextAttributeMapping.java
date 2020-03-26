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
package org.codice.ddf.security.policy.context.attributes;

import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import java.util.Arrays;

/** Default implementation of ContextAttributeMapping */
public class DefaultContextAttributeMapping implements ContextAttributeMapping {

  private String attributeName;

  private String attributeValue;

  private String context;

  private KeyValuePermission keyValuePermission;

  public DefaultContextAttributeMapping(
      String context, String attributeName, String attributeValue) {
    this.context = context;
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  @Override
  public String getAttributeName() {
    return attributeName;
  }

  @Override
  public String getAttributeValue() {
    return attributeValue;
  }

  @Override
  public KeyValuePermission getAttributePermission() {
    if (keyValuePermission == null) {
      keyValuePermission = new KeyValuePermissionImpl(attributeName, Arrays.asList(attributeValue));
    }
    return keyValuePermission;
  }

  @Override
  public String getContext() {
    return context;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Context: ");
    sb.append(context);
    sb.append(", Attribute Name: ");
    sb.append(attributeName);
    sb.append(", Attribute Value: ");
    sb.append(attributeValue);
    return sb.toString();
  }
}
