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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;

public class AdminConfigPolicyTest {

  static final String TEST_PERMISSION_VALUE = "test-permission-value";

  static final String TEST_ATTRIBUTE_NAME = "test-attri";

  static final String TEST_ATTRIBUTE_VALUE = "test-attri-value";

  static final String UNAUTHORIZED = "-unauthorized";

  @Test
  public void testInvalidPolicyConfigs() {

    AdminConfigPolicy adminConfigPolicy = new AdminConfigPolicy();
    adminConfigPolicy.setFeaturePolicies(getValidPolicyPermissions());

    for (int i = 0; i < 3; i++) {
      List<KeyValueCollectionPermission> featurePolicyPermissions =
          adminConfigPolicy.featurePolicyPermissions.get(TEST_PERMISSION_VALUE + i);
      assertNotNull(featurePolicyPermissions);
      assertEquals(featurePolicyPermissions.size(), 1);

      KeyValuePermission featurePolicyPermission =
          (KeyValuePermission) featurePolicyPermissions.get(0).getPermissionList().get(0);
      assertEquals(featurePolicyPermission.getKey(), TEST_ATTRIBUTE_NAME + i);
      assertEquals(featurePolicyPermission.getValues(), Sets.newHashSet(TEST_ATTRIBUTE_VALUE + i));
    }
  }

  @Test
  public void testValidateAllPermissions() {
    AdminConfigPolicy adminConfigPolicy = new AdminConfigPolicy();
    adminConfigPolicy.setFeaturePolicies(getValidPolicyPermissions());
    adminConfigPolicy.setServicePolicies(getValidPolicyPermissions());

    KeyValueCollectionPermission requestedFeaturePermissions =
        new KeyValueCollectionPermissionImpl(
            AdminConfigPolicy.VIEW_FEATURE_ACTION,
            getMatchOnePermissions(AdminConfigPolicy.FEATURE_NAME)
                .stream()
                .toArray(KeyValuePermissionImpl[]::new));

    KeyValueCollectionPermission requestedServicePermissions =
        new KeyValueCollectionPermissionImpl(
            AdminConfigPolicy.VIEW_SERVICE_ACTION,
            getMatchOnePermissions(AdminConfigPolicy.SERVICE_PID)
                .stream()
                .toArray(KeyValuePermissionImpl[]::new));

    assertTrue(
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(), requestedFeaturePermissions, requestedFeaturePermissions)
            .isEmpty());

