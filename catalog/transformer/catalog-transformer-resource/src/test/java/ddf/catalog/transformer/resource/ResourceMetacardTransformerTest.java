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
package ddf.catalog.transformer.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Unit tests for the Resource Transformer. */
public class ResourceMetacardTransformerTest {

  private static final String TEST_ID = "123456";

  private static final String TEST_SITE = "ddf";

  private static final String TEST_PATH = "/src/test/resources/data/";

  private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

  private static final String JPEG_FILE_NAME_1 = "flower.jpg";

  private static final String JPEG_MIME_TYPE = "image/jpeg";

  private static final String VIDEO_MIME_TYPE = "video/mpeg";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final String TS_FILE_NAME_1 = "transport-stream.ts";

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGetResourceJpeg() throws Exception {
    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    boolean expectSuccess = true;
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    CatalogFramework framework = getFramework(getResourceResponse(getResource(mimeType, uri)));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  /**
   * Tests that the metacard source name is passed to the {@link CatalogFramework}
   *
   * @throws Exception
   */
  @Test
  public void testGetResourceJpegFromFedSource() throws Exception {

    // given
    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    String federatedSourceId = "fedSourceId";
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri, federatedSourceId);
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    ArgumentCapture capture = new ArgumentCapture(getResourceResponse(getResource(mimeType, uri)));
    CatalogFramework framework = givenFramework(capture);
    ResourceMetacardTransformer resourceTransformer = new ResourceMetacardTransformer(framework);

    // when
    resourceTransformer.transform(metacard, new HashMap<String, Serializable>());

    // then
    assertEquals(federatedSourceId, capture.inputArgs[1]);
  }

