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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
  public List<String> getSuggestedFeatureNames(String query, int maxResults) {
    try {
      return geoEntryQueryable.getSuggestedNames(query, maxResults);
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Suggestion query failed", e);
    }
    return Collections.emptyList();
  }

  @Override
  public SimpleFeature getFeatureByName(String name) {
    List<GeoEntry> entries;
    try {
      entries = this.geoEntryQueryable.query(name, 1);
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error while making feature service request.", e);
      return null;
    }
    if (entries.isEmpty()) {
      return null;
    }
    GeoEntry entry = entries.get(0);

    SimpleFeature feature = findDetailedFeatureForGeoEntry(entry);
    if (feature != null) {
      return feature;
    }

    GeoResult geoResult = GeoResultCreator.createGeoResult(entry);
    return getFeatureFromGeoResult(geoResult);
  }

  private SimpleFeature findDetailedFeatureForGeoEntry(GeoEntry entry) {
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

  public static SimpleFeature getFeatureFromGeoResult(GeoResult geoResult) {
    Polygon polygon = getPolygonFromBBox(geoResult.getBbox());
    SimpleFeatureBuilder builder = getSimpleFeatureBuilder(polygon);
    return builder.buildFeature(geoResult.getFullName());
  }

  public static SimpleFeatureBuilder getSimpleFeatureBuilder(Geometry geometry) {
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("featureType");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.add("coordinates", geometry.getClass());
    SimpleFeatureType featureType = typeBuilder.buildFeatureType();
    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
    builder.add(geometry);
    return builder;
  }

  public static Polygon getPolygonFromBBox(List<Point> bbox) {
    double[] p0 = bbox.get(0).getDirectPosition().getCoordinate();
    double[] p1 = bbox.get(1).getDirectPosition().getCoordinate();
    Envelope envelope = new Envelope();
    envelope.expandToInclude(p0[0], p0[1]);
    envelope.expandToInclude(p1[0], p1[1]);
    return JTS.toGeometry(envelope);
  }
}
