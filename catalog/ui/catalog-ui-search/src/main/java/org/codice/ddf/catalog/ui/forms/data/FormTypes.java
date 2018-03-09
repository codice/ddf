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
package org.codice.ddf.catalog.ui.forms.data;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Central definition for all the metacard types needed for forms support. */
public class FormTypes {
  private FormTypes() {}

  /**
   * {@link MetacardType} for handling customizable search forms on the UI, persisted by an {@link
   * QueryTemplateMetacardImpl}.
   */
  public static class Query extends CompoundMetacardType {
    public Query() {
      super(
          FormAttributes.Query.TAG,
          new CoreAttributes(),
          new SecurityAttributes(),
          new FormAttributes.Sharing(),
          new FormAttributes.Query());
    }
  }

  /**
   * {@link MetacardType} for handling levels of detail and result views, persisted by an {@link
   * ResultTemplateMetacardImpl}.
   */
  public static class Result extends CompoundMetacardType {
    public Result() {
      super(
          FormAttributes.Result.TAG,
          new CoreAttributes(),
          new SecurityAttributes(),
          new FormAttributes.Sharing(),
          new FormAttributes.Result());
    }
  }

  /** An immutable container to route hybrid type information without duplicating code. */
  private static class CompoundMetacardType implements MetacardType {
    private final String name;

    private final Set<AttributeDescriptor> descriptors;

    CompoundMetacardType(String name, MetacardType... types) {
      this.name = name;
      this.descriptors =
          ImmutableSet.copyOf(
              Arrays.stream(types)
                  .map(MetacardType::getAttributeDescriptors)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toSet()));
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
      return descriptors;
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
      return descriptors
          .stream()
          .filter(ad -> ad.getName().equals(attributeName))
          .findFirst()
          .orElse(null);
    }
  }
}
