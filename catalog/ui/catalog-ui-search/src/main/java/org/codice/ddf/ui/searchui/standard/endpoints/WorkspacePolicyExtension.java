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
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;
import org.codice.ddf.security.common.Security;

import ddf.security.SubjectUtils;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;

public class WorkspacePolicyExtension implements PolicyExtension {

    public static final String EMAIL_ADDRESS_CLAIM_URI =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

    public static final String ROLES_CLAIM_URI =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    /**
     * Are workspaces private? A private workspace only allows access to owners and subjects
     * that have a given role.
     */
    private boolean isWorkspacePrivate = true;

    /**
     * Determine if a permission list has a role claim.
     *
     * @param permissions
     * @return
     */
    private static Optional<KeyValuePermission> find(List<Permission> permissions, String name) {
        return permissions.stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> permission.getKey()
                        .equals(name))
                .findFirst();
    }

    public void setIsWorkspacePrivate(boolean isWorkspacePrivate) {
        this.isWorkspacePrivate = isWorkspacePrivate;
    }

    private Predicate<KeyValuePermission> filterPermissions(boolean areRolesSet,
            boolean isSystemSubject) {
        return (permission) -> {
            String key = permission.getKey();

            if (isSystemSubject || !isWorkspacePrivate) {
                return !EMAIL_ADDRESS_CLAIM_URI.equals(key) && !ROLES_CLAIM_URI.equals(key);
            }

            if (isWorkspacePrivate && areRolesSet) {
                return !EMAIL_ADDRESS_CLAIM_URI.equals(key);
            }

            return true;
        };
    }

    private String getSystemEmail() {
        return SubjectUtils.getEmailAddress(Security.getInstance()
                .getSystemSubject());
    }

    private boolean isSystem(List<Permission> permissions) {
        Optional<KeyValuePermission> subjectEmail = find(permissions, EMAIL_ADDRESS_CLAIM_URI);

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

    /**
     * Extension policy for workspace security to achieve the following constraints.
     * <p>
     * If workspaces are private
     * -   If roles are set
     * -       remove owner permission but keep roles
     * -   Else
     * -       Keep owner permission
     * Else
     * -   remove both owner and roles permissions
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

        boolean areRolesSet = find(permissions, ROLES_CLAIM_URI).isPresent();
        boolean isSystemSubject = isSystem(subject.getPermissionList());

        List<KeyValuePermission> filteredPermissions = permissions.stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(filterPermissions(areRolesSet, isSystemSubject))
                .collect(Collectors.toList());

        return new KeyValueCollectionPermission(matchOne.getAction(), filteredPermissions);
    }
}
