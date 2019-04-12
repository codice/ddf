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
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityManager;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.ConnectException;
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
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
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
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.reader.GetRecordsMessageBodyReader;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractCswSource provides a DDF {@link FederatedSource} and {@link ConnectedSource} for CSW
 * 2.0.2 services.
 */
public abstract class AbstractCswSource extends MaskableImpl
    implements FederatedSource, ConnectedSource, ConfiguredService {

  public static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

  @SuppressWarnings("squid:S2068")
  protected static final String PASSWORD_PROPERTY = "password";

  protected static final String CSW_SERVER_ERROR = "Error received from CSW server.";
  protected static final String CSWURL_PROPERTY = "cswUrl";
  protected static final String ID_PROPERTY = "id";
  protected static final String USERNAME_PROPERTY = "username";
  protected static final String CERT_ALIAS_PROPERTY = "certAlias";
  protected static final String KEYSTORE_PATH_PROPERTY = "keystorePath";
  protected static final String SSL_PROTOCOL_PROPERTY = "sslProtocol";
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
  protected static final String SECURITY_ATTRIBUTES_PROPERTY = "securityAttributeStrings";
  protected static final String EVENT_SERVICE_ADDRESS = "eventServiceAddress";
  protected static final String REGISTER_FOR_EVENTS = "registerForEvents";
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCswSource.class);
  private static final String DEFAULT_CSW_TRANSFORMER_ID = "csw";
  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  @SuppressWarnings("squid:S1845")
  private static final String DESCRIPTION = "description";

  private static final String ORGANIZATION = "organization";
  private static final String VERSION = "version";
  private static final String TITLE = "name";
  private static final int CONTENT_TYPE_SAMPLE_SIZE = 50;
  private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

  private static final String BYTES_SKIPPED = "bytes-skipped";

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";
  private static final String ERROR_ID_PRODUCT_RETRIEVAL = "Error retrieving resource for ID: %s";
  private static final Security SECURITY = Security.getInstance();
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

  protected final ClientFactoryFactory clientFactoryFactory;

  protected String configurationPid;
  protected CswSourceConfiguration cswSourceConfiguration;
  protected CswFilterDelegate cswFilterDelegate;
  protected Converter cswTransformConverter;
  protected String forceSpatialFilter = NO_FORCE_SPATIAL_FILTER;
  protected ScheduledFuture<?> availabilityPollFuture;
  protected SecurityManager securityManager;
  protected FilterBuilder filterBuilder;
  protected FilterAdapter filterAdapter;
  protected CapabilitiesType capabilities;
  protected SecureCxfClientFactory<Csw> factory;
  protected SecureCxfClientFactory<CswSubscribe> subscribeClientFactory;
  protected CswJAXBElementProvider<GetRecordsType> getRecordsTypeProvider;
  protected QueryFilterTransformerProvider cswQueryFilterTransformerProvider;
  protected List<String> jaxbElementClassNames = new ArrayList<>();
  protected Map<String, String> jaxbElementClassMap = new HashMap<>();
  protected String filterlessSubscriptionId = null;
  private EncryptionService encryptionService;
  private Set<SourceMonitor> sourceMonitors = new HashSet<>();
  private Map<String, ContentType> contentTypes = new ConcurrentHashMap<>();
  private ResourceReader resourceReader;
  private DomainType supportedOutputSchemas;
  private Set<ElementSetType> detailLevels;
  private BundleContext context;

  @SuppressWarnings("squid:S1845")
  private String description = null;

  private String cswVersion;
  private ScheduledExecutorService scheduler;
  private AvailabilityTask availabilityTask;
  private boolean isConstraintCql;

  /**
   * Instantiates a CswSource. This constructor is for unit tests
   *
   * @param context The {@link BundleContext} from the OSGi Framework
   * @param cswSourceConfiguration the configuration of this source
   * @param provider transform provider to transform results
   * @param clientFactoryFactory client factory already configured for this source
   */
  public AbstractCswSource(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      Converter provider,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
    this.context = context;
    this.cswSourceConfiguration = cswSourceConfiguration;
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("abstractCswSourceThread"));
    this.clientFactoryFactory = clientFactoryFactory;
    setConsumerMap();
  }

  /** @deprecated */
  @Deprecated
  public AbstractCswSource(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      Converter provider,
      ClientFactoryFactory clientFactoryFactory) {
    this(context, cswSourceConfiguration, provider, clientFactoryFactory, null);
  }

  /** Instantiates a CswSource. */
  public AbstractCswSource(
      EncryptionService encryptionService, ClientFactoryFactory clientFactoryFactory) {
    this.encryptionService = encryptionService;
    cswSourceConfiguration = new CswSourceConfiguration(encryptionService);
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("abstractCswSourceThread"));
    this.clientFactoryFactory = clientFactoryFactory;
  }

  /** @deprecated */
  @Deprecated
  public AbstractCswSource() {
    this(null, null);
  }

  private static JAXBContext initJaxbContext() {

    JAXBContext jaxbContext = null;

    String contextPath =
        StringUtils.join(
            new String[] {
              CswConstants.OGC_CSW_PACKAGE,
              CswConstants.OGC_FILTER_PACKAGE,
              CswConstants.OGC_GML_PACKAGE,
              CswConstants.OGC_OWS_PACKAGE
            },
            ":");

    try {
      jaxbContext = JAXBContext.newInstance(contextPath, AbstractCswSource.class.getClassLoader());
    } catch (JAXBException e) {
      LOGGER.info("Failed to initialize JAXBContext", e);
    }

    return jaxbContext;
  }

  /** Initializes the CswSource by connecting to the Server */
  public void init() {
    setConsumerMap();
    LOGGER.debug("{}: Entering init()", cswSourceConfiguration.getId());
    initClientFactory();
    setupAvailabilityPoll();

    configureEventService();
  }

  protected void initClientFactory() {
    if (StringUtils.isNotBlank(cswSourceConfiguration.getUsername())
        && StringUtils.isNotBlank(cswSourceConfiguration.getPassword())) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getCswUrl(),
              Csw.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout(),
              cswSourceConfiguration.getUsername(),
              cswSourceConfiguration.getPassword());
    } else if (StringUtils.isNotBlank(cswSourceConfiguration.getCertAlias())
        && StringUtils.isNotBlank(cswSourceConfiguration.getKeystorePath())) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getCswUrl(),
              Csw.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout(),
              cswSourceConfiguration.getCertAlias(),
              cswSourceConfiguration.getKeystorePath(),
              cswSourceConfiguration.getSslProtocol());
    } else {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getCswUrl(),
              Csw.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout());
    }
  }

  protected void initSubscribeClientFactory() {
    if (StringUtils.isNotBlank(cswSourceConfiguration.getUsername())
        && StringUtils.isNotBlank(cswSourceConfiguration.getPassword())) {
      subscribeClientFactory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getEventServiceAddress(),
              CswSubscribe.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout(),
              cswSourceConfiguration.getUsername(),
              cswSourceConfiguration.getPassword());
    } else if (StringUtils.isNotBlank(cswSourceConfiguration.getCertAlias())
        && StringUtils.isNotBlank(cswSourceConfiguration.getKeystorePath())) {
      subscribeClientFactory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getCswUrl(),
              CswSubscribe.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout(),
              cswSourceConfiguration.getCertAlias(),
              cswSourceConfiguration.getKeystorePath(),
              cswSourceConfiguration.getSslProtocol());
    } else {
      subscribeClientFactory =
          clientFactoryFactory.getSecureCxfClientFactory(
              cswSourceConfiguration.getEventServiceAddress(),
              CswSubscribe.class,
              initProviders(cswTransformConverter, cswSourceConfiguration),
              null,
              cswSourceConfiguration.getDisableCnCheck(),
              false,
              cswSourceConfiguration.getConnectionTimeout(),
              cswSourceConfiguration.getReceiveTimeout());
    }
  }

  /** Sets the consumerMap to manipulate cswSourceConfiguration when refresh is called. */
  private void setConsumerMap() {

    consumerMap.put(ID_PROPERTY, value -> setId((String) value));

    consumerMap.put(PASSWORD_PROPERTY, value -> cswSourceConfiguration.setPassword((String) value));

    consumerMap.put(USERNAME_PROPERTY, value -> cswSourceConfiguration.setUsername((String) value));

    consumerMap.put(
        CERT_ALIAS_PROPERTY, value -> cswSourceConfiguration.setCertAlias((String) value));

    consumerMap.put(
        KEYSTORE_PATH_PROPERTY, value -> cswSourceConfiguration.setKeystorePath((String) value));

    consumerMap.put(
        SSL_PROTOCOL_PROPERTY, value -> cswSourceConfiguration.setSslProtocol((String) value));

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

    consumerMap.put(
        SECURITY_ATTRIBUTES_PROPERTY,
        value -> cswSourceConfiguration.setSecurityAttributes((String[]) value));

    consumerMap.put(
        REGISTER_FOR_EVENTS, value -> cswSourceConfiguration.setRegisterForEvents((Boolean) value));

    consumerMap.put(
        EVENT_SERVICE_ADDRESS,
        value -> cswSourceConfiguration.setEventServiceAddress((String) value));

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
    if (!newCswUrlProp.equals(cswSourceConfiguration.getCswUrl())) {
      cswSourceConfiguration.setCswUrl(newCswUrlProp);
      LOGGER.debug("Setting url : {}.", newCswUrlProp);
    }
  }

  private void forcePoll() {
    if (availabilityPollFuture != null) {
      availabilityPollFuture.cancel(true);
    }
    setupAvailabilityPoll();
  }

  protected List<Object> initProviders(
      Converter cswTransformProvider, CswSourceConfiguration cswSourceConfiguration) {
    getRecordsTypeProvider = new CswJAXBElementProvider<>();
    getRecordsTypeProvider.setMarshallAsJaxbElement(true);

    // Adding class names that need to be marshalled/unmarshalled to
    // jaxbElementClassNames list
    jaxbElementClassNames.add(GetRecordsType.class.getName());
    jaxbElementClassNames.add(CapabilitiesType.class.getName());
    jaxbElementClassNames.add(GetCapabilitiesType.class.getName());
    jaxbElementClassNames.add(GetRecordsResponseType.class.getName());
    jaxbElementClassNames.add(AcknowledgementType.class.getName());

    getRecordsTypeProvider.setJaxbElementClassNames(jaxbElementClassNames);

    // Adding map entry of <Class Name>,<Qualified Name> to jaxbElementClassMap
    String expandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS).toString();
    String message = "{} expanded name: {}";
    LOGGER.debug(message, CswConstants.GET_RECORDS, expandedName);
    jaxbElementClassMap.put(GetRecordsType.class.getName(), expandedName);

    String getCapsExpandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.GET_CAPABILITIES, expandedName);
    jaxbElementClassMap.put(GetCapabilitiesType.class.getName(), getCapsExpandedName);

    String capsExpandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.CAPABILITIES, capsExpandedName);
    jaxbElementClassMap.put(CapabilitiesType.class.getName(), capsExpandedName);

    String caps201ExpandedName =
        new QName("http://www.opengis.net/cat/csw", CswConstants.CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.CAPABILITIES, caps201ExpandedName);
    jaxbElementClassMap.put(CapabilitiesType.class.getName(), caps201ExpandedName);

    String acknowledgmentName =
        new QName("http://www.opengis.net/cat/csw/2.0.2", "Acknowledgement").toString();
    jaxbElementClassMap.put(AcknowledgementType.class.getName(), acknowledgmentName);
    getRecordsTypeProvider.setJaxbElementClassMap(jaxbElementClassMap);

    GetRecordsMessageBodyReader grmbr =
        new GetRecordsMessageBodyReader(cswTransformProvider, cswSourceConfiguration);

    return Arrays.asList(getRecordsTypeProvider, new CswResponseExceptionMapper(), grmbr);
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

    // if the event service address has changed attempt to remove the subscription before changing
    // to the new event service address
    if (cswSourceConfiguration.getEventServiceAddress() != null
        && cswSourceConfiguration.isRegisterForEvents()
        && !cswSourceConfiguration
            .getEventServiceAddress()
            .equals(configuration.get(EVENT_SERVICE_ADDRESS))) {
      removeEventServiceSubscription();
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

    initClientFactory();
    configureEventService();

    forcePoll();
  }

  private Map<String, Object> filter(Map<String, Object> configuration) {
    Map<String, Object> filteredConfiguration = new HashMap<>();

    // Filter out Blank Strings and null Integers and Booleans
    filteredConfiguration.putAll(
        configuration
            .entrySet()
            .stream()
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
      // Schedule the availability check every 1 second. The actually call to
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
    Subject subject = (Subject) queryRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
    Csw csw = factory.getClientForSubject(subject);

    return query(queryRequest, ElementSetType.FULL, null, csw);
  }

  protected SourceResponse query(
      QueryRequest queryRequest, ElementSetType elementSetName, List<QName> elementNames, Csw csw)
      throws UnsupportedQueryException {

    Query query = queryRequest.getQuery();
    LOGGER.debug("{}: Received query:\n{}", cswSourceConfiguration.getId(), query);

    GetRecordsType getRecordsType =
        createGetRecordsRequest(queryRequest, elementSetName, elementNames);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{}: GetRecords request:\n {}",
          cswSourceConfiguration.getId(),
          getGetRecordsTypeAsXml(getRecordsType));
    }

    LOGGER.debug(
        "{}: Sending query to: {}",
        cswSourceConfiguration.getId(),
        cswSourceConfiguration.getCswUrl());

    List<Result> results;
    Long totalHits;

    try {

      CswRecordCollection cswRecordCollection = csw.getRecords(getRecordsType);

      if (cswRecordCollection == null) {
        throw new UnsupportedQueryException("Invalid results returned from server");
      }
      this.availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
      LOGGER.debug(
          "{}: Received [{}] record(s) of the [{}] record(s) matched from {}.",
          cswSourceConfiguration.getId(),
          cswRecordCollection.getNumberOfRecordsReturned(),
          cswRecordCollection.getNumberOfRecordsMatched(),
          cswSourceConfiguration.getCswUrl());

      results = createResults(cswRecordCollection);
      totalHits = cswRecordCollection.getNumberOfRecordsMatched();
    } catch (CswException cswe) {
      LOGGER.info(CSW_SERVER_ERROR, cswe);
      throw new UnsupportedQueryException(CSW_SERVER_ERROR, cswe);
    } catch (WebApplicationException wae) {
      String msg = handleWebApplicationException(wae);
      throw new UnsupportedQueryException(msg, wae);
    } catch (Exception ce) {
      String msg = handleClientException(ce);
      throw new UnsupportedQueryException(msg, ce);
    }

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
    if (StringUtils.isNotBlank(cswVersion)) {
      return cswVersion;
    }
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
    String username = cswSourceConfiguration.getUsername();
    String password = cswSourceConfiguration.getPassword();

    if (requestProperties != null) {
      serializableId = requestProperties.get(Core.ID);
      if (StringUtils.isNotBlank(username)) {
        requestProperties.put(USERNAME_PROPERTY, username);
        requestProperties.put(PASSWORD_PROPERTY, password);
      }
    }

    if (canRetrieveResourceById()) {
      // If no resource reader was found, retrieve the product through a GetRecordById request
      if (serializableId == null) {
        throw new ResourceNotFoundException(
            "Unable to retrieve resource because no metacard ID was found.");
      }
      String metacardId = serializableId.toString();
      LOGGER.debug("Retrieving resource for ID : {}", metacardId);
      ResourceResponse response = retrieveResourceById(requestProperties, metacardId);
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

  private ResourceResponse retrieveResourceById(
      Map<String, Serializable> requestProperties, String metacardId)
      throws ResourceNotFoundException {
    Csw csw =
        factory.getClientForSubject(
            (Subject) requestProperties.get(SecurityConstants.SECURITY_SUBJECT));
    GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setService(CswConstants.CSW);
    getRecordByIdRequest.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdRequest.setId(metacardId);

    String rangeValue = "";
    long requestedBytesToSkip = 0;
    if (requestProperties.containsKey(CswConstants.BYTES_TO_SKIP)) {
      requestedBytesToSkip = (Long) requestProperties.get(CswConstants.BYTES_TO_SKIP);
      rangeValue =
          String.format(
              "%s%s-",
              CswConstants.BYTES_EQUAL,
              requestProperties.get(CswConstants.BYTES_TO_SKIP).toString());
      LOGGER.debug("Range: {}", rangeValue);
    }
    CswRecordCollection recordCollection;
    try {
      recordCollection = csw.getRecordById(getRecordByIdRequest, rangeValue);

      Resource resource = recordCollection.getResource();
      if (resource != null) {

        long responseBytesSkipped = 0L;
        if (recordCollection.getResourceProperties().get(BYTES_SKIPPED) != null) {
          responseBytesSkipped = (Long) recordCollection.getResourceProperties().get(BYTES_SKIPPED);
        }
        alignStream(resource.getInputStream(), requestedBytesToSkip, responseBytesSkipped);

        return new ResourceResponseImpl(
            new ResourceImpl(
                new BufferedInputStream(resource.getInputStream()),
                resource.getMimeTypeValue(),
                FilenameUtils.getName(resource.getName())));
      } else {
        return null;
      }
    } catch (CswException | IOException e) {
      throw new ResourceNotFoundException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, metacardId), e);
    }
  }

  private void alignStream(InputStream in, long requestedBytesToSkip, long responseBytesSkipped)
      throws IOException {
    long misalignment = requestedBytesToSkip - responseBytesSkipped;

    if (misalignment == 0) {
      LOGGER.trace("Server responded with the correct byte range.");
      return;
    }

    try {
      if (requestedBytesToSkip > responseBytesSkipped) {
        LOGGER.debug(
            "Server returned incorrect byte range, skipping first [{}] bytes", misalignment);
        if (in.skip(misalignment) != misalignment) {
          throw new IOException(
              String.format("Input Stream could not be skipped %d bytes.", misalignment));
        }

      } else {
        throw new IOException("Server skipped more bytes than requested in the range header.");
      }
    } catch (IOException e) {
      /*In the event an IOException is thrown, the InputStream should be closed to prevent resource
      leakage. Otherwise the stream should be kept open to pass into the ResourceImpl constructor.*/
      IOUtils.closeQuietly(in);
      throw new IOException(
          String.format(
              "Unable to align input stream with the requested byteOffset of %d",
              requestedBytesToSkip));
    }
  }

  public void setCswUrl(String cswUrl) {
    LOGGER.debug("Setting cswUrl to {}", cswUrl);

    cswSourceConfiguration.setCswUrl(cswUrl);
  }

  public void setUsername(String username) {
    cswSourceConfiguration.setUsername(username);
  }

  public void setPassword(String password) {
    String updatedPassword = password;
    if (encryptionService != null) {
      updatedPassword = encryptionService.decryptValue(password);
    }
    cswSourceConfiguration.setPassword(updatedPassword);
  }

  public void setCertAlias(String certAlias) {
    cswSourceConfiguration.setCertAlias(certAlias);
  }

  public void setKeystorePath(String keystorePath) {
    cswSourceConfiguration.setKeystorePath(keystorePath);
  }

  public void setSslProtocol(String sslProtocol) {
    cswSourceConfiguration.setSslProtocol(sslProtocol);
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
    getRecordsType.setVersion(cswVersion);
    getRecordsType.setService(CswConstants.CSW);
    getRecordsType.setResultType(ResultType.RESULTS);
    getRecordsType.setStartPosition(BigInteger.valueOf(query.getStartIndex()));
    getRecordsType.setMaxRecords(BigInteger.valueOf(query.getPageSize()));
    getRecordsType.setOutputFormat(MediaType.APPLICATION_XML);
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
    ObjectFactory objectFactory = new ObjectFactory();
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

  private String getGetRecordsTypeAsXml(GetRecordsType getRecordsType) {
    Writer writer = new StringWriter();
    try {
      Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      JAXBElement<GetRecordsType> jaxbElement =
          new JAXBElement<>(
              new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS),
              GetRecordsType.class,
              getRecordsType);
      marshaller.marshal(jaxbElement, writer);
    } catch (JAXBException e) {
      LOGGER.debug(
          "{}: Unable to marshall {} to XML.",
          cswSourceConfiguration.getId(),
          GetRecordsType.class,
          e);
    }
    return writer.toString();
  }

  protected CapabilitiesType getCapabilities() {
    CapabilitiesType caps = null;
    Subject subject = getSystemSubject();
    Csw csw = factory.getClientForSubject(subject);

    try {
      LOGGER.debug("Doing getCapabilities() call for CSW");
      GetCapabilitiesRequest request = new GetCapabilitiesRequest(CswConstants.CSW);
      request.setAcceptVersions(CswConstants.VERSION_2_0_2 + "," + CswConstants.VERSION_2_0_1);
      caps = csw.getCapabilities(request);
    } catch (CswException cswe) {
      LOGGER.info(
          CSW_SERVER_ERROR
              + " Received HTTP code '{}' from server for source with id='{}'. Set Logging to DEBUG for details.",
          cswe.getHttpStatus(),
          cswSourceConfiguration.getId());
      LOGGER.debug(CSW_SERVER_ERROR, cswe);
    } catch (WebApplicationException wae) {
      LOGGER.debug(handleWebApplicationException(wae), wae);
    } catch (Exception ce) {
      handleClientException(ce);
    }
    return caps;
  }

  protected Subject getSystemSubject() {
    return SECURITY.runAsAdmin(SECURITY::getSystemSubject);
  }

  public void configureCswSource() {
    detailLevels = EnumSet.noneOf(ElementSetType.class);

    capabilities = getCapabilities();

    if (null != capabilities) {
      cswVersion = capabilities.getVersion();
      if (CswConstants.VERSION_2_0_1.equals(cswVersion)) {
        setCsw201();
      }
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
              "{}: \"{}\" is not a ElementSetType, Error: {}",
              cswSourceConfiguration.getId(),
              esn,
              iae);
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
      LOGGER.info("{}: Failed to read Content-Types from CSW Server, Error: {}", getId(), e);
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

  protected String handleWebApplicationException(WebApplicationException wae) {
    Response response = wae.getResponse();
    CswException cswException = new CswResponseExceptionMapper().fromResponse(response);

    // Add the CswException message to the error message being logged. Do
    // not include the CswException stack trace because it will not be
    // meaningful since it will not show the root cause of the exception
    // because the ExceptionReport was sent from CSW as an "OK" JAX-RS
    // status rather than an error status.

    return CSW_SERVER_ERROR
        + " "
        + cswSourceConfiguration.getId()
        + "\n"
        + cswException.getMessage();
  }

  @SuppressWarnings("squid:S1192")
  protected String handleClientException(Exception ce) {
    String msg;
    Throwable cause = ce.getCause();
    String sourceId = cswSourceConfiguration.getId();
    if (cause instanceof WebApplicationException) {
      msg = handleWebApplicationException((WebApplicationException) cause);
    } else if (cause instanceof IllegalArgumentException) {
      msg =
          CSW_SERVER_ERROR
              + " Source '"
              + sourceId
              + "'. The URI '"
              + cswSourceConfiguration.getCswUrl()
              + "' does not specify a valid protocol or could not be correctly parsed. "
              + ce.getMessage();
    } else if (cause instanceof SSLHandshakeException) {
      msg =
          CSW_SERVER_ERROR
              + " Source '"
              + sourceId
              + "' with URL '"
              + cswSourceConfiguration.getCswUrl()
              + "': "
              + cause;
    } else if (cause instanceof ConnectException) {
      msg = CSW_SERVER_ERROR + " Source '" + sourceId + "' may not be running.\n" + ce.getMessage();
    } else {
      msg = CSW_SERVER_ERROR + " Source '" + sourceId + "'\n" + ce;
    }

    LOGGER.info(msg);
    LOGGER.debug(msg, ce);
    return msg;
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

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  @Override
  public String getConfigurationPid() {
    return configurationPid;
  }

  @Override
  public void setConfigurationPid(String configurationPid) {
    this.configurationPid = configurationPid;
  }

  @Override
  public Map<String, Set<String>> getSecurityAttributes() {
    return this.cswSourceConfiguration.getSecurityAttributes();
  }

  public void setMetacardTypes(List<MetacardType> types) {}

  /**
   * Set the version to CSW 2.0.1. The schemas don't vary much between 2.0.2 and 2.0.1. The largest
   * difference is the namespace itself. This method tells CXF JAX-RS to transform outgoing messages
   * CSW namespaces to 2.0.1.
   */
  public void setCsw201() {
    Map<String, String> outTransformElements = new HashMap<>();
    outTransformElements.put(
        "{" + CswConstants.CSW_OUTPUT_SCHEMA + "}*", "{http://www.opengis.net/cat/csw}*");
    getRecordsTypeProvider.setOutTransformElements(outTransformElements);
  }

  private void configureEventService() {

    if (!cswSourceConfiguration.isRegisterForEvents()) {
      LOGGER.debug("registerForEvents = false - do not configure site {} for events", this.getId());
      removeEventServiceSubscription();
      return;
    }

    if (StringUtils.isEmpty(cswSourceConfiguration.getEventServiceAddress())) {
      LOGGER.debug(
          "eventServiceAddress is NULL or empty - do not configure site {} for events",
          this.getId());
      return;
    }

    // If filterless subscription has already been configured then do not
    // try to configure
    // another one (because the DDF will allow it and you will get multiple
    // events sent when
    // a single event should be sent)
    if (filterlessSubscriptionId != null) {
      LOGGER.debug(
          "filterless subscription already configured for site {}", filterlessSubscriptionId);
      return;
    }

    initSubscribeClientFactory();
    CswSubscribe cswSubscribe = subscribeClientFactory.getClientForSubject(getSystemSubject());
    GetRecordsType request = createSubscriptionGetRecordsRequest();
    try (Response response = cswSubscribe.createRecordsSubscription(request)) {
      if (Response.Status.OK.getStatusCode() == response.getStatus()) {
        AcknowledgementType acknowledgementType = response.readEntity(AcknowledgementType.class);
        filterlessSubscriptionId = acknowledgementType.getRequestId();
      }
    } catch (CswException e) {
      LOGGER.info(
          "Failed to register a subscription for events from csw source with id of "
              + this.getId());
    }
  }

  private GetRecordsType createSubscriptionGetRecordsRequest() {
    GetRecordsType getRecordsType = new GetRecordsType();
    getRecordsType.setVersion(cswVersion);
    getRecordsType.setService(CswConstants.CSW);
    getRecordsType.setResultType(ResultType.RESULTS);
    getRecordsType.setStartPosition(BigInteger.ONE);
    getRecordsType.setMaxRecords(BigInteger.TEN);
    getRecordsType.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordsType.setOutputSchema("urn:catalog:metacard");
    getRecordsType
        .getResponseHandler()
        .add(SystemBaseUrl.EXTERNAL.constructUrl("csw/subscription/event", true));
    QueryType queryType = new QueryType();
    queryType.setElementSetName(createElementSetName(ElementSetType.FULL));
    ObjectFactory objectFactory = new ObjectFactory();
    getRecordsType.setAbstractQuery(objectFactory.createQuery(queryType));
    return getRecordsType;
  }

  private void removeEventServiceSubscription() {

    if (filterlessSubscriptionId != null && subscribeClientFactory != null) {
      CswSubscribe cswSubscribe = subscribeClientFactory.getClientForSubject(getSystemSubject());
      try {
        cswSubscribe.deleteRecordsSubscription(filterlessSubscriptionId);

      } catch (CswException e) {
        LOGGER.info(
            "Failed to remove filterless subscription registered for id {} for csw source with id of {}",
            filterlessSubscriptionId,
            this.getId());
      }
      filterlessSubscriptionId = null;
    }
  }

  public void setRegisterForEvents(Boolean registerForEvents) {
    this.cswSourceConfiguration.setRegisterForEvents(registerForEvents);
  }

  public void setEventServiceAddress(String eventServiceAddress) {
    this.cswSourceConfiguration.setEventServiceAddress(eventServiceAddress);
  }

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
    removeEventServiceSubscription();
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
