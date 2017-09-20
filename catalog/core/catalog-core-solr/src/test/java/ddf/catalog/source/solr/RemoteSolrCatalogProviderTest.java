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
package ddf.catalog.source.solr;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoteSolrCatalogProviderTest {

  private static String cipherSuites;

  private static String protocols;

  @BeforeClass
  public static void setUp() {
    cipherSuites = System.getProperty("https.cipherSuites");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    protocols = System.getProperty("https.protocols");
    System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
  }

  @AfterClass
  public static void tearDown() {
    if (cipherSuites != null) {
      System.setProperty("https.cipherSuites", cipherSuites);
    } else {
      System.clearProperty("https.cipherSuites");
    }
    if (protocols != null) {
      System.setProperty("https.protocols", protocols);
    } else {
      System.clearProperty("https.protocols");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullFilterAdapator() {
    new RemoteSolrCatalogProvider(
        null,
        mock(SolrClient.class),
        mock(SolrFilterDelegateFactory.class),
        mock(DynamicSchemaResolver.class)) {
      @Override
      protected Future<SolrClient> createClient() {
        return null;
      }
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSolrFilterDelegateFactory() {
    new RemoteSolrCatalogProvider(
        mock(FilterAdapter.class),
        mock(SolrClient.class),
        null,
        mock(DynamicSchemaResolver.class)) {
      @Override
      protected Future<SolrClient> createClient() {
        return null;
      }
    };
  }

  @Test
  public void testId() {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.maskId("myId");

    assertEquals("myId", provider.getId());
  }

  @Test
  public void testDescribableProperties() {

    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    assertNotNull(provider.getTitle());
    assertNotNull(provider.getDescription());
    assertNotNull(provider.getOrganization());
    assertNotNull(provider.getVersion());
  }

  @Test
  public void testUnconfiguredCreate() throws IngestException, SolrServerException, IOException {
    SolrClient givenClient = givenSolrClient(false);
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);

    try {
      provider.create(mock(CreateRequest.class));
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), containsString("Solr client is not connected"));
    }
  }

  /**
   * Tests what happens when a {@link SolrException} is thrown when Solr is pinged
   *
   * @throws IngestException
   * @throws SolrServerException
   * @throws IOException
   */
  @Test
  public void testUnconfiguredCreateSolrException()
      throws IngestException, SolrServerException, IOException {
    // given
    SolrClient givenClient = mock(SolrClient.class);
    when(givenClient.ping()).thenThrow(SolrException.class);
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);

    // when
    String message = null;
    try {
      provider.create(mock(CreateRequest.class));
    } catch (IllegalStateException e) {

      message = e.getMessage();
    }

    // then
    assertThat(message, containsString("Solr client is not connected"));
    verify(givenClient, times(1)).ping();
  }

  @Test(expected = IllegalStateException.class)
  public void testUnconfiguredDelete() throws IngestException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.delete(mock(DeleteRequest.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testUnconfiguredUpdate() throws IngestException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.update(mock(UpdateRequest.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testUnconfiguredQuery() throws UnsupportedQueryException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.query(mock(QueryRequest.class));
  }

  @Test
  public void testAvailability() throws IngestException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    assertThat(provider.isAvailable(), is(false));
  }

  @Test
  public void testAvailabilitySourceMonitor() throws IngestException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    assertThat(provider.isAvailable(null), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void testUnconfiguredGetContentTypes() throws IngestException {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.getContentTypes();
  }

  /**
   * Tests that property replacement is performed on the URL provided to {@link
   * RemoteSolrCatalogProvider#setUrl(String)}.
   */
  @Test
  public void testSetUrlResolvesProperties() throws Exception {
    // given
    SolrClient givenClient = givenSolrClient(true);
    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);
    System.setProperty("test-property", "solr-url");

    // when
    provider.setUrl("https://hostname:443/${test-property}");

    // then
    assertThat(provider.getUrl(), is("https://hostname:443/solr-url"));
    // should be no failures/exceptions
  }

  @Test
  public void testSetUrlToNull() throws IngestException {
    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.setUrl(null);

    // should be no failures/exceptions
  }

  @Test
  public void testUpdateConfigurationUrlPropertyNull() throws Exception {
    // given
    SolrClient givenClient = givenSolrClient(true);
    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);

    // when
    provider.setUrl(null);

    // then
    assertThat(provider.getUrl(), is(nullValue()));
    verify(givenClient, times(0)).ping();
    verify(givenClient, times(0)).close();
    // should be no failures/exceptions
  }

  /**
   * Tests if the ConfigurationStore is set properly
   *
   * @throws IngestException
   */
  @Test
  public void testForceAutoCommit() throws IngestException {

    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(null);

    provider.setForceAutoCommit(true);

    assertThat(ConfigurationStore.getInstance().isForceAutoCommit(), is(true));
  }

  /**
   * If the first connection was a failure, do a ping and attempt to connect to the new address.
   *
   * @throws IngestException
   * @throws SolrServerException
   * @throws IOException
   */
  @Test
  public void testReAttemptSolrConnectionOnFail()
      throws IngestException, SolrServerException, IOException {

    // given
    String badAddress = "http://localhost:8183/solr";
    boolean clientStatus = false;
    SolrClient givenClient = givenSolrClient(clientStatus);
    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);

    // when
    try {
      provider.create(mock(CreateRequest.class));
    } catch (IllegalStateException e) {
    }

    provider.setUrl(badAddress);

    // then
    verify(givenClient, times(1)).close();
    verify(givenClient, times(1)).ping();
  }

  @Test
  public void testShutdown() throws SolrServerException, IOException {
    boolean clientStatus = true;
    SolrClient givenClient = givenSolrClient(clientStatus);
    RemoteSolrCatalogProvider provider = new MockedRemoteSolrCatalogProvider(givenClient);

    provider.shutdown();
    verify(givenClient, times(1)).close();
  }

  /**
   * @return
   * @throws IOException
   * @throws SolrServerException
   */
  private SolrClient givenSolrClient(boolean ok) throws SolrServerException, IOException {
    SolrClient client = mock(SolrClient.class);

    SolrPingResponse pingResponse = mock(SolrPingResponse.class);

    NamedList<Object> namedList = new NamedList<Object>();

    if (ok) {
      namedList.add("status", "OK");
    } else {
      namedList.add("status", "NOT_OK");
    }

    when(pingResponse.getResponse()).thenReturn(namedList);

    when(client.ping()).thenReturn(pingResponse);

    return client;
  }

  private class MockedRemoteSolrCatalogProvider extends RemoteSolrCatalogProvider {

    public MockedRemoteSolrCatalogProvider(SolrClient client) {
      super(
          mock(FilterAdapter.class),
          client,
          mock(SolrFilterDelegateFactory.class),
          mock(DynamicSchemaResolver.class));
    }

    @Override
    protected Future<SolrClient> createClient() {
      return null;
    }
  }
}
