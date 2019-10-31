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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static java.util.stream.Collectors.toSet;
import static org.codice.ddf.libs.geo.util.GeospatialUtil.LAT_LON_ORDER;

import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.stream.Collectors;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import net.opengis.wfs.v_1_1_0.GetFeatureType;
import net.opengis.wfs.v_1_1_0.ObjectFactory;
import net.opengis.wfs.v_1_1_0.QueryType;
import net.opengis.wfs.v_1_1_0.ResultTypeType;
import net.opengis.wfs.v_1_1_0.WFSCapabilitiesType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.catalog.common.ContentTypeFilterDelegate;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.AbstractWfsSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsMetadataImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl.MetacardMapperImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.FeatureTransformationService;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.source.reader.XmlSchemaMessageBodyReaderWfs11;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a Federated and Connected source implementation for OGC WFS servers. */
public class WfsSource extends AbstractWfsSource {

  static final int WFS_MAX_FEATURES_RETURNED = 1000;

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsSource.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String DESCRIPTION = "description";

  private static final String ORGANIZATION = "organization";

  private static final String VERSION = "version";

  private static final String TITLE = "name";

  private static final String WFSURL_KEY = "wfsUrl";

  private static final String ID_KEY = "id";

  private static final String USERNAME_KEY = "username";

  @SuppressWarnings("squid:S2068" /*Key for the requestProperties map, not a hardcoded password*/)
  private static final String PASSWORD_KEY = "password";

  private static final String NON_QUERYABLE_PROPS_KEY = "nonQueryableProperties";

  private static final String FORCED_FEATURE_TYPE_KEY = "forcedFeatureType";

  private static final String SPATIAL_FILTER_KEY = "forceSpatialFilter";

  private static final String NO_FORCED_SPATIAL_FILTER = "NO_FILTER";

  private static final String CONNECTION_TIMEOUT_KEY = "connectionTimeout";

  private static final String RECEIVE_TIMEOUT_KEY = "receiveTimeout";

  private static final String WFS_ERROR_MESSAGE = "Error received from Wfs Server.";

  private static final String DEFAULT_WFS_TRANSFORMER_ID = "wfs";

  private static final String POLL_INTERVAL_KEY = "pollInterval";

  private static final String DISABLE_CN_CHECK_KEY = "disableCnCheck";

  private static final String COORDINATE_ORDER_KEY = "coordinateOrder";

  private static final String ALLOW_REDIRECTS_KEY = "allowRedirects";

  private static final String SRS_NAME_KEY = "srsName";

  private static final Properties DESCRIBABLE_PROPERTIES = new Properties();

  private static final String SOURCE_MSG = " Source '";

  private final EncryptionService encryptionService;

  private final ClientFactoryFactory clientFactoryFactory;

  private String wfsUrl;

  private String wfsVersion;

  private Map<QName, WfsFilterDelegate> featureTypeFilters = new HashMap<>();

  private String username;

  private String password;

  private String coordinateOrder = LAT_LON_ORDER;

  private Boolean disableCnCheck = Boolean.FALSE;

  private FilterAdapter filterAdapter;

  private BundleContext context;

  private String[] nonQueryableProperties;

  private Integer pollInterval;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private String forceSpatialFilter = NO_FORCED_SPATIAL_FILTER;

  private ScheduledExecutorService scheduler;

  private ScheduledFuture<?> availabilityPollFuture;

  private AvailabilityTask availabilityTask;

  private Set<SourceMonitor> sourceMonitors = new HashSet<>();

  private SecureCxfClientFactory<Wfs> factory;

  private String configurationPid;

  private String forcedFeatureType;

  private List<MetacardTypeEnhancer> metacardTypeEnhancers;

  private List<MetacardMapper> metacardMappers;

  private String srsName;

  private boolean allowRedirects;

  private WfsMetadata<FeatureTypeType> wfsMetadata;

  private FeatureTransformationService featureTransformationService;

