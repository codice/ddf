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
package org.codice.ddf.spatial.ogc.csw.catalog.api;

import ddf.catalog.transform.QueryFilterTransformerProvider;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.QueryType;

public interface CswXmlValidator {
  /**
   * Verifies that that if types are passed, then they are fully qualified
   *
   * @param types List of QNames representing types
   */
  void validateFullyQualifiedTypes(List<QName> types) throws CswException;

  void validateOutputSchema(String schema, TransformerManager schemaTransformerManager)
      throws CswException;

  void validateVersion(String versions) throws CswException;

  void validateOutputFormat(String format, TransformerManager mimeTypeTransformerManager)
      throws CswException;

  void validateSchemaLanguage(String schemaLanguage) throws CswException;

  void validateTypeNameToNamespaceMappings(
      String typeNames, String namespaces, Map<String, String> namespacePrefixToUriMappings)
      throws CswException;

  void setQueryFilterTransformerProvider(
      QueryFilterTransformerProvider queryFilterTransformerHelper);

  void validateTypes(List<QName> types, String version) throws CswException;

  void validateElementNames(QueryType query) throws CswException;
}
