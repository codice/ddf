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
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.service.SecurityServiceException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GazetteerConstants;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogFeatureIndexer implements FeatureIndexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFeatureIndexer.class);

  private Security security = Security.getInstance();

  private CatalogFramework catalogFramework;

  private CatalogHelper catalogHelper;

  private IndexCallback indexCallback;
  private int extractionCount;
  private boolean doCreate;

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      new ThreadLocal<WKTWriter>() {
        @Override
        protected WKTWriter initialValue() {
          return new WKTWriter();
        }
      };

  public void setSecurity(Security security) {
    this.security = security;
  }

  public CatalogFeatureIndexer(CatalogFramework catalogFramework, CatalogHelper catalogHelper) {
    this.catalogFramework = catalogFramework;
    this.catalogHelper = catalogHelper;
  }

  @Override
  public void updateIndex(
      String resource, FeatureExtractor featureExtractor, boolean create, IndexCallback callback)
      throws FeatureExtractionException, FeatureIndexingException {
    if (featureExtractor == null) {
      throw new IllegalArgumentException("featureExtractor can't be null");
    }
    doCreate = create;
    indexCallback = callback;
    extractionCount = 0;

    if (create) {
      removeExistingMetacards();
    }

    featureExtractor.pushFeaturesToExtractionCallback(resource, this::handleFeatureExtraction);
  }

  private void handleFeatureExtraction(SimpleFeature feature) throws FeatureIndexingException {
    createOrUpdateMetacardForFeature(feature, doCreate);
    indexCallback.indexed(++extractionCount);
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
      LOGGER.warn("Failed to index feature", e);
      throw new FeatureIndexingException(e.getMessage());
    }
  }

  private Metacard createMetacardForFeature(SimpleFeature feature) throws FeatureIndexingException {
    Metacard metacard = new MetacardImpl();

    String countryCode = (String) feature.getAttribute("ISO_A3");
    metacard.setAttribute(new AttributeImpl(Metacard.TITLE, countryCode));

    String wkt = WKT_WRITER_THREAD_LOCAL.get().write((Geometry) feature.getDefaultGeometry());
    metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));

    List<Serializable> tags = new ArrayList<>();
    tags.add(GazetteerConstants.DEFAULT_TAG);
    tags.add(GazetteerConstants.COUNTRY_TAG);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));

    return metacard;
  }

  private Metacard findMetacardForFeature(SimpleFeature feature) throws FeatureIndexingException {
    String countryCode = (String) feature.getAttribute("ISO_A3");

    QueryRequest queryRequest =
        new QueryRequestImpl(catalogHelper.getQueryForCountryCode(countryCode));
    try {
      SourceResponse response = catalogFramework.query(queryRequest);
      if (response.getResults().isEmpty()) {
        return null;
      }
      return response.getResults().get(0).getMetacard();
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.warn("Failed to query for existing feature", e);
      throw new FeatureIndexingException(e.getMessage());
    }
  }

  private void removeExistingMetacards() throws FeatureIndexingException {
    QueryRequest queryRequest = new QueryRequestImpl(catalogHelper.getQueryForAllCountries());

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
      LOGGER.warn("Failed to remove existing feature", e);
      throw new FeatureIndexingException(e.getMessage());
    }
  }
}
