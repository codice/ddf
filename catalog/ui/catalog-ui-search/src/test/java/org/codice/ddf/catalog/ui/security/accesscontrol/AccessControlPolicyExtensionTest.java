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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import static ddf.security.permission.CollectionPermission.READ_ACTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.security.SubjectIdentity;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.MatchOneCollectionPermission;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.shiro.authz.Permission;
import org.codice.ddf.catalog.ui.security.Constants;
import org.junit.Before;
import org.junit.Test;

public class AccessControlPolicyExtensionTest {

  private static final Permission SYSTEM_ROLE =
      makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of("system-user"));

  private static final Set<String> VALUES = ImmutableSet.of("owner", "value1", "value2", "value3");

  private static final Set<String> EMAILS =
      ImmutableSet.of("owner@connexta.com", "owner2@connexta.com");

  private static final Permission RANDOM = makePermission("random", VALUES);

  private static final Permission ROLES = makePermission(SecurityAttributes.ACCESS_GROUPS, VALUES);

  private static final Permission ROLES_READ_ONLY =
      makePermission(SecurityAttributes.ACCESS_GROUPS_READ, VALUES);

  private static final Permission ADMINISTRATORS =
      makePermission(SecurityAttributes.ACCESS_ADMINISTRATORS, EMAILS);

  private static final Permission INDIVIDUALS =
      makePermission(SecurityAttributes.ACCESS_INDIVIDUALS, EMAILS);

  private static final Permission INDIVIDUALS_READ_ONLY =
      makePermission(SecurityAttributes.ACCESS_INDIVIDUALS_READ, EMAILS);

  private static final Permission OWNER =
      makePermission(Core.METACARD_OWNER, ImmutableSet.of("owner"));

  private AccessControlPolicyExtension extension;

  private AccessControlSecurityConfiguration config;

  private SubjectIdentity subjectIdentity;

  @Before
  public void setUp() {
    subjectIdentity = mock(SubjectIdentity.class);
    when(subjectIdentity.getIdentityAttribute()).thenReturn(Constants.EMAIL_ADDRESS_CLAIM_URI);
    config = new AccessControlSecurityConfiguration();
    extension = new AccessControlPolicyExtension(config, subjectIdentity);
  }

  private static CollectionPermission subjectFrom(List<Permission> ps) {
    return new MatchOneCollectionPermission(ps);
  }

  private static CollectionPermission subjectFrom(Permission p) {
    return subjectFrom(ImmutableList.of(p));
  }

  private static KeyValuePermission makePermission(String key, Set<String> values) {
    return new KeyValuePermission(key, values) {
      @Override
      public boolean equals(Object obj) {
        KeyValuePermission permission = (KeyValuePermission) obj;
        return permission.getKey().equals(this.getKey())
            && permission.getValues().equals(this.getValues());
      }

      @Override
      public int hashCode() {
        return 0;
      }
    };
  }

  private static KeyValueCollectionPermission coll(List<Permission> permissions) {
    KeyValueCollectionPermission match = mock(KeyValueCollectionPermission.class);
    doReturn(permissions).when(match).getPermissionList();
    return match;
  }

  private static KeyValueCollectionPermission collRead(List<Permission> permissions) {
    KeyValueCollectionPermission match = mock(KeyValueCollectionPermission.class);
    doReturn(permissions).when(match).getPermissionList();
    doReturn(READ_ACTION).when(match).getAction();
    return match;
  }

  @Test
  public void testOwnerOfMetacardImpliesAll() {
    List<Permission> before = ImmutableList.of(OWNER, INDIVIDUALS, ROLES, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(Collections.emptyList()));
  }

  @Test
  public void testAccessAdminOfAclAlwaysImplied() {
    List<Permission> before = ImmutableList.of(ADMINISTRATORS, INDIVIDUALS, ROLES);

    CollectionPermission subject =
        subjectFrom(
            makePermission(
                Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner@connexta.com")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(Collections.emptyList()));
  }

  @Test
  public void testLackingIndividualReadImpliesNone() {
    List<Permission> before = ImmutableList.of(INDIVIDUALS_READ_ONLY);

    CollectionPermission subject =
        subjectFrom(
            makePermission(
                Constants.EMAIL_ADDRESS_CLAIM_URI,
                ImmutableSet.of("non-existent-email@connexta.com")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(INDIVIDUALS_READ_ONLY)));
  }

  @Test
  public void testLackingRoleReadImpliesNone() {
    List<Permission> before = ImmutableList.of(ROLES_READ_ONLY);
    CollectionPermission subject =
        subjectFrom(
            makePermission(
                Constants.EMAIL_ADDRESS_CLAIM_URI,
                ImmutableSet.of("non-existent-email@connexta.com")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(ROLES_READ_ONLY)));
  }

  @Test
  public void testIndividualReadImplies() {
    List<Permission> before = ImmutableList.of(INDIVIDUALS_READ_ONLY);

    CollectionPermission subject =
        subjectFrom(
            makePermission(
                Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner@connexta.com")));

    List<Permission> after =
        extension
            .isPermittedMatchAll(subject, collRead(before), collRead(before))
            .getPermissionList();

    assertThat(after, is(Collections.emptyList()));
  }

  @Test
  public void testRoleReadImplies() {
    List<Permission> before = ImmutableList.of(ROLES_READ_ONLY);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension
            .isPermittedMatchAll(subject, collRead(before), collRead(before))
            .getPermissionList();

    assertThat(after, is(Collections.emptyList()));
  }

  @Test
  public void testNonAclMetacardsIgnored() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM);

    CollectionPermission subject = subjectFrom(OWNER);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testMissingAccessGroupShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM)));
  }

  @Test
  public void testMissingAccessIndividualShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testAccessIndividualShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ROLES, INDIVIDUALS, RANDOM)));
  }

  @Test
  public void testSystemShouldImplyAll() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, ADMINISTRATORS, INDIVIDUALS, RANDOM);

    CollectionPermission subject = subjectFrom(SYSTEM_ROLE);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideSystemUserShouldImplyAll() {
    String email = "admin@localhost";
    List<Permission> before = ImmutableList.of(OWNER, ROLES, ADMINISTRATORS, INDIVIDUALS, RANDOM);

    config.setSystemUserAttribute(Constants.EMAIL_ADDRESS_CLAIM_URI);
    config.setSystemUserAttributeValue(email);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of(email)));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideSystemRoleShouldImplyAll() {
    String role = "system-user";
    List<Permission> before = ImmutableList.of(OWNER, ROLES, ADMINISTRATORS, INDIVIDUALS, RANDOM);

    config.setSystemUserAttribute(Constants.ROLES_CLAIM_URI);
    config.setSystemUserAttributeValue(role);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of(role)));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideOwnerShouldImplyAll() {
    when(subjectIdentity.getIdentityAttribute()).thenReturn("another");
    extension = new AccessControlPolicyExtension(config, subjectIdentity);
    String attr = "another";
    when(subjectIdentity.getIdentityAttribute()).thenReturn(attr);

    List<Permission> before = ImmutableList.of(OWNER, ADMINISTRATORS, ROLES, INDIVIDUALS, RANDOM);

    CollectionPermission subject = subjectFrom(makePermission(attr, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideOwnerShouldImplyNone() {
    when(subjectIdentity.getIdentityAttribute()).thenReturn("another");
    extension = new AccessControlPolicyExtension(config, subjectIdentity);
    List<Permission> before = ImmutableList.of(OWNER, ADMINISTRATORS, ROLES, INDIVIDUALS, RANDOM);

    when(subjectIdentity.getIdentityAttribute()).thenReturn("another");

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ADMINISTRATORS, ROLES, INDIVIDUALS, RANDOM)));
  }
}
