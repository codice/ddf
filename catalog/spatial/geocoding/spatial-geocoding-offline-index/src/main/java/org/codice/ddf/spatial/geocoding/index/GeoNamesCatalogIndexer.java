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
package org.codice.ddf.spatial.geocoding.index;

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
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoNamesCatalogIndexer implements GeoEntryIndexer {

  private static final int BATCH_SIZE = 250;

  private static final String TITLE_FORMAT = "%s, %s";

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesCatalogIndexer.class);

  private CatalogFramework catalogFramework;

  private MetacardType geoNamesMetacardType;

  private UuidGenerator uuidGenerator;

  private Filter filter;

  private List<CatalogProvider> catalogProviders;

  private FilterBuilder filterBuilder;

  public GeoNamesCatalogIndexer(
      CatalogFramework catalogFramework,
      UuidGenerator uuidGenerator,
      MetacardType metacardType,
      FilterBuilder filterBuilder,
      List<CatalogProvider> catalogProviders) {
    this.catalogFramework = catalogFramework;
    this.uuidGenerator = uuidGenerator;
    this.geoNamesMetacardType = metacardType;
    this.catalogProviders = catalogProviders;
    this.filterBuilder = filterBuilder;
    filter =
        filterBuilder.allOf(
            filterBuilder
                .attribute(Core.METACARD_TAGS)
                .is()
                .equalTo()
                .text(GeoCodingConstants.GEONAMES_TAG),
            filterBuilder
                .attribute(Core.METACARD_TAGS)
                .is()
                .equalTo()
                .text(GAZETTEER_METACARD_TAG));
  }

  private Metacard transformGeoEntryToMetacard(GeoEntry geoEntry) {
    if (!GeoCodingConstants.CITY_FEATURE_CODES.contains(geoEntry.getFeatureCode())) {
      return null;
    }

    Metacard metacard = new MetacardImpl(geoNamesMetacardType);
    String id = uuidGenerator.generateUuid();
    metacard.setAttribute(
        new AttributeImpl(
            Core.TITLE,
            String.format(TITLE_FORMAT, geoEntry.getName(), geoEntry.getCountryCode())));
    metacard.setAttribute(new AttributeImpl(Core.DESCRIPTION, geoEntry.getAlternateNames()));
    metacard.setAttribute(new AttributeImpl(Location.COUNTRY_CODE, geoEntry.getCountryCode()));
    metacard.setAttribute(new AttributeImpl(Core.ID, id));
    metacard.setAttribute(
        new AttributeImpl(
            GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME, geoEntry.getFeatureCode()));
    Integer gazetteerSortValue = getGeoNameGazetterSortByFeatureClass(geoEntry);
    if (gazetteerSortValue != null) {
      metacard.setAttribute(
          new AttributeImpl(
              GeoEntryAttributes.GAZETTEER_SORT_VALUE, gazetteerSortValue.intValue()));
    } else {
      metacard.setAttribute(
          new AttributeImpl(GeoEntryAttributes.GAZETTEER_SORT_VALUE, geoEntry.getPopulation()));
    }
    metacard.setAttribute(
        new AttributeImpl(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME, geoEntry.getPopulation()));
    if (StringUtils.isNotBlank(geoEntry.getImportLocation())) {
      metacard.setAttribute(
          new AttributeImpl(GeoEntryAttributes.IMPORT_LOCATION, geoEntry.getImportLocation()));
    }

    Double latitude = geoEntry.getLatitude();
    Double longitude = geoEntry.getLongitude();

    if (latitude != null && longitude != null) {
      Coordinate coordinate = new Coordinate(longitude, latitude);
      Geometry geometry = new GeometryFactory().createPoint(coordinate);
      String wkt = WKT_WRITER_THREAD_LOCAL.get().write(geometry);
      metacard.setAttribute(new AttributeImpl(Core.LOCATION, wkt));
    }

    metacard.setAttribute(
        new AttributeImpl(
            Core.METACARD_TAGS,
            Arrays.asList(GAZETTEER_METACARD_TAG, GeoCodingConstants.GEONAMES_TAG)));
    return metacard;
  }

  @Override
  public void updateIndex(
      String resource,
      GeoEntryExtractor geoEntryExtractor,
      boolean create,
      ProgressCallback progressCallback)
      throws GeoEntryIndexingException, GeoEntryExtractionException,
          GeoNamesRemoteDownloadException {
    if (StringUtils.isBlank(resource)) {
      LOGGER.debug("The resource was null or empty.");
      return;
    }

    List<Metacard> metacardList = new ArrayList<>();

    final GeoEntryExtractor.ExtractionCallback extractionCallback =
        new GeoEntryExtractor.ExtractionCallback() {
          @Override
          public void extracted(final GeoEntry newEntry) throws GeoEntryIndexingException {
            Metacard metacard = transformGeoEntryToMetacard(newEntry);
            if (metacard != null) {
              metacardList.add(metacard);
            }
          }

          @Override
          public void updateProgress(final int progress) {
            if (progressCallback != null) {
              progressCallback.updateProgress(progress);
            }
          }
        };

    if (create) {
      RetryPolicy retryPolicy =
          new RetryPolicy()
              .withDelay(10, TimeUnit.SECONDS)
              .withMaxDuration(5, TimeUnit.MINUTES)
              .retryOn(Exception.class);

      Failsafe.with(retryPolicy)
          .run(() -> removeGeoNamesMetacardsFromCatalog(resource, extractionCallback));
    }

    geoEntryExtractor.pushGeoEntriesToExtractionCallback(resource, extractionCallback);

    if (CollectionUtils.isEmpty(metacardList)) {
      LOGGER.debug("No Metacards were created from the resource.");
      return;
    }

    executeCreateMetacardRequest(metacardList);

    LOGGER.trace("All data created for: {}", resource);
  }

  private void removeGeoNamesMetacardsFromCatalog(
      String resource, ProgressCallback extractionCallback)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          IngestException {
    extractionCallback.updateProgress(0);

    Optional<CatalogProvider> catalogProviderOptional = catalogProviders.stream().findFirst();
    if (catalogProviderOptional.isPresent()) {
      CatalogProvider catalogProvider = catalogProviderOptional.get();

      if (StringUtils.isNotBlank(resource)) {
        filter =
            filterBuilder.allOf(
                filterBuilder
                    .attribute(GeoEntryAttributes.IMPORT_LOCATION)
                    .is()
                    .equalTo()
                    .text(resource),
                filter);
      }

      while (true) {
        Query query =
            new QueryImpl(
                filter, 1, BATCH_SIZE, SortBy.NATURAL_ORDER, false, TimeUnit.SECONDS.toMillis(90));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        LOGGER.trace("Removing existing geonames data with filter: {}", filter);

        QueryResponse response = catalogFramework.query(queryRequest);
        List<Serializable> metacardsToDelete =
            response
                .getResults()
                .stream()
                .map(Result::getMetacard)
                .map(Metacard::getId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(metacardsToDelete)) {
          break;
        }

        LOGGER.trace("Deleting {} GeoNames metacards", metacardsToDelete.size());
        removeMetacards(catalogProvider, extractionCallback, metacardsToDelete);
      }
    }
    extractionCallback.updateProgress(50);
  }

  private void removeMetacards(
      CatalogProvider catalogProvider,
      ProgressCallback extractionCallback,
      List<Serializable> metacards)
      throws IngestException {
    for (int i = 0; i < metacards.size(); i += BATCH_SIZE) {
      int lastIndex = i + BATCH_SIZE;
      if (lastIndex > metacards.size()) {
        lastIndex = metacards.size();
      }

      List<Serializable> sublist = metacards.subList(i, lastIndex);

      DeleteRequest deleteRequest = new DeleteRequestImpl(sublist, Core.ID, new HashMap<>());
      DeleteResponse deleteResponse = catalogProvider.delete(deleteRequest);
      List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();
      LOGGER.debug("{} metacards deleted.", deletedMetacards == null ? 0 : deletedMetacards.size());
      extractionCallback.updateProgress((int) (((double) (i + 1) / metacards.size()) * 50));
    }
  }

  @Override
  public void updateIndex(
      List<GeoEntry> newEntries, boolean create, ProgressCallback progressCallback, String resource)
      throws GeoEntryIndexingException {
    if (create) {
      RetryPolicy retryPolicy =
          new RetryPolicy()
              .withDelay(10, TimeUnit.SECONDS)
              .withMaxDuration(5, TimeUnit.MINUTES)
              .retryOn(Exception.class);

      Failsafe.with(retryPolicy)
          .run(() -> removeGeoNamesMetacardsFromCatalog(resource, progressCallback));
    }

    List<Metacard> metacards = new ArrayList<>();
    for (GeoEntry geoEntry : newEntries) {
      metacards.add(transformGeoEntryToMetacard(geoEntry));
    }
    executeCreateMetacardRequest(metacards);
  }

  private void executeCreateMetacardRequest(List<Metacard> metacards) {
    int totalMetacards = metacards.size();

    for (int i = 0; i < totalMetacards; i += BATCH_SIZE) {

      int lastIndex = i + BATCH_SIZE;
      if (lastIndex > metacards.size()) {
        lastIndex = metacards.size();
      }

      List<Metacard> sublist = metacards.subList(i, lastIndex);
      Map<String, Serializable> properties = new HashMap<>();
      CreateRequest createRequest = new CreateRequestImpl(sublist, properties);
      try {
        CreateResponse createResponse = catalogFramework.create(createRequest);
        List<Metacard> createdMetacards = createResponse.getCreatedMetacards();
        LOGGER.trace(
            "Created {} metacards.", createdMetacards == null ? 0 : createdMetacards.size());
      } catch (IngestException | SourceUnavailableException e) {
        LOGGER.debug("Unable to create Metacards", e);
      }
    }

    LOGGER.trace("Created {} metacards.", totalMetacards);
  }

  private Integer getGeoNameGazetterSortByFeatureClass(GeoEntry geoEntry) {
    Integer gazetteerSortValue = null;
    if (geoEntry.getFeatureClass() == null) {
      return gazetteerSortValue;
    } else {
      switch (geoEntry.getFeatureClass()) {
        case GeoCodingConstants.ADMIN_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(GeoCodingConstants.ADMIN_FEATURE_CLASS);
          break;
        case GeoCodingConstants.HYDROGRAPHIC_FEATURE_CLASS:
          if (geoEntry.getFeatureCode().equals(GeoCodingConstants.OCEAN_FEATURE_CODE)
              || geoEntry.getFeatureCode().equals(GeoCodingConstants.SEA_FEATURE_CODE)) {
            gazetteerSortValue = GeoCodingConstants.SPECIAL_GAZETTEER_SORT_VALUE;
          } else {
            gazetteerSortValue =
                GeoCodingConstants.FEATURE_CLASS_VALUES.get(
                    GeoCodingConstants.HYDROGRAPHIC_FEATURE_CLASS);
          }
          break;
        case GeoCodingConstants.AREA_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(GeoCodingConstants.AREA_FEATURE_CLASS);
          break;
        case GeoCodingConstants.POPULATED_FEATURE_CLASS:
          break;
        case GeoCodingConstants.ROAD_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(GeoCodingConstants.ROAD_FEATURE_CLASS);
          break;
        case GeoCodingConstants.SPOT_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(GeoCodingConstants.SPOT_FEATURE_CLASS);
          break;
        case GeoCodingConstants.MOUNTAIN_FEATURE_CLASS:
          if (geoEntry.getFeatureCode().equals(GeoCodingConstants.MOUNTAIN_FEATURE_CODE)
              || geoEntry.getFeatureCode().equals(GeoCodingConstants.MOUNTAIN_RANGE_FEATURE_CODE)) {
            gazetteerSortValue = GeoCodingConstants.SPECIAL_GAZETTEER_SORT_VALUE;
          } else {
            gazetteerSortValue =
                GeoCodingConstants.FEATURE_CLASS_VALUES.get(
                    GeoCodingConstants.MOUNTAIN_FEATURE_CLASS);
          }
          break;
        case GeoCodingConstants.UNDERSEA_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(
                  GeoCodingConstants.UNDERSEA_FEATURE_CLASS);
          break;
        case GeoCodingConstants.VEGETATION_FEATURE_CLASS:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(
                  GeoCodingConstants.VEGETATION_FEATURE_CLASS);
          break;
        default:
          gazetteerSortValue =
              GeoCodingConstants.FEATURE_CLASS_VALUES.get(
                  GeoCodingConstants.VEGETATION_FEATURE_CLASS);
          break;
      }
    }
    return gazetteerSortValue;
  }
}
