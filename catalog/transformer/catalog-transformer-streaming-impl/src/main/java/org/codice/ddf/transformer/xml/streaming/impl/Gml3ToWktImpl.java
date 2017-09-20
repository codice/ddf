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
package org.codice.ddf.transformer.xml.streaming.impl;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.bind.JAXBElement;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.jvnet.jaxb2_commons.locator.DefaultRootObjectLocator;
import org.jvnet.ogc.gml.v_3_1_1.jts.ConversionFailedException;
import org.jvnet.ogc.gml.v_3_1_1.jts.GML311ToJTSGeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gml3ToWktImpl implements Gml3ToWkt {
  private final Parser parser;

  private final ParserConfigurator configurator;

  private static final Logger LOGGER = LoggerFactory.getLogger(Gml3ToWkt.class);

  public Gml3ToWktImpl(Parser parser) {
    this.parser = parser;
    this.configurator =
        parser.configureParser(
            Collections.singletonList(AbstractGeometryType.class.getPackage().getName()),
            Gml3ToWktImpl.class.getClassLoader());
  }

  public String convert(String xml) throws ValidationExceptionImpl {
    try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      return convert(stream);
    } catch (IOException e) {
      LOGGER.debug("IO exception during conversion of {}", xml, e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("IO exception during conversion"), new ArrayList<String>());
    }
  }

  @SuppressWarnings("unchecked")
  public String convert(InputStream xml) throws ValidationExceptionImpl {
    AbstractGeometryType geometry = null;
    try {
      JAXBElement<AbstractGeometryType> jaxbElement =
          parser.unmarshal(configurator, JAXBElement.class, xml);
      geometry = jaxbElement.getValue();
      GML311ToJTSGeometryConverter geometryConverter = new GML311ToJTSGeometryConverter();
      Geometry jtsGeo =
          geometryConverter.createGeometry(new DefaultRootObjectLocator(jaxbElement), geometry);
      WKTWriter wktWriter = new WKTWriter();
      return wktWriter.write(jtsGeo);
    } catch (ParserException e) {
      LOGGER.debug("Cannot parse gml", e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("Cannot parse gml"), new ArrayList<String>());
    } catch (ConversionFailedException e) {
      LOGGER.debug("Cannot convert gml311 geo object {} to jts", geometry, e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("Cannot convert geo object"), new ArrayList<String>());
    }
  }
}
