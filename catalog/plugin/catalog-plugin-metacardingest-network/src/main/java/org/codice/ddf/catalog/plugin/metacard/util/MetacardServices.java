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
package org.codice.ddf.catalog.plugin.metacard.util;

import static org.apache.commons.lang.Validate.notNull;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Support and bulk operations for {@link Metacard}s. Contains state relevant to the provided
 * services.
 */
public class MetacardServices {

  private final List<MetacardType> systemMetacardTypes;

  /** Initializes the services with an empty list of system {@link MetacardType}s. */
  public MetacardServices() {
    this.systemMetacardTypes = new ArrayList<>();
  }

  /**
   * Initializes the services with the provided list of {@link MetacardType}s.
   *
   * @param systemMetacardTypes The list of metacard types to use.
   */
  public MetacardServices(List<MetacardType> systemMetacardTypes) {
    this.systemMetacardTypes = systemMetacardTypes;
  }

  /**
   * Returns a new list of new {@link Metacard}s created from the {@param metacards} collection and
   * applies each attribute in the {@param attributeMap} only if the attribute is not already set on
   * the {@link Metacard}. That is, the attribute must be {@code null} or not in the {@link
   * Metacard}'s map. Appropriate {@link AttributeDescriptor}s are injected into a new {@link
   * MetacardType} for each new {@link Metacard} created.
   *
   * @param metacards The list of metacards to attempt to add the attributes to. Can be empty.
   * @param attributeMap The map of attributes to attempt to add. Attributes already set on any
   *     given {@link Metacard} will not be changed. For multi-valued attributes, the value string
   *     should separate different entities with commas.
   * @param attributeFactory The factory to use to create attributes.
   */
  public List<Metacard> setAttributesIfAbsent(
      List<Metacard> metacards,
      Map<String, String> attributeMap,
      AttributeFactory attributeFactory) {

    notNull(metacards, "The list of metacards cannot be null");
    notNull(attributeMap, "The map of new attributes cannot be null");
    notNull(attributeFactory, "The attribute factory cannot be null");

    if (metacards.isEmpty() || attributeMap.isEmpty()) {
      return metacards;
    }

    Map<String, AttributeDescriptor> systemAndMetacardDescriptors =
        getUniqueSystemAndMetacardDescriptors(metacards);

    return metacards.stream()
        .map(
            metacard -> {
              Set<AttributeDescriptor> relevantDescriptors =
                  attributeMap.keySet().stream()
                      .filter(key -> metacard.getAttribute(key) == null)
                      .map(systemAndMetacardDescriptors::get)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toSet());
              return createNewMetacardWithInjectedAttributes(
                  metacard,
                  relevantDescriptors.stream()
                      .map(
                          descriptor ->
                              attributeFactory.createAttribute(
                                  descriptor, attributeMap.get(descriptor.getName())))
                      .collect(Collectors.toList()),
                  relevantDescriptors);
            })
        .collect(Collectors.toList());
  }

  /**
   * Helper method to combine all system-recognized {@link AttributeDescriptor}s with any new ones
   * on the given list of {@link Metacard}s without repeats.
   *
   * @param metacards List of metacards whose attribute descriptors should be added to the map.
   * @return A map of all system-recognized descriptors plus any new ones introduced by the
   *     metacards.
   */
  private Map<String, AttributeDescriptor> getUniqueSystemAndMetacardDescriptors(
      List<Metacard> metacards) {
    List<MetacardType> systemMetacardTypesCopy = ImmutableList.copyOf(systemMetacardTypes);
    return Stream.concat(
            systemMetacardTypesCopy.stream()
                .map(MetacardType::getAttributeDescriptors)
                .flatMap(Set::stream)
                .filter(Objects::nonNull),
            metacards.stream()
                .map(Metacard::getMetacardType)
                .map(MetacardType::getAttributeDescriptors)
                .flatMap(Set::stream)
                .filter(Objects::nonNull))
        .collect(
            Collectors.toMap(
                AttributeDescriptor::getName,
                Function.identity(),
                (oldValue, newValue) -> oldValue));
  }

  /**
   * Creates a new metacard from the original with all original attributes plus the new ones given
   * in the list. A new metacard type is created for this new metacard using the original type and
   * the given descriptors.
   */
  private Metacard createNewMetacardWithInjectedAttributes(
      Metacard originalMetacard, List<Attribute> attributes, Set<AttributeDescriptor> descriptors) {
    MetacardImpl newMetacard;
    if (descriptors.isEmpty()) {
      newMetacard = new MetacardImpl(originalMetacard);
    } else {
      MetacardTypeImpl injectedAttributeType =
          new MetacardTypeImpl(
              originalMetacard.getMetacardType().getName(),
              originalMetacard.getMetacardType(),
              descriptors);
      newMetacard = new MetacardImpl(originalMetacard, injectedAttributeType);
    }
    attributes.forEach(newMetacard::setAttribute);
    return newMetacard;
  }
}
