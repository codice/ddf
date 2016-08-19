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

import static java.util.stream.Stream.concat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;

public class WorkspacePolicyExtension implements PolicyExtension {

    private Stream<KeyValuePermission> injectedPermissions() {
        return ImmutableList.of(new KeyValuePermission("admin", ImmutableSet.of("admin")))
                .stream();
    }

    private Map<String, String> keyMapping = ImmutableMap.of("admin",
            Constants.ROLES_CLAIM_URI,
            Core.METACARD_OWNER,
            Constants.EMAIL_ADDRESS_CLAIM_URI,
            SecurityAttributes.ACCESS_INDIVIDUALS,
            Constants.EMAIL_ADDRESS_CLAIM_URI,
            SecurityAttributes.ACCESS_GROUPS,
            Constants.ROLES_CLAIM_URI);

    private static Predicate<KeyValuePermission> byKeys(Set<String> keys) {
        return permission -> keys.contains(permission.getKey());
    }

    private static Function<KeyValuePermission, KeyValuePermission> remapKeys(
            Map<String, String> mapping) {
        return permission -> new KeyValuePermission(mapping.get(permission.getKey()),
                permission.getValues());
    }

    @Override
    public KeyValueCollectionPermission isPermittedMatchAll(CollectionPermission subject,
            KeyValueCollectionPermission match) {

        List<KeyValuePermission> permissions = match.getPermissionList()
                .stream()
                .filter(p -> p instanceof KeyValuePermission)
                .map(p -> (KeyValuePermission) p)
                .collect(Collectors.toList());

        Stream<KeyValuePermission> stream = concat(injectedPermissions(), permissions.stream());

        boolean matched = stream.filter(byKeys(keyMapping.keySet()))
                .map(remapKeys(keyMapping))
                .filter(subject::implies)
                .findFirst()
                .isPresent();

        return new KeyValueCollectionPermission(match.getAction(),
                permissions.stream()
                        .filter(matched ? byKeys(keyMapping.keySet()).negate() : p -> true)
                        .collect(Collectors.toList()));
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
        return matchOne;
    }
}
