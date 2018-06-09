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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
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
import org.junit.Ignore;
import org.junit.Test;

public class ShareableMetacardPolicyExtensionTest {

  private static final Permission DEFAULT_SYSTEM_ROLE =
      makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of("system-user"));

  private static final Set<String> VALUES = ImmutableSet.of("value1", "value2", "value3");

  private static final Permission RANDOM = makePermission("random", VALUES);

  private static final Permission ROLES = makePermission(SecurityAttributes.ACCESS_GROUPS, VALUES);

  private static final Permission EMAILS =
      makePermission(SecurityAttributes.ACCESS_INDIVIDUALS, VALUES);

  private static final Permission OWNER =
      makePermission(Core.METACARD_OWNER, ImmutableSet.of("owner"));

  private ShareableMetacardPolicyExtension extension;

  private ShareableMetacardSecurityConfiguration config;

  private SubjectIdentity subjectIdentity;

  @Before
  public void setUp() {
    subjectIdentity = mock(SubjectIdentity.class);
    when(subjectIdentity.getIdentityAttribute()).thenReturn(Constants.EMAIL_ADDRESS_CLAIM_URI);
    config = new ShareableMetacardSecurityConfiguration();
    extension = new ShareableMetacardPolicyExtension(config, subjectIdentity);
  }

  @Test
  public void testSharedMetacardTagShouldAlwaysBeImplied() {
    List<Permission> before = ImmutableList.of(RANDOM);

    CollectionPermission subject = makeSubject((p) -> false);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    // The policy extension did not remove ("imply") any of the permissions
    assertThat(after, is(before));
  }

  @Test
  public void testShouldIgnoreNonSharedMetacards() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(OWNER);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testAccessGroupShouldImplySharable() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, VALUES));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(RANDOM)));
  }

  @Test
  public void testAccessGroupShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testAccessIndividualShouldImplyShareable() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, VALUES));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(ImmutableList.of(RANDOM)));
  }

  @Test
  public void testAccessIndividualShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of()));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  public void testSystemShouldImplyAll() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(DEFAULT_SYSTEM_ROLE);

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  @Test
  // TODO: Understand distinction with testMatchOneOnwerShouldImplyAll
  public void testOwnerShouldImplyAll() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  @Test
  public void testOverrideSystemUserShouldImplyAll() {
    String email = "admin@localhost";
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    config.setSystemUserAttribute(Constants.EMAIL_ADDRESS_CLAIM_URI);
    config.setSystemUserAttributeValue(email);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of(email)));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  @Test
  public void testOverrideSystemRoleShouldImplyAll() {
    String role = "system";
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    config.setSystemUserAttribute(Constants.ROLES_CLAIM_URI);
    config.setSystemUserAttributeValue(role);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.ROLES_CLAIM_URI, ImmutableSet.of(role)));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  @Test
  public void testOverrideOwnerShouldImplyAll() {
    String attr = "another";
    when(subjectIdentity.getIdentityAttribute()).thenReturn(attr);

    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject = subjectFrom(makePermission(attr, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  @Test
  public void testOverrideOwnerShouldImplyNone() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    when(subjectIdentity.getIdentityAttribute()).thenReturn("another");

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchAll(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(before));
  }

  @Test
  @Ignore
  // TODO: Understand distinction
  public void testMatchOneOwnerShouldImplyAll() {
    List<Permission> before = ImmutableList.of(OWNER, ROLES, EMAILS, RANDOM);

    CollectionPermission subject =
        subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI, ImmutableSet.of("owner")));

    List<Permission> after =
        extension.isPermittedMatchOne(subject, coll(before), coll(before)).getPermissionList();

    assertThat(after, is(empty()));
  }

  private static KeyValueCollectionPermission coll(List<Permission> permissions) {
    KeyValueCollectionPermission match = new KeyValueCollectionPermission();
    match.addAll(permissions);
    return match;
  }

  private static CollectionPermission subjectFrom(Permission p) {
    return makeSubject(p::equals);
  }

  private static CollectionPermission makeSubject(Predicate<KeyValuePermission> fn) {
    return new TestPredicateCollectionPermission(fn);
  }

  private static KeyValuePermission makePermission(String key, Set<String> values) {
    return new TestKeyValuePermission(key, values);
  }

  /**
   * An implementation of {@link CollectionPermission} that implies based upon the provided {@link
   * Predicate}.
   */
  private static class TestPredicateCollectionPermission extends CollectionPermission {
    private final Predicate<KeyValuePermission> predicate;

    TestPredicateCollectionPermission(Predicate<KeyValuePermission> predicate) {
      super();
      this.predicate = predicate;
    }

    @Override
    public boolean implies(Permission p) {
      return predicate.test((KeyValuePermission) p);
    }
  }

  /**
   * Simple {@link KeyValuePermission} that overrides {@link #equals(Object)} for the sake of
   * testing.
   */
  private static class TestKeyValuePermission extends KeyValuePermission {
    TestKeyValuePermission(String key, Set<String> values) {
      super(key, values);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof KeyValuePermission)) {
        return false;
      }
      KeyValuePermission permission = (KeyValuePermission) obj;
      return permission.getKey().equals(this.getKey())
          && permission.getValues().equals(this.getValues());
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }
}
