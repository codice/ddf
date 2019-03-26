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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.xml.adapter.MetacardTypeAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlInputTransformer extends AbstractXmlTransformer implements InputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformer.class);

  private static final String FAILED_TRANSFORMATION =
      "Failed Transformation.  Could not create Metacard from XML.";

  private List<MetacardType> metacardTypes;

  public XmlInputTransformer(Parser parser) {
    super(parser);
  }

  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {
    return testXform(input, id);
  }

  private Metacard testXform(InputStream input, String id)
      throws IOException, CatalogTransformerException {
    ParserConfigurator parserConfigurator =
        getParserConfigurator()
            .setAdapter(new MetacardTypeAdapter(metacardTypes))
            .setHandler(new XmlValidationEventHandler());

    try {
      Metacard metacard = getParser().unmarshal(parserConfigurator, Metacard.class, input);

      if (metacard == null) {
        throw new CatalogTransformerException(FAILED_TRANSFORMATION);
      }

      if (!StringUtils.isEmpty(id)) {
        metacard.setAttribute(new AttributeImpl(Core.ID, id));
      }

      return metacard;
    } catch (ParserException e) {
      LOGGER.debug("Error parsing metacard", e);
      throw new CatalogTransformerException(FAILED_TRANSFORMATION, e);
    }
  }

  /** @param metacardTypes the metacardTypes to set */
  public void setMetacardTypes(List<MetacardType> metacardTypes) {
    this.metacardTypes = metacardTypes;
  }
}
