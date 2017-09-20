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

import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Before;
import org.junit.Test;

public class UninitializedSolrClientAdaptorTest {
  private UninitializedSolrClientAdaptor uninitializedSolrClientAdaptor;

  @Before
  public void setUp() {
    uninitializedSolrClientAdaptor = UninitializedSolrClientAdaptor.getInstance();
  }

  @Test
  public void getInstance() {
    assertThat(
        UninitializedSolrClientAdaptor.getInstance(),
        is(instanceOf(UninitializedSolrClientAdaptor.class)));
  }

  @Test
  public void commitDoesNothing() {
    try {
      uninitializedSolrClientAdaptor.commit();
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void closeDoesNothing() {
    try {
      uninitializedSolrClientAdaptor.close();
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void deleteByQuery() throws Exception {
    UpdateResponse response =
        uninitializedSolrClientAdaptor.deleteByQuery("test-delete-by-query-string");

    assertThat(response.getElapsedTime(), is(equalTo(0L)));
    assertThat(response.getRequestUrl(), is(equalTo("")));
    assertThat(response.getResponse().size(), is(equalTo(0)));
  }
}
