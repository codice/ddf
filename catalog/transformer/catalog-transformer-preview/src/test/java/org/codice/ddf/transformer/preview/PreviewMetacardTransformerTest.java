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
package org.codice.ddf.transformer.preview;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PreviewMetacardTransformerTest {

  private static final String NO_PREVIEW =
      "<head><meta charset=\"utf-8\"/>No preview text available.</head>";

  private static final String EXTRACTED_TEXT = "Some value\nAnother value";

  private static final String TRANSFORMED_TEXT =
      "<head><meta charset=\"utf-8\"/>Some value<br>Another value</head>";

  private static final String METADATA =
          "<?xml version=\"1.0\"?>\n"
                  + "<metacard xmlns=\"urn:catalog:metacard\" xmlns:gml=\"http://www.opengis.net/gml\">\n"
                  + "  <type>ddf.metacard</type>\n"
                  + "  <source>ddf.distribution</source>\n"
                  + "  <stringxml name=\"metadata\">\n"
                  + "    <value>\n"
                  + "      <REUTERS>\n"
                  + "        <TEXT>\n"
                  + "          <BODY>"
                  + EXTRACTED_TEXT
                  + "          </BODY>\n"
                  + "        </TEXT>\n"
                  + "      </REUTERS>\n"
                  + "    </value>\n"
                  + "  </stringxml>\n"
                  + "</metacard>";

  private PreviewMetacardTransformer previewMetacardTransformer = new PreviewMetacardTransformer();

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardThrowsCatalogTransformerException() throws Exception {
    previewMetacardTransformer.transform(null, null);
  }

  @Test
  public void noExtractedText() throws CatalogTransformerException, IOException {
    Metacard metacard = mock(Metacard.class);
    BinaryContent content = previewMetacardTransformer.transform(metacard, null);

    assertThat(content, notNullValue());

    String preview = new String(content.getByteArray());
    assertThat(preview, is(equalTo(NO_PREVIEW)));
  }

  @Test
  public void testTransformation() throws CatalogTransformerException, IOException {
    Attribute extractedText = new AttributeImpl(Extracted.EXTRACTED_TEXT, EXTRACTED_TEXT);
    Metacard metacard = mock(Metacard.class);

    doReturn(extractedText).when(metacard).getAttribute(Extracted.EXTRACTED_TEXT);

    BinaryContent content = previewMetacardTransformer.transform(metacard, null);

    assertThat(content, notNullValue());

    String preview = new String(content.getByteArray());
    assertThat(preview, is(equalTo(TRANSFORMED_TEXT)));
  }

  /** Test that text-based products can still have previews even without a set EXTRACTED_TEXT */
  @Test
  public void testMetacardTextPreviewMetadataSource()
      throws CatalogTransformerException, IOException {

    Metacard metacard = mock(Metacard.class);

    doReturn(null).when(metacard).getAttribute(Extracted.EXTRACTED_TEXT);
    doReturn(METADATA).when(metacard).getMetadata();

    List<String> previewElements = new ArrayList<>();
    previewElements.add("text");
    previewElements.add("TEXT");

    previewMetacardTransformer.setPreviewFromMetadata(true);
    previewMetacardTransformer.setPreviewElements(previewElements);
    BinaryContent content = previewMetacardTransformer.transform(metacard, null);

    assertThat(content, notNullValue());

    String preview = new String(content.getByteArray());
    assertThat(preview, is(equalTo(TRANSFORMED_TEXT)));
  }

  @Test
  public void testMetacardTextPreviewMetadataNoMatchingXPath()
          throws CatalogTransformerException, IOException {
    Metacard metacard = mock(Metacard.class);

    doReturn(null).when(metacard).getAttribute(Extracted.EXTRACTED_TEXT);
    doReturn(METADATA).when(metacard).getMetadata();

    // make specified preview elements invalid for xpath
    List<String> previewElements = new ArrayList<>();
    previewElements.add("no match");

    previewMetacardTransformer.setPreviewFromMetadata(true);
    previewMetacardTransformer.setPreviewElements(previewElements);
    BinaryContent content = previewMetacardTransformer.transform(metacard, null);

    assertThat(content, notNullValue());

    String preview = new String(content.getByteArray());
    assertThat(preview, is(equalTo(NO_PREVIEW)));
  }

}
