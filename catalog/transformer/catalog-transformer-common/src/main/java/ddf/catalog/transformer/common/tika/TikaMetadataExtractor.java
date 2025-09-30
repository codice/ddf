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
package ddf.catalog.transformer.common.tika;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.transformer.common.tika.handler.BodyAndMetadataContentHandler;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class TikaMetadataExtractor {

  public static final Logger LOGGER = LoggerFactory.getLogger(TikaMetadataExtractor.class);

  public static final String METADATA_LIMIT_REACHED_MSG =
      "Document metadata limit reached. To prevent this, increase the limit.";

  private final BodyAndMetadataContentHandler bodyAndMetadataContentHandler;

  private Metadata metadata;

  /**
   * Constructs a new tika extractor which parses the provided input stream into a tika Metadata
   * object, the body text, and the metadata XML
   *
   * @param inputStream - the input stream to be parsed
   * @throws TikaException - if parsing fails
   */
  public TikaMetadataExtractor(InputStream inputStream) throws TikaException {
    this(inputStream, -1, -1);
  }

  /**
   * Constructs a new tika extractor which parses the provided input stream into a tika Metadata
   * object, the body text, and the metadata XML The body text is truncated after maxLength
   *
   * @param inputStream - the input stream to be parsed
   * @param maxBodyLength - the max length of the parsed body text
   * @param maxMetadataLength - the max length of the parsed metadata.
   * @throws TikaException - if parsing fails
   */
  public TikaMetadataExtractor(InputStream inputStream, int maxBodyLength, int maxMetadataLength)
      throws TikaException {
    notNull(inputStream);
    this.metadata = new Metadata();
    this.bodyAndMetadataContentHandler =
        new BodyAndMetadataContentHandler(maxBodyLength, maxMetadataLength);
    parseMetadata(inputStream);
  }

  private void parseMetadata(InputStream inputStream) throws TikaException {

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Object>)
              () -> {
                Parser parser = new AutoDetectParser();
                parser.parse(
                    inputStream, this.bodyAndMetadataContentHandler, metadata, new ParseContext());
                return null;
              });
    } catch (PrivilegedActionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw new TikaException("Unexpected IOException. Stream may already be closed", cause);
      } else if (cause instanceof SAXException) {
        LOGGER.debug("Unexpected tika parsing failure", cause);
      } else {
        LOGGER.warn("tika failed", e);
      }
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public String getBodyText() {
    return bodyAndMetadataContentHandler.getBodyText();
  }

  public String getMetadataXml() {
    return bodyAndMetadataContentHandler.getMetadataText();
  }

  public Metadata getMetadata() {
    return metadata;
  }
}
