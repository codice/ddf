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
package ddf.catalog.transformer.output.rtf;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.activation.MimeTypeParseException;
import org.junit.Test;

public class RtfQueryResponseAndMetacardTransformerTest extends BaseTestConfiguration {

  @Test
  public void testCreateTransformer() throws MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformNullMetacard()
      throws CatalogTransformerException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    Metacard nullCard = null;

    transformer.transform(nullCard, Collections.emptyMap());
  }

  @Test
  public void testTransformMetacard()
      throws CatalogTransformerException, IOException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    Metacard mockMetacard = createMockMetacard("Test Metacard Title");

    BinaryContent content = transformer.transform(mockMetacard, Collections.emptyMap());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtf = getReferenceMetacardRtfFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtf));
  }

  @Test
  public void testTransformMetacardWithMalformedImageData()
      throws MimeTypeParseException, CatalogTransformerException, IOException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    Metacard mockMetacard = createMockMetacardWithBadImageData("Test Metacard With Bad Image");

    BinaryContent content = transformer.transform(mockMetacard, Collections.emptyMap());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtfWithEmptyThumbnail = getReferenceMetacardRtfWithEmptyThumbnailFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtfWithEmptyThumbnail));
  }

  @Test
  public void testTransformMetacardWithGifThumbnail()
      throws MimeTypeParseException, CatalogTransformerException, IOException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    Metacard mockMetacard = createMockMetacardWithGifImageData("Test Metacard With Gif Thumbnail");

    BinaryContent content = transformer.transform(mockMetacard, Collections.emptyMap());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtfWithEmptyThumbnail = getReferenceMetacardWithGifRtfFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtfWithEmptyThumbnail));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformNullSourceResponse()
      throws CatalogTransformerException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    SourceResponse response = null;
    transformer.transform(response, Collections.emptyMap());
  }

  @Test
  public void testTransformSourceResponse()
      throws CatalogTransformerException, IOException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    SourceResponse mockSourceResponse = mock(SourceResponse.class);

    List<Result> results = createMockResults();
    when(mockSourceResponse.getResults()).thenReturn(results);

    BinaryContent content = transformer.transform(mockSourceResponse, Collections.emptyMap());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtf = getReferenceSourceResponseRtfFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtf));
  }

  @Test
  public void testTransformMetacardWithArgs()
      throws CatalogTransformerException, IOException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    Metacard mockMetacard = createMockMetacard("Test Metacard Title");

    Map<String, Serializable> arguments = new HashMap<>();
    arguments.put(
        RtfQueryResponseAndMetacardTransformer.COLUMN_ORDER_KEY,
        COLUMN_ORDER.stream().collect(Collectors.joining(",")));
    arguments.put(
        RtfQueryResponseAndMetacardTransformer.COLUMN_ALIAS_KEY,
        COLUMN_ALIASES.entrySet().stream()
            .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
            .collect(Collectors.joining(",")));

    BinaryContent content = transformer.transform(mockMetacard, arguments);

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtf = getReferenceMetacardWithArgsRtfFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtf));
  }

  @Test
  public void testTransformSourceResponseWithArgs()
      throws CatalogTransformerException, IOException, MimeTypeParseException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    SourceResponse mockSourceResponse = mock(SourceResponse.class);

    List<Result> results = createMockResults();
    when(mockSourceResponse.getResults()).thenReturn(results);
    Map<String, Serializable> arguments = new HashMap<>();
    arguments.put(
        RtfQueryResponseAndMetacardTransformer.COLUMN_ORDER_KEY, new LinkedList<>(COLUMN_ORDER));
    arguments.put(
        RtfQueryResponseAndMetacardTransformer.COLUMN_ALIAS_KEY, new HashMap<>(COLUMN_ALIASES));

    BinaryContent content = transformer.transform(mockSourceResponse, arguments);

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));

    String rtfResult = inputStreamToString(content.getInputStream());

    assertThat("Content must not be empty", rtfResult, is(not(isEmptyOrNullString())));

    String referenceRtf = getReferenceSourceResponseWithArgsRtfFile();

    assertThat(
        "Produced RTF document must match reference",
        rtfResult,
        equalToIgnoringWhiteSpace(referenceRtf));
  }

  private RtfQueryResponseAndMetacardTransformer createTransformer() throws MimeTypeParseException {
    return new RtfQueryResponseAndMetacardTransformer(MOCK_CATEGORY);
  }

  List<Result> createMockResults() {
    return IntStream.range(1, 5)
        .mapToObj(value -> "Metacard " + value)
        .map(this::createMockMetacard)
        .map(
            metacard -> {
              Result result = mock(Result.class);
              when(result.getMetacard()).thenReturn(metacard);
              return result;
            })
        .collect(Collectors.toList());
  }
}
