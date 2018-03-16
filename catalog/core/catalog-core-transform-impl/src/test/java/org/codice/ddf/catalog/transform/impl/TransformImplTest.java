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
package org.codice.ddf.catalog.transform.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.catalog.locator.TransformerLocator;
import org.codice.ddf.catalog.transform.MultiInputTransformer;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
public class TransformImplTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File tempFile;

  private Metacard metacard;

  private TransformerLocator transformerLocator;

  private Transform transform;

  private Supplier<String> idSupplier;

  @Before
  public void setup() throws IOException {
    tempFile = temporaryFolder.newFile("tempFile");
    metacard = mock(Metacard.class);
    transformerLocator = mock(TransformerLocator.class);
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    transform = new TransformImpl(transformerLocator, uuidGenerator);
    idSupplier = mock(Supplier.class);
    when(idSupplier.get()).thenReturn(null);
  }

  @Test
  public void testTransformSourceResponseByTransformerId() throws CatalogTransformerException {

    BinaryContent mockBinaryContent = mock(BinaryContent.class);

    QueryResponseTransformer transformer = mock(QueryResponseTransformer.class);
    when(transformer.transform(any(SourceResponse.class), any(Map.class)))
        .thenReturn(mockBinaryContent);

    when(transformerLocator.findQueryResponseTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    BinaryContent binaryContent =
        transform.transform(mock(SourceResponse.class), "xyz", Collections.emptyMap());

    assertThat(binaryContent, is(mockBinaryContent));
  }

  @Test
  public void testTransformSourceResponseByMimeType()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent mockBinaryContent = mock(BinaryContent.class);

    QueryResponseTransformer transformer = mock(QueryResponseTransformer.class);
    when(transformer.transform(any(SourceResponse.class), any(Map.class)))
        .thenReturn(mockBinaryContent);

    when(transformerLocator.findQueryResponseTransformers(any(MimeType.class)))
        .thenReturn(Collections.singletonList(transformer));

    BinaryContent binaryContent =
        transform.transform(
            mock(SourceResponse.class), new MimeType("text/xml"), Collections.emptyMap());

    assertThat(binaryContent, is(mockBinaryContent));
  }

  @Test
  public void testIsMetacardTransformerIdValidBadId() throws CatalogTransformerException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    boolean isValid = transform.isMetacardTransformerIdValid("xxx");

    assertThat(isValid, is(false));
  }

  @Test
  public void testIsMetacardTransformerIdValidGoodId() throws CatalogTransformerException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    boolean isValid = transform.isMetacardTransformerIdValid("xyz");

    assertThat(isValid, is(true));
  }

  @Test
  public void testTransformInputStreamByTransformerId()
      throws MimeTypeParseException, MetacardCreationException, IOException,
          CatalogTransformerException {

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    MultiInputTransformer transformer = mock(MultiInputTransformer.class);
    when(transformer.transform(any(InputStream.class), any(Map.class)))
        .thenReturn(transformResponse);

    when(transformerLocator.findMultiInputTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    TransformResponse transformResponseActual =
        transform.transform(
            new MimeType("text/xml"),
            "1",
            idSupplier,
            new ByteArrayInputStream("<xml></xml>".getBytes()),
            "xyz",
            Collections.emptyMap());

    assertThat(transformResponseActual.getParentMetacard().isPresent(), is(true));
    assertThat(transformResponseActual.getParentMetacard().get(), is(metacard));
    assertThat(transformResponseActual.getDerivedContentItems(), hasSize(0));
    assertThat(transformResponseActual.getDerivedMetacards(), hasSize(0));
  }

  @Test
  public void testTransformInputStreamByMimeType()
      throws MimeTypeParseException, MetacardCreationException, IOException,
          CatalogTransformerException {

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    MultiInputTransformer transformer = mock(MultiInputTransformer.class);
    when(transformer.transform(any(InputStream.class), any(Map.class)))
        .thenReturn(transformResponse);

    when(transformerLocator.findMultiInputTransformers(any(MimeType.class)))
        .thenReturn(Collections.singletonList(transformer));

    TransformResponse transformResponseActual =
        transform.transform(
            new MimeType("text/xml"),
            "1",
            idSupplier,
            new ByteArrayInputStream("<xml></xml>".getBytes()),
            null,
            Collections.emptyMap());

    assertThat(transformResponseActual.getParentMetacard().isPresent(), is(true));
    assertThat(transformResponseActual.getParentMetacard().get(), is(metacard));
  }

  @Test(expected = MetacardCreationException.class)
  public void testTransformInputStreamByTransformerIdAllTransformersFail()
      throws MimeTypeParseException, MetacardCreationException, IOException,
          CatalogTransformerException {

    MultiInputTransformer transformer1 = mock(MultiInputTransformer.class);
    when(transformer1.transform(any(InputStream.class), any(Map.class)))
        .thenThrow(CatalogTransformerException.class);

    MultiInputTransformer transformer2 = mock(MultiInputTransformer.class);
    when(transformer2.transform(any(InputStream.class), any(Map.class)))
        .thenThrow(CatalogTransformerException.class);

    when(transformerLocator.findMultiInputTransformers(eq("xyz")))
        .thenReturn(Arrays.asList(transformer1, transformer2));

    transform.transform(
        new MimeType("text/xml"),
        "1",
        idSupplier,
        new ByteArrayInputStream("<xml></xml>".getBytes()),
        "xyz",
        Collections.emptyMap());
  }

  @Test(expected = MetacardCreationException.class)
  public void testTransformInputStreamByTransformerIdTransformerThrowsIOException()
      throws MimeTypeParseException, MetacardCreationException, IOException,
          CatalogTransformerException {

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    MultiInputTransformer transformer = mock(MultiInputTransformer.class);
    when(transformer.transform(any(InputStream.class), any(Map.class)))
        .thenReturn(transformResponse);

    when(transformerLocator.findMultiInputTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    InputStream inputStream = mock(InputStream.class);
    when(inputStream.read(any())).thenThrow(IOException.class);

    transform.transform(
        new MimeType("text/xml"), "1", idSupplier, inputStream, "xyz", Collections.emptyMap());
  }

  @Test
  public void testTransformFile()
      throws IOException, CatalogTransformerException, MimeTypeParseException,
          MetacardCreationException {

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    MultiInputTransformer transformer = mock(MultiInputTransformer.class);
    when(transformer.transform(any(InputStream.class), any(Map.class)))
        .thenReturn(transformResponse);

    when(transformerLocator.findMultiInputTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      fos.write("<xml></xml>".getBytes());
    }
    final ArgumentCaptor<Attribute> captor = ArgumentCaptor.forClass(Attribute.class);

    TransformResponse transformResponseActual =
        transform.transform(
            new MimeType("text/xml"),
            "1",
            idSupplier,
            "fileName",
            tempFile,
            "xyz",
            Collections.emptyMap());

    assertThat(transformResponseActual.getParentMetacard().isPresent(), is(true));
    assertThat(transformResponseActual.getParentMetacard().get(), is(metacard));

    verify(metacard, times(2)).setAttribute(captor.capture());

    assertThat(captor.getValue().getName(), is(Metacard.TITLE));
    assertThat(captor.getValue().getValues(), is(Collections.singletonList("fileName")));
  }

  @Test
  public void testTransformMetacardsByTransformerId()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    List<BinaryContent> binaryContents =
        transform.transform(
            Collections.singletonList(metacard),
            new MimeType("text/xml"),
            "xyz",
            Collections.emptyMap());

    assertThat(binaryContents, hasSize(1));
    assertThat(binaryContents.get(0), is(binaryContent));
  }

  @Test
  public void testTransformMetacardsByMimeType()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(any(MimeType.class)))
        .thenReturn(Collections.singletonList(transformer));

    List<BinaryContent> binaryContents =
        transform.transform(
            Collections.singletonList(metacard),
            new MimeType("text/xml"),
            null,
            Collections.emptyMap());

    assertThat(binaryContents, hasSize(1));
    assertThat(binaryContents.get(0), is(binaryContent));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformMetacardsEmptyMetacards()
      throws CatalogTransformerException, MimeTypeParseException {

    transform.transform(
        Collections.emptyList(), new MimeType("text/xml"), "xyz", Collections.emptyMap());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformMetacardsEmptyTransformers()
      throws CatalogTransformerException, MimeTypeParseException {

    when(transformerLocator.findMultiMetacardTransformers(any(MimeType.class)))
        .thenReturn(Collections.emptyList());

    transform.transform(
        Collections.singletonList(metacard),
        new MimeType("text/xml"),
        null,
        Collections.emptyMap());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformMetacardsEmptyBinaryContents()
      throws CatalogTransformerException, MimeTypeParseException {

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.emptyList());

    when(transformerLocator.findMultiMetacardTransformers(any(MimeType.class)))
        .thenReturn(Collections.singletonList(transformer));

    transform.transform(
        Collections.singletonList(metacard),
        new MimeType("text/xml"),
        null,
        Collections.emptyMap());
  }

  @Test
  public void testTransformMetacardsOneFailOneSuccess()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer1 = mock(MultiMetacardTransformer.class);
    when(transformer1.transform(any(List.class), any(Map.class)))
        .thenThrow(CatalogTransformerException.class);

    MultiMetacardTransformer transformer2 = mock(MultiMetacardTransformer.class);
    when(transformer2.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(any(MimeType.class)))
        .thenReturn(Arrays.asList(transformer1, transformer2));

    List<BinaryContent> binaryContents =
        transform.transform(
            Collections.singletonList(metacard),
            new MimeType("text/xml"),
            null,
            Collections.emptyMap());

    assertThat(binaryContents, hasSize(1));
    assertThat(binaryContents.get(0), is(binaryContent));
  }

  @Test
  public void testTransformMetacardsByTransformerIdOnly()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    List<BinaryContent> binaryContents =
        transform.transform(Collections.singletonList(metacard), "xyz", Collections.emptyMap());

    assertThat(binaryContents, hasSize(1));
    assertThat(binaryContents.get(0), is(binaryContent));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformMetacardsByTransformerIdOnlyEmptyMetacards()
      throws CatalogTransformerException, MimeTypeParseException {

    transform.transform(Collections.emptyList(), "xyz", Collections.emptyMap());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformMetacardsByTransformerIdOnlyEmptyTransformerId()
      throws CatalogTransformerException, MimeTypeParseException {

    transform.transform(Collections.singletonList(metacard), null, Collections.emptyMap());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformMetacardsByTransformerIdOnlyTransformerNotFound()
      throws CatalogTransformerException, MimeTypeParseException {

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.emptyList());

    transform.transform(Collections.singletonList(metacard), "xyz", Collections.emptyMap());
  }

  @Test
  public void testTransformMetacardsByTransformerIdOnlyOneFailOneSuccess()
      throws CatalogTransformerException, MimeTypeParseException {

    BinaryContent binaryContent = mock(BinaryContent.class);

    MultiMetacardTransformer transformer1 = mock(MultiMetacardTransformer.class);
    when(transformer1.transform(any(List.class), any(Map.class)))
        .thenThrow(CatalogTransformerException.class);

    MultiMetacardTransformer transformer2 = mock(MultiMetacardTransformer.class);
    when(transformer2.transform(any(List.class), any(Map.class)))
        .thenReturn(Collections.singletonList(binaryContent));

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Arrays.asList(transformer1, transformer2));

    List<BinaryContent> binaryContents =
        transform.transform(Collections.singletonList(metacard), "xyz", Collections.emptyMap());

    assertThat(binaryContents, hasSize(1));
    assertThat(binaryContents.get(0), is(binaryContent));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformMetacardsByTransformerIdOnlyEmptyBinaryContents()
      throws CatalogTransformerException, MimeTypeParseException {

    MultiMetacardTransformer transformer = mock(MultiMetacardTransformer.class);
    when(transformer.transform(any(List.class), any(Map.class)))
        .thenThrow(CatalogTransformerException.class);

    when(transformerLocator.findMultiMetacardTransformers(eq("xyz")))
        .thenReturn(Collections.singletonList(transformer));

    transform.transform(Collections.singletonList(metacard), "xyz", Collections.emptyMap());
  }
}
