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
package org.codice.ddf.admin.config.policy;

import com.google.common.collect.Sets;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import ddf.security.policy.extension.PolicyExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConfigPolicy implements PolicyExtension {

  public static final String FEATURE_NAME = "feature.name";

  public static final String SERVICE_PID = "service.pid";

  public static final String VIEW_FEATURE_ACTION = "view-" + FEATURE_NAME;

  public static final String VIEW_SERVICE_ACTION = "view-" + SERVICE_PID;

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminConfigPolicy.class);

  Map<String, List<KeyValueCollectionPermission>> featurePolicyPermissions = new HashMap<>();

  Map<String, List<KeyValueCollectionPermission>> servicePolicyPermissions = new HashMap<>();

  @Override
  public KeyValueCollectionPermission isPermittedMatchAll(
      CollectionPermission subjectAllCollection,
      KeyValueCollectionPermission matchAllCollection,
      KeyValueCollectionPermission allPermissionsCollection) {
    return isPermittedMatchOne(subjectAllCollection, matchAllCollection, allPermissionsCollection);
  }

  @Override
  public KeyValueCollectionPermission isPermittedMatchOne(
      CollectionPermission subjectAllCollection,
      KeyValueCollectionPermission matchOneCollection,
      KeyValueCollectionPermission allPermissionsCollection) {

    if (matchOneCollection.getAction() == null
        || (!matchOneCollection.getAction().equals(VIEW_FEATURE_ACTION)
            && !matchOneCollection.getAction().equals(VIEW_SERVICE_ACTION))) {
      return matchOneCollection;
    }

    List<Permission> newMatchOneCollectionPermissions =
        new ArrayList<>(matchOneCollection.getPermissionList());
    for (Permission permission : matchOneCollection.getPermissionList()) {

      if (!(permission instanceof KeyValuePermission)) {
        continue;
      }

      String matchPermissionName = ((KeyValuePermission) permission).getKey();

      Map<String, List<KeyValueCollectionPermission>> policyPermissions;

      if (matchPermissionName.equals(FEATURE_NAME)) {
        policyPermissions = featurePolicyPermissions;
      } else if (matchPermissionName.equals(SERVICE_PID)) {
        policyPermissions = servicePolicyPermissions;
      } else {
        continue;
      }

      Set<String> valuesToMatch = new HashSet<>();
      valuesToMatch.addAll(((KeyValuePermission) permission).getValues());

      // Typically only one feature or service is desired to be permitted at a time but there is
      // support for multiple feature
      // If there are multiple features in the permission and one is not authorized, the user is not
      // authorized to see any of the features in the group
      for (String matchPermissionValue : ((KeyValuePermission) permission).getValues()) {
        List<KeyValueCollectionPermission> matchOneAttributes =
            policyPermissions.get(matchPermissionValue);

        // If null, there is no configuration with this attribute in the policy, the feature or
        // service is white listed
        if (matchOneAttributes == null) {
          valuesToMatch.remove(matchPermissionValue);
        } else {
          for (KeyValueCollectionPermission attributePermissions : matchOneAttributes) {
            if (subjectAllCollection.implies(attributePermissions)) {
              valuesToMatch.remove(matchPermissionValue);
              break;
            }
          }
        }
      }

      if (valuesToMatch.isEmpty()) {
        newMatchOneCollectionPermissions.remove(permission);
      }
    }

    return new KeyValueCollectionPermissionImpl(
        matchOneCollection.getAction(),
        newMatchOneCollectionPermissions.stream().toArray(KeyValuePermissionImpl[]::new));
  }

  public void setFeaturePolicies(List<String> featurePolicies) {
    featurePolicyPermissions.clear();
    featurePolicyPermissions.putAll(parsePermissions(featurePolicies));
  }

  public void setServicePolicies(List<String> servicePolicies) {
    servicePolicyPermissions.clear();
    servicePolicyPermissions.putAll(parsePermissions(servicePolicies));
  }

  public Map<String, List<KeyValueCollectionPermission>> parsePermissions(List<String> policies) {

    Map<String, List<KeyValueCollectionPermission>> newPolicyPermissions = new HashMap<>();

    for (String policy : policies) {

      if (StringUtils.isEmpty(policy)) {
        continue;
      }

      // Example input: featureName="attributeName=attributeValue","attributeName2=attributeValue2"
      String[] policyTrimmed = policy.replaceAll("\\s+", "").split("=", 2);
      String permissionName = policyTrimmed[0];
      String policyAttributes = policyTrimmed[1];

      List<KeyValueCollectionPermission> permissionAttributeMap = new ArrayList<>();

      for (String policyAttribute : policyAttributes.split(",")) {
        policyAttribute = policyAttribute.replace("\"", "");
        policyAttribute = policyAttribute.replaceAll("\\s+", "");
        String[] policyAttributeSplit = policyAttribute.split("=");
        String attributeName = policyAttributeSplit[0];
        String attributeValue = policyAttributeSplit[1];

        KeyValueCollectionPermission newPermission =
            new KeyValueCollectionPermissionImpl(
                null, new KeyValuePermissionImpl(attributeName, Sets.newHashSet(attributeValue)));
        permissionAttributeMap.add(newPermission);
      }

      if (newPolicyPermissions.containsKey(permissionName)) {
        LOGGER.debug("Policy extension settings for {} already exist, overwriting", permissionName);
      }
      newPolicyPermissions.put(permissionName, permissionAttributeMap);
    }

    return newPolicyPermissions;
  }
}
