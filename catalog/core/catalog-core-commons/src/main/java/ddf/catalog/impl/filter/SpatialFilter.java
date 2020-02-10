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
package ddf.catalog.impl.filter;

import java.text.ParseException;
import java.util.Locale;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PrimitiveFactoryImpl;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.io.WKTReader;
import org.opengis.geometry.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpatialFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpatialFilter.class);

  protected String geometryWkt;

  private WKTParser parser;

  private WKTReader reader;

  public SpatialFilter() {
    this(null);
  }

  public SpatialFilter(String geometryWkt) {
    this.geometryWkt = geometryWkt;

    GeometryBuilder builder = new GeometryBuilder(DefaultGeographicCRS.WGS84);
    this.parser = new WKTParser(builder);
    this.reader = new WKTReader();

    // This fixed the NPE in parser.parse() - seems GeoTools has bug with
    // keeping the CRS hint set ...
    parser.setFactory(new PrimitiveFactoryImpl(DefaultGeographicCRS.WGS84));
  }

  public String getGeometryWkt() {
    return geometryWkt;
  }

  public void setGeometryWkt(String geometryWkt) {
    this.geometryWkt = geometryWkt;
  }

  public Geometry getGeometry() {
    Geometry geometry = null;

    try {
      if (geometryWkt.toLowerCase(Locale.US).startsWith("multi")
          || geometryWkt.toLowerCase(Locale.US).trim().indexOf("geometrycollection") != -1) {
        // WKTParser does not currently support MultiPolygon,
        // MultiLineString, or MultiPoint
        org.locationtech.jts.geom.Geometry geo = reader.read(geometryWkt);

        geometry = new JTSGeometryWrapper(geo);
      } else {
        geometry = parser.parse(geometryWkt);
      }

    } catch (ParseException e) {
      LOGGER.debug("Unable to compute geometry for WKT = {}", this.geometryWkt, e);
    } catch (org.locationtech.jts.io.ParseException e) {
      LOGGER.debug("Unable to read multi geometry for WKT = {}", this.geometryWkt, e);
    }

    return geometry;
  }
}
