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
package org.codice.ddf.spatial.geocoder;

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.ADMINISTRATIVE_DIVISION;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.DIVISION_FIFTH_ORDER;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.DIVISION_FIRST_ORDER;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.DIVISION_FOURTH_ORDER;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.DIVISION_SECOND_ORDER;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.DIVISION_THIRD_ORDER;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.POLITICAL_ENTITY;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.POPULATED_PLACE;

import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public final class GeoResultCreator {
  private GeoResultCreator() {}

  public static GeoResult createGeoResult(
      final String name,
      final double latitude,
      final double longitude,
      final String featureCode,
      final double population) {
    double offset = 0.1;
    if (featureCode != null) {
      if (featureCode.startsWith(ADMINISTRATIVE_DIVISION)) {
        offset = getOffsetAdministrativeDivision(featureCode);
      } else if (featureCode.startsWith(POLITICAL_ENTITY)) {
        offset = getOffsetPoliticalEntity(population);
      } else if (featureCode.startsWith(POPULATED_PLACE)) {
        offset = getOffsetPopulatedPlace(population);
      }
    }

    final GeometryFactory geometryFactory = new GeometryFactory();

    final Coordinate northWest = new Coordinate(longitude - offset, latitude + offset);
    final Coordinate southEast = new Coordinate(longitude + offset, latitude - offset);
    final List<Point> bbox = new ArrayList<>();
    bbox.add(geometryFactory.createPoint(northWest));
    bbox.add(geometryFactory.createPoint(southEast));

    final Coordinate directPosition = new Coordinate(longitude, latitude);

    final GeoResult geoResult = new GeoResult();
    geoResult.setPoint(geometryFactory.createPoint(directPosition));
    geoResult.setBbox(bbox);
    geoResult.setFullName(name);
    return geoResult;
  }

  private static double getOffsetPopulatedPlace(double population) {
    double offset = 0.5;
    if (population > 10_000_000) {
      offset *= 1.5;
    } else if (population > 1_000_000) {
      offset *= 0.8;
    } else if (population > 100_000) {
      offset *= 0.5;
    } else if (population > 10_000) {
      offset *= 0.3;
    } else if (population > 0) {
      offset *= 0.2;
    }
    return offset;
  }

  private static double getOffsetPoliticalEntity(double population) {
    double offset = 6;
    if (population > 100_000_000) {
      offset *= 2;
    } else if (population > 10_000_000) {
      offset *= 1;
    } else if (population > 1_000_000) {
      offset *= 0.8;
    } else if (population > 0) {
      offset *= 0.5;
    }
    return offset;
  }

  private static double getOffsetAdministrativeDivision(String featureCode) {
    double offset = 0;
    if (featureCode.endsWith(DIVISION_FIRST_ORDER)) {
      offset = 5;
    } else if (featureCode.endsWith(DIVISION_SECOND_ORDER)) {
      offset = 4;
    } else if (featureCode.endsWith(DIVISION_THIRD_ORDER)) {
      offset = 3;
    } else if (featureCode.endsWith(DIVISION_FOURTH_ORDER)) {
      offset = 2;
    } else if (featureCode.endsWith(DIVISION_FIFTH_ORDER)) {
      offset = 1;
    }
    return offset;
  }

  public static GeoResult createGeoResult(GeoEntry geoEntry) {
    final String name = geoEntry.getName();
    final double latitude = geoEntry.getLatitude();
    final double longitude = geoEntry.getLongitude();
    final String featureCode = geoEntry.getFeatureCode();
    final long population = geoEntry.getPopulation();

    return createGeoResult(name, latitude, longitude, featureCode, population);
  }
}
