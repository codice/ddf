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
package org.codice.ddf.spatial.geocoding.query;

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;
import static ddf.catalog.Constants.SUGGESTION_CONTEXT_KEY;
import static ddf.catalog.Constants.SUGGESTION_DICT_KEY;
import static ddf.catalog.Constants.SUGGESTION_QUERY_KEY;
import static ddf.catalog.Constants.SUGGESTION_RESULT_KEY;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.SUGGEST_PLACE_KEY;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.context.impl.NearbyLocationImpl;
import org.codice.ddf.spatial.geocoding.context.impl.SuggestionImpl;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.impl.PointImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GazetteerQueryCatalog implements GeoEntryQueryable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerQueryCatalog.class);

  private static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;

  private static final String ERROR_MESSAGE = "Unable to execute query on the catalog.";

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private static final ThreadLocal<WKTReader> WKT_READER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTReader::new);

  private static final int KM_TO_M = 1000;

  private static final long TIMEOUT = 10000L;

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private Filter tagFilter;

  private List<Filter> featureCodeFilters;

  public GazetteerQueryCatalog(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.featureCodeFilters = new ArrayList<>();

    for (String cityFeatureCode : GeoCodingConstants.CITY_FEATURE_CODES) {
      Filter filter =
          filterBuilder
              .attribute(GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME)
              .is()
              .equalTo()
              .text(cityFeatureCode);
      featureCodeFilters.add(filter);
    }

    tagFilter =
        filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(GAZETTEER_METACARD_TAG);
  }

  @Override
  public List<GeoEntry> query(String queryString, int maxResults) throws GeoEntryQueryException {
    Filter textFilter = filterBuilder.attribute(Core.TITLE).is().like().text(queryString);
    Filter queryFilter = filterBuilder.allOf(tagFilter, textFilter);
    Map<String, Serializable> properties = new HashMap<>();

    SortBy featureCodeSortBy =
        new SortByImpl(GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME, SortOrder.ASCENDING);
    SortBy populationSortBy =
        new SortByImpl(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME, SortOrder.DESCENDING);
    SortBy[] sortbys = {populationSortBy};
    properties.put(ADDITIONAL_SORT_BYS, sortbys);

    Query query = new QueryImpl(queryFilter, 1, maxResults, featureCodeSortBy, false, TIMEOUT);
    QueryRequest queryRequest = new QueryRequestImpl(query, properties);
    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new GeoEntryQueryException(ERROR_MESSAGE, e);
    }

    return queryResponse
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .map(this::transformMetacardToGeoEntry)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public GeoEntry queryById(String id) throws GeoEntryQueryException {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id cannot be blank or null");
    }

    Filter idFilter = filterBuilder.attribute(Core.ID).is().text(id);
    Filter queryFilter = filterBuilder.allOf(tagFilter, idFilter);

    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(queryFilter)));
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new GeoEntryQueryException(ERROR_MESSAGE, e);
    }

    if (!queryResponse.getResults().isEmpty()) {
      Result result = queryResponse.getResults().get(0);
      return transformMetacardToGeoEntry(result.getMetacard());
    }

    return null;
  }

  @Override
  public List<Suggestion> getSuggestedNames(String queryString, int maxResults)
      throws GeoEntryQueryException {
    Map<String, Serializable> suggestProps = new HashMap<>();
    suggestProps.put(SUGGESTION_QUERY_KEY, queryString);
    suggestProps.put(SUGGESTION_CONTEXT_KEY, GAZETTEER_METACARD_TAG);
    suggestProps.put(SUGGESTION_DICT_KEY, SUGGEST_PLACE_KEY);

    Query suggestionQuery = new QueryImpl(filterBuilder.attribute(Core.TITLE).text(queryString));

    QueryRequest suggestionRequest = new QueryRequestImpl(suggestionQuery, suggestProps);

    try {
      QueryResponse suggestionResponse = catalogFramework.query(suggestionRequest);

      if (suggestionResponse.getPropertyValue(SUGGESTION_RESULT_KEY) instanceof List) {
        List<Map.Entry<String, String>> suggestions =
            (List<Map.Entry<String, String>>)
                suggestionResponse.getPropertyValue(SUGGESTION_RESULT_KEY);

        return suggestions
            .stream()
            .map(suggestion -> new SuggestionImpl(suggestion.getKey(), suggestion.getValue()))
            .limit(maxResults)
            .collect(Collectors.toList());
      }

    } catch (SourceUnavailableException | FederationException | UnsupportedQueryException e) {
      throw new GeoEntryQueryException("Failed to execute suggestion query", e);
    }
    return Collections.emptyList();
  }

  private GeoEntry transformMetacardToGeoEntry(Metacard metacard) {
    GeoEntry.Builder geoEntryBuilder = new GeoEntry.Builder();
    String featureCode =
        getStringAttributeFromMetacard(metacard, GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME);
    if (StringUtils.isNotBlank(featureCode)) {
      geoEntryBuilder.featureCode(featureCode);
    }

    String countryCode = getStringAttributeFromMetacard(metacard, Location.COUNTRY_CODE);
    if (StringUtils.isNotBlank(countryCode)) {
      geoEntryBuilder.countryCode(countryCode);
    }

    String name = getStringAttributeFromMetacard(metacard, Core.TITLE);
    if (StringUtils.isNotBlank(name)) {
      geoEntryBuilder.name(name);
    }

    Attribute attribute = metacard.getAttribute(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME);
    if (attribute != null && attribute.getValue() instanceof Long) {
      Long population = (Long) attribute.getValue();
      geoEntryBuilder.population(population);
    } else {
      LOGGER.debug("GeoEntry metacard does not contain population attribute.");
    }

    attribute = metacard.getAttribute(GeoEntryAttributes.GAZETTEER_SORT_VALUE);
    if (attribute != null) {
      geoEntryBuilder.gazetteerSort((Integer) attribute.getValue());
    } else {
      LOGGER.debug("GeoEntry does not contain Gazetteer Sort Value");
    }

    String location = getStringAttributeFromMetacard(metacard, Core.LOCATION);
    if (StringUtils.isNotBlank(location)) {
      try {
        Geometry geometry = WKT_READER_THREAD_LOCAL.get().read(location);
        Coordinate coordinate = geometry.getCoordinate();
        Double lat = coordinate.y;
        Double lon = coordinate.x;
        geoEntryBuilder.latitude(lat);
        geoEntryBuilder.longitude(lon);
      } catch (org.locationtech.jts.io.ParseException e) {
        LOGGER.debug("GeoEntry metacard does not contain location attribute.");
      }
    }
    return geoEntryBuilder.build();
  }

  private String getStringAttributeFromMetacard(Metacard metacard, String attributeName) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute != null) {
      Serializable serializable = attribute.getValue();
      if (serializable instanceof String) {
        return (String) serializable;
      }
    }
    return null;
  }

  @Override
  public List<NearbyLocation> getNearestCities(String location, int radiusInKm, int maxResults)
      throws ParseException, GeoEntryQueryException {
    Filter featureCodeFilter = filterBuilder.anyOf(featureCodeFilters);
    int radiusInMeters = radiusInKm * KM_TO_M;

    Filter textFilter =
        filterBuilder.attribute(Core.LOCATION).withinBuffer().wkt(location, radiusInMeters);

    Filter queryFilter = filterBuilder.allOf(featureCodeFilter, tagFilter, textFilter);
    Query query = new QueryImpl(queryFilter, 1, maxResults, SortBy.NATURAL_ORDER, false, TIMEOUT);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new GeoEntryQueryException(ERROR_MESSAGE, e);
    }

    return queryResponse
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .map(metacard -> transformMetacardToNearbyLocation(location, metacard))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private NearbyLocation transformMetacardToNearbyLocation(String location, Metacard metacard) {
    String metacardLocation = getStringAttributeFromMetacard(metacard, Core.LOCATION);
    String name = getStringAttributeFromMetacard(metacard, Core.TITLE);
    if (StringUtils.isEmpty(metacardLocation) || StringUtils.isEmpty(name)) {
      LOGGER.debug("GeoEntry metacard does not contain required attribute.");
      return null;
    }

    Double lat;
    Double lon;
    PointImpl centerPoint;

    try {
      Geometry geometry = WKT_READER_THREAD_LOCAL.get().read(metacardLocation);
      Coordinate coordinate = geometry.getCoordinate();
      lat = coordinate.x;
      lon = coordinate.y;

      Point center = WKT_READER_THREAD_LOCAL.get().read(location).getCentroid();
      centerPoint = new PointImpl(center.getY(), center.getX(), SPATIAL_CONTEXT);

    } catch (org.locationtech.jts.io.ParseException e) {
      LOGGER.debug("GeoEntry metacard does not contain location attribute.");
      return null;
    }

    return new NearbyLocationImpl(centerPoint, new PointImpl(lon, lat, SPATIAL_CONTEXT), name);
  }

  @Override
  public Optional<String> getCountryCode(String wktLocation, int radius)
      throws GeoEntryQueryException, ParseException {
    String wkt;

    try {
      Point center = WKT_READER_THREAD_LOCAL.get().read(wktLocation).getCentroid();
      Geometry geometry = GEOMETRY_FACTORY.createPoint(center.getCoordinate());
      wkt = WKT_WRITER_THREAD_LOCAL.get().write(geometry);
    } catch (org.locationtech.jts.io.ParseException e) {
      return Optional.empty();
    }

    int radiusInMeters = KM_TO_M * radius;
    Filter filter = filterBuilder.attribute(Core.LOCATION).withinBuffer().wkt(wkt, radiusInMeters);
    Filter queryFilter = filterBuilder.allOf(tagFilter, filter);
    Query query = new QueryImpl(queryFilter);
    QueryRequest queryRequest = new QueryRequestImpl(query);
    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new GeoEntryQueryException(ERROR_MESSAGE, e);
    }

    List<Result> results = queryResponse.getResults();
    if (CollectionUtils.isNotEmpty(results)) {
      Result firstResult = results.get(0);
      Metacard metacard = firstResult.getMetacard();
      String countryCode = getStringAttributeFromMetacard(metacard, Location.COUNTRY_CODE);
      return Optional.ofNullable(countryCode);
    }
    return Optional.empty();
  }
}
