/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.source.reader.GetRecordsMessageBodyReader;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
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
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;

/**
 * CswSource provides a DDF {@link FederatedSource} and {@link ConnectedSource} for CSW 2.0.2
 * services.
 */
public class CswSource extends MaskableImpl
        implements FederatedSource, ConnectedSource, ConfiguredService {

    protected static final String CSW_SERVER_ERROR = "Error received from CSW server.";

    protected static final String CSWURL_PROPERTY = "cswUrl";

    protected static final String ID_PROPERTY = "id";

    protected static final String USERNAME_PROPERTY = "username";

    protected static final String PASSWORD_PROPERTY = "password";

    protected static final String EFFECTIVE_DATE_MAPPING_PROPERTY = "effectiveDateMapping";

    protected static final String CREATED_DATE_MAPPING_PROPERTY = "createdDateMapping";

    protected static final String MODIFIED_DATE_MAPPING_PROPERTY = "modifiedDateMapping";

    protected static final String CONTENT_TYPE_MAPPING_PROPERTY = "contentTypeMapping";

    protected static final String IDENTIFIER_MAPPING_PROPERTY = "identifierMapping";

    protected static final String COORDINATE_ORDER_PROPERTY = "coordinateOrder";

    protected static final String CSW_AXIS_ORDER_PROPERTY = "cswAxisOrder";

    protected static final String POLL_INTERVAL_PROPERTY = "pollInterval";

    protected static final String OUTPUT_SCHEMA_PROPERTY = "outputSchema";

    protected static final String FORCE_SPATIAL_FILTER_PROPERTY = "forceSpatialFilter";

    protected static final String NO_FORCE_SPATIAL_FILTER = "NO_FILTER";

    protected static final String CONNECTION_TIMEOUT_PROPERTY = "connectionTimeout";

    protected static final String RECEIVE_TIMEOUT_PROPERTY = "receiveTimeout";

    protected static final String QUERY_TYPE_QNAME_PROPERTY = "queryTypeQName";

    protected static final String QUERY_TYPE_PREFIX_PROPERTY = "queryTypePrefix";

    private static final Logger LOGGER = LoggerFactory.getLogger(CswSource.class);

    private static final String DEFAULT_CSW_TRANSFORMER_ID = "csw";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String DESCRIPTION = "description";

    private static final String ORGANIZATION = "organization";

    private static final String VERSION = "version";

    private static final String TITLE = "name";

    private static final int CONTENT_TYPE_SAMPLE_SIZE = 50;

    public static final String DISABLE_CN_CHECK_PROPERTY = "disableCnCheck";

    private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

    private static final String USE_POS_LIST_PROPERTY = "usePosList";

    private static Properties describableProperties = new Properties();

    protected String configurationPid;

    static {
        try (InputStream properties = CswSource.class
                .getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(properties);
        } catch (IOException e) {
            LOGGER.info("Failed to load properties", e);
        }
    }

    protected CswSourceConfiguration cswSourceConfiguration;

    protected CswFilterDelegate cswFilterDelegate;

    protected Converter cswTransformProvider;

    protected String forceSpatialFilter = NO_FORCE_SPATIAL_FILTER;

    protected ScheduledFuture<?> availabilityPollFuture;

    protected SecurityManager securityManager;

    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private Set<SourceMonitor> sourceMonitors = new HashSet<SourceMonitor>();

    private Map<String, ContentType> contentTypes = new ConcurrentHashMap<>();

    private ResourceReader resourceReader;

    private DomainType supportedOutputSchemas;

    private Set<ElementSetType> detailLevels;

    private BundleContext context;

    private String description = null;

    private CapabilitiesType capabilities;

    private String cswVersion;

    private SpatialCapabilitiesType spatialCapabilities;

    private ScheduledExecutorService scheduler;

    private AvailabilityTask availabilityTask;

    private boolean isConstraintCql;

    private SecureCxfClientFactory factory;

    protected CswJAXBElementProvider<GetRecordsType> getRecordsTypeProvider;

    protected List<String> jaxbElementClassNames = new ArrayList<String>();

    protected Map<String, String> jaxbElementClassMap = new HashMap<String, String>();

    /**
     * Instantiates a CswSource. This constructor is for unit tests
     *
     * @param context                The {@link BundleContext} from the OSGi Framework
     * @param cswSourceConfiguration the configuration of this source
     * @param provider               transform provider to transform results
     * @param factory                client factory already configured for this source
     */
    public CswSource(BundleContext context, CswSourceConfiguration cswSourceConfiguration,
            CswTransformProvider provider, SecureCxfClientFactory factory) {
        this.context = context;
        this.cswSourceConfiguration = cswSourceConfiguration;
        this.cswTransformProvider = provider;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        this.factory = factory;
    }

    /**
     * Instantiates a CswSource.
     */
    public CswSource() {
        cswSourceConfiguration = new CswSourceConfiguration();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private static JAXBContext initJaxbContext() {

        JAXBContext jaxbContext = null;

        String contextPath = StringUtils
                .join(new String[] {CswConstants.OGC_CSW_PACKAGE, CswConstants.OGC_FILTER_PACKAGE,
                        CswConstants.OGC_GML_PACKAGE, CswConstants.OGC_OWS_PACKAGE}, ":");

        try {
            jaxbContext = JAXBContext.newInstance(contextPath, CswSource.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Failed to initialize JAXBContext", e);
        }

        return jaxbContext;
    }

    /**
     * Initializes the CswSource by connecting to the Server
     */

    public void init() {
        LOGGER.debug("{}: Entering init()", cswSourceConfiguration.getId());
        factory = new SecureCxfClientFactory(cswSourceConfiguration.getCswUrl(), Csw.class,
                initProviders(cswTransformProvider, cswSourceConfiguration), null,
                cswSourceConfiguration.getDisableCnCheck(),
                cswSourceConfiguration.getConnectionTimeout(),
                cswSourceConfiguration.getReceiveTimeout());
        setupAvailabilityPoll();
    }

    /**
     * Clean-up when shutting down the CswSource
     */

    public void destroy(int code) {
        LOGGER.debug("{}: Entering destroy()", cswSourceConfiguration.getId());
        availabilityPollFuture.cancel(true);
        scheduler.shutdownNow();
    }

    protected List<? extends Object> initProviders(Converter cswTransformProvider,
            CswSourceConfiguration cswSourceConfiguration) {
        getRecordsTypeProvider = new CswJAXBElementProvider<GetRecordsType>();
        getRecordsTypeProvider.setMarshallAsJaxbElement(true);

        // Adding class names that need to be marshalled/unmarshalled to
        // jaxbElementClassNames list
        jaxbElementClassNames.add(GetRecordsType.class.getName());
        jaxbElementClassNames.add(CapabilitiesType.class.getName());
        jaxbElementClassNames.add(GetCapabilitiesType.class.getName());
        jaxbElementClassNames.add(GetRecordsResponseType.class.getName());

        getRecordsTypeProvider.setJaxbElementClassNames(jaxbElementClassNames);

        // Adding map entry of <Class Name>,<Qualified Name> to jaxbElementClassMap
        String expandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS)
                .toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.GET_RECORDS, expandedName);
        jaxbElementClassMap.put(GetRecordsType.class.getName(), expandedName);

        String getCapsExpandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.GET_CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.GET_CAPABILITIES, expandedName);
        jaxbElementClassMap.put(GetCapabilitiesType.class.getName(), getCapsExpandedName);

        String capsExpandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.CAPABILITIES, capsExpandedName);
        jaxbElementClassMap.put(CapabilitiesType.class.getName(), capsExpandedName);

        String caps201ExpandedName = new QName("http://www.opengis.net/cat/csw",
                CswConstants.CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.CAPABILITIES, caps201ExpandedName);
        jaxbElementClassMap.put(CapabilitiesType.class.getName(), caps201ExpandedName);

        getRecordsTypeProvider.setJaxbElementClassMap(jaxbElementClassMap);

        GetRecordsMessageBodyReader grmbr = new GetRecordsMessageBodyReader(cswTransformProvider,
                cswSourceConfiguration);
        return Arrays.asList(getRecordsTypeProvider, new CswResponseExceptionMapper(), grmbr);
    }

    /**
     * Reinitializes the CswSource when there is a configuration change. Otherwise, it checks with
     * the server to see if any capabilities have changed.
     *
     * @param configuration The configuration with which to connect to the server
     */

    public void refresh(Map<String, Object> configuration) throws SecurityServiceException {
        LOGGER.debug("{}: Entering refresh()", cswSourceConfiguration.getId());

        if (configuration == null || configuration.isEmpty()) {
            LOGGER.error("Recieved null or empty configuration during refresh for {}: {}",
                    this.getClass().getSimpleName(), cswSourceConfiguration.getId());
            return;
        }

        String idProp = (String) configuration.get(ID_PROPERTY);
        if (StringUtils.isNotBlank(idProp)) {
            setId(idProp);
        }

        String passProp = (String) configuration.get(PASSWORD_PROPERTY);
        if (StringUtils.isNotBlank(passProp)) {
            cswSourceConfiguration.setPassword(passProp);
        }

        String userProp = (String) configuration.get(USERNAME_PROPERTY);
        if (StringUtils.isNotBlank(userProp)) {
            cswSourceConfiguration.setUsername(userProp);
        }

        Integer newConnTimeout = (Integer) configuration.get(CONNECTION_TIMEOUT_PROPERTY);
        if (newConnTimeout != null) {
            cswSourceConfiguration.setConnectionTimeout(newConnTimeout);
        }

        Integer newRecTimeout = (Integer) configuration.get(RECEIVE_TIMEOUT_PROPERTY);
        if (newRecTimeout != null) {
            cswSourceConfiguration.setReceiveTimeout(newRecTimeout);
        }

        String schemaProp = (String) configuration.get(OUTPUT_SCHEMA_PROPERTY);
        if (StringUtils.isNotBlank(schemaProp)) {
            String oldOutputSchema = cswSourceConfiguration.getOutputSchema();
            cswSourceConfiguration.setOutputSchema(schemaProp);

            LOGGER.debug("{}: new output schema: {}", cswSourceConfiguration.getId(),
                    cswSourceConfiguration.getOutputSchema());
            LOGGER.debug("{}: old output schema: {}", cswSourceConfiguration.getId(),
                    oldOutputSchema);
        }

        String queryTypeQName = (String) configuration.get(QUERY_TYPE_QNAME_PROPERTY);
        if (StringUtils.isNotBlank(queryTypeQName)) {
            cswSourceConfiguration.setQueryTypeQName(queryTypeQName);
        }

        String queryTypePrefix = (String) configuration.get(QUERY_TYPE_PREFIX_PROPERTY);
        if (StringUtils.isNotBlank(queryTypePrefix)) {
            cswSourceConfiguration.setQueryTypePrefix(queryTypePrefix);
        }

        String identifierMapping = (String) configuration.get(IDENTIFIER_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(identifierMapping)) {
            cswSourceConfiguration.setIdentifierMapping(identifierMapping);
        }

        Boolean sslProp = (Boolean) configuration
                .get(DISABLE_CN_CHECK_PROPERTY);
        if (sslProp != null) {
            cswSourceConfiguration.setDisableCnCheck(sslProp);
        }

        String coordinateOrder = (String) configuration.get(COORDINATE_ORDER_PROPERTY);
        if (coordinateOrder != null) {
            setCoordinateOrder(coordinateOrder);
            configuration.put(CSW_AXIS_ORDER_PROPERTY, cswSourceConfiguration.getCswAxisOrder());
        }

        Boolean posListProp = (Boolean) configuration.get(USE_POS_LIST_PROPERTY);
        if (posListProp != null) {
            cswSourceConfiguration.setUsePosList(posListProp);
        }

        String spatialFilter = (String) configuration.get(FORCE_SPATIAL_FILTER_PROPERTY);
        if (StringUtils.isBlank(spatialFilter)) {
            spatialFilter = NO_FORCE_SPATIAL_FILTER;
        }
        forceSpatialFilter = spatialFilter;

        String createdProp = (String) configuration.get(CREATED_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(createdProp)) {
            cswSourceConfiguration.setCreatedDateMapping(createdProp);
        }

        String effectiveProp = (String) configuration.get(EFFECTIVE_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(effectiveProp)) {
            cswSourceConfiguration.setEffectiveDateMapping(effectiveProp);
        }

        String modifiedProp = (String) configuration.get(MODIFIED_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(modifiedProp)) {
            cswSourceConfiguration.setModifiedDateMapping(modifiedProp);
        }

        String currentContentTypeMapping = ((String) configuration
                .get(CONTENT_TYPE_MAPPING_PROPERTY));

        if (StringUtils.isNotBlank(currentContentTypeMapping)) {
            String previousContentTypeMapping = cswSourceConfiguration.getContentTypeMapping();
            LOGGER.debug("{}: Previous content type mapping: {}.", cswSourceConfiguration.getId(),
                    previousContentTypeMapping);
            currentContentTypeMapping = currentContentTypeMapping.trim();
            if (!currentContentTypeMapping.equals(previousContentTypeMapping)) {
                LOGGER.debug("{}: The content type has been updated from {} to {}.",
                        cswSourceConfiguration.getId(), previousContentTypeMapping,
                        currentContentTypeMapping);
                contentTypes.clear();
            }
        } else {
            currentContentTypeMapping = CswRecordMetacardType.CSW_TYPE;
        }

        cswSourceConfiguration.setContentTypeMapping(currentContentTypeMapping);

        LOGGER.debug("{}: Current content type mapping: {}.", cswSourceConfiguration.getId(),
                currentContentTypeMapping);

        Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_PROPERTY);

        if (newPollInterval != null && !newPollInterval
                .equals(cswSourceConfiguration.getPollIntervalMinutes())) {
            LOGGER.debug("Poll Interval was changed for source {}.",
                    cswSourceConfiguration.getId());
            cswSourceConfiguration.setPollIntervalMinutes(newPollInterval);
            availabilityPollFuture.cancel(true);
            setupAvailabilityPoll();
        }

        String cswUrlProp = (String) configuration.get(CSWURL_PROPERTY);
        if (StringUtils.isNotBlank(cswUrlProp) &&
                !cswUrlProp.equals(cswSourceConfiguration.getCswUrl())) {
            cswSourceConfiguration.setCswUrl(cswUrlProp);
            factory = new SecureCxfClientFactory(cswUrlProp, Csw.class,
                    initProviders(cswTransformProvider, cswSourceConfiguration), null,
                    cswSourceConfiguration.getDisableCnCheck(),
                    cswSourceConfiguration.getConnectionTimeout(),
                    cswSourceConfiguration.getReceiveTimeout());
        }
        configureCswSource();
    }

    protected void setupAvailabilityPoll() {
        LOGGER.debug("Setting Availability poll task for {} minute(s) on Source {}",
                cswSourceConfiguration.getPollIntervalMinutes(), cswSourceConfiguration.getId());
        CswSourceAvailabilityCommand command = new CswSourceAvailabilityCommand();
        long interval = TimeUnit.MINUTES.toMillis(cswSourceConfiguration.getPollIntervalMinutes());
        if (availabilityPollFuture == null || availabilityPollFuture.isCancelled()) {
            if (availabilityTask == null) {
                availabilityTask = new AvailabilityTask(interval, command,
                        cswSourceConfiguration.getId());
            } else {
                availabilityTask.setInterval(interval);
            }

            // Run the availability check immediately prior to scheduling it in a thread.
            // This is necessary to allow the catalog framework to have the correct
            // availability when the source is bound
            availabilityTask.run();
            // Schedule the availability check every 1 second. The actually call to
            // the remote server will only occur if the pollInterval has
            // elapsed.
            availabilityPollFuture = scheduler
                    .scheduleWithFixedDelay(availabilityTask, AvailabilityTask.NO_DELAY,
                            AvailabilityTask.ONE_SECOND, TimeUnit.SECONDS);
        } else {
            LOGGER.debug("No changes being made on the poller.");
        }

    }

    public void setConnectionTimeout(Integer timeout) {
        this.cswSourceConfiguration.setConnectionTimeout(timeout);
    }

    public Integer getConnectionTimeout() {
        return this.cswSourceConfiguration.getConnectionTimeout();
    }

    public void setReceiveTimeout(Integer timeout) {
        this.cswSourceConfiguration.setReceiveTimeout(timeout);
    }

    public Integer getReceiveTimeout() {
        return this.cswSourceConfiguration.getReceiveTimeout();
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return new HashSet<ContentType>(contentTypes.values());
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

    public void setQueryTypeQName(String queryTypeQName) {
        cswSourceConfiguration.setQueryTypeQName(queryTypeQName);
        LOGGER.debug("Setting queryTypeQName to: {}", queryTypeQName);
    }

    public void setQueryTypePrefix(String queryTypePrefix) {
        cswSourceConfiguration.setQueryTypePrefix(queryTypePrefix);
        LOGGER.debug("Setting queryTypePrefix to: {}", queryTypePrefix);
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
        try {
            Subject subject = (Subject) queryRequest
                    .getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            Csw csw = getClient(subject);
            return query(queryRequest, ElementSetType.FULL, null, csw);
        } catch (SecurityServiceException e) {
            throw new UnsupportedQueryException("Could not get client for CSW Source: " + getId(),
                    e);
        }
    }

    protected SourceResponse query(QueryRequest queryRequest, ElementSetType elementSetName,
            List<QName> elementNames, Csw csw) throws UnsupportedQueryException {

        Query query = queryRequest.getQuery();
        LOGGER.debug("{}: Received query:\n{}", cswSourceConfiguration.getId(), query);

        GetRecordsType getRecordsType = createGetRecordsRequest(query, elementSetName,
                elementNames);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: GetRecords request:\n {}", cswSourceConfiguration.getId(),
                    getGetRecordsTypeAsXml(getRecordsType));
        }

        LOGGER.debug("{}: Sending query to: {}", cswSourceConfiguration.getId(),
                cswSourceConfiguration.getCswUrl());

        List<Result> results = null;
        Long totalHits = 0L;

        try {

            CswRecordCollection cswRecordCollection = csw.getRecords(getRecordsType);

            if (cswRecordCollection == null) {
                throw new UnsupportedQueryException("Invalid results returned from server");
            }
            this.availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
            LOGGER.debug("{}: Received [{}] record(s) of the [{}] record(s) matched from {}.",
                    cswSourceConfiguration.getId(),
                    cswRecordCollection.getNumberOfRecordsReturned(),
                    cswRecordCollection.getNumberOfRecordsMatched(),
                    cswSourceConfiguration.getCswUrl());

            results = createResults(cswRecordCollection);
            totalHits = cswRecordCollection.getNumberOfRecordsMatched();
        } catch (CswException cswe) {
            LOGGER.error(CSW_SERVER_ERROR, cswe);
            throw new UnsupportedQueryException(CSW_SERVER_ERROR, cswe);
        } catch (WebApplicationException wae) {
            String msg = handleWebApplicationException(wae);
            throw new UnsupportedQueryException(msg, wae);
        } catch (Exception ce) {
            String msg = handleClientException(ce);
            throw new UnsupportedQueryException(msg, ce);
        }

        LOGGER.debug("{}: Adding {} result(s) to the source response.",
                cswSourceConfiguration.getId(), results.size());

        SourceResponseImpl sourceResponse = new SourceResponseImpl(queryRequest, results,
                totalHits);
        addContentTypes(sourceResponse);
        return sourceResponse;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(describableProperties.getProperty(DESCRIPTION))
                .append(System.getProperty("line.separator")).append(description);
        return sb.toString();
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

    @Override
    public Set<String> getOptions(Metacard arg0) {
        return null;
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return null;
    }

    @Override
    public ResourceResponse retrieveResource(URI resourceUri,
            Map<String, Serializable> requestProperties)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {

        LOGGER.debug("retrieving resource at : {}", resourceUri);
        return resourceReader.retrieveResource(resourceUri, requestProperties);
    }

    public void setCswUrl(String cswUrl) {
        LOGGER.debug("Setting cswUrl to {}", cswUrl);

        cswSourceConfiguration.setCswUrl(cswUrl);
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

        LOGGER.debug("{}: Setting CSW coordinate order to {}", cswSourceConfiguration.getId(), cswAxisOrder);
        cswSourceConfiguration.setCswAxisOrder(cswAxisOrder);
    }

    public void setUsePosList(Boolean usePosList) {
        cswSourceConfiguration.setUsePosList(usePosList);
        LOGGER.debug("Using posList rather than individual pos elements?: {}", usePosList);
    }

    public void setIsCqlForced(Boolean isCqlForced) {
        cswSourceConfiguration.setIsCqlForced(isCqlForced);
    }

    public void setEffectiveDateMapping(String effectiveDateMapping) {
        cswSourceConfiguration.setEffectiveDateMapping(effectiveDateMapping);
    }

    public void setCreatedDateMapping(String createdDateMapping) {
        cswSourceConfiguration.setCreatedDateMapping(createdDateMapping);
    }

    public void setModifiedDateMapping(String modifiedDateMapping) {
        cswSourceConfiguration.setModifiedDateMapping(modifiedDateMapping);
    }

    public void setResourceUriMapping(String resourceUriMapping) {
        cswSourceConfiguration.setResourceUriMapping(resourceUriMapping);
    }

    public void setThumbnailMapping(String thumbnailMapping) {
        cswSourceConfiguration.setThumbnailMapping(thumbnailMapping);
    }

    public void setIdentifierMapping(String identifierMapping) {
        cswSourceConfiguration.setIdentifierMapping(identifierMapping);
    }

    public void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setContentTypeMapping(String contentTypeMapping) {
        if (StringUtils.isEmpty(contentTypeMapping)) {
            contentTypeMapping = CswRecordMetacardType.CSW_TYPE;
        }
        cswSourceConfiguration.setContentTypeMapping(contentTypeMapping);
    }

    public void setPollInterval(Integer interval) {
        this.cswSourceConfiguration.setPollIntervalMinutes(interval);
    }

    public Converter getCswTransformProvider() {
        return this.cswTransformProvider;
    }

    public void setCswTransformProvider(Converter provider) {
        this.cswTransformProvider = provider;
    }

    public String getForceSpatialFilter() {
        return forceSpatialFilter;
    }

    public void setForceSpatialFilter(String forceSpatialFilter) {
        this.forceSpatialFilter = forceSpatialFilter;
    }

    private GetRecordsType createGetRecordsRequest(Query query, ElementSetType elementSetName,
            List<QName> elementNames) throws UnsupportedQueryException {
        GetRecordsType getRecordsType = new GetRecordsType();
        getRecordsType.setVersion(cswVersion);
        getRecordsType.setService(CswConstants.CSW);
        getRecordsType.setResultType(ResultType.RESULTS);
        getRecordsType.setStartPosition(BigInteger.valueOf(query.getStartIndex()));
        getRecordsType.setMaxRecords(BigInteger.valueOf(query.getPageSize()));
        getRecordsType.setOutputFormat(MediaType.APPLICATION_XML);
        if (!isOutputSchemaSupported()) {
            String msg = "CSW Source: " + cswSourceConfiguration.getId()
                    + " does not support output schema: " + cswSourceConfiguration.getOutputSchema()
                    + ".";
            throw new UnsupportedQueryException(msg);
        }
        getRecordsType.setOutputSchema(cswSourceConfiguration.getOutputSchema());
        getRecordsType.setAbstractQuery(createQuery(query, elementSetName, elementNames));
        return getRecordsType;
    }

    private ElementSetNameType createElementSetName(ElementSetType type) {
        ElementSetNameType elementSetNameType = new ElementSetNameType();
        elementSetNameType.setValue(type);
        return elementSetNameType;
    }

    private JAXBElement<QueryType> createQuery(Query query, ElementSetType elementSetType,
            List<QName> elementNames) throws UnsupportedQueryException {
        QueryType queryType = new QueryType();

        QName queryTypeQName = null;
        try {
            queryTypeQName = new QName(
                    QName.valueOf(cswSourceConfiguration.getQueryTypeQName()).getNamespaceURI(),
                    QName.valueOf(cswSourceConfiguration.getQueryTypeQName()).getLocalPart(),
                    cswSourceConfiguration.getQueryTypePrefix());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unable to parse query type QName of {}.  Defaulting to CSW Record",
                    cswSourceConfiguration.getQueryTypeQName());
            queryTypeQName = new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                    CswConstants.CSW_RECORD_LOCAL_NAME, CswConstants.CSW_NAMESPACE_PREFIX);
        }

        queryType.setTypeNames(Arrays.asList(new QName[] {queryTypeQName}));
        if (null != elementSetType) {
            queryType.setElementSetName(createElementSetName(elementSetType));
        } else if (!CollectionUtils.isEmpty(elementNames)) {
            queryType.setElementName(elementNames);
        } else {
            queryType.setElementSetName(createElementSetName(ElementSetType.FULL));
        }
        SortByType sortBy = createSortBy(query);
        if (sortBy != null) {
            queryType.setSortBy(sortBy);
        }
        QueryConstraintType constraint = createQueryConstraint(query);
        if (null != constraint) {
            queryType.setConstraint(constraint);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createQuery(queryType);
    }

    private SortByType createSortBy(Query query) {

        SortByType sortBy = null;

        if (query.getSortBy() != null) {
            sortBy = new SortByType();
            SortPropertyType sortProperty = new SortPropertyType();
            PropertyNameType propertyName = new PropertyNameType();
            String propName = query.getSortBy().getPropertyName().getPropertyName();
            if (propName != null) {
                if (Result.TEMPORAL.equals(propName) || Metacard.ANY_DATE.equals(propName)) {
                    propName = Metacard.MODIFIED;
                } else if (Result.RELEVANCE.equals(propName) || Metacard.ANY_TEXT
                        .equals(propName)) {
                    propName = Metacard.TITLE;
                } else if (Result.DISTANCE.equals(propName) || Metacard.ANY_GEO.equals(propName)) {
                    return null;
                }
            }

            propertyName.setContent(
                    Arrays.asList((Object) cswFilterDelegate.mapPropertyName(propName)));
            sortProperty.setPropertyName(propertyName);
            if (SortOrder.DESCENDING.equals(query.getSortBy().getSortOrder())) {
                sortProperty.setSortOrder(SortOrderType.DESC);
            } else {
                sortProperty.setSortOrder(SortOrderType.ASC);
            }
            sortBy.getSortProperty().add(sortProperty);
        }

        return sortBy;
    }

    private QueryConstraintType createQueryConstraint(Query query)
            throws UnsupportedQueryException {
        FilterType filter = createFilter(query);
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
            LOGGER.error("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
            return new FilterType();
        }
        if (null == capabilities.getFilterCapabilities()) {
            LOGGER.warn(
                    "{}: CSW Source did not provide Filter Capabilities, unable to preform query.",
                    cswSourceConfiguration.getId());
            throw new UnsupportedQueryException(cswSourceConfiguration.getId()
                    + ": CSW Source did not provide Filter Capabilities, unable to preform query.");
        }

        return this.filterAdapter.adapt(query, cswFilterDelegate);
    }

    protected List<Result> createResults(CswRecordCollection cswRecordCollection) {
        List<Result> results = new ArrayList<Result>();

        LOGGER.debug("Found {} metacard(s) in the CswRecordCollection.",
                cswRecordCollection.getCswRecords().size());

        String transformerId = getMetadataTransformerId();

        MetadataTransformer transformer = lookupMetadataTransformer(transformerId);

        for (Metacard metacard : cswRecordCollection.getCswRecords()) {
            MetacardImpl wrappedMetacard = new MetacardImpl(metacard);
            wrappedMetacard.setSourceId(getId());
            if (wrappedMetacard.getAttribute(Metacard.RESOURCE_DOWNLOAD_URL) != null
                    && wrappedMetacard.getAttribute(Metacard.RESOURCE_DOWNLOAD_URL).getValue()
                    != null) {
                wrappedMetacard.setAttribute(Metacard.RESOURCE_URI,
                        wrappedMetacard.getAttribute(Metacard.RESOURCE_DOWNLOAD_URL).getValue());
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
     * Transforms the Metacard created from the CSW Record using the transformer specified by its
     * ID.
     *
     * @param metacard
     * @return
     */
    protected Metacard transform(Metacard metacard, MetadataTransformer transformer) {

        if (metacard == null) {
            throw new IllegalArgumentException(
                    cswSourceConfiguration.getId() + ": Metacard is null.");
        }

        try {
            return transformer.transform(metacard);
        } catch (CatalogTransformerException e) {
            LOGGER.warn("{} :Metadata Transformation Failed for metacard: {}",
                    cswSourceConfiguration.getId(), metacard.getId(), e);
        }
        return metacard;

    }

    protected MetadataTransformer lookupMetadataTransformer(String transformerId) {
        ServiceReference<?>[] refs = null;

        try {
            refs = context.getServiceReferences(MetadataTransformer.class.getName(),
                    "(" + Constants.SERVICE_ID + "=" + transformerId + ")");
        } catch (InvalidSyntaxException e) {
            LOGGER.warn(cswSourceConfiguration.getId() + ": Invalid transformer ID.", e);
            return null;
        }

        if (refs == null || refs.length == 0) {
            LOGGER.info("{}: Metadata Transformer " + transformerId + " not found.",
                    cswSourceConfiguration.getId());
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

            JAXBElement<GetRecordsType> jaxbElement = new JAXBElement<GetRecordsType>(
                    new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS),
                    GetRecordsType.class, getRecordsType);
            marshaller.marshal(jaxbElement, writer);
        } catch (JAXBException e) {
            LOGGER.error("{}: Unable to marshall {} to XML.", cswSourceConfiguration.getId(),
                    GetRecordsType.class, e);
        }
        return writer.toString();
    }

    protected CapabilitiesType getCapabilities() throws SecurityServiceException {
        CapabilitiesType caps = null;
        Csw csw = getClient(null);

        try {
            LOGGER.debug("Doing getCapabilities() call for CSW");
            GetCapabilitiesRequest request = new GetCapabilitiesRequest(CswConstants.CSW);
            request.setAcceptVersions(
                    CswConstants.VERSION_2_0_2 + "," + CswConstants.VERSION_2_0_1);
            caps = csw.getCapabilities(request);
        } catch (CswException cswe) {
            LOGGER.error(CSW_SERVER_ERROR, cswe);
        } catch (WebApplicationException wae) {
            LOGGER.error(wae.getMessage(), wae);
            handleWebApplicationException(wae);
        } catch (Exception ce) {
            handleClientException(ce);
        }
        return caps;
    }

    public void configureCswSource() throws SecurityServiceException {
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
            LOGGER.debug("{}: {}", cswSourceConfiguration.getId(), capabilities.toString());
        } else {
            LOGGER.error("{}: CSW Server did not return any capabilities.",
                    cswSourceConfiguration.getId());
        }
    }

    private Operation getOperation(OperationsMetadata operations, String operation) {
        for (Operation op : operations.getOperation()) {
            if (operation.equals(op.getName())) {
                return op;
            }
        }

        LOGGER.error("{}: CSW Server did not contain getRecords operation",
                cswSourceConfiguration.getId());
        return null;

    }

    /**
     * Parses the getRecords {@link Operation} to understand the capabilities of the org.codice.ddf.spatial.ogc.csw.catalog.common.Csw Server. A
     * sample GetRecords Operation may look like this:
     * <p/>
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
     * @param capabilitiesType The capabilities the org.codice.ddf.spatial.ogc.csw.catalog.common.Csw Server supports
     */
    private void readGetRecordsOperation(CapabilitiesType capabilitiesType) {
        OperationsMetadata operationsMetadata = capabilitiesType.getOperationsMetadata();
        if (null == operationsMetadata) {
            LOGGER.error("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
            return;
        }

        description = capabilitiesType.getServiceIdentification().getAbstract();

        Operation getRecordsOp = getOperation(operationsMetadata, CswConstants.GET_RECORDS);

        if (null == getRecordsOp) {
            LOGGER.error("{}: CSW Source contains no getRecords Operation",
                    cswSourceConfiguration.getId());
            return;
        }

        this.supportedOutputSchemas = getParameter(getRecordsOp,
                CswConstants.OUTPUT_SCHEMA_PARAMETER);

        DomainType constraintLanguage = getParameter(getRecordsOp,
                CswConstants.CONSTRAINT_LANGUAGE_PARAMETER);
        if (null != constraintLanguage) {
            DomainType outputFormatValues = getParameter(getRecordsOp,
                    CswConstants.OUTPUT_FORMAT_PARAMETER);
            DomainType resultTypesValues = getParameter(getRecordsOp,
                    CswConstants.RESULT_TYPE_PARAMETER);
            readSetDetailLevels(
                    getParameter(getRecordsOp, CswConstants.ELEMENT_SET_NAME_PARAMETER));

            List<String> constraints = new ArrayList<String>();
            for (String s : constraintLanguage.getValue()) {
                constraints.add(s.toLowerCase());
            }

            if (constraints.contains(CswConstants.CONSTRAINT_LANGUAGE_CQL.toLowerCase())
                    && !constraints
                    .contains(CswConstants.CONSTRAINT_LANGUAGE_FILTER.toLowerCase())) {
                isConstraintCql = true;
            } else {
                isConstraintCql = false;
            }

            setFilterDelegate(new CswRecordMetacardType(), getRecordsOp,
                    capabilitiesType.getFilterCapabilities(), outputFormatValues, resultTypesValues,
                    cswSourceConfiguration);

            spatialCapabilities = capabilitiesType.getFilterCapabilities().getSpatialCapabilities();

            if (!NO_FORCE_SPATIAL_FILTER.equals(forceSpatialFilter)) {
                SpatialOperatorType sot = new SpatialOperatorType();
                SpatialOperatorNameType sont = SpatialOperatorNameType
                        .fromValue(forceSpatialFilter);
                sot.setName(sont);
                sot.setGeometryOperands(cswFilterDelegate.getGeoOpsForSpatialOp(sont));
                SpatialOperatorsType spatialOperators = new SpatialOperatorsType();
                spatialOperators.setSpatialOperator(Arrays.asList(sot));
                cswFilterDelegate.setSpatialOps(spatialOperators);
            }
        }
    }

    /**
     * Sets the {@link ddf.catalog.filter.FilterDelegate} used by the CswSource. May be overridden
     * in order to provide a custom ddf.catalog.filter.FilterDelegate implementation.
     *
     * @param metacardType
     * @param getRecordsOp
     * @param filterCapabilities
     * @param outputFormatValues
     * @param resultTypesValues
     * @param cswSourceConfiguration
     */
    protected void setFilterDelegate(MetacardType metacardType, Operation getRecordsOp,
            FilterCapabilities filterCapabilities, DomainType outputFormatValues,
            DomainType resultTypesValues, CswSourceConfiguration cswSourceConfiguration) {
        LOGGER.trace("Setting cswFilterDelegate to default CswFilterDelegate");

        cswFilterDelegate = new CswFilterDelegate(metacardType, getRecordsOp, filterCapabilities,
                outputFormatValues, resultTypesValues, cswSourceConfiguration);
    }

    private void readSetDetailLevels(DomainType elementSetNamesValues) {
        if (null != elementSetNamesValues) {
            for (String esn : elementSetNamesValues.getValue()) {
                try {
                    detailLevels.add(ElementSetType.fromValue(esn.toLowerCase()));
                } catch (IllegalArgumentException iae) {
                    LOGGER.warn("{}: \"{}\" is not a ElementSetType, Error: {}",
                            cswSourceConfiguration.getId(), esn, iae);
                }
            }
        }
    }

    protected void loadContentTypes() {
        Filter filter = filterBuilder.attribute(CswConstants.ANY_TEXT).is().like()
                .text(CswConstants.WILD_CARD);
        Query query = new QueryImpl(filter, 1, CONTENT_TYPE_SAMPLE_SIZE, null, true, 0);
        QueryRequest queryReq = new QueryRequestImpl(query);

        try {
            query(queryReq);
        } catch (UnsupportedQueryException e) {
            LOGGER.error("{}: Failed to read Content-Types from CSW Server, Error: {}", getId(), e);
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
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("{}: CSW Operation \"{}\" did not contain the \"{}\" parameter",
                    new Object[] {cswSourceConfiguration.getId(), operation.getName(), name});
        }
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
        String msg = "Error received from CSW Server " + cswSourceConfiguration.getId() + "\n"
                + cswException.getMessage();
        LOGGER.error(msg, wae);

        return msg;
    }

    private String handleClientException(Exception ce) {
        String msg = "";
        if (ce.getCause() instanceof WebApplicationException) {
            msg = handleWebApplicationException((WebApplicationException) ce.getCause());
        } else {
            msg = "Error received from CSW Server " + cswSourceConfiguration.getId();
        }
        LOGGER.error(msg);
        return msg;
    }

    private void availabilityChanged(boolean isAvailable) {

        if (isAvailable) {
            LOGGER.info("CSW source {} is available.", cswSourceConfiguration.getId());
        } else {
            LOGGER.info("CSW source {} is unavailable.", cswSourceConfiguration.getId());
        }

        for (SourceMonitor monitor : this.sourceMonitors) {
            if (isAvailable) {
                LOGGER.debug("Notifying source monitor that CSW source {} is available.",
                        cswSourceConfiguration.getId());
                monitor.setAvailable();
            } else {
                LOGGER.debug("Notifying source monitor that CSW source {} is unavailable.",
                        cswSourceConfiguration.getId());
                monitor.setUnavailable();
            }
        }
    }

    private boolean isOutputSchemaSupported() {
        return this.cswSourceConfiguration.getOutputSchema() != null
                && this.supportedOutputSchemas != null ?
                this.supportedOutputSchemas.getValue()
                        .contains(cswSourceConfiguration.getOutputSchema()) :
                false;
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

    /**
     * Callback class to check the Availability of the CswSource.
     * <p/>
     * NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
     * have an AvailabilityTask that cached each Source's availability. Until that is done, allow
     * the command to handle the logic of managing availability.
     */
    private class CswSourceAvailabilityCommand implements AvailabilityCommand {

        @Override
        public boolean isAvailable() {
            LOGGER.debug("Checking availability for source {} ", cswSourceConfiguration.getId());
            boolean oldAvailability = CswSource.this.isAvailable();
            boolean newAvailability = false;
            try {
                // Simple "ping" to ensure the source is responding
                newAvailability = (getCapabilities() != null);
                if (oldAvailability != newAvailability) {
                    availabilityChanged(newAvailability);
                    // If the source becomes available, configure it.
                    if (newAvailability) {
                        configureCswSource();
                    }
                }
            } catch (SecurityServiceException sse) {
                LOGGER.error("Could not get client for the endpointURL.", sse);
            }
            return newAvailability;
        }

    }

    /**
     * Creates a new client using Basic Auth or a Security Subject. If it cannot, it
     * will instead create an unsecure client, if possible.
     *
     * @param subj - the Security Subject
     * @return a new Csw client
     * @throws SecurityServiceException
     */
    protected Csw getClient(Subject subj) throws SecurityServiceException {
        Csw csw;
        String username = cswSourceConfiguration.getUsername();
        String password = cswSourceConfiguration.getPassword();
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            csw = (Csw) factory.getClientForBasicAuth(username, password);
        } else if (subj != null) {
            csw = (Csw) factory.getClientForSubject(subj);
        } else {
            csw = (Csw) factory.getUnsecuredClient();
        }
        return csw;
    }

    /**
     * Set the version to CSW 2.0.1. The schemas don't vary much between 2.0.2 and 2.0.1. The
     * largest difference is the namespace itself. This method tells CXF JAX-RS to transform
     * outgoing messages CSW namespaces to 2.0.1.
     */
    public void setCsw201() {
        Map<String, String> outTransformElements = new HashMap<String, String>();
        outTransformElements.put("{" + CswConstants.CSW_OUTPUT_SCHEMA + "}*",
                "{http://www.opengis.net/cat/csw}*");
        getRecordsTypeProvider.setOutTransformElements(outTransformElements);
    }
}
