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
package org.codice.ddf.spatial.geocoding.feature;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang.Validate;
import org.codice.ddf.spatial.geocoding.FeatureQueryException;
import org.codice.ddf.spatial.geocoding.FeatureQueryable;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogFeatureQueryable implements FeatureQueryable {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFeatureQueryable.class);

  private static final ThreadLocal<WKTReader> WKT_READER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTReader::new);

  private CatalogFramework catalogFramework;

  private CatalogHelper catalogHelper;

  public CatalogFeatureQueryable(CatalogFramework catalogFramework, CatalogHelper catalogHelper) {
    this.catalogFramework = catalogFramework;
    this.catalogHelper = catalogHelper;
  }

  @Override
  public List<SimpleFeature> query(String queryString, String featureCode, int maxResults)
      throws FeatureQueryException {
    Validate.notNull(queryString, "queryString can't be null");

    if (maxResults < 0) {
      throw new IllegalArgumentException("maxResults can't be negative");
    }

    Query query = catalogHelper.getQueryForName(queryString);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    SourceResponse response;
    try {
      response = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new FeatureQueryException("Failed to query catalog", e);
    }

    return response
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .map(this::getFeatureForMetacard)
        .filter(Objects::nonNull)
        .limit(maxResults)
        .collect(Collectors.toList());
  }

  private SimpleFeature getFeatureForMetacard(Metacard metacard) {
    String countryCode = (String) metacard.getAttribute(Core.TITLE).getValue();
    String geometryWkt = (String) metacard.getAttribute(Core.LOCATION).getValue();

    try {
      Geometry geometry = WKT_READER_THREAD_LOCAL.get().read(geometryWkt);
      SimpleFeatureBuilder builder = FeatureBuilder.forGeometry(geometry);
      return builder.buildFeature(countryCode);
    } catch (ParseException e) {
      LOGGER.debug("Failed to parse feature", e);
    }
    return null;
  }
}
