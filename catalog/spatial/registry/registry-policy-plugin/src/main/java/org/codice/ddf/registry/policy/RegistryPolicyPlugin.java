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
package org.codice.ddf.registry.policy;

import ddf.catalog.data.Attribute;
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
import ddf.security.permission.Permissions;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;

public class RegistryPolicyPlugin implements PolicyPlugin {

  private boolean whiteList = false;

  private boolean registryDisabled = false;

  private Set<String> registryEntryIds = new HashSet<>();

  private Map<String, Set<String>> bypassAccessPolicy = new HashMap<>();

  private Map<String, Set<String>> createAccessPolicy = new HashMap<>();

  private Map<String, Set<String>> updateAccessPolicy = new HashMap<>();

  private Map<String, Set<String>> deleteAccessPolicy = new HashMap<>();

  private Map<String, Set<String>> readAccessPolicy = new HashMap<>();

  public void init() {
    addRemoveIdentity();
  }

  /**
   * @return Returns true if the registry entry ids represents a set of 'white listed' entries.
   *     Default is false.
   */
  public boolean isWhiteList() {
    return whiteList;
  }

  /**
   * Sets the whether or not the registry entry ids list is a 'white list' or not.
   *
   * @param whiteList boolean value for whiteList
   */
  public void setWhiteList(boolean whiteList) {
    this.whiteList = whiteList;
    addRemoveIdentity();
  }

  /**
   * Get a string set of the federation service ids allowed to be queried
   *
   * @return A set of registry entry id strings
   */
  public Set<String> getRegistryEntryIds() {
    return registryEntryIds;
  }

  /**
   * Setter for the list of allowed federation service ids. All registry ids in this list will be
   * allowed to be queried and returned
   *
   * @param registryEntryIds Set of registry entry ids
   */
  public void setRegistryEntryIds(Set<String> registryEntryIds) {
    this.registryEntryIds = registryEntryIds;
  }

  public boolean isRegistryDisabled() {
    return registryDisabled;
  }

  public void setRegistryDisabled(boolean registryDisabled) {
    this.registryDisabled = registryDisabled;
  }

  public void setCreateAccessPolicyStrings(List<String> accessPolicyStrings) {
    parsePermissionsFromString(accessPolicyStrings, createAccessPolicy);
  }

  public void setUpdateAccessPolicyStrings(List<String> accessPolicyStrings) {
    parsePermissionsFromString(accessPolicyStrings, updateAccessPolicy);
  }

  public void setDeleteAccessPolicyStrings(List<String> accessPolicyStrings) {
    parsePermissionsFromString(accessPolicyStrings, deleteAccessPolicy);
  }

  public void setReadAccessPolicyStrings(List<String> readAccessPolicyStrings) {
    parsePermissionsFromString(readAccessPolicyStrings, readAccessPolicy);
  }

  /**
   * Setter used by the ui to set the permissions/attributes
   *
   * @param permStrings The list of bypass permissions
   */
  public void setRegistryBypassPolicyStrings(List<String> permStrings) {
    parsePermissionsFromString(permStrings, bypassAccessPolicy);
  }

  /**
   * Parses a string array representation of permission attributes
   *
   * @param permStrings String array of permissions to parse
   * @param policy The policy map to put the parsed permissions in
   */
  private void parsePermissionsFromString(
      List<String> permStrings, Map<String, Set<String>> policy) {
    policy.clear();
    if (permStrings != null) {
      for (String perm : permStrings) {
        String[] parts = perm.trim().split("=");
        if (parts.length == 2) {
          String attributeName = parts[0];
          String attributeValue = parts[1];
          policy.put(attributeName, Collections.singleton(attributeValue));
        }
      }
    }
  }

  private PolicyResponse getWritePolicy(
      Metacard input, Map<String, Serializable> properties, Map<String, Set<String>> policy) {

    Map<String, Set<String>> operationPolicy = new HashMap<>();
    Map<String, Set<String>> securityAttributes = new HashMap<>();

    if (Requests.isLocal(properties)
        && input != null
        && (RegistryUtility.isInternalRegistryMetacard(input)
            || RegistryUtility.isRegistryMetacard(input))) {

      Attribute securityAttribute = input.getAttribute(RegistryObjectMetacardType.SECURITY_LEVEL);
      if (securityAttribute != null) {
        securityAttributes.putAll(
            Permissions.parsePermissionsFromString(
                securityAttribute
                    .getValues()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())));
      }
      String registryBaseUrl =
          RegistryUtility.getStringAttribute(
              input, RegistryObjectMetacardType.REGISTRY_BASE_URL, null);
      if (isRegistryDisabled()
          || (registryBaseUrl != null && registryBaseUrl.startsWith(SystemBaseUrl.getBaseUrl()))) {
        operationPolicy.putAll(bypassAccessPolicy);
      } else {
        operationPolicy.putAll(policy);
      }
    }
    if (securityAttributes.isEmpty()) {
      return new PolicyResponseImpl(operationPolicy, operationPolicy);
    } else {
      return new PolicyResponseImpl(operationPolicy, securityAttributes);
    }
  }

  private void addRemoveIdentity() {
    String identityNodeRegId = System.getProperty(RegistryConstants.REGISTRY_ID_PROPERTY);
    if (identityNodeRegId != null) {
      if (whiteList) {
        registryEntryIds.add(identityNodeRegId);
      } else {
        registryEntryIds.remove(identityNodeRegId);
      }
    }
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return getWritePolicy(input, properties, createAccessPolicy);
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return getWritePolicy(input, properties, updateAccessPolicy);
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (metacards != null) {
      for (Metacard metacard : metacards) {
        PolicyResponse response = getWritePolicy(metacard, properties, deleteAccessPolicy);
        if (!response.operationPolicy().isEmpty()) {
          return response;
        }
      }
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
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    HashMap<String, Set<String>> itemPolicy = new HashMap<>();
    Metacard metacard = input.getMetacard();
    if (RegistryUtility.isRegistryMetacard(metacard)) {
      String regId = RegistryUtility.getRegistryId(metacard);
      if (isRegistryDisabled()
          || (whiteList && !registryEntryIds.contains(regId))
          || (!whiteList && registryEntryIds.contains(regId))) {
        itemPolicy.putAll(bypassAccessPolicy);
      } else {
        itemPolicy.putAll(readAccessPolicy);
      }
    }
    return new PolicyResponseImpl(new HashMap<>(), itemPolicy);
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  public Map<String, Set<String>> getBypassAccessPolicy() {
    return Collections.unmodifiableMap(bypassAccessPolicy);
  }

  public Map<String, Set<String>> getCreateAccessPolicy() {
    return Collections.unmodifiableMap(createAccessPolicy);
  }

  public Map<String, Set<String>> getUpdateAccessPolicy() {
    return Collections.unmodifiableMap(updateAccessPolicy);
  }

  public Map<String, Set<String>> getDeleteAccessPolicy() {
    return Collections.unmodifiableMap(deleteAccessPolicy);
  }

  public Map<String, Set<String>> getReadAccessPolicy() {
    return Collections.unmodifiableMap(readAccessPolicy);
  }
}
