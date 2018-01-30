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
package org.codice.ddf.catalog.ui.security;

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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.shiro.authz.Permission;
import org.junit.Before;
import org.junit.Test;

public class WorkspacePolicyExtensionTest {

  private static final Permission WORKSPACE =
      makePermission(Constants.IS_WORKSPACE, ImmutableSet.of());

  private static final Permission ADMIN_ROLE =
      makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of("admin"));

  private static final Set<String> VALUES = ImmutableSet.of("value1", "value2", "value3");

  private static final Permission RANDOM = makePermission("random", VALUES);

  private static final Permission ROLES = makePermission(SecurityAttributes.ACCESS_GROUPS, VALUES);

  private static final Permission EMAILS =
      makePermission(SecurityAttributes.ACCESS_INDIVIDUALS, VALUES);

  private static final Permission OWNER =
      makePermission(Core.METACARD_OWNER, ImmutableSet.of("owner"));

  private WorkspacePolicyExtension extension;

  private WorkspaceSecurityConfiguration config;

  private SubjectIdentity subjectIdentity;

  @Before
  public void setUp() {
    subjectIdentity = mock(SubjectIdentity.class);
    when(subjectIdentity.getIdentityAttribute()).thenReturn(Constants.EMAIL_ADDRESS_CLAIM_URI);
    config = new WorkspaceSecurityConfiguration();
    extension = new WorkspacePolicyExtension(config, subjectIdentity);
  }

  private static CollectionPermission makeSubject(Predicate<KeyValuePermission> fn) {
    return new CollectionPermission() {
      @Override
      public boolean implies(Permission p) {
        return fn.test((KeyValuePermission) p);
      }
    };
  }

  private static CollectionPermission subjectFrom(List<Permission> ps) {
    return makeSubject(
        (KeyValuePermission p2) ->
            ps.stream().filter((p1) -> p1.equals(p2)).findFirst().isPresent());
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

  @Test
  public void testWorkspaceTagShouldAlwaysBeImplied() {
    List<Permission> before = ImmutableList.of(WORKSPACE, RANDOM);

    CollectionPermission subject = makeSubject((p) -> false);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(RANDOM)));
  }

  @Test
  public void testShouldIgnoreNonWorkspaceMetacards() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(OWNER);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testAccessGroupShouldImplyWorkspace() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, VALUES));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(RANDOM)));
  }

  @Test
  public void testAccessGroupShouldImplyNone() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM)));
  }

  @Test
  public void testAccessIndividualShouldImplyWorkspace() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, VALUES));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(RANDOM)));
  }

  @Test
  public void testAccessIndividualShouldImplyNone() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM)));
  }

  @Test
  public void testSystemShouldImplyAll() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(ADMIN_ROLE);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOwnerShouldImplAll() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideSystemUserShouldImplyAll() {
    String email = "admin@localhost";
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

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
    String role = "system";
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

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
    String attr = "another";
    when(subjectIdentity.getIdentityAttribute()).thenReturn(attr);

    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(makePermission(attr, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of()));
  }

  @Test
  public void testOverrideOwnerShouldImplyNone() {
    List<Permission> before = ImmutableList.of(WORKSPACE, OWNER, ROLES, EMAILS, RANDOM);

    when(subjectIdentity.getIdentityAttribute()).thenReturn("another");

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM)));
  }
}
