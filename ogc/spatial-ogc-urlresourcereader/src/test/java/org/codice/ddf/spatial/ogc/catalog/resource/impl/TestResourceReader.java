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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.custom.CustomMimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import ddf.mime.tika.TikaMimeTypeResolver;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestResourceReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourceReader.class);

    private static final String TEST_PATH = "/src/test/resources/data/";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final String JPEG_MIME_TYPE = "image/jpeg";

    private static final String VIDEO_MIME_TYPE = "video/mpeg";

    private static final String MP4_MIME_TYPE = "video/mp4";

    private static final String CUSTOM_MIME_TYPE = "image/xyz";

    private static final String CUSTOM_FILE_EXTENSION = "xyz";

    private static final String JPEG_FILE_NAME_1 = "flower.jpg";

    private static final String MPEG_FILE_NAME_1 = "test.mpeg";

    private static final String MP4_FILE_NAME_1 = "sample.mp4";

    private static final String PPT_FILE_NAME_1 = "MissionPlan.ppt";

    private static final String PPTX_FILE_NAME_1 = "MissionPlan.pptx";

    private static final String HTTP_SCHEME_PLUS_SEP = "http://";

    private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

    private static final int MOCK_HTTP_SERVER_PORT = 29992;

    private static final String MOCK_HTTP_SERVER_HOST = "localhost";

    private static final String MOCK_HTTP_SERVER_PATH = "/mock/http/path";

    private static final int MOCK_HTTP_SERVER_STOP_DELAY = 1;

    private static final String MOCK_HTTP_SERVER_ENCODING = "UTF-8";

    private static final int HTTP_SUCCESS_CODE = 200;

    private MimeTypeMapper mimeTypeMapper;

    private CustomMimeTypeResolver customResolver;

    static class MockHttpServerSuccessResponse implements HttpHandler {

        private String mockHost;

        private int mockPort;

        public MockHttpServerSuccessResponse(String mockHost, int mockPort) {
            this.mockHost = mockHost;
            this.mockPort = mockPort;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "<!DOCTYPE HTML><html><head><title>dummy</title></head><body>dummy</body></html>";
            InputStream inputStream = httpExchange.getRequestBody();
            LOGGER.info("request was '{}'", getStringFromInputStream(inputStream));
            //TODO assert
            httpExchange.sendResponseHeaders(HTTP_SUCCESS_CODE, response.length());
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }

    @Rule
    public MethodRule watchman = new TestWatchman() {
        public void starting(FrameworkMethod method) {
            LOGGER.debug("***************************  STARTING: {}  **************************\n"
                    + method.getName());
        }

        public void finished(FrameworkMethod method) {
            LOGGER.debug("***************************  END: {}  **************************\n"
                    + method.getName());
        }
    };

    @Before
    public void setUp() {
        MimeTypeResolver tikaResolver = new TikaMimeTypeResolver();
        this.customResolver = new CustomMimeTypeResolver();
        List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();
        resolvers.add(tikaResolver);
        resolvers.add(this.customResolver);
        this.mimeTypeMapper = new MimeTypeMapperImpl(resolvers);
    }

    @Test
    public void testReadJPGFile() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
        verifyFile(filePath, JPEG_FILE_NAME_1, JPEG_MIME_TYPE);
    }

    @Test
    public void testReadMPEGFile() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + MPEG_FILE_NAME_1;
        verifyFile(filePath, MPEG_FILE_NAME_1, VIDEO_MIME_TYPE);
    }

    @Test
    public void testReadMP4File() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + MP4_FILE_NAME_1;
        verifyFile(filePath, MP4_FILE_NAME_1, MP4_MIME_TYPE);
    }

    @Test
    public void testReadPPTFile() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + PPT_FILE_NAME_1;
        verifyFile(filePath, PPT_FILE_NAME_1, "application/vnd.ms-powerpoint");
    }

    @Test
    public void testReadPPTXFile() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + PPTX_FILE_NAME_1;
        verifyFile(filePath, PPTX_FILE_NAME_1,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    @Test
    public void testReadFileWithUnknownExtension() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + "UnknownExtension.hugh";
        verifyFile(filePath, "UnknownExtension.hugh", DEFAULT_MIME_TYPE);
    }

    @Test
    public void testReadFileWithNoExtension() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + "JpegWithoutExtension";
        verifyFile(filePath, "JpegWithoutExtension", JPEG_MIME_TYPE);
    }

    @Test
    public void testReadFileWithCustomExtension() {
        // Add custom file extension to mime type mapping to custom mime type
        // resolver
        this.customResolver.setCustomMimeTypes(new String[] {CUSTOM_FILE_EXTENSION + "="
                + CUSTOM_MIME_TYPE});

        String filePath = ABSOLUTE_PATH + TEST_PATH + "CustomExtension.xyz";
        verifyFile(filePath, "CustomExtension.xyz", CUSTOM_MIME_TYPE);
    }

    @Test
    public void testJpegWithUnknownExtension() {
        String filePath = ABSOLUTE_PATH + TEST_PATH + "JpegWithUnknownExtension.hugh";
        verifyFile(filePath, "JpegWithUnknownExtension.hugh", JPEG_MIME_TYPE);
    }

    @Test
    public void testJpegWithCustomExtension() {
        // Add custom file extension to mime type mapping to custom mime type
        // resolver
        this.customResolver.setCustomMimeTypes(new String[] {CUSTOM_FILE_EXTENSION + "="
                + CUSTOM_MIME_TYPE});

        String filePath = ABSOLUTE_PATH + TEST_PATH + "JpegWithCustomExtension.xyz";
        verifyFile(filePath, "JpegWithCustomExtension.xyz", CUSTOM_MIME_TYPE);
    }

    @Test
    public void testJpegWithOverriddenExtension() {
        // Override/redefine .jpg file extension to custom mime type mapping of
        // "image/xyz"
        this.customResolver.setCustomMimeTypes(new String[] {"jpg=" + CUSTOM_MIME_TYPE});

        String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
        verifyFile(filePath, JPEG_FILE_NAME_1, CUSTOM_MIME_TYPE);
    }

    @Test
    public void testOgcUrlResourceReaderHtmlContent() throws Exception {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mimeTypeMapper);

        HttpServer mockHttpServer = HttpServer
                .create(new InetSocketAddress(MOCK_HTTP_SERVER_PORT), 0);
        mockHttpServer.createContext(MOCK_HTTP_SERVER_PATH,
                new MockHttpServerSuccessResponse(MOCK_HTTP_SERVER_HOST, MOCK_HTTP_SERVER_PORT));
        mockHttpServer.setExecutor(null);
        mockHttpServer.start();

        String httpUriStr =
                HTTP_SCHEME_PLUS_SEP + MOCK_HTTP_SERVER_HOST + ":" + MOCK_HTTP_SERVER_PORT
                        + MOCK_HTTP_SERVER_PATH;

        URI uri = new URI(httpUriStr);
        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

        StringWriter writer = new StringWriter();
        IOUtils.copy(resourceResponse.getResource().getInputStream(), writer,
                MOCK_HTTP_SERVER_ENCODING);
        String responseString = writer.toString();

        LOGGER.info("Response " + responseString);
        assertEquals(responseString,
                "<html><script type=\"text/javascript\">window.location.replace(\"" + httpUriStr
                        + "\");</script></html>");

        mockHttpServer.stop(MOCK_HTTP_SERVER_STOP_DELAY);
    }

    private void verifyFile(String filePath, String filename, String expectedMimeType) {
        OgcUrlResourceReader resourceReader = new OgcUrlResourceReader(mimeTypeMapper);

        HashMap<String, Serializable> arguments = new HashMap<>();

        try {
            LOGGER.info("Getting resource: " + filePath);

            // Test using the URL ResourceReader
            File file = new File(filePath);

            URI uri = file.toURI();
            LOGGER.info("URI: " + uri.toString());

            ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

            Resource resource = resourceResponse.getResource();
            assert (resource != null);

            LOGGER.info("MimeType: " + resource.getMimeType());
            LOGGER.info("Got resource: " + resource.getName());
            String name = resource.getName();
            assertNotNull(name);
            assertTrue(name.equals(filename));
            assertTrue(resource.getMimeType().toString().contains(expectedMimeType));

        } catch (IOException e) {
            LOGGER.info("Caught unexpected IOException");
            fail();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Caught unexpected ResourceNotFoundException");
            fail();
        }
    }

    private static String getStringFromInputStream(InputStream inputStream) {
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        } catch (IOException e) {
            LOGGER.error("IOException on reading input stream: " + e.getMessage());
            fail(e.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing bufferedReader in test");
                }
            }
        }
        return stringBuilder.toString();
    }

}
