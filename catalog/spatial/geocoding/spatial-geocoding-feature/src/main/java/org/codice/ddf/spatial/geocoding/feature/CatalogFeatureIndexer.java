/**
 * Copyright (c) Codice Foundation
 *
 * <p>
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.geocoding.feature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.service.SecurityServiceException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

public class CatalogFeatureIndexer implements FeatureIndexer {

  private Security security = Security.getInstance();

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  public void setSecurity(Security security) {
    this.security = security;
  }

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @Override
  public void updateIndex(
      String resource, FeatureExtractor featureExtractor, boolean create, IndexCallback callback)
      throws FeatureExtractionException, FeatureIndexingException {

    if (create) {
      removeExistingMetacards();
    }

    AtomicInteger count = new AtomicInteger(0);

    final FeatureExtractor.ExtractionCallback extractionCallback =
        (SimpleFeature feature) -> {
          createOrUpdateMetacardForFeature(feature, create);
          callback.indexed(count.incrementAndGet());
        };

    featureExtractor.pushFeaturesToExtractionCallback(resource, extractionCallback);
  }

  private void createOrUpdateMetacardForFeature(SimpleFeature feature, boolean create)
      throws FeatureIndexingException {
    try {
      security.runWithSubjectOrElevate(
          () -> {
            Metacard metacard = null;

            if (!create) {
              metacard = findMetacardForFeature(feature);
            }
            if (metacard == null) {
              metacard = createMetacardForFeature(feature);
              catalogFramework.create(new CreateRequestImpl(metacard));
            } else {
              catalogFramework.update(new UpdateRequestImpl(metacard.getId(), metacard));
            }
            return null;
          });
    } catch (SecurityServiceException | InvocationTargetException e) {
      throw new FeatureIndexingException(e.getMessage());
    }
  }

  private Metacard createMetacardForFeature(SimpleFeature feature) throws FeatureIndexingException {
    Metacard metacard = new MetacardImpl();

    String countryCode = (String) feature.getAttribute("ISO_A3");
    metacard.setAttribute(new AttributeImpl(Metacard.TITLE, countryCode));

    WKTWriter writer = new WKTWriter();
    String wkt = writer.write((Geometry) feature.getDefaultGeometry());
    metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));

    List<Serializable> tags = new ArrayList<>();
    tags.add("gazetteer");
    tags.add("country");
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));

    return metacard;
  }

  private Metacard findMetacardForFeature(SimpleFeature feature) throws FeatureIndexingException {
    String countryCode = (String) feature.getAttribute("ISO_A3");

    QueryRequest queryRequest =
        new QueryRequestImpl(new QueryImpl(getFilterForCountryCode(countryCode)));
    try {
      SourceResponse response = catalogFramework.query(queryRequest);
      if (response.getResults().isEmpty()) {
        return null;
      }
      return response.getResults().get(0).getMetacard();
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new FeatureIndexingException(e.getMessage());
    }
  }

  private void removeExistingMetacards() throws FeatureIndexingException {
    QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(getFilter()));

    try {
      security.runWithSubjectOrElevate(
          () -> {
            SourceResponse response = catalogFramework.query(queryRequest);
            for (Result result : response.getResults()) {
              catalogFramework.delete(new DeleteRequestImpl(result.getMetacard().getId()));
            }
            return null;
          });
    } catch (SecurityServiceException | InvocationTargetException e) {
      throw new FeatureIndexingException(e.getMessage());
    }
  }

  private Filter getFilterForCountryCode(String countryCode) {
    List<Filter> filters = new ArrayList<>();
    filters.add(filterBuilder.attribute(Metacard.TAGS).is().like().text("gazetteer"));
    filters.add(filterBuilder.attribute(Metacard.TAGS).is().like().text("country"));
    if (countryCode != null) {
      filters.add(filterBuilder.attribute(Metacard.TITLE).is().equalTo().text(countryCode));
    }
    return filterBuilder.allOf(filters);
  }

  private Filter getFilter() {
    return getFilterForCountryCode(null);
  }
}
