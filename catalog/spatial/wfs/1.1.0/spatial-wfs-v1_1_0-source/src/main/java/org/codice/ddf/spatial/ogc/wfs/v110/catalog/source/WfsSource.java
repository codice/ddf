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
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityServiceException;
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
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
import net.opengis.wfs.v_1_1_0.WFSCapabilitiesType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.cxf.ClientKeyInfo;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.catalog.common.ContentTypeFilterDelegate;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.AbstractWfsSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.converter.FeatureConverterFactoryV110;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.converter.impl.GenericFeatureConverterWfs11;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.source.reader.FeatureCollectionMessageBodyReaderWfs11;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.source.reader.XmlSchemaMessageBodyReaderWfs11;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a Federated and Connected source implementation for OGC WFS servers. */
@SuppressWarnings({"unused", "WeakerAccess"})
public class WfsSource extends AbstractWfsSource {

  static final int WFS_MAX_FEATURES_RETURNED = 1000;

  static final int WFS_QUERY_PAGE_SIZE_MULTIPLIER = 3;

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

  private List<MetacardMapper> metacardToFeatureMappers;

  private String wfsUrl;

  private String wfsVersion;

  private Map<QName, WfsFilterDelegate> featureTypeFilters = new HashMap<>();

  private String username;

  private String password;

  private String coordinateOrder = GeospatialUtil.LAT_LON_ORDER;

  private Boolean disableCnCheck = Boolean.FALSE;

  private FilterAdapter filterAdapter;

  private BundleContext context;

  private Map<String, ServiceRegistration> metacardTypeServiceRegistrations = new HashMap<>();

  private String[] nonQueryableProperties;

  private List<FeatureConverterFactoryV110> featureConverterFactories;

  private Integer pollInterval;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private String forceSpatialFilter = NO_FORCED_SPATIAL_FILTER;

  private List<String> supportedGeoFilters;

  private ScheduledExecutorService scheduler;

  private ScheduledFuture<?> availabilityPollFuture;

  private AvailabilityTask availabilityTask;

  private Set<SourceMonitor> sourceMonitors = new HashSet<>();

  private SecureCxfClientFactory<Wfs> factory;

  private String configurationPid;

  private String forcedFeatureType;

  private FeatureCollectionMessageBodyReaderWfs11 featureCollectionReader;

  private List<MetacardTypeEnhancer> metacardTypeEnhancers;

  private String srsName;

  private boolean allowRedirects;

  static {
    try (InputStream properties =
        WfsSource.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(properties);
    } catch (IOException e) {
      LOGGER.error(
          "failed to load describable properties files: {}", DESCRIBABLE_PROPERTIES_FILE, e);
    }
  }

  @SuppressWarnings("unchecked")
  public WfsSource(
      FilterAdapter filterAdapter,
      BundleContext context,
      AvailabilityTask task,
      SecureCxfClientFactory factory,
      EncryptionService encryptionService,
      List<MetacardTypeEnhancer> metacardTypeEnhancers)
      throws SecurityServiceException {

    this.filterAdapter = filterAdapter;
    this.context = context;
    this.availabilityTask = task;
    this.factory = factory;
    this.encryptionService = encryptionService;
    this.metacardToFeatureMappers = Collections.emptyList();
    this.metacardTypeEnhancers = metacardTypeEnhancers;
    initProviders();
    configureWfsFeatures();
  }

  public WfsSource(EncryptionService encryptionService) {
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("wfsSourceThread"));
    this.encryptionService = encryptionService;
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

