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
package ddf.catalog.resource.impl;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.custom.CustomMimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import ddf.mime.tika.TikaMimeTypeResolver;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.impl.ClientFactoryFactoryImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLResourceReaderTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(URLResourceReaderTest.class);

  private static final Path TEST_PATH = Paths.get("src", "test", "resources", "data");

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

  private static final Path INVALID_PATH = Paths.get("my", "invalid", "path");

  private static final String ROOT_PATH = "/";

  private static final String HOST = "127.0.0.1";

  private static final String BAD_FILE_NAME =
      "mydata?uri=63f30ff4dc85436ea507fceeb1396940_blahblahblah&this=that";

  private static final String BYTES_TO_SKIP = "BytesToSkip";

  @Rule
  public MethodRule watchman =
      new TestWatchman() {
        public void starting(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  STARTING: {}  **************************\n"
                  + method.getName());
        }

        public void finished(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  END: {}  **************************\n"
                  + method.getName());
        }
      };

  private ClientFactoryFactory clientFactoryFactory;

  private MimeTypeMapper mimeTypeMapper;

  private CustomMimeTypeResolver customResolver;

  private WebClient mockWebClient = mock(WebClient.class);

  private static InputStream getBinaryData() {

    byte[] sampleBytes = {65, 66, 67, 68, 69};

    return new ByteArrayInputStream(sampleBytes);
  }

  private static InputStream getBinaryDataWithOffset(int offset) throws IOException {

    byte[] sampleBytes = {65, 66, 67, 68, 69};
    InputStream is = new ByteArrayInputStream(sampleBytes);
    is.skip(offset);

    return is;
  }

  @Before
  public void setUp() {
    MimeTypeResolver tikaResolver = new TikaMimeTypeResolver();
    this.customResolver = new CustomMimeTypeResolver();
    List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();
    resolvers.add(tikaResolver);
    resolvers.add(this.customResolver);
    this.mimeTypeMapper = new MimeTypeMapperImpl(resolvers);
    this.clientFactoryFactory = new ClientFactoryFactoryImpl();
  }

  @Test
  public void testURLResourceReaderBadQualifier() {
    URLResourceReader resourceReader =
        new TestURLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(ImmutableSet.of(ABSOLUTE_PATH + TEST_PATH));
    URI uri = TEST_PATH.resolve(MPEG_FILE_NAME_1).toUri();

    HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
    try {
      LOGGER.info("Getting resource: {}", uri);

      resourceReader.retrieveResource(uri, arguments);
    } catch (IOException e) {
      LOGGER.info("Successfully caught expected IOException");
      fail();
    } catch (ResourceNotFoundException e) {
      LOGGER.info("Caught unexpected ResourceNotFoundException");
      assert (true);
    }
  }

  @Test
  public void testReadJPGFile() throws Exception {
    String filePath = TEST_PATH.resolve(JPEG_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        JPEG_FILE_NAME_1,
        JPEG_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadMPEGFile() throws Exception {
    String filePath = TEST_PATH.resolve(MPEG_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        MPEG_FILE_NAME_1,
        VIDEO_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadMP4File() throws Exception {
    String filePath = TEST_PATH.resolve(MP4_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        MP4_FILE_NAME_1,
        MP4_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadPPTFile() throws Exception {
    String filePath = TEST_PATH.resolve(PPT_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        PPT_FILE_NAME_1,
        "application/vnd.ms-powerpoint",
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadPPTXFile() throws Exception {
    String filePath = TEST_PATH.resolve(PPTX_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        PPTX_FILE_NAME_1,
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadFileWithUnknownExtension() throws Exception {
    String filePath = TEST_PATH.resolve("UnknownExtension.hugh").toAbsolutePath().toString();
    verifyFile(
        filePath,
        "UnknownExtension.hugh",
        DEFAULT_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadFileWithNoExtension() throws Exception {
    String filePath = TEST_PATH.resolve("JpegWithoutExtension").toAbsolutePath().toString();
    verifyFile(
        filePath,
        "JpegWithoutExtension",
        JPEG_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testReadFileWithCustomExtension() throws Exception {
    // Add custom file extension to mime type mapping to custom mime type
    // resolver
    this.customResolver.setCustomMimeTypes(
        new String[] {CUSTOM_FILE_EXTENSION + "=" + CUSTOM_MIME_TYPE});

    String filePath = TEST_PATH.resolve("CustomExtension.xyz").toAbsolutePath().toString();
    verifyFile(
        filePath,
        "CustomExtension.xyz",
        CUSTOM_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testJpegWithUnknownExtension() throws Exception {
    String filePath =
        TEST_PATH.resolve("JpegWithUnknownExtension.hugh").toAbsolutePath().toString();
    verifyFile(
        filePath,
        "JpegWithUnknownExtension.hugh",
        JPEG_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testJpegWithCustomExtension() throws Exception {
    // Add custom file extension to mime type mapping to custom mime type
    // resolver
    this.customResolver.setCustomMimeTypes(
        new String[] {CUSTOM_FILE_EXTENSION + "=" + CUSTOM_MIME_TYPE});

    String filePath = TEST_PATH.resolve("JpegWithCustomExtension.xyz").toAbsolutePath().toString();
    verifyFile(
        filePath,
        "JpegWithCustomExtension.xyz",
        CUSTOM_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testJpegWithOverriddenExtension() throws Exception {
    // Override/redefine .jpg file extension to custom mime type mapping of
    // "image/xyz"
    this.customResolver.setCustomMimeTypes(new String[] {"jpg=" + CUSTOM_MIME_TYPE});

    String filePath = TEST_PATH.resolve(JPEG_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(
        filePath,
        JPEG_FILE_NAME_1,
        CUSTOM_MIME_TYPE,
        TEST_PATH.toAbsolutePath().toString(),
        TEST_PATH.toAbsolutePath().toString() + "pdf");
  }

  @Test
  public void testURLResourceIOException() throws Exception {
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);

    String filePath = "JUMANJI!!!!";

    HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
    try {
      LOGGER.info("Getting resource: {}", filePath);
      URI uri = new URI(FILE_SCHEME_PLUS_SEP + filePath);
      resourceReader.retrieveResource(uri, arguments);
    } catch (IOException e) {
      LOGGER.info("Successfully caught IOException");
      fail();
    } catch (ResourceNotFoundException e) {
      LOGGER.info("Caught ResourceNotFoundException");
      assert (true);
    } catch (URISyntaxException e) {
      LOGGER.info("Caught unexpected URISyntaxException");
      fail();
    }
  }

  @Test
  public void testUrlToNonExistentFile() throws Exception {
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);

    String filePath = TEST_PATH.resolve("NonExistentFile.jpg").toAbsolutePath().toString();

    HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
    try {
      LOGGER.info("Getting resource: {}", filePath);
      File file = new File(filePath);
      URI uri = file.toURI();
      resourceReader.retrieveResource(uri, arguments);
    } catch (IOException e) {
      LOGGER.info("Successfully caught IOException");
      fail();
    } catch (ResourceNotFoundException e) {
      LOGGER.info("Caught ResourceNotFoundException");
      assert (true);
    }
  }

  @Test
  public void testHTTPReturnsFileNameWithoutPath() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + JPEG_FILE_NAME_1);

    verifyFileFromURLResourceReader(uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, 5);
  }

  @Test
  public void testNameInContentDisposition() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);
    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.<Object>asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());

    // verify that we got the entire resource
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, null, null, 5, uri.toString());
  }

  /**
   * Exactly the same as {@link URLResourceReaderTest#testNameInContentDisposition} except with a
   * "qualifier" property. Tests that the {@link WebClient} is created with the correct URL.
   */
  @Test
  public void testRetrieveQualifiedResource() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);
    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.<Object>asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());

    final String qualifierValue = "qualifierValue";
    final String expectedWebClientUri =
        String.format("%s&%s=%s", uri, ContentItem.QUALIFIER_KEYWORD, qualifierValue);
    // verify that we got the entire resource
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, null, qualifierValue, 5, expectedWebClientUri);
  }

  /**
   * Tests that a Partial Content response that has the same byte offset as what was requested
   * returns an input stream starting at the requested byte offset.
   *
   * @throws Exception
   */
  @Test
  public void testServerSupportsPartialContentResponseWithCorrectOffset() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);

    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    map.put(HttpHeaders.CONTENT_RANGE, Arrays.asList("Bytes 2-4/5"));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.PARTIAL_CONTENT.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryDataWithOffset(2));

    String bytesToSkip = "2";

    // verify that the requested bytes 3-5 were returned
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, bytesToSkip, null, 3, uri.toString());
  }

  /**
   * Tests that a Partial Content response that has a smaller byte offset than what was requested
   * still returns an input stream starting at the requested byte offset by skipping ahead in the
   * input stream.
   *
   * @throws Exception
   */
  @Test
  public void testServerSupportsPartialContentResponseWithNotEnoughOffset() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);

    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    map.put(HttpHeaders.CONTENT_RANGE, Arrays.asList("Bytes 1-4/5"));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.PARTIAL_CONTENT.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryDataWithOffset(1));

    String bytesToSkip = "2";

    // verify that the requested bytes 3-5 were returned
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, bytesToSkip, null, 3, uri.toString());
  }

  /**
   * Tests that a Partial Content response that has a higher byte offset as what was requested
   * throws an IOException in order to prevent data loss.
   *
   * @throws Exception
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testServerSupportsPartialContentResponseTooMuchOffset() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);

    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    map.put(HttpHeaders.CONTENT_RANGE, Arrays.asList("Bytes 3-4/5"));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.PARTIAL_CONTENT.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryDataWithOffset(3));

    String bytesToSkip = "2";

    // this should throw an IOException since more bytes were skipped than requested
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, bytesToSkip, null, 2135, uri.toString());
  }

  /**
   * Tests that if the server does not support range-header requests and responds with the entire
   * product's contents (no Content-Range header and a 200 response), an input is still returned
   * with the requested byte offset.
   *
   * @throws Exception
   */
  @Test
  public void testServerDoesNotSupportPartialContent() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);

    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.asList("inline; filename=\"" + JPEG_FILE_NAME_1 + "\""));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());

    String bytesToSkip = "2";

    // verify that the requested bytes 3-5 were returned
    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, bytesToSkip, null, 3, uri.toString());
  }

  @Test
  public void testUnquotedNameInContentDisposition() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);

    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.<Object>asList("inline; filename=" + JPEG_FILE_NAME_1));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());

    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, null, null, 5, uri.toString());
  }

  @Test
  public void testUnquotedNameEndingSemicolonInContentDisposition() throws Exception {
    URI uri = new URI(HTTP_SCHEME_PLUS_SEP + HOST + "/src/test/resources/data/" + BAD_FILE_NAME);
    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.<Object>asList("inline;filename=" + JPEG_FILE_NAME_1 + ";"));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());

    verifyFileFromURLResourceReader(
        uri, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, null, null, 5, uri.toString());
  }

  @Test
  public void testURLResourceReaderQualifierSet() throws Exception {
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);

    Set<String> qualifiers = resourceReader.getSupportedSchemes();

    assert (qualifiers != null);
    assert (qualifiers.contains(HTTP_SCHEME));
    assert (qualifiers.size() == 3);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testReadFileInvalidResourcePath() throws Exception {
    String invalidFilePath = INVALID_PATH + JPEG_FILE_NAME_1;
    verifyFile(
        invalidFilePath,
        JPEG_FILE_NAME_1,
        JPEG_MIME_TYPE,
        ABSOLUTE_PATH + TEST_PATH,
        ABSOLUTE_PATH + TEST_PATH + "pdf");
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testReadFileInvalidResourcePathWithBackReferences() throws Exception {
    String invalidFilePath = ABSOLUTE_PATH + TEST_PATH + "../../../../../" + JPEG_FILE_NAME_1;
    verifyFile(
        invalidFilePath,
        JPEG_FILE_NAME_1,
        JPEG_MIME_TYPE,
        ABSOLUTE_PATH + TEST_PATH,
        ABSOLUTE_PATH + TEST_PATH + "pdf");
  }

  @Test
  public void testReadFileResourceDirectoryIsRoot() throws Exception {
    String filePath = TEST_PATH.resolve(JPEG_FILE_NAME_1).toAbsolutePath().toString();
    verifyFile(filePath, JPEG_FILE_NAME_1, JPEG_MIME_TYPE, ROOT_PATH);
  }

  @Test
  public void testRemoveARootResourceDirectory() throws Exception {
    // Setup (2 paths)
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(), TEST_PATH.toAbsolutePath().toString() + "pdf"));

    // Perform Test (remove a path). NOTE: the complete set of configured root resource
    // directories
    // is passed in (not just the path to remove). In this case, ABSOLUTE_PATH + TEST_PATH +
    // "pdf" is removed.
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(TEST_PATH.toAbsolutePath().toString()));

    // Verify
    Set<String> rootResourceDirectories = resourceReader.getRootResourceDirectories();
    assertThat(rootResourceDirectories.size(), is(1));
    assertThat(
        rootResourceDirectories,
        hasItems(Paths.get(TEST_PATH.toAbsolutePath().toString()).normalize().toString()));
  }

  @Test
  public void testAddARootResourceDirectory() throws Exception {
    // Setup (2 paths)
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(), TEST_PATH.toAbsolutePath().toString() + "pdf"));

    // Perform Test (add a path). NOTE: the complete set of configured root resource directories
    // is passed in (not just the path to add). In this case, ABSOLUTE_PATH + TEST_PATH + "doc"
    // is added.
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(),
            TEST_PATH.toAbsolutePath().toString() + "pdf",
            TEST_PATH.toAbsolutePath().toString() + "doc"));

    // Verify
    Set<String> rootResourceDirectories = resourceReader.getRootResourceDirectories();
    assertThat(rootResourceDirectories.size(), is(3));
    assertThat(
        rootResourceDirectories,
        hasItems(
            Paths.get(TEST_PATH.toAbsolutePath().toString()).normalize().toString(),
            Paths.get(TEST_PATH.toAbsolutePath().toString() + "pdf").normalize().toString(),
            Paths.get(TEST_PATH.toAbsolutePath().toString() + "doc").normalize().toString()));
  }

  /**
   * Verify the URLResourceReader's Root Resource Directories gets set to an empty Set when null is
   * passed into setRootResourceDirectories.
   */
  @Test
  public void testSetRootResourceDirectoriesNullInput() throws Exception {
    // Setup (2 paths)
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(), TEST_PATH.toAbsolutePath().toString() + "pdf"));

    // Perform Test
    resourceReader.setRootResourceDirectories(null);

    // Verify
    Set<String> rootResourceDirectories = resourceReader.getRootResourceDirectories();
    assertThat(rootResourceDirectories.size(), is(0));
  }

  /**
   * Verify the URLResourceReader's Root Resource Directories gets set to an empty Set when an empty
   * Set is passed into setRootResourceDirectories.
   */
  @Test
  public void testSetRootResourceDirectoriesEmptySetInput() throws Exception {
    // Setup (2 paths)
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(), TEST_PATH.toAbsolutePath().toString() + "pdf"));

    // Perform Test
    resourceReader.setRootResourceDirectories(new HashSet<String>());

    // Verify
    Set<String> rootResourceDirectories = resourceReader.getRootResourceDirectories();
    assertThat(rootResourceDirectories.size(), is(0));
  }

  @Test
  public void testSetRootResourceDirectoriesInvalidPath() throws Exception {
    // Setup (1 valid paths, 1 invalid path)
    String invalidPath = "\0";
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);

    // Perform Test
    resourceReader.setRootResourceDirectories(
        ImmutableSet.of(
            TEST_PATH.toAbsolutePath().toString(),
            TEST_PATH.toAbsolutePath().toString() + invalidPath));

    // Verify
    Set<String> rootResourceDirectories = resourceReader.getRootResourceDirectories();
    assertThat(rootResourceDirectories.size(), is(1));
  }

  private void verifyFile(
      String filePath, String filename, String expectedMimeType, String... rootResourceDirectories)
      throws Exception {
    URLResourceReader resourceReader = new URLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(
        new HashSet<String>(Arrays.asList(rootResourceDirectories)));

    HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();

    LOGGER.info("Getting resource: {}", filePath);

    // Test using the URL ResourceReader
    File file = new File(filePath);

    URI uri = file.toURI();
    LOGGER.info("URI: {}", uri.toString());

    ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

    Resource resource = resourceResponse.getResource();
    assert (resource != null);

    LOGGER.info("MimeType: {}", resource.getMimeType());
    LOGGER.info("Got resource: {}", resource.getName());
    String name = resource.getName();
    assertNotNull(name);
    assertThat(name, is(filename));
    assertThat(resource.getMimeType().toString(), containsString(expectedMimeType));
  }

  private void verifyFileFromURLResourceReader(
      URI uri, String filename, String expectedMimeType, int expectedResponseResourceLength)
      throws URISyntaxException, IOException, ResourceNotFoundException {
    Response mockResponse = mock(Response.class);
    when(mockWebClient.get()).thenReturn(mockResponse);
    MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
    map.put(
        HttpHeaders.CONTENT_DISPOSITION,
        Arrays.<Object>asList("inline; filename=\"" + filename + "\""));
    when(mockResponse.getHeaders()).thenReturn(map);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    when(mockResponse.getEntity()).thenReturn(getBinaryData());
    verifyFileFromURLResourceReader(
        uri,
        filename,
        expectedMimeType,
        null,
        null,
        expectedResponseResourceLength,
        uri.toString());
  }

  // Create arguments, adding bytesToSkip and qualifier if present, and verify
  private void verifyFileFromURLResourceReader(
      URI uri,
      String filename,
      String expectedMimeType,
      String bytesToSkip,
      String qualifier,
      int expectedResponseResourceLength,
      String expectedWebClientUri)
      throws URISyntaxException, IOException, ResourceNotFoundException {

    Map<String, Serializable> arguments = new HashMap<String, Serializable>();

    if (bytesToSkip != null) {
      arguments.put(BYTES_TO_SKIP, bytesToSkip);
    }

    if (qualifier != null) {
      arguments.put(ContentItem.QUALIFIER_KEYWORD, qualifier);
    }

    TestURLResourceReader resourceReader =
        new TestURLResourceReader(mimeTypeMapper, clientFactoryFactory);
    resourceReader.setRootResourceDirectories(ImmutableSet.of(ABSOLUTE_PATH + TEST_PATH));

    // Test using the URL ResourceReader
    LOGGER.info("URI: {}", uri.toString());

    ResourceResponse resourceResponse = resourceReader.retrieveResource(uri, arguments);

    Resource resource = resourceResponse.getResource();
    assert (resource != null);

    LOGGER.info("MimeType: {}", resource.getMimeType());
    LOGGER.info("Got resource: {}", resource.getName());
    String name = resource.getName();
    assertNotNull(name);
    assertThat(name, is(filename));
    assertTrue(resource.getMimeType().toString().contains(expectedMimeType));

    assertThat(
        "The length of the resource in the response should be " + expectedResponseResourceLength,
        resourceResponse.getResource().getByteArray().length,
        is(expectedResponseResourceLength));

    assertThat(
        "The web client should be created with uri=" + uri,
        resourceReader.capturedWebClientUri,
        equalTo(expectedWebClientUri));
  }

  private class TestURLResourceReader extends URLResourceReader {

    public String capturedWebClientUri;

    public TestURLResourceReader(
        MimeTypeMapper mimeTypeMapper, ClientFactoryFactory clientFactoryFactory) {
      super(mimeTypeMapper, clientFactoryFactory);
    }

    @Override
    protected WebClient getWebClient(String uri, Map<String, Serializable> properties) {
      capturedWebClientUri = uri;
      return mockWebClient;
    }
  }
}
