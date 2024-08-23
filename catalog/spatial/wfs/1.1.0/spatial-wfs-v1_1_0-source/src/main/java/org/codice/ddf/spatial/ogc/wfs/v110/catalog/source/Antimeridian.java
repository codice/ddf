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
 * ******************************************************************************
 *
 * <p>The shift, cut, and unwrap methods in this class are either taken directly or adapted from
 * org.locationtech.spatial4j.shape.jts.JtsGeometry.
 *
 * <p>Copyright (c) 2015 Voyager Search and MITRE All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * <http://www.apache.org/licenses/LICENSE-2.0.txt
 * ****************************************************************************
 */
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.GeometryFilter;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Antimeridian {

  private static final Logger LOGGER = LoggerFactory.getLogger(Antimeridian.class);
  private static final WKTReader WKT_READER = new WKTReader();
  private static final WKTWriter WKT_WRITER = new WKTWriter();

  private Antimeridian() {}

  public static String normalizeWkt(String wkt) {
    String normalizedWkt = wkt;
    try {
      Geometry geo = WKT_READER.read(wkt);
      Coordinate[] coordinates = geo.getCoordinates();
      // keep coordinates within [-180,180]
      for (Coordinate coord : coordinates) {
        if (coord.x > 180) {
          coord.x -= 360;
        } else if (coord.x < -180) {
          coord.x += 360;
        }
      }
      Geometry normalizedGeo = new GeometryFactory().createPolygon(coordinates);
      normalizedWkt = WKT_WRITER.write(normalizedGeo);
    } catch (Exception e) {
      LOGGER.debug("Unable to adjust WKT. Continuing with original WKT.");
      return wkt;
    }
    return normalizedWkt;
  }

  public static String unwrapAndSplitWkt(String wkt) {
    String normalizedWkt = normalizeWkt(wkt);
    try {
      Geometry geo = WKT_READER.read(normalizedWkt);
      Geometry unwrappedGeo = unwrapAntimeridian(geo);
      Geometry splitGeo = cutUnwrappedGeomInto360(unwrappedGeo);
      normalizedWkt = WKT_WRITER.write(splitGeo);
    } catch (Exception e) {
      LOGGER.debug("Unable to adjust WKT. Continuing with original WKT.");
      return wkt;
    }
    return normalizedWkt;
  }

  public static Geometry unwrapAntimeridian(Geometry geo) {
    if (geo.getEnvelopeInternal().getWidth() < 180.0 || geo instanceof MultiPoint) {
      return geo;
    }

    if (geo instanceof GeometryCollection) {
      return unwrapGeoCollection(geo);
    }

    return unwrapGeo(geo);
  }

  public static Geometry unwrapGeoCollection(Geometry geo) {
    GeometryCollection geoCollection = (GeometryCollection) geo;
    List<Geometry> list = new ArrayList<>(geoCollection.getNumGeometries());
    boolean didUnwrap = false;

    for (int n = 0; n < geoCollection.getNumGeometries(); ++n) {
      Geometry geometryN = geoCollection.getGeometryN(n);
      Geometry geometryUnwrapped = unwrapAntimeridian(geometryN);
      list.add(geometryUnwrapped);
      didUnwrap |= geometryUnwrapped != geometryN;
    }

    return !didUnwrap ? geo : geo.getFactory().buildGeometry(list);
  }

  public static Geometry unwrapGeo(Geometry geo) {
    Geometry newGeom = geo.copy();
    final int[] crossings = new int[] {0};
    newGeom.apply(
        (GeometryFilter)
            geometry -> {
              if (geometry.getEnvelopeInternal().getWidth() < 180.0) {
                return;
              }
              int cross;
              if (geometry instanceof LineString) {
                cross = unwrapLineString((LineString) geometry);
              } else {
                if (!(geometry instanceof Polygon)) {
                  return;
                }
                cross = unwrapPolygon((Polygon) geometry);
              }
              crossings[0] = Math.max(crossings[0], cross);
            });
    if (crossings[0] > 0) {
      newGeom.geometryChanged();
      return newGeom;
    }
    return geo;
  }

  public static int unwrapPolygon(Polygon poly) {
    LineString exteriorRing = poly.getExteriorRing();
    int cross = unwrapLineString(exteriorRing);
    if (cross > 0) {
      for (int i = 0; i < poly.getNumInteriorRing(); ++i) {
        LineString innerLineString = poly.getInteriorRingN(i);
        unwrapLineString(innerLineString);

        for (int shiftCount = 0; !exteriorRing.contains(innerLineString); ++shiftCount) {
          if (shiftCount > cross) {
            throw new IllegalArgumentException(
                "The inner ring doesn't appear to be within the exterior: "
                    + exteriorRing
                    + " inner: "
                    + innerLineString);
          }

          shiftGeomByX(innerLineString, 360);
        }
      }
    }

    return cross;
  }

  public static int unwrapLineString(LineString lineString) {
    CoordinateSequence cseq = lineString.getCoordinateSequence();
    int size = cseq.size();
    if (size <= 1) {
      return 0;
    } else {
      int shiftX = 0;
      int shiftXPage = 0;
      int shiftXPageMin = 0;
      int shiftXPageMax = 0;
      double prevX = cseq.getX(0);

      int i;
      for (i = 1; i < size; ++i) {
        double thisXorig = cseq.getX(i);

        assert thisXorig >= -180.0 && thisXorig <= 180.0 : "X not in geo bounds";

        double thisX = thisXorig + shiftX;
        if (prevX - thisX > 180.0) {
          thisX += 360.0;
          shiftX += 360;
          ++shiftXPage;
          shiftXPageMax = Math.max(shiftXPageMax, shiftXPage);
        } else if (thisX - prevX > 180.0) {
          thisX -= 360.0;
          shiftX -= 360;
          --shiftXPage;
          shiftXPageMin = Math.min(shiftXPageMin, shiftXPage);
        }

        if (shiftXPage != 0) {
          cseq.setOrdinate(i, 0, thisX);
        }

        prevX = thisX;
      }

      if (lineString instanceof LinearRing) {
        assert cseq.getCoordinate(0).equals(cseq.getCoordinate(size - 1));

        assert shiftXPage == 0;
      }

      shiftGeomByX(lineString, shiftXPageMin * -360);
      i = shiftXPageMax - shiftXPageMin;
      return i;
    }
  }

  public static Geometry cutUnwrappedGeomInto360(Geometry geom) {
    if (!geom.isValid()) {
      throw new IllegalArgumentException("Invalid geometry");
    }

    Envelope geomEnv = geom.getEnvelopeInternal();
    if (geomEnv.getMinX() >= -180.0 && geomEnv.getMaxX() <= 180.0) {
      return geom;
    }

    List<Geometry> geomList = new ArrayList<>();
    int page = 0;

    while (true) {
      double minX = (-180 + page * 360);
      if (geomEnv.getMaxX() <= minX) {
        return UnaryUnionOp.union(geomList);
      }

      Geometry rect = geom.getFactory().toGeometry(new Envelope(minX, minX + 360.0, -90.0, 90.0));

      assert rect.isValid() : "rect";

      Geometry pageGeom = rect.intersection(geom);

      assert pageGeom.isValid() : "pageGeom";

      shiftGeomByX(pageGeom, page * -360);
      geomList.add(pageGeom);
      ++page;
    }
  }

  public static void shiftGeomByX(Geometry geom, final int xShift) {
    if (xShift != 0) {
      geom.apply(
          new CoordinateSequenceFilter() {
            public void filter(CoordinateSequence seq, int i) {
              seq.setOrdinate(i, 0, seq.getX(i) + xShift);
            }

            public boolean isDone() {
              return false;
            }

            public boolean isGeometryChanged() {
              return true;
            }
          });
    }
  }
}
