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
package org.codice.ddf.catalog.ui.query.utility;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface CsvTransform {
  Set<String> getHiddenFields();

  void setHiddenFields(Set<String> hiddenFields);

  List<String> getColumnOrder();

  void setColumnOrder(List<String> columnOrder);

  Map<String, String> getColumnAliasMap();

  void setColumnAliasMap(Map<String, String> columnAliasMap);

  List<Map<String, Object>> getMetacards();

  void setMetacards(List<Map<String, Object>> metacards);

  boolean isApplyGlobalHidden();

  void setApplyGlobalHidden(boolean applyGlobalHidden);

  List<Metacard> getTransformedMetacards(
      List<MetacardType> metacardTypes, AttributeRegistry attributeRegistry);
}
