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

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import ddf.catalog.transform.CatalogTransformerException;

public class TikaMetadataExtractor {
    private final Parser parser;

    private final ContentHandler handler;

    /**
     * Creates a new {@code TikaMetadataExtractor} with a {@link Parser} and {@link ContentHandler}.
     *
     * @param parser  the {@code Parser} to use
     * @param handler the {@code ContentHandler} to use with {@code parser}
     */
    public TikaMetadataExtractor(final Parser parser, final ContentHandler handler) {
        this.parser = parser;
        this.handler = handler;
    }

    /**
     * Parses metadata from {@code inputStream} using the supplied {@link Parser},
     * {@link ContentHandler}, and {@link ParseContext}.
     *
     * @param inputStream  the data to parse
     * @param parseContext context information to pass to the {@code Parser}, may be null
     * @return a {@link Metadata} object containing the metadata that the {@code Parser} was able to
     * extract from {@code inputStream}
     * @throws CatalogTransformerException
     * @throws IOException
     */
    public Metadata parseMetadata(final InputStream inputStream, final ParseContext parseContext)
            throws CatalogTransformerException, IOException {
        final Metadata metadata = new Metadata();

        try {
            parser.parse(inputStream, handler, metadata, parseContext);
        } catch (SAXException | TikaException e) {
            throw new CatalogTransformerException(e);
        }

        return metadata;
    }
}