    assertTrue(
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(), requestedServicePermissions, requestedServicePermissions)
            .isEmpty());
  }

  @Test
  public void testRemoveSomePermissions() {
    AdminConfigPolicy adminConfigPolicy = new AdminConfigPolicy();
    List<String> featurePolicyPermissions = getValidPolicyPermissions();
    List<String> servicePolicyPermissions = getValidPolicyPermissions();

    featurePolicyPermissions.add(
        TEST_PERMISSION_VALUE
            + UNAUTHORIZED
            + "=\""
            + TEST_ATTRIBUTE_NAME
            + UNAUTHORIZED
            + "="
            + TEST_ATTRIBUTE_VALUE
            + UNAUTHORIZED
            + "\"");
    servicePolicyPermissions.add(
        TEST_PERMISSION_VALUE
            + UNAUTHORIZED
            + "=\""
            + TEST_ATTRIBUTE_NAME
            + UNAUTHORIZED
            + "="
            + TEST_ATTRIBUTE_VALUE
            + UNAUTHORIZED
            + "\"");

    adminConfigPolicy.setFeaturePolicies(featurePolicyPermissions);
    adminConfigPolicy.setServicePolicies(servicePolicyPermissions);

    List<KeyValuePermission> matchOneFeaturePermissions =
        getMatchOnePermissions(AdminConfigPolicy.FEATURE_NAME);
    matchOneFeaturePermissions.add(
        new KeyValuePermissionImpl(
            AdminConfigPolicy.FEATURE_NAME, Sets.newHashSet(TEST_PERMISSION_VALUE + UNAUTHORIZED)));

    List<KeyValuePermission> matchOneServicePermissions =
        getMatchOnePermissions(AdminConfigPolicy.SERVICE_PID);
    matchOneServicePermissions.add(
        new KeyValuePermissionImpl(
            AdminConfigPolicy.SERVICE_PID, Sets.newHashSet(TEST_PERMISSION_VALUE + UNAUTHORIZED)));

    List<KeyValuePermission> matchOneInvalidActionPermission = new ArrayList<>();
    matchOneInvalidActionPermission.add(
        new KeyValuePermissionImpl("UNKNOWN_ACTION", Sets.newHashSet(TEST_PERMISSION_VALUE)));

    KeyValueCollectionPermission requestedFeaturePermissions =
        new KeyValueCollectionPermissionImpl(
            AdminConfigPolicy.VIEW_FEATURE_ACTION,
            matchOneFeaturePermissions.stream().toArray(KeyValuePermissionImpl[]::new));

    KeyValueCollectionPermission requestedServicePermissions =
        new KeyValueCollectionPermissionImpl(
            AdminConfigPolicy.VIEW_SERVICE_ACTION,
            matchOneServicePermissions.stream().toArray(KeyValuePermissionImpl[]::new));

    KeyValueCollectionPermission requestedInvalidActionPermissions =
        new KeyValueCollectionPermissionImpl(
            "UNKNOWN_ACTION",
            matchOneInvalidActionPermission.stream().toArray(KeyValuePermissionImpl[]::new));

    assertEquals(
        1,
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(), requestedFeaturePermissions, requestedFeaturePermissions)
            .getPermissionList()
            .size());

    assertEquals(
        1,
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(), requestedServicePermissions, requestedServicePermissions)
            .getPermissionList()
            .size());

    assertEquals(
        1,
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(), requestedServicePermissions, requestedServicePermissions)
            .getPermissionList()
            .size());

    assertEquals(
        1,
        adminConfigPolicy
            .isPermittedMatchOne(
                getSubjectPermissions(),
                requestedInvalidActionPermissions,
                requestedInvalidActionPermissions)
            .getPermissionList()
            .size());
  }

  @Test
  public void testRemoveUnknownAttribute() {
    AdminConfigPolicy adminConfigPolicy = new AdminConfigPolicy();
    List<KeyValuePermission> matchOneServicePermissions = new ArrayList<>();
    matchOneServicePermissions.add(
        new KeyValuePermissionImpl(
            AdminConfigPolicy.SERVICE_PID, Sets.newHashSet("UNKNOWN_ATTRIBUTE_NAME")));

    KeyValueCollectionPermission requestedServicePermissions =
        new KeyValueCollectionPermissionImpl(
            AdminConfigPolicy.VIEW_SERVICE_ACTION,
            matchOneServicePermissions.stream().toArray(KeyValuePermissionImpl[]::new));

    assertTrue(
        adminConfigPolicy
            .isPermittedMatchAll(
                getSubjectPermissions(), requestedServicePermissions, requestedServicePermissions)
            .isEmpty());
  }

  public List<String> getValidPolicyPermissions() {
    List<String> policyPermissions = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      policyPermissions.add(
          TEST_PERMISSION_VALUE
              + i
              + "=\""
              + TEST_ATTRIBUTE_NAME
              + i
              + "="
              + TEST_ATTRIBUTE_VALUE
              + i
              + "\"");
    }
    return policyPermissions;
  }

  public KeyValueCollectionPermission getSubjectPermissions() {
    KeyValueCollectionPermission subjectCollectionPermissions =
        new KeyValueCollectionPermissionImpl();
    subjectCollectionPermissions.addAll(
        new HashMap<String, List<String>>() {
          {
            for (int i = 0; i < 3; i++) {
              put(TEST_ATTRIBUTE_NAME + i, Arrays.asList(TEST_ATTRIBUTE_VALUE + i));
            }
          }
        });

    return subjectCollectionPermissions;
  }

  public List<KeyValuePermission> getMatchOnePermissions(String permissionKey) {
    List<KeyValuePermission> matchOneServicePermissions = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      matchOneServicePermissions.add(
          new KeyValuePermissionImpl(permissionKey, Sets.newHashSet(TEST_PERMISSION_VALUE + i)));
    }

    return matchOneServicePermissions;
  }
}
