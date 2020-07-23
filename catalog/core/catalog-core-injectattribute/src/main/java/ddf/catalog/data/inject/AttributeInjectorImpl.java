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
package ddf.catalog.data.inject;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AttributeInjectorImpl implements AttributeInjector {
  private final AttributeRegistry attributeRegistry;

  private List<InjectableAttribute> injectableAttributes = new ArrayList<>();

  public AttributeInjectorImpl(AttributeRegistry attributeRegistry) {
    this.attributeRegistry = attributeRegistry;
  }

  public void setInjectableAttributes(List<InjectableAttribute> injectableAttributes) {
    this.injectableAttributes = injectableAttributes;
  }

  private Set<String> injectableAttributes(String metacardTypeName) {
    return injectableAttributes.stream()
        .filter(
            injectableAttribute ->
                isInjected(injectableAttribute.metacardTypes(), metacardTypeName))
        .map(InjectableAttribute::attribute)
        .collect(toSet());
  }

  private boolean isInjected(Set<String> metacardTypes, String metacardTypeName) {
    return metacardTypes.isEmpty() || metacardTypes.contains(metacardTypeName);
  }

  @Override
  public MetacardType injectAttributes(MetacardType original) {
    notNull(original, "The metacard type cannot be null.");

    final String metacardTypeName = original.getName();

    final Set<AttributeDescriptor> injectAttributes =
        injectableAttributes(metacardTypeName).stream()
            .map(attributeRegistry::lookup)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

    if (injectAttributes.isEmpty()) {
      return original;
    } else {
      return new MetacardTypeImpl(original.getName(), original, injectAttributes);
    }
  }

  @Override
  public Metacard injectAttributes(Metacard original) {
    notNull(original, "The metacard cannot be null.");

    final MetacardType newMetacardType = injectAttributes(original.getMetacardType());

    if (newMetacardType == original.getMetacardType()) {
      return original;
    } else {
      return changeMetacardType(original, newMetacardType);
    }
  }

  private Metacard changeMetacardType(Metacard original, MetacardType newMetacardType) {
    MetacardImpl newMetacard = new MetacardImpl(original);
    newMetacard.setType(newMetacardType);
    newMetacard.setSourceId(original.getSourceId());
    return newMetacard;
  }
}
