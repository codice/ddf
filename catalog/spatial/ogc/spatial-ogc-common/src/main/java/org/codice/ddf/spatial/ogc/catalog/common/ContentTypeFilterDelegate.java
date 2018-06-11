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
package org.codice.ddf.spatial.ogc.catalog.common;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Extracts list of content types from filter
 *
 * @author Jason Smith
 */
public class ContentTypeFilterDelegate extends SimpleFilterDelegate<List<ContentType>> {

  @Override
  public <S> List<ContentType> defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    return Collections.<ContentType>emptyList();
  }

  // Logical operators
  @Override
  public List<ContentType> and(List<List<ContentType>> operands) {
    return combineLists(operands);
  }

  @Override
  public List<ContentType> or(List<List<ContentType>> operands) {
    return combineLists(operands);
  }

  // PropertyIsLike
  @Override
  public List<ContentType> propertyIsLike(
      String propertyName, String pattern, boolean isCaseSensitive) {
    return propertyIsEqualTo(propertyName, pattern, isCaseSensitive);
  }

  // PropertyIsEqualTo
  @Override
  public List<ContentType> propertyIsEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    List<ContentType> types = null;
    verifyInputData(propertyName, literal);

    if (propertyName.equalsIgnoreCase(Metacard.CONTENT_TYPE)) {

      ContentType type = new ContentTypeImpl(literal, "");
      types = new ArrayList<ContentType>();
      types.add(type);
    } else {
      types = Collections.<ContentType>emptyList();
    }

    return types;
  }

  private void verifyInputData(String propertyName, String pattern) {
    if (StringUtils.isEmpty(propertyName) || StringUtils.isEmpty(pattern)) {
      throw new UnsupportedOperationException(
          "PropertyName and Literal value is required for search.");
    }
  }

  private List<ContentType> combineLists(List<List<ContentType>> lists) {
    List<ContentType> combinedTypes = new ArrayList<ContentType>();
    if (lists != null) {
      for (List<ContentType> list : lists) {
        combinedTypes.addAll(list);
      }
    }
    return combinedTypes;
  }
}