  public void destroy(int code) {
    unregisterAllMetacardTypes();
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
  public void refresh(Map<String, Object> configuration) throws SecurityServiceException {

    LOGGER.trace("WfsSource {}: Refresh called", getId());
    String url = (String) configuration.get(WFSURL_KEY);
    String coordOrder = (String) configuration.get(COORDINATE_ORDER_KEY);
    String passwordValue = (String) configuration.get(PASSWORD_KEY);
    String usernameValue = (String) configuration.get(USERNAME_KEY);
    Boolean disableCnCheckProp = (Boolean) configuration.get(DISABLE_CN_CHECK_KEY);
    String id = (String) configuration.get(ID_KEY);
    Boolean allowRedirects = (Boolean) configuration.get(ALLOW_REDIRECTS_KEY);
    if (hasSourceIdChanged(id)) {
      setId(id);
      configureWfsFeatures();
    }

    setConnectionTimeout((Integer) configuration.get(CONNECTION_TIMEOUT_KEY));
    setReceiveTimeout((Integer) configuration.get(RECEIVE_TIMEOUT_KEY));
    setSrsName((String) configuration.get(SRS_NAME_KEY));

    this.nonQueryableProperties = (String[]) configuration.get(NON_QUERYABLE_PROPS_KEY);

    Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_KEY);

    if (hasWfsUrlChanged(url)
        || hasDisableCnCheckChanged(disableCnCheckProp)
        || hasUsernameChanged(usernameValue)
        || hasPasswordChanged(passwordValue)
        || hasAllowRedirectsChanged(allowRedirects)) {
      this.wfsUrl = url;
      this.password = encryptionService.decryptValue(passwordValue);
      this.username = usernameValue;
      this.disableCnCheck = disableCnCheckProp;
      this.coordinateOrder = coordOrder;
      this.allowRedirects = allowRedirects;
      createClientFactory();
      configureWfsFeatures();
    } else {
      // Only need to update the supportedGeos if we don't reconnect.
      String spatialFilter = (String) configuration.get(SPATIAL_FILTER_KEY);
      if (!StringUtils.equals(forceSpatialFilter, spatialFilter)) {
        List<String> geoFilters = new ArrayList<>();
        if (NO_FORCED_SPATIAL_FILTER.equals(spatialFilter)) {
          geoFilters.addAll(supportedGeoFilters);
        } else {
          geoFilters.add(spatialFilter);
        }
        for (WfsFilterDelegate delegate : featureTypeFilters.values()) {
          delegate.setSupportedGeoFilters(geoFilters);
        }
      }
    }

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
  @SuppressWarnings("unchecked")
  private void createClientFactory() {
    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
      factory =
          new SecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              this.allowRedirects,
              null,
              null,
              username,
              password);
    } else if (StringUtils.isNotBlank(getCertAlias())
        && StringUtils.isNotBlank(getKeystorePath())) {
      factory =
          new SecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              this.allowRedirects,
              null,
              null,
              new ClientKeyInfo(getCertAlias(), getKeystorePath()),
              getSslProtocol());
    } else {
      factory =
          new SecureCxfClientFactory(
              wfsUrl,
              Wfs.class,
              initProviders(),
              new MarkableStreamInterceptor(),
              this.disableCnCheck,
              this.allowRedirects);
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

    featureCollectionReader = new FeatureCollectionMessageBodyReaderWfs11();

    return Arrays.asList(
        provider,
        new WfsResponseExceptionMapper(),
        new XmlSchemaMessageBodyReaderWfs11(),
        featureCollectionReader);
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

  private void availabilityChanged(boolean isAvailable) {
    if (isAvailable) {
      LOGGER.debug("WFS source {} is available.", getId());
    } else {
      LOGGER.debug("WFS source {} is unavailable.", getId());
    }

    for (SourceMonitor monitor : this.sourceMonitors) {
      if (isAvailable) {
        LOGGER.debug("Notifying source monitor that WFS source {} is available.", getId());
        monitor.setAvailable();
      } else {
        LOGGER.debug("Notifying source monitor that WFS source {} is unavailable.", getId());
        monitor.setUnavailable();
      }
    }
  }

  private WFSCapabilitiesType getCapabilities() throws SecurityServiceException {
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

  private void configureWfsFeatures() throws SecurityServiceException {
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
    supportedGeoFilters = new ArrayList<>();

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

  private void buildFeatureFilters(List<FeatureTypeType> featureTypes, List<String> supportedGeo)
      throws SecurityServiceException {
    Wfs wfs = factory.getClient();

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
          schema =
              wfs.describeFeatureType(
                  new DescribeFeatureTypeRequest(
                      new QName(
                          featureTypeType.getName().getNamespaceURI(),
                          featureTypeType.getName().getLocalPart(),
                          "")));
        }

        if (schema != null) {
          MetacardTypeRegistration registration =
              createFeatureMetacardTypeRegistration(featureTypeType, ftSimpleName, schema);
          mcTypeRegs.put(ftSimpleName, registration);
          FeatureMetacardType featureMetacardType = registration.getFtMetacard();
          lookupFeatureConverter(ftSimpleName, featureMetacardType, registration.getSrs());

          this.featureTypeFilters.put(
              featureMetacardType.getFeatureType(),
              new WfsFilterDelegate(featureMetacardType, supportedGeo));
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

  private void lookupFeatureConverter(
      String ftSimpleName, FeatureMetacardType ftMetacard, String srs) {
    FeatureConverter featureConverter = null;

    /**
     * The list of feature converter factories injected into this class is a live list. So, feature
     * converter factories can be added and removed from the system while running.
     */
    if (org.apache.commons.collections.CollectionUtils.isNotEmpty(featureConverterFactories)) {
      for (FeatureConverterFactoryV110 featureConverterFactory : featureConverterFactories) {
        if (ftSimpleName.equalsIgnoreCase(featureConverterFactory.getFeatureType())) {
          featureConverter = featureConverterFactory.createConverter();
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

      if (featurePropertyToMetacardAttributeMapper != null) {
        featureConverter =
            new GenericFeatureConverterWfs11(featurePropertyToMetacardAttributeMapper);
        LOGGER.debug(
            "WFS Source {}: Created {} for feature type {} with feature property to metacard attribute mapper.",
            getId(),
            featureConverter.getClass().getSimpleName(),
            ftSimpleName);
      } else {
        featureConverter = new GenericFeatureConverterWfs11(srs);
        featureConverter.setCoordinateOrder(coordinateOrder);
        LOGGER.debug(
            "WFS Source {}: Created {} for feature type {} with no feature property to metacard attribute mapper.",
            getId(),
            featureConverter.getClass().getSimpleName(),
            ftSimpleName);
      }
    }

    featureConverter.setSourceId(getId());
    featureConverter.setMetacardType(ftMetacard);
    featureConverter.setWfsUrl(wfsUrl);

    // Add the Feature Type name as an alias for xstream
    LOGGER.debug(
        "Registering feature converter {} for feature type {}.",
        featureConverter.getClass().getSimpleName(),
        ftSimpleName);
    featureCollectionReader.registerConverter(featureConverter);
  }

  private MetacardTypeRegistration createFeatureMetacardTypeRegistration(
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
                ? Arrays.asList(nonQueryableProperties)
                : new ArrayList<>(),
            Wfs11Constants.GML_3_1_1_NAMESPACE,
            metacardTypeEnhancer);

    Dictionary<String, Object> props = new DictionaryMap<>();
    props.put(Metacard.CONTENT_TYPE, new String[] {ftName});

    LOGGER.debug("WfsSource {}: Registering MetacardType: {}", getId(), ftName);

    return new MetacardTypeRegistration(ftMetacard, props, featureTypeType.getDefaultSRS());
  }

  private MetacardMapper lookupMetacardAttributeToFeaturePropertyMapper(QName featureType) {
    MetacardMapper metacardAttributeToFeaturePropertyMapper = null;

    if (this.metacardToFeatureMappers != null) {
      for (MetacardMapper mapper : this.metacardToFeatureMappers) {
        if (mapper != null && StringUtils.equals(mapper.getFeatureType(), featureType.toString())) {
          logFeatureType(featureType, "Found {} for feature type {}.");
          metacardAttributeToFeaturePropertyMapper = mapper;
          break;
        }
      }

      if (metacardAttributeToFeaturePropertyMapper == null) {
        logFeatureType(featureType, "Unable to find a {} for feature type {}.");
      }
    }

    return metacardAttributeToFeaturePropertyMapper;
  }

  private void logFeatureType(QName featureType, String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message, MetacardMapper.class.getSimpleName(), featureType);
    }
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

    SourceResponseImpl simpleResponse;

    int origPageSize = query.getPageSize();
    if (origPageSize <= 0 || origPageSize > WFS_MAX_FEATURES_RETURNED) {
      origPageSize = WFS_MAX_FEATURES_RETURNED;
    }
    QueryImpl modifiedQuery = new QueryImpl(query);

    int pageNumber = query.getStartIndex() / origPageSize + 1;

    int modifiedPageSize =
        Math.min(
            pageNumber * origPageSize * WFS_QUERY_PAGE_SIZE_MULTIPLIER, WFS_MAX_FEATURES_RETURNED);
    LOGGER.debug("WFS Source {}: modified page size = {}", getId(), modifiedPageSize);
    modifiedQuery.setPageSize(modifiedPageSize);

    GetFeatureType getFeature = buildGetFeatureRequest(modifiedQuery);

    try {
      LOGGER.debug("WFS Source {}: Sending query ...", getId());
      WfsFeatureCollection featureCollection = wfs.getFeature(getFeature);

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
              (origPageSize * pageNumber) + query.getStartIndex(),
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
      Long totalHits = (long) featureCollection.getFeatureMembers().size();
      simpleResponse = new SourceResponseImpl(request, results, totalHits);
    } catch (WfsException wfse) {
      LOGGER.debug(WFS_ERROR_MESSAGE, wfse);
      throw new UnsupportedQueryException("Error received from WFS Server", wfse);
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }

    return simpleResponse;
  }

  private GetFeatureType buildGetFeatureRequest(Query query) throws UnsupportedQueryException {
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
      getFeatureType.setMaxFeatures(BigInteger.valueOf(query.getPageSize()));
      getFeatureType.getQuery().addAll(queries);
      getFeatureType.setService(Wfs11Constants.WFS);
      getFeatureType.setVersion(Wfs11Constants.VERSION_1_1_0);
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

  private void unregisterAllMetacardTypes() {
    metacardTypeServiceRegistrations
        .values()
        .stream()
        .filter(Objects::nonNull)
        .forEach(ServiceRegistration::unregister);
    metacardTypeServiceRegistrations.clear();
  }

  @Override
  public Set<ContentType> getContentTypes() {
    Set<QName> typeNames = featureTypeFilters.keySet();
    return typeNames
        .stream()
        .map(featureName -> new ContentTypeImpl(featureName.getLocalPart(), getVersion()))
        .collect(Collectors.toSet());
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
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
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

  public void setFeatureConverterFactoryList(List<FeatureConverterFactoryV110> factories) {
    this.featureConverterFactories = factories;
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
        LOGGER.debug("WfsSource {}: {}", getId(), writer.toString());
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

      LOGGER.debug("Transform complete. Metacard: {}", sb.toString());
    }
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

  public List<MetacardMapper> getMetacardToFeatureMapper() {
    return this.metacardToFeatureMappers;
  }

  public void setMetacardToFeatureMapper(List<MetacardMapper> mappers) {
    this.metacardToFeatureMappers = mappers;
  }

  private boolean hasWfsUrlChanged(String wfsUrl) {
    return !StringUtils.equals(this.wfsUrl, wfsUrl);
  }

  private boolean hasUsernameChanged(String usernameValue) {
    return !StringUtils.equals(this.username, usernameValue);
  }

  private boolean hasPasswordChanged(String passwordValue) {
    return !StringUtils.equals(this.password, passwordValue);
  }

  private boolean hasAllowRedirectsChanged(boolean allowRedirects) {
    return this.allowRedirects != allowRedirects;
  }

  private boolean hasSourceIdChanged(String id) {
    return !StringUtils.equals(getId(), id);
  }

  private boolean hasDisableCnCheckChanged(Boolean disableCnCheck) {
    return this.disableCnCheck != disableCnCheck;
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

    @Override
    public boolean isAvailable() {
      LOGGER.debug("Checking availability for source {} ", getId());
      boolean oldAvailability = WfsSource.this.isAvailable();
      boolean newAvailability = false;
      try {
        // Simple "ping" to ensure the source is responding
        newAvailability = (null != getCapabilities());
        if (oldAvailability != newAvailability) {
          availabilityChanged(newAvailability);
          // If the source becomes available, configure it.
          if (newAvailability) {
            configureWfsFeatures();
          }
        }
      } catch (SecurityServiceException sse) {
        LOGGER.info("Could not get a client to connect to the endpointUrl.", sse);
      }
      return newAvailability;
    }
  }
}
