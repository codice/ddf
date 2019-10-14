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
import org.junit.Test;

public class PreviewMetacardTransformerTest {

  private static final String NO_PREVIEW = "No preview text available.";

  private static final String EXTRACTED_TEXT = "\nSome value";

  private static final String TRANSFORMED_TEXT =
      "<head><meta charset=\"utf-8\"/><br>Some value</head>";

  private PreviewMetacardTransformer previewMetacardTransformer = new PreviewMetacardTransformer();

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardThrowsCatalogTransformerException() throws Exception {
    previewMetacardTransformer.transform(null, null);
  }

  @Test
  public void noExtractedText() throws Exception {
    Metacard metacard = mock(Metacard.class);
    BinaryContent content = previewMetacardTransformer.transform(metacard, null);

    assertThat(content, notNullValue());

    String preview = new String(content.getByteArray());
    assertThat(preview, is(equalTo(NO_PREVIEW)));
  }

  @Test
  public void testTransformation() throws Exception {
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
  public void testMetacardAlternativeTextPreviewSource() {}
}
