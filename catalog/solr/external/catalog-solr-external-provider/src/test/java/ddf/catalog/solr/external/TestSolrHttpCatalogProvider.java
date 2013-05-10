/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.solr.external;

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

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;

/**
 * Unit tests for {@link SolrHttpCatalogProvider}
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestSolrHttpCatalogProvider {

    static {
        BasicConfigurator.configure();
    }

    @Test
    public void testId() {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        provider.maskId("myId");

        assertEquals("myId", provider.getId());
    }

    @Test
    public void testDescribableProperties() {

        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        assertNotNull(provider.getTitle());
        assertNotNull(provider.getDescription());
        assertNotNull(provider.getOrganization());
        assertNotNull(provider.getVersion());

    }

    @Test
    public void testUnconfiguredCreate() throws IngestException,
            SolrServerException, IOException {
        SolrServer givenServer = givenSolrServer(false);
        CatalogProvider provider = new SolrHttpCatalogProvider(null,
                givenServer);

        try {
            provider.create(mock(CreateRequest.class));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                    containsString("Solr Server is not connected"));
        }

    }

    /**
     * Tests what happens when a {@link SolrException} is thrown when Solr is
     * pinged
     * 
     * @throws IngestException
     * @throws SolrServerException
     * @throws IOException
     */
    @Test
    public void testUnconfiguredCreate_SolrException() throws IngestException,
            SolrServerException, IOException {
        // given
        SolrServer givenServer = mock(SolrServer.class);
        when(givenServer.ping()).thenThrow(SolrException.class);
        CatalogProvider provider = new SolrHttpCatalogProvider(null,
                givenServer);

        // when
        String message = null;
        try {
            provider.create(mock(CreateRequest.class));
        } catch (IllegalArgumentException e) {

            message = e.getMessage();
        }

        // then
        assertThat(message, containsString("Solr Server is not connected"));
        verify(givenServer, times(1)).ping();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnconfiguredDelete() throws IngestException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        provider.delete(mock(DeleteRequest.class));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnconfiguredUpdate() throws IngestException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        provider.update(mock(UpdateRequest.class));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnconfiguredQuery() throws UnsupportedQueryException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        provider.query(mock(QueryRequest.class));

    }

    @Test
    public void testAvailability() throws IngestException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        assertThat(provider.isAvailable(), is(false));
    }

    @Test
    public void testAvailabilitySourceMonitor() throws IngestException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        assertThat(provider.isAvailable(null), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnconfiguredGetContentTypes() throws IngestException {
        CatalogProvider provider = new SolrHttpCatalogProvider(null, null);

        provider.getContentTypes();

    }

    @Test()
    public void testUpdateConfigurationNullProperties() throws IngestException {
        SolrHttpCatalogProvider provider = new SolrHttpCatalogProvider(null,
                null);

        provider.updateServer(null);

        // should be no failures/exceptions

    }

    @Test()
    public void testUpdateConfigurationUrlPropertyNull() throws IngestException, SolrServerException, IOException {
        // given
        SolrServer givenServer = givenSolrServer(true);
        SolrHttpCatalogProvider provider = new SolrHttpCatalogProvider(null,
                givenServer);

        // when
        provider.updateServer(null);

        // then
        assertThat(provider.getUrl(), is(nullValue()));
        verify(givenServer, times(0)).ping();
        verify(givenServer, times(0)).shutdown();
        // should be no failures/exceptions

    }

    /**
     * Tests if the ConfigurationStore is set properly
     * 
     * @throws IngestException
     */
    @Test()
    public void testForceAutoCommit() throws IngestException {

        SolrHttpCatalogProvider provider = new SolrHttpCatalogProvider(null,
                null);

        provider.setForceAutoCommit(true);

        assertThat(ConfigurationStore.getInstance().isForceAutoCommit(),
                is(true));

    }

    /**
     * If the first connection was a failure, do a ping and attempt to connect
     * to the new address.
     * 
     * @throws IngestException
     * @throws SolrServerException
     * @throws IOException
     */
    @Test()
    public void testReAttemptSolrConnectionOnFail() throws IngestException,
            SolrServerException, IOException {

        // given
        String badAddress = "http://localhost:8183/solr";
        boolean serverStatus = false;
        SolrServer givenServer = givenSolrServer(serverStatus);
        SolrHttpCatalogProvider provider = new SolrHttpCatalogProvider(null,
                givenServer);
        // when
        try {
            provider.create(mock(CreateRequest.class));
        } catch (IllegalArgumentException e) {
        }
        provider.updateServer(badAddress);

        // then
        verify(givenServer, times(1)).shutdown();
        verify(givenServer, times(1)).ping();

    }

    @Test()
    public void testShutdown() throws SolrServerException, IOException {
        boolean serverStatus = true;
        SolrServer givenServer = givenSolrServer(serverStatus);
        SolrHttpCatalogProvider provider = new SolrHttpCatalogProvider(null,
                givenServer);

        provider.shutdown();
        verify(givenServer, times(1)).shutdown();
    }

    /**
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    private SolrServer givenSolrServer(boolean ok) throws SolrServerException,
            IOException {
        SolrServer server = mock(SolrServer.class);

        SolrPingResponse pingResponse = mock(SolrPingResponse.class);

        NamedList<Object> namedList = new NamedList<Object>();

        if (ok) {
            namedList.add("status", "OK");
        } else {
            namedList.add("status", "NOT_OK");
        }

        when(pingResponse.getResponse()).thenReturn(namedList);

        when(server.ping()).thenReturn(pingResponse);

        return server;
    }

}
