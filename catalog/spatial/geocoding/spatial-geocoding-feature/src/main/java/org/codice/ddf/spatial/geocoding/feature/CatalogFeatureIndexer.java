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

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.Validate;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeature;

public class CatalogFeatureIndexer implements FeatureIndexer {
  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private static final String NAME_KEY = "name";

  private Security security = Security.getInstance();

  private CatalogFramework catalogFramework;

  private CatalogHelper catalogHelper;

  private MetacardType metacardType;

  public void setSecurity(Security security) {
    this.security = security;
  }

  public CatalogFeatureIndexer(
      CatalogFramework catalogFramework, CatalogHelper catalogHelper, MetacardType metacardType) {
    this.catalogFramework = catalogFramework;
    this.catalogHelper = catalogHelper;
    this.metacardType = metacardType;
  }

  @Override
  public void updateIndex(
      String resource, FeatureExtractor featureExtractor, boolean create, IndexCallback callback)
      throws FeatureExtractionException, FeatureIndexingException {
    Validate.notNull(featureExtractor, "featureExtractor can't be null");

    if (create) {
      removeExistingMetacards();
    }

    final AtomicInteger extractionCount = new AtomicInteger();

    featureExtractor.pushFeaturesToExtractionCallback(
        resource, feature -> handleFeatureExtraction(feature, create, callback, extractionCount));
  }

  private void handleFeatureExtraction(
      SimpleFeature feature,
      boolean create,
      IndexCallback indexCallback,
      AtomicInteger extractionCount)
      throws FeatureIndexingException {
    createOrUpdateMetacardForFeature(feature, create);
    indexCallback.indexed(extractionCount.incrementAndGet());
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
    Metacard metacard = new MetacardImpl(metacardType);
    String countryCode = feature.getID();

    Object nameObject = feature.getAttribute(NAME_KEY);
    if (nameObject instanceof String) {
      metacard.setAttribute(new AttributeImpl(Core.TITLE, (String) nameObject));

      metacard.setAttribute(new AttributeImpl(Location.COUNTRY_CODE, countryCode));
      String wkt = WKT_WRITER_THREAD_LOCAL.get().write((Geometry) feature.getDefaultGeometry());
      metacard.setAttribute(new AttributeImpl(Core.LOCATION, wkt));

      List<Serializable> tags =
          Arrays.asList(GAZETTEER_METACARD_TAG, GeoCodingConstants.COUNTRY_TAG);
      metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, tags));
      metacard.setAttribute(
          new AttributeImpl(
              GeoEntryAttributes.GAZETTEER_SORT_VALUE,
              GeoCodingConstants.COUNTRY_GAZETTEER_SORT_VALUE));

      if (isCountry(feature)) {
        metacard.setAttribute(
            new AttributeImpl(
                GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME,
                GeoCodingConstants.POLITICAL_ENTITY));
      }

      return metacard;
    }

    throw new FeatureIndexingException("Malformed feature");
  }

  private boolean isCountry(SimpleFeature feature) {
    return GeoCodingConstants.COUNTRY_TAG.equals(
        feature.getAttribute(GeoCodingConstants.GEOMETRY_TYPE));
  }

  private Metacard findMetacardForFeature(SimpleFeature feature) throws FeatureIndexingException {
    Object nameObject = feature.getAttribute(NAME_KEY);
    if (nameObject instanceof String) {
      String name = (String) nameObject;
      QueryRequest queryRequest = new QueryRequestImpl(catalogHelper.getQueryForName(name));
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
    throw new FeatureIndexingException("Unable to find feature");
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
      throw new FeatureIndexingException(e.getMessage());
    }
  }
}
