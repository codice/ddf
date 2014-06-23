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
package ddf.catalog.resource.impl;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.custom.CustomMimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import ddf.mime.tika.TikaMimeTypeResolver;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceReaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReaderTest.class);

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

    private static final String HTTP_SCHEME = "http";

    private static final String HTTP_SCHEME_PLUS_SEP = "http://";

    private static final String FILE_SCHEME_PLUS_SEP = "file:///";

    private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

    private static final String HOST = "127.0.0.1";

    private static final String BAD_FILE_NAME = "mydata?uri=63f30ff4dc85436ea507fceeb1396940_blahblahblah&this=that";

    private static final String BYTES_TO_SKIP = "BytesToSkip";

    private MimeTypeMapper mimeTypeMapper;

    private CustomMimeTypeResolver customResolver;

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
    public void testURLResourceReaderBadQualifier() {
        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);
        String filePath = TEST_PATH + MPEG_FILE_NAME_1;

        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        try {
            LOGGER.info("Getting resource: " + filePath);
            URI uri = new URI(FILE_SCHEME_PLUS_SEP + filePath);

            resourceReader.retrieveResource(uri, arguments);
        } catch (IOException e) {
            LOGGER.info("Successfully caught expected IOException");
            assert (true);
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Caught unexpected ResourceNotFoundException");
            fail();
        } catch (URISyntaxException e) {
            LOGGER.info("Caught unexpected URISyntaxException");
            fail();
        }
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
    public void testURLResourceIOException() {
        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);

        String filePath = "JUMANJI!!!!";

        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        try {
            LOGGER.info("Getting resource: " + filePath);
            URI uri = new URI(FILE_SCHEME_PLUS_SEP + filePath);
            resourceReader.retrieveResource(uri, arguments);
        } catch (IOException e) {
            LOGGER.info("Successfully caught IOException");
            assert (true);
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Caught unexpected ResourceNotFoundException");
            fail();
        } catch (URISyntaxException e) {
            LOGGER.info("Caught unexpected URISyntaxException");
            fail();
        }
    }

    @Test
    public void testUrlToNonExistentFile() {
        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);

        String filePath = ABSOLUTE_PATH + TEST_PATH + "NonExistentFile.jpg";

        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
        try {
            LOGGER.info("Getting resource: " + filePath);
            File file = new File(filePath);
            URI uri = file.toURI();
            resourceReader.retrieveResource(uri, arguments);
        } catch (IOException e) {
            LOGGER.info("Successfully caught IOException");
            assert (true);
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Caught unexpected ResourceNotFoundException");
            fail();
        }
    }

    @Test
    public void testHTTPReturnsFileNameWithoutPath() throws URISyntaxException, IOException,
        ResourceNotFoundException {
        URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + TEST_PATH + JPEG_FILE_NAME_1);

        verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE);

        uri = new URI(FILE_SCHEME_PLUS_SEP + TEST_PATH + JPEG_FILE_NAME_1);

        verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE);
    }

    @Test
    public void testNameInContentDisposition() throws URISyntaxException, IOException,
            ResourceNotFoundException {
        URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + TEST_PATH + BAD_FILE_NAME);
        URLConnection conn = mock(URLConnection.class);

        when(conn.getHeaderField(URLResourceReader.CONTENT_DISPOSITION)).thenReturn(
                "inline; filename=\"" + JPEG_FILE_NAME_1 + "\"");
        when(conn.getInputStream()).thenReturn(getBinaryData());

        ResourceResponse response = verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, conn, null);

        // verify that we got the entire resource
        Assert.assertEquals(5, response.getResource().getByteArray().length);
    }

    @Test
    public void testRetrievingPartialContent() throws URISyntaxException, IOException,
        ResourceNotFoundException {
        URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + TEST_PATH + BAD_FILE_NAME);
        URLConnection conn = mock(URLConnection.class);

        when(conn.getHeaderField(URLResourceReader.CONTENT_DISPOSITION)).thenReturn(
                "inline; filename=\"" + JPEG_FILE_NAME_1 + "\"");
        when(conn.getInputStream()).thenReturn(getBinaryData());

        String bytesToSkip = "2";

        ResourceResponse response = verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, conn, bytesToSkip);

        // verify that we got the entire resource
        Assert.assertEquals(3, response.getResource().getByteArray().length);
    }

    @Test
    public void testUnquotedNameInContentDisposition() throws URISyntaxException, IOException,
        ResourceNotFoundException {
        URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + TEST_PATH + BAD_FILE_NAME);
        URLConnection conn = mock(URLConnection.class);

        when(conn.getHeaderField(URLResourceReader.CONTENT_DISPOSITION)).thenReturn(
                "inline; filename=" + JPEG_FILE_NAME_1);
        when(conn.getInputStream()).thenReturn(null);

        verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, conn, null);
    }

    @Test
    public void testUnquotedNameEndingSemicolonInContentDisposition() throws URISyntaxException,
        IOException, ResourceNotFoundException {
        URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + TEST_PATH + BAD_FILE_NAME);
        URLConnection conn = mock(URLConnection.class);

        when(conn.getHeaderField(URLResourceReader.CONTENT_DISPOSITION)).thenReturn(
                "inline;filename=" + JPEG_FILE_NAME_1 + ";");
        when(conn.getInputStream()).thenReturn(null);

        verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, conn, null);
    }

    @Test
    public void testURLResourceReaderQualifierSet() {
        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);

        Set<String> qualifiers = resourceReader.getSupportedSchemes();

        assert (qualifiers != null);
        assert (qualifiers.contains(HTTP_SCHEME));
        assert (qualifiers.size() == 3);
    }

    private void verifyFile(String filePath, String filename, String expectedMimeType) {
        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);

        HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();

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
            LOGGER.info("Caught unexpected ResourceNotFoundException", e);
            fail();
        }
    }

    private void verifyFileFromURLResourceReader(URI uri, String filename, String expectedMimeType)
        throws URISyntaxException, IOException, ResourceNotFoundException {
        URLConnection conn = mock(URLConnection.class);
        when(conn.getInputStream()).thenReturn(null);
        verifyFileFromURLResourceReader(uri, filename, expectedMimeType, conn, null);
    }

    // Create arguments, adding bytesToSkip if present, and call doVerification
    private ResourceResponse verifyFileFromURLResourceReader(URI uri, String filename, String expectedMimeType,
                                                             URLConnection conn, String bytesToSkip)
            throws URISyntaxException, IOException, ResourceNotFoundException {

        Map<String, Serializable> arguments = new HashMap<String, Serializable>();

        if (bytesToSkip != null) {
            arguments.put(BYTES_TO_SKIP, bytesToSkip);
        }

        return doVerification(uri, filename, expectedMimeType, conn, arguments);

    }

    private ResourceResponse doVerification(URI uri, String filename, String expectedMimeType, URLConnection conn,
                                            Map<String, Serializable> arguments)
            throws URISyntaxException, IOException, ResourceNotFoundException {

        URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper);

        resourceReader.setConn(conn);

        // Test using the URL ResourceReader
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

        return resourceResponse;
    }

    private static InputStream getBinaryData() {

        byte[] sampleBytes = {65, 66, 67, 68, 69};

        return new ByteArrayInputStream(sampleBytes);
    }

}