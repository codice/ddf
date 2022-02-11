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

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import java.util.List;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

public interface CswQueryFactory {

  QueryRequest getQueryById(List<String> ids);

  QueryRequest getQuery(GetRecordsType request) throws CswException;

  QueryRequest getQuery(QueryConstraintType constraint, String typeName) throws CswException;

  QueryRequest updateQueryRequestTags(QueryRequest queryRequest, String schema)
      throws UnsupportedQueryException;

  void setSchemaToTagsMapping(String[] schemaToTagsMappingStrings);

  void setAttributeRegistry(AttributeRegistry attributeRegistry);

  void setQueryFilterTransformerProvider(
      QueryFilterTransformerProvider queryFilterTransformerProvider);
}
