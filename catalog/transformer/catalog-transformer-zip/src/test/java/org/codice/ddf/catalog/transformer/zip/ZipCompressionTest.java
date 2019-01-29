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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class ZipCompressionTest {

  private ZipCompression zipCompression;

  private static final SourceResponse EMPTY_SOURCE_RESPONSE =
      new SourceResponseImpl(null, Collections.emptyList());

  private static final SourceResponse SINGLE_RESULT_SOURCE_RESPONSE =
      new SourceResponseImpl(null, ImmutableList.of(new ResultImpl()));

  @Mock private BundleContext bundleContext;

  @Mock private MetacardTransformer transformer;

  @Before
  public void setUp() throws Exception {
    List<ServiceReference> serviceReferences =
        ImmutableList.of(
            createMockServiceReference("html", "text/html"),
            createMockServiceReference(null, "foobar"),
            createMockServiceReference("barfoo", null),
            createMockServiceReference("hello", "world"));

    zipCompression = new ZipCompression(bundleContext, serviceReferences);

    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(transformer);

    InputStream inputStream = ZipCompressionTest.class.getResourceAsStream("/export.html");
    when(transformer.transform(any(Metacard.class), any(Map.class)))
        .thenReturn(new BinaryContentImpl(inputStream));
  }

  @Test
  public void testCompressionWithSpecifiedTransformer() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacardId");
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", "html").build();

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
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", "barfoo").build();

    BinaryContent binaryContent = zipCompression.transform(sourceResponse, arguments);

    assertZipContents(binaryContent, Collections.singletonList("metacards/metacardId"));
  }

  @Test
  public void testCompressionWithSpecifiedFormatMimeTypeParserError() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacardId");
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", "hello").build();

    BinaryContent binaryContent = zipCompression.transform(sourceResponse, arguments);

    assertZipContents(binaryContent, Collections.singletonList("metacards/metacardId"));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionNullSourceResponse() throws Exception {
    zipCompression.transform(null, Collections.emptyMap());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionEmptySourceResponse() throws Exception {
    zipCompression.transform(EMPTY_SOURCE_RESPONSE, null);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressNullTransformerId() throws Exception {
    Map<String, Serializable> arguments = Collections.singletonMap("transformerId", null);

    zipCompression.transform(SINGLE_RESULT_SOURCE_RESPONSE, arguments);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressEmptyTransformerId() throws Exception {
    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", "").build();

    zipCompression.transform(SINGLE_RESULT_SOURCE_RESPONSE, arguments);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressBlankTransformerId() throws Exception {
    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", " ").build();

    zipCompression.transform(SINGLE_RESULT_SOURCE_RESPONSE, arguments);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCompressionWithSpecifiedTransformerNotFound() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    List<Result> results = Collections.singletonList(new ResultImpl(metacard));
    SourceResponse sourceResponse = new SourceResponseImpl(null, results);

    Map<String, Serializable> arguments =
        new ImmutableMap.Builder<String, Serializable>().put("transformerId", "foobar").build();

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

  private ServiceReference<MetacardTransformer> createMockServiceReference(
      String id, String mimeType) {
    ServiceReference<MetacardTransformer> serviceRef = mock(ServiceReference.class);

    when(serviceRef.getProperty("id")).thenReturn(id);
    when(serviceRef.getProperty("mime-type")).thenReturn(mimeType);

    return serviceRef;
  }
}
