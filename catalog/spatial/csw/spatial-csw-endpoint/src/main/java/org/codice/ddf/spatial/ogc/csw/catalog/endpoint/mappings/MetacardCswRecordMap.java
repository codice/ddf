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

import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswRecordMap;
import org.xml.sax.helpers.NamespaceSupport;

public class MetacardCswRecordMap implements CswRecordMap {

  @Override
  public String getProperty(String propertyName, NamespaceSupport context) {
    return DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(propertyName, context);
  }

  @Override
  public String getProperty(String propertyName) {
    return DefaultCswRecordMap.getDefaultMetacardFieldFor(propertyName);
  }

  @Override
  public boolean hasProperty(String propertyName, NamespaceSupport context) {
    return DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyName, context);
  }

  @Override
  public boolean hasProperty(String propertyName) {
    return DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(propertyName);
  }
}
