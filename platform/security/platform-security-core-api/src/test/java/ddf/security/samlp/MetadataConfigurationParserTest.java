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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

public class MetadataConfigurationParserTest {

    private Path descriptorPath;

    private LocalTestServer server;

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

        server = new LocalTestServer(null, null);
        server.register("/*", handler);
        server.start();

        serverAddress = server.getServiceAddress()
                .getHostString() + ":" + server.getServiceAddress()
                .getPort();
    }

    @After
    public void after() throws Exception {
        server.stop();
    }

    @Test
    public void testMetadataFolder() throws Exception {
        System.setProperty("ddf.home",
                descriptorPath.getParent()
                        .getParent()
                        .getParent()
                        .toString());
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.<String>emptyList());
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataFile() throws Exception {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList("file:" + descriptorPath.toString()));
        Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntryDescriptions();

        assertThat(entities.size(), is(1));
    }

    @Test
    public void testMetadataString() throws Exception {
        MetadataConfigurationParser metadataConfigurationParser = new MetadataConfigurationParser(
                Collections.singletonList(IOUtils.toString(descriptorPath.toUri())));
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
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                HttpResponse response = (HttpResponse) invocationOnMock.getArguments()[1];
                response.setEntity(new StringEntity(message));
                return null;
            }
        }).when(handler)
                .handle(any(), any(), any());
    }

}
