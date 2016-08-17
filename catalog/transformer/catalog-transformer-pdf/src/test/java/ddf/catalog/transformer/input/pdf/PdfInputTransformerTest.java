/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.input.pdf;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;

public class PdfInputTransformerTest {

    private PdfInputTransformer pdfInputTransformer;

    private PDDocument pdDocument;

    private PDDocumentInformation documentInformation;

    @Before
    public void setup() {
        pdfInputTransformer = new PdfInputTransformer(mock(MetacardTypeImpl.class),
                false,
                inputStream -> pdDocument,
                pdDocument1 -> null,
                pdDocument1 -> new byte[0]);

        pdDocument = mock(PDDocument.class);
        documentInformation = mock(PDDocumentInformation.class);

        when(pdDocument.getDocumentInformation()).thenReturn(documentInformation);

    }

    @Test
    public void testUsePdfTitleAsTitleFalse() throws IOException, CatalogTransformerException {
        pdfInputTransformer.setUsePdfTitleAsTitle(false);
        when(documentInformation.getTitle()).thenReturn("TheTitle");

        Metacard metacard = pdfInputTransformer.transform(null);

        assertThat(metacard.getTitle(), is(nullValue()));
    }

    @Test
    public void testUsePdfTitleAsTitleTrue() throws IOException, CatalogTransformerException {
        String title = "TheTitle";
        pdfInputTransformer.setUsePdfTitleAsTitle(true);
        when(documentInformation.getTitle()).thenReturn(title);

        Metacard metacard = pdfInputTransformer.transform(null);

        assertThat(metacard.getTitle(), is(title));
    }

    @Test
    public void testAuthor() throws IOException, CatalogTransformerException {
        String author = "TheAuthor";

        when(documentInformation.getAuthor()).thenReturn(author);

        Metacard metacard = pdfInputTransformer.transform(mock(InputStream.class));

        assertThat(metacard.getAttribute(Contact.CREATOR_NAME)
                .getValue(), is(author));

    }

    @Test
    public void testKeywords() throws IOException, CatalogTransformerException {
        String keywords = "TheKeywords";

        when(documentInformation.getKeywords()).thenReturn(keywords);

        Metacard metacard = pdfInputTransformer.transform(mock(InputStream.class));

        assertThat(metacard.getAttribute(Topic.KEYWORD)
                .getValue(), is(keywords));

    }

}
