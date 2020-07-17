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
package ddf.catalog.solr.offlinegazetteer;

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COLLECTION_NAME;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COUNTRY_CODE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.FEATURE_CODE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_REQUEST_HANDLER;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.LOCATION;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.NAME;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.POPULATION;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SORT_VALUE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_BUILD_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_Q_KEY;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.types.Location;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.context.jts.ValidationRule;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GazetteerQueryOfflineSolr implements GeoEntryQueryable {

  private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerQueryOfflineSolr.class);

  private static final String CITY_SOLR_QUERY =
      GeoCodingConstants.CITY_FEATURE_CODES
          .stream()
          .map(fc -> String.format("feature-code_txt:%s", fc))
          .collect(Collectors.joining(" OR ", "(", ")"));

  private static final int MAX_RESULTS = 100;
  private static final double KM_PER_DEGREE = 111.139;

  private static final Map<String, String> SPATIAL_CONTEXT_ARGUMENTS =
      ImmutableMap.of(
          "spatialContextFactory",
          JtsSpatialContextFactory.class.getName(),
          "validationRule",
          ValidationRule.repairConvexHull.name());

  private static final SpatialContext SPATIAL_CONTEXT =
      SpatialContextFactory.makeSpatialContext(SPATIAL_CONTEXT_ARGUMENTS, null);

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private static final ThreadLocal<WKTReader> WKT_READER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTReader::new);

  private final SolrClient client;

  public GazetteerQueryOfflineSolr(
      SolrClientFactory clientFactory) {

    this.client = clientFactory.newClient(COLLECTION_NAME);
  }

  @Override
  public List<GeoEntry> query(String queryString, int maxResults) throws GeoEntryQueryException {
    SolrQuery solrQuery =
        new SolrQuery(String.format("title_txt:\"%s\"", ClientUtils.escapeQueryChars(queryString)));
    solrQuery.setRows(Math.min(maxResults, GazetteerQueryOfflineSolr.MAX_RESULTS));

    QueryResponse response = null;
    try {
      response = client.query(solrQuery, METHOD.POST);
    } catch (SolrServerException | IOException e) {
      throw new GeoEntryQueryException("Error while querying", e);
    }
    return response
        .getResults()
        .stream()
        .map(this::transformMetacardToGeoEntry)
        .collect(Collectors.toList());
  }

  @Override
  public GeoEntry queryById(String id) throws GeoEntryQueryException {
    SolrQuery solrQuery =
        new SolrQuery(String.format("id_txt:\"%s\"", ClientUtils.escapeQueryChars(id)));
    solrQuery.setRows(1);

    QueryResponse response = null;
    try {
      response = client.query(solrQuery, METHOD.POST);
    } catch (SolrServerException | IOException e) {
      throw new GeoEntryQueryException("Error while querying by ID", e);
    }

    return response
        .getResults()
        .stream()
        .map(this::transformMetacardToGeoEntry)
        .findFirst()
        .orElseThrow(() -> new GeoEntryQueryException("Could not find id"));
  }

  @Override
  public List<Suggestion> getSuggestedNames(String queryString, int maxResults)
      throws GeoEntryQueryException {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setRequestHandler(GAZETTEER_REQUEST_HANDLER);
    solrQuery.setParam(SUGGEST_Q_KEY, ClientUtils.escapeQueryChars(queryString));
    solrQuery.setParam(SUGGEST_DICT_KEY, SUGGEST_DICT);
    solrQuery.setParam("suggest.count", Integer.toString(Math.min(maxResults, MAX_RESULTS)));

    QueryResponse response;
    try {
      response = client.query(solrQuery);
    } catch (SolrServerException | IOException e) {
      throw new GeoEntryQueryException("Error while querying", e);
    }

    return Optional.ofNullable(response)
        .map(QueryResponse::getSuggesterResponse)
        .map(SuggesterResponse::getSuggestions)
        .map(suggestionsPerDict -> suggestionsPerDict.get(SUGGEST_DICT))
        .orElse(Collections.emptyList())
        .stream()
        .map(suggestion -> new SuggestionImpl(suggestion.getPayload(), suggestion.getTerm()))
        .collect(Collectors.toList());
  }

  public static final class SuggestionImpl implements Suggestion {
    private final String id;
    private final String name;

    public SuggestionImpl(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  @Override
  public List<NearbyLocation> getNearestCities(String location, int radiusInKm, int maxResults)
      throws ParseException, GeoEntryQueryException {
    Geometry geometry;
    try {
      geometry = WKT_READER_THREAD_LOCAL.get().read(location);
    } catch (org.locationtech.jts.io.ParseException e) {
      throw new GeoEntryQueryException("Could not parse location");
    }
    final Geometry originalGeometry = geometry;
    Geometry bufferedGeo = originalGeometry.buffer(convertKilometerToDegree(radiusInKm), 14);
    String wkt = WKT_WRITER_THREAD_LOCAL.get().write(bufferedGeo);

    String q =
        String.format(
            "location_geo_index:\"Intersects( %s ) AND %s\"",
            ClientUtils.escapeQueryChars(wkt), CITY_SOLR_QUERY);

    SolrQuery solrQuery = new SolrQuery(q);
    solrQuery.setRows(Math.min(maxResults, MAX_RESULTS));

    QueryResponse response;
    try {
      response = client.query(solrQuery, METHOD.POST);
    } catch (SolrServerException | IOException e) {
      throw new GeoEntryQueryException("Error executing query for nearest cities", e);
    }

    return response
        .getResults()
        .stream()
        .map(result -> convert(result, originalGeometry))
        .collect(Collectors.toList());
  }

  private NearbyLocation convert(SolrDocument doc, Geometry originalLocation) {
    String location = getField(doc, "location_geo", String.class);
    String title =
        Optional.ofNullable(getField(doc, "title_txt", String.class))
            .filter(Objects::nonNull)
            .filter(s -> !s.isEmpty())
            .orElse("NO TITLE");

    String cardinalDirection = "";
    double distance = 0;
    try {
      Geometry geo = WKT_READER_THREAD_LOCAL.get().read(location);
      cardinalDirection =
          bearingToCardinalDirection(getBearing(originalLocation.getCentroid(), geo.getCentroid()));
      distance = convertDegreeToKilometer(originalLocation.distance(geo.getCentroid()));
    } catch (org.locationtech.jts.io.ParseException e) {
      LOGGER.debug("Could not parse location for item (object: {})", doc.toString(), e);
    }

    return new NearbyLocationImpl(title, cardinalDirection, distance);
  }

  public static final class NearbyLocationImpl implements NearbyLocation {
    private final String name;
    private final String cardinalDirection;
    private final double distance;

    public NearbyLocationImpl(String name, String cardinalDirection, double distance) {
      this.name = name;
      this.cardinalDirection = cardinalDirection;
      this.distance = distance;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getCardinalDirection() {
      return cardinalDirection;
    }

    @Override
    public double getDistance() {
      return distance;
    }
  }
  /**
   * Calculates the bearing from the start point to the end point (i.e., the <em>initial bearing
   * </em>) in degrees.
   *
   * @param startPoint the point from which to start
   * @param endPoint the point at which to end
   * @return the bearing from {@code startPoint} to {@code endPoint}, in degrees
   */
  private static double getBearing(final Point startPoint, final Point endPoint) {
    final double lat1 = startPoint.getY();
    final double lon1 = startPoint.getX();

    final double lat2 = endPoint.getY();
    final double lon2 = endPoint.getX();

    final double lonDiffRads = Math.toRadians(lon2 - lon1);
    final double lat1Rads = Math.toRadians(lat1);
    final double lat2Rads = Math.toRadians(lat2);
    final double y = Math.sin(lonDiffRads) * Math.cos(lat2Rads);
    final double x =
        Math.cos(lat1Rads) * Math.sin(lat2Rads)
            - Math.sin(lat1Rads) * Math.cos(lat2Rads) * Math.cos(lonDiffRads);

    return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
  }

  /**
   * Takes a bearing in degrees and returns the corresponding cardinal direction as a string.
   *
   * @param bearing the bearing, in degrees
   * @return the cardinal direction corresponding to {@code bearing} (N, NE, E, SE, S, SW, W, NW)
   */
  private static String bearingToCardinalDirection(final double bearing) {
    final String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
    return directions[(int) Math.round(bearing / 45)];
  }

  @Override
  public Optional<String> getCountryCode(String wktLocation, int radius)
      throws GeoEntryQueryException, ParseException {
    String wkt;
    try {
      Point center =
          WKT_READER_THREAD_LOCAL
              .get()
              .read(fixSelfIntersectingGeometry(wktLocation))
              .getCentroid();
      wkt = WKT_WRITER_THREAD_LOCAL.get().write(center.buffer(convertKilometerToDegree(radius)));
    } catch (org.locationtech.jts.io.ParseException e) {
      LOGGER.debug("Could not parse wkt: {}", wktLocation, e);
      throw new GeoEntryQueryException("Could not parse wkt", e);
    }

    SolrQuery solrQuery =
        new SolrQuery(
            String.format(
                "location_geo_index:\"Intersects( %s )\"", ClientUtils.escapeQueryChars(wkt)));
    solrQuery.setRows(1);

    QueryResponse response;
    try {
      response = client.query(solrQuery, METHOD.POST);
    } catch (SolrServerException | IOException e) {
      LOGGER.debug("Could not query for country code ({})", wktLocation, e);
      throw new GeoEntryQueryException("Error encountered when querying", e);
    }

    return response
        .getResults()
        .stream()
        .findFirst()
        .map(doc -> getField(doc, Location.COUNTRY_CODE + "_txt", String.class));
  }

  private String fixSelfIntersectingGeometry(String wkt) {
    try {
      Shape wktShape = SPATIAL_CONTEXT.getFormats().getWktReader().read(wkt);
      // All polygons will be an instance of JtsGeometry. If it is not a polygon we don't need
      // to do anything with it so just return the original wkt string.
      if (!(wktShape instanceof JtsGeometry)) {
        return wkt;
      }
      return SPATIAL_CONTEXT.getFormats().getWktWriter().toString(wktShape);
    } catch (IOException | java.text.ParseException | InvalidShapeException e) {
      LOGGER.info("Failed to fix or read WKT: {}", wkt, e);
    }
    return wkt;
  }

  private GeoEntry transformMetacardToGeoEntry(SolrDocument document) {
    GeoEntry.Builder geoEntryBuilder = new GeoEntry.Builder();
    String featureCode = getField(document, FEATURE_CODE, String.class);

    if (StringUtils.isNotBlank(featureCode)) {
      geoEntryBuilder.featureCode(featureCode);
    }

    String countryCode = getField(document, COUNTRY_CODE, String.class);
    if (StringUtils.isNotBlank(countryCode)) {
      geoEntryBuilder.countryCode(countryCode);
    }

    String name = getField(document, NAME, String.class);
    if (StringUtils.isNotBlank(name)) {
      geoEntryBuilder.name(name);
    }

    Long population = getField(document, POPULATION, Long.class);
    if (population != null) {
      geoEntryBuilder.population(population);
    }

    Integer sortValue = getField(document, SORT_VALUE, Integer.class);
    if (sortValue != null) {
      geoEntryBuilder.gazetteerSort(sortValue);
    }

    String location = getField(document, LOCATION, String.class);
    if (StringUtils.isNotBlank(location)) {
      try {
        Geometry geometry = WKT_READER_THREAD_LOCAL.get().read(location);
        Point coordinate = geometry.getCentroid();
        if (!coordinate.isEmpty()) {
          Double lat = coordinate.getY();
          Double lon = coordinate.getX();
          geoEntryBuilder.latitude(lat);
          geoEntryBuilder.longitude(lon);
        }
      } catch (org.locationtech.jts.io.ParseException e) {
        LOGGER.debug("GeoEntry metacard does not contain (readable) location attribute.");
      }
    }
    LOGGER.trace("Original solr document: {}", document);
    LOGGER.trace("GeoEntry Builder: {}", geoEntryBuilder);

    return geoEntryBuilder.build();
  }

  private <T> T getField(SolrDocument document, String attribute, Class<T> clazz) {
    return Optional.of(document)
        .map(d -> d.get(attribute))
        .filter(List.class::isInstance)
        .map(l -> (List<Object>) l)
        .orElse(Collections.emptyList())
        .stream()
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .findFirst()
        .orElse(null);
  }

  private double convertKilometerToDegree(int distanceInKilometers) {
    return distanceInKilometers / KM_PER_DEGREE;
  }

  private double convertDegreeToKilometer(double distanceInDegrees) {
    return distanceInDegrees * KM_PER_DEGREE;
  }
}
