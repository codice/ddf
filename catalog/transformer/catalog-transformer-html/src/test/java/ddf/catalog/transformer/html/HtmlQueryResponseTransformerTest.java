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
package ddf.catalog.transformer.html;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

public class HtmlQueryResponseTransformerTest {

  private static HtmlQueryResponseTransformer htmlTransformer;

  private static final String METACARD_CLASS = ".metacard";

  private static final List<HtmlExportCategory> EMPTY_CATEGORY_LIST = Collections.emptyList();

  @Before
  public void setup() {
    htmlTransformer = new HtmlQueryResponseTransformer(EMPTY_CATEGORY_LIST);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransform() throws CatalogTransformerException {
    htmlTransformer.transform(null, Collections.emptyMap());
  }

  @Test
  public void testQueryResponseTransform() throws CatalogTransformerException, IOException {
    SourceResponse sourceResponse = createSourceResponse(2, 2L);
    BinaryContent binaryContent = htmlTransformer.transform(sourceResponse, null);

    Document doc = Jsoup.parse(binaryContent.getInputStream(), "UTF-8", "");

    assertThat(doc.select(METACARD_CLASS), hasSize(sourceResponse.getResults().size()));
  }

  private SourceResponse createSourceResponse(int count, long hits) throws IOException {
    List<Result> results = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      results.add(createResult());
    }

    return new SourceResponseImpl(null, results, hits);
  }

  private Result createResult() throws IOException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("Hello World");

    return new ResultImpl(metacard);
  }
}
