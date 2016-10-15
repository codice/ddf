/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.shiro.authz.Permission;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

public class WorkspacePolicyExtensionTest {

    private static final Permission ADMIN_ROLE = makePermission(Constants.ROLES_CLAIM_URI,
            ImmutableSet.of("admin"));

    private static final Permission ADMIN_EMAIL = makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI,
            ImmutableSet.of("admin@localhost"));

    private static final Permission SYSTEM_ROLE = makePermission(Constants.ROLES_CLAIM_URI,
            ImmutableSet.of("system"));

    private static final Set<String> VALUES = ImmutableSet.of("value1", "value2", "value3");

    private static final Permission RANDOM = makePermission("random", VALUES);

    private static final Permission ROLES = makePermission(SecurityAttributes.ACCESS_GROUPS,
            VALUES);

    private static final Permission EMAILS = makePermission(SecurityAttributes.ACCESS_INDIVIDUALS,
            VALUES);

    private WorkspacePolicyExtension extension;

    private KeyValueCollectionPermission match;

    @Before
    public void setUp() {
        extension = new WorkspacePolicyExtension();
        match = mock(KeyValueCollectionPermission.class);
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
        return makeSubject((KeyValuePermission p2) -> ps.stream()
                .filter((p1) -> p1.equals(p2))
                .findFirst()
                .isPresent());
    }

    private static CollectionPermission subjectFrom(Permission p) {
        return subjectFrom(ImmutableList.of(p));
    }

    private static KeyValuePermission makePermission(String key, Set<String> values) {
        return new KeyValuePermission(key, values) {
            @Override
            public boolean equals(Object obj) {
                KeyValuePermission permission = (KeyValuePermission) obj;
                return permission.getKey()
                        .equals(this.getKey()) && permission.getValues()
                        .equals(this.getValues());
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };
    }

    @Test
    public void testShouldKeepAllWhenNoneImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = makeSubject((p) -> false);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(before));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenAnyImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = makeSubject((p) -> true);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenRoleImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = subjectFrom(makePermission(Constants.ROLES_CLAIM_URI,
                VALUES));

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenEmailImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = subjectFrom(makePermission(Constants.EMAIL_ADDRESS_CLAIM_URI,
                VALUES));

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldIgnoreRandom() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = subjectFrom(RANDOM);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(before));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenAdmin() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        CollectionPermission subject = subjectFrom(ADMIN_ROLE);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenOverridden1() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        extension.setSystemUserAttribute(Constants.EMAIL_ADDRESS_CLAIM_URI);
        extension.setSystemUserAttributeValue("admin@localhost");

        CollectionPermission subject = subjectFrom(ADMIN_EMAIL);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenOverridden2() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(match)
                .getPermissionList();

        extension.setSystemUserAttributeValue("system");

        CollectionPermission subject = subjectFrom(SYSTEM_ROLE);

        List<Permission> after = extension.isPermittedMatchAll(subject, match)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

}