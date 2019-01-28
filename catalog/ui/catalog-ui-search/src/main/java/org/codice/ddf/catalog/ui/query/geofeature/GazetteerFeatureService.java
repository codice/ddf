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
package org.codice.ddf.catalog.ui.query.geofeature;

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.COUNTRY_FEATURE_CODES;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.FeatureQueryException;
import org.codice.ddf.spatial.geocoding.FeatureQueryable;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.primitive.Point;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a FeatureService using gazetteer(s). Currently the only gazetteer is provided by the
 * GeoEntryQueryable interface, which queries geonames.org data. It is intended that more will be
 * added and the results federated.
 */
public class GazetteerFeatureService implements FeatureService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerFeatureService.class);

  private GeoEntryQueryable geoEntryQueryable;

  private FeatureQueryable featureQueryable;

  public void setGeoEntryQueryable(GeoEntryQueryable geoEntryQueryable) {
    this.geoEntryQueryable = geoEntryQueryable;
  }

  public void setFeatureQueryable(FeatureQueryable featureQueryable) {
    this.featureQueryable = featureQueryable;
  }

  @Override
  public List<Suggestion> getSuggestedFeatureNames(String query, int maxResults) {
    try {
      return geoEntryQueryable.getSuggestedNames(query, maxResults);
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Suggestion query failed", e);
    }
    return Collections.emptyList();
  }

  @Override
  public SimpleFeature getFeatureById(String id) {
    GeoEntry entry;
    try {
      entry = this.geoEntryQueryable.queryById(id);
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error while making feature service request.", e);
      return null;
    }
    if (entry == null) {
      return null;
    }

    if (COUNTRY_FEATURE_CODES.contains(entry.getFeatureCode())) {
      SimpleFeature feature = findCountryShape(entry);
      if (feature != null) {
        return feature;
      }
    }

    return getFeatureFromGeoResult(GeoResultCreator.createGeoResult(entry));
  }

  private SimpleFeature findCountryShape(GeoEntry entry) {
    String countryCode = entry.getCountryCode();

    if (countryCode == null) {
      return null;
    }

    try {
      List<SimpleFeature> countries = this.featureQueryable.query(countryCode, null, 1);
      if (CollectionUtils.isNotEmpty(countries)) {
        return countries.get(0);
      }
    } catch (FeatureQueryException e) {
      LOGGER.debug("Error while querying for feature.", e);
    } catch (ServiceUnavailableException e) {
      LOGGER.debug("Feature index unavailable", e);
    }
    return null;
  }

  private static SimpleFeature getFeatureFromGeoResult(GeoResult geoResult) {
    Polygon polygon = getPolygonFromBBox(geoResult.getBbox());
    SimpleFeatureBuilder builder = getSimpleFeatureBuilder(polygon);
    return builder.buildFeature(geoResult.getFullName());
  }

  @VisibleForTesting
  static SimpleFeatureBuilder getSimpleFeatureBuilder(Geometry geometry) {
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("featureType");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.add("coordinates", geometry.getClass());
    SimpleFeatureType featureType = typeBuilder.buildFeatureType();
    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
    builder.add(geometry);
    return builder;
  }

  private static Polygon getPolygonFromBBox(List<Point> bbox) {
    double[] p0 = bbox.get(0).getDirectPosition().getCoordinate();
    double[] p1 = bbox.get(1).getDirectPosition().getCoordinate();
    Envelope envelope = new Envelope();
    envelope.expandToInclude(p0[0], p0[1]);
    envelope.expandToInclude(p1[0], p1[1]);
    return JTS.toGeometry(envelope);
  }
}
