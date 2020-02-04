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
package org.codice.ddf.catalog.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.query.utility.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.utility.CqlRequest;
import org.codice.ddf.catalog.ui.query.utility.CqlResult;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class CqlQueriesImplTest {

  private CqlQueriesImpl cqlQueryUtil;

  private FilterBuilder filterBuilderMock;

  private FilterAdapter filterAdapterMock;

  private ActionRegistry actionRegistryMock;

  private QueryResponse responseMock;

  CatalogFramework catalogFrameworkMock;

  @Before
  public void setUp() throws Exception {

    catalogFrameworkMock = mock(CatalogFramework.class);

    Filter filterMock = mock(Filter.class);

    AttributeBuilder attributeBuilderMock = mock(AttributeBuilder.class);

    ConfigurationApplication configurationApplicationMock = mock(ConfigurationApplication.class);

    ContextualExpressionBuilder contextualExpressionBuilderMock =
        mock(ContextualExpressionBuilder.class);

    filterBuilderMock = mock(FilterBuilder.class);
    filterAdapterMock = mock(FilterAdapter.class);
    actionRegistryMock = mock(ActionRegistry.class);
    responseMock = mock(QueryResponse.class);

    when(filterBuilderMock.attribute(any())).thenReturn(attributeBuilderMock);
    when(attributeBuilderMock.is()).thenReturn(attributeBuilderMock);
    when(attributeBuilderMock.like()).thenReturn(contextualExpressionBuilderMock);
    when(contextualExpressionBuilderMock.text(anyString())).thenReturn(filterMock);
    when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);
    when(configurationApplicationMock.getMaximumUploadSize()).thenReturn(1 << 20);

    cqlQueryUtil =
        new CqlQueriesImpl(
            catalogFrameworkMock, filterBuilderMock, filterAdapterMock, actionRegistryMock);
  }

  private CqlRequest generateCqlRequest(int count) {
    CqlRequest cqlRequest = new CqlRequest();
    cqlRequest.setCount(count);
    cqlRequest.setCql("anyText ILIKE '*'");

    return cqlRequest;
  }

  @Test
  public void testHitCountOnlyQuery() throws Exception {
    long hitCount = 12L;
    when(responseMock.getResults()).thenReturn(Collections.emptyList());
    when(responseMock.getHits()).thenReturn(hitCount);
    when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);

    CqlQueryResponse cqlQueryResponse = cqlQueryUtil.executeCqlQuery(generateCqlRequest(0));
    List<CqlResult> results = cqlQueryResponse.getResults();
    assertThat(results, hasSize(0));
    assertThat(cqlQueryResponse.getQueryResponse().getHits(), is(hitCount));
  }
}
