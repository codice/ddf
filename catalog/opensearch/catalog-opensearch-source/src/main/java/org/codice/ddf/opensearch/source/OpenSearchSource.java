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
package org.codice.ddf.opensearch.source;

import com.google.common.annotations.VisibleForTesting;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.geotools.xml.filter.FilterTransformer;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Federated site that talks via OpenSearch to the DDF platform. Communication is usually performed
 * via https which requires a keystore and trust store to be provided.
 */
public class OpenSearchSource implements FederatedSource, ConfiguredService {

  private static final String COULD_NOT_RETRIEVE_RESOURCE_MESSAGE = "Could not retrieve resource";

  private static final String UNABLE_TO_CREATE_HTTP_CLIENT = "Unable to create HTTP Client";

  private static final String ORGANIZATION = "DDF";

  private static final String TITLE = "OpenSearch DDF Federated Source";

  private static final String DESCRIPTION =
      "Queries DDF using the synchronous federated OpenSearch query";

  protected static final String USERNAME_PROPERTY = "username";

  @SuppressWarnings("squid:S2068" /*Key for the requestProperties map, not a hardcoded password*/)
  protected static final String PASSWORD_PROPERTY = "password";

  private static final int MIN_DISTANCE_TOLERANCE_IN_METERS = 1;

  private static final int MIN_NUM_POINT_RADIUS_VERTICES = 4;