  private WfsMetacardTypeRegistry wfsMetacardTypeRegistry;

  private static final String FEATURE_MEMBER_ELEMENT = "featureMember";

  static {
    try (InputStream properties =
        WfsSource.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(properties);
    } catch (IOException e) {
      LOGGER.error(
          "failed to load describable properties files: {}", DESCRIBABLE_PROPERTIES_FILE, e);
    }
  }

  public WfsSource(
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService,
      ScheduledExecutorService scheduler) {
    this.clientFactoryFactory = clientFactoryFactory;
    this.encryptionService = encryptionService;
    wfsMetadata =
        new WfsMetadataImpl<>(
            this::getId,
            this::getCoordinateOrder,
            Collections.singletonList(FEATURE_MEMBER_ELEMENT),
            FeatureTypeType.class);
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
  @SuppressWarnings("unused")
  public void init() {
    createClientFactory();
    setupAvailabilityPoll();
  }

  @SuppressWarnings({
    "squid:S1172" /* The code parameter is required in blueprint-cm-1.0.7. See https://issues.apache.org/jira/browse/ARIES-1436. */,
    "unused"
  })
  public void destroy(int code) {
    wfsMetacardTypeRegistry.clear();
    availabilityPollFuture.cancel(true);
    scheduler.shutdownNow();
  }

  /**
   * Refresh is called if the bundle configuration is updated.
   *
   * <p>If any of the connection related properties change, an attempt is made to re-connect.
   *
   * @param configuration configuration settings
   */
  public void refresh(Map<String, Object> configuration) {
    LOGGER.trace("WfsSource {}: Refresh called with {}", getId(), configuration);

    setId((String) configuration.get(ID_KEY));
    setWfsUrl((String) configuration.get(WFSURL_KEY));
    setDisableCnCheck((Boolean) configuration.get(DISABLE_CN_CHECK_KEY));
    setAllowRedirects((Boolean) configuration.get(ALLOW_REDIRECTS_KEY));
    setCoordinateOrder((String) configuration.get(COORDINATE_ORDER_KEY));
    setUsername((String) configuration.get(USERNAME_KEY));
    setPassword((String) configuration.get(PASSWORD_KEY));
    setCertAlias((String) configuration.get(CERT_ALIAS_KEY));
    setKeystorePath((String) configuration.get(KEYSTORE_PATH_KEY));
    setSslProtocol((String) configuration.get(SSL_PROTOCOL_KEY));
    setForcedFeatureType((String) configuration.get(FORCED_FEATURE_TYPE_KEY));
    setForceSpatialFilter((String) configuration.get(SPATIAL_FILTER_KEY));
    setNonQueryableProperties((String[]) configuration.get(NON_QUERYABLE_PROPS_KEY));
    setConnectionTimeout((Integer) configuration.get(CONNECTION_TIMEOUT_KEY));
    setReceiveTimeout((Integer) configuration.get(RECEIVE_TIMEOUT_KEY));
    setSrsName((String) configuration.get(SRS_NAME_KEY));

    createClientFactory();
    configureWfsFeatures();

    Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_KEY);

    if (!pollInterval.equals(newPollInterval)) {
      LOGGER.trace("Poll Interval was changed for source {}.", getId());
      setPollInterval(newPollInterval);
      availabilityPollFuture.cancel(true);
      setupAvailabilityPoll();
    }
  }

  public void setAllowRedirects(Boolean allowRedirects) {
    this.allowRedirects = allowRedirects;
  }

