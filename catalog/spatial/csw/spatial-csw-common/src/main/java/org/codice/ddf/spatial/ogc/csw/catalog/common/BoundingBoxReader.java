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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.libs.geo.GeoFormatException;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses OWS Bounding Box geometry XML and converts it to WKT.
 *
 * <p>Bounding Box XML is of the form:
 *
 * <p>
 *
 * <pre>{@code
 * <ows:BoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
 *    <ows:LowerCorner>60.042 13.754</ows:LowerCorner>
 *    <ows:UpperCorner>68.410 17.920</ows:UpperCorner>
 * </ows:BoundingBox>
 * }</pre>
 *
 * @author rodgersh
 */
public class BoundingBoxReader {
  private static final transient Logger LOGGER = LoggerFactory.getLogger(BoundingBoxReader.class);

  private static final String SPACE = " ";

  private static final String DEFAULT_CRS = "urn:ogc:def:crs:EPSG:4326";

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      new ThreadLocal<WKTWriter>() {
        @Override
        protected WKTWriter initialValue() {
          return new WKTWriter();
        }
      };

  private HierarchicalStreamReader reader;

  private CswAxisOrder cswAxisOrder;

  private WKTReader wktReader;

  public BoundingBoxReader(HierarchicalStreamReader reader, CswAxisOrder cswAxisOrder) {
    this.reader = reader;
    this.cswAxisOrder = cswAxisOrder;
    wktReader = new WKTReader(GEOMETRY_FACTORY);
  }

  public String getWkt() throws CswException {
    String wkt = null;

    // reader should initially be positioned at <ows:BoundingBox> element
    LOGGER.debug("Initial node name = {}", reader.getNodeName());
    if (!reader.getNodeName().contains("BoundingBox")) {
      throw new CswException(
          "BoundingBoxReader.getWkt(): supplied reader does not contain a BoundingBox.");
    }

    String crs = reader.getAttribute("crs");
    // For now, default an empty CRS to EPSG:4326
    crs = StringUtils.isEmpty(crs) ? DEFAULT_CRS : crs;

    LOGGER.debug("crs = {}, coordinate order = {}", crs, this.cswAxisOrder);

    // Move down to the first child node of <BoundingBox>, which should
    // be the <LowerCorner> tag
    reader.moveDown();

    // Parse LowerCorner position from bounding box
    String[] lowerCornerPosition = null;
    if (reader.getNodeName().contains("LowerCorner")) {
      String value = reader.getValue();
      lowerCornerPosition = getCoordinates(value);
    }

    // Move back up to the <BoundingBox> parent tag
    reader.moveUp();

    // Move down to the next child node of the <BoundingBox> tag, which
    // should be the <UpperCorner> tag
    reader.moveDown();

    // Parse UpperCorner position from bounding box
    String[] upperCornerPosition = null;
    if (reader.getNodeName().contains("UpperCorner")) {
      String value = reader.getValue();
      upperCornerPosition = getCoordinates(value);
    }

    // If both corner positions parsed, then compute other 2 corner
    // positions of the bounding box and create the WKT for the bounding box.
    if (lowerCornerPosition != null
        && lowerCornerPosition.length == 2
        && upperCornerPosition != null
        && upperCornerPosition.length == 2) {

      /**
       * The WKT created here need to be in LON/LAT. This is the order that we store WKT in the
       * metacard location field.
       */
      wkt = createWkt(lowerCornerPosition, upperCornerPosition);
    } else {
      throw new CswException(
          "BoundingBoxReader.getWkt(): could not find either LowerCorner or UpperCorner tags.");
    }

    // Move position back up to the parent <BoundingBox> tag, where we started
    reader.moveUp();

    if (!isEPSG4326(crs) || cswAxisOrder.equals(CswAxisOrder.LAT_LON)) {
      wkt = convertToEPSG4326(wkt, crs);
    }
    LOGGER.debug("Returning WKT {}.", wkt);
    return wkt;
  }

  /**
   * We want to create WKT in LON/LAT order.
   *
   * <p>Points for bounding box will be computed starting with the lower corner, and proceeding in a
   * clockwise rotation. So lower corner point would be point 1, upper corner point would be point
   * 3, and points 2 and 4 would be computed.
   */
  private String createWkt(String[] lowerCornerPosition, String[] upperCornerPosition) {
    LOGGER.debug("Creating WKT in LON/LAT coordinate order.");
    if (upperCornerPosition[0].equals(lowerCornerPosition[0])
        && upperCornerPosition[1].equals(lowerCornerPosition[1])) {
      return "POINT (" + lowerCornerPosition[0] + SPACE + lowerCornerPosition[1] + ")";
    }
    return "POLYGON (("
        + lowerCornerPosition[0]
        + SPACE
        + lowerCornerPosition[1]
        + ", "
        + upperCornerPosition[0]
        + SPACE
        + lowerCornerPosition[1]
        + ", "
        + upperCornerPosition[0]
        + SPACE
        + upperCornerPosition[1]
        + ", "
        + lowerCornerPosition[0]
        + SPACE
        + upperCornerPosition[1]
        + ", "
        + lowerCornerPosition[0]
        + SPACE
        + lowerCornerPosition[1]
        + "))";
  }

  /**
   * @param coords The latitude and longitude coordinates (in no particular order).
   * @return The coordinates in an array
   */
  private String[] getCoordinates(String coords) {
    return coords.split(SPACE);
  }

  private String convertToEPSG4326(String wkt, String crs) throws CswException {
    try {
      Geometry geometry = wktReader.read(wkt);
      Geometry convertedGeometry = GeospatialUtil.transformToEPSG4326LonLatFormat(geometry, crs);

      if (convertedGeometry != null && withinEPSG4326Bounds(convertedGeometry)) {
        return WKT_WRITER_THREAD_LOCAL.get().write(convertedGeometry);
      } else {
        throw new GeoFormatException(
            "Converted Geometry is not set, or not within EPSG4326 bounds");
      }
    } catch (ParseException | GeoFormatException e) {
      throw new CswException(String.format("Unable to convert %s from %s to EPSG:4326", wkt, crs));
    }
  }

  private boolean isEPSG4326(String crs) {
    return crs.contains("4326") || crs.contains("CRS84");
  }

  private boolean withinEPSG4326Bounds(Geometry geometry) {
    Coordinate[] coordinates = geometry.getCoordinates();
    for (Coordinate coordinate : coordinates) {
      // Longitude
      if (coordinate.x < -180 || coordinate.x > 180) {
        return false;
      }
      // Latitude
      if (coordinate.y < -90 || coordinate.y > 90) {
        return false;
      }
    }
    return true;
  }
}
