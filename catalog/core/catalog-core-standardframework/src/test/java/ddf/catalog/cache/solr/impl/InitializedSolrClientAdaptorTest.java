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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InitializedSolrClientAdaptorTest {
  private InitializedSolrClientAdaptor initializedSolrClientAdaptor;

  @Mock private SolrClient mockSolrClient;

  @Before
  public void setUp() {
    initializedSolrClientAdaptor = new InitializedSolrClientAdaptor(mockSolrClient);
  }

  @Test
  public void commit() throws Exception {
    initializedSolrClientAdaptor.commit();
    verify(mockSolrClient).commit();
  }

  @Test(expected = IOException.class)
  public void commitThrowsException() throws Exception {
    doThrow(new IOException()).when(mockSolrClient).commit();
    initializedSolrClientAdaptor.commit();
  }

  @Test
  public void close() throws Exception {
    initializedSolrClientAdaptor.close();
    verify(mockSolrClient).close();
  }

  @Test(expected = IOException.class)
  public void closeThrowsException() throws Exception {
    doThrow(new IOException()).when(mockSolrClient).close();
    initializedSolrClientAdaptor.close();
  }

  @Test
  public void deleteByQuery() throws Exception {
    UpdateResponse expectedResponse = new UpdateResponse();
    when(mockSolrClient.deleteByQuery("")).thenReturn(expectedResponse);

    UpdateResponse response = initializedSolrClientAdaptor.deleteByQuery("");

    assertThat(response, is(expectedResponse));
    verify(mockSolrClient).deleteByQuery("");
  }

  @Test(expected = IOException.class)
  public void deleteByQueryThrowsException() throws Exception {
    doThrow(new IOException()).when(mockSolrClient).deleteByQuery("");
    initializedSolrClientAdaptor.deleteByQuery("");
  }
}