  /** This method should only be called after all properties have been set. */
  private void createClientFactory() {
    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              this.allowRedirects,
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
              this.allowRedirects,
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
              this.allowRedirects,
              connectionTimeout,
              receiveTimeout);
    }
  }

  private List<?> initProviders() {
    // We need to tell the JAXBElementProvider to marshal the GetFeatureType
    // class as an element because it is missing the @XmlRootElement Annotation
    JAXBElementProvider<GetFeatureType> provider = new JAXBElementProvider<>();
    Map<String, String> jaxbClassMap = new HashMap<>();

    // Ensure a namespace is used when the GetFeature request is generated
    String expandedName =
        new QName(Wfs11Constants.WFS_NAMESPACE, Wfs11Constants.GET_FEATURE).toString();
    jaxbClassMap.put(GetFeatureType.class.getName(), expandedName);
    provider.setJaxbElementClassMap(jaxbClassMap);
    provider.setMarshallAsJaxbElement(true);

    return Arrays.asList(
        provider,
        new WfsResponseExceptionMapper(),
        new XmlSchemaMessageBodyReaderWfs11(),
        new WfsMessageBodyReader(featureTransformationService, () -> wfsMetadata));
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
      LOGGER.debug(
          WFS_ERROR_MESSAGE + " Received HTTP code '{}' from server for source with id='{}'",
          wfse.getHttpStatus(),
          getId(),
          wfse);
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
      List<String> supportedGeo = getSupportedGeo(capabilities);
      buildFeatureFilters(featureTypes, supportedGeo);
    } else {
      LOGGER.info("WfsSource {}: WFS Server did not return any capabilities.", getId());
    }
  }

  private List<FeatureTypeType> getFeatureTypes(WFSCapabilitiesType capabilities) {
    List<FeatureTypeType> featureTypes = capabilities.getFeatureTypeList().getFeatureType();
    if (featureTypes.isEmpty()) {
      LOGGER.debug("WfsSource {}: No feature types found.", getId());
    }
    return featureTypes;
  }

  private List<String> getSupportedGeo(WFSCapabilitiesType capabilities) {
    final List<String> supportedGeoFilters = new ArrayList<>();

    List<SpatialOperatorType> geoTypes =
        capabilities
            .getFilterCapabilities()
            .getSpatialCapabilities()
            .getSpatialOperators()
            .getSpatialOperator();

    supportedGeoFilters.addAll(
        geoTypes.stream().map(geoType -> geoType.getName().value()).collect(Collectors.toList()));

    if (!NO_FORCED_SPATIAL_FILTER.equals(forceSpatialFilter)) {
      return Collections.singletonList(forceSpatialFilter);
    }
    return supportedGeoFilters;
  }

  private void buildFeatureFilters(List<FeatureTypeType> featureTypes, List<String> supportedGeo) {
    Wfs wfs = factory.getClient();

    // Use local Map for metacardtype registrations and once they are populated with latest
    // MetacardTypes, then do actual registration
    Map<String, FeatureMetacardType> mcTypeRegs = new HashMap<>();
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
          schema =
              wfs.describeFeatureType(
                  new DescribeFeatureTypeRequest(
                      new QName(
                          featureTypeType.getName().getNamespaceURI(),
                          featureTypeType.getName().getLocalPart(),
                          "")));
        }

        if (schema != null) {
          FeatureMetacardType featureMetacardType =
              createFeatureMetacardTypeRegistration(featureTypeType, ftSimpleName, schema);

          MetacardMapper metacardMapper = getMetacardMapper(featureTypeType.getName());
          this.featureTypeFilters.put(
              featureMetacardType.getFeatureType(),
              new WfsFilterDelegate(
                  featureMetacardType, metacardMapper, supportedGeo, getCoordinateStrategy()));

          mcTypeRegs.put(ftSimpleName, featureMetacardType);

          ((WfsMetadataImpl<FeatureTypeType>) wfsMetadata).addEntry(featureTypeType);
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
  }

  private MetacardMapper getMetacardMapper(final QName featureTypeName) {
    final Predicate<MetacardMapper> matchesFeatureType =
        mapper -> mapper.getFeatureType().equals(featureTypeName.toString());
    return metacardMappers
        .stream()
        .filter(matchesFeatureType)
        .findAny()
        .orElseGet(
            () -> {
              LOGGER.debug(
                  "Could not find a MetacardMapper for featureType {}. Returning a default implementation.",
                  featureTypeName);
              return new MetacardMapperImpl();
            });
  }

  private void registerFeatureMetacardTypes(Map<String, FeatureMetacardType> mcTypeRegs) {
    // Unregister all MetacardType services - the DescribeFeatureTypeRequest should
    // have returned all of the most current metacard types that will now be registered.
    // As Source(s) are added/removed from this instance or to other Source(s)
    // that this instance is federated to, the list of metacard types will change.
    // This is done here vs. inside the above loop so that minimal time is spent clearing and
    // registering the MetacardTypes - the concern is that if this registration is too lengthy
    // a query could come in that is handled while the MetacardType registrations are
    // in a state of flux.
    wfsMetacardTypeRegistry.clear();

    List<String> featureNames = new ArrayList<>();
    if (!mcTypeRegs.isEmpty()) {
      for (FeatureMetacardType metacardType : mcTypeRegs.values()) {
        String simpleName = metacardType.getFeatureType().getLocalPart();
        featureNames.add(simpleName);
        wfsMetacardTypeRegistry.registerMetacardType(metacardType, this.getId(), simpleName);
      }
    }

    WfsMetadataImpl wfsMetadataImpl =
        new WfsMetadataImpl<>(
            this::getId, this::getCoordinateOrder, featureNames, FeatureTypeType.class);
    this.wfsMetadata.getDescriptors().forEach(wfsMetadataImpl::addEntry);
    this.wfsMetadata = wfsMetadataImpl;
  }

  private FeatureMetacardType createFeatureMetacardTypeRegistration(
      FeatureTypeType featureTypeType, String ftName, XmlSchema schema) {

    MetacardTypeEnhancer metacardTypeEnhancer =
        metacardTypeEnhancers
            .stream()
            .filter(me -> me.getFeatureName() != null)
            .filter(me -> me.getFeatureName().equalsIgnoreCase(ftName))
            .findAny()
            .orElse(FeatureMetacardType.DEFAULT_METACARD_TYPE_ENHANCER);

    FeatureMetacardType ftMetacard =
        new FeatureMetacardType(
            schema,
            featureTypeType.getName(),
            nonQueryableProperties != null
                ? Arrays.stream(nonQueryableProperties).collect(toSet())
                : new HashSet<>(),
            Wfs11Constants.GML_3_1_1_NAMESPACE,
            metacardTypeEnhancer);

    Dictionary<String, Object> props = new DictionaryMap<>();
    props.put(Metacard.CONTENT_TYPE, new String[] {ftName});

    LOGGER.debug("WfsSource {}: Registering MetacardType: {}", getId(), ftName);

    return ftMetacard;
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    Wfs wfs = factory.getClient();

    Query query = request.getQuery();
    LOGGER.debug("WFS Source {}: Received query: \n{}", getId(), query);

    if (query.getStartIndex() < 1) {
      throw new UnsupportedQueryException(
          "Start Index is one-based and must be an integer greater than 0; should not be ["
              + query.getStartIndex()
              + "]");
    }

    int origPageSize = query.getPageSize();
    if (origPageSize <= 0 || origPageSize > WFS_MAX_FEATURES_RETURNED) {
      origPageSize = WFS_MAX_FEATURES_RETURNED;
    }
    QueryImpl modifiedQuery = new QueryImpl(query);

    int pageNumber = query.getStartIndex() / origPageSize + 1;

    int modifiedPageSize = Math.min(pageNumber * origPageSize, WFS_MAX_FEATURES_RETURNED);
    LOGGER.debug("WFS Source {}: modified page size = {}", getId(), modifiedPageSize);
    modifiedQuery.setPageSize(modifiedPageSize);

    final GetFeatureType getHits = buildGetFeatureRequestHits(modifiedQuery);
    final GetFeatureType getResults = buildGetFeatureRequestResults(modifiedQuery);

    try {
      LOGGER.debug("WFS Source {}: Getting hits.", getId());
      final WfsFeatureCollection hitsResponse = wfs.getFeature(getHits);
      final long totalHits = hitsResponse.getNumberOfFeatures();

      LOGGER.debug("The query has {} hits.", totalHits);

      LOGGER.debug("WFS Source {}: Sending query ...", getId());
      final WfsFeatureCollection featureCollection = wfs.getFeature(getResults);

      if (featureCollection == null) {
        throw new UnsupportedQueryException("Invalid results returned from server");
      }
      availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
      LOGGER.debug(
          "WFS Source {}: Received featureCollection with {} metacards.",
          getId(),
          featureCollection.getFeatureMembers().size());

      // Only return the number of results originally asked for in the
      // query, or the entire list of results if it is smaller than the
      // original page size.
      int numberOfResultsToReturn =
          Math.min(origPageSize, featureCollection.getFeatureMembers().size());
      List<Result> results = new ArrayList<>(numberOfResultsToReturn);

      int stopIndex =
          Math.min(
              origPageSize + query.getStartIndex(),
              featureCollection.getFeatureMembers().size() + 1);

      LOGGER.debug(
          "WFS Source {}: startIndex = {}, stopIndex = {}, origPageSize = {}, pageNumber = {}",
          getId(),
          query.getStartIndex(),
          stopIndex,
          origPageSize,
          pageNumber);

      for (int i = query.getStartIndex(); i < stopIndex; i++) {
        Metacard mc = featureCollection.getFeatureMembers().get(i - 1);
        mc = transform(mc, DEFAULT_WFS_TRANSFORMER_ID);
        Result result = new ResultImpl(mc);
        results.add(result);
        debugResult(result);
      }
      return new SourceResponseImpl(request, results, totalHits);
    } catch (WfsException wfse) {
      LOGGER.debug(WFS_ERROR_MESSAGE, wfse);
      throw new UnsupportedQueryException("Error received from WFS Server", wfse);
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }
  }

  private GetFeatureType buildGetFeatureRequestHits(final Query query)
      throws UnsupportedQueryException {
    return buildGetFeatureRequest(query, ResultTypeType.HITS, null);
  }

  private GetFeatureType buildGetFeatureRequestResults(final Query query)
      throws UnsupportedQueryException {
    return buildGetFeatureRequest(
        query, ResultTypeType.RESULTS, BigInteger.valueOf(query.getPageSize()));
  }

  private GetFeatureType buildGetFeatureRequest(
      Query query, ResultTypeType resultType, BigInteger maxFeatures)
      throws UnsupportedQueryException {
    List<ContentType> contentTypes = getContentTypesFromQuery(query);
    List<QueryType> queries = new ArrayList<>();

    for (Entry<QName, WfsFilterDelegate> filterDelegateEntry : featureTypeFilters.entrySet()) {
      if (contentTypes.isEmpty()
          || isFeatureTypeInQuery(contentTypes, filterDelegateEntry.getKey().getLocalPart())) {
        QueryType wfsQuery = new QueryType();
        wfsQuery.setTypeName(Collections.singletonList(filterDelegateEntry.getKey()));
        if (StringUtils.isNotBlank(srsName)) {
          wfsQuery.setSrsName(srsName);
        }
        FilterType filter = filterAdapter.adapt(query, filterDelegateEntry.getValue());
        if (filter != null) {
          if (areAnyFiltersSet(filter)) {
            wfsQuery.setFilter(filter);
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
      getFeatureType.setMaxFeatures(maxFeatures);
      getFeatureType.getQuery().addAll(queries);
      getFeatureType.setService(Wfs11Constants.WFS);
      getFeatureType.setVersion(Wfs11Constants.VERSION_1_1_0);
      getFeatureType.setResultType(resultType);
      logMessage(getFeatureType);
      return getFeatureType;
    } else {
      throw new UnsupportedQueryException(
          "Unable to build query. No filters could be created from query criteria.");
    }
  }

  private boolean areAnyFiltersSet(FilterType filter) {
    return filter != null
        && (filter.isSetComparisonOps()
            || filter.isSetLogicOps()
            || filter.isSetSpatialOps()
            || filter.isSetId());
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

    ServiceReference[] refs;
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

  @Override
  public Set<ContentType> getContentTypes() {
    Set<QName> typeNames = featureTypeFilters.keySet();
    return typeNames
        .stream()
        .map(featureName -> new ContentTypeImpl(featureName.getLocalPart(), getVersion()))
        .collect(toSet());
  }

  @Override
  public void maskId(String newSourceId) {
    final String methodName = "maskId";
    LOGGER.trace("ENTERING: {} with sourceId = {}", methodName, newSourceId);

    if (newSourceId != null) {
      super.maskId(newSourceId);
    }

    LOGGER.trace("EXITING: {}", methodName);
  }

  @Override
  public String getDescription() {
    return DESCRIBABLE_PROPERTIES.getProperty(DESCRIPTION);
  }

  @Override
  public String getOrganization() {
    return DESCRIBABLE_PROPERTIES.getProperty(ORGANIZATION);
  }

  @Override
  public String getTitle() {
    return DESCRIBABLE_PROPERTIES.getProperty(TITLE);
  }

  @Override
  public String getVersion() {
    if (StringUtils.isNotBlank(wfsVersion)) {
      return wfsVersion;
    }
    return DESCRIBABLE_PROPERTIES.getProperty(VERSION);
  }

  @Override
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments) {
    String html =
        "<html><script type=\"text/javascript\">window.location.replace(\""
            + uri
            + "\");</script></html>";

    Resource resource =
        new ResourceImpl(
            IOUtils.toInputStream(html, StandardCharsets.UTF_8),
            MediaType.TEXT_HTML,
            getId() + " Resource");

    return new ResourceResponseImpl(resource);
  }

  @Override
  public Set<String> getSupportedSchemes() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getOptions(Metacard metacard) {
    return Collections.emptySet();
  }

  public String getWfsUrl() {
    return wfsUrl;
  }

  public void setWfsUrl(String wfsUrl) {
    this.wfsUrl = wfsUrl;
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

  public void setMetacardTypeEnhancers(List<MetacardTypeEnhancer> metacardTypeEnhancers) {
    this.metacardTypeEnhancers = metacardTypeEnhancers;
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

  private String handleWebApplicationException(WebApplicationException wae) {
    Response response = wae.getResponse();
    WfsException wfsException = new WfsResponseExceptionMapper().fromResponse(response);

    return "Error received from WFS Server " + getId() + "\n" + wfsException.getMessage();
  }

  private String handleClientException(Exception ce) {
    String msg;
    Throwable cause = ce.getCause();
    String sourceId = getId();
    if (cause instanceof WebApplicationException) {
      msg = handleWebApplicationException((WebApplicationException) cause);
    } else if (cause instanceof IllegalArgumentException) {
      msg =
          WFS_ERROR_MESSAGE
              + SOURCE_MSG
              + sourceId
              + "'. The URI '"
              + getWfsUrl()
              + "' does not specify a valid protocol or could not be correctly parsed. "
              + ce.getMessage();
    } else if (cause instanceof SSLHandshakeException) {
      msg =
          WFS_ERROR_MESSAGE
              + SOURCE_MSG
              + sourceId
              + "' with URL '"
              + getWfsUrl()
              + "': "
              + ce.getMessage();
    } else if (cause instanceof ConnectException) {
      msg = WFS_ERROR_MESSAGE + SOURCE_MSG + sourceId + "' may not be running.\n" + ce.getMessage();
    } else {
      msg = WFS_ERROR_MESSAGE + SOURCE_MSG + sourceId + "'\n" + ce;
    }
    LOGGER.debug(msg, ce);
    return msg;
  }

  private void logMessage(GetFeatureType getFeature) {
    if (LOGGER.isDebugEnabled()) {
      try {
        StringWriter writer = new StringWriter();
        JAXBContext contextObj = JAXBContext.newInstance(GetFeatureType.class);

        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        marshallerObj.marshal(new ObjectFactory().createGetFeature(getFeature), writer);
        LOGGER.debug("WfsSource {}: {}", getId(), writer);
      } catch (JAXBException e) {
        LOGGER.debug("An error occurred debugging the GetFeature request", e);
      }
    }
  }

  private void debugResult(Result result) {
    if (LOGGER.isDebugEnabled() && result != null && result.getMetacard() != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("\nid:\t").append(result.getMetacard().getId());
      sb.append("\nmetacardType:\t").append(result.getMetacard().getMetacardType());
      if (result.getMetacard().getMetacardType() != null) {
        sb.append("\nmetacardType name:\t")
            .append(result.getMetacard().getMetacardType().getName());
      }
      sb.append("\ncontentType:\t").append(result.getMetacard().getContentTypeName());
      sb.append("\ntitle:\t").append(result.getMetacard().getTitle());
      sb.append("\nsource:\t").append(result.getMetacard().getSourceId());
      sb.append("\nmetadata:\t").append(result.getMetacard().getMetadata());
      sb.append("\nlocation:\t").append(result.getMetacard().getLocation());

      LOGGER.debug("Transform complete. Metacard: {}", sb);
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

  public void setForcedFeatureType(String featureType) {
    this.forcedFeatureType = featureType;
  }

  public void setCoordinateOrder(String coordinateOrder) {
    this.coordinateOrder = coordinateOrder;
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

  /**
   * Callback class to check the Availability of the WfsSource.
   *
   * <p>NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
   * have an AvailabilityTask that cached each Source's availability. Until that is done, allow the
   * command to handle the logic of managing availability.
   *
   * @author kcwire
   */
  private class WfsSourceAvailabilityCommand implements AvailabilityCommand {
    private void availabilityChangedToAvailable() {
      LOGGER.debug("WFS source {} is available.", getId());

      for (SourceMonitor monitor : WfsSource.this.sourceMonitors) {
        LOGGER.debug("Notifying source monitor that WFS source {} is available.", getId());
        monitor.setAvailable();
      }
    }

    private void availabilityChangedToUnavailable() {
      LOGGER.debug("WFS source {} is unavailable.", getId());

      for (SourceMonitor monitor : WfsSource.this.sourceMonitors) {
        LOGGER.debug("Notifying source monitor that WFS source {} is unavailable.", getId());
        monitor.setUnavailable();
      }
    }

    @Override
    public boolean isAvailable() {
      LOGGER.debug("Checking availability for source {} ", getId());
      boolean oldAvailability = WfsSource.this.isAvailable();
      // Simple "ping" to ensure the source is responding
      boolean newAvailability = (null != getCapabilities());
      if (oldAvailability != newAvailability) {
        // If the source becomes available, configure it.
        if (newAvailability) {
          availabilityChangedToAvailable();
          configureWfsFeatures();
        } else {
          availabilityChangedToUnavailable();
        }
      }
      return newAvailability;
    }
  }

  public void setFeatureTransformationService(
      FeatureTransformationService featureTransformationService) {
    this.featureTransformationService = featureTransformationService;
  }

  public String getCoordinateOrder() {
    return this.coordinateOrder;
  }

  private CoordinateStrategy getCoordinateStrategy() {
    return LAT_LON_ORDER.equals(coordinateOrder)
        ? new LatLonCoordinateStrategy()
        : new LonLatCoordinateStrategy();
  }

  public void setWfsMetacardTypeRegistry(WfsMetacardTypeRegistry wfsMetacardTypeRegistry) {
    this.wfsMetacardTypeRegistry = wfsMetacardTypeRegistry;
  }

  public void setMetacardMappers(final List<MetacardMapper> metacardMappers) {
    this.metacardMappers = metacardMappers;
  }
}
