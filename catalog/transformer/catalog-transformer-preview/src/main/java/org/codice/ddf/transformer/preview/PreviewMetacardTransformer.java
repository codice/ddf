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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class PreviewMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER =
          LoggerFactory.getLogger(PreviewMetacardTransformer.class.getName());

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
          throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Cannot transform null metacard.");
    }

    String preview = "No preview text available.";
    if (metacard.getAttribute(Extracted.EXTRACTED_TEXT) != null
            && metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue() != null) {
      preview =
              StringEscapeUtils.escapeHtml4(
                      metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString())
                      .replaceAll("[\n|\r]", "<br>");
      preview = String.format("<head><meta charset=\"utf-8\"/>%s</head>", preview);
    } else if (StringUtils.isNotEmpty(metacard.getMetadata())) {
      enrichMetacard(metacard);
    }

    return new BinaryContentImpl(IOUtils.toInputStream(preview));
  }

  public Metacard enrichMetacard(Metacard metacard) {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    try {
      XMLEventReader xmlEventReader =
              xmlInputFactory.createXMLEventReader(new StringReader(metacard.getMetadata()));

      while (xmlEventReader.hasNext()) {
        XMLEvent xmlEvent = xmlEventReader.nextEvent();
      }


      metacard.setAttribute(new AttributeImpl(Extracted.EXTRACTED_TEXT, ""));
    } catch (XMLStreamException e) {
      LOGGER.error("Error parsing metacard metadata", e);
    }

    return metacard;
  }
}
