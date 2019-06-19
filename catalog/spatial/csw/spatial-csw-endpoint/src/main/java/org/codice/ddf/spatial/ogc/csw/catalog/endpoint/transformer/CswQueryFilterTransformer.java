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

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.transform.QueryFilterTransformer;
import java.io.Serializable;
import java.util.Map;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.Filter;

public class CswQueryFilterTransformer implements QueryFilterTransformer {

  private CswRecordMapperFilterVisitor filterVisitor;

  public CswQueryFilterTransformer(CswRecordMap recordMap, AttributeRegistry attributeRegistry) {
    filterVisitor = new CswRecordMapperFilterVisitor(recordMap, attributeRegistry);
  }

  @Override
  public QueryRequest transform(QueryRequest queryRequest, Map<String, Serializable> properties) {
    Query query = queryRequest.getQuery();
    Filter filter = (Filter) query.accept(filterVisitor, new FilterFactoryImpl());
    Query transformedQuery = query.newInstanceWithFilter(filter);

    return new QueryRequestImpl(
        transformedQuery,
        queryRequest.isEnterprise(),
        queryRequest.getSourceIds(),
        queryRequest.getProperties());
  }
}
