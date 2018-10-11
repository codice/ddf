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
import ddf.catalog.data.types.Security;
import ddf.security.SubjectIdentity;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;
import org.apache.shiro.authz.Permission;

public class AccessControlPolicyExtension implements PolicyExtension {

  // Marker interface to clean up method signature for predicate
  public interface SecurityPredicate
      extends BiFunction<Map<String, Set<String>>, Map<String, Set<String>>, Boolean> {}

  private static final Set<String> ACCESS_CONTROL_IMPLIED =
      new ImmutableSet.Builder<String>()
          .add(
              Core.METACARD_OWNER,
              SecurityAttributes.ACCESS_ADMINISTRATORS,
              SecurityAttributes.ACCESS_INDIVIDUALS,
              SecurityAttributes.ACCESS_GROUPS)
          .build();

  private final SecurityPredicate isOwner;

  private final SecurityPredicate hasAccessAdministrators;

  private final SecurityPredicate hasAccessGroups;

  private final SecurityPredicate hasAccessIndividuals;

  private final Predicate<Map<String, Set<String>>> isSystem;

  public AccessControlPolicyExtension(
      AccessControlSecurityConfiguration config, SubjectIdentity subjectIdentity) {
    isOwner = predicate(subjectIdentity.getIdentityAttribute(), Core.METACARD_OWNER);

    isSystem =
        (s) -> {
          Set<String> subject =
              s.getOrDefault(config.getSystemUserAttribute(), Collections.emptySet());
          return subject.contains(config.getSystemUserAttributeValue());
        };

    hasAccessAdministrators =
        predicate(subjectIdentity.getIdentityAttribute(), Security.ACCESS_ADMINISTRATORS);

    hasAccessGroups = predicate(Constants.ROLES_CLAIM_URI, SecurityAttributes.ACCESS_GROUPS);

    hasAccessIndividuals =
        predicate(subjectIdentity.getIdentityAttribute(), SecurityAttributes.ACCESS_INDIVIDUALS);
  }

  private Map<String, Set<String>> getPermissions(List<Permission> permissions) {
    return permissions
        .stream()
        .filter(p -> p instanceof KeyValuePermission)
        .map(p -> (KeyValuePermission) p)
        .collect(
            Collectors.toMap(
                KeyValuePermission::getKey, KeyValuePermission::getValues, Sets::union));
  }

  private SecurityPredicate predicate(String subjectAttribute, String metacardAttribute) {
    return (s, m) -> {
      Set<String> subject = s.getOrDefault(subjectAttribute, Collections.emptySet());
      Set<String> metacard = m.getOrDefault(metacardAttribute, Collections.emptySet());
      subject = subject.stream().map(String::toLowerCase).collect(Collectors.toSet());
      metacard = metacard.stream().map(String::toLowerCase).collect(Collectors.toSet());
      return !SetUtils.intersection(metacard, subject).isEmpty();
    };
  }

  private KeyValueCollectionPermission isPermitted(
      CollectionPermission s,
      KeyValueCollectionPermission match,
      KeyValueCollectionPermission allPerms) {
    Map<String, Set<String>> subject = getPermissions(s.getPermissionList());
    Map<String, Set<String>> metacard = getPermissions(allPerms.getPermissionList());

    // There is nothing to imply if the incoming permission set doesn't contain _ALL_ ACL attributes
    if (Collections.disjoint(metacard.keySet(), ACCESS_CONTROL_IMPLIED)) {
      return match; // Simply imply nothing early on (essentially a no-op in this extension)
    }

    // get all permissions implied by the subject, this function returns what permissions
    // to filter from the key-value permission collection
    Supplier<Set<String>> impliedPermissions =
        () -> {
          if (isSystem.test(subject) || isOwner.apply(subject, metacard)) {
            return metacard.keySet(); // all permissions are implied
          } else if (hasAccessAdministrators.apply(subject, metacard)
              || hasAccessIndividuals.apply(subject, metacard)
              || hasAccessGroups.apply(subject, metacard)) {
            return ACCESS_CONTROL_IMPLIED; // access control perms implied
          } else {
            return Collections.emptySet(); // nothing is implied
          }
        };

    // filter out all implied permissions
    Function<Set<String>, KeyValueCollectionPermission> filterPermissions =
        (implied) -> {
          List<KeyValuePermission> values =
              match
                  .getPermissionList()
                  .stream()
                  .map(p -> (KeyValuePermission) p)
                  .filter((permission) -> !implied.contains((permission).getKey()))
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
