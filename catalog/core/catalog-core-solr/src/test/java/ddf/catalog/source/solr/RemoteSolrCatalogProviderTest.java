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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.util.NamedList;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.client.solrj.SolrClient.Listener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
        mock(DynamicSchemaResolver.class)) {};
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSolrClient() {
    new RemoteSolrCatalogProvider(
        mock(FilterAdapter.class),
        null,
        mock(SolrFilterDelegateFactory.class),
        mock(DynamicSchemaResolver.class)) {};
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSolrFilterDelegateFactory() {
    new RemoteSolrCatalogProvider(
        mock(FilterAdapter.class),
        mock(SolrClient.class),
        null,
        mock(DynamicSchemaResolver.class)) {};
  }

  @Test
  public void testId() {
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(mock(SolrClient.class));

    provider.maskId("myId");

    assertEquals("myId", provider.getId());
  }

  @Test
  public void testDescribableProperties() {

    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(mock(SolrClient.class));

    assertNotNull(provider.getTitle());
    assertNotNull(provider.getDescription());
    assertNotNull(provider.getOrganization());
    assertNotNull(provider.getVersion());
  }

  @Test
  public void testIsAvailableWhenSolrClientStatusNotOk() throws Exception {
    final SolrClient client = givenSolrClient(false);
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(client);

    assertThat(provider.isAvailable(), is(false));

    verify(client).ping();
  }

  @Test
  public void testAvailabilitySourceMonitor() throws Exception {
    final SolrClient client = givenSolrClient(false);
    CatalogProvider provider = new MockedRemoteSolrCatalogProvider(client);
    final SourceMonitor monitor = mock(SourceMonitor.class);

    assertThat(provider.isAvailable(monitor), is(false));
    final ArgumentCaptor<Listener> listener = ArgumentCaptor.forClass(Listener.class);

    verify(client).isAvailable(listener.capture());
    verify(client).ping();

    // when the underlying listener is called with not available
    listener.getValue().changed(client, false);

    // so should our monitor
    verify(monitor).setUnavailable();

    // when the underlying listener is called with available
    listener.getValue().changed(client, true);

    // so should our monitor
    verify(monitor).setAvailable();
  }

  /** Tests if the ConfigurationStore is set properly */
  @Test
  public void testForceAutoCommit() throws IngestException {

    RemoteSolrCatalogProvider provider =
        new MockedRemoteSolrCatalogProvider(mock(SolrClient.class));

    provider.setForceAutoCommit(true);

    assertThat(ConfigurationStore.getInstance().isForceAutoCommit(), is(true));
  }

  @Test
  public void testShutdown() throws SolrServerException, IOException {
    SolrClient givenClient = mock(SolrClient.class);
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

    NamedList<Object> namedList = new NamedList<>();

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
  }
}
