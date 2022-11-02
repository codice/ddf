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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.OAuthFederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import ddf.catalog.util.impl.MaskableImpl;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.AcceptVersionsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractCswSource provides a DDF {@link OAuthFederatedSource} and {@link ConnectedSource} for CSW
 * 2.0.2 services.
 */
public abstract class AbstractCswSource extends MaskableImpl
    implements FederatedSource, ConnectedSource, ConfiguredService {

  public static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

  @SuppressWarnings("squid:S2068")
  protected static final String PASSWORD_PROPERTY = "password";

  protected static final String CSWURL_PROPERTY = "cswUrl";
  protected static final String ID_PROPERTY = "id";
  protected static final String AUTHENTICATION_TYPE = "authenticationType";
  protected static final String USERNAME_PROPERTY = "username";
  protected static final String METACARD_MAPPINGS_PROPERTY = "metacardMappings";
  protected static final String COORDINATE_ORDER_PROPERTY = "coordinateOrder";
  protected static final String POLL_INTERVAL_PROPERTY = "pollInterval";
  protected static final String OUTPUT_SCHEMA_PROPERTY = "outputSchema";
  protected static final String IS_CQL_FORCED_PROPERTY = "isCqlForced";
  protected static final String FORCE_SPATIAL_FILTER_PROPERTY = "forceSpatialFilter";
  protected static final String NO_FORCE_SPATIAL_FILTER = "NO_FILTER";
  protected static final String CONNECTION_TIMEOUT_PROPERTY = "connectionTimeout";
  protected static final String RECEIVE_TIMEOUT_PROPERTY = "receiveTimeout";
  protected static final String QUERY_TYPE_NAME_PROPERTY = "queryTypeName";
  protected static final String QUERY_TYPE_NAMESPACE_PROPERTY = "queryTypeNamespace";
  protected static final String USE_POS_LIST_PROPERTY = "usePosList";
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCswSource.class);
  private static final String DEFAULT_CSW_TRANSFORMER_ID = "csw";
  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  @SuppressWarnings("squid:S1845")
  private static final String DESCRIPTION = "description";

  private static final String ORGANIZATION = "organization";
  private static final String VERSION = "version";
  private static final String TITLE = "name";
  private static final int CONTENT_TYPE_SAMPLE_SIZE = 50;

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String APPLICATION_XML = "application/xml";
  private static final String ERROR_ID_PRODUCT_RETRIEVAL = "Error retrieving resource for ID: %s";
  private static Properties describableProperties = new Properties();
  private static Map<String, Consumer<Object>> consumerMap = new HashMap<>();

  static {
    try (InputStream properties =
        AbstractCswSource.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(properties);
    } catch (IOException e) {
      LOGGER.info("Failed to load properties", e);
    }
  }

  protected String configurationPid;
  protected CswSourceConfiguration cswSourceConfiguration;
  protected CswFilterDelegate cswFilterDelegate;
  protected Converter cswTransformConverter;
  protected String forceSpatialFilter = NO_FORCE_SPATIAL_FILTER;
  protected ScheduledFuture<?> availabilityPollFuture;
  protected FilterBuilder filterBuilder;
  protected FilterAdapter filterAdapter;
  protected CapabilitiesType capabilities;
  protected CswClient cswClient;
  protected CswXmlParser parser = new CswXmlParser(new XmlParser());
  //  protected CswXmlParser parser =
  //      new CswXmlParser(
  //          new XmlParser(),
  //          List.of("ddf.catalog.transformer.xml.binding",
  // "ddf.catalog.transformer.xml.adapter"));
  protected QueryFilterTransformerProvider cswQueryFilterTransformerProvider;
  private Set<SourceMonitor> sourceMonitors = new HashSet<>();
  private Map<String, ContentType> contentTypes = new ConcurrentHashMap<>();
  private ResourceReader resourceReader;
  private DomainType supportedOutputSchemas;
  private Set<ElementSetType> detailLevels;
  private BundleContext context;
  private ObjectFactory objectFactory = new ObjectFactory();

  @SuppressWarnings("squid:S1845")
  private String description = null;

  private ScheduledExecutorService scheduler;
  private AvailabilityTask availabilityTask;
  private boolean isConstraintCql;

  /** Instantiates a CswSource. This constructor is for unit tests */
  public AbstractCswSource(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      CswXmlParser parser,
      Converter provider,
      CswClient cswClient) {
    this.context = context;
    this.cswSourceConfiguration = cswSourceConfiguration;
    this.parser = parser;
    this.cswTransformConverter = provider;
    this.cswClient = cswClient;
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("abstractCswSourceThread"));
    setConsumerMap();
  }

  /** Instantiates a CswSource. */
  public AbstractCswSource() {
    cswSourceConfiguration = new CswSourceConfiguration();
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("abstractCswSourceThread"));
  }

  /** Initializes the CswSource by connecting to the Server */
  public void init() {
    setConsumerMap();
    LOGGER.debug("{}: Entering init()", cswSourceConfiguration.getId());
    cswClient = createCswClient();
    setupAvailabilityPoll();
  }

  protected CswClient createCswClient() {
    return new CswClient(parser, objectFactory, cswTransformConverter, cswSourceConfiguration);
  }

  /** Sets the consumerMap to manipulate cswSourceConfiguration when refresh is called. */
  private void setConsumerMap() {

    consumerMap.put(ID_PROPERTY, value -> setId((String) value));

    consumerMap.put(
        AUTHENTICATION_TYPE, value -> cswSourceConfiguration.setAuthenticationType((String) value));

    consumerMap.put(PASSWORD_PROPERTY, value -> cswSourceConfiguration.setPassword((String) value));

    consumerMap.put(USERNAME_PROPERTY, value -> cswSourceConfiguration.setUsername((String) value));

    consumerMap.put(
        CONNECTION_TIMEOUT_PROPERTY,
        value -> cswSourceConfiguration.setConnectionTimeout((Integer) value));

    consumerMap.put(
        RECEIVE_TIMEOUT_PROPERTY,
        value -> cswSourceConfiguration.setReceiveTimeout((Integer) value));

    consumerMap.put(
        OUTPUT_SCHEMA_PROPERTY, value -> setConsumerOutputSchemaProperty((String) value));

    consumerMap.put(
        QUERY_TYPE_NAME_PROPERTY, value -> cswSourceConfiguration.setQueryTypeName((String) value));

    consumerMap.put(
        QUERY_TYPE_NAMESPACE_PROPERTY,
        value -> cswSourceConfiguration.setQueryTypeNamespace((String) value));

    consumerMap.put(METACARD_MAPPINGS_PROPERTY, value -> setMetacardMappings((String[]) value));

    consumerMap.put(
        DISABLE_CN_CHECK_PROPERTY,
        value -> cswSourceConfiguration.setDisableCnCheck((Boolean) value));

    consumerMap.put(COORDINATE_ORDER_PROPERTY, value -> setCoordinateOrder((String) value));

    consumerMap.put(
        USE_POS_LIST_PROPERTY, value -> cswSourceConfiguration.setUsePosList((Boolean) value));

    consumerMap.put(POLL_INTERVAL_PROPERTY, value -> setConsumerPollInterval((Integer) value));

    consumerMap.put(CSWURL_PROPERTY, value -> setConsumerUrlProp((String) value));

    consumerMap.put(
        IS_CQL_FORCED_PROPERTY, value -> cswSourceConfiguration.setIsCqlForced((Boolean) value));

    consumerMap.putAll(getAdditionalConsumers());
  }

  protected abstract Map<String, Consumer<Object>> getAdditionalConsumers();

  /** Consumer function that sets the cswSourceConfiguration OutputSchema if it is changed. */
  private void setConsumerOutputSchemaProperty(String newSchemaProp) {
    String oldOutputSchema = cswSourceConfiguration.getOutputSchema();
    cswSourceConfiguration.setOutputSchema(newSchemaProp);

    LOGGER.debug(
        "{}: new output schema: {}",
        cswSourceConfiguration.getId(),
        cswSourceConfiguration.getOutputSchema());
    LOGGER.debug("{}: old output schema: {}", cswSourceConfiguration.getId(), oldOutputSchema);
  }

  /** Consumer function that sets the cswSourceConfiguration PollInterval if it is changed. */
  private void setConsumerPollInterval(Integer newPollInterval) {
    if (!newPollInterval.equals(cswSourceConfiguration.getPollIntervalMinutes())) {
      LOGGER.debug("Poll Interval was changed for source {}.", cswSourceConfiguration.getId());
      cswSourceConfiguration.setPollIntervalMinutes(newPollInterval);

      forcePoll();
    }
  }

  /** Consumer function that sets the cswSourceConfiguration CswUrl if it is changed. */
  private void setConsumerUrlProp(String newCswUrlProp) {
    String newCswUrl = PropertyResolver.resolveProperties(newCswUrlProp);
    if (!newCswUrl.equals(cswSourceConfiguration.getCswUrl())) {
      cswSourceConfiguration.setCswUrl(newCswUrl);
      LOGGER.debug("Setting url : {}.", newCswUrl);
    }
  }

  private void forcePoll() {
    if (availabilityPollFuture != null) {
      availabilityPollFuture.cancel(true);
    }
    setupAvailabilityPoll();
  }

  /**
   * Reinitializes the CswSource when there is a configuration change. Otherwise, it checks with the
   * server to see if any capabilities have changed.
   *
   * @param configuration The configuration with which to connect to the server
   */
  public void refresh(Map<String, Object> configuration) {
    LOGGER.debug("{}: Entering refresh()", cswSourceConfiguration.getId());
    if (configuration == null || configuration.isEmpty()) {
      LOGGER.info(
          "Received null or empty configuration during refresh for {}: {}",
          this.getClass().getSimpleName(),
          cswSourceConfiguration.getId());
      return;
    }

    // Set Blank Defaults
    String spatialFilter = (String) configuration.get(FORCE_SPATIAL_FILTER_PROPERTY);
    if (StringUtils.isBlank(spatialFilter)) {
      spatialFilter = NO_FORCE_SPATIAL_FILTER;
    }
    setForceSpatialFilter(spatialFilter);

    String currentContentTypeMapping = (String) configuration.get(Metacard.CONTENT_TYPE);
    if (StringUtils.isBlank(currentContentTypeMapping)) {
      cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, CswConstants.CSW_TYPE);
    }

    // Filter Configuration Map
    Map<String, Object> filteredConfiguration = filter(configuration);

    // Run Consumers from Filtered Configuration Map
    for (Map.Entry<String, Object> entry : filteredConfiguration.entrySet()) {
      String key = entry.getKey();
      Consumer consumer = consumerMap.get(key);
      if (consumer != null) {
        LOGGER.debug("Refreshing Configuration : {} with : {}", key, entry.getValue());
        consumer.accept(entry.getValue());
      }
    }

    configureCswSource();

    cswClient = createCswClient();

    forcePoll();
  }

  private Map<String, Object> filter(Map<String, Object> configuration) {
    Map<String, Object> filteredConfiguration = new HashMap<>();

    // Filter out Blank Strings and null Integers and Booleans
    filteredConfiguration.putAll(
        configuration.entrySet().stream()
            .filter(entry -> (entry.getValue() != null))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return filteredConfiguration;
  }

  protected AvailabilityCommand getAvailabilityCommand() {
    return new CswSourceAvailabilityCommand();
  }

  protected void setupAvailabilityPoll() {
    LOGGER.debug(
        "Setting Availability poll task for {} minute(s) on Source {}",
        cswSourceConfiguration.getPollIntervalMinutes(),
        cswSourceConfiguration.getId());
    long interval = TimeUnit.MINUTES.toMillis(cswSourceConfiguration.getPollIntervalMinutes());
    if (availabilityPollFuture == null || availabilityPollFuture.isCancelled()) {
      if (availabilityTask == null) {
        availabilityTask =
            new AvailabilityTask(
                interval, getAvailabilityCommand(), cswSourceConfiguration.getId());
      } else {
        // Any run of the polling operation should trigger the task to run
        availabilityTask.updateLastAvailableTimestamp(0L);
        availabilityTask.setInterval(interval);
      }

      // Run the availability check immediately prior to scheduling it in a thread.
      // This is necessary to allow the catalog framework to have the correct
      // availability when the source is bound
      availabilityTask.run();
      // Schedule the availability check every 1 second. The actual call to
      // the remote server will only occur if the pollInterval has
      // elapsed.
      availabilityPollFuture =
          scheduler.scheduleWithFixedDelay(
              availabilityTask,
              AvailabilityTask.NO_DELAY,
              AvailabilityTask.ONE_SECOND,
              TimeUnit.SECONDS);
    } else {
      LOGGER.debug("No changes being made on the poller.");
    }
  }

  public Integer getConnectionTimeout() {
    return this.cswSourceConfiguration.getConnectionTimeout();
  }

  public void setConnectionTimeout(Integer timeout) {
    this.cswSourceConfiguration.setConnectionTimeout(timeout);
  }

  public Integer getReceiveTimeout() {
    return this.cswSourceConfiguration.getReceiveTimeout();
  }

  public void setReceiveTimeout(Integer timeout) {
    this.cswSourceConfiguration.setReceiveTimeout(timeout);
  }

  public void setContext(BundleContext context) {
    this.context = context;
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return new HashSet<>(contentTypes.values());
  }

  public ResourceReader getResourceReader() {
    return resourceReader;
  }

  public void setResourceReader(ResourceReader resourceReader) {
    this.resourceReader = resourceReader;
  }

  public void setOutputSchema(String outputSchema) {
    cswSourceConfiguration.setOutputSchema(outputSchema);
    LOGGER.debug("Setting output schema to: {}", outputSchema);
  }

  public void setQueryTypeName(String queryTypeName) {
    cswSourceConfiguration.setQueryTypeName(queryTypeName);
    LOGGER.debug("Setting queryTypeQName to: {}", queryTypeName);
  }

  public void setQueryTypeNamespace(String queryTypeNamespace) {
    cswSourceConfiguration.setQueryTypeNamespace(queryTypeNamespace);
    LOGGER.debug("Setting queryTypePrefix to: {}", queryTypeNamespace);
  }

  @Override
  public boolean isAvailable() {
    return availabilityTask.isAvailable();
  }

  @Override
  public boolean isAvailable(SourceMonitor sourceMonitor) {
    sourceMonitors.add(sourceMonitor);
    return isAvailable();
  }

  @Override
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    return query(queryRequest, ElementSetType.FULL, null);
  }

  protected SourceResponse query(
      QueryRequest queryRequest, ElementSetType elementSetName, List<QName> elementNames)
      throws UnsupportedQueryException {

    Query query = queryRequest.getQuery();
    LOGGER.debug("{}: Received query:\n{}", cswSourceConfiguration.getId(), query);

    GetRecordsType getRecordsType =
        createGetRecordsRequest(queryRequest, elementSetName, elementNames);

    LOGGER.debug(
        "{}: Sending query to: {}",
        cswSourceConfiguration.getId(),
        cswSourceConfiguration.getCswUrl());

    List<Result> results;
    Long totalHits;

    CswRecordCollection cswRecordCollection = cswClient.getRecords(getRecordsType);

    this.availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
    LOGGER.debug(
        "{}: Received [{}] record(s) of the [{}] record(s) matched from {}.",
        cswSourceConfiguration.getId(),
        cswRecordCollection.getNumberOfRecordsReturned(),
        cswRecordCollection.getNumberOfRecordsMatched(),
        cswSourceConfiguration.getCswUrl());

    results = createResults(cswRecordCollection);
    totalHits = cswRecordCollection.getNumberOfRecordsMatched();

    LOGGER.debug(
        "{}: Adding {} result(s) to the source response.",
        cswSourceConfiguration.getId(),
        results.size());

    SourceResponseImpl sourceResponse = new SourceResponseImpl(queryRequest, results, totalHits);
    addContentTypes(sourceResponse);
    return sourceResponse;
  }

  @Override
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append(describableProperties.getProperty(DESCRIPTION))
        .append(System.getProperty("line.separator"))
        .append(description);
    return sb.toString();
  }

  @SuppressWarnings("squid:S1488")
  @Override
  public String getId() {
    String sourceId = super.getId();
    // Note, returning "UNKNOWN" causes issues for collecting source metrics on
    // ConnectedSources. This method is called initially when the connected source is first
    // added and the sourceId is null at that time, but this causes metrics for an UNKNOWN
    // source to be created and never deleted. Returning super.getId() for the ConnectedSources
    // until a problem is discovered.
    return sourceId;
  }

  @Override
  public void setId(String id) {
    cswSourceConfiguration.setId(id);
    super.setId(id);
  }

  @Override
  public void maskId(String newSourceId) {
    final String methodName = "maskId";
    LOGGER.debug("ENTERING: {} with sourceId = {}", methodName, newSourceId);

    if (newSourceId != null) {
      super.maskId(newSourceId);
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public String getOrganization() {
    return describableProperties.getProperty(ORGANIZATION);
  }

  @Override
  public String getTitle() {
    return describableProperties.getProperty(TITLE);
  }

  @Override
  public String getVersion() {
    return describableProperties.getProperty(VERSION);
  }

  @SuppressWarnings("squid:S1168")
  @Override
  public Set<String> getOptions(Metacard arg0) {
    return null;
  }

  @SuppressWarnings("squid:S1168")
  @Override
  public Set<String> getSupportedSchemes() {
    return null;
  }

  @Override
  public ResourceResponse retrieveResource(
      URI resourceUri, Map<String, Serializable> requestProperties)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {

    Serializable serializableId = null;
    if (requestProperties != null) {
      serializableId = requestProperties.get(Core.ID);
    }

    if (canRetrieveResourceById()) {
      // If no resource reader was found, retrieve the product through a GetRecordById request
      if (serializableId == null) {
        throw new ResourceNotFoundException(
            "Unable to retrieve resource because no metacard ID was found.");
      }
      String metacardId = serializableId.toString();
      LOGGER.debug("Retrieving resource for ID : {}", metacardId);
      ResourceResponse response = retrieveResourceById(metacardId);
      if (response != null) {
        return response;
      }
    }

    if (resourceUri == null) {
      throw new IllegalArgumentException(
          "Unable to retrieve resource because no resource URI was given");
    }
    LOGGER.debug("Retrieving resource at : {}", resourceUri);
    return resourceReader.retrieveResource(resourceUri, requestProperties);
  }

  private ResourceResponse retrieveResourceById(String metacardId)
      throws ResourceNotFoundException {
    GetRecordByIdType getRecordByIdRequest = objectFactory.createGetRecordByIdType();
    getRecordByIdRequest.setService(CswConstants.CSW);
    getRecordByIdRequest.setVersion(CswConstants.VERSION_2_0_2);
    getRecordByIdRequest.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdRequest.setOutputFormat(APPLICATION_OCTET_STREAM);

    getRecordByIdRequest.setId(List.of(metacardId));

    CswRecordCollection recordCollection;
    try {
      recordCollection = cswClient.getRecordById(getRecordByIdRequest);

      Resource resource = recordCollection.getResource();
      if (resource != null) {
        return new ResourceResponseImpl(
            new ResourceImpl(
                new BufferedInputStream(resource.getInputStream()),
                resource.getMimeTypeValue(),
                FilenameUtils.getName(resource.getName())));
      } else {
        return null;
      }
    } catch (UnsupportedQueryException e) {
      throw new ResourceNotFoundException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, metacardId), e);
    }
  }

  public void setCswUrl(String cswUrl) {
    String newCswUrl = PropertyResolver.resolveProperties(cswUrl);
    if (!newCswUrl.equals(cswSourceConfiguration.getCswUrl())) {
      cswSourceConfiguration.setCswUrl(newCswUrl);
      LOGGER.debug("Setting cswUrl to {}", cswUrl);
    }
  }

  public String getAuthenticationType() {
    return cswSourceConfiguration.getAuthenticationType();
  }

  public void setAuthenticationType(String authenticationType) {
    cswSourceConfiguration.setAuthenticationType(authenticationType);
  }

  public void setUsername(String username) {
    cswSourceConfiguration.setUsername(username);
  }

  public void setPassword(String password) {
    cswSourceConfiguration.setPassword(password);
  }

  public void setDisableCnCheck(Boolean disableCnCheck) {
    cswSourceConfiguration.setDisableCnCheck(disableCnCheck);
  }

  public void setCoordinateOrder(String coordinateOrder) {
    CswAxisOrder cswAxisOrder = CswAxisOrder.LON_LAT;

    if (StringUtils.isNotBlank(coordinateOrder)) {
      cswAxisOrder = CswAxisOrder.valueOf(CswAxisOrder.class, coordinateOrder);

      if (cswAxisOrder == null) {
        cswAxisOrder = CswAxisOrder.LON_LAT;
      }
    }

    LOGGER.debug(
        "{}: Setting CSW coordinate order to {}", cswSourceConfiguration.getId(), cswAxisOrder);
    cswSourceConfiguration.setCswAxisOrder(cswAxisOrder);
  }

  public void setUsePosList(Boolean usePosList) {
    cswSourceConfiguration.setUsePosList(usePosList);
    LOGGER.debug("Using posList rather than individual pos elements?: {}", usePosList);
  }

  public void setIsCqlForced(Boolean isCqlForced) {
    cswSourceConfiguration.setIsCqlForced(isCqlForced);
  }

  public void setMetacardMappings(String[] configuredMappings) {
    if (configuredMappings != null && configuredMappings.length > 0) {
      Map<String, String> mappings = new HashMap<>(configuredMappings.length);
      Arrays.stream(configuredMappings)
          .forEach(
              m -> {
                String[] parts = m.split("=");
                mappings.put(parts[0], parts[1]);
              });
      cswSourceConfiguration.setMetacardCswMappings(mappings);
    }
  }

  public void setFilterAdapter(FilterAdapter filterAdapter) {
    this.filterAdapter = filterAdapter;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public void setPollInterval(Integer interval) {
    this.cswSourceConfiguration.setPollIntervalMinutes(interval);
  }

  public Converter getCswTransformConverter() {
    return this.cswTransformConverter;
  }

  public void setCswTransformConverter(Converter provider) {
    this.cswTransformConverter = provider;
  }

  public QueryFilterTransformerProvider getCswQueryFilterTransformerProvider() {
    return cswQueryFilterTransformerProvider;
  }

  public void setCswQueryFilterTransformerProvider(
      QueryFilterTransformerProvider cswQueryFilterTransformerProvider) {
    this.cswQueryFilterTransformerProvider = cswQueryFilterTransformerProvider;
  }

  public String getForceSpatialFilter() {
    return forceSpatialFilter;
  }

  public void setForceSpatialFilter(String forceSpatialFilter) {
    this.forceSpatialFilter = forceSpatialFilter;
  }

  private GetRecordsType createGetRecordsRequest(
      QueryRequest queryRequest, ElementSetType elementSetName, List<QName> elementNames)
      throws UnsupportedQueryException {
    Query query = queryRequest.getQuery();
    GetRecordsType getRecordsType = new GetRecordsType();
    getRecordsType.setVersion(CswConstants.VERSION_2_0_2);
    getRecordsType.setService(CswConstants.CSW);
    getRecordsType.setResultType(ResultType.RESULTS);
    getRecordsType.setStartPosition(BigInteger.valueOf(query.getStartIndex()));
    getRecordsType.setMaxRecords(BigInteger.valueOf(query.getPageSize()));
    getRecordsType.setOutputFormat(APPLICATION_XML);
    if (!isOutputSchemaSupported()) {
      String msg =
          "CSW Source: "
              + cswSourceConfiguration.getId()
              + " does not support output schema: "
              + cswSourceConfiguration.getOutputSchema()
              + ".";
      throw new UnsupportedQueryException(msg);
    }
    getRecordsType.setOutputSchema(cswSourceConfiguration.getOutputSchema());
    getRecordsType.setAbstractQuery(createQuery(queryRequest, elementSetName, elementNames));
    return getRecordsType;
  }

  private ElementSetNameType createElementSetName(ElementSetType type) {
    ElementSetNameType elementSetNameType = new ElementSetNameType();
    elementSetNameType.setValue(type);
    return elementSetNameType;
  }

  private JAXBElement<QueryType> createQuery(
      QueryRequest queryRequest, ElementSetType elementSetType, List<QName> elementNames)
      throws UnsupportedQueryException {
    QueryType queryType = new QueryType();

    QName queryTypeQName = null;
    try {
      if (StringUtils.isNotBlank(cswSourceConfiguration.getQueryTypeName())) {
        String[] parts = cswSourceConfiguration.getQueryTypeName().split(":");
        if (parts.length > 1) {
          queryTypeQName =
              new QName(cswSourceConfiguration.getQueryTypeNamespace(), parts[1], parts[0]);
        } else {
          queryTypeQName =
              new QName(
                  cswSourceConfiguration.getQueryTypeNamespace(),
                  cswSourceConfiguration.getQueryTypeName());
        }
      }
    } catch (IllegalArgumentException e) {
      LOGGER.debug(
          "Unable to parse query type QName of {}.  Defaulting to CSW Record",
          cswSourceConfiguration.getQueryTypeName());
    }
    if (queryTypeQName == null) {
      queryTypeQName =
          new QName(
              CswConstants.CSW_OUTPUT_SCHEMA,
              CswConstants.CSW_RECORD_LOCAL_NAME,
              CswConstants.CSW_NAMESPACE_PREFIX);
    }

    QueryRequest transformedQueryRequest =
        cswQueryFilterTransformerProvider != null
            ? cswQueryFilterTransformerProvider
                .getTransformer(cswSourceConfiguration.getQueryTypeName())
                .map(it -> it.transform(queryRequest, null))
                .orElse(queryRequest)
            : queryRequest;

    queryType.setTypeNames(Arrays.asList(queryTypeQName));
    if (null != elementSetType) {
      queryType.setElementSetName(createElementSetName(elementSetType));
    } else if (!CollectionUtils.isEmpty(elementNames)) {
      queryType.setElementName(elementNames);
    } else {
      queryType.setElementSetName(createElementSetName(ElementSetType.FULL));
    }
    SortByType sortBy = createSortBy(transformedQueryRequest);
    if (sortBy != null) {
      queryType.setSortBy(sortBy);
    }
    QueryConstraintType constraint = createQueryConstraint(transformedQueryRequest);
    if (null != constraint) {
      queryType.setConstraint(constraint);
    }
    return objectFactory.createQuery(queryType);
  }

  private SortByType createSortBy(QueryRequest queryRequest) {

    Query query = queryRequest.getQuery();
    SortByType cswSortBy = null;

    if (query != null && query.getSortBy() != null && query.getSortBy().getPropertyName() != null) {
      List<SortBy> sortBys = new ArrayList<>();
      sortBys.add(query.getSortBy());
      Serializable extSortBySer = queryRequest.getPropertyValue(ADDITIONAL_SORT_BYS);
      if (extSortBySer instanceof SortBy[]) {
        SortBy[] extSortBys = (SortBy[]) extSortBySer;
        if (extSortBys.length > 0) {
          sortBys.addAll(Arrays.asList(extSortBys));
        }
      }

      for (SortBy sortBy : sortBys) {
        SortPropertyType sortProperty = new SortPropertyType();
        PropertyNameType propertyName = new PropertyNameType();

        if (sortBy.getPropertyName() != null) {
          String propName = sortBy.getPropertyName().getPropertyName();

          if (propName != null) {
            if (Result.TEMPORAL.equals(propName) || Metacard.ANY_DATE.equals(propName)) {
              propName = Core.MODIFIED;
            } else if (Result.RELEVANCE.equals(propName) || Metacard.ANY_TEXT.equals(propName)) {
              propName = Core.TITLE;
            } else if (Result.DISTANCE.equals(propName) || Metacard.ANY_GEO.equals(propName)) {
              continue;
            }

            if (cswSortBy == null) {
              cswSortBy = new SortByType();
            }

            propertyName.setContent(
                Arrays.asList((Object) cswFilterDelegate.mapPropertyName(propName)));
            sortProperty.setPropertyName(propertyName);
            if (SortOrder.DESCENDING.equals(query.getSortBy().getSortOrder())) {
              sortProperty.setSortOrder(SortOrderType.DESC);
            } else {
              sortProperty.setSortOrder(SortOrderType.ASC);
            }
            cswSortBy.getSortProperty().add(sortProperty);
          }
        }
      }
    } else {
      return null;
    }

    return cswSortBy;
  }

  private QueryConstraintType createQueryConstraint(QueryRequest queryRequest)
      throws UnsupportedQueryException {
    FilterType filter = createFilter(queryRequest.getQuery());
    if (null == filter) {
      return null;
    }
    QueryConstraintType queryConstraintType = new QueryConstraintType();
    queryConstraintType.setVersion(CswConstants.CONSTRAINT_VERSION);
    if (isConstraintCql || cswSourceConfiguration.isCqlForced()) {
      queryConstraintType.setCqlText(CswCqlTextFilter.getInstance().getCqlText(filter));
    } else {
      queryConstraintType.setFilter(filter);
    }
    return queryConstraintType;
  }

  private FilterType createFilter(Query query) throws UnsupportedQueryException {
    OperationsMetadata operationsMetadata = capabilities.getOperationsMetadata();
    if (null == operationsMetadata) {
      LOGGER.debug("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
      return new FilterType();
    }
    if (null == capabilities.getFilterCapabilities()) {
      LOGGER.debug(
          "{}: CSW Source did not provide Filter Capabilities, unable to preform query.",
          cswSourceConfiguration.getId());
      throw new UnsupportedQueryException(
          cswSourceConfiguration.getId()
              + ": CSW Source did not provide Filter Capabilities, unable to preform query.");
    }
    return this.filterAdapter.adapt(query, cswFilterDelegate);
  }

  protected List<Result> createResults(CswRecordCollection cswRecordCollection) {
    List<Result> results = new ArrayList<>();

    LOGGER.debug(
        "Found {} metacard(s) in the CswRecordCollection.",
        cswRecordCollection.getCswRecords().size());

    String transformerId = getMetadataTransformerId();

    MetadataTransformer transformer = lookupMetadataTransformer(transformerId);

    for (Metacard metacard : cswRecordCollection.getCswRecords()) {
      MetacardImpl wrappedMetacard = new MetacardImpl(metacard);
      wrappedMetacard.setSourceId(getId());
      if (wrappedMetacard.getAttribute(Core.RESOURCE_DOWNLOAD_URL) != null
          && wrappedMetacard.getAttribute(Core.RESOURCE_DOWNLOAD_URL).getValue() != null) {
        wrappedMetacard.setAttribute(
            Core.RESOURCE_URI, wrappedMetacard.getAttribute(Core.RESOURCE_DOWNLOAD_URL).getValue());
      }
      if (wrappedMetacard.getAttribute(Core.DERIVED_RESOURCE_DOWNLOAD_URL) != null
          && !wrappedMetacard
              .getAttribute(Core.DERIVED_RESOURCE_DOWNLOAD_URL)
              .getValues()
              .isEmpty()) {
        wrappedMetacard.setAttribute(
            new AttributeImpl(
                Core.DERIVED_RESOURCE_URI,
                wrappedMetacard.getAttribute(Core.DERIVED_RESOURCE_DOWNLOAD_URL).getValues()));
      }
      Metacard tranformedMetacard = wrappedMetacard;
      if (transformer != null) {
        tranformedMetacard = transform(metacard, transformer);
      }
      Result result = new ResultImpl(tranformedMetacard);
      results.add(result);
    }

    return results;
  }

  protected String getMetadataTransformerId() {
    return DEFAULT_CSW_TRANSFORMER_ID;
  }

  /**
   * Transforms the Metacard created from the CSW Record using the transformer specified by its ID.
   *
   * @param metacard
   * @return
   */
  protected Metacard transform(Metacard metacard, MetadataTransformer transformer) {

    if (metacard == null) {
      throw new IllegalArgumentException(cswSourceConfiguration.getId() + ": Metacard is null.");
    }

    try {
      return transformer.transform(metacard);
    } catch (CatalogTransformerException e) {
      LOGGER.debug(
          "{} :Metadata Transformation Failed for metacard: {}",
          cswSourceConfiguration.getId(),
          metacard.getId(),
          e);
    }
    return metacard;
  }

  protected MetadataTransformer lookupMetadataTransformer(String transformerId) {
    ServiceReference<?>[] refs;

    try {
      refs =
          context.getServiceReferences(
              MetadataTransformer.class.getName(),
              "(" + Constants.SERVICE_ID + "=" + transformerId + ")");
    } catch (InvalidSyntaxException e) {
      LOGGER.debug("{}: Invalid transformer ID.", cswSourceConfiguration.getId(), e);
      return null;
    }

    if (refs == null || refs.length == 0) {
      LOGGER.debug(
          "{}: Metadata Transformer {} not found.", cswSourceConfiguration.getId(), transformerId);
      return null;
    } else {
      return (MetadataTransformer) context.getService(refs[0]);
    }
  }

  protected CapabilitiesType getCapabilities() {
    CapabilitiesType caps = null;

    try {
      LOGGER.debug("Doing getCapabilities() call for CSW");
      GetCapabilitiesType request = objectFactory.createGetCapabilitiesType();
      request.setService(CswConstants.CSW);

      AcceptVersionsType acceptVersions = new AcceptVersionsType();
      acceptVersions.setVersion(List.of(CswConstants.VERSION_2_0_2));
      request.setAcceptVersions(acceptVersions);

      caps = cswClient.getCapabilities(request);
    } catch (Exception e) {
      LOGGER.debug("Unable to get Capabilities.", e);
    }
    return caps;
  }

  public void configureCswSource() {
    detailLevels = EnumSet.noneOf(ElementSetType.class);
    capabilities = getCapabilities();

    if (capabilities != null) {
      if (capabilities.getFilterCapabilities() == null) {
        return;
      }

      readGetRecordsOperation(capabilities);

      loadContentTypes();
      LOGGER.debug("{}: {}", cswSourceConfiguration.getId(), capabilities);
    } else {
      LOGGER.info(
          "{}: CSW Server did not return any capabilities.", cswSourceConfiguration.getId());
    }
  }

  private Operation getOperation(OperationsMetadata operations, String operation) {
    for (Operation op : operations.getOperation()) {
      if (operation.equals(op.getName())) {
        return op;
      }
    }

    LOGGER.info(
        "{}: CSW Server did not contain getRecords operation", cswSourceConfiguration.getId());
    return null;
  }

  /**
   * Parses the getRecords {@link Operation} to understand the capabilities of the
   * org.codice.ddf.spatial.ogc.csw.catalog.common.Csw Server. A sample GetRecords Operation may
   * look like this:
   *
   * <p>
   *
   * <pre>
   *   <ows:Operation name="GetRecords">
   *     <ows:DCP>
   *       <ows:HTTP>
   *         <ows:Get  xlink:href="http://www.cubewerx.com/cwcsw.cgi?" />
   *         <ows:Post xlink:href="http://www.cubewerx.com/cwcsw.cgi" />
   *       </ows:HTTP>
   *     </ows:DCP>
   *     <ows:Parameter name="TypeName">
   *       <ows:Value>csw:Record</ows:Value>
   *     </ows:Parameter>
   *     <ows:Parameter name="outputFormat">
   *       <ows:Value>application/xml</ows:Value>
   *       <ows:Value>text/html</ows:Value>
   *       <ows:Value>text/plain</ows:Value>
   *     </ows:Parameter>
   *     <ows:Parameter name="outputSchema">
   *       <ows:Value>http://www.opengis.net/cat/csw/2.0.2</ows:Value>
   *     </ows:Parameter>
   *     <ows:Parameter name="resultType">
   *       <ows:Value>hits</ows:Value>
   *       <ows:Value>results</ows:Value>
   *       <ows:Value>validate</ows:Value>
   *     </ows:Parameter>
   *     <ows:Parameter name="ElementSetName">
   *       <ows:Value>brief</ows:Value>
   *       <ows:Value>summary</ows:Value>
   *       <ows:Value>full</ows:Value>
   *     </ows:Parameter>
   *     <ows:Parameter name="CONSTRAINTLANGUAGE">
   *       <ows:Value>Filter</ows:Value>
   *     </ows:Parameter>
   *   </ows:Operation>
   * </pre>
   *
   * @param capabilitiesType The capabilities the org.codice.ddf.spatial.ogc.csw.catalog.common.Csw
   *     Server supports
   */
  private void readGetRecordsOperation(CapabilitiesType capabilitiesType) {
    OperationsMetadata operationsMetadata = capabilitiesType.getOperationsMetadata();
    if (null == operationsMetadata) {
      LOGGER.info("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
      return;
    }

    description = capabilitiesType.getServiceIdentification().getAbstract();

    Operation getRecordsOp = getOperation(operationsMetadata, CswConstants.GET_RECORDS);

    if (null == getRecordsOp) {
      LOGGER.info(
          "{}: CSW Source contains no getRecords Operation", cswSourceConfiguration.getId());
      return;
    }

    this.supportedOutputSchemas = getParameter(getRecordsOp, CswConstants.OUTPUT_SCHEMA_PARAMETER);

    DomainType constraintLanguage =
        getParameter(getRecordsOp, CswConstants.CONSTRAINT_LANGUAGE_PARAMETER);
    if (null != constraintLanguage) {
      DomainType outputFormatValues =
          getParameter(getRecordsOp, CswConstants.OUTPUT_FORMAT_PARAMETER);
      DomainType resultTypesValues = getParameter(getRecordsOp, CswConstants.RESULT_TYPE_PARAMETER);
      readSetDetailLevels(getParameter(getRecordsOp, CswConstants.ELEMENT_SET_NAME_PARAMETER));

      List<String> constraints = new ArrayList<>();
      for (String s : constraintLanguage.getValue()) {
        constraints.add(s.toLowerCase());
      }

      if (constraints.contains(CswConstants.CONSTRAINT_LANGUAGE_CQL.toLowerCase())
          && !constraints.contains(CswConstants.CONSTRAINT_LANGUAGE_FILTER.toLowerCase())) {
        isConstraintCql = true;
      } else {
        isConstraintCql = false;
      }

      setFilterDelegate(
          getRecordsOp,
          capabilitiesType.getFilterCapabilities(),
          outputFormatValues,
          resultTypesValues,
          cswSourceConfiguration);

      if (!NO_FORCE_SPATIAL_FILTER.equals(forceSpatialFilter)) {
        SpatialOperatorType sot = new SpatialOperatorType();
        SpatialOperatorNameType sont = SpatialOperatorNameType.fromValue(forceSpatialFilter);
        sot.setName(sont);
        sot.setGeometryOperands(cswFilterDelegate.getGeoOpsForSpatialOp(sont));
        SpatialOperatorsType spatialOperators = new SpatialOperatorsType();
        spatialOperators.setSpatialOperator(Arrays.asList(sot));
        cswFilterDelegate.setSpatialOps(spatialOperators);
      }
    }
  }

  /**
   * Sets the {@link ddf.catalog.filter.FilterDelegate} used by the AbstractCswSource. May be
   * overridden in order to provide a custom ddf.catalog.filter.FilterDelegate implementation.
   *
   * @param getRecordsOp
   * @param filterCapabilities
   * @param outputFormatValues
   * @param resultTypesValues
   * @param cswSourceConfiguration
   */
  protected void setFilterDelegate(
      Operation getRecordsOp,
      FilterCapabilities filterCapabilities,
      DomainType outputFormatValues,
      DomainType resultTypesValues,
      CswSourceConfiguration cswSourceConfiguration) {
    LOGGER.trace("Setting cswFilterDelegate to default CswFilterDelegate");

    cswFilterDelegate =
        new CswFilterDelegate(
            parser,
            getRecordsOp,
            filterCapabilities,
            outputFormatValues,
            resultTypesValues,
            cswSourceConfiguration);
  }

  private void readSetDetailLevels(DomainType elementSetNamesValues) {
    if (null != elementSetNamesValues) {
      for (String esn : elementSetNamesValues.getValue()) {
        try {
          detailLevels.add(ElementSetType.fromValue(esn.toLowerCase()));
        } catch (IllegalArgumentException iae) {
          LOGGER.debug(
              "{}: \"{}\" is not a ElementSetType", cswSourceConfiguration.getId(), esn, iae);
        }
      }
    }
  }

  protected void loadContentTypes() {
    Filter filter =
        filterBuilder.attribute(CswConstants.ANY_TEXT).is().like().text(CswConstants.WILD_CARD);
    Query query = new QueryImpl(filter, 1, CONTENT_TYPE_SAMPLE_SIZE, null, true, 0);
    QueryRequest queryReq = new QueryRequestImpl(query);

    try {
      query(queryReq);
    } catch (UnsupportedQueryException e) {
      LOGGER.info("{}: Failed to read Content-Types from CSW Server", getId(), e);
    }
  }

  /**
   * Searches every query response for previously unknown content types
   *
   * @param response A Query Response
   */
  private void addContentTypes(SourceResponse response) {
    if (response == null || response.getResults() == null) {
      return;
    }

    for (Result result : response.getResults()) {
      Metacard metacard = result.getMetacard();
      if (metacard != null) {
        final String name = metacard.getContentTypeName();
        String version = metacard.getContentTypeVersion();
        final URI namespace = metacard.getContentTypeNamespace();
        if (!StringUtils.isEmpty(name) && !contentTypes.containsKey(name)) {
          if (version == null) {
            version = "";
          }
          contentTypes.put(name, new ContentTypeImpl(name, version, namespace));
        }
      }
    }
  }

  public DomainType getParameter(Operation operation, String name) {
    for (DomainType parameter : operation.getParameter()) {
      if (name.equalsIgnoreCase(parameter.getName())) {
        return parameter;
      }
    }
    LOGGER.debug(
        "{}: CSW Operation \"{}\" did not contain the \"{}\" parameter",
        cswSourceConfiguration.getId(),
        operation.getName(),
        name);
    return null;
  }

  protected void availabilityChanged(boolean isAvailable) {

    if (isAvailable) {
      LOGGER.debug("CSW source {} is available.", cswSourceConfiguration.getId());
    } else {
      LOGGER.debug("CSW source {} is unavailable.", cswSourceConfiguration.getId());
    }

    for (SourceMonitor monitor : this.sourceMonitors) {
      if (isAvailable) {
        LOGGER.debug(
            "Notifying source monitor that CSW source {} is available.",
            cswSourceConfiguration.getId());
        monitor.setAvailable();
      } else {
        LOGGER.debug(
            "Notifying source monitor that CSW source {} is unavailable.",
            cswSourceConfiguration.getId());
        monitor.setUnavailable();
      }
    }
  }

  /**
   * Determine if the resource should be retrieved using a ResourceReader or by calling
   * GetRecordById.
   */
  private boolean canRetrieveResourceById() {
    OperationsMetadata operationsMetadata = capabilities.getOperationsMetadata();
    Operation getRecordByIdOp = getOperation(operationsMetadata, CswConstants.GET_RECORD_BY_ID);
    if (getRecordByIdOp != null) {
      DomainType getRecordByIdOutputSchemas =
          getParameter(getRecordByIdOp, CswConstants.OUTPUT_SCHEMA_PARAMETER);
      if (getRecordByIdOutputSchemas != null
          && getRecordByIdOutputSchemas.getValue() != null
          && getRecordByIdOutputSchemas.getValue().contains(OCTET_STREAM_OUTPUT_SCHEMA)) {
        return true;
      }
    }
    return false;
  }

  private boolean isOutputSchemaSupported() {
    return (this.cswSourceConfiguration.getOutputSchema() != null
            && this.supportedOutputSchemas != null)
        && this.supportedOutputSchemas
            .getValue()
            .contains(cswSourceConfiguration.getOutputSchema());
  }

  public void setAvailabilityTask(AvailabilityTask availabilityTask) {
    this.availabilityTask = availabilityTask;
  }

  @Override
  public String getConfigurationPid() {
    return configurationPid;
  }

  @Override
  public void setConfigurationPid(String configurationPid) {
    this.configurationPid = configurationPid;
  }

  public void setMetacardTypes(List<MetacardType> types) {}

  protected void addSourceMonitor(SourceMonitor sourceMonitor) {
    sourceMonitors.add(sourceMonitor);
  }

  /** Clean-up when shutting down the CswSource */
  @SuppressWarnings(
      "squid:S1172" /* The code parameter is required in blueprint-cm-1.0.7. See https://issues.apache.org/jira/browse/ARIES-1436. */)
  public void destroy(int code) {
    LOGGER.debug("{}: Entering destroy()", cswSourceConfiguration.getId());
    availabilityPollFuture.cancel(true);
    scheduler.shutdownNow();
  }

  /**
   * Callback class to check the Availability of the CswSource.
   *
   * <p>NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
   * have an AvailabilityTask that cached each Source's availability. Until that is done, allow the
   * command to handle the logic of managing availability.
   */
  private class CswSourceAvailabilityCommand implements AvailabilityCommand {

    @Override
    public boolean isAvailable() {
      LOGGER.debug("Checking availability for source {} ", cswSourceConfiguration.getId());
      boolean oldAvailability = AbstractCswSource.this.isAvailable();
      boolean newAvailability;
      // Simple "ping" to ensure the source is responding
      newAvailability = (getCapabilities() != null);
      if (oldAvailability != newAvailability) {
        // If the source becomes available, configure it.
        if (newAvailability) {
          configureCswSource();
        }
        availabilityChanged(newAvailability);
      }
      return newAvailability;
    }
  }
}
