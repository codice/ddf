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
package org.codice.ddf.catalog.ui.query.cql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.ActionRegistry;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Collections;
import org.junit.Test;

public class CqlResultImplTest {

  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  @Test
  public void testInvalidInfiniteDistance() {
    distanceCheck(Double.POSITIVE_INFINITY, null);
  }

  @Test
  public void testInvalidNegativeDistance() {
    distanceCheck(-1.0, null);
  }

  @Test
  public void testValidDistance() {
    distanceCheck(123.0, 123.0);
  }

  private void distanceCheck(Double input, Double output) {
    MetacardImpl metacard = new MetacardImpl();
    ResultImpl result = new ResultImpl(metacard);
    result.setDistanceInMeters(input);
    ActionRegistry actionRegistry = mock(ActionRegistry.class);
    when(actionRegistry.list(any())).thenReturn(Collections.emptyList());
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(filterBuilder.attribute("test").equalTo().text("value")));
    CqlResultImpl cqlResult =
        new CqlResultImpl(result, null, request, false, filterAdapter, actionRegistry);
    assertThat(cqlResult.getDistance(), is(output));
  }
}
