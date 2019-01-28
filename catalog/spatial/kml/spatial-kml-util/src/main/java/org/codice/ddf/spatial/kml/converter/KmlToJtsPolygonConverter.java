/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.converter;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

public class KmlToJtsPolygonConverter {
  private KmlToJtsPolygonConverter() {}

  public static org.locationtech.jts.geom.Polygon from(Polygon kmlPolygon) {
    if (!isValidPolygon(kmlPolygon)) {
      return null;
    }

    LinearRing jtsShell = getJtsShell(kmlPolygon.getOuterBoundaryIs());

    List<LinearRing> jtsHoles = getJtsHoles(kmlPolygon.getInnerBoundaryIs());

    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    return geometryFactory.createPolygon(jtsShell, jtsHoles.toArray(new LinearRing[0]));
  }

  private static LinearRing getJtsShell(Boundary kmlOuterBoundary) {
    if (kmlOuterBoundary == null) {
      return null;
    }

    return KmlToJtsLinearRingConverter.from(kmlOuterBoundary.getLinearRing());
  }

  private static List<LinearRing> getJtsHoles(List<Boundary> kmlInnerBoundaries) {
    if (CollectionUtils.isEmpty(kmlInnerBoundaries)) {
      return new ArrayList<>();
    }

    return kmlInnerBoundaries
        .stream()
        .map(Boundary::getLinearRing)
        .filter(Objects::nonNull)
        .map(KmlToJtsLinearRingConverter::from)
        .collect(Collectors.toList());
  }

  public static boolean isValidPolygon(Polygon kmlPolygon) {
    return kmlPolygon != null && isValidKmlBoundary(kmlPolygon.getOuterBoundaryIs());
  }

  private static boolean isValidKmlBoundary(Boundary kmlBoundary) {
    if (kmlBoundary == null) {
      return false;
    }

    return KmlToJtsLinearRingConverter.isValidKmlLinearRing(kmlBoundary.getLinearRing());
  }
}
