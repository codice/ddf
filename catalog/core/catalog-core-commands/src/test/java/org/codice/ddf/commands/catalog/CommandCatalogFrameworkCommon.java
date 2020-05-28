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
package org.codice.ddf.commands.catalog;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

public class CommandCatalogFrameworkCommon extends ConsoleOutputCommon {

  protected CatalogFramework givenCatalogFramework(List<Result> list)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    final CatalogFramework catalogFramework = mock(CatalogFramework.class);

    QueryResponse queryResponse = mock(QueryResponse.class);

    when(queryResponse.getResults()).thenReturn(list);

    when(catalogFramework.query(isA(QueryRequest.class))).thenReturn(queryResponse);
    return catalogFramework;
  }

  protected List<Result> getResultList(String... ids) {
    List<Result> results = new ArrayList<>();

    for (int i = 0; i < ids.length; i++) {

      String id = ids[i];
      MetacardImpl metacard = new MetacardImpl();
      metacard.setAttribute(
          new AttributeImpl(Core.CREATED, new DateTime(2010 + i, 3, 11, 14, 3).toDate()));
      metacard.setId(id);
      Result result = new ResultImpl(metacard);
      results.add(result);
    }

    return results;
  }

  protected List<Result> getEmptyResultList() {
    return new ArrayList<>();
  }
}
