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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.xml.sax.helpers.NamespaceSupport;

public interface DefaultCswRecordMap {
  /** NOTE: This is a {@link CaseInsensitiveMap}. */
  Map<String, String> getCswToMetacardAttributeNames();

  String getDefaultMetacardFieldFor(QName cswField);

  boolean hasDefaultMetacardFieldFor(QName cswField);

  boolean hasDefaultMetacardFieldFor(String cswField);

  String getDefaultMetacardFieldFor(String cswField);

  boolean hasDefaultMetacardFieldForPrefixedString(String name);

  boolean hasDefaultMetacardFieldForPrefixedString(
      String propertyName, NamespaceSupport namespaceSupport);

  String getDefaultMetacardFieldForPrefixedString(String name);

  String getDefaultMetacardFieldForPrefixedString(
      String propertyName, NamespaceSupport namespaceSupport);

  List<QName> getCswFieldsFor(String metacardField);

  Map<String, String> getPrefixToUriMapping();
}
