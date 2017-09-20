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
package ddf.catalog.transformer.xml;

import ddf.catalog.transformer.xml.adapter.AdaptedMetacard;
import ddf.catalog.transformer.xml.binding.MetacardElement;
import java.util.ArrayList;
import java.util.List;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXmlTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmlTransformer.class);

  private static final List<String> CONTEXT_PATH;

  public static final MimeType MIME_TYPE;

  static {
    try {
      CONTEXT_PATH = new ArrayList<>();
      CONTEXT_PATH.add(MetacardElement.class.getPackage().getName());
      CONTEXT_PATH.add(AdaptedMetacard.class.getPackage().getName());
      CONTEXT_PATH.add(AbstractGeometryType.class.getPackage().getName());

      MIME_TYPE = new MimeType("text", "xml");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  private final Parser parser;

  protected AbstractXmlTransformer(Parser parser) {
    super();
    this.parser = parser;
  }

  protected final ParserConfigurator getParserConfigurator() {
    return parser.configureParser(CONTEXT_PATH, AbstractXmlTransformer.class.getClassLoader());
  }

  protected final Parser getParser() {
    return parser;
  }
}
