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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.security.SubjectIdentity;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ShareableMetacardPolicyExtension implements PolicyExtension {

  private static final Set<String> SHARED_PERMISSIONS_IMPLIED =
      new ImmutableSet.Builder<String>()
          .addAll(Constants.SHAREABLE_TAGS.iterator())
          .add(
              Core.METACARD_OWNER,
              SecurityAttributes.ACCESS_INDIVIDUALS,
              SecurityAttributes.ACCESS_GROUPS)
          .build();

  private ShareableMetacardSecurityConfiguration config;

  private SubjectIdentity subjectIdentity;

  public ShareableMetacardPolicyExtension(
      ShareableMetacardSecurityConfiguration config, SubjectIdentity subjectIdentity) {
    this.config = config;
    this.subjectIdentity = subjectIdentity;
  }

  private List<KeyValuePermission> getPermissions(KeyValueCollectionPermission collection) {
    return collection
        .getPermissionList()
        .stream()
        .filter(p -> p instanceof KeyValuePermission)
        .map(p -> (KeyValuePermission) p)
        .collect(Collectors.toList());
  }

  private Map<String, Set<String>> groupPermissionsByKey(List<KeyValuePermission> permissions) {
    return permissions
        .stream()
        .collect(
            Collectors.toMap(
                KeyValuePermission::getKey, KeyValuePermission::getValues, Sets::union));
  }

  private Predicate<CollectionPermission> system() {
    Set<String> values = ImmutableSet.of(config.getSystemUserAttributeValue());
    KeyValuePermission perm = new KeyValuePermission(config.getSystemUserAttribute(), values);
    return (subject) -> subject.implies(perm);
  }

  private Predicate<CollectionPermission> predicate(
      Map<String, Set<String>> permissions, String k1, String k2) {
    if (!permissions.containsKey(k1)) {
      return (subject) -> false;
    }
    return (subject) -> subject.implies(new KeyValuePermission(k2, permissions.get(k1)));
  }

  private Predicate<CollectionPermission> owner(Map<String, Set<String>> permissions) {
    return predicate(permissions, Core.METACARD_OWNER, subjectIdentity.getIdentityAttribute());
  }

  private Predicate<CollectionPermission> individuals(Map<String, Set<String>> permissions) {
    return predicate(
        permissions, SecurityAttributes.ACCESS_INDIVIDUALS, subjectIdentity.getIdentityAttribute());
  }

  private Predicate<CollectionPermission> groups(Map<String, Set<String>> permissions) {
    return predicate(permissions, SecurityAttributes.ACCESS_GROUPS, Constants.ROLES_CLAIM_URI);
  }

  private KeyValueCollectionPermission isPermitted(
      CollectionPermission subject,
      KeyValueCollectionPermission match,
      KeyValueCollectionPermission allPerms) {
    List<KeyValuePermission> permissions = getPermissions(allPerms);
    Map<String, Set<String>> grouped = groupPermissionsByKey(permissions);
    if (Collections.disjoint(grouped.keySet(), SHARED_PERMISSIONS_IMPLIED)) {
      return match; // ignore all but shareable permissions
    }

    Predicate<CollectionPermission> isSystem = system();
    Predicate<CollectionPermission> isOwner = owner(grouped);
    Predicate<CollectionPermission> hasAccessIndividuals = individuals(grouped);
    Predicate<CollectionPermission> hasAccessGroups = groups(grouped);

    // get all permissions implied by the subject, this function returns what permissions
    // to filter from the key-value permission collection
    Supplier<Set<String>> impliedPermissions =
        () -> {
          if (isSystem.test(subject) || isOwner.test(subject)) {
            return grouped.keySet(); // all permissions are implied
          } else if (hasAccessIndividuals.test(subject) || hasAccessGroups.test(subject)) {
            return SHARED_PERMISSIONS_IMPLIED;
          } else {
            return Constants.SHAREABLE_TAGS;
          }
        };

    // filter out all implied permissions
    Function<Set<String>, KeyValueCollectionPermission> filterPermissions =
        (implied) -> {
          List<KeyValuePermission> values =
              permissions
                  .stream()
                  .filter((permission) -> !implied.contains(permission.getKey()))
                  .collect(Collectors.toList());

          return new KeyValueCollectionPermission(match.getAction(), values);
        };

    return filterPermissions.apply(impliedPermissions.get());
  }

  @Override
  public KeyValueCollectionPermission isPermittedMatchAll(
      CollectionPermission subject,
      KeyValueCollectionPermission matchAll,
      KeyValueCollectionPermission allPermissionsCollection) {
    return isPermitted(subject, matchAll, allPermissionsCollection);
  }

  @Override
  public KeyValueCollectionPermission isPermittedMatchOne(
      CollectionPermission subject,
      KeyValueCollectionPermission matchOne,
      KeyValueCollectionPermission allPermissionsCollection) {
    return isPermitted(subject, matchOne, allPermissionsCollection);
  }
}
