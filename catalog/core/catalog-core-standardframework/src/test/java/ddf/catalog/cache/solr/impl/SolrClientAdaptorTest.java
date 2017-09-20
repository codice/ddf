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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.codice.solr.factory.SolrClientFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class SolrClientAdaptorTest {

  public static final String CORE_NAME = "example-core-name";

  public static final String TEST_DELETE_BY_QUERY_STRING = "test-delete-by-query-string";

  private SolrClientAdaptor solrClientAdaptor;

  @Mock private SolrClientFactory mockSolrClientFactory;

  @Mock private Future<SolrClient> mockFutureSolrClient;

  @Mock private FilterAdapter mockFilterAdapter;

  @Mock private SolrFilterDelegateFactory mockSolrFilterDelegateFactory;

  @Mock private CacheSolrMetacardClient mockCacheSolrMetacardClient;

  @Mock private InitializedSolrClientAdaptor mockInitializedSolrClientAdaptor;

  @Test
  public void hasNoOpSolrMetacardClientBeforeInitIsCalled() throws Exception {
    solrClientAdaptor =
        new SolrClientAdaptor(
            CORE_NAME, mockFilterAdapter, mockSolrClientFactory, mockSolrFilterDelegateFactory);

    assertThat(
        solrClientAdaptor.getSolrMetacardClient(), is(instanceOf(NoOpSolrMetacardClient.class)));
  }

  @Test
  public void hasUninitializedClientAdaptorBeforeInitIsCalled() throws Exception {
    solrClientAdaptor =
        new SolrClientAdaptor(
            CORE_NAME, mockFilterAdapter, mockSolrClientFactory, mockSolrFilterDelegateFactory);

    assertThat(solrClientAdaptor.getState(), instanceOf(UninitializedSolrClientAdaptor.class));
  }

  @Test
  public void setsSolrMetacardClient() throws Exception {
    whenSolrClientIsSuccessfullyRetrieved();

    assertThat(solrClientAdaptor.getSolrMetacardClient(), is(mockCacheSolrMetacardClient));

    verifySolrClientIsSuccessfullyRetrieved();
  }

  @Test
  public void retriesToGetSolrClientWhenNull() throws Exception {
    when(mockSolrClientFactory.newClient(CORE_NAME)).thenReturn(mockFutureSolrClient);
    //Try to get the client twice
    when(mockFutureSolrClient.get(5, TimeUnit.SECONDS))
        .thenAnswer(
            new Answer<SolrClient>() {
              public SolrClient answer(InvocationOnMock invocation) throws Throwable {
                assertThat(
                    solrClientAdaptor.getState(), instanceOf(UninitializedSolrClientAdaptor.class));
                return null;
              }
            })
        .thenReturn(mock(SolrClient.class));

    solrClientAdaptor =
        new SolrClientAdaptor(
            CORE_NAME, mockFilterAdapter, mockSolrClientFactory, mockSolrFilterDelegateFactory);

    setMockSuppliersAndCallInit();

    assertThat(solrClientAdaptor.getSolrMetacardClient(), is(mockCacheSolrMetacardClient));
    assertThat(solrClientAdaptor.getState(), instanceOf(InitializedSolrClientAdaptor.class));

    verify(mockSolrClientFactory, times(2)).newClient(CORE_NAME);
    verify(mockFutureSolrClient, times(2)).get(5, TimeUnit.SECONDS);
  }

  @Test
  public void retriesToGetSolrClientWhenTimesOut() throws Exception {
    when(mockSolrClientFactory.newClient(CORE_NAME)).thenReturn(mockFutureSolrClient);
    //Try to get the client twice
    when(mockFutureSolrClient.get(5, TimeUnit.SECONDS))
        .thenAnswer(
            new Answer<SolrClient>() {
              public SolrClient answer(InvocationOnMock invocation) throws Throwable {
                assertThat(
                    solrClientAdaptor.getState(), instanceOf(UninitializedSolrClientAdaptor.class));
                throw new TimeoutException();
              }
            })
        .thenReturn(mock(SolrClient.class));

    solrClientAdaptor =
        new SolrClientAdaptor(
            CORE_NAME, mockFilterAdapter, mockSolrClientFactory, mockSolrFilterDelegateFactory);

    setMockSuppliersAndCallInit();

    assertThat(solrClientAdaptor.getSolrMetacardClient(), is(mockCacheSolrMetacardClient));
    assertThat(solrClientAdaptor.getState(), instanceOf(InitializedSolrClientAdaptor.class));

    verify(mockSolrClientFactory, times(1)).newClient(CORE_NAME);
    verify(mockFutureSolrClient, times(2)).get(5, TimeUnit.SECONDS);
  }

  @Test
  public void commit() throws Exception {
    whenSolrClientIsSuccessfullyRetrieved();

    solrClientAdaptor.commit();

    verify(mockInitializedSolrClientAdaptor).commit();
  }

  @Test
  public void close() throws Exception {
    whenSolrClientIsSuccessfullyRetrieved();

    solrClientAdaptor.close();

    verify(mockInitializedSolrClientAdaptor).close();
  }

  @Test
  public void deleteByQuery() throws Exception {
    whenSolrClientIsSuccessfullyRetrieved();
    UpdateResponse expectedResponse = new UpdateResponse();
    when(mockInitializedSolrClientAdaptor.deleteByQuery(TEST_DELETE_BY_QUERY_STRING))
        .thenReturn(expectedResponse);

    UpdateResponse response = solrClientAdaptor.deleteByQuery(TEST_DELETE_BY_QUERY_STRING);

    assertThat(response, is(expectedResponse));
    verify(mockInitializedSolrClientAdaptor).deleteByQuery(TEST_DELETE_BY_QUERY_STRING);
  }

  private void whenSolrClientIsSuccessfullyRetrieved() throws Exception {
    when(mockSolrClientFactory.newClient(anyString())).thenReturn(mockFutureSolrClient);
    when(mockFutureSolrClient.get(anyInt(), any(TimeUnit.class)))
        .thenReturn(mock(SolrClient.class));

    solrClientAdaptor =
        new SolrClientAdaptor(
            CORE_NAME, mockFilterAdapter, mockSolrClientFactory, mockSolrFilterDelegateFactory);

    setMockSuppliersAndCallInit();
  }

  private void setMockSuppliersAndCallInit() {
    solrClientAdaptor.setMetacardClientSupplierFunction(
        (solrClient) -> mockCacheSolrMetacardClient);
    solrClientAdaptor.setClientAdaptorSupplierFunction(
        (solrClient) -> mockInitializedSolrClientAdaptor);
    solrClientAdaptor.init();
  }

  private void verifySolrClientIsSuccessfullyRetrieved() throws Exception {
    verify(mockSolrClientFactory).newClient(CORE_NAME);
    verify(mockFutureSolrClient).get(5, TimeUnit.SECONDS);
  }
}
