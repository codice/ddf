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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;

public class WorkspacePolicyExtensionTest {

    private static final String SYSTEM_EMAIL = "system@localhost";

    private static final Permission SYSTEM =
            makePermission(WorkspacePolicyExtension.EMAIL_ADDRESS_CLAIM_URI,
                    ImmutableSet.of(SYSTEM_EMAIL));

    private static final Permission ADMIN =
            makePermission(WorkspacePolicyExtension.EMAIL_ADDRESS_CLAIM_URI,
                    ImmutableSet.of("admin@localhost"));

    private static final Set<String> VALUES = ImmutableSet.of("value1", "value2", "value3");

    private static final Permission RANDOM = makePermission("random", VALUES);

    private static final Permission ROLES = makePermission(WorkspacePolicyExtension.ROLES_CLAIM_URI,
            VALUES);

    private static final Permission EMAILS =
            makePermission(WorkspacePolicyExtension.EMAIL_ADDRESS_CLAIM_URI, VALUES);

    private PolicyExtension extension;

    private CollectionPermission subject;

    private KeyValueCollectionPermission matchOne;

    @Before
    public void setUp() {
        extension = new WorkspacePolicyExtension() {
            @Override
            protected String getSystemEmail() {
                return SYSTEM_EMAIL;
            }
        };
        subject = mock(CollectionPermission.class);
        matchOne = mock(KeyValueCollectionPermission.class);
    }

    private static KeyValuePermission makePermission(String key, Set<String> values) {
        KeyValuePermission permission = mock(KeyValuePermission.class);
        doReturn(key).when(permission)
                .getKey();
        doReturn(values).when(permission)
                .getValues();
        return permission;
    }

    @Test
    public void testIsPermittedMatchAllPassThrough() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(matchOne)
                .getPermissionList();

        List<Permission> after = extension.isPermittedMatchAll(subject, matchOne)
                .getPermissionList();

        assertThat(after, is(before));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenSystem() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(matchOne)
                .getPermissionList();

        doReturn(ImmutableList.of(SYSTEM)).when(subject)
                .getPermissionList();

        List<Permission> after = extension.isPermittedMatchOne(subject, matchOne)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldRemoveRolesAndEmailsWhenAnyImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(matchOne)
                .getPermissionList();

        doReturn(ImmutableList.of(ADMIN)).when(subject)
                .getPermissionList();

        doReturn(true).when(subject)
                .implies(any(Permission.class));

        List<Permission> after = extension.isPermittedMatchOne(subject, matchOne)
                .getPermissionList();

        assertThat(after, is(ImmutableList.of(RANDOM)));
    }

    @Test
    public void testShouldKeepRolesAndEmailsWhenNoneImplied() {
        List<Permission> before = ImmutableList.of(RANDOM, ROLES, EMAILS);

        doReturn(before).when(matchOne)
                .getPermissionList();

        doReturn(ImmutableList.of(ADMIN)).when(subject)
                .getPermissionList();

        doReturn(false).when(subject)
                .implies(any(Permission.class));

        List<Permission> after = extension.isPermittedMatchOne(subject, matchOne)
                .getPermissionList();

        assertThat(after, is(before));
    }

}