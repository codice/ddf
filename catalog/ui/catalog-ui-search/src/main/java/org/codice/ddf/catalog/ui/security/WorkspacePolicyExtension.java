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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;
import org.codice.ddf.security.common.Security;

import com.google.common.collect.ImmutableSet;

import ddf.security.SubjectUtils;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;

public class WorkspacePolicyExtension implements PolicyExtension {

    public static final Set<String> CLAIMS = ImmutableSet.of(Constants.ROLES_CLAIM_URI,
            Constants.EMAIL_ADDRESS_CLAIM_URI);

    /**
     * Find a permission from a list of permissions by key.
     *
     * @param permissions
     * @return
     */
    private static Optional<KeyValuePermission> find(List<Permission> permissions, String key) {
        return permissions.stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> permission.getKey()
                        .equals(key))
                .findFirst();
    }

    protected String getSystemEmail() {
        return Security.runAsAdmin(() -> SubjectUtils.getEmailAddress(Security.getInstance()
                .getSystemSubject()));
    }

    private boolean isSystem(List<Permission> permissions) {
        Optional<KeyValuePermission> subjectEmail = find(permissions,
                Constants.EMAIL_ADDRESS_CLAIM_URI);

        if (subjectEmail.isPresent()) {
            return subjectEmail.get()
                    .getValues()
                    .iterator()
                    .next()
                    .equals(getSystemEmail());
        }

        return false;
    }

    @Override
    public KeyValueCollectionPermission isPermittedMatchAll(
            CollectionPermission subjectAllCollection,
            KeyValueCollectionPermission matchAllCollection) {
        return matchAllCollection;
    }

    private List<KeyValuePermission> removePermissions(List<Permission> permissions,
            Set<String> keys) {
        return permissions.stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> !keys.contains(permission.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Extension policy for workspace security to achieve the following constraints.
     * <p>
     * - If roles and emails are set
     * -   If either roles or emails can be implied, removed both
     * - Otherwise, do nothing.
     * <p>
     * NOTE: no other permissions should be touched/removed.
     *
     * @param subject
     * @param matchOne
     * @return
     */
    @Override
    public KeyValueCollectionPermission isPermittedMatchOne(CollectionPermission subject,
            KeyValueCollectionPermission matchOne) {
        List<Permission> permissions = matchOne.getPermissionList();

        if (isSystem(subject.getPermissionList())) {
            return new KeyValueCollectionPermission(matchOne.getAction(),
                    removePermissions(permissions, CLAIMS));
        }

        boolean isImplied = CLAIMS.stream()
                .map(claim -> find(permissions, claim))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(subject::implies)
                .findFirst()
                .isPresent();

        if (isImplied) {
            return new KeyValueCollectionPermission(matchOne.getAction(),
                    removePermissions(permissions, CLAIMS));
        }

        return matchOne;
    }
}
