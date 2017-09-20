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
package ddf.catalog.cache.solr.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import java.util.Collections;
import org.junit.Test;

public class NoOpSolrMetacardClientTest {

  private NoOpSolrMetacardClient noOpSolrMetacardClient = NoOpSolrMetacardClient.getInstance();

  @Test
  public void getInstance() {
    assertThat(NoOpSolrMetacardClient.getInstance(), is(instanceOf(NoOpSolrMetacardClient.class)));
  }

  @Test
  public void queryWithRequest() throws Exception {
    QueryRequest mockRequest = mock(QueryRequest.class);
    SourceResponse response = noOpSolrMetacardClient.query(mockRequest);

    assertThat(response.getRequest(), is(mockRequest));
    assertThat(response.getResults(), is(empty()));
    assertThat(response.getHits(), is(equalTo(0L)));
  }

  @Test
  public void queryWithString() throws Exception {
    assertThat(noOpSolrMetacardClient.query(""), is(empty()));
  }

  @Test
  public void getContentTypes() {
    assertThat(noOpSolrMetacardClient.getContentTypes(), is(empty()));
  }

  @Test
  public void add() throws Exception {
    assertThat(noOpSolrMetacardClient.add(Collections.emptyList(), true), is(empty()));
  }
}
