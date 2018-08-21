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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

public class RtfQueryResponseAndMetacardTransformerTest extends BaseTestConfiguration {

  private List<RtfCategory> mockCategories;

  @Before
  public void setup() {
    mockCategories = getCategories();
  }

  @Test
  public void testCreateTransformer() {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformNullMetacard() throws CatalogTransformerException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());

    Metacard nullCard = null;

    transformer.transform(nullCard, new HashMap<>());
  }

  @Test
  public void testTransformMetacard() throws CatalogTransformerException, IOException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());

    Metacard mockMetacard = createMockMetacard("Test Metacard Title");

    BinaryContent content = transformer.transform(mockMetacard, new HashMap<>());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));
    assertThat("Content must not be empty", content.getByteArray().length, greaterThan(0));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testTransformNullSourceResponse() throws CatalogTransformerException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());

    SourceResponse response = null;
    transformer.transform(response, new HashMap<>());
  }

  @Test
  public void testTransformSourceResponse() throws CatalogTransformerException, IOException {
    RtfQueryResponseAndMetacardTransformer transformer = createTransformer();

    assertThat("Transformer cannot be null", transformer, notNullValue());

    SourceResponse mockSourceResponse = mock(SourceResponse.class);

    List<Result> results = createMockResults();
    when(mockSourceResponse.getResults()).thenReturn(results);

    when(mockSourceResponse.getResults()).thenReturn(results);

    BinaryContent content = transformer.transform(mockSourceResponse, new HashMap<>());

    assertThat("Transformed content cannot be null", content, notNullValue());
    assertThat(
        "Content mime type must be 'application/rtf'",
        content.getMimeType().toString(),
        equalTo("application/rtf"));
    assertThat("Content must not be empty", content.getByteArray().length, greaterThan(0));
  }

  private RtfQueryResponseAndMetacardTransformer createTransformer() {
    return new RtfQueryResponseAndMetacardTransformer(mockCategories);
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
