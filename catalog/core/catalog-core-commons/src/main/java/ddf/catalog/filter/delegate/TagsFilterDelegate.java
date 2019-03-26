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
package ddf.catalog.filter.delegate;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagsFilterDelegate extends SimpleFilterDelegate<Boolean> {

  public static final String NULL_TAGS = Core.METACARD_TAGS + "_NULL";

  private Set<String> tags;

  public TagsFilterDelegate() {}

  public TagsFilterDelegate(String type) {
    this(Collections.singleton(type), false);
  }

  public TagsFilterDelegate(Set<String> types) {
    this(types, false);
  }

  public TagsFilterDelegate(Set<String> types, boolean wildcardMatches) {
    this.tags = new HashSet<>(types);
    if (wildcardMatches) {
      this.tags.add(FilterDelegate.WILDCARD_CHAR);
    }
  }

  @Override
  public <S> Boolean defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    return false;
  }

  @Override
  public Boolean and(List<Boolean> operands) {
    return operands.stream().anyMatch(op -> op);
  }

  @Override
  public Boolean or(List<Boolean> operands) {
    return operands.stream().allMatch(op -> op);
  }

  @Override
  public Boolean not(Boolean operand) {
    return tags == null ? operand : false;
  }

  @Override
  public Boolean propertyIsEqualTo(String propertyName, String pattern, boolean isCaseSensitive) {
    return propertyName.equals(Core.METACARD_TAGS) && (tags == null || tags.contains(pattern));
  }

  @Override
  public Boolean propertyIsNull(String propertyName) {
    return propertyName.equals(Core.METACARD_TAGS)
        && (tags == null
            || tags.contains(NULL_TAGS)
            || tags.contains(FilterDelegate.WILDCARD_CHAR));
  }

  @Override
  public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
    return propertyName.equals(Core.METACARD_TAGS) && (tags == null || tags.contains(pattern));
  }
}
