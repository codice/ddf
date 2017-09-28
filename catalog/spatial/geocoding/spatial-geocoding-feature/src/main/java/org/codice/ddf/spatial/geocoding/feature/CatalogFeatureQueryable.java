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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.spatial.geocoding.FeatureQueryable;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

public class CatalogFeatureQueryable implements FeatureQueryable {

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @Override
  public List<SimpleFeature> query(String queryString, int maxResults) {
    Filter filter = getFilterForQuery(queryString);

    QueryImpl query = new QueryImpl(filter);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    SourceResponse response;
    try {
      response = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException e) {
      return Collections.emptyList();
    } catch (SourceUnavailableException e) {
      return Collections.emptyList();
    } catch (FederationException e) {
      return Collections.emptyList();
    }

    List<SimpleFeature> results = new ArrayList<>();
    for (Result result : response.getResults()) {
      SimpleFeature feature = getFeatureForMetacard(result.getMetacard());
      if (feature != null) {
        results.add(feature);
      }
    }
    return results;
  }

  private Filter getFilterForQuery(String queryString) {
    Filter countryCodeFilter =
        filterBuilder.attribute(Metacard.TITLE).is().equalTo().text(queryString);
    Filter tagsFilter = filterBuilder.attribute(Metacard.TAGS).is().like().text("gazetteer");

    return filterBuilder.allOf(countryCodeFilter, tagsFilter);
  }

  private SimpleFeature getFeatureForMetacard(Metacard metacard) {
    String countryCode = (String) metacard.getAttribute(Metacard.TITLE).getValue();
    String geometryWkt = (String) metacard.getAttribute(Metacard.GEOGRAPHY).getValue();

    WKTReader wkt = new WKTReader();
    try {
      Geometry geometry = wkt.read(geometryWkt);
      SimpleFeatureBuilder builder = FeatureBuilder.forGeometry(geometry);
      return builder.buildFeature(countryCode);
    } catch (ParseException e) {
    }
    return null;
  }
}
