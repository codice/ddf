/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings;

import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

public class SourceIdFilterVisitor extends DuplicatingFilterVisitor {
  private List<String> sourceIds = new ArrayList<>();

  @Override
  public Object visit(PropertyIsEqualTo filter, Object extraData) {
    if (filter.getExpression1() instanceof PropertyName) {
      String propertyName = ((PropertyName) filter.getExpression1()).getPropertyName();
      if (StringUtils.equals(Core.SOURCE_ID, propertyName)) {
        Literal sourceId = (Literal) filter.getExpression2();
        sourceIds.add((String) sourceId.getValue());
        return null;
      }
    }

    return super.visit(filter, extraData);
  }

  public List<String> getSourceIds() {
    return sourceIds;
  }
}
