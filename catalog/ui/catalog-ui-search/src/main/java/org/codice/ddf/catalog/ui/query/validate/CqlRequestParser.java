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
package org.codice.ddf.catalog.ui.query.validate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import java.io.IOException;
import java.util.Date;
import org.codice.ddf.catalog.ui.query.cql.CqlRequestImpl;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import spark.Request;

public class CqlRequestParser {

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new DateLongFormatTypeAdapter())
          .create();

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private EndpointUtil endpointUtil;

  public CqlRequestParser(
      CatalogFramework catalogFramework, FilterBuilder filterBuilder, EndpointUtil endpointUtil) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.endpointUtil = endpointUtil;
  }

  public QueryRequest parse(Request request) throws IOException {
    CqlRequestImpl cqlRequest =
        GSON.fromJson(endpointUtil.safeGetBody(request), CqlRequestImpl.class);
    return cqlRequest.createQueryRequest(catalogFramework.getId(), filterBuilder);
  }
}
