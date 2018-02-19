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
package ddf.catalog.source.opensearch.impl;

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
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
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
import ddf.catalog.source.opensearch.OpenSearchParser;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.endpoints.OpenSearch;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.geotools.filter.FilterTransformer;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
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

  static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
  static final String BYTES = "bytes";
  private static final String COULD_NOT_RETRIEVE_RESOURCE_MESSAGE = "Could not retrieve resource";
  private static final String ORGANIZATION = "DDF";

  private static final String TITLE = "OpenSearch DDF Federated Source";

  private static final String DESCRIPTION =
      "Queries DDF using the synchronous federated OpenSearch query";

  private static final long AVAILABLE_TIMEOUT_CHECK = 60000; // 60 seconds, in milliseconds

  private static final String URL_SRC_PARAMETER = "src";

  private static final String LOCAL_SEARCH_PARAMETER = "local";

  private static final String USERNAME_PROPERTY = "username";

  @SuppressWarnings("squid:S2068" /*Key for the requestProperties map, not a hardcoded password*/)
  private static final String PASSWORD_PROPERTY = "password";

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSource.class);

  private final EncryptionService encryptionService;

  protected SecureCxfClientFactory<OpenSearch> factory;

  private boolean isInitialized = false;

  // service properties
  private String shortname;

  private boolean lastAvailable;

  private Date lastAvailableDate = null;

  private boolean localQueryOnly;

  private boolean shouldConvertToBBox;

  private PropertyResolver endpointUrl;

  private FilterAdapter filterAdapter;

  private String configurationPid;

  private List<String> parameters;

  private Set<String> markUpSet;

  private String username;

  private String password;

  private XMLInputFactory xmlInputFactory;

  private ResourceReader resourceReader;

  private OpenSearchParser openSearchParser;

  private OpenSearchFilterVisitor openSearchFilterVisitor;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private boolean disableCnCheck = false;

  private boolean allowRedirects = false;

  private BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer;

  /**
   * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
   * overwritten using the setter methods.
   */
  public OpenSearchSource(
      FilterAdapter filterAdapter,
      OpenSearchParser openSearchParser,
      OpenSearchFilterVisitor openSearchFilterVisitor,
      EncryptionService encryptionService) {
    this(
        filterAdapter,
        openSearchParser,
        openSearchFilterVisitor,
        encryptionService,
        (elements, sourceResponse) -> {});
  }

  /**
   * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
   * overwritten using the setter methods.
   */
  public OpenSearchSource(
      FilterAdapter filterAdapter,
      OpenSearchParser openSearchParser,
      OpenSearchFilterVisitor openSearchFilterVisitor,
      EncryptionService encryptionService,
      BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer) {
    this.filterAdapter = filterAdapter;
    this.encryptionService = encryptionService;
    this.openSearchParser = openSearchParser;
    this.openSearchFilterVisitor = openSearchFilterVisitor;
    this.foreignMarkupBiConsumer = foreignMarkupBiConsumer;
  }

  /**
   * Called when this OpenSearch Source is created, but after all of the setter methods have been
   * called for each property specified in the metatype.xml file.
   */
  public void init() {
    factory = createClientFactory(endpointUrl.getResolvedString(), username, password);
    configureXmlInputFactory();
    isInitialized = true;
  }

  protected SecureCxfClientFactory<OpenSearch> createClientFactory(
      String url, String username, String password) {
    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
      return new SecureCxfClientFactory<>(
          url,
          OpenSearch.class,
          null,
          null,
          disableCnCheck,
          allowRedirects,
          connectionTimeout,
          receiveTimeout,
          username,
          password);
    } else {
      return new SecureCxfClientFactory<>(
          url,
          OpenSearch.class,
          null,
          null,
          disableCnCheck,
          allowRedirects,
          connectionTimeout,
          receiveTimeout);
    }
  }

  private void configureXmlInputFactory() {
    xmlInputFactory = XMLInputFactory2.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlInputFactory.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
  }

  public void destroy(int code) {
    LOGGER.debug("Nothing to destroy.");
  }

  @Override
  public boolean isAvailable() {
    boolean isAvailable = false;
    if (!lastAvailable
        || (lastAvailableDate.before(
            new Date(System.currentTimeMillis() - AVAILABLE_TIMEOUT_CHECK)))) {

      WebClient client;
      Response response;

      try {
        client = factory.getWebClient();
        response = client.head();
      } catch (Exception e) {
        LOGGER.debug("Web Client was unable to connect to endpoint.", e);
        return false;
      }

      if (response != null && !(response.getStatus() >= 404 || response.getStatus() == 402)) {
        isAvailable = true;
        lastAvailableDate = new Date();
      }
    } else {
      isAvailable = lastAvailable;
    }
    lastAvailable = isAvailable;
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

    Serializable metacardId = queryRequest.getPropertyValue(Metacard.ID);
    SourceResponseImpl response = null;

    Subject subject = null;
    WebClient restWebClient;
    if (queryRequest.hasProperties()) {
      Object subjectObj = queryRequest.getProperties().get(SecurityConstants.SECURITY_SUBJECT);
      subject = (Subject) subjectObj;
    }
    restWebClient = factory.getWebClientForSubject(subject);

    Query query = queryRequest.getQuery();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Received query: {}", query);
    }

    boolean canDoOpenSearch = setOpenSearchParameters(queryRequest, subject, restWebClient);

    if (canDoOpenSearch) {

      InputStream responseStream = performRequest(restWebClient);

      response = new SourceResponseImpl(queryRequest, new ArrayList<>());

      if (responseStream != null) {
        response = processResponse(responseStream, queryRequest);
      }
    } else {
      if (StringUtils.isEmpty((String) metacardId)) {
        OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
            (OpenSearchFilterVisitorObject)
                query.accept(openSearchFilterVisitor, new OpenSearchFilterVisitorObject());
        metacardId = openSearchFilterVisitorObject.getId();
      }
      restWebClient = newRestClient(query, (String) metacardId, false, subject);

      if (restWebClient != null) {

        InputStream responseStream = performRequest(restWebClient);

        Metacard metacard = null;
        List<Result> resultQueue = new ArrayList<>();
        try (TemporaryFileBackedOutputStream fileBackedOutputStream =
            new TemporaryFileBackedOutputStream()) {
          if (responseStream != null) {
            IOUtils.copyLarge(responseStream, fileBackedOutputStream);
            InputTransformer inputTransformer = null;
            try (InputStream inputStream = fileBackedOutputStream.asByteSource().openStream()) {
              inputTransformer = getInputTransformer(inputStream);
            } catch (IOException e) {
              LOGGER.debug("Problem with transformation.", e);
            }
            if (inputTransformer != null) {
              try (InputStream inputStream = fileBackedOutputStream.asByteSource().openStream()) {
                metacard = inputTransformer.transform(inputStream);
              } catch (IOException e) {
                LOGGER.debug("Problem with transformation.", e);
              }
            }
          }
        } catch (IOException | CatalogTransformerException e) {
          LOGGER.debug("Problem with transformation.", e);
        }
        if (metacard != null) {
          metacard.setSourceId(getId());
          ResultImpl result = new ResultImpl(metacard);
          resultQueue.add(result);
          response = new SourceResponseImpl(queryRequest, resultQueue);
          response.setHits(resultQueue.size());
        }
      }
    }

    setSourceId(response);

    LOGGER.trace(methodName);

    return response;
  }

  /** Set the source-id on every metacard this is missing a source-id. */
  private void setSourceId(SourceResponse sourceResponse) {
    if (sourceResponse != null && sourceResponse.getResults() != null) {
      sourceResponse
          .getResults()
          .stream()
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
   * @param client Client to perform the GET request on.
   * @return The entity of the response as an InputStream.
   * @throws UnsupportedQueryException
   */
  private InputStream performRequest(WebClient client) throws UnsupportedQueryException {
    Response clientResponse = client.get();

    InputStream stream = null;
    Object entityObj = clientResponse.getEntity();
    if (entityObj != null) {
      stream = (InputStream) entityObj;
    }
    if (Response.Status.OK.getStatusCode() != clientResponse.getStatus()) {
      String error = "";
      try {
        if (stream != null) {
          error = IOUtils.toString(stream);
        }
      } catch (IOException ioe) {
        LOGGER.debug("Could not convert error message to a string for output.", ioe);
      }
      String errorMsg =
          "Received error code from remote source (status "
              + clientResponse.getStatus()
              + "): "
              + error;
      throw new UnsupportedQueryException(errorMsg);
    }

    return stream;
  }

  /** Package-private so that tests may set the foreign markup consumer. */
  void setForeignMarkupBiConsumer(
      BiConsumer<List<Element>, SourceResponse> foreignMarkupBiConsumer) {
    this.foreignMarkupBiConsumer = foreignMarkupBiConsumer;
  }

  // Refactored from query() and made protected so JUnit tests could be written for this logic
  protected boolean setOpenSearchParameters(
      QueryRequest queryRequest, Subject subject, WebClient client) {
    Query query = queryRequest.getQuery();
    if (LOGGER.isDebugEnabled()) {
      FilterTransformer transform = new FilterTransformer();
      transform.setIndentation(2);
      try {
        LOGGER.debug(transform.transform(query));
      } catch (TransformerException e) {
        LOGGER.debug("Error transforming query to XML", e);
      }
    }

    OpenSearchFilterVisitorObject openSearchFilterVisitorObject =
        (OpenSearchFilterVisitorObject)
            query.accept(openSearchFilterVisitor, new OpenSearchFilterVisitorObject());

    ContextualSearch contextualFilter = openSearchFilterVisitorObject.getContextualSearch();

    // TODO fix this so we aren't just triggering off of a contextual query
    if (contextualFilter != null && MapUtils.isNotEmpty(contextualFilter.getSearchPhraseMap())) {
      // All queries must have at least a search phrase to be valid, hence this check
      // for a contextual filter with a non-empty search phrase
      openSearchParser.populateSearchOptions(client, queryRequest, subject, parameters);
      openSearchParser.populateContextual(
          client, contextualFilter.getSearchPhraseMap(), parameters);

      applyFilters(openSearchFilterVisitorObject, client);
      return true;

      // ensure that there is no search phrase - we will add our own
    } else if ((openSearchFilterVisitorObject.getSpatialSearch() != null
            && contextualFilter != null
            && MapUtils.isEmpty(contextualFilter.getSearchPhraseMap()))
        || (openSearchFilterVisitorObject.getSpatialSearch() != null && contextualFilter == null)) {

      openSearchParser.populateSearchOptions(client, queryRequest, subject, parameters);

      Map<String, String> searchPhraseMap;
      if (contextualFilter == null) {
        searchPhraseMap = new HashMap<>();
      } else {
        searchPhraseMap = contextualFilter.getSearchPhraseMap();
      }
      searchPhraseMap.put(OpenSearchParserImpl.SEARCH_TERMS, "*");

      // add a wildcard search term - this query came in with no search phrase and a search phrase
      // is necessary
      openSearchParser.populateContextual(client, searchPhraseMap, parameters);

      applyFilters(openSearchFilterVisitorObject, client);
      return true;
    }
    return false;
  }

  /**
   * @param is
   * @param queryRequest
   * @return
   * @throws ddf.catalog.source.UnsupportedQueryException
   */
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
   * @throws ddf.catalog.source.UnsupportedQueryException
   */
  private List<Result> createResponseFromEntry(SyndEntry entry) throws UnsupportedQueryException {
    String id = entry.getUri();
    if (id != null && !id.isEmpty()) {
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
      if (relevance == null || relevance.isEmpty()) {
        LOGGER.debug("couldn't find valid relevance. Setting relevance to 0");
        relevance = "0";
      }
      result.setRelevanceScore(new Double(relevance));
      results.add(result);
    }

    return results;
  }

  private Metacard parseContent(String content, String id) {
    if (content != null) {
      InputTransformer inputTransformer =
          getInputTransformer(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
      if (inputTransformer != null && !content.isEmpty()) {
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

  /**
   * Get the URL of the endpoint.
   *
   * @return
   */
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
    if (isInitialized) {
      factory = createClientFactory(endpointUrl, username, password);
    }
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

  private InputTransformer getInputTransformer(InputStream inputStream) {
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
        LOGGER.debug("failed to close namespace reader", e);
      }
    }
    return null;
  }

  protected InputTransformer lookupTransformerReference(String namespaceUri)
      throws InvalidSyntaxException {
    LOGGER.trace("Looking up Input Transformer by schema : {}", namespaceUri);

    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    if (bundle != null) {
      BundleContext bundleContext = bundle.getBundleContext();
      Collection<ServiceReference<InputTransformer>> transformerReference =
          bundleContext.getServiceReferences(
              InputTransformer.class, "(schema=" + namespaceUri + ")");
      if (transformerReference.isEmpty()) {
        LOGGER.trace("Failed to find Input Transformer by schema : {}", namespaceUri);
      } else {
        LOGGER.trace(
            "Found Input Transformer {} by schema {}",
            transformerReference.iterator().next().getBundle().getSymbolicName(),
            namespaceUri);
        return bundleContext.getService(transformerReference.iterator().next());
      }
    }
    return null;
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
   *
   * @return
   */
  public boolean getShouldConvertToBBox() {
    return shouldConvertToBBox;
  }

  /**
   * Sets the boolean flag that tells the code to convert point-radius and polygon geometries to a
   * bounding box before sending them.
   *
   * @param shouldConvertToBBox
   */
  public void setShouldConvertToBBox(boolean shouldConvertToBBox) {
    this.shouldConvertToBBox = shouldConvertToBBox;
  }

  @Override
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> requestProperties)
      throws ResourceNotFoundException, ResourceNotSupportedException, IOException {

    final String methodName = "retrieveResource";
    LOGGER.trace("ENTRY: {}", methodName);

    if (requestProperties == null) {
      throw new ResourceNotFoundException("Could not retrieve resource with null properties.");
    }

    Serializable serializableId = requestProperties.get(Metacard.ID);

    if (serializableId != null) {
      String metacardId = serializableId.toString();
      WebClient restClient = newRestClient(null, metacardId, true, null);
      if (StringUtils.isNotBlank(username)) {
        requestProperties.put(USERNAME_PROPERTY, username);
        requestProperties.put(PASSWORD_PROPERTY, password);
      }
      return resourceReader.retrieveResource(restClient.getCurrentURI(), requestProperties);
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
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = encryptionService.decryptValue(password);
  }

  public Boolean getDisableCnCheck() {
    return disableCnCheck;
  }

  public void setDisableCnCheck(Boolean disableCnCheck) {
    this.disableCnCheck = disableCnCheck;
  }

  public Boolean getAllowRedirects() {
    return allowRedirects;
  }

  public void setAllowRedirects(Boolean allowRedirects) {
    this.allowRedirects = allowRedirects;
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public Integer getReceiveTimeout() {
    return receiveTimeout;
  }

  public void setReceiveTimeout(Integer receiveTimeout) {
    this.receiveTimeout = receiveTimeout;
  }

  private WebClient newRestClient(
      Query query, String metacardId, boolean retrieveResource, Subject subj) {
    String url = endpointUrl.getResolvedString();
    if (query != null) {
      url = createRestUrl(query, url, retrieveResource);
    } else {
      RestUrl restUrl = newRestUrl(url);

      if (restUrl != null) {
        if (StringUtils.isNotEmpty(metacardId)) {
          restUrl.setId(metacardId);
        }
        restUrl.setRetrieveResource(retrieveResource);
        url = restUrl.buildUrl();
      }
    }
    return newOpenSearchClient(url, subj);
  }

  private String createRestUrl(Query query, String endpointUrl, boolean retrieveResource) {

    String url = null;
    RestFilterDelegate delegate = null;
    RestUrl restUrl = newRestUrl(endpointUrl);
    if (restUrl != null) {
      restUrl.setRetrieveResource(retrieveResource);
      delegate = new RestFilterDelegate(restUrl);
    }

    if (delegate != null) {
      try {
        filterAdapter.adapt(query, delegate);
        url = delegate.getRestUrl().buildUrl();
      } catch (UnsupportedQueryException e) {
        LOGGER.debug("Not a REST request.", e);
      }
    }
    return url;
  }

  public void setResourceReader(ResourceReader reader) {
    this.resourceReader = reader;
  }

  /**
   * Creates a new RestUrl object based on an OpenSearch URL
   *
   * @param url
   * @return RestUrl object for a DDF REST endpoint
   */
  private RestUrl newRestUrl(String url) {
    RestUrl restUrl = null;
    try {
      restUrl = RestUrl.newInstance(url);
      restUrl.setRetrieveResource(true);
    } catch (MalformedURLException | URISyntaxException e) {
      LOGGER.debug("Bad url given for remote source", e);
    }
    return restUrl;
  }

  /**
   * Creates a new webClient based off a url and, if BasicAuth is not used, a Security Subject
   *
   * @param url - the endpoint url
   * @param subj - the Security Subject, if applicable
   * @return A webclient for the endpoint URL either using BasicAuth, using the Security Subject, or
   *     an insecure client.
   */
  private WebClient newOpenSearchClient(String url, Subject subj) {
    SecureCxfClientFactory<OpenSearch> clientFactory = createClientFactory(url, username, password);
    return clientFactory.getWebClientForSubject(subj);
  }

  private void applyFilters(OpenSearchFilterVisitorObject visitor, WebClient client) {

    TemporalFilter temporalFilter = visitor.getTemporalSearch();
    if (temporalFilter != null) {
      LOGGER.debug("startDate = {}", temporalFilter.getStartDate());
      LOGGER.debug("endDate = {}", temporalFilter.getEndDate());

      openSearchParser.populateTemporal(client, temporalFilter, parameters);
    }

    SpatialFilter spatialFilter = visitor.getSpatialSearch();
    if (spatialFilter != null) {
      if (spatialFilter instanceof SpatialDistanceFilter) {
        try {
          openSearchParser.populateGeospatial(
              client, (SpatialDistanceFilter) spatialFilter, shouldConvertToBBox, parameters);
        } catch (UnsupportedQueryException e) {
          LOGGER.debug("Problem with populating geospatial criteria. ", e);
        }
      } else {
        try {
          openSearchParser.populateGeospatial(
              client, spatialFilter, shouldConvertToBBox, parameters);
        } catch (UnsupportedQueryException e) {
          LOGGER.debug("Problem with populating geospatial criteria. ", e);
        }
      }
    }

    if (localQueryOnly) {
      client.replaceQueryParam(URL_SRC_PARAMETER, LOCAL_SEARCH_PARAMETER);
    } else {
      client.replaceQueryParam(URL_SRC_PARAMETER, "");
    }
  }

  private List<Metacard> processAdditionalForeignMarkups(Element element, String id) {
    List<Metacard> metacards = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(markUpSet) && markUpSet.contains(element.getName())) {
      XMLOutputter xmlOutputter = new XMLOutputter();
      Metacard metacard = parseContent(xmlOutputter.outputString(element), id);
      metacards.add(metacard);
    }
    return metacards;
  }
}
