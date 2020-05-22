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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AccessControlUtil {

  // Should not be instantiated
  private AccessControlUtil() {}

  /**
   * Takes a {@link Metacard} and attribute name and returns a set of the corresponding values if
   * they exist
   */
  public static final BiFunction<Metacard, String, Set<String>> ATTRIBUTE_TO_SET =
      (metacard, attr) -> new HashSet<>(getValuesOrEmpty(metacard, attr));

  private static final BiFunction<Metacard, String, Boolean> METACARD_DESCRIPTORS_CONTAIN =
      (metacard, attr) -> metacard.getAttribute(attr) != null;

  /**
   * It will be a pre-condition contain the full set of security attributes to enable access
   * control.
   */
  public static final Predicate<Metacard> CONTAINS_ACL_ATTRIBUTES =
      metacard ->
          METACARD_DESCRIPTORS_CONTAIN.apply(metacard, Security.ACCESS_ADMINISTRATORS)
              || METACARD_DESCRIPTORS_CONTAIN.apply(metacard, Security.ACCESS_GROUPS)
              || METACARD_DESCRIPTORS_CONTAIN.apply(metacard, Security.ACCESS_INDIVIDUALS);

  /**
   * Does a diff between the old set of {@link Metacard} owners with the new set to see if anyone
   * was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> OWNER_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Core.METACARD_OWNER);

  /**
   * Does a diff between the old set of {@link Metacard} access-administrators with the new set to
   * see if anyone was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> ACCESS_ADMIN_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Security.ACCESS_ADMINISTRATORS);

  /**
   * Does a diff between the old set of {@link Metacard} access-individuals with the new set to see
   * if anyone was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> ACCESS_INDIVIDUALS_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Security.ACCESS_INDIVIDUALS);

  /**
   * Does a diff between the old set of {@link Metacard} access-individuals_read with the new set to
   * see if anyone was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> ACCESS_INDIVIDUALS_READ_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Security.ACCESS_INDIVIDUALS_READ);

  /**
   * Does a diff between the old set of {@link Metacard} access-groups with the new set to see if a
   * role was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> ACCESS_GROUPS_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Security.ACCESS_GROUPS);

  /**
   * Does a diff between the old set of {@link Metacard} access-groups_read with the new set to see
   * if a role was added or removed.
   */
  public static final BiFunction<Metacard, Metacard, Boolean> ACCESS_GROUPS_READ_HAS_CHANGED =
      (oldMetacard, newMetacard) ->
          attributeHasChanged(oldMetacard, newMetacard, Security.ACCESS_GROUPS_READ);

  /**
   * Does a diff between the old set of values on the specified {@link Metacard} {@link Attribute}
   * with the new set to see if a value was added, removed, or otherwise altered.
   *
   * @param oldMetacard the old version of the metacard that contains the attribute to check.
   * @param newMetacard the new version of the metacard that contains the attribute to check.
   * @param attribute the name of the attribute to check.
   * @return true if the given attribute changed across versions of the metacard, false otherwise.
   */
  public static boolean attributeHasChanged(
      Metacard oldMetacard, Metacard newMetacard, String attribute) {
    return !Sets.symmetricDifference(
            ATTRIBUTE_TO_SET.apply(oldMetacard, attribute),
            ATTRIBUTE_TO_SET.apply(newMetacard, attribute))
        .isEmpty();
  }

  /**
   * Pulls off a list of values associated with a particular attribute on a {@link Metacard}
   *
   * @param metacard that you want to pull values from
   * @param attributeName that you are targeting to pull off in the input {@link Metacard}
   * @return Populated list of string values if attribute was found, otherwise empty list
   */
  public static List<String> getValuesOrEmpty(Metacard metacard, String attributeName) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute != null) {
      return attribute.getValues().stream().map(String::valueOf).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /**
   * @return List of access-individuals from input {@link Metacard} if they exist, otherwise return
   *     an empty set
   */
  public static Set<String> getAccessIndividuals(Metacard metacard) {
    return ATTRIBUTE_TO_SET.apply(metacard, Security.ACCESS_INDIVIDUALS);
  }

  /**
   * @return List of access-individuals with read-only perms from input {@link Metacard} if they
   *     exist, otherwise return an empty set
   */
  public static Set<String> getAccessReadOnlyIndividuals(Metacard metacard) {
    return ATTRIBUTE_TO_SET.apply(metacard, Security.ACCESS_INDIVIDUALS_READ);
  }

  /**
   * @return List of access-groups from input {@link Metacard} if they exist, otherwise return an
   *     empty set
   */
  public static Set<String> getAccessGroups(Metacard metacard) {
    return ATTRIBUTE_TO_SET.apply(metacard, Security.ACCESS_GROUPS);
  }

  /**
   * @return List of access-groups with read-only perms from input {@link Metacard} if they exist,
   *     otherwise return an empty set
   */
  public static Set<String> getAccessReadOnlyGroups(Metacard metacard) {
    return ATTRIBUTE_TO_SET.apply(metacard, Security.ACCESS_GROUPS_READ);
  }

  /**
   * @return List of access-administrators from input {@link Metacard} if they exist, otherwise
   *     return an empty set
   */
  public static Set<String> getAccessAdministrators(Metacard metacard) {
    return ATTRIBUTE_TO_SET.apply(metacard, Security.ACCESS_ADMINISTRATORS);
  }

  /** Sets owner value associated with a particular {@link Metacard} */
  public static Metacard setOwner(Metacard metacard, String subjectIdentity) {
    metacard.setAttribute(new AttributeImpl(Core.METACARD_OWNER, subjectIdentity));
    return metacard;
  }

  /** Retrieves owner value associated with a particular {@link Metacard} */
  public static String getOwner(Metacard metacard) {
    List<String> values = getValuesOrEmpty(metacard, Core.METACARD_OWNER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  /** Retrieves owner value associated with a particular {@link Metacard} */
  public static Set<String> getOwnerOrEmptySet(Metacard metacard) {
    List<String> values = getValuesOrEmpty(metacard, Core.METACARD_OWNER);
    if (!values.isEmpty()) {
      return ImmutableSet.of(values.get(0));
    }
    return Collections.emptySet();
  }

  /** Given a variable number of arguments, checks if any are null */
  public static boolean isAnyObjectNull(Object... objects) {
    for (Object o : objects) {
      if (o == null) {
        return true;
      }
    }
    return false;
  }
}
