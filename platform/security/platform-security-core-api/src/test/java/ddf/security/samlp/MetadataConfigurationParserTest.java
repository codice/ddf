/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

public class MetadataConfigurationParserTest {

    private Path descriptorPath;

    private HttpServer server;

    private String serverAddress;

    @Mock
    HttpRequestHandler handler;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        OpenSAMLUtil.initSamlEngine();

        descriptorPath = Paths.get(getClass().getResource("/etc/metadata/entityDescriptor.xml")
                .toURI());
        System.setProperty("ddf.home", "");

        this.server = ServerBootstrap.bootstrap()
                .setSocketConfig(SocketConfig.custom()
                        .setSoTimeout(5000)
                        .build())
                .registerHandler("/*", handler)
                .create();
        this.server.start();
        serverAddress = "localhost:" + server.getLocalPort();
    }

    @After
    public void after() throws Exception {
        server.stop();
    }

    @Test
    public void testMetadataFolder() throws Exception {
        metadataFolderEntities(null);
    }

    @Test
    public void testMetadataFolderCallback() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        metadataFolderEntities(ed -> invoked.set(true));
        assertThat("Callback was not invoked", invoked.get());
    }

    private void metadataFolderEntities(Consumer<EntityDescriptor> updateCallback)
            throws Exception {
        System.setProperty("ddf.home",
                descriptorPath.getParent()
                        .getParent()
                        .getParent()
                        .toString());
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.emptyList(),
                updateCallback);
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataFile() throws Exception {
        metadataFile(null);
    }

    @Test
    public void testMetadataFileCallback() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        metadataFile(ed -> invoked.set(true));
        assertThat("Callback was not invoked", invoked.get());
    }

    private void metadataFile(Consumer<EntityDescriptor> updateCallback) throws IOException {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList("file:" + descriptorPath.toString()),
                updateCallback);
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataString() throws Exception {
        metadataString(null);
    }

    @Test
    public void testMetadataStringCallback() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        metadataString(ed -> invoked.set(true));
        assertThat("Callback was not invoked", invoked.get());
    }

    private void metadataString(Consumer<EntityDescriptor> updateCallback) throws IOException {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList(IOUtils.toString(descriptorPath.toUri())),
                updateCallback);
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataHttp() throws Exception {
        serverRespondsWith(IOUtils.toString(descriptorPath.toUri()));

        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList("http://" + serverAddress));

        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        while (entities.size() != 1) {
            TimeUnit.MILLISECONDS.sleep(10);
        }

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataBadString() throws Exception {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList("bad xml"));
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(0));
    }

    @Test
    public void testMetadataBadFile() throws Exception {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList("file:bad path"));
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(0));
    }

    private void serverRespondsWith(String message) throws HttpException, IOException {
        doAnswer(invocationOnMock -> {
            HttpResponse response = (HttpResponse) invocationOnMock.getArguments()[1];
            response.setEntity(new StringEntity(message));
            response.setStatusCode(200);
            return null;
        }).when(handler)
                .handle(any(), any(), any());
    }

}
