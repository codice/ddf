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
package org.codice.ddf.spatial.ogc.catalog.resource.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.tika.Tika;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.URLResourceReader;

public class TestResourceReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourceReader.class);
    
    private WebClient mockWebClient = mock(WebClient.class);
    
    private Tika mockTika = mock(Tika.class);

    private static final String HTTP_SCHEME_PLUS_SEP = "http://";

    private static final int MOCK_HTTP_SERVER_PORT = 29992;

    private static final String MOCK_HTTP_SERVER_HOST = "localhost";

    private static final String MOCK_HTTP_SERVER_PATH = "/mock/http/path";

    private static final String MOCK_HTTP_SERVER_ENCODING = "UTF-8";

    @Rule
    public MethodRule watchman = new TestWatchman() {
        public void starting(FrameworkMethod method) {
            LOGGER.debug("***************************  STARTING: {}  **************************\n"
                    + method.getName());
        }

        public void finished(FrameworkMethod method) {
            LOGGER.debug(
                    "***************************  END: {}  **************************\n" + method
                            .getName());
        }
    };

    @Test
    public void testRetrieveResourceTextHtmlDetectedByTika() throws Exception {    
        // Setup
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(MediaType.TEXT_HTML);
        
        URLResourceReader urlResourceReader = new URLResourceReader(null) {
            @Override
            protected WebClient getWebClient(String uri) {
                return mockWebClient;
            }
        };
        
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(urlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        
        // Perform Test
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        // Verify
        StringWriter writer = new StringWriter();
        IOUtils.copy(resourceResponse.getResource().getInputStream(), writer,
                MOCK_HTTP_SERVER_ENCODING);
        String responseString = writer.toString();

        LOGGER.info("Response " + responseString);
        
        assertThat(responseString,
                is("<html><script type=\"text/javascript\">window.location.replace(\"" + httpUriStr
                        + "\");</script></html>"));
    }
    
    /**
     * Tests the case in which the Resource in the ResourceResponse returned by the URLResourceReader has
     * a null mime type.
     */
    @Test
    public void testRetrieveResourceNullResourceMimeType() throws Exception {
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(MediaType.TEXT_HTML);
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getMimeType()).thenReturn(null);
        
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);
        
        URLResourceReader mockUrlResourceReader = mock(URLResourceReader.class);
        when(mockUrlResourceReader.retrieveResource(any(URI.class), any(Map.class))).thenReturn(mockResourceResponse);
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mockUrlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        assertThat(resourceResponse, is(mockResourceResponse));
    }
    
    /**
     * Tests the case in which the Resource in the ResourceResponse returned by the URLResourceReader has
     * a text/html mime type.
     */
    @Test
    public void testRetrieveResourceMimeTypeTextHtml() throws Exception {
        // Setup
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(null);
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getMimeType()).thenReturn(new MimeType(MediaType.TEXT_HTML));
        
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);
        
        URLResourceReader mockUrlResourceReader = mock(URLResourceReader.class);
        when(mockUrlResourceReader.retrieveResource(any(URI.class), any(Map.class))).thenReturn(mockResourceResponse);
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mockUrlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        
        // Perform Test
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);
        
        // Verify
        StringWriter writer = new StringWriter();
        IOUtils.copy(resourceResponse.getResource().getInputStream(), writer,
                MOCK_HTTP_SERVER_ENCODING);
        String responseString = writer.toString();

        LOGGER.info("Response " + responseString);
        
        assertThat(responseString,
                is("<html><script type=\"text/javascript\">window.location.replace(\"" + httpUriStr
                        + "\");</script></html>"));
    }
    
    /**
     * Tests the case in which the Resource in the ResourceResponse returned by the URLResourceReader has
     * an application/unknown mime type.
     */
    @Test
    public void testRetrieveResourceApplicationUnknownResourceMimeType() throws Exception {
        // Setup
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(MediaType.TEXT_HTML);
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getMimeType()).thenReturn(new MimeType("application/unknown"));
        
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);
        
        URLResourceReader mockUrlResourceReader = mock(URLResourceReader.class);
        when(mockUrlResourceReader.retrieveResource(any(URI.class), any(Map.class))).thenReturn(mockResourceResponse);
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mockUrlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        
        // Perform Test
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        // Verify
        StringWriter writer = new StringWriter();
        IOUtils.copy(resourceResponse.getResource().getInputStream(), writer,
                MOCK_HTTP_SERVER_ENCODING);
        String responseString = writer.toString();

        LOGGER.info("Response " + responseString);
        
        assertThat(responseString,
                is("<html><script type=\"text/javascript\">window.location.replace(\"" + httpUriStr
                        + "\");</script></html>"));
    }
    
    /**
     * Tests the case in which the Resource in the ResourceResponse returned by the URLResourceReader has
     * an application/octet-stream mime type.
     */
    @Test
    public void testRetrieveResourceApplicationOctetStreamResourceMimeType() throws Exception {
        // Setup
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(MediaType.TEXT_HTML);
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getMimeType()).thenReturn(new MimeType("application/octet-stream"));
        
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);
        
        URLResourceReader mockUrlResourceReader = mock(URLResourceReader.class);
        when(mockUrlResourceReader.retrieveResource(any(URI.class), any(Map.class))).thenReturn(mockResourceResponse);
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mockUrlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        
        // Perform Test
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        // Verify
        StringWriter writer = new StringWriter();
        IOUtils.copy(resourceResponse.getResource().getInputStream(), writer,
                MOCK_HTTP_SERVER_ENCODING);
        String responseString = writer.toString();

        LOGGER.info("Response " + responseString);
        
        assertThat(responseString,
                is("<html><script type=\"text/javascript\">window.location.replace(\"" + httpUriStr
                        + "\");</script></html>"));
    }
    
    /**
     * Tests the case in which the Resource in the ResourceResponse returned by the URLResourceReader has
     * a null mime type and tika can't detect the mime type.
     */
    @Test
    public void testRetrieveResourceCantDetectMimeType() throws Exception {
        // Setup
        Response mockResponse = mock(Response.class);
        when(mockWebClient.get()).thenReturn(mockResponse);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        when(mockResponse.getHeaders()).thenReturn(map);
        InputStream mockInputStream = mock(InputStream.class);
        when(mockResponse.getEntity()).thenReturn(mockInputStream);
        when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(mockTika.detect(Mockito.any(URL.class))).thenReturn(null);
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getMimeType()).thenReturn(null);
        
        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);
        
        URLResourceReader mockUrlResourceReader = mock(URLResourceReader.class);
        when(mockUrlResourceReader.retrieveResource(any(URI.class), any(Map.class))).thenReturn(mockResourceResponse);
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mockUrlResourceReader, mockTika);

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        
        // Perform Test
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        // Verify
        assertThat(resourceResponse, is(mockResourceResponse));
    }
    
    /**
     * The OgcUrlResourceReader only supports http and https.
     */
    @Test
    public void testGetSupportedSchemes() throws Exception {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        Set<String> supportedSchemes = resourceReader.getSupportedSchemes();
        assertThat(supportedSchemes.size(), is(2));
        assertThat(supportedSchemes, hasItems("http", "https"));
        
    }
    
    @Test
    public void testOptions() throws Exception {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        Set<String> options = resourceReader.getOptions(null);
        assertThat(options.size(), is(0));        
    }
    
    @Test
    public void testGetDescription() {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        assertNotNull(resourceReader.getDescription());
    }
    
    @Test
    public void testGetId() {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        assertNotNull(resourceReader.getId());
    }
    
    @Test
    public void testGetOrganization() {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        assertNotNull(resourceReader.getOrganization());
    }
    
    @Test
    public void testGetTitle() {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        assertNotNull(resourceReader.getTitle());
    }
    
    @Test
    public void testGetVersion() {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(null, null);
        assertNotNull(resourceReader.getVersion());
    }
}
