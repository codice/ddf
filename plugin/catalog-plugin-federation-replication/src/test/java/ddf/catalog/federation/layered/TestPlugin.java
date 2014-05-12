/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.federation.layered;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.layered.replication.RestReplicatorPlugin;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class TestPlugin {

    private static MockRestEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(TestPlugin.class);

    // I changed the port so that it would not conflict in testing with other services
    private static final String ENDPOINT_ADDRESS = "http://localhost:8282/services/catalog";

    private static final String WADL_ADDRESS = ENDPOINT_ADDRESS + "?_wadl";

    private static Server server;

    private static RestReplicatorPlugin plugin;

    private static MetacardTransformer transformer;

    private static Metacard metacard;

    @BeforeClass
    public static void initialize() throws InterruptedException {
        // startServer();
        // waitForWADL();
    }

    private static void startServer() {
        LOGGER.info("Starting server.");

        endpoint = mock(MockRestEndpoint.class);

        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

        sf.setResourceClasses(MockRestEndpoint.class);

        sf.setAddress(ENDPOINT_ADDRESS);

        sf.setResourceProvider(MockRestEndpoint.class,
                new SingletonResourceProvider(endpoint, true));

        LOGGER.info("Creating server.");
        server = sf.create();
    }

    // Optional step - may be needed to ensure that by the time individual
    // tests start running the endpoint has been fully initialized
    private static void waitForWADL() throws InterruptedException {
        LOGGER.info("Waiting for wadl");
        WebClient client = WebClient.create(WADL_ADDRESS);
        // wait for 20 secs or so
        for (int i = 0; i < 20; i++) {
            Thread.currentThread().sleep(200);
            Response response = client.get();
            if (response.getStatus() == 200) {
                break;
            }
        }
        // no WADL is available yet - throw an exception or give tests a chance
        // to run anyway
    }

    @AfterClass
    public static void destroy() {

        if (server != null) {
            server.stop();
            server.destroy();
        }

    }

    @Before
    public void setup() {
        // given
        plugin = new RestReplicatorPlugin(ENDPOINT_ADDRESS);
        transformer = mock(MetacardTransformer.class);
        BinaryContent bc = mock(BinaryContent.class);
        byte[] bytes = {86};
        try {
            when(bc.getByteArray()).thenReturn(bytes);
            when(transformer.transform(isA(Metacard.class), isNull(Map.class))).thenReturn(bc);
        } catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }

        plugin.setTransformer(transformer);
        metacard = getMockMetacard();
    }

    private Metacard getMockMetacard() {
        Metacard metacard = mock(Metacard.class);
        when(metacard.getMetadata()).thenReturn(getSample());
        return metacard;
    }

    private String getSample() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<xml></xml>\r\n";
    }

    @Test
    @Ignore
    public void testUpdateNullRequest() throws PluginExecutionException, IngestException,
        SourceUnavailableException {
        // given
        UpdateResponse updateResponse = new UpdateResponseImpl(null, null, Arrays.asList(metacard),
                Arrays.asList(metacard));

        // when
        UpdateResponse response = plugin.process(updateResponse);

        // then
        verify(endpoint, never()).updateDocument(isA(String.class), isA(HttpHeaders.class),
                isA(InputStream.class));

        assertThat(response, sameInstance(updateResponse));
    }

    @Test
    @Ignore
    public void testUpdate() throws PluginExecutionException, IngestException,
        SourceUnavailableException {
        // given
        UpdateResponse updateResponse = new UpdateResponseImpl(
                new UpdateRequestImpl("23", metacard), null, Arrays.asList(metacard),
                Arrays.asList(metacard));

        // when
        UpdateResponse response = plugin.process(updateResponse);

        // then
        verify(endpoint).updateDocument(argThat(is("23")), isA(HttpHeaders.class),
                isA(InputStream.class));

        assertThat(response, sameInstance(updateResponse));
    }

    @Test
    @Ignore
    public void testCreateNullParent() throws PluginExecutionException, IngestException,
        SourceUnavailableException {
        // given
        CreateResponse createResponse = new CreateResponseImpl(new CreateRequestImpl(metacard),
                null, Arrays.asList(metacard));

        // when
        plugin.process(createResponse);

        // then
        verify(endpoint, never()).addDocument(isA(HttpHeaders.class), isA(UriInfo.class),
                isA(InputStream.class));
    }

    @Test
    @Ignore
    public void testCreateNullTransformer() throws PluginExecutionException, IngestException,
        SourceUnavailableException {
        // given
        plugin = new RestReplicatorPlugin(null);
        CreateResponse createResponse = new CreateResponseImpl(new CreateRequestImpl(metacard),
                null, Arrays.asList(metacard));

        // when
        plugin.process(createResponse);

        // then
        verify(endpoint, never()).addDocument(isA(HttpHeaders.class), isA(UriInfo.class),
                isA(InputStream.class));
    }

    @Test(expected = PluginExecutionException.class)
    public void testCreateBadTransform() throws PluginExecutionException,
        CatalogTransformerException, IOException, IngestException, SourceUnavailableException {
        // given
        when(transformer.transform(isA(Metacard.class), isNull(Map.class))).thenThrow(
                CatalogTransformerException.class);
        CreateResponse createResponse = new CreateResponseImpl(new CreateRequestImpl(metacard),
                null, Arrays.asList(metacard));

        // when
        plugin.process(createResponse);

    }

    @Test
    @Ignore
    public void testCreate() throws PluginExecutionException, CatalogTransformerException,
        IOException, IngestException, SourceUnavailableException {
        // given
        CreateResponse createResponse = new CreateResponseImpl(new CreateRequestImpl(metacard),
                null, Arrays.asList(metacard));

        // when
        CreateResponse response = plugin.process(createResponse);

        // then
        verify(endpoint).addDocument(isA(HttpHeaders.class), isA(UriInfo.class),
                isA(InputStream.class));

        assertThat(response, sameInstance(createResponse));
    }

    @Test
    @Ignore
    public void testDelete() throws PluginExecutionException, CatalogTransformerException,
        IOException, IngestException, SourceUnavailableException {
        // given
        when(metacard.getId()).thenReturn("23");

        DeleteResponse deleteResponse = new DeleteResponseImpl(null, null, Arrays.asList(metacard));

        // when
        DeleteResponse response = plugin.process(deleteResponse);

        // then
        verify(endpoint).deleteDocument(argThat(is("23")));

        assertThat(response, sameInstance(deleteResponse));
    }

    @Test
    @Ignore
    public void testParentAddress() {
        // given
        plugin.setParentAddress(null);

        plugin.setParentAddress(ENDPOINT_ADDRESS);
    }
}
