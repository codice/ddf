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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xsd.Parser;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/** CswCqlTextFilter converts a {@link FilterType} to the equivalent CQL Text. */
public final class CswCqlTextFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswCqlTextFilter.class);

  private final ObjectFactory objectFactory = new ObjectFactory();

  private final CswXmlParser parser = new CswXmlParser();

  private static CswCqlTextFilter instance;

  private CswCqlTextFilter() {}

  public static CswCqlTextFilter getInstance() {
    if (instance == null) {
      instance = new CswCqlTextFilter();
    }
    return instance;
  }

  public String getCqlText(FilterType filterType) throws UnsupportedQueryException {
    Parser parser = new Parser(new OGCConfiguration());
    try {
      StringReader reader = new StringReader(marshalFilterType(filterType));
      Object parsedFilter = parser.parse(reader);
      if (parsedFilter instanceof Filter) {
        String cql = ECQL.toCQL((Filter) parsedFilter);
        LOGGER.debug("Generated CQL from Filter => {}", cql);
        return cql;
      } else {
        throw new UnsupportedQueryException("Query did not produce a valid filter.");
      }
    } catch (IOException | SAXException | ParserConfigurationException | ParserException e) {
      throw new UnsupportedQueryException("Unable to create CQL Filter.", e);
    }
  }

  private String marshalFilterType(FilterType filterType) throws ParserException {
    String result = parser.marshal(objectFactory.createFilter(filterType));
    LOGGER.debug("Filter as XML => {}", result);
    return result;
  }
}
