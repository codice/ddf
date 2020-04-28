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
package org.codice.ddf.catalog.ui.enumeration;

import static org.apache.commons.lang.StringUtils.isBlank;

import com.google.common.collect.Sets;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class ExperimentalEnumerationExtractor {
  private final AttributeValidatorRegistry attributeValidatorRegistry;

  private final List<MetacardType> metacardTypes;

  private final List<AttributeInjector> attributeInjectors;

  /**
   * @param attributeValidatorRegistry
   * @param metacardTypes
   * @deprecated This constructor does not take into account injected attributes. The other
   *     constructor {@link #ExperimentalEnumerationExtractor(AttributeValidatorRegistry, List,
   *     List)} should be used.
   */
  @Deprecated
  public ExperimentalEnumerationExtractor(
      AttributeValidatorRegistry attributeValidatorRegistry, List<MetacardType> metacardTypes) {
    this(attributeValidatorRegistry, metacardTypes, Collections.emptyList());
  }

  /**
   * @param attributeValidatorRegistry validators to build enumerations from
   * @param metacardTypes metacard types to associate attributes with types
   * @param attributeInjectors injected attributes
   */
  public ExperimentalEnumerationExtractor(
      AttributeValidatorRegistry attributeValidatorRegistry,
      List<MetacardType> metacardTypes,
      List<AttributeInjector> attributeInjectors) {
    this.attributeValidatorRegistry = attributeValidatorRegistry;
    this.metacardTypes = metacardTypes;
    this.attributeInjectors = attributeInjectors;
  }

  public Map<String, Set<Map<String, String>>> getAttributeEnumerations(String attribute) {
    return attributeValidatorRegistry
        .getValidators(attribute)
        .stream()
        .map(av -> av.validate(new AttributeImpl(attribute, "null")))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(avr -> !avr.getSuggestedValues().isEmpty())
        .map(
            avr ->
                avr.getAttributeValidationViolations()
                    .stream()
                    .map(ValidationViolation::getAttributes)
                    .flatMap(Set::stream)
                    .distinct()
                    .collect(Collectors.toMap(o -> o, o -> avr.getSuggestedValues())))
        .reduce(
            (m1, m2) -> {
              m2.entrySet().forEach(e -> m1.merge(e.getKey(), e.getValue(), Sets::union));
              return m1;
            })
        .orElseGet(HashMap::new);
  }

  public Map<String, Set<Map<String, String>>> getEnumerations(@Nullable String metacardType) {
    if (isBlank(metacardType)) {
      metacardType = MetacardImpl.BASIC_METACARD.getName();
    }
    MetacardType type = getTypeFromName(metacardType);

    if (type == null) {
      return new HashMap<>();
    }

    type = applyInjectors(type, attributeInjectors);

    return type.getAttributeDescriptors()
        .stream()
        .map(ad -> this.getAttributeEnumerations(ad.getName()))
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Nullable
  private MetacardType getTypeFromName(String metacardType) {
    return metacardTypes
        .stream()
        .filter(mt -> mt.getName().equals(metacardType))
        .findFirst()
        .orElse(null);
  }

  private MetacardType applyInjectors(MetacardType original, List<AttributeInjector> injectors) {
    Metacard metacard = new MetacardImpl(original);
    for (AttributeInjector injector : injectors) {
      metacard = injector.injectAttributes(metacard);
    }
    return metacard.getMetacardType();
  }
}
