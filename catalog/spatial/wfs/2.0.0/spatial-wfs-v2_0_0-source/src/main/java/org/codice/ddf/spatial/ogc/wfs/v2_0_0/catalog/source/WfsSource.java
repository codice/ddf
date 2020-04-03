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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import static java.util.stream.Collectors.toSet;

import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.encryption.EncryptionService;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.FilterType;
import net.opengis.filter.v_2_0_0.SortByType;
import net.opengis.filter.v_2_0_0.SortOrderType;
import net.opengis.filter.v_2_0_0.SortPropertyType;
import net.opengis.filter.v_2_0_0.SpatialOperatorType;
import net.opengis.filter.v_2_0_0.SpatialOperatorsType;
import net.opengis.wfs.v_2_0_0.FeatureTypeType;
import net.opengis.wfs.v_2_0_0.GetFeatureType;
import net.opengis.wfs.v_2_0_0.QueryType;
import net.opengis.wfs.v_2_0_0.WFSCapabilitiesType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.catalog.common.ContentTypeFilterDelegate;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.AbstractWfsSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl.MetacardMapperImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20JaxbElementProvider;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.FeatureConverterFactory;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl.GenericFeatureConverterWfs20;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader.FeatureCollectionMessageBodyReaderWfs20;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader.XmlSchemaMessageBodyReaderWfs20;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a Federated and Connected source implementation for OGC WFS servers. */
public class WfsSource extends AbstractWfsSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(WfsSource.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String DESCRIPTION = "description";

  private static final String ORGANIZATION = "organization";

  private static final String VERSION = "version";

  private static final String TITLE = "name";

  private static final String WFSURL_PROPERTY = "wfsUrl";

  private static final String ID_PROPERTY = "id";

  private static final String AUTHENTICATION_TYPE = "authenticationType";

  private static final String USERNAME_PROPERTY = "username";

  @SuppressWarnings("squid:S2068" /* Real credentials are not hard coded */)
  private static final String PASSWORD_PROPERTY = "password";

  private static final String NON_QUERYABLE_PROPS_PROPERTY = "nonQueryableProperties";

  private static final String SPATIAL_FILTER_PROPERTY = "forceSpatialFilter";

  private static final String COORDINATE_ORDER = "coordinateOrder";

  private static final String DISABLE_SORTING = "disableSorting";

  private static final String NO_FORCED_SPATIAL_FILTER = "NO_FILTER";

  private static final String CONNECTION_TIMEOUT_PROPERTY = "connectionTimeout";

  private static final String RECEIVE_TIMEOUT_PROPERTY = "receiveTimeout";

  private static final String SRS_NAME_PROPERTY = "srsName";

  private static final String WFS_ERROR_MESSAGE = "Error received from Wfs Server.";

  private static final String UNKNOWN = "unknown";

  private static final String DEFAULT_WFS_TRANSFORMER_ID = "wfs_2_0";

  private static final String POLL_INTERVAL_PROPERTY = "pollInterval";

  private static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

  private static final String BASIC = "basic";

  private static Properties describableProperties = new Properties();

  private final EncryptionService encryptionService;

  private final ClientFactoryFactory clientFactoryFactory;

  static {
    try (InputStream properties =
        WfsSource.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(properties);
    } catch (IOException e) {
      LOGGER.info(e.getMessage(), e);
    }
  }

  protected Map<QName, WfsFilterDelegate> featureTypeFilters = new HashMap<>();

  private String wfsUrl;

  private String wfsVersion;

  private String authenticationType;

  private String username;

  private String password;

  private Boolean disableCnCheck = Boolean.FALSE;

  private String coordinateOrder = GeospatialUtil.LAT_LON_ORDER;

  private FilterAdapter filterAdapter;

  private BundleContext context;

  private Map<String, ServiceRegistration> metacardTypeServiceRegistrations = new HashMap<>();

  private String[] nonQueryableProperties;

  private List<FeatureConverterFactory> featureConverterFactories;

  private Integer pollInterval;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private String forceSpatialFilter = NO_FORCED_SPATIAL_FILTER;

  private ScheduledExecutorService scheduler;

  private ScheduledFuture<?> availabilityPollFuture;

  private AvailabilityTask availabilityTask;

  private Set<SourceMonitor> sourceMonitors = new HashSet<>();

  private List<MetacardMapper> metacardToFeatureMappers;

  private boolean disableSorting;

  private SecureCxfClientFactory<Wfs> factory;

  protected String configurationPid;

  private FeatureCollectionMessageBodyReaderWfs20 featureCollectionReader;

  private String forcedFeatureType;

  private String srsName;

  public WfsSource(
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService,
      ScheduledExecutorService scheduler) {
    // Required for bean creation
    LOGGER.debug("Creating {}", WfsSource.class.getName());
    this.clientFactoryFactory = clientFactoryFactory;
    this.encryptionService = encryptionService;
    this.scheduler = scheduler;
  }

  /**
   * Init is called when the bundle is initially configured.
   *
   * <p>
   *
   * <p>The init process creates a RemoteWfs object using the connection parameters from the
   * configuration.
   */
  public void init() {
    createClientFactory();
    setupAvailabilityPoll();
  }

  @SuppressWarnings(
      "squid:S1172" /* The code parameter is required in blueprint-cm-1.0.7. See https://issues.apache.org/jira/browse/ARIES-1436. */)
  public void destroy(int code) {
    unregisterAllMetacardTypes();
    availabilityPollFuture.cancel(true);
    scheduler.shutdownNow();
  }

  /**
   * Refresh is called if the bundle configuration is updated.
   *
   * <p>
   *
   * <p>If any of the connection related properties change, an attempt is made to re-connect.
   *
   * @param configuration
   */
  public void refresh(Map<String, Object> configuration) {
    LOGGER.debug("WfsSource {}: Refresh called", getId());
    String wfsUrl = (String) configuration.get(WFSURL_PROPERTY);
    String password = (String) configuration.get(PASSWORD_PROPERTY);
    String username = (String) configuration.get(USERNAME_PROPERTY);
    String authenticationType = (String) configuration.get(AUTHENTICATION_TYPE);
    Boolean disableCnCheckProp = (Boolean) configuration.get(DISABLE_CN_CHECK_PROPERTY);
    String coordinateOrder = (String) configuration.get(COORDINATE_ORDER);
    boolean disableSorting = (Boolean) configuration.get(DISABLE_SORTING);
    String id = (String) configuration.get(ID_PROPERTY);

    setConnectionTimeout((Integer) configuration.get(CONNECTION_TIMEOUT_PROPERTY));
    setReceiveTimeout((Integer) configuration.get(RECEIVE_TIMEOUT_PROPERTY));
    setSrsName((String) configuration.get(SRS_NAME_PROPERTY));

    String[] nonQueryableProperties = (String[]) configuration.get(NON_QUERYABLE_PROPS_PROPERTY);

    this.nonQueryableProperties = nonQueryableProperties;

    Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_PROPERTY);
    super.setId(id);

    this.wfsUrl = wfsUrl;
    this.password = encryptionService.decryptValue(password);
    this.username = username;
    this.authenticationType = authenticationType;
    this.disableCnCheck = disableCnCheckProp;
    this.coordinateOrder = coordinateOrder;
    this.disableSorting = disableSorting;
    this.forceSpatialFilter = (String) configuration.get(SPATIAL_FILTER_PROPERTY);
    createClientFactory();
    configureWfsFeatures();

    if (!pollInterval.equals(newPollInterval)) {
      LOGGER.debug("Poll Interval was changed for source {}.", getId());
      setPollInterval(newPollInterval);
      availabilityPollFuture.cancel(true);
      setupAvailabilityPoll();
    }
  }

  private List<? extends Object> initProviders() {
    // We need to tell the JAXBElementProvider to marshal the GetFeatureType
    // class as an element
    // because it is missing the @XmlRootElement Annotation
    JAXBElementProvider<GetFeatureType> provider = new Wfs20JaxbElementProvider<>();
    Map<String, String> jaxbClassMap = new HashMap<>();

    // Ensure a namespace is used when the GetFeature request is generated
    String expandedName =
        new QName(Wfs20Constants.WFS_2_0_NAMESPACE, Wfs20Constants.GET_FEATURE).toString();
    jaxbClassMap.put(GetFeatureType.class.getName(), expandedName);
    provider.setJaxbElementClassMap(jaxbClassMap);
    provider.setMarshallAsJaxbElement(true);

    featureCollectionReader = new FeatureCollectionMessageBodyReaderWfs20();
    return Arrays.asList(provider, new XmlSchemaMessageBodyReaderWfs20(), featureCollectionReader);
  }

  /* This method should only be called after all properties have been set. */
  private void createClientFactory() {
    if (BASIC.equals(authenticationType)
        && StringUtils.isNotBlank(username)
        && StringUtils.isNotBlank(password)) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              false,
              connectionTimeout,
              receiveTimeout,
              username,
              password);
    } else if (StringUtils.isNotBlank(getCertAlias())
        && StringUtils.isNotBlank(getKeystorePath())) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              false,
              connectionTimeout,
              receiveTimeout,
              getCertAlias(),
              getKeystorePath(),
              getSslProtocol());
    } else {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              false,
              connectionTimeout,
              receiveTimeout);
    }
  }

  private void setupAvailabilityPoll() {
    LOGGER.debug(
        "Setting Availability poll task for {} minute(s) on Source {}", pollInterval, getId());
    WfsSourceAvailabilityCommand command = new WfsSourceAvailabilityCommand();
    long interval = TimeUnit.MINUTES.toMillis(pollInterval);
    if (availabilityPollFuture == null || availabilityPollFuture.isCancelled()) {
      if (availabilityTask == null) {
        availabilityTask = new AvailabilityTask(interval, command, getId());
      } else {
        availabilityTask.setInterval(interval);
      }
      // Run the availability check immediately prior to scheduling it in a thread.
      // This is necessary to allow the catalog framework to have the correct
      // availability when the source is bound
      availabilityTask.run();
      // Run the availability check every 1 second. The actually call to
      // the remote server will only occur if the pollInterval has
      // elapsed.
      availabilityPollFuture =
          scheduler.scheduleWithFixedDelay(
              availabilityTask,
              AvailabilityTask.NO_DELAY,
              AvailabilityTask.ONE_SECOND,
              TimeUnit.SECONDS);
    }
  }

  private WFSCapabilitiesType getCapabilities() {
    WFSCapabilitiesType capabilities = null;
    Wfs wfs = factory.getClient();

    try {
      capabilities = wfs.getCapabilities(new GetCapabilitiesRequest());
    } catch (WfsException wfse) {
      LOGGER.info(
          WFS_ERROR_MESSAGE + " Received HTTP code '{}' from server for source with id='{}'",
          wfse.getHttpStatus(),
          getId());
      LOGGER.debug(WFS_ERROR_MESSAGE, wfse);
    } catch (WebApplicationException wae) {
      LOGGER.debug(handleWebApplicationException(wae), wae);
    } catch (Exception e) {
      handleClientException(e);
    }
    return capabilities;
  }

  private void configureWfsFeatures() {
    WFSCapabilitiesType capabilities = getCapabilities();

    if (capabilities != null) {
      wfsVersion = capabilities.getVersion();
      List<FeatureTypeType> featureTypes = getFeatureTypes(capabilities);
      buildFeatureFilters(featureTypes, capabilities.getFilterCapabilities());
    } else {
      LOGGER.debug("WfsSource {}: WFS Server did not return any capabilities.", getId());
    }
  }

  private List<FeatureTypeType> getFeatureTypes(WFSCapabilitiesType capabilities) {
    List<FeatureTypeType> featureTypes = capabilities.getFeatureTypeList().getFeatureType();
    if (featureTypes.isEmpty()) {
      LOGGER.debug("\"WfsSource {}: No feature types found.", getId());
    }
    return featureTypes;
  }

  private void updateSupportedSpatialOperators(SpatialOperatorsType spatialOperatorsType) {
    if (spatialOperatorsType == null) {
      return;
    }

    if (NO_FORCED_SPATIAL_FILTER.equals(forceSpatialFilter)) {
      return;
    }

    SpatialOperatorsType forcedSpatialOpType = new SpatialOperatorsType();
    SpatialOperatorType sot = new SpatialOperatorType();
    sot.setName(forceSpatialFilter);
    forcedSpatialOpType.getSpatialOperator().add(sot);
    for (WfsFilterDelegate delegate : featureTypeFilters.values()) {
      delegate.setSpatialOps(forcedSpatialOpType);
    }
  }

  private void buildFeatureFilters(
      List<FeatureTypeType> featureTypes, FilterCapabilities filterCapabilities) {
    Wfs wfs = factory.getClient();
    if (filterCapabilities == null) {
      return;
    }

    // Use local Map for metacardtype registrations and once they are populated with latest
    // MetacardTypes, then do actual registration
    Map<String, MetacardTypeRegistration> mcTypeRegs = new HashMap<>();
    this.featureTypeFilters.clear();

    for (FeatureTypeType featureTypeType : featureTypes) {
      String ftSimpleName = featureTypeType.getName().getLocalPart();

      if (StringUtils.isNotBlank(forcedFeatureType)
          && !StringUtils.equals(forcedFeatureType, ftSimpleName)) {
        continue;
      }

      if (mcTypeRegs.containsKey(ftSimpleName)) {
        LOGGER.debug(
            "WfsSource {}: MetacardType {} is already registered - skipping to next metacard type",
            getId(),
            ftSimpleName);
        continue;
      }

      LOGGER.debug("ftName: {}", ftSimpleName);
      try {
        XmlSchema schema =
            wfs.describeFeatureType(new DescribeFeatureTypeRequest(featureTypeType.getName()));

        if (schema == null) {
          // Some WFS 2.0.0 DescribeFeatureRequests return inconsistent results when
          // the `:` character is encoded in the URL, ie Prefix:SomeFeature is encoded to
          // Prefix%3ASomeFeature. To avoid this issue, we will make a call without the prefix
          // if the previous call does not work.
          schema =
              wfs.describeFeatureType(
                  new DescribeFeatureTypeRequest(
                      new QName(
                          featureTypeType.getName().getNamespaceURI(),
                          featureTypeType.getName().getLocalPart(),
                          "")));
        }

        if (schema != null) {
          // Update local map with enough info to create actual MetacardType registrations
          // later
          MetacardTypeRegistration registration =
              createFeatureMetacardTypeRegistration(featureTypeType, ftSimpleName, schema);
          mcTypeRegs.put(ftSimpleName, registration);
          FeatureMetacardType featureMetacardType = registration.getFtMetacard();
          lookupFeatureConverter(ftSimpleName, featureMetacardType);

          MetacardMapper metacardAttributeToFeaturePropertyMapper =
              lookupMetacardAttributeToFeaturePropertyMapper(featureMetacardType.getFeatureType());

          this.featureTypeFilters.put(
              featureMetacardType.getFeatureType(),
              new WfsFilterDelegate(
                  featureMetacardType,
                  filterCapabilities,
                  registration.getSrs(),
                  metacardAttributeToFeaturePropertyMapper,
                  coordinateOrder));
        }
      } catch (WfsException | IllegalArgumentException wfse) {
        LOGGER.debug(WFS_ERROR_MESSAGE, wfse);
      } catch (WebApplicationException wae) {
        LOGGER.debug(handleWebApplicationException(wae), wae);
      }
    }

    registerFeatureMetacardTypes(mcTypeRegs);

    if (featureTypeFilters.isEmpty()) {
      LOGGER.debug("Wfs Source {}: No Feature Type schemas validated.", getId());
    }
    LOGGER.debug(
        "Wfs Source {}: Number of validated Features = {}", getId(), featureTypeFilters.size());
    updateSupportedSpatialOperators(
        filterCapabilities.getSpatialCapabilities().getSpatialOperators());
  }

  private void registerFeatureMetacardTypes(Map<String, MetacardTypeRegistration> mcTypeRegs) {
    // Unregister all MetacardType services - the DescribeFeatureTypeRequest should
    // have returned all of the most current metacard types that will now be registered.
    // As Source(s) are added/removed from this instance or to other Source(s)
    // that this instance is federated to, the list of metacard types will change.
    // This is done here vs. inside the above loop so that minimal time is spent clearing and
    // registering the MetacardTypes - the concern is that if this registration is too lengthy
    // a query could come in that is handled while the MetacardType registrations are
    // in a state of flux.
    unregisterAllMetacardTypes();
    if (!mcTypeRegs.isEmpty()) {
      for (MetacardTypeRegistration registration : mcTypeRegs.values()) {
        FeatureMetacardType ftMetacard = registration.getFtMetacard();
        String simpleName = ftMetacard.getFeatureType().getLocalPart();
        ServiceRegistration serviceRegistration =
            context.registerService(
                MetacardType.class.getName(), ftMetacard, registration.getProps());
        this.metacardTypeServiceRegistrations.put(simpleName, serviceRegistration);
      }
    }
  }

  private void lookupFeatureConverter(String ftSimpleName, FeatureMetacardType ftMetacard) {
    FeatureConverter featureConverter = null;

    /**
     * The list of feature converter factories injected into this class is a live list. So, feature
     * converter factories can be added and removed from the system while running.
     */
    if (CollectionUtils.isNotEmpty(featureConverterFactories)) {
      for (FeatureConverterFactory factory : featureConverterFactories) {
        if (ftSimpleName.equalsIgnoreCase(factory.getFeatureType())) {
          featureConverter = factory.createConverter();
          break;
        }
      }
    }

    // Found a specific feature converter
    if (featureConverter != null) {
      LOGGER.debug(
          "WFS Source {}: Features of type: {} will be converted using {}",
          getId(),
          ftSimpleName,
          featureConverter.getClass().getSimpleName());
    } else {
      LOGGER.debug(
          "WfsSource {}: Unable to find a feature specific converter; {} will be converted using the GenericFeatureConverter",
          getId(),
          ftSimpleName);

      // Since we have no specific converter, we will check to see if we have a mapper to do
      // feature property to metacard attribute mappings.
      MetacardMapper featurePropertyToMetacardAttributeMapper =
          lookupMetacardAttributeToFeaturePropertyMapper(ftMetacard.getFeatureType());

      featureConverter = new GenericFeatureConverterWfs20(featurePropertyToMetacardAttributeMapper);
      LOGGER.debug(
          "WFS Source {}: Created {} for feature type {} with feature property to metacard attribute mapper.",
          getId(),
          featureConverter.getClass().getSimpleName(),
          ftSimpleName);
    }

    featureConverter.setSourceId(getId());
    featureConverter.setMetacardType(ftMetacard);
    featureConverter.setWfsUrl(wfsUrl);
    featureConverter.setCoordinateOrder(coordinateOrder);

    // Add the Feature Type name as an alias for xstream
    LOGGER.debug(
        "Registering feature converter {} for feature type {}.",
        featureConverter.getClass().getSimpleName(),
        ftSimpleName);
    getFeatureCollectionReader().registerConverter(featureConverter);
  }

  private MetacardMapper lookupMetacardAttributeToFeaturePropertyMapper(QName featureType) {
    final Predicate<MetacardMapper> matchesFeatureType =
        mapper -> mapper.getFeatureType().equals(featureType.toString());
    return metacardToFeatureMappers.stream()
        .filter(matchesFeatureType)
        .findAny()
        .orElseGet(
            () -> {
              LOGGER.debug(
                  "Could not find a MetacardMapper for featureType {}. Returning a default implementation.",
                  featureType);
              return new MetacardMapperImpl();
            });
  }

  private MetacardTypeRegistration createFeatureMetacardTypeRegistration(
      FeatureTypeType featureTypeType, String ftName, XmlSchema schema) {
    FeatureMetacardType ftMetacard =
        new FeatureMetacardType(
            schema,
            featureTypeType.getName(),
            nonQueryableProperties != null
                ? Arrays.stream(nonQueryableProperties).collect(toSet())
                : new HashSet<>(),
            Wfs20Constants.GML_3_2_NAMESPACE);

    Dictionary<String, Object> props = new DictionaryMap<>();
    props.put(Metacard.CONTENT_TYPE, new String[] {ftName});

    LOGGER.debug("WfsSource {}: Registering MetacardType: {}", getId(), ftName);

    return new MetacardTypeRegistration(ftMetacard, props, featureTypeType.getDefaultCRS());
  }

  @Override
  public boolean isAvailable() {
    return availabilityTask.isAvailable();
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    this.sourceMonitors.add(callback);
    return isAvailable();
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    Wfs wfs = factory.getClient();
    Query query = request.getQuery();

    if (query == null) {
      LOGGER.debug("WFS Source {}: Incoming query is null.", getId());
      return null;
    }

    LOGGER.debug("WFS Source {}: Received query: \n{}", getId(), query);

    SourceResponseImpl simpleResponse = null;
    GetFeatureType getFeature = buildGetFeatureRequest(query);

    try {
      LOGGER.debug("WFS Source {}: Sending query ...", getId());
      Wfs20FeatureCollection featureCollection = wfs.getFeature(getFeature);
      int numResults = -1;

      if (featureCollection == null) {
        throw new UnsupportedQueryException("Invalid results returned from server");
      }

      numResults = featureCollection.getMembers().size();

      if (featureCollection.getNumberReturned() == null) {
        LOGGER.debug("Number Returned Attribute was not added to the response");
      } else if (!featureCollection.getNumberReturned().equals(BigInteger.valueOf(numResults))) {
        LOGGER.debug(
            "Number Returned Attribute ({}) did not match actual number returned ({})",
            featureCollection.getNumberReturned(),
            numResults);
      }

      availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
      LOGGER.debug(
          "WFS Source {}: Received featureCollection with {} metacards.", getId(), numResults);

      List<Result> results = new ArrayList<>(numResults);

      for (int i = 0; i < numResults; i++) {
        Metacard mc = featureCollection.getMembers().get(i);
        mc = transform(mc, DEFAULT_WFS_TRANSFORMER_ID);
        Result result = new ResultImpl(mc);
        results.add(result);
        debugResult(result);
      }

      // Fetch total results available
      long totalResults = 0;
      if (featureCollection.getNumberMatched() == null) {
        totalResults = numResults;
      } else if (featureCollection.getNumberMatched().equals(UNKNOWN)) {
        totalResults = numResults;
      } else if (StringUtils.isNumeric(featureCollection.getNumberMatched())) {
        totalResults = Long.parseLong(featureCollection.getNumberMatched());
      }

      simpleResponse = new SourceResponseImpl(request, results, totalResults);

    } catch (WfsException wfse) {
      LOGGER.debug(WFS_ERROR_MESSAGE, wfse);
      throw new UnsupportedQueryException("Error received from WFS Server", wfse);
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }

    return simpleResponse;
  }

  protected GetFeatureType buildGetFeatureRequest(Query query) throws UnsupportedQueryException {
    List<ContentType> contentTypes = getContentTypesFromQuery(query);

    List<QueryType> queries = new ArrayList<>();
    for (Entry<QName, WfsFilterDelegate> filterDelegateEntry : featureTypeFilters.entrySet()) {
      if (contentTypes.isEmpty()
          || isFeatureTypeInQuery(contentTypes, filterDelegateEntry.getKey().getLocalPart())) {
        QueryType wfsQuery = new QueryType();

        String typeName = null;
        if (StringUtils.isEmpty(filterDelegateEntry.getKey().getPrefix())) {
          typeName = filterDelegateEntry.getKey().getLocalPart();
        } else {
          typeName =
              filterDelegateEntry.getKey().getPrefix()
                  + ":"
                  + filterDelegateEntry.getKey().getLocalPart();
        }

        if (StringUtils.isNotBlank(srsName)) {
          wfsQuery.setSrsName(srsName);
        }

        wfsQuery.setTypeNames(Arrays.asList(typeName));
        wfsQuery.setHandle(filterDelegateEntry.getKey().getLocalPart());
        FilterType filter = filterAdapter.adapt(query, filterDelegateEntry.getValue());
        if (filter != null) {
          if (areAnyFiltersSet(filter)) {
            wfsQuery.setAbstractSelectionClause(
                new net.opengis.filter.v_2_0_0.ObjectFactory().createFilter(filter));
          }

          if (!this.disableSorting) {
            if (query.getSortBy() != null) {
              SortOrder sortOrder = query.getSortBy().getSortOrder();

              if (filterDelegateEntry.getValue().isSortingSupported()
                  && filterDelegateEntry.getValue().getAllowedSortOrders().contains(sortOrder)) {

                JAXBElement<SortByType> sortBy =
                    buildSortBy(filterDelegateEntry.getKey(), query.getSortBy());

                if (sortBy != null) {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sorting using sort order of [{}].", sortOrder.identifier());
                  }
                  wfsQuery.setAbstractSortingClause(sortBy);
                }
              } else if (filterDelegateEntry.getValue().isSortingSupported()
                  && CollectionUtils.isEmpty(
                      filterDelegateEntry.getValue().getAllowedSortOrders())) {

                JAXBElement<SortByType> sortBy =
                    buildSortBy(filterDelegateEntry.getKey(), query.getSortBy());

                if (sortBy != null) {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                        "No sort orders defined in getCapabilities.  Attempting to sort using sort order of [{}].",
                        sortOrder.identifier());
                  }
                  wfsQuery.setAbstractSortingClause(sortBy);
                }
              } else if (filterDelegateEntry.getValue().isSortingSupported()
                  && !filterDelegateEntry.getValue().getAllowedSortOrders().contains(sortOrder)) {
                LOGGER.debug(
                    "Unsupported sort order of [{}]. Supported sort orders are {}.",
                    sortOrder,
                    filterDelegateEntry.getValue().getAllowedSortOrders());
              } else if (!filterDelegateEntry.getValue().isSortingSupported()) {
                LOGGER.debug("Sorting is not supported.");
              }
            }
          } else {
            LOGGER.debug("Sorting is disabled.");
          }

          queries.add(wfsQuery);
        } else {
          LOGGER.debug(
              "WFS Source {}: {} has an invalid filter.", getId(), filterDelegateEntry.getKey());
        }
      }
    }
    if (!queries.isEmpty()) {

      GetFeatureType getFeatureType = new GetFeatureType();
      int pageSize = query.getPageSize();
      if (pageSize < 0) {
        LOGGER.debug("Page size has a negative value");
        throw new UnsupportedQueryException(
            "Unable to build query. Page size has a negative value.");
      }

      int startIndex = query.getStartIndex();

      if (startIndex < 0) {
        LOGGER.debug("Start index has a negative value");
        throw new UnsupportedQueryException(
            "Unable to build query. Start index has a negative value.");
      } else if (startIndex != 0) {
        // Convert DDF index of 1 back to index of 0 for WFS 2.0
        startIndex = query.getStartIndex() - 1;
      } else {
        LOGGER.debug("Query already has a start index of 0");
      }

      getFeatureType.setCount(BigInteger.valueOf(query.getPageSize()));
      getFeatureType.setStartIndex(BigInteger.valueOf(startIndex));
      List<JAXBElement<?>> incomingQueries = getFeatureType.getAbstractQueryExpression();
      for (QueryType queryType : queries) {
        incomingQueries.add(new net.opengis.wfs.v_2_0_0.ObjectFactory().createQuery(queryType));
      }
      logMessage(getFeatureType);

      return getFeatureType;
    } else {
      throw new UnsupportedQueryException(
          "Unable to build query. No filters could be created from query criteria.");
    }
  }

  private JAXBElement<SortByType> buildSortBy(QName featureType, SortBy incomingSortBy)
      throws UnsupportedQueryException {
    net.opengis.filter.v_2_0_0.ObjectFactory filterObjectFactory =
        new net.opengis.filter.v_2_0_0.ObjectFactory();

    String propertyName =
        mapSortByPropertyName(featureType, incomingSortBy.getPropertyName().getPropertyName());

    if (propertyName != null) {

      SortOrder sortOrder = incomingSortBy.getSortOrder();

      SortPropertyType sortPropertyType = filterObjectFactory.createSortPropertyType();
      sortPropertyType.setValueReference(propertyName);

      if (SortOrder.ASCENDING.equals(sortOrder)) {
        sortPropertyType.setSortOrder(SortOrderType.ASC);
      } else if (SortOrder.DESCENDING.equals(sortOrder)) {
        sortPropertyType.setSortOrder(SortOrderType.DESC);
      } else {
        throw new UnsupportedQueryException(
            "Unable to build query. Unknown sort order of [" + sortOrder.identifier() + "].");
      }

      SortByType sortByType = filterObjectFactory.createSortByType();
      sortByType.getSortProperty().add(sortPropertyType);

      return filterObjectFactory.createSortBy(sortByType);
    } else {
      return null;
    }
  }

  /**
   * If a MetacardMapper cannot be found or there is no mapping for the incomingPropertyName, return
   * null. This will cause a query to be constructed without an AbstractSortingClause.
   */
  private String mapSortByPropertyName(QName featureType, String incomingPropertyName) {
    MetacardMapper metacardToFeaturePropertyMapper =
        lookupMetacardAttributeToFeaturePropertyMapper(featureType);
    String mappedPropertyName;

    if (StringUtils.equals(Result.TEMPORAL, incomingPropertyName)
        || StringUtils.equals(Metacard.EFFECTIVE, incomingPropertyName)) {
      mappedPropertyName =
          StringUtils.isNotBlank(metacardToFeaturePropertyMapper.getSortByTemporalFeatureProperty())
              ? metacardToFeaturePropertyMapper.getSortByTemporalFeatureProperty()
              : null;
    } else if (StringUtils.equals(Result.RELEVANCE, incomingPropertyName)) {
      mappedPropertyName =
          StringUtils.isNotBlank(
                  metacardToFeaturePropertyMapper.getSortByRelevanceFeatureProperty())
              ? metacardToFeaturePropertyMapper.getSortByRelevanceFeatureProperty()
              : null;
    } else if (StringUtils.equals(Result.DISTANCE, incomingPropertyName)) {
      mappedPropertyName =
          StringUtils.isNotBlank(metacardToFeaturePropertyMapper.getSortByDistanceFeatureProperty())
              ? metacardToFeaturePropertyMapper.getSortByDistanceFeatureProperty()
              : null;
    } else {
      mappedPropertyName = null;
    }

    return mappedPropertyName;
  }

  private boolean areAnyFiltersSet(FilterType filter) {
    if (filter != null) {
      return (filter.isSetComparisonOps()
          || filter.isSetId()
          || filter.isSetLogicOps()
          || filter.isSetSpatialOps()
          || filter.isSetTemporalOps());
    } else {
      return false;
    }
  }

  private boolean isFeatureTypeInQuery(
      final List<ContentType> contentTypes, final String featureTypeName) {

    for (ContentType contentType : contentTypes) {
      if (featureTypeName.equalsIgnoreCase(contentType.getName())) {
        return true;
      }
    }
    return false;
  }

  private Metacard transform(Metacard mc, String transformerId) {
    if (mc == null) {
      throw new IllegalArgumentException("Metacard is null");
    }

    ServiceReference[] refs = null;
    try {
      refs =
          context.getServiceReferences(
              MetadataTransformer.class.getName(),
              "(" + Constants.SERVICE_ID + "=" + transformerId + ")");
    } catch (InvalidSyntaxException e) {
      LOGGER.debug("Invalid transformer ID. Returning original metacard.", e);
      return mc;
    }

    if (refs == null || refs.length == 0) {
      LOGGER.debug("MetadataTransformer not found.  Returning original metacard.");
      return mc;
    } else {
      try {
        MetadataTransformer transformer = (MetadataTransformer) context.getService(refs[0]);
        return transformer.transform(mc);
      } catch (CatalogTransformerException e) {
        LOGGER.debug(
            "Transformation Failed for transformer: {}. Returning original metacard",
            transformerId,
            e);
        return mc;
      }
    }
  }

  private List<ContentType> getContentTypesFromQuery(final Query query) {
    List<ContentType> contentTypes = null;

    try {
      contentTypes = filterAdapter.adapt(query, new ContentTypeFilterDelegate());
    } catch (UnsupportedQueryException e) {
      LOGGER.debug("WFS Source {}: Unable to get content types from query.", getId(), e);
    }

    return contentTypes != null ? contentTypes : new ArrayList<>();
  }

  private void unregisterAllMetacardTypes() {
    for (ServiceRegistration metacardTypeServiceRegistration :
        metacardTypeServiceRegistrations.values()) {
      if (metacardTypeServiceRegistration != null) {
        metacardTypeServiceRegistration.unregister();
      }
    }
    metacardTypeServiceRegistrations.clear();
  }

  @Override
  public Set<ContentType> getContentTypes() {
    Set<QName> typeNames = featureTypeFilters.keySet();
    Set<ContentType> contentTypes = new HashSet<>();
    for (QName featureName : typeNames) {
      contentTypes.add(new ContentTypeImpl(featureName.getLocalPart(), getVersion()));
    }
    return contentTypes;
  }

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
  public void maskId(String newSourceId) {
    final String methodName = "maskId";
    LOGGER.debug("ENTERING: {} with sourceId = {}", methodName, newSourceId);

    if (newSourceId != null) {
      super.maskId(newSourceId);
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public String getDescription() {
    return describableProperties.getProperty(DESCRIPTION);
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
    if (StringUtils.isNotBlank(wfsVersion)) {
      return wfsVersion;
    }
    return describableProperties.getProperty(VERSION);
  }

  @Override
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments) {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("<html><script type=\"text/javascript\">window.location.replace(\"");
    strBuilder.append(uri);
    strBuilder.append("\");</script></html>");

    Resource resource =
        new ResourceImpl(
            IOUtils.toInputStream(strBuilder.toString()),
            MediaType.TEXT_HTML,
            getId() + " Resource");

    return new ResourceResponseImpl(resource);
  }

  @Override
  public Set<String> getSupportedSchemes() {
    // TODO Auto-generated method stub -
    return new HashSet<>();
  }

  @Override
  public Set<String> getOptions(Metacard metacard) {
    // TODO Auto-generated method stub
    return new HashSet<>();
  }

  public String getWfsUrl() {
    return wfsUrl;
  }

  public void setWfsUrl(String wfsUrl) {
    this.wfsUrl = wfsUrl;
    factory = clientFactoryFactory.getSecureCxfClientFactory(wfsUrl, Wfs.class);
  }

  public void setAuthenticationType(String authenticationType) {
    this.authenticationType = authenticationType;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = encryptionService.decryptValue(password);
  }

  public void setDisableCnCheck(Boolean disableCnCheck) {
    this.disableCnCheck = disableCnCheck;
  }

  public void setPollInterval(Integer interval) {
    this.pollInterval = interval;
  }

  public Integer getPollInterval() {
    return pollInterval;
  }

  public void setConnectionTimeout(Integer timeout) {
    this.connectionTimeout = timeout;
  }

  public Integer getConnectionTimeout() {
    return this.connectionTimeout;
  }

  public void setReceiveTimeout(Integer timeout) {
    this.receiveTimeout = timeout;
  }

  public Integer getReceiveTimeout() {
    return this.receiveTimeout;
  }

  public void setSrsName(String srsName) {
    this.srsName = srsName;
  }

  public String getSrsName() {
    return this.srsName;
  }

  public void setFilterAdapter(FilterAdapter filterAdapter) {
    this.filterAdapter = filterAdapter;
  }

  public void setFilterDelgates(Map<QName, WfsFilterDelegate> delegates) {
    this.featureTypeFilters = delegates;
  }

  public void setContext(BundleContext context) {
    this.context = context;
  }

  public void setNonQueryableProperties(String[] newNonQueryableProperties) {
    if (newNonQueryableProperties == null) {
      this.nonQueryableProperties = new String[0];
    } else {
      this.nonQueryableProperties =
          Arrays.copyOf(newNonQueryableProperties, newNonQueryableProperties.length);
    }
  }

  public String getForceSpatialFilter() {
    return forceSpatialFilter;
  }

  public void setForceSpatialFilter(String forceSpatialFilter) {
    this.forceSpatialFilter = forceSpatialFilter;
  }

  public void setFeatureConverterFactoryList(List<FeatureConverterFactory> factories) {
    this.featureConverterFactories = factories;
  }

  public List<MetacardMapper> getMetacardToFeatureMapper() {
    return this.metacardToFeatureMappers;
  }

  public void setMetacardToFeatureMapper(List<MetacardMapper> mappers) {
    this.metacardToFeatureMappers = mappers;
  }

  public void setCoordinateOrder(String coordinateOrder) {
    this.coordinateOrder = coordinateOrder;
  }

  public void setDisableSorting(boolean disableSorting) {
    this.disableSorting = disableSorting;
  }

  private String handleWebApplicationException(WebApplicationException wae) {
    Response response = wae.getResponse();
    WfsException wfsException = new WfsResponseExceptionMapper().fromResponse(response);

    return "Error received from WFS Server " + getId() + "\n" + wfsException.getMessage();
  }

  private String handleClientException(Exception ce) {
    String msg = "";
    Throwable cause = ce.getCause();
    String sourceId = getId();
    if (cause instanceof WebApplicationException) {
      msg = handleWebApplicationException((WebApplicationException) cause);
    } else if (cause instanceof IllegalArgumentException) {
      msg =
          WFS_ERROR_MESSAGE
              + " Source '"
              + sourceId
              + "'. The URI '"
              + getWfsUrl()
              + "' does not specify a valid protocol or could not be correctly parsed. "
              + ce.getMessage();
    } else if (cause instanceof SSLHandshakeException) {
      msg =
          WFS_ERROR_MESSAGE
              + " Source '"
              + sourceId
              + "' with URL '"
              + getWfsUrl()
              + "': "
              + ce.getMessage();
    } else if (cause instanceof ConnectException) {
      msg =
          WFS_ERROR_MESSAGE + " Source '" + sourceId + "' may not be running.\n" + ce.getMessage();
    } else {
      msg = WFS_ERROR_MESSAGE + " Source '" + sourceId + "'\n" + ce;
    }
    LOGGER.info(msg);
    LOGGER.debug(msg, ce);
    return msg;
  }

  private void logMessage(GetFeatureType getFeature) {
    if (LOGGER.isDebugEnabled()) {
      try {
        StringWriter writer = new StringWriter();
        String context =
            StringUtils.join(
                new String[] {
                  Wfs20Constants.OGC_FILTER_PACKAGE,
                  Wfs20Constants.OGC_GML_PACKAGE,
                  Wfs20Constants.OGC_OWS_PACKAGE,
                  Wfs20Constants.OGC_WFS_PACKAGE
                },
                ":");
        JAXBContext contextObj = JAXBContext.newInstance(context, WfsSource.class.getClassLoader());

        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        marshallerObj.marshal(
            new net.opengis.wfs.v_2_0_0.ObjectFactory().createGetFeature(getFeature), writer);
        LOGGER.debug("WfsSource {}: {}", getId(), writer);
      } catch (JAXBException e) {
        LOGGER.debug("An error occurred debugging the GetFeature request", e);
      }
    }
  }

  private void debugResult(Result result) {
    if (LOGGER.isDebugEnabled()) {
      if (result != null && result.getMetacard() != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nid:\t" + result.getMetacard().getId());
        sb.append("\nmetacardType:\t" + result.getMetacard().getMetacardType());
        if (result.getMetacard().getMetacardType() != null) {
          sb.append("\nmetacardType name:\t" + result.getMetacard().getMetacardType().getName());
        }
        sb.append("\ncontentType:\t" + result.getMetacard().getContentTypeName());
        sb.append("\ntitle:\t" + result.getMetacard().getTitle());
        sb.append("\nsource:\t" + result.getMetacard().getSourceId());
        sb.append("\nmetadata:\t" + result.getMetacard().getMetadata());
        sb.append("\nlocation:\t" + result.getMetacard().getLocation());

        LOGGER.debug("Transform complete. Metacard: {}", sb);
      }
    }
  }

  @Override
  public String getConfigurationPid() {
    return configurationPid;
  }

  @Override
  public void setConfigurationPid(String configurationPid) {
    this.configurationPid = configurationPid;
  }

  private static class MetacardTypeRegistration {

    private FeatureMetacardType ftMetacard;

    private Dictionary<String, Object> props;

    private String srs;

    public MetacardTypeRegistration(
        FeatureMetacardType ftMetacard, Dictionary<String, Object> props, String srs) {
      this.ftMetacard = ftMetacard;
      this.props = props;
      this.srs = srs;
    }

    public FeatureMetacardType getFtMetacard() {
      return ftMetacard;
    }

    public Dictionary<String, Object> getProps() {
      return props;
    }

    public String getSrs() {
      return srs;
    }
  }

  /**
   * Callback class to check the Availability of the WfsSource.
   *
   * <p>NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
   * have an AvailabilityTask that cached each Source's availability. Until that is done, allow the
   * command to handle the logic of managing availability.
   */
  private class WfsSourceAvailabilityCommand implements AvailabilityCommand {
    private void availabilityChanged(boolean isAvailable) {
      if (isAvailable) {
        LOGGER.debug("WFS source {} is available.", getId());
      } else {
        LOGGER.debug("WFS source {} is unavailable.", getId());
      }

      for (SourceMonitor monitor : WfsSource.this.sourceMonitors) {
        if (isAvailable) {
          LOGGER.debug("Notifying source monitor that WFS source {} is available.", getId());
          monitor.setAvailable();
        } else {
          LOGGER.debug("Notifying source monitor that WFS source {} is unavailable.", getId());
          monitor.setUnavailable();
        }
      }
    }

    @Override
    public boolean isAvailable() {
      LOGGER.debug("Checking availability for source {} ", getId());
      boolean oldAvailability = WfsSource.this.isAvailable();
      // Simple "ping" to ensure the source is responding
      boolean newAvailability = (null != getCapabilities());
      // If the source becomes available, configure it.
      // When the source is available, we need to account for new feature converter factories
      // being added while the system is running.
      if (newAvailability) {
        LOGGER.debug("WFS Source {} is available...configuring.", getId());
        configureWfsFeatures();
        newAvailability = !featureTypeFilters.isEmpty();
      }

      if (oldAvailability != newAvailability) {
        availabilityChanged(newAvailability);
      }
      return newAvailability;
    }
  }

  public FeatureCollectionMessageBodyReaderWfs20 getFeatureCollectionReader() {
    return this.featureCollectionReader;
  }

  public void setFeatureCollectionReader(
      FeatureCollectionMessageBodyReaderWfs20 featureCollectionMessageBodyReaderWfs20) {
    this.featureCollectionReader = featureCollectionMessageBodyReaderWfs20;
  }

  public void setForcedFeatureType(String featureType) {
    this.forcedFeatureType = featureType;
  }
}