  private static final int MAX_NUM_POINT_RADIUS_VERTICES = 32;

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSource.class);

  private HttpClient httpClient;

  // service properties
  protected String shortname;

  protected boolean localQueryOnly;

  protected boolean shouldConvertToBBox;

  protected int numMultiPointRadiusVertices;

  protected int distanceTolerance;

  protected PropertyResolver endpointUrl =
      new PropertyResolver(
          "${org.codice.ddf.external.protocol}${org.codice.ddf.external.hostname}:${org.codice.ddf.external.port}${org.codice.ddf.external.context}${org.codice.ddf.system.rootContext}/catalog/query");

  protected final FilterAdapter filterAdapter;

  protected String configurationPid;

  protected List<String> parameters;

  protected Set<String> markUpSet;

  protected String username = "";

  protected String password = "";

  private XMLInputFactory xmlInputFactory;

  protected ResourceReader resourceReader;

  protected final OpenSearchFilterVisitor openSearchFilterVisitor;

  protected Integer connectionTimeout = 30000;

  protected Integer receiveTimeout = 60000;

  protected boolean disableCnCheck = false;

  protected boolean allowRedirects = false;

  protected BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer;

  /** flag indicating whether the source could be contacted */
  private volatile boolean isAvailable;

  private ScheduledExecutorService scheduler;

  protected Integer pollInterval = 5;

  /**
   * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
   * overwritten using the setter methods.
   */
  public OpenSearchSource(
      FilterAdapter filterAdapter, OpenSearchFilterVisitor openSearchFilterVisitor) {
    this(filterAdapter, openSearchFilterVisitor, (elements, sourceResponse) -> {});
  }

  /**
   * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
   * overwritten using the setter methods.
   */
  public OpenSearchSource(
      FilterAdapter filterAdapter,
      OpenSearchFilterVisitor openSearchFilterVisitor,
      BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer) {
    this.filterAdapter = filterAdapter;
    this.openSearchFilterVisitor = openSearchFilterVisitor;
    this.foreignMarkupBiConsumer = foreignMarkupBiConsumer;
  }

  /**
   * Called when this OpenSearch Source is created, but after all of the setter methods have been
   * called for each property specified in the metatype.xml file.
   */
  public void init() {
    configureXmlInputFactory();
    updateFactory();
  }

  private void updateFactory() {
    httpClient = createHttpClient();
    updateScheduler();
  }

  private void updateScheduler() {
    LOGGER.debug(
        "Setting availability poll task for {} minute(s) on Source {}", pollInterval, getId());

    isAvailable = false;

    if (scheduler != null) {
      LOGGER.debug("Cancelling availability poll task on Source {}", getId());
      scheduler.shutdownNow();
    }

    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("openSearchSourceThread"));

    try {
      HttpRequest head =
          HttpRequest.newBuilder()
              .version(HttpClient.Version.HTTP_1_1)
              .method("HEAD", HttpRequest.BodyPublishers.noBody())
              .uri(new URI(getEndpointUrl()))
              .timeout(Duration.of(getReceiveTimeout(), ChronoUnit.MILLIS))
              .build();

      scheduler.scheduleWithFixedDelay(
          new Runnable() {
            private boolean availabilityCheck() {
              LOGGER.debug("Checking availability for source {} ", getId());
              try {
                final HttpResponse<Void> response =
                    httpClient.send(head, HttpResponse.BodyHandlers.discarding());
                return response != null
                    && !(response.statusCode() >= 404 || response.statusCode() == 402);
              } catch (Exception e) {
                LOGGER.debug("Web Client was unable to connect to endpoint.", e);
                return false;
              }
            }

            @Override
            public void run() {
              isAvailable = availabilityCheck();
            }
          },
          1,
          pollInterval.longValue() * 60L,
          TimeUnit.SECONDS);
    } catch (URISyntaxException e) {
      LOGGER.debug("Unable to convert endpoint to URI", e);
    }
  }

  public void destroy(int code) {
    if (scheduler != null) {
      LOGGER.debug("Cancelling availability poll task on Source {}", getId());
      scheduler.shutdownNow();
    }
  }

  protected HttpClient createHttpClient() {
    HttpClient.Builder clientBuilder = HttpClient.newBuilder();

    clientBuilder.connectTimeout(Duration.of(getConnectionTimeout(), ChronoUnit.MILLIS));

    if (getAllowRedirects()) {
      clientBuilder.followRedirects(HttpClient.Redirect.ALWAYS);
    }

    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
      clientBuilder.authenticator(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(username, password.toCharArray());
            }
          });
    }

    return clientBuilder.build();
  }

  private void configureXmlInputFactory() {
    xmlInputFactory = XMLInputFactory2.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlInputFactory.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
  }

  @Override
  public boolean isAvailable() {
    return isAvailable;
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    if (isAvailable()) {
      callback.setAvailable();
      return true;
    } else {
      callback.setUnavailable();
      return false;
    }
  }

  @Override
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    String methodName = "query";
    LOGGER.trace(methodName);

    final SourceResponse response;

    Query query = queryRequest.getQuery();
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Received query: {}", query);
      FilterTransformer transform = new FilterTransformer();
      transform.setIndentation(2);
      try {
        LOGGER.trace(transform.transform(query));
      } catch (TransformerException e) {
        LOGGER.debug("Error transforming query to XML", e);
      }
    }

    final OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        (OpenSearchFilterVisitorObject)
            query.accept(openSearchFilterVisitor, new OpenSearchFilterVisitorObject());

    final ContextualSearch contextualSearch = openSearchFilterVisitorObject.getContextualSearch();
    final SpatialSearch spatialSearch =
        createCombinedSpatialSearch(
            openSearchFilterVisitorObject.getPointRadiusSearches(),
            openSearchFilterVisitorObject.getGeometrySearches(),
            numMultiPointRadiusVertices,
            distanceTolerance);
    final TemporalFilter temporalSearch = openSearchFilterVisitorObject.getTemporalSearch();
    final String idSearch =
        StringUtils.defaultIfEmpty(
            (String) queryRequest.getPropertyValue(Metacard.ID),
            openSearchFilterVisitorObject.getId());

    final Map<String, String> searchPhraseMap =
        contextualSearch == null ? new HashMap<>() : contextualSearch.getSearchPhraseMap();

    if (MapUtils.isNotEmpty(searchPhraseMap)
        || spatialSearch != null
        || temporalSearch != null
        || StringUtils.isNotEmpty(idSearch)) {
      if (httpClient == null) {
        throw new UnsupportedQueryException(UNABLE_TO_CREATE_HTTP_CLIENT);
      }
      response =
          doOpenSearchQuery(queryRequest, spatialSearch, temporalSearch, idSearch, searchPhraseMap);
    } else {
      LOGGER.debug(
          "The OpenSearch Source only supports id searches or searches with certain keyword, \"{}\" temporal, or \"{}\" spatial criteria, but the query was {}. See the documentation for more details about supported searches.",
          OpenSearchConstants.SUPPORTED_TEMPORAL_SEARCH_TERM,
          OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM,
          query);
      throw new UnsupportedQueryException(
          "OpenSearch query parameters could not be created from the query criteria.");
    }

    setSourceId(response);

    LOGGER.trace(methodName);

    return response;
  }

  private SourceResponse doOpenSearchQuery(
      QueryRequest queryRequest,
      SpatialSearch spatialSearch,
      TemporalFilter temporalSearch,
      String idSearch,
      Map<String, String> searchPhraseMap)
      throws UnsupportedQueryException {
    URIBuilder uriBuilder;
    try {
      uriBuilder = new URIBuilder(getEndpointUrl());

      // All queries must have at least a search phrase to be valid
      if (searchPhraseMap.isEmpty() && temporalSearch == null && spatialSearch == null) {
        searchPhraseMap.put(OpenSearchConstants.SEARCH_TERMS, "*");
      }
      OpenSearchUriBuilder.populateSearchOptions(uriBuilder, queryRequest, parameters);
      OpenSearchUriBuilder.populateContextual(uriBuilder, searchPhraseMap, parameters);
      OpenSearchUriBuilder.populateTemporal(uriBuilder, temporalSearch, parameters);
      if (spatialSearch != null) {
        OpenSearchUriBuilder.populateSpatial(
            uriBuilder,
            spatialSearch.getGeometry(),
            spatialSearch.getBoundingBox(),
            spatialSearch.getPolygon(),
            spatialSearch.getPointRadius(),
            parameters);
      }

      if (idSearch != null && !idSearch.isBlank()) {
        uriBuilder.setParameter(OpenSearchConstants.RECORD_IDS, idSearch);
      }

      if (localQueryOnly) {
        uriBuilder.setParameter(OpenSearchConstants.SOURCES, OpenSearchConstants.LOCAL_SOURCE);
      } else {
        uriBuilder.setParameter(OpenSearchConstants.SOURCES, "");
      }

      InputStream responseStream = performRequest(uriBuilder.build());
      return processResponse(responseStream, queryRequest);
    } catch (URISyntaxException e) {
      throw new UnsupportedQueryException(UNABLE_TO_CREATE_HTTP_CLIENT, e);
    }
  }

  /** Set the source-id on every metacard this is missing a source-id. */
  private void setSourceId(SourceResponse sourceResponse) {
    if (sourceResponse != null && sourceResponse.getResults() != null) {
      sourceResponse.getResults().stream()
          .filter(Objects::nonNull)
          .map(Result::getMetacard)
          .filter(Objects::nonNull)
          .filter(metacard -> StringUtils.isBlank(metacard.getSourceId()))
          .forEach(metacard -> metacard.setSourceId(getId()));
    }
  }

  /**
   * Performs a GET request on the client and returns the entity as an InputStream.
   *
   * @param uri URI to perform the GET request on.
   * @return The entity of the response as an InputStream.
   */
  private InputStream performRequest(URI uri) throws UnsupportedQueryException {
    HttpResponse<InputStream> clientResponse = null;
    try {
      clientResponse =
          httpClient.send(
              HttpRequest.newBuilder()
                  .version(HttpClient.Version.HTTP_1_1)
                  .GET()
                  .uri(uri)
                  .timeout(Duration.of(getReceiveTimeout(), ChronoUnit.MILLIS))
                  .build(),
              HttpResponse.BodyHandlers.ofInputStream());
    } catch (IOException | InterruptedException e) {
      throw new UnsupportedQueryException(UNABLE_TO_CREATE_HTTP_CLIENT, e);
    }

    final InputStream stream = clientResponse.body();
    if (stream == null) {
      throw new UnsupportedQueryException("The response message does not contain an entity body.");
    }

    if (clientResponse.statusCode() != 200) {
      String error = "";
      try {
        error = IOUtils.toString(stream, StandardCharsets.UTF_8);
      } catch (IOException ioe) {
        LOGGER.debug("Could not convert error message to a string for output.", ioe);
      }
      String errorMsg =
          "Received error code from remote source (status "
              + clientResponse.statusCode()
              + "): "
              + error;
      throw new UnsupportedQueryException(errorMsg);
    }

    return stream;
  }

  /** Package-private so that tests may set the foreign markup consumer. */
  @VisibleForTesting
  void setForeignMarkupBiConsumer(
      BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer) {
    this.foreignMarkupBiConsumer = foreignMarkupBiConsumer;
  }

  private SourceResponseImpl processResponse(InputStream is, QueryRequest queryRequest)
      throws UnsupportedQueryException {
    List<Result> resultQueue = new ArrayList<>();

    SyndFeedInput syndFeedInput = new SyndFeedInput();
    SyndFeed syndFeed = null;
    try {
      syndFeed = syndFeedInput.build(new InputStreamReader(is, StandardCharsets.UTF_8));
    } catch (FeedException e) {
      LOGGER.debug("Unable to read RSS/Atom feed.", e);
    }

    List<SyndEntry> entries;
    long totalResults = 0;
    List<Element> foreignMarkup = null;
    if (syndFeed != null) {
      entries = syndFeed.getEntries();
      for (SyndEntry entry : entries) {
        resultQueue.addAll(createResponseFromEntry(entry));
      }
      totalResults = entries.size();
      foreignMarkup = syndFeed.getForeignMarkup();
      for (Element element : foreignMarkup) {
        if (element.getName().equals("totalResults")) {
          try {
            totalResults = Long.parseLong(element.getContent(0).getValue());
          } catch (NumberFormatException | IndexOutOfBoundsException e) {
            // totalResults is already initialized to the correct value, so don't change it here.
            LOGGER.debug("Received invalid number of results.", e);
          }
        }
      }
    }

    SourceResponseImpl response = new SourceResponseImpl(queryRequest, resultQueue);
    response.setHits(totalResults);

    if (foreignMarkup != null) {
      this.foreignMarkupBiConsumer.accept(Collections.unmodifiableList(foreignMarkup), response);
    }

    return response;
  }

  /**
   * Creates a single response from input parameters. Performs XPath operations on the document to
   * retrieve data not passed in.
   *
   * @param entry a single Atom entry
   * @return single response
   */
  private List<Result> createResponseFromEntry(SyndEntry entry) throws UnsupportedQueryException {
    String id = entry.getUri();
    if (StringUtils.isNotEmpty(id)) {
      id = id.substring(id.lastIndexOf(':') + 1);
    }

    List<SyndContent> contents = entry.getContents();
    List<SyndCategory> categories = entry.getCategories();
    List<Metacard> metacards = new ArrayList<>();
    List<Element> foreignMarkup = entry.getForeignMarkup();
    String relevance = "";

    for (Element element : foreignMarkup) {
      if (element.getName().equals("score")) {
        relevance = element.getContent(0).getValue();
      }
      metacards.addAll(processAdditionalForeignMarkups(element, id));
    }
    // we currently do not support downloading content via an RSS enclosure, this support can be
    // added at a later date if we decide to include it
    for (SyndContent content : contents) {
      Metacard metacard = parseContent(content.getValue(), id);
      if (metacard != null) {
        metacard.setSourceId(this.shortname);
        String title = metacard.getTitle();
        if (StringUtils.isEmpty(title)) {
          metacard.setAttribute(new AttributeImpl(Core.TITLE, entry.getTitle()));
        }
        metacards.add(metacard);
      }
    }
    for (int i = 0; i < categories.size() && i < metacards.size(); i++) {
      SyndCategory category = categories.get(i);
      Metacard metacard = metacards.get(i);
      if (StringUtils.isBlank(metacard.getContentTypeName())) {
        metacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, category.getName()));
      }
    }

    List<Result> results = new ArrayList<>();
    for (Metacard metacard : metacards) {
      ResultImpl result = new ResultImpl(metacard);
      if (StringUtils.isEmpty(relevance)) {
        LOGGER.debug("Couldn't find valid relevance. Setting relevance to 0");
        relevance = "0";
      }
      result.setRelevanceScore(new Double(relevance));
      results.add(result);
    }

    return results;
  }

  @Nullable
  private Metacard parseContent(String content, String id) throws UnsupportedQueryException {
    if (StringUtils.isNotEmpty(content)) {
      InputTransformer inputTransformer =
          getInputTransformer(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
      if (inputTransformer != null) {
        try {
          return inputTransformer.transform(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), id);
        } catch (IOException e) {
          LOGGER.debug("Unable to read metacard content from Atom feed.", e);
        } catch (CatalogTransformerException e) {
          LOGGER.debug(
              "Unable to convert metacard content from Atom feed into Metacard object.", e);
        }
      }
    }
    return null;
  }

  /** Get the URL of the endpoint. */
  public String getEndpointUrl() {
    LOGGER.trace("getEndpointUrl:  endpointUrl = {}", endpointUrl);
    return endpointUrl.toString();
  }

  /**
   * Set URL of the endpoint.
   *
   * @param endpointUrl Full url of the endpoint.
   */
  public void setEndpointUrl(String endpointUrl) {
    this.endpointUrl = new PropertyResolver(endpointUrl);
    updateFactory();
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public String getOrganization() {
    return ORGANIZATION;
  }

  @Override
  public String getId() {
    return shortname;
  }

  /**
   * Sets the shortname for this site. This shortname is used to identify the site when performing
   * federated queries.
   *
   * @param shortname Name of this site.
   */
  public void setShortname(String shortname) {
    this.shortname = shortname;
  }

  @Override
  public String getTitle() {
    return TITLE;
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  private InputTransformer getInputTransformer(InputStream inputStream)
      throws UnsupportedQueryException {
    XMLStreamReader xmlStreamReader = null;
    try {
      xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
      while (xmlStreamReader.hasNext()) {
        int next = xmlStreamReader.next();
        if (next == XMLStreamConstants.START_ELEMENT) {
          String namespaceUri = xmlStreamReader.getNamespaceURI();
          InputTransformer transformerReference = lookupTransformerReference(namespaceUri);
          if (transformerReference != null) {
            return transformerReference;
          }
        }
      }
    } catch (XMLStreamException | InvalidSyntaxException e) {
      LOGGER.debug("Failed to parse transformer namespace", e);
    } finally {
      try {
        if (xmlStreamReader != null) {
          xmlStreamReader.close();
        }
      } catch (XMLStreamException e) {
        LOGGER.debug("Failed to close namespace reader", e);
      }
    }

    throw new UnsupportedQueryException(
        "Unable to find applicable InputTransformer for metacard content from Atom feed.");
  }

  @Nullable
  protected InputTransformer lookupTransformerReference(String namespaceUri)
      throws InvalidSyntaxException {
    LOGGER.trace("Looking up Input Transformer by schema : {}", namespaceUri);

    Bundle bundle = getBundle();
    if (bundle != null) {
      BundleContext bundleContext = bundle.getBundleContext();
      Collection<ServiceReference<InputTransformer>> transformerReferences =
          bundleContext.getServiceReferences(
              InputTransformer.class, "(schema=" + namespaceUri + ")");
      if (CollectionUtils.isNotEmpty(transformerReferences)) {
        ServiceReference<InputTransformer> transformer = transformerReferences.iterator().next();
        LOGGER.trace(
            "Found Input Transformer {} by schema {}",
            transformer.getBundle().getSymbolicName(),
            namespaceUri);
        return bundleContext.getService(transformer);
      }
      LOGGER.trace("Failed to find Input Transformer by schema : {}", namespaceUri);
    }
    return null;
  }

  @VisibleForTesting
  protected Bundle getBundle() {
    return FrameworkUtil.getBundle(this.getClass());
  }

  /**
   * Get the boolean flag that indicates only local queries are being executed by this OpenSearch
   * Source.
   *
   * @return true indicates only local queries, false indicates enterprise query
   */
  public boolean getLocalQueryOnly() {
    return localQueryOnly;
  }

  /**
   * Sets the boolean flag that indicates all queries executed should be to its local source only,
   * i.e., no federated or enterprise queries.
   *
   * @param localQueryOnly true indicates only local queries, false indicates enterprise query
   */
  public void setLocalQueryOnly(boolean localQueryOnly) {
    LOGGER.trace("Setting localQueryOnly = {}", localQueryOnly);
    this.localQueryOnly = localQueryOnly;
  }

  /**
   * Get the boolean flag that determines if point-radius and polygon geometries should be
   * converting to bounding boxes before sending.
   */
  public boolean getShouldConvertToBBox() {
    return shouldConvertToBBox;
  }

  /**
   * Sets the boolean flag that tells the code to convert point-radius and polygon geometries to a
   * bounding box before sending them.
   */
  public void setShouldConvertToBBox(boolean shouldConvertToBBox) {
    this.shouldConvertToBBox = shouldConvertToBBox;
  }

  /**
   * Get the number of vertices an approximation polygon will have when converting a multi
   * point-radius search to a multi-polygon search.
   */
  public int getNumMultiPointRadiusVertices() {
    return numMultiPointRadiusVertices;
  }

  /**
   * Sets the number of vertices to use when approximating a polygon to fit to a multi point-radius
   * search.
   */
  public void setNumMultiPointRadiusVertices(int numMultiPointRadiusVertices) {
    if (numMultiPointRadiusVertices < MIN_NUM_POINT_RADIUS_VERTICES) {
      this.numMultiPointRadiusVertices = MIN_NUM_POINT_RADIUS_VERTICES;
      LOGGER.debug(
          "Admin supplied max number of vertices is too low. Defaulting to the minimum number of {} vertices",
          MIN_NUM_POINT_RADIUS_VERTICES);
    } else if (numMultiPointRadiusVertices > 32) {
      this.numMultiPointRadiusVertices = MAX_NUM_POINT_RADIUS_VERTICES;
      LOGGER.debug(
          "Admin supplied max number of vertices is too high. Defaulting to the maximum number of {} vertices",
          MAX_NUM_POINT_RADIUS_VERTICES);
    } else {
      this.numMultiPointRadiusVertices = numMultiPointRadiusVertices;
    }
  }

  /** Get the distance tolerance value used for simplification of circular geometries. */
  public int getDistanceTolerance() {
    return distanceTolerance;
  }

  /** Sets the distance tolerance value used for simplification of circular geometries. */
  public void setDistanceTolerance(int distanceTolerance) {
    if (distanceTolerance < MIN_DISTANCE_TOLERANCE_IN_METERS) {
      this.distanceTolerance = distanceTolerance;
      LOGGER.debug(
          "Admin supplied distance tolerance is too low. Defaulting to the minimum of {} meter",
          MIN_DISTANCE_TOLERANCE_IN_METERS);
    } else {
      this.distanceTolerance = distanceTolerance;
    }
  }

  public void setPollInterval(Integer interval) {
    this.pollInterval = Math.max(1, interval);
    updateScheduler();
  }

  @Override
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> requestProperties)
      throws ResourceNotFoundException, ResourceNotSupportedException, IOException {

    final String methodName = "retrieveResource";
    LOGGER.trace("ENTRY: {}", methodName);

    if (requestProperties == null) {
      throw new ResourceNotFoundException("Could not retrieve resource with null properties.");
    }

    if (requestProperties.get(Metacard.RESOURCE_URI) != null) {
      URI resourceUri;
      try {
        resourceUri = new URI(requestProperties.get(Metacard.RESOURCE_URI).toString());
      } catch (URISyntaxException e) {
        throw new ResourceNotFoundException(
            "Could not retrieve resource with invalid OpenSearch endpoint URL.");
      }

      if (StringUtils.isNotBlank(username)) {
        requestProperties.put(USERNAME_PROPERTY, username);
        requestProperties.put(PASSWORD_PROPERTY, password);
      }

      return resourceReader.retrieveResource(resourceUri, requestProperties);
    }

    LOGGER.trace("EXIT: {}", methodName);
    throw new ResourceNotFoundException(COULD_NOT_RETRIEVE_RESOURCE_MESSAGE);
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getSupportedSchemes() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getOptions(Metacard metacard) {
    LOGGER.trace("ENTERING/EXITING: getOptions");
    LOGGER.debug("OpenSearch Source \"{}\" does not support resource retrieval options.", getId());
    return Collections.emptySet();
  }

  @Override
  public String getConfigurationPid() {
    return configurationPid;
  }

  @Override
  public void setConfigurationPid(String configurationPid) {
    this.configurationPid = configurationPid;
  }

  public List<String> getMarkUpSet() {
    return new ArrayList<>(markUpSet);
  }

  public void setMarkUpSet(List<String> markUpSet) {
    this.markUpSet = new HashSet<>(markUpSet);
  }

  public List<String> getParameters() {
    return parameters;
  }

  public void setParameters(List<String> parameters) {
    this.parameters = parameters;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
    updateFactory();
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
    updateFactory();
  }

  public Boolean getDisableCnCheck() {
    return disableCnCheck;
  }

  public void setDisableCnCheck(Boolean disableCnCheck) {
    this.disableCnCheck = disableCnCheck;
    updateFactory();
  }

  public Boolean getAllowRedirects() {
    return allowRedirects;
  }

  public void setAllowRedirects(Boolean allowRedirects) {
    this.allowRedirects = allowRedirects;
    updateFactory();
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    updateFactory();
  }

  public Integer getReceiveTimeout() {
    return receiveTimeout;
  }

  public void setReceiveTimeout(Integer receiveTimeout) {
    this.receiveTimeout = receiveTimeout;
    updateFactory();
  }

  public void setResourceReader(ResourceReader reader) {
    this.resourceReader = reader;
  }

  // For testing
  void setHttpClient(HttpClient client) {
    this.httpClient = client;
  }

  private List<Metacard> processAdditionalForeignMarkups(Element element, String id)
      throws UnsupportedQueryException {
    List<Metacard> metacards = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(markUpSet) && markUpSet.contains(element.getName())) {
      XMLOutputter xmlOutputter = new XMLOutputter();
      Metacard metacard = parseContent(xmlOutputter.outputString(element), id);
      if (metacard != null) {
        metacards.add(metacard);
      }
    }
    return metacards;
  }

  protected static class SpatialSearch {

    private final Geometry geometry;
    private final BoundingBox boundingBox;
    private final Polygon polygon;
    private final PointRadius pointRadius;

    public SpatialSearch(
        @Nullable Geometry geometry,
        @Nullable BoundingBox boundingBox,
        @Nullable Polygon polygon,
        @Nullable PointRadius pointRadius) {

      if (geometry == null && boundingBox == null && polygon == null && pointRadius == null) {
        throw new IllegalArgumentException(
            "All spatial criteria are null. Unable to create a spatial search");
      }

      this.geometry = geometry;
      this.boundingBox = boundingBox;
      this.polygon = polygon;
      this.pointRadius = pointRadius;
    }

    @Nullable
    public Geometry getGeometry() {
      return geometry;
    }

    @Nullable
    public BoundingBox getBoundingBox() {
      return boundingBox;
    }

    @Nullable
    public Polygon getPolygon() {
      return polygon;
    }

    @Nullable
    public PointRadius getPointRadius() {
      return pointRadius;
    }
  }

  /**
   * Method to combine spatial searches into either geometry collection or a bounding box.
   * OpenSearch endpoints and the query framework allow for multiple spatial query parameters. This
   * method has been refactored out and is protected so that downstream projects may try to
   * implement another algorithm (e.g. best-effort) to combine searches.
   *
   * @return null if there is no search specified, or a {@linkSpatialSearch} with one search that is
   *     the combination of all of the spatial criteria
   */
  @Nullable
  protected SpatialSearch createCombinedSpatialSearch(
      final Queue<PointRadius> pointRadiusSearches,
      final Queue<Geometry> geometrySearches,
      final int numMultiPointRadiusVertices,
      final int distanceTolerance) {
    Geometry geometrySearch = null;
    BoundingBox boundingBox = null;
    PointRadius pointRadius = null;
    SpatialSearch spatialSearch = null;

    Set<Geometry> combinedGeometrySearches = new HashSet<>(geometrySearches);

    if (CollectionUtils.isNotEmpty(pointRadiusSearches)) {
      if (shouldConvertToBBox) {
        for (PointRadius search : pointRadiusSearches) {
          BoundingBox bbox = BoundingBoxUtils.createBoundingBox(search);
          List<List<Double>> bboxCoordinate = BoundingBoxUtils.getBoundingBoxCoordinatesList(bbox);
          List<List<List<Double>>> coordinates = new ArrayList<>();
          coordinates.add(bboxCoordinate);
          combinedGeometrySearches.add(ddf.geo.formatter.Polygon.buildPolygon(coordinates));
          LOGGER.trace(
              "Point radius searches are converted to a (rough approximation) square using Vincenty's formula (direct)");
        }
      } else {
        if (pointRadiusSearches.size() == 1) {
          pointRadius = pointRadiusSearches.remove();
        } else {
          for (PointRadius search : pointRadiusSearches) {
            Geometry circle =
                GeospatialUtil.createCirclePolygon(
                    search.getLat(),
                    search.getLon(),
                    search.getRadius(),
                    numMultiPointRadiusVertices,
                    distanceTolerance);
            combinedGeometrySearches.add(circle);
            LOGGER.trace(
                "Point radius searches are converted to a polygon with a max of {} vertices.",
                numMultiPointRadiusVertices);
          }
        }
      }
    }

    if (CollectionUtils.isNotEmpty(combinedGeometrySearches)) {
      // if there is more than one geometry, create a geometry collection
      if (combinedGeometrySearches.size() > 1) {
        geometrySearch =
            GEOMETRY_FACTORY.createGeometryCollection(
                combinedGeometrySearches.toArray(new Geometry[0]));
      } else {
        geometrySearch = combinedGeometrySearches.iterator().next();
      }

      /**
       * If convert to bounding box is enabled, extracts the approximate envelope. In the case of
       * multiple geometry, a large approximate envelope encompassing all of the geometry is
       * returned. Area between the geometries are also included in this spatial search. Hence widen
       * the search area.
       */
      if (shouldConvertToBBox) {
        if (combinedGeometrySearches.size() > 1) {
          LOGGER.trace(
              "An approximate envelope encompassing all the geometry is returned. Area between the geometries are also included in this spatial search. Hence widen the search area.");
        }
        boundingBox = BoundingBoxUtils.createBoundingBox((Polygon) geometrySearch.getEnvelope());
        geometrySearch = null;
      }
    }

    if (geometrySearch != null || boundingBox != null || pointRadius != null) {
      // Geo Draft 2 default always geometry instead of polygon
      spatialSearch = new SpatialSearch(geometrySearch, boundingBox, null, pointRadius);
    }
    return spatialSearch;
  }
}
