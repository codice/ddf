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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.adapter.GeometryAdapter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;

/** Transforms Geometry object to BinaryContent */
class GeometryTransformer extends AbstractXmlTransformer {
  private static final int BUFFER_SIZE = 512;

  public GeometryTransformer(Parser parser) {
    super(parser);
  }

  public BinaryContent transform(Attribute attribute) throws CatalogTransformerException {
    ParserConfigurator parserConfigurator =
        getParserConfigurator().setHandler(new XmlValidationEventHandler());

    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);
      getParser().marshal(parserConfigurator, GeometryAdapter.marshalFrom(attribute), os);
      ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());
      return new BinaryContentImpl(bais, MIME_TYPE);
    } catch (ParserException e) {
      throw new CatalogTransformerException("Failed to marshall geometry data", e);
    }
  }
}
