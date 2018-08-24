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

import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AccessControlUtil {
  /**
   * Takes a {@link Metacard} and attribute name and returns a set of the corresponding values if
   * they exist
   */
  public static BiFunction<Metacard, String, Set<String>> attributeToSet =
      (metacard, attr) -> new HashSet<>(getValuesOrEmpty(metacard, attr));

  /**
   * It will be a pre-condition contain the full set of security attributes to enable access
   * control.
   */
  public static Predicate<Metacard> containsACLAttributes =
      (metacard) ->
          !isAnyObjectNull(
              metacard
                  .getMetacardType()
                  .getAttributeDescriptors()
                  .contains(Security.ACCESS_INDIVIDUALS),
              metacard.getMetacardType().getAttributeDescriptors().contains(Security.ACCESS_GROUPS),
              metacard
                  .getMetacardType()
                  .getAttributeDescriptors()
                  .contains(Security.ACCESS_ADMINISTRATORS));

  /**
   * Does a diff between the old set of {@link Metacard} access-administrators with the set to see
   * if anyone was added or removed.
   */
  public static BiFunction<Metacard, Metacard, Boolean> accessAdminHasChanged =
      (oldMetacard, newMetacard) ->
          !Sets.symmetricDifference(
                  attributeToSet.apply(oldMetacard, Security.ACCESS_ADMINISTRATORS),
                  attributeToSet.apply(newMetacard, Security.ACCESS_ADMINISTRATORS))
              .isEmpty();

  /**
   * Does a diff between the old set of {@link Metacard} access-individuals with the set to see if
   * anyone was added or removed.
   */
  public static BiFunction<Metacard, Metacard, Boolean> accessIndividualsHasChanged =
      (oldMetacard, newMetacard) ->
          !Sets.symmetricDifference(
                  attributeToSet.apply(oldMetacard, Security.ACCESS_INDIVIDUALS),
                  attributeToSet.apply(newMetacard, Security.ACCESS_INDIVIDUALS))
              .isEmpty();

  /**
   * Does a diff between the old set of {@link Metacard} access-groups with the set to see if a role
   * was added or removed.
   */
  public static BiFunction<Metacard, Metacard, Boolean> accessGroupsHasChanged =
      (oldMetacard, newMetacard) ->
          !Sets.symmetricDifference(
                  attributeToSet.apply(oldMetacard, Security.ACCESS_GROUPS),
                  attributeToSet.apply(newMetacard, Security.ACCESS_GROUPS))
              .isEmpty();

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
    return attributeToSet.apply(metacard, Security.ACCESS_INDIVIDUALS);
  }

  /**
   * @return List of access-groups from input {@link Metacard} if they exist, otherwise return an
   *     empty set
   */
  public static Set<String> getAccessGroups(Metacard metacard) {
    return attributeToSet.apply(metacard, Security.ACCESS_GROUPS);
  }

  /**
   * @return List of access-administrators from input {@link Metacard} if they exist, otherwise
   *     return an empty set
   */
  public static Set<String> getAccessAdministrators(Metacard metacard) {
    return attributeToSet.apply(metacard, Security.ACCESS_ADMINISTRATORS);
  }

  /** Sets owner value associated with a particular {@link Metacard} */
  public static Metacard setOwner(Metacard metacard, String email) {
    metacard.setAttribute(new AttributeImpl(Core.METACARD_OWNER, email));
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

  /** Creates a metacard from a map of desired attributes */
  public static Metacard metacardFromAttributes(Map<String, Serializable> attributes) {
    Metacard metacard = new MetacardImpl();
    attributes
        .entrySet()
        .stream()
        .forEach(
            entry -> metacard.setAttribute(new AttributeImpl(entry.getKey(), entry.getValue())));

    return metacard;
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
