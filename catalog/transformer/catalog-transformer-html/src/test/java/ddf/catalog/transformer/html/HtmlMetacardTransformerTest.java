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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class HtmlMetacardTransformerTest {

  private static final String METACARD_CLASS = ".metacard";

  private static final List<HtmlExportCategory> EMPTY_CATEGORY_LIST = Collections.emptyList();

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransform() throws CatalogTransformerException {
    HtmlMetacardTransformer htmlTransformer = new HtmlMetacardTransformer(EMPTY_CATEGORY_LIST);
    htmlTransformer.transform(null, Collections.emptyMap());
  }

  @Test
  public void testMetacardTransform() throws CatalogTransformerException, IOException {
    Metacard metacard = new MetacardImpl();
    HtmlMetacardTransformer htmlTransformer = new HtmlMetacardTransformer(EMPTY_CATEGORY_LIST);
    BinaryContent binaryContent = htmlTransformer.transform(metacard, Collections.emptyMap());

    Document doc = getHtmlDocument(binaryContent);

    assertThat(doc.select(METACARD_CLASS), hasSize(1));
  }

  private Document getHtmlDocument(BinaryContent binaryContent) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(binaryContent.getInputStream(), writer, "UTF-8");

    return Jsoup.parse(writer.toString());
  }
}
