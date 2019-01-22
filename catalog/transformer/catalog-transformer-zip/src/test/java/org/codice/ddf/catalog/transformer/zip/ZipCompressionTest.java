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
package org.codice.ddf.catalog.transformer.zip;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class ZipCompressionTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String LOCAL_RESOURCE_FILENAME = "localresource.txt";

  private static final String LOCAL_RESOURCE_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(LOCAL_RESOURCE_FILENAME).getPath();

  private static final String CONTENT_SCHEME = "content:";

  private static final String HTTP_SCHEME = "http://example.com";

  private static final String CONTENT_PATH = "content" + File.separator;

  private static final String PREVIEW_PATH = CONTENT_PATH + "preview" + File.separator;

  private static final String ID_1 = "id1";

  private static final String ID_2 = "id2";

  private static final String ID_3 = "id3";

  private static final String METACARD_1 = ZipCompression.METACARD_PATH + ID_1;

  private static final String METACARD_2 = ZipCompression.METACARD_PATH + ID_2;

  private static final String METACARD_3 = ZipCompression.METACARD_PATH + ID_3;

  private static final String METACARD_3_CONTENT =
      CONTENT_PATH + ID_3 + "-" + LOCAL_RESOURCE_FILENAME;

  private static final String METACARD_3_DERIVED_CONTENT =
      PREVIEW_PATH + ID_3 + "-" + LOCAL_RESOURCE_FILENAME;

  private static final List<String> METACARD_ID_LIST = Arrays.asList(ID_1, ID_2, ID_3);

  private static final List<String> METACARD_RESULT_LIST_NO_CONTENT =
      Arrays.asList(METACARD_1, METACARD_2, METACARD_3);

  private static final List<String> METACARD_RESULT_LIST_WITH_CONTENT =
      Arrays.asList(METACARD_1, METACARD_2, METACARD_3, METACARD_3_CONTENT);

  private static final List<String> METACARD_RESULT_LIST_WITH_CONTENT_AND_DERIVED_RESOURCES =
      Arrays.asList(
          METACARD_1, METACARD_2, METACARD_3, METACARD_3_CONTENT, METACARD_3_DERIVED_CONTENT);

  private ZipCompression zipCompression;

  private SourceResponse sourceResponse;

  private Map<String, Serializable> filePathArgument;

  private CatalogFramework catalogFramework;

  @Mock private BundleContext bundleContext;

  @Mock private MetacardTransformer transformer;

  @Before
  public void setUp() throws Exception {
    JarSigner jarSigner = mock(JarSigner.class);
    doNothing()
        .when(jarSigner)
        .signJar(any(File.class), anyString(), anyString(), anyString(), anyString());
    List<ServiceReference> serviceReferences =
        ImmutableList.of(
            createMockServiceReference("html", "text/html"),
            createMockServiceReference(null, "foobar"),
            createMockServiceReference("barfoo", null));
    zipCompression = new ZipCompression(jarSigner, serviceReferences, bundleContext);
    sourceResponse = createSourceResponseWithURISchemes(null, null);
    filePathArgument = new HashMap<>();
    filePathArgument.put(
        "filePath", temporaryFolder.getRoot().getAbsolutePath() + File.separator + "signed.zip");
    catalogFramework = mock(CatalogFramework.class);
    Resource resource = mock(Resource.class);
    InputStream resourceFileStream = new FileInputStream(new File(LOCAL_RESOURCE_PATH));
    when(resource.getName()).thenReturn(LOCAL_RESOURCE_FILENAME);
    when(resource.getInputStream()).thenReturn(resourceFileStream);
    ResourceResponse resourceResponse = new ResourceResponseImpl(resource);
    when(catalogFramework.getLocalResource(any(ResourceRequestById.class)))
        .thenReturn(resourceResponse);
    zipCompression.setCatalogFramework(catalogFramework);
  }

  @Test
  public void testGetCatalogFramework() {
    assertThat(catalogFramework, is(zipCompression.getCatalogFramework()));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionEmptyFilePathKeyArgument() throws Exception {
    HashMap<String, Serializable> arguments = new HashMap<>();
    arguments.put(ZipDecompression.FILE_PATH, "");
    zipCompression.transform(sourceResponse, arguments);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionNullSourceResponse() throws Exception {
    zipCompression.transform(null, filePathArgument);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressioNullListInSourceResponse() throws Exception {
    zipCompression.transform(new SourceResponseImpl(null, null), filePathArgument);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionEmptyListInSourceResponse() throws Exception {
    zipCompression.transform(new SourceResponseImpl(null, new ArrayList<>()), filePathArgument);
  }

  @Test
  public void testCompressionWithoutContent() throws Exception {
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_NO_CONTENT);
  }

  @Test
  public void testCompressionWithRemoteContent() throws Exception {
    SourceResponse sourceResponse = createSourceResponseWithURISchemes(HTTP_SCHEME, null);
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_NO_CONTENT);
  }

  @Test
  public void testCompressionWithLocalContent() throws Exception {
    SourceResponse sourceResponse = createSourceResponseWithURISchemes(CONTENT_SCHEME, null);
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_WITH_CONTENT);
  }

  @Test
  public void testCompressionWithDerivedContent() throws Exception {
    SourceResponse sourceResponse =
        createSourceResponseWithURISchemes(CONTENT_SCHEME, "content:id3#preview");
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_WITH_CONTENT_AND_DERIVED_RESOURCES);
  }

  @Test
  public void testCompressionWithNullResources() throws Exception {
    when(catalogFramework.getLocalResource(any(ResourceRequestById.class)))
        .thenThrow(ResourceNotFoundException.class);
    SourceResponse sourceResponse =
        createSourceResponseWithURISchemes(CONTENT_SCHEME, "content:1234#preview");
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_NO_CONTENT);
  }

  @Test
  public void testCompressionWithDerivedContentInvalidURI() throws Exception {
    String invalidUri = "^";
    SourceResponse sourceResponse = createSourceResponseWithURISchemes(CONTENT_SCHEME, invalidUri);
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_WITH_CONTENT);
  }

  @Test
  public void testCompressionWithDerivedContentNullFragment() throws Exception {
    SourceResponse sourceResponse =
        createSourceResponseWithURISchemes(CONTENT_SCHEME, "content:1234");
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_WITH_CONTENT);
  }

  @Test
  public void testCompressionWithRemoteDerivedContent() throws Exception {
    SourceResponse sourceResponse = createSourceResponseWithURISchemes(CONTENT_SCHEME, HTTP_SCHEME);
    BinaryContent binaryContent = zipCompression.transform(sourceResponse, filePathArgument);
    assertThat(binaryContent, notNullValue());
    assertZipContents(binaryContent, METACARD_RESULT_LIST_WITH_CONTENT);
  }

  @Test
  public void testCompressionWithSpecifiedTransformer() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacardId");
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>()
            .putAll(filePathArgument)
            .put("transformerId", "html")
            .build();

    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(transformer);

    InputStream inputStream = ZipCompressionTest.class.getResourceAsStream("/export.html");
    when(transformer.transform(any(Metacard.class), any(Map.class)))
        .thenReturn(new BinaryContentImpl(inputStream));

    BinaryContent binaryContent = zipCompression.transform(sourceResponse, arguments);

    assertZipContents(binaryContent, Collections.singletonList("metacards/metacardId.html"));
  }

  @Test
  public void testCompressionWithSpecifiedFormatMimeTypeNotFound() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacardId");
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>()
            .putAll(filePathArgument)
            .put("transformerId", "barfoo")
            .build();

    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(transformer);

    InputStream inputStream = ZipCompressionTest.class.getResourceAsStream("/export.html");
    when(transformer.transform(any(Metacard.class), any(Map.class)))
        .thenReturn(new BinaryContentImpl(inputStream));

    BinaryContent binaryContent = zipCompression.transform(sourceResponse, arguments);

    assertZipContents(binaryContent, Collections.singletonList("metacards/metacardId"));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionWithSpecifiedTransformerNotFound() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>()
            .putAll(filePathArgument)
            .put("transformerId", "foobar")
            .build();

    zipCompression.transform(sourceResponse, arguments);
  }

  private void assertZipContents(BinaryContent binaryContent, List<String> ids) throws IOException {
    ZipInputStream zipInputStream =
        new ZipInputStream(new ByteArrayInputStream(binaryContent.getByteArray()));
    List<String> entryNames = new ArrayList<>();

    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null) {
      entryNames.add(zipEntry.getName());
      zipEntry = zipInputStream.getNextEntry();
    }
    assertThat(entryNames.size(), is(ids.size()));

    for (String id : ids) {
      assertThat(entryNames, hasItem(id));
    }
  }

  private SourceResponse createSourceResponseWithURISchemes(
      String scheme, String derivedResourceScheme) throws Exception {

    List<Result> resultList = new ArrayList<>();

    for (String string : METACARD_ID_LIST) {
      MetacardImpl metacard = new MetacardImpl();
      metacard.setId(string);

      if (scheme != null && string.equals(METACARD_ID_LIST.get(METACARD_ID_LIST.size() - 1))) {

        URI uri = new URI(scheme + metacard.getId());
        metacard.setResourceURI(uri);
        if (StringUtils.isNotBlank(derivedResourceScheme)) {
          metacard.setAttribute(Metacard.DERIVED_RESOURCE_URI, derivedResourceScheme);
        }
      }
      Result result = new ResultImpl(metacard);
      resultList.add(result);
    }

    return new SourceResponseImpl(null, resultList);
  }

  private ServiceReference<MetacardTransformer> createMockServiceReference(
      String id, String mimeType) {
    ServiceReference<MetacardTransformer> serviceRef = mock(ServiceReference.class);

    when(serviceRef.getProperty("id")).thenReturn(id);
    when(serviceRef.getProperty("mime-type")).thenReturn(mimeType);

    return serviceRef;
  }
}
