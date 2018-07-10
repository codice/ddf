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
package ddf.catalog.transformer.input.pdf;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Topic;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.Before;
import org.junit.Test;

public class PdfInputTransformerTest {

  private PdfInputTransformer pdfInputTransformer;

  private PDDocument pdDocument;

  private PDDocumentInformation documentInformation;

  @Before
  public void setup() {
    pdfInputTransformer =
        new PdfInputTransformer(
            mock(MetacardTypeImpl.class),
            false,
            inputStream -> pdDocument,
            pdDocument1 -> null,
            pdDocument1 -> Optional.empty());

    pdDocument = mock(PDDocument.class);
    documentInformation = mock(PDDocumentInformation.class);

    when(pdDocument.getDocumentInformation()).thenReturn(documentInformation);
  }

  @Test
  public void testUsePdfTitleAsTitleFalse() throws IOException, CatalogTransformerException {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    pdfInputTransformer.setUsePdfTitleAsTitle(false);
    when(documentInformation.getTitle()).thenReturn("TheTitle");

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard.getTitle(), is(nullValue()));
  }

  @Test
  public void testUsePdfTitleAsTitleTrue() throws IOException, CatalogTransformerException {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    String title = "TheTitle";
    pdfInputTransformer.setUsePdfTitleAsTitle(true);
    when(documentInformation.getTitle()).thenReturn(title);

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard.getTitle(), is(title));
  }

  @Test
  public void testAuthor() throws IOException, CatalogTransformerException {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    String author = "TheAuthor";

    when(documentInformation.getAuthor()).thenReturn(author);

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard.getAttribute(Contact.CREATOR_NAME).getValue(), is(author));
  }

  @Test
  public void testKeywords() throws IOException, CatalogTransformerException {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    String keywords = "TheKeywords";

    when(documentInformation.getKeywords()).thenReturn(keywords);

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard.getAttribute(Topic.KEYWORD).getValue(), is(keywords));
  }

  @Test
  public void testPdfMetacardValues() throws IOException, CatalogTransformerException {

    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    pdfInputTransformer.setPreviewMaxLength(-1);

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard, notNullValue());

    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), equalTo("Text"));
    assertThat(
        (String) (metacard.getAttribute(Extracted.EXTRACTED_TEXT)).getValue(),
        containsString("TEST"));
    assertThat(metacard.getMetadata(), containsString("2016-02-22T14:09:16Z\""));
  }

  @Test
  public void testPdfBodyTruncation() throws IOException, CatalogTransformerException {

    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.pdf");
    pdfInputTransformer.setPreviewMaxLength(10);

    Metacard metacard = pdfInputTransformer.transform(stream);

    assertThat(metacard, notNullValue());

    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), equalTo("Text"));
    assertThat(
        (String) (metacard.getAttribute(Extracted.EXTRACTED_TEXT)).getValue(),
        not(containsString("TEST")));
    assertThat(metacard.getMetadata(), containsString("2016-02-22T14:09:16Z\""));
  }
}
