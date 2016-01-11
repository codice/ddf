/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.common.tika;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import ddf.catalog.transform.CatalogTransformerException;

public class TikaMetadataExtractorTest {
    private TikaMetadataExtractor tikaMetadataExtractor;

    private Parser mockParser;

    @Before
    public void setUp() {
        mockParser = mock(Parser.class);
        tikaMetadataExtractor = new TikaMetadataExtractor(mockParser, mock(ContentHandler.class));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testParserThrowsSaxException() throws Exception {
        doThrow(SAXException.class).when(mockParser)
                .parse(any(InputStream.class),
                        any(ContentHandler.class),
                        any(Metadata.class),
                        any(ParseContext.class));

        tikaMetadataExtractor.parseMetadata(mock(InputStream.class), mock(ParseContext.class));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testParserThrowsTikaException() throws Exception {
        doThrow(TikaException.class).when(mockParser)
                .parse(any(InputStream.class),
                        any(ContentHandler.class),
                        any(Metadata.class),
                        any(ParseContext.class));

        tikaMetadataExtractor.parseMetadata(mock(InputStream.class), mock(ParseContext.class));
    }
}
