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
package org.codice.ddf.catalog.security;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.util.impl.Requests;
import ddf.security.permission.impl.Permissions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IngestPlugin is a PreIngestPlugin that restricts the create/update/delete operations on the
 * catalog to a group defined by a set of configurable user attributes.
 */
public class CatalogPolicy implements PolicyPlugin {

  private String[] createPermissions;

  private String[] updatePermissions;

  private String[] deletePermissions;

  private String[] readPermissions;

  private Map<String, Set<String>> createPermissionMap = new HashMap<>();

  private Map<String, Set<String>> updatePermissionMap = new HashMap<>();

  private Map<String, Set<String>> deletePermissionMap = new HashMap<>();

  private Map<String, Set<String>> readPermissionMap = new HashMap<>();

  /**
   * Getter used by the framework to populate the configuration ui
   *
   * @return
   */
  public String[] getCreatePermissions() {
    if (createPermissions != null) {
      return Arrays.copyOf(this.createPermissions, createPermissions.length);
    }
    return null;
  }

  /**
   * Getter used by the framework to populate the configuration ui
   *
   * @return
   */
  public String[] getUpdatePermissions() {
    if (updatePermissions != null) {
      return Arrays.copyOf(this.updatePermissions, updatePermissions.length);
    }
    return null;
  }

  /**
   * Getter used by the framework to populate the configuration ui
   *
   * @return
   */
  public String[] getDeletePermissions() {
    if (deletePermissions != null) {
      return Arrays.copyOf(this.deletePermissions, deletePermissions.length);
    }
    return null;
  }

  /**
   * Getter used by the framework to populate the configuration ui
   *
   * @return
   */
  public String[] getReadPermissions() {
    if (readPermissions != null) {
      return Arrays.copyOf(this.readPermissions, readPermissions.length);
    }
    return null;
  }

  /**
   * Get the KeyValuePermission that have been parsed from the permissions string
   *
   * @return
   */
  public Map<String, Set<String>> getCreatePermissionMap() {
    return Collections.unmodifiableMap(createPermissionMap);
  }

  /**
   * Get the KeyValuePermission that have been parsed from the permissions string
   *
   * @return
   */
  public Map<String, Set<String>> getUpdatePermissionMap() {
    return Collections.unmodifiableMap(updatePermissionMap);
  }

  /**
   * Get the KeyValuePermission that have been parsed from the permissions string
   *
   * @return
   */
  public Map<String, Set<String>> getDeletePermissionMap() {
    return Collections.unmodifiableMap(deletePermissionMap);
  }

  /**
   * Get the KeyValuePermission that have been parsed from the permissions string
   *
   * @return
   */
  public Map<String, Set<String>> getReadPermissionMap() {
    return Collections.unmodifiableMap(readPermissionMap);
  }

  /**
   * Setter used by the ui to set the permissions/attributes
   *
   * @param permStrings
   */
  public void setCreatePermissions(String[] permStrings) {
    if (permStrings != null) {
      this.createPermissions = Arrays.copyOf(permStrings, permStrings.length);
      parsePermissionsFromString(permStrings, createPermissionMap);
    }
  }

  /**
   * Setter used by the ui to set the permissions/attributes
   *
   * @param permStrings
   */
  public void setUpdatePermissions(String[] permStrings) {
    if (permStrings != null) {
      this.updatePermissions = Arrays.copyOf(permStrings, permStrings.length);
      parsePermissionsFromString(permStrings, updatePermissionMap);
    }
  }

  /**
   * Setter used by the ui to set the permissions/attributes
   *
   * @param permStrings
   */
  public void setDeletePermissions(String[] permStrings) {
    if (permStrings != null) {
      this.deletePermissions = Arrays.copyOf(permStrings, permStrings.length);
      parsePermissionsFromString(permStrings, deletePermissionMap);
    }
  }

  /**
   * Setter used by the ui to set the permissions/attributes
   *
   * @param permStrings
   */
  public void setReadPermissions(String[] permStrings) {
    if (permStrings != null) {
      this.readPermissions = Arrays.copyOf(permStrings, permStrings.length);
      parsePermissionsFromString(permStrings, readPermissionMap);
    }
  }

  /**
   * Parses a string array representation of permission attributes
   *
   * @param permStrings String array of permissions to parse
   */
  private void parsePermissionsFromString(
      String[] permStrings, Map<String, Set<String>> permissions) {
    permissions.clear();
    permissions.putAll(Permissions.parsePermissionsFromString(permStrings));
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (Requests.isLocal(properties)) {
      return new PolicyResponseImpl(getCreatePermissionMap(), null);
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (Requests.isLocal(properties)) {
      return new PolicyResponseImpl(getUpdatePermissionMap(), null);
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {

    if (Requests.isLocal(properties)) {
      return new PolicyResponseImpl(getDeletePermissionMap(), null);
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (Requests.isLocal(properties)) {
      return new PolicyResponseImpl(getReadPermissionMap(), null);
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    if (Requests.isLocal(resourceRequest.getProperties())) {
      return new PolicyResponseImpl(getReadPermissionMap(), null);
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }
}