  @Test
  public void testGetResourceTransportStream() throws Exception {
    String filePath = ABSOLUTE_PATH + TEST_PATH + TS_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    boolean expectSuccess = true;
    MimeType mimeType = getMimeType(VIDEO_MIME_TYPE);
    CatalogFramework framework = getFramework(getResourceResponse(getResource(mimeType, uri)));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testNullMetacard() throws Exception {
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage(
        "Could not transform metacard to a resource because the metacard is not valid.");
    String filePath = ABSOLUTE_PATH + TEST_PATH + TS_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = null;
    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(VIDEO_MIME_TYPE);
    CatalogFramework framework = getFramework(getResourceResponse(getResource(mimeType, uri)));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testNullMetacardId() throws Exception {
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage(
        "Could not transform metacard to a resource because the metacard is not valid.");
    String filePath = ABSOLUTE_PATH + TEST_PATH + TS_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    when(metacard.getId()).thenReturn(null);
    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(VIDEO_MIME_TYPE);
    CatalogFramework framework = getFramework(getResourceResponse(getResource(mimeType, uri)));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testNullResourceUri() throws Exception {
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Unable to retrieve resource.");
    Metacard metacard = getMockMetacard(null);
    CatalogFramework framework = getFrameworkException(new ResourceNotFoundException(""));
    ResourceMetacardTransformer resourceTransformer = new ResourceMetacardTransformer(framework);
    resourceTransformer.transform(metacard, new HashMap<String, Serializable>());
  }

  @Test
  public void testNullResourceResponse() throws Exception {

    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Unable to retrieve resource.");
    thrown.expectMessage("Metacard id: " + TEST_ID);
    thrown.expectMessage("Uri: " + uri);
    thrown.expectMessage("Source: " + TEST_SITE);
    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    CatalogFramework framework = getFramework(null);
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testFrameworkThrowsIoException() throws Exception {

    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Unable to retrieve resource.");
    thrown.expectMessage("Metacard id: " + TEST_ID);
    thrown.expectMessage("Uri: " + uri);
    thrown.expectMessage("Source: " + TEST_SITE);

    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    CatalogFramework framework = getFrameworkException(new IOException("Test IO Exception"));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testFrameworkThrowsResourceNotFoundException() throws Exception {
    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);

    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Unable to retrieve resource.");
    thrown.expectMessage("Metacard id: " + TEST_ID);
    thrown.expectMessage("Uri: " + uri);
    thrown.expectMessage("Source: " + TEST_SITE);

    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    CatalogFramework framework =
        getFrameworkException(new ResourceNotFoundException("Test Resource Not Found Exception"));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testFrameworkThrowsResourceNotSupportedException() throws Exception {

    String filePath = ABSOLUTE_PATH + TEST_PATH + JPEG_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Unable to retrieve resource.");
    thrown.expectMessage("Metacard id: " + TEST_ID);
    thrown.expectMessage("Uri: " + uri);
    thrown.expectMessage("Source: " + TEST_SITE);
    boolean expectSuccess = false;
    MimeType mimeType = getMimeType(JPEG_MIME_TYPE);
    CatalogFramework framework =
        getFrameworkException(
            new ResourceNotSupportedException("Test Resource Not Supported Exception"));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  @Test
  public void testResourceHasNullMimeType() throws Exception {
    String filePath = ABSOLUTE_PATH + TEST_PATH + TS_FILE_NAME_1;
    URI uri = getUri(filePath);
    Metacard metacard = getMockMetacard(uri);
    boolean expectSuccess = true;
    MimeType mimeType = getMimeType(DEFAULT_MIME_TYPE);
    CatalogFramework framework = getFramework(getResourceResponse(getResource(null, uri)));
    testGetResource(metacard, filePath, mimeType, framework, expectSuccess);
  }

  private void testGetResource(
      Metacard metacard,
      String filePath,
      MimeType mimeType,
      CatalogFramework framework,
      boolean expectSuccess)
      throws Exception {

    ResourceMetacardTransformer resourceTransformer = new ResourceMetacardTransformer(framework);

    BinaryContent binaryContent =
        resourceTransformer.transform(metacard, new HashMap<String, Serializable>());

    byte[] fileContents = FileUtils.readFileToByteArray(new File(filePath));

    byte[] contentsFromResults = IOUtils.toByteArray(binaryContent.getInputStream());
    if (expectSuccess) {
      assertEquals(binaryContent.getMimeTypeValue(), mimeType.toString());
      assertTrue(Arrays.equals(fileContents, contentsFromResults));
    }
  }

  private ResourceResponse getResourceResponse(Resource resource) {
    ResourceResponse resourceResponse = mock(ResourceResponse.class);
    when(resourceResponse.getResource()).thenReturn(resource);
    return resourceResponse;
  }

  private CatalogFramework givenFramework(ArgumentCapture answer)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {

    CatalogFramework framework = mock(CatalogFramework.class);

    when(framework.getId()).thenReturn(TEST_SITE);

    when(framework.getResource(any(ResourceRequest.class), isA(String.class))).thenAnswer(answer);

    return framework;
  }

  private CatalogFramework getFramework(ResourceResponse resourceResponse) throws Exception {
    CatalogFramework framework = mock(CatalogFramework.class);
    when(framework.getId()).thenReturn(TEST_SITE);
    when(framework.getResource(any(ResourceRequest.class), eq(TEST_SITE)))
        .thenReturn(resourceResponse);
    return framework;
  }

  private CatalogFramework getFrameworkException(Exception e) throws Exception {
    CatalogFramework framework = mock(CatalogFramework.class);
    when(framework.getId()).thenReturn(TEST_SITE);
    when(framework.getResource(any(ResourceRequest.class), eq(TEST_SITE))).thenThrow(e);
    return framework;
  }

  private Resource getResource(MimeType mimeType, URI uri) throws Exception {
    Resource resource = mock(Resource.class);
    when(resource.getMimeType()).thenReturn(mimeType);
    when(resource.getMimeTypeValue())
        .thenReturn((mimeType == null) ? null : mimeType.getBaseType());
    when(resource.getInputStream()).thenReturn(uri.toURL().openConnection().getInputStream());
    return resource;
  }

  private Metacard getMockMetacard(URI resourceUri, String sourceName) {
    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(TEST_ID);
    when(metacard.getResourceURI()).thenReturn(resourceUri);
    when(metacard.getSourceId()).thenReturn(sourceName);
    return metacard;
  }

  private Metacard getMockMetacard(URI resourceUri) {
    return getMockMetacard(resourceUri, null);
  }

  private MimeType getMimeType(String mimeTypeStr) throws Exception {
    return new MimeType(mimeTypeStr);
  }

  private URI getUri(String filePath) {
    File file = new File(filePath);
    URI uri = file.toURI();
    return uri;
  }

  private class ArgumentCapture implements Answer<ResourceResponse> {

    private Object[] inputArgs;

    private ResourceResponse resourceResponse;

    public ArgumentCapture(ResourceResponse resourceResponse) {
      this.resourceResponse = resourceResponse;
    }

    public Object[] getInputArgs() {
      return inputArgs;
    }

    @Override
    public ResourceResponse answer(InvocationOnMock invocation) throws Throwable {
      this.inputArgs = invocation.getArguments();

      return this.resourceResponse;
    }
  }
}
